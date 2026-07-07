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
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.internal.CredentialsInvalidationUtils;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;
import software.amazon.awssdk.utils.cache.CacheRefreshUtils;
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
@SdkPublicApi
public abstract class StsCredentialsProvider implements AwsCredentialsProvider, SdkAutoCloseable {
    private static final Logger log = Logger.loggerFor(StsCredentialsProvider.class);

    private static final Duration DEFAULT_STALE_TIME = Duration.ofMinutes(1);

    /**
     * The STS client that should be used for periodically updating the session credentials.
     */
    final StsClient stsClient;

    /**
     * The session cache that handles automatically updating the credentials when they get close to expiring.
     */
    private final CachedSupplier<AwsSessionCredentials> sessionCache;

    private final Duration staleTime;
    private final Duration prefetchTime;
    private final Boolean asyncCredentialUpdateEnabled;

    StsCredentialsProvider(BaseBuilder<?, ?> builder, String asyncThreadName) {
        this.stsClient = Validate.notNull(builder.stsClient, "STS client must not be null.");

        this.staleTime = Optional.ofNullable(builder.staleTime).orElse(DEFAULT_STALE_TIME);
        this.prefetchTime = builder.prefetchTime;
        if (this.prefetchTime != null) {
            Validate.isTrue(this.staleTime.compareTo(this.prefetchTime) <= 0,
                            "staleTime (%s) must be less than or equal to prefetchTime (%s).", this.staleTime, this.prefetchTime);
        }

        this.asyncCredentialUpdateEnabled = builder.asyncCredentialUpdateEnabled;
        CachedSupplier.Builder<AwsSessionCredentials> cacheBuilder =
            CachedSupplier.builder(this::updateSessionCredentials)
                          .cachedValueName(toString())
                          .staleValueBehavior(CachedSupplier.StaleValueBehavior.ALLOW);
        if (builder.asyncCredentialUpdateEnabled) {
            cacheBuilder.prefetchStrategy(new NonBlocking(asyncThreadName));
        }
        this.sessionCache = cacheBuilder.build();
    }

    /**
     * Update the expiring session credentials by calling STS. Invoked by {@link CachedSupplier} when the credentials
     * are close to expiring.
     */
    private RefreshResult<AwsSessionCredentials> updateSessionCredentials() {
        AwsSessionCredentials credentials = getUpdatedCredentials(stsClient);
        Instant actualTokenExpiration =
            credentials.expirationTime()
                       .orElseThrow(() -> new IllegalStateException("Sourced credentials have no expiration value"));

        Instant now = Instant.now();
        Duration effectivePrefetchWindow = CacheRefreshUtils.computePrefetchWindow(actualTokenExpiration, prefetchTime, now);

        return RefreshResult.builder(credentials)
                            .staleTime(actualTokenExpiration.minus(staleTime))
                            .prefetchTime(actualTokenExpiration.minus(effectivePrefetchWindow))
                            .build();
    }

    @Override
    public AwsCredentials resolveCredentials() {
        AwsSessionCredentials credentials = sessionCache.get();
        credentials.expirationTime().ifPresent(t -> {
            log.debug(() -> "Using STS credentials with expiration time of " + t);
        });
        return credentials;
    }

    @Override
    public void close() {
        sessionCache.close();
    }

    @Override
    public CompletableFuture<Void> invalidate(AwsCredentialsIdentity identity) {
        return CredentialsInvalidationUtils.invalidateCredentialsCache(
            identity, sessionCache, cachedCreds -> (AwsCredentialsIdentity) cachedCreds);
    }

    /**
     * The amount of time, relative to credential expiration, that defines the mandatory refresh window. When credentials are
     * within this window, all threads will block until the credentials are updated.
     */
    public Duration staleTime() {
        return staleTime;
    }

    /**
     * The amount of time, relative to credential expiration, that defines the advisory refresh window. When credentials are
     * within this window, the provider proactively attempts to refresh them.
     */
    public Duration prefetchTime() {
        return prefetchTime;
    }

    @Override
    public String toString() {
        return ToString.create(providerName());
    }

    /**
     * Implemented by a child class to call STS and get a new set of credentials to be used by this provider.
     */
    abstract AwsSessionCredentials getUpdatedCredentials(StsClient stsClient);

    abstract String providerName();

    /**
     * Extended by child class's builders to share configuration across credential providers.
     */
    @NotThreadSafe
    @SdkPublicApi
    public abstract static class BaseBuilder<B extends BaseBuilder<B, T>, T extends ToCopyableBuilder<B, T>>
        implements CopyableBuilder<B, T> {
        private final Function<B, T> providerConstructor;

        private Boolean asyncCredentialUpdateEnabled = false;
        private StsClient stsClient;
        private Duration staleTime;
        private Duration prefetchTime;

        BaseBuilder(Function<B, T> providerConstructor) {
            this.providerConstructor = providerConstructor;
        }

        BaseBuilder(Function<B, T> providerConstructor, StsCredentialsProvider provider) {
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
         * Configure whether the provider should fetch credentials asynchronously in the background. When enabled, a
         * dedicated thread performs credential refreshes during the advisory refresh window (defined by
         * {@link #prefetchTime(Duration)}), so that callers are less likely to block waiting for credentials. Additional
         * resources (a thread) are used to maintain the provider.
         *
         * <p>Regardless of this setting, callers will block if credentials enter the mandatory refresh window (defined by
         * {@link #staleTime(Duration)}).
         *
         * <p>By default, this is disabled.</p>
         */
        @SuppressWarnings("unchecked")
        public B asyncCredentialUpdateEnabled(Boolean asyncCredentialUpdateEnabled) {
            this.asyncCredentialUpdateEnabled = asyncCredentialUpdateEnabled;
            return (B) this;
        }

        /**
         * Configure the amount of time, relative to credential expiration, that defines the mandatory refresh window. When
         * the cached credentials are within this window (i.e., their remaining lifetime is less than this duration), the
         * provider will block all callers until a refresh attempt completes. If the refresh attempt fails, the provider
         * returns the cached credentials and will not attempt another refresh until a backoff period has elapsed.
         *
         * <p>This value must be less than or equal to {@link #prefetchTime(Duration)}. Setting this equal to
         * {@code prefetchTime} effectively disables prefetch, causing all refreshes to be mandatory (blocking).
         *
         * <p>By default, this is 1 minute.</p>
         *
         * @param staleTime the duration before expiration that triggers mandatory (blocking) refresh
         */
        @SuppressWarnings("unchecked")
        public B staleTime(Duration staleTime) {
            this.staleTime = staleTime;
            return (B) this;
        }

        /**
         * Configure the amount of time, relative to credential expiration, that defines the advisory refresh window. When
         * the cached credentials are within this window (i.e., their remaining lifetime is less than this duration), the
         * provider will attempt to refresh them proactively. If the refresh fails, the provider returns the existing cached
         * credentials without error and will not attempt another refresh until a backoff period has elapsed.
         *
         * <p>When {@link #asyncCredentialUpdateEnabled(Boolean)} is true, advisory refreshes happen in a background thread
         * and callers immediately receive the current cached credentials. When it is false, one caller will block to perform
         * the refresh while other callers receive the current cached credentials.
         *
         * <p>This value must be greater than or equal to {@link #staleTime(Duration)}. Setting this equal to
         * {@code staleTime} effectively disables prefetch, causing all refreshes to be mandatory (blocking).
         *
         * <p>If not explicitly set, the advisory refresh window is computed dynamically based on the credential's
         * remaining lifetime: 5 minutes for credentials with less than 20 minutes remaining, 15 minutes for 20-90
         * minutes remaining, and 60 minutes for 90+ minutes remaining. This dynamic window is recomputed on each
         * successful refresh.</p>
         *
         * @param prefetchTime the duration before expiration that triggers advisory (proactive) refresh
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

        /**
         * Whether the provider should fetch credentials asynchronously in the background.
         * <p>By default, this is false.</p>
         */
        Boolean asyncCredentialUpdateEnabled() {
            return asyncCredentialUpdateEnabled;
        }
    }
}
