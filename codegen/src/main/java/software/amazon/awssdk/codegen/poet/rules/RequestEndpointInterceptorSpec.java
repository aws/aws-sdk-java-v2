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
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.SdkHttpRequest;

public class RequestEndpointInterceptorSpec implements ClassSpec {
    private final EndpointRulesSpecUtils endpointRulesSpecUtils;

    public RequestEndpointInterceptorSpec(IntermediateModel model) {
        this.endpointRulesSpecUtils = new EndpointRulesSpecUtils(model);
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder b = PoetUtils.createClassBuilder(className())
                                      .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                      .addAnnotation(SdkInternalApi.class)
                                      .addSuperinterface(ExecutionInterceptor.class);

        b.addMethod(modifyHttpRequestMethod());

        return b.build();
    }

    @Override
    public ClassName className() {
        return endpointRulesSpecUtils.requestModifierInterceptorName();
    }

    private MethodSpec modifyHttpRequestMethod() {

        MethodSpec.Builder b = MethodSpec.methodBuilder("modifyHttpRequest")
                                         .addModifiers(Modifier.PUBLIC)
                                         .addAnnotation(Override.class)
                                         .returns(SdkHttpRequest.class)
                                         .addParameter(Context.ModifyHttpRequest.class, "context")
                                         .addParameter(ExecutionAttributes.class, "executionAttributes");


        // We skip setting the endpoint here if the source of the endpoint is the endpoint discovery call
        b.beginControlFlow("if ($1T.endpointIsDiscovered(executionAttributes))",
                           endpointRulesSpecUtils.rulesRuntimeClassName("AwsEndpointProviderUtils"))
         .addStatement("return context.httpRequest()")
         .endControlFlow().build();

        b.addStatement("$1T endpoint = ($1T) executionAttributes.getAttribute($2T.RESOLVED_ENDPOINT)",
                       Endpoint.class,
                       SdkInternalExecutionAttribute.class);
        b.addStatement("return $T.setUri(context.httpRequest(),"
                       + "executionAttributes.getAttribute($T.CLIENT_ENDPOINT),"
                       + "endpoint.url())",
                       endpointRulesSpecUtils.rulesRuntimeClassName("AwsEndpointProviderUtils"),
                       SdkExecutionAttribute.class);
        return b.build();
    }

}
