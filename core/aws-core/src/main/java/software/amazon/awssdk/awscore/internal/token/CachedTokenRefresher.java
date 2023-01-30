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

import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.auth.token.credentials.SdkToken;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.cache.CachedSupplier;
import software.amazon.awssdk.utils.cache.NonBlocking;
import software.amazon.awssdk.utils.cache.RefreshResult;

/**
 * Class to cache Tokens which are supplied by the Suppliers while constructing this class. Automatic refresh can be enabled by
 * setting autoRefreshDuration in builder methods.
 */
@ThreadSafe
@SdkInternalApi
public final class CachedTokenRefresher<TokenT extends SdkToken> implements TokenRefresher<TokenT> {

    private static final Duration DEFAULT_STALE_TIME = Duration.ofMinutes(1);

    private static final String THREAD_CLASS_NAME = "sdk-token-refresher";
    private final Supplier<TokenT> tokenRetriever;
    private final Duration staleDuration;
    private final Duration prefetchDuration;
    private final Function<SdkException, TokenT> exceptionHandler;
    private final CachedSupplier<TokenT> tokenCacheSupplier;

    private CachedTokenRefresher(Builder builder) {
        Validate.paramNotNull(builder.tokenRetriever, "tokenRetriever");
        this.staleDuration = builder.staleDuration == null ? DEFAULT_STALE_TIME : builder.staleDuration;
        this.prefetchDuration = builder.prefetchDuration == null ? this.staleDuration : builder.prefetchDuration;
        Function<SdkException, TokenT> defaultExceptionHandler = exp -> {
            throw exp;
        };
        this.exceptionHandler = builder.exceptionHandler == null ? defaultExceptionHandler : builder.exceptionHandler;
        this.tokenRetriever = builder.tokenRetriever;
        CachedSupplier.Builder<TokenT> cachedBuilder = CachedSupplier.builder(this::refreshResult);
        if (builder.asyncRefreshEnabled) {
            cachedBuilder.prefetchStrategy(new NonBlocking(THREAD_CLASS_NAME));
        }
        this.tokenCacheSupplier = cachedBuilder.build();
    }

    /**
     * Builder method to construct instance of CachedTokenRefresher.
     *
     * @return
     */
    public static Builder builder() {
        return new Builder();
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

    private RefreshResult<TokenT> refreshResult() {
        TokenT tokenT = refreshAndGetTokenFromSupplier();


        Instant staleTime = tokenT.expirationTime().isPresent()
                            ? tokenT.expirationTime().get().minus(staleDuration)
                            : Instant.now();

        Instant prefetchTime = tokenT.expirationTime().isPresent()
                               ? tokenT.expirationTime().get().minus(prefetchDuration)
                               : null;

        return RefreshResult.builder(tokenT).staleTime(staleTime).prefetchTime(prefetchTime).build();
    }

    @Override
    public void close() {
        tokenCacheSupplier.close();
    }

    public static class Builder<TokenT extends SdkToken> {

        private Function<SdkException, TokenT> exceptionHandler;
        private Duration staleDuration;
        private Duration prefetchDuration;
        private Supplier<TokenT> tokenRetriever;
        private Boolean asyncRefreshEnabled = false;

        /**
         * @param tokenRetriever Supplier to retrieve the token from its respective sources.
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
         * Configure the amount of time, relative to SSO session token expiration, that the cached credentials are considered
         * close to stale and should be updated. See {@link #asyncRefreshEnabled}.
         *
         * <p>By default, this is 5 minutes.</p>
         */
        public Builder prefetchTime(Duration prefetchTime) {
            this.prefetchDuration = prefetchTime;
            return this;
        }

        /**
         * Configure whether this refresher should fetch tokens asynchronously in the background. If this is true, threads are
         * less likely to block when {@link #refreshIfStaleAndFetch()} ()} is called, but additional resources are used to
         * maintain the provider.
         *
         * <p>By default, this is disabled.</p>
         */
        public Builder asyncRefreshEnabled(Boolean asyncRefreshEnabled) {
            this.asyncRefreshEnabled = asyncRefreshEnabled;
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

        public CachedTokenRefresher build() {
            CachedTokenRefresher cachedTokenRefresher = new CachedTokenRefresher(this);
            return cachedTokenRefresher;
        }
    }
}
