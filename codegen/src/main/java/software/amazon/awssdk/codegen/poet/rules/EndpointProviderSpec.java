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
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.service.ParameterModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.rules.DefaultRuleEngine;
import software.amazon.awssdk.core.rules.EndpointRuleset;
import software.amazon.awssdk.core.rules.Identifier;
import software.amazon.awssdk.core.rules.RuleEngine;
import software.amazon.awssdk.core.rules.Value;
import software.amazon.awssdk.core.rules.model.Endpoint;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.utils.Lazy;

public class EndpointProviderSpec implements ClassSpec {
    private static final String ENGINE_FIELD_NAME = "RULES_ENGINE";
    private static final String RULE_SET_FIELD_NAME = "ENDPOINT_RULE_SET";

    private final IntermediateModel intermediateModel;
    private final EndpointRulesSpecUtils endpointRulesSpecUtils;


    public EndpointProviderSpec(IntermediateModel intermediateModel) {
        this.intermediateModel = intermediateModel;
        this.endpointRulesSpecUtils = new EndpointRulesSpecUtils(intermediateModel);
    }

    @Override
    public TypeSpec poetSpec() {
        return PoetUtils.createClassBuilder(className())
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addSuperinterface(endpointRulesSpecUtils.providerInterfaceName())
                        .addField(lazyEndpointRuleSet())
                        .addField(ruleEngine())
            .addMethod(resolveEndpointMethod())
                        .addMethod(loadRuleSetMethod())
                        .addMethod(toIdentifierValueMap())
                        .addAnnotation(SdkInternalApi.class)
                        .build();
    }

    @Override
    public ClassName className() {
        Metadata md = intermediateModel.getMetadata();
        return ClassName.get(md.getFullInternalEndpointRulesPackageName(),
                             "Default" + endpointRulesSpecUtils.providerInterfaceName().simpleName());
    }

    private MethodSpec loadRuleSetMethod() {
        MethodSpec.Builder b = MethodSpec.methodBuilder("loadRuleSet")
                                         .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                                         .returns(EndpointRuleset.class);

        b.addCode(CodeBlock.builder()
                           .beginControlFlow("try ($T is = $T.class.getResourceAsStream($S))",
                                             InputStream.class,
                                             className(),
                                             endpointRuleSetResourcePath())
                           .addStatement("return $T.fromNode($T.parser().parse(is))", EndpointRuleset.class, JsonNode.class)
                           .endControlFlow()
                           .beginControlFlow("catch ($T e)", IOException.class)
                           .addStatement("throw $T.create($S, e)", SdkClientException.class, "Unable to close input stream")
                           .endControlFlow()
                           .build());

        return b.build();
    }


    private String endpointRuleSetResourcePath() {
        return String.format("/software/amazon/awssdk/services/%s/internal/endpoint-rule-set.json",
                             intermediateModel.getMetadata().getServiceName().toLowerCase(Locale.ENGLISH));
    }

    private FieldSpec lazyEndpointRuleSet() {
        return FieldSpec.builder(ParameterizedTypeName.get(Lazy.class, EndpointRuleset.class), RULE_SET_FIELD_NAME)
                        .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("new $T<>($T::loadRuleSet)", Lazy.class, className())
                        .build();
    }


    private FieldSpec ruleEngine() {
        return FieldSpec.builder(RuleEngine.class, ENGINE_FIELD_NAME)
                        .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("new $T()", DefaultRuleEngine.class)
                        .build();
    }

    private MethodSpec toIdentifierValueMap() {
        ParameterizedTypeName resultType = ParameterizedTypeName.get(Map.class, Identifier.class, Value.class);

        String paramsName = "params";
        MethodSpec.Builder b = MethodSpec.methodBuilder("toIdentifierValueMap")
                                         .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .addParameter(endpointRulesSpecUtils.parametersClassName(), paramsName)
                                         .returns(resultType);

        Map<String, ParameterModel> params = intermediateModel.getEndpointRuleSetModel().getParameters();

        String resultName = "paramsMap";
        b.addStatement("$T $N = new $T<>()", resultType, resultName, HashMap.class);

        params.forEach((name, model) -> {
            TypeName paramType = endpointRulesSpecUtils.toJavaType(model.getType());
            String methodVarName = Utils.unCapitalize(name);

            b.addStatement("$T $N = $N.$N()", paramType, methodVarName, paramsName, methodVarName);

            CodeBlock identifierExpr = CodeBlock.builder()
                                                .add("$T.of($S)", Identifier.class, name)
                                                .build();

            CodeBlock valueExpr = endpointRulesSpecUtils.valueCreationCode(model.getType(),
                                                                           CodeBlock.builder()
                                                                                    .add("$N", methodVarName)
                                                                                    .build());
            b.beginControlFlow("if ($N != null)", methodVarName)
                .addStatement("$N.put($L, $L)", resultName, identifierExpr, valueExpr);
            b.endControlFlow();
        });

        b.addStatement("return $N", resultName);

        return b.build();
    }

    private MethodSpec resolveEndpointMethod() {
        String paramsName = "endpointParams";

        MethodSpec.Builder b = MethodSpec.methodBuilder("resolveEndpoint")
            .addModifiers(Modifier.PUBLIC)
            .returns(Endpoint.class)
            .addAnnotation(Override.class)
            .addParameter(endpointRulesSpecUtils.parametersClassName(), paramsName);

        b.addStatement("$N.evaluate($N.getValue(), toIdentifierValueMap($N))",
                       ENGINE_FIELD_NAME, RULE_SET_FIELD_NAME, paramsName);

        return b.build();
    }
}
