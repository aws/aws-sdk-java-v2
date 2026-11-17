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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.http.HttpTestUtils.createProvider;
import static software.amazon.awssdk.http.crt.CrtHttpClientTestUtils.createRequest;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.crt.CrtRuntimeException;
import software.amazon.awssdk.crt.http.HttpException;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.RecordingResponseHandler;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.IoUtils;

/**
 * With maxConcurrency(1) and maxPendingConnectionAcquires(1), one request holds the only connection (or stream, on HTTP/2),
 * one waits, and a third concurrent request is over the limit. The servers defer every response until the test releases them,
 * so the first two requests stay outstanding for as long as the test needs, with no fixed delay.
 */
public class AwsCrtMaxPendingConnectionAcquiresTest {
    private static final int CONNECTION_MANAGER_MAX_PENDING_ACQUISITIONS_EXCEEDED = 2094;
    private static final int STREAM_MANAGER_MAX_PENDING_ACQUISITIONS_EXCEEDED = 2096;

    private static DeferredResponseServer h1Server;
    private static DeferredResponseServer h2Server;
    private static ExecutorService executor;

    @BeforeAll
    static void setUp() throws InterruptedException {
        h1Server = DeferredResponseServer.start(Protocol.HTTP1_1);
        h2Server = DeferredResponseServer.start(Protocol.HTTP2);
        executor = Executors.newFixedThreadPool(3);
    }

    @AfterAll
    static void tearDown() throws InterruptedException {
        executor.shutdownNow();
        h1Server.close();
        h2Server.close();
    }

    @BeforeEach
    void resetServers() {
        h1Server.reset();
        h2Server.reset();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void asyncBuilderMaxPendingConnectionAcquires_nonPositive_throwsIllegalArgumentException(int value) {
        assertThatThrownBy(() -> AwsCrtAsyncHttpClient.builder().maxPendingConnectionAcquires(value))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("maxPendingConnectionAcquires");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void syncBuilderMaxPendingConnectionAcquires_nonPositive_throwsIllegalArgumentException(int value) {
        assertThatThrownBy(() -> AwsCrtHttpClient.builder().maxPendingConnectionAcquires(value))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("maxPendingConnectionAcquires");
    }

    @Test
    void builderMaxPendingConnectionAcquires_null_isAccepted() {
        assertThatCode(() -> AwsCrtAsyncHttpClient.builder().maxPendingConnectionAcquires(null)).doesNotThrowAnyException();
        assertThatCode(() -> AwsCrtHttpClient.builder().maxPendingConnectionAcquires(null)).doesNotThrowAnyException();
    }

    @Test
    void asyncExecute_overLimit_failsFutureWithHttpException() throws Exception {
        try (SdkAsyncHttpClient client = limitedAsyncClient()) {
            CompletableFuture<Void> first = execute(client, h1Server, new RecordingResponseHandler());
            CompletableFuture<Void> second = execute(client, h1Server, new RecordingResponseHandler());

            CompletableFuture<Void> rejected = execute(client, h1Server, new RecordingResponseHandler());

            assertThatThrownBy(() -> rejected.get(5, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .satisfies(e -> assertMaxPendingAcquisitionsExceeded(e.getCause()));
            h1Server.releaseResponses();
            CompletableFuture.allOf(first, second).get(5, TimeUnit.SECONDS);
        }
    }

    // Fails until awscrt.version includes awslabs/aws-crt-java#1032: aws-crt 0.50.0 and earlier ignore the limit on HTTP/2,
    // so the third request waits for a stream instead of being rejected, and the bounded get times out. With #1032 the
    // rejection completes the future before execute returns, so the short bound only limits the failing case.
    @Test
    void asyncExecuteH2_overLimit_failsFutureWithCrtRuntimeException() throws Exception {
        try (SdkAsyncHttpClient client = AwsCrtAsyncHttpClient.builder()
                                                              .protocol(Protocol.HTTP2)
                                                              .maxConcurrency(1)
                                                              .maxPendingConnectionAcquires(1)
                                                              .build()) {
            CompletableFuture<Void> first = execute(client, h2Server, new RecordingResponseHandler());
            CompletableFuture<Void> second = execute(client, h2Server, new RecordingResponseHandler());

            CompletableFuture<Void> rejected = execute(client, h2Server, new RecordingResponseHandler());

            assertThatThrownBy(() -> rejected.get(250, TimeUnit.MILLISECONDS))
                .isInstanceOf(ExecutionException.class)
                .satisfies(e -> assertThat(e.getCause())
                    .isInstanceOfSatisfying(CrtRuntimeException.class, c -> assertThat(c.errorCode)
                        .isEqualTo(STREAM_MANAGER_MAX_PENDING_ACQUISITIONS_EXCEEDED)));
            h2Server.releaseResponses();
            CompletableFuture.allOf(first, second).get(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void asyncExecute_overLimit_notifiesResponseHandlerOnError() throws Exception {
        try (SdkAsyncHttpClient client = limitedAsyncClient()) {
            CompletableFuture<Void> first = execute(client, h1Server, new RecordingResponseHandler());
            CompletableFuture<Void> second = execute(client, h1Server, new RecordingResponseHandler());
            RecordingResponseHandler rejectedHandler = new RecordingResponseHandler();

            execute(client, h1Server, rejectedHandler);

            assertThatThrownBy(() -> rejectedHandler.completeFuture().get(5, TimeUnit.SECONDS))
                .hasCauseInstanceOf(HttpException.class);
            h1Server.releaseResponses();
            CompletableFuture.allOf(first, second).get(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void asyncExecute_overLimit_doesNotSendRejectedRequest() throws Exception {
        try (SdkAsyncHttpClient client = limitedAsyncClient()) {
            CompletableFuture<Void> first = execute(client, h1Server, new RecordingResponseHandler());
            CompletableFuture<Void> second = execute(client, h1Server, new RecordingResponseHandler());
            CompletableFuture<Void> rejected = execute(client, h1Server, new RecordingResponseHandler());

            assertThatThrownBy(() -> rejected.get(5, TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class);
            h1Server.releaseResponses();
            CompletableFuture.allOf(first, second).get(5, TimeUnit.SECONDS);

            assertThat(h1Server.receivedRequests()).isEqualTo(2);
        }
    }

    @Test
    void asyncExecute_limitNotSet_queuesAllRequests() throws Exception {
        try (SdkAsyncHttpClient client = AwsCrtAsyncHttpClient.builder().maxConcurrency(1).build()) {
            assertAllSucceedAfterRelease(executeThree(client));
        }
    }

    @Test
    void asyncBuildWithDefaults_maxPendingConnectionAcquiresServiceDefault_isNotApplied() throws Exception {
        AttributeMap serviceDefaults = AttributeMap.builder()
                                                   .put(SdkHttpConfigurationOption.MAX_PENDING_CONNECTION_ACQUIRES, 1)
                                                   .build();
        try (SdkAsyncHttpClient client = AwsCrtAsyncHttpClient.builder().maxConcurrency(1).buildWithDefaults(serviceDefaults)) {
            assertAllSucceedAfterRelease(executeThree(client));
        }
    }

    @Test
    void syncCall_overLimit_throwsHttpException() throws Exception {
        try (SdkHttpClient client = AwsCrtHttpClient.builder().maxConcurrency(1).maxPendingConnectionAcquires(1).build()) {
            List<CompletableFuture<Throwable>> outcomes = startConcurrentSyncCalls(client, 3);

            // While responses are deferred, only the rejected call can finish.
            CompletableFuture.anyOf(outcomes.toArray(new CompletableFuture[0])).get(5, TimeUnit.SECONDS);
            h1Server.releaseResponses();
            List<Throwable> failures = outcomes.stream()
                                               .map(CompletableFuture::join)
                                               .filter(t -> t != null)
                                               .collect(Collectors.toList());

            assertThat(failures).hasSize(1);
            assertMaxPendingAcquisitionsExceeded(failures.get(0));
        }
    }

    private static SdkAsyncHttpClient limitedAsyncClient() {
        return AwsCrtAsyncHttpClient.builder().maxConcurrency(1).maxPendingConnectionAcquires(1).build();
    }

    private static void assertMaxPendingAcquisitionsExceeded(Throwable failure) {
        assertThat(failure).isInstanceOfSatisfying(HttpException.class, e -> assertThat(e.getErrorCode())
                               .isEqualTo(CONNECTION_MANAGER_MAX_PENDING_ACQUISITIONS_EXCEEDED));
    }

    private static List<CompletableFuture<Void>> executeThree(SdkAsyncHttpClient client) {
        return IntStream.range(0, 3)
                        .mapToObj(i -> execute(client, h1Server, new RecordingResponseHandler()))
                        .collect(Collectors.toList());
    }

    private static void assertAllSucceedAfterRelease(List<CompletableFuture<Void>> futures) {
        h1Server.releaseResponses();
        assertThatCode(() -> CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(5, TimeUnit.SECONDS))
            .doesNotThrowAnyException();
    }

    private static CompletableFuture<Void> execute(SdkAsyncHttpClient client, DeferredResponseServer server,
                                                   RecordingResponseHandler handler) {
        return client.execute(AsyncExecuteRequest.builder()
                                                 .request(createRequest(server.uri()))
                                                 .requestContentPublisher(createProvider(""))
                                                 .responseHandler(handler)
                                                 .build());
    }

    private static List<CompletableFuture<Throwable>> startConcurrentSyncCalls(SdkHttpClient client, int count) {
        HttpExecuteRequest request = HttpExecuteRequest.builder()
                                                       .request(createRequest(h1Server.uri()))
                                                       .contentStreamProvider(() -> new ByteArrayInputStream(new byte[0]))
                                                       .build();
        return IntStream.range(0, count)
                        .mapToObj(i -> CompletableFuture.runAsync(() -> callAndCloseBody(client, request), executor)
                                                        .handle((r, t) -> t instanceof CompletionException ? t.getCause() : t))
                        .collect(Collectors.toList());
    }

    private static void callAndCloseBody(SdkHttpClient client, HttpExecuteRequest request) {
        invokeSafely(() -> client.prepareRequest(request).call()
                                 .responseBody()
                                 .ifPresent(body -> IoUtils.closeQuietly(body, null)));
    }
}
