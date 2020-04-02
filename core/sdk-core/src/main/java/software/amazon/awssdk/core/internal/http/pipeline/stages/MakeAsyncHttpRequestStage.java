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

package software.amazon.awssdk.core.internal.http.pipeline.stages;

import static software.amazon.awssdk.core.internal.http.timers.TimerUtils.resolveTimeoutInMillis;
import static software.amazon.awssdk.http.Header.CONTENT_LENGTH;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.ApiCallAttemptTimeoutException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.TransformingAsyncResponseHandler;
import software.amazon.awssdk.core.internal.http.async.SimpleHttpContentPublisher;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.internal.http.timers.TimeoutTracker;
import software.amazon.awssdk.core.internal.http.timers.TimerUtils;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;
import software.amazon.awssdk.utils.Logger;

/**
 * Delegate to the HTTP implementation to make an HTTP request and receive the response.
 */
@SdkInternalApi
public final class MakeAsyncHttpRequestStage<OutputT>
    implements RequestPipeline<SdkHttpFullRequest, CompletableFuture<Response<OutputT>>> {

    private static final Logger log = Logger.loggerFor(MakeAsyncHttpRequestStage.class);

    private final SdkAsyncHttpClient sdkAsyncHttpClient;
    private final TransformingAsyncResponseHandler<Response<OutputT>> responseHandler;
    private final Executor futureCompletionExecutor;
    private final ScheduledExecutorService timeoutExecutor;
    private final Duration apiCallAttemptTimeout;

    public MakeAsyncHttpRequestStage(TransformingAsyncResponseHandler<Response<OutputT>> responseHandler,
                                     HttpClientDependencies dependencies) {
        this.responseHandler = responseHandler;
        this.futureCompletionExecutor =
            dependencies.clientConfiguration().option(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR);
        this.sdkAsyncHttpClient = dependencies.clientConfiguration().option(SdkClientOption.ASYNC_HTTP_CLIENT);
        this.apiCallAttemptTimeout = dependencies.clientConfiguration().option(SdkClientOption.API_CALL_ATTEMPT_TIMEOUT);
        this.timeoutExecutor = dependencies.clientConfiguration().option(SdkClientOption.SCHEDULED_EXECUTOR_SERVICE);
    }

    @Override
    public CompletableFuture<Response<OutputT>> execute(SdkHttpFullRequest request,
                                                        RequestExecutionContext context) throws Exception {
        return executeHttpRequest(request, context);
    }

    private static final class WrappedErrorForwardingResponseHandler<T>
            implements TransformingAsyncResponseHandler<T> {

        private final TransformingAsyncResponseHandler<T> wrappedHandler;
        private final CompletableFuture<T> responseFuture;

        private WrappedErrorForwardingResponseHandler(TransformingAsyncResponseHandler<T> wrappedHandler,
                                                      CompletableFuture<T> responseFuture) {
            this.wrappedHandler = wrappedHandler;
            this.responseFuture = responseFuture;

        }

        private static <T> WrappedErrorForwardingResponseHandler<T> of(
                TransformingAsyncResponseHandler<T> wrappedHandler,
                CompletableFuture<T> responseFuture) {

            return new WrappedErrorForwardingResponseHandler<>(wrappedHandler, responseFuture);
        }

        @Override
        public CompletableFuture<T> prepare() {
            return wrappedHandler.prepare();
        }

        @Override
        public void onHeaders(SdkHttpResponse headers) {
            wrappedHandler.onHeaders(headers);
        }

        @Override
        public void onStream(Publisher<ByteBuffer> stream) {
            wrappedHandler.onStream(stream);
        }

        @Override
        public void onError(Throwable error) {
            responseFuture.completeExceptionally(error);
            wrappedHandler.onError(error);
        }
    }

    private CompletableFuture<Response<OutputT>> executeHttpRequest(SdkHttpFullRequest request,
                                                                    RequestExecutionContext context) {
        CompletableFuture<Response<OutputT>> responseFuture = new CompletableFuture<>();

        // Wrap the response handler in a layer that will notify the newly created responseFuture when the onError event
        // is triggered
        TransformingAsyncResponseHandler<Response<OutputT>> wrappedResponseHandler =
            WrappedErrorForwardingResponseHandler.of(responseHandler, responseFuture);

        CompletableFuture<Response<OutputT>> responseHandlerFuture = wrappedResponseHandler.prepare();

        SdkHttpContentPublisher requestProvider = context.requestProvider() == null
                                                  ? new SimpleHttpContentPublisher(request)
                                                  : new SdkHttpContentPublisherAdapter(context.requestProvider());
        // Set content length if it hasn't been set already.
        SdkHttpFullRequest requestWithContentLength = getRequestWithContentLength(request, requestProvider);

        AsyncExecuteRequest executeRequest = AsyncExecuteRequest.builder()
                                                                .request(requestWithContentLength)
                                                                .requestContentPublisher(requestProvider)
                                                                .responseHandler(wrappedResponseHandler)
                                                                .fullDuplex(isFullDuplex(context.executionAttributes()))
                                                                .build();

        CompletableFuture<Void> httpClientFuture = sdkAsyncHttpClient.execute(executeRequest);

        TimeoutTracker timeoutTracker = setupAttemptTimer(responseFuture, context);
        context.apiCallAttemptTimeoutTracker(timeoutTracker);

        // Forward the cancellation
        responseFuture.whenComplete((r, t) -> {
            if (t != null) {
                httpClientFuture.completeExceptionally(t);
            }
        });

        // Offload the completion of the future returned from this stage onto
        // the future completion executor
        responseHandlerFuture.whenCompleteAsync((r, t) -> {
            if (t == null) {
                responseFuture.complete(r);
            } else {
                responseFuture.completeExceptionally(t);
            }
        }, futureCompletionExecutor);

        return responseFuture;
    }

    private boolean isFullDuplex(ExecutionAttributes executionAttributes) {
        return executionAttributes.getAttribute(SdkInternalExecutionAttribute.IS_FULL_DUPLEX) != null &&
               executionAttributes.getAttribute(SdkInternalExecutionAttribute.IS_FULL_DUPLEX);
    }

    private SdkHttpFullRequest getRequestWithContentLength(SdkHttpFullRequest request, SdkHttpContentPublisher requestProvider) {
        if (shouldSetContentLength(request, requestProvider)) {
            return request.toBuilder()
                          .putHeader(CONTENT_LENGTH, String.valueOf(requestProvider.contentLength().get()))
                          .build();
        }
        return request;
    }

    private boolean shouldSetContentLength(SdkHttpFullRequest request, SdkHttpContentPublisher requestProvider) {

        if (request.method() == SdkHttpMethod.GET || request.method() == SdkHttpMethod.HEAD ||
            request.firstMatchingHeader(CONTENT_LENGTH).isPresent()) {
            return false;
        }

        return Optional.ofNullable(requestProvider).flatMap(SdkHttpContentPublisher::contentLength).isPresent();
    }

    private TimeoutTracker setupAttemptTimer(CompletableFuture<Response<OutputT>> executeFuture, RequestExecutionContext ctx) {
        long timeoutMillis = resolveTimeoutInMillis(ctx.requestConfig()::apiCallAttemptTimeout, apiCallAttemptTimeout);
        Supplier<SdkClientException> exceptionSupplier = () -> ApiCallAttemptTimeoutException.create(timeoutMillis);

        return TimerUtils.timeAsyncTaskIfNeeded(executeFuture,
                                                timeoutExecutor,
                                                exceptionSupplier,
                                                timeoutMillis);
    }

    /**
     * When an operation has a streaming input, the customer must supply an {@link AsyncRequestBody} to
     * provide the request content in a non-blocking manner. This adapts that interface to the
     * {@link SdkHttpContentPublisher} which the HTTP client SPI expects.
     */
    private static final class SdkHttpContentPublisherAdapter implements SdkHttpContentPublisher {

        private final AsyncRequestBody asyncRequestBody;

        private SdkHttpContentPublisherAdapter(AsyncRequestBody asyncRequestBody) {
            this.asyncRequestBody = asyncRequestBody;
        }

        @Override
        public Optional<Long> contentLength() {
            return asyncRequestBody.contentLength();
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> s) {
            asyncRequestBody.subscribe(s);
        }
    }
}
