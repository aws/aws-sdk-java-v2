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

package software.amazon.awssdk.awscore.internal.token;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.auth.token.AwsToken;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.cache.CachedSupplier;
import software.amazon.awssdk.utils.cache.RefreshResult;

/**
 * Class to cache Tokens which are supplied by the Suppliers while constructing this class.
 * Automatic refresh can be enabled by setting enableAutoFetch flag in builder methods.
 */
@ThreadSafe
@SdkInternalApi
public final class CachedTokenRefresher<TokenT extends AwsToken> implements TokenRefresher<TokenT> {

    public static final Duration FIVE_MINUTES_DURATION = Duration.ofMinutes(5);
    private static final Logger log = Logger.loggerFor(CachedTokenRefresher.class);
    private static final String THREAD_CLASS_NAME = "sdk-token-refresher";
    private final Supplier<TokenT> tokenRetriever;
    private final Duration staleDuration;
    private final Function<SdkException, TokenT> exceptionHandler;
    private final ScheduledExecutorService scheduledExecutorService;
    private final Supplier<TokenT> tokenCacheSupplier;
    private final Clock clock;

    private CachedTokenRefresher(Builder builder) {
        Validate.paramNotNull(builder.tokenRetriever, "tokenRetriever");
        this.staleDuration = builder.staleDuration == null ? FIVE_MINUTES_DURATION : builder.staleDuration;
        Function<SdkException, TokenT> defaultExceptionHandler = exp -> {
            throw exp;
        };
        this.exceptionHandler = builder.exceptionHandler == null ? defaultExceptionHandler : builder.exceptionHandler;
        this.tokenRetriever = builder.tokenRetriever;
        this.tokenCacheSupplier = CachedSupplier.builder(this::refreshResult).build();
        this.clock = Clock.systemUTC();

        scheduledExecutorService = builder.enableAutoFetch != null && builder.enableAutoFetch
                                   ? defaultScheduledExecutorService()
                                   : null;

        if (builder.enableAutoFetch != null && builder.enableAutoFetch) {
            // Wait for initially stale duration time to start the auto refresh cycle.
            this.scheduledExecutorService.schedule(() -> this.scheduleRefresh(), staleDuration.toMillis(),
                                                   TimeUnit.MILLISECONDS);
        }
    }

    @SdkTestInternalApi
    public CachedTokenRefresher(Supplier<TokenT> tokenRetriever, Duration staleDuration,
                                Function<SdkException, TokenT> exceptionHandler,
                                ScheduledExecutorService scheduledExecutorService, Supplier<TokenT> tokenCacheSupplier,
                                Clock clock, Boolean enableAutoFetch) {

        this.tokenRetriever = tokenRetriever;
        this.staleDuration = staleDuration;
        this.exceptionHandler = exceptionHandler;
        this.tokenCacheSupplier = tokenCacheSupplier == null ? CachedSupplier.builder(this::refreshResult).build() :
                                  tokenCacheSupplier;
        this.clock = clock == null ? Clock.systemUTC() : clock;
        this.scheduledExecutorService = scheduledExecutorService != null ? scheduledExecutorService :
                                        defaultScheduledExecutorService();

        if (enableAutoFetch != null && enableAutoFetch) {
            // Wait for initially stale duration time to start the auto refresh cycle.
            this.scheduledExecutorService.schedule(() -> this.scheduleRefresh(), staleDuration.toMillis(),
                                                   TimeUnit.MILLISECONDS);

        }
    }

    /**
     * Builder method to construct instance of CachedTokenRefresher.
     *
     * @return
     */
    public static Builder builder() {
        return new Builder();
    }

    private static ScheduledExecutorService defaultScheduledExecutorService() {
        return Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder().threadNamePrefix(THREAD_CLASS_NAME).build());
    }

    @Override
    public TokenT refreshIfStaleAndFetch() {
        return tokenCacheSupplier.get();
    }

    private TokenT refreshAndGetTokenFromSupplier() {
        try {
            TokenT freshToken = tokenRetriever.get();
            return freshToken;
        } catch (SdkException exception) {
            return exceptionHandler.apply(exception);
        }
    }

    @Override
    public void close() {
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdown();
        }
    }

    private RefreshResult<TokenT> refreshResult() {
        TokenT tokenT = refreshAndGetTokenFromSupplier();
        return RefreshResult.builder(tokenT).staleTime(tokenT.expirationTime().minus(staleDuration)).build();
    }

    private void scheduleRefresh() {
        long lookAheadRefreshTimeInMillis = staleDuration.toMillis();
        try {
            TokenT tokenT = tokenCacheSupplier.get();
            lookAheadRefreshTimeInMillis = getLookAheadRefreshTime(tokenT);
        } catch (Exception e) {
            // Ignore the exceptions and make sure the auto refresh should keep calling refresh method at regular intervals.
            String errorMessage = "Could not auto refresh, retrying after " + lookAheadRefreshTimeInMillis + " millis";
            log.debug(() -> errorMessage, e);
        } finally {
            long delayedRefreshDurationInMillis = lookAheadRefreshTimeInMillis;
            log.debug(() -> "Refreshing token after " + delayedRefreshDurationInMillis + " milliseconds");
            scheduledExecutorService.schedule(() -> this.scheduleRefresh(), delayedRefreshDurationInMillis,
                                              TimeUnit.MILLISECONDS);
        }
    }

    private long getLookAheadRefreshTime(TokenT token) {
        Instant actualExpirationDate = token.expirationTime();
        Instant currentTimeNow = clock.instant();
        //Look-ahead refresh time
        if (actualExpirationDate.isAfter(currentTimeNow)) {
            // Not within the refresh window.
            if (currentTimeNow.plus(staleDuration).isBefore(actualExpirationDate)) {
                return Duration.between(currentTimeNow, actualExpirationDate.minus(staleDuration)).toMillis();
            }
        }
        return staleDuration.toMillis();
    }

    public static class Builder<TokenT extends AwsToken> {

        private Function<SdkException, TokenT> exceptionHandler;
        private Duration staleDuration;
        private Supplier<TokenT> tokenRetriever;
        private Boolean enableAutoFetch;

        /**
         * @param tokenRetriever that retrieves cached token from disc, if the token in disc is stale then it gets fresh token
         *                      from the service.
         * @return
         */
        public Builder tokenRetriever(Supplier<TokenT> tokenRetriever) {
            this.tokenRetriever = tokenRetriever;
            return this;
        }

        /**
         * @param staleDuration The time before which the token is marked as stale to indicate it is time to fetch a fresh token.
         * @return
         */
        public Builder staleDuration(Duration staleDuration) {
            this.staleDuration = staleDuration;
            return this;
        }

        /**
         * @param exceptionHandler Handler which takes action when a Runtime exception occurs while fetching a token. Handler can
         *                         return a previously stored token or throw back the exception.
         * @return
         */
        public Builder exceptionHandler(Function<SdkException, TokenT> exceptionHandler) {
            this.exceptionHandler = exceptionHandler;
            return this;
        }

        /**
         * @param enableAutoFetch This is set to true to enable auto refresh of token where it will fetch tokens asynchronously in
         *                        the background.
         * @return
         */
        public Builder enableAutoFetch(Boolean enableAutoFetch) {
            this.enableAutoFetch = enableAutoFetch;
            return this;
        }

        public CachedTokenRefresher build() {
            CachedTokenRefresher cachedTokenRefresher = new CachedTokenRefresher(this);
            return cachedTokenRefresher;
        }
    }
}
