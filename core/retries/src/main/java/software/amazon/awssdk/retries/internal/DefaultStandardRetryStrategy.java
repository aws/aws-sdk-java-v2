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

import java.time.Duration;
import java.util.Optional;
import java.util.function.Predicate;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.retries.api.RefreshRetryTokenRequest;
import software.amazon.awssdk.retries.internal.circuitbreaker.TokenBucketStore;
import software.amazon.awssdk.utils.Logger;

@SdkInternalApi
public final class DefaultStandardRetryStrategy
    extends BaseRetryStrategy implements StandardRetryStrategy {
    private static final Logger LOG = Logger.loggerFor(DefaultStandardRetryStrategy.class);
    private static final Duration FIVE_SECONDS = Duration.ofSeconds(5);
    private final Boolean retries2026Enabled;

    DefaultStandardRetryStrategy(Builder builder) {
        super(LOG, builder);
        this.retries2026Enabled = builder.retries2026Enabled;
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected Duration computeAcquireFailureBackoff(RefreshRetryTokenRequest request) {
        if (!isRetries2026Enabled() || !request.isLongPolling()) {
            return super.computeAcquireFailureBackoff(request);
        }

        DefaultRetryToken attemptIncremented = asDefaultRetryToken(request.token()).toBuilder()
                                                                                   .increaseAttempt()
                                                                                   .build();
        return computeBackoff(request, attemptIncremented);
    }

    @Override
    protected Duration computeBackoff(RefreshRetryTokenRequest request, DefaultRetryToken token) {
        if (!isRetries2026Enabled()) {
            return super.computeBackoff(request, token);
        }

        Duration strategyBackoff;
        if (treatAsThrottling.test(request.failure())) {
            strategyBackoff = throttlingBackoffStrategy.computeDelay(token.attempt());
        } else {
            strategyBackoff = backoffStrategy.computeDelay(token.attempt());
        }

        Optional<Duration> optionalSuggested = request.suggestedDelay();

        if (!optionalSuggested.isPresent()) {
            return strategyBackoff;
        }

        // the suggested delay needs to be at least what the strategy computed, OR
        // not greater than 5s more than what the strat computed
        Duration minBackoff = strategyBackoff;
        Duration maxBackoff = strategyBackoff.plus(FIVE_SECONDS);

        Duration backoff = optionalSuggested.get();

        backoff = maxOf(minBackoff, backoff);
        backoff = minOf(maxBackoff, backoff);

        return backoff;
    }

    private boolean isRetries2026Enabled() {
        return Boolean.TRUE.equals(retries2026Enabled);
    }

    public static class Builder extends BaseRetryStrategy.Builder implements StandardRetryStrategy.Builder {
        private Boolean retries2026Enabled;

        Builder() {
        }

        Builder(DefaultStandardRetryStrategy strategy) {
            super(strategy);
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
        public Builder treatAsThrottling(Predicate<Throwable> treatAsThrottling) {
            setTreatAsThrottling(treatAsThrottling);
            return this;
        }

        @Override
        public Builder circuitBreakerEnabled(Boolean circuitBreakerEnabled) {
            setCircuitBreakerEnabled(circuitBreakerEnabled);
            return this;
        }

        public Builder tokenBucketExceptionCost(int exceptionCost) {
            setTokenBucketExceptionCost(exceptionCost);
            return this;
        }

        public Builder throttlingTokenBucketExceptionCost(int throttlingExceptionCost) {
            setThrottlingTokenBucketExceptionCost(throttlingExceptionCost);
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

        /**
         * Whether retries 2.1 behavior is enabled.
         */
        public Builder retries2026Enabled(Boolean retries2026Enabled) {
            this.retries2026Enabled = retries2026Enabled;
            return this;
        }

        @Override
        public StandardRetryStrategy build() {
            return new DefaultStandardRetryStrategy(this);
        }
    }
}
