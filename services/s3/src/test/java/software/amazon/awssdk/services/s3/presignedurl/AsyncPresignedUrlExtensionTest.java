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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.presignedurl.model.PresignedUrlDownloadRequest;

@WireMockTest
public class AsyncPresignedUrlExtensionTest {

    private S3AsyncClient s3AsyncClient;
    private AsyncPresignedUrlExtension presignedUrlExtension;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setup(WireMockRuntimeInfo wireMockRuntimeInfo) {
        s3AsyncClient = S3AsyncClient.builder()
                                     .endpointOverride(URI.create(wireMockRuntimeInfo.getHttpBaseUrl()))
                                     .build();
        presignedUrlExtension = s3AsyncClient.presignedUrlExtension();
    }

    @AfterEach
    void cleanup() {
        if (s3AsyncClient != null) {
            s3AsyncClient.close();
        }
    }

    @Nested
    class BasicFunctionality {
        @Test
        void givenS3AsyncClient_whenPresignedUrlExtensionRequested_thenReturnsNonNullInstance() {
            assertThat(presignedUrlExtension).isNotNull();
        }

        @Test
        void givenValidPresignedUrl_whenGetObjectCalled_thenReturnsExpectedContent(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
            String testContent = "Hello world";
            String testETag = "\"d6eb32081c822ed572b70567826d9d9d\"";
            String presignedUrlPath = "/presigned-test-object";

            stubSuccessResponse(presignedUrlPath, testContent, testETag);

            URL presignedUrl = createPresignedUrl(wireMockRuntimeInfo, presignedUrlPath);
            PresignedUrlDownloadRequest request = createRequest(presignedUrl);

            CompletableFuture<ResponseBytes<GetObjectResponse>> result =
                presignedUrlExtension.getObject(request, AsyncResponseTransformer.toBytes());

            ResponseBytes<GetObjectResponse> response = result.get();
            assertSuccessfulResponse(response, testContent, testETag);
        }

        @Test
        void givenValidPresignedUrl_whenGetObjectWithConsumerBuilderCalled_thenReturnsExpectedContent(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
            String testContent = "Hello consumer";
            String testETag = "\"c1234567890abcdef1234567890abcdef\"";
            String presignedUrlPath = "/presigned-test-consumer";

            stubSuccessResponse(presignedUrlPath, testContent, testETag);

            URL presignedUrl = createPresignedUrl(wireMockRuntimeInfo, presignedUrlPath);

            CompletableFuture<ResponseBytes<GetObjectResponse>> result =
                presignedUrlExtension.getObject(
                    request -> request.presignedUrl(presignedUrl),
                    AsyncResponseTransformer.toBytes()
                );

            ResponseBytes<GetObjectResponse> response = result.get();
            assertSuccessfulResponse(response, testContent, testETag);
        }
        
        @Test
        void givenEmptyFile_whenGetObjectCalled_thenReturnsEmptyContent(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
            String testContent = "";
            String testETag = "\"empty-file-etag\"";
            String presignedUrlPath = "/empty-file-memory";

            // Using custom stubbing for empty file to include Content-Length header
            stubFor(get(urlEqualTo(presignedUrlPath))
                        .willReturn(aResponse()
                                        .withStatus(200)
                                        .withHeader("ETag", testETag)
                                        .withHeader("Content-Length", "0")
                                        .withBody(testContent)));

            URL presignedUrl = createPresignedUrl(wireMockRuntimeInfo, presignedUrlPath);
            PresignedUrlDownloadRequest request = createRequest(presignedUrl);

            CompletableFuture<ResponseBytes<GetObjectResponse>> result =
                presignedUrlExtension.getObject(request, AsyncResponseTransformer.toBytes());
            
            ResponseBytes<GetObjectResponse> response = result.get();
            assertThat(response).isNotNull();
            assertThat(response.asByteArray()).isEmpty();
            assertThat(response.response().eTag()).isEqualTo(testETag);
        }
        
        @Test
        void givenRangeRequest_whenGetObjectCalled_thenReturnsPartialContent(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
            String expectedContent = "Hello";
            String contentRange = "bytes 0-4/11";
            String range = "bytes=0-4";
            String testETag = "\"d6eb32081c822ed572b70567826d9d9d\"";
            String presignedUrlPath = "/presigned-test-range";

            // Custom stubbing for range request with 206 status
            stubFor(get(urlEqualTo(presignedUrlPath))
                        .willReturn(aResponse()
                                        .withStatus(206)
                                        .withHeader("ETag", testETag)
                                        .withHeader("Content-Range", contentRange)
                                        .withBody(expectedContent)));

            URL presignedUrl = createPresignedUrl(wireMockRuntimeInfo, presignedUrlPath);
            PresignedUrlDownloadRequest request = PresignedUrlDownloadRequest.builder()
                                                                               .presignedUrl(presignedUrl)
                                                                               .range(range)
                                                                               .build();

            CompletableFuture<ResponseBytes<GetObjectResponse>> result =
                presignedUrlExtension.getObject(request, AsyncResponseTransformer.toBytes());

            ResponseBytes<GetObjectResponse> response = result.get();
            assertThat(response).isNotNull();
            assertThat(response.asUtf8String()).isEqualTo(expectedContent);
        }
    }

    @Nested
    class FileDownloads {
        @Test
        void givenValidPresignedUrl_whenGetObjectToFileCalled_thenDownloadsFileWithExpectedContent(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
            String testContent = "File content for download";
            String testETag = generateRandomETag();
            String presignedUrlPath = "/presigned-test-file";

            stubSuccessResponse(presignedUrlPath, testContent, testETag);

            URL presignedUrl = createPresignedUrl(wireMockRuntimeInfo, presignedUrlPath);
            PresignedUrlDownloadRequest request = createRequest(presignedUrl);

            Path downloadFile = tempDir.resolve("download-" + UUID.randomUUID() + ".txt");

            CompletableFuture<GetObjectResponse> result =
                presignedUrlExtension.getObject(request, downloadFile);

            GetObjectResponse response = result.get();
            assertThat(response).isNotNull();
            assertThat(response.eTag()).isEqualTo(testETag);

            String fileContent = new String(Files.readAllBytes(downloadFile), StandardCharsets.UTF_8);
            assertThat(fileContent).isEqualTo(testContent);
        }

        @Test
        void givenValidPresignedUrl_whenGetObjectToFileWithConsumerBuilderCalled_thenDownloadsFileWithExpectedContent(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
            String testContent = "File content for consumer download";
            String testETag = generateRandomETag();
            String presignedUrlPath = "/presigned-test-consumer-file";

            stubSuccessResponse(presignedUrlPath, testContent, testETag);

            URL presignedUrl = createPresignedUrl(wireMockRuntimeInfo, presignedUrlPath);
            Path downloadFile = tempDir.resolve("consumer-download-" + UUID.randomUUID() + ".txt");

            CompletableFuture<GetObjectResponse> result =
                presignedUrlExtension.getObject(
                    request -> request.presignedUrl(presignedUrl),
                    downloadFile
                );

            GetObjectResponse response = result.get();
            assertThat(response).isNotNull();
            assertThat(response.eTag()).isEqualTo(testETag);

            String fileContent = new String(Files.readAllBytes(downloadFile), StandardCharsets.UTF_8);
            assertThat(fileContent).isEqualTo(testContent);
        }

        @Test
        void givenEmptyFile_whenGetObjectToFileCalled_thenDownloadsEmptyFile(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
            String testContent = "";
            String testETag = "\"empty-file-etag\"";
            String presignedUrlPath = "/empty-file";

            // Custom stubbing for empty file to include Content-Length header
            stubFor(get(urlEqualTo(presignedUrlPath))
                        .willReturn(aResponse()
                                        .withStatus(200)
                                        .withHeader("ETag", testETag)
                                        .withHeader("Content-Length", "0")
                                        .withBody(testContent)));

            URL presignedUrl = createPresignedUrl(wireMockRuntimeInfo, presignedUrlPath);
            PresignedUrlDownloadRequest request = createRequest(presignedUrl);
            
            Path downloadFile = tempDir.resolve("empty-file-" + UUID.randomUUID() + ".txt");
            CompletableFuture<GetObjectResponse> result = presignedUrlExtension.getObject(request, downloadFile);
            
            GetObjectResponse response = result.get();
            assertThat(response).isNotNull();
            assertThat(response.eTag()).isEqualTo(testETag);
            assertThat(Files.size(downloadFile)).isEqualTo(0);
        }

        @Test
        void givenLargeFile_whenGetObjectCalled_thenDownloadsCompleteContent(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
            int contentSize = 1024 * 1024 + 512 * 1024; // 1.5MB
            byte[] largeContent = new byte[contentSize];
            new Random().nextBytes(largeContent);
            
            String testETag = "\"large-file-etag\"";
            String presignedUrlPath = "/large-file";

            // Custom stubbing for large file with Content-Length header
            stubFor(get(urlEqualTo(presignedUrlPath))
                        .willReturn(aResponse()
                                        .withStatus(200)
                                        .withHeader("ETag", testETag)
                                        .withHeader("Content-Length", String.valueOf(contentSize))
                                        .withBody(largeContent)));

            URL presignedUrl = createPresignedUrl(wireMockRuntimeInfo, presignedUrlPath);
            PresignedUrlDownloadRequest request = createRequest(presignedUrl);

            Path downloadFile = tempDir.resolve("large-file-" + UUID.randomUUID() + ".bin");
            CompletableFuture<GetObjectResponse> result = presignedUrlExtension.getObject(request, downloadFile);
            
            GetObjectResponse response = result.get();
            assertThat(response).isNotNull();
            assertThat(response.eTag()).isEqualTo(testETag);
            
            assertThat(Files.size(downloadFile)).isEqualTo(contentSize);
            
            byte[] downloadedContent = Files.readAllBytes(downloadFile);
            assertThat(downloadedContent).isEqualTo(largeContent);
        }
    }

    @Nested
    class RetryBehavior {
        @Test
        void givenNetworkError_whenGetObjectCalled_thenRetriesAndEventuallySucceeds(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
            String testContent = "Success after network error";
            String testETag = "\"network-retry-etag\"";
            String presignedUrlPath = "/network-error-retry";
            String scenarioName = "network-error-scenario";

            stubFor(get(urlEqualTo(presignedUrlPath))
                        .inScenario(scenarioName)
                        .whenScenarioStateIs(Scenario.STARTED)
                        .willReturn(aResponse()
                                        .withFault(Fault.CONNECTION_RESET_BY_PEER))
                        .willSetStateTo("after-network-error"));

            stubFor(get(urlEqualTo(presignedUrlPath))
                        .inScenario(scenarioName)
                        .whenScenarioStateIs("after-network-error")
                        .willReturn(aResponse()
                                        .withStatus(200)
                                        .withHeader("ETag", testETag)
                                        .withBody(testContent)));

            URL presignedUrl = createPresignedUrl(wireMockRuntimeInfo, presignedUrlPath);
            PresignedUrlDownloadRequest request = createRequest(presignedUrl);

            CompletableFuture<ResponseBytes<GetObjectResponse>> result =
                presignedUrlExtension.getObject(request, AsyncResponseTransformer.toBytes());

            ResponseBytes<GetObjectResponse> response = result.get();
            assertSuccessfulResponse(response, testContent, testETag);

            verify(exactly(2), getRequestedFor(urlEqualTo(presignedUrlPath)));
        }

        @Test
        void givenTemporaryFailure_whenGetObjectCalled_thenRetriesAndSucceeds(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
            String testContent = "Success after retry";
            String testETag = "\"retry-success-etag\"";
            String presignedUrlPath = "/retry-test";
            String scenarioName = "retry-scenario";

            stubFor(get(urlEqualTo(presignedUrlPath))
                        .inScenario(scenarioName)
                        .whenScenarioStateIs(Scenario.STARTED)
                        .willReturn(aResponse()
                                        .withStatus(503)
                                        .withBody("Service Unavailable"))
                        .willSetStateTo("retry"));

            stubFor(get(urlEqualTo(presignedUrlPath))
                        .inScenario(scenarioName)
                        .whenScenarioStateIs("retry")
                        .willReturn(aResponse()
                                        .withStatus(200)
                                        .withHeader("ETag", testETag)
                                        .withBody(testContent)));

            URL presignedUrl = createPresignedUrl(wireMockRuntimeInfo, presignedUrlPath);
            PresignedUrlDownloadRequest request = createRequest(presignedUrl);

            CompletableFuture<ResponseBytes<GetObjectResponse>> result =
                presignedUrlExtension.getObject(request, AsyncResponseTransformer.toBytes());

            ResponseBytes<GetObjectResponse> response = result.get();
            assertSuccessfulResponse(response, testContent, testETag);

            verify(exactly(2), getRequestedFor(urlEqualTo(presignedUrlPath)));
        }
    }

    @Nested
    class ErrorScenarios {
        @Test
        void givenMalformedResponse_whenGetObjectCalled_thenCompletableFutureCompletesExceptionally(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
            String presignedUrlPath = "/malformed-response";
            stubFor(get(urlEqualTo(presignedUrlPath))
                        .willReturn(aResponse()
                                        .withFault(Fault.CONNECTION_RESET_BY_PEER)));

            URL presignedUrl = createPresignedUrl(wireMockRuntimeInfo, presignedUrlPath);
            PresignedUrlDownloadRequest request = createRequest(presignedUrl);

            CompletableFuture<ResponseBytes<GetObjectResponse>> result =
                presignedUrlExtension.getObject(request, AsyncResponseTransformer.toBytes());
            try {
                result.get();
                throw new AssertionError("Expected exception was not thrown");
            } catch (ExecutionException e) {
                assertThat(e.getCause()).isInstanceOf(SdkClientException.class);
            }
        }

        @Test
        void givenNotFoundError_whenGetObjectCalled_thenCompletableFutureCompletesExceptionally(WireMockRuntimeInfo wireMockRuntimeInfo) {
            String urlPath = "/nonexistent-key";
            
            stubFor(get(urlEqualTo(urlPath))
                        .willReturn(aResponse()
                                        .withStatus(404)
                                        .withBody("<Error><Code>NoSuchKey</Code></Error>")));

            URL presignedUrl;
            try {
                presignedUrl = new URL(wireMockRuntimeInfo.getHttpBaseUrl() + urlPath);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            PresignedUrlDownloadRequest request = PresignedUrlDownloadRequest.builder()
                                                                               .presignedUrl(presignedUrl)
                                                                               .build();

            CompletableFuture<ResponseBytes<GetObjectResponse>> result =
                presignedUrlExtension.getObject(request, AsyncResponseTransformer.toBytes());

            assertThatThrownBy(() -> result.get())
                .isInstanceOf(ExecutionException.class);
        }
        
        @Test
        void givenAccessDeniedError_whenGetObjectCalled_thenCompletableFutureCompletesExceptionally(WireMockRuntimeInfo wireMockRuntimeInfo) {
            String urlPath = "/forbidden-key";
            
            stubFor(get(urlEqualTo(urlPath))
                        .willReturn(aResponse()
                                        .withStatus(403)
                                        .withBody("<Error><Code>AccessDenied</Code></Error>")));

            URL presignedUrl;
            try {
                presignedUrl = new URL(wireMockRuntimeInfo.getHttpBaseUrl() + urlPath);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            PresignedUrlDownloadRequest request = PresignedUrlDownloadRequest.builder()
                                                                               .presignedUrl(presignedUrl)
                                                                               .build();

            CompletableFuture<ResponseBytes<GetObjectResponse>> result =
                presignedUrlExtension.getObject(request, AsyncResponseTransformer.toBytes());

            assertThatThrownBy(() -> result.get())
                .isInstanceOf(ExecutionException.class);
        }
    }

    @Nested
    class ConcurrentOperations {
        @Test
        void givenMultipleRequests_whenGetObjectCalledConcurrently_thenAllDownloadsComplete(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
            int concurrentRequests = 5;
            List<String> contents = new ArrayList<>();
            List<String> eTags = new ArrayList<>();
            List<String> paths = new ArrayList<>();
            
            for (int i = 0; i < concurrentRequests; i++) {
                String content = "Content for concurrent download " + i;
                String eTag = "\"concurrent-etag-" + i + "\"";
                String path = "/concurrent-" + i;
                
                contents.add(content);
                eTags.add(eTag);
                paths.add(path);
                
                stubSuccessResponse(path, content, eTag);
            }
            
            List<CompletableFuture<ResponseBytes<GetObjectResponse>>> futures = new ArrayList<>();
            for (int i = 0; i < concurrentRequests; i++) {
                URL presignedUrl = createPresignedUrl(wireMockRuntimeInfo, paths.get(i));
                PresignedUrlDownloadRequest request = createRequest(presignedUrl);
                
                futures.add(presignedUrlExtension.getObject(request, AsyncResponseTransformer.toBytes()));
            }
            
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            allFutures.get();
            
            for (int i = 0; i < concurrentRequests; i++) {
                ResponseBytes<GetObjectResponse> response = futures.get(i).get();
                assertSuccessfulResponse(response, contents.get(i), eTags.get(i));
            }
        }
    }

    // Helper methods
    private URL createPresignedUrl(WireMockRuntimeInfo wireMockRuntimeInfo, String path) {
        try {
            return new URL(wireMockRuntimeInfo.getHttpBaseUrl() + path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private PresignedUrlDownloadRequest createRequest(URL presignedUrl) {
        return PresignedUrlDownloadRequest.builder()
                                           .presignedUrl(presignedUrl)
                                           .build();
    }

    private String generateRandomETag() {
        return "\"" + UUID.randomUUID().toString().replace("-", "") + "\"";
    }

    private void stubSuccessResponse(String path, String content, String eTag) {
        stubFor(get(urlEqualTo(path))
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withHeader("ETag", eTag)
                                    .withBody(content)));
    }

    private void stubErrorResponse(String path, int statusCode, String body) {
        stubFor(get(urlEqualTo(path))
                    .willReturn(aResponse()
                                    .withStatus(statusCode)
                                    .withBody(body)));
    }

    private void assertSuccessfulResponse(ResponseBytes<GetObjectResponse> response, String expectedContent, String expectedETag) {
        assertThat(response).isNotNull();
        assertThat(response.asUtf8String()).isEqualTo(expectedContent);
        assertThat(response.response().eTag()).isEqualTo(expectedETag);
    }

}