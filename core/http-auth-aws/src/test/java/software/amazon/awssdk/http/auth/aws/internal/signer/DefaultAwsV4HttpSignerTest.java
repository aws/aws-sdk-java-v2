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
import static software.amazon.awssdk.http.auth.aws.TestUtils.generateBasicAsyncRequest;
import static software.amazon.awssdk.http.auth.aws.TestUtils.generateBasicRequest;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner.AUTH_LOCATION;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner.CHECKSUM_ALGORITHM;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner.CHUNK_ENCODING_ENABLED;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner.EXPIRATION_DURATION;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner.PAYLOAD_SIGNING_ENABLED;

import java.net.URI;
import java.time.Duration;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import software.amazon.awssdk.http.Header;
import software.amazon.awssdk.http.auth.aws.TestUtils;
import software.amazon.awssdk.http.auth.aws.eventstream.internal.io.SigV4DataFramePublisher;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner.AuthLocation;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.utils.ClassLoaderHelper;

/**
 * Test the delegation of signing to the correct implementations.
 */
// TODO(sra-identity-and-auth): missing tests for async code path
public class DefaultAwsV4HttpSignerTest {

    DefaultAwsV4HttpSigner signer = new DefaultAwsV4HttpSigner();

    @Test
    public void sign_WithNoAdditonalProperties_DelegatesToHeaderSigner() {
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
    public void sign_WithQueryAuthLocation_DelegatesToQuerySigner() {
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
    public void sign_WithHeaderAuthLocationAndExpirationDuration_Throws() {
        SignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> {
            },
            signRequest -> signRequest
                .putProperty(AUTH_LOCATION, AuthLocation.HEADER)
                .putProperty(EXPIRATION_DURATION, Duration.ZERO)
        );

        assertThrows(UnsupportedOperationException.class, () -> signer.sign(request));
    }

    @Test
    public void sign_withAnonymousCreds_shouldNotSign() {
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
    public void sign_WithQueryAuthLocationAndExpiration_DelegatesToPresignedSigner() {
        SignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> {
            },
            signRequest -> signRequest
                .putProperty(AUTH_LOCATION, AuthLocation.QUERY_STRING)
                .putProperty(EXPIRATION_DURATION, Duration.ZERO)
        );

        SignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingRawQueryParameter("X-Amz-Expires")).isPresent();
    }

    @Test
    public void sign_WithPayloadSigningFalse_DelegatesToUnsignedPayloadSigner() {
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
    public void sign_WithEventStreamContentTypeWithoutHttpAuthAwsEventStreamModule_throws() {
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
    public void sign_WithChunkEncodingTrue_DelegatesToAwsChunkedPayloadSigner() {
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
        Assertions.assertThat(signedRequest.request().firstMatchingHeader(Header.CONTENT_LENGTH)).hasValue("193");
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-decoded-content-length")).hasValue("20");
    }

    @Test
    public void sign_WithChunkEncodingTrueAndChecksumAlgorithm_DelegatesToAwsChunkedPayloadSigner() {
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
        Assertions.assertThat(signedRequest.request().firstMatchingHeader(Header.CONTENT_LENGTH)).hasValue("314");
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-decoded-content-length")).hasValue("20");
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-trailer")).hasValue("x-amz-checksum-crc32");
    }

    @Test
    public void sign_WithPayloadSigningFalseAndChunkEncodingTrueAndTrailer_DelegatesToAwsChunkedPayloadSigner() {
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
        Assertions.assertThat(signedRequest.request().firstMatchingHeader(Header.CONTENT_LENGTH)).hasValue("62");
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-decoded-content-length")).hasValue("20");
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-trailer")).hasValue("x-amz-checksum-crc32");
    }

    @Test
    public void sign_WithPayloadSigningFalseAndChunkEncodingTrueWithoutTrailer_Throws() {
        SignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> httpRequest
                .putHeader(Header.CONTENT_LENGTH, "20"),
            signRequest -> signRequest
                .putProperty(PAYLOAD_SIGNING_ENABLED, false)
                .putProperty(CHUNK_ENCODING_ENABLED, true)
        );

        assertThrows(UnsupportedOperationException.class, () -> signer.sign(request));
    }

    @Test
    public void sign_withChecksumAlgorithm_DelegatesToChecksummerWithThatChecksumAlgorithm() {
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
    public void sign_withChecksumAlgorithmAndPayloadSigningFalse_DelegatesToChecksummerWithThatChecksumAlgorithm() {
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
    public void sign_WithEventStreamContentType_DelegatesToEventStreamPayloadSigner() {
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
    public void sign_WithEventStreamContentTypeAndUnsignedPayload_Throws() {
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
    public void sign_WithPayloadSigningFalseAndHttp_FallsBackToPayloadSigning() {
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
    public void sign_WithPayloadSigningNullAndHttp_FallsBackToPayloadSigning() {
        SignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> httpRequest.uri(URI.create("http://demo.us-east-1.amazonaws.com")),
            signRequest -> signRequest
                .putProperty(PAYLOAD_SIGNING_ENABLED, null)
        );

        SignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("x-amz-content-sha256"))
            .hasValue("a15c8292b1d12abbbbe4148605f7872fbdf645618fee5ab0e8072a7b34f155e2");
    }
}
