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
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import java.util.Locale;
import java.util.Map;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.rules.AwsProviderUtils;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.ParameterModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.rules.ProviderUtils;
import software.amazon.awssdk.core.rules.model.Endpoint;
import software.amazon.awssdk.http.SdkHttpRequest;

public class EndpointProviderInterceptorSpec implements ClassSpec {
    private final IntermediateModel model;
    private final EndpointRulesSpecUtils endpointRulesSpecUtils;

    public EndpointProviderInterceptorSpec(IntermediateModel model) {
        this.model = model;
        this.endpointRulesSpecUtils = new EndpointRulesSpecUtils(model);
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder b = PoetUtils.createClassBuilder(className())
                                      .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                      .addAnnotation(SdkInternalApi.class)
                                      .addSuperinterface(ExecutionInterceptor.class);

        b.addMethod(modifyHttpRequestMethod());

        b.addMethod(ruleParams());

        return b.build();
    }

    @Override
    public ClassName className() {
        return endpointRulesSpecUtils.interceptorName();
    }

    private MethodSpec modifyHttpRequestMethod() {

        MethodSpec.Builder b = MethodSpec.methodBuilder("modifyHttpRequest")
                                         .addModifiers(Modifier.PUBLIC)
                                         .addAnnotation(Override.class)
                                         .returns(SdkHttpRequest.class)
                                         .addParameter(Context.ModifyHttpRequest.class, "context")
                                         .addParameter(ExecutionAttributes.class, "executionAttributes");

        String providerVar = "provider";

        // We skip resolution if the source of the endpoint is the endpoint discovery call
        // Note: endpointIsOverridden is a workaround until all rules handle endpoint overrides internally
        b.beginControlFlow("if ($1T.endpointIsOverridden(executionAttributes)"
                           + "|| $1T.endpointIsDiscovered(executionAttributes))",
                           ProviderUtils.class)
         .addStatement("return context.httpRequest()")
         .endControlFlow();

        b.addStatement("$1T $2N = ($1T) executionAttributes.getAttribute($3T.ENDPOINT_PROVIDER)",
                       endpointRulesSpecUtils.providerInterfaceName(), providerVar, SdkInternalExecutionAttribute.class);
        b.addStatement("$T result = $N.resolveEndpoint(ruleParams(executionAttributes))", Endpoint.class, providerVar);
        b.addStatement("return $T.setUri(context.httpRequest(), result.url())", ProviderUtils.class);
        return b.build();
    }

    private MethodSpec ruleParams() {
        MethodSpec.Builder b = MethodSpec.methodBuilder("ruleParams")
                                         .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                                         .returns(endpointRulesSpecUtils.parametersClassName())
                                         .addParameter(ExecutionAttributes.class, "executionAttributes");

        b.addCode("return $T.builder()", endpointRulesSpecUtils.parametersClassName());

        Map<String, ParameterModel> parameters = model.getEndpointRuleSetModel().getParameters();

        parameters.forEach((n, m) -> {
            if (m.getBuiltIn() == null) {
                return;
            }

            String setterName = endpointRulesSpecUtils.paramMethodName(n);
            String builtInFn;
            switch (m.getBuiltIn().toLowerCase(Locale.ENGLISH)) {
                case "aws::region":
                    builtInFn = "regionBuiltIn";
                    break;
                case "aws::usedualstack":
                    builtInFn = "dualStackEnabledBuiltIn";
                    break;
                case "aws::usefips":
                    builtInFn = "fipsEnabledBuiltIn";
                    break;
                default:
                    throw new RuntimeException("Don't know how to set built-in " + m.getBuiltIn());
            }

            b.addCode(".$N($T.$N(executionAttributes))", setterName, AwsProviderUtils.class, builtInFn);
        });

        b.addStatement(".build()");
        return b.build();
    }
}
