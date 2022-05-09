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

package software.amazon.awssdk.services.ssooidc;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.auth.token.credentials.SdkToken;
import software.amazon.awssdk.auth.token.credentials.SdkTokenProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.awscore.internal.token.CachedTokenRefresher;
import software.amazon.awssdk.awscore.internal.token.TokenManager;
import software.amazon.awssdk.awscore.internal.token.TokenRefresher;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.ssooidc.internal.OnDiskTokenManager;
import software.amazon.awssdk.services.ssooidc.internal.SsoOidcToken;
import software.amazon.awssdk.services.ssooidc.internal.SsoOidcTokenTransformer;
import software.amazon.awssdk.services.ssooidc.model.CreateTokenRequest;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.Validate;

/**
 * Implementation of {@link SdkTokenProvider} that is capable of loading and
 * storing SSO tokens to {@code ~/.aws/sso/cache}. This is also capable of
 * refreshing the cached token via the SSO-OIDC service.
 */
@SdkPublicApi
@ThreadSafe
public final class SsoOidcTokenProvider implements SdkTokenProvider, SdkAutoCloseable {

    private static final Duration DEFAULT_STALE_DURATION = Duration.ofMinutes(1);
    private static final Duration DEFAULT_PREFETCH_DURATION = Duration.ofMinutes(5);
    private static final Logger log = Logger.loggerFor(SsoOidcTokenProvider.class);
    private final TokenManager<SsoOidcToken> onDiskTokenManager;

    private final TokenRefresher<SsoOidcToken> tokenRefresher;
    private final SsoOidcClient ssoOidcClient;

    private final Duration staleTime;
    private final Duration prefetchTime;

    private SsoOidcTokenProvider(BuilderImpl builder) {
        Validate.paramNotNull(builder.sessionName, "sessionName");
        Validate.paramNotNull(builder.ssoOidcClient, "ssoOidcClient");

        this.ssoOidcClient = builder.ssoOidcClient;
        this.staleTime = builder.staleTime == null ? DEFAULT_STALE_DURATION : builder.staleTime;
        this.prefetchTime = builder.prefetchTime == null ? DEFAULT_PREFETCH_DURATION : builder.prefetchTime;

        this.onDiskTokenManager = OnDiskTokenManager.create(builder.sessionName);

        this.tokenRefresher = CachedTokenRefresher.builder()
                                                  .tokenRetriever(getDefaultSsoTokenRetriever(this.ssoOidcClient,
                                                                                              this.onDiskTokenManager,
                                                                                              this.staleTime, this.prefetchTime))
                                                  .exceptionHandler(exceptionHandler())
                                                  .prefetchTime(this.prefetchTime)
                                                  .staleDuration(this.staleTime)
                                                  .asyncRefreshEnabled(builder.asyncTokenUpdateEnabled)
                                                  .build();
    }


    private Function<SdkException, SsoOidcToken> exceptionHandler() {
        return e -> {
            if (e instanceof AwsServiceException) {
                log.warn(() -> "Failed to fetch token.", e);
                // If we fail to get token from service then fetch the previous cached token from disc.
                return onDiskTokenManager.loadToken()
                                         .orElseThrow(() -> SdkClientException.create("Unable to load SSO token"));
            }
            throw e;
        };
    }

    @Override
    public SdkToken resolveToken() {
        SsoOidcToken ssoOidcToken = tokenRefresher.refreshIfStaleAndFetch();
        if (isExpired(ssoOidcToken)) {
            throw SdkClientException.create("Token is expired");
        }
        return ssoOidcToken;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public void close() {
        tokenRefresher.close();
    }

    public interface Builder {
        /**
         * The sessionName used to retrieve the SSO token.
         */
        Builder sessionName(String sessionName);

        /**
         *
         * Client to fetch token from SSO OIDC service.
         */
        Builder ssoOidcClient(SsoOidcClient ssoOidcClient);

        /**
         * Configure the amount of time, relative to Sso-Oidc token , that the cached tokens in refresher are considered
         * stale and should no longer be used.
         *
         * <p>By default, this is 5 minute.</p>
         */
        Builder staleTime(Duration onDiskStaleDuration);

        /**
         *
         * Configure the amount of time, relative to Sso-Oidc token , that the cached tokens in refresher are considered
         * prefetched from service..
         */
        Builder prefetchTime(Duration prefetchTime);

        /**
         * Configure whether the provider should fetch tokens asynchronously in the background. If this is true,
         * threads are less likely to block when token are loaded, but additional resources are used to maintain
         * the provider.
         *
         * <p>By default, this is disabled.</p>
         */
        Builder asyncTokenUpdateEnabled(Boolean asyncTokenUpdateEnabled);

        SsoOidcTokenProvider build();
    }

    private boolean isExpired(SsoOidcToken token) {
        Instant expiration = token.expirationTime().get();
        Instant now = Instant.now();
        return now.isAfter(expiration);
    }

    private static boolean isWithinRefreshWindow(SsoOidcToken token, Duration staleTime) {
        Instant expiration = token.expirationTime().get();
        Instant now = Instant.now();
        return expiration.isAfter(now.plus(staleTime));
    }

    private static void validateToken(SsoOidcToken token) {
        Validate.notNull(token.token(), "token cannot be null");
        Validate.notNull(token.expirationTime(), "expirationTime cannot be null");
    }

    private static class BuilderImpl implements Builder {
        private String sessionName;
        private SsoOidcClient ssoOidcClient;
        private Duration staleTime;
        private Duration prefetchTime;
        private Boolean asyncTokenUpdateEnabled = false;


        private BuilderImpl() {
        }

        @Override
        public Builder sessionName(String sessionName) {
            this.sessionName = sessionName;
            return this;
        }

        @Override
        public Builder ssoOidcClient(SsoOidcClient ssoOidcClient) {
            this.ssoOidcClient = ssoOidcClient;
            return this;
        }

        @Override
        public Builder staleTime(Duration staleTime) {
            this.staleTime = staleTime;
            return this;
        }

        @Override
        public Builder prefetchTime(Duration prefetchTime) {
            this.prefetchTime = prefetchTime;
            return this;
        }

        @Override
        public Builder asyncTokenUpdateEnabled(Boolean asyncTokenUpdateEnabled) {
            this.asyncTokenUpdateEnabled = asyncTokenUpdateEnabled;
            return this;
        }

        @Override
        public SsoOidcTokenProvider build() {
            return new SsoOidcTokenProvider(this);
        }
    }

    private static Supplier<SsoOidcToken> getDefaultSsoTokenRetriever(SsoOidcClient ssoOidcClient,
                                                                      TokenManager<SsoOidcToken> tokenManager,
                                                                      Duration staleTime,
                                                                      Duration prefetchTime) {
        return () -> {
            SsoOidcToken baseToken = tokenManager.loadToken()
                                                 .orElseThrow(() -> SdkClientException.create("Unable to load SSO token"));
            validateToken(baseToken);

            if (isWithinRefreshWindow(baseToken, staleTime)
                && isWithinRefreshWindow(baseToken, prefetchTime)) {
                return baseToken;
            }

            SsoOidcTokenTransformer ssoOidcTokenTransformer = SsoOidcTokenTransformer.create(baseToken);
            SsoOidcToken refreshToken = ssoOidcTokenTransformer.transform(ssoOidcClient.createToken(
                CreateTokenRequest.builder()
                                  .grantType("refresh_token")
                                  .clientId(baseToken.clientId())
                                  .clientSecret(baseToken.clientSecret())
                                  .refreshToken(baseToken.refreshToken())
                                  .build()));
            tokenManager.storeToken(refreshToken);
            return refreshToken;
        };
    }


}
