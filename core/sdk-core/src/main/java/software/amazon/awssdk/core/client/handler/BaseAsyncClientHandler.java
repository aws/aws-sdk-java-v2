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

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.http.Crc32Validation;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.internal.http.AmazonAsyncHttpClient;
import software.amazon.awssdk.core.internal.http.TransformingAsyncResponseHandler;
import software.amazon.awssdk.core.internal.http.async.AsyncAfterTransmissionInterceptorCallingResponseHandler;
import software.amazon.awssdk.core.internal.http.async.AsyncResponseHandler;
import software.amazon.awssdk.core.internal.http.async.AsyncStreamingResponseHandler;
import software.amazon.awssdk.core.internal.util.ThrowableUtils;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.utils.CompletableFutureUtils;

@SdkProtectedApi
public abstract class BaseAsyncClientHandler extends BaseClientHandler implements AsyncClientHandler {
    private final SdkClientConfiguration clientConfiguration;
    private final AmazonAsyncHttpClient client;
    private final Function<SdkHttpFullResponse, SdkHttpFullResponse> crc32Validator;

    protected BaseAsyncClientHandler(SdkClientConfiguration clientConfiguration,
                                     AmazonAsyncHttpClient client) {
        super(clientConfiguration);
        this.clientConfiguration = clientConfiguration;
        this.client = client;
        this.crc32Validator = response -> Crc32Validation.validate(isCalculateCrc32FromCompressedData(), response);
    }

    @Override
    public <InputT extends SdkRequest, OutputT extends SdkResponse> CompletableFuture<OutputT> execute(
        ClientExecutionParams<InputT, OutputT> executionParams) {
        ExecutionContext executionContext = createExecutionContext(executionParams);

        HttpResponseHandler<OutputT> decoratedResponseHandlers =
            decorateResponseHandlers(executionParams.getResponseHandler(), executionContext);

        TransformingAsyncResponseHandler<OutputT> asyncResponseHandler =
            new AsyncResponseHandler<>(decoratedResponseHandlers,
                                       crc32Validator,
                                       executionContext.executionAttributes());

        return doExecute(executionParams, executionContext, asyncResponseHandler);
    }

    @Override
    public <InputT extends SdkRequest, OutputT extends SdkResponse, ReturnT> CompletableFuture<ReturnT> execute(
        ClientExecutionParams<InputT, OutputT> executionParams,
        AsyncResponseTransformer<OutputT, ReturnT> asyncResponseTransformer) {

        ExecutionContext context = createExecutionContext(executionParams);

        HttpResponseHandler<OutputT> decoratedResponseHandlers =
            decorateResponseHandlers(executionParams.getResponseHandler(), context);

        AsyncStreamingResponseHandler<OutputT, ReturnT> asyncStreamingResponseHandler =
            new AsyncStreamingResponseHandler<>(asyncResponseTransformer, decoratedResponseHandlers);

        return doExecute(executionParams, context, asyncStreamingResponseHandler);
    }

    private <InputT extends SdkRequest, OutputT extends SdkResponse, ReturnT> CompletableFuture<ReturnT> doExecute(
        ClientExecutionParams<InputT, OutputT> executionParams,
        ExecutionContext executionContext,
        TransformingAsyncResponseHandler<ReturnT> asyncResponseHandler) {

        try {

            // Running beforeExecution interceptors and modifyRequest interceptors.
            InterceptorContext finalizeSdkRequestContext = finalizeSdkRequest(executionContext);
            InputT inputT = (InputT) finalizeSdkRequestContext.request();

            // Running beforeMarshalling, afterMarshalling and modifyHttpRequest, modifyHttpContent,
            // modifyAsyncHttpContent interceptors
            InterceptorContext finalizeSdkHttpRequestContext = finalizeSdkHttpFullRequest(executionParams,
                                                                                          executionContext,
                                                                                          inputT,
                                                                                          clientConfiguration);

            SdkHttpFullRequest marshalled = (SdkHttpFullRequest) finalizeSdkHttpRequestContext.httpRequest();

            // For non-streaming requests, RequestBody can be modified in the interceptors. eg:
            // CreateMultipartUploadRequestInterceptor
            if (!finalizeSdkHttpRequestContext.asyncRequestBody().isPresent() &&
                finalizeSdkHttpRequestContext.requestBody().isPresent()) {
                marshalled = marshalled.toBuilder()
                                       .contentStreamProvider(
                                           finalizeSdkHttpRequestContext.requestBody().get().contentStreamProvider())
                                       .build();
            }

            TransformingAsyncResponseHandler<ReturnT> successResponseHandler =
                new AsyncAfterTransmissionInterceptorCallingResponseHandler<>(asyncResponseHandler, executionContext);

            TransformingAsyncResponseHandler<? extends SdkException> errorHandler =
                resolveErrorResponseHandler(executionParams, executionContext, crc32Validator);

            return invoke(marshalled, finalizeSdkHttpRequestContext.asyncRequestBody().orElse(null), inputT,
                          executionContext, successResponseHandler, errorHandler)
                .handle((resp, err) -> {
                    if (err != null) {
                        throw ThrowableUtils.failure(err);
                    }
                    return resp;
                });
        } catch (Throwable t) {
            return CompletableFutureUtils.failedFuture(ThrowableUtils.asSdkException(t));
        }
    }

    @Override
    public void close() {
        client.close();
    }

    /**
     * Error responses are never streaming so we always use {@link AsyncResponseHandler}.
     *
     * @return Async handler for error responses.
     */
    private TransformingAsyncResponseHandler<? extends SdkException> resolveErrorResponseHandler(
        ClientExecutionParams<?, ?> executionParams,
        ExecutionContext executionContext,
        Function<SdkHttpFullResponse, SdkHttpFullResponse> responseAdapter) {
        AsyncResponseHandler<? extends SdkException> result =
            new AsyncResponseHandler<>(executionParams.getErrorResponseHandler(),
                                       responseAdapter,
                                       executionContext.executionAttributes());
        return new AsyncAfterTransmissionInterceptorCallingResponseHandler<>(result, executionContext);
    }

    /**
     * Invoke the request using the http client. Assumes credentials (or lack thereof) have been
     * configured in the ExecutionContext beforehand.
     **/
    private <InputT extends SdkRequest, OutputT> CompletableFuture<OutputT> invoke(
        SdkHttpFullRequest request,
        AsyncRequestBody requestProvider,
        InputT originalRequest,
        ExecutionContext executionContext,
        TransformingAsyncResponseHandler<OutputT> responseHandler,
        TransformingAsyncResponseHandler<? extends SdkException> errorResponseHandler) {
        return client.requestExecutionBuilder()
                     .requestProvider(requestProvider)
                     .request(request)
                     .originalRequest(originalRequest)
                     .executionContext(executionContext)
                     .errorResponseHandler(errorResponseHandler)
                     .execute(responseHandler);
    }
}
