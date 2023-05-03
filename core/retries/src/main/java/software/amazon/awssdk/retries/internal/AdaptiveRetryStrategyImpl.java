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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.retries.AdaptiveRetryStrategy;
import software.amazon.awssdk.retries.api.AcquireInitialTokenRequest;
import software.amazon.awssdk.retries.api.AcquireInitialTokenResponse;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.retries.api.RecordSuccessRequest;
import software.amazon.awssdk.retries.api.RecordSuccessResponse;
import software.amazon.awssdk.retries.api.RefreshRetryTokenRequest;
import software.amazon.awssdk.retries.api.RefreshRetryTokenResponse;
import software.amazon.awssdk.retries.api.RetryToken;
import software.amazon.awssdk.retries.api.TokenAcquisitionFailedException;
import software.amazon.awssdk.retries.api.internal.RefreshRetryTokenResponseImpl;
import software.amazon.awssdk.retries.internal.circuitbreaker.AcquireResponse;
import software.amazon.awssdk.retries.internal.circuitbreaker.ReleaseResponse;
import software.amazon.awssdk.retries.internal.circuitbreaker.TokenBucket;
import software.amazon.awssdk.retries.internal.circuitbreaker.TokenBucketStore;
import software.amazon.awssdk.retries.internal.ratelimiter.RateLimiterAcquireResponse;
import software.amazon.awssdk.retries.internal.ratelimiter.RateLimiterTokenBucket;
import software.amazon.awssdk.retries.internal.ratelimiter.RateLimiterTokenBucketStore;
import software.amazon.awssdk.retries.internal.ratelimiter.RateLimiterUpdateResponse;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * Implementation of the {@link AdaptiveRetryStrategy} interface.
 */
@SdkInternalApi
public final class AdaptiveRetryStrategyImpl implements AdaptiveRetryStrategy {
    private static final Logger LOG = Logger.loggerFor(AdaptiveRetryStrategyImpl.class);
    private final List<Predicate<Throwable>> retryPredicates;
    private final int maxAttempts;
    private final boolean circuitBreakerEnabled;
    private final BackoffStrategy backoffStrategy;
    private final int tokenBucketMaxCapacity;
    private final int exceptionCost;
    private final Predicate<Throwable> treatAsThrottling;
    private final TokenBucketStore tokenBucketStore;
    private final RateLimiterTokenBucketStore rateLimiterTokenBucketStore;

    private AdaptiveRetryStrategyImpl(Builder builder) {
        this.retryPredicates = Collections.unmodifiableList(Validate.paramNotNull(builder.retryPredicates, "predicates"));
        this.maxAttempts = Validate.isPositive(builder.maxAttempts, "maxAttempts");
        this.circuitBreakerEnabled = builder.circuitBreakerEnabled;
        this.backoffStrategy = Validate.paramNotNull(builder.backoffStrategy, "backoffStrategy");
        this.exceptionCost = builder.exceptionCost;
        this.tokenBucketMaxCapacity = builder.tokenBucketMaxCapacity;
        this.treatAsThrottling = Validate.paramNotNull(builder.treatAsThrottling, "treatAsThrottling");
        this.tokenBucketStore = Validate.paramNotNull(builder.tokenBucketStore, "tokenBucketStore");
        this.rateLimiterTokenBucketStore = Validate.paramNotNull(builder.rateLimiterTokenBucketStore,
                                                                 "rateLimiterTokenBucketStore");
    }

    @Override
    public AcquireInitialTokenResponse acquireInitialToken(AcquireInitialTokenRequest request) {
        logAcquireInitialToken(request);
        return AcquireInitialTokenResponse.create(
            DefaultRetryToken.builder().scope(request.scope()).build(), Duration.ZERO);
    }

    @Override
    public RefreshRetryTokenResponse refreshRetryToken(RefreshRetryTokenRequest request) {
        DefaultRetryToken token = asStandardRetryToken(request.token());
        AcquireResponse acquireResponse = requestAcquireCapacity(request, token);

        // Check if we meet the preconditions needed for retrying. These will throw if the expected condition is not meet.
        // 1) is retryable?
        throwOnNonRetryableException(request, acquireResponse);
        // 2) max attempts reached?
        throwOnMaxAttemptsReached(request, acquireResponse);
        // 3) can we acquire a token?
        throwOnAcquisitionFailure(request, acquireResponse);

        // All the conditions required to retry were meet, update the send rate if the error is categorized as throttling.
        Throwable failure = request.failure();
        RateLimiterTokenBucket rateLimiterTokenBucket = rateLimiterTokenBucketStore.tokenBucketForScope(token.scope());
        if (this.treatAsThrottling.test(failure)) {
            rateLimiterTokenBucket.updateRateAfterThrottling();
        }

        // Refresh the retry token and compute the backoff delay.
        DefaultRetryToken refreshedToken = refreshToken(request, acquireResponse);
        Duration backoff = backoffStrategy.computeDelay(refreshedToken.attempt());

        // Acquire capacity from the adaptive token.
        RateLimiterAcquireResponse rateLimiterAcquireResponse = rateLimiterTokenBucket.tryAcquire();

        // Take the max delay between the suggested delay, the backoff delay and the delay of the adaptive strategy.
        Duration adaptiveDelay = rateLimiterAcquireResponse.delay();
        Duration suggested = request.suggestedDelay().orElse(Duration.ZERO);
        Duration finalDelay = maxOf(suggested, backoff).plus(adaptiveDelay);

        logRefreshTokenSuccess(refreshedToken, acquireResponse, finalDelay);
        return RefreshRetryTokenResponseImpl.create(refreshedToken, finalDelay);
    }

    @Override
    public RecordSuccessResponse recordSuccess(RecordSuccessRequest request) {
        DefaultRetryToken token = asStandardRetryToken(request.token());

        // Update the adaptive token bucket.
        updateAdaptiveTokenBucket(token);

        // Update the circuit breaker token bucket.
        ReleaseResponse releaseResponse = updateCircuitBreakerTokenBucket(token);

        // Refresh the retry token and return
        DefaultRetryToken refreshedToken = refreshRetryTokenAfterSuccess(token, releaseResponse);

        // Log success and return.
        logRecordSuccess(token, releaseResponse);
        return RecordSuccessResponse.create(refreshedToken);
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    /**
     * Returns a builder to fine-tune this retry strategy.
     *
     * @return a builder for this retry strategy.
     */
    public static Builder builder() {
        return new Builder();
    }

    private Duration maxOf(Duration left, Duration right) {
        if (left.compareTo(right) >= 0) {
            return left;
        }
        return right;
    }

    private RateLimiterUpdateResponse updateAdaptiveTokenBucket(DefaultRetryToken token) {
        RateLimiterTokenBucket rateLimiterTokenBucket = rateLimiterTokenBucketStore.tokenBucketForScope(token.scope());
        return rateLimiterTokenBucket.updateRateAfterSuccess();
    }

    private ReleaseResponse updateCircuitBreakerTokenBucket(DefaultRetryToken token) {
        TokenBucket bucket = tokenBucketStore.tokenBucketForScope(token.scope());
        int capacityReleased = token.capacityAcquired();
        return bucket.release(capacityReleased);
    }

    private DefaultRetryToken refreshRetryTokenAfterSuccess(DefaultRetryToken token, ReleaseResponse releaseResponse) {
        return token.toBuilder()
                    .capacityAcquired(0)
                    .capacityRemaining(releaseResponse.currentCapacity())
                    .state(DefaultRetryToken.TokenState.SUCCEEDED)
                    .build();
    }

    private void throwOnAcquisitionFailure(RefreshRetryTokenRequest request, AcquireResponse acquireResponse) {
        DefaultRetryToken token = asStandardRetryToken(request.token());
        if (acquireResponse.acquisitionFailed()) {
            Throwable failure = request.failure();
            DefaultRetryToken refreshedToken =
                token.toBuilder()
                     .capacityRemaining(acquireResponse.capacityRemaining())
                     .capacityAcquired(acquireResponse.capacityAcquired())
                     .state(DefaultRetryToken.TokenState.TOKEN_ACQUISITION_FAILED)
                     .addFailure(failure)
                     .build();
            String message = acquisitionFailedMessage(acquireResponse);
            LOG.error(() -> message, failure);
            throw new TokenAcquisitionFailedException(message, refreshedToken, failure);
        }
    }

    private void throwOnMaxAttemptsReached(RefreshRetryTokenRequest request, AcquireResponse acquireResponse) {
        DefaultRetryToken token = asStandardRetryToken(request.token());
        if (maxAttemptsReached(token)) {
            Throwable failure = request.failure();
            DefaultRetryToken refreshedToken =
                token.toBuilder()
                     .capacityRemaining(acquireResponse.capacityRemaining())
                     .capacityAcquired(acquireResponse.capacityAcquired())
                     .state(DefaultRetryToken.TokenState.MAX_RETRIES_REACHED)
                     .addFailure(failure)
                     .build();
            String message = maxAttemptsReachedMessage(refreshedToken);
            LOG.error(() -> message, failure);
            throw new TokenAcquisitionFailedException(message, refreshedToken, failure);
        }
    }

    private void throwOnNonRetryableException(RefreshRetryTokenRequest request, AcquireResponse acquireResponse) {
        DefaultRetryToken token = asStandardRetryToken(request.token());
        Throwable failure = request.failure();
        if (isNonRetryableException(request)) {
            String message = nonRetryableExceptionMessage(token);
            LOG.error(() -> message, failure);
            DefaultRetryToken refreshedToken =
                token.toBuilder()
                     .capacityRemaining(acquireResponse.capacityRemaining())
                     .capacityAcquired(acquireResponse.capacityAcquired())
                     .state(DefaultRetryToken.TokenState.NON_RETRYABLE_EXCEPTION)
                     .addFailure(failure)
                     .build();
            throw new TokenAcquisitionFailedException(message, refreshedToken, failure);
        }
        int attempt = token.attempt();
        LOG.warn(() -> String.format("Request attempt %d encountered retryable failure.", attempt), failure);
    }

    private String nonRetryableExceptionMessage(DefaultRetryToken token) {
        return String.format("Request attempt %d encountered non-retryable failure", token.attempt());
    }

    private String maxAttemptsReachedMessage(DefaultRetryToken token) {
        return String.format("Request will not be retried. Retries have been exhausted "
                             + "(cost: 0, capacity: %d/%d)",
                             token.capacityAcquired(),
                             token.capacityRemaining());
    }

    private String acquisitionFailedMessage(AcquireResponse acquireResponse) {
        return String.format("Request will not be retried to protect the caller and downstream service. "
                             + "The cost of retrying (%d) "
                             + "exceeds the available retry capacity (%d/%d).",
                             acquireResponse.capacityRequested(),
                             acquireResponse.capacityRemaining(),
                             acquireResponse.maxCapacity());
    }

    private void logAcquireInitialToken(AcquireInitialTokenRequest request) {
        // Request attempt 1 token acquired (backoff: 0ms, cost: 0, capacity: 500/500)
        TokenBucket tokenBucket = tokenBucketStore.tokenBucketForScope(request.scope());
        LOG.debug(() -> String.format("Request attempt 1 token acquired "
                                      + "(backoff: 0ms, cost: 0, capacity: %d/%d)",
                                      tokenBucket.currentCapacity(), tokenBucket.maxCapacity()));
    }

    private void logRefreshTokenSuccess(DefaultRetryToken token, AcquireResponse acquireResponse, Duration delay) {
        LOG.debug(() -> String.format("Request attempt %d token acquired "
                                      + "(backoff: %dms, cost: %d, capacity: %d/%d)",
                                      token.attempt(), delay.toMillis(),
                                      acquireResponse.capacityAcquired(),
                                      acquireResponse.capacityRemaining(),
                                      acquireResponse.maxCapacity()));
    }

    private void logRecordSuccess(DefaultRetryToken token, ReleaseResponse release) {
        LOG.debug(() -> String.format("Request attempt %d succeeded (cost: -%d, capacity: %d/%d)",
                                      token.attempt(), release.capacityReleased(),
                                      release.currentCapacity(), release.maxCapacity()));

    }

    private boolean maxAttemptsReached(DefaultRetryToken token) {
        return token.attempt() >= maxAttempts;
    }

    private boolean isNonRetryableException(RefreshRetryTokenRequest request) {
        Throwable failure = request.failure();
        for (Predicate<Throwable> retryPredicate : retryPredicates) {
            if (retryPredicate.test(failure)) {
                return false;
            }
        }
        return true;
    }

    static DefaultRetryToken asStandardRetryToken(RetryToken token) {
        return Validate.isInstanceOf(DefaultRetryToken.class, token,
                                     "RetryToken is of unexpected class (%s), "
                                     + "This token was not created by this retry strategy.",
                                     token.getClass().getName());
    }

    private AcquireResponse requestAcquireCapacity(RefreshRetryTokenRequest request, DefaultRetryToken token) {
        TokenBucket tokenBucket = tokenBucketStore.tokenBucketForScope(token.scope());
        return tokenBucket.tryAcquire(exceptionCost);
    }

    private DefaultRetryToken refreshToken(RefreshRetryTokenRequest request, AcquireResponse acquireResponse) {
        DefaultRetryToken token = asStandardRetryToken(request.token());
        return token.toBuilder()
                    .increaseAttempt()
                    .state(DefaultRetryToken.TokenState.IN_PROGRESS)
                    .capacityAcquired(acquireResponse.capacityAcquired())
                    .capacityRemaining(acquireResponse.capacityRemaining())
                    .addFailure(request.failure())
                    .build();
    }

    public static class Builder implements AdaptiveRetryStrategy.Builder {
        private List<Predicate<Throwable>> retryPredicates;
        private int maxAttempts;
        private boolean circuitBreakerEnabled;
        private int tokenBucketMaxCapacity;
        private int exceptionCost;
        private Predicate<Throwable> treatAsThrottling;
        private BackoffStrategy backoffStrategy;
        private TokenBucketStore tokenBucketStore;
        private RateLimiterTokenBucketStore rateLimiterTokenBucketStore;

        Builder() {
            retryPredicates = new ArrayList<>();
        }

        Builder(AdaptiveRetryStrategyImpl strategy) {
            this.retryPredicates = new ArrayList<>(strategy.retryPredicates);
            this.maxAttempts = strategy.maxAttempts;
            this.circuitBreakerEnabled = strategy.circuitBreakerEnabled;
            this.tokenBucketMaxCapacity = strategy.tokenBucketMaxCapacity;
            this.exceptionCost = strategy.exceptionCost;
            this.treatAsThrottling = strategy.treatAsThrottling;
            this.backoffStrategy = strategy.backoffStrategy;
            this.tokenBucketStore = strategy.tokenBucketStore;
            this.rateLimiterTokenBucketStore = strategy.rateLimiterTokenBucketStore;
        }

        @Override
        public Builder retryOnException(Predicate<Throwable> shouldRetry) {
            this.retryPredicates.add(shouldRetry);
            return this;
        }

        @Override
        public Builder maxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        @Override
        public Builder treatAsThrottling(Predicate<Throwable> treatAsThrottling) {
            this.treatAsThrottling = treatAsThrottling;
            return this;
        }

        public Builder tokenBucketStore(TokenBucketStore tokenBucketStore) {
            this.tokenBucketStore = tokenBucketStore;
            return this;
        }

        public Builder rateLimiterTokenBucketStore(RateLimiterTokenBucketStore rateLimiterTokenBucketStore) {
            this.rateLimiterTokenBucketStore = rateLimiterTokenBucketStore;
            return this;
        }

        public Builder backoffStrategy(BackoffStrategy backoffStrategy) {
            this.backoffStrategy = backoffStrategy;
            return this;
        }

        public Builder tokenBucketExceptionCost(int exceptionCost) {
            this.exceptionCost = exceptionCost;
            return this;
        }

        @Override
        public AdaptiveRetryStrategyImpl build() {
            return new AdaptiveRetryStrategyImpl(this);
        }
    }
}
