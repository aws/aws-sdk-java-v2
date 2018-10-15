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

package software.amazon.awssdk.core.internal.http.pipeline.stages;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkStandardLogger;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.NonRetryableException;
import software.amazon.awssdk.core.exception.ResetException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.internal.Response;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.internal.retry.RetryHandler;
import software.amazon.awssdk.core.internal.util.CapacityManager;
import software.amazon.awssdk.core.internal.util.ClockSkewUtil;
import software.amazon.awssdk.core.internal.util.ThrowableUtils;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.RetryUtils;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Wrapper around the pipeline for a single request to provide retry functionality.
 */
@SdkInternalApi
public final class AsyncRetryableStage<OutputT> implements RequestPipeline<SdkHttpFullRequest,
    CompletableFuture<Response<OutputT>>> {

    private static final Logger log = LoggerFactory.getLogger(AsyncRetryableStage.class);

    private final RequestPipeline<SdkHttpFullRequest, CompletableFuture<Response<OutputT>>> requestPipeline;
    private final ScheduledExecutorService scheduledExecutor;
    private final HttpClientDependencies dependencies;
    private final CapacityManager retryCapacity;
    private final RetryPolicy retryPolicy;

    public AsyncRetryableStage(HttpClientDependencies dependencies,
                               RequestPipeline<SdkHttpFullRequest, CompletableFuture<Response<OutputT>>> requestPipeline) {
        this.dependencies = dependencies;
        this.scheduledExecutor = dependencies.clientConfiguration().option(SdkClientOption.SCHEDULED_EXECUTOR_SERVICE);
        this.retryPolicy = dependencies.clientConfiguration().option(SdkClientOption.RETRY_POLICY);
        this.retryCapacity = dependencies.retryCapacity();
        this.requestPipeline = requestPipeline;
    }

    public CompletableFuture<Response<OutputT>> execute(SdkHttpFullRequest request, RequestExecutionContext context) throws
                                                                                                                     Exception {
        return new RetryExecutor(request, context).execute();
    }

    /**
     * Reset the input stream of the request before a retry.
     *
     * @throws ResetException If Input Stream can't be reset which means the request can't be retried.
     */
    private static void resetRequestInputStream(InputStream inputStream) throws ResetException {
        if (inputStream.markSupported()) {
            try {
                inputStream.reset();
            } catch (IOException ex) {
                throw ResetException.builder()
                                    .message("Failed to reset the request input stream")
                                    .cause(ex)
                                    .build();
            }
        }
    }

    /**
     * Created for every request to encapsulate mutable state between retries.
     */
    private class RetryExecutor {

        private final SdkHttpFullRequest request;
        private final RequestExecutionContext context;
        private final RetryHandler retryHandler;

        private int requestCount = 0;

        private RetryExecutor(SdkHttpFullRequest request, RequestExecutionContext context) {
            this.request = request;
            this.context = context;
            this.retryHandler = new RetryHandler(retryPolicy, retryCapacity);
        }

        public CompletableFuture<Response<OutputT>> execute() throws Exception {
            CompletableFuture<Response<OutputT>> future = new CompletableFuture<>();
            return execute(future);
        }

        public CompletableFuture<Response<OutputT>> execute(CompletableFuture<Response<OutputT>> future) throws Exception {
            beforeExecute();
            doExecute().whenComplete((resp, err) -> retryIfNeeded(future, resp, err));
            return future;
        }

        private void retryIfNeeded(CompletableFuture<Response<OutputT>> future,
                                   Response<OutputT> resp,
                                   Throwable err) {
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
            } else {
                SdkException err = resp.exception();

                if (RetryUtils.isClockSkewException(err)) {
                    int clockSkew = ClockSkewUtil.parseClockSkewOffset(resp.httpResponse());
                    dependencies.updateTimeOffset(clockSkew);
                }

                if (shouldRetry(resp.httpResponse(), resp.exception())) {
                    retryHandler.setLastRetriedException(err);
                    executeRetry(future);
                } else {
                    future.completeExceptionally(err);
                }
            }
        }

        private void retryErrorIfNeeded(SdkException err, CompletableFuture<Response<OutputT>> future) {
            if (err instanceof NonRetryableException) {
                future.completeExceptionally(err);
                return;
            }

            if (shouldRetry(null, err)) {
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
            final int retriesAttempted = requestCount - 2;
            Duration delay = retryHandler.computeDelayBeforeNextRetry();

            SdkStandardLogger.REQUEST_LOGGER.debug(() -> "Retryable error detected, will retry in " + delay.toMillis() + "ms,"
                                                         + " attempt number " + retriesAttempted);
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
            if (retryHandler.isRetry()) {
                request.content().ifPresent(AsyncRetryableStage::resetRequestInputStream);
            }

            request.content().ifPresent(IoUtils::markStreamWithMaxReadLimit);

            SdkStandardLogger.REQUEST_LOGGER.debug(() -> (retryHandler.isRetry() ? "Retrying " : "Sending ") +
                                                         "Request: " + request);

            return requestPipeline.execute(retryHandler.addRetryInfoHeader(request, requestCount), context);
        }
    }
}