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


package software.amazon.awssdk.http;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.http.LongRunningRequestTestSupport.assertAcquireTimeoutFailure;
import static software.amazon.awssdk.http.LongRunningRequestTestSupport.assertFailsWithIoExceptionWithinTimeBound;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.server.DeferredResponseServer;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Validates that HTTP/2 {@link SdkAsyncHttpClient} implementations fail a request that cannot get a stream within the
 * connection acquire timeout, instead of letting it wait without a time limit.
 */
public abstract class SdkAsyncHttpClientH2AcquireTimeoutTestSuite {
    protected static final Duration ACQUIRE_TIMEOUT = Duration.ofMillis(500);

    private static DeferredResponseServer server;

    @BeforeAll
    public static void startServer() throws InterruptedException {
        server = DeferredResponseServer.start(Protocol.HTTP2);
    }

    @AfterAll
    public static void stopServer() throws InterruptedException {
        server.close();
    }

    @BeforeEach
    public void resetServer() {
        server.reset();
    }

    /**
     * Returns a client built with {@code config}, which selects HTTP/2 with prior knowledge, allows one concurrent request,
     * and sets the connection acquire timeout to {@link #ACQUIRE_TIMEOUT}.
     */
    protected abstract SdkAsyncHttpClient createSdkAsyncHttpClient(AttributeMap config);

    /**
     * The start of the message of the exception a request fails with when it cannot get a stream in time, or {@code null}
     * to not check the message.
     */
    protected String expectedAcquireTimeoutMessagePrefix() {
        return null;
    }

    /**
     * The type of the cause of the exception a request fails with when it cannot get a stream in time, or {@code null} to
     * not check the cause.
     */
    protected Class<? extends Throwable> expectedAcquireTimeoutCause() {
        return null;
    }

    @Test
    public void executeWhenStreamsExhaustedFailsWithinTimeoutBound() throws Exception {
        try (SdkAsyncHttpClient client = createSdkAsyncHttpClient(config())) {
            // Open the connection first, so that the held request below is assigned to it at once.
            server.releaseResponses();
            sendRequest(client, server.uri()).get(5, TimeUnit.SECONDS);
            server.reset();
            CompletableFuture<byte[]> held = sendRequest(client, server.uri());
            awaitReceivedRequests(1);

            assertAcquireTimeout(sendRequest(client, server.uri()));

            server.releaseResponses();
            held.get(5, TimeUnit.SECONDS);
        }
    }

    // The server accepts the TCP connection but never answers the TLS ClientHello, so the new connection is still being
    // set up when the acquire timeout fires. The server is closed before the client, so the client does not wait out its
    // TLS negotiation timeout when it closes.
    @Test
    public void executeWhenNewConnectionSlowerThanAcquireTimeoutFailsWithinTimeoutBound() throws Exception {
        try (SdkAsyncHttpClient client = createSdkAsyncHttpClient(config());
             ServerSocket silentServer = new ServerSocket(0, 50, InetAddress.getLoopbackAddress())) {
            assertAcquireTimeout(sendRequest(client, URI.create("https://127.0.0.1:" + silentServer.getLocalPort())));
        }
    }

    /**
     * Asserts that the request fails within the acquire timeout bound with an {@link java.io.IOException} in its cause
     * chain, and with the expected message and cause, when given.
     */
    protected void assertAcquireTimeout(CompletableFuture<?> request) {
        Throwable failure = assertFailsWithIoExceptionWithinTimeBound(request, ACQUIRE_TIMEOUT);
        assertAcquireTimeoutFailure(failure, expectedAcquireTimeoutMessagePrefix(), expectedAcquireTimeoutCause());
    }

    private static AttributeMap config() {
        return AttributeMap.builder()
                           .put(SdkHttpConfigurationOption.PROTOCOL, Protocol.HTTP2)
                           .put(SdkHttpConfigurationOption.MAX_CONNECTIONS, 1)
                           .put(SdkHttpConfigurationOption.CONNECTION_ACQUIRE_TIMEOUT, ACQUIRE_TIMEOUT)
                           .build();
    }

    private static void awaitReceivedRequests(int count) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (server.receivedRequests() < count && System.nanoTime() < deadline) {
            Thread.sleep(5);
        }
        assertThat(server.receivedRequests()).isEqualTo(count);
    }

    private static CompletableFuture<byte[]> sendRequest(SdkAsyncHttpClient client, URI uri) {
        SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                                                       .uri(uri)
                                                       .method(SdkHttpMethod.GET)
                                                       .putHeader("Host", uri.getHost())
                                                       .build();
        return HttpTestUtils.sendRequest(client, request);
    }
}
