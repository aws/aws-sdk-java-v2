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

package software.amazon.awssdk.utils.cache;

import java.time.Instant;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * A wrapper for the value returned by the {@link Supplier} underlying a {@link CachedSupplier}. The underlying {@link Supplier}
 * returns this to specify when the underlying value should be refreshed.
 */
@SdkProtectedApi
public final class RefreshResult<T> implements ToCopyableBuilder<RefreshResult.Builder<T>, RefreshResult<T>> {
    private final T value;
    private final Instant staleTime;
    private final Instant prefetchTime;
    private final Instant expiration;

    private RefreshResult(Builder<T> builder) {
        this.value = builder.value;
        this.staleTime = builder.staleTime;
        this.prefetchTime = builder.prefetchTime;
        this.expiration = builder.expiration;
    }

    /**
     * Get a builder for creating a {@link RefreshResult}.
     *
     * @param value The value that should be cached by the supplier.
     */
    public static <T> Builder<T> builder(T value) {
        return new Builder<>(value);
    }

    /**
     * The value resulting from the refresh.
     */
    public T value() {
        return value;
    }

    /**
     * When the configured value is stale and should no longer be used. All threads will block until the value is updated.
     */
    public Instant staleTime() {
        return staleTime;
    }

    /**
     * When the configured value is getting close to stale and should be updated using the supplier's
     * {@link CachedSupplier#prefetchStrategy}.
     */
    public Instant prefetchTime() {
        return prefetchTime;
    }

    /**
     * When the value actually expires, or null if not specified. Unlike {@link #staleTime()}, which can be earlier to force a
     * blocking refresh before expiry, this is the true expiration. With {@link CachedSupplier.StaleValueBehavior#ALLOW}, a
     * fetched value is treated as a failed refresh only if this time is at or before now. If not specified,
     * {@link #staleTime()} is used instead.
     */
    public Instant expiration() {
        return expiration;
    }

    @Override
    public RefreshResult.Builder<T> toBuilder() {
        return new RefreshResult.Builder<>(this);
    }

    /**
     * A builder for a {@link RefreshResult}.
     */
    public static final class Builder<T> implements CopyableBuilder<Builder<T>, RefreshResult<T>> {
        private final T value;
        private Instant staleTime = Instant.MAX;
        private Instant prefetchTime = Instant.MAX;
        private Instant expiration;

        private Builder(T value) {
            this.value = value;
        }

        private Builder(RefreshResult<T> value) {
            this.value = value.value;
            this.staleTime = value.staleTime;
            this.prefetchTime = value.prefetchTime;
            this.expiration = value.expiration;
        }

        /**
         * Specify the time at which the value in this cache is stale, and all calls to {@link CachedSupplier#get()} should block
         * to try to update the value.
         *
         * If this isn't specified, all threads will never block to update the value.
         */
        public Builder<T> staleTime(Instant staleTime) {
            this.staleTime = staleTime;
            return this;
        }

        /**
         * Specify the time at which a thread that calls {@link CachedSupplier#get()} should trigger a cache prefetch. The
         * exact behavior of a "prefetch" is defined when the cache is created with
         * {@link CachedSupplier.Builder#prefetchStrategy(CachedSupplier.PrefetchStrategy)}, and may either have one thread block
         * to refresh the cache or have an asynchronous task reload the value in the background.
         *
         * If this isn't specified, the prefetch strategy will never be used and all threads will block to update the value when
         * the {@link #staleTime(Instant)} arrives.
         */
        public Builder<T> prefetchTime(Instant prefetchTime) {
            this.prefetchTime = prefetchTime;
            return this;
        }

        /**
         * Specify the time at which the value actually expires. This can be later than the {@link #staleTime(Instant)}, which
         * may be set earlier to force a blocking refresh before the value expires.
         *
         * <p>With {@link CachedSupplier.StaleValueBehavior#ALLOW}, a fetched value whose stale time has passed is still used
         * as long as this expiration has not passed. Only a value whose expiration is at or before now is treated as a failed
         * refresh. {@link CachedSupplier.StaleValueBehavior#STRICT} ignores this value.
         *
         * If this isn't specified, the {@link #staleTime(Instant)} is treated as the expiration.
         */
        public Builder<T> expiration(Instant expiration) {
            this.expiration = expiration;
            return this;
        }

        /**
         * Build a {@link RefreshResult} using the values currently configured in this builder.
         */
        public RefreshResult<T> build() {
            return new RefreshResult<>(this);
        }
    }
}
