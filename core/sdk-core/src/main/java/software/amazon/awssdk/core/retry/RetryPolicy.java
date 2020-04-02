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

package software.amazon.awssdk.core.retry;

import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.internal.retry.SdkDefaultRetrySetting;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.core.retry.conditions.AndRetryCondition;
import software.amazon.awssdk.core.retry.conditions.MaxNumberOfRetriesCondition;
import software.amazon.awssdk.core.retry.conditions.RetryCondition;
import software.amazon.awssdk.core.retry.conditions.TokenBucketRetryCondition;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Interface for specifying a retry policy to use when evaluating whether or not a request should be retried. The
 * {@link #builder()}} can be used to construct a retry policy from SDK provided policies or policies that directly implement
 * {@link BackoffStrategy} and/or {@link RetryCondition}. This is configured on a client via
 * {@link ClientOverrideConfiguration.Builder#retryPolicy}.
 *
 * When using the {@link #builder()} the SDK will use default values for fields that are not provided. The default number of
 * retries and condition is based on the current {@link RetryMode}.
 *
 * @see RetryCondition for a list of SDK provided retry condition strategies
 * @see BackoffStrategy for a list of SDK provided backoff strategies
 */
@Immutable
@SdkPublicApi
public final class RetryPolicy implements ToCopyableBuilder<RetryPolicy.Builder, RetryPolicy> {
    private final boolean additionalRetryConditionsAllowed;
    private final RetryMode retryMode;
    private final BackoffStrategy backoffStrategy;
    private final BackoffStrategy throttlingBackoffStrategy;
    private final Integer numRetries;
    private final RetryCondition retryCondition;
    private final RetryCondition retryCapacityCondition;

    private final RetryCondition aggregateRetryCondition;

    private RetryPolicy(BuilderImpl builder) {
        this.additionalRetryConditionsAllowed = builder.additionalRetryConditionsAllowed;
        this.retryMode = builder.retryMode;
        this.backoffStrategy = builder.backoffStrategy;
        this.throttlingBackoffStrategy = builder.throttlingBackoffStrategy;
        this.numRetries = builder.numRetries;
        this.retryCondition = builder.retryCondition;
        this.retryCapacityCondition = builder.retryCapacityCondition;

        this.aggregateRetryCondition = generateAggregateRetryCondition();
    }

    /**
     * Create a {@link RetryPolicy} using the {@link RetryMode#defaultRetryMode()} defaults.
     */
    public static RetryPolicy defaultRetryPolicy() {
        return forRetryMode(RetryMode.defaultRetryMode());
    }

    /**
     * Create a {@link RetryPolicy} using the provided {@link RetryMode} defaults.
     */
    public static RetryPolicy forRetryMode(RetryMode retryMode) {
        return RetryPolicy.builder(retryMode).build();
    }

    /**
     * Create a {@link RetryPolicy} that will NEVER retry.
     */
    public static RetryPolicy none() {
        return RetryPolicy.builder()
                          .numRetries(0)
                          .backoffStrategy(BackoffStrategy.none())
                          .throttlingBackoffStrategy(BackoffStrategy.none())
                          .retryCondition(RetryCondition.none())
                          .additionalRetryConditionsAllowed(false)
                          .build();
    }

    /**
     * Create a {@link RetryPolicy.Builder} populated with the defaults from the {@link RetryMode#defaultRetryMode()}.
     */
    public static Builder builder() {
        return new BuilderImpl(RetryMode.defaultRetryMode());
    }

    /**
     * Create a {@link RetryPolicy.Builder} populated with the defaults from the provided {@link RetryMode}.
     */
    public static Builder builder(RetryMode retryMode) {
        return new BuilderImpl(retryMode);
    }

    /**
     * Retrieve the {@link RetryMode} that was used to determine the defaults for this retry policy.
     */
    public RetryMode retryMode() {
        return retryMode;
    }

    /**
     * Returns true if service-specific conditions are allowed on this policy (e.g. more conditions may be added by the SDK if
     * they are recommended).
     */
    public boolean additionalRetryConditionsAllowed() {
        return additionalRetryConditionsAllowed;
    }

    /**
     * Retrieve the retry condition that aggregates the {@link Builder#retryCondition(RetryCondition)},
     * {@link Builder#numRetries(Integer)} and {@link Builder#retryCapacityCondition(RetryCondition)} configured on the builder.
     */
    public RetryCondition aggregateRetryCondition() {
        return aggregateRetryCondition;
    }

    /**
     * Retrieve the {@link Builder#retryCondition(RetryCondition)} configured on the builder.
     */
    public RetryCondition retryCondition() {
        return retryCondition;
    }

    /**
     * Retrieve the {@link Builder#backoffStrategy(BackoffStrategy)} configured on the builder.
     */
    public BackoffStrategy backoffStrategy() {
        return backoffStrategy;
    }

    /**
     * Retrieve the {@link Builder#throttlingBackoffStrategy(BackoffStrategy)} configured on the builder.
     */
    public BackoffStrategy throttlingBackoffStrategy() {
        return throttlingBackoffStrategy;
    }

    /**
     * Retrieve the {@link Builder#numRetries(Integer)} configured on the builder.
     */
    public Integer numRetries() {
        return numRetries;
    }

    private RetryCondition generateAggregateRetryCondition() {
        RetryCondition aggregate = AndRetryCondition.create(MaxNumberOfRetriesCondition.create(numRetries),
                                                            retryCondition);
        if (retryCapacityCondition != null) {
            return AndRetryCondition.create(aggregate, retryCapacityCondition);
        }
        return aggregate;
    }

    public Builder toBuilder() {
        return builder(retryMode).additionalRetryConditionsAllowed(additionalRetryConditionsAllowed)
                                 .numRetries(numRetries)
                                 .retryCondition(retryCondition)
                                 .backoffStrategy(backoffStrategy)
                                 .throttlingBackoffStrategy(throttlingBackoffStrategy)
                                 .retryCapacityCondition(retryCapacityCondition);
    }

    @Override
    public String toString() {
        return ToString.builder("RetryPolicy")
                       .add("additionalRetryConditionsAllowed", additionalRetryConditionsAllowed)
                       .add("aggregateRetryCondition", aggregateRetryCondition)
                       .add("backoffStrategy", backoffStrategy)
                       .add("throttlingBackoffStrategy", throttlingBackoffStrategy)
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

        RetryPolicy that = (RetryPolicy) o;

        if (additionalRetryConditionsAllowed != that.additionalRetryConditionsAllowed) {
            return false;
        }
        if (!aggregateRetryCondition.equals(that.aggregateRetryCondition)) {
            return false;
        }
        if (!backoffStrategy.equals(that.backoffStrategy)) {
            return false;
        }
        if (!throttlingBackoffStrategy.equals(that.throttlingBackoffStrategy)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = aggregateRetryCondition.hashCode();
        result = 31 * result + Boolean.hashCode(additionalRetryConditionsAllowed);
        result = 31 * result + backoffStrategy.hashCode();
        result = 31 * result + throttlingBackoffStrategy.hashCode();
        return result;
    }

    public interface Builder extends CopyableBuilder<Builder, RetryPolicy> {
        /**
         * Configure whether further conditions can be added to this policy after it is created. This may include service-
         * specific retry conditions that may not otherwise be covered by the {@link RetryCondition#defaultRetryCondition()}.
         *
         * <p>
         * By default, this is true.
         */
        Builder additionalRetryConditionsAllowed(boolean additionalRetryConditionsAllowed);

        /**
         * @see #additionalRetryConditionsAllowed(boolean)
         */
        boolean additionalRetryConditionsAllowed();

        /**
         * Configure the backoff strategy that should be used for waiting in between retry attempts. If the retry is because of
         * throttling reasons, the {@link #throttlingBackoffStrategy(BackoffStrategy)} is used instead.
         */
        Builder backoffStrategy(BackoffStrategy backoffStrategy);

        /**
         * @see #backoffStrategy(BackoffStrategy)
         */
        BackoffStrategy backoffStrategy();

        /**
         * Configure the backoff strategy that should be used for waiting in between retry attempts after a throttling error
         * is encountered. If the retry is not because of throttling reasons, the {@link #backoffStrategy(BackoffStrategy)} is
         * used instead.
         */
        Builder throttlingBackoffStrategy(BackoffStrategy backoffStrategy);

        /**
         * @see #throttlingBackoffStrategy(BackoffStrategy)
         */
        BackoffStrategy throttlingBackoffStrategy();

        /**
         * Configure the condition under which the request should be retried.
         *
         * <p>
         * While this can be any interface that implements {@link RetryCondition}, it is encouraged to use
         * {@link #numRetries(Integer)} when attempting to limit the number of times the SDK will retry an attempt or the
         * {@link #retryCapacityCondition(RetryCondition)} when attempting to configure the throttling of retries. This guidance
         * is because the SDK uses the {@link #aggregateRetryCondition()} when determining whether or not to retry a request,
         * and the {@code aggregateRetryCondition} includes the {@code numRetries} and {@code retryCapacityCondition} in its
         * determination.
         */
        Builder retryCondition(RetryCondition retryCondition);

        /**
         * @see #retryCondition(RetryCondition)
         */
        RetryCondition retryCondition();

        /**
         * Configure the {@link RetryCondition} that should be used to throttle the number of retries attempted by the SDK client
         * as a whole.
         *
         * <p>
         * While any {@link RetryCondition} (or null) can be used, by convention these conditions are usually stateful and work
         * globally for the whole client to limit the overall capacity of the client to execute retries.
         *
         * <p>
         * By default the {@link TokenBucketRetryCondition} is used. This can be disabled by setting the value to {@code null}
         * (not {@code RetryPolicy#none()}, which would completely disable retries).
         */
        Builder retryCapacityCondition(RetryCondition retryCapacityCondition);

        /**
         * @see #retryCapacityCondition(RetryCondition)
         */
        RetryCondition retryCapacityCondition();

        /**
         * Configure the maximum number of times that a single request should be retried, assuming it fails for a retryable error.
         */
        Builder numRetries(Integer numRetries);

        /**
         * @see #numRetries(Integer)
         */
        Integer numRetries();
        
        RetryPolicy build();
    }

    /**
     * Builder for a {@link RetryPolicy}.
     */
    private static final class BuilderImpl implements Builder {
        private final RetryMode retryMode;

        private boolean additionalRetryConditionsAllowed;
        private Integer numRetries;
        private BackoffStrategy backoffStrategy;
        private BackoffStrategy throttlingBackoffStrategy;
        private RetryCondition retryCondition;
        private RetryCondition retryCapacityCondition;

        private BuilderImpl(RetryMode retryMode) {
            this.retryMode = retryMode;
            this.numRetries = SdkDefaultRetrySetting.maxAttempts(retryMode) - 1;
            this.additionalRetryConditionsAllowed = true;
            this.backoffStrategy = BackoffStrategy.defaultStrategy();
            this.throttlingBackoffStrategy = BackoffStrategy.defaultThrottlingStrategy();
            this.retryCondition = RetryCondition.defaultRetryCondition();
            this.retryCapacityCondition = TokenBucketRetryCondition.forRetryMode(retryMode);
        }

        @Override
        public Builder additionalRetryConditionsAllowed(boolean additionalRetryConditionsAllowed) {
            this.additionalRetryConditionsAllowed = additionalRetryConditionsAllowed;
            return this;
        }

        public void setadditionalRetryConditionsAllowed(boolean additionalRetryConditionsAllowed) {
            additionalRetryConditionsAllowed(additionalRetryConditionsAllowed);
        }

        @Override
        public boolean additionalRetryConditionsAllowed() {
            return additionalRetryConditionsAllowed;
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
        public Builder backoffStrategy(BackoffStrategy backoffStrategy) {
            this.backoffStrategy = backoffStrategy;
            return this;
        }

        public void setBackoffStrategy(BackoffStrategy backoffStrategy) {
            backoffStrategy(backoffStrategy);
        }

        @Override
        public BackoffStrategy backoffStrategy() {
            return backoffStrategy;
        }

        @Override
        public Builder throttlingBackoffStrategy(BackoffStrategy throttlingBackoffStrategy) {
            this.throttlingBackoffStrategy = throttlingBackoffStrategy;
            return this;
        }

        @Override
        public BackoffStrategy throttlingBackoffStrategy() {
            return throttlingBackoffStrategy;
        }

        public void setThrottlingBackoffStrategy(BackoffStrategy throttlingBackoffStrategy) {
            this.throttlingBackoffStrategy = throttlingBackoffStrategy;
        }

        @Override
        public Builder retryCondition(RetryCondition retryCondition) {
            this.retryCondition = retryCondition;
            return this;
        }

        public void setRetryCondition(RetryCondition retryCondition) {
            retryCondition(retryCondition);
        }

        @Override
        public RetryCondition retryCondition() {
            return retryCondition;
        }

        @Override
        public Builder retryCapacityCondition(RetryCondition retryCapacityCondition) {
            this.retryCapacityCondition = retryCapacityCondition;
            return this;
        }

        public void setRetryCapacityCondition(RetryCondition retryCapacityCondition) {
            retryCapacityCondition(retryCapacityCondition);
        }

        @Override
        public RetryCondition retryCapacityCondition() {
            return this.retryCapacityCondition;
        }

        @Override
        public RetryPolicy build() {
            return new RetryPolicy(this);
        }
    }
}
