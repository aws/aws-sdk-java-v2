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
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presignedurl.model.PresignedUrlDownloadRequest;

/**
 * WireMock tests verifying that signed headers supplied via {@link PresignedUrlDownloadRequest#putHeader}
 * are sent in the HTTP request and that missing signed headers result in a signature mismatch (403).
 */
@WireMockTest
class PresignedUrlSignedHeadersWiremockTest {

    private static final String BODY = "hello-presigned";
    private static final String KEY_PATH = "/test-key";

    private S3AsyncClient s3Client;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmInfo) {
        s3Client = S3AsyncClient.builder()
                                .endpointOverride(java.net.URI.create(wmInfo.getHttpBaseUrl()))
                                .region(Region.US_EAST_1)
                                .credentialsProvider(AnonymousCredentialsProvider.create())
                                .forcePathStyle(true)
                                .build();
    }

    @AfterEach
    void tearDown() {
        if (s3Client != null) {
            s3Client.close();
        }
    }

    @Test
    void getObject_withRangeHeader_shouldSendRangeInHttpRequest(WireMockRuntimeInfo wmInfo) throws Exception {
        stubFor(get(urlPathEqualTo(KEY_PATH))
                    .withHeader("Range", equalTo("bytes=0-1023"))
                    .willReturn(aResponse()
                                    .withStatus(206)
                                    .withHeader("Content-Length", String.valueOf(BODY.length()))
                                    .withHeader("Content-Range", "bytes 0-1023/2048")
                                    .withBody(BODY)));

        URL presignedUrl = presignedUrlWithSignedHeaders(wmInfo, "host%3Brange");

        ResponseBytes<GetObjectResponse> result = s3Client.presignedUrlExtension()
            .getObject(PresignedUrlDownloadRequest.builder()
                                                  .presignedUrl(presignedUrl)
                                                  .putHeader("Range", "bytes=0-1023")
                                                  .build(),
                       AsyncResponseTransformer.toBytes())
            .join();

        assertThat(result.asUtf8String()).isEqualTo(BODY);
        verify(getRequestedFor(urlPathEqualTo(KEY_PATH))
                   .withHeader("Range", equalTo("bytes=0-1023")));
    }

    @Test
    void getObject_withIfMatchHeader_shouldSendIfMatchInHttpRequest(WireMockRuntimeInfo wmInfo) throws Exception {
        stubFor(get(urlPathEqualTo(KEY_PATH))
                    .withHeader("If-Match", equalTo("\"abc123\""))
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withHeader("Content-Length", String.valueOf(BODY.length()))
                                    .withBody(BODY)));

        URL presignedUrl = presignedUrlWithSignedHeaders(wmInfo, "host%3Bif-match");

        ResponseBytes<GetObjectResponse> result = s3Client.presignedUrlExtension()
            .getObject(PresignedUrlDownloadRequest.builder()
                                                  .presignedUrl(presignedUrl)
                                                  .putHeader("If-Match", "\"abc123\"")
                                                  .build(),
                       AsyncResponseTransformer.toBytes())
            .join();

        assertThat(result.asUtf8String()).isEqualTo(BODY);
        verify(getRequestedFor(urlPathEqualTo(KEY_PATH))
                   .withHeader("If-Match", equalTo("\"abc123\"")));
    }

    @Test
    void getObject_withMultipleSignedHeaders_shouldSendAllInHttpRequest(WireMockRuntimeInfo wmInfo) throws Exception {
        stubFor(get(urlPathEqualTo(KEY_PATH))
                    .withHeader("Range", equalTo("bytes=0-99"))
                    .withHeader("If-None-Match", equalTo("\"old-etag\""))
                    .withHeader("x-amz-server-side-encryption-customer-algorithm", equalTo("AES256"))
                    .willReturn(aResponse()
                                    .withStatus(206)
                                    .withHeader("Content-Length", String.valueOf(BODY.length()))
                                    .withHeader("Content-Range", "bytes 0-99/200")
                                    .withBody(BODY)));

        URL presignedUrl = presignedUrlWithSignedHeaders(wmInfo,
            "host%3Bif-none-match%3Brange%3Bx-amz-server-side-encryption-customer-algorithm");

        ResponseBytes<GetObjectResponse> result = s3Client.presignedUrlExtension()
            .getObject(PresignedUrlDownloadRequest.builder()
                                                  .presignedUrl(presignedUrl)
                                                  .putHeader("Range", "bytes=0-99")
                                                  .putHeader("If-None-Match", "\"old-etag\"")
                                                  .putHeader("x-amz-server-side-encryption-customer-algorithm", "AES256")
                                                  .build(),
                       AsyncResponseTransformer.toBytes())
            .join();

        assertThat(result.asUtf8String()).isEqualTo(BODY);
        verify(getRequestedFor(urlPathEqualTo(KEY_PATH))
                   .withHeader("Range", equalTo("bytes=0-99"))
                   .withHeader("If-None-Match", equalTo("\"old-etag\""))
                   .withHeader("x-amz-server-side-encryption-customer-algorithm", equalTo("AES256")));
    }

    @Test
    void getObject_withMissingSignedHeader_serverReturns403(WireMockRuntimeInfo wmInfo) throws Exception {
        // Simulate S3 returning 403 SignatureDoesNotMatch when a signed header is missing
        stubFor(get(urlPathEqualTo(KEY_PATH))
                    .withHeader("Range", WireMock.absent())
                    .willReturn(aResponse()
                                    .withStatus(403)
                                    .withHeader("Content-Type", "application/xml")
                                    .withBody("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                                              + "<Error>"
                                              + "<Code>SignatureDoesNotMatch</Code>"
                                              + "<Message>The request signature we calculated does not match "
                                              + "the signature you provided.</Message>"
                                              + "</Error>")));

        URL presignedUrl = presignedUrlWithSignedHeaders(wmInfo, "host%3Brange");

        // Caller omits the Range header that was signed into the URL
        CompletableFuture<ResponseBytes<GetObjectResponse>> future = s3Client.presignedUrlExtension()
            .getObject(PresignedUrlDownloadRequest.builder()
                                                  .presignedUrl(presignedUrl)
                                                  .build(),
                       AsyncResponseTransformer.toBytes());

        assertThatThrownBy(future::join)
            .hasCauseInstanceOf(S3Exception.class)
            .hasMessageContaining("signature");
    }

    @Test
    void getObject_withWrongHeaderValue_serverReturns403(WireMockRuntimeInfo wmInfo) throws Exception {
        // S3 rejects if the header value doesn't match what was signed
        stubFor(get(urlPathEqualTo(KEY_PATH))
                    .withHeader("Range", equalTo("bytes=0-1023"))
                    .willReturn(aResponse()
                                    .withStatus(206)
                                    .withHeader("Content-Length", "1024")
                                    .withHeader("Content-Range", "bytes 0-1023/2048")
                                    .withBody(BODY)));

        // Any request with a different Range gets 403
        stubFor(get(urlPathEqualTo(KEY_PATH))
                    .withHeader("Range", WireMock.notMatching("bytes=0-1023"))
                    .willReturn(aResponse()
                                    .withStatus(403)
                                    .withHeader("Content-Type", "application/xml")
                                    .withBody("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                                              + "<Error>"
                                              + "<Code>SignatureDoesNotMatch</Code>"
                                              + "<Message>The request signature we calculated does not match "
                                              + "the signature you provided.</Message>"
                                              + "</Error>")));

        URL presignedUrl = presignedUrlWithSignedHeaders(wmInfo, "host%3Brange");

        // Caller sends a different Range value than what was signed
        CompletableFuture<ResponseBytes<GetObjectResponse>> future = s3Client.presignedUrlExtension()
            .getObject(PresignedUrlDownloadRequest.builder()
                                                  .presignedUrl(presignedUrl)
                                                  .putHeader("Range", "bytes=500-999")
                                                  .build(),
                       AsyncResponseTransformer.toBytes());

        assertThatThrownBy(future::join)
            .hasCauseInstanceOf(S3Exception.class)
            .hasMessageContaining("signature");
    }

    @Test
    void getObject_withNoSignedHeaders_shouldSucceedWithoutExtraHeaders(WireMockRuntimeInfo wmInfo) throws Exception {
        stubFor(get(urlPathEqualTo(KEY_PATH))
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withHeader("Content-Length", String.valueOf(BODY.length()))
                                    .withBody(BODY)));

        // URL signed with only "host" — no additional headers needed
        URL presignedUrl = presignedUrlWithSignedHeaders(wmInfo, "host");

        ResponseBytes<GetObjectResponse> result = s3Client.presignedUrlExtension()
            .getObject(PresignedUrlDownloadRequest.builder()
                                                  .presignedUrl(presignedUrl)
                                                  .build(),
                       AsyncResponseTransformer.toBytes())
            .join();

        assertThat(result.asUtf8String()).isEqualTo(BODY);
    }

    private static URL presignedUrlWithSignedHeaders(WireMockRuntimeInfo wmInfo, String signedHeaders) throws Exception {
        return new URL(wmInfo.getHttpBaseUrl() + KEY_PATH + "?"
                       + "X-Amz-Algorithm=AWS4-HMAC-SHA256&"
                       + "X-Amz-SignedHeaders=" + signedHeaders + "&"
                       + "X-Amz-Signature=fakesig&"
                       + "X-Amz-Expires=600");
    }
}
