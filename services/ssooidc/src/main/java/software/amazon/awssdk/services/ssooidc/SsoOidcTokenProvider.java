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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.token.SdkToken;
import software.amazon.awssdk.auth.token.SdkTokenProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.awscore.internal.token.CachedTokenRefresher;
import software.amazon.awssdk.awscore.internal.token.TokenManager;
import software.amazon.awssdk.awscore.internal.token.TokenRefresher;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.regions.Region;
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

    private static final Duration DEFAULT_STALE_DURATION = Duration.ofMinutes(5);
    private static final Logger log = Logger.loggerFor(SsoOidcTokenProvider.class);

    private final String startUrl;
    private final String region;
    private final TokenManager<SsoOidcToken> onDiskTokenManager;
    private final Clock clock;

    private final TokenRefresher<SsoOidcToken> tokenRefresher;
    private final SsoOidcClient ssoOidcClient;

    private final Duration staleDuration;

    private SsoOidcTokenProvider(BuilderImpl builder) {
        this.startUrl = builder.startUrl;
        this.region = builder.region;
        Supplier<SsoOidcToken> tokenRetriever = builder.tokenRetriever == null ? getDefaultSsoTokenRetriever()
                                                                               : builder.tokenRetriever;

        validateSupplierAndClient(builder.tokenRetriever, builder.ssoOidcClient);
        this.ssoOidcClient = builder.ssoOidcClient == null ? defaultSsoOidcClient(this.region) : builder.ssoOidcClient;
        this.staleDuration = builder.staleDuration == null ? DEFAULT_STALE_DURATION : builder.staleDuration;
        this.onDiskTokenManager = OnDiskTokenManager.create(startUrl);
        this.clock = Clock.systemUTC();
        this.tokenRefresher = getDefaultTokenRefresher(tokenRetriever, this.staleDuration);
    }


    @SdkTestInternalApi
    public SsoOidcTokenProvider(String startUrl, String region, TokenManager<SsoOidcToken> onDiskTokenManager, Clock clock,
                                TokenRefresher<SsoOidcToken> tokenRefresher, SsoOidcClient ssoOidcClient,
                                Duration staleDuration) {
        this.startUrl = startUrl;
        this.region = region;
        this.onDiskTokenManager = onDiskTokenManager == null ? OnDiskTokenManager.create(this.startUrl) : onDiskTokenManager;
        this.clock = clock == null ? Clock.systemUTC() : clock;
        this.ssoOidcClient = ssoOidcClient == null ? defaultSsoOidcClient(this.region) : ssoOidcClient;
        this.staleDuration = staleDuration == null ? DEFAULT_STALE_DURATION : staleDuration;
        this.tokenRefresher = tokenRefresher == null ? getDefaultTokenRefresher(getDefaultSsoTokenRetriever(),
                                                                                this.staleDuration) : tokenRefresher;
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
         * The startUrl used to retrieve the SSO token.
         */
        Builder startUrl(String startUrl);

        /**
         * The region used to retrieve the SSO token.
         */
        Builder region(String region);

        /**
         *
         * Supplier that will implement retrieval of the token from Disc or SSO-Oidc server if onDisc token has expired.
         */
        Builder tokenRetriever(Supplier<SsoOidcToken> tokenRetriever);

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
        Builder staleDuration(Duration onDiskStaleDuration);

        SsoOidcTokenProvider build();
    }

    private boolean isExpired(SsoOidcToken token) {
        Instant expiration = token.expirationTime().get();
        Instant now = clock.instant();
        return now.isAfter(expiration);
    }

    private boolean isWithinRefreshWindow(SsoOidcToken token) {
        Instant expiration = token.expirationTime().get();
        Instant now = clock.instant();
        return expiration.isAfter(now.plus(staleDuration));
    }

    private void validateToken(SsoOidcToken token) {
        Validate.notNull(token.token(), "token cannot be null");
        Validate.notNull(token.expirationTime(), "expirationTime cannot be null");
    }

    private static class BuilderImpl implements Builder {
        private String startUrl;
        private String region;
        private Supplier<SsoOidcToken> tokenRetriever;
        private SsoOidcClient ssoOidcClient;
        private Duration staleDuration;

        @Override
        public Builder startUrl(String startUrl) {
            this.startUrl = startUrl;
            return this;
        }

        @Override
        public Builder region(String region) {
            this.region = region;
            return this;
        }

        @Override
        public Builder tokenRetriever(Supplier<SsoOidcToken> tokenRetriever) {
            this.tokenRetriever = tokenRetriever;
            return this;
        }

        @Override
        public Builder ssoOidcClient(SsoOidcClient ssoOidcClient) {
            this.ssoOidcClient = ssoOidcClient;
            return this;
        }

        @Override
        public Builder staleDuration(Duration staleDuration) {
            this.staleDuration = staleDuration;
            return this;
        }

        @Override
        public SsoOidcTokenProvider build() {
            return new SsoOidcTokenProvider(this);
        }
    }

    private Supplier<SsoOidcToken> getDefaultSsoTokenRetriever() {
        return () -> {
            SsoOidcToken baseToken = onDiskTokenManager.loadToken()
                                                       .orElseThrow(() -> SdkClientException.create("Unable to load SSO token"));
            validateToken(baseToken);

            if (isWithinRefreshWindow(baseToken)) {
                return baseToken;
            }

            SsoOidcTokenTransformer ssoOidcTokenTransformer = SsoOidcTokenTransformer.create(baseToken);
            SsoOidcToken refreshToken = ssoOidcTokenTransformer.transform(ssoOidcClient.createToken(
                CreateTokenRequest.builder()
                                  .grantType("refreshToken")
                                  .clientId(baseToken.clientId())
                                  .clientSecret(baseToken.clientSecret())
                                  .refreshToken(baseToken.refreshToken())
                                  .build()));
            onDiskTokenManager.storeToken(refreshToken);

            return refreshToken;
        };
    }

    private CachedTokenRefresher getDefaultTokenRefresher(Supplier<SsoOidcToken> tokenRetriever,
                                                          Duration staleTime) {
        return CachedTokenRefresher.builder()
                                   .tokenRetriever(tokenRetriever)
                                   .exceptionHandler(exceptionHandler())
                                   .enableAutoFetch(true)
                                   .staleDuration(staleTime)
                                   .build();
    }

    private SsoOidcClient defaultSsoOidcClient(String region) {
        return SsoOidcClient.builder()
                            .region(Region.of(region))
                            .credentialsProvider(AnonymousCredentialsProvider.create())
                            .build();
    }

    private void validateSupplierAndClient(Supplier<SsoOidcToken> tokenRetriever, SsoOidcClient ssoOidcClient) {
        if (tokenRetriever != null && ssoOidcClient != null) {
            throw new IllegalStateException("Cannot provide both SsoOidcClient and a tokenRetriever.");
        }
    }
}
