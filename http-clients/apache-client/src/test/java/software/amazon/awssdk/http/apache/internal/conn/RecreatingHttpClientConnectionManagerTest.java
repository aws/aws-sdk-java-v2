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

package software.amazon.awssdk.http.apache.internal.conn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
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
import org.apache.http.conn.ConnectionRequest;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.routing.HttpRoute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link RecreatingHttpClientConnectionManager}, which keeps an Apache HTTP client usable after Apache shuts
 * its connection pool down in response to a {@link Error}.
 */
public class RecreatingHttpClientConnectionManagerTest {

    private static final HttpRoute ROUTE = new HttpRoute(new org.apache.http.HttpHost("localhost", 8080));

    private List<HttpClientConnectionManager> created;
    private Supplier<HttpClientConnectionManager> factory;

    @BeforeEach
    public void setup() {
        created = new ArrayList<>();
        factory = () -> {
            HttpClientConnectionManager cm = mock(HttpClientConnectionManager.class);
            when(cm.requestConnection(any(), any())).thenReturn(mock(ConnectionRequest.class));
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
    public void requestConnection_noShutdown_reusesSamePool() {
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(factory);

        cm.requestConnection(ROUTE, null);
        cm.requestConnection(ROUTE, null);

        assertThat(created).hasSize(1);
        assertThat(cm.recreationCount()).isZero();
        verify(created.get(0), times(2)).requestConnection(ROUTE, null);
    }

    @Test
    public void shutdown_shutsDownCurrentPoolButDoesNotRebuildEagerly() {
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(factory);

        cm.shutdown();

        verify(created.get(0)).shutdown();
        // The replacement is built lazily, so a client that is shut down and never used again costs nothing.
        assertThat(created).hasSize(1);
        assertThat(cm.recreationCount()).isZero();
    }

    @Test
    public void requestConnection_afterShutdown_buildsReplacementPool() {
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(factory);
        HttpClientConnectionManager original = created.get(0);

        cm.shutdown();
        cm.requestConnection(ROUTE, null);

        assertThat(created).hasSize(2);
        HttpClientConnectionManager replacement = created.get(1);
        assertThat(cm.currentDelegate()).isSameAs(replacement);
        assertThat(cm.recreationCount()).isEqualTo(1);
        verify(replacement).requestConnection(ROUTE, null);
        verify(original, never()).requestConnection(any(), any());
    }

    @Test
    public void requestConnection_afterShutdown_onlyBuildsOneReplacement() {
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(factory);

        cm.shutdown();
        cm.requestConnection(ROUTE, null);
        cm.requestConnection(ROUTE, null);

        assertThat(created).hasSize(2);
        assertThat(cm.recreationCount()).isEqualTo(1);
    }

    @Test
    public void repeatedShutdowns_eachRebuildOnce() {
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(factory);

        for (int i = 0; i < 3; i++) {
            cm.shutdown();
            cm.requestConnection(ROUTE, null);
        }

        assertThat(created).hasSize(4);
        assertThat(cm.recreationCount()).isEqualTo(3);
    }

    @Test
    public void closePermanently_doesNotRebuild() {
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(factory);
        HttpClientConnectionManager original = created.get(0);

        cm.closePermanently();
        cm.requestConnection(ROUTE, null);

        // Using a closed SDK HTTP client must keep failing, so the pool is never replaced.
        assertThat(created).hasSize(1);
        assertThat(cm.recreationCount()).isZero();
        verify(original).shutdown();
        verify(original).requestConnection(ROUTE, null);
    }

    @Test
    public void shutdown_afterClosePermanently_isIgnored() {
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(factory);
        HttpClientConnectionManager original = created.get(0);

        cm.closePermanently();
        cm.shutdown();
        cm.requestConnection(ROUTE, null);

        assertThat(created).hasSize(1);
        verify(original, times(1)).shutdown();
    }

    @Test
    public void closePermanently_isIdempotent() {
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(factory);

        cm.closePermanently();
        cm.closePermanently();

        verify(created.get(0), times(1)).shutdown();
    }

    @Test
    public void nonLeaseOperations_doNotBuildAReplacement() throws Exception {
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(factory);
        cm.shutdown();

        // These all relate to connections leased from the pool that was just discarded. Building a fresh pool for them
        // would be pointless, and reaping idle connections must not resurrect a client that is sitting unused.
        cm.releaseConnection(mock(org.apache.http.HttpClientConnection.class), null, 0, TimeUnit.MILLISECONDS);
        cm.closeIdleConnections(1, TimeUnit.MILLISECONDS);
        cm.closeExpiredConnections();

        assertThat(created).hasSize(1);
        assertThat(cm.recreationCount()).isZero();
    }

    @Test
    public void closeIdleConnections_delegatesToCurrentPool() {
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(factory);

        cm.closeIdleConnections(5, TimeUnit.SECONDS);

        verify(created.get(0)).closeIdleConnections(eq(5L), eq(TimeUnit.SECONDS));
    }

    @Test
    public void connect_delegatesToCurrentPool() throws Exception {
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(factory);
        org.apache.http.HttpClientConnection conn = mock(org.apache.http.HttpClientConnection.class);

        cm.connect(conn, ROUTE, 1000, null);

        verify(created.get(0)).connect(eq(conn), eq(ROUTE), anyInt(), any());
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
            org.apache.http.impl.conn.PoolingHttpClientConnectionManager pool =
                new org.apache.http.impl.conn.PoolingHttpClientConnectionManager();
            pool.setMaxTotal(maxTotal.getAndIncrement());
            return pool;
        });

        assertThat(cm.poolStats().getMax()).isEqualTo(5);

        cm.shutdown();
        cm.requestConnection(ROUTE, null);

        // Stats follow the replacement pool rather than reporting a dead one.
        assertThat(cm.poolStats().getMax()).isEqualTo(6);
    }

    @Test
    public void concurrentRequestsAfterShutdown_buildExactlyOneReplacement() throws Exception {
        int threads = 16;
        AtomicInteger factoryCalls = new AtomicInteger();
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(() -> {
            factoryCalls.incrementAndGet();
            return mock(HttpClientConnectionManager.class);
        });

        cm.shutdown();

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            for (int i = 0; i < threads; i++) {
                executor.submit(() -> {
                    try {
                        start.await();
                        cm.requestConnection(ROUTE, null);
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
    public void shutdownConcurrentWithRequests_alwaysLeavesAUsablePool() throws Exception {
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(factory);

        List<Throwable> failures = new CopyOnWriteArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            CountDownLatch done = new CountDownLatch(2);
            executor.submit(() -> {
                try {
                    for (int i = 0; i < 200; i++) {
                        cm.shutdown();
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
                        cm.requestConnection(ROUTE, null);
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

        // Whatever interleaving occurred, a subsequent request is served by the pool this manager currently holds,
        // never by one that was discarded.
        assertThat(cm.requestConnection(ROUTE, null)).isNotNull();
        verify(cm.currentDelegate(), atLeastOnce()).requestConnection(ROUTE, null);
    }

    @Test
    public void shutdown_doesNotLeakTheDiscardedPool() {
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(factory);
        HttpClientConnectionManager original = created.get(0);

        cm.shutdown();
        cm.requestConnection(ROUTE, null);

        // The discarded pool, along with any leases Apache's Error path failed to release, is gone: nothing is
        // forwarded to it any more.
        cm.closeIdleConnections(1, TimeUnit.MILLISECONDS);
        verify(original, never()).closeIdleConnections(anyLong(), any());
        assertThat(cm.currentDelegate()).isNotSameAs(original);
    }
}
