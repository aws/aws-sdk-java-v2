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
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
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

    public BddEndpointProviderSpec(IntermediateModel intermediateModel) {
        this.intermediateModel = intermediateModel;
        this.endpointBddModel = intermediateModel.getEndpointBddModel();
        this.endpointRulesSpecUtils = new EndpointRulesSpecUtils(intermediateModel);
        String packageName = intermediateModel.getMetadata().getFullInternalEndpointRulesPackageName();

        this.typeMirror = new RuleRuntimeTypeMirror(packageName);
        this.knownEndpointAttributes = knownEndpointAttributes(intermediateModel);
        this.registerInfoMap = buildRegisterInfoMap();
        this.endpointCaching = intermediateModel.getCustomizationConfig().getEnableEndpointProviderUriCaching();
    }

    @Override
    public TypeSpec poetSpec() {
        return PoetUtils.createClassBuilder(className())
                                            .addAnnotation(SdkInternalApi.class)
                                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                            .addSuperinterface(endpointRulesSpecUtils.providerInterfaceName())
                                            .addField(bddDefinition())
                                            .addMethod(resolveEndpointMethod())
            .build();

    }

    // generate the BDD_DEFINITION array which defines the nodes in a compact form:
    // an array of 3*numNodes.  3 integers per node, (conditionRef, highRef, lowRef)
    private FieldSpec bddDefinition() {
        String arrayLiteral = endpointBddModel.getCompactDecodedNodes().stream()
                                              .map(String::valueOf)
                                              .collect(Collectors.joining(", "));

        return FieldSpec.builder(int[].class, "BDD_DEFINITION",
                                 Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("{ $L }", arrayLiteral)
                        .build();
    }

    private CodeBlock conditionSwitchBlock() {
        CodeBlock.Builder builder = CodeBlock.builder();
        builder.beginControlFlow("switch (BDD_DEFINITION[nodeI*3])");

        for (int cI  = 0; cI < endpointBddModel.getConditions().size(); cI++) {
            builder
                .add("case $L:\n", cI)
                .indent();

            ConditionModel c = endpointBddModel.getConditions().get(cI);
            RuleModel synthetic = new RuleModel();
            synthetic.setType("error");
            synthetic.setError("synthetic");
            synthetic.setConditions(Collections.singletonList(c));
            RuleExpression parsedSynthetic = ExpressionParser
                .parseRuleSetExpression(synthetic)
                .accept(new PrepareForCodegenVisitor());

            parsedSynthetic.accept(new ConditionFnCodeGeneratorVisitor(builder, typeMirror, registerInfoMap));

            builder
                .addStatement("break")
                .unindent();
        }

        builder
            .addStatement("default: throw new $T(\"Unknown condition index\")", IllegalArgumentException.class)
            .endControlFlow(); // end switch

        return builder.build();
    }

    private CodeBlock resultSwitchBlock() {
        CodeBlock.Builder builder = CodeBlock.builder();
        builder.beginControlFlow("switch (nodeRef-100000001)");

        for (int rI  = 0; rI < endpointBddModel.getResults().size(); rI++) {
            builder
                .add("case $L:\n", rI)
                .indent();

            RuleModel resultRuleModel = endpointBddModel.getResults().get(rI);
            RuleExpression parsedSynthetic = ExpressionParser
                .parseRuleSetExpression(resultRuleModel)
                .accept(new PrepareForCodegenVisitor());

            parsedSynthetic.accept(new ResultFnCodeGeneratorVisitor(
                builder, typeMirror, registerInfoMap, knownEndpointAttributes, endpointCaching));

            builder
                .addStatement("break")
                .unindent();
        }

        builder
            .addStatement("default: throw new $T(\"Unknown result index\")", IllegalArgumentException.class)
            .endControlFlow(); // end switch


        return builder.build();
    }

    private MethodSpec resolveEndpointMethod() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("resolveEndpoint")
                                               .addModifiers(Modifier.PUBLIC)
                                               .returns(endpointRulesSpecUtils.resolverReturnType())
                                               .addAnnotation(Override.class)
                                               .addParameter(endpointRulesSpecUtils.parametersClassName(), "params");

        builder.beginControlFlow("try");
        builder.addCode(validateRequiredParams());

        // initialize all variables to null
        registerInfoMap.forEach((k,r) -> {
            builder.addStatement("$T $L = null", r.getRuleType().type().box(), r.getName());
        });

        // region builtin parameter needs to be mapped from Region to String
        String regionParamName = regionParamName();
        if (regionParamName != null) {
            String regionMethodName = endpointRulesSpecUtils.paramMethodName(regionParamName);
            builder.addStatement("$L = params.$L() == null ? null : params.$L().id()" ,
                                 registerInfoMap.get(regionParamName).getName(),
                                 regionMethodName, regionMethodName);
        }

        // add all other parameters
        for (Map.Entry<String, ParameterModel> entry : endpointBddModel.getParameters().entrySet()) {
            if (!entry.getKey().equals(regionParamName)) {
                builder.addStatement("$L = params.$L()",
                                     registerInfoMap.get(entry.getKey()).getName(),
                                     endpointRulesSpecUtils.paramMethodName(entry.getKey()));
            }
        }

        builder.addStatement("int nodeRef = $L", endpointBddModel.getRoot());
        builder
            .beginControlFlow("while (nodeRef != 1 && nodeRef != -1 && nodeRef < 100000000)")
            .addStatement("boolean complemented = nodeRef < 0")
            .addStatement("int nodeI = $L.abs(nodeRef) - 1", ClassName.get(Math.class))
            .addStatement("boolean conditionResult = false")
            .addCode(conditionSwitchBlock())
            .beginControlFlow("if (complemented == conditionResult)")
            .addStatement("nodeRef = BDD_DEFINITION[nodeI*3+2]") // follow highRef
            .nextControlFlow("else")
            .addStatement("nodeRef = BDD_DEFINITION[nodeI*3+1]") // follow lowRef
            .endControlFlow()
            .endControlFlow();

        builder.beginControlFlow("if (nodeRef == -1 || nodeRef == 1)")
               .addStatement("throw $T.create($S)", SdkClientException.class, "Rule engine did not reach an error or "
                                                                              + "endpoint result")
               .nextControlFlow("else")
               .addStatement("RuleResult result = null")
               .addCode(resultSwitchBlock())
               .beginControlFlow("if (result.isError())")
               .addStatement("String errorMsg = result.error()")
               .beginControlFlow("if (errorMsg.contains(\"Invalid ARN\") && errorMsg.contains(\":s3:::\"))")
               .addStatement("errorMsg += $S", ". Use the bucket name instead of simple bucket ARNs in "
                                               + "GetBucketLocationRequest.")
               .endControlFlow()
               .addStatement("throw $T.create(errorMsg)", SdkClientException.class)
               .endControlFlow()
               .addStatement("return $T.completedFuture(result.endpoint())", CompletableFuture.class)

               .endControlFlow();

        builder
            .nextControlFlow("catch ($T error)", Exception.class)
            .addStatement("return $T.failedFuture(error)", CompletableFutureUtils.class)
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
                  .filter(e -> Boolean.TRUE.equals(e.getValue().isRequired()))
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
            registryInfo.put(entry.getKey(), new RegistryInfo(name, index, fromParameterModel(entry.getValue())));
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
