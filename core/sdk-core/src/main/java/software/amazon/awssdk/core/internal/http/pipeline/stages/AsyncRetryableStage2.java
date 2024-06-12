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

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.TransformingAsyncResponseHandler;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.internal.http.pipeline.stages.utils.RetryableStageHelper2;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.utils.CompletableFutureUtils;

/**
 * Wrapper around the pipeline for a single request to provide retry, clockskew and request throttling functionality.
 */
@SdkInternalApi
public final class AsyncRetryableStage2<OutputT> implements RequestPipeline<SdkHttpFullRequest,
    CompletableFuture<Response<OutputT>>> {

    private final TransformingAsyncResponseHandler<Response<OutputT>> responseHandler;
    private final RequestPipeline<SdkHttpFullRequest, CompletableFuture<Response<OutputT>>> requestPipeline;
    private final ScheduledExecutorService scheduledExecutor;
    private final HttpClientDependencies dependencies;

    public AsyncRetryableStage2(TransformingAsyncResponseHandler<Response<OutputT>> responseHandler,
                                HttpClientDependencies dependencies,
                                RequestPipeline<SdkHttpFullRequest, CompletableFuture<Response<OutputT>>> requestPipeline) {
        this.responseHandler = responseHandler;
        this.dependencies = dependencies;
        this.scheduledExecutor = dependencies.clientConfiguration().option(SdkClientOption.SCHEDULED_EXECUTOR_SERVICE);
        this.requestPipeline = requestPipeline;
    }

    @Override
    public CompletableFuture<Response<OutputT>> execute(SdkHttpFullRequest request,
                                                        RequestExecutionContext context) throws Exception {
        return new RetryingExecutor(request, context).execute();
    }

    private final class RetryingExecutor {
        private final AsyncRequestBody originalRequestBody;
        private final RequestExecutionContext context;
        private final RetryableStageHelper2 retryableStageHelper;

        private RetryingExecutor(SdkHttpFullRequest request, RequestExecutionContext context) {
            this.originalRequestBody = context.requestProvider();
            this.context = context;
            this.retryableStageHelper = new RetryableStageHelper2(request, context, dependencies);
        }

        public CompletableFuture<Response<OutputT>> execute() {
            CompletableFuture<Response<OutputT>> future = new CompletableFuture<>();
            attemptFirstExecute(future);
            return future;
        }

        public void attemptFirstExecute(CompletableFuture<Response<OutputT>> future) {
            Duration backoffDelay = retryableStageHelper.acquireInitialToken();
            if (backoffDelay.isZero()) {
                attemptExecute(future);
            } else {
                retryableStageHelper.logBackingOff(backoffDelay);
                long totalDelayMillis = backoffDelay.toMillis();
                scheduledExecutor.schedule(() -> attemptExecute(future), totalDelayMillis, MILLISECONDS);
            }
        }

        private void attemptExecute(CompletableFuture<Response<OutputT>> future) {
            CompletableFuture<Response<OutputT>> responseFuture;
            try {
                retryableStageHelper.startingAttempt();
                retryableStageHelper.logSendingRequest();
                responseFuture = requestPipeline.execute(retryableStageHelper.requestToSend(), context);

                // If the result future fails, go ahead and fail the response future.
                CompletableFutureUtils.forwardExceptionTo(future, responseFuture);
            } catch (SdkException | IOException e) {
                maybeRetryExecute(future, e);
                return;
            } catch (Throwable e) {
                future.completeExceptionally(e);
                return;
            }

            responseFuture.whenComplete((response, exception) -> {
                if (exception != null) {
                    if (exception instanceof Exception) {
                        maybeRetryExecute(future, (Exception) exception);
                    } else {
                        future.completeExceptionally(exception);
                    }
                    return;
                }

                retryableStageHelper.setLastResponse(response.httpResponse());
                if (!response.isSuccess()) {
                    retryableStageHelper.adjustClockIfClockSkew(response);
                    maybeRetryExecute(future, response.exception());
                    return;
                }

                retryableStageHelper.recordAttemptSucceeded();
                future.complete(response);
            });
        }

        public void maybeAttemptExecute(CompletableFuture<Response<OutputT>> future) {
            Optional<Duration> delay = retryableStageHelper.tryRefreshToken(Duration.ZERO);
            if (!delay.isPresent()) {
                future.completeExceptionally(retryableStageHelper.retryPolicyDisallowedRetryException());
                return;
            }
            // We failed the last attempt, but will retry. The response handler wants to know when that happens.
            responseHandler.onError(retryableStageHelper.getLastException());

            // Reset the request provider to the original one before retries, in case it was modified downstream.
            context.requestProvider(originalRequestBody);

            Duration backoffDelay = delay.get();
            retryableStageHelper.logBackingOff(backoffDelay);
            long totalDelayMillis = backoffDelay.toMillis();
            scheduledExecutor.schedule(() -> attemptExecute(future), totalDelayMillis, MILLISECONDS);
        }

        private void maybeRetryExecute(CompletableFuture<Response<OutputT>> future, Exception exception) {
            retryableStageHelper.setLastException(exception);
            maybeAttemptExecute(future);
        }
    }
}
