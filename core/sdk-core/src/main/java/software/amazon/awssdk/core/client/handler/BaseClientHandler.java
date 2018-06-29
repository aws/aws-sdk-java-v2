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

package software.amazon.awssdk.core.client.handler;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.Request;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkRequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.http.SdkHttpFullRequestAdapter;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.internal.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.internal.http.DefaultSdkHttpResponse;
import software.amazon.awssdk.core.internal.http.response.SdkErrorResponseHandler;
import software.amazon.awssdk.core.internal.interceptor.ExecutionInterceptorChain;
import software.amazon.awssdk.core.internal.interceptor.InterceptorContext;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpResponse;

@SdkProtectedApi
public abstract class BaseClientHandler {
    private SdkClientConfiguration clientConfiguration;

    protected BaseClientHandler(SdkClientConfiguration clientConfiguration) {
        this.clientConfiguration = clientConfiguration;
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
        request.setEndpoint(clientConfiguration.option(SdkClientOption.ENDPOINT));

        executionContext.executionAttributes().putAttribute(SdkExecutionAttribute.SERVICE_NAME,
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

    /**
     * Add {@link SdkHttpResponse} to SdkResponse.
     */
    @SuppressWarnings("unchecked")
    private static <OutputT extends SdkResponse> HttpResponseHandler<OutputT> addHttpResponseMetadataResponseHandler(
        HttpResponseHandler<OutputT> delegate) {
        return (response, executionAttributes) -> {
            OutputT sdkResponse = delegate.handle(response, executionAttributes);

            return (OutputT) sdkResponse.toBuilder()
                                        .sdkHttpResponse(DefaultSdkHttpResponse.from(response))
                                        .build();
        };
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
        ExecutionAttributes executionAttributes = new ExecutionAttributes()
            .putAttribute(SdkExecutionAttribute.REQUEST_CONFIG, originalRequest.overrideConfiguration()
                                                                               .filter(c -> c instanceof
                                                                                        SdkRequestOverrideConfiguration)
                                                                               .map(c -> (RequestOverrideConfiguration) c)
                                                                               .orElse(SdkRequestOverrideConfiguration.builder()
                                                                                                                       .build()))
            .putAttribute(SdkExecutionAttribute.SERVICE_CONFIG,
                          clientConfiguration.option(SdkClientOption.SERVICE_CONFIGURATION))
            .putAttribute(SdkExecutionAttribute.REQUEST_CONFIG, originalRequest.overrideConfiguration()
                                                                               .map(c -> (SdkRequestOverrideConfiguration) c)
                                                                               .orElse(SdkRequestOverrideConfiguration.builder()
                                                                                                                       .build()));

        ExecutionInterceptorChain interceptorChain =
                new ExecutionInterceptorChain(clientConfiguration.option(SdkClientOption.EXECUTION_INTERCEPTORS));

        return ExecutionContext.builder()
                               .interceptorChain(interceptorChain)
                               .interceptorContext(InterceptorContext.builder()
                                                                     .request(originalRequest)
                                                                     .build())
                               .executionAttributes(executionAttributes)
                               .signer(clientConfiguration.option(SdkAdvancedClientOption.SIGNER))
                               .build();
    }

    protected boolean isCalculateCrc32FromCompressedData() {
        return clientConfiguration.option(SdkClientOption.CRC32_FROM_COMPRESSED_DATA_ENABLED);
    }

    /**
     * Decorate response handlers by running after unmarshalling Interceptors and adding http response metadata.
     */
    static <OutputT extends SdkResponse> HttpResponseHandler<OutputT> decorateResponseHandlers(
        HttpResponseHandler<OutputT> delegate, ExecutionContext executionContext) {
        HttpResponseHandler<OutputT> interceptorCallingResponseHandler = interceptorCalling(delegate, executionContext);
        return addHttpResponseMetadataResponseHandler(interceptorCallingResponseHandler);
    }
}
