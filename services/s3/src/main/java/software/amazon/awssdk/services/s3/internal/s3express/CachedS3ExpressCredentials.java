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

package software.amazon.awssdk.services.s3.internal.s3express;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.services.s3.model.SessionCredentials;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.cache.CachedSupplier;
import software.amazon.awssdk.utils.cache.NonBlocking;
import software.amazon.awssdk.utils.cache.RefreshResult;

/**
 * This class represents a single cached S3Express credential. It utilizes the existing class {@link CachedSupplier}
 * to wrap the credential value and provide refresh capabilities, set with the {@link NonBlocking} prefetch strategy
 * in order to get asynchronous refresh with a common thread pool.
 * <p>
 * Each time the value is requested through the {@link #get()} method, the class delegates to the cached supplier to get
 * the value. The cached supplier uses the {@link #refreshResult(Function, S3ExpressIdentityKey)} function to retrieve the
 * value.
 * <p>
 * Stale time - the time before, and relative to, the expiration time that the credentials are considered stale (invalid)
 * Prefetch time - the time before, and relative, the expiration time that the credentials are eligible for refresh
 * <pre>
 *       prefetch   stale expiration
 * ---------|---------|---|---------------
 *          <-------->
 *           refresh
 * </pre>
 * <p>
 * Make sure to {@link #close} instances when they are no longer used.
 */
@SdkInternalApi
public final class CachedS3ExpressCredentials implements SdkAutoCloseable {

    //Credentials currently have a max expiration time of 5 minutes
    private static final Duration DEFAULT_EXPIRATION_TIME = Duration.ofMinutes(5);
    private static final Duration DEFAULT_STALE_TIME = Duration.ofSeconds(15);
    private static final Duration DEFAULT_PREFETCH_TIME = Duration.ofSeconds(60);
    private final Duration staleTime;
    private final Duration prefetchTime;
    private final Function<S3ExpressIdentityKey, SessionCredentials> credentialsSupplier;
    private final S3ExpressIdentityKey key;
    private final CachedSupplier<SessionCredentials> sessionCache;
    private final NonBlocking prefetchStrategy;

    private CachedS3ExpressCredentials(Builder builder) {
        this.credentialsSupplier = builder.supplier;
        this.key = builder.key;
        this.staleTime = Optional.ofNullable(builder.staleTime).orElse(DEFAULT_STALE_TIME);
        this.prefetchTime = Optional.ofNullable(builder.prefetchTime).orElse(DEFAULT_PREFETCH_TIME);
        this.prefetchStrategy = new NonBlocking("s3-express-credentials");
        this.sessionCache = CachedSupplier.builder(() -> refreshResult(credentialsSupplier, key))
                                          .prefetchStrategy(prefetchStrategy)
                                          .build();
    }

    public static Builder builder(Function<S3ExpressIdentityKey, SessionCredentials> supplier) {
        return new Builder(supplier);
    }

    @SdkTestInternalApi
    NonBlocking prefetchStrategy() {
        return prefetchStrategy;
    }

    public SessionCredentials get() {
        return sessionCache.get();
    }

    private RefreshResult<SessionCredentials> refreshResult(
        Function<S3ExpressIdentityKey, SessionCredentials> identitySupplier, S3ExpressIdentityKey key) {

        SessionCredentials newCredentials = identitySupplier.apply(key);
        Instant expirationTime = newCredentials.expiration();
        if (expirationTime == null) {
            expirationTime = Instant.now().plus(DEFAULT_EXPIRATION_TIME);
        }
        return RefreshResult.builder(newCredentials)
                     .prefetchTime(expirationTime.minus(prefetchTime))
                     .staleTime(expirationTime.minus(staleTime))
                     .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CachedS3ExpressCredentials that = (CachedS3ExpressCredentials) o;

        return key != null ? key.equals(that.key) : that.key == null;
    }

    @Override
    public int hashCode() {
        return key != null ? key.hashCode() : 0;
    }

    @Override
    public void close() {
        sessionCache.close();
    }

    public static final class Builder {

        private final Function<S3ExpressIdentityKey, SessionCredentials> supplier;
        private Duration staleTime;
        private Duration prefetchTime;
        private S3ExpressIdentityKey key;

        private Builder(Function<S3ExpressIdentityKey, SessionCredentials> supplier) {
            this.supplier = supplier;
        }

        public CachedS3ExpressCredentials.Builder key(S3ExpressIdentityKey key) {
            this.key = key;
            return this;
        }

        public CachedS3ExpressCredentials.Builder staleTime(Duration staleTime) {
            this.staleTime = staleTime;
            return this;
        }

        public CachedS3ExpressCredentials.Builder prefetchTime(Duration prefetchTime) {
            this.prefetchTime = prefetchTime;
            return this;
        }

        public CachedS3ExpressCredentials build() {
            return new CachedS3ExpressCredentials(this);
        }
    }
}
