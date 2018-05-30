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

package software.amazon.awssdk.core.retry.conditions;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.retry.RetryPolicyContext;
import software.amazon.awssdk.core.retry.RetryUtils;
import software.amazon.awssdk.core.retry.SdkDefaultRetrySettings;

@SdkPublicApi
@FunctionalInterface
public interface RetryCondition {

    RetryCondition DEFAULT = OrRetryCondition.create(
        RetryOnStatusCodeCondition.create(SdkDefaultRetrySettings.RETRYABLE_STATUS_CODES),
        RetryOnExceptionsCondition.create(SdkDefaultRetrySettings.RETRYABLE_EXCEPTIONS),
        c -> RetryUtils.isClockSkewException(c.exception()),
        c -> RetryUtils.isThrottlingException(c.exception()));
    RetryCondition NONE = MaxNumberOfRetriesCondition.create(0);

    default OrRetryCondition or(RetryCondition other) {
        return OrRetryCondition.create(this, other);
    }

    default AndRetryCondition and(RetryCondition other) {
        return AndRetryCondition.create(this, other);
    }

    /**
     * Determine whether a request should or should not be retried.
     *
     * @param context Context about the state of the last request and information about the number of requests made.
     * @return True if the request should be retried, false if not.
     */
    boolean shouldRetry(RetryPolicyContext context);
}
