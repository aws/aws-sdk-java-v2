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

package software.amazon.awssdk.retriesapi;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.utils.NumericUtils;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * Determines how long to wait before each execution attempt.
 */
@SdkPublicApi
@ThreadSafe
public interface BackoffStrategy {
    Duration BASE_DELAY_CEILING = Duration.ofMillis(Integer.MAX_VALUE); // Around ~24.8 days
    Duration MAX_BACKOFF_CEILING = Duration.ofMillis(Integer.MAX_VALUE); // Around ~24.8 days
    /**
     * Max permitted retry times. To prevent exponentialDelay from overflow, there must be 2 ^ retriesAttempted &lt;= 2 ^ 31 - 1,
     * which means retriesAttempted &lt;= 30, so that is the ceil for retriesAttempted.
     */
    int RETRIES_ATTEMPTED_CEILING = (int) Math.floor(Math.log(Integer.MAX_VALUE) / Math.log(2));

    /**
     * Do not back off: retry immediately.
     */
    static BackoffStrategy retryImmediately() {
        return new Immediately();
    }

    /**
     * Wait for a random period of time between 0ms and the provided delay.
     */
    static BackoffStrategy fixedDelay(Duration delay) {
        return new FixedDelayWithJitter(ThreadLocalRandom::current, delay);
    }

    /**
     * Wait for a period of time equal to the provided delay.
     */
    static BackoffStrategy fixedDelayWithoutJitter(Duration delay) {
        return new FixedDelayWithoutJitter(delay);
    }

    /**
     * Wait for a random period of time between 0ms and an exponentially increasing amount of time between each subsequent attempt
     * of the same call.
     *
     * <p>Specifically, the first attempt waits 0ms, and each subsequent attempt waits between
     * 0ms and {@code min(maxDelay, baseDelay * (1 << (attempt - 2)))}.
     */
    static BackoffStrategy exponentialDelay(Duration baseDelay, Duration maxDelay) {
        return new ExponentialDelayWithJitter(ThreadLocalRandom::current, baseDelay, maxDelay);
    }

    /**
     * Wait for an exponentially increasing amount of time between each subsequent attempt of the same call.
     *
     * <p>Specifically, the first attempt waits 0ms, and each subsequent attempt waits for
     * {@code min(maxDelay, baseDelay * (1 << (attempt - 2)))}.
     */
    static BackoffStrategy exponentialDelayWithoutJitter(Duration baseDelay, Duration maxDelay) {
        return new ExponentialDelayWithoutJitter(baseDelay, maxDelay);
    }

    /**
     * Returns the computed exponential delay in milliseconds given the retries attempted, the base delay and the max backoff
     * time.
     *
     * <p>Specifically it returns {@code min(maxDelay, baseDelay * (1 << (attempt - 2)))}. To prevent overflowing the attempts
     * get capped to 30.
     */
    default int calculateExponentialDelay(int retriesAttempted, Duration baseDelay, Duration maxBackoffTime) {
        int cappedRetries = Math.min(retriesAttempted, RETRIES_ATTEMPTED_CEILING);
        return (int) Math.min(baseDelay.multipliedBy(1L << (cappedRetries - 2)).toMillis(), maxBackoffTime.toMillis());
    }

    /**
     * Compute the amount of time to wait before the provided attempt number is executed.
     *
     * @param attempt The attempt to compute the delay for, starting at one.
     * @throws IllegalArgumentException If the given attempt is less or equal to zero.
     */
    Duration computeDelay(int attempt);

    /**
     * Strategy that do not back off: retry immediately.
     */
    final class Immediately implements BackoffStrategy {
        @Override
        public Duration computeDelay(int attempt) {
            Validate.isPositive(attempt, "attempt");
            return Duration.ZERO;
        }

        @Override
        public String toString() {
            return "(Immediately)";
        }
    }

    /**
     * Strategy that waits for a period of time equal to the provided delay.
     */
    final class FixedDelayWithoutJitter implements BackoffStrategy {
        private final Duration delay;

        FixedDelayWithoutJitter(Duration delay) {
            this.delay = Validate.isPositive(delay, "delay");
        }

        @Override
        public Duration computeDelay(int attempt) {
            Validate.isPositive(attempt, "attempt");
            return delay;
        }

        @Override
        public String toString() {
            return ToString.builder("FixedDelayWithoutJitter")
                           .add("delay", delay)
                           .build();
        }
    }

    /**
     * Strategy that waits for a random period of time between 0ms and the provided delay.
     */
    final class FixedDelayWithJitter implements BackoffStrategy {
        private final Supplier<Random> randomSupplier;
        private final Duration delay;

        FixedDelayWithJitter(Supplier<Random> randomSupplier, Duration delay) {
            this.randomSupplier = Validate.paramNotNull(randomSupplier, "random");
            this.delay = NumericUtils.min(Validate.isPositive(delay, "delay"), BASE_DELAY_CEILING);
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

    /**
     * Strategy that waits for a random period of time between 0ms and an exponentially increasing amount of time between each
     * subsequent attempt of the same call.
     *
     * <p>Specifically, the first attempt waits 0ms, and each subsequent attempt waits between
     * 0ms and {@code min(maxDelay, baseDelay * (1 << (attempt - 2)))}.
     */
    final class ExponentialDelayWithJitter implements BackoffStrategy {
        private final Supplier<Random> randomSupplier;
        private final Duration baseDelay;
        private final Duration maxDelay;

        ExponentialDelayWithJitter(Supplier<Random> randomSupplier, Duration baseDelay, Duration maxDelay) {
            this.randomSupplier = Validate.paramNotNull(randomSupplier, "random");
            this.baseDelay = NumericUtils.min(Validate.isPositive(baseDelay, "baseDelay"), BASE_DELAY_CEILING);
            this.maxDelay = NumericUtils.min(Validate.isPositive(maxDelay, "maxDelay"), MAX_BACKOFF_CEILING);
        }

        @Override
        public Duration computeDelay(int attempt) {
            Validate.isPositive(attempt, "attempt");
            if (attempt == 1) {
                return Duration.ZERO;
            }
            int delay = calculateExponentialDelay(attempt, baseDelay, maxDelay);
            int randInt = randomSupplier.get().nextInt(delay);
            return Duration.ofMillis(randInt);
        }

        @Override
        public String toString() {
            return ToString.builder("ExponentialDelayWithJitter")
                           .add("baseDelay", baseDelay)
                           .add("maxDelay", maxDelay)
                           .build();
        }
    }

    /**
     * Strategy that waits for an exponentially increasing amount of time between each subsequent attempt of the same call.
     *
     * <p>Specifically, the first attempt waits 0ms, and each subsequent attempt waits for
     * {@code min(maxDelay, baseDelay * (1 << (attempt - 2)))}.
     */
    final class ExponentialDelayWithoutJitter implements BackoffStrategy {
        private final Duration baseDelay;
        private final Duration maxDelay;

        public ExponentialDelayWithoutJitter(Duration baseDelay, Duration maxDelay) {
            this.baseDelay = NumericUtils.min(Validate.isPositive(baseDelay, "baseDelay"), BASE_DELAY_CEILING);
            this.maxDelay = NumericUtils.min(Validate.isPositive(maxDelay, "maxDelay"), MAX_BACKOFF_CEILING);
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
}
