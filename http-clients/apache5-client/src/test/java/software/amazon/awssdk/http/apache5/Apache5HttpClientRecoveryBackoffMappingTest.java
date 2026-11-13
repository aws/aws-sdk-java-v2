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

package software.amazon.awssdk.http.apache5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.io.LeaseRequest;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.util.Timeout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.http.ConnectionPoolShutdownException;
import software.amazon.awssdk.http.ConnectionPoolShutdownException.Reason;
import software.amazon.awssdk.http.apache5.internal.conn.RecreatingHttpClientConnectionManager;
import software.amazon.awssdk.http.apache5.internal.impl.ConnectionManagerAwareHttpClient;

/**
 * How {@link Apache5HttpClient} reports Apache's "Connection pool shut down" when recovery is enabled, using a real
 * recreating wrapper over mock pools and a frozen clock.
 */
public class Apache5HttpClientRecoveryBackoffMappingTest {

    private static final HttpRoute ROUTE = new HttpRoute(new HttpHost("localhost", 8080));
    private static final Timeout TIMEOUT = Timeout.ofSeconds(1);

    private final AtomicLong now = new AtomicLong(1_000_000_000L);
    private final List<HttpClientConnectionManager> pools = new ArrayList<>();
    private final List<ExecutorService> executors = new ArrayList<>();
    private ConnectionManagerAwareHttpClient httpClient;
    private RecreatingHttpClientConnectionManager cm;
    private Apache5HttpClient client;

    @BeforeEach
    public void setup() throws Exception {
        cm = RecreatingHttpClientConnectionManager.create(this::newPool, now::get);
        httpClient = mock(ConnectionManagerAwareHttpClient.class);
        when(httpClient.getHttpClientConnectionManager()).thenReturn(cm);
        client = Apache5HttpClientPoolShutdownMappingTest.newClient(httpClient);
    }

    @AfterEach
    public void teardown() {
        executors.forEach(ExecutorService::shutdownNow);
    }

    @Test
    public void execute_poolShutDownIllegalState_inRecoveryBackoff_mapsToRecoveryBackoff() throws Exception {
        cm.close(CloseMode.IMMEDIATE);
        cm.lease("id", ROUTE, TIMEOUT, null);
        cm.close(CloseMode.IMMEDIATE);
        assertThat(cm.inRecoveryBackoff()).isTrue();
        IllegalStateException apacheException = new IllegalStateException("Connection pool shut down");
        when(httpClient.executeOpen(any(), any(), any())).thenThrow(apacheException);

        Throwable thrown = catchThrowable(() -> Apache5HttpClientPoolShutdownMappingTest.call(client));

        assertThat(thrown).isInstanceOf(ConnectionPoolShutdownException.class).hasMessage("Connection pool shut down");
        assertThat(((ConnectionPoolShutdownException) thrown).reason()).isEqualTo(Reason.RECOVERY_BACKOFF);
        assertThat(thrown.getCause()).isSameAs(apacheException);
    }

    /**
     * Recovery is enabled ({@code WITH_BACKOFF}) and the client is not in backoff, so the next attempt rebuilds the pool:
     * the failure is transient, and is reported as an IOException, which the SDK retries.
     */
    @Test
    public void execute_poolShutDownIllegalState_withBackoffNotInBackoff_mapsToIOException() throws Exception {
        assertPoolShutDownMapsToIOException(httpClient, client);
    }

    /**
     * With a zero window ({@code ALWAYS}) a close right after a rebuild never enters backoff, so the failure is still
     * transient and never reported as RECOVERY_BACKOFF.
     */
    @Test
    public void execute_poolShutDownIllegalState_always_mapsToIOException() throws Exception {
        RecreatingHttpClientConnectionManager alwaysCm =
            RecreatingHttpClientConnectionManager.create(this::newPool, now::get, Duration.ZERO);
        ConnectionManagerAwareHttpClient alwaysHttpClient = mock(ConnectionManagerAwareHttpClient.class);
        when(alwaysHttpClient.getHttpClientConnectionManager()).thenReturn(alwaysCm);
        Apache5HttpClient alwaysClient = Apache5HttpClientPoolShutdownMappingTest.newClient(alwaysHttpClient);
        alwaysCm.close(CloseMode.IMMEDIATE);
        alwaysCm.lease("id", ROUTE, TIMEOUT, null);
        alwaysCm.close(CloseMode.IMMEDIATE);
        assertThat(alwaysCm.inRecoveryBackoff()).isFalse();

        assertPoolShutDownMapsToIOException(alwaysHttpClient, alwaysClient);
    }

    private static void assertPoolShutDownMapsToIOException(ConnectionManagerAwareHttpClient mockHttpClient,
                                                            Apache5HttpClient clientUnderTest) throws Exception {
        IllegalStateException apacheException = new IllegalStateException("Connection pool shut down");
        when(mockHttpClient.executeOpen(any(), any(), any())).thenThrow(apacheException);

        Throwable thrown = catchThrowable(() -> Apache5HttpClientPoolShutdownMappingTest.call(clientUnderTest));

        assertThat(thrown).isExactlyInstanceOf(IOException.class);
        assertThat(thrown.getCause()).isSameAs(apacheException);
        for (Throwable t = thrown; t != null; t = t.getCause()) {
            assertThat(t).isNotInstanceOf(ConnectionPoolShutdownException.class);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void execute_anyResult_clearsCurrentThreadLease(boolean requestSucceeds) throws Exception {
        when(httpClient.executeOpen(any(), any(), any())).thenAnswer(invocation -> {
            // The request leases through the wrapper, which records the pool on this thread.
            cm.lease("id", ROUTE, TIMEOUT, null);
            if (!requestSucceeds) {
                throw new IOException("request failed");
            }
            return new BasicClassicHttpResponse(200, "OK");
        });
        catchThrowable(() -> Apache5HttpClientPoolShutdownMappingTest.call(client));

        // Another thread destroys the pool the test thread used, and a third rebuilds it.
        newExecutor().submit(() -> cm.close(CloseMode.IMMEDIATE)).get(10, TimeUnit.SECONDS);
        newExecutor().submit(() -> cm.lease("id", ROUTE, TIMEOUT, null)).get(10, TimeUnit.SECONDS);
        cm.close(CloseMode.IMMEDIATE);

        // With the record cleared, the test thread's close is not mistaken for a stale one: it destroys the rebuilt
        // pool, inside the window.
        assertThat(cm.inRecoveryBackoff()).isTrue();
    }

    private HttpClientConnectionManager newPool() {
        HttpClientConnectionManager pool = mock(HttpClientConnectionManager.class);
        when(pool.lease(any(), any(), any(), any())).thenReturn(mock(LeaseRequest.class));
        pools.add(pool);
        return pool;
    }

    private ExecutorService newExecutor() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executors.add(executor);
        return executor;
    }
}
