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

package software.amazon.awssdk.core.internal.http.pipeline.stages;

import static software.amazon.awssdk.core.internal.http.timers.TimerUtils.resolveTimeoutInMillis;
import static software.amazon.awssdk.http.Header.CONTENT_LENGTH;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkStandardLogger;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.ApiCallAttemptTimeoutException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.Response;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.TransformingAsyncResponseHandler;
import software.amazon.awssdk.core.internal.http.async.SimpleHttpContentPublisher;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.internal.http.timers.TimeoutTracker;
import software.amazon.awssdk.core.internal.http.timers.TimerUtils;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
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
    private final TransformingAsyncResponseHandler<OutputT> responseHandler;
    private final TransformingAsyncResponseHandler<? extends SdkException> errorResponseHandler;
    private final Executor futureCompletionExecutor;
    private final ScheduledExecutorService timeoutExecutor;
    private final Duration apiCallAttemptTimeout;

    public MakeAsyncHttpRequestStage(TransformingAsyncResponseHandler<OutputT> responseHandler,
                                     TransformingAsyncResponseHandler<? extends SdkException> errorResponseHandler,
                                     HttpClientDependencies dependencies) {
        this.responseHandler = responseHandler;
        this.errorResponseHandler = errorResponseHandler;
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

    private CompletableFuture<Response<OutputT>> executeHttpRequest(SdkHttpFullRequest request,
                                                                    RequestExecutionContext context) {

        CompletableFuture<OutputT> preparedTransformFuture = responseHandler.prepare();
        CompletableFuture<? extends SdkException> preparedErrorTransformFuture = errorResponseHandler == null ? null :
                                                                                 errorResponseHandler.prepare();
        CompletableFuture<Response<OutputT>> responseFuture = new CompletableFuture<>();

        //FIXME(dongie): We need to be careful to only call responseHandler.prepare() exactly once per execute() call
        //because it calls prepare() under the hood and we guarantee that we call that once per execution. It would be good
        //to find a way to prevent multiple calls to prepare() within a single execution to only call prepare() once.
        ResponseHandler handler = new ResponseHandler(responseFuture, preparedTransformFuture, preparedErrorTransformFuture);

        CompletableFuture<Response<OutputT>> preparedWrapperTransformFuture = handler.prepare();

        SdkHttpContentPublisher requestProvider = context.requestProvider() == null
                                                  ? new SimpleHttpContentPublisher(request)
                                                  : new SdkHttpContentPublisherAdapter(context.requestProvider());
        // Set content length if it hasn't been set already.
        SdkHttpFullRequest requestWithContentLength = getRequestWithContentLength(request, requestProvider);

        AsyncExecuteRequest executeRequest = AsyncExecuteRequest.builder()
                                                                .request(requestWithContentLength)
                                                                .requestContentPublisher(requestProvider)
                                                                .responseHandler(handler)
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
        preparedWrapperTransformFuture.whenCompleteAsync((r, t) -> {
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
        return TimerUtils.timeAsyncTaskIfNeeded(executeFuture,
                                                timeoutExecutor,
                                                ApiCallAttemptTimeoutException.create(timeoutMillis),
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

    /**
     * Detects whether the response succeeded or failed and delegates to appropriate response handler.
     */
    private class ResponseHandler implements TransformingAsyncResponseHandler<Response<OutputT>> {
        private final CompletableFuture<Response<OutputT>> responseFuture;
        private final CompletableFuture<OutputT> transformFuture;
        private final CompletableFuture<? extends SdkException> errorTransformFuture;
        private CompletableFuture<SdkHttpResponse> headersFuture;
        private volatile SdkHttpFullResponse response;

        /**
         * @param responseFuture the response future to be returned from
         * {@link MakeAsyncHttpRequestStage#executeHttpRequest(SdkHttpFullRequest, RequestExecutionContext)}
         * @param transformFuture the transformFuture returned from {@link MakeAsyncHttpRequestStage#responseHandler#prepare()}
         * @param errorTransformFuture the error transform future returned from
         * {@link MakeAsyncHttpRequestStage#errorResponseHandler#prepare()}
         */
        ResponseHandler(CompletableFuture<Response<OutputT>> responseFuture,
                        CompletableFuture<OutputT> transformFuture,
                        CompletableFuture<? extends SdkException> errorTransformFuture) {
            this.responseFuture = responseFuture;
            this.transformFuture = transformFuture;
            this.errorTransformFuture = errorTransformFuture;
        }

        @Override
        public void onHeaders(SdkHttpResponse response) {
            headersFuture.complete(response);
            if (response.isSuccessful()) {
                SdkStandardLogger.REQUEST_LOGGER.debug(() -> "Received successful response: " + response.statusCode());
                responseHandler.onHeaders(response);
            } else {
                SdkStandardLogger.REQUEST_LOGGER.debug(() -> "Received error response: " + response.statusCode());
                errorResponseHandler.onHeaders(response);
            }
            this.response = toFullResponse(response);
        }

        @Override
        public void onError(Throwable error) {
            // Note: We don't notify the wrapped handlers' onError here because
            // we need context about retries; we only want to notify onError if
            // we know that the request will be retried. Let the
            // AsyncRetryableStage handle it.
            responseFuture.completeExceptionally(error);
            headersFuture.completeExceptionally(error);
        }

        @Override
        public void onStream(Publisher<ByteBuffer> publisher) {
            if (response.isSuccessful()) {
                responseHandler.onStream(publisher);
            } else {
                errorResponseHandler.onStream(publisher);
            }
        }

        @Override
        public CompletableFuture<Response<OutputT>> prepare() {
            headersFuture = new CompletableFuture<>();
            return headersFuture.thenCompose(headers -> {
                if (headers.isSuccessful()) {
                    return transformFuture.thenApply(r -> Response.fromSuccess(r, response));
                }

                if (errorTransformFuture != null) {
                    return errorTransformFuture.thenApply(e -> Response.fromFailure(e, response));
                }
                return CompletableFuture.completedFuture(Response.fromFailure(null, response));
            });
        }
    }

    private static SdkHttpFullResponse toFullResponse(SdkHttpResponse response) {
        SdkHttpFullResponse.Builder builder = SdkHttpFullResponse.builder()
                                                                 .statusCode(response.statusCode())
                                                                 .headers(response.headers());
        response.statusText().ifPresent(builder::statusText);
        return builder.build();
    }
}
