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

package software.amazon.awssdk.http.pipeline.stages;

import static java.util.Collections.singletonList;
import static software.amazon.awssdk.event.SdkProgressPublisher.publishProgress;
import static software.amazon.awssdk.http.AmazonHttpClient.THROTTLED_RETRY_COST;
import static software.amazon.awssdk.http.pipeline.stages.RetryableStage.HEADER_SDK_RETRY_INFO;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import software.amazon.awssdk.RequestExecutionContext;
import software.amazon.awssdk.ResetException;
import software.amazon.awssdk.Response;
import software.amazon.awssdk.SdkBaseException;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.event.ProgressEventType;
import software.amazon.awssdk.event.ProgressListener;
import software.amazon.awssdk.handlers.AwsHandlerKeys;
import software.amazon.awssdk.http.AmazonHttpClient;
import software.amazon.awssdk.http.HttpClientDependencies;
import software.amazon.awssdk.http.HttpResponse;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.pipeline.RequestPipeline;
import software.amazon.awssdk.metrics.spi.AwsRequestMetrics;
import software.amazon.awssdk.retry.RetryUtils;
import software.amazon.awssdk.retry.v2.RetryPolicy;
import software.amazon.awssdk.retry.v2.RetryPolicyContext;
import software.amazon.awssdk.util.CapacityManager;
import software.amazon.awssdk.util.DateUtils;

/**
 * Wrapper around the pipeline for a single request to provide retry functionality.
 */
public class AsyncRetryableStage<OutputT> implements RequestPipeline<SdkHttpFullRequest, CompletableFuture<Response<OutputT>>> {

    private static final Log log = LogFactory.getLog(AsyncRetryableStage.class);

    private final RequestPipeline<SdkHttpFullRequest, CompletableFuture<Response<OutputT>>> requestPipeline;
    private final ScheduledExecutorService retrySubmitter;
    private final HttpClientDependencies dependencies;
    private final CapacityManager retryCapacity;
    private final RetryPolicy retryPolicy;

    public AsyncRetryableStage(HttpClientDependencies dependencies,
                               RequestPipeline<SdkHttpFullRequest, CompletableFuture<Response<OutputT>>> requestPipeline) {
        this.dependencies = dependencies;
        this.retrySubmitter = dependencies.executorService();
        this.retryCapacity = dependencies.retryCapacity();
        this.retryPolicy = dependencies.retryPolicy();
        this.requestPipeline = requestPipeline;
    }

    public CompletableFuture<Response<OutputT>> execute(SdkHttpFullRequest request, RequestExecutionContext context) throws
                                                                                                                     Exception {
        // add the service endpoint to the logs. You can infer service name from service endpoint
        context.awsRequestMetrics()
               .addPropertyWith(AwsRequestMetrics.Field.RequestType, context.requestConfig().getRequestType())
               .addPropertyWith(AwsRequestMetrics.Field.ServiceName, request.handlerContext(AwsHandlerKeys.SERVICE_NAME))
               .addPropertyWith(AwsRequestMetrics.Field.ServiceEndpoint, request.getEndpoint());
        return new RetryExecutor(request, context).execute();
    }

    /**
     * Reset the input stream of the request before a retry.
     *
     * @throws ResetException If Input Stream can't be reset which means the request can't be retried.
     */
    private static void resetRequestInputStream(InputStream inputStream) throws ResetException {
        if (inputStream != null && inputStream.markSupported()) {
            try {
                inputStream.reset();
            } catch (IOException ex) {
                throw new ResetException("Failed to reset the request input stream", ex);
            }
        }
    }

    /**
     * Returns the difference between the client's clock time and the service clock time in unit
     * of seconds.
     */
    private static int parseClockSkewOffset(HttpResponse httpResponse) {
        Optional<String> dateHeader = Optional.ofNullable(httpResponse.getHeader("Date"));
        try {
            Date serverDate = dateHeader
                    .filter(h -> !h.isEmpty())
                    .map(DateUtils::parseRfc822Date)
                    .orElseThrow(() -> new RuntimeException(
                            "Unable to parse clock skew offset from response. Server Date header missing"));
            long diff = System.currentTimeMillis() - serverDate.getTime();
            return (int) (diff / 1000);
        } catch (RuntimeException e) {
            log.warn("Unable to parse clock skew offset from response: " + dateHeader.orElse(""), e);
            return 0;
        }
    }

    /**
     * Created for every request to encapsulate mutable state between retries.
     */
    private class RetryExecutor {

        private final SdkHttpFullRequest request;
        private final RequestExecutionContext context;
        private final ProgressListener progressListener;
        private final AwsRequestMetrics awsRequestMetrics;

        private Optional<SdkBaseException> retriedException;
        private RetryPolicyContext retryPolicyContext;
        private int requestCount;
        private long lastBackoffDelay;
        private boolean retryCapacityConsumed;

        private RetryExecutor(SdkHttpFullRequest request, RequestExecutionContext context) {
            this.request = request;
            this.context = context;
            this.progressListener = context.requestConfig().getProgressListener();
            this.awsRequestMetrics = context.awsRequestMetrics();
            this.retriedException = Optional.empty();
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
                    releaseRetryCapacity();
                    future.complete(resp);
                } else if (resp != null) {
                    setRetriedException(handleSdkException(resp));
                    executeRetry(future);
                } else if (err instanceof IOException) {
                    setRetriedException(handleIoException((IOException) err));
                    executeRetry(future);
                } else {
                    future.completeExceptionally(err);
                }
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
            return null;
        }

        private void executeRetry(CompletableFuture<Response<OutputT>> future) {
            publishProgress(progressListener, ProgressEventType.CLIENT_REQUEST_RETRY_EVENT);
            awsRequestMetrics.startEvent(AwsRequestMetrics.Field.RetryPauseTime);
            final int retriesAttempted = requestCount - 2;
            long delay = retryPolicy.computeDelayBeforeNextRetry(this.retryPolicyContext);
            lastBackoffDelay = delay;

            if (log.isDebugEnabled()) {
                log.debug("Retriable error detected, will retry in " + delay + "ms, attempt number: " +
                          retriesAttempted);
            }
            retrySubmitter.schedule(() -> {
                awsRequestMetrics.endEvent(AwsRequestMetrics.Field.RetryPauseTime);
                execute(future);
                return null;
            }, delay, TimeUnit.MILLISECONDS);
        }

        /**
         * If this was a successful retry attempt we'll release the full retry capacity that the attempt originally consumed.  If
         * this was a successful initial request we release a lesser amount.
         */
        private void releaseRetryCapacity() {
            if (isRetry() && retryCapacityConsumed) {
                retryCapacity.release(THROTTLED_RETRY_COST);
            } else {
                retryCapacity.release();
            }
        }

        private void beforeExecute() {
            retryCapacityConsumed = false;
            context.awsRequestMetrics().setCounter(AwsRequestMetrics.Field.RequestCount, ++requestCount);
        }

        private CompletableFuture<Response<OutputT>> doExecute() throws Exception {
            if (isRetry()) {
                resetRequestInputStream(request.getContent());
            }

            markInputStream(request.getContent());

            if (AmazonHttpClient.REQUEST_LOG.isDebugEnabled()) {
                AmazonHttpClient.REQUEST_LOG
                        .debug((isRetry() ? "Retrying " : "Sending ") + "Request: " + request);
            }
            return requestPipeline.execute(addRetryInfoHeader(request), context);
        }

        private boolean isRetry() {
            return retriedException.isPresent();
        }

        private void setRetriedException(SdkBaseException e) {
            this.retriedException = Optional.of(e);
        }

        private SdkBaseException handleSdkException(Response<OutputT> response) {
            SdkBaseException exception = response.getException();
            if (!shouldRetry(response.getHttpResponse(), exception)) {
                throw exception;
            }
            /**
             * Checking for clock skew error again because we don't want to set the global time offset
             * for every service exception.
             */
            if (RetryUtils.isClockSkewError(exception)) {
                int clockSkew = parseClockSkewOffset(response.getHttpResponse());
                dependencies.updateTimeOffset(clockSkew);
            }
            return exception;
        }

        private SdkBaseException handleIoException(IOException ioe) {
            SdkClientException sdkClientException = new SdkClientException(
                    "Unable to execute HTTP request: " + ioe.getMessage(), ioe);
            boolean willRetry = shouldRetry(null, sdkClientException);
            if (log.isDebugEnabled()) {
                log.debug(sdkClientException.getMessage() + (willRetry ? " Request will be retried." : ""), ioe);
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
            if (originalContent != null && originalContent.markSupported()) {
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

        /**
         * Returns true if a failed request should be retried.
         *
         * @param exception The client/service exception from the failed request.
         * @return True if the failed request should be retried.
         */
        private boolean shouldRetry(HttpResponse httpResponse,
                                    SdkBaseException exception) {
            final int retriesAttempted = requestCount - 1;

            // Do not use retry capacity for throttling exceptions
            if (!RetryUtils.isThrottlingException(exception)) {
                // See if we have enough available retry capacity to be able to execute this retry attempt.
                if (!retryCapacity.acquire(THROTTLED_RETRY_COST)) {
                    awsRequestMetrics.incrementCounter(AwsRequestMetrics.Field.ThrottledRetryCount);
                    return false;
                }
                this.retryCapacityConsumed = true;
            }

            this.retryPolicyContext = RetryPolicyContext.builder()
                                                        .request(request)
                                                        .originalRequest(context.requestConfig().getOriginalRequest())
                                                        .exception(exception)
                                                        .retriesAttempted(retriesAttempted)
                                                        .httpStatusCode(
                                                                httpResponse == null ? null : httpResponse.getStatusCode())
                                                        .build();
            // Finally, pass all the context information to the RetryCondition and let it decide whether it should be retried.
            if (!retryPolicy.shouldRetry(retryPolicyContext)) {
                // If the retry policy fails we immediately return consumed capacity to the pool.
                if (retryCapacityConsumed) {
                    retryCapacity.release(THROTTLED_RETRY_COST);
                }
                return false;
            }

            return true;
        }

        /**
         * Add the {@value RetryableStage#HEADER_SDK_RETRY_INFO} header to the request. Contains metadata about request count,
         * backoff, and retry capacity.
         *
         * @return Request with retry info header added.
         */
        private SdkHttpFullRequest addRetryInfoHeader(SdkHttpFullRequest request) throws Exception {
            int availableRetryCapacity = retryCapacity.availableCapacity();
            return request.toBuilder()
                          .header(HEADER_SDK_RETRY_INFO,
                                  singletonList(String.format("%s/%s/%s",
                                                              requestCount - 1,
                                                              lastBackoffDelay,
                                                              availableRetryCapacity >= 0 ? availableRetryCapacity : "")))
                          .build();
        }

    }
}
