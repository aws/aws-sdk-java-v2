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

package software.amazon.awssdk.core.client.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.protocol.VoidSdkResponse;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import utils.HttpTestUtils;
import utils.ValidSdkObjects;

class AsyncClientHandlerLateAttemptResponseTest {
    private static final Duration ATTEMPT_TIMEOUT = Duration.ofSeconds(1);
    private static final String ATTEMPT_1_ETAG = "\"attempt-1\"";
    private static final String ATTEMPT_2_ETAG = "\"attempt-2\"";
    private static final int BODY_SIZE = 65_536;

    @TempDir
    Path tempDir;

    private ScheduledExecutorService timeoutExecutor;
    private LateDeliveryAsyncHttpClient httpClient;
    private SdkAsyncClientHandler clientHandler;
    private ClientExecutionParams<SdkRequest, SdkResponse> executionParams;

    @BeforeEach
    void setUp() {
        timeoutExecutor = Executors.newScheduledThreadPool(1);
        httpClient = new LateDeliveryAsyncHttpClient();

        SdkRequest request = mock(SdkRequest.class);
        when(request.overrideConfiguration()).thenReturn(Optional.empty());

        executionParams =
            new ClientExecutionParams<SdkRequest, SdkResponse>()
                .withInput(request)
                .withMarshaller(r -> ValidSdkObjects.sdkHttpFullRequest().build())
                .withResponseHandler((response, attributes) -> VoidSdkResponse.builder()
                                                                              .sdkHttpResponse(response)
                                                                              .build())
                .withErrorResponseHandler((response, attributes) -> SdkServiceException.builder().build());
    }

    @AfterEach
    void tearDown() {
        if (clientHandler != null) {
            clientHandler.close();
        }
        timeoutExecutor.shutdownNow();
    }

    @Test
    void execute_toFile_whenTimedOutAttemptRespondsAfterRetryIsSent_shouldKeepRetriedDownload() throws Exception {
        clientHandler = createClientHandler(Runnable::run);
        Path destination = tempDir.resolve("download.bin");
        byte[] attempt2Body = randomBytes(2);

        CompletableFuture<SdkResponse> result =
            clientHandler.execute(executionParams, AsyncResponseTransformer.toFile(destination));

        Attempt attempt1 = httpClient.nextAttempt();
        Attempt attempt2 = httpClient.nextAttempt();
        assertThat(attempt1.httpClientFuture).isCompletedExceptionally();

        attempt1.respond(ATTEMPT_1_ETAG, randomBytes(1));
        assertThatThrownBy(() -> result.get(300, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);

        attempt2.respond(ATTEMPT_2_ETAG, attempt2Body);
        attempt2.httpClientFuture.complete(null);

        SdkResponse response = result.get(5, TimeUnit.SECONDS);
        assertThat(eTag(response)).isEqualTo(ATTEMPT_2_ETAG);
        assertThat(destination).hasBinaryContent(attempt2Body);
    }

    /**
     * Attempt 1 writes the whole file, but times out before the SDK accepts its result, so the retry must still be able to
     * replace that file.
     */
    @Test
    void execute_toFile_whenAttemptTimesOutAfterWritingFile_shouldReplaceFileWithRetriedDownload() throws Exception {
        HoldFirstTaskExecutor completionExecutor = new HoldFirstTaskExecutor();
        clientHandler = createClientHandler(completionExecutor);
        Path destination = tempDir.resolve("download.bin");
        byte[] attempt1Body = randomBytes(1);
        byte[] attempt2Body = randomBytes(2);

        CompletableFuture<SdkResponse> result =
            clientHandler.execute(executionParams, AsyncResponseTransformer.toFile(destination));

        Attempt attempt1 = httpClient.nextAttempt();
        attempt1.respond(ATTEMPT_1_ETAG, attempt1Body);
        completionExecutor.awaitHeldTask();
        assertThat(destination).hasBinaryContent(attempt1Body);

        Attempt attempt2 = httpClient.nextAttempt();
        attempt2.respond(ATTEMPT_2_ETAG, attempt2Body);
        attempt2.httpClientFuture.complete(null);

        SdkResponse response = result.get(5, TimeUnit.SECONDS);
        assertThat(eTag(response)).isEqualTo(ATTEMPT_2_ETAG);
        assertThat(destination).hasBinaryContent(attempt2Body);
    }

    private SdkAsyncClientHandler createClientHandler(Executor futureCompletionExecutor) {
        SdkClientConfiguration config =
            HttpTestUtils.testClientConfiguration().toBuilder()
                         .option(SdkClientOption.ASYNC_HTTP_CLIENT, httpClient)
                         .option(SdkClientOption.SCHEDULED_EXECUTOR_SERVICE, timeoutExecutor)
                         .option(SdkClientOption.API_CALL_ATTEMPT_TIMEOUT, ATTEMPT_TIMEOUT)
                         .option(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR, futureCompletionExecutor)
                         .build();
        return new SdkAsyncClientHandler(config);
    }

    private static String eTag(SdkResponse response) {
        return response.sdkHttpResponse().firstMatchingHeader("ETag").orElse(null);
    }

    private static byte[] randomBytes(long seed) {
        byte[] bytes = new byte[BODY_SIZE];
        new Random(seed).nextBytes(bytes);
        return bytes;
    }

    private static final class Attempt {
        private final SdkAsyncHttpResponseHandler responseHandler;
        private final CompletableFuture<Void> httpClientFuture;

        private Attempt(SdkAsyncHttpResponseHandler responseHandler, CompletableFuture<Void> httpClientFuture) {
            this.responseHandler = responseHandler;
            this.httpClientFuture = httpClientFuture;
        }

        private void respond(String eTag, byte[] body) {
            responseHandler.onHeaders(SdkHttpFullResponse.builder()
                                                         .statusCode(200)
                                                         .putHeader("ETag", eTag)
                                                         .putHeader("Content-Length", String.valueOf(body.length))
                                                         .build());
            responseHandler.onStream(Flowable.just(ByteBuffer.wrap(body)));
        }
    }

    /**
     * Responds only when the test asks, including for attempts the SDK has already failed, as the CRT client can before it
     * closes the connection.
     */
    private static final class LateDeliveryAsyncHttpClient implements SdkAsyncHttpClient {
        private final BlockingQueue<Attempt> attempts = new LinkedBlockingQueue<>();

        @Override
        public CompletableFuture<Void> execute(AsyncExecuteRequest request) {
            CompletableFuture<Void> httpClientFuture = new CompletableFuture<>();
            attempts.add(new Attempt(request.responseHandler(), httpClientFuture));
            return httpClientFuture;
        }

        private Attempt nextAttempt() throws InterruptedException {
            Attempt attempt = attempts.poll(5, TimeUnit.SECONDS);
            assertThat(attempt).as("Expected the SDK to send another request attempt").isNotNull();
            return attempt;
        }

        @Override
        public void close() {
        }
    }

    /**
     * Never runs its first task, so the first attempt's result is not accepted before the attempt times out.
     */
    private static final class HoldFirstTaskExecutor implements Executor {
        private final CompletableFuture<Runnable> heldTask = new CompletableFuture<>();

        @Override
        public void execute(Runnable task) {
            if (!heldTask.complete(task)) {
                task.run();
            }
        }

        private void awaitHeldTask() throws Exception {
            heldTask.get(5, TimeUnit.SECONDS);
        }
    }
}
