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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.io.ConnectionEndpoint;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.io.LeaseRequest;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link RecreatingHttpClientConnectionManager}, which keeps an Apache HTTP client usable after Apache closes
 * its connection pool in response to a {@link Error}.
 */
public class RecreatingHttpClientConnectionManagerTest {

    private static final HttpRoute ROUTE = new HttpRoute(new HttpHost("localhost", 8080));
    private static final Timeout TIMEOUT = Timeout.ofSeconds(1);

    private List<HttpClientConnectionManager> created;
    private Supplier<HttpClientConnectionManager> factory;

    @BeforeEach
    public void setup() {
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
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(factory);

        for (int i = 0; i < 3; i++) {
            cm.close(CloseMode.IMMEDIATE);
            cm.lease("id-" + i, ROUTE, TIMEOUT, null);
        }

        assertThat(created).hasSize(4);
        assertThat(cm.recreationCount()).isEqualTo(3);
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
            org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager pool =
                new org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager();
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
}
