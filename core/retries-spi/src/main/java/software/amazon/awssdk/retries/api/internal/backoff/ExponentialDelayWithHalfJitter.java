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
import java.util.Random;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.utils.NumericUtils;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * Strategy that waits for a random period of time between a lower bound x and an exponentially increasing amount of time between
 * each subsequent attempt of the same call. The lower bound x is half the amount of the computed exponential delay.
 *
 * <p>
 * Specifically, the first attempt waits 0ms, and each subsequent attempt waits between
 * {@code min(maxDelay, baseDelay * (1 << (attempt - 2))) / 2} and {@code min(maxDelay, baseDelay * (1 << (attempt - 2)))}.
 *
 * <p>
 * This is in contrast to {@link ExponentialDelayWithJitter} where the final computed delay before the next retry will be between
 * 0 and the computed exponential delay.
 */
@SdkProtectedApi
public final class ExponentialDelayWithHalfJitter implements BackoffStrategy {
    private final Supplier<Random> randomSupplier;
    private final Duration baseDelay;
    private final Duration maxDelay;

    public ExponentialDelayWithHalfJitter(Supplier<Random> randomSupplier, Duration baseDelay, Duration maxDelay) {
        this.randomSupplier = Validate.paramNotNull(randomSupplier, "random");
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
        int ceil = calculateExponentialDelay(attempt, baseDelay, maxDelay);
        return Duration.ofMillis((ceil / 2) + randomSupplier.get().nextInt((ceil / 2) + 1));
    }

    @Override
    public String toString() {
        return ToString.builder("ExponentialDelayWithHalfJitter")
                       .add("baseDelay", baseDelay)
                       .add("maxDelay", maxDelay)
                       .build();
    }
}

