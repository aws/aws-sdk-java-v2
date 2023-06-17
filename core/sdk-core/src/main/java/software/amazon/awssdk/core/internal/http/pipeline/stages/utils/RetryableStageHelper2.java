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

package software.amazon.awssdk.core.internal.http.pipeline.stages.utils;

import static software.amazon.awssdk.core.internal.InternalCoreExecutionAttribute.EXECUTION_ATTEMPT;
import static software.amazon.awssdk.core.internal.InternalCoreExecutionAttribute.RETRY_TOKEN;
import static software.amazon.awssdk.core.internal.http.pipeline.stages.utils.RetryableStageHelper.LAST_BACKOFF_DELAY_DURATION;
import static software.amazon.awssdk.core.metrics.CoreMetric.RETRY_COUNT;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.SdkStandardLogger;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.stages.AsyncRetryableStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.RetryableStage;
import software.amazon.awssdk.core.internal.retry.ClockSkewAdjuster;
import software.amazon.awssdk.core.internal.retry.RetryPolicyAdapter;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.RetryPolicyContext;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.retries.AdaptiveRetryStrategy;
import software.amazon.awssdk.retries.api.AcquireInitialTokenRequest;
import software.amazon.awssdk.retries.api.AcquireInitialTokenResponse;
import software.amazon.awssdk.retries.api.RecordSuccessRequest;
import software.amazon.awssdk.retries.api.RefreshRetryTokenRequest;
import software.amazon.awssdk.retries.api.RefreshRetryTokenResponse;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.retries.api.RetryToken;
import software.amazon.awssdk.retries.api.TokenAcquisitionFailedException;

/**
 * Contains the logic shared by {@link RetryableStage} and {@link AsyncRetryableStage} when querying and interacting with a
 * {@link RetryStrategy}.
 */
@SdkInternalApi
public final class RetryableStageHelper2 {
    public static final String SDK_RETRY_INFO_HEADER = "amz-sdk-request";
    private final SdkHttpFullRequest request;
    private final RequestExecutionContext context;
    private final RetryPolicy retryPolicy;
    private RetryPolicyAdapter retryPolicyAdapter;
    private final RetryStrategy<?, ?> retryStrategy;
    private final HttpClientDependencies dependencies;
    private final List<String> exceptionMessageHistory = new ArrayList<>();
    private int attemptNumber = 0;
    private SdkHttpResponse lastResponse;
    private SdkException lastException;

    public RetryableStageHelper2(SdkHttpFullRequest request,
                                 RequestExecutionContext context,
                                 HttpClientDependencies dependencies) {
        this.request = request;
        this.context = context;
        this.retryPolicy = dependencies.clientConfiguration().option(SdkClientOption.RETRY_POLICY);
        this.retryStrategy = dependencies.clientConfiguration().option(SdkClientOption.RETRY_STRATEGY);
        this.dependencies = dependencies;
    }

    /**
     * Invoke when starting a request attempt, before querying the retry policy.
     */
    public void startingAttempt() {
        ++attemptNumber;
        context.executionAttributes().putAttribute(EXECUTION_ATTEMPT, attemptNumber);
    }

    /**
     * Invoke when starting the first attempt. This method will acquire the initial token and store it as an execution attribute.
     * This method returns a delay that the caller have to wait before attempting the first request. If this method returns
     * {@link Duration#ZERO} if the calling code does not have to wait. As of today the only strategy that might return a non-zero
     * value is {@link AdaptiveRetryStrategy}.
     */
    public Duration acquireInitialToken() {
        String scope = "GLOBAL";
        AcquireInitialTokenRequest acquireRequest = AcquireInitialTokenRequest.create(scope);
        AcquireInitialTokenResponse acquireResponse = retryStrategy().acquireInitialToken(acquireRequest);
        RetryToken retryToken = acquireResponse.token();
        Duration delay = acquireResponse.delay();
        context.executionAttributes().putAttribute(RETRY_TOKEN, retryToken);
        context.executionAttributes().putAttribute(LAST_BACKOFF_DELAY_DURATION, delay);
        return delay;
    }

    /**
     * Notify the retry strategy that the request attempt succeeded.
     */
    public void recordAttemptSucceeded() {
        RetryToken retryToken = context.executionAttributes().getAttribute(RETRY_TOKEN);
        RecordSuccessRequest recordSuccessRequest = RecordSuccessRequest.create(retryToken);
        retryStrategy().recordSuccess(recordSuccessRequest);
        context.executionContext().metricCollector().reportMetric(RETRY_COUNT, retriesAttemptedSoFar());
    }

    /**
     * Invoked after a failed attempt and before retrying. The returned optional will be non-empty if the client can retry or
     * empty if the retry-strategy disallows the retry. The calling code is expected to wait the delay represented in the duration
     * if present before retrying the request.
     *
     * @param suggestedDelay A suggested delay, presumably coming from the server response. The response when present will be at
     *                       least this amount.
     * @return An optional time to wait. If the value is not present the retry strategy disallowed the retry and the calling code
     * should not retry.
     */
    public Optional<Duration> tryRefreshToken(Duration suggestedDelay) {
        RetryToken retryToken = context.executionAttributes().getAttribute(RETRY_TOKEN);
        RefreshRetryTokenResponse refreshResponse;
        try {
            RefreshRetryTokenRequest refreshRequest = RefreshRetryTokenRequest.builder()
                                                                              .failure(this.lastException)
                                                                              .token(retryToken)
                                                                              .suggestedDelay(suggestedDelay)
                                                                              .build();
            refreshResponse = retryStrategy().refreshRetryToken(refreshRequest);
        } catch (TokenAcquisitionFailedException e) {
            context.executionAttributes().putAttribute(RETRY_TOKEN, e.token());
            return Optional.empty();
        }
        Duration delay = refreshResponse.delay();
        context.executionAttributes().putAttribute(RETRY_TOKEN, refreshResponse.token());
        context.executionAttributes().putAttribute(LAST_BACKOFF_DELAY_DURATION, delay);
        return Optional.of(delay);
    }

    /**
     * Return the exception that should be thrown, because the retry strategy did not allow the request to be retried.
     */
    public SdkException retryPolicyDisallowedRetryException() {
        context.executionContext().metricCollector().reportMetric(RETRY_COUNT, retriesAttemptedSoFar());
        for (int i = 0; i < exceptionMessageHistory.size() - 1; i++) {
            SdkClientException pastException =
                SdkClientException.builder()
                                  .message("Request attempt " + (i + 1) + " failure: " + exceptionMessageHistory.get(i))
                                  .writableStackTrace(false)
                                  .build();
            lastException.addSuppressed(pastException);
        }
        return lastException;
    }

    /**
     * Log a message to the user at the debug level to indicate how long we will wait before retrying the request.
     */
    public void logBackingOff(Duration backoffDelay) {
        SdkStandardLogger.REQUEST_LOGGER.debug(() -> "Retryable error detected. Will retry in " +
                                                     backoffDelay.toMillis() + "ms. Request attempt number " +
                                                     attemptNumber, lastException);
    }

    /**
     * Retrieve the request to send to the service, including any detailed retry information headers.
     */
    public SdkHttpFullRequest requestToSend() {
        return request.toBuilder()
                      .putHeader(SDK_RETRY_INFO_HEADER, "attempt=" + attemptNumber
                                                        + "; max=" + retryStrategy().maxAttempts())
                      .build();
    }

    /**
     * Log a message to the user at the debug level to indicate that we are sending the request to the service.
     */
    public void logSendingRequest() {
        SdkStandardLogger.REQUEST_LOGGER.debug(() -> (isInitialAttempt() ? "Sending" : "Retrying") + " Request: " + request);
    }

    /**
     * Adjust the client-side clock skew if the provided response indicates that there is a large skew between the client and
     * service. This will allow a retried request to be signed with what is likely to be a more accurate time.
     */
    public void adjustClockIfClockSkew(Response<?> response) {
        ClockSkewAdjuster clockSkewAdjuster = dependencies.clockSkewAdjuster();
        if (!response.isSuccess() && clockSkewAdjuster.shouldAdjust(response.exception())) {
            dependencies.updateTimeOffset(clockSkewAdjuster.getAdjustmentInSeconds(response.httpResponse()));
        }
    }

    /**
     * Retrieve the last call failure exception encountered by this execution, updated whenever {@link #setLastException} is
     * invoked.
     */
    public SdkException getLastException() {
        return lastException;
    }

    /**
     * Update the {@link #getLastException()} value for this helper. This will be used to determine whether the request should be
     * retried.
     */
    public void setLastException(Throwable lastException) {
        if (lastException instanceof CompletionException) {
            setLastException(lastException.getCause());
        } else if (lastException instanceof SdkException) {
            this.lastException = (SdkException) lastException;
            exceptionMessageHistory.add(this.lastException.getMessage());
        } else {
            this.lastException = SdkClientException.create("Unable to execute HTTP request: " + lastException.getMessage(),
                                                           lastException);
            exceptionMessageHistory.add(this.lastException.getMessage());
        }
    }

    /**
     * Set the last HTTP response returned by the service. This will be used to determine whether the request should be retried.
     */
    public void setLastResponse(SdkHttpResponse lastResponse) {
        this.lastResponse = lastResponse;
    }

    /**
     * Returns true if this is the first attempt.
     */
    private boolean isInitialAttempt() {
        return attemptNumber == 1;
    }

    /**
     * Retrieve the current attempt number, updated whenever {@link #startingAttempt()} is invoked.
     */
    public int getAttemptNumber() {
        return attemptNumber;
    }

    /**
     * Retrieve the number of retries sent so far in the request execution.
     */
    private int retriesAttemptedSoFar() {
        return Math.max(0, attemptNumber - 1);
    }

    /**
     * Returns the {@link RetryStrategy} to be used by this class. If there's a client configured retry-policy then an adapter to
     * wrap it is returned. This allows this code to be backwards compatible with previously configured retry-policies by the
     * calling code.
     */
    private RetryStrategy<?, ?> retryStrategy() {
        if (retryPolicy != null) {
            if (retryPolicyAdapter == null) {
                retryPolicyAdapter = RetryPolicyAdapter.builder()
                                                       .retryPolicy(this.retryPolicy)
                                                       .retryPolicyContext(retryPolicyContext())
                                                       .build();
            } else {
                retryPolicyAdapter = retryPolicyAdapter.toBuilder()
                                                       .retryPolicyContext(retryPolicyContext())
                                                       .build();
            }
            return retryPolicyAdapter;
        }
        return retryStrategy;
    }

    /**
     * Creates a RetryPolicyContext to be used when the using the retry policy to strategy adapter.
     */
    private RetryPolicyContext retryPolicyContext() {
        return RetryPolicyContext.builder()
                                 .request(request)
                                 .originalRequest(context.originalRequest())
                                 .exception(lastException)
                                 .retriesAttempted(retriesAttemptedSoFar())
                                 .executionAttributes(context.executionAttributes())
                                 .httpStatusCode(lastResponse == null ? null : lastResponse.statusCode())
                                 .build();
    }
}
