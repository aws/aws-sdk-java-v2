/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.client;

import java.util.Optional;
import software.amazon.awssdk.core.Request;
import software.amazon.awssdk.core.RequestOverrideConfig;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkRequestOverrideConfig;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.ServiceAdvancedConfiguration;
import software.amazon.awssdk.core.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.config.InternalAdvancedClientOption;
import software.amazon.awssdk.core.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.config.SdkClientConfiguration;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.http.SdkHttpFullRequestAdapter;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptorChain;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttributes;
import software.amazon.awssdk.core.internal.http.response.SdkErrorResponseHandler;
import software.amazon.awssdk.http.SdkHttpFullRequest;

public abstract class BaseClientHandler {
    private final ServiceAdvancedConfiguration serviceAdvancedConfiguration;
    private SdkClientConfiguration clientConfiguration;

    protected BaseClientHandler(SdkClientConfiguration clientConfiguration,
                                ServiceAdvancedConfiguration serviceAdvancedConfiguration) {
        this.clientConfiguration = clientConfiguration;
        this.serviceAdvancedConfiguration = serviceAdvancedConfiguration;
    }

    static <InputT extends SdkRequest> InputT finalizeSdkRequest(ExecutionContext executionContext) {
        runBeforeExecutionInterceptors(executionContext);
        return runModifyRequestInterceptors(executionContext);
    }

    static <InputT extends SdkRequest, OutputT> SdkHttpFullRequest finalizeSdkHttpFullRequest(
        ClientExecutionParams<InputT, OutputT> executionParams,
        ExecutionContext executionContext, InputT inputT,
        SdkClientConfiguration clientConfiguration) {

        runBeforeMarshallingInterceptors(executionContext);
        Request<InputT> request = executionParams.getMarshaller().marshall(inputT);
        request.setEndpoint(clientConfiguration.endpoint());

        executionContext.executionAttributes().putAttribute(SdkExecutionAttributes.SERVICE_NAME,
                                                            request.getServiceName());

        addHttpRequest(executionContext, SdkHttpFullRequestAdapter.toHttpFullRequest(request));
        runAfterMarshallingInterceptors(executionContext);
        return runModifyHttpRequestInterceptors(executionContext);
    }

    private static void runBeforeExecutionInterceptors(ExecutionContext executionContext) {
        executionContext.interceptorChain().beforeExecution(executionContext.interceptorContext(),
                                                            executionContext.executionAttributes());
    }

    private static <T> T runModifyRequestInterceptors(ExecutionContext executionContext) {
        InterceptorContext interceptorContext =
            executionContext.interceptorChain().modifyRequest(executionContext.interceptorContext(),
                                                              executionContext.executionAttributes());
        executionContext.interceptorContext(interceptorContext);
        return (T) interceptorContext.request();
    }

    private static void runBeforeMarshallingInterceptors(ExecutionContext executionContext) {
        executionContext.interceptorChain().beforeMarshalling(executionContext.interceptorContext(),
                                                              executionContext.executionAttributes());
    }

    private static void addHttpRequest(ExecutionContext executionContext, SdkHttpFullRequest request) {
        InterceptorContext interceptorContext = executionContext.interceptorContext().copy(b -> b.httpRequest(request));
        executionContext.interceptorContext(interceptorContext);
    }

    private static void runAfterMarshallingInterceptors(ExecutionContext executionContext) {
        executionContext.interceptorChain().afterMarshalling(executionContext.interceptorContext(),
                                                             executionContext.executionAttributes());
    }

    private static SdkHttpFullRequest runModifyHttpRequestInterceptors(ExecutionContext executionContext) {
        InterceptorContext interceptorContext =
            executionContext.interceptorChain().modifyHttpRequest(executionContext.interceptorContext(),
                                                                  executionContext.executionAttributes());
        executionContext.interceptorContext(interceptorContext);
        return interceptorContext.httpRequest();
    }

    private static <OutputT extends SdkResponse> OutputT runAfterUnmarshallingInterceptors(OutputT response,
                                                                                           ExecutionContext context) {
        // Update interceptor context to include response
        InterceptorContext interceptorContext =
            context.interceptorContext().copy(b -> b.response(response));

        context.interceptorChain().afterUnmarshalling(interceptorContext, context.executionAttributes());

        interceptorContext = context.interceptorChain().modifyResponse(interceptorContext, context.executionAttributes());

        // Store updated context
        context.interceptorContext(interceptorContext);

        return (OutputT) interceptorContext.response();
    }

    static <OutputT extends SdkResponse> HttpResponseHandler<OutputT> interceptorCalling(
        HttpResponseHandler<OutputT> delegate, ExecutionContext context) {
        return (response, executionAttributes) ->
            runAfterUnmarshallingInterceptors(delegate.handle(response, executionAttributes), context);
    }

    protected static <InputT extends SdkRequest, OutputT> ClientExecutionParams<InputT, OutputT> addErrorResponseHandler(
        ClientExecutionParams<InputT, OutputT> params) {
        return params.withErrorResponseHandler(
            new SdkErrorResponseHandler(params.getErrorResponseHandler()));
    }

    protected ExecutionContext createExecutionContext(SdkRequest originalRequest) {

        ClientOverrideConfiguration overrideConfiguration = clientConfiguration.overrideConfiguration();

        ExecutionAttributes executionAttributes = new ExecutionAttributes()
            .putAttribute(SdkExecutionAttributes.REQUEST_CONFIG, originalRequest.requestOverrideConfig()
                                                                                .filter(c -> c instanceof
                                                                                    SdkRequestOverrideConfig)
                                                                                .map(c -> (RequestOverrideConfig) c)
                                                                                .orElse(SdkRequestOverrideConfig.builder()
                                                                                                                .build()))
            .putAttribute(SdkExecutionAttributes.SERVICE_ADVANCED_CONFIG, serviceAdvancedConfiguration)
            .putAttribute(SdkExecutionAttributes.REQUEST_CONFIG, originalRequest.requestOverrideConfig()
                                                                                .map(c -> (SdkRequestOverrideConfig) c)
                                                                                .orElse(SdkRequestOverrideConfig.builder()
                                                                                                                .build()));

        return ExecutionContext.builder()
                               .interceptorChain(new ExecutionInterceptorChain(overrideConfiguration.executionInterceptors()))
                               .interceptorContext(InterceptorContext.builder()
                                                                     .request(originalRequest)
                                                                     .build())
                               .executionAttributes(executionAttributes)
                               .signerProvider(overrideConfiguration.advancedOption(SdkAdvancedClientOption.SIGNER_PROVIDER))
                               .build();
    }

    protected boolean isCalculateCrc32FromCompressedData() {
        return Optional.ofNullable(clientConfiguration.overrideConfiguration()
                                                      .advancedOption(InternalAdvancedClientOption
                                                                          .CRC32_FROM_COMPRESSED_DATA_ENABLED)).orElse(false);
    }
}
