/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.http.pipeline.stages;

import static software.amazon.awssdk.core.event.SdkProgressPublisher.publishProgress;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.RequestExecutionContext;
import software.amazon.awssdk.core.ResetException;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.SdkBaseException;
import software.amazon.awssdk.core.SdkClientException;
import software.amazon.awssdk.core.SdkStandardLoggers;
import software.amazon.awssdk.core.event.ProgressEventType;
import software.amazon.awssdk.core.event.ProgressListener;
import software.amazon.awssdk.core.http.HttpAsyncClientDependencies;
import software.amazon.awssdk.core.http.HttpClientDependencies;
import software.amazon.awssdk.core.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.retry.RetryHandler;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.RetryUtils;
import software.amazon.awssdk.core.util.CapacityManager;
import software.amazon.awssdk.core.util.ClockSkewUtil;
import software.amazon.awssdk.http.SdkHttpFullRequest;

/**
 * Wrapper around the pipeline for a single request to provide retry functionality.
 */
public class AsyncRetryableStage<OutputT> implements RequestPipeline<SdkHttpFullRequest, CompletableFuture<Response<OutputT>>> {

    private static final Logger log = LoggerFactory.getLogger(AsyncRetryableStage.class);

    private final RequestPipeline<SdkHttpFullRequest, CompletableFuture<Response<OutputT>>> requestPipeline;
    private final ScheduledExecutorService retrySubmitter;
    private final HttpClientDependencies dependencies;
    private final CapacityManager retryCapacity;
    private final RetryPolicy retryPolicy;

    public AsyncRetryableStage(HttpAsyncClientDependencies dependencies,
                               RequestPipeline<SdkHttpFullRequest, CompletableFuture<Response<OutputT>>> requestPipeline) {
        this.dependencies = dependencies;
        this.retrySubmitter = dependencies.asyncClientConfiguration().asyncExecutorService();
        this.retryCapacity = dependencies.retryCapacity();
        this.retryPolicy = dependencies.asyncClientConfiguration().overrideConfiguration().retryPolicy();
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
                throw new ResetException("Failed to reset the request input stream", ex);
            }
        }
    }

    /**
     * Created for every request to encapsulate mutable state between retries.
     */
    private class RetryExecutor {

        private final SdkHttpFullRequest request;
        private final RequestExecutionContext context;
        private final ProgressListener progressListener;
        private final RetryHandler retryHandler;

        private int requestCount = 0;

        private RetryExecutor(SdkHttpFullRequest request, RequestExecutionContext context) {
            this.request = request;
            this.context = context;
            this.progressListener = context.requestConfig().getProgressListener();
            this.retryHandler = new RetryHandler(retryPolicy, retryCapacity);
        }

        public CompletableFuture<Response<OutputT>> execute() throws Exception {
            CompletableFuture<Response<OutputT>> future = new CompletableFuture<>();
            execute(future);
            return future;
        }

        public void execute(CompletableFuture<Response<OutputT>> future) throws Exception {
            beforeExecute();
            doExecute().handle((resp, err) -> handle(future, resp, err));
        }

        private Void handle(CompletableFuture<Response<OutputT>> future,
                            Response<OutputT> resp,
                            Throwable err) {
            try {
                if (resp != null && resp.isSuccess()) {
                    retryHandler.releaseRetryCapacity();
                    future.complete(resp);
                } else if (resp != null) {
                    retryHandler.setLastRetriedException(handleSdkException(resp));
                    executeRetry(future);
                } else {
                    SdkClientException exception = new SdkClientException(err);
                    retryHandler.setLastRetriedException(handleSdkException(Response.fromFailure(exception, null)));
                    executeRetry(future);
                }
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
            return null;
        }

        private void executeRetry(CompletableFuture<Response<OutputT>> future) {
            publishProgress(progressListener, ProgressEventType.CLIENT_REQUEST_RETRY_EVENT);
            final int retriesAttempted = requestCount - 2;
            Duration delay = retryHandler.computeDelayBeforeNextRetry();

            if (log.isDebugEnabled()) {
                log.debug("Retryable error detected, will retry in " + delay + "ms, attempt number: " +
                          retriesAttempted);
            }
            retrySubmitter.schedule(() -> {
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

            request.content().ifPresent(this::markInputStream);

            SdkStandardLoggers.REQUEST_LOGGER.debug(() -> (retryHandler.isRetry() ? "Retrying " : "Sending ") +
                                                          "Request: " + request);

            return requestPipeline.execute(retryHandler.addRetryInfoHeader(request, requestCount), context);
        }

        private SdkBaseException handleSdkException(Response<OutputT> response) {
            SdkBaseException exception = response.getException();
            if (!retryHandler.shouldRetry(response.getHttpResponse(), request, context, exception, requestCount)) {
                throw exception;
            }
            /**
             * Checking for clock skew error again because we don't want to set the global time offset
             * for every service exception.
             */

            if (RetryUtils.isClockSkewError(exception)) {
                int clockSkew = ClockSkewUtil.parseClockSkewOffset(response.getHttpResponse());
                dependencies.updateTimeOffset(clockSkew);
            }
            return exception;
        }

        /**
         * Mark the input stream at the current position to allow a reset on retries.
         */
        private void markInputStream(InputStream originalContent) {
            if (originalContent.markSupported()) {
                originalContent.mark(readLimit());
            }
        }

        /**
         * @return Allowed read limit that we can mark request input stream. If we read past this limit we cannot reset the stream
         * so we cannot retry the request.
         */
        private int readLimit() {
            return context.requestConfig().getRequestClientOptions().getReadLimit();
        }
    }
}
