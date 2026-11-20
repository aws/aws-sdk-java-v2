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

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.sun.management.UnixOperatingSystemMXBean;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.impl.io.DefaultHttpClientConnectionOperator;
import org.apache.hc.client5.http.io.ConnectionEndpoint;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.io.ManagedHttpClientConnection;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.pool.DefaultDisposalCallback;
import org.apache.hc.core5.pool.DisposalCallback;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Real sockets: a pool whose Error-path close failed part way must not keep its idle sockets open. Apache's manager-level
 * closeIdle cannot reach them once the manager is closed, so the SDK reclaims them through the pool. Connections are made
 * through the manager API rather than HTTP requests, so the number of sockets is exact.
 */
public class RecreatingHttpClientConnectionManagerSocketReclaimTest {

    private static final int CONNECTIONS = 3;
    private static final Timeout TIMEOUT = Timeout.ofSeconds(1);

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
                                                         .options(wireMockConfig().dynamicPort())
                                                         .build();

    private final List<Socket> sockets = new CopyOnWriteArrayList<>();
    private final List<HttpClientConnectionManager> managers = new ArrayList<>();
    private final AtomicLong now = new AtomicLong(1_000_000_000L);
    private HttpRoute route;
    private long baselineFds;

    @BeforeEach
    public void setup() throws Exception {
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        assumeTrue(os instanceof UnixOperatingSystemMXBean, "Open file descriptor counts are only available on Unix");
        route = new HttpRoute(new HttpHost("http", "localhost", wireMock.getPort()));
        // Warm up first: the first connection lazily opens descriptors (class loading, the server's selectors) that stay
        // open, and must not be mistaken for leaked sockets.
        SafePoolingHttpClientConnectionManager warmUp = newPool(new DefaultDisposalCallback<>());
        establishIdleConnections(warmUp);
        warmUp.close(CloseMode.IMMEDIATE);
        sockets.clear();
        baselineFds = settledFileDescriptors();
    }

    @AfterEach
    public void teardown() throws IOException {
        for (HttpClientConnectionManager manager : managers) {
            try {
                if (manager instanceof SafePoolingHttpClientConnectionManager) {
                    ((SafePoolingHttpClientConnectionManager) manager).reclaimAfterFailedClose();
                }
                manager.close(CloseMode.IMMEDIATE);
            } catch (Throwable t) {
                // Best effort, so that a failing test cannot leak sockets into later tests.
            }
        }
        for (Socket socket : sockets) {
            socket.close();
        }
    }

    @Test
    public void lease_afterInterruptedClose_reclaimsIdleSockets() throws Exception {
        SafePoolingHttpClientConnectionManager failingPool = newPool(firstDisposalFails());
        AtomicBoolean firstHandedOut = new AtomicBoolean();
        RecreatingHttpClientConnectionManager cm = RecreatingHttpClientConnectionManager.create(
            () -> firstHandedOut.getAndSet(true) ? newPool(new DefaultDisposalCallback<>()) : failingPool, now::get);
        establishIdleConnections(cm);
        cm.clearCurrentThreadLease();

        // The close fails on the first connection it discards.
        cm.close(CloseMode.IMMEDIATE);
        assertThat(openSockets()).isEqualTo(CONNECTIONS - 1);

        cm.lease("id", route, TIMEOUT, null);

        assertThat(openSockets()).isZero();
        awaitFileDescriptorsBackToBaseline();
    }

    /** Documents why the SDK cannot use Apache's manager-level closeIdle for this. */
    @Test
    public void managerCloseIdle_afterInterruptedClose_reclaimsNothing() throws Exception {
        SafePoolingHttpClientConnectionManager pool = newPool(firstDisposalFails());
        establishIdleConnections(pool);

        assertThatThrownBy(() -> pool.close(CloseMode.IMMEDIATE)).isInstanceOf(OutOfMemoryError.class);
        pool.closeIdle(TimeValue.ZERO_MILLISECONDS);

        assertThat(openSockets()).isEqualTo(CONNECTIONS - 1);
        assertThat(openFileDescriptors()).isGreaterThanOrEqualTo(baselineFds + CONNECTIONS - 1);

        pool.reclaimAfterFailedClose();

        assertThat(openSockets()).isZero();
        awaitFileDescriptorsBackToBaseline();
    }

    /** Leases every connection before releasing any, so that they are distinct idle connections. */
    private void establishIdleConnections(HttpClientConnectionManager cm) throws Exception {
        List<ConnectionEndpoint> endpoints = new ArrayList<>();
        for (int i = 0; i < CONNECTIONS; i++) {
            ConnectionEndpoint endpoint = cm.lease("id-" + i, route, TIMEOUT, null).get(TIMEOUT);
            cm.connect(endpoint, TimeValue.ofSeconds(1), HttpClientContext.create());
            endpoints.add(endpoint);
        }
        for (ConnectionEndpoint endpoint : endpoints) {
            cm.release(endpoint, null, TimeValue.ofMinutes(1));
        }
        assertThat(sockets).hasSize(CONNECTIONS);
        assertThat(openSockets()).isEqualTo(CONNECTIONS);
    }

    private SafePoolingHttpClientConnectionManager newPool(DisposalCallback<ManagedHttpClientConnection> disposal) {
        DefaultHttpClientConnectionOperator operator = new DefaultHttpClientConnectionOperator(
            proxy -> {
                Socket socket = proxy == null ? new Socket() : new Socket(proxy);
                sockets.add(socket);
                return socket;
            },
            null,
            null,
            RegistryBuilder.<TlsSocketStrategy>create().build());
        SafeStrictConnPool strictPool = new SafeStrictConnPool(10, 10, null, PoolReusePolicy.LIFO, disposal, null);
        SafePoolingHttpClientConnectionManager pool = SafePoolingHttpClientConnectionManager.create(operator, strictPool);
        managers.add(pool);
        return pool;
    }

    /** Apache's disposal, which closes the connection, followed by a simulated Error on its first call only. */
    private static DisposalCallback<ManagedHttpClientConnection> firstDisposalFails() {
        DisposalCallback<ManagedHttpClientConnection> real = new DefaultDisposalCallback<>();
        AtomicBoolean first = new AtomicBoolean(true);
        return (connection, closeMode) -> {
            real.execute(connection, closeMode);
            if (first.getAndSet(false)) {
                throw new OutOfMemoryError("simulated");
            }
        };
    }

    private long openSockets() {
        return sockets.stream().filter(s -> !s.isClosed()).count();
    }

    /**
     * Counts open file descriptors once the count has stopped changing, so that descriptors still closing from earlier
     * work are not part of the baseline.
     */
    private static long settledFileDescriptors() throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        long previous = openFileDescriptors();
        while (System.nanoTime() < deadline) {
            Thread.sleep(100);
            long current = openFileDescriptors();
            if (current == previous) {
                return current;
            }
            previous = current;
        }
        return previous;
    }

    private static long openFileDescriptors() {
        return ((UnixOperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getOpenFileDescriptorCount();
    }

    /** Server-side sockets close asynchronously, so poll. */
    private void awaitFileDescriptorsBackToBaseline() throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (openFileDescriptors() > baselineFds && System.nanoTime() < deadline) {
            Thread.sleep(50);
        }
        assertThat(openFileDescriptors()).isLessThanOrEqualTo(baselineFds);
    }
}
