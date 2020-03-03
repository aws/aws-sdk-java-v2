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

package software.amazon.awssdk.core.retry.backoff;

import java.time.Duration;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.internal.retry.SdkDefaultRetrySetting;
import software.amazon.awssdk.core.retry.RetryPolicyContext;

@SdkPublicApi
@FunctionalInterface
public interface BackoffStrategy {

    /**
     * Max permitted retry times. To prevent exponentialDelay from overflow, there must be 2 ^ retriesAttempted
     * <= 2 ^ 31 - 1, which means retriesAttempted <= 30, so that is the ceil for retriesAttempted.
     */
    int RETRIES_ATTEMPTED_CEILING = (int) Math.floor(Math.log(Integer.MAX_VALUE) / Math.log(2));

    /**
     * Compute the delay before the next retry request. This strategy is only consulted when there will be a next retry.
     *
     * @param context Context about the state of the last request and information about the number of requests made.
     * @return Amount of time in milliseconds to wait before the next attempt. Must be non-negative (can be zero).
     */
    Duration computeDelayBeforeNextRetry(RetryPolicyContext context);

    default int calculateExponentialDelay(int retriesAttempted, Duration baseDelay, Duration maxBackoffTime) {
        int cappedRetries = Math.min(retriesAttempted, RETRIES_ATTEMPTED_CEILING);
        return (int) Math.min(baseDelay.multipliedBy(1L << cappedRetries).toMillis(), maxBackoffTime.toMillis());
    }

    static BackoffStrategy defaultStrategy() {
        return FullJitterBackoffStrategy.builder()
                                        .baseDelay(SdkDefaultRetrySetting.BASE_DELAY)
                                        .maxBackoffTime(SdkDefaultRetrySetting.MAX_BACKOFF)
                                        .build();
    }

    static BackoffStrategy defaultThrottlingStrategy() {
        return EqualJitterBackoffStrategy.builder()
                                         .baseDelay(SdkDefaultRetrySetting.THROTTLED_BASE_DELAY)
                                         .maxBackoffTime(SdkDefaultRetrySetting.MAX_BACKOFF)
                                         .build();
    }

    static BackoffStrategy none() {
        return FixedDelayBackoffStrategy.create(Duration.ofMillis(1));
    }
}
