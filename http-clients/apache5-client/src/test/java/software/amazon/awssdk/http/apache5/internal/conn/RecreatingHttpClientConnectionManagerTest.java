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

package software.amazon.awssdk.http.apache5.internal.conn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.http.apache5.internal.conn.SafePoolingHttpClientConnectionManagerTest.idleEntries;
import static software.amazon.awssdk.http.apache5.internal.conn.SafePoolingHttpClientConnectionManagerTest.newManager;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.io.ConnectionEndpoint;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.io.LeaseRequest;
import org.apache.hc.client5.http.io.ManagedHttpClientConnection;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.http.apache5.internal.conn.SafePoolingHttpClientConnectionManagerTest.FailingDisposal;
import software.amazon.awssdk.testutils.LogCaptor;

/**
 * Tests for {@link RecreatingHttpClientConnectionManager}, which keeps an Apache HTTP client usable after Apache closes
 * its connection pool in response to a {@link Error}.
 */
public class RecreatingHttpClientConnectionManagerTest {

    private static final HttpRoute ROUTE = new HttpRoute(new HttpHost("localhost", 8080));
    private static final HttpRoute OTHER_ROUTE = new HttpRoute(new HttpHost("example.com", 8080));
    private static final HttpRoute LEASE_ROUTE = new HttpRoute(new HttpHost("lease.example.com", 8080));
    private static final Timeout TIMEOUT = Timeout.ofSeconds(1);
    private static final long FIVE_MINUTES = TimeUnit.MINUTES.toNanos(5);
    private static final String WRAPPER_LOGGER = RecreatingHttpClientConnectionManager.class.getName();

    private List<HttpClientConnectionManager> created;
    private Supplier<HttpClientConnectionManager> factory;
    /** A fake clock for the recovery backoff. It only moves when a test moves it. */
    private AtomicLong now;
    private final List<ExecutorService> executors = new ArrayList<>();

    @BeforeEach
    public void setup() {
        now = new AtomicLong(1_000_000_000L);
        created = new ArrayList<>();
        factory = () -> {
            HttpClientConnectionManager cm = mock(HttpClientConnectionManager.class);
            when(cm.lease(any(), any(), any(), any())).thenReturn(mock(LeaseRequest.class));
            created.add(cm);
            return cm;
        };
    }

    @Test
    public void create_buildsPoolEagerly() {
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(factory);

        assertThat(created).hasSize(1);
        assertThat(cm.currentDelegate()).isSameAs(created.get(0));
        assertThat(cm.recreationCount()).isZero();
    }

    @Test
    public void lease_noClose_reusesSamePool() {
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(factory);

        cm.lease("id-1", ROUTE, TIMEOUT, null);
        cm.lease("id-2", ROUTE, TIMEOUT, null);

        assertThat(created).hasSize(1);
        assertThat(cm.recreationCount()).isZero();
        verify(created.get(0), times(2)).lease(any(), eq(ROUTE), any(), any());
    }

    @Test
    public void close_closesCurrentPoolButDoesNotRebuildEagerly() {
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(factory);

        cm.close(CloseMode.IMMEDIATE);

        verify(created.get(0)).close(CloseMode.IMMEDIATE);
        // The replacement is built lazily, so a client that is closed and never used again costs nothing.
        assertThat(created).hasSize(1);
        assertThat(cm.recreationCount()).isZero();
    }

    @Test
    public void lease_afterClose_buildsReplacementPool() {
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(factory);
        HttpClientConnectionManager original = created.get(0);

        cm.close(CloseMode.IMMEDIATE);
        cm.lease("id-1", ROUTE, TIMEOUT, null);

        assertThat(created).hasSize(2);
        HttpClientConnectionManager replacement = created.get(1);
        assertThat(cm.currentDelegate()).isSameAs(replacement);
        assertThat(cm.recreationCount()).isEqualTo(1);
        verify(replacement).lease(any(), eq(ROUTE), any(), any());
        verify(original, never()).lease(any(), any(), any(), any());
    }

    @Test
    public void lease_afterCloseable_close_buildsReplacementPool() throws Exception {
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(factory);

        // Closeable#close is the other method Apache can reach us through.
        cm.close();
        cm.lease("id-1", ROUTE, TIMEOUT, null);

        assertThat(created).hasSize(2);
        assertThat(cm.recreationCount()).isEqualTo(1);
        verify(created.get(0)).close();
    }

    @Test
    public void repeatedCloses_eachRebuildOnce() {
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(factory, now::get);

        for (int i = 0; i < 3; i++) {
            cm.close(CloseMode.IMMEDIATE);
            cm.lease("id-" + i, ROUTE, TIMEOUT, null);
            // Each cycle lands outside the previous rebuild's backoff window.
            now.addAndGet(FIVE_MINUTES);
        }

        assertThat(created).hasSize(4);
        assertThat(cm.recreationCount()).isEqualTo(3);
    }

    /**
     * Apache calls close() from inside {@code catch (Error error) { ...; throw error; }}. If our close throws, that
     * rethrow never runs and the application sees a cleanup failure instead of the Error that actually caused the
     * problem, so a failure here must not escape.
     */
    @Test
    public void close_poolCloseThrows_doesNotPropagate() {
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(() -> {
            HttpClientConnectionManager pool = mock(HttpClientConnectionManager.class);
            when(pool.lease(any(), any(), any(), any())).thenReturn(mock(LeaseRequest.class));
            doThrow(new OutOfMemoryError("Java heap space")).when(pool).close(any(CloseMode.class));
            created.add(pool);
            return pool;
        });

        cm.close(CloseMode.IMMEDIATE);

        // Still recovers on the next request despite the failed close.
        assertThat(cm.lease("id-1", ROUTE, TIMEOUT, null)).isNotNull();
        assertThat(cm.recreationCount()).isEqualTo(1);
    }

    /**
     * A close that dies part way through sets the pool's shut-down flag before discarding its entries, so a retried close
     * early-returns and those sockets are never closed. The manager's own closeIdle cannot reach them either, because it
     * returns early once the manager is closed, so the rebuild reclaims them through the pool directly.
     */
    @Test
    public void lease_afterFailedClose_closesIdleConnectionsOfAbandonedPool() throws Exception {
        SafeStrictConnPool pool = SafePoolingHttpClientConnectionManagerTest.newPool(new FailingDisposal(1), 10);
        List<ManagedHttpClientConnection> idle = idleEntries(pool, ROUTE, ROUTE, OTHER_ROUTE);
        RecreatingHttpClientConnectionManager cm = createWithFirstPool(newManager(pool));

        cm.close(CloseMode.IMMEDIATE);
        assertThat(closedCount(idle)).isEqualTo(1);

        cm.lease("id-1", ROUTE, TIMEOUT, null);

        assertThat(created).hasSize(1);
        assertThat(cm.recreationCount()).isEqualTo(1);
        assertEachClosedOnce(idle);
    }

    @Test
    public void lease_afterCleanClose_doesNotTouchTheOldPool() throws Exception {
        SafeStrictConnPool pool = SafePoolingHttpClientConnectionManagerTest.newPool(new FailingDisposal(), 10);
        List<ManagedHttpClientConnection> idle = idleEntries(pool, ROUTE, OTHER_ROUTE);
        RecreatingHttpClientConnectionManager cm = createWithFirstPool(newManager(pool));

        cm.close(CloseMode.IMMEDIATE);
        cm.lease("id-1", ROUTE, TIMEOUT, null);

        // The close succeeded and closed everything, and the cleanup does not call back into the discarded pool.
        assertEachClosedOnce(idle);
    }

    @Test
    public void lease_abandonedPoolCleanupThrows_stillRebuilds() throws Exception {
        // The close fails, and so does the cleanup's reclaim.
        SafeStrictConnPool pool = SafePoolingHttpClientConnectionManagerTest.newPool(new FailingDisposal(1, 2), 10);
        idleEntries(pool, ROUTE, ROUTE, ROUTE);
        RecreatingHttpClientConnectionManager cm = createWithFirstPool(newManager(pool));

        cm.close(CloseMode.IMMEDIATE);

        // Cleanup is best effort: failing at it must not stop the client from recovering.
        assertThat(cm.lease("id-1", ROUTE, TIMEOUT, null)).isNotNull();
        assertThat(cm.recreationCount()).isEqualTo(1);
    }

    @Test
    public void lease_afterFailedClose_cleansUpOnlyOnce() throws Exception {
        SafeStrictConnPool pool = SafePoolingHttpClientConnectionManagerTest.newPool(new FailingDisposal(1), 10);
        List<ManagedHttpClientConnection> idle = idleEntries(pool, ROUTE, ROUTE, OTHER_ROUTE);
        RecreatingHttpClientConnectionManager cm = createWithFirstPool(newManager(pool));

        cm.close(CloseMode.IMMEDIATE);
        cm.lease("id-1", ROUTE, TIMEOUT, null);
        now.addAndGet(FIVE_MINUTES);
        cm.close(CloseMode.IMMEDIATE);
        cm.lease("id-2", ROUTE, TIMEOUT, null);

        // The second close succeeded, so the first pool must not be revisited.
        assertEachClosedOnce(idle);
    }

    @Test
    public void closePermanently_doesNotRebuild() {
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(factory);
        HttpClientConnectionManager original = created.get(0);

        cm.closePermanently();
        cm.lease("id-1", ROUTE, TIMEOUT, null);

        // Using a closed SDK HTTP client must keep failing, so the pool is never replaced.
        assertThat(created).hasSize(1);
        assertThat(cm.recreationCount()).isZero();
        verify(original).close(CloseMode.IMMEDIATE);
        verify(original).lease(any(), eq(ROUTE), any(), any());
    }

    @Test
    public void close_afterClosePermanently_isIgnored() {
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(factory);
        HttpClientConnectionManager original = created.get(0);

        cm.closePermanently();
        cm.close(CloseMode.IMMEDIATE);
        cm.lease("id-1", ROUTE, TIMEOUT, null);

        assertThat(created).hasSize(1);
        verify(original, times(1)).close(CloseMode.IMMEDIATE);
    }

    @Test
    public void closePermanently_isIdempotent() {
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(factory);

        cm.closePermanently();
        cm.closePermanently();

        verify(created.get(0), times(1)).close(CloseMode.IMMEDIATE);
    }

    @Test
    public void nonLeaseOperations_doNotBuildAReplacement() {
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(factory);
        cm.close(CloseMode.IMMEDIATE);

        // These all relate to endpoints leased from the pool that was just discarded. Building a fresh pool for them
        // would be pointless, and reaping idle connections must not resurrect a client that is sitting unused.
        cm.release(mock(ConnectionEndpoint.class), null, TimeValue.ZERO_MILLISECONDS);
        cm.closeIdle(TimeValue.ofSeconds(1));

        assertThat(created).hasSize(1);
        assertThat(cm.recreationCount()).isZero();
    }

    /**
     * A caller can read the current pool, have Apache destroy it a moment later, and only then try to lease from it.
     * Apache reports that as {@code IllegalStateException: Connection pool shut down} - the very exception this change
     * exists to eliminate, and one the SDK's retry policy does not retry. It must not escape.
     */
    @Test
    public void lease_poolDestroyedMidLease_retriesOnRebuiltPool() {
        List<HttpClientConnectionManager> pools = new ArrayList<>();
        RecreatingHttpClientConnectionManager[] holder = new RecreatingHttpClientConnectionManager[1];
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(() -> {
            HttpClientConnectionManager pool = mock(HttpClientConnectionManager.class);
            if (pools.isEmpty()) {
                // The first pool is destroyed by a concurrent Error handler just as it is leased from.
                when(pool.lease(any(), any(), any(), any())).thenAnswer(invocation -> {
                    holder[0].close(CloseMode.IMMEDIATE);
                    throw new IllegalStateException("Connection pool shut down");
                });
            } else {
                when(pool.lease(any(), any(), any(), any())).thenReturn(mock(LeaseRequest.class));
            }
            pools.add(pool);
            return pool;
        });
        holder[0] = cm;

        assertThat(cm.lease("id-1", ROUTE, TIMEOUT, null)).isNotNull();

        assertThat(pools).hasSize(2);
        assertThat(cm.recreationCount()).isEqualTo(1);
        verify(pools.get(1)).lease(any(), eq(ROUTE), any(), any());
    }

    @Test
    public void lease_illegalStateWithLivePool_isNotRetried() {
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(() -> {
            HttpClientConnectionManager pool = mock(HttpClientConnectionManager.class);
            when(pool.lease(any(), any(), any(), any())).thenThrow(new IllegalStateException("something else"));
            created.add(pool);
            return pool;
        });

        // The pool was never destroyed, so this is a real error and must surface rather than triggering a rebuild.
        assertThatThrownBy(() -> cm.lease("id-1", ROUTE, TIMEOUT, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("something else");
        assertThat(created).hasSize(1);
        assertThat(cm.recreationCount()).isZero();
    }

    @Test
    public void lease_afterClosePermanently_isNotRetried() {
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(() -> {
            HttpClientConnectionManager pool = mock(HttpClientConnectionManager.class);
            when(pool.lease(any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("Connection pool shut down"));
            created.add(pool);
            return pool;
        });

        cm.closePermanently();

        // Closing must stay permanent: this is the documented behaviour of using a closed SDK HTTP client.
        assertThatThrownBy(() -> cm.lease("id-1", ROUTE, TIMEOUT, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Connection pool shut down");
        assertThat(created).hasSize(1);
        assertThat(cm.recreationCount()).isZero();
    }

    @Test
    public void lease_rebuiltPoolAlsoFails_propagatesRatherThanLooping() {
        RecreatingHttpClientConnectionManager[] holder = new RecreatingHttpClientConnectionManager[1];
        AtomicInteger factoryCalls = new AtomicInteger();
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(() -> {
            factoryCalls.incrementAndGet();
            HttpClientConnectionManager pool = mock(HttpClientConnectionManager.class);
            // Every pool is destroyed the instant it is used, so the retry cannot succeed either.
            when(pool.lease(any(), any(), any(), any())).thenAnswer(invocation -> {
                holder[0].close(CloseMode.IMMEDIATE);
                throw new IllegalStateException("Connection pool shut down");
            });
            return pool;
        });
        holder[0] = cm;

        assertThatThrownBy(() -> cm.lease("id-1", ROUTE, TIMEOUT, null))
            .isInstanceOf(IllegalStateException.class);

        // Exactly one retry: the initial pool, plus one rebuild. No unbounded rebuild loop.
        assertThat(factoryCalls.get()).isEqualTo(2);
    }

    /**
     * If building a replacement pool fails - a bad TLS configuration, or an OOM that has not abated - the failure must
     * reach the caller and leave this manager willing to try again later, not wedged in a half-built state.
     */
    @Test
    public void lease_factoryFailsDuringRebuild_propagatesAndRemainsRetryable() {
        AtomicInteger factoryCalls = new AtomicInteger();
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(() -> {
            int call = factoryCalls.incrementAndGet();
            if (call == 2) {
                throw new IllegalStateException("cannot build a pool right now");
            }
            HttpClientConnectionManager pool = mock(HttpClientConnectionManager.class);
            when(pool.lease(any(), any(), any(), any())).thenReturn(mock(LeaseRequest.class));
            return pool;
        });

        cm.close(CloseMode.IMMEDIATE);

        assertThatThrownBy(() -> cm.lease("id-1", ROUTE, TIMEOUT, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("cannot build a pool right now");

        // The next request tries again and succeeds, so a transient failure to rebuild is not permanent.
        assertThat(cm.lease("id-2", ROUTE, TIMEOUT, null)).isNotNull();
        assertThat(factoryCalls.get()).isEqualTo(3);
        assertThat(cm.recreationCount()).isEqualTo(1);
    }

    @Test
    public void lease_factoryThrowsError_propagatesAndRemainsRetryable() {
        AtomicInteger factoryCalls = new AtomicInteger();
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(() -> {
            int call = factoryCalls.incrementAndGet();
            if (call == 2) {
                // The heap is still exhausted when we try to rebuild.
                throw new OutOfMemoryError("Java heap space");
            }
            HttpClientConnectionManager pool = mock(HttpClientConnectionManager.class);
            when(pool.lease(any(), any(), any(), any())).thenReturn(mock(LeaseRequest.class));
            return pool;
        });

        cm.close(CloseMode.IMMEDIATE);

        assertThatThrownBy(() -> cm.lease("id-1", ROUTE, TIMEOUT, null)).isInstanceOf(OutOfMemoryError.class);
        assertThat(cm.lease("id-2", ROUTE, TIMEOUT, null)).isNotNull();
        assertThat(factoryCalls.get()).isEqualTo(3);
    }

    /**
     * A request in flight when the pool was replaced still returns its endpoint afterwards, and that endpoint belongs to
     * the pool that was discarded. Apache 5.x {@code StrictConnPool.release} throws for an entry it never leased (4.x
     * ignores it), which would fail a request that merely overlapped someone else's Error with an exception the SDK
     * cannot retry.
     */
    @Test
    public void release_endpointFromReplacedPool_isIgnored() {
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(() -> {
            HttpClientConnectionManager pool = mock(HttpClientConnectionManager.class);
            when(pool.lease(any(), any(), any(), any())).thenReturn(mock(LeaseRequest.class));
            doThrow(new IllegalStateException("Pool entry is not present in the set of leased entries"))
                .when(pool).release(any(), any(), any());
            created.add(pool);
            return pool;
        });

        // Force a replacement so the manager knows a pool has been discarded.
        cm.close(CloseMode.IMMEDIATE);
        cm.lease("id-1", ROUTE, TIMEOUT, null);
        assertThat(cm.recreationCount()).isEqualTo(1);

        cm.release(mock(ConnectionEndpoint.class), null, TimeValue.ZERO_MILLISECONDS);
    }

    @Test
    public void release_foreignEntryBeforeAnyReplacement_propagates() {
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(() -> {
            HttpClientConnectionManager pool = mock(HttpClientConnectionManager.class);
            doThrow(new IllegalStateException("Pool entry is not present in the set of leased entries"))
                .when(pool).release(any(), any(), any());
            created.add(pool);
            return pool;
        });

        // No pool has been replaced, so this cannot be a stale endpoint and must surface as the bug it would be.
        assertThatThrownBy(() -> cm.release(mock(ConnectionEndpoint.class), null, TimeValue.ZERO_MILLISECONDS))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("not present in the set of leased entries");
    }

    @Test
    public void release_unrelatedIllegalState_propagates() {
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(() -> {
            HttpClientConnectionManager pool = mock(HttpClientConnectionManager.class);
            when(pool.lease(any(), any(), any(), any())).thenReturn(mock(LeaseRequest.class));
            doThrow(new IllegalStateException("Endpoint not acquired / already released"))
                .when(pool).release(any(), any(), any());
            created.add(pool);
            return pool;
        });

        cm.close(CloseMode.IMMEDIATE);
        cm.lease("id-1", ROUTE, TIMEOUT, null);

        // Even after a replacement, only the foreign-entry case is ignored.
        assertThatThrownBy(() -> cm.release(mock(ConnectionEndpoint.class), null, TimeValue.ZERO_MILLISECONDS))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Endpoint not acquired");
    }

    /**
     * The reason {@link IdleConnectionCloser} exists: the reaper registers this wrapper once and must keep reaping
     * across pool replacements. Registering the pool itself would leave the reaper holding a discarded pool, and would
     * break deregistration in {@code close()}.
     */
    @Test
    public void reaper_afterRecreation_reapsReplacementPoolNotDiscardedOne() throws Exception {
        List<PoolingHttpClientConnectionManager> pools = new ArrayList<>();
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(() -> {
            PoolingHttpClientConnectionManager pool = mock(PoolingHttpClientConnectionManager.class);
            when(pool.lease(any(), any(), any(), any())).thenReturn(mock(LeaseRequest.class));
            pools.add(pool);
            return pool;
        });

        Map<IdleConnectionCloser, Long> registered = new HashMap<>();
        ExecutorService reaperExecutor = Executors.newSingleThreadExecutor();
        IdleConnectionReaper reaper = new IdleConnectionReaper(registered, () -> reaperExecutor, 50);
        try {
            reaper.registerConnectionManager(cm, 1L);

            // Apache destroys the pool, then the next request builds a replacement.
            cm.close(CloseMode.IMMEDIATE);
            cm.lease("id-1", ROUTE, TIMEOUT, null);
            assertThat(pools).hasSize(2);

            PoolingHttpClientConnectionManager discarded = pools.get(0);
            PoolingHttpClientConnectionManager replacement = pools.get(1);
            verify(discarded, never()).closeIdle(any(TimeValue.class));

            // The reaper still holds the one registration it was given, so reaping now has to reach the replacement.
            Thread.sleep(400);

            verify(replacement, atLeastOnce()).closeIdle(any(TimeValue.class));
            verify(discarded, never()).closeIdle(any(TimeValue.class));
            assertThat(reaper.deregisterConnectionManager(cm)).isTrue();
        } finally {
            reaperExecutor.shutdownNow();
        }
    }

    @Test
    public void closeIdle_afterRecreation_delegatesToReplacementPool() {
        List<PoolingHttpClientConnectionManager> pools = new ArrayList<>();
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(() -> {
            PoolingHttpClientConnectionManager pool = mock(PoolingHttpClientConnectionManager.class);
            when(pool.lease(any(), any(), any(), any())).thenReturn(mock(LeaseRequest.class));
            pools.add(pool);
            return pool;
        });

        cm.close(CloseMode.IMMEDIATE);
        cm.lease("id-1", ROUTE, TIMEOUT, null);
        cm.closeIdle(TimeValue.ofSeconds(5));

        verify(pools.get(1)).closeIdle(TimeValue.ofSeconds(5));
        verify(pools.get(0), never()).closeIdle(any(TimeValue.class));
    }

    @Test
    public void closeIdle_nonPoolingManager_isANoOp() {
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(factory);

        // The mock is not a PoolingHttpClientConnectionManager, so there is nothing to reap and nothing should throw.
        cm.closeIdle(TimeValue.ofSeconds(1));

        assertThat(cm.recreationCount()).isZero();
    }

    @Test
    public void poolStats_nonPoolingManager_returnsNull() {
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(factory);

        assertThat(cm.poolStats()).isNull();
    }

    @Test
    public void poolStats_poolingManager_reportsCurrentPool() {
        AtomicInteger maxTotal = new AtomicInteger(5);
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(() -> {
            PoolingHttpClientConnectionManager pool = new PoolingHttpClientConnectionManager();
            pool.setMaxTotal(maxTotal.getAndIncrement());
            return pool;
        });

        assertThat(cm.poolStats().getMax()).isEqualTo(5);

        cm.close(CloseMode.IMMEDIATE);
        cm.lease("id-1", ROUTE, TIMEOUT, null);

        // Stats follow the replacement pool rather than reporting a dead one.
        assertThat(cm.poolStats().getMax()).isEqualTo(6);
    }

    @Test
    public void concurrentLeasesAfterClose_buildExactlyOneReplacement() throws Exception {
        int threads = 16;
        AtomicInteger factoryCalls = new AtomicInteger();
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(() -> {
            factoryCalls.incrementAndGet();
            HttpClientConnectionManager mockCm = mock(HttpClientConnectionManager.class);
            when(mockCm.lease(any(), any(), any(), any())).thenReturn(mock(LeaseRequest.class));
            return mockCm;
        });

        cm.close(CloseMode.IMMEDIATE);

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            for (int i = 0; i < threads; i++) {
                executor.submit(() -> {
                    try {
                        start.await();
                        cm.lease("id", ROUTE, TIMEOUT, null);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdownNow();
        }

        // One up-front pool plus exactly one replacement, no matter how many threads raced.
        assertThat(factoryCalls.get()).isEqualTo(2);
        assertThat(cm.recreationCount()).isEqualTo(1);
    }

    @Test
    public void closeConcurrentWithLeases_alwaysLeavesAUsablePool() throws Exception {
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(factory);

        List<Throwable> failures = new CopyOnWriteArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            CountDownLatch done = new CountDownLatch(2);
            executor.submit(() -> {
                try {
                    for (int i = 0; i < 200; i++) {
                        cm.close(CloseMode.IMMEDIATE);
                    }
                } catch (Throwable t) {
                    failures.add(t);
                } finally {
                    done.countDown();
                }
            });
            executor.submit(() -> {
                try {
                    for (int i = 0; i < 200; i++) {
                        cm.lease("id", ROUTE, TIMEOUT, null);
                    }
                } catch (Throwable t) {
                    failures.add(t);
                } finally {
                    done.countDown();
                }
            });
            assertThat(done.await(20, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdownNow();
        }

        assertThat(failures).isEmpty();

        // Whatever interleaving occurred, a subsequent lease is served by the pool this manager currently holds, never
        // by one that was discarded.
        assertThat(cm.lease("final", ROUTE, TIMEOUT, null)).isNotNull();
        verify(cm.currentDelegate(), atLeastOnce()).lease(any(), eq(ROUTE), any(), any());
    }

    @Test
    public void close_doesNotLeakTheDiscardedPool() {
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(factory);
        HttpClientConnectionManager original = created.get(0);

        cm.close(CloseMode.IMMEDIATE);
        cm.lease("id-1", ROUTE, TIMEOUT, null);

        // The discarded pool, along with any leases Apache's Error path failed to release, is gone: nothing is
        // forwarded to it any more.
        cm.release(mock(ConnectionEndpoint.class), null, TimeValue.ZERO_MILLISECONDS);
        verify(original, never()).release(any(), any(), any());
        assertThat(cm.currentDelegate()).isNotSameAs(original);
    }

    @AfterEach
    public void shutDownExecutors() {
        executors.forEach(ExecutorService::shutdownNow);
        executors.clear();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Recovery backoff
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    public void close_firstError_nextLeaseRebuildsImmediately() {
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(factory, now::get);
        now.addAndGet(TimeUnit.SECONDS.toNanos(1));

        cm.close(CloseMode.IMMEDIATE);
        cm.lease("id", ROUTE, TIMEOUT, null);

        // The client's construction does not start a window: the first recovery is immediate.
        assertThat(created).hasSize(2);
        assertThat(cm.recreationCount()).isEqualTo(1);
        assertThat(cm.inRecoveryBackoff()).isFalse();
    }

    @Test
    public void close_insideBackoffWindow_entersRecoveryBackoffAndDoesNotRebuild() {
        RecreatingHttpClientConnectionManager cm =
            RecreatingHttpClientConnectionManager.create(this::poolRejectingLeasesAfterClose, now::get);

        cm.close(CloseMode.IMMEDIATE);
        cm.lease("id", ROUTE, TIMEOUT, null);
        now.addAndGet(TimeUnit.MINUTES.toNanos(1));
        cm.close(CloseMode.IMMEDIATE);

        assertThat(cm.inRecoveryBackoff()).isTrue();
        for (int i = 0; i < 3; i++) {
            // The dead pool rejects the lease, and the wrapper does not retry it.
            assertThatThrownBy(() -> cm.lease("id", ROUTE, TIMEOUT, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Connection pool shut down");
        }
        assertThat(created).hasSize(2);
        assertThat(cm.recreationCount()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(longs = {-1L, 0L})
    public void lease_atBackoffBoundary_rebuildsOnlyOnceWindowHasEnded(long offsetFromWindowEnd) {
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(factory, now::get);
        cm.close(CloseMode.IMMEDIATE);
        cm.lease("id", ROUTE, TIMEOUT, null);
        long rebuiltAt = now.get();
        now.addAndGet(TimeUnit.MINUTES.toNanos(1));
        cm.close(CloseMode.IMMEDIATE);

        now.set(rebuiltAt + FIVE_MINUTES + offsetFromWindowEnd);
        cm.lease("id", ROUTE, TIMEOUT, null);

        if (offsetFromWindowEnd < 0) {
            assertThat(created).hasSize(2);
            assertThat(cm.inRecoveryBackoff()).isTrue();
        } else {
            // The boundary is inclusive: at exactly 5 minutes the window has ended.
            assertThat(created).hasSize(3);
            assertThat(cm.inRecoveryBackoff()).isFalse();
            assertThat(cm.recreationCount()).isEqualTo(2);
        }
    }

    @Test
    public void lease_afterBackoffWindow_startsNewWindow() {
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(factory, now::get);
        cm.close(CloseMode.IMMEDIATE);
        cm.lease("id", ROUTE, TIMEOUT, null);
        long firstRebuild = now.get();
        now.addAndGet(TimeUnit.MINUTES.toNanos(1));
        cm.close(CloseMode.IMMEDIATE);

        now.set(firstRebuild + FIVE_MINUTES);
        cm.lease("id", ROUTE, TIMEOUT, null);
        now.addAndGet(TimeUnit.SECONDS.toNanos(1));
        cm.close(CloseMode.IMMEDIATE);

        assertThat(created).hasSize(3);
        assertThat(cm.inRecoveryBackoff()).isTrue();
    }

    @Test
    public void close_duringBackoff_doesNotExtendWindow() {
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(factory, now::get);
        cm.close(CloseMode.IMMEDIATE);
        cm.lease("id", ROUTE, TIMEOUT, null);
        long rebuiltAt = now.get();
        HttpClientConnectionManager rebuilt = created.get(1);

        now.set(rebuiltAt + TimeUnit.MINUTES.toNanos(1));
        cm.close(CloseMode.IMMEDIATE);
        now.set(rebuiltAt + TimeUnit.MINUTES.toNanos(4));
        cm.close(CloseMode.IMMEDIATE);
        now.set(rebuiltAt + FIVE_MINUTES);
        cm.lease("id", ROUTE, TIMEOUT, null);

        // The pool was already down, so the second Error was a no-op, and the window still ended 5 minutes after the
        // rebuild.
        verify(rebuilt, times(1)).close(CloseMode.IMMEDIATE);
        assertThat(created).hasSize(3);
        assertThat(cm.inRecoveryBackoff()).isFalse();
    }

    @Test
    public void close_afterWindowWithoutLease_rebuildsImmediately() {
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(factory, now::get);
        cm.close(CloseMode.IMMEDIATE);
        cm.lease("id", ROUTE, TIMEOUT, null);

        now.addAndGet(TimeUnit.MINUTES.toNanos(6));
        cm.close(CloseMode.IMMEDIATE);

        assertThat(cm.inRecoveryBackoff()).isFalse();
        cm.lease("id", ROUTE, TIMEOUT, null);
        assertThat(created).hasSize(3);
        assertThat(cm.recreationCount()).isEqualTo(2);
    }

    @Test
    public void lease_factoryFailsDuringRebuild_doesNotStartWindow() {
        AtomicInteger factoryCalls = new AtomicInteger();
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(() -> {
            if (factoryCalls.incrementAndGet() == 2) {
                throw new IllegalStateException("cannot build a pool right now");
            }
            return factory.get();
        }, now::get);
        long start = now.get();

        cm.close(CloseMode.IMMEDIATE);
        assertThatThrownBy(() -> cm.lease("id", ROUTE, TIMEOUT, null)).hasMessage("cannot build a pool right now");
        now.set(start + TimeUnit.MINUTES.toNanos(10));
        cm.lease("id", ROUTE, TIMEOUT, null);
        now.set(start + TimeUnit.MINUTES.toNanos(14));
        cm.close(CloseMode.IMMEDIATE);

        // The window started at the successful rebuild (10 minutes), not at the failed attempt (0 minutes).
        assertThat(cm.inRecoveryBackoff()).isTrue();
    }

    @Test
    public void lease_factoryFailsAfterBackoffWindow_nextLeaseStillRebuilds() {
        AtomicInteger factoryCalls = new AtomicInteger();
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(() -> {
            if (factoryCalls.incrementAndGet() == 3) {
                throw new IllegalStateException("cannot build a pool right now");
            }
            return factory.get();
        }, now::get);
        cm.close(CloseMode.IMMEDIATE);
        cm.lease("id", ROUTE, TIMEOUT, null);
        long rebuiltAt = now.get();
        now.set(rebuiltAt + TimeUnit.MINUTES.toNanos(1));
        cm.close(CloseMode.IMMEDIATE);

        now.set(rebuiltAt + FIVE_MINUTES);
        assertThatThrownBy(() -> cm.lease("id", ROUTE, TIMEOUT, null)).hasMessage("cannot build a pool right now");
        now.set(rebuiltAt + FIVE_MINUTES + TimeUnit.SECONDS.toNanos(1));
        cm.lease("id", ROUTE, TIMEOUT, null);

        // The failed attempt did not start a new window, so the next lease rebuilt the pool.
        assertThat(factoryCalls.get()).isEqualTo(4);
        assertThat(cm.inRecoveryBackoff()).isFalse();
        assertThat(cm.recreationCount()).isEqualTo(2);
    }

    @Test
    public void lease_inRecoveryBackoff_cleansUpAbandonedPoolOnce() throws Exception {
        SafeStrictConnPool pool = SafePoolingHttpClientConnectionManagerTest.newPool(new FailingDisposal(1), 10);
        List<ManagedHttpClientConnection> idle = idleEntries(pool, ROUTE, ROUTE, OTHER_ROUTE);
        SafePoolingHttpClientConnectionManager rebuiltPool = newManager(pool);
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(() -> {
            if (created.size() == 1) {
                created.add(rebuiltPool);
                return rebuiltPool;
            }
            return factory.get();
        }, now::get);
        cm.close(CloseMode.IMMEDIATE);
        // A route with no idle entries, so this lease does not take one of the connections under test.
        cm.lease("id", LEASE_ROUTE, TIMEOUT, null);
        now.addAndGet(TimeUnit.MINUTES.toNanos(1));
        cm.close(CloseMode.IMMEDIATE);
        assertThat(cm.inRecoveryBackoff()).isTrue();
        assertThat(closedCount(idle)).isEqualTo(1);

        // Both leases fail fast on the dead pool; the first one reclaims what its interrupted close left open.
        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(() -> cm.lease("id", ROUTE, TIMEOUT, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Connection pool shut down");
        }

        assertEachClosedOnce(idle);
        assertThat(created).hasSize(2);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Attribution of a close to the pool the failing request used
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    public void lease_recordsPoolBeforeDelegateLease() {
        ExecutorService threadA = newExecutor();
        ExecutorService threadC = newExecutor();
        RecreatingHttpClientConnectionManager[] holder = new RecreatingHttpClientConnectionManager[1];
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(() -> {
            HttpClientConnectionManager pool = mock(HttpClientConnectionManager.class);
            if (created.isEmpty()) {
                // While P1 is leasing on the test thread: A destroys P1, C rebuilds P2, then the test thread's own
                // request asks for a close. It used P1, so that must be ignored.
                when(pool.lease(any(), any(), any(), any())).thenAnswer(invocation -> {
                    runOn(threadA, () -> holder[0].close(CloseMode.IMMEDIATE));
                    runOn(threadC, () -> holder[0].lease("c", ROUTE, TIMEOUT, null));
                    holder[0].close(CloseMode.IMMEDIATE);
                    return mock(LeaseRequest.class);
                });
            } else {
                when(pool.lease(any(), any(), any(), any())).thenReturn(mock(LeaseRequest.class));
            }
            created.add(pool);
            return pool;
        }, now::get);
        holder[0] = cm;

        cm.lease("test", ROUTE, TIMEOUT, null);

        verify(created.get(1), never()).close(any(CloseMode.class));
        assertThat(cm.inRecoveryBackoff()).isFalse();
        assertThat(cm.recreationCount()).isEqualTo(1);
    }

    @Test
    public void close_fromRequestOnReplacedPool_isIgnored() {
        ExecutorService threadA = newExecutor();
        ExecutorService threadB = newExecutor();
        ExecutorService threadC = newExecutor();
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(factory, now::get);

        runOn(threadB, () -> cm.lease("b", ROUTE, TIMEOUT, null));
        runOn(threadA, () -> {
            cm.lease("a", ROUTE, TIMEOUT, null);
            cm.close(CloseMode.IMMEDIATE);
        });
        runOn(threadC, () -> cm.lease("c", ROUTE, TIMEOUT, null));
        runOn(threadB, () -> cm.close(CloseMode.IMMEDIATE));

        HttpClientConnectionManager replacement = created.get(1);
        verify(replacement, never()).close(any(CloseMode.class));
        assertThat(cm.inRecoveryBackoff()).isFalse();
        assertThat(created).hasSize(2);
        assertThat(cm.recreationCount()).isEqualTo(1);
        cm.lease("d", ROUTE, TIMEOUT, null);
        verify(replacement, times(2)).lease(any(), eq(ROUTE), any(), any());
    }

    @Test
    public void close_fromRequestOnCurrentPool_isHonoured() {
        ExecutorService threadA = newExecutor();
        ExecutorService threadC = newExecutor();
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(factory, now::get);

        runOn(threadA, () -> {
            cm.lease("a", ROUTE, TIMEOUT, null);
            cm.close(CloseMode.IMMEDIATE);
        });
        runOn(threadC, () -> {
            cm.lease("c", ROUTE, TIMEOUT, null);
            cm.close(CloseMode.IMMEDIATE);
        });

        // C leased from the current pool, so its Error destroys that pool, inside the window.
        verify(created.get(1)).close(CloseMode.IMMEDIATE);
        assertThat(cm.inRecoveryBackoff()).isTrue();
    }

    @Test
    public void close_fromThreadWithoutLease_closesCurrentPool() {
        ExecutorService threadA = newExecutor();
        ExecutorService threadC = newExecutor();
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(factory, now::get);

        runOn(threadA, () -> cm.close(CloseMode.IMMEDIATE));
        runOn(threadC, () -> cm.lease("c", ROUTE, TIMEOUT, null));
        // The test thread never leased, so there is nothing to attribute the close to: it falls back to destroying the
        // current pool.
        cm.close(CloseMode.IMMEDIATE);

        verify(created.get(1)).close(CloseMode.IMMEDIATE);
        assertThat(cm.inRecoveryBackoff()).isTrue();
    }

    @Test
    public void clearCurrentThreadLease_removesRecord() {
        ExecutorService threadA = newExecutor();
        ExecutorService threadB = newExecutor();
        ExecutorService threadC = newExecutor();
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(factory, now::get);

        runOn(threadB, () -> {
            cm.lease("b", ROUTE, TIMEOUT, null);
            cm.clearCurrentThreadLease();
        });
        runOn(threadA, () -> cm.close(CloseMode.IMMEDIATE));
        runOn(threadC, () -> cm.lease("c", ROUTE, TIMEOUT, null));
        runOn(threadB, () -> cm.close(CloseMode.IMMEDIATE));

        // Without B's stale record, its close is unattributed and destroys the rebuilt pool.
        verify(created.get(1)).close(CloseMode.IMMEDIATE);
        assertThat(cm.inRecoveryBackoff()).isTrue();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // closePermanently
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    public void closePermanently_fromRecoveryBackoff_movesToClosedAndNeverRebuilds() {
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(factory, now::get);
        cm.close(CloseMode.IMMEDIATE);
        cm.lease("id", ROUTE, TIMEOUT, null);
        now.addAndGet(TimeUnit.MINUTES.toNanos(1));
        cm.close(CloseMode.IMMEDIATE);
        HttpClientConnectionManager dead = created.get(1);

        cm.closePermanently();
        now.addAndGet(TimeUnit.MINUTES.toNanos(10));
        cm.lease("id", ROUTE, TIMEOUT, null);
        cm.close(CloseMode.IMMEDIATE);

        assertThat(created).hasSize(2);
        assertThat(cm.recreationCount()).isEqualTo(1);
        assertThat(cm.inRecoveryBackoff()).isFalse();
        // Once by the Error path and once by closePermanently(); the later close is a no-op.
        verify(dead, times(2)).close(CloseMode.IMMEDIATE);
    }

    @Test
    public void closePermanently_withAbandonedPool_cleansItUp() throws Exception {
        SafeStrictConnPool pool = SafePoolingHttpClientConnectionManagerTest.newPool(new FailingDisposal(1), 10);
        List<ManagedHttpClientConnection> idle = idleEntries(pool, ROUTE, ROUTE, OTHER_ROUTE);
        RecreatingHttpClientConnectionManager cm = createWithFirstPool(newManager(pool));
        cm.close(CloseMode.IMMEDIATE);
        assertThat(closedCount(idle)).isEqualTo(1);

        cm.closePermanently();

        assertEachClosedOnce(idle);
        assertThat(created).isEmpty();
    }

    @Test
    public void close_noArgument_closesDelegateWithNoArgumentClose() throws Exception {
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(factory, now::get);
        cm.close();
        verify(created.get(0), times(1)).close();
        verify(created.get(0), never()).close(any(CloseMode.class));

        cm.lease("id", ROUTE, TIMEOUT, null);
        now.addAndGet(FIVE_MINUTES);
        cm.close(CloseMode.IMMEDIATE);
        verify(created.get(1), times(1)).close(CloseMode.IMMEDIATE);
        verify(created.get(1), never()).close();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Logging
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    public void close_whileNeedsRecreateOrInBackoff_isNoOp() {
        try (LogCaptor logCaptor = LogCaptor.create(Level.DEBUG)) {
            RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(factory, now::get);
            cm.close(CloseMode.IMMEDIATE);
            logCaptor.clear();
            cm.close(CloseMode.IMMEDIATE);
            verify(created.get(0), times(1)).close(CloseMode.IMMEDIATE);
            assertThat(wrapperEvents(logCaptor)).isEmpty();

            cm.lease("id", ROUTE, TIMEOUT, null);
            now.addAndGet(TimeUnit.MINUTES.toNanos(1));
            cm.close(CloseMode.IMMEDIATE);
            logCaptor.clear();
            cm.close(CloseMode.IMMEDIATE);
            cm.close(CloseMode.IMMEDIATE);
            verify(created.get(1), times(1)).close(CloseMode.IMMEDIATE);
            assertThat(wrapperEvents(logCaptor)).isEmpty();
        }
    }

    enum CloseCase {
        NEEDS_RECREATE,
        NEEDS_RECREATE_NO_ARGUMENT_CLOSE,
        BACKOFF_ENTRY,
        FAILING_CLOSE
    }

    /**
     * The Error path runs right after an Error, often an OutOfMemoryError, so the log calls (which allocate) must come
     * after the pool close: a failure while logging may then replace Apache's rethrow, but never skip the close.
     */
    /** Every close case with the {@code WITH_BACKOFF} window, and every one that can happen with a zero window. */
    private static Stream<Arguments> closeCasesAndWindows() {
        return Stream.of(CloseCase.values())
                     .flatMap(closeCase -> Stream.of(Duration.ofMinutes(5), Duration.ZERO)
                                                 .filter(window -> !(closeCase == CloseCase.BACKOFF_ENTRY
                                                                     && window.isZero()))
                                                 .map(window -> Arguments.of(closeCase, window)));
    }

    @ParameterizedTest(name = "{0}, window {1}")
    @MethodSource("closeCasesAndWindows")
    public void close_logsOnlyAfterPoolClose(CloseCase closeCase, Duration window) throws Exception {
        try (LogCaptor logCaptor = LogCaptor.create(Level.DEBUG)) {
            AtomicInteger eventsBeforePoolClose = new AtomicInteger(-1);
            Runnable recordEvents = () -> eventsBeforePoolClose.compareAndSet(-1, wrapperEvents(logCaptor).size());
            List<ManagedHttpClientConnection> idle = new ArrayList<>();
            HttpClientConnectionManager recordingPool;
            if (closeCase == CloseCase.FAILING_CLOSE) {
                FailingDisposal disposal = new FailingDisposal(1).beforeFailure(recordEvents);
                SafeStrictConnPool pool = SafePoolingHttpClientConnectionManagerTest.newPool(disposal, 10);
                idle.addAll(idleEntries(pool, ROUTE, ROUTE, OTHER_ROUTE));
                recordingPool = newManager(pool);
            } else {
                recordingPool = mock(HttpClientConnectionManager.class);
                when(recordingPool.lease(any(), any(), any(), any())).thenReturn(mock(LeaseRequest.class));
                doAnswer(invocation -> {
                    recordEvents.run();
                    return null;
                }).when(recordingPool).close(any(CloseMode.class));
                doAnswer(invocation -> {
                    recordEvents.run();
                    return null;
                }).when(recordingPool).close();
            }
            // For the backoff entry the recording pool is the rebuilt one; otherwise it is the first.
            boolean recordingIsRebuilt = closeCase == CloseCase.BACKOFF_ENTRY;
            int recordingPoolNumber = recordingIsRebuilt ? 1 : 0;
            HttpClientConnectionManager finalRecordingPool = recordingPool;
            AtomicInteger poolNumber = new AtomicInteger();
            RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(
                () -> poolNumber.getAndIncrement() == recordingPoolNumber ? finalRecordingPool : factory.get(),
                now::get,
                window);
            if (recordingIsRebuilt) {
                cm.close(CloseMode.IMMEDIATE);
                cm.lease("id", ROUTE, TIMEOUT, null);
                now.addAndGet(TimeUnit.MINUTES.toNanos(1));
            }
            assertThat(cm.currentDelegate()).isSameAs(recordingPool);

            logCaptor.clear();
            if (closeCase == CloseCase.NEEDS_RECREATE_NO_ARGUMENT_CLOSE) {
                cm.close();
            } else {
                cm.close(CloseMode.IMMEDIATE);
            }

            assertThat(eventsBeforePoolClose.get()).isZero();
            List<LogEvent> events = wrapperEvents(logCaptor);
            switch (closeCase) {
                case NEEDS_RECREATE:
                case NEEDS_RECREATE_NO_ARGUMENT_CLOSE:
                    assertThat(events).hasSize(1);
                    assertThat(events.get(0).getLevel()).isEqualTo(Level.DEBUG);
                    break;
                case BACKOFF_ENTRY:
                    assertThat(events).hasSize(1);
                    assertThat(events.get(0).getLevel()).isEqualTo(Level.WARN);
                    assertThat(events.get(0).getMessage().getFormattedMessage()).contains("secondsUntilRecovery=");
                    break;
                case FAILING_CLOSE:
                    assertThat(events).hasSize(2);
                    assertThat(events.get(0).getLevel()).isEqualTo(Level.WARN);
                    assertThat(events.get(0).getMessage().getFormattedMessage()).contains("did not complete");
                    assertThat(events.get(1).getLevel()).isEqualTo(Level.DEBUG);

                    // abandonedPool was set: the next lease reclaims the pool's remaining connections, exactly once.
                    cm.lease("id", ROUTE, TIMEOUT, null);
                    cm.lease("id", ROUTE, TIMEOUT, null);
                    assertEachClosedOnce(idle);
                    break;
                default:
                    throw new IllegalStateException("Unexpected case " + closeCase);
            }
        }
    }

    @Test
    public void rebuild_logsOneWarnPerRecovery() {
        try (LogCaptor logCaptor = LogCaptor.create(Level.DEBUG)) {
            RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(factory, now::get);

            cm.close(CloseMode.IMMEDIATE);
            cm.lease("id", ROUTE, TIMEOUT, null);
            now.addAndGet(FIVE_MINUTES);
            cm.close(CloseMode.IMMEDIATE);
            cm.lease("id", ROUTE, TIMEOUT, null);

            List<String> warnings = wrapperMessages(logCaptor, Level.WARN);
            assertThat(warnings).hasSize(2);
            assertThat(warnings.get(0)).contains("Rebuilt the Apache HTTP client's connection pool")
                                       .contains("recoveryNumber=1");
            assertThat(warnings.get(1)).contains("Rebuilt the Apache HTTP client's connection pool")
                                       .contains("recoveryNumber=2");
            assertThat(wrapperMessages(logCaptor, Level.DEBUG))
                .filteredOn(message -> message.contains("will be rebuilt on the next request"))
                .hasSize(2);
        }
    }

    @Test
    public void close_enteringBackoff_logsOneWarn_notPerLease() {
        try (LogCaptor logCaptor = LogCaptor.create(Level.DEBUG)) {
            RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(factory, now::get);
            cm.close(CloseMode.IMMEDIATE);
            cm.lease("id", ROUTE, TIMEOUT, null);
            now.addAndGet(TimeUnit.MINUTES.toNanos(1));
            cm.close(CloseMode.IMMEDIATE);

            for (int i = 0; i < 10; i++) {
                cm.lease("id", ROUTE, TIMEOUT, null);
            }
            cm.close(CloseMode.IMMEDIATE);
            cm.close(CloseMode.IMMEDIATE);

            List<String> backoffWarnings = wrapperMessages(logCaptor, Level.WARN)
                .stream()
                .filter(message -> message.contains("will not be rebuilt until"))
                .collect(Collectors.toList());
            assertThat(backoffWarnings).hasSize(1);
            assertThat(backoffWarnings.get(0)).contains("secondsUntilRecovery=240").contains("recoveryNumber=1");
            assertThat(wrapperMessages(logCaptor, Level.WARN)).hasSize(2);
        }
    }

    @Test
    public void concurrentClosesAndLeases_neverRebuildMoreThanOncePerWindow() throws Exception {
        RecreatingHttpClientConnectionManager cm =
            RecreatingHttpClientConnectionManager.create(this::poolRejectingLeasesAfterClose, now::get);
        cm.close(CloseMode.IMMEDIATE);
        cm.lease("id", ROUTE, TIMEOUT, null);

        List<Throwable> failures = new CopyOnWriteArrayList<>();
        ExecutorService executor = newExecutor(4);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(4);
        for (int t = 0; t < 4; t++) {
            boolean closes = t % 2 == 0;
            executor.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < 200; i++) {
                        try {
                            if (closes) {
                                cm.close(CloseMode.IMMEDIATE);
                            } else {
                                cm.lease("id", ROUTE, TIMEOUT, null);
                            }
                        } catch (IllegalStateException e) {
                            if (!"Connection pool shut down".equals(e.getMessage())) {
                                failures.add(e);
                            }
                        }
                    }
                } catch (Throwable t2) {
                    failures.add(t2);
                } finally {
                    done.countDown();
                }
                return null;
            });
        }
        start.countDown();
        assertThat(done.await(20, TimeUnit.SECONDS)).isTrue();

        assertThat(failures).isEmpty();
        // The initial pool and the one recovery: every later close fell inside the frozen window.
        assertThat(created).hasSize(2);
        assertThat(cm.recreationCount()).isEqualTo(1);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Zero backoff window (ALWAYS)
    // ---------------------------------------------------------------------------------------------------------------

    private static Stream<Arguments> invalidCreateArguments() {
        LongSupplier clock = () -> 0L;
        return Stream.of(Arguments.of("null recoveryBackoff", clock, null, IllegalArgumentException.class),
                         Arguments.of("negative recoveryBackoff", clock, Duration.ofNanos(-1),
                                      IllegalArgumentException.class),
                         Arguments.of("null nanoClock", null, Duration.ZERO, NullPointerException.class));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidCreateArguments")
    public void create_invalidArguments_throwsWithoutBuildingPool(String name, LongSupplier nanoClock,
                                                                  Duration recoveryBackoff,
                                                                  Class<? extends Throwable> expected) {
        AtomicInteger factoryCalls = new AtomicInteger();
        Supplier<HttpClientConnectionManager> countingFactory = () -> {
            factoryCalls.incrementAndGet();
            return factory.get();
        };

        assertThatThrownBy(() -> RecreatingHttpClientConnectionManager.create(countingFactory, nanoClock, recoveryBackoff))
            .isExactlyInstanceOf(expected);
        assertThat(factoryCalls).hasValue(0);
    }

    @Test
    public void close_zeroBackoff_repeatedErrors_eachNextLeaseRebuilds() {
        RecreatingHttpClientConnectionManager cm =
            RecreatingHttpClientConnectionManager.create(factory, now::get, Duration.ZERO);

        for (int i = 0; i < 3; i++) {
            cm.close(CloseMode.IMMEDIATE);
            assertThat(cm.inRecoveryBackoff()).isFalse();
            cm.lease("id", ROUTE, TIMEOUT, null);
        }

        assertThat(created).hasSize(4);
        assertThat(cm.recreationCount()).isEqualTo(3);
    }

    @Test
    public void close_zeroBackoff_clockBehindLastRebuild_neverEntersBackoff() {
        RecreatingHttpClientConnectionManager cm =
            RecreatingHttpClientConnectionManager.create(factory, now::get, Duration.ZERO);
        cm.close(CloseMode.IMMEDIATE);
        cm.lease("id", ROUTE, TIMEOUT, null);

        // A clock that reads earlier than the last rebuild must not put a zero window into backoff.
        now.decrementAndGet();
        cm.close(CloseMode.IMMEDIATE);

        assertThat(cm.inRecoveryBackoff()).isFalse();
        cm.lease("id", ROUTE, TIMEOUT, null);
        assertThat(cm.recreationCount()).isEqualTo(2);
    }

    @Test
    public void close_zeroBackoff_fromRequestOnReplacedPool_isIgnored() {
        ExecutorService threadA = newExecutor();
        ExecutorService threadB = newExecutor();
        ExecutorService threadC = newExecutor();
        RecreatingHttpClientConnectionManager cm =
            RecreatingHttpClientConnectionManager.create(factory, now::get, Duration.ZERO);

        runOn(threadB, () -> cm.lease("b", ROUTE, TIMEOUT, null));
        runOn(threadA, () -> {
            cm.lease("a", ROUTE, TIMEOUT, null);
            cm.close(CloseMode.IMMEDIATE);
        });
        runOn(threadC, () -> cm.lease("c", ROUTE, TIMEOUT, null));
        runOn(threadB, () -> cm.close(CloseMode.IMMEDIATE));

        HttpClientConnectionManager replacement = created.get(1);
        verify(replacement, never()).close(any(CloseMode.class));
        assertThat(created).hasSize(2);
        assertThat(cm.recreationCount()).isEqualTo(1);
        cm.lease("d", ROUTE, TIMEOUT, null);
        verify(replacement, times(2)).lease(any(), eq(ROUTE), any(), any());
    }

    @Test
    public void lease_zeroBackoff_afterFailedClose_cleansUpAbandonedPoolBeforeRebuild() throws Exception {
        SafeStrictConnPool pool = SafePoolingHttpClientConnectionManagerTest.newPool(new FailingDisposal(1), 10);
        List<ManagedHttpClientConnection> idle = idleEntries(pool, ROUTE, ROUTE, OTHER_ROUTE);
        HttpClientConnectionManager firstPool = newManager(pool);
        AtomicInteger factoryCalls = new AtomicInteger();
        AtomicBoolean cleanedUpBeforeRebuild = new AtomicBoolean();
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(() -> {
            if (factoryCalls.getAndIncrement() == 0) {
                return firstPool;
            }
            cleanedUpBeforeRebuild.set(closedCount(idle) == idle.size());
            return factory.get();
        }, now::get, Duration.ZERO);

        // The first disposal throws, so the pool is abandoned part way through its close.
        cm.close(CloseMode.IMMEDIATE);
        cm.lease("id", LEASE_ROUTE, TIMEOUT, null);

        assertThat(cleanedUpBeforeRebuild).isTrue();
        assertEachClosedOnce(idle);
        assertThat(cm.recreationCount()).isEqualTo(1);
        assertThat(cm.inRecoveryBackoff()).isFalse();
    }

    @Test
    public void lease_zeroBackoff_logsRecoveryWarningAtMostOncePerFiveMinutes() {
        try (LogCaptor logCaptor = LogCaptor.create(Level.DEBUG)) {
            long start = now.get();
            RecreatingHttpClientConnectionManager cm =
                RecreatingHttpClientConnectionManager.create(factory, now::get, Duration.ZERO);
            long[] rebuildTimes = {start, start, start, start + FIVE_MINUTES - 1, start + FIVE_MINUTES,
                                   start + TimeUnit.MINUTES.toNanos(20)};

            for (long rebuildTime : rebuildTimes) {
                now.set(rebuildTime);
                cm.close(CloseMode.IMMEDIATE);
                cm.lease("id", ROUTE, TIMEOUT, null);
            }

            List<String> warnings = wrapperMessages(logCaptor, Level.WARN);
            assertThat(warnings).hasSize(3);
            assertThat(warnings.get(0)).contains("[recoveryNumber=1, rebuildsSincePreviousWarning=1]");
            assertThat(warnings.get(1)).contains("[recoveryNumber=5, rebuildsSincePreviousWarning=4]");
            assertThat(warnings.get(2)).contains("[recoveryNumber=6, rebuildsSincePreviousWarning=1]");
            assertThat(warnings).allMatch(m -> m.contains("The connection pool recovery mode is ALWAYS"));
            assertThat(wrapperMessages(logCaptor, Level.DEBUG))
                .filteredOn(m -> m.startsWith("Rebuilt the Apache HTTP client's connection pool after"))
                .hasSize(3)
                .anyMatch(m -> m.contains("[recoveryNumber=2]"))
                .anyMatch(m -> m.contains("[recoveryNumber=3]"))
                .anyMatch(m -> m.contains("[recoveryNumber=4]"));
        }
    }

    @Test
    public void close_zeroBackoff_neverLogsBackoffWarning() {
        try (LogCaptor logCaptor = LogCaptor.create(Level.DEBUG)) {
            RecreatingHttpClientConnectionManager cm =
                RecreatingHttpClientConnectionManager.create(factory, now::get, Duration.ZERO);

            for (int i = 0; i < 3; i++) {
                cm.close(CloseMode.IMMEDIATE);
                cm.lease("id", ROUTE, TIMEOUT, null);
            }
            cm.close(CloseMode.IMMEDIATE);

            assertThat(wrapperEvents(logCaptor))
                .noneMatch(e -> e.getMessage().getFormattedMessage().contains("will not be rebuilt"));
        }
    }

    @Test
    public void lease_withBackoff_recoveryWarningHasNoRebuildCount() {
        try (LogCaptor logCaptor = LogCaptor.create(Level.DEBUG)) {
            RecreatingHttpClientConnectionManager cm =
                RecreatingHttpClientConnectionManager.create(factory, now::get, Duration.ofMinutes(5));

            cm.close(CloseMode.IMMEDIATE);
            cm.lease("id", ROUTE, TIMEOUT, null);

            assertThat(wrapperMessages(logCaptor, Level.WARN)).containsExactly(
                "Rebuilt the Apache HTTP client's connection pool, which the client closed because a java.lang.Error "
                + "(commonly an OutOfMemoryError or StackOverflowError) escaped a request [recoveryNumber=1]. Requests "
                + "in flight when the pool was closed have failed; check application logs for the underlying Error. If "
                + "an Error closes the pool again within 5 minutes of this rebuild, the pool is not rebuilt until those 5 "
                + "minutes have passed.");
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------------------------

    /** A wrapper whose first pool is the given one; later pools come from {@link #factory}. */
    private RecreatingHttpClientConnectionManager createWithFirstPool(HttpClientConnectionManager firstPool) {
        AtomicBoolean firstHandedOut = new AtomicBoolean();
        return RecreatingHttpClientConnectionManager.create(
            () -> firstHandedOut.getAndSet(true) ? factory.get() : firstPool, now::get);
    }

    /** A mock pool that, like Apache's, rejects leases once it has been closed. */
    private HttpClientConnectionManager poolRejectingLeasesAfterClose() {
        HttpClientConnectionManager pool = mock(HttpClientConnectionManager.class);
        AtomicBoolean closed = new AtomicBoolean();
        doAnswer(invocation -> {
            closed.set(true);
            return null;
        }).when(pool).close(any(CloseMode.class));
        when(pool.lease(any(), any(), any(), any())).thenAnswer(invocation -> {
            if (closed.get()) {
                throw new IllegalStateException("Connection pool shut down");
            }
            return mock(LeaseRequest.class);
        });
        created.add(pool);
        return pool;
    }

    private static long closedCount(List<ManagedHttpClientConnection> connections) {
        return connections.stream().filter(c -> closeCalls(c) > 0).count();
    }

    private static void assertEachClosedOnce(List<ManagedHttpClientConnection> connections) {
        assertThat(connections).isNotEmpty();
        for (ManagedHttpClientConnection connection : connections) {
            assertThat(closeCalls(connection)).as("close calls on %s", connection).isEqualTo(1);
        }
    }

    private static long closeCalls(ManagedHttpClientConnection connection) {
        return mockingDetails(connection).getInvocations()
                                         .stream()
                                         .filter(i -> "close".equals(i.getMethod().getName()))
                                         .count();
    }

    private ExecutorService newExecutor() {
        return newExecutor(1);
    }

    private ExecutorService newExecutor(int threads) {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        executors.add(executor);
        return executor;
    }

    /** Runs the action on the given thread and waits for it, so that the order of steps across threads is fixed. */
    private static void runOn(ExecutorService thread, Runnable action) {
        Callable<Void> callable = () -> {
            action.run();
            return null;
        };
        try {
            thread.submit(callable).get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<LogEvent> wrapperEvents(LogCaptor logCaptor) {
        return logCaptor.loggedEvents()
                        .stream()
                        .filter(event -> WRAPPER_LOGGER.equals(event.getLoggerName()))
                        .collect(Collectors.toList());
    }

    private static List<String> wrapperMessages(LogCaptor logCaptor, Level level) {
        return wrapperEvents(logCaptor).stream()
                                       .filter(event -> event.getLevel() == level)
                                       .map(event -> event.getMessage().getFormattedMessage())
                                       .collect(Collectors.toList());
    }
}
