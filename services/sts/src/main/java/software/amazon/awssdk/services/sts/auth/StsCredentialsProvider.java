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

package software.amazon.awssdk.services.sts.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Function;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.Credentials;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;
import software.amazon.awssdk.utils.cache.CachedSupplier;
import software.amazon.awssdk.utils.cache.NonBlocking;
import software.amazon.awssdk.utils.cache.RefreshResult;


/**
 * An implementation of {@link AwsCredentialsProvider} that is extended within this package to provide support for periodically-
 * updating session credentials.
 *
 * When credentials get close to expiration, this class will attempt to update them automatically either with a single calling
 * thread (by default) or asynchronously (if {@link #asyncCredentialUpdateEnabled} is true). If the credentials expire, this
 * class will block all calls to {@link #resolveCredentials()} until the credentials are updated.
 *
 * Users of this provider must {@link #close()} it when they are finished using it.
 */
@ThreadSafe
@SdkInternalApi
abstract class StsCredentialsProvider implements AwsCredentialsProvider, SdkAutoCloseable {

    private static final Duration DEFAULT_STALE_TIME = Duration.ofMinutes(1);
    private static final Duration DEFAULT_PREFETCH_TIME = Duration.ofMinutes(5);

    /**
     * The STS client that should be used for periodically updating the session credentials.
     */
    final StsClient stsClient;

    /**
     * The session cache that handles automatically updating the credentials when they get close to expiring.
     */
    private final CachedSupplier<SessionCredentialsHolder> sessionCache;

    private final Duration staleTime;
    private final Duration prefetchTime;
    private final Boolean asyncCredentialUpdateEnabled;

    protected StsCredentialsProvider(BaseBuilder<?, ?> builder, String asyncThreadName) {
        this.stsClient = Validate.notNull(builder.stsClient, "STS client must not be null.");

        this.staleTime = Optional.ofNullable(builder.staleTime).orElse(DEFAULT_STALE_TIME);
        this.prefetchTime = Optional.ofNullable(builder.prefetchTime).orElse(DEFAULT_PREFETCH_TIME);

        this.asyncCredentialUpdateEnabled = builder.asyncCredentialUpdateEnabled;
        CachedSupplier.Builder<SessionCredentialsHolder> cacheBuilder = CachedSupplier.builder(this::updateSessionCredentials);
        if (builder.asyncCredentialUpdateEnabled) {
            cacheBuilder.prefetchStrategy(new NonBlocking(asyncThreadName));
        }
        this.sessionCache = cacheBuilder.build();
    }

    /**
     * Update the expiring session credentials by calling STS. Invoked by {@link CachedSupplier} when the credentials
     * are close to expiring.
     */
    private RefreshResult<SessionCredentialsHolder> updateSessionCredentials() {
        SessionCredentialsHolder credentials = new SessionCredentialsHolder(getUpdatedCredentials(stsClient));
        Instant actualTokenExpiration = credentials.getSessionCredentialsExpiration().toInstant();

        return RefreshResult.builder(credentials)
                            .staleTime(actualTokenExpiration.minus(staleTime))
                            .prefetchTime(actualTokenExpiration.minus(prefetchTime))
                            .build();
    }

    @Override
    public AwsCredentials resolveCredentials() {
        return sessionCache.get().getSessionCredentials();
    }

    @Override
    public void close() {
        sessionCache.close();
    }

    /**
     * The amount of time, relative to STS token expiration, that the cached credentials are considered stale and
     * should no longer be used. All threads will block until the value is updated.
     */
    public Duration staleTime() {
        return staleTime;
    }

    /**
     * The amount of time, relative to STS token expiration, that the cached credentials are considered close to stale
     * and should be updated.
     */
    public Duration prefetchTime() {
        return prefetchTime;
    }

    /**
     * Implemented by a child class to call STS and get a new set of credentials to be used by this provider.
     */
    protected abstract Credentials getUpdatedCredentials(StsClient stsClient);

    /**
     * Extended by child class's builders to share configuration across credential providers.
     */
    @NotThreadSafe
    protected abstract static class BaseBuilder<B extends BaseBuilder<B, T>, T extends ToCopyableBuilder<B, T>>
        implements CopyableBuilder<B, T> {
        private final Function<B, T> providerConstructor;

        private Boolean asyncCredentialUpdateEnabled = false;
        private StsClient stsClient;
        private Duration staleTime;
        private Duration prefetchTime;

        protected BaseBuilder(Function<B, T> providerConstructor) {
            this.providerConstructor = providerConstructor;
        }

        public BaseBuilder(Function<B, T> providerConstructor, StsCredentialsProvider provider) {
            this.providerConstructor = providerConstructor;
            this.asyncCredentialUpdateEnabled = provider.asyncCredentialUpdateEnabled;
            this.stsClient = provider.stsClient;
            this.staleTime = provider.staleTime;
            this.prefetchTime = provider.prefetchTime;
        }

        /**
         * Configure the {@link StsClient} to use when calling STS to update the session. This client should not be shut
         * down as long as this credentials provider is in use.
         *
         * @param stsClient The STS client to use for communication with STS.
         * @return This object for chained calls.
         */
        @SuppressWarnings("unchecked")
        public B stsClient(StsClient stsClient) {
            this.stsClient = stsClient;
            return (B) this;
        }

        /**
         * Configure whether the provider should fetch credentials asynchronously in the background. If this is true,
         * threads are less likely to block when credentials are loaded, but additional resources are used to maintain
         * the provider.
         *
         * <p>By default, this is disabled.</p>
         */
        @SuppressWarnings("unchecked")
        public B asyncCredentialUpdateEnabled(Boolean asyncCredentialUpdateEnabled) {
            this.asyncCredentialUpdateEnabled = asyncCredentialUpdateEnabled;
            return (B) this;
        }

        /**
         * Configure the amount of time, relative to STS token expiration, that the cached credentials are considered
         * stale and must be updated. All threads will block until the value is updated.
         *
         * <p>By default, this is 1 minute.</p>
         */
        @SuppressWarnings("unchecked")
        public B staleTime(Duration staleTime) {
            this.staleTime = staleTime;
            return (B) this;
        }

        /**
         * Configure the amount of time, relative to STS token expiration, that the cached credentials are considered
         * close to stale and should be updated.
         *
         * Prefetch updates will occur between the specified time and the stale time of the provider. Prefetch updates may be
         * asynchronous. See {@link #asyncCredentialUpdateEnabled}.
         *
         * <p>By default, this is 5 minutes.</p>
         */
        @SuppressWarnings("unchecked")
        public B prefetchTime(Duration prefetchTime) {
            this.prefetchTime = prefetchTime;
            return (B) this;
        }


        /**
         * Build the credentials provider using the configuration applied to this builder.
         */
        @SuppressWarnings("unchecked")
        public T build() {
            return providerConstructor.apply((B) this);
        }
    }
}
