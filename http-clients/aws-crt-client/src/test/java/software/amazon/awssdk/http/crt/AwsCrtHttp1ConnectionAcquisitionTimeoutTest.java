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
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.crt.http.HttpException;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.RecordingResponseHandler;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.utils.FunctionalUtils.UnsafeRunnable;
import software.amazon.awssdk.utils.IoUtils;

public class AwsCrtHttp1ConnectionAcquisitionTimeoutTest {
    private static final int CONNECTION_MANAGER_ACQUISITION_TIMEOUT = 2093;
    private static final Duration ACQUISITION_TIMEOUT = Duration.ofMillis(200);

    private static DeferredResponseServer h1Server;

    @BeforeAll
    static void setUp() throws InterruptedException {
        h1Server = DeferredResponseServer.start(Protocol.HTTP1_1);
    }

    @AfterAll
    static void tearDown() throws InterruptedException {
        h1Server.close();
    }

    @BeforeEach
    void resetServer() {
        h1Server.reset();
    }

    @Test
    void asyncExecute_waitingForConnectionPastAcquisitionTimeout_failsWithAcquisitionTimeout() throws Exception {
        try (SdkAsyncHttpClient client = AwsCrtAsyncHttpClient.builder()
                                                              .maxConcurrency(1)
                                                              .connectionAcquisitionTimeout(ACQUISITION_TIMEOUT)
                                                              .build()) {
            warmUpConnection(() -> execute(client).get(5, TimeUnit.SECONDS));
            CompletableFuture<Void> first = execute(client);

            CompletableFuture<Void> waiting = execute(client);

            assertThatThrownBy(() -> waiting.get(5, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .satisfies(e -> assertAcquisitionTimeout(e.getCause()));
            h1Server.releaseResponses();
            first.get(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void syncCall_waitingForConnectionPastAcquisitionTimeout_throwsAcquisitionTimeout() throws Exception {
        try (SdkHttpClient client = AwsCrtHttpClient.builder()
                                                    .maxConcurrency(1)
                                                    .connectionAcquisitionTimeout(ACQUISITION_TIMEOUT)
                                                    .build()) {
            warmUpConnection(() -> call(client));
            CompletableFuture<Void> first = CompletableFuture.runAsync(() -> call(client));
            awaitReceivedRequests(1);

            assertThatThrownBy(() -> client.prepareRequest(request()).call())
                .satisfies(AwsCrtHttp1ConnectionAcquisitionTimeoutTest::assertAcquisitionTimeout);
            h1Server.releaseResponses();
            first.get(5, TimeUnit.SECONDS);
        }
    }

    // Open the connection first so that the first request is assigned to it at once.
    private static void warmUpConnection(UnsafeRunnable request) throws Exception {
        h1Server.releaseResponses();
        request.run();
        h1Server.reset();
    }

    private static void awaitReceivedRequests(int count) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (h1Server.receivedRequests() < count && System.nanoTime() < deadline) {
            Thread.sleep(5);
        }
        assertThat(h1Server.receivedRequests()).isEqualTo(count);
    }

    private static void assertAcquisitionTimeout(Throwable failure) {
        assertThat(failure).isExactlyInstanceOf(IOException.class)
                           .hasMessageStartingWith("Acquire operation took longer than the configured maximum time.");
        assertThat(failure.getCause()).isInstanceOfSatisfying(HttpException.class, c -> assertThat(c.getErrorCode())
            .isEqualTo(CONNECTION_MANAGER_ACQUISITION_TIMEOUT));
    }

    private static CompletableFuture<Void> execute(SdkAsyncHttpClient client) {
        return client.execute(AsyncExecuteRequest.builder()
                                                 .request(createRequest(h1Server.uri()))
                                                 .requestContentPublisher(createProvider(""))
                                                 .responseHandler(new RecordingResponseHandler())
                                                 .build());
    }

    private static void call(SdkHttpClient client) {
        invokeSafely(() -> client.prepareRequest(request()).call()
                                 .responseBody()
                                 .ifPresent(body -> IoUtils.closeQuietly(body, null)));
    }

    private static HttpExecuteRequest request() {
        return HttpExecuteRequest.builder()
                                 .request(createRequest(h1Server.uri()))
                                 .contentStreamProvider(() -> new ByteArrayInputStream(new byte[0]))
                                 .build();
    }
}
