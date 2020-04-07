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

import static software.amazon.awssdk.utils.NumericUtils.min;
import static software.amazon.awssdk.utils.Validate.isNotNegative;

import java.time.Duration;
import java.util.Random;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.retry.RetryPolicyContext;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Backoff strategy that uses equal jitter for computing the delay before the next retry. An equal jitter
 * backoff strategy will first compute an exponential delay based on the current number of retries, base delay
 * and max delay. The final computed delay before the next retry will keep half of this computed delay plus
 * a random delay computed as a random number between 0 and half of the exponential delay plus one.
 *
 * For example, using a base delay of 100, a max backoff time of 10000 an exponential delay of 400 is computed
 * for a second retry attempt. The final computed delay before the next retry will be half of the computed exponential
 * delay, in this case 200, plus a random number between 0 and 201. Therefore the range for delay would be between
 * 200 and 401.
 *
 * This is in contrast to {@link FullJitterBackoffStrategy} where the final computed delay before the next retry will be
 * between 0 and the computed exponential delay.
 */
@SdkPublicApi
public final class EqualJitterBackoffStrategy implements BackoffStrategy,
                                                         ToCopyableBuilder<EqualJitterBackoffStrategy.Builder,
                                                             EqualJitterBackoffStrategy> {

    private static final Duration BASE_DELAY_CEILING = Duration.ofMillis(Integer.MAX_VALUE); // Around 24 days
    private static final Duration MAX_BACKOFF_CEILING = Duration.ofMillis(Integer.MAX_VALUE); // Around 24 days

    private final Duration baseDelay;
    private final Duration maxBackoffTime;
    private final Random random;

    private EqualJitterBackoffStrategy(BuilderImpl builder) {
        this(builder.baseDelay, builder.maxBackoffTime, new Random());
    }

    EqualJitterBackoffStrategy(final Duration baseDelay, final Duration maxBackoffTime, final Random random) {
        this.baseDelay = min(isNotNegative(baseDelay, "baseDelay"), BASE_DELAY_CEILING);
        this.maxBackoffTime = min(isNotNegative(maxBackoffTime, "maxBackoffTime"), MAX_BACKOFF_CEILING);
        this.random = random;
    }

    @Override
    public Duration computeDelayBeforeNextRetry(RetryPolicyContext context) {
        int ceil = calculateExponentialDelay(context.retriesAttempted(), baseDelay, maxBackoffTime);
        return Duration.ofMillis((ceil / 2) + random.nextInt((ceil / 2) + 1));
    }

    @Override
    public Builder toBuilder() {
        return builder().baseDelay(baseDelay).maxBackoffTime(maxBackoffTime);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder extends CopyableBuilder<EqualJitterBackoffStrategy.Builder, EqualJitterBackoffStrategy> {
        Builder baseDelay(Duration baseDelay);

        Duration baseDelay();

        Builder maxBackoffTime(Duration maxBackoffTime);

        Duration maxBackoffTime();

        EqualJitterBackoffStrategy build();
    }

    private static final class BuilderImpl implements Builder {

        private Duration baseDelay;
        private Duration maxBackoffTime;

        private BuilderImpl() {
        }

        @Override
        public Builder baseDelay(Duration baseDelay) {
            this.baseDelay = baseDelay;
            return this;
        }

        public void setBaseDelay(Duration baseDelay) {
            baseDelay(baseDelay);
        }

        @Override
        public Duration baseDelay() {
            return baseDelay;
        }

        @Override
        public Builder maxBackoffTime(Duration maxBackoffTime) {
            this.maxBackoffTime = maxBackoffTime;
            return this;
        }

        public void setMaxBackoffTime(Duration maxBackoffTime) {
            maxBackoffTime(maxBackoffTime);
        }

        @Override
        public Duration maxBackoffTime() {
            return maxBackoffTime;
        }

        @Override
        public EqualJitterBackoffStrategy build() {
            return new EqualJitterBackoffStrategy(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EqualJitterBackoffStrategy that = (EqualJitterBackoffStrategy) o;

        if (!baseDelay.equals(that.baseDelay)) {
            return false;
        }
        return maxBackoffTime.equals(that.maxBackoffTime);
    }

    @Override
    public int hashCode() {
        int result = baseDelay.hashCode();
        result = 31 * result + maxBackoffTime.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("EqualJitterBackoffStrategy")
                       .add("baseDelay", baseDelay)
                       .add("maxBackoffTime", maxBackoffTime)
                       .build();
    }
}
