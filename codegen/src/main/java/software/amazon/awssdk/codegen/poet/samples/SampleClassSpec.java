/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.codegen.poet.samples;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static software.amazon.awssdk.utils.internal.CodegenNamingUtils.lowercaseFirstChar;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtension;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.utils.PaginatorUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.utils.StringUtils;

public class SampleClassSpec implements ClassSpec {

    private final IntermediateModel model;
    private final String modelPackage;
    private final Map<String, Sample> samples;
    private final ClassName sampleClassName;
    private final PoetExtension poetExtensions;

    public SampleClassSpec(IntermediateModel model) {
        this.model = model;
        this.modelPackage = model.getMetadata().getFullModelPackageName();
        this.samples = model.getSamples();
        this.poetExtensions = new PoetExtension(model);
        this.sampleClassName = poetExtensions.getSampleClass();
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder typeSpecBuilder = PoetUtils.createClassBuilder(className());
        typeSpecBuilder.addModifiers(PUBLIC);
        typeSpecBuilder.addField(clientClassName(), "client", PRIVATE, FINAL);
        typeSpecBuilder.addMethod(constructor());
        typeSpecBuilder.addMethods(sampleMethods());

        return typeSpecBuilder.build();
    }

    private MethodSpec constructor() {
        MethodSpec.Builder ctor = MethodSpec.constructorBuilder()
                                            .addModifiers(PUBLIC);
        ctor.addStatement("this.client = $T.builder().build()", clientClassName());
        return ctor.build();
    }

    private List<MethodSpec> sampleMethods() {
        return samples.entrySet()
                      .stream()
                      .flatMap(this::samples)
                      .collect(Collectors.toList());
    }

    private Stream<MethodSpec> samples(Map.Entry<String, Sample> sample) {
        List<MethodSpec> methods = new ArrayList<>();
        methods.add(sampleMethod(sample));
        return methods.stream();
    }

    private MethodSpec sampleMethod(Map.Entry<String, Sample> sample) {
        String sampleName = sample.getKey();
        Sample sampleDefinition = sample.getValue();

        String documentation = sampleDefinition.getDocumentation();
        MethodSpec.Builder builder = MethodSpec.methodBuilder(lowercaseFirstChar(sampleName))
                                               .addModifiers(PUBLIC)
                                               .addJavadoc(documentation);

        List<Instruction> instructions = sampleDefinition.getInstructions();

        instructions.forEach(instruction -> addCodeForEachInstruction(builder, CodeBlock.builder(), instruction));

        return builder.build();
    }

    private void addCodeForEachInstruction(MethodSpec.Builder builder,
                                           CodeBlock.Builder codeBlockBuilder, Instruction instruction) {
        codeBlockBuilder.add("\n");
        switch (instruction.getType()) {
            case SERVICE_OPERATION:
                addServiceOperation(builder, codeBlockBuilder, instruction);
                break;
            case SDK_OPERATION:
                addSdkOperation(builder, codeBlockBuilder, instruction);
                break;
            case SERVICE_WAITER:
                addWaiterOperation(builder, codeBlockBuilder, instruction);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported instruction: " + instruction.getType());
        }
    }

    private void addWaiterOperation(MethodSpec.Builder methodBuilder, CodeBlock.Builder codeBlockBuilder,
                                    Instruction instruction) {
        String name = instruction.getName();

        String waiterMethodName = getWaiterMethodName(name);

        codeBlockBuilder.add("client.waiter().$N(request->request", waiterMethodName);

        instruction.getInput().forEach(i -> {
            // List<ShapeModel> shapesByC2jName = Utils.findMemberShapeModelByC2jNameIfExists(i.getName());
            //String fluentSetterMethodName = fluentSetterMethod(operationModel, i);
            // FIXME: should use member model to find out the setter name instead of lower case first char
            switch (i.getType()) {
                case STRING:
                    codeBlockBuilder.add(".$N($S)",
                                         lowercaseFirstChar(i.getName()), i.getValue());
                    break;
                case REFERENCE:
                    codeBlockBuilder.add(".$N($N())",
                                         lowercaseFirstChar(i.getName()), i.getValue());
                    break;
                default:
                    throw new UnsupportedOperationException("unsupported type " + i.getType());
            }
        });
        codeBlockBuilder.add(");");
        methodBuilder.addCode(codeBlockBuilder.build());

    }

    private ClassName waiterClassName() {
        return poetExtensions.getSyncWaiterClass();
    }

    private String getWaiterMethodName(String waiterMethodName) {
        return "waitUntil" + waiterMethodName;
    }

    private void addSdkOperation(MethodSpec.Builder builder, CodeBlock.Builder codeBlockBuilder, Instruction instruction) {

        String name = instruction.getName();
        if (!name.equals("print")) {
            throw new UnsupportedOperationException("Unsupported instruction: " + instruction.getName());
        }
        codeBlockBuilder.add("System.out.println(");
        instruction.getInput().forEach(input -> {
            switch (input.getType()) {
                case STRING:
                    codeBlockBuilder.add("$S", input.getValue());
                    break;
                case REFERENCE:
                    codeBlockBuilder.add("+ $N", input.getValue());
                    break;
                default:
                    throw new UnsupportedOperationException("unsupported type " + input.getType());
            }
        });
        codeBlockBuilder.add(")");
        builder.addStatement(codeBlockBuilder.build());
    }

    private void addServiceOperation(MethodSpec.Builder methodBuilder, CodeBlock.Builder codeBlockBuilder,
                                     Instruction instruction) {
        String operationName = instruction.getName();
        OperationModel operationModel = model.getOperation(operationName);
        if (StringUtils.isNotBlank(instruction.getDocumentation())) {
            codeBlockBuilder.add("// $L", instruction.getDocumentation());
            codeBlockBuilder.add("\n");
        }

        OutputConsumer outputConsumer = instruction.getOutputConsumer();
        if (outputConsumer != null) {
            String outputVariable = outputConsumer.getOutputVariable();
            ClassName responseType = ClassName.get(modelPackage, operationModel.getReturnType().getReturnType());

            codeBlockBuilder.add("$T $N = ", responseType, outputVariable);
        }

        if (instruction.isUsePaginator()) {
            String paginatorMethod = PaginatorUtils.getPaginatedMethodName(operationName);
            codeBlockBuilder.add("client.$N", lowercaseFirstChar(paginatorMethod));
        } else {
            codeBlockBuilder.add("client.$N", lowercaseFirstChar(operationModel.getOperationName()));
        }

        codeBlockBuilder.add("(request -> request");

        List<Input> input = instruction.getInput();
        input.forEach(i -> {
            String fluentSetterMethodName = fluentSetterMethod(operationModel, i);
            switch (i.getType()) {
                case STRING:
                    codeBlockBuilder.add(".$N($S)",
                                         fluentSetterMethodName, i.getValue());
                    break;
                case REFERENCE:
                    codeBlockBuilder.add(".$N($N())",
                                         fluentSetterMethodName, i.getValue());
                    break;
                default:
                    throw new UnsupportedOperationException("unsupported type " + i.getType());
            }
        });

        handleStreamingInputIfNeeded(codeBlockBuilder, instruction);

        handleStreamingOutputIfNeeded(codeBlockBuilder, instruction);

        if (instruction.isUsePaginator()) {
            //TODO: hard code
            codeBlockBuilder.add(").contents().forEach(System.out::println)");
        } else {
            codeBlockBuilder.add(")");
        }

        methodBuilder.addStatement(codeBlockBuilder.build());

        if (outputConsumer != null) {
            List<Instruction> nestedInstructions = outputConsumer.getInstructions();
            nestedInstructions.forEach(i -> addCodeForEachInstruction(methodBuilder, CodeBlock.builder(), i));
        }
    }

    private String fluentSetterMethod(OperationModel operationModel, Input input) {
        MemberModel memberModel = operationModel.getInputShape().findMemberModelByC2jName(input.getName());
        return memberModel.getFluentSetterMethodName();
    }

    private void handleStreamingOutputIfNeeded(CodeBlock.Builder codeBlockBuilder, Instruction instruction) {
        Streaming streamingOutput = instruction.getStreamingOutput();
        if (streamingOutput != null) {
            switch (streamingOutput.getType()) {
                case FILE:
                    codeBlockBuilder.add(", $T.toFile($T.get($S))", ResponseTransformer.class, Paths.class,
                                         streamingOutput.getValue());
                    break;
                default:
                    throw new UnsupportedOperationException("unsupported type " + streamingOutput.getType());
            }
        }
    }

    private void handleStreamingInputIfNeeded(CodeBlock.Builder codeBlockBuilder, Instruction instruction) {
        Streaming streamingInput = instruction.getStreamingInput();
        if (streamingInput != null) {
            switch (streamingInput.getType()) {
                case FILE:
                    codeBlockBuilder.add(", $T.fromFile($T.get($S))", RequestBody.class, Paths.class, streamingInput.getValue());
                    break;
                default:
                    throw new UnsupportedOperationException("unsupported type " + streamingInput.getType());
            }
        }
    }

    private ClassName clientClassName() {
        return poetExtensions.getClientClass(model.getMetadata().getSyncInterface());
    }


    @Override
    public ClassName className() {
        return sampleClassName;
    }

}
