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

package software.amazon.awssdk.services.sso.auth;

import static software.amazon.awssdk.utils.Validate.notNull;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.services.sso.SsoClient;
import software.amazon.awssdk.services.sso.internal.SessionCredentialsHolder;
import software.amazon.awssdk.services.sso.model.GetRoleCredentialsRequest;
import software.amazon.awssdk.services.sso.model.RoleCredentials;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.cache.CachedSupplier;
import software.amazon.awssdk.utils.cache.NonBlocking;
import software.amazon.awssdk.utils.cache.RefreshResult;

/**
 * <p>
 * An implementation of {@link AwsCredentialsProvider} that is extended within this package to provide support for
 * periodically updating session credentials. This credential provider maintains a {@link Supplier<GetRoleCredentialsRequest>}
 * for a {@link SsoClient#getRoleCredentials(Consumer)} call to retrieve the credentials needed.
 * </p>
 *
 * <p>
 * While creating the {@link GetRoleCredentialsRequest}, an access token is needed to be resolved from a token file.
 * In default, the token is assumed unexpired, and if it's expired then an {@link ExpiredTokenException} will be thrown.
 * If the users want to change the behavior of this, please implement your own token resolving logic and override the
 * {@link Builder#refreshRequest).
 * </p>
 *
 * <p>
 * When credentials get close to expiration, this class will attempt to update them asynchronously. If the credentials
 * end up expiring, this class will block all calls to {@link #resolveCredentials()} until the credentials can be updated.
 * </p>
 */
@SdkPublicApi
public final class SsoCredentialsProvider implements AwsCredentialsProvider, SdkAutoCloseable {

    private static final Duration DEFAULT_STALE_TIME = Duration.ofMinutes(1);
    private static final Duration DEFAULT_PREFETCH_TIME = Duration.ofMinutes(5);

    private static final String ASYNC_THREAD_NAME = "sdk-sso-credentials-provider";

    private final Supplier<GetRoleCredentialsRequest> getRoleCredentialsRequestSupplier;

    private final SsoClient ssoClient;
    private final Duration staleTime;
    private final Duration prefetchTime;

    private final CachedSupplier<SessionCredentialsHolder> credentialCache;

    /**
     * @see #builder()
     */
    private SsoCredentialsProvider(BuilderImpl builder) {
        this.ssoClient = notNull(builder.ssoClient, "SSO client must not be null.");
        this.getRoleCredentialsRequestSupplier = builder.getRoleCredentialsRequestSupplier;

        this.staleTime = Optional.ofNullable(builder.staleTime).orElse(DEFAULT_STALE_TIME);
        this.prefetchTime = Optional.ofNullable(builder.prefetchTime).orElse(DEFAULT_PREFETCH_TIME);

        CachedSupplier.Builder<SessionCredentialsHolder> cacheBuilder = CachedSupplier.builder(this::updateSsoCredentials);
        if (builder.asyncCredentialUpdateEnabled) {
            cacheBuilder.prefetchStrategy(new NonBlocking(ASYNC_THREAD_NAME));
        }

        this.credentialCache = cacheBuilder.build();
    }

    /**
     * Update the expiring session SSO credentials by calling SSO. Invoked by {@link CachedSupplier} when the credentials
     * are close to expiring.
     */
    private RefreshResult<SessionCredentialsHolder> updateSsoCredentials() {
        SessionCredentialsHolder credentials = getUpdatedCredentials(ssoClient);
        Instant acutalTokenExpiration = credentials.sessionCredentialsExpiration();

        return RefreshResult.builder(credentials)
                            .staleTime(acutalTokenExpiration.minus(staleTime))
                            .prefetchTime(acutalTokenExpiration.minus(prefetchTime))
                            .build();
    }

    private SessionCredentialsHolder getUpdatedCredentials(SsoClient ssoClient) {
        GetRoleCredentialsRequest request = getRoleCredentialsRequestSupplier.get();
        notNull(request, "GetRoleCredentialsRequest can't be null.");
        RoleCredentials roleCredentials = ssoClient.getRoleCredentials(request).roleCredentials();
        AwsSessionCredentials sessionCredentials = AwsSessionCredentials.create(roleCredentials.accessKeyId(),
                                                                                roleCredentials.secretAccessKey(),
                                                                                roleCredentials.sessionToken());
        return new SessionCredentialsHolder(sessionCredentials, Instant.ofEpochMilli(roleCredentials.expiration()));
    }

    /**
     * The amount of time, relative to session token expiration, that the cached credentials are considered stale and
     * should no longer be used. All threads will block until the value is updated.
     */
    public Duration staleTime() {
        return staleTime;
    }

    /**
     * The amount of time, relative to session token expiration, that the cached credentials are considered close to stale
     * and should be updated.
     */
    public Duration prefetchTime() {
        return prefetchTime;
    }

    /**
     * Get a builder for creating a custom {@link SsoCredentialsProvider}.
     */
    public static BuilderImpl builder() {
        return new BuilderImpl();
    }

    @Override
    public AwsCredentials resolveCredentials() {
        return credentialCache.get().sessionCredentials();
    }

    @Override
    public void close() {
        credentialCache.close();
    }

    /**
     * A builder for creating a custom {@link SsoCredentialsProvider}.
     */
    public interface Builder {

        /**
         * Configure the {@link SsoClient} to use when calling SSO to update the session. This client should not be shut
         * down as long as this credentials provider is in use.
         */
        Builder ssoClient(SsoClient ssoclient);

        /**
         * Configure whether the provider should fetch credentials asynchronously in the background. If this is true,
         * threads are less likely to block when credentials are loaded, but addtiional resources are used to maintian
         * the provider.
         *
         * <p>By default, this is disabled.</p>
         */
        Builder asyncCredentialUpdateEnabled(Boolean asyncCredentialUpdateEnabled);

        /**
         * Configure the amount of time, relative to SSO session token expiration, that the cached credentials are considered
         * stale and should no longer be used. All threads will block until the value is updated.
         *
         * <p>By default, this is 1 minute.</p>
         */
        Builder staleTime(Duration staleTime);

        /**
         * Configure the amount of time, relative to SSO session token expiration, that the cached credentials are considered
         * close to stale and should be updated. See {@link #asyncCredentialUpdateEnabled}.
         *
         * <p>By default, this is 5 minutes.</p>
         */
        Builder prefetchTime(Duration prefetchTime);

        /**
         * Configure the {@link GetRoleCredentialsRequest} that should be periodically sent to the SSO service to update the
         * credentials.
         */
        Builder refreshRequest(GetRoleCredentialsRequest getRoleCredentialsRequest);

        /**
         * Similar to {@link #refreshRequest(GetRoleCredentialsRequest)}, but takes a {@link Supplier} to supply the request to
         * SSO.
         */
        Builder refreshRequest(Supplier<GetRoleCredentialsRequest> getRoleCredentialsRequestSupplier);

        /**
         * Create a {@link SsoCredentialsProvider} using the configuration applied to this builder.
         * @return
         */
        SsoCredentialsProvider build();

    }

    protected static final class BuilderImpl implements Builder {
        private Boolean asyncCredentialUpdateEnabled = false;
        private SsoClient ssoClient;
        private Duration staleTime;
        private Duration prefetchTime;
        private Supplier<GetRoleCredentialsRequest> getRoleCredentialsRequestSupplier;

        BuilderImpl() {

        }

        @Override
        public Builder ssoClient(SsoClient ssoClient) {
            this.ssoClient = ssoClient;
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
        public Builder refreshRequest(GetRoleCredentialsRequest getRoleCredentialsRequest) {
            return refreshRequest(() -> getRoleCredentialsRequest);
        }

        @Override
        public Builder refreshRequest(Supplier<GetRoleCredentialsRequest> getRoleCredentialsRequestSupplier) {
            this.getRoleCredentialsRequestSupplier = getRoleCredentialsRequestSupplier;
            return this;
        }

        @Override
        public SsoCredentialsProvider build() {
            return new SsoCredentialsProvider(this);
        }

    }
}
