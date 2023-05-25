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

package software.amazon.awssdk.retries;

import java.util.function.Predicate;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.retries.internal.DefaultLegacyRetryStrategy2;
import software.amazon.awssdk.retries.internal.circuitbreaker.TokenBucketStore;

/**
 * The legacy retry strategy is a {@link RetryStrategy} for normal use-cases.
 * <p>
 * The legacy retry strategy by default:
 * <ol>
 *     <li>Retries on the conditions configured in the {@link Builder}.
 *     <li>Retries 3 times (4 total attempts). Adjust with {@link Builder#maxAttempts(int)}
 *     <li>For non-throttling exceptions uses the {@link BackoffStrategy#exponentialDelay} backoff strategy, with a base delay
 *     of 100 milliseconds and max delay of 20 seconds. Adjust with {@link Builder#backoffStrategy}
 *     <li>For throttling exceptions uses the {@link BackoffStrategy#exponentialDelay} backoff strategy, with a base delay of
 *     500 milliseconds  and max delay of 20 seconds. Adjust with {@link LegacyRetryStrategy.Builder#throttlingBackoffStrategy}
 *     <li>Circuit breaking (disabling retries) in the event of high downstream failures across the scope of
 *     the strategy. The circuit breaking will never prevent a successful first attempt. Adjust with
 *     {@link Builder#circuitBreakerEnabled}
 *     <li>The state of the circuit breaker is not affected by throttling exceptions
 * </ol>
 *
 * @see StandardRetryStrategy
 * @see AdaptiveRetryStrategy
 */
@SdkPublicApi
@ThreadSafe
public interface LegacyRetryStrategy extends RetryStrategy<LegacyRetryStrategy.Builder, LegacyRetryStrategy> {
    /**
     * Create a new {@link LegacyRetryStrategy.Builder}.
     *
     * <p>Example Usage
     * <pre>
     * LegacyRetryStrategy retryStrategy =
     *     LegacyRetryStrategy.builder()
     *                          .retryOnExceptionInstanceOf(IllegalArgumentException.class)
     *                          .retryOnExceptionInstanceOf(IllegalStateException.class)
     *                          .build();
     * </pre>
     */
    static Builder builder() {
        return DefaultLegacyRetryStrategy2
            .builder()
            .maxAttempts(DefaultRetryStrategy.Legacy.MAX_ATTEMPTS)
            .tokenBucketStore(TokenBucketStore
                                  .builder()
                                  .tokenBucketMaxCapacity(DefaultRetryStrategy.Legacy.TOKEN_BUCKET_SIZE)
                                  .build())
            .tokenBucketExceptionCost(DefaultRetryStrategy.Legacy.DEFAULT_EXCEPTION_TOKEN_COST)
            .tokenBucketThrottlingExceptionCost(DefaultRetryStrategy.Legacy.THROTTLE_EXCEPTION_TOKEN_COST);
    }

    @Override
    Builder toBuilder();

    interface Builder extends RetryStrategy.Builder<Builder, LegacyRetryStrategy> {
        /**
         * Configure the backoff strategy used by this strategy.
         *
         * <p>By default, this uses jittered exponential backoff.
         */
        Builder backoffStrategy(BackoffStrategy backoffStrategy);

        /**
         * Configure the backoff strategy used for throttling exceptions by this strategy.
         *
         * <p>By default, this uses jittered exponential backoff.
         */
        Builder throttlingBackoffStrategy(BackoffStrategy throttlingBackoffStrategy);

        /**
         * Whether circuit breaking is enabled for this strategy.
         *
         * <p>The circuit breaker will prevent attempts (even below the {@link #maxAttempts(int)}) if a large number of
         * failures are observed by this executor.
         *
         * <p>Note: The circuit breaker scope is local to the created {@link RetryStrategy},
         * and will therefore not be effective unless the {@link RetryStrategy} is used for more than one call. It's recommended
         * that a {@link RetryStrategy} be reused for all calls to a single unreliable resource. It's also recommended that
         * separate {@link RetryStrategy}s be used for calls to unrelated resources.
         *
         * <p>By default, this is {@code true}.
         */
        Builder circuitBreakerEnabled(Boolean circuitBreakerEnabled);

        /**
         * Configure the predicate to allow the strategy categorize a Throwable as throttling exception.
         */
        Builder treatAsThrottling(Predicate<Throwable> treatAsThrottling);

        @Override
        LegacyRetryStrategy build();
    }
}
