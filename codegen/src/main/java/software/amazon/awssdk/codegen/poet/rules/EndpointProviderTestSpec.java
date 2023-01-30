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

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Modifier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.BuiltInParameter;
import software.amazon.awssdk.codegen.model.rules.endpoints.EndpointTestModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.ParameterModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.core.rules.testing.BaseEndpointProviderTest;
import software.amazon.awssdk.core.rules.testing.EndpointProviderTestCase;
import software.amazon.awssdk.regions.Region;

public class EndpointProviderTestSpec implements ClassSpec {
    private static final String PROVIDER_NAME = "PROVIDER";
    private final IntermediateModel model;
    private final EndpointRulesSpecUtils endpointRulesSpecUtils;

    public EndpointProviderTestSpec(IntermediateModel model) {
        this.model = model;
        this.endpointRulesSpecUtils = new EndpointRulesSpecUtils(model);
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder b = PoetUtils.createClassBuilder(className())
                                      .superclass(BaseEndpointProviderTest.class)
                                      .addField(ruleEngine())
                                      .addModifiers(Modifier.PUBLIC);

        b.addMethod(testMethod());
        b.addMethod(testsCasesMethod());

        return b.build();
    }

    @Override
    public ClassName className() {
        return endpointRulesSpecUtils.endpointProviderTestsName();
    }

    private FieldSpec ruleEngine() {
        return FieldSpec.builder(endpointRulesSpecUtils.providerInterfaceName(), PROVIDER_NAME)
                        .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$T.defaultProvider()", endpointRulesSpecUtils.providerInterfaceName())
                        .build();
    }

    private MethodSpec testsCasesMethod() {

        TypeName returnType = ParameterizedTypeName.get(List.class, EndpointProviderTestCase.class);

        MethodSpec.Builder b = MethodSpec.methodBuilder("testCases")
                                         .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                                         .returns(returnType);

        b.addStatement("$T testCases = new $T<>()", returnType, ArrayList.class);

        model.getEndpointTestSuiteModel().getTestCases().forEach(test -> {
            b.addStatement("testCases.add(new $T($L, $L))",
                           EndpointProviderTestCase.class,
                           createTestCase(test),
                           TestGeneratorUtils.createExpect(test.getExpect(), null, null));
        });

        b.addStatement("return testCases");
        return b.build();
    }

    private MethodSpec testMethod() {
        AnnotationSpec methodSourceSpec = AnnotationSpec.builder(MethodSource.class)
                                                        .addMember("value", "$S", "testCases")
                                                        .build();

        MethodSpec.Builder b = MethodSpec.methodBuilder("resolvesCorrectEndpoint")
                                         .addModifiers(Modifier.PUBLIC)
                                         .addParameter(EndpointProviderTestCase.class, "tc")
                                         .addAnnotation(methodSourceSpec)
                                         .addAnnotation(ParameterizedTest.class)
                                         .returns(void.class);

        b.addStatement("verify(tc)");

        return b.build();
    }

    private CodeBlock createTestCase(EndpointTestModel test) {
        CodeBlock.Builder b = CodeBlock.builder();

        b.beginControlFlow("() ->");
        ClassName parametersClass = endpointRulesSpecUtils.parametersClassName();
        b.add("$T builder = $T.builder();", parametersClass.nestedClass("Builder"), parametersClass);

        if (test.getParams() != null) {
            test.getParams().forEach((n, v) -> {
                if (!isDeclaredParam(n)) {
                    return;
                }

                String setterName = endpointRulesSpecUtils.paramMethodName(n);
                CodeBlock valueLiteral = endpointRulesSpecUtils.treeNodeToLiteral(v);
                if (isRegionBuiltIn(n)) {
                    b.add("builder.$N($T.of($L));", setterName, Region.class, valueLiteral);
                } else {
                    b.add("builder.$N($L);", setterName, valueLiteral);
                }
            });
        }
        b.add("return $N.resolveEndpoint(builder.build()).join();", PROVIDER_NAME);
        b.endControlFlow();
        return b.build();
    }

    private boolean isRegionBuiltIn(String paramName) {
        Map<String, ParameterModel> parameters = model.getEndpointRuleSetModel().getParameters();
        ParameterModel param = parameters.get(paramName);
        return param.getBuiltInEnum() == BuiltInParameter.AWS_REGION;
    }

    private boolean isDeclaredParam(String paramName) {
        Map<String, ParameterModel> parameters = model.getEndpointRuleSetModel().getParameters();
        return parameters.containsKey(paramName);
    }
}
