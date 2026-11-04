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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.io.HttpClientConnectionOperator;
import org.apache.hc.client5.http.io.ManagedHttpClientConnection;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.pool.DisposalCallback;
import org.apache.hc.core5.pool.PoolEntry;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.pool.PoolStats;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SafePoolingHttpClientConnectionManagerTest {
    private static final HttpRoute ROUTE_A = new HttpRoute(new HttpHost("a.example.com", 80));
    private static final HttpRoute ROUTE_B = new HttpRoute(new HttpHost("b.example.com", 80));

    private static WireMockServer wm;

    @BeforeAll
    static void setup() {
        wm = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wm.start();
    }

    @AfterAll
    static void teardown() {
        wm.stop();
    }

    @Test
    void leaseInterrupted_doesNotLeakConnections_probabilistic() throws IOException, InterruptedException, ParseException {
        wm.stubFor(WireMock.get(WireMock.anyUrl()).willReturn(WireMock.aResponse().withStatus(204)));
        int rounds = 16;
        for (int i = 0; i < rounds; ++i) {
            leaseInterrupted_doesNotLeakConnections();
        }
    }

    private void leaseInterrupted_doesNotLeakConnections() throws InterruptedException, IOException, ParseException {
        int numRequests = 10_000;

        SafePoolingHttpClientConnectionManager connectionManager = SafePoolingHttpClientConnectionManagerBuilder.create().build();
        connectionManager.setMaxTotal(100);
        connectionManager.setDefaultMaxPerRoute(40);

        try (CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(connectionManager).build()) {
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);
            ExecutorService executor = Executors.newFixedThreadPool(100);

            try {
                List<Future<?>> futures = new ArrayList<>(numRequests);
                CountDownLatch go = new CountDownLatch(numRequests);

                for (int i = 0; i < numRequests; i++) {
                    Future<?> f =
                        executor.submit(
                            () -> {
                                go.countDown();
                                try {
                                    doRequest(httpClient);
                                    successCount.incrementAndGet();
                                } catch (Exception e) {
                                    errorCount.incrementAndGet();
                                }
                            });
                    futures.add(f);
                }

                go.await();

                for (Future<?> future : futures) {
                    future.cancel(true);
                }
            } finally {
                executor.shutdown();
                executor.awaitTermination(100, TimeUnit.SECONDS);
            }
            connectionManager.closeExpired();
            assertNoLeases(connectionManager);
            doRequest(httpClient);
        }
    }

    private static void doRequest(CloseableHttpClient httpClient) throws IOException, ParseException {
        HttpGet httpGet = new HttpGet("http://localhost:" + wm.port());
        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                EntityUtils.toString(entity);
            }
        }
    }

    private static void assertNoLeases(SafePoolingHttpClientConnectionManager manager) {
        PoolStats totalStats = manager.getTotalStats();
        assertThat(totalStats.getLeased()).as("Leased connection count").isEqualTo(0);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // reclaimAfterFailedClose(): closing what a close that failed part way left open
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    void reclaimAfterFailedClose_reclaimsIdleEntriesLeftByInterruptedClose() throws Exception {
        FailingDisposal disposal = new FailingDisposal(1);
        SafeStrictConnPool pool = newPool(disposal, 10);
        SafePoolingHttpClientConnectionManager manager = newManager(pool);
        List<ManagedHttpClientConnection> idle = idleEntries(pool, ROUTE_A, ROUTE_A, ROUTE_B);

        assertThatThrownBy(() -> manager.close(CloseMode.IMMEDIATE)).isInstanceOf(OutOfMemoryError.class);
        assertThat(closedCount(idle)).isEqualTo(1);

        manager.reclaimAfterFailedClose();

        for (ManagedHttpClientConnection connection : idle) {
            verify(connection, times(1)).close(any(CloseMode.class));
        }
    }

    /**
     * Documents the Apache behavior that makes {@link SafePoolingHttpClientConnectionManager#reclaimAfterFailedClose()}
     * necessary: the manager's own closeIdle returns early once the manager is closed.
     */
    @Test
    void closeIdle_afterInterruptedClose_reclaimsNothing() throws Exception {
        FailingDisposal disposal = new FailingDisposal(1);
        SafeStrictConnPool pool = newPool(disposal, 10);
        SafePoolingHttpClientConnectionManager manager = newManager(pool);
        List<ManagedHttpClientConnection> idle = idleEntries(pool, ROUTE_A, ROUTE_A, ROUTE_B);
        assertThatThrownBy(() -> manager.close(CloseMode.IMMEDIATE)).isInstanceOf(OutOfMemoryError.class);

        manager.closeIdle(TimeValue.ZERO_MILLISECONDS);

        assertThat(closedCount(idle)).isEqualTo(1);
    }

    @Test
    void reclaimAfterFailedClose_doesNotTouchLeasedEntries() throws Exception {
        FailingDisposal disposal = new FailingDisposal(1);
        SafeStrictConnPool pool = newPool(disposal, 10);
        SafePoolingHttpClientConnectionManager manager = newManager(pool);
        ManagedHttpClientConnection leased = leasedEntry(pool, ROUTE_A).getConnection();
        idleEntries(pool, ROUTE_A, ROUTE_A);

        // The close fails on the first idle entry, before the route's leased entries are reached.
        assertThatThrownBy(() -> manager.close(CloseMode.IMMEDIATE)).isInstanceOf(OutOfMemoryError.class);
        manager.reclaimAfterFailedClose();

        // A known limitation: leased entries of a route whose close was interrupted are left to their requests.
        verify(leased, never()).close(any(CloseMode.class));
    }

    @Test
    void reclaimAfterFailedClose_doesNotCompletePendingLeases() throws Exception {
        FailingDisposal disposal = new FailingDisposal(1);
        SafeStrictConnPool pool = newPool(disposal, 1);
        SafePoolingHttpClientConnectionManager manager = newManager(pool);
        leasedEntry(pool, ROUTE_A);
        Future<PoolEntry<HttpRoute, ManagedHttpClientConnection>> pending =
            pool.lease(ROUTE_A, null, Timeout.ofMilliseconds(50), null);
        assertThat(pending.isDone()).isFalse();
        // Let the pending lease's deadline pass, so that the reclaim's processPendingRequests() fails and queues it.
        Thread.sleep(100);

        // The close fails on the leased entry's disposal, before it clears the pending requests.
        assertThatThrownBy(() -> manager.close(CloseMode.IMMEDIATE)).isInstanceOf(OutOfMemoryError.class);
        manager.reclaimAfterFailedClose();

        // Completing a waiter takes fireCallbacks(), which nothing runs on a pool whose close has started.
        assertThat(pending.isDone()).isFalse();
    }

    /**
     * A close that fails before StrictConnPool.close starts (for example in Apache's debug log call) leaves the pool fully
     * open and serving leases. The reclaim performs the close Apache asked for.
     */
    @Test
    void reclaimAfterFailedClose_poolNeverStartedClosing_shutsPoolDownAndClosesAllConnections() throws Exception {
        SafeStrictConnPool pool = newPool(new FailingDisposal(), 10);
        SafePoolingHttpClientConnectionManager manager = newManager(pool);
        ManagedHttpClientConnection idle = idleEntries(pool, ROUTE_A).get(0);
        ManagedHttpClientConnection leased = leasedEntry(pool, ROUTE_B).getConnection();

        manager.reclaimAfterFailedClose();

        verify(idle, times(1)).close(any(CloseMode.class));
        verify(leased, times(1)).close(any(CloseMode.class));
        assertThat(pool.isShutdown()).isTrue();
        assertThatThrownBy(() -> manager.lease("id", ROUTE_A, Timeout.ofSeconds(1), null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Connection pool shut down");
    }

    static SafeStrictConnPool newPool(DisposalCallback<ManagedHttpClientConnection> disposal, int maxTotal) {
        return new SafeStrictConnPool(maxTotal, maxTotal, null, PoolReusePolicy.LIFO, disposal, null);
    }

    static SafePoolingHttpClientConnectionManager newManager(SafeStrictConnPool pool) {
        return SafePoolingHttpClientConnectionManager.create(mock(HttpClientConnectionOperator.class), pool);
    }

    /**
     * Creates one idle entry with a mock connection per route given, in the pool's available list. All are leased before
     * any is released, so that each is a distinct entry.
     */
    static List<ManagedHttpClientConnection> idleEntries(SafeStrictConnPool pool, HttpRoute... routes) throws Exception {
        List<PoolEntry<HttpRoute, ManagedHttpClientConnection>> entries = new ArrayList<>();
        for (HttpRoute route : routes) {
            entries.add(leasedEntry(pool, route));
        }
        List<ManagedHttpClientConnection> connections = new ArrayList<>();
        for (PoolEntry<HttpRoute, ManagedHttpClientConnection> entry : entries) {
            connections.add(entry.getConnection());
            pool.release(entry, true);
        }
        return connections;
    }

    static PoolEntry<HttpRoute, ManagedHttpClientConnection> leasedEntry(SafeStrictConnPool pool, HttpRoute route)
            throws Exception {
        PoolEntry<HttpRoute, ManagedHttpClientConnection> entry = pool.lease(route, null).get(1, TimeUnit.SECONDS);
        entry.assignConnection(mock(ManagedHttpClientConnection.class));
        return entry;
    }

    private static long closedCount(List<ManagedHttpClientConnection> connections) {
        return connections.stream()
                          .filter(connection -> mockingDetails(connection).getInvocations()
                                                                    .stream()
                                                                    .anyMatch(i -> "close".equals(i.getMethod().getName())))
                          .count();
    }

    /**
     * Closes the connection as Apache's disposal does, then throws a simulated OutOfMemoryError on the given calls
     * (1-based). With no failing calls it never throws.
     */
    static final class FailingDisposal implements DisposalCallback<ManagedHttpClientConnection> {
        private final List<Integer> failingCalls = new ArrayList<>();
        private final AtomicInteger calls = new AtomicInteger();
        private volatile Runnable beforeFailure = () -> { };

        FailingDisposal(int... failingCalls) {
            for (int failingCall : failingCalls) {
                this.failingCalls.add(failingCall);
            }
        }

        /** Runs the action just before each simulated failure is thrown. */
        FailingDisposal beforeFailure(Runnable action) {
            this.beforeFailure = action;
            return this;
        }

        @Override
        public void execute(ManagedHttpClientConnection connection, CloseMode closeMode) {
            connection.close(closeMode);
            if (failingCalls.contains(calls.incrementAndGet())) {
                beforeFailure.run();
                throw new OutOfMemoryError("simulated");
            }
        }
    }
}
