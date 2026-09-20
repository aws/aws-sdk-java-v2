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

package software.amazon.awssdk.services.s3.internal.presignedurl;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.checksums.ChecksumValidation;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.presignedurl.model.PresignedUrlDownloadRequest;

/**
 * WireMock test verifying checksum validation behavior for presigned URL downloads.
 */
@WireMockTest
class PresignedUrlChecksumValidationWiremockTest {

    private static final String BODY = "test-content-for-checksum";
    private static final String CORRECT_CRC32 = "HUUjuQ=="; // CRC32 of "test-content-for-checksum"
    private static final String INCORRECT_CRC32 = "AAAAAAAA==";

    private S3AsyncClient s3Client;
    private final AtomicReference<ChecksumValidation> checksumValidationStatus = new AtomicReference<>();

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmInfo) {
        checksumValidationStatus.set(null);
        s3Client = S3AsyncClient.builder()
                                .endpointOverride(java.net.URI.create(wmInfo.getHttpBaseUrl()))
                                .region(Region.US_EAST_1)
                                .credentialsProvider(AnonymousCredentialsProvider.create())
                                .forcePathStyle(true)
                                .overrideConfiguration(c -> c.addExecutionInterceptor(new ExecutionInterceptor() {
                                    @Override
                                    public void afterExecution(Context.AfterExecution context, ExecutionAttributes attrs) {
                                        checksumValidationStatus.set(
                                            attrs.getAttribute(SdkExecutionAttribute.HTTP_RESPONSE_CHECKSUM_VALIDATION));
                                    }
                                }))
                                .build();
    }

    @AfterEach
    void tearDown() {
        if (s3Client != null) {
            s3Client.close();
        }
    }

    @Test
    void getObject_withMatchingChecksum_shouldSucceed(WireMockRuntimeInfo wmInfo) throws Exception {
        stubFor(get(urlPathEqualTo("/test-key"))
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withHeader("Content-Length", String.valueOf(BODY.length()))
                                    .withHeader("x-amz-checksum-crc32", CORRECT_CRC32)
                                    .withHeader("x-amz-checksum-type", "FULL_OBJECT")
                                    .withBody(BODY)));

        URL presignedUrl = new URL(wmInfo.getHttpBaseUrl() + "/test-key?" +
                                   "X-Amz-Algorithm=AWS4-HMAC-SHA256&" +
                                   "X-Amz-SignedHeaders=host%3Bx-amz-checksum-mode&" +
                                   "X-Amz-Signature=fake&" +
                                   "X-Amz-Expires=600");

        ResponseBytes<GetObjectResponse> result = s3Client.presignedUrlExtension()
                                                          .getObject(
                                                              PresignedUrlDownloadRequest.builder().presignedUrl(presignedUrl).build(),
                                                              AsyncResponseTransformer.toBytes())
                                                          .join();

        assertThat(result.asUtf8String()).isEqualTo(BODY);
        assertThat(checksumValidationStatus.get())
            .isEqualTo(ChecksumValidation.VALIDATED);
    }

    @Test
    void getObject_withMismatchingChecksum_shouldThrow(WireMockRuntimeInfo wmInfo) throws Exception {
        stubFor(get(urlPathEqualTo("/test-key"))
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withHeader("Content-Length", String.valueOf(BODY.length()))
                                    .withHeader("x-amz-checksum-crc32", INCORRECT_CRC32)
                                    .withHeader("x-amz-checksum-type", "FULL_OBJECT")
                                    .withBody(BODY)));

        URL presignedUrl = new URL(wmInfo.getHttpBaseUrl() + "/test-key?" +
                                   "X-Amz-Algorithm=AWS4-HMAC-SHA256&" +
                                   "X-Amz-SignedHeaders=host%3Bx-amz-checksum-mode&" +
                                   "X-Amz-Signature=fake&" +
                                   "X-Amz-Expires=600");

        CompletableFuture<ResponseBytes<GetObjectResponse>> future = s3Client.presignedUrlExtension()
                                                                             .getObject(
                                                                                 PresignedUrlDownloadRequest.builder().presignedUrl(presignedUrl).build(),
                                                                                 AsyncResponseTransformer.toBytes());

        assertThatThrownBy(future::join)
            .hasCauseInstanceOf(SdkClientException.class)
            .hasMessageContaining("checksum");
    }

    @Test
    void getObject_withNoChecksumHeader_shouldSucceedWithoutValidation(WireMockRuntimeInfo wmInfo) throws Exception {
        stubFor(get(urlPathEqualTo("/test-key"))
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withHeader("Content-Length", String.valueOf(BODY.length()))
                                    .withBody(BODY)));

        URL presignedUrl = new URL(wmInfo.getHttpBaseUrl() + "/test-key?" +
                                   "X-Amz-Algorithm=AWS4-HMAC-SHA256&" +
                                   "X-Amz-SignedHeaders=host&" +
                                   "X-Amz-Signature=fake&" +
                                   "X-Amz-Expires=600");

        ResponseBytes<GetObjectResponse> result = s3Client.presignedUrlExtension()
                                                          .getObject(
                                                              PresignedUrlDownloadRequest.builder().presignedUrl(presignedUrl).build(),
                                                              AsyncResponseTransformer.toBytes())
                                                          .join();

        assertThat(result.asUtf8String()).isEqualTo(BODY);
        assertThat(checksumValidationStatus.get()).isNull();
    }

    @Test
    void getObject_withChecksumModeButNoChecksumInResponse_shouldSetAlgorithmNotFound(WireMockRuntimeInfo wmInfo)
        throws Exception {
        // URL has checksum mode signed, but S3 response has no checksum header (e.g., ranged GET on single-PUT)
        stubFor(get(urlPathEqualTo("/test-key"))
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withHeader("Content-Length", String.valueOf(BODY.length()))
                                    .withBody(BODY)));

        URL presignedUrl = new URL(wmInfo.getHttpBaseUrl() + "/test-key?" +
                                   "X-Amz-Algorithm=AWS4-HMAC-SHA256&" +
                                   "X-Amz-SignedHeaders=host%3Bx-amz-checksum-mode&" +
                                   "X-Amz-Signature=fake&" +
                                   "X-Amz-Expires=600");

        ResponseBytes<GetObjectResponse> result = s3Client.presignedUrlExtension()
                                                          .getObject(
                                                              PresignedUrlDownloadRequest.builder().presignedUrl(presignedUrl).build(),
                                                              AsyncResponseTransformer.toBytes())
                                                          .join();

        assertThat(result.asUtf8String()).isEqualTo(BODY);
        assertThat(checksumValidationStatus.get())
            .isEqualTo(ChecksumValidation.CHECKSUM_ALGORITHM_NOT_FOUND);
    }

    @Test
    void getObject_withCompositeChecksum_shouldSkipValidationAndSucceed(WireMockRuntimeInfo wmInfo) throws Exception {
        stubFor(get(urlPathEqualTo("/test-key"))
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withHeader("Content-Length", String.valueOf(BODY.length()))
                                    .withHeader("x-amz-checksum-crc32", "i0T6cA==-3")
                                    .withHeader("x-amz-checksum-type", "COMPOSITE")
                                    .withBody(BODY)));

        URL presignedUrl = new URL(wmInfo.getHttpBaseUrl() + "/test-key?" +
                                   "X-Amz-Algorithm=AWS4-HMAC-SHA256&" +
                                   "X-Amz-SignedHeaders=host%3Bx-amz-checksum-mode&" +
                                   "X-Amz-Signature=fake&" +
                                   "X-Amz-Expires=600");

        ResponseBytes<GetObjectResponse> result = s3Client.presignedUrlExtension()
                                                          .getObject(
                                                              PresignedUrlDownloadRequest.builder().presignedUrl(presignedUrl).build(),
                                                              AsyncResponseTransformer.toBytes())
                                                          .join();

        assertThat(result.asUtf8String()).isEqualTo(BODY);
        assertThat(checksumValidationStatus.get())
            .isEqualTo(ChecksumValidation.FORCE_SKIP);

    }
}
