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

package software.amazon.awssdk.services.signin.auth;

import static software.amazon.awssdk.utils.UserHomeDirectoryUtils.userHomeDirectory;
import static software.amazon.awssdk.utils.Validate.notNull;
import static software.amazon.awssdk.utils.Validate.paramNotBlank;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.services.signin.SigninClient;
import software.amazon.awssdk.services.signin.internal.AccessTokenManager;
import software.amazon.awssdk.services.signin.internal.DpopAuthScheme;
import software.amazon.awssdk.services.signin.internal.LoginAccessToken;
import software.amazon.awssdk.services.signin.internal.LoginCacheDirectorySystemSetting;
import software.amazon.awssdk.services.signin.internal.OnDiskTokenManager;
import software.amazon.awssdk.services.signin.model.CreateOAuth2TokenRequest;
import software.amazon.awssdk.services.signin.model.CreateOAuth2TokenResponse;
import software.amazon.awssdk.services.signin.model.SigninException;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;
import software.amazon.awssdk.utils.cache.CachedSupplier;
import software.amazon.awssdk.utils.cache.NonBlocking;
import software.amazon.awssdk.utils.cache.RefreshResult;

/**
 * An implementation of {@link AwsCredentialsProvider} that loads and refreshes AWS Login Session credentials.
 * It periodically sends a {@link CreateOAuth2TokenRequest} to the AWS
 * Sign-On Service to refresh short-lived sessions to use for authentication. These sessions are updated using a single
 * calling thread (by default) or asynchronously (if {@link Builder#asyncCredentialUpdateEnabled(Boolean)} is set).
 *
 * If the credentials are not successfully updated before expiration, calls to {@link #resolveCredentials()} will block until
 * they are updated successfully.
 *
 * Users of this provider must {@link #close()} it when they are finished using it.
 *
 * This is created using {@link LoginCredentialsProvider#builder()}.
 */
@SdkPublicApi
@ThreadSafe
public class LoginCredentialsProvider implements
                                      AwsCredentialsProvider, SdkAutoCloseable,
                                      ToCopyableBuilder<LoginCredentialsProvider.Builder, LoginCredentialsProvider> {
    private static final Logger log = Logger.loggerFor(LoginCredentialsProvider.class);

    private static final String PROVIDER_NAME = BusinessMetricFeatureId.CREDENTIALS_LOGIN.value();

    private static final Duration DEFAULT_STALE_TIME = Duration.ofMinutes(1);
    private static final Duration DEFAULT_PREFETCH_TIME = Duration.ofMinutes(5);
    private static final Path DEFAULT_TOKEN_LOCATION = Paths.get(userHomeDirectory(), ".aws", "login", "cache");

    private static final String ASYNC_THREAD_NAME = "sdk-login-credentials-provider";

    private final String loginSession;
    private final String sourceChain;
    private final String providerName;

    private final SigninClient signinClient;
    private final Duration staleTime;
    private final Duration prefetchTime;
    private final Path tokenCacheLocation;

    private final CachedSupplier<AwsCredentials> credentialCache;
    private final AccessTokenManager onDiskTokenManager;

    private final Boolean asyncCredentialUpdateEnabled;

    /**
     *
     * @see #builder()
     */
    private LoginCredentialsProvider(BuilderImpl builder) {
        this.signinClient = notNull(builder.signinClient, "SigninClient must not be null.");
        this.loginSession = paramNotBlank(builder.loginSession, "LoginSession");

        this.staleTime = Optional.ofNullable(builder.staleTime).orElse(DEFAULT_STALE_TIME);
        this.prefetchTime = Optional.ofNullable(builder.prefetchTime).orElse(DEFAULT_PREFETCH_TIME);
        this.sourceChain = builder.sourceChain;

        this.providerName = StringUtils.isEmpty(builder.sourceChain)
                            ? PROVIDER_NAME
                            : builder.sourceChain + "," + PROVIDER_NAME;

        this.tokenCacheLocation = Optional.ofNullable(builder.tokenCacheLocation).orElseGet(
            () -> new LoginCacheDirectorySystemSetting().getStringValue()
                                                        .map(p -> Paths.get(p))
                                                        .orElse(DEFAULT_TOKEN_LOCATION));

        this.onDiskTokenManager = OnDiskTokenManager.create(this.tokenCacheLocation, this.loginSession);

        this.asyncCredentialUpdateEnabled = builder.asyncCredentialUpdateEnabled;
        CachedSupplier.Builder<AwsCredentials> cacheBuilder =
            CachedSupplier.builder(this::updateSigninCredentials)
                          .cachedValueName(toString());
        if (builder.asyncCredentialUpdateEnabled) {
            cacheBuilder.prefetchStrategy(new NonBlocking(ASYNC_THREAD_NAME));
        }

        this.credentialCache = cacheBuilder.build();
    }

    /**
     * Update the expiring session SSO credentials by calling SSO. Invoked by {@link CachedSupplier} when the credentials are
     * close to expiring.
     */
    private RefreshResult<AwsCredentials> updateSigninCredentials() {
        // always re-load token from the disk in case it has been updated elsewhere
        LoginAccessToken tokenFromDisc = onDiskTokenManager.loadToken().orElseThrow(
            () -> SdkClientException.create("Token cache file for login_session `" + loginSession + "` not found. "
                                            + "You must re-authenticate."));

        Instant currentExpirationTime = tokenFromDisc.getAccessToken().expirationTime().orElseThrow(
            () -> SdkClientException.create("Invalid token expiration time. You must re-authenticate.")
        );

        if (shouldNotRefresh(currentExpirationTime, staleTime)
            && shouldNotRefresh(currentExpirationTime, prefetchTime)) {
            log.debug(() -> "Using access token from disk, current expiration time is : " + currentExpirationTime);
            AwsCredentials credentials = tokenFromDisc.getAccessToken()
                .toBuilder()
                .providerName(this.providerName)
                .build();

            return RefreshResult.builder(credentials)
                                .staleTime(currentExpirationTime.minus(staleTime))
                                .prefetchTime(currentExpirationTime.minus(prefetchTime))
                                .build();
        }

        return refreshFromSigninService(tokenFromDisc);
    }

    private RefreshResult<AwsCredentials> refreshFromSigninService(LoginAccessToken tokenFromDisc) {
        log.debug(() -> "Credentials are near expiration/expired, refreshing from Signin service.");

        try {
            SdkPlugin dpopAuthPlugin = DpopAuthScheme.DpopAuthPlugin.create(tokenFromDisc.getDpopKey());
            CreateOAuth2TokenRequest refreshRequest =
                CreateOAuth2TokenRequest
                    .builder()
                    .tokenInput(t -> t
                        .clientId(tokenFromDisc.getClientId())
                        .refreshToken(tokenFromDisc.getRefreshToken())
                        .grantType("refresh_token"))
                    .overrideConfiguration(c -> c.addPlugin(dpopAuthPlugin))
                    .build();

            CreateOAuth2TokenResponse createTokenResponse = signinClient.createOAuth2Token(refreshRequest);

            Instant newExpiration = Instant.now().plusSeconds(createTokenResponse.tokenOutput().expiresIn());
            AwsSessionCredentials updatedCredentials = AwsSessionCredentials
                .builder()
                .accessKeyId(createTokenResponse.tokenOutput().accessToken().accessKeyId())
                .secretAccessKey(createTokenResponse.tokenOutput().accessToken().secretAccessKey())
                .sessionToken(createTokenResponse.tokenOutput().accessToken().sessionToken())
                .accountId(tokenFromDisc.getAccessToken().accountId().orElseThrow(
                    () -> SdkClientException.create("Invalid access token, missing account ID. You must re-authenticate.")
                ))
                .expirationTime(newExpiration)
                .providerName(this.providerName)
                .build();

            onDiskTokenManager.storeToken(tokenFromDisc.toBuilder()
                                                       .accessToken(updatedCredentials)
                                                       .refreshToken(createTokenResponse.tokenOutput().refreshToken())
                                                       .build());

            return RefreshResult.builder((AwsCredentials) updatedCredentials)
                                .staleTime(newExpiration.minus(staleTime))
                                .prefetchTime(newExpiration.minus(prefetchTime))
                                .build();
        } catch (SigninException serviceException) {
            throw SdkClientException.create(
                "Unable to refresh AWS Signin Access Token: You must re-authenticate.",
                serviceException);
        }
    }

    /**
     * The amount of time, relative to session token expiration, that the cached credentials are considered stale and should no
     * longer be used. All threads will block until the value is updated.
     */
    public Duration staleTime() {
        return staleTime;
    }

    /**
     * The amount of time, relative to session token expiration, that the cached credentials are considered close to stale and
     * should be updated.
     */
    public Duration prefetchTime() {
        return prefetchTime;
    }

    /**
     * Get a builder for creating a custom {@link LoginCredentialsProvider}.
     */
    public static BuilderImpl builder() {
        return new BuilderImpl();
    }

    @Override
    public AwsCredentials resolveCredentials() {
        return credentialCache.get();
    }

    @Override
    public void close() {
        credentialCache.close();
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }


    /**
     *
     * @return true if the token does NOT need to be refreshed - it is after the given refresh window, eg stale/prefetch time.
     */
    private static boolean shouldNotRefresh(Instant expiration, Duration refreshWindow) {
        Instant now = Instant.now();
        return expiration.isAfter(now.plus(refreshWindow));
    }

    /**
     * A builder for creating a custom {@link LoginCredentialsProvider}.
     */
    public interface Builder extends CopyableBuilder<Builder, LoginCredentialsProvider> {
        /**
         * Configure the {@link SigninClient} to use when calling Signin to update the session. This client should not be shut
         * down as long as this credentials provider is in use.
         */
        Builder signinClient(SigninClient signinClient);

        /**
         * Configure whether the provider should fetch credentials asynchronously in the background. If this is true, threads are
         * less likely to block when credentials are loaded, but additional resources are used to maintain the provider.
         *
         * <p>By default, this is disabled.</p>
         */
        Builder asyncCredentialUpdateEnabled(Boolean asyncCredentialUpdateEnabled);

        /**
         * Configure the amount of time, relative to signin token expiration, that the cached credentials are considered stale and
         * should no longer be used. All threads will block until the value is updated.
         *
         * <p>By default, this is 1 minute.</p>
         */
        Builder staleTime(Duration staleTime);

        /**
         * Configure the amount of time, relative to signin token expiration, that the cached credentials are considered close to
         * stale and should be updated.
         * <p>
         * Prefetch updates will occur between the specified time and the stale time of the provider. Prefetch updates may be
         * asynchronous. See {@link #asyncCredentialUpdateEnabled}.
         *
         * <p>By default, this is 5 minutes.</p>
         */
        Builder prefetchTime(Duration prefetchTime);

        /**
         * The login session name used to retrieve the cached token.
         */
        Builder loginSession(String loginSession);

        /**
         * Configure the path to the token cache.  Defaults to the value of the AWS_LOGIN_CACHE_DIRECTORY
         * environment variable or if unset to HOME/.aws/login/cache.
         */
        Builder tokenCacheLocation(Path tokenCacheLocation);

        /**
         * An optional string denoting previous credentials providers that are chained with this one. This method is primarily
         * intended for use by AWS SDK internal components and should not be used directly by external users.
         */
        Builder sourceChain(String sourceChain);

        /**
         * Create a {@link LoginCredentialsProvider} using the configuration applied to this builder.
         *
         * @return
         */
        @Override
        LoginCredentialsProvider build();
    }

    protected static final class BuilderImpl implements Builder {
        private Boolean asyncCredentialUpdateEnabled = false;
        private SigninClient signinClient;
        private Duration staleTime;
        private Duration prefetchTime;
        private String loginSession;
        private String sourceChain;
        private Path tokenCacheLocation;

        BuilderImpl() {

        }

        public BuilderImpl(LoginCredentialsProvider provider) {
            this.asyncCredentialUpdateEnabled = provider.asyncCredentialUpdateEnabled;
            this.signinClient = provider.signinClient;
            this.staleTime = provider.staleTime;
            this.prefetchTime = provider.prefetchTime;
            this.loginSession = provider.loginSession;
            this.sourceChain = provider.sourceChain;
        }

        @Override
        public Builder signinClient(SigninClient signinClient) {
            this.signinClient = signinClient;
            return this;
        }

        @Override
        public Builder asyncCredentialUpdateEnabled(Boolean asyncCredentialUpdateEnabled) {
            this.asyncCredentialUpdateEnabled = asyncCredentialUpdateEnabled;
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
        public Builder loginSession(String loginSession) {
            this.loginSession = loginSession;
            return this;
        }

        @Override
        public Builder sourceChain(String sourceChain) {
            this.sourceChain = sourceChain;
            return this;
        }

        @Override
        public Builder tokenCacheLocation(Path tokenCacheLocation) {
            this.tokenCacheLocation = tokenCacheLocation;
            return this;
        }

        @Override
        public LoginCredentialsProvider build() {
            return new LoginCredentialsProvider(this);
        }
    }
}
