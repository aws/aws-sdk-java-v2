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
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.moreThan;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;
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
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class PresignedUrlMultipartDownloaderSubscriberRetryWiremockTest {

    private static final String PRESIGNED_URL_PATH = "/presigned-url";
    private static final int PART_SIZE = 16;
    private static final byte[] TEST_DATA = "This is exactly a 32 byte string".getBytes(StandardCharsets.UTF_8);
    private static final byte[] PART_1_DATA = Arrays.copyOfRange(TEST_DATA, 0, PART_SIZE);
    private static final byte[] PART_2_DATA = Arrays.copyOfRange(TEST_DATA, PART_SIZE, TEST_DATA.length);

    private S3AsyncClient s3AsyncClient;
    private URL presignedUrl;
    private Path tempFile;

    @BeforeEach
    void setup(WireMockRuntimeInfo wiremock) throws MalformedURLException {
        s3AsyncClient = S3AsyncClient.builder()
                                     .endpointOverride(URI.create(wiremock.getHttpBaseUrl()))
                                     .multipartEnabled(true)
                                     .multipartConfiguration(MultipartConfiguration.builder()
                                                                                   .minimumPartSizeInBytes((long) PART_SIZE)
                                                                                   .build())
                                     .build();
        presignedUrl = new URL(wiremock.getHttpBaseUrl() + PRESIGNED_URL_PATH);
    }

    static Stream<Arguments> transformerTypes() {
        return Stream.of(
            Arguments.of("toBytes"),
            Arguments.of("toFile")
        );
    }

    @ParameterizedTest(name = "errorOnFirstPart_nonRetryable_shouldFailImmediately [{0}]")
    @MethodSource("transformerTypes")
    void errorOnFirstPart_nonRetryable_shouldFailImmediately(String transformerType) throws IOException {
        stubError(1, 403, "<Error><Code>AccessDenied</Code><Message>Access denied</Message></Error>");

        assertThatThrownBy(() -> executeDownload(transformerType).join())
            .isInstanceOf(CompletionException.class)
            .hasCauseInstanceOf(S3Exception.class)
            .hasMessageContaining("Access denied");

        verify(exactly(1), getRequestedFor(urlEqualTo(PRESIGNED_URL_PATH)));
    }

    @ParameterizedTest(name = "errorOnMiddlePart_nonRetryable_shouldFail [{0}]")
    @MethodSource("transformerTypes")
    void errorOnMiddlePart_nonRetryable_shouldFail(String transformerType) throws IOException {
        stubSuccessPart1();
        stubError(2, 403, "<Error><Code>AccessDenied</Code><Message>Access denied on part 2</Message></Error>");

        assertThatThrownBy(() -> executeDownload(transformerType).join())
            .isInstanceOf(CompletionException.class);

        verify(exactly(1), getRequestedFor(urlEqualTo(PRESIGNED_URL_PATH))
            .withHeader("Range", matching("bytes=0-15")));
        verify(exactly(1), getRequestedFor(urlEqualTo(PRESIGNED_URL_PATH))
            .withHeader("Range", matching("bytes=16-31")));
    }

    @ParameterizedTest(name = "errorOnFirstPart_retryable_exhaustsRetries_shouldFail [{0}]")
    @MethodSource("transformerTypes")
    void errorOnFirstPart_retryable_exhaustsRetries_shouldFail(String transformerType) throws IOException {
        stubError(1, 500, "<Error><Code>InternalError</Code><Message>Server error</Message></Error>");

        assertThatThrownBy(() -> executeDownload(transformerType).join())
            .isInstanceOf(CompletionException.class)
            .hasCauseInstanceOf(S3Exception.class)
            .hasMessageContaining("Server error");

        verify(moreThan(1), getRequestedFor(urlEqualTo(PRESIGNED_URL_PATH)));
    }

    @ParameterizedTest(name = "errorOnMiddlePart_retryable_exhaustsRetries_shouldFail [{0}]")
    @MethodSource("transformerTypes")
    void errorOnMiddlePart_retryable_exhaustsRetries_shouldFail(String transformerType) throws IOException {
        stubSuccessPart1();
        stubError(2, 500, "<Error><Code>InternalError</Code><Message>Permanent failure</Message></Error>");

        assertThatThrownBy(() -> executeDownload(transformerType).join())
            .isInstanceOf(CompletionException.class);

        verify(exactly(1), getRequestedFor(urlEqualTo(PRESIGNED_URL_PATH))
            .withHeader("Range", matching("bytes=0-15")));
        verify(moreThan(1), getRequestedFor(urlEqualTo(PRESIGNED_URL_PATH))
            .withHeader("Range", matching("bytes=16-31")));
    }

    @ParameterizedTest(name = "ioErrorOnFirstPart_exhaustsRetries_shouldFail [{0}]")
    @MethodSource("transformerTypes")
    void ioErrorOnFirstPart_exhaustsRetries_shouldFail(String transformerType) throws IOException {
        stubIoError(1);

        assertThatThrownBy(() -> executeDownload(transformerType).join())
            .isInstanceOf(CompletionException.class)
            .hasCauseInstanceOf(SdkClientException.class);

        verify(moreThan(1), getRequestedFor(urlEqualTo(PRESIGNED_URL_PATH))
            .withHeader("Range", matching("bytes=0-15")));
    }

    @ParameterizedTest(name = "errorOnFirstPart_retryable_thenSucceeds [{0}]")
    @MethodSource("transformerTypes")
    void errorOnFirstPart_retryable_thenSucceeds(String transformerType) throws IOException {
        stubFor(get(urlEqualTo(PRESIGNED_URL_PATH))
                    .withHeader("Range", matching("bytes=0-15"))
                    .inScenario("retry-first")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(aResponse()
                                    .withStatus(500)
                                    .withBody("<Error><Code>InternalError</Code><Message>retry</Message></Error>"))
                    .willSetStateTo("retry"));

        stubFor(get(urlEqualTo(PRESIGNED_URL_PATH))
                    .withHeader("Range", matching("bytes=0-15"))
                    .inScenario("retry-first")
                    .whenScenarioStateIs("retry")
                    .willReturn(successPart1Response())
                    .willSetStateTo("part1-done"));

        stubFor(get(urlEqualTo(PRESIGNED_URL_PATH))
                    .withHeader("Range", matching("bytes=16-31"))
                    .inScenario("retry-first")
                    .whenScenarioStateIs("part1-done")
                    .willReturn(successPart2Response()));

        Object result = executeDownload(transformerType).join();
        assertSuccessfulDownload(transformerType, result);

        verify(exactly(2), getRequestedFor(urlEqualTo(PRESIGNED_URL_PATH))
            .withHeader("Range", matching("bytes=0-15")));
    }

    @ParameterizedTest(name = "errorOnMiddlePart_retryable_thenSucceeds [{0}]")
    @MethodSource("transformerTypes")
    void errorOnMiddlePart_retryable_thenSucceeds(String transformerType) throws IOException {
        stubSuccessPart1();

        stubFor(get(urlEqualTo(PRESIGNED_URL_PATH))
                    .withHeader("Range", matching("bytes=16-31"))
                    .inScenario("retry-middle")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(aResponse()
                                    .withStatus(500)
                                    .withBody("<Error><Code>InternalError</Code><Message>retry</Message></Error>"))
                    .willSetStateTo("retry"));

        stubFor(get(urlEqualTo(PRESIGNED_URL_PATH))
                    .withHeader("Range", matching("bytes=16-31"))
                    .inScenario("retry-middle")
                    .whenScenarioStateIs("retry")
                    .willReturn(successPart2Response()));

        Object result = executeDownload(transformerType).join();
        assertSuccessfulDownload(transformerType, result);

        verify(exactly(1), getRequestedFor(urlEqualTo(PRESIGNED_URL_PATH))
            .withHeader("Range", matching("bytes=0-15")));
        verify(exactly(2), getRequestedFor(urlEqualTo(PRESIGNED_URL_PATH))
            .withHeader("Range", matching("bytes=16-31")));
    }

    @ParameterizedTest(name = "ioErrorOnFirstPart_thenSucceeds [{0}]")
    @MethodSource("transformerTypes")
    void ioErrorOnFirstPart_thenSucceeds(String transformerType) throws IOException {
        stubFor(get(urlEqualTo(PRESIGNED_URL_PATH))
                    .withHeader("Range", matching("bytes=0-15"))
                    .inScenario("io-retry")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER))
                    .willSetStateTo("retry"));

        stubFor(get(urlEqualTo(PRESIGNED_URL_PATH))
                    .withHeader("Range", matching("bytes=0-15"))
                    .inScenario("io-retry")
                    .whenScenarioStateIs("retry")
                    .willReturn(successPart1Response())
                    .willSetStateTo("part1-done"));

        stubFor(get(urlEqualTo(PRESIGNED_URL_PATH))
                    .withHeader("Range", matching("bytes=16-31"))
                    .inScenario("io-retry")
                    .whenScenarioStateIs("part1-done")
                    .willReturn(successPart2Response()));

        Object result = executeDownload(transformerType).join();
        assertSuccessfulDownload(transformerType, result);

        verify(exactly(2), getRequestedFor(urlEqualTo(PRESIGNED_URL_PATH))
            .withHeader("Range", matching("bytes=0-15")));
    }

    @ParameterizedTest(name = "retryableError_thenUrlExpires_shouldFailWithExpiredError [{0}]")
    @MethodSource("transformerTypes")
    void retryableError_thenUrlExpires_shouldFailWithExpiredError(String transformerType) throws IOException {
        stubFor(get(urlEqualTo(PRESIGNED_URL_PATH))
                    .withHeader("Range", matching("bytes=0-15"))
                    .inScenario("url-expires")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(aResponse()
                                    .withStatus(500)
                                    .withBody("<Error><Code>InternalError</Code><Message>Server error</Message></Error>"))
                    .willSetStateTo("expired"));

        stubFor(get(urlEqualTo(PRESIGNED_URL_PATH))
                    .withHeader("Range", matching("bytes=0-15"))
                    .inScenario("url-expires")
                    .whenScenarioStateIs("expired")
                    .willReturn(aResponse()
                                    .withStatus(403)
                                    .withBody("<Error><Code>AccessDenied</Code>"
                                              + "<Message>Request has expired</Message></Error>")));

        assertThatThrownBy(() -> executeDownload(transformerType).join())
            .isInstanceOf(CompletionException.class)
            .hasCauseInstanceOf(S3Exception.class)
            .hasMessageContaining("Request has expired");

        verify(exactly(2), getRequestedFor(urlEqualTo(PRESIGNED_URL_PATH)));
    }


    private java.util.concurrent.CompletableFuture<?> executeDownload(String transformerType) throws IOException {
        PresignedUrlDownloadRequest request = PresignedUrlDownloadRequest.builder()
                                                                         .presignedUrl(presignedUrl)
                                                                         .build();
        if ("toFile".equals(transformerType)) {
            tempFile = Files.createTempFile("test-" + UUID.randomUUID(), ".tmp");
            Files.deleteIfExists(tempFile);
            return s3AsyncClient.presignedUrlExtension().getObject(request, AsyncResponseTransformer.toFile(tempFile));
        }
        return s3AsyncClient.presignedUrlExtension().getObject(request, AsyncResponseTransformer.toBytes());
    }

    @SuppressWarnings("unchecked")
    private void assertSuccessfulDownload(String type, Object result) throws IOException {
        if ("toBytes".equals(type)) {
            assertArrayEquals(TEST_DATA, ((ResponseBytes<GetObjectResponse>) result).asByteArray());
        } else {
            assertArrayEquals(TEST_DATA, Files.readAllBytes(tempFile));
        }
    }

    private void stubSuccessPart1() {
        stubFor(get(urlEqualTo(PRESIGNED_URL_PATH))
                    .withHeader("Range", matching("bytes=0-15"))
                    .willReturn(successPart1Response()));
    }

    private com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder successPart1Response() {
        return aResponse()
            .withStatus(206)
            .withHeader("Content-Length", String.valueOf(PART_SIZE))
            .withHeader("Content-Range", "bytes 0-15/32")
            .withHeader("ETag", "\"etag\"")
            .withBody(PART_1_DATA);
    }

    private com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder successPart2Response() {
        return aResponse()
            .withStatus(206)
            .withHeader("Content-Length", String.valueOf(PART_SIZE))
            .withHeader("Content-Range", "bytes 16-31/32")
            .withHeader("ETag", "\"etag\"")
            .withBody(PART_2_DATA);
    }

    private void stubError(int partNumber, int status, String body) {
        String rangePattern = partNumber == 1 ? "bytes=0-15" : "bytes=16-31";
        stubFor(get(urlEqualTo(PRESIGNED_URL_PATH))
                    .withHeader("Range", matching(rangePattern))
                    .willReturn(aResponse()
                                    .withStatus(status)
                                    .withBody(body)));
    }

    private void stubIoError(int partNumber) {
        String rangePattern = partNumber == 1 ? "bytes=0-15" : "bytes=16-31";
        stubFor(get(urlEqualTo(PRESIGNED_URL_PATH))
                    .withHeader("Range", matching(rangePattern))
                    .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
    }

    @AfterEach
    void cleanup() {
        if (s3AsyncClient != null) {
            s3AsyncClient.close();
        }
        if (tempFile != null && Files.exists(tempFile)) {
            try {
                Files.delete(tempFile);
            } catch (IOException e) {
            }
        }
    }
}
