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

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.http.Crc32Validation;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.internal.http.AmazonAsyncHttpClient;
import software.amazon.awssdk.core.internal.http.TransformingAsyncResponseHandler;
import software.amazon.awssdk.core.internal.http.async.SyncResponseHandlerAdapter;
import software.amazon.awssdk.core.internal.util.ThrowableUtils;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
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

        TransformingAsyncResponseHandler<OutputT> sdkHttpResponseHandler =
                new SyncResponseHandlerAdapter<>(decoratedResponseHandlers,
                                                 crc32Validator,
                                                 executionContext.executionAttributes());

        return execute(executionParams, executionContext, sdkHttpResponseHandler);
    }

    @Override
    public <InputT extends SdkRequest, OutputT extends SdkResponse, ReturnT> CompletableFuture<ReturnT> execute(
        ClientExecutionParams<InputT, OutputT> executionParams,
        AsyncResponseTransformer<OutputT, ReturnT> asyncResponseTransformer) {

        ExecutionContext context = createExecutionContext(executionParams);

        return execute(executionParams, context, new UnmarshallingSdkHttpResponseHandler<>(asyncResponseTransformer, context,
                                                                                           executionParams.getResponseHandler()));
    }

    private <InputT extends SdkRequest, OutputT extends SdkResponse, ReturnT> CompletableFuture<ReturnT> execute(
        ClientExecutionParams<InputT, OutputT> executionParams,
        ExecutionContext executionContext,
        TransformingAsyncResponseHandler<ReturnT> sdkHttpResponseHandler) {

        try {
            InterceptorContext finalizeSdkRequestContext = finalizeSdkRequest(executionContext);
            InputT inputT = (InputT) finalizeSdkRequestContext.request();

            InterceptorContext finalizeSdkHttpRequestContext = finalizeSdkHttpFullRequest(executionParams,
                                                                                          executionContext,
                                                                                          inputT,
                                                                                          clientConfiguration);

            SdkHttpFullRequest marshalled = (SdkHttpFullRequest) finalizeSdkHttpRequestContext.httpRequest();

            TransformingAsyncResponseHandler<ReturnT> successResponseHandler = new InterceptorCallingHttpResponseHandler<>(
                sdkHttpResponseHandler, executionContext);

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
     * Error responses are never streaming so we always use {@link SyncResponseHandlerAdapter}.
     *
     * @return Async handler for error responses.
     */
    private TransformingAsyncResponseHandler<? extends SdkException> resolveErrorResponseHandler(
        ClientExecutionParams<?, ?> executionParams,
        ExecutionContext executionContext,
        Function<SdkHttpFullResponse, SdkHttpFullResponse> responseAdapter) {
        SyncResponseHandlerAdapter<? extends SdkException> result =
            new SyncResponseHandlerAdapter<>(executionParams.getErrorResponseHandler(),
                                             responseAdapter,
                                             executionContext.executionAttributes());
        return new InterceptorCallingHttpResponseHandler<>(result, executionContext);
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

    private static final class InterceptorCallingHttpResponseHandler<T> implements TransformingAsyncResponseHandler<T> {
        private final TransformingAsyncResponseHandler<T> delegate;
        private final ExecutionContext context;

        private InterceptorCallingHttpResponseHandler(TransformingAsyncResponseHandler<T> delegate, ExecutionContext context) {
            this.delegate = delegate;
            this.context = context;
        }

        private SdkHttpResponse beforeUnmarshalling(SdkHttpFullResponse response, ExecutionContext context) {
            // Update interceptor context to include response
            InterceptorContext interceptorContext =
                context.interceptorContext().copy(b -> b.httpResponse(response));

            // interceptors.afterTransmission
            context.interceptorChain().afterTransmission(interceptorContext, context.executionAttributes());

            // interceptors.modifyHttpResponse
            interceptorContext = context.interceptorChain().modifyHttpResponse(interceptorContext, context.executionAttributes());

            // interceptors.beforeUnmarshalling
            context.interceptorChain().beforeUnmarshalling(interceptorContext, context.executionAttributes());

            // Store updated context
            context.interceptorContext(interceptorContext);

            return interceptorContext.httpResponse();
        }

        @Override
        public void onHeaders(SdkHttpResponse response) {
            delegate.onHeaders(beforeUnmarshalling((SdkHttpFullResponse) response, context)); // TODO: Ew
        }

        @Override
        public void onError(Throwable error) {
            delegate.onError(error);
        }

        @Override
        public void onStream(Publisher<ByteBuffer> publisher) {
            Optional<Publisher<ByteBuffer>> newPublisher = context.interceptorChain()
                                                                  .modifyAsyncHttpResponse(context.interceptorContext()
                                                                                                  .toBuilder()
                                                                                                  .responsePublisher(publisher)
                                                                                                  .build(),
                                                                                 context.executionAttributes())
                                                                  .responsePublisher();

            if (newPublisher.isPresent()) {
                delegate.onStream(newPublisher.get());
            } else {
                delegate.onStream(publisher);
            }
        }

        @Override
        public CompletableFuture<T> prepare() {
            return delegate.prepare();
        }
    }

    /**
     * Adapter to {@link AsyncResponseTransformer} that performs unmarshalling and calls {@link
     * software.amazon.awssdk.core.interceptor.ExecutionInterceptor}
     * callbacks.
     *
     * @param <OutputT> Unmarshalled POJO response type.
     * @param <ReturnT> Return type of {@link AsyncResponseTransformer}
     */
    private class UnmarshallingSdkHttpResponseHandler<OutputT extends SdkResponse, ReturnT>
        implements TransformingAsyncResponseHandler<ReturnT> {

        private final AsyncResponseTransformer<OutputT, ReturnT> asyncResponseTransformer;
        private final ExecutionContext executionContext;
        private final HttpResponseHandler<OutputT> responseHandler;
        private CompletableFuture<ReturnT> transformFuture;

        UnmarshallingSdkHttpResponseHandler(AsyncResponseTransformer<OutputT, ReturnT> asyncResponseTransformer,
                                            ExecutionContext executionContext,
                                            HttpResponseHandler<OutputT> responseHandler) {
            this.asyncResponseTransformer = asyncResponseTransformer;
            this.executionContext = executionContext;
            this.responseHandler = responseHandler;
        }

        @Override
        public void onHeaders(SdkHttpResponse response) {
            try {
                // TODO would be better to pass in AwsExecutionAttributes to the async response handler so we can
                // provide them to HttpResponseHandler
                OutputT resp =
                    decorateResponseHandlers(responseHandler, executionContext)
                        .handle((SdkHttpFullResponse) response, null);

                asyncResponseTransformer.onResponse(resp);
            } catch (Exception e) {
                transformFuture.completeExceptionally(e);
            }
        }

        @Override
        public void onStream(Publisher<ByteBuffer> publisher) {
            asyncResponseTransformer.onStream(SdkPublisher.adapt(publisher));
        }

        @Override
        public void onError(Throwable error) {
            asyncResponseTransformer.exceptionOccurred(error);
        }

        @Override
        public CompletableFuture<ReturnT> prepare() {
            this.transformFuture = asyncResponseTransformer.prepare();
            return transformFuture;
        }
    }
}
