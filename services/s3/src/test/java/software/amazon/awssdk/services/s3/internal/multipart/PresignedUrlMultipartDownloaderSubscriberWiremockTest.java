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
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
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
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
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

    static Stream<Arguments> transformerTypes() {
        return Stream.of(
            Arguments.of("toBytes"),
            Arguments.of("toFile")
        );
    }

    private CompletableFuture<?> executeDownload(PresignedUrlDownloadRequest request, String transformerType)
        throws IOException {
        if ("toFile".equals(transformerType)) {
            tempFile = createUniqueTempFile();
            return s3AsyncClient.presignedUrlExtension().getObject(request, AsyncResponseTransformer.toFile(tempFile));
        }
        return s3AsyncClient.presignedUrlExtension().getObject(request, AsyncResponseTransformer.toBytes());
    }

    @SuppressWarnings("unchecked")
    private void assertSuccessfulDownload(String type, Object result) throws IOException {
        if ("toBytes".equals(type)) {
            assertArrayEquals(TEST_DATA, ((ResponseBytes<GetObjectResponse>) result).asByteArray());
        } else {
            assertThat(tempFile.toFile()).exists();
            byte[] fileContent = Files.readAllBytes(tempFile);
            assertArrayEquals(TEST_DATA, fileContent);
        }
    }

    @ParameterizedTest(name = "presignedUrlDownload_withMultipartData_shouldReceiveCompleteBody [{0}]")
    @MethodSource("transformerTypes")
    void presignedUrlDownload_withMultipartData_shouldReceiveCompleteBody(String transformerType) throws IOException {
        stubSuccessfulPresignedUrlResponse();
        PresignedUrlDownloadRequest request = PresignedUrlDownloadRequest.builder()
                                                                         .presignedUrl(presignedUrl)
                                                                         .build();
        Object result = executeDownload(request, transformerType).join();
        assertSuccessfulDownload(transformerType, result);
    }

    @ParameterizedTest(name = "presignedUrlDownload_smallObjectSmallerThanPartSize_shouldSucceed [{0}]")
    @MethodSource("transformerTypes")
    @SuppressWarnings("unchecked")
    void presignedUrlDownload_smallObjectSmallerThanPartSize_shouldSucceed(String transformerType) throws IOException {
        byte[] smallData = "0123456789".getBytes(StandardCharsets.UTF_8);
        stubFor(get(urlEqualTo(PRESIGNED_URL_PATH))
                    .willReturn(aResponse()
                                    .withStatus(206)
                                    .withHeader("Content-Type", "application/octet-stream")
                                    .withHeader("Content-Length", "10")
                                    .withHeader("Content-Range", "bytes 0-9/10")
                                    .withHeader("ETag", "\"small-etag\"")
                                    .withBody(smallData)));

        PresignedUrlDownloadRequest request = PresignedUrlDownloadRequest.builder()
                                                                         .presignedUrl(presignedUrl)
                                                                         .build();
        Object result = executeDownload(request, transformerType).join();
        if ("toBytes".equals(transformerType)) {
            assertArrayEquals(smallData, ((ResponseBytes<GetObjectResponse>) result).asByteArray());
        } else {
            assertThat(tempFile.toFile()).exists();
            assertArrayEquals(smallData, Files.readAllBytes(tempFile));
        }
    }

    @ParameterizedTest(name = "presignedUrlDownload_withRangeHeader_shouldReceivePartialContent [{0}]")
    @MethodSource("transformerTypes")
    @SuppressWarnings("unchecked")
    void presignedUrlDownload_withRangeHeader_shouldReceivePartialContent(String transformerType) throws IOException {
        stubSuccessfulRangeResponse();
        PresignedUrlDownloadRequest request = PresignedUrlDownloadRequest.builder()
                                                                         .presignedUrl(presignedUrl)
                                                                         .putHeader("Range", "bytes=0-10")
                                                                         .build();
        Object result = executeDownload(request, transformerType).join();
        byte[] expectedPartial = Arrays.copyOfRange(TEST_DATA, 0, 11);
        if ("toBytes".equals(transformerType)) {
            assertArrayEquals(expectedPartial, ((ResponseBytes<GetObjectResponse>) result).asByteArray());
        } else {
            byte[] fileContent = Files.readAllBytes(tempFile);
            assertArrayEquals(expectedPartial, fileContent);
        }
    }

    @ParameterizedTest(name = "presignedUrlDownload_whenRequestFails_shouldThrowException [{0}]")
    @MethodSource("transformerTypes")
    void presignedUrlDownload_whenRequestFails_shouldThrowException(String transformerType) {
        stubFailedPresignedUrlResponse();
        PresignedUrlDownloadRequest request = PresignedUrlDownloadRequest.builder()
                                                                         .presignedUrl(presignedUrl)
                                                                         .build();
        assertThatThrownBy(() -> executeDownload(request, transformerType).join())
            .hasRootCauseInstanceOf(S3Exception.class);
    }

    @ParameterizedTest(name = "presignedUrlDownload_whenFirstRequestFails_shouldThrowException [{0}]")
    @MethodSource("transformerTypes")
    void presignedUrlDownload_whenFirstRequestFails_shouldThrowException(String transformerType) {
        stubInternalServerError();
        PresignedUrlDownloadRequest request = PresignedUrlDownloadRequest.builder()
                                                                         .presignedUrl(presignedUrl)
                                                                         .build();
        assertThatThrownBy(() -> executeDownload(request, transformerType).join())
            .hasRootCauseInstanceOf(S3Exception.class);
    }

    @ParameterizedTest(name = "presignedUrlDownload_whenSecondRequestFails_shouldThrowException [{0}]")
    @MethodSource("transformerTypes")
    void presignedUrlDownload_whenSecondRequestFails_shouldThrowException(String transformerType) {
        stubPartialFailureScenario();
        PresignedUrlDownloadRequest request = PresignedUrlDownloadRequest.builder()
                                                                         .presignedUrl(presignedUrl)
                                                                         .build();
        assertThatThrownBy(() -> executeDownload(request, transformerType).join())
            .hasRootCauseInstanceOf(S3Exception.class);
    }

    @ParameterizedTest(name = "presignedUrlDownload_whenIOErrorOccurs_shouldThrowException [{0}]")
    @MethodSource("transformerTypes")
    void presignedUrlDownload_whenIOErrorOccurs_shouldThrowException(String transformerType) {
        stubConnectionReset();
        PresignedUrlDownloadRequest request = PresignedUrlDownloadRequest.builder()
                                                                         .presignedUrl(presignedUrl)
                                                                         .build();
        assertThatThrownBy(() -> executeDownload(request, transformerType).join())
            .hasCauseInstanceOf(SdkClientException.class);
    }

    @ParameterizedTest(name = "presignedUrlDownload_withMissingContentRange_shouldFailRequest [{0}]")
    @MethodSource("transformerTypes")
    void presignedUrlDownload_withMissingContentRange_shouldFailRequest(String transformerType) {
        stubResponseWithMissingContentRange();
        PresignedUrlDownloadRequest request = PresignedUrlDownloadRequest.builder()
                                                                         .presignedUrl(presignedUrl)
                                                                         .build();
        assertThatThrownBy(() -> executeDownload(request, transformerType).join())
            .hasRootCauseInstanceOf(SdkClientException.class)
            .hasMessageContaining("No Content-Range header in response");
    }

    @ParameterizedTest(name = "presignedUrlDownload_withInvalidContentLength_shouldFailRequest [{0}]")
    @MethodSource("transformerTypes")
    void presignedUrlDownload_withInvalidContentLength_shouldFailRequest(String transformerType) {
        stubResponseWithInvalidContentLength();
        PresignedUrlDownloadRequest request = PresignedUrlDownloadRequest.builder()
                                                                         .presignedUrl(presignedUrl)
                                                                         .build();
        assertThatThrownBy(() -> executeDownload(request, transformerType).join())
            .hasRootCauseInstanceOf(SdkClientException.class)
            .hasMessageContaining("Invalid or missing Content-Length in response");
    }

    @ParameterizedTest(name = "presignedUrlDownload_withContentRangeMismatch_shouldFailRequest [{0}]")
    @MethodSource("transformerTypes")
    void presignedUrlDownload_withContentRangeMismatch_shouldFailRequest(String transformerType) {
        stubResponseWithContentRangeMismatch();
        PresignedUrlDownloadRequest request = PresignedUrlDownloadRequest.builder()
                                                                         .presignedUrl(presignedUrl)
                                                                         .build();
        assertThatThrownBy(() -> executeDownload(request, transformerType).join())
            .hasRootCauseInstanceOf(SdkClientException.class)
            .hasMessageContaining("Content-Range mismatch");
    }

    @ParameterizedTest(name = "presignedUrlDownload_withContentLengthMismatch_shouldFailRequest [{0}]")
    @MethodSource("transformerTypes")
    void presignedUrlDownload_withContentLengthMismatch_shouldFailRequest(String transformerType) {
        stubResponseWithContentLengthMismatch();
        PresignedUrlDownloadRequest request = PresignedUrlDownloadRequest.builder()
                                                                         .presignedUrl(presignedUrl)
                                                                         .build();
        assertThatThrownBy(() -> executeDownload(request, transformerType).join())
            .hasRootCauseInstanceOf(SdkClientException.class);
    }

    @Test
    void onNext_withNullTransformer_shouldThrowException() {
        PresignedUrlMultipartDownloaderSubscriber subscriber = createTestSubscriber();

        assertThatThrownBy(() -> subscriber.onNext(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("onNext must not be called with null asyncResponseTransformer");
    }

    @ParameterizedTest(name = "presignedUrlDownload_emptyObject_shouldFallbackToNonRangeGet [{0}]")
    @MethodSource("transformerTypes")
    @SuppressWarnings("unchecked")
    void presignedUrlDownload_emptyObject_shouldFallbackToNonRangeGet(String transformerType) throws IOException {
        stubFor(get(urlEqualTo(PRESIGNED_URL_PATH))
                    .withHeader("Range", matching("bytes=.*"))
                    .willReturn(aResponse()
                                    .withStatus(416)
                                    .withBody("<Error><Code>InvalidRange</Code>"
                                              + "<Message>The requested range is not satisfiable</Message></Error>")));
        stubFor(get(urlEqualTo(PRESIGNED_URL_PATH))
                    .withHeader("Range", absent())
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withHeader("Content-Length", "0")
                                    .withHeader("ETag", "\"empty-etag\"")
                                    .withBody(new byte[0])));

        PresignedUrlDownloadRequest request = PresignedUrlDownloadRequest.builder()
                                                                         .presignedUrl(presignedUrl)
                                                                         .build();
        Object result = executeDownload(request, transformerType).join();
        if ("toBytes".equals(transformerType)) {
            ResponseBytes<GetObjectResponse> bytes = (ResponseBytes<GetObjectResponse>) result;
            assertThat(bytes.asByteArray()).isEmpty();
        } else {
            assertThat(tempFile.toFile()).exists();
            assertThat(Files.size(tempFile)).isEqualTo(0L);
        }
    }

    @ParameterizedTest(name = "presignedUrlDownload_416OnSecondRequest_shouldFailWithError [{0}]")
    @MethodSource("transformerTypes")
    void presignedUrlDownload_416OnSecondRequest_shouldFailWithError(String transformerType) {
        stubFor(get(urlEqualTo(PRESIGNED_URL_PATH))
                    .inScenario("416-on-second")
                    .whenScenarioStateIs("Started")
                    .withHeader("Range", matching("bytes=0-15"))
                    .willReturn(aResponse()
                                    .withStatus(206)
                                    .withHeader("Content-Length", "16")
                                    .withHeader("Content-Range", "bytes 0-15/32")
                                    .withHeader("ETag", "\"test-etag\"")
                                    .withBody(Arrays.copyOfRange(TEST_DATA, 0, 16)))
                    .willSetStateTo("first-done"));

        stubFor(get(urlEqualTo(PRESIGNED_URL_PATH))
                    .inScenario("416-on-second")
                    .whenScenarioStateIs("first-done")
                    .willReturn(aResponse()
                                    .withStatus(416)
                                    .withBody("<Error><Code>InvalidRange</Code>"
                                              + "<Message>The requested range is not satisfiable</Message></Error>")));

        PresignedUrlDownloadRequest request = PresignedUrlDownloadRequest.builder()
                                                                         .presignedUrl(presignedUrl)
                                                                         .build();
        assertThatThrownBy(() -> executeDownload(request, transformerType).join())
            .hasRootCauseInstanceOf(S3Exception.class);
    }

    @ParameterizedTest(name = "presignedUrlDownload_withRangeHeader_emptyObject_shouldThrow416 [{0}]")
    @MethodSource("transformerTypes")
    void presignedUrlDownload_withRangeHeader_emptyObject_shouldThrow416(String transformerType) {
        stubFor(get(urlEqualTo(PRESIGNED_URL_PATH))
                    .willReturn(aResponse()
                                    .withStatus(416)
                                    .withBody("<Error><Code>InvalidRange</Code>"
                                              + "<Message>The requested range is not satisfiable</Message></Error>")));

        PresignedUrlDownloadRequest request = PresignedUrlDownloadRequest.builder()
                                                                         .presignedUrl(presignedUrl)
                                                                         .putHeader("Range", "bytes=0-1024")
                                                                         .build();
        assertThatThrownBy(() -> executeDownload(request, transformerType).join())
            .hasRootCauseInstanceOf(S3Exception.class);
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
            1024L,
            new CompletableFuture<>());
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
