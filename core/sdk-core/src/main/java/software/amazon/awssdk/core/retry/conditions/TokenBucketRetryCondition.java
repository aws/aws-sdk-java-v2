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

package software.amazon.awssdk.core.retry.conditions;

import static software.amazon.awssdk.core.internal.retry.SdkDefaultRetrySetting.TOKEN_BUCKET_SIZE;

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.internal.capacity.TokenBucket;
import software.amazon.awssdk.core.internal.retry.SdkDefaultRetrySetting;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.RetryPolicyContext;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * A {@link RetryCondition} that limits the number of retries made by the SDK using a token bucket algorithm. "Tokens" are
 * acquired from the bucket whenever {@link #shouldRetry} returns true, and are released to the bucket whenever
 * {@link #requestSucceeded} or {@link #requestWillNotBeRetried} are invoked.
 *
 * <p>
 * If "tokens" cannot be acquired from the bucket, it means too many requests have failed and the request will not be allowed
 * to retry until we start to see initial non-retried requests succeed via {@link #requestSucceeded(RetryPolicyContext)}.
 *
 * <p>
 * This prevents the client from holding the calling thread to retry when it's likely that it will fail anyway.
 *
 * <p>
 * This is currently included in the default {@link RetryPolicy#aggregateRetryCondition()}, but can be disabled by setting the
 * {@link RetryPolicy.Builder#retryCapacityCondition} to null.
 */
@SdkPublicApi
public class TokenBucketRetryCondition implements RetryCondition {
    private static final ExecutionAttribute<Capacity> LAST_ACQUIRED_CAPACITY =
        new ExecutionAttribute<>("TokenBucketRetryCondition.LAST_ACQUIRED_CAPACITY");

    private static final ExecutionAttribute<Integer> RETRY_COUNT_OF_LAST_CAPACITY_ACQUISITION =
        new ExecutionAttribute<>("TokenBucketRetryCondition.RETRY_COUNT_OF_LAST_CAPACITY_ACQUISITION");

    private final TokenBucket capacity;
    private final TokenBucketExceptionCostFunction exceptionCostFunction;

    private TokenBucketRetryCondition(Builder builder) {
        this.capacity = new TokenBucket(Validate.notNull(builder.tokenBucketSize, "tokenBucketSize"));
        this.exceptionCostFunction = Validate.notNull(builder.exceptionCostFunction, "exceptionCostFunction");
    }

    /**
     * Create a condition using the {@link RetryMode#defaultRetryMode()}. This is equivalent to
     * {@code forRetryMode(RetryMode.defaultRetryMode())}.
     *
     * <p>
     * For more detailed control, see {@link #builder()}.
     */
    public static TokenBucketRetryCondition create() {
        return forRetryMode(RetryMode.defaultRetryMode());
    }

    /**
     * Create a condition using the configured {@link RetryMode}. The {@link RetryMode#LEGACY} does not subtract tokens from
     * the token bucket when throttling exceptions are encountered. The {@link RetryMode#STANDARD} treats throttling and non-
     * throttling exceptions as the same cost.
     *
     * <p>
     * For more detailed control, see {@link #builder()}.
     */
    public static TokenBucketRetryCondition forRetryMode(RetryMode retryMode) {
        return TokenBucketRetryCondition.builder()
                                        .tokenBucketSize(TOKEN_BUCKET_SIZE)
                                        .exceptionCostFunction(SdkDefaultRetrySetting.tokenCostFunction(retryMode))
                                        .build();
    }

    /**
     * Create a builder that allows fine-grained control over the token policy of this condition.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * If {@link #shouldRetry(RetryPolicyContext)} returned true for the provided execution, this method returns the
     * {@link Capacity} consumed by the request.
     */
    public static Optional<Capacity> getCapacityForExecution(ExecutionAttributes attributes) {
        return Optional.ofNullable(attributes.getAttribute(LAST_ACQUIRED_CAPACITY));
    }

    /**
     * Retrieve the number of tokens currently available in the token bucket. This is a volatile snapshot of the current value.
     * See {@link #getCapacityForExecution(ExecutionAttributes)} to see how much capacity was left in the bucket after a specific
     * execution was considered.
     */
    public int tokensAvailable() {
        return capacity.currentCapacity();
    }

    @Override
    public boolean shouldRetry(RetryPolicyContext context) {
        int costOfFailure = exceptionCostFunction.apply(context.exception());
        Validate.isTrue(costOfFailure >= 0, "Cost of failure must not be negative, but was " + costOfFailure);

        Optional<Capacity> capacity = this.capacity.tryAcquire(costOfFailure);

        capacity.ifPresent(c -> {
            context.executionAttributes().putAttribute(LAST_ACQUIRED_CAPACITY, c);
            context.executionAttributes().putAttribute(RETRY_COUNT_OF_LAST_CAPACITY_ACQUISITION,
                                                       context.retriesAttempted());
        });

        return capacity.isPresent();
    }

    @Override
    public void requestWillNotBeRetried(RetryPolicyContext context) {
        Integer lastAcquisitionRetryCount = context.executionAttributes().getAttribute(RETRY_COUNT_OF_LAST_CAPACITY_ACQUISITION);

        if (lastAcquisitionRetryCount != null && context.retriesAttempted() == lastAcquisitionRetryCount) {
            // We said yes to "should-retry", but something else caused it not to retry
            Capacity lastAcquiredCapacity = context.executionAttributes().getAttribute(LAST_ACQUIRED_CAPACITY);
            Validate.validState(lastAcquiredCapacity != null, "Last acquired capacity should not be null.");
            capacity.release(lastAcquiredCapacity.capacityAcquired());
        }
    }

    @Override
    public void requestSucceeded(RetryPolicyContext context) {
        Capacity lastAcquiredCapacity = context.executionAttributes().getAttribute(LAST_ACQUIRED_CAPACITY);

        if (lastAcquiredCapacity == null || lastAcquiredCapacity.capacityAcquired() == 0) {
            capacity.release(1);
        } else {
            capacity.release(lastAcquiredCapacity.capacityAcquired());
        }
    }

    @Override
    public String toString() {
        return ToString.builder("TokenBucketRetryCondition")
                       .add("capacity", capacity.currentCapacity() + "/" + capacity.maxCapacity())
                       .add("exceptionCostFunction", exceptionCostFunction)
                       .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TokenBucketRetryCondition that = (TokenBucketRetryCondition) o;

        if (!capacity.equals(that.capacity)) {
            return false;
        }
        return exceptionCostFunction.equals(that.exceptionCostFunction);
    }

    @Override
    public int hashCode() {
        int result = capacity.hashCode();
        result = 31 * result + exceptionCostFunction.hashCode();
        return result;
    }

    /**
     * Configure and create a {@link TokenBucketRetryCondition}.
     */
    public static final class Builder {
        private Integer tokenBucketSize;
        private TokenBucketExceptionCostFunction exceptionCostFunction;

        /**
         * Create using {@link TokenBucketRetryCondition#builder()}.
         */
        private Builder() {
        }

        /**
         * Specify the maximum number of tokens in the token bucket. This is also used as the initial value for the number of
         * tokens in the bucket.
         */
        public Builder tokenBucketSize(int tokenBucketSize) {
            this.tokenBucketSize = tokenBucketSize;
            return this;
        }

        /**
         * Configure a {@link TokenBucketExceptionCostFunction} that is used to calculate the number of tokens that should be
         * taken out of the bucket for each specific exception. These tokens will be returned in case of successful retries.
         */
        public Builder exceptionCostFunction(TokenBucketExceptionCostFunction exceptionCostFunction) {
            this.exceptionCostFunction = exceptionCostFunction;
            return this;
        }

        /**
         * Build a {@link TokenBucketRetryCondition} using the provided configuration.
         */
        public TokenBucketRetryCondition build() {
            return new TokenBucketRetryCondition(this);
        }
    }

    /**
     * The number of tokens in the token bucket after a specific token acquisition succeeds. This can be retrieved via
     * {@link #getCapacityForExecution(ExecutionAttributes)}.
     */
    public static final class Capacity {
        private final int capacityAcquired;
        private final int capacityRemaining;

        private Capacity(Builder builder) {
            this.capacityAcquired = Validate.notNull(builder.capacityAcquired, "capacityAcquired");
            this.capacityRemaining = Validate.notNull(builder.capacityRemaining, "capacityRemaining");
        }

        public static Builder builder() {
            return new Builder();
        }

        /**
         * The number of tokens acquired by the last token acquisition.
         */
        public int capacityAcquired() {
            return capacityAcquired;
        }

        /**
         * The number of tokens in the token bucket.
         */
        public int capacityRemaining() {
            return capacityRemaining;
        }

        public static class Builder {
            private Integer capacityAcquired;
            private Integer capacityRemaining;

            private Builder() {
            }

            public Builder capacityAcquired(Integer capacityAcquired) {
                this.capacityAcquired = capacityAcquired;
                return this;
            }

            public Builder capacityRemaining(Integer capacityRemaining) {
                this.capacityRemaining = capacityRemaining;
                return this;
            }

            public Capacity build() {
                return new Capacity(this);
            }
        }
    }
}
