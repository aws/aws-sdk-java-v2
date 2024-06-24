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
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.internal.http.pipeline.RequestToResponsePipeline;
import software.amazon.awssdk.core.internal.http.pipeline.stages.utils.RetryableStageHelper2;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;

/**
 * Wrapper around the pipeline for a single request to provide retry, clock-skew and request throttling functionality.
 */
@SdkInternalApi
public final class RetryableStage2<OutputT> implements RequestToResponsePipeline<OutputT> {
    private static final String RETRY_AFTER_HEADER = "Retry-After";
    private final RequestPipeline<SdkHttpFullRequest, Response<OutputT>> requestPipeline;
    private final HttpClientDependencies dependencies;

    public RetryableStage2(HttpClientDependencies dependencies,
                           RequestPipeline<SdkHttpFullRequest, Response<OutputT>> requestPipeline) {
        this.dependencies = dependencies;
        this.requestPipeline = requestPipeline;
    }

    @Override
    public Response<OutputT> execute(SdkHttpFullRequest request, RequestExecutionContext context) throws Exception {
        RetryableStageHelper2 retryableStageHelper = new RetryableStageHelper2(request, context, dependencies);
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
                Optional<Duration> backoffDelay = retryableStageHelper.tryRefreshToken(suggestedDelay);
                if (backoffDelay.isPresent()) {
                    Duration delay = backoffDelay.get();
                    retryableStageHelper.logBackingOff(delay);
                    TimeUnit.MILLISECONDS.sleep(delay.toMillis());
                } else {
                    throw retryableStageHelper.retryPolicyDisallowedRetryException();
                }
            }
        }
    }

    private Duration suggestedDelay(Exception e) {
        if (e instanceof SdkExceptionWithRetryAfterHint) {
            SdkExceptionWithRetryAfterHint except = (SdkExceptionWithRetryAfterHint) e;
            return Duration.ofSeconds(except.retryAfter());
        }
        return Duration.ZERO;
    }

    /**
     * Executes the requests and returns the result. If the response is not successful throws the wrapped exception.
     */
    private Response<OutputT> executeRequest(RetryableStageHelper2 retryableStageHelper,
                                             RequestExecutionContext context) throws Exception {
        retryableStageHelper.logSendingRequest();
        Response<OutputT> response = requestPipeline.execute(retryableStageHelper.requestToSend(), context);
        retryableStageHelper.setLastResponse(response.httpResponse());
        if (!response.isSuccess()) {
            retryableStageHelper.adjustClockIfClockSkew(response);
            throw responseException(response);
        }
        return response;
    }

    private RuntimeException responseException(Response<OutputT> response) {
        Optional<Integer> optionalRetryAfter = retryAfter(response.httpResponse());
        if (optionalRetryAfter.isPresent()) {
            return new SdkExceptionWithRetryAfterHint(optionalRetryAfter.get(), response.exception());
        }
        return response.exception();
    }

    private Optional<Integer> retryAfter(SdkHttpFullResponse response) {
        Optional<String> optionalRetryAfterHeader = response.firstMatchingHeader(RETRY_AFTER_HEADER);
        if (optionalRetryAfterHeader.isPresent()) {
            String retryAfterHeader = optionalRetryAfterHeader.get();
            try {
                return Optional.of(Integer.parseInt(retryAfterHeader));
            } catch (NumberFormatException e) {
                // Ignore and fallback to returning empty.
            }
        }
        return Optional.empty();
    }

    // This probably should go directly into SdkException
    static class SdkExceptionWithRetryAfterHint extends RuntimeException {
        private final SdkException cause;
        private final int seconds;

        SdkExceptionWithRetryAfterHint(int seconds, SdkException cause) {
            this.seconds = seconds;
            this.cause = cause;
        }

        public int retryAfter() {
            return seconds;
        }

        public SdkException cause() {
            return cause;
        }

        public int seconds() {
            return seconds;
        }
    }
}
