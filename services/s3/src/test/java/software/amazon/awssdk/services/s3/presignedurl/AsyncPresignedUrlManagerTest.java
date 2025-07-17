/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.services.s3.presignedurl;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.core.retry.conditions.RetryCondition;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.presignedurl.model.PresignedUrlGetObjectRequest;

@WireMockTest
public class AsyncPresignedUrlManagerTest {

    private S3AsyncClient s3AsyncClient;
    private AsyncPresignedUrlManager presignedUrlManager;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setup(WireMockRuntimeInfo wireMockRuntimeInfo) {
        // Configure retry policy for testing
        RetryPolicy retryPolicy = RetryPolicy.builder()
                                             .numRetries(3)
                                             .retryCondition(RetryCondition.defaultRetryCondition())
                                             .backoffStrategy(BackoffStrategy.defaultStrategy())
                                             .throttlingBackoffStrategy(BackoffStrategy.defaultThrottlingStrategy())
                                             .build();

        s3AsyncClient = S3AsyncClient.builder()
                                     .endpointOverride(URI.create(wireMockRuntimeInfo.getHttpBaseUrl()))
                                     .overrideConfiguration(ClientOverrideConfiguration.builder()
                                                                                       .retryPolicy(retryPolicy)
                                                                                       .apiCallTimeout(Duration.ofSeconds(10))
                                                                                       .apiCallAttemptTimeout(Duration.ofSeconds(2))
                                                                                       .build())
                                     .build();
        presignedUrlManager = s3AsyncClient.presignedUrlManager();
    }

    @AfterEach
    void cleanup() {
        if (s3AsyncClient != null) {
            s3AsyncClient.close();
        }
    }

    @Test
    void testGetPresignedUrlManagerFromS3AsyncClient() {
        assertThat(presignedUrlManager).isNotNull();
    }

    // Test Method 1: getObject(PresignedUrlGetObjectRequest, AsyncResponseTransformer)
    @Test
    void testGetObjectWithPresignedUrl(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        String testContent = "Hello world";
        String testETag = "\"d6eb32081c822ed572b70567826d9d9d\"";
        String presignedUrlPath = "/presigned-test-object";

        stubFor(get(urlEqualTo(presignedUrlPath))
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withHeader("ETag", testETag)
                                    .withBody(testContent)));

        URL presignedUrl = new URL(wireMockRuntimeInfo.getHttpBaseUrl() + presignedUrlPath);
        PresignedUrlGetObjectRequest request = PresignedUrlGetObjectRequest.builder()
                                                                           .presignedUrl(presignedUrl)
                                                                           .build();

        CompletableFuture<ResponseBytes<GetObjectResponse>> result =
            presignedUrlManager.getObject(request, AsyncResponseTransformer.toBytes());

        ResponseBytes<GetObjectResponse> response = result.get();
        assertThat(response).isNotNull();
        assertThat(response.asUtf8String()).isEqualTo(testContent);
        assertThat(response.response().eTag()).isEqualTo(testETag);
    }

    // Test Method 2: getObject(Consumer<Builder>, AsyncResponseTransformer)
    @Test
    void testGetObjectWithConsumerBuilder(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        String testContent = "Hello consumer";
        String testETag = "\"c1234567890abcdef1234567890abcdef\"";
        String presignedUrlPath = "/presigned-test-consumer";

        stubFor(get(urlEqualTo(presignedUrlPath))
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withHeader("ETag", testETag)
                                    .withBody(testContent)));

        URL presignedUrl = new URL(wireMockRuntimeInfo.getHttpBaseUrl() + presignedUrlPath);

        CompletableFuture<ResponseBytes<GetObjectResponse>> result =
            presignedUrlManager.getObject(
                request -> request.presignedUrl(presignedUrl),
                AsyncResponseTransformer.toBytes()
            );

        ResponseBytes<GetObjectResponse> response = result.get();
        assertThat(response).isNotNull();
        assertThat(response.asUtf8String()).isEqualTo(testContent);
        assertThat(response.response().eTag()).isEqualTo(testETag);
    }

    // Test Method 3: getObject(PresignedUrlGetObjectRequest, Path)
    @Test
    void testGetObjectToFile(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        String testContent = "File content for download";
        String testETag = "\"" + UUID.randomUUID().toString().replace("-", "") + "\"";
        String presignedUrlPath = "/presigned-test-file";

        stubFor(get(urlEqualTo(presignedUrlPath))
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withHeader("ETag", testETag)
                                    .withBody(testContent)));

        URL presignedUrl = new URL(wireMockRuntimeInfo.getHttpBaseUrl() + presignedUrlPath);
        PresignedUrlGetObjectRequest request = PresignedUrlGetObjectRequest.builder()
                                                                           .presignedUrl(presignedUrl)
                                                                           .build();

        Path downloadFile = tempDir.resolve("download-" + UUID.randomUUID() + ".txt");

        CompletableFuture<GetObjectResponse> result =
            presignedUrlManager.getObject(request, downloadFile);

        GetObjectResponse response = result.get();
        assertThat(response).isNotNull();
        assertThat(response.eTag()).isEqualTo(testETag);

        String fileContent = new String(Files.readAllBytes(downloadFile), StandardCharsets.UTF_8);
        assertThat(fileContent).isEqualTo(testContent);
    }

    // Test Method 4: getObject(Consumer<Builder>, Path)
    @Test
    void testGetObjectToFileWithConsumerBuilder(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        String testContent = "Consumer file content";
        String testETag = "\"" + UUID.randomUUID().toString().replace("-", "") + "\"";
        String presignedUrlPath = "/presigned-test-file-consumer";

        stubFor(get(urlEqualTo(presignedUrlPath))
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withHeader("ETag", testETag)
                                    .withBody(testContent)));

        URL presignedUrl = new URL(wireMockRuntimeInfo.getHttpBaseUrl() + presignedUrlPath);
        Path downloadFile = tempDir.resolve("consumer-download-" + UUID.randomUUID() + ".txt");

        CompletableFuture<GetObjectResponse> result =
            presignedUrlManager.getObject(
                request -> request.presignedUrl(presignedUrl),
                downloadFile
            );

        GetObjectResponse response = result.get();
        assertThat(response).isNotNull();
        assertThat(response.eTag()).isEqualTo(testETag);

        String fileContent = new String(Files.readAllBytes(downloadFile), StandardCharsets.UTF_8);
        assertThat(fileContent).isEqualTo(testContent);
    }

    // Test Range Requests functionality
    @ParameterizedTest(name = "Range: ''{0}'' -> ''{1}''")
    @MethodSource("rangeTestData")
    void testGetObjectWithRange(String range, String expectedContent, String contentRange, WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        String presignedUrlPath = "/presigned-test-range";
        String testETag = "\"d6eb32081c822ed572b70567826d9d9d\"";

        stubFor(get(urlEqualTo(presignedUrlPath))
                    .willReturn(aResponse()
                                    .withStatus(206)
                                    .withHeader("ETag", testETag)
                                    .withHeader("Content-Range", contentRange)
                                    .withBody(expectedContent)));

        URL presignedUrl = new URL(wireMockRuntimeInfo.getHttpBaseUrl() + presignedUrlPath);
        PresignedUrlGetObjectRequest request = PresignedUrlGetObjectRequest.builder()
                                                                           .presignedUrl(presignedUrl)
                                                                           .range(range)
                                                                           .build();

        CompletableFuture<ResponseBytes<GetObjectResponse>> result =
            presignedUrlManager.getObject(request, AsyncResponseTransformer.toBytes());

        ResponseBytes<GetObjectResponse> response = result.get();
        assertThat(response).isNotNull();
        assertThat(response.asUtf8String()).isEqualTo(expectedContent);
    }

    // Test Error Scenarios
    @ParameterizedTest(name = "Error: HTTP {0}")
    @MethodSource("errorScenarios")
    void testGetObjectErrorScenarios(int httpStatus, String errorCode, String urlPath, WireMockRuntimeInfo wireMockRuntimeInfo) {
        stubFor(get(urlEqualTo(urlPath))
                    .willReturn(aResponse()
                                    .withStatus(httpStatus)
                                    .withBody(String.format("<Error><Code>%s</Code></Error>", errorCode))));

        URL presignedUrl;
        try {
            presignedUrl = new URL(wireMockRuntimeInfo.getHttpBaseUrl() + urlPath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        PresignedUrlGetObjectRequest request = PresignedUrlGetObjectRequest.builder()
                                                                           .presignedUrl(presignedUrl)
                                                                           .build();

        CompletableFuture<ResponseBytes<GetObjectResponse>> result =
            presignedUrlManager.getObject(request, AsyncResponseTransformer.toBytes());

        assertThatThrownBy(() -> result.get())
            .isInstanceOf(ExecutionException.class);
    }

    // Retry test
    @Test
    void testRetryOnTimeout(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        String testContent = "Timeout retry success";
        String presignedUrlPath = "/presigned-timeout";

        // First call times out, second succeeds
        stubFor(get(urlEqualTo(presignedUrlPath))
                    .inScenario("Timeout Retry")
                    .whenScenarioStateIs(STARTED)
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withFixedDelay(3000)) // Longer than attempt timeout
                    .willSetStateTo("Timeout Occurred"));

        stubFor(get(urlEqualTo(presignedUrlPath))
                    .inScenario("Timeout Retry")
                    .whenScenarioStateIs("Timeout Occurred")
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withBody(testContent)));

        URL presignedUrl = new URL(wireMockRuntimeInfo.getHttpBaseUrl() + presignedUrlPath);
        PresignedUrlGetObjectRequest request = PresignedUrlGetObjectRequest.builder()
                                                                           .presignedUrl(presignedUrl)
                                                                           .build();

        CompletableFuture<ResponseBytes<GetObjectResponse>> result =
            presignedUrlManager.getObject(request, AsyncResponseTransformer.toBytes());

        ResponseBytes<GetObjectResponse> response = result.get();
        assertThat(response.asUtf8String()).isEqualTo(testContent);

        verify(exactly(2), getRequestedFor(urlEqualTo(presignedUrlPath)));
    }

    static Stream<Arguments> rangeTestData() {
        return Stream.of(
            Arguments.of("bytes=0-4", "Hello", "bytes 0-4/11"),
            Arguments.of("bytes=6-10", "world", "bytes 6-10/11"),
            Arguments.of("bytes=0-0", "H", "bytes 0-0/11"),
            Arguments.of("bytes=-5", "world", "bytes 6-10/11")
        );
    }

    static Stream<Arguments> errorScenarios() {
        return Stream.of(
            Arguments.of(404, "NoSuchKey", "/nonexistent-key"),
            Arguments.of(403, "AccessDenied", "/forbidden-key"),
            Arguments.of(409, "InvalidObjectState", "/archived-key")
        );
    }
}