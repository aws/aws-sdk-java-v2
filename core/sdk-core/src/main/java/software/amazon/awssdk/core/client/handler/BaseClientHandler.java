/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.net.URI;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptorChain;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.utils.StringUtils;

@SdkProtectedApi
public abstract class BaseClientHandler {
    private SdkClientConfiguration clientConfiguration;

    protected BaseClientHandler(SdkClientConfiguration clientConfiguration) {
        this.clientConfiguration = clientConfiguration;
    }

    /**
     * Finalize {@link SdkRequest} by running beforeExecution and modifyRequest interceptors.
     *
     * @param executionContext the execution context
     * @return the {@link InterceptorContext}
     */
    static InterceptorContext finalizeSdkRequest(ExecutionContext executionContext) {
        runBeforeExecutionInterceptors(executionContext);
        return runModifyRequestInterceptors(executionContext);
    }

    /**
     * Finalize {@link SdkHttpFullRequest} by running beforeMarshalling, afterMarshalling,
     * modifyHttpRequest, modifyHttpContent and modifyAsyncHttpContent interceptors
     */
    static <InputT extends SdkRequest, OutputT> InterceptorContext finalizeSdkHttpFullRequest(
        ClientExecutionParams<InputT, OutputT> executionParams,
        ExecutionContext executionContext, InputT inputT,
        SdkClientConfiguration clientConfiguration) {

        runBeforeMarshallingInterceptors(executionContext);
        SdkHttpFullRequest request = executionParams.getMarshaller().marshall(inputT);
        request = modifyEndpointHostIfNeeded(request, clientConfiguration, executionParams);

        addHttpRequest(executionContext, request);
        runAfterMarshallingInterceptors(executionContext);
        return runModifyHttpRequestAndHttpContentInterceptors(executionContext);
    }

    private static void runBeforeExecutionInterceptors(ExecutionContext executionContext) {
        executionContext.interceptorChain().beforeExecution(executionContext.interceptorContext(),
                                                            executionContext.executionAttributes());
    }

    private static InterceptorContext runModifyRequestInterceptors(ExecutionContext executionContext) {
        InterceptorContext interceptorContext =
            executionContext.interceptorChain().modifyRequest(executionContext.interceptorContext(),
                                                              executionContext.executionAttributes());
        executionContext.interceptorContext(interceptorContext);
        return interceptorContext;
    }

    private static void runBeforeMarshallingInterceptors(ExecutionContext executionContext) {
        executionContext.interceptorChain().beforeMarshalling(executionContext.interceptorContext(),
                                                              executionContext.executionAttributes());
    }

    /**
     * Modifies the given {@link SdkHttpFullRequest} with new host if host prefix is enabled and set.
     */
    private static SdkHttpFullRequest modifyEndpointHostIfNeeded(SdkHttpFullRequest originalRequest,
                                                                 SdkClientConfiguration clientConfiguration,
                                                                 ClientExecutionParams executionParams) {
        if (executionParams.discoveredEndpoint() != null) {
            URI discoveredEndpoint = executionParams.discoveredEndpoint();
            return originalRequest.toBuilder().host(discoveredEndpoint.getHost()).port(discoveredEndpoint.getPort()).build();
        }

        Boolean disableHostPrefixInjection = clientConfiguration.option(SdkAdvancedClientOption.DISABLE_HOST_PREFIX_INJECTION);
        if ((disableHostPrefixInjection != null && disableHostPrefixInjection.equals(Boolean.TRUE)) ||
            StringUtils.isEmpty(executionParams.hostPrefixExpression())) {
            return originalRequest;
        }

        return originalRequest.toBuilder()
                              .host(executionParams.hostPrefixExpression() + originalRequest.host())
                              .build();
    }

    private static void addHttpRequest(ExecutionContext executionContext, SdkHttpFullRequest request) {
        InterceptorContext interceptorContext = executionContext.interceptorContext().copy(b -> b.httpRequest(request));
        executionContext.interceptorContext(interceptorContext);
    }

    private static void runAfterMarshallingInterceptors(ExecutionContext executionContext) {
        executionContext.interceptorChain().afterMarshalling(executionContext.interceptorContext(),
                                                             executionContext.executionAttributes());
    }

    private static InterceptorContext runModifyHttpRequestAndHttpContentInterceptors(ExecutionContext executionContext) {
        InterceptorContext interceptorContext =
            executionContext.interceptorChain().modifyHttpRequestAndHttpContent(executionContext.interceptorContext(),
                                                                                executionContext.executionAttributes());
        executionContext.interceptorContext(interceptorContext);
        return interceptorContext;
    }

    /**
     * Run afterUnmarshalling and modifyResponse interceptors.
     */
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

    protected <InputT extends SdkRequest, OutputT extends SdkResponse> ExecutionContext createExecutionContext(
        ClientExecutionParams<InputT, OutputT> params) {

        SdkRequest originalRequest = params.getInput();
        ExecutionAttributes executionAttributes = new ExecutionAttributes()
            .putAttribute(SdkExecutionAttribute.SERVICE_CONFIG,
                          clientConfiguration.option(SdkClientOption.SERVICE_CONFIGURATION))
            .putAttribute(SdkExecutionAttribute.SERVICE_NAME, clientConfiguration.option(SdkClientOption.SERVICE_NAME));

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
    <OutputT extends SdkResponse> HttpResponseHandler<OutputT> decorateResponseHandlers(
        HttpResponseHandler<OutputT> delegate, ExecutionContext executionContext) {
        HttpResponseHandler<OutputT> interceptorCallingResponseHandler = interceptorCalling(delegate, executionContext);
        return new AttachHttpMetadataResponseHandler<>(interceptorCallingResponseHandler);
    }
}
