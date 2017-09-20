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

package software.amazon.awssdk.retry.v2;

import software.amazon.awssdk.annotation.ReviewBeforeRelease;

/**
 * Aggregate interface combining a {@link RetryCondition} and {@link BackoffStrategy} into a single policy.
 */
public interface RetryPolicy extends RetryCondition, BackoffStrategy {
    /**
     * When throttled retries are enabled, each retry attempt will consume this much capacity.
     * Successful retry attempts will release this capacity back to the pool while failed retries
     * will not.  Successful initial (non-retry) requests will always release 1 capacity unit to the
     * pool.
     */
    @ReviewBeforeRelease("There is probably a better place for this after we refactor retries.")
    int THROTTLED_RETRY_COST = 5;

    /**
     * When throttled retries are enabled, this is the total number of subsequent failed retries
     * that may be attempted before retry capacity is fully drained.
     */
    @ReviewBeforeRelease("There is probably a better place for this after we refactor retries.")
    int THROTTLED_RETRIES = 100;
}
