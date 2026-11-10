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

package software.amazon.awssdk.http.nio.netty.fault;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.fail;
import static software.amazon.awssdk.http.ProtocolNegotiation.ALPN;
import static software.amazon.awssdk.http.ProtocolNegotiation.ASSUME_PROTOCOL;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES;
import static software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClientTestUtils.createProvider;
import static software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClientTestUtils.createRequest;
import static software.amazon.awssdk.http.nio.netty.fault.TlsFaultProxy.Fault.CLOSE;
import static software.amazon.awssdk.http.nio.netty.fault.TlsFaultProxy.Fault.RESET;
import static software.amazon.awssdk.http.nio.netty.fault.TlsFaultProxy.Fault.STALL;

import io.netty.handler.ssl.SslProvider;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.http.HttpMetric;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.ProtocolNegotiation;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.MockH2Server;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.RecordingResponseHandler;
import software.amazon.awssdk.http.nio.netty.fault.TlsFaultProxy.Fault;
import software.amazon.awssdk.http.nio.netty.internal.utils.NettyUtils;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Verifies that an HTTP/2 connection whose TLS handshake fails after TCP connects fails its request and returns its
 * connection pool permit, so that requests succeed again once the server is healthy.
 *
 * <p>The client uses {@code maxConcurrency(1)}, so a single permit that is not returned makes every later request fail
 * with an acquire timeout.
 */
@EnabledIf("alpnSupported")
class TlsHandshakeFailureConnectionPoolTest {
    private static final Duration TLS_NEGOTIATION_TIMEOUT = Duration.ofMillis(300);
    private static final Duration CONNECTION_ACQUIRE_TIMEOUT = Duration.ofSeconds(2);
    private static final long TIMEOUT_SECONDS = 10;
    private static final int MAX_FAULTED_REQUEST_ATTEMPTS = 3;
    private static final String CLOSED_BEFORE_PROTOCOL = "closed before the protocol could be determined";
    // The JDK reports a reset as "Connection reset" or "Connection reset by peer", and the SDK can reword it as
    // "The connection was closed during the request".
    private static final String CONNECTION_RESET = "(?i)connection (reset|was closed)";

    private static MockH2Server server;

    private TlsFaultProxy proxy;
    private SdkAsyncHttpClient client;

    @BeforeAll
    static void startServer() throws Exception {
        server = new MockH2Server(true);
        server.start();
    }

    @AfterAll
    static void stopServer() throws Exception {
        server.stop();
    }

    @BeforeEach
    void setup() throws IOException {
        proxy = new TlsFaultProxy(server.getHttpsUri().getPort());
    }

    @AfterEach
    void teardown() throws IOException {
        if (client != null) {
            client.close();
        }
        proxy.close();
    }

    static Stream<Arguments> faultCases() {
        return Stream.of(
            Arguments.of(SslProvider.JDK, ASSUME_PROTOCOL, STALL, CLOSED_BEFORE_PROTOCOL),
            Arguments.of(SslProvider.JDK, ASSUME_PROTOCOL, CLOSE, CLOSED_BEFORE_PROTOCOL),
            Arguments.of(SslProvider.JDK, ASSUME_PROTOCOL, RESET, CONNECTION_RESET),
            Arguments.of(SslProvider.JDK, ALPN, STALL, CLOSED_BEFORE_PROTOCOL),
            Arguments.of(SslProvider.JDK, ALPN, CLOSE, CLOSED_BEFORE_PROTOCOL),
            Arguments.of(SslProvider.JDK, ALPN, RESET, CLOSED_BEFORE_PROTOCOL),
            Arguments.of(SslProvider.OPENSSL, ALPN, STALL, CLOSED_BEFORE_PROTOCOL),
            Arguments.of(SslProvider.OPENSSL, ALPN, CLOSE, CLOSED_BEFORE_PROTOCOL),
            Arguments.of(SslProvider.OPENSSL, ALPN, RESET, CLOSED_BEFORE_PROTOCOL));
    }

    @ParameterizedTest(name = "{0}, {1}, {2}")
    @MethodSource("faultCases")
    void execute_whenTlsHandshakeFailsOnNewConnection_shouldReleasePermitForNextRequest(
        SslProvider sslProvider, ProtocolNegotiation negotiation, Fault fault, String expectedMessage) throws Exception {

        // A successful request initializes the protocol, so the faulted connection is opened under a pool permit.
        client = createClient(sslProvider, negotiation);
        executeSuccessfully(MetricCollector.create("initial-request"));
        proxy.injectFault(fault);
        proxy.closeForwardedConnectionsAndAwaitClientClose(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        Throwable failure = executeOnNewConnectionExpectingFailure();

        MetricCollector recoveryMetrics = MetricCollector.create("recovery-request");
        assertRecoveryRequestSucceeds(recoveryMetrics);
        assertThat(recoveryMetrics.collect().metricValues(HttpMetric.LEASED_CONCURRENCY))
            .as("Permits in use when the recovery request started")
            .containsExactly(0);
        assertTlsFailure(failure, expectedMessage);
    }

    @ParameterizedTest(name = "{0}, {1}, {2}")
    @MethodSource("faultCases")
    void execute_whenTlsHandshakeFailsOnFirstConnection_shouldInitializeProtocolOnNextRequest(
        SslProvider sslProvider, ProtocolNegotiation negotiation, Fault fault, String expectedMessage) throws Exception {

        client = createClient(sslProvider, negotiation);
        proxy.injectFault(fault);

        Throwable failure = executeOnNewConnectionExpectingFailure();

        assertRecoveryRequestSucceeds(MetricCollector.create("recovery-request"));
        assertTlsFailure(failure, expectedMessage);
    }

    /**
     * Sends a request that must open exactly one new connection and fail within the TLS negotiation timeout plus one
     * second, and returns its failure. A slower failure means the request waited for the acquire timeout.
     *
     * <p>The proxy sees the client's FIN before the client pool drops the closed connection's record, so a request can
     * fail on that stale record without opening a connection. Such a request is sent again.
     */
    private Throwable executeOnNewConnectionExpectingFailure() throws Exception {
        for (int attempt = 0; attempt < MAX_FAULTED_REQUEST_ATTEMPTS; attempt++) {
            int acceptedBefore = proxy.acceptedConnections();
            long start = System.nanoTime();
            Throwable failure = executeExpectingFailure();
            if (proxy.acceptedConnections() == acceptedBefore) {
                continue;
            }
            assertThat(Duration.ofNanos(System.nanoTime() - start))
                .as("Time until the faulted request failed")
                .isLessThan(TLS_NEGOTIATION_TIMEOUT.plusSeconds(1));
            assertThat(proxy.acceptedConnections())
                .as("New connections opened by the faulted request")
                .isEqualTo(acceptedBefore + 1);
            return failure;
        }
        return fail("No faulted request opened a new connection in " + MAX_FAULTED_REQUEST_ATTEMPTS + " attempts");
    }

    private void assertRecoveryRequestSucceeds(MetricCollector metrics) {
        proxy.injectFault(Fault.NONE);
        assertThatCode(() -> executeSuccessfully(metrics))
            .as("Request after the server recovered. It fails if the faulted connection kept its pool permit.")
            .doesNotThrowAnyException();
    }

    /**
     * The faulted request must fail with the connection error, not with an acquire timeout. An acquire timeout means
     * nothing completed the connection's protocol negotiation.
     */
    private static void assertTlsFailure(Throwable failure, String expectedMessage) {
        assertThat(failure)
            .as("Failure of the faulted request")
            .isInstanceOf(IOException.class)
            .hasMessageFindingMatch(expectedMessage);
    }

    private void executeSuccessfully(MetricCollector metrics) throws Exception {
        execute(metrics).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private Throwable executeExpectingFailure() throws Exception {
        try {
            execute(MetricCollector.create("faulted-request")).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            return e.getCause();
        }
        return fail("The request was expected to fail because the TLS handshake failed");
    }

    private CompletableFuture<Void> execute(MetricCollector metrics) {
        URI uri = URI.create("https://localhost:" + proxy.port());
        return client.execute(AsyncExecuteRequest.builder()
                                                 .request(createRequest(uri))
                                                 .requestContentPublisher(createProvider(""))
                                                 .responseHandler(new RecordingResponseHandler())
                                                 .metricCollector(metrics)
                                                 .build());
    }

    private static SdkAsyncHttpClient createClient(SslProvider sslProvider, ProtocolNegotiation negotiation) {
        return NettyNioAsyncHttpClient.builder()
                                      .sslProvider(sslProvider)
                                      .protocol(Protocol.HTTP2)
                                      .protocolNegotiation(negotiation)
                                      .maxConcurrency(1)
                                      .tlsNegotiationTimeout(TLS_NEGOTIATION_TIMEOUT)
                                      .connectionAcquisitionTimeout(CONNECTION_ACQUIRE_TIMEOUT)
                                      .buildWithDefaults(AttributeMap.builder()
                                                                     .put(TRUST_ALL_CERTIFICATES, true)
                                                                     .build());
    }

    private static boolean alpnSupported() {
        return NettyUtils.isAlpnSupported(SslProvider.JDK);
    }
}
