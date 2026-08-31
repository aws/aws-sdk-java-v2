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

import static java.time.Instant.now;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;
import static software.amazon.awssdk.utils.cache.CachedSupplier.StaleValueBehavior.ALLOW;
import static software.amazon.awssdk.utils.cache.CachedSupplier.StaleValueBehavior.STRICT;

import java.io.Closeable;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.utils.cache.CachedSupplier.StaleValueBehavior;

/**
 * Validate the functionality of {@link CachedSupplier}.
 */
public class CachedSupplierTest {

    // --- Constants mirroring CachedSupplier's internal values, for test readability ---

    /** Minimum static stability backoff duration in seconds (5 minutes). */
    private static final long BACKOFF_MIN_SECONDS = 300;

    /** Maximum static stability backoff duration in seconds (10 minutes). */
    private static final long BACKOFF_MAX_SECONDS = 600;

    /** Minimum duration (seconds) that a non-recoverable error stays cached. */
    private static final long NON_RECOVERABLE_ERROR_CACHE_MIN_SECONDS = 1;

    /** Maximum duration (seconds) that a non-recoverable error stays cached. */
    private static final long NON_RECOVERABLE_ERROR_CACHE_MAX_SECONDS = 5;

    /** A duration safely past the non-recoverable error cache max, guaranteeing the cache has expired. */
    private static final long PAST_NON_RECOVERABLE_ERROR_CACHE = NON_RECOVERABLE_ERROR_CACHE_MAX_SECONDS + 1;

    /**
     * A duration guaranteed to fall strictly inside the non-recoverable error cache window, whichever duration the
     * jitter picked. This must stay below the cache minimum: the cache expires exactly at its expiry instant, so
     * advancing by the minimum itself lands on an already-expired cache whenever the jitter picks that minimum.
     */
    private static final long WITHIN_NON_RECOVERABLE_ERROR_CACHE_MILLIS =
        NON_RECOVERABLE_ERROR_CACHE_MIN_SECONDS * 1000 - 100;

    /** A duration safely past the maximum backoff, guaranteeing the backoff has elapsed. */
    private static final long PAST_MAX_BACKOFF = BACKOFF_MAX_SECONDS + 1;

    /** Long prefetch time (seconds) used in tests where credentials have a 1-hour stale time and 5-minute advisory window. */
    private static final long LONG_PREFETCH_SECONDS = 300;
    /**
     * An executor for performing "get" on the cached supplier asynchronously. This, along with the {@link WaitingSupplier} allows
     * near-manual scheduling of threads so that we can test that the cache is only calling the underlying supplier when we want
     * it to.
     */
    private ExecutorService executorService;

    /**
     * All executions added to the {@link #executorService} since the beginning of an individual test method.
     */
    private List<Future<?>> allExecutions;

    /**
     * Create an executor service for async testing.
     */
    @BeforeEach
    public void setup() {
        executorService = Executors.newFixedThreadPool(50);
        allExecutions = new ArrayList<>();
    }

    /**
     * Shut down the executor service when we're done.
     */
    @AfterEach
    public void shutdown() {
        executorService.shutdown();
    }

    private static class MutableSupplier implements Supplier<RefreshResult<String>> {
        private volatile RuntimeException thingToThrow;
        private volatile RefreshResult<String> thingToReturn;

        @Override
        public RefreshResult<String> get() {
            if (thingToThrow != null) {
                throw thingToThrow;
            }
            return thingToReturn;
        }

        private MutableSupplier set(RuntimeException exception) {
            this.thingToThrow = exception;
            this.thingToReturn = null;
            return this;
        }

        private MutableSupplier set(RefreshResult<String> value) {
            this.thingToThrow = null;
            this.thingToReturn = value;
            return this;
        }
    }

    /**
     * An implementation of {@link Supplier} that allows us to (more or less) manually schedule threads so that we can make sure
     * the CachedSupplier is only calling the underlying supplier when we expect it to.
     */
    private static class WaitingSupplier implements Supplier<RefreshResult<String>>, Closeable {
        /**
         * A semaphore that is counted up each time a "get" is started. This is useful during testing for waiting for a certain
         * number of "gets" to start.
         */
        private final Semaphore startedGetPermits = new Semaphore(0);

        /**
         * A semaphore that is counted down each time a "get" is started. This is useful during testing for blocking the threads
         * performing the "get" until it is time for them to complete.
         */
        private final Semaphore permits = new Semaphore(0);

        /**
         * A semaphore that is counted up each time a "get" is finished. This is useful during testing for waiting for a certain
         * number of "gets" to finish.
         */
        private final Semaphore finishedGetPermits = new Semaphore(0);

        private final Supplier<Instant> staleTime;
        private final Supplier<Instant> prefetchTime;

        private WaitingSupplier(Instant staleTime, Instant prefetchTime) {
            this(() -> staleTime, () -> prefetchTime);
        }

        private WaitingSupplier(Supplier<Instant> staleTime, Supplier<Instant> prefetchTime) {
            this.staleTime = staleTime;
            this.prefetchTime = prefetchTime;
        }

        @Override
        public RefreshResult<String> get() {
            startedGetPermits.release(1);

            try {
                permits.acquire(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
                fail();
            }

            finishedGetPermits.release(1);
            return RefreshResult.builder("value")
                                .staleTime(staleTime.get())
                                .prefetchTime(prefetchTime.get())
                                .build();
        }

        /**
         * Wait for a certain number of "gets" to have started. This will time out and fail the test after a certain amount of
         * time if the "gets" never actually start.
         */
        public void waitForGetsToHaveStarted(int numExpectedGets) {
            assertTrue(invokeSafely(() -> startedGetPermits.tryAcquire(numExpectedGets, 10, TimeUnit.SECONDS)));
        }

        /**
         * Wait for a certain number of "gets" to have finished. This will time out and fail the test after a certain amount of
         * time if the "gets" never finish.
         */
        public void waitForGetsToHaveFinished(int numExpectedGets) {
            assertTrue(invokeSafely(() -> finishedGetPermits.tryAcquire(numExpectedGets, 10, TimeUnit.SECONDS)));
        }

        /**
         * Release all threads blocked in this supplier.
         */
        @Override
        public void close() {
            permits.release(50);
        }
    }

    @Test
    public void allCallsBeforeInitializationBlock() {
        try (WaitingSupplier waitingSupplier = new WaitingSupplier(future(), future())) {
            CachedSupplier<String> cachedSupplier = CachedSupplier.builder(waitingSupplier).build();

            // Perform two "gets".
            performAsyncGets(cachedSupplier, 2);

            // Make sure both "gets" are started.
            waitingSupplier.waitForGetsToHaveStarted(2);
        }
    }

    @Test
    public void staleValueBlocksAllCalls() throws InterruptedException {
        AdjustableClock clock = new AdjustableClock();
        try (WaitingSupplier waitingSupplier = new WaitingSupplier(() -> now().plus(1, ChronoUnit.MINUTES), this::future)) {
            CachedSupplier<String> cachedSupplier = CachedSupplier.builder(waitingSupplier)
                                                                  .clock(clock)
                                                                  .build();

            // Perform one successful "get".
            waitingSupplier.permits.release(1);
            clock.time = now();
            waitFor(performAsyncGet(cachedSupplier));

            // Perform two "get"s that will attempt to refresh the value, and wait for them to get stuck.
            clock.time = now().plus(61, ChronoUnit.SECONDS);
            List<Future<?>> futures = performAsyncGets(cachedSupplier, 2);
            waitingSupplier.waitForGetsToHaveStarted(3);
            Thread.sleep(1_000);
            assertThat(futures).allMatch(f -> !f.isDone());

            // Release any "gets" that blocked and wait for them to finish.
            waitingSupplier.permits.release(50);
            waitForAsyncGetsToFinish();

            // Make extra sure all 3 "gets" actually happened.
            waitingSupplier.waitForGetsToHaveFinished(3);
        }
    }

    @Test
    public void staleValueBlocksAllCallsEvenWithStaleValuesAllowed() throws InterruptedException {
        // This test case may seem unintuitive: why block for a stale value refresh if we allow stale values to be used? We do
        // this because values may become stale from disuse in sync prefetch strategies. If there's a new value available, we'd
        // still like to hold threads a little to give them a chance at a non-stale value.

        AdjustableClock clock = new AdjustableClock();
        try (WaitingSupplier waitingSupplier = new WaitingSupplier(() -> now().plus(1, ChronoUnit.MINUTES), this::future)) {
            CachedSupplier<String> cachedSupplier = CachedSupplier.builder(waitingSupplier)
                                                                  .clock(clock)
                                                                  .staleValueBehavior(ALLOW)
                                                                  .build();

            // Perform one successful "get".
            waitingSupplier.permits.release(1);
            clock.time = now();
            waitFor(performAsyncGet(cachedSupplier));

            // Perform two "get"s that will attempt to refresh the value, and wait for them to get stuck.
            clock.time = now().plus(61, ChronoUnit.SECONDS);
            List<Future<?>> futures = performAsyncGets(cachedSupplier, 2);
            waitingSupplier.waitForGetsToHaveStarted(3);
            Thread.sleep(1_000);
            assertThat(futures).allMatch(f -> !f.isDone());

            // Release any "gets" that blocked and wait for them to finish.
            waitingSupplier.permits.release(50);
            waitForAsyncGetsToFinish();

            // Make extra sure all 3 "gets" actually happened.
            waitingSupplier.waitForGetsToHaveFinished(3);
        }
    }

    @Test
    public void firstRetrieveFailureThrowsForStrictStaleMode() {
        firstRetrievalFails(STRICT);
    }

    @Test
    public void firstRetrieveFailureThrowsForAllowStaleMode() {
        firstRetrievalFails(ALLOW);
    }

    private void firstRetrievalFails(StaleValueBehavior staleValueBehavior) {
        RuntimeException e = new RuntimeException();
        try (CachedSupplier<?> cachedSupplier = CachedSupplier.builder(() -> { throw e; })
                                                              .staleValueBehavior(staleValueBehavior)
                                                              .build()) {
            assertThatThrownBy(cachedSupplier::get).isEqualTo(e);
        }
    }

    @Test
    public void prefetchThrowIsHiddenIfValueIsNotStaleForStrictMode() {
        prefetchThrowIsHiddenIfValueIsNotStale(STRICT);
    }

    @Test
    public void prefetchThrowIsHiddenIfValueIsNotStaleForAllowMode() {
        prefetchThrowIsHiddenIfValueIsNotStale(ALLOW);
    }

    private void prefetchThrowIsHiddenIfValueIsNotStale(StaleValueBehavior staleValueBehavior) {
        MutableSupplier supplier = new MutableSupplier();
        try (CachedSupplier<?> cachedSupplier = CachedSupplier.builder(supplier)
                                                              .staleValueBehavior(staleValueBehavior)
                                                              .build()) {
            supplier.set(RefreshResult.builder("")
                                      .prefetchTime(now())
                                      .build());

            assertThat(cachedSupplier.get()).isEqualTo("");

            supplier.set(new RuntimeException());

            assertThat(cachedSupplier.get()).isEqualTo("");
        }
    }

    @Test
    public void valueIsCachedForAShortTimeIfValueIsStaleInStrictMode() throws Throwable {
        MutableSupplier supplier = new MutableSupplier();
        try (CachedSupplier<?> cachedSupplier = CachedSupplier.builder(supplier)
                                                              .staleValueBehavior(STRICT)
                                                              .build()) {
            supplier.set(RefreshResult.builder("")
                                      .staleTime(now())
                                      .build());

            assertThat(cachedSupplier.get()).isEqualTo("");

            RuntimeException e = new RuntimeException();
            supplier.set(e);

            assertThat(cachedSupplier.get()).isEqualTo("");
        }
    }

    @Test
    public void throwIsPropagatedIfValueIsStaleInStrictMode() throws InterruptedException {
        MutableSupplier supplier = new MutableSupplier();
        try (CachedSupplier<?> cachedSupplier = CachedSupplier.builder(supplier)
                                                              .staleValueBehavior(STRICT)
                                                              .build()) {
            supplier.set(RefreshResult.builder("")
                                      .staleTime(now())
                                      .build());

            assertThat(cachedSupplier.get()).isEqualTo("");

            RuntimeException e = new RuntimeException();
            supplier.set(e);

            Thread.sleep(1001); // Wait to avoid the light rate-limiting we apply
            assertThatThrownBy(cachedSupplier::get).isEqualTo(e);
        }
    }

    @Test
    public void throwIsHiddenIfValueIsStaleInAllowMode() throws InterruptedException {
        MutableSupplier supplier = new MutableSupplier();
        try (CachedSupplier<?> cachedSupplier = CachedSupplier.builder(supplier)
                                                              .staleValueBehavior(ALLOW)
                                                              .build()) {
            supplier.set(RefreshResult.builder("")
                                      .staleTime(now().plusSeconds(1))
                                      .build());

            assertThat(cachedSupplier.get()).isEqualTo("");

            RuntimeException e = new RuntimeException();
            supplier.set(e);

            Thread.sleep(1000);
            assertThat(cachedSupplier.get()).isEqualTo("");
        }
    }

    @Test
    public void allowMode_returnsCachedValueOnNonCacheInvalidatingFailure() throws InterruptedException {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        try (CachedSupplier<String> cachedSupplier = CachedSupplier.builder(supplier)
                                                                   .staleValueBehavior(ALLOW)
                                                                   .clock(clock)
                                                                   .prefetchJitterEnabled(false)
                                                                   .build()) {
            Instant now = Instant.now();
            clock.time = now;

            // Initial successful fetch
            supplier.set(RefreshResult.builder("cached-creds")
                                      .staleTime(now.plusSeconds(60))
                                      .prefetchTime(now.plusSeconds(30))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("cached-creds");

            // Advance past stale time
            clock.time = now.plusSeconds(61);
            supplier.set(new RuntimeException("service unavailable"));

            // Should return cached value instead of throwing
            assertThat(cachedSupplier.get()).isEqualTo("cached-creds");
        }
    }

    @Test
    public void allowMode_cacheInvalidatingError_isRethrown() throws InterruptedException {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        try (CachedSupplier<String> cachedSupplier = CachedSupplier.builder(supplier)
                                                                   .staleValueBehavior(ALLOW)
                                                                   .nonRecoverableErrorPredicate(
                                                                       e -> e instanceof CacheInvalidatingRuntimeException)
                                                                   .clock(clock)
                                                                   .prefetchJitterEnabled(false)
                                                                   .build()) {
            Instant now = Instant.now();
            clock.time = now;

            // Initial successful fetch
            supplier.set(RefreshResult.builder("cached-creds")
                                      .staleTime(now.plusSeconds(60))
                                      .prefetchTime(now.plusSeconds(30))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("cached-creds");

            // Advance past stale time and throw cache-invalidating error
            clock.time = now.plusSeconds(61);
            CacheInvalidatingRuntimeException invalidatingError =
                new CacheInvalidatingRuntimeException("token expired");
            supplier.set(invalidatingError);

            // Should re-throw even though cached value exists
            assertThatThrownBy(cachedSupplier::get).isEqualTo(invalidatingError);
        }
    }

    @Test
    public void allowMode_backoffIsInExpectedRange() throws InterruptedException {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();

        // Run multiple iterations to verify backoff range
        for (int i = 0; i < 50; i++) {
            try (CachedSupplier<String> cachedSupplier = CachedSupplier.builder(supplier)
                                                                       .staleValueBehavior(ALLOW)
                                                                       .clock(clock)
                                                                       .prefetchJitterEnabled(false)
                                                                       .build()) {
                Instant now = Instant.parse("2024-01-01T00:00:00Z");
                clock.time = now;

                supplier.set(RefreshResult.builder("cached-creds")
                                          .staleTime(now.plusSeconds(60))
                                          .prefetchTime(now.plusSeconds(30))
                                          .build());
                cachedSupplier.get();

                // Advance past stale time and trigger failure
                clock.time = now.plusSeconds(61);
                supplier.set(new RuntimeException("service unavailable"));
                cachedSupplier.get();

                // Now nextAllowedRefreshTime is set to now(61) + [300,600]s
                // The cached value should be returned while rate limited
                Instant minBackoffEnd = now.plusSeconds(61 + BACKOFF_MIN_SECONDS);
                Instant maxBackoffEnd = now.plusSeconds(61 + BACKOFF_MAX_SECONDS);

                // Advance just before the minimum backoff end - should still be rate limited
                clock.time = minBackoffEnd.minusSeconds(1);
                supplier.set(RefreshResult.builder("new-creds")
                                          .staleTime(Instant.MAX)
                                          .prefetchTime(Instant.MAX)
                                          .build());
                // Rate limited: returns cached value without contacting source
                assertThat(cachedSupplier.get()).isEqualTo("cached-creds");

                // Advance past maximum possible backoff - rate limit expired, will refresh
                clock.time = maxBackoffEnd.plusSeconds(1);
                assertThat(cachedSupplier.get()).isEqualTo("new-creds");
            }
        }
    }

    @Test
    public void allowMode_prefetchWindowFailure_setsBackoffGate() {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        try (CachedSupplier<String> cachedSupplier = CachedSupplier.builder(supplier)
                                                                   .staleValueBehavior(ALLOW)
                                                                   .clock(clock)
                                                                   .prefetchJitterEnabled(false)
                                                                   .build()) {
            Instant now = Instant.parse("2024-01-01T00:00:00Z");
            clock.time = now;

            // Initial successful fetch with prefetch in the future, stale much later
            supplier.set(RefreshResult.builder("cached-creds")
                                      .staleTime(now.plusSeconds(3600))
                                      .prefetchTime(now.plusSeconds(60))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("cached-creds");

            // Advance past prefetch time but before stale time
            clock.time = now.plusSeconds(61);
            supplier.set(new RuntimeException("service unavailable"));

            // Should return cached value (not throw) and set nextAllowedRefreshTime
            assertThat(cachedSupplier.get()).isEqualTo("cached-creds");

            // Verify that a subsequent call shortly after does NOT attempt another refresh
            // (because nextAllowedRefreshTime was set as a backoff gate)
            clock.time = now.plusSeconds(62);
            supplier.set(RefreshResult.builder("should-not-get-this")
                                      .staleTime(Instant.MAX)
                                      .prefetchTime(Instant.MAX)
                                      .build());
            // The rate limit is active, so this should still return cached
            assertThat(cachedSupplier.get()).isEqualTo("cached-creds");
        }
    }

    @Test
    public void allowMode_prefetchWindowFailure_preservesStaleTime() {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        try (CachedSupplier<String> cachedSupplier = CachedSupplier.builder(supplier)
                                                                   .staleValueBehavior(ALLOW)
                                                                   .clock(clock)
                                                                   .prefetchJitterEnabled(false)
                                                                   .build()) {
            Instant now = Instant.parse("2024-01-01T00:00:00Z");
            clock.time = now;

            // Initial successful fetch: stale at +3600s (1 hour), prefetch at +60s
            Instant originalStaleTime = now.plusSeconds(3600);
            supplier.set(RefreshResult.builder("cached-creds")
                                      .staleTime(originalStaleTime)
                                      .prefetchTime(now.plusSeconds(60))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("cached-creds");

            // Advance past prefetch time but well before stale time
            clock.time = now.plusSeconds(61);
            supplier.set(new RuntimeException("service unavailable"));

            // Trigger failure during prefetch window
            assertThat(cachedSupplier.get()).isEqualTo("cached-creds");

            // Advance past the maximum possible backoff (61 + 600 = 661s from now) but still before stale time (3600s).
            // The nextAllowedRefreshTime backoff will have elapsed, so a prefetch refresh will be attempted.
            clock.time = now.plusSeconds(61 + PAST_MAX_BACKOFF);
            supplier.set(RefreshResult.builder("refreshed-creds")
                                      .staleTime(Instant.MAX)
                                      .prefetchTime(Instant.MAX)
                                      .build());
            // Backoff elapsed, prefetchTime (60s) is in the past, so prefetch is triggered and succeeds
            assertThat(cachedSupplier.get()).isEqualTo("refreshed-creds");
        }
    }

    @Test
    public void allowMode_prefetchWindowFailure_cacheInvalidatingError_isRethrown() {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        try (CachedSupplier<String> cachedSupplier = CachedSupplier.builder(supplier)
                                                                   .staleValueBehavior(ALLOW)
                                                                   .nonRecoverableErrorPredicate(
                                                                       e -> e instanceof CacheInvalidatingRuntimeException)
                                                                   .clock(clock)
                                                                   .prefetchJitterEnabled(false)
                                                                   .build()) {
            Instant now = Instant.parse("2024-01-01T00:00:00Z");
            clock.time = now;

            // Initial successful fetch with prefetch in the future, stale much later
            supplier.set(RefreshResult.builder("cached-creds")
                                      .staleTime(now.plusSeconds(3600))
                                      .prefetchTime(now.plusSeconds(60))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("cached-creds");

            // Advance past prefetch time but before stale time
            clock.time = now.plusSeconds(61);
            CacheInvalidatingRuntimeException invalidatingError =
                new CacheInvalidatingRuntimeException("token expired");
            supplier.set(invalidatingError);

            // Should re-throw cache-invalidating error even in prefetch window
            assertThatThrownBy(cachedSupplier::get).isEqualTo(invalidatingError);
        }
    }

    /**
     * A RuntimeException that represents a cache-invalidating error for testing.
     */
    private static class CacheInvalidatingRuntimeException extends RuntimeException {
        CacheInvalidatingRuntimeException(String message) {
            super(message);
        }
    }

    @Test
    public void basicCachingWorks() {
        try (WaitingSupplier waitingSupplier = new WaitingSupplier(future(), future())) {
            CachedSupplier<String> cachedSupplier = CachedSupplier.builder(waitingSupplier).build();

            // Perform 5 "gets".
            waitingSupplier.permits.release(5);
            waitFor(performAsyncGets(cachedSupplier, 5));

            // Make extra sure only 1 "get" actually happened.
            waitingSupplier.waitForGetsToHaveFinished(1);
        }
    }

    @Test
    public void oneCallerBlocksPrefetchStrategyWorks() throws InterruptedException {
        try (WaitingSupplier waitingSupplier = new WaitingSupplier(future(), past())) {
            CachedSupplier<String> cachedSupplier = CachedSupplier.builder(waitingSupplier)
                                                                  .prefetchStrategy(new OneCallerBlocks())
                                                                  .prefetchJitterEnabled(false)
                                                                  .build();

            // Perform one successful "get" to prime the cache.
            waitingSupplier.permits.release(1);
            waitFor(performAsyncGet(cachedSupplier));

            // Perform one "get" that will attempt to refresh the value, and wait for that one to get stuck.
            performAsyncGet(cachedSupplier);
            waitingSupplier.waitForGetsToHaveStarted(2);

            // Perform a successful "get" because one is already blocked to refresh.
            waitFor(performAsyncGet(cachedSupplier));

            // Release any "gets" that blocked and wait for them to finish.
            waitingSupplier.permits.release(50);
            waitForAsyncGetsToFinish();

            // Make extra sure only 2 "gets" actually happened.
            waitingSupplier.waitForGetsToHaveFinished(2);
        }
    }

    @Test
    public void nonBlockingPrefetchStrategyWorks() {
        try (WaitingSupplier waitingSupplier = new WaitingSupplier(future(), past());
             CachedSupplier<String> cachedSupplier = CachedSupplier.builder(waitingSupplier)
                                                                   .prefetchStrategy(new NonBlocking("test-%s"))
                                                                   .prefetchJitterEnabled(false)
                                                                   .build()) {
            // Perform one successful "get" to prime the cache.
            waitingSupplier.permits.release(1);
            waitFor(performAsyncGet(cachedSupplier));

            // Perform one successful "get" to kick off the async refresh.
            waitFor(performAsyncGet(cachedSupplier));

            // Wait for the async "get" in the background to start (if it hasn't already).
            waitingSupplier.waitForGetsToHaveStarted(2);

            // Make sure only one "get" has actually happened (the async get is currently waiting to be released).
            waitingSupplier.waitForGetsToHaveFinished(1);
        }
    }

    @Test
    public void nonBlockingPrefetchStrategyRefreshesInBackground() {
        try (WaitingSupplier waitingSupplier = new WaitingSupplier(now().plusSeconds(62), now().plusSeconds(1));
             CachedSupplier<String> cachedSupplier = CachedSupplier.builder(waitingSupplier)
                                                                   .prefetchStrategy(new NonBlocking("test-%s"))
                                                                   .prefetchJitterEnabled(false)
                                                                   .build()) {
            waitingSupplier.permits.release(2);
            cachedSupplier.get();

            // Ensure two "get"s happens even though we only made one call to the cached supplier.
            waitingSupplier.waitForGetsToHaveStarted(2);

            assertThat(cachedSupplier.get()).isNotNull();
        }
    }

    @Test
    public void nonBlockingPrefetchStrategyHasOneMinuteMinimumByDefault() {
        try (WaitingSupplier waitingSupplier = new WaitingSupplier(now().plusSeconds(60), now());
             CachedSupplier<String> cachedSupplier = CachedSupplier.builder(waitingSupplier)
                                                                   .prefetchStrategy(new NonBlocking("test-%s"))
                                                                   .build()) {
            waitingSupplier.permits.release(2);
            cachedSupplier.get();

            // Ensure two "get"s happens even though we only made one call to the cached supplier.
            assertThat(invokeSafely(() -> waitingSupplier.startedGetPermits.tryAcquire(2, 2, TimeUnit.SECONDS))).isFalse();
        }
    }

    @Test
    public void nonBlockingPrefetchStrategyBackgroundRefreshesHitCache() throws InterruptedException {
        try (WaitingSupplier waitingSupplier = new WaitingSupplier(future(), future());
             CachedSupplier<String> cachedSupplier = CachedSupplier.builder(waitingSupplier)
                                                                   .prefetchStrategy(new NonBlocking("test-%s"))
                                                                   .build()) {
            waitingSupplier.permits.release(5);
            cachedSupplier.get();

            Thread.sleep(1_000);

            assertThat(waitingSupplier.permits.availablePermits()).isEqualTo(4); // Only 1 call to supplier
        }
    }

    @Test
    public void nonBlockingPrefetchStrategyDoesNotRefreshUntilItIsCalled() throws InterruptedException {
        try (WaitingSupplier waitingSupplier = new WaitingSupplier(future(), past());
             CachedSupplier<String> cachedSupplier = CachedSupplier.builder(waitingSupplier)
                                                                   .prefetchStrategy(new NonBlocking("test-%s"))
                                                                   .build()) {
            waitingSupplier.startedGetPermits.release();

            Thread.sleep(1_000);

            assertThat(waitingSupplier.startedGetPermits.availablePermits()).isEqualTo(1);
        }
    }

    @Test
    public void threadsAreSharedBetweenNonBlockingInstances() throws InterruptedException {
        int maxActive = runAndCountThreads(() -> {
            List<CachedSupplier<?>> css = new ArrayList<>();
            for (int i = 0; i < 99; i++) {
                CachedSupplier<?> supplier =
                    CachedSupplier.builder(() -> RefreshResult.builder("foo")
                                                              .prefetchTime(now().plusMillis(10))
                                                              .staleTime(future())
                                                              .build())
                                  .prefetchStrategy(new NonBlocking("test"))
                                  .prefetchJitterEnabled(false)
                                  .build();
                supplier.get();
                css.add(supplier);
            }
            return css;
        });

        assertThat(maxActive).isBetween(1, 99);
    }

    @Test
    public void activeThreadsHaveMaxCount() throws InterruptedException {
        ExecutorService executor = Executors.newCachedThreadPool();
        try {
            int maxActive = runAndCountThreads(() -> {
                List<CachedSupplier<?>> css = new ArrayList<>();

                // Create 1000 concurrent non-blocking instances
                for (int i = 0; i < 1000; i++) {
                    CachedSupplier<String> supplier =
                        CachedSupplier.builder(() -> {
                                          invokeSafely(() -> Thread.sleep(100));
                                          return RefreshResult.builder("foo")
                                                              .prefetchTime(now().plusMillis(10))
                                                              .staleTime(now().plusSeconds(60))
                                                              .build();
                                      }).prefetchStrategy(new NonBlocking("test"))
                                      .prefetchJitterEnabled(false)
                                      .build();
                    executor.submit(supplier::get);
                    css.add(supplier);
                }

                executor.shutdown();
                assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
                return css;
            });

            assertThat(maxActive).isBetween(2, 150);
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Run the provided supplier, measure the non-blocking executor thread count, and return the result. If the result is 0,
     * try again. This makes our stochastic tests ~100% reliable instead of ~99%.
     */
    private int runAndCountThreads(ThrowingSupplier suppliersConstructor) throws InterruptedException {
        for (int attempt = 0; attempt < 10; attempt++) {
            Collection<CachedSupplier<?>> suppliers = emptyList();
            try {
                suppliers = suppliersConstructor.get();

                int maxActive = 0;
                for (int j = 0; j < 1000; j++) {
                    maxActive = Math.max(maxActive, NonBlocking.executor().getActiveCount());
                    Thread.sleep(1);
                }

                if (maxActive != 0) {
                    return maxActive;
                }
            } finally {
                suppliers.forEach(CachedSupplier::close);
            }
        }

        throw new AssertionError("Thread count never exceeded 0.");
    }

    @FunctionalInterface
    interface ThrowingSupplier {
        Collection<CachedSupplier<?>> get() throws InterruptedException;
    }

    /**
     * Asynchronously perform a "get" on the provided supplier, returning the future that will be completed when the "get"
     * finishes.
     */
    private Future<?> performAsyncGet(CachedSupplier<?> supplier) {
        return executorService.submit(supplier::get);
    }

    /**
     * Asynchronously perform multiple "gets" on the provided supplier, returning the collection of futures to be completed when
     * the "get" finishes.
     */
    private List<Future<?>> performAsyncGets(CachedSupplier<?> supplier, int count) {
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < count; ++i) {
            futures.add(performAsyncGet(supplier));
        }
        allExecutions.addAll(futures);
        return futures;
    }

    /**
     * Wait for the provided future to complete, failing the test if it does not.
     */
    private void waitFor(Future<?> future) {
        invokeSafely(() -> future.get(10, TimeUnit.SECONDS));
    }

    /**
     * Wait for all futures in the provided collection fo complete, failing the test if they do not all complete.
     */
    private void waitFor(Collection<Future<?>> futures) {
        futures.forEach(this::waitFor);
    }

    /**
     * Wait for all async gets ever created by this class to complete, failing the test if they do not all complete.
     */
    private void waitForAsyncGetsToFinish() {
        waitFor(allExecutions);
    }

    private Instant past() {
        return now().minusSeconds(1);
    }

    private Instant future() {
        return Instant.MAX;
    }

    private static class AdjustableClock extends Clock {
        private Instant time;

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Instant instant() {
            return time;
        }
    }

    // --- invalidate() tests ---

    @Test
    public void invalidate_predicateMatches_triggersRefresh() {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;

        try (CachedSupplier<String> cache = CachedSupplier.builder(supplier)
                                                          .staleValueBehavior(ALLOW)
                                                          .clock(clock)
                                                          .prefetchJitterEnabled(false)
                                                          .build()) {
            supplier.set(RefreshResult.builder("value-1").staleTime(now.plusSeconds(3600)).prefetchTime(now.plusSeconds(1800)).build());
            assertThat(cache.get()).isEqualTo("value-1");

            clock.time = now.plusSeconds(10);
            cache.invalidate(v -> v.equals("value-1"));

            supplier.set(RefreshResult.builder("value-2").staleTime(now.plusSeconds(7200)).prefetchTime(now.plusSeconds(5400)).build());
            assertThat(cache.get()).isEqualTo("value-2");
        }
    }

    @Test
    public void invalidate_predicateDoesNotMatch_doesNotTriggerRefresh() {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;

        try (CachedSupplier<String> cache = CachedSupplier.builder(supplier)
                                                          .staleValueBehavior(ALLOW)
                                                          .clock(clock)
                                                          .prefetchJitterEnabled(false)
                                                          .build()) {
            supplier.set(RefreshResult.builder("value-1").staleTime(now.plusSeconds(3600)).prefetchTime(now.plusSeconds(1800)).build());
            assertThat(cache.get()).isEqualTo("value-1");

            cache.invalidate(v -> v.equals("different-value"));

            supplier.set(RefreshResult.builder("value-2").staleTime(now.plusSeconds(7200)).prefetchTime(now.plusSeconds(5400)).build());
            assertThat(cache.get()).isEqualTo("value-1");
        }
    }

    @Test
    public void invalidate_beforeFirstGet_isNoOp() {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        clock.time = Instant.parse("2024-01-01T00:00:00Z");

        try (CachedSupplier<String> cache = CachedSupplier.builder(supplier)
                                                          .staleValueBehavior(ALLOW)
                                                          .clock(clock)
                                                          .prefetchJitterEnabled(false)
                                                          .build()) {
            cache.invalidate(v -> true); // should not throw

            supplier.set(RefreshResult.builder("value-1").staleTime(Instant.MAX).prefetchTime(Instant.MAX).build());
            assertThat(cache.get()).isEqualTo("value-1");
        }
    }

    @Test
    public void invalidate_doesNotBypassRefreshBackoff() {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;

        try (CachedSupplier<String> cache = CachedSupplier.builder(supplier)
                                                          .staleValueBehavior(ALLOW)
                                                          .clock(clock)
                                                          .prefetchJitterEnabled(false)
                                                          .build()) {
            supplier.set(RefreshResult.builder("old").staleTime(now.plusSeconds(60)).prefetchTime(now.plusSeconds(30)).build());
            assertThat(cache.get()).isEqualTo("old");

            // Trigger failure to set backoff
            clock.time = now.plusSeconds(61);
            supplier.set(new RuntimeException("unavailable"));
            assertThat(cache.get()).isEqualTo("old");

            // Invalidate — marks stale but doesn't clear backoff
            clock.time = now.plusSeconds(62);
            cache.invalidate(v -> v.equals("old"));
            supplier.set(RefreshResult.builder("new").staleTime(Instant.MAX).prefetchTime(Instant.MAX).build());

            // Still within backoff — returns stale
            assertThat(cache.get()).isEqualTo("old");

            // Past backoff — returns fresh
            clock.time = now.plusSeconds(62 + PAST_MAX_BACKOFF);
            assertThat(cache.get()).isEqualTo("new");
        }
    }

    @Test
    public void invalidate_concurrentWithGet_doesNotCorrupt() throws Exception {
        AdjustableClock clock = new AdjustableClock();
        clock.time = Instant.parse("2024-01-01T00:00:00Z");
        AtomicInteger counter = new AtomicInteger(0);

        try (CachedSupplier<String> cache = CachedSupplier.builder(() ->
                 RefreshResult.builder("v-" + counter.incrementAndGet())
                              .staleTime(Instant.MAX)
                              .prefetchTime(Instant.MAX)
                              .build())
                 .staleValueBehavior(ALLOW)
                 .clock(clock)
                 .prefetchJitterEnabled(false)
                 .build()) {

            cache.get(); // prime

            ExecutorService executor = Executors.newFixedThreadPool(10);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<?>> futures = new ArrayList<>();

            for (int i = 0; i < 10; i++) {
                int idx = i;
                futures.add(executor.submit(() -> {
                    try { start.await(); } catch (InterruptedException e) { return; }
                    for (int j = 0; j < 50; j++) {
                        if (idx % 2 == 0) {
                            cache.invalidate(v -> true);
                        } else {
                            assertThat(cache.get()).isNotNull();
                        }
                    }
                }));
            }

            start.countDown();
            for (Future<?> f : futures) { f.get(30, TimeUnit.SECONDS); }
            executor.shutdown();

            assertThat(cache.get()).isNotNull();
        }
    }

    // --- stale credentials from source tests ---

    @Test
    public void allowMode_staleCredentialsFromSource_advisoryWindow_retainsCachedAndAppliesBackoff() {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;

        try (CachedSupplier<String> cachedSupplier = CachedSupplier.builder(supplier)
                                                                   .staleValueBehavior(ALLOW)
                                                                   .clock(clock)
                                                                   .prefetchJitterEnabled(false)
                                                                   .build()) {
            // Initial successful fetch: stale at +3600s, prefetch at +300s
            supplier.set(RefreshResult.builder("original-creds")
                                      .staleTime(now.plusSeconds(3600))
                                      .prefetchTime(now.plusSeconds(300))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("original-creds");

            // Advance into advisory window (past prefetch but before stale)
            clock.time = now.plusSeconds(LONG_PREFETCH_SECONDS + 1);

            // Source returns credentials with staleTime in the past (already expired)
            supplier.set(RefreshResult.builder("stale-creds")
                                      .staleTime(now.plusSeconds(100))  // stale time in the past relative to clock
                                      .prefetchTime(now.plusSeconds(50))
                                      .build());

            // Should return the original cached credentials, not the stale ones
            assertThat(cachedSupplier.get()).isEqualTo("original-creds");

            // Verify backoff was applied: a subsequent call should still return cached without contacting source
            clock.time = now.plusSeconds(LONG_PREFETCH_SECONDS + 2);
            supplier.set(RefreshResult.builder("should-not-reach")
                                      .staleTime(Instant.MAX)
                                      .prefetchTime(Instant.MAX)
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("original-creds");

            // Advance past max backoff (600s from the stale response time)
            clock.time = now.plusSeconds(LONG_PREFETCH_SECONDS + 1 + PAST_MAX_BACKOFF);
            assertThat(cachedSupplier.get()).isEqualTo("should-not-reach");
        }
    }

    @Test
    public void allowMode_staleCredentialsFromSource_mandatoryWindow_retainsCachedAndAppliesBackoff() {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;

        try (CachedSupplier<String> cachedSupplier = CachedSupplier.builder(supplier)
                                                                   .staleValueBehavior(ALLOW)
                                                                   .clock(clock)
                                                                   .prefetchJitterEnabled(false)
                                                                   .build()) {
            // Initial successful fetch: stale at +60s, prefetch at +30s
            supplier.set(RefreshResult.builder("original-creds")
                                      .staleTime(now.plusSeconds(60))
                                      .prefetchTime(now.plusSeconds(30))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("original-creds");

            // Advance past stale time (mandatory refresh territory)
            clock.time = now.plusSeconds(61);

            // Source returns credentials with staleTime in the past (already expired)
            supplier.set(RefreshResult.builder("stale-creds")
                                      .staleTime(now.plusSeconds(30))  // stale time in the past relative to clock
                                      .prefetchTime(now.plusSeconds(15))
                                      .build());

            // Should return the original cached credentials, not the stale ones
            assertThat(cachedSupplier.get()).isEqualTo("original-creds");

            // Verify backoff was applied: a subsequent call should still be rate limited
            clock.time = now.plusSeconds(62);
            supplier.set(RefreshResult.builder("fresh-creds")
                                      .staleTime(Instant.MAX)
                                      .prefetchTime(Instant.MAX)
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("original-creds");

            // Advance past max backoff (600s from the stale response time)
            clock.time = now.plusSeconds(61 + PAST_MAX_BACKOFF);
            assertThat(cachedSupplier.get()).isEqualTo("fresh-creds");
        }
    }

    // --- non-recoverable error follow-up tests ---

    @Test
    public void allowMode_nonRecoverableError_noBackoff_nextCallContactsSource() {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;

        try (CachedSupplier<String> cachedSupplier = CachedSupplier.builder(supplier)
                                                                   .staleValueBehavior(ALLOW)
                                                                   .nonRecoverableErrorPredicate(
                                                                       e -> e instanceof CacheInvalidatingRuntimeException)
                                                                   .clock(clock)
                                                                   .prefetchJitterEnabled(false)
                                                                   .build()) {
            // Initial successful fetch
            supplier.set(RefreshResult.builder("cached-creds")
                                      .staleTime(now.plusSeconds(3600))
                                      .prefetchTime(now.plusSeconds(300))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("cached-creds");

            // Advance past prefetch time (advisory window)
            clock.time = now.plusSeconds(LONG_PREFETCH_SECONDS + 1);
            supplier.set(new CacheInvalidatingRuntimeException("non-recoverable"));

            // Non-recoverable error is thrown
            assertThatThrownBy(cachedSupplier::get).isInstanceOf(CacheInvalidatingRuntimeException.class);

            // Advance past the non-recoverable error cache window (max 5 seconds) — source should be contacted
            clock.time = now.plusSeconds(LONG_PREFETCH_SECONDS + 1 + PAST_NON_RECOVERABLE_ERROR_CACHE);
            supplier.set(RefreshResult.builder("refreshed-creds")
                                      .staleTime(now.plusSeconds(7200))
                                      .prefetchTime(now.plusSeconds(5400))
                                      .build());
            // No 5-10 minute backoff was applied; after the short error cache expires, source is contacted
            assertThat(cachedSupplier.get()).isEqualTo("refreshed-creds");
        }
    }

    @Test
    public void allowMode_nonRecoverableError_mandatoryWindow_noBackoff_nextCallContactsSource() {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;

        try (CachedSupplier<String> cachedSupplier = CachedSupplier.builder(supplier)
                                                                   .staleValueBehavior(ALLOW)
                                                                   .nonRecoverableErrorPredicate(
                                                                       e -> e instanceof CacheInvalidatingRuntimeException)
                                                                   .clock(clock)
                                                                   .prefetchJitterEnabled(false)
                                                                   .build()) {
            // Initial successful fetch
            supplier.set(RefreshResult.builder("cached-creds")
                                      .staleTime(now.plusSeconds(60))
                                      .prefetchTime(now.plusSeconds(30))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("cached-creds");

            // Advance past stale time (mandatory window / expired)
            clock.time = now.plusSeconds(61);
            supplier.set(new CacheInvalidatingRuntimeException("non-recoverable"));

            // Non-recoverable error is thrown
            assertThatThrownBy(cachedSupplier::get).isInstanceOf(CacheInvalidatingRuntimeException.class);

            // Advance past the non-recoverable error cache window (max 5 seconds) — source should be contacted
            clock.time = now.plusSeconds(61 + PAST_NON_RECOVERABLE_ERROR_CACHE);
            supplier.set(RefreshResult.builder("refreshed-creds")
                                      .staleTime(now.plusSeconds(7200))
                                      .prefetchTime(now.plusSeconds(5400))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("refreshed-creds");
        }
    }

    // --- non-recoverable error caching tests ---

    @Test
    public void allowMode_nonRecoverableErrorCached_withinCacheWindow_reRaisesWithoutCallingSource() {
        AdjustableClock clock = new AdjustableClock();
        AtomicInteger supplierCallCount = new AtomicInteger(0);
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;

        CacheInvalidatingRuntimeException nonRecoverableError = new CacheInvalidatingRuntimeException("token expired");

        Supplier<RefreshResult<String>> countingSupplier = () -> {
            supplierCallCount.incrementAndGet();
            throw nonRecoverableError;
        };

        try (CachedSupplier<String> cachedSupplier = CachedSupplier.builder(countingSupplier)
                                                                   .staleValueBehavior(ALLOW)
                                                                   .nonRecoverableErrorPredicate(
                                                                       e -> e instanceof CacheInvalidatingRuntimeException)
                                                                   .clock(clock)
                                                                   .prefetchJitterEnabled(false)
                                                                   .build()) {
            // First call fails — initial fetch, no cached value
            assertThatThrownBy(cachedSupplier::get).isEqualTo(nonRecoverableError);
            assertThat(supplierCallCount.get()).isEqualTo(1);

            // Second call within the cache window — should re-raise without calling source
            clock.time = now.plusMillis(WITHIN_NON_RECOVERABLE_ERROR_CACHE_MILLIS);
            assertThatThrownBy(cachedSupplier::get).isEqualTo(nonRecoverableError);
            assertThat(supplierCallCount.get()).isEqualTo(1); // Still 1 — source was NOT contacted
        }
    }

    @Test
    public void allowMode_nonRecoverableErrorCached_afterCacheExpires_contactsSourceAgain() {
        AdjustableClock clock = new AdjustableClock();
        AtomicInteger supplierCallCount = new AtomicInteger(0);
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;

        CacheInvalidatingRuntimeException nonRecoverableError = new CacheInvalidatingRuntimeException("token expired");

        Supplier<RefreshResult<String>> countingSupplier = () -> {
            supplierCallCount.incrementAndGet();
            throw nonRecoverableError;
        };

        try (CachedSupplier<String> cachedSupplier = CachedSupplier.builder(countingSupplier)
                                                                   .staleValueBehavior(ALLOW)
                                                                   .nonRecoverableErrorPredicate(
                                                                       e -> e instanceof CacheInvalidatingRuntimeException)
                                                                   .clock(clock)
                                                                   .prefetchJitterEnabled(false)
                                                                   .build()) {
            // First call fails
            assertThatThrownBy(cachedSupplier::get).isEqualTo(nonRecoverableError);
            assertThat(supplierCallCount.get()).isEqualTo(1);

            // Advance past the maximum cache window (5 seconds) — source should be contacted again
            clock.time = now.plusSeconds(PAST_NON_RECOVERABLE_ERROR_CACHE);
            assertThatThrownBy(cachedSupplier::get).isEqualTo(nonRecoverableError);
            assertThat(supplierCallCount.get()).isEqualTo(2); // Source WAS contacted
        }
    }

    @Test
    public void allowMode_nonRecoverableErrorCached_successfulRefreshClearsCache() {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;

        try (CachedSupplier<String> cachedSupplier = CachedSupplier.builder(supplier)
                                                                   .staleValueBehavior(ALLOW)
                                                                   .nonRecoverableErrorPredicate(
                                                                       e -> e instanceof CacheInvalidatingRuntimeException)
                                                                   .clock(clock)
                                                                   .prefetchJitterEnabled(false)
                                                                   .build()) {
            // Initial fetch fails with non-recoverable error
            supplier.set(new CacheInvalidatingRuntimeException("token expired"));
            assertThatThrownBy(cachedSupplier::get).isInstanceOf(CacheInvalidatingRuntimeException.class);

            // Advance past the cache window and fix the underlying issue (source now returns credentials)
            clock.time = now.plusSeconds(PAST_NON_RECOVERABLE_ERROR_CACHE);
            supplier.set(RefreshResult.builder("fresh-creds")
                                      .staleTime(now.plusSeconds(3600))
                                      .prefetchTime(now.plusSeconds(300))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("fresh-creds");

            // Advance into the prefetch window — trigger another non-recoverable, then verify it clears on success
            clock.time = now.plusSeconds(LONG_PREFETCH_SECONDS + 1);
            supplier.set(new CacheInvalidatingRuntimeException("token expired again"));
            assertThatThrownBy(cachedSupplier::get).isInstanceOf(CacheInvalidatingRuntimeException.class);

            // Advance past cache window, fix again
            clock.time = now.plusSeconds(LONG_PREFETCH_SECONDS + 1 + PAST_NON_RECOVERABLE_ERROR_CACHE);
            supplier.set(RefreshResult.builder("newer-creds")
                                      .staleTime(now.plusSeconds(7200))
                                      .prefetchTime(now.plusSeconds(5400))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("newer-creds");
        }
    }

    @Test
    public void allowMode_nonRecoverableErrorCached_staleWindow_reRaisesWithoutCallingSource() {
        AdjustableClock clock = new AdjustableClock();
        AtomicInteger supplierCallCount = new AtomicInteger(0);
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;

        MutableSupplier supplier = new MutableSupplier();

        try (CachedSupplier<String> cachedSupplier = CachedSupplier.builder(supplier)
                                                                   .staleValueBehavior(ALLOW)
                                                                   .nonRecoverableErrorPredicate(
                                                                       e -> e instanceof CacheInvalidatingRuntimeException)
                                                                   .clock(clock)
                                                                   .prefetchJitterEnabled(false)
                                                                   .build()) {
            // Initial successful fetch
            supplier.set(RefreshResult.builder("cached-creds")
                                      .staleTime(now.plusSeconds(60))
                                      .prefetchTime(now.plusSeconds(30))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("cached-creds");

            // Advance past stale time (mandatory window)
            clock.time = now.plusSeconds(61);
            CacheInvalidatingRuntimeException error = new CacheInvalidatingRuntimeException("token expired");
            supplier.set(error);

            // First failure — thrown and cached
            assertThatThrownBy(cachedSupplier::get).isEqualTo(error);

            // Immediately retry (within cache window) — should re-raise the same error without calling source
            clock.time = now.plusSeconds(61).plusMillis(WITHIN_NON_RECOVERABLE_ERROR_CACHE_MILLIS);
            // Swap supplier to something that would succeed — if called, we'd get "new-creds" not an exception
            supplier.set(RefreshResult.builder("new-creds")
                                      .staleTime(Instant.MAX)
                                      .prefetchTime(Instant.MAX)
                                      .build());
            assertThatThrownBy(cachedSupplier::get).isEqualTo(error);
        }
    }

    @Test
    public void allowMode_nonRecoverableErrorCached_prefetchWindow_reRaisesWithoutCallingSource() {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;

        try (CachedSupplier<String> cachedSupplier = CachedSupplier.builder(supplier)
                                                                   .staleValueBehavior(ALLOW)
                                                                   .nonRecoverableErrorPredicate(
                                                                       e -> e instanceof CacheInvalidatingRuntimeException)
                                                                   .clock(clock)
                                                                   .prefetchJitterEnabled(false)
                                                                   .build()) {
            // Initial successful fetch
            supplier.set(RefreshResult.builder("cached-creds")
                                      .staleTime(now.plusSeconds(3600))
                                      .prefetchTime(now.plusSeconds(60))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("cached-creds");

            // Advance into prefetch window
            clock.time = now.plusSeconds(61);
            CacheInvalidatingRuntimeException error = new CacheInvalidatingRuntimeException("token expired");
            supplier.set(error);

            // First call — non-recoverable error thrown and cached
            assertThatThrownBy(cachedSupplier::get).isEqualTo(error);

            // Immediately retry (within cache window) — should re-raise without calling source
            clock.time = now.plusSeconds(61).plusMillis(WITHIN_NON_RECOVERABLE_ERROR_CACHE_MILLIS);
            supplier.set(RefreshResult.builder("new-creds")
                                      .staleTime(Instant.MAX)
                                      .prefetchTime(Instant.MAX)
                                      .build());
            assertThatThrownBy(cachedSupplier::get).isEqualTo(error);
        }
    }

    @Test
    public void allowMode_nonRecoverableErrorCached_cacheWindowIsJitteredBetween1And5Seconds() {
        // Run many iterations to verify the cache window is within [1, 5] seconds
        for (int i = 0; i < 100; i++) {
            AdjustableClock clock = new AdjustableClock();
            Instant now = Instant.parse("2024-01-01T00:00:00Z");
            clock.time = now;

            CacheInvalidatingRuntimeException error = new CacheInvalidatingRuntimeException("expired");
            AtomicInteger callCount = new AtomicInteger(0);

            try (CachedSupplier<String> cachedSupplier = CachedSupplier.<String>builder(() -> {
                                                                           callCount.incrementAndGet();
                                                                           throw error;
                                                                       })
                                                                       .staleValueBehavior(ALLOW)
                                                                       .nonRecoverableErrorPredicate(
                                                                           e -> e instanceof CacheInvalidatingRuntimeException)
                                                                       .clock(clock)
                                                                       .prefetchJitterEnabled(false)
                                                                       .build()) {
                // First call caches the error
                assertThatThrownBy(cachedSupplier::get).isEqualTo(error);
                assertThat(callCount.get()).isEqualTo(1);

                // Just under the cache minimum — should still be cached whatever the jitter picked
                clock.time = now.plusMillis(WITHIN_NON_RECOVERABLE_ERROR_CACHE_MILLIS);
                assertThatThrownBy(cachedSupplier::get).isEqualTo(error);
                assertThat(callCount.get()).isEqualTo(1);

                // At PAST_NON_RECOVERABLE_ERROR_CACHE — cache must have expired (cache max is 5s)
                clock.time = now.plusSeconds(PAST_NON_RECOVERABLE_ERROR_CACHE);
                assertThatThrownBy(cachedSupplier::get).isEqualTo(error);
                assertThat(callCount.get()).isEqualTo(2); // Source was contacted again
            }
        }
    }

    @Test
    public void allowMode_nonRecoverableErrorCached_recoverableErrorDoesNotUseErrorCache() {
        AdjustableClock clock = new AdjustableClock();
        AtomicInteger supplierCallCount = new AtomicInteger(0);
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;

        MutableSupplier supplier = new MutableSupplier();

        try (CachedSupplier<String> cachedSupplier = CachedSupplier.builder(supplier)
                                                                   .staleValueBehavior(ALLOW)
                                                                   .nonRecoverableErrorPredicate(
                                                                       e -> e instanceof CacheInvalidatingRuntimeException)
                                                                   .clock(clock)
                                                                   .prefetchJitterEnabled(false)
                                                                   .build()) {
            // Initial successful fetch
            supplier.set(RefreshResult.builder("cached-creds")
                                      .staleTime(now.plusSeconds(60))
                                      .prefetchTime(now.plusSeconds(30))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("cached-creds");

            // Advance past stale time and fail with a RECOVERABLE error
            clock.time = now.plusSeconds(61);
            supplier.set(new RuntimeException("service unavailable"));
            assertThat(cachedSupplier.get()).isEqualTo("cached-creds"); // Static stability applies

            // Verify the error is NOT cached as non-recoverable (no cachedNonRecoverableError set)
            // The rate-limiting backoff (nextAllowedRefreshTime) is applied instead.
            // Advance 1 second — still within the 5-10min backoff, returns cached
            clock.time = now.plusSeconds(62);
            supplier.set(RefreshResult.builder("should-not-get")
                                      .staleTime(Instant.MAX)
                                      .prefetchTime(Instant.MAX)
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("cached-creds"); // Rate limited, not error-cached
        }
    }

    // --- integrated advisory window recomputation test ---

    @Test
    public void allowMode_advisoryWindowRecomputedOnRefreshWithDifferentLifetime() {
        AdjustableClock clock = new AdjustableClock();
        AtomicInteger fetchCount = new AtomicInteger(0);
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;

        // First fetch: 6-hour credentials (should select 60-minute advisory window)
        // Expiration = now + 6h, staleTime = expiration - 1min, prefetchTime = expiration - 60min
        Instant firstExpiration = now.plus(Duration.ofHours(6));
        Instant firstStale = firstExpiration.minus(Duration.ofMinutes(1));
        Instant firstPrefetch = firstExpiration.minus(Duration.ofMinutes(60));

        // Second fetch: 10-minute credentials (should select 5-minute advisory window)
        // These will be set up when the time advances
        Instant secondFetchTime = firstPrefetch.plusSeconds(1); // just past the first prefetch time
        Instant secondExpiration = secondFetchTime.plus(Duration.ofMinutes(10));
        Instant secondStale = secondExpiration.minus(Duration.ofMinutes(1));
        Instant secondPrefetch = secondExpiration.minus(Duration.ofMinutes(5));

        Supplier<RefreshResult<String>> dynamicSupplier = () -> {
            int count = fetchCount.incrementAndGet();
            if (count == 1) {
                return RefreshResult.builder("6h-creds")
                                    .staleTime(firstStale)
                                    .prefetchTime(firstPrefetch)
                                    .build();
            } else {
                return RefreshResult.builder("10m-creds")
                                    .staleTime(secondStale)
                                    .prefetchTime(secondPrefetch)
                                    .build();
            }
        };

        try (CachedSupplier<String> cachedSupplier = CachedSupplier.builder(dynamicSupplier)
                                                                   .staleValueBehavior(ALLOW)
                                                                   .clock(clock)
                                                                   .prefetchJitterEnabled(false)
                                                                   .build()) {
            // Initial fetch — 6-hour creds
            assertThat(cachedSupplier.get()).isEqualTo("6h-creds");
            assertThat(fetchCount.get()).isEqualTo(1);

            // Advance to just before the 60-minute advisory window — should NOT trigger refresh
            clock.time = firstPrefetch.minusSeconds(1);
            assertThat(cachedSupplier.get()).isEqualTo("6h-creds");
            assertThat(fetchCount.get()).isEqualTo(1);

            // Advance into the 60-minute advisory window — should trigger refresh
            clock.time = secondFetchTime;
            assertThat(cachedSupplier.get()).isEqualTo("10m-creds");
            assertThat(fetchCount.get()).isEqualTo(2);

            // Verify new advisory window: advance to just before the 5-minute window — should NOT trigger
            clock.time = secondPrefetch.minusSeconds(1);
            assertThat(cachedSupplier.get()).isEqualTo("10m-creds");
            assertThat(fetchCount.get()).isEqualTo(2);
        }
    }

    // --- prefetch jitter tests ---

    /**
     * Records the {@link RefreshResult} the cache actually stores, which is the supplier's result after jitter has been
     * applied to it.
     */
    private static class RecordingPrefetchStrategy implements CachedSupplier.PrefetchStrategy {
        private final List<Instant> recordedPrefetchTimes = new ArrayList<>();

        @Override
        public void prefetch(Runnable valueUpdater) {
            valueUpdater.run();
        }

        @Override
        public <T> RefreshResult<T> fetch(Supplier<RefreshResult<T>> supplier) {
            RefreshResult<T> result = supplier.get();
            recordedPrefetchTimes.add(result.prefetchTime());
            return result;
        }
    }

    private List<Instant> recordPrefetchTimes(Instant staleTime, Instant prefetchTime, Boolean jitterEnabled, int iterations) {
        List<Instant> prefetchTimes = new ArrayList<>();
        for (int i = 0; i < iterations; i++) {
            RecordingPrefetchStrategy strategy = new RecordingPrefetchStrategy();
            CachedSupplier.Builder<String> builder =
                CachedSupplier.builder(() -> RefreshResult.builder("value")
                                                          .staleTime(staleTime)
                                                          .prefetchTime(prefetchTime)
                                                          .build())
                              .prefetchStrategy(strategy);
            if (jitterEnabled != null) {
                builder.prefetchJitterEnabled(jitterEnabled);
            }
            try (CachedSupplier<String> cachedSupplier = builder.build()) {
                cachedSupplier.get();
            }
            prefetchTimes.addAll(strategy.recordedPrefetchTimes);
        }
        return prefetchTimes;
    }

    @Test
    public void prefetchJitterEnabledByDefault_movesPrefetchTimeLater() {
        Instant expiration = now().plus(Duration.ofMinutes(60));
        Instant staleTime = expiration.minus(Duration.ofMinutes(1));
        Instant prefetchTime = expiration.minus(Duration.ofMinutes(5));

        List<Instant> prefetchTimes = recordPrefetchTimes(staleTime, prefetchTime, null, 100);

        // Jitter never moves the prefetch time earlier, and never past one minute before the stale time.
        assertThat(prefetchTimes).allSatisfy(t -> assertThat(t).isBetween(prefetchTime,
                                                                         staleTime.minus(Duration.ofMinutes(1))));
        // The requested prefetch time is not honored as-is: the effective time varies from fetch to fetch.
        assertThat(new HashSet<>(prefetchTimes)).hasSizeGreaterThan(50);
    }

    @Test
    public void prefetchJitterDisabled_honorsRequestedPrefetchTimeExactly() {
        Instant expiration = now().plus(Duration.ofMinutes(60));
        Instant staleTime = expiration.minus(Duration.ofMinutes(1));
        Instant prefetchTime = expiration.minus(Duration.ofMinutes(5));

        List<Instant> prefetchTimes = recordPrefetchTimes(staleTime, prefetchTime, false, 100);

        assertThat(prefetchTimes).containsOnly(prefetchTime);
    }
}
