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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static software.amazon.awssdk.services.s3.internal.multipart.utils.MultipartDownloadTestUtils.internalErrorBody;
import static software.amazon.awssdk.services.s3.internal.multipart.utils.MultipartDownloadTestUtils.slowdownErrorBody;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

@WireMockTest
@Timeout(value = 30, unit = TimeUnit.SECONDS)
public class S3MultipartClientGetObjectRetryBehaviorWiremockTest {
    private static final String BUCKET = "Example-Bucket";
    private static final String KEY = "Key";
    private static final int MAX_ATTEMPTS = 7;
    private static final int TOTAL_PARTS = 3;
    private static final int PART_SIZE = 1024;
    private static final byte[] PART_1_DATA = new byte[PART_SIZE];
    private static final byte[] PART_2_DATA = new byte[PART_SIZE];
    private static final byte[] PART_3_DATA = new byte[PART_SIZE];
    private static byte[] expectedBody;
    private S3AsyncClient multipartClient;

    @BeforeAll
    public static void init() {
        new Random().nextBytes(PART_1_DATA);
        new Random().nextBytes(PART_2_DATA);
        new Random().nextBytes(PART_3_DATA);

        expectedBody = new byte[TOTAL_PARTS * PART_SIZE];
        System.arraycopy(PART_1_DATA, 0, expectedBody, 0, PART_SIZE);
        System.arraycopy(PART_2_DATA, 0, expectedBody, PART_SIZE, PART_SIZE);
        System.arraycopy(PART_3_DATA, 0, expectedBody, 2 * PART_SIZE, PART_SIZE);
    }

    @BeforeEach
    public void setup(WireMockRuntimeInfo wm) {
        multipartClient = S3AsyncClient.builder()
                                       .region(Region.US_EAST_1)
                                       .endpointOverride(URI.create(wm.getHttpBaseUrl()))
                                       .multipartEnabled(true)
                                       .httpClientBuilder(NettyNioAsyncHttpClient.builder().maxConcurrency(100).connectionAcquisitionTimeout(Duration.ofSeconds(100)))
                                       .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("key", "secret")))
                                       .overrideConfiguration(
                                           o -> o.retryStrategy(b -> b.maxAttempts(MAX_ATTEMPTS)))
                                       .build();
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

        int numRuns = 100;
        for (int i = 0; i < numRuns; i++) {
            CompletableFuture<ResponseBytes<GetObjectResponse>> resp = mockRetryableErrorThen200Response(multipartClient, i);
            futures.add(resp);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    @Test
    public void getObject_5xxErrorResponses_shouldNotReuseInitialErrorResponseWhenLogging() {
        String firstRequestId = UUID.randomUUID().toString();
        String secondRequestId = UUID.randomUUID().toString();
        int firstErrorStatusCode = 503;
        int secondErrorStatusCode = 500;

        stubFor(any(anyUrl())
                    .inScenario("errors")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(aResponse()
                                    .withHeader("x-amz-request-id", firstRequestId)
                                    .withStatus(firstErrorStatusCode)
                                    .withBody(slowdownErrorBody()))
                    .willSetStateTo("SecondAttempt"));

        stubFor(any(anyUrl())
                    .inScenario("errors")
                    .whenScenarioStateIs("SecondAttempt")
                    .willReturn(aResponse()
                                    .withHeader("x-amz-request-id", secondRequestId)
                                    .withStatus(secondErrorStatusCode)
                                    .withBody(internalErrorBody())));

        assertThatThrownBy(() -> multipartClient.getObject(b -> b.bucket(BUCKET).key(KEY),
                                                           AsyncResponseTransformer.toBytes()).join())
            .isInstanceOf(CompletionException.class)
            .hasCauseInstanceOf(S3Exception.class)
            .hasMessageNotContaining(firstRequestId)
            .hasMessageNotContaining(String.valueOf(firstErrorStatusCode))
            .hasMessageContaining(secondRequestId)
            .hasMessageContaining(String.valueOf(secondErrorStatusCode));

        verify(MAX_ATTEMPTS, getRequestedFor(urlEqualTo(String.format("/%s/%s?partNumber=1", BUCKET, KEY))));
        verify(0, getRequestedFor(urlEqualTo(String.format("/%s/%s?partNumber=2", BUCKET, KEY))));
        verify(0, getRequestedFor(urlEqualTo(String.format("/%s/%s?partNumber=3", BUCKET, KEY))));
    }

    @Test
    public void multipartDownload_secondPart500ResponseOnly_shouldExhaustRetriesAndFail() {
        stub200SuccessPart1();
        stubError(2, internalErrorBody());

        CompletableFuture<ResponseBytes<GetObjectResponse>> future =
            multipartClient.getObject(GetObjectRequest.builder().bucket(BUCKET).key(KEY).build(),
                                      AsyncResponseTransformer.toBytes());

        assertThatThrownBy(future::join).hasCauseInstanceOf(S3Exception.class)
                                        .hasMessageContaining("We encountered an internal error. Please try again. (Service: S3, Status Code: 500");

        verify(1, getRequestedFor(urlEqualTo(String.format("/%s/%s?partNumber=1", BUCKET, KEY))));
        verify(MAX_ATTEMPTS, getRequestedFor(urlEqualTo(String.format("/%s/%s?partNumber=2", BUCKET, KEY))));
        verify(0, getRequestedFor(urlEqualTo(String.format("/%s/%s?partNumber=3", BUCKET, KEY))));
    }

    @Test
    public void multipartDownload_503OnFirstPartAndSecondPart_shouldRetrySuccessfully() {
        // Stub Part 1 - 503 on first attempt, 200 on retry
        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=1", BUCKET, KEY)))
                    .inScenario("part1-retry")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(aResponse()
                                    .withStatus(503)
                                    .withHeader("x-amz-request-id", UUID.randomUUID().toString())
                                    .withBody(slowdownErrorBody()))
                    .willSetStateTo("retry-attempt"));

        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=1", BUCKET, KEY)))
                    .inScenario("part1-retry")
                    .whenScenarioStateIs("retry-attempt")
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withHeader("x-amz-mp-parts-count", String.valueOf(TOTAL_PARTS))
                                    .withHeader("x-amz-request-id", UUID.randomUUID().toString())
                                    .withBody(PART_1_DATA)));


        // Stub Part 2 - 503 on first attempt, 200 on retry
        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=2", BUCKET, KEY)))
                    .inScenario("part2-retry")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(aResponse()
                                    .withStatus(500)
                                    .withHeader("x-amz-request-id", UUID.randomUUID().toString())
                                    .withBody(internalErrorBody()))
                    .willSetStateTo("retry-attempt"));

        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=2", BUCKET, KEY)))
                    .inScenario("part2-retry")
                    .whenScenarioStateIs("retry-attempt")
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withHeader("x-amz-mp-parts-count", String.valueOf(TOTAL_PARTS))
                                    .withHeader("x-amz-request-id", UUID.randomUUID().toString())
                                    .withBody(PART_2_DATA)));

        stub200SuccessPart3();
        ResponseBytes<GetObjectResponse> response = multipartClient.getObject(b -> b.bucket(BUCKET).key(KEY),
                                                                              AsyncResponseTransformer.toBytes()).join();

        byte[] actualBody = response.asByteArray();
        assertArrayEquals(expectedBody, actualBody);

        // Verify that part 1 and 2 were requested twice (initial 503 + retry)
        verify(2, getRequestedFor(urlEqualTo(String.format("/%s/%s?partNumber=1", BUCKET, KEY))));
        verify(2, getRequestedFor(urlEqualTo(String.format("/%s/%s?partNumber=2", BUCKET, KEY))));
        verify(1, getRequestedFor(urlEqualTo(String.format("/%s/%s?partNumber=3", BUCKET, KEY))));
    }

    @Test
    public void multipartDownload_ioExceptionOnly_shouldExhaustRetriesAndFail() {
        stubIoError(1);
        stub200SuccessPart2();
        stub200SuccessPart3();
        assertThatThrownBy(() -> multipartClient.getObject(b -> b.bucket(BUCKET).key(KEY),
                                                           AsyncResponseTransformer.toBytes()).join())
            .isInstanceOf(CompletionException.class)
            .hasCauseInstanceOf(SdkClientException.class);

        verify(MAX_ATTEMPTS, getRequestedFor(urlEqualTo(String.format("/%s/%s?partNumber=1", BUCKET, KEY))));
        verify(0, getRequestedFor(urlEqualTo(String.format("/%s/%s?partNumber=2", BUCKET, KEY))));
        verify(0, getRequestedFor(urlEqualTo(String.format("/%s/%s?partNumber=3", BUCKET, KEY))));
    }

    @Test
    public void getObject_iOErrorThen200Response_shouldRetrySuccessfully() {
        stubFor(any(anyUrl())
                    .inScenario("io-error")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER))
                    .willSetStateTo("retry"));

        stubFor(any(anyUrl())
                    .inScenario("io-error")
                    .whenScenarioStateIs("retry")
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withHeader("x-amz-request-id", UUID.randomUUID().toString())
                                    .withBody("Hello World")));

        ResponseBytes<GetObjectResponse> response = multipartClient.getObject(GetObjectRequest.builder()
                                                                                              .bucket(BUCKET)
                                                                                              .key(KEY)
                                                                                              .build(),
                                                                              AsyncResponseTransformer.toBytes()).join();

        assertArrayEquals("Hello World".getBytes(StandardCharsets.UTF_8), response.asByteArray());

        verify(2, getRequestedFor(urlEqualTo("/" + BUCKET + "/" + KEY + "?partNumber=1")));
    }

    private void stubError(int partNumber, String errorBody) {
        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=%d", BUCKET, KEY, partNumber)))
                    .willReturn(aResponse()
                                    .withHeader("x-amz-request-id", String.valueOf(UUID.randomUUID()))
                                    .withHeader("x-amz-mp-parts-count", String.valueOf(TOTAL_PARTS))
                                    .withStatus(500).withBody(errorBody)));
    }

    private void stubIoError(int partNumber) {
        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=%d", BUCKET, KEY, partNumber)))
                    .willReturn(aResponse()
                                    .withFault(Fault.CONNECTION_RESET_BY_PEER)));
    }

    private void stub200SuccessPart1() {
        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=1", BUCKET, KEY)))
                    .willReturn(aResponse()
                                    .withHeader("x-amz-request-id", String.valueOf(UUID.randomUUID()))
                                    .withHeader("x-amz-mp-parts-count", String.valueOf(TOTAL_PARTS))
                                    .withStatus(200).withBody(PART_1_DATA)));
    }

    private void stub200SuccessPart2() {
        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=2", BUCKET, KEY)))
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withHeader("x-amz-mp-parts-count", String.valueOf(TOTAL_PARTS))
                                    .withHeader("x-amz-request-id", UUID.randomUUID().toString())
                                    .withBody(PART_2_DATA)));
    }

    private void stub200SuccessPart3() {
        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=3", BUCKET, KEY)))
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withHeader("x-amz-mp-parts-count", String.valueOf(TOTAL_PARTS))
                                    .withHeader("x-amz-request-id", UUID.randomUUID().toString())
                                    .withBody(PART_3_DATA)));
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
                                    .withStatus(500)
                                    .withBody(internalErrorBody())
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
}
