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
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.multipart.MultipartConfiguration;
import software.amazon.awssdk.services.s3.presignedurl.model.PresignedUrlDownloadRequest;

@WireMockTest
class PresignedUrlMultipartDownloaderSubscriberWiremockTest {

    private static final String PRESIGNED_URL_PATH = "/presigned-url";
    private static final byte[] TEST_DATA = "This is exactly a 32 byte string".getBytes(StandardCharsets.UTF_8);

    private S3AsyncClient s3AsyncClient;
    private String presignedUrlBase;
    private URL presignedUrl;
    private Path tempFile;

    @BeforeEach
    public void setup(WireMockRuntimeInfo wiremock) throws MalformedURLException {
        MultipartConfiguration multipartConfig = MultipartConfiguration.builder()
                                                                       .minimumPartSizeInBytes(16L)
                                                                       .build();
        s3AsyncClient = S3AsyncClient.builder()
                                     .endpointOverride(URI.create("http://localhost:" + wiremock.getHttpPort()))
                                     .multipartEnabled(true)
                                     .multipartConfiguration(multipartConfig)
                                     .build();
        presignedUrlBase = "http://localhost:" + wiremock.getHttpPort();
        presignedUrl = createPresignedUrl();
    }

    @Test
    void presignedUrlDownload_withMultipartData_shouldReceiveCompleteBody() {
        stubSuccessfulPresignedUrlResponse();
        byte[] result = s3AsyncClient.presignedUrlExtension()
                                     .getObject(PresignedUrlDownloadRequest.builder()
                                                                           .presignedUrl(presignedUrl)
                                                                           .build(),
                                                AsyncResponseTransformer.toBytes())
                                     .join()
                                     .asByteArray();
        assertArrayEquals(TEST_DATA, result);
    }

    @Test
    void presignedUrlDownload_withRangeHeader_shouldReceivePartialContent() {
        stubSuccessfulRangeResponse();
        byte[] result = s3AsyncClient.presignedUrlExtension()
                                     .getObject(PresignedUrlDownloadRequest.builder()
                                                                           .presignedUrl(presignedUrl)
                                                                           .range("bytes=0-10")
                                                                           .build(),
                                                AsyncResponseTransformer.toBytes())
                                     .join()
                                     .asByteArray();
        byte[] expectedPartial = Arrays.copyOfRange(TEST_DATA, 0, 11);
        assertArrayEquals(expectedPartial, result);
    }

    @Test
    void presignedUrlDownload_whenRequestFails_shouldThrowException() {
        stubFailedPresignedUrlResponse();
        assertThatThrownBy(() -> s3AsyncClient.presignedUrlExtension()
                                              .getObject(PresignedUrlDownloadRequest.builder()
                                                                                    .presignedUrl(presignedUrl)
                                                                                    .build(),
                                                         AsyncResponseTransformer.toBytes())
                                              .join())
            .hasRootCauseInstanceOf(S3Exception.class);
    }

    @Test
    void presignedUrlDownload_withFileTransformer_shouldWork() throws IOException {
        stubSuccessfulPresignedUrlResponse();
        tempFile = createUniqueTempFile();
        s3AsyncClient.presignedUrlExtension()
                     .getObject(PresignedUrlDownloadRequest.builder()
                                                           .presignedUrl(presignedUrl)
                                                           .build(),
                                AsyncResponseTransformer.toFile(tempFile))
                     .join();
        assertThat(tempFile.toFile()).exists();
        assertThat(tempFile.toFile().length()).isGreaterThan(0);
    }

    @Test
    void presignedUrlDownload_whenFirstRequestFails_shouldThrowException() {
        stubInternalServerError();
        assertThatThrownBy(() -> s3AsyncClient.presignedUrlExtension()
                                              .getObject(PresignedUrlDownloadRequest.builder()
                                                                                    .presignedUrl(presignedUrl)
                                                                                    .build(),
                                                         AsyncResponseTransformer.toBytes())
                                              .join())
            .hasRootCauseInstanceOf(S3Exception.class);
    }

    @Test
    void presignedUrlDownload_whenSecondRequestFails_shouldThrowException() {
        stubPartialFailureScenario();
        assertThatThrownBy(() -> s3AsyncClient.presignedUrlExtension()
                                              .getObject(PresignedUrlDownloadRequest.builder()
                                                                                    .presignedUrl(presignedUrl)
                                                                                    .build(),
                                                         AsyncResponseTransformer.toBytes())
                                              .join())
            .hasRootCauseInstanceOf(S3Exception.class);
    }

    @Test
    void presignedUrlDownload_whenIOErrorOccurs_shouldThrowException() {
        stubConnectionReset();
        assertThatThrownBy(() -> s3AsyncClient.presignedUrlExtension()
                                              .getObject(PresignedUrlDownloadRequest.builder()
                                                                                    .presignedUrl(presignedUrl)
                                                                                    .build(),
                                                         AsyncResponseTransformer.toBytes())
                                              .join())
            .hasCauseInstanceOf(IOException.class);
    }


    @Test
    void onNext_withNullTransformer_shouldThrowException() {
        PresignedUrlMultipartDownloaderSubscriber subscriber = createTestSubscriber();

        assertThatThrownBy(() -> subscriber.onNext(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("onNext must not be called with null asyncResponseTransformer");
    }

    @Test
    void presignedUrlDownload_withMissingContentRange_shouldFailRequest() {
        stubResponseWithMissingContentRange();
        assertThatThrownBy(() -> s3AsyncClient.presignedUrlExtension()
                                              .getObject(PresignedUrlDownloadRequest.builder()
                                                                                    .presignedUrl(presignedUrl)
                                                                                    .build(),
                                                         AsyncResponseTransformer.toBytes())
                                              .join())
            .hasRootCauseInstanceOf(SdkClientException.class)
            .hasMessageContaining("No Content-Range header in response");
    }

    @Test
    void presignedUrlDownload_withInvalidContentLength_shouldFailRequest() {
        stubResponseWithInvalidContentLength();
        assertThatThrownBy(() -> s3AsyncClient.presignedUrlExtension()
                                              .getObject(PresignedUrlDownloadRequest.builder()
                                                                                    .presignedUrl(presignedUrl)
                                                                                    .build(),
                                                         AsyncResponseTransformer.toBytes())
                                              .join())
            .hasRootCauseInstanceOf(SdkClientException.class)
            .hasMessageContaining("Invalid or missing Content-Length in response");
    }

    @Test
    void presignedUrlDownload_withContentRangeMismatch_shouldFailRequest() {
        stubResponseWithContentRangeMismatch();
        assertThatThrownBy(() -> s3AsyncClient.presignedUrlExtension()
                                              .getObject(PresignedUrlDownloadRequest.builder()
                                                                                    .presignedUrl(presignedUrl)
                                                                                    .build(),
                                                         AsyncResponseTransformer.toBytes())
                                              .join())
            .hasRootCauseInstanceOf(SdkClientException.class)
            .hasMessageContaining("Content-Range mismatch");
    }

    @Test
    void presignedUrlDownload_withContentLengthMismatch_shouldFailRequest() {
        stubResponseWithContentLengthMismatch();
        assertThatThrownBy(() -> s3AsyncClient.presignedUrlExtension()
                                              .getObject(PresignedUrlDownloadRequest.builder()
                                                                                    .presignedUrl(presignedUrl)
                                                                                    .build(),
                                                        AsyncResponseTransformer.toBytes())
                                              .join())
            .hasRootCauseInstanceOf(SdkClientException.class)
            .hasMessageContaining("Part content length validation failed");
    }

    @AfterEach
    void cleanup() {
        if (tempFile != null && Files.exists(tempFile)) {
            try {
                Files.delete(tempFile);
            } catch (IOException e) {
            }
        }
    }

    private static Path createUniqueTempFile() throws IOException {
        String uniqueName = "test-" + UUID.randomUUID().toString();
        Path tempFile = Files.createTempFile(uniqueName, ".tmp");
        Files.deleteIfExists(tempFile);
        return tempFile;
    }

    private void stubSuccessfulPresignedUrlResponse() {
        // Stub for first part (bytes 0-15)
        stubFor(get(urlEqualTo(PRESIGNED_URL_PATH))
                    .withHeader("Range", matching("bytes=0-15"))
                    .willReturn(aResponse()
                                    .withStatus(206)
                                    .withHeader("Content-Type", "application/octet-stream")
                                    .withHeader("Content-Length", "16")
                                    .withHeader("Content-Range", "bytes 0-15/32")
                                    .withHeader("ETag", "\"test-etag\"")
                                    .withBody(Arrays.copyOfRange(TEST_DATA, 0, 16))));
        
        // Stub for second part (bytes 16-31)
        stubFor(get(urlEqualTo(PRESIGNED_URL_PATH))
                    .withHeader("Range", matching("bytes=16-31"))
                    .willReturn(aResponse()
                                    .withStatus(206)
                                    .withHeader("Content-Type", "application/octet-stream")
                                    .withHeader("Content-Length", "16")
                                    .withHeader("Content-Range", "bytes 16-31/32")
                                    .withHeader("ETag", "\"test-etag\"")
                                    .withBody(Arrays.copyOfRange(TEST_DATA, 16, 32))));
    }

    private void stubSuccessfulRangeResponse() {
        byte[] partialData = Arrays.copyOfRange(TEST_DATA, 0, 11);
        stubFor(get(urlEqualTo(PRESIGNED_URL_PATH))
                    .willReturn(aResponse()
                                    .withStatus(206)
                                    .withHeader("Content-Type", "application/octet-stream")
                                    .withHeader("Content-Length", String.valueOf(partialData.length))
                                    .withHeader("Content-Range", "bytes 0-10/" + TEST_DATA.length)
                                    .withHeader("ETag", "\"test-etag\"")
                                    .withBody(partialData)));
    }

    private void stubFailedPresignedUrlResponse() {
        stubFor(get(urlEqualTo(PRESIGNED_URL_PATH))
                    .willReturn(aResponse()
                                    .withStatus(404)
                                    .withBody("<Error><Code>NoSuchKey</Code><Message>The specified key does not exist.</Message></Error>")));
    }

    private void stubInternalServerError() {
        stubFor(get(urlEqualTo(PRESIGNED_URL_PATH))
                    .willReturn(aResponse()
                                    .withStatus(500)
                                    .withBody("<Error><Code>InternalError</Code><Message>Internal Server Error</Message></Error>")));
    }

    private void stubPartialFailureScenario() {
        stubFor(get(urlEqualTo(PRESIGNED_URL_PATH))
                    .inScenario("partial-failure")
                    .whenScenarioStateIs("Started")
                    .willReturn(aResponse()
                                    .withStatus(206)
                                    .withHeader("Content-Type", "application/octet-stream")
                                    .withHeader("Content-Length", "16")
                                    .withHeader("Content-Range", "bytes 0-15/" + TEST_DATA.length)
                                    .withHeader("ETag", "\"test-etag\"")
                                    .withBody(Arrays.copyOfRange(TEST_DATA, 0, 16)))
                    .willSetStateTo("first-success"));

        stubFor(get(urlEqualTo(PRESIGNED_URL_PATH))
                    .inScenario("partial-failure")
                    .whenScenarioStateIs("first-success")
                    .willReturn(aResponse()
                                    .withStatus(500)
                                    .withBody("<Error><Code>InternalError</Code><Message>Second request failed</Message></Error>")));
    }

    private void stubConnectionReset() {
        stubFor(get(urlEqualTo(PRESIGNED_URL_PATH))
                    .willReturn(aResponse()
                                    .withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)));
    }

    private URL createPresignedUrl() throws MalformedURLException {
        return new URL(presignedUrlBase + PRESIGNED_URL_PATH);
    }

    private PresignedUrlMultipartDownloaderSubscriber createTestSubscriber() {
        return new PresignedUrlMultipartDownloaderSubscriber(
            s3AsyncClient,
            PresignedUrlDownloadRequest.builder().presignedUrl(presignedUrl).build(),
            1024L);
    }

    private void stubResponseWithMissingContentRange() {
        stubFor(get(urlEqualTo(PRESIGNED_URL_PATH))
                    .willReturn(aResponse()
                                    .withStatus(206)
                                    .withHeader("Content-Type", "application/octet-stream")
                                    .withHeader("Content-Length", "16")
                                    .withHeader("ETag", "\"test-etag\"")
                                    .withBody(Arrays.copyOfRange(TEST_DATA, 0, 16))));
    }

    private void stubResponseWithInvalidContentLength() {
        stubFor(get(urlEqualTo(PRESIGNED_URL_PATH))
                    .willReturn(aResponse()
                                    .withStatus(206)
                                    .withHeader("Content-Type", "application/octet-stream")
                                    .withHeader("Content-Range", "bytes 0-15/" + TEST_DATA.length)
                                    .withHeader("ETag", "\"test-etag\"")
                                    .withBody(Arrays.copyOfRange(TEST_DATA, 0, 16))));
    }

    private void stubResponseWithContentRangeMismatch() {
        stubFor(get(urlEqualTo(PRESIGNED_URL_PATH))
                    .willReturn(aResponse()
                                    .withStatus(206)
                                    .withHeader("Content-Type", "application/octet-stream")
                                    .withHeader("Content-Length", "16")
                                    .withHeader("Content-Range", "bytes 5000-5015/" + TEST_DATA.length)
                                    .withHeader("ETag", "\"test-etag\"")
                                    .withBody(Arrays.copyOfRange(TEST_DATA, 0, 16))));
    }

    private void stubResponseWithContentLengthMismatch() {
        stubFor(get(urlEqualTo(PRESIGNED_URL_PATH))
                    .willReturn(aResponse()
                                    .withStatus(206)
                                    .withHeader("Content-Type", "application/octet-stream")
                                    .withHeader("Content-Length", "8")
                                    .withHeader("Content-Range", "bytes 0-15/" + TEST_DATA.length)
                                    .withHeader("ETag", "\"test-etag\"")
                                    .withBody(Arrays.copyOfRange(TEST_DATA, 0, 8))));
    }
}