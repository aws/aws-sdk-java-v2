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

package software.amazon.awssdk.retries.backoff;

import java.time.Duration;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Constants and utility functions shared by the BackoffStrategy implementations.
 */
@SdkInternalApi
class BackoffStrategiesConstants {
    static final Duration BASE_DELAY_CEILING = Duration.ofMillis(Integer.MAX_VALUE); // Around ~24.8 days
    static final Duration MAX_BACKOFF_CEILING = Duration.ofMillis(Integer.MAX_VALUE); // Around ~24.8 days
    /**
     * Max permitted retry times. To prevent exponentialDelay from overflow, there must be 2 ^ retriesAttempted &lt;= 2 ^ 31 - 1,
     * which means retriesAttempted &lt;= 30, so that is the ceil for retriesAttempted.
     */
    static final int RETRIES_ATTEMPTED_CEILING = (int) Math.floor(Math.log(Integer.MAX_VALUE) / Math.log(2));

    private BackoffStrategiesConstants() {
    }

    /**
     * Returns the computed exponential delay in milliseconds given the retries attempted, the base delay and the max backoff
     * time.
     *
     * <p>Specifically it returns {@code min(maxDelay, baseDelay * (1 << (attempt - 2)))}. To prevent overflowing the attempts
     * get capped to 30.
     */
    static int calculateExponentialDelay(int retriesAttempted, Duration baseDelay, Duration maxBackoffTime) {
        int cappedRetries = Math.min(retriesAttempted, BackoffStrategiesConstants.RETRIES_ATTEMPTED_CEILING);
        return (int) Math.min(baseDelay.multipliedBy(1L << (cappedRetries - 2)).toMillis(), maxBackoffTime.toMillis());
    }
}
