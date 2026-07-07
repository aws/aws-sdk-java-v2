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

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.utils.cache.CachedSupplier.StaleValueBehavior.ALLOW;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CachedSupplier#invalidate} and the {@code nextAllowedRefreshTime} backoff behavior.
 *
 * Validates Requirements: 8, 9, 10, 12, 13
 */
public class CachedSupplierInvalidateTest {

    // --- Test 1: Predicate matching — true case ---

    @Test
    public void invalidate_predicateMatches_setsStaleTimeToNow_triggersRefresh() {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;

        try (CachedSupplier<String> cachedSupplier = CachedSupplier.builder(supplier)
                                                                   .staleValueBehavior(ALLOW)
                                                                   .clock(clock)
                                                                   .jitterEnabled(false)
                                                                   .build()) {
            // Initial fetch
            supplier.set(RefreshResult.builder("value-1")
                                      .staleTime(now.plusSeconds(3600))
                                      .prefetchTime(now.plusSeconds(1800))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("value-1");

            // Invalidate with matching predicate
            clock.time = now.plusSeconds(10);
            cachedSupplier.invalidate(v -> v.equals("value-1"));

            // Set up a new value for the refresh
            supplier.set(RefreshResult.builder("value-2")
                                      .staleTime(now.plusSeconds(7200))
                                      .prefetchTime(now.plusSeconds(5400))
                                      .build());

            // Next get() should trigger refresh because staleTime was set to now
            assertThat(cachedSupplier.get()).isEqualTo("value-2");
        }
    }

    // --- Test 2: Predicate matching — false case ---

    @Test
    public void invalidate_predicateDoesNotMatch_nothingChanges() {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;

        try (CachedSupplier<String> cachedSupplier = CachedSupplier.builder(supplier)
                                                                   .staleValueBehavior(ALLOW)
                                                                   .clock(clock)
                                                                   .jitterEnabled(false)
                                                                   .build()) {
            // Initial fetch
            supplier.set(RefreshResult.builder("value-1")
                                      .staleTime(now.plusSeconds(3600))
                                      .prefetchTime(now.plusSeconds(1800))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("value-1");

            // Invalidate with non-matching predicate
            cachedSupplier.invalidate(v -> v.equals("different-value"));

            // Set a new value — should NOT be fetched since cache is still valid
            supplier.set(RefreshResult.builder("value-2")
                                      .staleTime(now.plusSeconds(7200))
                                      .prefetchTime(now.plusSeconds(5400))
                                      .build());

            // Should still return the original cached value
            assertThat(cachedSupplier.get()).isEqualTo("value-1");
        }
    }

    // --- Test 3: Null cached value ---

    @Test
    public void invalidate_nullCachedValue_doesNothing() {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;

        try (CachedSupplier<String> cachedSupplier = CachedSupplier.builder(supplier)
                                                                   .staleValueBehavior(ALLOW)
                                                                   .clock(clock)
                                                                   .jitterEnabled(false)
                                                                   .build()) {
            // Call invalidate before any get() — cachedValue is null
            // Should not throw
            cachedSupplier.invalidate(v -> true);

            // Now set up a supplier and get the value normally
            supplier.set(RefreshResult.builder("value-1")
                                      .staleTime(now.plusSeconds(3600))
                                      .prefetchTime(now.plusSeconds(1800))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("value-1");
        }
    }

    // --- Test 4: Value preservation — successful refresh returns fresh value ---

    @Test
    public void invalidate_successfulRefresh_returnsFreshValue() {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;

        try (CachedSupplier<String> cachedSupplier = CachedSupplier.builder(supplier)
                                                                   .staleValueBehavior(ALLOW)
                                                                   .clock(clock)
                                                                   .jitterEnabled(false)
                                                                   .build()) {
            // Initial fetch
            supplier.set(RefreshResult.builder("old-creds")
                                      .staleTime(now.plusSeconds(3600))
                                      .prefetchTime(now.plusSeconds(1800))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("old-creds");

            // Invalidate
            clock.time = now.plusSeconds(10);
            cachedSupplier.invalidate(v -> v.equals("old-creds"));

            // Supplier returns fresh value
            supplier.set(RefreshResult.builder("new-creds")
                                      .staleTime(now.plusSeconds(7200))
                                      .prefetchTime(now.plusSeconds(5400))
                                      .build());

            // get() triggers refresh and returns new value
            assertThat(cachedSupplier.get()).isEqualTo("new-creds");
        }
    }

    // --- Test 4b: Value preservation — backoff active returns stale cached value ---

    @Test
    public void invalidate_backoffActive_returnsStaleCachedValue() {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;

        try (CachedSupplier<String> cachedSupplier = CachedSupplier.builder(supplier)
                                                                   .staleValueBehavior(ALLOW)
                                                                   .clock(clock)
                                                                   .jitterEnabled(false)
                                                                   .build()) {
            // Initial fetch
            supplier.set(RefreshResult.builder("old-creds")
                                      .staleTime(now.plusSeconds(60))
                                      .prefetchTime(now.plusSeconds(30))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("old-creds");

            // Advance past stale time and trigger a failure to set nextAllowedRefreshTime
            clock.time = now.plusSeconds(61);
            supplier.set(new RuntimeException("service unavailable"));
            assertThat(cachedSupplier.get()).isEqualTo("old-creds");

            // Now nextAllowedRefreshTime is set (now+61 + [300,600]s)
            // Call invalidate — it should NOT clear nextAllowedRefreshTime
            clock.time = now.plusSeconds(62);
            cachedSupplier.invalidate(v -> v.equals("old-creds"));

            // Set up a fresh value in the supplier
            supplier.set(RefreshResult.builder("new-creds")
                                      .staleTime(Instant.MAX)
                                      .prefetchTime(Instant.MAX)
                                      .build());

            // get() should still return the cached (stale) value because backoff is active
            assertThat(cachedSupplier.get()).isEqualTo("old-creds");
        }
    }

    // --- Test 5: Backoff gating — invalidate does NOT clear nextAllowedRefreshTime ---

    @Test
    public void invalidate_doesNotClearNextAllowedRefreshTime() {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;

        try (CachedSupplier<String> cachedSupplier = CachedSupplier.builder(supplier)
                                                                   .staleValueBehavior(ALLOW)
                                                                   .clock(clock)
                                                                   .jitterEnabled(false)
                                                                   .build()) {
            // Initial fetch
            supplier.set(RefreshResult.builder("cached-creds")
                                      .staleTime(now.plusSeconds(60))
                                      .prefetchTime(now.plusSeconds(30))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("cached-creds");

            // Advance past stale time and trigger failure to set backoff gate
            clock.time = now.plusSeconds(61);
            supplier.set(new RuntimeException("service unavailable"));
            assertThat(cachedSupplier.get()).isEqualTo("cached-creds");

            // Backoff gate is now set to somewhere in [61+300, 61+600] seconds from 'now'
            // Call invalidate
            clock.time = now.plusSeconds(70);
            cachedSupplier.invalidate(v -> v.equals("cached-creds"));

            // Prepare a new value — but backoff should prevent refresh
            supplier.set(RefreshResult.builder("new-creds")
                                      .staleTime(Instant.MAX)
                                      .prefetchTime(Instant.MAX)
                                      .build());

            // Still within backoff — should return cached value
            clock.time = now.plusSeconds(100);
            assertThat(cachedSupplier.get()).isEqualTo("cached-creds");

            // Advance past maximum possible backoff (61 + 600 = 661s)
            clock.time = now.plusSeconds(700);
            // Now backoff has elapsed — refresh should succeed
            assertThat(cachedSupplier.get()).isEqualTo("new-creds");
        }
    }

    // --- Test 5b: After nextAllowedRefreshTime elapses, get() attempts refresh ---

    @Test
    public void afterBackoffElapses_getAttemptsRefresh() {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;

        try (CachedSupplier<String> cachedSupplier = CachedSupplier.builder(supplier)
                                                                   .staleValueBehavior(ALLOW)
                                                                   .clock(clock)
                                                                   .jitterEnabled(false)
                                                                   .build()) {
            // Initial fetch
            supplier.set(RefreshResult.builder("cached-creds")
                                      .staleTime(now.plusSeconds(60))
                                      .prefetchTime(now.plusSeconds(30))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("cached-creds");

            // Trigger failure and set backoff
            clock.time = now.plusSeconds(61);
            supplier.set(new RuntimeException("service unavailable"));
            assertThat(cachedSupplier.get()).isEqualTo("cached-creds");

            // Invalidate so staleTime = now
            clock.time = now.plusSeconds(62);
            cachedSupplier.invalidate(v -> v.equals("cached-creds"));

            // Prepare fresh value
            supplier.set(RefreshResult.builder("fresh-creds")
                                      .staleTime(now.plusSeconds(7200))
                                      .prefetchTime(now.plusSeconds(5400))
                                      .build());

            // Advance past maximum backoff: 61 + 600 = 661s from now
            clock.time = now.plusSeconds(700);
            // Backoff elapsed, staleTime (set to now by invalidate) is in the past → mandatory refresh
            assertThat(cachedSupplier.get()).isEqualTo("fresh-creds");
        }
    }

    // --- Test 6: Successful refresh clears nextAllowedRefreshTime ---

    @Test
    public void successfulRefresh_clearsNextAllowedRefreshTime() {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;

        try (CachedSupplier<String> cachedSupplier = CachedSupplier.builder(supplier)
                                                                   .staleValueBehavior(ALLOW)
                                                                   .clock(clock)
                                                                   .jitterEnabled(false)
                                                                   .build()) {
            // Initial fetch
            supplier.set(RefreshResult.builder("value-1")
                                      .staleTime(now.plusSeconds(60))
                                      .prefetchTime(now.plusSeconds(30))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("value-1");

            // Trigger failure — sets nextAllowedRefreshTime
            clock.time = now.plusSeconds(61);
            supplier.set(new RuntimeException("service unavailable"));
            assertThat(cachedSupplier.get()).isEqualTo("value-1");

            // Advance past maximum backoff and do a successful refresh
            clock.time = now.plusSeconds(700);
            supplier.set(RefreshResult.builder("value-2")
                                      .staleTime(now.plusSeconds(3700))
                                      .prefetchTime(now.plusSeconds(2000))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("value-2");

            // Now invalidate and verify immediate refresh happens (no backoff gate blocking)
            clock.time = now.plusSeconds(710);
            cachedSupplier.invalidate(v -> v.equals("value-2"));

            supplier.set(RefreshResult.builder("value-3")
                                      .staleTime(now.plusSeconds(7200))
                                      .prefetchTime(now.plusSeconds(5400))
                                      .build());

            // Since nextAllowedRefreshTime was cleared by the successful refresh,
            // the invalidation should trigger an immediate refresh
            assertThat(cachedSupplier.get()).isEqualTo("value-3");
        }
    }

    // --- Test 7: Thread safety ---

    @Test
    public void concurrentInvalidateAndGet_noCorruption() throws Exception {
        AdjustableClock clock = new AdjustableClock();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;

        AtomicInteger counter = new AtomicInteger(0);
        Supplier<RefreshResult<String>> supplier = () -> {
            String val = "value-" + counter.incrementAndGet();
            return RefreshResult.builder(val)
                                .staleTime(now.plusSeconds(3600))
                                .prefetchTime(now.plusSeconds(1800))
                                .build();
        };

        try (CachedSupplier<String> cachedSupplier = CachedSupplier.builder(supplier)
                                                                   .staleValueBehavior(ALLOW)
                                                                   .clock(clock)
                                                                   .jitterEnabled(false)
                                                                   .build()) {
            // Prime the cache
            assertThat(cachedSupplier.get()).isNotNull();

            int threadCount = 20;
            int iterations = 100;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            List<Future<?>> futures = new ArrayList<>();

            for (int i = 0; i < threadCount; i++) {
                final int threadIdx = i;
                futures.add(executor.submit(() -> {
                    try {
                        startLatch.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    for (int j = 0; j < iterations; j++) {
                        if (threadIdx % 2 == 0) {
                            // Half the threads call invalidate
                            cachedSupplier.invalidate(v -> true);
                        } else {
                            // Half the threads call get
                            String value = cachedSupplier.get();
                            assertThat(value).isNotNull();
                        }
                    }
                }));
            }

            // Release all threads simultaneously
            startLatch.countDown();

            // Wait for all to complete
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }

            executor.shutdown();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

            // Final get() should return a non-null value
            assertThat(cachedSupplier.get()).isNotNull();
        }
    }

    // --- Test 8: Invalidate then refresh success (full cycle) ---

    @Test
    public void invalidateThenRefreshSuccess_fullCycle() {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;

        try (CachedSupplier<String> cachedSupplier = CachedSupplier.builder(supplier)
                                                                   .staleValueBehavior(ALLOW)
                                                                   .clock(clock)
                                                                   .jitterEnabled(false)
                                                                   .build()) {
            // Step 1: Initial get()
            supplier.set(RefreshResult.builder("initial-value")
                                      .staleTime(now.plusSeconds(3600))
                                      .prefetchTime(now.plusSeconds(1800))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("initial-value");

            // Step 2: invalidate()
            clock.time = now.plusSeconds(10);
            cachedSupplier.invalidate(v -> v.equals("initial-value"));

            // Step 3: get() triggers refresh → success returns new value
            supplier.set(RefreshResult.builder("refreshed-value")
                                      .staleTime(now.plusSeconds(7200))
                                      .prefetchTime(now.plusSeconds(5400))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("refreshed-value");
        }
    }

    // --- Test 9: Invalidate does NOT modify nextAllowedRefreshTime (explicit scenario) ---

    @Test
    public void invalidate_withActiveBackoff_doesNotModifyNextAllowedRefreshTime() {
        AdjustableClock clock = new AdjustableClock();
        MutableSupplier supplier = new MutableSupplier();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;

        try (CachedSupplier<String> cachedSupplier = CachedSupplier.builder(supplier)
                                                                   .staleValueBehavior(ALLOW)
                                                                   .clock(clock)
                                                                   .jitterEnabled(false)
                                                                   .build()) {
            // Initial fetch
            supplier.set(RefreshResult.builder("cached-creds")
                                      .staleTime(now.plusSeconds(60))
                                      .prefetchTime(now.plusSeconds(30))
                                      .build());
            assertThat(cachedSupplier.get()).isEqualTo("cached-creds");

            // Advance past stale time and trigger failure to set nextAllowedRefreshTime
            clock.time = now.plusSeconds(61);
            supplier.set(new RuntimeException("temporary failure"));
            assertThat(cachedSupplier.get()).isEqualTo("cached-creds");

            // At this point, nextAllowedRefreshTime is set somewhere in [361, 661] seconds from 'now'
            // Call invalidate — should NOT modify nextAllowedRefreshTime
            clock.time = now.plusSeconds(65);
            cachedSupplier.invalidate(v -> v.equals("cached-creds"));

            // Prepare a new value that would be returned if refresh is attempted
            supplier.set(RefreshResult.builder("new-creds")
                                      .staleTime(Instant.MAX)
                                      .prefetchTime(Instant.MAX)
                                      .build());

            // Still within the backoff window (65s < minimum backoff end at 361s)
            // get() should return cached value without attempting refresh
            assertThat(cachedSupplier.get()).isEqualTo("cached-creds");

            // Advance slightly — still within backoff range
            clock.time = now.plusSeconds(200);
            assertThat(cachedSupplier.get()).isEqualTo("cached-creds");

            // Advance past maximum possible backoff (61 + 600 = 661s)
            clock.time = now.plusSeconds(700);
            // Now the backoff has elapsed — refresh should be attempted and succeed
            assertThat(cachedSupplier.get()).isEqualTo("new-creds");
        }
    }

    // --- Helper classes ---

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

    private static class AdjustableClock extends Clock {
        private volatile Instant time;

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
}
