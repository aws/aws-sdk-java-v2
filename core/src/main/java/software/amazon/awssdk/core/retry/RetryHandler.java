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

package software.amazon.awssdk.core.retry;

import static java.util.Collections.singletonList;

import java.time.Duration;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.RequestExecutionContext;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.http.HttpResponse;
import software.amazon.awssdk.core.util.CapacityManager;
import software.amazon.awssdk.http.SdkHttpFullRequest;

@SdkInternalApi
public class RetryHandler {

    public static final String HEADER_SDK_RETRY_INFO = "amz-sdk-retry";

    private final RetryPolicy retryPolicy;
    private final CapacityManager retryCapacity;

    private Duration lastBackoffDelay = Duration.ZERO;
    private boolean retryCapacityConsumed;
    private RetryPolicyContext retryPolicyContext;
    private Optional<SdkException> lastRetriedException = Optional.empty();

    public RetryHandler(RetryPolicy retryPolicy,
                        CapacityManager retryCapacity) {
        this.retryPolicy = retryPolicy;
        this.retryCapacity = retryCapacity;
    }

    public boolean shouldRetry(HttpResponse httpResponse,
                               SdkHttpFullRequest request,
                               RequestExecutionContext context,
                               SdkException exception,
                               int requestCount) {

        final int retriesAttempted = requestCount - 1;

        // Do not use retry capacity for throttling exceptions
        if (!RetryUtils.isThrottlingException(exception)) {
            // See if we have enough available retry capacity to be able to execute this retry attempt.
            if (!retryCapacity.acquire(SdkDefaultRetrySettings.RETRY_THROTTLING_COST)) {
                return false;
            }
            this.retryCapacityConsumed = true;
        }

        this.retryPolicyContext = RetryPolicyContext.builder()
                                                    .request(request)
                                                    .originalRequest(context.originalRequest())
                                                    .exception(exception)
                                                    .retriesAttempted(retriesAttempted)
                                                    .executionAttributes(context.executionAttributes())
                                                    .httpStatusCode(httpResponse == null ? null : httpResponse.getStatusCode())
                                                    .build();
        // Finally, pass all the context information to the RetryCondition and let it decide whether it should be retried.
        if (!retryPolicy.retryCondition().shouldRetry(retryPolicyContext)) {
            // If the retry policy fails we immediately return consumed capacity to the pool.
            if (retryCapacityConsumed) {
                retryCapacity.release(SdkDefaultRetrySettings.RETRY_THROTTLING_COST);
            }
            return false;
        }

        return true;
    }

    /**
     * If this was a successful retry attempt we'll release the full retry capacity that the attempt originally consumed.  If
     * this was a successful initial request we release a lesser amount.
     */
    public void releaseRetryCapacity() {
        if (isRetry() && retryCapacityConsumed) {
            retryCapacity.release(SdkDefaultRetrySettings.RETRY_THROTTLING_COST);
        } else {
            retryCapacity.release();
        }
    }

    /**
     * Computes the delay before the next retry should be attempted based on the retry policy context.
     * @return long value of how long to wait
     */
    public Duration computeDelayBeforeNextRetry() {
        lastBackoffDelay = retryPolicy.backoffStrategy().computeDelayBeforeNextRetry(retryPolicyContext);
        return lastBackoffDelay;
    }

    /**
     * Sets whether retry capacity has been consumed for this request
     */
    public void retryCapacityConsumed(boolean retryCapacityConsumed) {
        this.retryCapacityConsumed = retryCapacityConsumed;
    }

    /**
     * Add the {@value HEADER_SDK_RETRY_INFO} header to the request. Contains metadata about request count,
     * backoff, and retry capacity.
     *
     * @return Request with retry info header added.
     */
    public SdkHttpFullRequest addRetryInfoHeader(SdkHttpFullRequest request, int requestCount) throws Exception {
        int availableRetryCapacity = retryCapacity.availableCapacity();
        return request.toBuilder()
                      .header(HEADER_SDK_RETRY_INFO,
                              singletonList(String.format("%s/%s/%s",
                                                          requestCount - 1,
                                                          lastBackoffDelay.toMillis(),
                                                          availableRetryCapacity >= 0 ? availableRetryCapacity : "")))
                      .build();
    }

    /**
     * Sets the last exception the has been seen by the retry handler.
     * @param exception - the last exception seen
     */
    public void setLastRetriedException(SdkException exception) {
        this.lastRetriedException = Optional.of(exception);
    }

    /**
     * Whether or not the current request is a retry. True if the original request has been retried at least one time.
     */
    public boolean isRetry() {
        return lastRetriedException.isPresent();
    }
}
