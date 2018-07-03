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

package software.amazon.awssdk.core.retry.backoff;

import static software.amazon.awssdk.utils.Validate.isNotNegative;

import java.time.Duration;
import java.util.Random;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.retry.RetryPolicyContext;
import software.amazon.awssdk.utils.Validate;
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

    private final Duration baseDelay;
    private final Duration maxBackoffTime;
    private final int numRetries;
    private final Random random = new Random();

    private EqualJitterBackoffStrategy(BuilderImpl builder) {
        this.baseDelay = isNotNegative(builder.baseDelay, "baseDelay");
        this.maxBackoffTime = isNotNegative(builder.maxBackoffTime, "maxBackoffTime");
        this.numRetries = Validate.isNotNegative(builder.numRetries, "numRetries");
    }

    @Override
    public Duration computeDelayBeforeNextRetry(RetryPolicyContext context) {
        int ceil = calculateExponentialDelay(context.retriesAttempted(), baseDelay, maxBackoffTime, numRetries);
        return Duration.ofMillis((ceil / 2) + random.nextInt((ceil / 2) + 1));
    }

    @Override
    public Builder toBuilder() {
        return builder().numRetries(numRetries).baseDelay(baseDelay).maxBackoffTime(maxBackoffTime);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder extends CopyableBuilder<EqualJitterBackoffStrategy.Builder, EqualJitterBackoffStrategy> {
        Builder baseDelay(Duration baseDelay);

        Duration baseDelay();

        Builder maxBackoffTime(Duration maxBackoffTime);

        Duration maxBackoffTime();

        Builder numRetries(Integer numRetries);

        Integer numRetries();

        EqualJitterBackoffStrategy build();
    }

    private static final class BuilderImpl implements Builder {

        private Duration baseDelay;
        private Duration maxBackoffTime;
        private int numRetries;

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
        public Builder numRetries(Integer numRetries) {
            this.numRetries = numRetries;
            return this;
        }

        public void setNumRetries(Integer numRetries) {
            numRetries(numRetries);
        }

        @Override
        public Integer numRetries() {
            return numRetries;
        }

        @Override
        public EqualJitterBackoffStrategy build() {
            return new EqualJitterBackoffStrategy(this);
        }
    }
}
