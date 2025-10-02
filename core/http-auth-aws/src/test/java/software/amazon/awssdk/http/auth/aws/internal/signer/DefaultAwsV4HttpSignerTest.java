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

package software.amazon.awssdk.http.auth.aws.internal.signer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static software.amazon.awssdk.checksums.DefaultChecksumAlgorithm.CRC32;
import static software.amazon.awssdk.checksums.DefaultChecksumAlgorithm.SHA256;
import static software.amazon.awssdk.http.auth.aws.TestUtils.generateBasicAsyncRequest;
import static software.amazon.awssdk.http.auth.aws.TestUtils.generateBasicRequest;
import static software.amazon.awssdk.http.auth.aws.TestUtils.testPayload;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.CONTENT_ENCODING;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner.AUTH_LOCATION;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner.CHECKSUM_ALGORITHM;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner.CHUNK_ENCODING_ENABLED;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner.EXPIRATION_DURATION;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner.PAYLOAD_SIGNING_ENABLED;
import static software.amazon.awssdk.http.auth.spi.signer.SdkInternalHttpSignerProperty.CHECKSUM_STORE;

import io.reactivex.Flowable;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.checksums.SdkChecksum;
import software.amazon.awssdk.checksums.spi.ChecksumAlgorithm;
import software.amazon.awssdk.http.Header;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.TestUtils;
import software.amazon.awssdk.http.auth.aws.eventstream.internal.io.SigV4DataFramePublisher;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner.AuthLocation;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.signer.PayloadChecksumStore;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.ClassLoaderHelper;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Test the delegation of signing to the correct implementations.
 */
public class DefaultAwsV4HttpSignerTest {

    DefaultAwsV4HttpSigner signer = new DefaultAwsV4HttpSigner();

    @Test
    void sign_WithNoAdditonalProperties_DelegatesToHeaderSigner() {
        SignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> {
            },
            signRequest -> {
            }
        );

        SignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("Authorization")).isPresent();
    }

    @Test
    void signAsync_WithNoAdditonalProperties_DelegatesToHeaderSigner() {
        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> {
            },
            signRequest -> {
            }
        );

        AsyncSignedRequest signedRequest = signer.signAsync(request).join();

        assertThat(signedRequest.request().firstMatchingHeader("Authorization")).isPresent();
    }

    @Test
    void sign_WithQueryAuthLocation_DelegatesToQuerySigner() {
        SignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> {
            },
            signRequest -> signRequest.putProperty(AUTH_LOCATION, AuthLocation.QUERY_STRING)
        );

        SignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingRawQueryParameter("X-Amz-Signature")).isPresent();
    }

    @Test
    void signAsync_WithQueryAuthLocation_DelegatesToQuerySigner() {
        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> {
            },
            signRequest -> signRequest.putProperty(AUTH_LOCATION, AuthLocation.QUERY_STRING)
        );

        AsyncSignedRequest signedRequest = signer.signAsync(request).join();

        assertThat(signedRequest.request().firstMatchingRawQueryParameter("X-Amz-Signature")).isPresent();
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 0, 604801})
    void sign_WithQueryAuthLocationAndInvalidExpiration_Throws(long seconds) {
        SignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> {
            },
            signRequest -> signRequest
                .putProperty(AUTH_LOCATION, AuthLocation.QUERY_STRING)
                .putProperty(EXPIRATION_DURATION, Duration.ofSeconds(seconds))
        );

        assertThrows(IllegalArgumentException.class, () -> signer.sign(request));
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 0, 604801})
    void signAsync_WithQueryAuthLocationAndInvalidExpiration_Throws(long seconds) {
        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> {
            },
            signRequest -> signRequest
                .putProperty(AUTH_LOCATION, AuthLocation.QUERY_STRING)
                .putProperty(EXPIRATION_DURATION, Duration.ofSeconds(seconds))
        );

        assertThrows(IllegalArgumentException.class, () -> signer.signAsync(request));
    }

    @Test
    void sign_WithQueryAuthLocationAndExpiration_DelegatesToPresignedSigner() {
        SignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> {
            },
            signRequest -> signRequest
                .putProperty(AUTH_LOCATION, AuthLocation.QUERY_STRING)
                .putProperty(EXPIRATION_DURATION, Duration.ofDays(1))
        );

        SignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingRawQueryParameter("X-Amz-Expires")).isPresent();
    }

    @Test
    void signAsync_WithQueryAuthLocationAndExpiration_DelegatesToPresignedSigner() {
        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> {
            },
            signRequest -> signRequest
                .putProperty(AUTH_LOCATION, AuthLocation.QUERY_STRING)
                .putProperty(EXPIRATION_DURATION, Duration.ofDays(1))
        );

        AsyncSignedRequest signedRequest = signer.signAsync(request).join();

        assertThat(signedRequest.request().firstMatchingRawQueryParameter("X-Amz-Expires")).isPresent();
    }

    @Test
    void sign_WithHeaderAuthLocationAndExpirationDuration_Throws() {
        SignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> {
            },
            signRequest -> signRequest
                .putProperty(AUTH_LOCATION, AuthLocation.HEADER)
                .putProperty(EXPIRATION_DURATION, Duration.ofDays(1))
        );

        assertThrows(UnsupportedOperationException.class, () -> signer.sign(request));
    }

    @Test
    void signAsync_WithHeaderAuthLocationAndExpirationDuration_Throws() {
        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> {
            },
            signRequest -> signRequest
                .putProperty(AUTH_LOCATION, AuthLocation.HEADER)
                .putProperty(EXPIRATION_DURATION, Duration.ofDays(1))
        );

        assertThrows(UnsupportedOperationException.class, () -> signer.signAsync(request).join());
    }

    @Test
    void sign_withAnonymousCreds_shouldNotSign() {
        SignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            new TestUtils.AnonymousCredentialsIdentity(),
            httpRequest -> {
            },
            signRequest -> {
            }
        );

        SignedRequest signedRequest = signer.sign(request);
        assertThat(signedRequest.request().firstMatchingRawQueryParameter("X-Amz-Signature"))
            .isNotPresent();
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-content-sha256")).isNotPresent();
    }

    @Test
    void signAsync_withAnonymousCreds_shouldNotSign() {
        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            new TestUtils.AnonymousCredentialsIdentity(),
            httpRequest -> {
            },
            signRequest -> {
            }
        );

        AsyncSignedRequest signedRequest = signer.signAsync(request).join();
        assertThat(signedRequest.request().firstMatchingRawQueryParameter("X-Amz-Signature"))
            .isNotPresent();
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-content-sha256")).isNotPresent();
    }

    @Test
    void sign_WithPayloadSigningFalse_DelegatesToUnsignedPayloadSigner() {
        SignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> {
            },
            signRequest -> signRequest
                .putProperty(PAYLOAD_SIGNING_ENABLED, false)
        );

        SignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("x-amz-content-sha256")).hasValue("UNSIGNED-PAYLOAD");
    }

    @Test
    void signAsync_WithPayloadSigningFalse_DelegatesToUnsignedPayloadSigner() {
        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> {
            },
            signRequest -> signRequest
                .putProperty(PAYLOAD_SIGNING_ENABLED, false)
        );

        AsyncSignedRequest signedRequest = signer.signAsync(request).join();

        assertThat(signedRequest.request().firstMatchingHeader("x-amz-content-sha256")).hasValue("UNSIGNED-PAYLOAD");
    }

    @Test
    void sign_WithPayloadSigningFalseAndHttpAndNoPayload_DelegatesToUnsignedPayloadSigner() {
        SignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest ->
                httpRequest.uri(URI.create("http://demo.us-east-1.amazonaws.com")),
            signRequest -> signRequest
                .putProperty(PAYLOAD_SIGNING_ENABLED, false)
                .payload(null)
        );

        SignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("x-amz-content-sha256")).hasValue("UNSIGNED-PAYLOAD");
    }

    @Test
    void signAsync_WithPayloadSigningFalseAndHttpAndNoPayload_DelegatesToUnsignedPayloadSigner() {
        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest ->
                httpRequest.uri(URI.create("http://demo.us-east-1.amazonaws.com")),
            signRequest -> signRequest
                .putProperty(PAYLOAD_SIGNING_ENABLED, false)
                .payload(null)
        );

        AsyncSignedRequest signedRequest = signer.signAsync(request).join();

        assertThat(signedRequest.request().firstMatchingHeader("x-amz-content-sha256")).hasValue("UNSIGNED-PAYLOAD");
    }

    @Test
    void sign_WithEventStreamContentTypeWithoutHttpAuthAwsEventStreamModule_throws() {
        SignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> httpRequest
                .putHeader("Content-Type", "application/vnd.amazon.eventstream"),
            signRequest -> {
            }
        );

        try (MockedStatic<ClassLoaderHelper> utilities = Mockito.mockStatic(ClassLoaderHelper.class)) {
            utilities.when(() ->ClassLoaderHelper.loadClass(
                "software.amazon.awssdk.http.auth.aws.eventstream.HttpAuthAwsEventStream",
                false)
            ).thenThrow(new ClassNotFoundException("boom!"));
            Exception e = assertThrows(RuntimeException.class, () -> signer.sign(request));
            assertThat(e).hasMessageContaining("http-auth-aws-eventstream");
        }
    }

    @Test
    void signAsync_WithEventStreamContentTypeWithoutHttpAuthAwsEventStreamModule_throws() {
        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> httpRequest
                .putHeader("Content-Type", "application/vnd.amazon.eventstream"),
            signRequest -> {
            }
        );

        try (MockedStatic<ClassLoaderHelper> utilities = Mockito.mockStatic(ClassLoaderHelper.class)) {
            utilities.when(() ->ClassLoaderHelper.loadClass(
                "software.amazon.awssdk.http.auth.aws.eventstream.HttpAuthAwsEventStream",
                false)
            ).thenThrow(new ClassNotFoundException("boom!"));
            Exception e = assertThrows(RuntimeException.class, () -> signer.signAsync(request).join());
            assertThat(e).hasMessageContaining("http-auth-aws-eventstream");
        }
    }

    @Test
    void sign_WithEventStreamContentType_Throws() {
        SignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> httpRequest
                .putHeader("Content-Type", "application/vnd.amazon.eventstream"),
            signRequest -> {
            }
        );

        assertThrows(UnsupportedOperationException.class, () -> signer.sign(request));
    }

    @Test
    void signAsync_WithEventStreamContentType_DelegatesToEventStreamPayloadSigner() {
        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> httpRequest
                .putHeader("Content-Type", "application/vnd.amazon.eventstream"),
            signRequest -> {
            }
        );

        AsyncSignedRequest signedRequest = signer.signAsync(request).join();

        assertThat(signedRequest.payload().get()).isInstanceOf(SigV4DataFramePublisher.class);
    }

    @Test
    void sign_WithEventStreamContentTypeAndUnsignedPayload_Throws() {
        SignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> httpRequest
                .putHeader("Content-Type", "application/vnd.amazon.eventstream"),
            signRequest -> signRequest
                .putProperty(PAYLOAD_SIGNING_ENABLED, false)
        );

        assertThrows(UnsupportedOperationException.class, () -> signer.sign(request));
    }

    @Test
    void signAsync_WithEventStreamContentTypeAndUnsignedPayload_Throws() {
        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> httpRequest
                .putHeader("Content-Type", "application/vnd.amazon.eventstream"),
            signRequest -> signRequest
                .putProperty(PAYLOAD_SIGNING_ENABLED, false)
        );

        assertThrows(UnsupportedOperationException.class, () -> signer.signAsync(request));
    }

    @Test
    void sign_WithChunkEncodingTrue_DelegatesToAwsChunkedPayloadSigner() {
        SignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> httpRequest
                .putHeader(Header.CONTENT_LENGTH, "20"),
            signRequest -> signRequest
                .putProperty(CHUNK_ENCODING_ENABLED, true)
        );

        SignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("x-amz-content-sha256"))
            .hasValue("STREAMING-AWS4-HMAC-SHA256-PAYLOAD");
        assertThat(signedRequest.request().firstMatchingHeader(Header.CONTENT_LENGTH)).hasValue("193");
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-decoded-content-length")).hasValue("20");
    }

    @Test
    void signAsync_WithChunkEncodingTrue_DelegatesToAwsChunkedPayloadSigner_futureBehavior() {
        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> httpRequest
                .putHeader(Header.CONTENT_LENGTH, "20"),
            signRequest -> signRequest
                .putProperty(CHUNK_ENCODING_ENABLED, true)
        );

        AsyncSignedRequest signedRequest = signer.signAsync(request).join();

        assertThat(signedRequest.request().firstMatchingHeader("x-amz-content-sha256"))
            .hasValue("STREAMING-AWS4-HMAC-SHA256-PAYLOAD");
        assertThat(signedRequest.request().firstMatchingHeader(CONTENT_ENCODING)).hasValue("aws-chunked");
        assertThat(signedRequest.request().firstMatchingHeader(Header.CONTENT_LENGTH)).hasValue("193");
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-decoded-content-length")).hasValue("20");
    }

    @Test
    void sign_WithChunkEncodingTrueAndChecksumAlgorithm_DelegatesToAwsChunkedPayloadSigner() {
        SignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> httpRequest
                .putHeader(Header.CONTENT_LENGTH, "20"),
            signRequest -> signRequest
                .putProperty(CHUNK_ENCODING_ENABLED, true)
                .putProperty(CHECKSUM_ALGORITHM, CRC32)
        );

        SignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("x-amz-content-sha256"))
            .hasValue("STREAMING-AWS4-HMAC-SHA256-PAYLOAD-TRAILER");
        assertThat(signedRequest.request().firstMatchingHeader(Header.CONTENT_LENGTH)).hasValue("314");
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-decoded-content-length")).hasValue("20");
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-trailer")).hasValue("x-amz-checksum-crc32");
    }

    @Test
    void signAsync_WithChunkEncodingTrueAndChecksumAlgorithm_DelegatesToAwsChunkedPayloadSigner_futureBehavior() {
        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> httpRequest
                .putHeader(Header.CONTENT_LENGTH, "20"),
            signRequest -> signRequest
                .putProperty(CHUNK_ENCODING_ENABLED, true)
                .putProperty(CHECKSUM_ALGORITHM, CRC32)
        );

        AsyncSignedRequest signedRequest = signer.signAsync(request).join();

        assertThat(signedRequest.request().firstMatchingHeader("x-amz-content-sha256"))
            .hasValue("STREAMING-AWS4-HMAC-SHA256-PAYLOAD-TRAILER");
        assertThat(signedRequest.request().firstMatchingHeader(CONTENT_ENCODING)).hasValue("aws-chunked");
        assertThat(signedRequest.request().firstMatchingHeader(Header.CONTENT_LENGTH)).hasValue("314");
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-decoded-content-length")).hasValue("20");
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-trailer")).hasValue("x-amz-checksum-crc32");
    }

    @Test
    void sign_WithPayloadSigningFalseAndChunkEncodingTrueAndFlexibleChecksum_DelegatesToAwsChunkedPayloadSigner() {
        SignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> httpRequest
                .putHeader(Header.CONTENT_LENGTH, "20"),
            signRequest -> signRequest
                .putProperty(PAYLOAD_SIGNING_ENABLED, false)
                .putProperty(CHUNK_ENCODING_ENABLED, true)
                .putProperty(CHECKSUM_ALGORITHM, CRC32)
        );

        SignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("x-amz-content-sha256"))
            .hasValue("STREAMING-UNSIGNED-PAYLOAD-TRAILER");
        assertThat(signedRequest.request().firstMatchingHeader(Header.CONTENT_LENGTH)).hasValue("62");
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-decoded-content-length")).hasValue("20");
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-trailer")).hasValue("x-amz-checksum-crc32");
    }

    @Test
    void signAsync_WithPayloadSigningFalseAndChunkEncodingTrueAndTrailer_DelegatesToAwsChunkedPayloadSigner_futureBehavior() {
        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> httpRequest
                .putHeader(Header.CONTENT_LENGTH, "20"),
            signRequest -> signRequest
                .putProperty(PAYLOAD_SIGNING_ENABLED, false)
                .putProperty(CHUNK_ENCODING_ENABLED, true)
                .putProperty(CHECKSUM_ALGORITHM, CRC32)
        );

        AsyncSignedRequest signedRequest = signer.signAsync(request).join();

        assertThat(signedRequest.request().firstMatchingHeader("x-amz-content-sha256"))
            .hasValue("STREAMING-UNSIGNED-PAYLOAD-TRAILER");
        assertThat(signedRequest.request().firstMatchingHeader(CONTENT_ENCODING)).hasValue("aws-chunked");
        assertThat(signedRequest.request().firstMatchingHeader(Header.CONTENT_LENGTH)).hasValue("62");
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-decoded-content-length")).hasValue("20");
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-trailer")).hasValue("x-amz-checksum-crc32");
    }

    @Test
    void sign_WithPayloadSigningFalseAndChunkEncodingTrue_DelegatesToUnsignedPayload() {
        // Currently, there is no use-case for unsigned chunk-encoding without trailers, so we should assert it falls back to
        // unsigned-payload (not chunk-encoded)
        SignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> httpRequest
                .putHeader(Header.CONTENT_LENGTH, "20"),
            signRequest -> signRequest
                .putProperty(PAYLOAD_SIGNING_ENABLED, false)
                .putProperty(CHUNK_ENCODING_ENABLED, true)
        );

        SignedRequest signedRequest = signer.sign(request);
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-content-sha256")).hasValue("UNSIGNED-PAYLOAD");
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-decoded-content-length")).isNotPresent();
    }

    @Test
    void signAsync_WithPayloadSigningFalseAndChunkEncodingTrueWithoutTrailer_DelegatesToUnsignedPayload() {
        // Currently, there is no use-case for unsigned chunk-encoding without trailers, so we should assert it falls back to
        // unsigned-payload (not chunk-encoded)
        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> httpRequest
                .putHeader(Header.CONTENT_LENGTH, "20"),
            signRequest -> signRequest
                .putProperty(PAYLOAD_SIGNING_ENABLED, false)
                .putProperty(CHUNK_ENCODING_ENABLED, true)
        );

        AsyncSignedRequest signedRequest = signer.signAsync(request).join();
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-content-sha256")).hasValue("UNSIGNED-PAYLOAD");
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-decoded-content-length")).isNotPresent();
    }

    @Test
    void sign_WithPayloadSigningFalseAndChunkEncodingTrueWithChecksumHeader_DelegatesToUnsignedPayload() {
        // If a checksum is given explicitly, we shouldn't treat it as a flexible-checksum-enabled request
        SignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> httpRequest
                .putHeader(Header.CONTENT_LENGTH, "20")
                .putHeader("x-amz-checksum-crc32", "bogus"),
            signRequest -> signRequest
                .putProperty(PAYLOAD_SIGNING_ENABLED, false)
                .putProperty(CHUNK_ENCODING_ENABLED, true)
        );

        SignedRequest signedRequest = signer.sign(request);
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-content-sha256")).hasValue("UNSIGNED-PAYLOAD");
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-checksum-crc32")).hasValue("bogus");
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-decoded-content-length")).isNotPresent();
    }

    @Test
    void signAsync_WithPayloadSigningFalseAndChunkEncodingTrueWithChecksumHeader_DelegatesToUnsignedPayload() {
        // If a checksum is given explicitly, we shouldn't treat it as a flexible-checksum-enabled request
        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> httpRequest
                .putHeader(Header.CONTENT_LENGTH, "20")
                .putHeader("x-amz-checksum-crc32", "bogus"),
            signRequest -> signRequest
                .putProperty(PAYLOAD_SIGNING_ENABLED, false)
                .putProperty(CHUNK_ENCODING_ENABLED, true)
        );

        AsyncSignedRequest signedRequest = signer.signAsync(request).join();
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-content-sha256")).hasValue("UNSIGNED-PAYLOAD");
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-checksum-crc32")).hasValue("bogus");
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-decoded-content-length")).isNotPresent();
    }

    @Test
    void sign_withChecksumAlgorithm_DelegatesToChecksummerWithThatChecksumAlgorithm() {
        SignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> {
            },
            signRequest -> signRequest.putProperty(CHECKSUM_ALGORITHM, CRC32)
        );

        SignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("x-amz-checksum-crc32")).isPresent();
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-content-sha256")).isPresent();
    }

    @Test
    void signAsync_withChecksumAlgorithm_DelegatesToChecksummerWithThatChecksumAlgorithm() {
        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> {
            },
            signRequest -> signRequest.putProperty(CHECKSUM_ALGORITHM, CRC32)
        );

        AsyncSignedRequest signedRequest = signer.signAsync(request).join();

        assertThat(signedRequest.request().firstMatchingHeader("x-amz-checksum-crc32")).isPresent();
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-content-sha256")).isPresent();
    }

    @Test
    void sign_withChecksumAlgorithmAndPayloadSigningFalse_DelegatesToChecksummerWithThatChecksumAlgorithm() {
        SignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> {
            },
            signRequest -> signRequest
                .putProperty(CHECKSUM_ALGORITHM, CRC32)
                .putProperty(PAYLOAD_SIGNING_ENABLED, false)
        );

        SignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("x-amz-checksum-crc32")).isPresent();
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-content-sha256")).hasValue("UNSIGNED-PAYLOAD");
    }

    @Test
    void signAsync_withChecksumAlgorithmAndPayloadSigningFalse_DelegatesToChecksummerWithThatChecksumAlgorithm() {
        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> {
            },
            signRequest -> signRequest
                .putProperty(CHECKSUM_ALGORITHM, CRC32)
                .putProperty(PAYLOAD_SIGNING_ENABLED, false)
        );

        AsyncSignedRequest signedRequest = signer.signAsync(request).join();

        assertThat(signedRequest.request().firstMatchingHeader("x-amz-checksum-crc32")).isPresent();
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-content-sha256")).hasValue("UNSIGNED-PAYLOAD");
    }

    @Test
    void sign_WithPayloadSigningFalseAndHttp_FallsBackToPayloadSigning() {
        SignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> httpRequest.uri(URI.create("http://demo.us-east-1.amazonaws.com")),
            signRequest -> signRequest
                .putProperty(PAYLOAD_SIGNING_ENABLED, false)
        );

        SignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("x-amz-content-sha256"))
            .hasValue("a15c8292b1d12abbbbe4148605f7872fbdf645618fee5ab0e8072a7b34f155e2");
    }

    @Test
    void signAsync_WithPayloadSigningFalseAndHttp_FallsBackToPayloadSigning() {
        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> httpRequest.uri(URI.create("http://demo.us-east-1.amazonaws.com")),
            signRequest -> signRequest
                .putProperty(PAYLOAD_SIGNING_ENABLED, false)
        );

        AsyncSignedRequest signedRequest = signer.signAsync(request).join();

        assertThat(signedRequest.request().firstMatchingHeader("x-amz-content-sha256"))
            .hasValue("a15c8292b1d12abbbbe4148605f7872fbdf645618fee5ab0e8072a7b34f155e2");
    }

    @Test
    void sign_WithPayloadSigningTrueAndChunkEncodingTrueAndHttp_SignsPayload() {
        SignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> httpRequest.uri(URI.create("http://demo.us-east-1.amazonaws.com")),
            signRequest -> signRequest
                .putProperty(PAYLOAD_SIGNING_ENABLED, true)
                .putProperty(CHUNK_ENCODING_ENABLED, true)
        );

        SignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("x-amz-content-sha256"))
            .hasValue("STREAMING-AWS4-HMAC-SHA256-PAYLOAD");
        assertThat(signedRequest.request().firstMatchingHeader(Header.CONTENT_LENGTH)).hasValue("193");
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-decoded-content-length")).hasValue("20");
    }

    @Test
    @Disabled("Fallback to signing is disabled to match pre-SRA behavior")
    // TODO: Enable this test once we figure out what the expected behavior is post SRA. See JAVA-8078
    void signAsync_WithPayloadSigningTrueAndChunkEncodingTrueAndHttp_RespectsPayloadSigning() {
        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> httpRequest.uri(URI.create("http://demo.us-east-1.amazonaws.com")),
            signRequest -> signRequest
                .putProperty(PAYLOAD_SIGNING_ENABLED, true)
                .putProperty(CHUNK_ENCODING_ENABLED, true)
        );

        AsyncSignedRequest signedRequest = signer.signAsync(request).join();

        assertThat(signedRequest.request().firstMatchingHeader("x-amz-content-sha256"))
            .hasValue("STREAMING-AWS4-HMAC-SHA256-PAYLOAD");
        assertThat(signedRequest.request().firstMatchingHeader(Header.CONTENT_LENGTH)).hasValue("193");
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-decoded-content-length")).hasValue("20");
    }

    @Test
    @Disabled("Fallback to signing is disabled to match pre-SRA behavior")
    // TODO: Enable this test once we figure out what the expected behavior is post SRA. See JAVA-8078
    void sign_WithPayloadSigningFalseAndChunkEncodingTrueAndHttp_SignsPayload() {
        SignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> httpRequest.uri(URI.create("http://demo.us-east-1.amazonaws.com")),
            signRequest -> signRequest
                .putProperty(PAYLOAD_SIGNING_ENABLED, false)
                .putProperty(CHUNK_ENCODING_ENABLED, true)
        );

        SignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("x-amz-content-sha256"))
            .hasValue("STREAMING-AWS4-HMAC-SHA256-PAYLOAD");
        assertThat(signedRequest.request().firstMatchingHeader(Header.CONTENT_LENGTH)).hasValue("193");
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-decoded-content-length")).hasValue("20");
    }

    @Test
    @Disabled("Fallback to signing is disabled to match pre-SRA behavior")
    // TODO: Enable this test once we figure out what the expected behavior is post SRA. See JAVA-8078
    void signAsync_WithPayloadSigningFalseAndChunkEncodingTrueAndHttp_FallsBackToPayloadSigning() {
        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> httpRequest.uri(URI.create("http://demo.us-east-1.amazonaws.com")),
            signRequest -> signRequest
                .putProperty(PAYLOAD_SIGNING_ENABLED, false)
                .putProperty(CHUNK_ENCODING_ENABLED, true)
        );

        AsyncSignedRequest signedRequest = signer.signAsync(request).join();

        assertThat(signedRequest.request().firstMatchingHeader("x-amz-content-sha256"))
            .hasValue("STREAMING-AWS4-HMAC-SHA256-PAYLOAD");
        assertThat(signedRequest.request().firstMatchingHeader(Header.CONTENT_LENGTH)).hasValue("193");
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-decoded-content-length")).hasValue("20");
    }

    @Test
    void sign_WithPayloadSigningFalseAndChunkEncodingTrueAndFlexibleChecksumAndHttp_SignsPayload() {
        SignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> httpRequest.uri(URI.create("http://demo.us-east-1.amazonaws.com")),
            signRequest -> signRequest
                .putProperty(PAYLOAD_SIGNING_ENABLED, false)
                .putProperty(CHUNK_ENCODING_ENABLED, true)
                .putProperty(CHECKSUM_ALGORITHM, CRC32)
        );

        SignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("x-amz-content-sha256"))
            .hasValue("STREAMING-AWS4-HMAC-SHA256-PAYLOAD-TRAILER");
        assertThat(signedRequest.request().firstMatchingHeader(Header.CONTENT_LENGTH)).hasValue("314");
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-decoded-content-length")).hasValue("20");
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-trailer")).hasValue("x-amz-checksum-crc32");
    }

    @Test
    @Disabled("Fallback to signing is disabled to match pre-SRA behavior")
    // TODO: Enable this test once we figure out what the expected behavior is post SRA. See JAVA-8078
    void signAsync_WithPayloadSigningFalseAndChunkEncodingTrueAndFlexibleChecksumAndHttp_FallsBackToPayloadSigning() {
        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> httpRequest.uri(URI.create("http://demo.us-east-1.amazonaws.com")),
            signRequest -> signRequest
                .putProperty(PAYLOAD_SIGNING_ENABLED, false)
                .putProperty(CHUNK_ENCODING_ENABLED, true)
                .putProperty(CHECKSUM_ALGORITHM, CRC32)
        );

        AsyncSignedRequest signedRequest = signer.signAsync(request).join();

        assertThat(signedRequest.request().firstMatchingHeader("x-amz-content-sha256"))
            .hasValue("STREAMING-AWS4-HMAC-SHA256-PAYLOAD-TRAILER");
        assertThat(signedRequest.request().firstMatchingHeader(Header.CONTENT_LENGTH)).hasValue("314");
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-decoded-content-length")).hasValue("20");
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-trailer")).hasValue("x-amz-checksum-crc32");
    }

    @Test
    void sign_WithPayloadSigningFalse_chunkEncodingTrue_cacheEmpty_storesComputedChecksum() throws IOException {
        PayloadChecksumStore cache = PayloadChecksumStore.create();

        SignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> httpRequest.uri(URI.create("http://demo.us-east-1.amazonaws.com")),
            signRequest -> signRequest
                .putProperty(PAYLOAD_SIGNING_ENABLED, false)
                .putProperty(CHUNK_ENCODING_ENABLED, true)
                .putProperty(CHECKSUM_ALGORITHM, CRC32)
                .putProperty(CHECKSUM_STORE, cache)
        );

        SignedRequest signedRequest = signer.sign(request);

        String requestPayload = IoUtils.toUtf8String(signedRequest.payload().get().newStream());

        byte[] payloadChecksum = computeChecksum(CRC32, testPayload());

        assertThat(cache.getChecksumValue(CRC32)).isEqualTo(payloadChecksum);
        assertThat(requestPayload).contains("x-amz-checksum-crc32:" + BinaryUtils.toBase64(payloadChecksum) + "\r\n");
    }

    @Test
    void sign_WithPayloadSigningFalse_chunkEncodingTrue_cacheContainsChecksum_usesCachedValue() throws IOException {
        PayloadChecksumStore cache = PayloadChecksumStore.create();

        byte[] checksumValue = "my-checksum".getBytes(StandardCharsets.UTF_8);
        cache.putChecksumValue(CRC32, checksumValue);

        SignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> httpRequest.uri(URI.create("http://demo.us-east-1.amazonaws.com")),
            signRequest -> signRequest
                .putProperty(PAYLOAD_SIGNING_ENABLED, false)
                .putProperty(CHUNK_ENCODING_ENABLED, true)
                .putProperty(CHECKSUM_ALGORITHM, CRC32)
                .putProperty(CHECKSUM_STORE, cache)
        );

        SignedRequest signedRequest = signer.sign(request);

        String requestPayload = IoUtils.toUtf8String(signedRequest.payload().get().newStream());

        assertThat(requestPayload).contains("x-amz-checksum-crc32:" + BinaryUtils.toBase64(checksumValue) + "\r\n");
    }

    @Test
    void sign_withPayloadSigningTrue_chunkEncodingFalse_withChecksum_cacheContainsCrc32AndSha256_usesCachedValues() {
        PayloadChecksumStore cache = PayloadChecksumStore.create();

        byte[] crc32Value = "my-crc32-checksum".getBytes(StandardCharsets.UTF_8);
        cache.putChecksumValue(CRC32, crc32Value);

        byte[] sha256Value = "my-sha256-checksum".getBytes(StandardCharsets.UTF_8);
        cache.putChecksumValue(SHA256, sha256Value);

        SignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> httpRequest.uri(URI.create("http://demo.us-east-1.amazonaws.com")),
            signRequest -> signRequest
                .putProperty(PAYLOAD_SIGNING_ENABLED, true)
                .putProperty(CHUNK_ENCODING_ENABLED, false)
                .putProperty(CHECKSUM_ALGORITHM, CRC32)
                .putProperty(CHECKSUM_STORE, cache)
        );

        SignedRequest signedRequest = signer.sign(request);

        SdkHttpRequest httpRequest = signedRequest.request();
        assertThat(httpRequest.firstMatchingHeader("x-amz-content-sha256")).hasValue(BinaryUtils.toHex(sha256Value));
        assertThat(httpRequest.firstMatchingHeader("x-amz-checksum-crc32")).hasValue(BinaryUtils.toBase64(crc32Value));
    }

    @Test
    void sign_withPayloadSigningTrue_chunkEncodingFalse_withChecksum_cacheEmpty_storesComputeChecksums() {
        PayloadChecksumStore cache = PayloadChecksumStore.create();

        SignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> httpRequest.uri(URI.create("http://demo.us-east-1.amazonaws.com")),
            signRequest -> signRequest
                .putProperty(PAYLOAD_SIGNING_ENABLED, true)
                .putProperty(CHUNK_ENCODING_ENABLED, false)
                .putProperty(CHECKSUM_ALGORITHM, CRC32)
                .putProperty(CHECKSUM_STORE, cache)
        );

        signer.sign(request);

        byte[] crc32Value = computeChecksum(CRC32, testPayload());
        byte[] sha256Value = computeChecksum(SHA256, testPayload());

        assertThat(cache.getChecksumValue(SHA256)).isEqualTo(sha256Value);
        assertThat(cache.getChecksumValue(CRC32)).isEqualTo(crc32Value);
    }

    @Test
    void signAsync_WithPayloadSigningFalse_chunkEncodingTrue_cacheEmpty_storesComputedChecksum() throws IOException {
        PayloadChecksumStore cache = PayloadChecksumStore.create();

        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> httpRequest.uri(URI.create("http://demo.us-east-1.amazonaws.com")),
            signRequest -> signRequest
                .putProperty(PAYLOAD_SIGNING_ENABLED, false)
                .putProperty(CHUNK_ENCODING_ENABLED, true)
                .putProperty(CHECKSUM_ALGORITHM, CRC32)
                .putProperty(CHECKSUM_STORE, cache)
        );

        AsyncSignedRequest signedRequest = signer.signAsync(request).join();

        getAllItems(signedRequest.payload().get());
        assertThat(cache.getChecksumValue(CRC32)).isEqualTo(computeChecksum(CRC32, testPayload()));
    }

    @Test
    void signAsync_WithPayloadSigningFalse_chunkEncodingTrue_cacheContainsChecksum_usesCachedValue() throws IOException {
        PayloadChecksumStore cache = PayloadChecksumStore.create();

        byte[] checksumValue = "my-checksum".getBytes(StandardCharsets.UTF_8);
        cache.putChecksumValue(CRC32, checksumValue);

        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> httpRequest.uri(URI.create("http://demo.us-east-1.amazonaws.com")),
            signRequest -> signRequest
                .putProperty(PAYLOAD_SIGNING_ENABLED, false)
                .putProperty(CHUNK_ENCODING_ENABLED, true)
                .putProperty(CHECKSUM_ALGORITHM, CRC32)
                .putProperty(CHECKSUM_STORE, cache)
        );

        AsyncSignedRequest signedRequest = signer.signAsync(request).join();

        List<ByteBuffer> content = getAllItems(signedRequest.payload().get());
        String contentAsString = content.stream().map(DefaultAwsV4HttpSignerTest::bufferAsString).collect(Collectors.joining());
        assertThat(contentAsString).contains("x-amz-checksum-crc32:" + BinaryUtils.toBase64(checksumValue) + "\r\n");
    }

    private static byte[] computeChecksum(ChecksumAlgorithm algorithm, byte[] data) {
        SdkChecksum checksum = SdkChecksum.forAlgorithm(algorithm);
        checksum.update(data, 0, data.length);
        return checksum.getChecksumBytes();
    }

    private List<ByteBuffer> getAllItems(Publisher<ByteBuffer> publisher) {
        return Flowable.fromPublisher(publisher).toList().blockingGet();
    }

    private static String bufferAsString(ByteBuffer buffer) {
        return StandardCharsets.UTF_8.decode(buffer.duplicate()).toString();
    }
}
