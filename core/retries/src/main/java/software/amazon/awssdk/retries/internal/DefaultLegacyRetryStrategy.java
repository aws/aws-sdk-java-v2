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

package software.amazon.awssdk.retries.internal;

import java.util.function.Predicate;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.retries.LegacyRetryStrategy;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.retries.api.RefreshRetryTokenRequest;
import software.amazon.awssdk.retries.internal.circuitbreaker.TokenBucketStore;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class DefaultLegacyRetryStrategy
    extends BaseRetryStrategy implements LegacyRetryStrategy {
    private static final Logger LOG = Logger.loggerFor(LegacyRetryStrategy.class);
    private final int throttlingExceptionCost;

    DefaultLegacyRetryStrategy(Builder builder) {
        super(LOG, builder);
        this.throttlingExceptionCost = Validate.paramNotNull(builder.throttlingExceptionCost, "throttlingExceptionCost");
    }

    @Override
    protected int exceptionCost(RefreshRetryTokenRequest request) {
        if (circuitBreakerEnabled) {
            if (treatAsThrottling.test(request.failure())) {
                return throttlingExceptionCost;
            }
            return exceptionCost;
        }
        return 0;
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseRetryStrategy.Builder implements LegacyRetryStrategy.Builder {
        private Integer throttlingExceptionCost;

        Builder() {
        }

        Builder(DefaultLegacyRetryStrategy strategy) {
            super(strategy);
            this.throttlingExceptionCost = strategy.throttlingExceptionCost;
        }

        @Override
        public Builder retryOnException(Predicate<Throwable> shouldRetry) {
            setRetryOnException(shouldRetry);
            return this;
        }

        @Override
        public Builder maxAttempts(int maxAttempts) {
            setMaxAttempts(maxAttempts);
            return this;
        }

        @Override
        public Builder backoffStrategy(BackoffStrategy backoffStrategy) {
            setBackoffStrategy(backoffStrategy);
            return this;
        }

        @Override
        public Builder throttlingBackoffStrategy(BackoffStrategy throttlingBackoffStrategy) {
            setThrottlingBackoffStrategy(throttlingBackoffStrategy);
            return this;
        }

        @Override
        public Builder circuitBreakerEnabled(Boolean circuitBreakerEnabled) {
            setCircuitBreakerEnabled(circuitBreakerEnabled);
            return this;
        }

        @Override
        public Builder treatAsThrottling(Predicate<Throwable> treatAsThrottling) {
            setTreatAsThrottling(treatAsThrottling);
            return this;
        }

        public Builder tokenBucketExceptionCost(int exceptionCost) {
            setTokenBucketExceptionCost(exceptionCost);
            return this;
        }

        public Builder tokenBucketThrottlingExceptionCost(int throttlingExceptionCost) {
            this.throttlingExceptionCost = throttlingExceptionCost;
            return this;
        }

        public Builder tokenBucketStore(TokenBucketStore tokenBucketStore) {
            setTokenBucketStore(tokenBucketStore);
            return this;
        }

        @Override
        public Builder useClientDefaults(boolean useClientDefaults) {
            setUseClientDefaults(useClientDefaults);
            return this;
        }

        @Override
        public LegacyRetryStrategy build() {
            return new DefaultLegacyRetryStrategy(this);
        }
    }
}
