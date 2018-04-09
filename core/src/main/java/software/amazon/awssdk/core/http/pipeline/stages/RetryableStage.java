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

package software.amazon.awssdk.core.http.pipeline.stages;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.core.RequestClientOptions;
import software.amazon.awssdk.core.RequestExecutionContext;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.SdkStandardLoggers;
import software.amazon.awssdk.core.exception.ResetException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.http.HttpClientDependencies;
import software.amazon.awssdk.core.http.InterruptMonitor;
import software.amazon.awssdk.core.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.http.pipeline.RequestToResponsePipeline;
import software.amazon.awssdk.core.retry.RetryHandler;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.RetryUtils;
import software.amazon.awssdk.core.util.CapacityManager;
import software.amazon.awssdk.core.util.ClockSkewUtil;
import software.amazon.awssdk.http.SdkHttpFullRequest;

/**
 * Wrapper around the pipeline for a single request to provide retry functionality.
 */
public class RetryableStage<OutputT> implements RequestToResponsePipeline<OutputT> {

    private static final Logger log = LoggerFactory.getLogger(RetryableStage.class);

    private final RequestPipeline<SdkHttpFullRequest, Response<OutputT>> requestPipeline;

    private final HttpClientDependencies dependencies;
    private final CapacityManager retryCapacity;
    private final RetryPolicy retryPolicy;

    public RetryableStage(HttpClientDependencies dependencies,
                          RequestPipeline<SdkHttpFullRequest, Response<OutputT>> requestPipeline) {
        this.dependencies = dependencies;
        this.retryCapacity = dependencies.retryCapacity();
        this.retryPolicy = dependencies.clientConfiguration().overrideConfiguration().retryPolicy();
        this.requestPipeline = requestPipeline;
    }

    public Response<OutputT> execute(SdkHttpFullRequest request, RequestExecutionContext context) throws Exception {
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
        private final RetryHandler retryHandler;

        private int requestCount = 0;

        private RetryExecutor(SdkHttpFullRequest request, RequestExecutionContext context) {
            this.request = request;
            this.context = context;
            this.retryHandler = new RetryHandler(retryPolicy, retryCapacity);
        }

        public Response<OutputT> execute() throws Exception {
            while (true) {
                try {
                    beforeExecute();
                    Response<OutputT> response = doExecute();
                    if (response.isSuccess()) {
                        retryHandler.releaseRetryCapacity();
                        return response;
                    } else {
                        retryHandler.setLastRetriedException(handleUnmarshalledException(response));
                    }
                } catch (SdkServiceException e) {
                    // TODO This can be cleaned up a bit if we have separate hierarchies for service and client exceptions
                    // as we can just catch the client exception below.
                    throw e;
                } catch (SdkException | IOException e) {
                    retryHandler.setLastRetriedException(handleThrownException(e));
                }
            }
        }

        private void beforeExecute() throws InterruptedException {
            retryHandler.retryCapacityConsumed(false);
            InterruptMonitor.checkInterrupted();
            ++requestCount;
        }

        private Response<OutputT> doExecute() throws Exception {
            if (retryHandler.isRetry()) {
                request.content().ifPresent(RetryableStage::resetRequestInputStream);
                doPauseBeforeRetry();
            }

            request.content().ifPresent(this::markInputStream);

            SdkStandardLoggers.REQUEST_LOGGER.debug(() -> (retryHandler.isRetry() ? "Retrying " : "Sending ") + "Request: " +
                                                          request);

            return requestPipeline.execute(retryHandler.addRetryInfoHeader(request, requestCount), context);
        }

        private SdkException handleUnmarshalledException(Response<OutputT> response) {
            SdkException exception = response.getException();
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

        private SdkException handleThrownException(Exception e) {
            SdkClientException sdkClientException = e instanceof SdkClientException ?
                    (SdkClientException) e : new SdkClientException("Unable to execute HTTP request: " + e.getMessage(), e);
            boolean willRetry = retryHandler.shouldRetry(null, request, context, sdkClientException, requestCount);

            if (log.isDebugEnabled()) {
                log.debug(sdkClientException.getMessage() + (willRetry ? " Request will be retried." : ""), e);
            }

            if (!willRetry) {
                throw sdkClientException;
            }

            return sdkClientException;
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
        @ReviewBeforeRelease("Do we still want to make read limit user-configurable as in V1?")
        private int readLimit() {
            return RequestClientOptions.DEFAULT_STREAM_BUFFER_SIZE;
        }

        /**
         * Sleep for a period of time on failed request to avoid flooding a service with retries.
         */
        private void doPauseBeforeRetry() throws InterruptedException {
            final int retriesAttempted = requestCount - 2;
            Duration delay = retryHandler.computeDelayBeforeNextRetry();

            if (log.isDebugEnabled()) {
                log.debug("Retriable error detected, " + "will retry in " + delay + "ms, attempt number: " + retriesAttempted);
            }
            TimeUnit.MILLISECONDS.sleep(delay.toMillis());
        }
    }
}
