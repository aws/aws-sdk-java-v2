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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import software.amazon.awssdk.utils.Logger;

/**
 * Shared helpers for the long-running request test suites.
 */
public final class LongRunningRequestTestSupport {

    public static final Duration CONFIGURED_TIMEOUT = Duration.ofSeconds(2);
    public static final Duration SERVER_DELAY = Duration.ofSeconds(10);
    public static final Duration TIME_BOUND_SAFETY_MARGIN = Duration.ofSeconds(10);
    public static final Duration HANG_DELAY = Duration.ofMinutes(1);

    private static final Logger log = Logger.loggerFor(LongRunningRequestTestSupport.class);

    private LongRunningRequestTestSupport() {
    }

    /**
     * Simulates long-polling and synchronous-invoke-with-extended-wait (SFN getActivityTask, SQS receiveMessage,
     * Lambda invoke): server delays the entire response.
     */
    public static void stubLongPolling(WireMockExtension mockServer) {
        mockServer.stubFor(any(urlPathEqualTo("/"))
                               .willReturn(aResponse().withStatus(200)
                                                      .withBody("hello")
                                                      .withFixedDelay((int) SERVER_DELAY.toMillis())));
    }

    /**
     * Simulates streaming APIs (Transcribe Streaming, Kinesis subscribeToShard): server dribbles body chunks with
     * gaps between them.
     */
    public static void stubStreamingWithPauses(WireMockExtension mockServer) {
        mockServer.stubFor(any(urlPathEqualTo("/"))
                               .willReturn(aResponse().withStatus(200)
                                                      .withBody("hello")
                                                      .withChunkedDribbleDelay(2, (int) SERVER_DELAY.toMillis())));
    }

    /**
     * Hangs indefinitely so a holding request can exhaust a single-slot pool while another racing request
     * exercises the acquire timeout.
     */
    public static void stubHanging(WireMockExtension mockServer) {
        mockServer.stubFor(any(urlPathEqualTo("/"))
                               .willReturn(aResponse().withStatus(200)
                                                      .withBody("hello")
                                                      .withFixedDelay((int) HANG_DELAY.toMillis())));
    }

    /**
     * Async-executes a POST against {@code mockServer} on a worker thread. A per-call random
     * {@code testReqId} is logged BEFORE the request is dispatched and propagated to the SDK via the
     * {@code x-aws-sdk-test-id} header so the SDK can prefix its lifecycle logs with the same id.
     */
    public static TestRequestExecution executeAsync(SdkHttpClient client, WireMockExtension mockServer) {
        String testReqId = "test-" + String.format("%08x", ThreadLocalRandom.current().nextInt());
        log.info(() -> "TEST REQUEST ID: " + testReqId + " (dispatching)");
        CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
            executeRequest(client, mockServer, testReqId);
            return null;
        });
        return new TestRequestExecution(testReqId, future);
    }

    private static void executeRequest(SdkHttpClient client, WireMockExtension mockServer, String testReqId) {
        URI uri = URI.create("http://localhost:" + mockServer.getPort());
        SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                                                       .uri(uri)
                                                       .method(SdkHttpMethod.POST)
                                                       .putHeader("Host", uri.getHost())
                                                       .putHeader("Content-Length", "4")
                                                       .putHeader("x-aws-sdk-test-id", testReqId)
                                                       .contentStreamProvider(() -> new ByteArrayInputStream(
                                                           "Body".getBytes(StandardCharsets.UTF_8)))
                                                       .build();
        try {
            HttpExecuteResponse response = client.prepareRequest(HttpExecuteRequest.builder()
                                                                                   .request(request)
                                                                                   .contentStreamProvider(
                                                                                       request.contentStreamProvider()
                                                                                              .orElse(null))
                                                                                   .build())
                                                 .call();
            response.responseBody().ifPresent(body -> {
                try {
                    while (body.read() != -1) {
                        // drain body so mid-body timeouts surface
                    }
                    body.close();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void assertFailsWithinTimeBound(TestRequestExecution execution, Duration expectedTimeout) {
        assertFailsWithinTimeBound(execution.future(), execution.testReqId(), expectedTimeout);
    }

    public static void assertFailsWithinTimeBound(CompletableFuture<?> future, Duration expectedTimeout) {
        assertFailsWithinTimeBound(future, "(unknown)", expectedTimeout);
    }

    private static void assertFailsWithinTimeBound(CompletableFuture<?> future, String testReqId, Duration expectedTimeout) {
        Duration maxWait = expectedTimeout.plus(TIME_BOUND_SAFETY_MARGIN);

        try {
            future.get(maxWait.toMillis(), TimeUnit.MILLISECONDS);
            throw new AssertionError("Expected request " + testReqId
                                     + " to throw an exception but it completed successfully");
        } catch (TimeoutException e) {
            // Bookend the thread dump with the test reqId so surefire output can be grep'd by request.
            log.error(() -> "TEST REQUEST ID: " + testReqId + " (timed out, dumping threads)");
            log.error(() -> dumpAllThreads());
            future.cancel(true);
            throw new AssertionError(
                "Expected request " + testReqId + " to fail within " + maxWait
                + " but it was still running - client appears to hang",
                e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Unexpected interruption while waiting for request " + testReqId + " to fail", e);
        } catch (ExecutionException e) {
            // expected
        }
    }

    /**
     * Bundles a worker future with the per-call test reqId so the assertion helper can reference it in
     * failure messages.
     */
    public static final class TestRequestExecution {
        private final String testReqId;
        private final CompletableFuture<Void> future;

        TestRequestExecution(String testReqId, CompletableFuture<Void> future) {
            this.testReqId = testReqId;
            this.future = future;
        }

        public String testReqId() {
            return testReqId;
        }

        public CompletableFuture<Void> future() {
            return future;
        }
    }

    static String dumpAllThreads() {
        ThreadMXBean tmx = ManagementFactory.getThreadMXBean();
        ThreadInfo[] infos = tmx.dumpAllThreads(true, true);
        StringBuilder sb = new StringBuilder("=== THREAD DUMP ===\n");
        for (ThreadInfo info : infos) {
            sb.append(info.toString()).append('\n');
        }
        sb.append("=== END THREAD DUMP ===\n");
        return sb.toString();
    }
}
