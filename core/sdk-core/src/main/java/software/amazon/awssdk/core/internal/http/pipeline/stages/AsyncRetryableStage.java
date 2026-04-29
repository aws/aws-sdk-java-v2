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
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.TransformingAsyncResponseHandler;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.internal.http.pipeline.stages.utils.RetryableStageHelper;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Either;
import software.amazon.awssdk.utils.Logger;

/**
 * Wrapper around the pipeline for a single request to provide retry, clockskew and request throttling functionality.
 */
@SdkInternalApi
public final class AsyncRetryableStage<OutputT> implements RequestPipeline<SdkHttpFullRequest,
    CompletableFuture<Response<OutputT>>> {
    private static final String X_AMZ_RETRY_AFTER_HEADER = "x-amz-retry-after";
    private static final Logger LOG = Logger.loggerFor(AsyncRetryableStage.class);

    private final TransformingAsyncResponseHandler<Response<OutputT>> responseHandler;
    private final RequestPipeline<SdkHttpFullRequest, CompletableFuture<Response<OutputT>>> requestPipeline;
    private final ScheduledExecutorService scheduledExecutor;
    private final HttpClientDependencies dependencies;

    public AsyncRetryableStage(TransformingAsyncResponseHandler<Response<OutputT>> responseHandler,
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
        private final RetryableStageHelper retryableStageHelper;

        private RetryingExecutor(SdkHttpFullRequest request, RequestExecutionContext context) {
            this.originalRequestBody = context.requestProvider();
            this.context = context;
            this.retryableStageHelper = new RetryableStageHelper(request, context, dependencies);
        }

        public CompletableFuture<Response<OutputT>> execute() {
            CompletableFuture<Response<OutputT>> future = new CompletableFuture<>();
            try {
                attemptFirstExecute(future);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
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
            Either<Duration, Duration> backoffDelay = retryableStageHelper.tryRefreshToken(suggestedDelay());

            Optional<Duration> acquireFailureDelay = backoffDelay.right();
            if (acquireFailureDelay.isPresent()) {
                Duration delay = acquireFailureDelay.get();
                retryableStageHelper.logAcquireFailureBackingOff(delay);
                SdkException disallowedException = retryableStageHelper.retryPolicyDisallowedRetryException();
                // Avoid needless scheduling if we won't wait
                if (delay.isZero()) {
                    future.completeExceptionally(disallowedException);
                } else {
                    scheduledExecutor.schedule(() -> future.completeExceptionally(disallowedException),
                                               delay.toMillis(), MILLISECONDS);
                }
                return;
            }
            // We failed the last attempt, but will retry. The response handler wants to know when that happens.
            responseHandler.onError(retryableStageHelper.getLastException());

            // Reset the request provider to the original one before retries, in case it was modified downstream.
            context.requestProvider(originalRequestBody);

            // get() is safe, Either requires left OR right to be present
            Duration successDelay = backoffDelay.left().get();
            retryableStageHelper.logBackingOff(successDelay);
            long totalDelayMillis = successDelay.toMillis();
            scheduledExecutor.schedule(() -> attemptExecute(future), totalDelayMillis, MILLISECONDS);
        }

        private void maybeRetryExecute(CompletableFuture<Response<OutputT>> future, Exception exception) {
            retryableStageHelper.setLastException(exception);
            try {
                maybeAttemptExecute(future);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        }

        private Duration suggestedDelay() {
            if (newRetries2026Enabled(context)) {
                return xAmzRetryAfter(retryableStageHelper.getLastResponse()).orElse(Duration.ZERO);
            }
            // Unlike in the sync RetryableStage, we never used 'Retry-After' for suggested delay in async
            // https://github.com/aws/aws-sdk-java-v2/blob/1483d30d071716ead3dc1fa6571441658013d5c1/core/sdk-core/src/main/java/software/amazon/awssdk/core/internal/http/pipeline/stages/AsyncRetryableStage.java#L137
            return Duration.ZERO;
        }
    }

    /**
     * Returns the suggested backoff delay based on the 'x-amz-retry-after' header value in the response.
     */
    private Optional<Duration> xAmzRetryAfter(SdkHttpResponse response) {
        Optional<String> optionalXAmzRetryAfter = response.firstMatchingHeader(X_AMZ_RETRY_AFTER_HEADER);
        return optionalXAmzRetryAfter.map(xAmzRetryAfter -> {
            try {
                return Duration.ofMillis(Integer.parseInt(xAmzRetryAfter));
            } catch (NumberFormatException e) {
                // Ignore and fallback to returning empty.
                LOG.debug(() -> String.format("Unable to parse header '%s' value '%s' as integer",
                                              X_AMZ_RETRY_AFTER_HEADER, xAmzRetryAfter), e);
                return null;
            }
        });
    }

    private boolean newRetries2026Enabled(RequestExecutionContext executionContext) {
        return executionContext.executionAttributes()
                               .getOptionalAttribute(SdkInternalExecutionAttribute.NEW_RETRIES_2026_ENABLED)
                               .orElse(false);
    }
}
