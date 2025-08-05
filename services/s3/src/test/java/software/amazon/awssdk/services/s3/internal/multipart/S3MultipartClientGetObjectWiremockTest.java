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

package software.amazon.awssdk.services.s3.internal.multipart;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@WireMockTest
public class S3MultipartClientGetObjectWiremockTest {
    public static final String ERROR_CODE = "InternalError";
    public static final String ERROR_MESSAGE = "We encountered an internal error. Please try again.";
    public static final String ERROR_BODY = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                                            + "<Error>\n"
                                            + "  <Code>" + ERROR_CODE + "</Code>\n"
                                            + "  <Message>" + ERROR_MESSAGE + "</Message>\n"
                                            + "</Error>";
    public static final String BUCKET = "Example-Bucket";
    public static final String KEY = "Key";
    private static final int MAX_ATTEMPTS = 7;
    private static final CapturingInterceptor capturingInterceptor = new CapturingInterceptor();

    private S3AsyncClient multipartClient;

    @BeforeEach
    public void setup(WireMockRuntimeInfo wm) {
        capturingInterceptor.clear();
        multipartClient = S3AsyncClient.builder()
                                       .region(Region.US_EAST_1)
                                       .endpointOverride(URI.create(wm.getHttpBaseUrl()))
                                       .multipartEnabled(true)
                                       .httpClientBuilder(NettyNioAsyncHttpClient.builder().maxConcurrency(100).connectionAcquisitionTimeout(Duration.ofSeconds(100)))
                                       .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("key", "secret")))
                                       .overrideConfiguration(
                                           o -> o.retryStrategy(AwsRetryStrategy.standardRetryStrategy().toBuilder()
                                                                                .maxAttempts(MAX_ATTEMPTS)
                                                                                .circuitBreakerEnabled(false)
                                                                                .build())
                                               .addExecutionInterceptor(capturingInterceptor))
                                       .build();
    }

    @Test
    public void getObject_concurrentCallsReturn200_shouldSucceed() {
        List<CompletableFuture<ResponseBytes<GetObjectResponse>>> futures = new ArrayList<>();

        int numRuns = 1000;
        for (int i = 0; i < numRuns; i++) {
            CompletableFuture<ResponseBytes<GetObjectResponse>> resp = mock200Response(multipartClient, i);
            futures.add(resp);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    @Test
    public void getObject_single500WithinMany200s_shouldRetrySuccessfully() {
        List<CompletableFuture<ResponseBytes<GetObjectResponse>>> futures = new ArrayList<>();

        int numRuns = 1000;
        for (int i = 0; i < numRuns; i++) {
            CompletableFuture<ResponseBytes<GetObjectResponse>> resp = mock200Response(multipartClient, i);
            futures.add(resp);
        }

        CompletableFuture<ResponseBytes<GetObjectResponse>> requestWithRetryableError =
            mockRetryableErrorThen200Response(multipartClient, 1);
        futures.add(requestWithRetryableError);

        for (int i = 0; i < numRuns; i++) {
            CompletableFuture<ResponseBytes<GetObjectResponse>> resp = mock200Response(multipartClient, i + 1000);
            futures.add(resp);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    @Test
    public void getObject_concurrent503s_shouldRetrySuccessfully() {
        List<CompletableFuture<ResponseBytes<GetObjectResponse>>> futures = new ArrayList<>();

        int numRuns = 1000;
        for (int i = 0; i < numRuns; i++) {
            CompletableFuture<ResponseBytes<GetObjectResponse>> resp = mockRetryableErrorThen200Response(multipartClient, i);
            futures.add(resp);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    @Test
    public void getObject_503Response_shouldNotReuseInitialRequestId() {
        String firstRequestId = UUID.randomUUID().toString();
        String secondRequestId = UUID.randomUUID().toString();

        stubFor(any(anyUrl())
                    .inScenario("errors")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(aResponse()
                                    .withHeader("x-amz-request-id", firstRequestId)
                                    .withStatus(503)
                                    .withBody(ERROR_BODY))
                    .willSetStateTo("SecondAttempt"));

        stubFor(any(anyUrl())
                    .inScenario("errors")
                    .whenScenarioStateIs("SecondAttempt")
                    .willReturn(aResponse()
                                    .withHeader("x-amz-request-id", secondRequestId)
                                    .withStatus(503)));

        assertThrows(CompletionException.class, () ->
            multipartClient.getObject(b -> b.bucket(BUCKET).key(KEY), AsyncResponseTransformer.toBytes()).join());

        List<SdkHttpResponse> responses = capturingInterceptor.getResponses();
        assertEquals(MAX_ATTEMPTS, responses.size(), () -> String.format("Expected exactly %s responses", MAX_ATTEMPTS));

        String actualFirstRequestId = responses.get(0).firstMatchingHeader("x-amz-request-id").orElse(null);
        String actualSecondRequestId = responses.get(1).firstMatchingHeader("x-amz-request-id").orElse(null);

        assertNotNull(actualFirstRequestId, "First response should have x-amz-request-id header");
        assertNotNull(actualSecondRequestId, "Second response should have x-amz-request-id header");

        assertNotEquals(actualFirstRequestId, actualSecondRequestId, "First request ID should not be reused on retry");

        assertEquals(firstRequestId, actualFirstRequestId, "First response should have expected request ID");
        assertEquals(secondRequestId, actualSecondRequestId, "Second response should have expected request ID");

        assertEquals(503, responses.get(0).statusCode());
        assertEquals(503, responses.get(1).statusCode());
    }

    @Test
    public void multipartDownload_200Response_shouldSucceed() {
        int totalParts = 3;
        int partSize = 1024;

        byte[] part1Data = new byte[partSize];
        byte[] part2Data = new byte[partSize];
        byte[] part3Data = new byte[partSize];
        new Random().nextBytes(part1Data);
        new Random().nextBytes(part2Data);
        new Random().nextBytes(part3Data);

        byte[] expectedBody = new byte[totalParts * partSize];
        System.arraycopy(part1Data, 0, expectedBody, 0, partSize);
        System.arraycopy(part2Data, 0, expectedBody, partSize, partSize);
        System.arraycopy(part3Data, 0, expectedBody, 2 * partSize, partSize);

        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=1", BUCKET, KEY)))
                    .willReturn(aResponse()
                                    .withHeader("x-amz-request-id", String.valueOf(UUID.randomUUID()))
                                    .withHeader("x-amz-mp-parts-count", String.valueOf(totalParts))
                                    .withStatus(200).withBody(part1Data)));

        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=2", BUCKET, KEY)))
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withHeader("x-amz-mp-parts-count", String.valueOf(totalParts))
                                    .withHeader("x-amz-request-id", UUID.randomUUID().toString())
                                    .withBody(part2Data)));

        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=3", BUCKET, KEY)))
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withHeader("x-amz-mp-parts-count", String.valueOf(totalParts))
                                    .withHeader("x-amz-request-id", UUID.randomUUID().toString())
                                    .withBody(part3Data)));

        CompletableFuture<ResponseBytes<GetObjectResponse>> future =
            multipartClient.getObject(GetObjectRequest.builder().bucket(BUCKET).key(KEY).build(),
                                      AsyncResponseTransformer.toBytes());

        ResponseBytes<GetObjectResponse> response = future.join();
        byte[] actualBody = response.asByteArray();
        assertArrayEquals(expectedBody, actualBody, "Downloaded body should match expected combined parts");

        // Verify that all 3 parts were requested only once
        verify(1, getRequestedFor(urlEqualTo(String.format("/%s/%s?partNumber=1", BUCKET, KEY))));
        verify(1, getRequestedFor(urlEqualTo(String.format("/%s/%s?partNumber=2", BUCKET, KEY))));
        verify(1, getRequestedFor(urlEqualTo(String.format("/%s/%s?partNumber=3", BUCKET, KEY))));
    }

    @Test
    public void multipartDownload_503OnFirstPart_shouldRetrySuccessfully() {
        int totalParts = 3;
        int partSize = 1024;

        byte[] part1Data = new byte[partSize];
        byte[] part2Data = new byte[partSize];
        byte[] part3Data = new byte[partSize];
        new Random().nextBytes(part1Data);
        new Random().nextBytes(part2Data);
        new Random().nextBytes(part3Data);

        byte[] expectedBody = new byte[totalParts * partSize];
        System.arraycopy(part1Data, 0, expectedBody, 0, partSize);
        System.arraycopy(part2Data, 0, expectedBody, partSize, partSize);
        System.arraycopy(part3Data, 0, expectedBody, 2 * partSize, partSize);


        // Stub Part 1 - 503 on first attempt, 200 on retry
        String part1Scenario = "part1-retry";
        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=1", BUCKET, KEY)))
                    .inScenario(part1Scenario)
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(aResponse()
                                    .withStatus(503)
                                    .withHeader("x-amz-request-id", UUID.randomUUID().toString())
                                    .withBody("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                              "<Error>\n" +
                                              "  <Code>SlowDown</Code>\n" +
                                              "  <Message>Please reduce your request rate.</Message>\n" +
                                              "</Error>"))
                    .willSetStateTo("retry-attempt"));

        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=1", BUCKET, KEY)))
                    .inScenario(part1Scenario)
                    .whenScenarioStateIs("retry-attempt")
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withHeader("x-amz-mp-parts-count", String.valueOf(totalParts))
                                    .withHeader("x-amz-request-id", UUID.randomUUID().toString())
                                    .withBody(part1Data)));

        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=2", BUCKET, KEY)))
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withHeader("x-amz-mp-parts-count", String.valueOf(totalParts))
                                    .withHeader("x-amz-request-id", UUID.randomUUID().toString())
                                    .withBody(part2Data)));

        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=3", BUCKET, KEY)))
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withHeader("x-amz-mp-parts-count", String.valueOf(totalParts))
                                    .withHeader("x-amz-request-id", UUID.randomUUID().toString())
                                    .withBody(part3Data)));

        CompletableFuture<ResponseBytes<GetObjectResponse>> future =
            multipartClient.getObject(GetObjectRequest.builder().bucket(BUCKET).key(KEY).build(),
                                      AsyncResponseTransformer.toBytes());

        ResponseBytes<GetObjectResponse> response = future.join();
        byte[] actualBody = response.asByteArray();
        assertArrayEquals(expectedBody, actualBody, "Downloaded body should match expected combined parts");

        // Verify that part 1 was requested twice (initial 503 + retry)
        verify(2, getRequestedFor(urlEqualTo(String.format("/%s/%s?partNumber=1", BUCKET, KEY))));
        verify(1, getRequestedFor(urlEqualTo(String.format("/%s/%s?partNumber=2", BUCKET, KEY))));
        verify(1, getRequestedFor(urlEqualTo(String.format("/%s/%s?partNumber=3", BUCKET, KEY))));
    }

    @Test
    public void getObject_iOError_shouldRetrySuccessfully() {
        String requestId = UUID.randomUUID().toString();

        stubFor(any(anyUrl())
                    .inScenario("io-error")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(aResponse()
                                    .withFault(Fault.CONNECTION_RESET_BY_PEER))
                    .willSetStateTo("retry"));

        stubFor(any(anyUrl())
                    .inScenario("io-error")
                    .whenScenarioStateIs("retry")
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withHeader("x-amz-request-id", requestId)
                                    .withBody("Hello World")));

        ResponseBytes<GetObjectResponse> response = multipartClient.getObject(GetObjectRequest.builder()
                                                                                              .bucket(BUCKET)
                                                                                              .key(KEY)
                                                                                              .build(),
                                                                              AsyncResponseTransformer.toBytes()).join();

        assertArrayEquals("Hello World".getBytes(StandardCharsets.UTF_8), response.asByteArray());

        verify(2, getRequestedFor(urlEqualTo("/" + BUCKET + "/" + KEY + "?partNumber=1")));

        List<SdkHttpResponse> responses = capturingInterceptor.getResponses();
        String finalRequestId = responses.get(responses.size() - 1)
                                         .firstMatchingHeader("x-amz-request-id")
                                         .orElse(null);

        assertEquals(requestId, finalRequestId);
    }

    @Test
    public void multipartDownload_errorDuringFirstPartAfterOnStream_shouldFailAndNotRetry() {
        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=1", BUCKET, KEY)))
                    .willReturn(aResponse()
                                    .withHeader("x-amz-mp-parts-count", String.valueOf(2))
                                    .withStatus(200)
                                    .withBody("Hello ")));

        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=2", BUCKET, KEY)))
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withHeader("x-amz-mp-parts-count", "2")
                                    .withBody("World")));

        StreamingErrorTransformer failingTransformer = new StreamingErrorTransformer();
        assertThrows(CompletionException.class, () ->
            multipartClient.getObject(b -> b.bucket(BUCKET).key(KEY), failingTransformer).join());

        assertTrue(failingTransformer.onStreamCalled.get());
        // Verify that the first part was requested only once and not retried
        verify(1, getRequestedFor(urlEqualTo(String.format("/%s/%s?partNumber=1", BUCKET, KEY))));
    }

    /**
     * Custom AsyncResponseTransformer that simulates an error occurring after onStream() has been called
     */
    private static final class StreamingErrorTransformer
        implements AsyncResponseTransformer<GetObjectResponse, ResponseBytes<GetObjectResponse>> {

        private final CompletableFuture<ResponseBytes<GetObjectResponse>> future = new CompletableFuture<>();
        private final AtomicBoolean errorThrown = new AtomicBoolean();
        private final AtomicBoolean onStreamCalled = new AtomicBoolean();

        @Override
        public CompletableFuture<ResponseBytes<GetObjectResponse>> prepare() {
            return future;
        }

        @Override
        public void onResponse(GetObjectResponse response) {
            //
        }

        @Override
        public void onStream(SdkPublisher<ByteBuffer> publisher) {
            onStreamCalled.set(true);
            publisher.subscribe(new Subscriber<ByteBuffer>() {
                private Subscription subscription;

                @Override
                public void onSubscribe(Subscription s) {
                    this.subscription = s;
                    s.request(1);
                }

                @Override
                public void onNext(ByteBuffer byteBuffer) {
                    if (errorThrown.compareAndSet(false, true)) {
                        future.completeExceptionally(new RuntimeException());
                        subscription.cancel();
                    }
                }

                @Override
                public void onError(Throwable t) {
                    future.completeExceptionally(t);
                }

                @Override
                public void onComplete() {
                    //
                }
            });
        }

        @Override
        public void exceptionOccurred(Throwable throwable) {
            future.completeExceptionally(throwable);
        }
    }

    private CompletableFuture<ResponseBytes<GetObjectResponse>> mock200Response(S3AsyncClient s3Client, int runNumber) {
        String runId = runNumber + " success";

        stubFor(any(anyUrl())
                    .withHeader("RunNum", matching(runId))
                    .inScenario(runId)
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(aResponse().withStatus(200)
                                           .withHeader("x-amz-request-id", String.valueOf(UUID.randomUUID()))
                                           .withBody("Hello World")));

        return s3Client.getObject(r -> r.bucket(BUCKET).key("key")
                                        .overrideConfiguration(c -> c.putHeader("RunNum", runId)),
                                  AsyncResponseTransformer.toBytes());
    }

    private CompletableFuture<ResponseBytes<GetObjectResponse>> mockRetryableErrorThen200Response(S3AsyncClient s3Client, int runNumber) {
        String runId = String.valueOf(runNumber);

        stubFor(any(anyUrl())
                    .withHeader("RunNum", matching(runId))
                    .inScenario(runId)
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(aResponse()
                                    .withHeader("x-amz-request-id", String.valueOf(UUID.randomUUID()))
                                    .withStatus(500).withBody(ERROR_BODY)
                    )
                    .willSetStateTo("SecondAttempt" + runId));

        stubFor(any(anyUrl())
                    .inScenario(runId)
                    .withHeader("RunNum", matching(runId))
                    .whenScenarioStateIs("SecondAttempt" + runId)
                    .willReturn(aResponse().withStatus(200)
                                           .withHeader("x-amz-request-id", String.valueOf(UUID.randomUUID()))
                                           .withBody("Hello World")));

        return s3Client.getObject(r -> r.bucket(BUCKET).key("key")
                                        .overrideConfiguration(c -> c.putHeader("RunNum", runId)),
                                  AsyncResponseTransformer.toBytes());
    }

    static class CapturingInterceptor implements ExecutionInterceptor {
        private final List<SdkHttpResponse> responses = new ArrayList<>();

        @Override
        public void afterTransmission(Context.AfterTransmission context, ExecutionAttributes executionAttributes) {
            responses.add(context.httpResponse());
        }

        public List<SdkHttpResponse> getResponses() {
            return new ArrayList<>(responses);
        }

        public void clear() {
            responses.clear();
        }
    }
}
