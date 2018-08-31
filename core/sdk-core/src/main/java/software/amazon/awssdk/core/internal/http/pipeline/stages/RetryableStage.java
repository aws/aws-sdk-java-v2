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
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.RequestOption;
import software.amazon.awssdk.core.SdkStandardLogger;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.ResetException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.internal.Response;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.InterruptMonitor;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.internal.http.pipeline.RequestToResponsePipeline;
import software.amazon.awssdk.core.internal.retry.RetryHandler;
import software.amazon.awssdk.core.internal.util.CapacityManager;
import software.amazon.awssdk.core.internal.util.ClockSkewUtil;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.RetryUtils;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.utils.Logger;

/**
 * Wrapper around the pipeline for a single request to provide retry functionality.
 */
@SdkInternalApi
public final class RetryableStage<OutputT> implements RequestToResponsePipeline<OutputT> {

    private static final Logger log = Logger.loggerFor(RetryableStage.class);

    private final RequestPipeline<SdkHttpFullRequest, Response<OutputT>> requestPipeline;

    private final HttpClientDependencies dependencies;
    private final CapacityManager retryCapacity;
    private final RetryPolicy retryPolicy;

    public RetryableStage(HttpClientDependencies dependencies,
                          RequestPipeline<SdkHttpFullRequest, Response<OutputT>> requestPipeline) {
        this.dependencies = dependencies;
        this.retryCapacity = dependencies.retryCapacity();
        this.retryPolicy = dependencies.clientConfiguration().option(SdkClientOption.RETRY_POLICY);
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
                } catch (SdkClientException | IOException e) {
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

            SdkStandardLogger.REQUEST_LOGGER.debug(() -> (retryHandler.isRetry() ? "Retrying " : "Sending ") + "Request: " +
                                                         request);

            return requestPipeline.execute(retryHandler.addRetryInfoHeader(request, requestCount), context);
        }

        private SdkException handleUnmarshalledException(Response<OutputT> response) {
            SdkException exception = response.exception();
            if (!retryHandler.shouldRetry(response.httpResponse(), request, context, exception, requestCount)) {
                throw exception;
            }
            /**
             * Checking for clock skew error again because we don't want to set the global time offset
             * for every service exception.
             */
            if (RetryUtils.isClockSkewException(exception)) {
                int clockSkew = ClockSkewUtil.parseClockSkewOffset(response.httpResponse());
                dependencies.updateTimeOffset(clockSkew);
            }
            return exception;
        }

        private SdkException handleThrownException(Exception e) {
            SdkClientException sdkClientException = e instanceof SdkClientException ?
                    (SdkClientException) e : SdkClientException.builder()
                                                               .message("Unable to execute HTTP request: " +
                                                                        e.getMessage())
                                                               .cause(e)
                                                               .build();
            boolean willRetry = retryHandler.shouldRetry(null, request, context, sdkClientException, requestCount);

            log.debug(() -> sdkClientException.getMessage() + (willRetry ? " Request will be retried." : ""), e);

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
            return RequestOption.DEFAULT_STREAM_BUFFER_SIZE;
        }

        /**
         * Sleep for a period of time on failed request to avoid flooding a service with retries.
         */
        private void doPauseBeforeRetry() throws InterruptedException {
            final int retriesAttempted = requestCount - 2;
            Duration delay = retryHandler.computeDelayBeforeNextRetry();

            SdkStandardLogger.REQUEST_LOGGER.debug(() -> "Retryable error detected, will retry in " + delay.toMillis() + "ms,"
                                                         + " attempt number " + retriesAttempted);
            TimeUnit.MILLISECONDS.sleep(delay.toMillis());
        }
    }
}
