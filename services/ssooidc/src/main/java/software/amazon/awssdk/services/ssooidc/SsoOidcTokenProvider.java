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
import software.amazon.awssdk.auth.token.credentials.ProfileTokenProvider;
import software.amazon.awssdk.auth.token.credentials.SdkToken;
import software.amazon.awssdk.auth.token.credentials.SdkTokenProvider;
import software.amazon.awssdk.auth.token.credentials.aws.DefaultAwsTokenProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.awscore.internal.token.CachedTokenRefresher;
import software.amazon.awssdk.awscore.internal.token.TokenManager;
import software.amazon.awssdk.awscore.internal.token.TokenRefresher;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.TokenIdentity;
import software.amazon.awssdk.services.ssooidc.internal.OnDiskTokenManager;
import software.amazon.awssdk.services.ssooidc.internal.SsoOidcToken;
import software.amazon.awssdk.services.ssooidc.internal.SsoOidcTokenTransformer;
import software.amazon.awssdk.services.ssooidc.model.CreateTokenRequest;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.Validate;

/**
 * An {@link IdentityProvider}{@code <}{@link TokenIdentity}{@code >} implementation that loads a token by
 * assuming a role from SSO based on an OIDC token loaded from {@code ~/.aws/sso/cache}.
 *
 * <p>
 * To log in with SSO, use <a href="https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-sso.html">{@code aws
 * configure sso} and {@code aws sso login} with the AWS CLI</a> or
 * <a href="https://docs.aws.amazon.com/powershell/latest/userguide/creds-idc-cli.html">{@code Initialize-AWSSSOConfiguration} and
 * {@code Invoke-AWSSSOLogin} with AWS Tools for PowerShell</a>. This will initialize the {@code ~/.aws/sso/cache} token
 * cache, which this token provider will use with {@link SsoOidcClient#createToken(CreateTokenRequest)} to get a
 * {@link TokenIdentity} that can be used with AWS services.
 *
 * <p>
 * This token provider caches the token, and will only invoke AWS SSO periodically
 * to keep the token "fresh". As a result, it is recommended that you create a single token provider of this type
 * and reuse it throughout your application. You may notice small latency increases on requests that refresh the cached
 * token. To avoid this latency increase, you can enable async refreshing with
 * {@link Builder#asyncTokenUpdateEnabled(Boolean)}. If you enable this setting, you must {@link #close()} the token
 * provider if you are done using it, to disable the background refreshing task. If you fail to do this, your application could
 * run out of resources.
 *
 * <p>
 * This token provider is used by the {@link ProfileTokenProvider} if the {@code sso_session} profile
 * property is configured. The {@code ProfileTokenProvider} is included in the {@link DefaultAwsTokenProvider}.
 *
 * <p>
 * Create using {@link #builder()}:
 * {@snippet :
 * SsoOidcClient ssoClient =
 *     SsoOidcClient.builder()
 *                  .credentialsProvider(AnonymousCredentialsProvider.create())
 *                  .build();
 *
 * SsoOidcTokenProvider tokenProvider =
 *     SsoOidcTokenProvider.builder() // @link substring="builder" target="#builder()"
 *                         .sessionName("my-sso-session-name")
 *                         .ssoOidcClient(ssoClient)
 *                         .build();
 *
 * ServiceClient service = ServiceClient.builder()
 *                                      .tokenProvider(tokenProvider)
 *                                      .build();
 * }
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

    /**
     * Get a new builder for creating a {@link SsoOidcTokenProvider}.
     * <p>
     * {@snippet :
     * SsoOidcClient ssoClient =
     *     SsoOidcClient.builder()
     *                  .credentialsProvider(AnonymousCredentialsProvider.create())
     *                  .build();
     *
     * SsoOidcTokenProvider tokenProvider =
     *     SsoOidcTokenProvider.builder() // @link substring="builder" target="#builder()"
     *                         .sessionName("my-sso-session-name")
     *                         .ssoOidcClient(ssoClient)
     *                         .build();
     *
     * ServiceClient service = ServiceClient.builder()
     *                                      .tokenProvider(tokenProvider)
     *                                      .build();
     * }
     */
    public static Builder builder() {
        return new BuilderImpl();
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

    /**
     * Release resources held by this token provider. This must be called when you're done using the token provider if
     * {@link Builder#asyncTokenUpdateEnabled(Boolean)} was set to {@code true}. This does not close the configured
     * {@link Builder#ssoOidcClient(SsoOidcClient)}.
     */
    @Override
    public void close() {
        tokenRefresher.close();
    }

    public interface Builder {
        /**
         * Specify the SSO session name that was used to log in with the AWS CLI or AWS Tools for Powershell.
         *
         * <p>
         * If not specified, token provider creation will fail.
         *
         * <p>
         * {@snippet :
         * SsoOidcTokenProvider.builder()
         *                     .sessionName("my-sso-session-name")
         *                     .ssoOidcClient(...)
         *                     .build();
         *}
         */
        Builder sessionName(String sessionName);

        /**
         * Specify an {@link SsoOidcClient} to use when retrieving the SSO session token.
         *
         * <p>
         * The provided client will not be closed if this token provider is {@link #close()}d.
         *
         * <p>
         * If not specified, token provider creation will fail.
         *
         * <p>
         * {@snippet :
         * SsoOidcClient ssoClient =
         *     SsoOidcClient.builder()
         *                  .credentialsProvider(AnonymousCredentialsProvider.create())
         *                  .build();
         *
         * SsoOidcTokenProvider tokenProvider =
         *     SsoOidcTokenProvider.builder()
         *                         .sessionName("my-sso-session-name")
         *                         .ssoOidcClient(ssoClient)
         *                         .build();
         *}
         */
        Builder ssoOidcClient(SsoOidcClient ssoOidcClient);

        /**
         * Configure the amount of time between when the token expires and when the token provider starts to pre-fetch
         * updated token.
         *
         * <p>
         * When the pre-fetch threshold is encountered, the SDK will block a single calling thread to refresh the token.
         * Other threads will continue to use the existing token. This prevents all SDK caller's latency from increasing
         * when the token gets close to expiration, but you may still see a single call with increased latency as that
         * thread refreshes the token.
         *
         * <p>
         * Greater than the {@link #staleTime(Duration)} ({@code prefetchTime > staleTime}).
         *
         * <p>
         * If not specified, {@code Duration.ofMinutes(5)} is used. (4 minutes before the default stale time).
         *
         * <p>
         * {@snippet :
         * SsoOidcTokenProvider.builder()
         *                     .sessionName("my-sso-session-name")
         *                     .ssoOidcClient(...)
         *                     .prefetchTime(Duration.ofMinutes(5))
         *                     .build();
         * }
         */
        Builder prefetchTime(Duration prefetchTime);

        /**
         * Configure the amount of time between when the token actually expires and when the token provider treats
         * the token as expired.
         *
         * <p>
         * If the SDK treated the token as expired exactly when the service reported they will expire (a stale time of 0
         * seconds), SDK calls could fail close to that expiration time. As a result, the SDK treats a token as expired
         * 1 minute before the service reported that that token will expire.
         *
         * <p>
         * The failures that could occur without this threshold are caused by two primary factors:
         * <ul>
         *     <li>Request latency: There is latency between when the token is loaded and when the service processes
         *     the request. The SDK has to sign the request, transmit to the service, and the service has to validate the
         *     signature.</li>
         *     <li>Clock skew: The client and service may not have the exact same measure of time, so an expiration time for
         *     the service may be off from the expiration time for the client.</li>
         * </ul>
         *
         * <p>
         * When the stale threshold is encountered, the SDK will block all calling threads until a successful refresh is achieved.
         * (Note: while all threads are blocked, only one thread will actually make the service call to refresh the
         * token). Because this increase in latency for all threads is undesirable, you should ensure that the
         * {@link #prefetchTime(Duration)} is greater than the {@code staleTime}. When configured correctly, the stale time is
         * only encountered when the prefetch calls did not succeed (e.g. due to an outage).
         *
         * <p>
         * This value should be less than the {@link #prefetchTime(Duration)} ({@code prefetchTime > staleTime}).
         *
         * <p>
         * If not specified, {@code Duration.ofMinutes(1)} is used. (4 minutes after the default {@link #prefetchTime(Duration)}).
         *
         * <p>
         * {@snippet :
         * SsoOidcTokenProvider.builder()
         *                     .sessionName("my-sso-session-name")
         *                     .ssoOidcClient(...)
         *                     .staleTime(Duration.ofMinutes(1))
         *                     .build();
         * }
         */
        Builder staleTime(Duration staleTime);

        /**
         * Configure whether this provider should fetch tokens asynchronously in the background. If this is {@code true},
         * threads are less likely to block when tokens are loaded, but additional resources are used to maintain
         * the provider.
         *
         * <p>
         * If not specified, this is {@code false}.
         *
         * <p>
         * {@snippet :
         * SsoOidcTokenProvider.builder()
         *                     .sessionName("my-sso-session-name")
         *                     .ssoOidcClient(...)
         *                     .asyncTokenUpdateEnabled(false)
         *                     .build();
         * }
         */
        Builder asyncTokenUpdateEnabled(Boolean asyncTokenUpdateEnabled);

        /**
         * Build the {@link SsoOidcTokenProvider}.
         *
         * <p>
         * {@snippet :
         * SsoOidcClient ssoClient =
         *     SsoOidcClient.builder()
         *                  .credentialsProvider(AnonymousCredentialsProvider.create())
         *                  .build();
         *
         * SsoOidcTokenProvider tokenProvider =
         *     SsoOidcTokenProvider.builder()
         *                         .sessionName("my-sso-session-name")
         *                         .ssoOidcClient(ssoClient)
         *                         .build();
         * }
         */
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
