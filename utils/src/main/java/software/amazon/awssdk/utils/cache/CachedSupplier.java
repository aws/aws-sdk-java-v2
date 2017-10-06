/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.Validate;

/**
 * A wrapper for a {@link Supplier} that applies certain caching rules to the retrieval of its value, including customizable
 * pre-fetching behaviors for updating values as they get close to expiring so that not all threads have to block to update the
 * value.
 *
 * For example, the {@link OneCallerBlocks} strategy will have a single caller block to update the value, and the
 * {@link NonBlocking} strategy maintains a thread pool for updating the value asynchronously in the background.
 *
 * This should be created using {@link #builder(Supplier)}.
 */
public final class CachedSupplier<T> implements Supplier<T>, SdkAutoCloseable {
    /**
     * Maximum time to wait for a blocking refresh lock before calling refresh again. This is to rate limit how many times we call
     * refresh. In the ideal case, refresh always occurs in a timely fashion and only one thread actually does the refresh.
     */
    private static final Duration BLOCKING_REFRESH_MAX_WAIT = Duration.ofSeconds(5);

    /**
     * Used as a primitive form of rate limiting for the speed of our refreshes. This will make sure that the backing supplier has
     * a period of time to update the value when the {@link RefreshResult#staleTime} arrives without getting called by every
     * thread that initiates a {@link #get()}.
     */
    private final Lock refreshLock = new ReentrantLock();

    /**
     * The strategy we should use for pre-fetching the cached data when the {@link RefreshResult#prefetchTime} arrives. This is
     * configured when the cache is created via {@link Builder#prefetchStrategy(PrefetchStrategy)}.
     */
    private final PrefetchStrategy prefetchStrategy;

    /**
     * The value currently stored in this cache.
     */
    private volatile RefreshResult<T> cachedValue = RefreshResult.builder((T) null)
                                                                 .staleTime(Instant.MIN)
                                                                 .prefetchTime(Instant.MIN)
                                                                 .build();

    /**
     * The "expensive" to call supplier that is used to refresh the {@link #cachedValue}.
     */
    private final Supplier<RefreshResult<T>> valueSupplier;

    private CachedSupplier(Builder<T> builder) {
        this.valueSupplier = Validate.notNull(builder.supplier, "builder.supplier");
        this.prefetchStrategy = Validate.notNull(builder.prefetchStrategy, "builder.prefetchStrategy");
    }

    /**
     * Retrieve a builder that can be used for creating a {@link CachedSupplier}.
     *
     * @param valueSupplier The value supplier that should have its value cached.
     */
    public static <T> CachedSupplier.Builder<T> builder(Supplier<RefreshResult<T>> valueSupplier) {
        return new CachedSupplier.Builder<>(valueSupplier);
    }

    @Override
    public T get() {
        if (cacheIsStale()) {
            refreshCache();
        } else if (shouldInitiateCachePrefetch()) {
            prefetchCache();
        }

        return this.cachedValue.value();
    }

    /**
     * Determines whether the value in this cache is stale, and all threads should block and wait for an updated value.
     */
    private boolean cacheIsStale() {
        return Instant.now().isAfter(cachedValue.staleTime());
    }

    /**
     * Determines whether the cached value's prefetch time has passed and we should initiate a pre-fetch on the value using the
     * configured {@link #prefetchStrategy}.
     */
    private boolean shouldInitiateCachePrefetch() {
        return Instant.now().isAfter(cachedValue.prefetchTime());
    }

    /**
     * Initiate a pre-fetch of the data using the configured {@link #prefetchStrategy}.
     */
    private void prefetchCache() {
        prefetchStrategy.prefetch(this::refreshCache);
    }

    /**
     * Perform a blocking refresh of the cached value. This will rate limit synchronous refresh calls based on the
     * {@link #BLOCKING_REFRESH_MAX_WAIT} time. This ensures that when the data needs to be updated, we won't immediately hammer
     * the underlying value refresher if it can get back to us in a reasonable time.
     */
    private void refreshCache() {
        try {
            boolean lockAcquired = refreshLock.tryLock(BLOCKING_REFRESH_MAX_WAIT.getSeconds(), TimeUnit.SECONDS);

            try {
                // Make sure the value was not refreshed while we waited for the lock.
                if (cacheIsStale() || shouldInitiateCachePrefetch()) {
                    // It wasn't, call the supplier to update it.
                    cachedValue = valueSupplier.get();
                }
            } finally {
                if (lockAcquired) {
                    refreshLock.unlock();
                }
            }
        } catch (InterruptedException e) {
            handleInterruptedException("Interrupted waiting to refresh the value.", e);
        }
    }

    @ReviewBeforeRelease("Should this throw a different exception, like AbortedException, from the core?")
    private void handleInterruptedException(String message, InterruptedException cause) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(message, cause);
    }

    /**
     * Free any resources consumed by the prefetch strategy this supplier is using.
     */
    @Override
    public void close() {
        prefetchStrategy.close();
    }

    /**
     * A Builder for {@link CachedSupplier}, created by {@link #builder(Supplier)}.
     */
    public static final class Builder<T> {
        private final Supplier<RefreshResult<T>> supplier;
        private PrefetchStrategy prefetchStrategy = new OneCallerBlocks();

        private Builder(Supplier<RefreshResult<T>> supplier) {
            this.supplier = supplier;
        }

        /**
         * Configure the way in which data in the cache should be pre-fetched when the data's {@link RefreshResult#prefetchTime()}
         * arrives.
         *
         * By default, this uses the {@link OneCallerBlocks} strategy, which will block a single {@link #get()} caller to update
         * the value.
         */
        public Builder<T> prefetchStrategy(PrefetchStrategy prefetchStrategy) {
            this.prefetchStrategy = prefetchStrategy;
            return this;
        }

        /**
         * Create a {@link CachedSupplier} using the current configuration of this builder.
         */
        public CachedSupplier<T> build() {
            return new CachedSupplier<>(this);
        }
    }

    /**
     * The way in which the cache should be pre-fetched when the data's {@link RefreshResult#prefetchTime()} arrives.
     *
     * @see OneCallerBlocks
     * @see NonBlocking
     */
    @FunctionalInterface
    public interface PrefetchStrategy extends SdkAutoCloseable {
        /**
         * Execute the provided value updater to update the cache. The specific implementation defines how this is invoked.
         */
        void prefetch(Runnable valueUpdater);

        /**
         * Free any resources associated with the strategy. This is invoked when the {@link CachedSupplier#close()} method is
         * invoked.
         */
        default void close() {}
    }
}
