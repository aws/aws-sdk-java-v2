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

package software.amazon.awssdk.core.retry;

import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.internal.retry.SdkDefaultRetrySetting;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.core.retry.conditions.AndRetryCondition;
import software.amazon.awssdk.core.retry.conditions.MaxNumberOfRetriesCondition;
import software.amazon.awssdk.core.retry.conditions.RetryCondition;
import software.amazon.awssdk.core.retry.conditions.SdkRetryCondition;
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
 * {@link SdkRetryCondition#DEFAULT} and the default backoff strategy is {@link BackoffStrategy#defaultStrategy()}.
 *
 * @see SdkRetryCondition for a list of SDK provided retry condition strategies
 * @see BackoffStrategy for a list of SDK provided backoff strategies
 */
@Immutable
@SdkPublicApi
public final class RetryPolicy implements ToCopyableBuilder<RetryPolicy.Builder, RetryPolicy> {

    public static final RetryPolicy DEFAULT = RetryPolicy.builder()
                                                         .backoffStrategy(BackoffStrategy.defaultStrategy())
                                                         .numRetries(SdkDefaultRetrySetting.DEFAULT_MAX_RETRIES)
                                                         .retryCondition(SdkRetryCondition.DEFAULT)
                                                         .build();

    public static final RetryPolicy NONE = RetryPolicy.builder()
                                                      .backoffStrategy(BackoffStrategy.none())
                                                      .retryCondition(SdkRetryCondition.NONE)
                                                      .build();

    private final RetryCondition retryConditionFromBuilder;
    private final RetryCondition retryCondition;
    private final BackoffStrategy backoffStrategy;
    private final Integer numRetries;

    private RetryPolicy(Builder builder) {
        this.backoffStrategy = builder.backoffStrategy;
        this.numRetries = builder.numRetries;
        this.retryConditionFromBuilder = builder.retryCondition;
        this.retryCondition = AndRetryCondition.create(MaxNumberOfRetriesCondition.create(numRetries), retryConditionFromBuilder);
    }

    public RetryCondition retryCondition() {
        return retryCondition;
    }

    public BackoffStrategy backoffStrategy() {
        return backoffStrategy;
    }

    public Integer numRetries() {
        return numRetries;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return builder().numRetries(numRetries).retryCondition(retryConditionFromBuilder).backoffStrategy(backoffStrategy);
    }

    /**
     * Builder for a {@link RetryPolicy}.
     */
    public static final class Builder implements CopyableBuilder<Builder, RetryPolicy> {

        private Integer numRetries = SdkDefaultRetrySetting.DEFAULT_MAX_RETRIES;
        private BackoffStrategy backoffStrategy = BackoffStrategy.defaultStrategy();
        private RetryCondition retryCondition = SdkRetryCondition.DEFAULT;

        private Builder(){
        }

        public Builder numRetries(Integer numRetries) {
            this.numRetries = numRetries;
            return this;
        }

        public Integer numRetries() {
            return numRetries;
        }

        public Builder backoffStrategy(BackoffStrategy backoffStrategy) {
            this.backoffStrategy = backoffStrategy;
            return this;
        }

        public BackoffStrategy backoffStrategy() {
            return backoffStrategy;
        }

        public Builder retryCondition(RetryCondition retryCondition) {
            this.retryCondition = retryCondition;
            return this;
        }

        public RetryCondition retryCondition() {
            return retryCondition;
        }

        @Override
        public RetryPolicy build() {
            return new RetryPolicy(this);
        }
    }
}
