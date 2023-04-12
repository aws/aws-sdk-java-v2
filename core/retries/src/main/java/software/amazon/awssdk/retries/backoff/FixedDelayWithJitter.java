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
import java.util.Random;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.utils.NumericUtils;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * Strategy that waits for a random period of time between 0ms and the provided delay.
 */
@SdkInternalApi
final class FixedDelayWithJitter implements BackoffStrategy {
    private final Supplier<Random> randomSupplier;
    private final Duration delay;

    FixedDelayWithJitter(Supplier<Random> randomSupplier, Duration delay) {
        this.randomSupplier = Validate.paramNotNull(randomSupplier, "random");
        this.delay = NumericUtils.min(Validate.isPositive(delay, "delay"), BackoffStrategiesConstants.BASE_DELAY_CEILING);
    }

    @Override
    public Duration computeDelay(int attempt) {
        Validate.isPositive(attempt, "attempt");
        return Duration.ofMillis(randomSupplier.get().nextInt((int) delay.toMillis()));
    }

    @Override
    public String toString() {
        return ToString.builder("FixedDelayWithJitter")
                       .add("delay", delay)
                       .build();
    }
}
