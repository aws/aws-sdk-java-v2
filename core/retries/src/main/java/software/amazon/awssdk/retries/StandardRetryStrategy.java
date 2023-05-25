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

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.retries.internal.DefaultStandardRetryStrategy2;
import software.amazon.awssdk.retries.internal.circuitbreaker.TokenBucketStore;

/**
 * The standard retry strategy is the recommended {@link RetryStrategy} for normal use-cases.
 * <p>
 * Unlike {@link AdaptiveRetryStrategy}, the standard strategy is generally useful across all retry use-cases.
 * <p>
 * The standard retry strategy by default:
 * <ol>
 *     <li>Retries on the conditions configured in the {@link Builder}.
 *     <li>Retries 2 times (3 total attempts). Adjust with {@link Builder#maxAttempts(int)}
 *     <li>Uses the {@link BackoffStrategy#exponentialDelay} backoff strategy, with a base delay of
 *     1 second and max delay of 20 seconds. Adjust with {@link Builder#backoffStrategy}
 *     <li>Circuit breaking (disabling retries) in the event of high downstream failures across the scope of
 *     the strategy. The circuit breaking will never prevent a successful first attempt. Adjust with
 *     {@link Builder#circuitBreakerEnabled}.
 * </ol>
 *
 * @see AdaptiveRetryStrategy
 */
@SdkPublicApi
@ThreadSafe
public interface StandardRetryStrategy extends RetryStrategy<StandardRetryStrategy.Builder, StandardRetryStrategy> {
    /**
     * Create a new {@link StandardRetryStrategy.Builder}.
     *
     * <p>Example Usage
     * <pre>
     * StandardRetryStrategy retryStrategy =
     *     StandardRetryStrategy.builder()
     *                          .retryOnExceptionInstanceOf(IllegalArgumentException.class)
     *                          .retryOnExceptionInstanceOf(IllegalStateException.class)
     *                          .build();
     * </pre>
     */
    static Builder builder() {
        return DefaultStandardRetryStrategy2
            .builder()
            .maxAttempts(DefaultRetryStrategy.Standard.MAX_ATTEMPTS)
            .tokenBucketStore(TokenBucketStore
                                  .builder()
                                  .tokenBucketMaxCapacity(DefaultRetryStrategy.Standard.TOKEN_BUCKET_SIZE)
                                  .build())
            .tokenBucketExceptionCost(DefaultRetryStrategy.Standard.DEFAULT_EXCEPTION_TOKEN_COST);
    }

    @Override
    Builder toBuilder();

    interface Builder extends RetryStrategy.Builder<Builder, StandardRetryStrategy> {
        /**
         * Configure the backoff strategy used by this executor.
         *
         * <p>By default, this uses jittered exponential backoff.
         */
        Builder backoffStrategy(BackoffStrategy backoffStrategy);

        /**
         * Whether circuit breaking is enabled for this executor.
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

        @Override
        StandardRetryStrategy build();
    }
}
