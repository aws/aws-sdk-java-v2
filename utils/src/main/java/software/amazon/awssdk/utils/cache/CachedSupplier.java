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

import static java.time.temporal.ChronoUnit.MINUTES;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.utils.ComparableUtils;
import software.amazon.awssdk.utils.Logger;
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
@SdkProtectedApi
public class CachedSupplier<T> implements Supplier<T>, SdkAutoCloseable {
    private static final Logger log = Logger.loggerFor(CachedSupplier.class);

    /**
     * Maximum time to wait for a blocking refresh lock before calling refresh again. This is to rate limit how many times we call
     * refresh. In the ideal case, refresh always occurs in a timely fashion and only one thread actually does the refresh.
     */
    private static final Duration BLOCKING_REFRESH_MAX_WAIT = Duration.ofSeconds(5);


    /**
     * Used as a primitive form of rate limiting for the speed of our refreshes. This will make sure that the backing supplier has
     * a period of time to update the value when the {@link RefreshResult#staleTime()} arrives without getting called by every
     * thread that initiates a {@link #get()}.
     */
    private final Lock refreshLock = new ReentrantLock();

    /**
     * The strategy we should use for pre-fetching the cached data when the {@link RefreshResult#prefetchTime()} arrives. This is
     * configured when the cache is created via {@link Builder#prefetchStrategy(PrefetchStrategy)}.
     */
    private final PrefetchStrategy prefetchStrategy;

    /**
     * Whether the {@link #prefetchStrategy} has been initialized via {@link PrefetchStrategy#initializeCachedSupplier}.
     */
    private final AtomicBoolean prefetchStrategyInitialized = new AtomicBoolean(false);

    /**
     * How the supplier should behave when the cached value is stale on retrieval or fails to be retrieved.
     */
    private final StaleValueBehavior staleValueBehavior;

    /**
     * The clock used by this supplier. Adjustable for testing.
     */
    private final Clock clock;

    /**
     * The number of consecutive failures encountered when updating a stale value.
     */
    private final AtomicInteger consecutiveStaleRetrievalFailures = new AtomicInteger(0);

    /**
     * The value currently stored in this cache.
     */
    private volatile RefreshResult<T> cachedValue;

    /**
     * The "expensive" to call supplier that is used to refresh the {@link #cachedValue}.
     */
    private final Supplier<RefreshResult<T>> valueSupplier;

    /**
     * Random instance used for jittering refresh results.
     */
    private final Random jitterRandom = new Random();

    private CachedSupplier(Builder<T> builder) {
        Validate.notNull(builder.supplier, "builder.supplier");
        Validate.notNull(builder.jitterEnabled, "builder.jitterEnabled");

        this.valueSupplier = jitteredPrefetchValueSupplier(builder.supplier, builder.jitterEnabled);
        this.prefetchStrategy = Validate.notNull(builder.prefetchStrategy, "builder.prefetchStrategy");
        this.staleValueBehavior = Validate.notNull(builder.staleValueBehavior, "builder.staleValueBehavior");
        this.clock = Validate.notNull(builder.clock, "builder.clock");
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
        RefreshResult<T> currentCachedValue = cachedValue;

        if (currentCachedValue == null) {
            return true;
        }

        if (currentCachedValue.staleTime() == null) {
            return false;
        }

        Instant now = clock.instant();
        return !now.isBefore(currentCachedValue.staleTime());
    }

    /**
     * Determines whether the cached value's prefetch time has passed and we should initiate a pre-fetch on the value using the
     * configured {@link #prefetchStrategy}.
     */
    private boolean shouldInitiateCachePrefetch() {
        RefreshResult<T> currentCachedValue = cachedValue;

        if (currentCachedValue == null) {
            return false;
        }

        if (currentCachedValue.prefetchTime() == null) {
            return false;
        }

        return !clock.instant().isBefore(currentCachedValue.prefetchTime());
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

                    if (prefetchStrategyInitialized.compareAndSet(false, true)) {
                        prefetchStrategy.initializeCachedSupplier(this);
                    }

                    try {
                        cachedValue = handleFetchedSuccess(prefetchStrategy.fetch(valueSupplier));
                    } catch (RuntimeException t) {
                        cachedValue = handleFetchFailure(t);
                    }
                }
            } finally {
                if (lockAcquired) {
                    refreshLock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted waiting to refresh a cached value.", e);
        }
    }

    /**
     * Perform necessary transformations of the successfully-fetched value based on the stale value behavior of this supplier.
     */
    private RefreshResult<T> handleFetchedSuccess(RefreshResult<T> fetch) {
        consecutiveStaleRetrievalFailures.set(0);

        Instant now = clock.instant();

        if (now.isBefore(fetch.staleTime())) {
            return fetch;
        }

        switch (staleValueBehavior) {
            case STRICT:
                Instant newStale = now.plusSeconds(1);
                log.warn(() -> "Retrieved value expiration is in the past (" + fetch.staleTime() + "). Using expiration "
                               + "of " + newStale);
                return fetch.toBuilder().staleTime(newStale).build(); // Refresh again in 1 second
            case ALLOW:
                Instant newStaleTime = jitterTime(now, Duration.ofMinutes(1), Duration.ofMinutes(10));
                log.warn(() -> "Cached value expiration has been extended to " + newStaleTime + " because the downstream "
                               + "service returned a time in the past: " + fetch.staleTime());

                return fetch.toBuilder()
                            .staleTime(newStaleTime)
                            .build();
            default:
                throw new IllegalStateException("Unknown stale-value-behavior: " + staleValueBehavior);
        }
    }

    /**
     * Perform necessary transformations of the currently-cached value based on the stale value behavior of this supplier.
     */
    private RefreshResult<T> handleFetchFailure(RuntimeException e) {
        RefreshResult<T> currentCachedValue = cachedValue;
        if (currentCachedValue == null) {
            throw e;
        }

        Instant now = clock.instant();
        if (!now.isBefore(currentCachedValue.staleTime())) {
            int numFailures = consecutiveStaleRetrievalFailures.incrementAndGet();

            switch (staleValueBehavior) {
                case STRICT:
                    throw e;
                case ALLOW:
                    Instant newStaleTime = jitterTime(now, Duration.ofMillis(1), maxStaleFailureJitter(numFailures));
                    log.warn(() -> "Cached value expiration has been extended to " + newStaleTime + " because calling the "
                                   + "downstream service failed (consecutive failures: " + numFailures + ").");

                    return currentCachedValue.toBuilder()
                                             .staleTime(newStaleTime)
                                             .build();
                default:
                    throw new IllegalStateException("Unknown stale-value-behavior: " + staleValueBehavior);
            }
        }

        return currentCachedValue;
    }

    /**
     * Wrap a value supplier with one that jitters its prefetch time.
     */
    private Supplier<RefreshResult<T>> jitteredPrefetchValueSupplier(Supplier<RefreshResult<T>> supplier,
                                                                     boolean prefetchJitterEnabled) {
        return () -> {
            RefreshResult<T> result = supplier.get();

            if (!prefetchJitterEnabled || result.prefetchTime() == null) {
                return result;
            }

            Duration maxJitter = maxPrefetchJitter(result);
            if (maxJitter.isZero()) {
                return result;
            }

            Instant newPrefetchTime = jitterTime(result.prefetchTime(), Duration.ZERO, maxJitter);
            return result.toBuilder()
                         .prefetchTime(newPrefetchTime)
                         .build();
        };
    }

    private Duration maxPrefetchJitter(RefreshResult<T> result) {
        Instant staleTime = result.staleTime() != null ? result.staleTime() : Instant.MAX;
        Instant oneMinuteBeforeStale = staleTime.minus(1, MINUTES);
        if (!result.prefetchTime().isBefore(oneMinuteBeforeStale)) {
            return Duration.ZERO;
        }

        Duration timeBetweenPrefetchAndStale = Duration.between(result.prefetchTime(), oneMinuteBeforeStale);
        if (timeBetweenPrefetchAndStale.toDays() > 365) {
            // The value will essentially never become stale. The user is likely using this for a value that should be
            // periodically refreshed on a best-effort basis. Use a 5-minute jitter range to respect their requested
            // prefetch time.
            return Duration.ofMinutes(5);
        }

        return timeBetweenPrefetchAndStale;
    }

    private Duration maxStaleFailureJitter(int numFailures) {
        long exponentialBackoffMillis = (1L << numFailures - 1) * 100;
        return ComparableUtils.minimum(Duration.ofMillis(exponentialBackoffMillis), Duration.ofSeconds(10));
    }

    private Instant jitterTime(Instant time, Duration jitterStart, Duration jitterEnd) {
        long jitterRange = jitterEnd.minus(jitterStart).toMillis();
        long jitterAmount = Math.abs(jitterRandom.nextLong() % jitterRange);
        return time.plus(jitterStart).plusMillis(jitterAmount);
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
        private Boolean jitterEnabled = true;
        private StaleValueBehavior staleValueBehavior = StaleValueBehavior.STRICT;
        private Clock clock = Clock.systemUTC();

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
         * Configure the way the cache should behave when a stale value is retrieved or when retrieving a value fails while the
         * cache is stale.
         *
         * By default, this uses {@link StaleValueBehavior#STRICT}.
         */
        public Builder<T> staleValueBehavior(StaleValueBehavior staleValueBehavior) {
            this.staleValueBehavior = staleValueBehavior;
            return this;
        }

        /**
         * Configure the clock used for this cached supplier. Configurable for testing.
         */
        @SdkTestInternalApi
        public Builder<T> clock(Clock clock) {
            this.clock = clock;
            return this;
        }

        /**
         * Whether jitter is enabled on the prefetch time. Can be disabled for testing.
         */
        @SdkTestInternalApi
        Builder<T> jitterEnabled(Boolean jitterEnabled) {
            this.jitterEnabled = jitterEnabled;
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
         * Invoke the provided supplier to retrieve the refresh result. This is useful for prefetch strategies to override when
         * they care about the refresh result.
         */
        default <T> RefreshResult<T> fetch(Supplier<RefreshResult<T>> supplier) {
            return supplier.get();
        }

        /**
         * Invoked when the prefetch strategy is registered with a {@link CachedSupplier}.
         */
        default void initializeCachedSupplier(CachedSupplier<?> cachedSupplier) {
        }

        /**
         * Free any resources associated with the strategy. This is invoked when the {@link CachedSupplier#close()} method is
         * invoked.
         */
        @Override
        default void close() {
        }
    }

    /**
     * How the cached supplier should behave when a stale value is retrieved from the underlying supplier or the underlying
     * supplier fails while the cached value is stale.
     */
    public enum StaleValueBehavior {
        /**
         * Strictly treat the stale time. Never return a stale cached value (except when the supplier returns an expired
         * value, in which case the supplier will return the value but only for a very short period of time to prevent
         * overloading the underlying supplier).
         */
        STRICT,

        /**
         * Allow stale values to be returned from the cache. Value retrieval will never fail, as long as the cache has
         * succeeded when calling the underlying supplier at least once.
         */
        ALLOW
    }
}
