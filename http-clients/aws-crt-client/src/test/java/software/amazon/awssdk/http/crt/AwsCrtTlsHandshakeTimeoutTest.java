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

package software.amazon.awssdk.http.crt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.http.HttpTestUtils.createProvider;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES;
import static software.amazon.awssdk.http.crt.CrtHttpClientTestUtils.createRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.crt.Log;
import software.amazon.awssdk.crt.http.HttpException;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.RecordingResponseHandler;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Functional test that exercises the CRT runtime's TLS-handshake-timeout machinery end-to-end. Stands up a raw
 * {@link ServerSocket} (NOT an {@code SSLServerSocket}) on loopback that accepts the TCP connection, drains a
 * little of the ClientHello into a discard buffer, and never writes a ServerHello. The CRT TLS-negotiation timer
 * fires before the connectionTimeout, completing the request future exceptionally.
 */
class AwsCrtTlsHandshakeTimeoutTest {

    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration ASSERTION_UPPER_BOUND = Duration.ofSeconds(10);

    private TlsStallingServer stallingServer;

    @BeforeAll
    static void beforeAll() {
        System.setProperty("aws.crt.debugnative", "true");
        Log.initLoggingToStdout(Log.LogLevel.Warn);
    }

    @BeforeEach
    void setUp() throws IOException {
        stallingServer = new TlsStallingServer();
        stallingServer.start();
    }

    @AfterEach
    void tearDown() {
        if (stallingServer != null) {
            stallingServer.stop();
        }
    }

    @Test
    void asyncClient_serverWithholdsServerHello_failsWithTlsNegotiationTimeoutDrivenByConfiguredValue() throws Exception {
        Duration configuredTimeout = Duration.ofSeconds(3);
        Duration elapsedFloor = Duration.ofMillis(1500);

        AttributeMap defaults = AttributeMap.builder().put(TRUST_ALL_CERTIFICATES, true).build();
        try (SdkAsyncHttpClient client = AwsCrtAsyncHttpClient.builder()
                                                              .tlsNegotiationTimeout(configuredTimeout)
                                                              .connectionTimeout(CONNECTION_TIMEOUT)
                                                              .buildWithDefaults(defaults)) {

            URI uri = URI.create("https://localhost:" + stallingServer.port());
            SdkHttpRequest request = createRequest(uri);
            RecordingResponseHandler recorder = new RecordingResponseHandler();

            long start = System.nanoTime();
            client.execute(AsyncExecuteRequest.builder()
                                              .request(request)
                                              .requestContentPublisher(createProvider(""))
                                              .responseHandler(recorder)
                                              .build());

            assertCompletedWithTlsNegotiationTimeout(recorder.completeFuture(), ASSERTION_UPPER_BOUND);
            Duration elapsed = Duration.ofNanos(System.nanoTime() - start);
            assertThat(elapsed).as("configured timeout %s should drive the deadline (elapsed=%s)", configuredTimeout, elapsed)
                               .isGreaterThanOrEqualTo(elapsedFloor);
        }
    }

    @Test
    void syncClient_serverWithholdsServerHello_failsWithTlsNegotiationTimeoutDrivenByConfiguredValue() {
        Duration configuredTimeout = Duration.ofSeconds(3);
        Duration elapsedFloor = Duration.ofMillis(1500);

        AttributeMap defaults = AttributeMap.builder().put(TRUST_ALL_CERTIFICATES, true).build();
        try (SdkHttpClient client = AwsCrtHttpClient.builder()
                                                    .tlsNegotiationTimeout(configuredTimeout)
                                                    .connectionTimeout(CONNECTION_TIMEOUT)
                                                    .buildWithDefaults(defaults)) {

            URI uri = URI.create("https://localhost:" + stallingServer.port());
            SdkHttpRequest request = createRequest(uri);
            HttpExecuteRequest httpExecuteRequest = HttpExecuteRequest.builder()
                                                                     .request(request)
                                                                     .contentStreamProvider(() -> new ByteArrayInputStream(new byte[0]))
                                                                     .build();
            ExecutableHttpRequest executableRequest = client.prepareRequest(httpExecuteRequest);

            long start = System.nanoTime();
            assertThatThrownBy(executableRequest::call)
                .isInstanceOf(IOException.class)
                .hasCauseInstanceOf(HttpException.class)
                .hasMessageContaining("tls negotiation timeout");
            Duration elapsed = Duration.ofNanos(System.nanoTime() - start);
            assertThat(elapsed).as("configured timeout %s should drive the deadline (elapsed=%s)", configuredTimeout, elapsed)
                               .isBetween(elapsedFloor, ASSERTION_UPPER_BOUND);
        }
    }

    private static void assertCompletedWithTlsNegotiationTimeout(CompletableFuture<?> future, Duration upperBound) throws Exception {
        try {
            future.get(upperBound.toMillis(), TimeUnit.MILLISECONDS);
            throw new AssertionError("Expected TLS-negotiation-timeout failure but the future completed successfully");
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new AssertionError("Future did not complete within " + upperBound + " - the TLS-handshake-timeout timer "
                                     + "did not fire (or the SDK did not surface it).", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            assertThat(cause).isInstanceOf(IOException.class);
            assertThat(cause).hasCauseInstanceOf(HttpException.class);
            assertThat(cause).hasMessageContaining("tls negotiation timeout");
        }
    }

    /**
     * Raw {@link ServerSocket} on loopback that accepts TCP connections and never completes the TLS handshake.
     * For each accepted client, a short-lived reader drains up to one buffer's worth of bytes (typically the
     * ClientHello) so the kernel send buffer doesn't back-pressure the client into a write block, then the socket
     * is held open until {@link #stop()} closes the listener. The client's TLS handshake timer fires while waiting
     * for a ServerHello that never arrives.
     */
    private static final class TlsStallingServer {
        private ServerSocket serverSocket;
        private Thread listenerThread;
        private volatile boolean running;

        void start() throws IOException {
            serverSocket = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
            running = true;
            listenerThread = new Thread(this::acceptLoop, "AwsCrtTlsHandshakeTimeoutTest-StallingServer");
            listenerThread.setDaemon(true);
            listenerThread.start();
        }

        int port() {
            return serverSocket.getLocalPort();
        }

        void stop() {
            running = false;
            try {
                serverSocket.close();
            } catch (IOException ignored) {
                // best-effort
            }
            if (listenerThread != null) {
                listenerThread.interrupt();
                try {
                    listenerThread.join(TimeUnit.SECONDS.toMillis(2));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        private void acceptLoop() {
            while (running && !Thread.currentThread().isInterrupted()) {
                Socket client;
                try {
                    client = serverSocket.accept();
                } catch (IOException e) {
                    return;
                }
                Thread worker = new Thread(() -> drainAndStall(client),
                                           "AwsCrtTlsHandshakeTimeoutTest-StallingClient");
                worker.setDaemon(true);
                worker.start();
            }
        }

        private void drainAndStall(Socket client) {
            try (InputStream in = client.getInputStream()) {
                byte[] discard = new byte[4096];
                client.setSoTimeout(200);
                try {
                    in.read(discard);
                } catch (IOException ignored) {
                    // expected: read times out or the client side closes when the TLS-negotiation timer fires.
                }
                while (running) {
                    Thread.sleep(50);
                }
            } catch (IOException | InterruptedException ignored) {
                // teardown path
            } finally {
                try {
                    client.close();
                } catch (IOException ignored) {
                    // best-effort
                }
            }
        }
    }
}
