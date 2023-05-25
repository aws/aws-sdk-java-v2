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
import software.amazon.awssdk.retries.api.AcquireInitialTokenRequest;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.retries.internal.DefaultAdaptiveRetryStrategy2;
import software.amazon.awssdk.retries.internal.circuitbreaker.TokenBucketStore;
import software.amazon.awssdk.retries.internal.ratelimiter.RateLimiterTokenBucketStore;

/**
 * The adaptive retry strategy is a {@link RetryStrategy} when executing against a very resource-constrained set of resources.
 * <p>
 * Unlike {@link StandardRetryStrategy}, care should be taken when using this strategy. Specifically, it should be used:
 * <ol>
 * <li>When the availability of downstream resources are mostly affected by callers that are also using
 * the {@link AdaptiveRetryStrategy}.
 * <li>The scope (either the whole strategy or the {@link AcquireInitialTokenRequest#scope}) of the strategy is constrained
 * to target "resource", so that availability issues in one resource cannot delay other, unrelated resource's availability.
 * <p>
 * The adaptive retry strategy by default:
 * <ol>
 *     <li>Retries on the conditions configured in the {@link Builder}.
 *     <li>Retries 2 times (3 total attempts). Adjust with {@link Builder#maxAttempts}
 *     <li>Uses a dynamic backoff delay based on load currently perceived against the downstream resource
 *     <li>Circuit breaking (disabling retries) in the event of high downstream failures within an individual scope.
 *     Circuit breaking may prevent a first attempt in outage scenarios to protect the downstream service.
 * </ol>
 *
 * @see StandardRetryStrategy
 */
@SdkPublicApi
@ThreadSafe
public interface AdaptiveRetryStrategy extends RetryStrategy<AdaptiveRetryStrategy.Builder, AdaptiveRetryStrategy> {

    /**
     * Create a new {@link AdaptiveRetryStrategy.Builder}.
     *
     * <p>Example Usage
     * <pre>
     * AdaptiveRetryStrategy retryStrategy =
     *     AdaptiveRetryStrategy.builder()
     *                          .retryOnExceptionInstanceOf(IllegalArgumentException.class)
     *                          .retryOnExceptionInstanceOf(IllegalStateException.class)
     *                          .build();
     * </pre>
     */
    static AdaptiveRetryStrategy.Builder builder() {
        return DefaultAdaptiveRetryStrategy2
            .builder()
            .maxAttempts(DefaultRetryStrategy.Adaptive.MAX_ATTEMPTS)
            .tokenBucketStore(TokenBucketStore.builder()
                                              .tokenBucketMaxCapacity(DefaultRetryStrategy.Standard.TOKEN_BUCKET_SIZE)
                                              .build())
            .tokenBucketExceptionCost(DefaultRetryStrategy.Standard.TOKEN_BUCKET_SIZE)
            .backoffStrategy(BackoffStrategy.exponentialDelay(DefaultRetryStrategy.Standard.BASE_DELAY,
                                                              DefaultRetryStrategy.Standard.MAX_BACKOFF))
            .rateLimiterTokenBucketStore(RateLimiterTokenBucketStore.builder().build());
    }

    @Override
    Builder toBuilder();

    interface Builder extends RetryStrategy.Builder<Builder, AdaptiveRetryStrategy> {
        /**
         * Configure the predicate to allow the strategy categorize a Throwable as throttling exception.
         */
        Builder treatAsThrottling(Predicate<Throwable> treatAsThrottling);

        @Override
        AdaptiveRetryStrategy build();
    }
}
