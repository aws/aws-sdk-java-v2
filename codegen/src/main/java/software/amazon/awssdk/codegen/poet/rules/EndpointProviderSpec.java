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

package software.amazon.awssdk.codegen.poet.rules;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import com.squareup.javapoet.WildcardTypeName;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.codegen.model.config.customization.EndpointAuthSchemeConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.rules.endpoints.BuiltInParameter;
import software.amazon.awssdk.codegen.model.rules.endpoints.ParameterModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

public class EndpointProviderSpec implements ClassSpec {
    private static final String RULE_SET_FIELD_NAME = "ENDPOINT_RULE_SET";
    private static final String LOGGER_FIELD_NAME = "LOG";

    private final IntermediateModel intermediateModel;
    private final EndpointRulesSpecUtils endpointRulesSpecUtils;

    public EndpointProviderSpec(IntermediateModel intermediateModel) {
        this.intermediateModel = intermediateModel;
        this.endpointRulesSpecUtils = new EndpointRulesSpecUtils(intermediateModel);
    }

    @Override
    public TypeSpec poetSpec() {
        FieldSpec endpointAuthSchemeStrategyFieldSpec = endpointAuthSchemeStrategyFieldSpec();
        TypeSpec.Builder b = PoetUtils.createClassBuilder(className())
                                      .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                      .addSuperinterface(endpointRulesSpecUtils.providerInterfaceName())
                                      .addField(logger())
                                      .addField(ruleSet())
                                      .addField(endpointAuthSchemeStrategyFieldSpec)
                                      .addMethod(resolveEndpointMethod())
                                      .addMethod(toIdentifierValueMap())
                                      .addAnnotation(SdkInternalApi.class);

        MethodSpec constructorMethod = constructorMethodSpec(endpointAuthSchemeStrategyFieldSpec.name);
        MethodSpec valueAsEndpointOrThrowMethod = valueAsEndpointOrThrowMethodSpec();

        b.addMethod(constructorMethod);
        b.addMethod(valueAsEndpointOrThrowMethod);
        b.addMethod(ruleSetBuildMethod(b));
        b.addMethod(equalsMethod());
        b.addMethod(hashCodeMethod());
        addKnownPropertiesMethodSpec(b, endpointAuthSchemeStrategyFieldSpec.name);

        return b.build();
    }

    private FieldSpec endpointAuthSchemeStrategyFieldSpec() {
        return FieldSpec.builder(endpointRulesSpecUtils.rulesRuntimeClassName("EndpointAuthSchemeStrategy"),
                                 "endpointAuthSchemeStrategy", Modifier.PRIVATE, Modifier.FINAL)
                        .build();
    }

    private MethodSpec constructorMethodSpec(String endpointAuthSchemeFieldName) {
        MethodSpec.Builder b = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
        EndpointAuthSchemeConfig endpointAuthSchemeConfig =
            intermediateModel.getCustomizationConfig().getEndpointAuthSchemeConfig();
        String factoryLocalVarName = "endpointAuthSchemeStrategyFactory";
        if (endpointAuthSchemeConfig != null && endpointAuthSchemeConfig.getAuthSchemeStrategyFactoryClass() != null) {
            String endpointAuthSchemeStrategyFactory = endpointAuthSchemeConfig.getAuthSchemeStrategyFactoryClass();
            b.addStatement("$T $N = new $T()",
                           endpointRulesSpecUtils.rulesRuntimeClassName("EndpointAuthSchemeStrategyFactory"),
                           factoryLocalVarName,
                           PoetUtils.classNameFromFqcn(endpointAuthSchemeStrategyFactory));
        } else {
            b.addStatement("$T $N = new $T()",
                           endpointRulesSpecUtils.rulesRuntimeClassName("EndpointAuthSchemeStrategyFactory"),
                           factoryLocalVarName,
                           endpointRulesSpecUtils.rulesRuntimeClassName("DefaultEndpointAuthSchemeStrategyFactory"));
        }
        b.addStatement("this.$N = $N.endpointAuthSchemeStrategy()", endpointAuthSchemeFieldName, factoryLocalVarName);
        return b.build();
    }

    private MethodSpec valueAsEndpointOrThrowMethodSpec() {
        String valueParamName = "value";
        ParameterSpec param = ParameterSpec.builder(endpointRulesSpecUtils.rulesRuntimeClassName("Value"), valueParamName)
                                           .build();
        MethodSpec.Builder b = MethodSpec.methodBuilder("valueAsEndpointOrThrow")
                                         .returns(ClassName.get(Endpoint.class))
                                         .addParameter(param);

        CodeBlock.Builder methodCode =
            CodeBlock.builder()
                     .beginControlFlow("if ($N instanceof $T)",
                                       valueParamName,
                                       endpointRulesSpecUtils.rulesRuntimeClassName("Value.Endpoint"))
                     .addStatement("$T endpoint = $N.expectEndpoint()",
                                   endpointRulesSpecUtils.rulesRuntimeClassName("Value.Endpoint"), valueParamName)
                     .addStatement("$T builder = Endpoint.builder()", Endpoint.Builder.class)
                     .addStatement("builder.url($T.create(endpoint.getUrl()))", URI.class)
                     .addStatement("$T headers = endpoint.getHeaders()",
                                   ParameterizedTypeName.get(ClassName.get(Map.class),
                                                             TypeName.get(String.class),
                                                             ParameterizedTypeName.get(List.class, String.class)))
                     .beginControlFlow("if (headers != null)")
                     .addStatement("headers.forEach((name, values) -> values.forEach(v -> builder.putHeader(name, v)))")
                     .endControlFlow()
                     .addStatement("addKnownProperties(builder, endpoint.getProperties())")
                     .addStatement("return builder.build()")

                     .nextControlFlow("else if ($N instanceof $T)",
                                      valueParamName,
                                      endpointRulesSpecUtils.rulesRuntimeClassName("Value.Str"))
                     .addStatement("$T errorMsg = $N.expectString()", String.class, valueParamName)
                     .beginControlFlow("if (errorMsg.contains($S) && errorMsg.contains($S))",
                                       "Invalid ARN", ":s3:::")
                     .addStatement("errorMsg += $S", ". Use the bucket name instead of simple bucket ARNs in "
                                                     + "GetBucketLocationRequest.")
                     .endControlFlow()
                     .addStatement("throw $T.create(errorMsg)", SdkClientException.class)
                     .nextControlFlow("else")
                     .addStatement("throw SdkClientException.create($S + $N)",
                                   "Rule engine return neither an endpoint result or error value. Returned value was: ",
                                   valueParamName)
                     .endControlFlow();

        b.addCode(methodCode.build());
        return b.build();
    }

    private void addKnownPropertiesMethodSpec(TypeSpec.Builder b, String endpointAuthSpecStrategyFieldName) {
        EndpointAuthSchemeConfig endpointAuthSchemeConfig =
            intermediateModel.getCustomizationConfig().getEndpointAuthSchemeConfig();
        if (endpointAuthSchemeConfig != null && endpointAuthSchemeConfig.getKnownEndpointProperties() != null) {
            addKnownEndpointPropertiesMethodOverride(b, endpointAuthSchemeConfig.getKnownEndpointProperties());
        } else {
            b.addMethod(defaultAddKnownEndpointPropertyMethod(endpointAuthSpecStrategyFieldName));
        }
    }

    private MethodSpec defaultAddKnownEndpointPropertyMethod(String endpointAuthSpecStrategyFieldName) {
        String builderParamName = "builder";
        String propertiesParamName = "properties";
        MethodSpec.Builder b = addKnowPropertiesSignature(builderParamName, propertiesParamName);

        CodeBlock.Builder switchStatementCode = CodeBlock.builder();
        switchStatementCode.beginControlFlow("switch (n)")
                           .add("case $S:\n", "authSchemes").indent()
                           .add(CodeBlock.builder()
                                         .addStatement("$N.putAttribute($T.AUTH_SCHEMES, $N.createAuthSchemes(v))",
                                                       builderParamName,
                                                       AwsEndpointAttribute.class,
                                                       endpointAuthSpecStrategyFieldName)
                                         .build())
                           .addStatement("break")
                           .add("default:\n").indent()
                           .add(CodeBlock.builder()
                                         .addStatement("$N.debug(() -> $S + n)",
                                                       LOGGER_FIELD_NAME,
                                                       "Ignoring unknown endpoint property: ")
                                         .build())
                           .addStatement("break")
                           .endControlFlow();
        CodeBlock.Builder methodCode = CodeBlock.builder();
        CodeBlock lambda = CodeBlock.builder()
                                    .add("(n, v) -> {\n").indent()
                                    .add(switchStatementCode.build())
                                    .unindent().add("}")
                                    .build();
        methodCode.add("$N.forEach($L);", propertiesParamName, lambda);
        b.addCode(methodCode.build());
        return b.build();
    }

    private MethodSpec equalsMethod() {
        return MethodSpec.methodBuilder("equals")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC)
                         .returns(boolean.class)
                         .addParameter(Object.class, "rhs")
                         .addStatement("return rhs != null && getClass().equals(rhs.getClass())")
                         .build();
    }

    private MethodSpec hashCodeMethod() {
        return MethodSpec.methodBuilder("hashCode")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC)
                         .returns(int.class)
                         .addStatement("return getClass().hashCode()")
                         .build();
    }

    private void addKnownEndpointPropertiesMethodOverride(TypeSpec.Builder b, String knowPropertyExpression) {
        MethodSpec singlePropertyMethod =
            MethodSpec.methodBuilder("addKnownProperty")
                      .addModifiers(Modifier.PRIVATE)
                      .addTypeVariable(TypeVariableName.get("T"))
                      .addParameter(ParameterSpec.builder(
                          ParameterizedTypeName.get(endpointRulesSpecUtils.rulesRuntimeClassName("EndpointAttributeProvider"),
                                                    TypeVariableName.get("T")),
                          "provider").build())
                      .addParameter(Endpoint.Builder.class, "builder")
                      .addParameter(endpointRulesSpecUtils.rulesRuntimeClassName("Value"), "value")
                      .addStatement("builder.putAttribute(provider.attributeKey(), provider.attributeValue(value))")
                      .build();
        b.addMethod(singlePropertyMethod);

        String builderParamName = "builder";
        String propertiesParamName = "properties";
        MethodSpec.Builder methodBuilder = addKnowPropertiesSignature(builderParamName, propertiesParamName);

        TypeName wildcardEndpointAttrType = ParameterizedTypeName.get(
            endpointRulesSpecUtils.rulesRuntimeClassName("EndpointAttributeProvider"),
            WildcardTypeName.subtypeOf(Object.class));
        methodBuilder.addStatement("$T knownProperties = $N",
                                   ParameterizedTypeName.get(ClassName.get(List.class), wildcardEndpointAttrType),
                                   knowPropertyExpression);
        methodBuilder.beginControlFlow("for ($T p: knownProperties)", wildcardEndpointAttrType);
        methodBuilder.beginControlFlow("if ($N.containsKey(p.propertyName()))", propertiesParamName);
        methodBuilder.addStatement("$N(p, $N, $N.get(p.propertyName()))",
                                   singlePropertyMethod.name,
                                   builderParamName,
                                   propertiesParamName);
        methodBuilder.endControlFlow();
        methodBuilder.endControlFlow();

        b.addMethod(methodBuilder.build());
    }

    private MethodSpec.Builder addKnowPropertiesSignature(String builderParamName, String propertiesParamName) {
        ParameterSpec builderParam = ParameterSpec.builder(ClassName.get(Endpoint.Builder.class), builderParamName).build();
        ParameterSpec propertiesParam = ParameterSpec
            .builder(ParameterizedTypeName.get(
                         ClassName.get(Map.class),
                         ClassName.get(String.class),
                         endpointRulesSpecUtils.rulesRuntimeClassName("Value")),
                     propertiesParamName)
            .build();
        return MethodSpec.methodBuilder("addKnownProperties")
                         .addModifiers(Modifier.PRIVATE)
                         .addParameter(builderParam)
                         .addParameter(propertiesParam);
    }

    @Override
    public ClassName className() {
        Metadata md = intermediateModel.getMetadata();
        return ClassName.get(md.getFullInternalEndpointRulesPackageName(),
                             "Default" + endpointRulesSpecUtils.providerInterfaceName().simpleName());
    }

    private FieldSpec logger() {
        return FieldSpec.builder(Logger.class, LOGGER_FIELD_NAME)
                        .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$T.loggerFor($T.class)", Logger.class, className())
                        .build();
    }

    private FieldSpec ruleSet() {
        return FieldSpec.builder(endpointRulesSpecUtils.rulesRuntimeClassName("EndpointRuleset"), RULE_SET_FIELD_NAME)
                        .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("ruleSet()")
                        .build();
    }

    private MethodSpec toIdentifierValueMap() {
        ParameterizedTypeName resultType = ParameterizedTypeName.get(ClassName.get(Map.class),
                                                                     endpointRulesSpecUtils.rulesRuntimeClassName("Identifier"),
                                                                     endpointRulesSpecUtils.rulesRuntimeClassName("Value"));

        String paramsName = "params";
        MethodSpec.Builder b = MethodSpec.methodBuilder("toIdentifierValueMap")
                                         .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                                         .addParameter(endpointRulesSpecUtils.parametersClassName(), paramsName)
                                         .returns(resultType);

        Map<String, ParameterModel> params = intermediateModel.getEndpointRuleSetModel().getParameters();

        String resultName = "paramsMap";
        b.addStatement("$T $N = new $T<>()", resultType, resultName, HashMap.class);

        params.forEach((name, model) -> {
            String methodVarName = endpointRulesSpecUtils.paramMethodName(name);

            CodeBlock identifierExpr =
                CodeBlock.of("$T.of($S)", endpointRulesSpecUtils.rulesRuntimeClassName("Identifier"), name);

            CodeBlock coerce;
            // We treat region specially and generate it as the Region type,
            // so we need to call id() to convert it back to string
            if (model.getBuiltInEnum() == BuiltInParameter.AWS_REGION) {
                coerce = CodeBlock.builder().add(".id()").build();
            } else {
                coerce = CodeBlock.builder().build();
            }
            CodeBlock valueExpr = endpointRulesSpecUtils.valueCreationCode(
                model.getType(),
                CodeBlock.builder()
                         .add("$N.$N()$L", paramsName, methodVarName, coerce)
                         .build());

            b.beginControlFlow("if ($N.$N() != null)", paramsName, methodVarName);
            b.addStatement("$N.put($L, $L)", resultName, identifierExpr, valueExpr);
            b.endControlFlow();
        });

        b.addStatement("return $N", resultName);

        return b.build();
    }

    private MethodSpec resolveEndpointMethod() {
        String paramsName = "endpointParams";

        MethodSpec.Builder b = MethodSpec.methodBuilder("resolveEndpoint")
                                         .addModifiers(Modifier.PUBLIC)
                                         .returns(endpointRulesSpecUtils.resolverReturnType())
                                         .addAnnotation(Override.class)
                                         .addParameter(endpointRulesSpecUtils.parametersClassName(), paramsName);

        b.addCode(validateRequiredParams());

        b.addStatement("$T res = new $T().evaluate($N, toIdentifierValueMap($N))",
                       endpointRulesSpecUtils.rulesRuntimeClassName("Value"),
                       endpointRulesSpecUtils.rulesRuntimeClassName("DefaultRuleEngine"),
                       RULE_SET_FIELD_NAME,
                       paramsName);

        b.beginControlFlow("try");
        b.addStatement("return $T.completedFuture(valueAsEndpointOrThrow($N))",
                       CompletableFuture.class,
                       "res");
        b.endControlFlow();
        b.beginControlFlow("catch ($T error)", Exception.class);
        b.addStatement("return $T.failedFuture(error)", CompletableFutureUtils.class);
        b.endControlFlow();

        return b.build();
    }

    private MethodSpec ruleSetBuildMethod(TypeSpec.Builder classBuilder) {
        RuleSetCreationSpec ruleSetCreationSpec = new RuleSetCreationSpec(intermediateModel);
        MethodSpec.Builder b = MethodSpec.methodBuilder("ruleSet")
                                         .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                                         .returns(endpointRulesSpecUtils.rulesRuntimeClassName("EndpointRuleset"))
                                         .addStatement("return $L", ruleSetCreationSpec.ruleSetCreationExpr());

        ruleSetCreationSpec.helperMethods().forEach(classBuilder::addMethod);
        return b.build();
    }

    private CodeBlock validateRequiredParams() {
        CodeBlock.Builder b = CodeBlock.builder();

        Map<String, ParameterModel> parameters = intermediateModel.getEndpointRuleSetModel().getParameters();
        parameters.entrySet().stream()
                  .filter(e -> Boolean.TRUE.equals(e.getValue().isRequired()))
                  .forEach(e -> {
                      b.addStatement("$T.notNull($N.$N(), $S)",
                                     Validate.class,
                                     "endpointParams",
                                     endpointRulesSpecUtils.paramMethodName(e.getKey()),
                                     String.format("Parameter '%s' must not be null", e.getKey()));
                  });

        return b.build();
    }
}
