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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtension;
import software.amazon.awssdk.codegen.poet.PoetUtils;

/**
 * Base class containing common logic shared between the sync waiter class and the async waiter class
 */
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

        switch (instruction.getType()) {
            case SERVICE_OPERATION:
                addCodeForServiceOperation(builder, codeBlockBuilder, instruction);
                break;
            case SDK_OPERATION:
                addSdkOperation(builder, codeBlockBuilder, instruction);
                break;
        }
    }

    private void addSdkOperation(MethodSpec.Builder builder, CodeBlock.Builder codeBlockBuilder, Instruction instruction) {

        String name = instruction.getName();
        if (!name.equals("print")) {
            return;
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

    private void addCodeForServiceOperation(MethodSpec.Builder builder, CodeBlock.Builder codeBlockBuilder,
                                            Instruction instruction) {
        String operationName = instruction.getName();
        OperationModel operationModel = model.getOperation(operationName);

        OutputConsumer outputConsumer = instruction.getOutputConsumer();
        if (outputConsumer != null) {
            String outputVariable = outputConsumer.getOutputVariable();
            ClassName responseType = ClassName.get(modelPackage, operationModel.getReturnType().getReturnType());

            codeBlockBuilder.add("$T $N = ", responseType, outputVariable);
        }

        codeBlockBuilder.add("client.$N", lowercaseFirstChar(operationModel.getOperationName()));

        codeBlockBuilder.add("(request -> request");

        List<Input> input = instruction.getInput();
        input.forEach(i -> {
            switch (i.getType()) {
                case STRING:
                    codeBlockBuilder.add(".$N($S)",
                                         i.getName(), i.getValue());
                    break;
                case REFERENCE:
                    codeBlockBuilder.add(".$N($N())",
                                         i.getName(), i.getValue());
                    break;
                default:
                    throw new UnsupportedOperationException("unsupported type " + i.getType());
            }
        });

        codeBlockBuilder.add(")");

        builder.addStatement(codeBlockBuilder.build());

        if (outputConsumer != null) {
            List<Instruction> nestedInstructions = outputConsumer.getInstructions();
            nestedInstructions.forEach(i -> addCodeForEachInstruction(builder, CodeBlock.builder(), i));
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
