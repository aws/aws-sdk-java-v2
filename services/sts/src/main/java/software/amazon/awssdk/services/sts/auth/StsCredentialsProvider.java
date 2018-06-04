/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.util.function.Function;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.services.sts.STSClient;
import software.amazon.awssdk.services.sts.model.Credentials;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.cache.CachedSupplier;
import software.amazon.awssdk.utils.cache.NonBlocking;
import software.amazon.awssdk.utils.cache.RefreshResult;

/**
 * An implementation of {@link AwsCredentialsProvider} that is extended within this package to provide support for periodically-
 * updating session credentials. When credentials get close to expiration, this class will attempt to update them asynchronously
 * using {@link #getUpdatedCredentials(STSClient)}. If the credentials end up expiring, this class will block all calls to
 * {@link #getCredentials()} until the credentials can be updated.
 */
@ThreadSafe
@SdkInternalApi
abstract class StsCredentialsProvider implements AwsCredentialsProvider, SdkAutoCloseable {
    /**
     * The STS client that should be used for periodically updating the session credentials in the background.
     */
    private final STSClient stsClient;

    /**
     * The session cache that will update the credentials asynchronously in the background when they get close to expiring.
     */
    private final CachedSupplier<SessionCredentialsHolder> sessionCache;

    protected StsCredentialsProvider(BaseBuilder<?, ?> builder, String asyncThreadName) {
        this.stsClient = Validate.notNull(builder.stsClient, "STS client must not be null.");

        CachedSupplier.Builder<SessionCredentialsHolder> cacheBuilder = CachedSupplier.builder(this::updateSessionCredentials);
        if (builder.asyncCredentialUpdateEnabled) {
            cacheBuilder.prefetchStrategy(new NonBlocking(asyncThreadName));
        }
        this.sessionCache = cacheBuilder.build();
    }

    /**
     * Update the expiring session credentials by calling STS. Invoked by {@link CachedSupplier} when the credentials are close to
     * expiring.
     */
    private RefreshResult<SessionCredentialsHolder> updateSessionCredentials() {
        SessionCredentialsHolder credentials = new SessionCredentialsHolder(getUpdatedCredentials(stsClient));
        Instant actualTokenExpiration = credentials.getSessionCredentialsExpiration().toInstant();
        return RefreshResult.builder(credentials)
                            .staleTime(actualTokenExpiration.minus(Duration.ofMinutes(1)))
                            .prefetchTime(actualTokenExpiration.minus(Duration.ofMinutes(5)))
                            .build();
    }

    @Override
    public AwsCredentials getCredentials() {
        return sessionCache.get().getSessionCredentials();
    }

    @Override
    public void close() {
        sessionCache.close();
    }

    /**
     * Implemented by a child class to call STS and get a new set of credentials to be used by this provider.
     */
    protected abstract Credentials getUpdatedCredentials(STSClient stsClient);

    /**
     * Extended by child class's builders to share configuration across credential providers.
     */
    @NotThreadSafe
    protected abstract static class BaseBuilder<B extends BaseBuilder<B, T>, T> {
        private final Function<B, T> providerConstructor;

        private Boolean asyncCredentialUpdateEnabled = false;
        private STSClient stsClient;

        protected BaseBuilder(Function<B, T> providerConstructor) {
            this.providerConstructor = providerConstructor;
        }

        /**
         * Configure the {@link STSClient} to use when calling STS to update the session. This client should not be shut down
         * as long as this credentials provider is in use.
         *
         * @param stsClient The STS client to use for communication with STS.
         * @return This object for chained calls.
         */
        @SuppressWarnings("unchecked")
        public B stsClient(STSClient stsClient) {
            this.stsClient = stsClient;
            return (B) this;
        }

        /**
         * Configure whether the provider should fetch credentials asynchronously in the background. If this is true, threads are
         * less likely to block when credentials are loaded, but additional resources are used to maintain the provider.
         *
         * <p>By default, this is disabled.</p>
         */
        @SuppressWarnings("unchecked")
        public B asyncCredentialUpdateEnabled(Boolean asyncCredentialUpdateEnabled) {
            this.asyncCredentialUpdateEnabled = asyncCredentialUpdateEnabled;
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
