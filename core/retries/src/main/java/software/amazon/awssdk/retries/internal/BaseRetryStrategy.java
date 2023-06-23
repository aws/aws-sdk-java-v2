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
import software.amazon.awssdk.retries.api.AcquireInitialTokenRequest;
import software.amazon.awssdk.retries.api.AcquireInitialTokenResponse;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.retries.api.RecordSuccessRequest;
import software.amazon.awssdk.retries.api.RecordSuccessResponse;
import software.amazon.awssdk.retries.api.RefreshRetryTokenRequest;
import software.amazon.awssdk.retries.api.RefreshRetryTokenResponse;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.retries.api.RetryToken;
import software.amazon.awssdk.retries.api.TokenAcquisitionFailedException;
import software.amazon.awssdk.retries.api.internal.RefreshRetryTokenResponseImpl;
import software.amazon.awssdk.retries.internal.circuitbreaker.AcquireResponse;
import software.amazon.awssdk.retries.internal.circuitbreaker.ReleaseResponse;
import software.amazon.awssdk.retries.internal.circuitbreaker.TokenBucket;
import software.amazon.awssdk.retries.internal.circuitbreaker.TokenBucketStore;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Generic class that implements that common logic for all the retries
 * strategies with extension points for specific strategies to tailor
 * the behavior to its needs.
 */
@SdkInternalApi
public abstract class BaseRetryStrategy<
    B extends CopyableBuilder<B, T> & RetryStrategy.Builder<B, T>,
    T extends ToCopyableBuilder<B, T> & RetryStrategy<B, T>> implements RetryStrategy<B, T> {

    protected final Logger log;
    protected final List<Predicate<Throwable>> retryPredicates;
    protected final int maxAttempts;
    protected final boolean circuitBreakerEnabled;
    protected final BackoffStrategy backoffStrategy;
    protected final int exceptionCost;
    protected final TokenBucketStore tokenBucketStore;

    BaseRetryStrategy(Logger log, Builder builder) {
        this.log = log;
        this.retryPredicates = Collections.unmodifiableList(Validate.paramNotNull(builder.retryPredicates, "retryPredicates"));
        this.maxAttempts = Validate.isPositive(builder.maxAttempts, "maxAttempts");
        this.circuitBreakerEnabled = builder.circuitBreakerEnabled == null || builder.circuitBreakerEnabled;
        this.backoffStrategy = Validate.paramNotNull(builder.backoffStrategy, "backoffStrategy");
        this.exceptionCost = Validate.paramNotNull(builder.exceptionCost, "exceptionCost");
        this.tokenBucketStore = Validate.paramNotNull(builder.tokenBucketStore, "tokenBucketStore");
    }

    /**
     * This method implements the logic of {@link
     * RetryStrategy#acquireInitialToken(AcquireInitialTokenRequest)}.
     *
     * @see RetryStrategy#acquireInitialToken(AcquireInitialTokenRequest)
     */
    @Override
    public final AcquireInitialTokenResponse acquireInitialToken(AcquireInitialTokenRequest request) {
        logAcquireInitialToken(request);
        DefaultRetryToken token = DefaultRetryToken.builder().scope(request.scope()).build();
        return AcquireInitialTokenResponse.create(token, computeInitialBackoff(request));
    }

    /**
     * This method implements the logic of  {@link
     * RetryStrategy#refreshRetryToken(RefreshRetryTokenRequest)}.
     *
     * @see RetryStrategy#refreshRetryToken(RefreshRetryTokenRequest)
     */
    @Override
    public final RefreshRetryTokenResponse refreshRetryToken(RefreshRetryTokenRequest request) {
        DefaultRetryToken token = asDefaultRetryToken(request.token());

        // Check if we meet the preconditions needed for retrying. These will throw if the expected condition is not meet.
        // 1) is retryable?
        throwOnNonRetryableException(request);

        // 2) max attempts reached?
        throwOnMaxAttemptsReached(request);

        // 3) can we acquire a token?
        AcquireResponse acquireResponse = requestAcquireCapacity(request, token);
        throwOnAcquisitionFailure(request, acquireResponse);

        // All the conditions required to retry were meet, update the internal state before retrying.
        updateStateForRetry(request);

        // Refresh the retry token and compute the backoff delay.
        DefaultRetryToken refreshedToken = refreshToken(request, acquireResponse);
        Duration backoff = computeBackoff(request, refreshedToken);

        logRefreshTokenSuccess(refreshedToken, acquireResponse, backoff);
        return RefreshRetryTokenResponseImpl.create(refreshedToken, backoff);
    }

    /**
     * This method implements the logic of {@link
     * RetryStrategy#recordSuccess(RecordSuccessRequest)}.
     *
     * @see RetryStrategy#recordSuccess(RecordSuccessRequest)
     */
    @Override
    public final RecordSuccessResponse recordSuccess(RecordSuccessRequest request) {
        DefaultRetryToken token = asDefaultRetryToken(request.token());

        // Update the circuit breaker token bucket.
        ReleaseResponse releaseResponse = releaseTokenBucketCapacity(token);

        // Refresh the retry token and return.
        DefaultRetryToken refreshedToken = refreshRetryTokenAfterSuccess(token, releaseResponse);

        // Update the state for the specific retry strategy.
        updateStateForSuccess(token);

        // Log success and return.
        logRecordSuccess(token, releaseResponse);
        return RecordSuccessResponse.create(refreshedToken);
    }

    @Override
    public int maxAttempts() {
        return maxAttempts;
    }

    @Override
    public abstract B toBuilder();


    /**
     * Computes the backoff before the first attempt, by default
     * {@link Duration#ZERO}. Extending classes can override
     * this method to compute different a different depending on their
     * logic.
     */
    protected Duration computeInitialBackoff(AcquireInitialTokenRequest request) {
        return Duration.ZERO;
    }

    /**
     * Computes the backoff before a retry using the configured
     * backoff strategy. Extending classes can override
     * this method to compute different a different depending on their
     * logic.
     */
    protected Duration computeBackoff(RefreshRetryTokenRequest request, DefaultRetryToken token) {
        Duration backoff = backoffStrategy.computeDelay(token.attempt());
        Duration suggested = request.suggestedDelay().orElse(Duration.ZERO);
        return maxOf(suggested, backoff);
    }

    /**
     * Called inside {@link #recordSuccess} to allow extending classes
     * to update their internal state after a successful request.
     */
    protected void updateStateForSuccess(DefaultRetryToken token) {
    }

    /**
     * Called inside {@link #refreshRetryToken} to allow extending
     * classes to update their internal state before retrying a
     * request.
     */
    protected void updateStateForRetry(RefreshRetryTokenRequest request) {
    }

    /**
     * Returns the amount of tokens to withdraw from the token
     * bucket. Extending classes can override this method to tailor
     * this amount for the specific kind of failure.
     */
    protected int exceptionCost(RefreshRetryTokenRequest request) {
        if (circuitBreakerEnabled) {
            return exceptionCost;
        }
        return 0;
    }

    private DefaultRetryToken refreshToken(RefreshRetryTokenRequest request, AcquireResponse acquireResponse) {
        DefaultRetryToken token = asDefaultRetryToken(request.token());
        return token.toBuilder()
                    .increaseAttempt()
                    .state(DefaultRetryToken.TokenState.IN_PROGRESS)
                    .capacityAcquired(acquireResponse.capacityAcquired())
                    .capacityRemaining(acquireResponse.capacityRemaining())
                    .addFailure(request.failure())
                    .build();
    }

    private AcquireResponse requestAcquireCapacity(RefreshRetryTokenRequest request, DefaultRetryToken token) {
        TokenBucket tokenBucket = tokenBucketStore.tokenBucketForScope(token.scope());
        return tokenBucket.tryAcquire(exceptionCost(request));
    }

    private ReleaseResponse releaseTokenBucketCapacity(DefaultRetryToken token) {
        TokenBucket bucket = tokenBucketStore.tokenBucketForScope(token.scope());
        int capacityReleased = token.capacityAcquired();
        return bucket.release(capacityReleased);
    }

    private DefaultRetryToken refreshRetryTokenAfterSuccess(DefaultRetryToken token, ReleaseResponse releaseResponse) {
        return token.toBuilder()
                    .capacityRemaining(releaseResponse.currentCapacity())
                    .state(DefaultRetryToken.TokenState.SUCCEEDED)
                    .build();
    }

    private void throwOnMaxAttemptsReached(RefreshRetryTokenRequest request) {
        DefaultRetryToken token = asDefaultRetryToken(request.token());
        if (maxAttemptsReached(token)) {
            Throwable failure = request.failure();
            TokenBucket tokenBucket = tokenBucketStore.tokenBucketForScope(token.scope());
            DefaultRetryToken refreshedToken =
                token.toBuilder()
                     .capacityRemaining(tokenBucket.currentCapacity())
                     .state(DefaultRetryToken.TokenState.MAX_RETRIES_REACHED)
                     .addFailure(failure)
                     .build();
            String message = maxAttemptsReachedMessage(refreshedToken);
            log.debug(() -> message, failure);
            throw new TokenAcquisitionFailedException(message, refreshedToken, failure);
        }
    }

    private void throwOnNonRetryableException(RefreshRetryTokenRequest request) {
        DefaultRetryToken token = asDefaultRetryToken(request.token());
        Throwable failure = request.failure();
        if (isNonRetryableException(request)) {
            String message = nonRetryableExceptionMessage(token);
            log.error(() -> message, failure);
            TokenBucket tokenBucket = tokenBucketStore.tokenBucketForScope(token.scope());
            DefaultRetryToken refreshedToken =
                token.toBuilder()
                     .capacityRemaining(tokenBucket.currentCapacity())
                     .state(DefaultRetryToken.TokenState.NON_RETRYABLE_EXCEPTION)
                     .addFailure(failure)
                     .build();
            throw new TokenAcquisitionFailedException(message, refreshedToken, failure);
        }
        int attempt = token.attempt();
        log.debug(() -> String.format("Request attempt %d encountered retryable failure.", attempt), failure);
    }

    private void throwOnAcquisitionFailure(RefreshRetryTokenRequest request, AcquireResponse acquireResponse) {
        DefaultRetryToken token = asDefaultRetryToken(request.token());
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
            log.debug(() -> message, failure);
            throw new TokenAcquisitionFailedException(message, refreshedToken, failure);
        }
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

    private String acquisitionFailedMessage(AcquireResponse response) {
        return String.format("Request will not be retried to protect the caller and downstream service. "
                             + "The cost of retrying (%d) "
                             + "exceeds the available retry capacity (%d/%d).",
                             response.capacityRequested(),
                             response.capacityRemaining(),
                             response.maxCapacity());
    }

    private void logAcquireInitialToken(AcquireInitialTokenRequest request) {
        // Request attempt 1 token acquired (backoff: 0ms, cost: 0, capacity: 500/500)
        TokenBucket tokenBucket = tokenBucketStore.tokenBucketForScope(request.scope());
        log.debug(() -> String.format("Request attempt 1 token acquired "
                                      + "(backoff: 0ms, cost: 0, capacity: %d/%d)",
                                      tokenBucket.currentCapacity(), tokenBucket.maxCapacity()));
    }

    private void logRefreshTokenSuccess(DefaultRetryToken token, AcquireResponse acquireResponse, Duration delay) {
        log.debug(() -> String.format("Request attempt %d token acquired "
                                      + "(backoff: %dms, cost: %d, capacity: %d/%d)",
                                      token.attempt(), delay.toMillis(),
                                      acquireResponse.capacityAcquired(),
                                      acquireResponse.capacityRemaining(),
                                      acquireResponse.maxCapacity()));
    }

    private void logRecordSuccess(DefaultRetryToken token, ReleaseResponse release) {
        log.debug(() -> String.format("Request attempt %d succeeded (cost: -%d, capacity: %d/%d)",
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

    static Duration maxOf(Duration left, Duration right) {
        if (left.compareTo(right) >= 0) {
            return left;
        }
        return right;
    }

    static DefaultRetryToken asDefaultRetryToken(RetryToken token) {
        return Validate.isInstanceOf(DefaultRetryToken.class, token,
                                     "RetryToken is of unexpected class (%s), "
                                     + "This token was not created by this retry strategy.",
                                     token.getClass().getName());
    }

    static class Builder {
        private List<Predicate<Throwable>> retryPredicates;
        private int maxAttempts;
        private Boolean circuitBreakerEnabled;
        private Integer exceptionCost;
        private BackoffStrategy backoffStrategy;
        private TokenBucketStore tokenBucketStore;

        Builder() {
            retryPredicates = new ArrayList<>();
        }

        Builder(BaseRetryStrategy strategy) {
            this.retryPredicates = new ArrayList<>(strategy.retryPredicates);
            this.maxAttempts = strategy.maxAttempts;
            this.circuitBreakerEnabled = strategy.circuitBreakerEnabled;
            this.exceptionCost = strategy.exceptionCost;
            this.backoffStrategy = strategy.backoffStrategy;
            this.tokenBucketStore = strategy.tokenBucketStore;
        }

        void setRetryOnException(Predicate<Throwable> shouldRetry) {
            this.retryPredicates.add(shouldRetry);
        }

        void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        void setTokenBucketStore(TokenBucketStore tokenBucketStore) {
            this.tokenBucketStore = tokenBucketStore;
        }

        void setCircuitBreakerEnabled(Boolean enabled) {
            this.circuitBreakerEnabled = enabled;
        }

        void setBackoffStrategy(BackoffStrategy backoffStrategy) {
            this.backoffStrategy = backoffStrategy;
        }

        void setTokenBucketExceptionCost(int exceptionCost) {
            this.exceptionCost = exceptionCost;
        }
    }
}
