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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import software.amazon.awssdk.utils.cache.CachedSupplier.StaleValueBehavior;

/**
 * Validate the functionality of {@link CachedSupplier}.
 */
public class CachedSupplierTest {

    // --- Constants mirroring CachedSupplier's internal values, for test readability ---

    /** Minimum refresh backoff in seconds, for every remaining lifetime. */
    private static final long BACKOFF_MIN_SECONDS = 60;

    /** Minimum refresh backoff in seconds when the cached value has expired (remaining lifetime 0). */
    private static final long EXPIRED_BACKOFF_MIN_SECONDS = BACKOFF_MIN_SECONDS;

    /** Maximum refresh backoff in seconds when the cached value has expired (remaining lifetime 0). */
    private static final long EXPIRED_BACKOFF_MAX_SECONDS = 90;

    /** Minimum refresh backoff in seconds when the cached value has 1200 seconds or more left, or never expires. */
    private static final long LONG_LIVED_BACKOFF_MIN_SECONDS = BACKOFF_MIN_SECONDS;

    /** Maximum refresh backoff in seconds when the cached value has 1200 seconds or more left, or never expires. */
    private static final long LONG_LIVED_BACKOFF_MAX_SECONDS = 600;

    /** Number of independent failed refreshes sampled by tests that check which value determined the backoff range. */
    private static final int BACKOFF_SAMPLES = 20;

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

    /** A duration safely past the maximum backoff for any remaining lifetime, guaranteeing the backoff has elapsed. */
    private static final long PAST_MAX_BACKOFF = LONG_LIVED_BACKOFF_MAX_SECONDS + 1;

    /** Long prefetch time (seconds) used in tests where credentials have a 1-hour stale time and 5-minute advisory window. */
    private static final long LONG_PREFETCH_SECONDS = 300;

    /** Number of threads that call {@link CachedSupplier#get()} at once in the concurrent mandatory refresh tests. */
    private static final int CONCURRENT_CALLERS = 5;

    /** Extracts the backoff duration from the failed refresh warning. */
    private static final Pattern RETRY_AFTER_SECONDS = Pattern.compile("attempted again after (\\d+) seconds");
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

                // The cached value expired at 60s, so nextAllowedRefreshTime is set to now(61) + [60,90]s
                // The cached value should be returned while rate limited
                Instant minBackoffEnd = now.plusSeconds(61 + EXPIRED_BACKOFF_MIN_SECONDS);
                Instant maxBackoffEnd = now.plusSeconds(61 + EXPIRED_BACKOFF_MAX_SECONDS);

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

            // Advance past the maximum possible backoff (61 + 601 = 662s from now) but still before stale time (3600s).
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

    /**
     * A {@link Random} whose {@link #nextInt(int)} always returns the lowest or the highest value in range, and records the
     * bound it was called with.
     */
    private static final class BoundaryRandom extends Random {
        private final boolean highest;
        private int lastBound;

        private BoundaryRandom(boolean highest) {
            this.highest = highest;
        }

        @Override
        public int nextInt(int bound) {
            lastBound = bound;
            return highest ? bound - 1 : 0;
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
            // No refresh backoff was applied; after the short error cache expires, source is contacted
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
            // Advance 1 second — still within the 60-90 second backoff for the expired cached value, returns cached
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

    // --- concurrent mandatory refresh tests ---

    @Test
    public void allowMode_concurrentMandatoryRefreshFails_sourceCalledOnce() throws InterruptedException {
        List<String> results = runConcurrentMandatoryRefresh(() -> {
            throw new RuntimeException("source down");
        });

        assertThat(results).hasSize(CONCURRENT_CALLERS).containsOnly("original");
    }

    @Test
    public void allowMode_concurrentMandatoryRefreshSucceeds_sourceCalledOnce() throws InterruptedException {
        List<String> results = runConcurrentMandatoryRefresh(() -> RefreshResult.builder("fresh")
                                                                                .staleTime(Instant.MAX)
                                                                                .prefetchTime(Instant.MAX)
                                                                                .build());

        assertThat(results).hasSize(CONCURRENT_CALLERS).containsOnly("fresh");
    }

    /**
     * Caches "original", moves the clock past its stale time, then runs the concurrent refresh described in
     * {@link #runConcurrentMandatoryRefresh(Function, long, Supplier)}.
     */
    private List<String> runConcurrentMandatoryRefresh(Supplier<RefreshResult<String>> refreshOutcome)
        throws InterruptedException {
        return runConcurrentMandatoryRefresh(
            now -> RefreshResult.builder("original").staleTime(now.plusSeconds(60)).prefetchTime(now.plusSeconds(30)).build(),
            61,
            refreshOutcome);
    }

    /**
     * Caches the value returned by {@code initialValue} (given the start time), moves the clock forward by
     * {@code refreshDelaySeconds}, then has {@link #CONCURRENT_CALLERS} threads call {@link CachedSupplier#get()} at once. The
     * first caller to reach the source blocks there until every caller is waiting (one in the source, the rest on the refresh
     * lock), then completes with {@code refreshOutcome}. Asserts that the source was called exactly once for the refresh, and
     * returns what each caller received.
     */
    private List<String> runConcurrentMandatoryRefresh(Function<Instant, RefreshResult<String>> initialValue,
                                                       long refreshDelaySeconds,
                                                       Supplier<RefreshResult<String>> refreshOutcome)
        throws InterruptedException {
        AdjustableClock clock = new AdjustableClock();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;

        AtomicInteger refreshCalls = new AtomicInteger(0);
        CountDownLatch releaseSource = new CountDownLatch(1);
        RefreshResult<String> initial = initialValue.apply(now);
        AtomicReference<Supplier<RefreshResult<String>>> source = new AtomicReference<>(() -> initial);

        List<String> results = Collections.synchronizedList(new ArrayList<>());
        List<Thread> callers = new ArrayList<>();

        try (CachedSupplier<String> cache = CachedSupplier.builder(() -> source.get().get())
                                                          .staleValueBehavior(ALLOW)
                                                          .clock(clock)
                                                          .prefetchJitterEnabled(false)
                                                          .build()) {
            assertThat(cache.get()).isEqualTo(initial.value());

            clock.time = now.plusSeconds(refreshDelaySeconds);
            source.set(() -> {
                refreshCalls.incrementAndGet();
                // Must be shorter than the 5 second refresh lock wait, so waiting callers are still waiting when this returns.
                invokeSafely(() -> releaseSource.await(3, TimeUnit.SECONDS));
                return refreshOutcome.get();
            });

            for (int i = 0; i < CONCURRENT_CALLERS; i++) {
                Thread caller = new Thread(() -> results.add(cache.get()));
                callers.add(caller);
                caller.start();
            }

            // Wait until one caller is in the source and the others are blocked on the refresh lock.
            Instant deadline = Instant.now().plusSeconds(2);
            while (!(refreshCalls.get() == 1 && callers.stream().allMatch(CachedSupplierTest::isWaiting))
                   && Instant.now().isBefore(deadline)) {
                Thread.sleep(10);
            }
            releaseSource.countDown();

            for (Thread caller : callers) {
                caller.join(10_000);
            }

            assertThat(refreshCalls).hasValue(1);
        }
        return results;
    }

    private static boolean isWaiting(Thread thread) {
        Thread.State state = thread.getState();
        return state == Thread.State.WAITING || state == Thread.State.TIMED_WAITING;
    }

    // --- fetched value inside the mandatory refresh window tests ---

    @Test
    public void allowMode_fetchedValueInsideMandatoryWindowButUnexpired_isAcceptedWithoutBackoff() {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;

        try (CachedSupplier<String> cachedSupplier = CachedSupplier.builder(supplier)
                                                                   .staleValueBehavior(ALLOW)
                                                                   .clock(clock)
                                                                   .prefetchJitterEnabled(false)
                                                                   .build()) {
            supplier.set(RefreshResult.builder("original")
                                      .staleTime(now.plusSeconds(60))
                                      .prefetchTime(now.plusSeconds(30))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("original");

            // Past the stale time, the source returns credentials that expire in 30 seconds: inside the mandatory refresh
            // window, but not expired.
            Instant refreshTime = now.plusSeconds(61);
            clock.time = refreshTime;
            supplier.set(RefreshResult.builder("short-lived")
                                      .staleTime(refreshTime.minusSeconds(30))
                                      .prefetchTime(refreshTime.minusSeconds(30))
                                      .expiration(refreshTime.plusSeconds(30))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("short-lived");

            // No backoff was applied: the next call contacts the source again.
            supplier.set(RefreshResult.builder("next")
                                      .staleTime(Instant.MAX)
                                      .prefetchTime(Instant.MAX)
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("next");
        }
    }

    @Test
    public void allowMode_fetchedValueExpirationAtNow_retainsCachedAndAppliesBackoff() {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;

        try (CachedSupplier<String> cachedSupplier = CachedSupplier.builder(supplier)
                                                                   .staleValueBehavior(ALLOW)
                                                                   .clock(clock)
                                                                   .prefetchJitterEnabled(false)
                                                                   .build()) {
            supplier.set(RefreshResult.builder("original")
                                      .staleTime(now.plusSeconds(60))
                                      .prefetchTime(now.plusSeconds(30))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("original");

            Instant refreshTime = now.plusSeconds(61);
            clock.time = refreshTime;
            supplier.set(RefreshResult.builder("expired")
                                      .staleTime(refreshTime.minusSeconds(60))
                                      .prefetchTime(refreshTime.minusSeconds(60))
                                      .expiration(refreshTime)
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("original");

            // The backoff was applied: the source is not contacted until it has elapsed.
            supplier.set(RefreshResult.builder("fresh")
                                      .staleTime(Instant.MAX)
                                      .prefetchTime(Instant.MAX)
                                      .build());
            clock.time = refreshTime.plusSeconds(1);
            assertThat(cachedSupplier.get()).isEqualTo("original");

            clock.time = refreshTime.plusSeconds(PAST_MAX_BACKOFF);
            assertThat(cachedSupplier.get()).isEqualTo("fresh");
        }
    }

    @Test
    public void strictMode_expirationIsIgnored_staleValueCachedForOneSecond() {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;

        try (CachedSupplier<String> cachedSupplier = CachedSupplier.builder(supplier)
                                                                   .staleValueBehavior(STRICT)
                                                                   .clock(clock)
                                                                   .prefetchJitterEnabled(false)
                                                                   .build()) {
            supplier.set(RefreshResult.builder("original")
                                      .staleTime(now.plusSeconds(60))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("original");

            // Same "inside the mandatory window but unexpired" result as the ALLOW test. STRICT ignores the expiration and
            // applies its existing rule: use the value, but only for one second.
            Instant refreshTime = now.plusSeconds(61);
            clock.time = refreshTime;
            supplier.set(RefreshResult.builder("short-lived")
                                      .staleTime(refreshTime.minusSeconds(30))
                                      .expiration(refreshTime.plusSeconds(30))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("short-lived");

            supplier.set(RefreshResult.builder("next")
                                      .staleTime(Instant.MAX)
                                      .build());
            clock.time = refreshTime.plusMillis(500);
            assertThat(cachedSupplier.get()).isEqualTo("short-lived");

            clock.time = refreshTime.plusSeconds(1);
            assertThat(cachedSupplier.get()).isEqualTo("next");
        }
    }

    // --- failed refresh messaging tests ---

    @Test
    public void allowMode_mandatoryWindowFailure_logsWarnWithErrorAndRetryTime() {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;
        String name = "mandatoryWindowFailure";

        try (CapturingAppender logs = new CapturingAppender();
             CachedSupplier<String> cachedSupplier = CachedSupplier.builder(supplier)
                                                                   .cachedValueName(name)
                                                                   .staleValueBehavior(ALLOW)
                                                                   .clock(clock)
                                                                   .prefetchJitterEnabled(false)
                                                                   .build()) {
            supplier.set(RefreshResult.builder("original")
                                      .staleTime(now.plusSeconds(60))
                                      .prefetchTime(now.plusSeconds(30))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("original");

            clock.time = now.plusSeconds(61);
            RuntimeException sourceError = new RuntimeException("source down");
            supplier.set(sourceError);
            assertThat(cachedSupplier.get()).isEqualTo("original");

            List<LogEvent> warnings = logs.warnEventsFor(name);
            assertThat(warnings).hasSize(1);
            // The retained value expired at 60s, so the backoff is the one for expired values.
            assertRefreshFailureWarning(warnings.get(0), "Credential refresh failed: source down",
                                        EXPIRED_BACKOFF_MIN_SECONDS, EXPIRED_BACKOFF_MAX_SECONDS);
            assertThat(warnings.get(0).getThrown()).isSameAs(sourceError);
        }
    }

    @Test
    public void allowMode_advisoryWindowFailure_logsWarnWithErrorAndRetryTime() {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;
        String name = "advisoryWindowFailure";

        try (CapturingAppender logs = new CapturingAppender();
             CachedSupplier<String> cachedSupplier = CachedSupplier.builder(supplier)
                                                                   .cachedValueName(name)
                                                                   .staleValueBehavior(ALLOW)
                                                                   .clock(clock)
                                                                   .prefetchJitterEnabled(false)
                                                                   .build()) {
            supplier.set(RefreshResult.builder("original")
                                      .staleTime(now.plusSeconds(3600))
                                      .prefetchTime(now.plusSeconds(LONG_PREFETCH_SECONDS))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("original");

            clock.time = now.plusSeconds(LONG_PREFETCH_SECONDS + 1);
            RuntimeException sourceError = new RuntimeException("source down");
            supplier.set(sourceError);
            assertThat(cachedSupplier.get()).isEqualTo("original");

            List<LogEvent> warnings = logs.warnEventsFor(name);
            assertThat(warnings).hasSize(1);
            // The retained value has 3299 seconds left, so the backoff is the one for long-lived values (1-10 minutes).
            assertRefreshFailureWarning(warnings.get(0), "Credential refresh failed: source down",
                                        LONG_LIVED_BACKOFF_MIN_SECONDS, LONG_LIVED_BACKOFF_MAX_SECONDS);
            assertThat(warnings.get(0).getThrown()).isSameAs(sourceError);
        }
    }

    @Test
    public void allowMode_fetchedValueAlreadyExpired_logsWarn() {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;
        String name = "fetchedValueAlreadyExpired";

        try (CapturingAppender logs = new CapturingAppender();
             CachedSupplier<String> cachedSupplier = CachedSupplier.builder(supplier)
                                                                   .cachedValueName(name)
                                                                   .staleValueBehavior(ALLOW)
                                                                   .clock(clock)
                                                                   .prefetchJitterEnabled(false)
                                                                   .build()) {
            supplier.set(RefreshResult.builder("original")
                                      .staleTime(now.plusSeconds(60))
                                      .prefetchTime(now.plusSeconds(30))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("original");

            clock.time = now.plusSeconds(61);
            Instant expiredAt = now.plusSeconds(30);
            supplier.set(RefreshResult.builder("expired")
                                      .staleTime(expiredAt)
                                      .prefetchTime(expiredAt)
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("original");

            List<LogEvent> warnings = logs.warnEventsFor(name);
            assertThat(warnings).hasSize(1);
            // The retained value expired at 60s, so the backoff is the one for expired values.
            assertRefreshFailureWarning(warnings.get(0), "Credential refresh failed: the credential source returned "
                                                         + "credentials that expired at " + expiredAt,
                                        EXPIRED_BACKOFF_MIN_SECONDS, EXPIRED_BACKOFF_MAX_SECONDS);
        }
    }

    @Test
    public void allowMode_nonRecoverableFailure_doesNotLogWarn() {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;
        String name = "nonRecoverableFailure";

        try (CapturingAppender logs = new CapturingAppender();
             CachedSupplier<String> cachedSupplier = CachedSupplier.builder(supplier)
                                                                   .cachedValueName(name)
                                                                   .staleValueBehavior(ALLOW)
                                                                   .nonRecoverableErrorPredicate(
                                                                       e -> e instanceof CacheInvalidatingRuntimeException)
                                                                   .clock(clock)
                                                                   .prefetchJitterEnabled(false)
                                                                   .build()) {
            supplier.set(RefreshResult.builder("original")
                                      .staleTime(now.plusSeconds(60))
                                      .prefetchTime(now.plusSeconds(30))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("original");

            clock.time = now.plusSeconds(61);
            supplier.set(new CacheInvalidatingRuntimeException("non-recoverable"));
            // The error is raised to the caller, so it is not also logged as a warning.
            assertThatThrownBy(cachedSupplier::get).isInstanceOf(CacheInvalidatingRuntimeException.class);

            assertThat(logs.warnEventsFor(name)).isEmpty();
        }
    }

    // --- refresh backoff scaled by remaining lifetime tests ---

    @ParameterizedTest(name = "remaining={0}s -> backoff in [{1}, {2}]s")
    @CsvSource({
        // Expired: remaining lifetime counts as 0
        "-600, 60, 90",
        "0, 60, 90",
        // Half the remaining lifetime is at or below the 90 second floor of the upper bound (3 minutes or less)
        "100, 60, 90",
        "180, 60, 90",
        // Between the floor and the cap: the upper bound is half the remaining lifetime
        "300, 60, 150",
        "600, 60, 300",
        "1000, 60, 500",
        // Half the remaining lifetime is at or above the 600 second cap (20 minutes or more)
        "1200, 60, 600",
        "3600, 60, 600"
    })
    public void get_whenRefreshFailsWithRemainingLifetime_backoffIsSixtySecondsToHalfRemainingClamped(long remainingSeconds,
                                                                                                     long expectedMinSeconds,
                                                                                                     long expectedMaxSeconds) {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;
        String name = "remainingLifetime" + remainingSeconds;
        Instant expiration = now.plusSeconds(7200);

        try (CapturingAppender logs = new CapturingAppender();
             CachedSupplier<String> cachedSupplier = allowModeSupplier(supplier, clock, name)) {
            supplier.set(RefreshResult.builder("original")
                                      .expiration(expiration)
                                      .staleTime(expiration.minusSeconds(60))
                                      .prefetchTime(now.plusSeconds(10))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("original");

            Instant failureTime = expiration.minusSeconds(remainingSeconds);
            clock.time = failureTime;
            supplier.set(new RuntimeException("source down"));
            assertThat(cachedSupplier.get()).isEqualTo("original");

            long backoffSeconds = singleRetryAfterSeconds(logs, name);
            assertThat(backoffSeconds).isBetween(expectedMinSeconds, expectedMaxSeconds);
            assertSourceNotContactedUntilBackoffElapses(cachedSupplier, clock, supplier, failureTime, backoffSeconds,
                                                        "original");
        }
    }

    @Test
    public void get_whenRefreshFailsAndCachedValueHasNoExpiration_usesStaleTimeForRemainingLifetime() {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;
        String name = "noExpiration";

        try (CapturingAppender logs = new CapturingAppender();
             CachedSupplier<String> cachedSupplier = allowModeSupplier(supplier, clock, name)) {
            supplier.set(RefreshResult.builder("original")
                                      .staleTime(now.plusSeconds(2000))
                                      .prefetchTime(now.plusSeconds(10))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("original");

            // 1000 seconds before the stale time: the upper bound of the backoff is half of 1000 seconds.
            // remainingLifetimeSeconds_* tests check the remaining lifetime exactly.
            Instant failureTime = now.plusSeconds(1000);
            clock.time = failureTime;
            supplier.set(new RuntimeException("source down"));
            assertThat(cachedSupplier.get()).isEqualTo("original");

            long backoffSeconds = singleRetryAfterSeconds(logs, name);
            assertThat(backoffSeconds).isBetween(BACKOFF_MIN_SECONDS, 500L);
            assertSourceNotContactedUntilBackoffElapses(cachedSupplier, clock, supplier, failureTime, backoffSeconds,
                                                        "original");
        }
    }

    @Test
    public void get_whenRefreshFailsAndCachedValueHasExpiration_usesExpirationRatherThanStaleTime() {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;
        String name = "expirationAfterStaleTime";

        try (CapturingAppender logs = new CapturingAppender();
             CachedSupplier<String> cachedSupplier = allowModeSupplier(supplier, clock, name)) {
            supplier.set(RefreshResult.builder("original")
                                      .staleTime(now.plusSeconds(100))
                                      .prefetchTime(now.plusSeconds(10))
                                      .expiration(now.plusSeconds(1100))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("original");

            // At the stale time, 1000 seconds remain until the expiration, so the upper bound of the backoff is 500 seconds.
            // Using the stale time would give 60-90 seconds. remainingLifetimeSeconds_* tests check this exactly.
            Instant failureTime = now.plusSeconds(100);
            clock.time = failureTime;
            supplier.set(new RuntimeException("source down"));
            assertThat(cachedSupplier.get()).isEqualTo("original");

            long backoffSeconds = singleRetryAfterSeconds(logs, name);
            assertThat(backoffSeconds).isBetween(BACKOFF_MIN_SECONDS, 500L);
            assertSourceNotContactedUntilBackoffElapses(cachedSupplier, clock, supplier, failureTime, backoffSeconds,
                                                        "original");
        }
    }

    @Test
    public void get_whenRefreshFailsAndCachedValueNeverExpires_backoffIsOneToTenMinutes() {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;
        String name = "neverExpires";

        try (CapturingAppender logs = new CapturingAppender();
             CachedSupplier<String> cachedSupplier = allowModeSupplier(supplier, clock, name)) {
            // No expiration and the default stale time (Instant.MAX): the value never expires.
            supplier.set(RefreshResult.builder("original")
                                      .prefetchTime(now.plusSeconds(10))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("original");

            Instant failureTime = now.plusSeconds(11);
            clock.time = failureTime;
            supplier.set(new RuntimeException("source down"));
            assertThat(cachedSupplier.get()).isEqualTo("original");

            long backoffSeconds = singleRetryAfterSeconds(logs, name);
            assertThat(backoffSeconds).isBetween(LONG_LIVED_BACKOFF_MIN_SECONDS, LONG_LIVED_BACKOFF_MAX_SECONDS);
            assertSourceNotContactedUntilBackoffElapses(cachedSupplier, clock, supplier, failureTime, backoffSeconds,
                                                        "original");
        }
    }

    @Test
    public void get_whenFetchedValueAlreadyExpired_backoffUsesRetainedValueLifetime() {
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        Instant expiration = now.plusSeconds(7200);
        List<Long> backoffs = new ArrayList<>();

        // Every backoff range starts at 60 seconds, so a single sample cannot tell which value determined the range. The
        // retained value has 3600 seconds left (60-600 seconds), while the fetched value has expired (60-90 seconds).
        for (int i = 0; i < BACKOFF_SAMPLES; i++) {
            AdjustableClock clock = new AdjustableClock();
            MutableSupplier supplier = new MutableSupplier();
            clock.time = now;
            String name = "fetchedExpiredRetainedLifetime" + i;

            try (CapturingAppender logs = new CapturingAppender();
                 CachedSupplier<String> cachedSupplier = allowModeSupplier(supplier, clock, name)) {
                supplier.set(RefreshResult.builder("original")
                                          .expiration(expiration)
                                          .staleTime(expiration.minusSeconds(60))
                                          .prefetchTime(now.plusSeconds(10))
                                          .build());
                assertThat(cachedSupplier.get()).isEqualTo("original");

                Instant failureTime = expiration.minusSeconds(3600);
                clock.time = failureTime;
                supplier.set(RefreshResult.builder("expired")
                                          .expiration(now.plusSeconds(100))
                                          .staleTime(now.plusSeconds(40))
                                          .prefetchTime(now.plusSeconds(10))
                                          .build());
                assertThat(cachedSupplier.get()).isEqualTo("original");

                long backoffSeconds = singleRetryAfterSeconds(logs, name);
                assertThat(backoffSeconds).isBetween(LONG_LIVED_BACKOFF_MIN_SECONDS, LONG_LIVED_BACKOFF_MAX_SECONDS);
                assertSourceNotContactedUntilBackoffElapses(cachedSupplier, clock, supplier, failureTime, backoffSeconds,
                                                            "original");
                backoffs.add(backoffSeconds);
            }
        }

        // With the fetched value's lifetime, no backoff would exceed 90 seconds. With the retained value's lifetime, the
        // chance that all samples are at most 90 seconds is (31/541)^20, about 1e-25.
        assertThat(backoffs).anySatisfy(b -> assertThat(b).isGreaterThan(EXPIRED_BACKOFF_MAX_SECONDS));
    }

    @Test
    public void get_whenRefreshFails_backoffUsesRemainingLifetimeMeasuredAfterSourceReturns() {
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        Instant expiration = now.plusSeconds(7200);

        // Every backoff range starts at 60 seconds, so each sample is checked against the narrower range. The failed call
        // starts with 1300 seconds left (60-600 seconds) and returns with 100 seconds left (60-90 seconds). If the remaining
        // lifetime were measured before the call, the chance that all samples are at most 90 seconds is (31/541)^20.
        for (int i = 0; i < BACKOFF_SAMPLES; i++) {
            AdjustableClock clock = new AdjustableClock();
            clock.time = now;
            String name = "measuredAfterSourceReturns" + i;
            AtomicInteger calls = new AtomicInteger();
            Supplier<RefreshResult<String>> slowFailingSource = () -> {
                switch (calls.incrementAndGet()) {
                    case 1:
                        return RefreshResult.builder("original")
                                            .expiration(expiration)
                                            .staleTime(expiration.minusSeconds(60))
                                            .prefetchTime(now.plusSeconds(10))
                                            .build();
                    case 2:
                        clock.time = clock.time.plusSeconds(1200);
                        throw new RuntimeException("source down after a slow call");
                    default:
                        return RefreshResult.builder("fresh")
                                            .staleTime(Instant.MAX)
                                            .prefetchTime(Instant.MAX)
                                            .build();
                }
            };

            try (CapturingAppender logs = new CapturingAppender();
                 CachedSupplier<String> cachedSupplier = CachedSupplier.builder(slowFailingSource)
                                                                       .cachedValueName(name)
                                                                       .staleValueBehavior(ALLOW)
                                                                       .clock(clock)
                                                                       .prefetchJitterEnabled(false)
                                                                       .build()) {
                assertThat(cachedSupplier.get()).isEqualTo("original");

                clock.time = expiration.minusSeconds(1300);
                assertThat(cachedSupplier.get()).isEqualTo("original");
                Instant failureRecordedTime = clock.time;
                assertThat(failureRecordedTime).isEqualTo(expiration.minusSeconds(100));

                long backoffSeconds = singleRetryAfterSeconds(logs, name);
                assertThat(backoffSeconds).isBetween(EXPIRED_BACKOFF_MIN_SECONDS, EXPIRED_BACKOFF_MAX_SECONDS);

                // The backoff also starts when the failed call returned, not when it started.
                clock.time = failureRecordedTime.plusSeconds(backoffSeconds - 1);
                assertThat(cachedSupplier.get()).isEqualTo("original");
                clock.time = failureRecordedTime.plusSeconds(backoffSeconds);
                assertThat(cachedSupplier.get()).isEqualTo("fresh");
                assertThat(calls.get()).isEqualTo(3);
            }
        }
    }

    @Test
    public void invalidate_whenRefreshThenFails_backoffTreatsInvalidatedValueAsExpired() {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;
        String name = "invalidatedThenFailed";

        try (CapturingAppender logs = new CapturingAppender();
             CachedSupplier<String> cachedSupplier = allowModeSupplier(supplier, clock, name)) {
            supplier.set(RefreshResult.builder("original")
                                      .expiration(now.plusSeconds(3600))
                                      .staleTime(now.plusSeconds(3540))
                                      .prefetchTime(now.plusSeconds(3300))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("original");

            // The value still has an hour left, but it is invalidated, so the failed refresh treats it as expired.
            Instant failureTime = now.plusSeconds(10);
            clock.time = failureTime;
            cachedSupplier.invalidate(v -> v.equals("original"));
            supplier.set(new RuntimeException("source down"));
            assertThat(cachedSupplier.get()).isEqualTo("original");

            long backoffSeconds = singleRetryAfterSeconds(logs, name);
            assertThat(backoffSeconds).isBetween(EXPIRED_BACKOFF_MIN_SECONDS, EXPIRED_BACKOFF_MAX_SECONDS);
            assertSourceNotContactedUntilBackoffElapses(cachedSupplier, clock, supplier, failureTime, backoffSeconds,
                                                        "original");
        }
    }

    @Test
    public void get_whenRefreshFailsRepeatedly_backoffIsJittered() {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        String name = "jitteredBackoff";
        int iterations = 50;

        try (CapturingAppender logs = new CapturingAppender()) {
            for (int i = 0; i < iterations; i++) {
                clock.time = now;
                try (CachedSupplier<String> cachedSupplier = allowModeSupplier(supplier, clock, name)) {
                    supplier.set(RefreshResult.builder("original")
                                              .expiration(now.plusSeconds(7200))
                                              .staleTime(now.plusSeconds(7140))
                                              .prefetchTime(now.plusSeconds(10))
                                              .build());
                    cachedSupplier.get();

                    // 3600 seconds left: the upper end of the range is at its cap.
                    clock.time = now.plusSeconds(3600);
                    supplier.set(new RuntimeException("source down"));
                    assertThat(cachedSupplier.get()).isEqualTo("original");
                }
            }

            List<Long> backoffs = logs.warnEventsFor(name).stream()
                                      .map(CachedSupplierTest::retryAfterSeconds)
                                      .collect(Collectors.toList());
            assertThat(backoffs).hasSize(iterations)
                                .allSatisfy(b -> assertThat(b).isBetween(LONG_LIVED_BACKOFF_MIN_SECONDS,
                                                                         LONG_LIVED_BACKOFF_MAX_SECONDS));
            assertThat(new HashSet<>(backoffs)).hasSizeGreaterThan(1);
        }
    }

    @ParameterizedTest(name = "remaining={0}s -> backoff range [60, {1}]s")
    @CsvSource({
        // Expired, or half the remaining lifetime at or below the 90 second floor (3 minutes or less)
        "0, 90",
        "1, 90",
        "179, 90",
        "180, 90",
        "181, 90",
        // Just above the floor: half the remaining lifetime, rounded down
        "182, 91",
        "183, 91",
        // The SEP's table: 5 minutes -> 2.5 minutes, 10 minutes -> 5 minutes
        "300, 150",
        "600, 300",
        // Just below, at and above the 600 second cap (20 minutes or more)
        "1199, 599",
        "1200, 600",
        "1201, 600",
        "3600, 600"
    })
    public void refreshBackoffSeconds_withRemainingLifetime_isUniformFromSixtySecondsToClampedHalfRemaining(
        long remainingSeconds, long expectedUpperSeconds) {
        int expectedChoices = (int) (expectedUpperSeconds - BACKOFF_MIN_SECONDS + 1);

        BoundaryRandom lowest = new BoundaryRandom(false);
        assertThat(CachedSupplier.refreshBackoffSeconds(remainingSeconds, lowest)).isEqualTo(BACKOFF_MIN_SECONDS);
        assertThat(lowest.lastBound).isEqualTo(expectedChoices);

        BoundaryRandom highest = new BoundaryRandom(true);
        assertThat(CachedSupplier.refreshBackoffSeconds(remainingSeconds, highest)).isEqualTo(expectedUpperSeconds);
        assertThat(highest.lastBound).isEqualTo(expectedChoices);
    }

    @ParameterizedTest(name = "expiration={0}s, stale={1}s -> remaining {2}s")
    @CsvSource({
        // The expiration determines the remaining lifetime, even when the stale time is earlier
        "1000, 100, 1000",
        "1000, 940, 1000",
        // Expired at or before now: 0
        "0, -60, 0",
        "-600, -660, 0",
        // Capped at 1200 seconds, which already selects the 600 second cap of the upper bound
        "5000, 4940, 1200",
        // No expiration: the stale time is used instead
        ", 1000, 1000",
        ", 0, 0",
        ", -60, 0"
    })
    public void remainingLifetimeSeconds_withExpirationAndStaleTime_usesExpirationOrElseStaleTime(Long expirationOffsetSeconds,
                                                                                                 long staleOffsetSeconds,
                                                                                                 long expectedRemainingSeconds) {
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        RefreshResult<String> value = RefreshResult.builder("value")
                                                   .expiration(offsetOrNull(now, expirationOffsetSeconds))
                                                   .staleTime(now.plusSeconds(staleOffsetSeconds))
                                                   .build();

        assertThat(CachedSupplier.remainingLifetimeSeconds(value, now)).isEqualTo(expectedRemainingSeconds);
    }

    @Test
    public void remainingLifetimeSeconds_withPartialSeconds_roundsDown() {
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        RefreshResult<String> value = RefreshResult.builder("value")
                                                   .expiration(now.plusMillis(300_999))
                                                   .build();

        assertThat(CachedSupplier.remainingLifetimeSeconds(value, now)).isEqualTo(300);
    }

    @Test
    public void remainingLifetimeSeconds_withNoCachedValue_isZero() {
        assertThat(CachedSupplier.remainingLifetimeSeconds(null, Instant.parse("2024-01-01T00:00:00Z"))).isZero();
    }

    @Test
    public void remainingLifetimeSeconds_withNeitherExpirationNorStaleTime_isTreatedAsNeverExpiring() {
        Instant now = Instant.parse("2024-01-01T00:00:00Z");

        // The default stale time is Instant.MAX. Some callers set the stale time to null for a value that never expires.
        RefreshResult<String> defaultStaleTime = RefreshResult.builder("value").build();
        RefreshResult<String> nullStaleTime = RefreshResult.builder("value").staleTime(null).build();

        assertThat(CachedSupplier.remainingLifetimeSeconds(defaultStaleTime, now)).isEqualTo(1200);
        assertThat(CachedSupplier.remainingLifetimeSeconds(nullStaleTime, now)).isEqualTo(1200);
        assertThat(CachedSupplier.refreshBackoffSeconds(1200, new BoundaryRandom(true)))
            .isEqualTo(LONG_LIVED_BACKOFF_MAX_SECONDS);
    }

    // --- initial fetch returns already-expired value tests ---

    @Test
    public void get_whenInitialFetchReturnsExpiredValue_returnsItWithoutThrowing() {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;

        try (CachedSupplier<String> cachedSupplier = allowModeSupplier(supplier, clock, "initialExpiredReturned")) {
            supplier.set(expiredInitialValue(now));

            assertThat(cachedSupplier.get()).isEqualTo("expired");
        }
    }

    @ParameterizedTest(name = "expiration={0}s, stale={1}s, prefetch={2}s")
    @CsvSource({
        // Prefetch at a fixed window before expiration, in the past (STS, SSO, Login, Container)
        "-60, -120, -960",
        // No prefetch time (IMDS for expired credentials)
        "-60, -120, ",
        // No expiration: the stale time is the expiration
        ", -60, -60"
    })
    public void get_whenInitialFetchReturnsExpiredValue_nextCallRefreshesWithoutBackoff(Long expirationOffsetSeconds,
                                                                                       long staleOffsetSeconds,
                                                                                       Long prefetchOffsetSeconds) {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;

        try (CachedSupplier<String> cachedSupplier = allowModeSupplier(supplier, clock, "initialExpiredNoBackoff")) {
            supplier.set(RefreshResult.builder("expired")
                                      .expiration(offsetOrNull(now, expirationOffsetSeconds))
                                      .staleTime(now.plusSeconds(staleOffsetSeconds))
                                      .prefetchTime(offsetOrNull(now, prefetchOffsetSeconds))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("expired");

            // The initial fetch did not arm the refresh backoff: the very next call, at the same instant, refreshes.
            supplier.set(RefreshResult.builder("fresh")
                                      .staleTime(Instant.MAX)
                                      .prefetchTime(Instant.MAX)
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("fresh");
        }
    }

    @Test
    public void get_whenInitialFetchReturnsExpiredValue_logsWarnWithoutRetryTime() {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;
        String name = "initialExpiredWarn";

        try (CapturingAppender logs = new CapturingAppender();
             CachedSupplier<String> cachedSupplier = allowModeSupplier(supplier, clock, name)) {
            RefreshResult<String> expired = expiredInitialValue(now);
            supplier.set(expired);
            assertThat(cachedSupplier.get()).isEqualTo("expired");

            List<LogEvent> warnings = logs.warnEventsFor(name);
            assertThat(warnings).hasSize(1);
            String message = warnings.get(0).getMessage().getFormattedMessage();
            assertThat(message).doesNotContain("Credential refresh failed")
                               .contains("The credential source returned credentials that expired at "
                                         + expired.expiration())
                               .contains("No other credentials are cached, so the SDK will use them.");
            assertThat(warnings.get(0).getThrown()).isNull();
            assertThat(message).contains("The next call will attempt to refresh these credentials.")
                               .doesNotContain("attempted again after");
        }
    }

    @Test
    public void get_whenInitialExpiredValueRefreshReturnsExpiredAgain_treatedAsFailedRefreshWithBackoff() {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;
        String name = "initialExpiredThenExpiredAgain";

        try (CapturingAppender logs = new CapturingAppender();
             CachedSupplier<String> cachedSupplier = allowModeSupplier(supplier, clock, name)) {
            supplier.set(expiredInitialValue(now));
            assertThat(cachedSupplier.get()).isEqualTo("expired");

            // The next call refreshes. A value is now cached, so another expired value is a failed refresh: the cached
            // value is kept and the backoff for an expired value (60-90 seconds) is armed.
            Instant failureTime = now.plusSeconds(1);
            clock.time = failureTime;
            Instant expiredAgainAt = failureTime.minusSeconds(10);
            supplier.set(RefreshResult.builder("expired-again")
                                      .expiration(expiredAgainAt)
                                      .staleTime(expiredAgainAt.minusSeconds(60))
                                      .prefetchTime(expiredAgainAt.minusSeconds(900))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("expired");

            List<LogEvent> warnings = logs.warnEventsFor(name);
            assertThat(warnings).hasSize(2);
            assertRefreshFailureWarning(warnings.get(1), "Credential refresh failed: the credential source returned "
                                                         + "credentials that expired at " + expiredAgainAt,
                                        EXPIRED_BACKOFF_MIN_SECONDS, EXPIRED_BACKOFF_MAX_SECONDS);
            assertSourceNotContactedUntilBackoffElapses(cachedSupplier, clock, supplier, failureTime,
                                                        retryAfterSeconds(warnings.get(1)), "expired");
        }
    }

    @Test
    public void get_whenInitialExpiredValueRefreshFails_returnsCachedValueAndBacksOff() {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;
        String name = "initialExpiredThenFails";

        try (CapturingAppender logs = new CapturingAppender();
             CachedSupplier<String> cachedSupplier = allowModeSupplier(supplier, clock, name)) {
            supplier.set(expiredInitialValue(now));
            assertThat(cachedSupplier.get()).isEqualTo("expired");

            // The next call refreshes and fails: the cached expired value is returned and the backoff is armed.
            Instant failureTime = now.plusSeconds(1);
            clock.time = failureTime;
            supplier.set(new RuntimeException("source down"));
            assertThat(cachedSupplier.get()).isEqualTo("expired");

            List<LogEvent> warnings = logs.warnEventsFor(name);
            assertThat(warnings).hasSize(2);
            assertRefreshFailureWarning(warnings.get(1), "Credential refresh failed: source down",
                                        EXPIRED_BACKOFF_MIN_SECONDS, EXPIRED_BACKOFF_MAX_SECONDS);
            assertSourceNotContactedUntilBackoffElapses(cachedSupplier, clock, supplier, failureTime,
                                                        retryAfterSeconds(warnings.get(1)), "expired");
        }
    }

    @Test
    public void get_whenInitialExpiredValueRefreshSucceeds_replacesIt() {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;
        String name = "initialExpiredThenSucceeds";

        try (CapturingAppender logs = new CapturingAppender();
             CachedSupplier<String> cachedSupplier = allowModeSupplier(supplier, clock, name)) {
            supplier.set(expiredInitialValue(now));
            assertThat(cachedSupplier.get()).isEqualTo("expired");

            // The next call refreshes and gets valid credentials, which replace the cached expired value.
            Instant refreshTime = now.plusSeconds(1);
            clock.time = refreshTime;
            supplier.set(RefreshResult.builder("fresh")
                                      .expiration(refreshTime.plusSeconds(3600))
                                      .staleTime(refreshTime.plusSeconds(3540))
                                      .prefetchTime(refreshTime.plusSeconds(2700))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("fresh");

            // The fresh value is used until its prefetch time, then refreshed without any backoff.
            supplier.set(RefreshResult.builder("next")
                                      .staleTime(Instant.MAX)
                                      .prefetchTime(Instant.MAX)
                                      .build());
            clock.time = refreshTime.plusSeconds(2699);
            assertThat(cachedSupplier.get()).isEqualTo("fresh");
            clock.time = refreshTime.plusSeconds(2700);
            assertThat(cachedSupplier.get()).isEqualTo("next");

            assertThat(logs.warnEventsFor(name)).hasSize(1);
        }
    }

    @Test
    public void get_whenInitialFetchReturnsExpiredValueInStrictMode_refreshesAfterOneSecond() {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;
        String name = "initialExpiredStrict";

        try (CapturingAppender logs = new CapturingAppender();
             CachedSupplier<String> cachedSupplier = CachedSupplier.builder(supplier)
                                                                   .cachedValueName(name)
                                                                   .staleValueBehavior(STRICT)
                                                                   .clock(clock)
                                                                   .prefetchJitterEnabled(false)
                                                                   .build()) {
            // No prefetch time, so only the stale time decides when the source is called again.
            supplier.set(RefreshResult.builder("expired")
                                      .expiration(now.minusSeconds(60))
                                      .staleTime(now.minusSeconds(120))
                                      .prefetchTime(null)
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("expired");

            supplier.set(RefreshResult.builder("fresh")
                                      .staleTime(Instant.MAX)
                                      .prefetchTime(Instant.MAX)
                                      .build());
            clock.time = now.plusMillis(999);
            assertThat(cachedSupplier.get()).isEqualTo("expired");
            clock.time = now.plusSeconds(1);
            assertThat(cachedSupplier.get()).isEqualTo("fresh");

            assertThat(logs.warnEventsFor(name)).isEmpty();
        }
    }

    @Test
    public void get_whenInitialFetchReturnsExpiredValueAfterNonRecoverableError_returnsItAndClearsTheError() {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;
        String name = "initialExpiredAfterNonRecoverable";

        try (CapturingAppender logs = new CapturingAppender();
             CachedSupplier<String> cachedSupplier = CachedSupplier.builder(supplier)
                                                                   .cachedValueName(name)
                                                                   .staleValueBehavior(ALLOW)
                                                                   .nonRecoverableErrorPredicate(
                                                                       e -> e instanceof CacheInvalidatingRuntimeException)
                                                                   .clock(clock)
                                                                   .prefetchJitterEnabled(false)
                                                                   .build()) {
            // The first initial fetch fails with a non-recoverable error, which is cached.
            CacheInvalidatingRuntimeException nonRecoverableError = new CacheInvalidatingRuntimeException("token expired");
            supplier.set(nonRecoverableError);
            assertThatThrownBy(cachedSupplier::get).isEqualTo(nonRecoverableError);

            // After the error cache window, the initial fetch returns expired credentials: they are returned, not the error.
            Instant initialFetchTime = now.plusSeconds(PAST_NON_RECOVERABLE_ERROR_CACHE);
            clock.time = initialFetchTime;
            supplier.set(expiredInitialValue(initialFetchTime));
            assertThat(cachedSupplier.get()).isEqualTo("expired");

            // The next call refreshes. A recoverable failure keeps the expired value and arms the backoff; the earlier
            // non-recoverable error is not re-raised.
            Instant failureTime = initialFetchTime.plusSeconds(1);
            clock.time = failureTime;
            supplier.set(new RuntimeException("source down"));
            assertThat(cachedSupplier.get()).isEqualTo("expired");

            List<LogEvent> warnings = logs.warnEventsFor(name);
            assertThat(warnings).hasSize(2);
            assertRefreshFailureWarning(warnings.get(1), "Credential refresh failed: source down",
                                        EXPIRED_BACKOFF_MIN_SECONDS, EXPIRED_BACKOFF_MAX_SECONDS);
            assertSourceNotContactedUntilBackoffElapses(cachedSupplier, clock, supplier, failureTime,
                                                        retryAfterSeconds(warnings.get(1)), "expired");
        }
    }

    @Test
    public void get_whenConcurrentRefreshOfInitialExpiredValueReturnsExpiredAgain_sourceCalledOnce()
        throws InterruptedException {
        List<String> results = runConcurrentMandatoryRefresh(CachedSupplierTest::expiredInitialValue, 1, () -> {
            Instant expiredAgainAt = Instant.parse("2024-01-01T00:00:00Z");
            return RefreshResult.builder("expired-again")
                                .expiration(expiredAgainAt)
                                .staleTime(expiredAgainAt.minusSeconds(60))
                                .prefetchTime(expiredAgainAt.minusSeconds(900))
                                .build();
        });

        assertThat(results).hasSize(CONCURRENT_CALLERS).containsOnly("expired");
    }

    // --- Values without a stale time ---

    /**
     * A value with a null stale time never becomes stale, like a value whose stale time is not specified. Credential providers
     * return one for credentials without an expiration. The initial fetch returns it, and the source is not called again until
     * the prefetch time.
     */
    @ParameterizedTest
    @EnumSource(StaleValueBehavior.class)
    public void get_whenInitialFetchHasNullStaleTime_returnsValueAndRefreshesAtPrefetchTime(StaleValueBehavior behavior) {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;
        String name = "nullStaleTimeInitial" + behavior;

        try (CapturingAppender logs = new CapturingAppender();
             CachedSupplier<String> cachedSupplier = supplierWithoutJitter(supplier, clock, name, behavior)) {
            supplier.set(RefreshResult.builder("no-stale-time")
                                      .staleTime(null)
                                      .prefetchTime(now.plusSeconds(3600))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("no-stale-time");

            supplier.set(RefreshResult.builder("next")
                                      .staleTime(Instant.MAX)
                                      .prefetchTime(Instant.MAX)
                                      .build());
            clock.time = now.plusSeconds(3599);
            assertThat(cachedSupplier.get()).isEqualTo("no-stale-time");
            clock.time = now.plusSeconds(3600);
            assertThat(cachedSupplier.get()).isEqualTo("next");

            assertThat(logs.warnEventsFor(name)).isEmpty();
        }
    }

    /**
     * A refresh that returns a value with a null stale time is a successful refresh, whether it happens in the prefetch window
     * (1800 seconds) or once the cached value is stale (3600 seconds). The value is used, no refresh backoff is applied, and
     * nothing is logged at WARN. The value does not become stale when the previous value would have, and is refreshed at its
     * own prefetch time.
     */
    @ParameterizedTest(name = "{0}, refresh at {1}s")
    @CsvSource({
        "STRICT, 1800",
        "STRICT, 3600",
        "ALLOW,  1800",
        "ALLOW,  3600"
    })
    public void get_whenRefreshReturnsNullStaleTime_usesNewValueWithoutBackoff(StaleValueBehavior behavior,
                                                                                long refreshOffsetSeconds) {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;
        String name = "nullStaleTimeRefresh" + behavior + refreshOffsetSeconds;

        try (CapturingAppender logs = new CapturingAppender();
             CachedSupplier<String> cachedSupplier = supplierWithoutJitter(supplier, clock, name, behavior)) {
            supplier.set(RefreshResult.builder("old")
                                      .expiration(now.plusSeconds(3660))
                                      .staleTime(now.plusSeconds(3600))
                                      .prefetchTime(now.plusSeconds(1800))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("old");

            Instant refreshTime = now.plusSeconds(refreshOffsetSeconds);
            Instant nextPrefetchTime = refreshTime.plusSeconds(3600);
            clock.time = refreshTime;
            supplier.set(RefreshResult.builder("no-stale-time")
                                      .staleTime(null)
                                      .prefetchTime(nextPrefetchTime)
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("no-stale-time");

            supplier.set(RefreshResult.builder("next")
                                      .staleTime(Instant.MAX)
                                      .prefetchTime(Instant.MAX)
                                      .build());
            // Past the previous value's expiration, the new value is still used without calling the source.
            clock.time = now.plusSeconds(3661);
            assertThat(cachedSupplier.get()).isEqualTo("no-stale-time");
            clock.time = nextPrefetchTime.minusSeconds(1);
            assertThat(cachedSupplier.get()).isEqualTo("no-stale-time");
            clock.time = nextPrefetchTime;
            assertThat(cachedSupplier.get()).isEqualTo("next");

            assertThat(logs.warnEventsFor(name)).isEmpty();
        }
    }

    /**
     * Invalidating a cached value that has a null stale time and no prefetch time, and so would otherwise never be refreshed,
     * makes the next call refresh it.
     */
    @ParameterizedTest
    @EnumSource(StaleValueBehavior.class)
    public void invalidate_whenCachedValueHasNullStaleTime_nextGetRefreshes(StaleValueBehavior behavior) {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;

        try (CachedSupplier<String> cachedSupplier = supplierWithoutJitter(supplier, clock, "nullStaleTimeInvalidate",
                                                                           behavior)) {
            supplier.set(RefreshResult.builder("no-stale-time")
                                      .staleTime(null)
                                      .prefetchTime(null)
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("no-stale-time");

            supplier.set(RefreshResult.builder("next")
                                      .staleTime(Instant.MAX)
                                      .prefetchTime(Instant.MAX)
                                      .build());
            clock.time = now.plus(Duration.ofDays(365));
            assertThat(cachedSupplier.get()).isEqualTo("no-stale-time");

            cachedSupplier.invalidate("no-stale-time"::equals);
            assertThat(cachedSupplier.get()).isEqualTo("next");
        }
    }

    /**
     * A value whose expiration, stale time, and prefetch time are all before {@code now}, shaped like the credentials an STS
     * or container credential provider would cache.
     */
    private static RefreshResult<String> expiredInitialValue(Instant now) {
        Instant expiration = now.minusSeconds(60);
        return RefreshResult.builder("expired")
                            .expiration(expiration)
                            .staleTime(expiration.minusSeconds(60))
                            .prefetchTime(expiration.minusSeconds(900))
                            .build();
    }

    private static Instant offsetOrNull(Instant time, Long offsetSeconds) {
        return offsetSeconds == null ? null : time.plusSeconds(offsetSeconds);
    }

    private static CachedSupplier<String> allowModeSupplier(MutableSupplier supplier, AdjustableClock clock, String name) {
        return supplierWithoutJitter(supplier, clock, name, ALLOW);
    }

    private static CachedSupplier<String> supplierWithoutJitter(MutableSupplier supplier, AdjustableClock clock, String name,
                                                                StaleValueBehavior behavior) {
        return CachedSupplier.builder(supplier)
                             .cachedValueName(name)
                             .staleValueBehavior(behavior)
                             .clock(clock)
                             .prefetchJitterEnabled(false)
                             .build();
    }

    /**
     * Returns the backoff reported by the only failed refresh warning logged for the cache with the given name.
     */
    private static long singleRetryAfterSeconds(CapturingAppender logs, String name) {
        List<LogEvent> warnings = logs.warnEventsFor(name);
        assertThat(warnings).hasSize(1);
        return retryAfterSeconds(warnings.get(0));
    }

    /**
     * Asserts that the source is not contacted until exactly {@code backoffSeconds} after {@code failureTime}: one second
     * before, the cached value is still returned; at that time, a fresh value from the source is returned.
     */
    private static void assertSourceNotContactedUntilBackoffElapses(CachedSupplier<String> cachedSupplier,
                                                                    AdjustableClock clock,
                                                                    MutableSupplier supplier,
                                                                    Instant failureTime,
                                                                    long backoffSeconds,
                                                                    String cachedValue) {
        supplier.set(RefreshResult.builder("fresh")
                                  .staleTime(Instant.MAX)
                                  .prefetchTime(Instant.MAX)
                                  .build());

        clock.time = failureTime.plusSeconds(backoffSeconds - 1);
        assertThat(cachedSupplier.get()).isEqualTo(cachedValue);

        clock.time = failureTime.plusSeconds(backoffSeconds);
        assertThat(cachedSupplier.get()).isEqualTo("fresh");
    }

    private static void assertRefreshFailureWarning(LogEvent event, String expectedFailure, long expectedMinBackoffSeconds,
                                                    long expectedMaxBackoffSeconds) {
        String message = event.getMessage().getFormattedMessage();
        assertThat(message).contains(expectedFailure)
                           .contains("The SDK will continue using cached credentials.")
                           .contains("A refresh of these credentials will be attempted again after");

        assertThat(retryAfterSeconds(event)).isBetween(expectedMinBackoffSeconds, expectedMaxBackoffSeconds);
    }

    /**
     * Returns the backoff, in seconds, reported by a failed refresh warning.
     */
    private static long retryAfterSeconds(LogEvent event) {
        Matcher retryAfter = RETRY_AFTER_SECONDS.matcher(event.getMessage().getFormattedMessage());
        assertThat(retryAfter.find()).isTrue();
        return Long.parseLong(retryAfter.group(1));
    }

    /**
     * Captures log events emitted while it is open. Modeled on {@code LogCaptor} in test-utils, which utils cannot depend on.
     */
    private static final class CapturingAppender extends AbstractAppender implements AutoCloseable {
        private final List<LogEvent> events = Collections.synchronizedList(new ArrayList<>());
        private final Level originalRootLevel;

        private CapturingAppender() {
            super(CapturingAppender.class.getName(), null, null, false, Property.EMPTY_ARRAY);
            this.originalRootLevel = rootLogger().getLevel();
            rootLogger().addAppender(this);
            start();
            Configurator.setRootLevel(Level.WARN);
        }

        @Override
        public void append(LogEvent event) {
            events.add(event.toImmutable());
        }

        /**
         * WARN events from {@link CachedSupplier} for the cache with the given name.
         */
        private List<LogEvent> warnEventsFor(String cachedValueName) {
            synchronized (events) {
                return events.stream()
                             .filter(e -> e.getLevel() == Level.WARN)
                             .filter(e -> CachedSupplier.class.getName().equals(e.getLoggerName()))
                             .filter(e -> e.getMessage().getFormattedMessage().startsWith("(" + cachedValueName + ")"))
                             .collect(Collectors.toList());
            }
        }

        @Override
        public void close() {
            rootLogger().removeAppender(this);
            stop();
            Configurator.setRootLevel(originalRootLevel);
        }

        private static org.apache.logging.log4j.core.Logger rootLogger() {
            return (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
        }
    }
}
