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

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.SdkStandardLogger;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.NonRetryableException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.internal.InternalCoreExecutionAttribute;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.TransformingAsyncResponseHandler;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.internal.retry.ClockSkewAdjuster;
import software.amazon.awssdk.core.internal.retry.RetryHandler;
import software.amazon.awssdk.core.internal.util.CapacityManager;
import software.amazon.awssdk.core.internal.util.ThrowableUtils;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.utils.CompletableFutureUtils;

/**
 * Wrapper around the pipeline for a single request to provide retry functionality.
 */
@SdkInternalApi
public final class AsyncRetryableStage<OutputT> implements RequestPipeline<SdkHttpFullRequest,
    CompletableFuture<Response<OutputT>>> {

    private static final Logger log = LoggerFactory.getLogger(AsyncRetryableStage.class);

    private final TransformingAsyncResponseHandler<Response<OutputT>> responseHandler;
    private final RequestPipeline<SdkHttpFullRequest, CompletableFuture<Response<OutputT>>> requestPipeline;
    private final ScheduledExecutorService scheduledExecutor;
    private final HttpClientDependencies dependencies;
    private final CapacityManager retryCapacity;
    private final RetryPolicy retryPolicy;

    public AsyncRetryableStage(TransformingAsyncResponseHandler<Response<OutputT>> responseHandler,
                               HttpClientDependencies dependencies,
                               RequestPipeline<SdkHttpFullRequest, CompletableFuture<Response<OutputT>>> requestPipeline) {
        this.responseHandler = responseHandler;
        this.dependencies = dependencies;
        this.scheduledExecutor = dependencies.clientConfiguration().option(SdkClientOption.SCHEDULED_EXECUTOR_SERVICE);
        this.retryPolicy = dependencies.clientConfiguration().option(SdkClientOption.RETRY_POLICY);
        this.retryCapacity = dependencies.retryCapacity();
        this.requestPipeline = requestPipeline;
    }

    @Override
    public CompletableFuture<Response<OutputT>> execute(SdkHttpFullRequest request, RequestExecutionContext context) throws
                                                                                                                     Exception {
        return new RetryExecutor(request, context).execute();
    }

    /**
     * Created for every request to encapsulate mutable state between retries.
     */
    private class RetryExecutor {

        private final SdkHttpFullRequest request;
        private final RequestExecutionContext context;
        private final RetryHandler retryHandler;
        private final AsyncRequestBody originalRequestBody;

        private int requestCount = 0;

        private RetryExecutor(SdkHttpFullRequest request, RequestExecutionContext context) {
            this.request = request;
            this.context = context;
            this.originalRequestBody = context.requestProvider();
            this.retryHandler = new RetryHandler(retryPolicy, retryCapacity);
        }

        public CompletableFuture<Response<OutputT>> execute() throws Exception {
            CompletableFuture<Response<OutputT>> future = new CompletableFuture<>();
            return execute(future);
        }

        public CompletableFuture<Response<OutputT>> execute(CompletableFuture<Response<OutputT>> future) throws Exception {
            beforeExecute();
            CompletableFuture<Response<OutputT>> executeFuture = doExecute();
            executeFuture.whenComplete((resp, err) -> retryIfNeeded(future, resp, err));
            return CompletableFutureUtils.forwardExceptionTo(future, executeFuture);
        }

        private void retryIfNeeded(CompletableFuture<Response<OutputT>> future,
                                   Response<OutputT> resp,
                                   Throwable err) {
            if (future.isDone()) {
                return;
            }

            try {
                if (resp != null) {
                    retryResponseIfNeeded(resp, future);
                } else {
                    if (err instanceof CompletionException) {
                        err = err.getCause();
                    }
                    SdkException sdkException = ThrowableUtils.asSdkException(err);
                    retryErrorIfNeeded(sdkException, future);
                }
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        }

        private void retryResponseIfNeeded(Response<OutputT> resp, CompletableFuture<Response<OutputT>> future) {
            if (resp.isSuccess()) {
                retryHandler.releaseRetryCapacity();
                future.complete(resp);
                return;
            }

            SdkException err = resp.exception();

            ClockSkewAdjuster clockSkewAdjuster = dependencies.clockSkewAdjuster();
            if (clockSkewAdjuster.shouldAdjust(err)) {
                dependencies.updateTimeOffset(clockSkewAdjuster.getAdjustmentInSeconds(resp.httpResponse()));
            }

            if (shouldRetry(resp.httpResponse(), resp.exception())) {
                // We only notify onError if we are retrying the request.
                // Otherwise we rely on the generated code in the in the
                // client class to forward exception to the handler's
                // exceptionOcurred method.
                responseHandler.onError(err);
                retryHandler.setLastRetriedException(err);
                executeRetry(future);
            } else {
                future.completeExceptionally(err);
            }
        }

        private void retryErrorIfNeeded(SdkException err, CompletableFuture<Response<OutputT>> future) {
            if (err instanceof NonRetryableException) {
                future.completeExceptionally(err);
                return;
            }

            if (shouldRetry(null, err)) {
                // We only notify onError if we are retrying the request.
                // Otherwise we rely on the generated code in the in the client
                // class to forward exception to the handler's exceptionOcurred
                // method.
                responseHandler.onError(err);
                retryHandler.setLastRetriedException(err);
                executeRetry(future);
            } else {
                future.completeExceptionally(err);
            }
        }

        private boolean shouldRetry(SdkHttpFullResponse httpResponse, SdkException exception) {
            return retryHandler.shouldRetry(httpResponse, request, context, exception, requestCount);
        }

        private void executeRetry(CompletableFuture<Response<OutputT>> future) {
            Duration delay = retryHandler.computeDelayBeforeNextRetry();

            SdkStandardLogger.REQUEST_LOGGER.debug(() -> "Retryable error detected, will retry in " + delay.toMillis() + "ms,"
                                                         + " attempt number " + requestCount);
            scheduledExecutor.schedule(() -> {
                execute(future);
                return null;
            }, delay.toMillis(), TimeUnit.MILLISECONDS);
        }

        private void beforeExecute() {
            retryHandler.retryCapacityConsumed(false);
            ++requestCount;
        }

        private CompletableFuture<Response<OutputT>> doExecute() throws Exception {
            SdkStandardLogger.REQUEST_LOGGER.debug(() -> (retryHandler.isRetry() ? "Retrying " : "Sending ") +
                                                         "Request: " + request);

            // Before each attempt, Modify the context to use original request body provider
            context.requestProvider(originalRequestBody);

            context.executionAttributes().putAttribute(InternalCoreExecutionAttribute.EXECUTION_ATTEMPT, requestCount);
            return requestPipeline.execute(retryHandler.addRetryInfoHeader(request, requestCount), context);
        }
    }
}
