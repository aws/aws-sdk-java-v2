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
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Interface for specifying a retry policy to use when evaluating whether or not a request should be retried. An implementation
 * of this interface can be provided to {@link ClientOverrideConfiguration#retryPolicy} or the {@link #builder()}} can be used
 * to construct a retry policy from SDK provided policies or policies that directly implement {@link BackoffStrategy} and/or
 * {@link RetryCondition}.
 *
 * When using the {@link #builder()} the SDK will use default values for fields that are not provided. The default number of
 * retries that will be used is {@link SdkDefaultRetrySetting#DEFAULT_MAX_RETRIES}. The default retry condition is
 * {@link RetryCondition#defaultRetryCondition()} and the default backoff strategy is {@link BackoffStrategy#defaultStrategy()}.
 *
 * @see RetryCondition for a list of SDK provided retry condition strategies
 * @see BackoffStrategy for a list of SDK provided backoff strategies
 */
@Immutable
@SdkPublicApi
public final class RetryPolicy implements ToCopyableBuilder<RetryPolicy.Builder, RetryPolicy> {

    private final RetryCondition retryConditionFromBuilder;
    private final RetryCondition retryCondition;
    private final BackoffStrategy backoffStrategy;
    private final BackoffStrategy throttlingBackoffStrategy;
    private final Integer numRetries;

    private RetryPolicy(BuilderImpl builder) {
        this.backoffStrategy = builder.backoffStrategy;
        this.throttlingBackoffStrategy = builder.throttlingBackoffStrategy;
        this.numRetries = builder.numRetries;
        this.retryConditionFromBuilder = builder.retryCondition;
        this.retryCondition = AndRetryCondition.create(MaxNumberOfRetriesCondition.create(numRetries),
                                                       retryConditionFromBuilder);
    }

    public RetryCondition retryCondition() {
        return retryCondition;
    }

    public BackoffStrategy backoffStrategy() {
        return backoffStrategy;
    }

    public BackoffStrategy throttlingBackoffStrategy() {
        return throttlingBackoffStrategy;
    }

    public Integer numRetries() {
        return numRetries;
    }

    public Builder toBuilder() {
        return builder().numRetries(numRetries)
                        .retryCondition(retryConditionFromBuilder)
                        .backoffStrategy(backoffStrategy)
                        .throttlingBackoffStrategy(throttlingBackoffStrategy);
    }

    @Override
    public String toString() {
        return ToString.builder("RetryPolicy")
                       .add("numRetries", numRetries)
                       .add("retryCondition", retryCondition)
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

        if (!retryCondition.equals(that.retryCondition)) {
            return false;
        }
        if (!backoffStrategy.equals(that.backoffStrategy)) {
            return false;
        }
        if (!throttlingBackoffStrategy.equals(that.throttlingBackoffStrategy)) {
            return false;
        }
        return numRetries.equals(that.numRetries);
    }

    @Override
    public int hashCode() {
        int result = retryCondition.hashCode();
        result = 31 * result + backoffStrategy.hashCode();
        result = 31 * result + throttlingBackoffStrategy.hashCode();
        result = 31 * result + numRetries.hashCode();
        return result;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public static RetryPolicy defaultRetryPolicy() {
        return RetryPolicy.builder()
                          .backoffStrategy(BackoffStrategy.defaultStrategy())
                          .throttlingBackoffStrategy(BackoffStrategy.defaultThrottlingStrategy())
                          .numRetries(SdkDefaultRetrySetting.DEFAULT_MAX_RETRIES)
                          .retryCondition(RetryCondition.defaultRetryCondition())
                          .build();
    }

    public static RetryPolicy none() {
        return RetryPolicy.builder()
                          .numRetries(0)
                          .backoffStrategy(BackoffStrategy.none())
                          .throttlingBackoffStrategy(BackoffStrategy.none())
                          .retryCondition(RetryCondition.none())
                          .build();
    }

    public interface Builder extends CopyableBuilder<Builder, RetryPolicy> {
        Builder numRetries(Integer numRetries);

        Integer numRetries();

        Builder backoffStrategy(BackoffStrategy backoffStrategy);

        BackoffStrategy backoffStrategy();

        Builder throttlingBackoffStrategy(BackoffStrategy backoffStrategy);

        BackoffStrategy throttlingBackoffStrategy();

        Builder retryCondition(RetryCondition retryCondition);

        RetryCondition retryCondition();

        RetryPolicy build();
    }

    /**
     * Builder for a {@link RetryPolicy}.
     */
    private static final class BuilderImpl implements Builder {

        private Integer numRetries = SdkDefaultRetrySetting.DEFAULT_MAX_RETRIES;
        private BackoffStrategy backoffStrategy = BackoffStrategy.defaultStrategy();
        private BackoffStrategy throttlingBackoffStrategy = BackoffStrategy.defaultThrottlingStrategy();
        private RetryCondition retryCondition = RetryCondition.defaultRetryCondition();

        private BuilderImpl(){
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
        public RetryPolicy build() {
            return new RetryPolicy(this);
        }
    }
}
