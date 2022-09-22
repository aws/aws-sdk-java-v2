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
import java.util.HashMap;
import java.util.Map;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.rules.endpoints.BuiltInParameter;
import software.amazon.awssdk.codegen.model.rules.endpoints.ParameterModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.core.rules.DefaultRuleEngine;
import software.amazon.awssdk.core.rules.EndpointRuleset;
import software.amazon.awssdk.core.rules.Identifier;
import software.amazon.awssdk.core.rules.ProviderUtils;
import software.amazon.awssdk.core.rules.Value;
import software.amazon.awssdk.core.rules.model.Endpoint;

public class EndpointProviderSpec implements ClassSpec {
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
                        .addField(ruleSet())
                        .addMethod(resolveEndpointMethod())
                        .addMethod(toIdentifierValueMap())
                        .addMethod(ruleSetBuildMethod())
                        .addAnnotation(SdkInternalApi.class)
                        .build();
    }

    @Override
    public ClassName className() {
        Metadata md = intermediateModel.getMetadata();
        return ClassName.get(md.getFullInternalEndpointRulesPackageName(),
                             "Default" + endpointRulesSpecUtils.providerInterfaceName().simpleName());
    }

    private FieldSpec ruleSet() {
        return FieldSpec.builder(EndpointRuleset.class, RULE_SET_FIELD_NAME)
                        .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("ruleSet()")
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
            String methodVarName = endpointRulesSpecUtils.paramMethodName(name);

            CodeBlock coerce;
            // We treat region specially and generate it as the Region type,
            // so we need to call id() to convert it back to string
            if (model.getBuiltInEnum() == BuiltInParameter.AWS_REGION) {
                coerce = CodeBlock.builder().add(".id()").build();
            } else {
                coerce = CodeBlock.builder().build();
            }

            b.addStatement("$T $N = $N.$N()$L", paramType, methodVarName, paramsName, methodVarName, coerce);

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

        b.addStatement("$T res = new $T().evaluate($N, toIdentifierValueMap($N))",
                       Value.class, DefaultRuleEngine.class, RULE_SET_FIELD_NAME, paramsName);

        b.addStatement("return $T.valueAsEndpointOrThrow($N)", ProviderUtils.class, "res");

        return b.build();
    }

    private MethodSpec ruleSetBuildMethod() {
        RuleSetCreationSpec ruleSetCreationSpec = new RuleSetCreationSpec(intermediateModel);
        MethodSpec.Builder b = MethodSpec.methodBuilder("ruleSet")
                                         .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                                         .returns(EndpointRuleset.class)
                                         .addStatement("return $L", ruleSetCreationSpec.ruleSetCreationExpr());
        return b.build();
    }
}
