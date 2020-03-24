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

package software.amazon.awssdk.core.retry.conditions;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.internal.retry.SdkDefaultRetrySetting;
import software.amazon.awssdk.core.retry.RetryPolicyContext;

@SdkPublicApi
@FunctionalInterface
public interface RetryCondition {
    /**
     * Determine whether a request should or should not be retried.
     *
     * @param context Context about the state of the last request and information about the number of requests made.
     * @return True if the request should be retried, false if not.
     */
    boolean shouldRetry(RetryPolicyContext context);

    /**
     * Called by the SDK to notify this condition that the provided request will not be retried, because some retry condition
     * determined that it shouldn't be retried.
     */
    default void requestWillNotBeRetried(RetryPolicyContext context) {
    }

    /**
     * Called by the SDK to notify this condition that the provided request succeeded. This method is invoked even if the
     * execution never failed before ({@link RetryPolicyContext#retriesAttempted()} is zero).
     */
    default void requestSucceeded(RetryPolicyContext context) {
    }

    static RetryCondition defaultRetryCondition() {
        return OrRetryCondition.create(
            RetryOnStatusCodeCondition.create(SdkDefaultRetrySetting.RETRYABLE_STATUS_CODES),
            RetryOnExceptionsCondition.create(SdkDefaultRetrySetting.RETRYABLE_EXCEPTIONS),
            RetryOnClockSkewCondition.create(),
            RetryOnThrottlingCondition.create());
    }

    /**
     * A retry condition that will NEVER allow retries.
     */
    static RetryCondition none() {
        return MaxNumberOfRetriesCondition.create(0);
    }
}
