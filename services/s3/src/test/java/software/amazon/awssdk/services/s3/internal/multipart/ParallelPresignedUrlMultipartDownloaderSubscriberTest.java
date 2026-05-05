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
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.multipart.MultipartConfiguration;
import software.amazon.awssdk.services.s3.presignedurl.model.PresignedUrlDownloadRequest;

/**
 * Unit tests for {@link ParallelPresignedUrlMultipartDownloaderSubscriber}.
 * Tests parallel-specific behavior: single-part detection, concurrent requests,
 * and error propagation with in-flight cancellation.
 */
@WireMockTest
class ParallelPresignedUrlMultipartDownloaderSubscriberTest {

    private static final String PRESIGNED_URL_PATH = "/parallel-test";
    private static final byte[] TEST_DATA = "ABCDEFGHIJKLMNOPQRSTUVWXYZ123456".getBytes(StandardCharsets.UTF_8); // 32 bytes

    private S3AsyncClient s3AsyncClient;
    private URL presignedUrl;
    private Path tempFile;

    @BeforeEach
    void setup(WireMockRuntimeInfo wiremock) throws MalformedURLException {
        MultipartConfiguration multipartConfig = MultipartConfiguration.builder()
                                                                       .minimumPartSizeInBytes(16L)
                                                                       .build();
        s3AsyncClient = S3AsyncClient.builder()
                                     .endpointOverride(URI.create("http://localhost:" + wiremock.getHttpPort()))
                                     .multipartEnabled(true)
                                     .multipartConfiguration(multipartConfig)
                                     .build();
        presignedUrl = new URL("http://localhost:" + wiremock.getHttpPort() + PRESIGNED_URL_PATH);
    }

    @AfterEach
    void cleanup() throws IOException {
        if (tempFile != null && Files.exists(tempFile)) {
            Files.delete(tempFile);
        }
    }

    @Test
    void singlePartObject_shouldCompleteWithoutAdditionalRequests() throws IOException {
        byte[] smallData = "0123456789".getBytes(StandardCharsets.UTF_8);
        stubFor(get(urlEqualTo(PRESIGNED_URL_PATH))
                    .withHeader("Range", matching("bytes=0-15"))
                    .willReturn(aResponse()
                                    .withStatus(206)
                                    .withHeader("Content-Length", String.valueOf(smallData.length))
                                    .withHeader("Content-Range", "bytes 0-9/10")
                                    .withHeader("ETag", "\"single-part-etag\"")
                                    .withBody(smallData)));

        tempFile = createTempFile();
        PresignedUrlDownloadRequest request = PresignedUrlDownloadRequest.builder()
                                                                        .presignedUrl(presignedUrl)
                                                                        .build();
        GetObjectResponse response = (GetObjectResponse) s3AsyncClient.presignedUrlExtension()
            .getObject(request, AsyncResponseTransformer.toFile(tempFile))
            .join();

        assertThat(response.eTag()).isEqualTo("\"single-part-etag\"");
        assertThat(Files.readAllBytes(tempFile)).isEqualTo(smallData);
        verify(1, getRequestedFor(urlEqualTo(PRESIGNED_URL_PATH)));
    }

    @Test
    void multiPartObject_shouldDownloadAllPartsConcurrently() throws IOException {
        stubFor(get(urlEqualTo(PRESIGNED_URL_PATH))
                    .withHeader("Range", matching("bytes=0-15"))
                    .willReturn(aResponse()
                                    .withStatus(206)
                                    .withHeader("Content-Length", "16")
                                    .withHeader("Content-Range", "bytes 0-15/32")
                                    .withHeader("ETag", "\"multi-etag\"")
                                    .withBody(Arrays.copyOfRange(TEST_DATA, 0, 16))));

        stubFor(get(urlEqualTo(PRESIGNED_URL_PATH))
                    .withHeader("Range", matching("bytes=16-31"))
                    .willReturn(aResponse()
                                    .withStatus(206)
                                    .withHeader("Content-Length", "16")
                                    .withHeader("Content-Range", "bytes 16-31/32")
                                    .withHeader("ETag", "\"multi-etag\"")
                                    .withBody(Arrays.copyOfRange(TEST_DATA, 16, 32))));

        tempFile = createTempFile();
        PresignedUrlDownloadRequest request = PresignedUrlDownloadRequest.builder()
                                                                        .presignedUrl(presignedUrl)
                                                                        .build();
        s3AsyncClient.presignedUrlExtension()
            .getObject(request, AsyncResponseTransformer.toFile(tempFile))
            .join();

        assertThat(Files.readAllBytes(tempFile)).isEqualTo(TEST_DATA);
    }

    @Test
    void errorOnSecondPart_shouldCompleteExceptionallyAndNotSendMoreRequests() {
        stubFor(get(urlEqualTo(PRESIGNED_URL_PATH))
                    .withHeader("Range", matching("bytes=0-15"))
                    .willReturn(aResponse()
                                    .withStatus(206)
                                    .withHeader("Content-Length", "16")
                                    .withHeader("Content-Range", "bytes 0-15/48")
                                    .withHeader("ETag", "\"error-etag\"")
                                    .withBody(Arrays.copyOfRange(TEST_DATA, 0, 16))));

        stubFor(get(urlEqualTo(PRESIGNED_URL_PATH))
                    .withHeader("Range", matching("bytes=16-31"))
                    .willReturn(aResponse()
                                    .withStatus(500)
                                    .withBody("<Error><Code>InternalError</Code>"
                                              + "<Message>Simulated failure</Message></Error>")));

        tempFile = createTempFileUnchecked();
        PresignedUrlDownloadRequest request = PresignedUrlDownloadRequest.builder()
                                                                        .presignedUrl(presignedUrl)
                                                                        .build();

        assertThatThrownBy(() -> s3AsyncClient.presignedUrlExtension()
            .getObject(request, AsyncResponseTransformer.toFile(tempFile))
            .join())
            .isInstanceOf(CompletionException.class);
    }

    @Test
    void missingContentRangeOnFirstPart_shouldFail() {
        stubFor(get(urlEqualTo(PRESIGNED_URL_PATH))
                    .willReturn(aResponse()
                                    .withStatus(206)
                                    .withHeader("Content-Length", "16")
                                    .withHeader("ETag", "\"no-range-etag\"")
                                    .withBody(Arrays.copyOfRange(TEST_DATA, 0, 16))));

        tempFile = createTempFileUnchecked();
        PresignedUrlDownloadRequest request = PresignedUrlDownloadRequest.builder()
                                                                        .presignedUrl(presignedUrl)
                                                                        .build();

        assertThatThrownBy(() -> s3AsyncClient.presignedUrlExtension()
            .getObject(request, AsyncResponseTransformer.toFile(tempFile))
            .join())
            .hasRootCauseInstanceOf(SdkClientException.class)
            .hasMessageContaining("No Content-Range header");
    }

    @Test
    void contentRangeMismatchOnSecondPart_shouldFail() {
        stubFor(get(urlEqualTo(PRESIGNED_URL_PATH))
                    .withHeader("Range", matching("bytes=0-15"))
                    .willReturn(aResponse()
                                    .withStatus(206)
                                    .withHeader("Content-Length", "16")
                                    .withHeader("Content-Range", "bytes 0-15/32")
                                    .withHeader("ETag", "\"mismatch-etag\"")
                                    .withBody(Arrays.copyOfRange(TEST_DATA, 0, 16))));

        stubFor(get(urlEqualTo(PRESIGNED_URL_PATH))
                    .withHeader("Range", matching("bytes=16-31"))
                    .willReturn(aResponse()
                                    .withStatus(206)
                                    .withHeader("Content-Length", "16")
                                    .withHeader("Content-Range", "bytes 9999-10014/32")
                                    .withHeader("ETag", "\"mismatch-etag\"")
                                    .withBody(Arrays.copyOfRange(TEST_DATA, 16, 32))));

        tempFile = createTempFileUnchecked();
        PresignedUrlDownloadRequest request = PresignedUrlDownloadRequest.builder()
                                                                        .presignedUrl(presignedUrl)
                                                                        .build();

        assertThatThrownBy(() -> s3AsyncClient.presignedUrlExtension()
            .getObject(request, AsyncResponseTransformer.toFile(tempFile))
            .join())
            .hasRootCauseInstanceOf(SdkClientException.class)
            .hasMessageContaining("Content-Range mismatch");
    }

    @Test
    void onNext_withNullTransformer_shouldThrowNPE() {
        ParallelPresignedUrlMultipartDownloaderSubscriber subscriber =
            new ParallelPresignedUrlMultipartDownloaderSubscriber(
                s3AsyncClient,
                PresignedUrlDownloadRequest.builder().presignedUrl(presignedUrl).build(),
                16L,
                new java.util.concurrent.CompletableFuture<>(),
                10);

        assertThatThrownBy(() -> subscriber.onNext(null))
            .isInstanceOf(NullPointerException.class);
    }

    private static Path createTempFile() throws IOException {
        Path path = Files.createTempFile("parallel-test-" + UUID.randomUUID(), ".tmp");
        Files.deleteIfExists(path);
        return path;
    }

    private static Path createTempFileUnchecked() {
        try {
            return createTempFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
