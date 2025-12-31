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

package software.amazon.awssdk.codegen.poet.rules2.bdd;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.endpoints.authscheme.EndpointAuthScheme;
import software.amazon.awssdk.codegen.model.config.customization.EndpointAuthSchemeConfig;
import software.amazon.awssdk.codegen.model.config.customization.KeyTypePair;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.rules.endpoints.BuiltInParameter;
import software.amazon.awssdk.codegen.model.rules.endpoints.ConditionModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.ParameterModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.RuleModel;
import software.amazon.awssdk.codegen.model.service.EndpointBddModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.rules.EndpointRulesSpecUtils;
import software.amazon.awssdk.codegen.poet.rules2.ExpressionParser;
import software.amazon.awssdk.codegen.poet.rules2.PrepareForCodegenVisitor;
import software.amazon.awssdk.codegen.poet.rules2.RuleExpression;
import software.amazon.awssdk.codegen.poet.rules2.RuleRuntimeTypeMirror;
import software.amazon.awssdk.codegen.poet.rules2.RuleType;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Validate;

public class BddEndpointProviderSpec implements ClassSpec  {
    private final IntermediateModel intermediateModel;
    private final EndpointBddModel endpointBddModel;
    private final EndpointRulesSpecUtils endpointRulesSpecUtils;
    private final Map<String, KeyTypePair> knownEndpointAttributes;
    private final RuleRuntimeTypeMirror typeMirror;
    private final Map<String, RegistryInfo> registerInfoMap;
    private final boolean endpointCaching;

    private final ClassName evaluatorType;
    private final ClassName conditionFnType;
    private final ClassName resultFnType;
    private final ClassName dynamicAuthBuilderType;

    public BddEndpointProviderSpec(IntermediateModel intermediateModel) {
        this.intermediateModel = intermediateModel;
        this.endpointBddModel = intermediateModel.getEndpointBddModel();
        this.endpointRulesSpecUtils = new EndpointRulesSpecUtils(intermediateModel);
        String packageName = intermediateModel.getMetadata().getFullInternalEndpointRulesPackageName();

        this.typeMirror = new RuleRuntimeTypeMirror(packageName);
        this.knownEndpointAttributes = knownEndpointAttributes(intermediateModel);
        this.registerInfoMap = buildRegisterInfoMap();
        this.endpointCaching = intermediateModel.getCustomizationConfig().getEnableEndpointProviderUriCaching();

        this.evaluatorType = className().nestedClass("Evaluator");
        this.conditionFnType = className().nestedClass("ConditionFn");
        this.resultFnType = className().nestedClass("ResultFn");
        this.dynamicAuthBuilderType = className().nestedClass("DynamicAuthBuilder");
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder builder = PoetUtils.createClassBuilder(className())
                                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                            .addSuperinterface(endpointRulesSpecUtils.providerInterfaceName())
                                            .addType(evaluatorClass())
                                            .addType(dynamicAuthBuilderClass())
                                            .addField(bddDefinition())
                                            .addStaticBlock(staticInitLoadBddDefinition())
                                            .addAnnotation(SdkInternalApi.class);

        builder.addMethod(resolveEndpointMethod());

        return builder.build();
    }

    private TypeSpec conditionFnInterface() {
        return TypeSpec.interfaceBuilder(conditionFnType)
                       .addAnnotation(FunctionalInterface.class)
                       .addMethod(MethodSpec.methodBuilder("test")
                                            .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                            .returns(boolean.class)
                                            .addParameter(evaluatorType, "registers")
                                            .build()).build();
    }

    private TypeSpec resultFnInterface() {
        return TypeSpec.interfaceBuilder(resultFnType)
                       .addAnnotation(FunctionalInterface.class)
                       .addMethod(MethodSpec.methodBuilder("apply")
                                            .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                            .returns(typeMirror.rulesResult().type())
                                            .addParameter(evaluatorType, "registers")
                                            .build()).build();
    }

    private TypeSpec evaluatorClass() {
        TypeSpec.Builder builder = TypeSpec.classBuilder(evaluatorType)
                                           .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL);
        registerInfoMap.forEach((k,r) -> {
            TypeName type = r.getRuleType().type();
            if (type.isPrimitive() && r.isNullable()) {
                type = type.box();
            }

            builder.addField(
                FieldSpec
                    .builder(type, r.getName())
                    .build()
            );
        });

        builder.addMethod(evaluatorConditionMethod());
        builder.addMethod(evaluatorResultMethod());
        builder.addMethods(resultFns());
        return builder.build();
    }

    private MethodSpec evaluatorConditionMethod() {
        MethodSpec.Builder methodSpec = MethodSpec.methodBuilder("cond")
                                                  .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                                  .returns(boolean.class)
                                                  .addParameter(int.class, "i");

        CodeBlock.Builder builder = CodeBlock.builder()
            .beginControlFlow("switch (i)");

        for (int cI  = 0; cI < endpointBddModel.getConditions().size(); cI++) {

            builder.beginControlFlow("case $L:", cI);

            ConditionModel c = endpointBddModel.getConditions().get(cI);

            // hack for now to work around ExpressionParser
            RuleModel synthetic = new RuleModel();
            synthetic.setType("error");
            synthetic.setError("synthetic");
            synthetic.setConditions(Collections.singletonList(c));
            RuleExpression parsedSynthetic = ExpressionParser
                .parseRuleSetExpression(synthetic)
                .accept(new PrepareForCodegenVisitor());

            parsedSynthetic.accept(new ConditionFnCodeGeneratorVisitor(builder, typeMirror, registerInfoMap));

            builder.endControlFlow(); //end case, no "break" required as all cases will return
        }


        builder
            .beginControlFlow("default:")
                .addStatement("throw new IllegalArgumentException($S)", "Unknown condition index")
                    .endControlFlow();
        builder.endControlFlow(); //end switch

        methodSpec.addCode(builder.build());

        return methodSpec.build();
    }

    private MethodSpec evaluatorResultMethod() {
        MethodSpec.Builder methodSpec = MethodSpec.methodBuilder("result")
                                                  .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                                  .returns(typeMirror.rulesResult().type())
                                                  .addParameter(int.class, "i");

        CodeBlock.Builder builder = CodeBlock.builder()
                                             .beginControlFlow("switch (i)");

        for (int rI  = 0; rI < endpointBddModel.getResults().size(); rI++) {

            builder.beginControlFlow("case $L:", rI)
                .addStatement("return result$L()", rI)
                .endControlFlow(); //end case, no "break" required as all cases will return
        }

        builder
            .beginControlFlow("default:")
            .addStatement("throw new IllegalArgumentException($S)", "Unknown condition index")
            .endControlFlow();
        builder.endControlFlow(); //end switch

        methodSpec.addCode(builder.build());
        return methodSpec.build();
    }


    // TODO: We can optimize this out in many cases
    private TypeSpec dynamicAuthBuilderClass() {
        TypeSpec.Builder builder =
            TypeSpec.classBuilder(dynamicAuthBuilderType)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addField(FieldSpec.builder(String.class, "name").build())
                    .addField(FieldSpec.builder(
                                           ParameterizedTypeName.get(
                                               Map.class,
                                               String.class,
                                               String.class),
                                           "properties",
                                           Modifier.PRIVATE)
                                       .initializer("new $T<>()",HashMap.class)
                                       .build())
                .addMethod(MethodSpec.methodBuilder("builder")
                               .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                               .returns(dynamicAuthBuilderType)
                               .addStatement("return new $T()", dynamicAuthBuilderType)
                               .build())
                    .addMethod(MethodSpec.methodBuilder("name")
                                         .addParameter(String.class, "name")
                                         .returns(dynamicAuthBuilderType)
                                         .addCode(CodeBlock.builder().addStatement("this.name = name").addStatement("return "
                                                                                                                    + "this").build())
                                         .build())
                    .addMethod(MethodSpec.methodBuilder("property")
                                         .addParameter(String.class, "key")
                                         .addParameter(String.class, "value")
                                         .returns(dynamicAuthBuilderType)
                                         .addCode(CodeBlock.builder()
                                                           .addStatement("properties.put(key, value)")
                                                           .addStatement("return this")
                                                           .build())
                                         .build());

        builder.addMethod(MethodSpec.methodBuilder("build")
                              .addModifiers(Modifier.PUBLIC)
                              .returns(EndpointAuthScheme.class)
                                    // TODO
                              .addCode(CodeBlock.builder().addStatement("return null").build())
                              .build());

        return builder.build();
    }

    // generate the BDD_DEFINITION array which defines the nodes in a compact form:
    // an array of 3*numNodes.  3 integers per node, (conditionRef, highRef, lowRef)
    private FieldSpec bddDefinition() {

        return FieldSpec.builder(int[].class, "BDD_DEFINITION",
                                 Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .build();
    }

    // static initialization code to load the BDD definition from the binary resource file
    private CodeBlock staticInitLoadBddDefinition() {
        String fileName = "/endpoints_bdd_" + Integer.toHexString(endpointBddModel.getNodes().hashCode()) + ".bin";
        return CodeBlock.builder()
                        .beginControlFlow("try ($T in = $T.class.getResourceAsStream($S))",
                                          ClassName.get("java.io", "InputStream"),
                                          className(),
                                          fileName)
                        .beginControlFlow("if (in == null)")
                        .addStatement("throw new $T($S)",
                                      IllegalStateException.class,
                                      "Resource " + fileName + " not found")
                        .endControlFlow()
                        .addStatement("BDD_DEFINITION = new int[$L]", endpointBddModel.getNodeCount()*3)
                        .addStatement("$T data = new $T(in)", DataInputStream.class, DataInputStream.class)
                        .beginControlFlow("for(int i=0; i < $L; i++)", endpointBddModel.getNodeCount()*3)
                        .addStatement("BDD_DEFINITION[i] = data.readInt()")
                        .endControlFlow()
                        .nextControlFlow("catch ($T e)", IOException.class)
                        .addStatement("throw new $T(e)",
                                      ExceptionInInitializerError.class)
                        .endControlFlow()
                        .build();
    }

    // generate the CONDITION_FNS array with functions for every condition
    private FieldSpec conditionFnsArray() {
        CodeBlock.Builder arrayInit = CodeBlock.builder()
                                               .add("{\n")
                                               .indent();
        for (int cI  = 0; cI < endpointBddModel.getConditions().size(); cI++) {
            arrayInit
                .add("$T::cond$L", className(), cI);
            if (cI < endpointBddModel.getConditions().size() - 1) {
                arrayInit.add(", ");
            }
            arrayInit.add("\n");
        }
        arrayInit.unindent().add("\n}");

        TypeName conditionFnArrayType = ArrayTypeName.of(conditionFnType);
        return FieldSpec.builder(conditionFnArrayType, "CONDITION_FNS",
                                 Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer(arrayInit.build())
                        .build();
    }

    private List<MethodSpec> conditionFns() {
        List<MethodSpec> methods = new ArrayList<>();
        for (int cI  = 0; cI < endpointBddModel.getConditions().size(); cI++) {
            ConditionModel c = endpointBddModel.getConditions().get(cI);
            CodeBlock.Builder builder = CodeBlock.builder();

            // hack for now to work around ExpressionParser
            RuleModel synthetic = new RuleModel();
            synthetic.setType("error");
            synthetic.setError("synthetic");
            synthetic.setConditions(Collections.singletonList(c));
            RuleExpression parsedSynthetic = ExpressionParser
                .parseRuleSetExpression(synthetic)
                .accept(new PrepareForCodegenVisitor());

            parsedSynthetic.accept(new ConditionFnCodeGeneratorVisitor(builder, typeMirror, registerInfoMap));

            methods.add(MethodSpec
                            .methodBuilder("cond" + cI)
                            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                            .returns(boolean.class)
                            .addCode(builder.build())
                            .build());
        }
        return methods;
    }

    // generate the RESULT_FNS array with functions for every result
    private FieldSpec resultFnsArray() {
        CodeBlock.Builder arrayInit = CodeBlock.builder()
                                               .add("{\n")
                                               .indent();
        for (int rI  = 0; rI < endpointBddModel.getResults().size(); rI++) {
            arrayInit
                .add("$T::result$L", className(), rI);
            if (rI < endpointBddModel.getResults().size() - 1) {
                arrayInit.add(", ");
            }
            arrayInit.add("\n");
        }
        arrayInit.unindent().add("\n}");

        TypeName resultFnArrayType = ArrayTypeName.of(resultFnType);
        return FieldSpec.builder(resultFnArrayType, "RESULT_FNS",
                                 Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer(arrayInit.build())
                        .build();
    }

    private List<MethodSpec> resultFns() {
        List<MethodSpec> methods = new ArrayList<>();
        for (int rI  = 0; rI < endpointBddModel.getResults().size(); rI++) {
            CodeBlock.Builder builder = CodeBlock.builder();
            RuleExpression parsedSynthetic = ExpressionParser
                .parseRuleSetExpression(endpointBddModel.getResults().get(rI))
                .accept(new PrepareForCodegenVisitor());

            parsedSynthetic.accept(new ResultFnCodeGeneratorVisitor(
                builder, dynamicAuthBuilderType, typeMirror, registerInfoMap, knownEndpointAttributes, endpointCaching));

            methods.add(MethodSpec
                            .methodBuilder("result" + rI)
                            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                            .returns(typeMirror.rulesResult().type())
                            .addCode(builder.build())
                            .build());
        }
        return methods;
    }

    private MethodSpec resolveEndpointMethod() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("resolveEndpoint")
                                               .addModifiers(Modifier.PUBLIC)
                                               .returns(endpointRulesSpecUtils.resolverReturnType())
                                               .addAnnotation(Override.class)
                                               .addParameter(endpointRulesSpecUtils.parametersClassName(), "params");

        builder.addCode(validateRequiredParams());

        builder.addStatement("$T evaluator = new $T()", evaluatorType, evaluatorType);

        // region builtin parameter needs to be mapped from Region to String
        String regionParamName = regionParamName();
        if (regionParamName != null) {
            String regionMethodName = endpointRulesSpecUtils.paramMethodName(regionParamName);
            builder.addStatement("evaluator.$L = params.$L() == null ? null : params.$L().id()" ,
                                 registerInfoMap.get(regionParamName).getName(),
                                 regionMethodName, regionMethodName);
        }

        // add all other parameters
        for (Map.Entry<String, ParameterModel> entry : endpointBddModel.getParameters().entrySet()) {
            if (!entry.getKey().equals(regionParamName)) {
                builder.addStatement("evaluator.$L = params.$L()",
                                     registerInfoMap.get(entry.getKey()).getName(),
                                     endpointRulesSpecUtils.paramMethodName(entry.getKey()));
            }
        }

        builder.addStatement("final $T bdd = BDD_DEFINITION", int[].class);
        builder.addStatement("int nodeRef = $L", endpointBddModel.getRoot());
        builder
            .beginControlFlow("while ((nodeRef > 1 || nodeRef < -1) && nodeRef < 100000000)")
            .addStatement("int base  = (Math.abs(nodeRef) - 1) * 3")
            .addStatement("int complemented = nodeRef >> 31 & 1; // 1 if complemented edge, else 0")
            .addStatement("int conditionResult = evaluator.cond(bdd[base]) ? 1 : 0")
            .addStatement("nodeRef = bdd[base + 2 - (complemented ^ conditionResult)]")
            .endControlFlow();

        builder.beginControlFlow("if (nodeRef == -1 || nodeRef == 1)")
               .addStatement("return $T.failedFuture($T.create($S))", CompletableFutureUtils.class,
                             SdkClientException.class, "Rule engine did not reach an error or endpoint result")
               .nextControlFlow("else")
               .addStatement("RuleResult result = evaluator.result(nodeRef-100000001)")
               .beginControlFlow("if (result.isError())")
               .addStatement("String errorMsg = result.error()")
               .beginControlFlow("if (errorMsg.contains(\"Invalid ARN\") && errorMsg.contains(\":s3:::\"))")
               .addStatement("errorMsg += $S", ". Use the bucket name instead of simple bucket ARNs in "
                                               + "GetBucketLocationRequest.")
               .endControlFlow()
               .addStatement("return $T.failedFuture($T.create(errorMsg))", CompletableFutureUtils.class, SdkClientException.class)
               .endControlFlow()
               .addStatement("return $T.completedFuture(result.endpoint())", CompletableFuture.class)
               .endControlFlow();
        return builder.build();
    }

    @Override
    public ClassName className() {
        Metadata md = intermediateModel.getMetadata();
        return ClassName.get(md.getFullInternalEndpointRulesPackageName(),
                             "Default" + endpointRulesSpecUtils.providerInterfaceName().simpleName());
    }

    // return the name of the region param (mapped to region builtin).  Returns null if none set.
    private String regionParamName() {
        for (Map.Entry<String, ParameterModel> entry : endpointBddModel.getParameters().entrySet()) {
            if (entry.getValue().getBuiltInEnum() == BuiltInParameter.AWS_REGION) {
                return entry.getKey();
            }
        }
        return null;
    }

    private CodeBlock validateRequiredParams() {
        CodeBlock.Builder b = CodeBlock.builder();
        Map<String, ParameterModel> parameters = endpointBddModel.getParameters();
        parameters.entrySet().stream()
                  .filter(e -> Boolean.TRUE.equals(e.getValue().isRequired()) && e.getValue().getDefault() == null)
                  .forEach(e -> {
                      b.addStatement("$T.notNull($N.$N(), $S)",
                                     Validate.class,
                                     "params",
                                     endpointRulesSpecUtils.paramMethodName(e.getKey()),
                                     String.format("Parameter '%s' must not be null", e.getKey()));
                  });

        return b.build();
    }

    private Map<String, RegistryInfo> buildRegisterInfoMap() {
        int index = 0;
        Map<String, RegistryInfo> registryInfo = new LinkedHashMap<>();

        // first add an entry for every parameter
        for (Map.Entry<String, ParameterModel> entry : endpointBddModel.getParameters().entrySet()) {
            String name = intermediateModel.getNamingStrategy().getVariableName(entry.getKey());
            boolean nullable = entry.getValue().getDefault() == null;
            registryInfo.put(entry.getKey(),
                             new RegistryInfo(name, index, fromParameterModel(entry.getValue()), null, nullable));
            index += 1;
        }

        // add an entry for every assigned variable.  assigns are guaranteed to be globally unique
        for (ConditionModel conditionModel : endpointBddModel.getConditions()) {
            if (conditionModel.getAssign() != null) {
                // at this point we don't know the type.
                // Create a RulesetExpression that will be used to infer the type using the visitor
                RuleModel synthetic = new RuleModel();
                synthetic.setType("error");
                synthetic.setError("synthetic");
                synthetic.setConditions(Collections.singletonList(conditionModel));
                String name = intermediateModel.getNamingStrategy().getVariableName(conditionModel.getAssign());
                registryInfo.put(
                    conditionModel.getAssign(),
                    new RegistryInfo(name, index, ExpressionParser.parseRuleSetExpression(synthetic)));
                index += 1;
            }
        }

        // visit all the conditions/assignments and infer types
        AssignTypeInferringVisitor typeVisitor = new AssignTypeInferringVisitor(typeMirror, registryInfo);
        registryInfo.values().forEach(r -> {
            if (r.getRuleSetExpression() != null) {
                r.getRuleSetExpression().accept(typeVisitor);
            }
        });

        // assert that we have type information for all registry values
        registryInfo.values().forEach(r -> {
            if (r.getRuleType() == null) {
                throw new IllegalStateException("Unable to infer type for `" + r.getName() + "`");
            }
        });

        return Collections.unmodifiableMap(registryInfo);
    }

    private static RuleType fromParameterModel(ParameterModel model) {
        switch (model.getType().toLowerCase(Locale.ENGLISH)) {
            case "boolean":
                return RuleRuntimeTypeMirror.BOOLEAN;
            case "string":
                return RuleRuntimeTypeMirror.STRING;
            case "stringarray":
                return RuleRuntimeTypeMirror.LIST_OF_STRING;
            default:
                throw new IllegalStateException("Cannot find rule type for: " + model.getType());
        }
    }

    private static Map<String, KeyTypePair> knownEndpointAttributes(IntermediateModel intermediateModel) {
        Map<String, KeyTypePair> knownEndpointAttributes = null;
        EndpointAuthSchemeConfig config = intermediateModel.getCustomizationConfig().getEndpointAuthSchemeConfig();
        if (config != null) {
            knownEndpointAttributes = config.getEndpointProviderTestKeys();
        }
        if (knownEndpointAttributes == null) {
            knownEndpointAttributes = Collections.emptyMap();
        }
        return knownEndpointAttributes;
    }

}
