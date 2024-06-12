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

package software.amazon.awssdk.retries.api.internal.backoff;

import static software.amazon.awssdk.retries.api.internal.backoff.BackoffStrategiesConstants.calculateExponentialDelay;

import java.time.Duration;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.utils.NumericUtils;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * Strategy that waits for an exponentially increasing amount of time between each subsequent attempt of the same call.
 *
 * <p>Specifically, the first attempt waits 0ms, and each subsequent attempt waits for
 * {@code min(maxDelay, baseDelay * (1 << (attempt - 2)))}.
 */
@SdkInternalApi
public final class ExponentialDelayWithoutJitter implements BackoffStrategy {
    private final Duration baseDelay;
    private final Duration maxDelay;

    public ExponentialDelayWithoutJitter(Duration baseDelay, Duration maxDelay) {
        this.baseDelay = NumericUtils.min(Validate.isPositive(baseDelay, "baseDelay"),
                                          BackoffStrategiesConstants.BASE_DELAY_CEILING);
        this.maxDelay = NumericUtils.min(Validate.isPositive(maxDelay, "maxDelay"),
                                         BackoffStrategiesConstants.MAX_BACKOFF_CEILING);
    }

    @Override
    public Duration computeDelay(int attempt) {
        Validate.isPositive(attempt, "attempt");
        if (attempt == 1) {
            return Duration.ZERO;
        }
        int delay = calculateExponentialDelay(attempt, baseDelay, maxDelay);
        return Duration.ofMillis(delay);
    }

    @Override
    public String toString() {
        return ToString.builder("ExponentialDelayWithoutJitter")
                       .add("baseDelay", baseDelay)
                       .add("maxDelay", maxDelay)
                       .build();
    }
}
