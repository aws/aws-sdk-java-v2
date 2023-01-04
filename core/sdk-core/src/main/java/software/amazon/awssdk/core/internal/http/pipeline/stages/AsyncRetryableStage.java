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
import java.util.OptionalDouble;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.TransformingAsyncResponseHandler;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.internal.http.pipeline.stages.utils.RetryableStageHelper;
import software.amazon.awssdk.core.internal.retry.RateLimitingTokenBucket;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.utils.CompletableFutureUtils;

/**
 * Wrapper around the pipeline for a single request to provide retry, clockskew and request throttling functionality.
 */
@SdkInternalApi
public final class AsyncRetryableStage<OutputT> implements RequestPipeline<SdkHttpFullRequest,
    CompletableFuture<Response<OutputT>>> {

    private final TransformingAsyncResponseHandler<Response<OutputT>> responseHandler;
    private final RequestPipeline<SdkHttpFullRequest, CompletableFuture<Response<OutputT>>> requestPipeline;
    private final ScheduledExecutorService scheduledExecutor;
    private final HttpClientDependencies dependencies;
    private final RateLimitingTokenBucket rateLimitingTokenBucket;

    public AsyncRetryableStage(TransformingAsyncResponseHandler<Response<OutputT>> responseHandler,
                               HttpClientDependencies dependencies,
                               RequestPipeline<SdkHttpFullRequest, CompletableFuture<Response<OutputT>>> requestPipeline) {
        this.responseHandler = responseHandler;
        this.dependencies = dependencies;
        this.scheduledExecutor = dependencies.clientConfiguration().option(SdkClientOption.SCHEDULED_EXECUTOR_SERVICE);
        this.rateLimitingTokenBucket = new RateLimitingTokenBucket();
        this.requestPipeline = requestPipeline;
    }

    @SdkTestInternalApi
    public AsyncRetryableStage(TransformingAsyncResponseHandler<Response<OutputT>> responseHandler,
                               HttpClientDependencies dependencies,
                               RequestPipeline<SdkHttpFullRequest, CompletableFuture<Response<OutputT>>> requestPipeline,
                               RateLimitingTokenBucket rateLimitingTokenBucket) {
        this.responseHandler = responseHandler;
        this.dependencies = dependencies;
        this.scheduledExecutor = dependencies.clientConfiguration().option(SdkClientOption.SCHEDULED_EXECUTOR_SERVICE);
        this.requestPipeline = requestPipeline;
        this.rateLimitingTokenBucket = rateLimitingTokenBucket;
    }

    @Override
    public CompletableFuture<Response<OutputT>> execute(SdkHttpFullRequest request,
                                                        RequestExecutionContext context) throws Exception {
        return new RetryingExecutor(request, context).execute();
    }

    private class RetryingExecutor {
        private final AsyncRequestBody originalRequestBody;
        private final RequestExecutionContext context;
        private final RetryableStageHelper retryableStageHelper;

        private RetryingExecutor(SdkHttpFullRequest request, RequestExecutionContext context) {
            this.originalRequestBody = context.requestProvider();
            this.context = context;
            this.retryableStageHelper = new RetryableStageHelper(request, context, rateLimitingTokenBucket, dependencies);
        }

        public CompletableFuture<Response<OutputT>> execute() throws Exception {
            CompletableFuture<Response<OutputT>> future = new CompletableFuture<>();
            maybeAttemptExecute(future);
            return future;
        }

        public void maybeAttemptExecute(CompletableFuture<Response<OutputT>> future) {
            retryableStageHelper.startingAttempt();

            if (!retryableStageHelper.retryPolicyAllowsRetry()) {
                future.completeExceptionally(retryableStageHelper.retryPolicyDisallowedRetryException());
                return;
            }

            if (retryableStageHelper.getAttemptNumber() > 1) {
                // We failed the last attempt, but will retry. The response handler wants to know when that happens.
                responseHandler.onError(retryableStageHelper.getLastException());

                // Reset the request provider to the original one before retries, in case it was modified downstream.
                context.requestProvider(originalRequestBody);
            }

            Duration backoffDelay = retryableStageHelper.getBackoffDelay();

            OptionalDouble tokenAcquireTimeSeconds = retryableStageHelper.getSendTokenNonBlocking();
            if (!tokenAcquireTimeSeconds.isPresent()) {
                String errorMessage = "Unable to acquire a send token immediately without waiting. This indicates that ADAPTIVE "
                                      + "retry mode is enabled, fast fail rate limiting is enabled, and that rate limiting is "
                                      + "engaged because of prior throttled requests. The request will not be executed.";
                future.completeExceptionally(SdkClientException.create(errorMessage));
                return;
            }
            long tokenAcquireTimeMillis = (long) (tokenAcquireTimeSeconds.getAsDouble() * 1000);

            if (!backoffDelay.isZero()) {
                retryableStageHelper.logBackingOff(backoffDelay);
            }

            long totalDelayMillis = backoffDelay.toMillis() + tokenAcquireTimeMillis;

            if (totalDelayMillis > 0) {
                scheduledExecutor.schedule(() -> attemptExecute(future), totalDelayMillis, MILLISECONDS);
            } else {
                attemptExecute(future);
            }
        }

        private void attemptExecute(CompletableFuture<Response<OutputT>> future) {
            CompletableFuture<Response<OutputT>> responseFuture;
            try {
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

                retryableStageHelper.updateClientSendingRateForSuccessResponse();

                retryableStageHelper.attemptSucceeded();
                future.complete(response);
            });
        }

        private void maybeRetryExecute(CompletableFuture<Response<OutputT>> future, Exception exception) {
            retryableStageHelper.setLastException(exception);
            retryableStageHelper.updateClientSendingRateForErrorResponse();
            maybeAttemptExecute(future);
        }
    }
}
