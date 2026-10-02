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

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.internal.http.pipeline.RequestToResponsePipeline;
import software.amazon.awssdk.core.internal.http.pipeline.stages.utils.RetryableStageHelper;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.utils.Either;
import software.amazon.awssdk.utils.Logger;

/**
 * Wrapper around the pipeline for a single request to provide retry, clock-skew and request throttling functionality.
 */
@SdkInternalApi
public final class RetryableStage<OutputT> implements RequestToResponsePipeline<OutputT> {
    private static final String RETRY_AFTER_HEADER = "Retry-After";
    private static final String X_AMZ_RETRY_AFTER_HEADER = "x-amz-retry-after";
    private static final Logger LOG = Logger.loggerFor(RetryableStage.class);

    private final RequestPipeline<SdkHttpFullRequest, Response<OutputT>> requestPipeline;
    private final HttpClientDependencies dependencies;

    public RetryableStage(HttpClientDependencies dependencies,
                          RequestPipeline<SdkHttpFullRequest, Response<OutputT>> requestPipeline) {
        this.dependencies = dependencies;
        this.requestPipeline = requestPipeline;
    }

    @Override
    public Response<OutputT> execute(SdkHttpFullRequest request, RequestExecutionContext context) throws Exception {
        RetryableStageHelper retryableStageHelper = new RetryableStageHelper(request, context, dependencies);
        Duration initialDelay = retryableStageHelper.acquireInitialToken();
        TimeUnit.MILLISECONDS.sleep(initialDelay.toMillis());
        while (true) {
            try {
                retryableStageHelper.startingAttempt();
                Response<OutputT> response = executeRequest(retryableStageHelper, context);
                retryableStageHelper.recordAttemptSucceeded();
                return response;
            } catch (SdkExceptionWithRetryAfterHint | SdkException | IOException e) {
                Throwable throwable = e;
                if (e instanceof SdkExceptionWithRetryAfterHint) {
                    SdkExceptionWithRetryAfterHint wrapper = (SdkExceptionWithRetryAfterHint) e;
                    throwable = wrapper.cause();
                }
                retryableStageHelper.setLastException(throwable);
                Duration suggestedDelay = suggestedDelay(e);
                Either<Duration, Duration> backoffDelay = retryableStageHelper.tryRefreshToken(suggestedDelay);
                Optional<Duration> successDelay = backoffDelay.left();
                if (successDelay.isPresent()) {
                    Duration delay = successDelay.get();
                    retryableStageHelper.logBackingOff(delay);
                    TimeUnit.MILLISECONDS.sleep(delay.toMillis());
                } else {
                    Optional<Duration> failureDelay = backoffDelay.right();
                    if (failureDelay.isPresent()) {
                        Duration delay = failureDelay.get();
                        retryableStageHelper.logAcquireFailureBackingOff(delay);
                        TimeUnit.MILLISECONDS.sleep(delay.toMillis());
                    }
                    throw retryableStageHelper.retryPolicyDisallowedRetryException();
                }
            }
        }
    }

    private Duration suggestedDelay(Exception e) {
        if (e instanceof SdkExceptionWithRetryAfterHint) {
            SdkExceptionWithRetryAfterHint except = (SdkExceptionWithRetryAfterHint) e;
            return except.retryAfter();
        }
        return Duration.ZERO;
    }

    /**
     * Executes the requests and returns the result. If the response is not successful throws the wrapped exception.
     */
    private Response<OutputT> executeRequest(RetryableStageHelper retryableStageHelper,
                                             RequestExecutionContext context) throws Exception {
        retryableStageHelper.logSendingRequest();
        Response<OutputT> response = requestPipeline.execute(retryableStageHelper.requestToSend(), context);
        retryableStageHelper.setLastResponse(response.httpResponse());
        if (!response.isSuccess()) {
            retryableStageHelper.adjustClockIfClockSkew(response);
            throw responseException(response, context);
        }
        return response;
    }

    private RuntimeException responseException(Response<OutputT> response, RequestExecutionContext context) {
        Optional<Duration> optionalRetryAfter;
        if (newRetries2026Enabled(context)) {
            optionalRetryAfter = xAmzRetryAfter(response.httpResponse());
        } else {
            optionalRetryAfter = retryAfter(response.httpResponse());
        }

        if (optionalRetryAfter.isPresent()) {
            return new SdkExceptionWithRetryAfterHint(optionalRetryAfter.get(), response.exception());
        }
        return response.exception();
    }

    /**
     * Returns the suggested backoff delay based on the 'x-amz-retry-after' header value in the response.
     */
    private Optional<Duration> xAmzRetryAfter(SdkHttpFullResponse response) {
        Optional<String> optionalXAmzRetryAfter = response.firstMatchingHeader(X_AMZ_RETRY_AFTER_HEADER);
        return optionalXAmzRetryAfter.map(xAmzRetryAfter -> {
            try {
                return Duration.ofMillis(Integer.parseInt(xAmzRetryAfter));
            } catch (NumberFormatException e) {
                // Ignore and fallback to returning empty.
                logIntParseException(X_AMZ_RETRY_AFTER_HEADER, xAmzRetryAfter, e);
                return null;
            }
        });
    }

    /**
     * Returns the suggested backoff delay based on the 'Retry-After' header value in the response.
     */
    private Optional<Duration> retryAfter(SdkHttpFullResponse response) {
        Optional<String> optionalRetryAfterHeader = response.firstMatchingHeader(RETRY_AFTER_HEADER);
        return optionalRetryAfterHeader.map(retryAfterHeader -> {
            try {
                return Duration.ofSeconds(Integer.parseInt(retryAfterHeader));
            } catch (NumberFormatException e) {
                // Ignore and fallback to returning empty.
                logIntParseException(RETRY_AFTER_HEADER, retryAfterHeader, e);
                return null;
            }
        });
    }

    private boolean newRetries2026Enabled(RequestExecutionContext executionContext) {
        return executionContext.executionAttributes()
                               .getOptionalAttribute(SdkInternalExecutionAttribute.NEW_RETRIES_2026_ENABLED)
                               .orElse(false);
    }

    private static void logIntParseException(String headerName, String headerValue, Throwable t) {
        LOG.debug(() -> String.format("Unable to parse header '%s' value '%s' as integer", headerName, headerValue), t);
    }

    // This probably should go directly into SdkException
    static class SdkExceptionWithRetryAfterHint extends RuntimeException {
        private final SdkException cause;
        private final Duration delay;

        SdkExceptionWithRetryAfterHint(Duration delay, SdkException cause) {
            this.delay = delay;
            this.cause = cause;
        }

        public Duration retryAfter() {
            return delay;
        }

        public SdkException cause() {
            return cause;
        }
    }
}
