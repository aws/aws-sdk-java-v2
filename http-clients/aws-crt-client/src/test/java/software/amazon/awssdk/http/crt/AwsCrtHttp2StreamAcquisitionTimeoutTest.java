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
import static software.amazon.awssdk.http.crt.CrtHttpClientTestUtils.createRequest;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.crt.CrtRuntimeException;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.RecordingResponseHandler;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;

/**
 * On HTTP/2, connectionAcquisitionTimeout bounds both the wait for a stream when all maxConcurrency streams are in use and the
 * wait for a new connection, and both fail the same way.
 */
public class AwsCrtHttp2StreamAcquisitionTimeoutTest {
    private static final int STREAM_MANAGER_ACQUISITION_TIMEOUT = 2097;
    private static final String ACQUIRE_TIMEOUT_MESSAGE = "Acquire operation took longer than the configured maximum time.";
    private static final Duration ACQUISITION_TIMEOUT = Duration.ofMillis(200);

    private static DeferredResponseServer h2Server;

    @BeforeAll
    static void setUp() throws InterruptedException {
        h2Server = DeferredResponseServer.start(Protocol.HTTP2);
    }

    @AfterAll
    static void tearDown() throws InterruptedException {
        h2Server.close();
    }

    @BeforeEach
    void resetServer() {
        h2Server.reset();
    }

    // Fails until awscrt.version includes awslabs/aws-crt-java#1032, which adds the HTTP/2 stream acquisition timeout.
    @Test
    void asyncExecuteH2_waitingForStreamPastAcquisitionTimeout_failsWithAcquisitionTimeout() throws Exception {
        try (SdkAsyncHttpClient client = AwsCrtAsyncHttpClient.builder()
                                                              .protocol(Protocol.HTTP2)
                                                              .maxConcurrency(1)
                                                              .connectionAcquisitionTimeout(ACQUISITION_TIMEOUT)
                                                              .build()) {
            warmUpConnection(client);
            CompletableFuture<Void> first = execute(client, h2Server.uri());

            CompletableFuture<Void> waiting = execute(client, h2Server.uri());

            assertFailsWithAcquisitionTimeout(waiting);
            h2Server.releaseResponses();
            first.get(5, TimeUnit.SECONDS);
            assertThat(h2Server.receivedRequests()).isEqualTo(1);
        }
    }

    // The server accepts the TCP connection but never answers the TLS ClientHello, so the new connection is still being
    // set up when the acquisition timeout fires. The TLS negotiation timeout (10s by default) does not fire first.
    @Test
    void asyncExecuteH2_newConnectionSlowerThanAcquisitionTimeout_failsWithAcquisitionTimeout() throws Exception {
        try (ServerSocket silentServer = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
             SdkAsyncHttpClient client = AwsCrtAsyncHttpClient.builder()
                                                              .protocol(Protocol.HTTP2)
                                                              .connectionAcquisitionTimeout(ACQUISITION_TIMEOUT)
                                                              .build()) {
            CompletableFuture<Void> request = execute(client, URI.create("https://127.0.0.1:" + silentServer.getLocalPort()));

            assertFailsWithAcquisitionTimeout(request);
        }
    }

    // The first request is subject to the same short timer while it waits for a new connection, so open the connection
    // first; the first request is then assigned to the idle connection at once.
    private static void warmUpConnection(SdkAsyncHttpClient client) throws Exception {
        h2Server.releaseResponses();
        execute(client, h2Server.uri()).get(5, TimeUnit.SECONDS);
        h2Server.reset();
    }

    private static void assertFailsWithAcquisitionTimeout(CompletableFuture<Void> request) {
        assertThatThrownBy(() -> request.get(5, TimeUnit.SECONDS))
            .isInstanceOf(ExecutionException.class)
            .satisfies(e -> {
                assertThat(e.getCause()).isExactlyInstanceOf(IllegalStateException.class)
                                        .hasMessageStartingWith(ACQUIRE_TIMEOUT_MESSAGE);
                assertThat(e.getCause().getCause()).isInstanceOfSatisfying(CrtRuntimeException.class, c -> assertThat(c.errorCode)
                    .isEqualTo(STREAM_MANAGER_ACQUISITION_TIMEOUT));
            });
    }

    private static CompletableFuture<Void> execute(SdkAsyncHttpClient client, URI endpoint) {
        return client.execute(AsyncExecuteRequest.builder()
                                                 .request(createRequest(endpoint))
                                                 .requestContentPublisher(createProvider(""))
                                                 .responseHandler(new RecordingResponseHandler())
                                                 .build());
    }
}
