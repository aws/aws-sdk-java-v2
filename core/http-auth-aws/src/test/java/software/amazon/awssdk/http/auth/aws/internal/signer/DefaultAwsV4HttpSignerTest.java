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
import static software.amazon.awssdk.http.auth.aws.AwsV4HttpSigner.AUTH_LOCATION;
import static software.amazon.awssdk.http.auth.aws.AwsV4HttpSigner.CHECKSUM_ALGORITHM;
import static software.amazon.awssdk.http.auth.aws.AwsV4HttpSigner.CHUNK_ENCODING_ENABLED;
import static software.amazon.awssdk.http.auth.aws.AwsV4HttpSigner.EXPIRATION_DURATION;
import static software.amazon.awssdk.http.auth.aws.AwsV4HttpSigner.PAYLOAD_SIGNING_ENABLED;
import static software.amazon.awssdk.http.auth.aws.TestUtils.generateBasicRequest;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.Header;
import software.amazon.awssdk.http.auth.aws.AwsV4HttpSigner.AuthLocation;
import software.amazon.awssdk.http.auth.aws.TestUtils;
import software.amazon.awssdk.http.auth.spi.SyncSignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

/**
 * Test the delegation of signing to the correct implementations.
 */
// TODO(sra-identity-and-auth): missing tests for async code path
public class DefaultAwsV4HttpSignerTest {

    DefaultAwsV4HttpSigner signer = new DefaultAwsV4HttpSigner();

    @Test
    public void sign_WithNoAdditonalProperties_DelegatesToHeaderSigner() {
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> {
            },
            signRequest -> {
            }
        );

        SyncSignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("Authorization")).isPresent();
    }

    @Test
    public void sign_WithQueryAuthLocation_DelegatesToQuerySigner() {
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> {
            },
            signRequest -> signRequest.putProperty(AUTH_LOCATION, AuthLocation.QUERY_STRING)
        );

        SyncSignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingRawQueryParameter("X-Amz-Signature")).isPresent();
    }

    @Test
    public void sign_WithHeaderAuthLocationAndExpirationDuration_Throws() {
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
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
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            new TestUtils.AnonymousCredentialsIdentity(),
            httpRequest -> {
            },
            signRequest -> {
            }
        );

        SyncSignedRequest signedRequest = signer.sign(request);
        assertThat(signedRequest.request().firstMatchingRawQueryParameter("X-Amz-Signature"))
            .isNotPresent();
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-content-sha256")).isNotPresent();
    }

    @Test
    public void sign_WithQueryAuthLocationAndExpiration_DelegatesToPresignedSigner() {
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> {
            },
            signRequest -> signRequest
                .putProperty(AUTH_LOCATION, AuthLocation.QUERY_STRING)
                .putProperty(EXPIRATION_DURATION, Duration.ZERO)
        );

        SyncSignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingRawQueryParameter("X-Amz-Expires")).isPresent();
    }

    @Test
    public void sign_WithPayloadSigningFalse_DelegatesToUnsignedPayloadSigner() {
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> {
            },
            signRequest -> signRequest
                .putProperty(PAYLOAD_SIGNING_ENABLED, false)
        );

        SyncSignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("x-amz-content-sha256")).hasValue("UNSIGNED-PAYLOAD");
    }

    @Test
    public void sign_WithEventStreamContentTypeWithoutHttpAuthAwsEventStreamModule_throws() {
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> httpRequest
                .putHeader("Content-Type", "application/vnd.amazon.eventstream"),
            signRequest -> {
            }
        );

        assertThrows(RuntimeException.class, () -> signer.sign(request));
    }

    @Test
    public void sign_WithChunkEncodingTrue_DelegatesToAwsChunkedPayloadSigner() {
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> httpRequest
                .putHeader(Header.CONTENT_LENGTH, "20"),
            signRequest -> signRequest
                .putProperty(CHUNK_ENCODING_ENABLED, true)
        );

        SyncSignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("x-amz-content-sha256"))
            .hasValue("STREAMING-AWS4-HMAC-SHA256-PAYLOAD");
        assertThat(signedRequest.request().firstMatchingHeader(Header.CONTENT_LENGTH)).isNotPresent();
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-decoded-content-length")).hasValue("20");
    }

    @Test
    public void sign_WithChunkEncodingTrueAndChecksumAlgorithm_DelegatesToAwsChunkedPayloadSigner() {
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> httpRequest
                .putHeader(Header.CONTENT_LENGTH, "20"),
            signRequest -> signRequest
                .putProperty(CHUNK_ENCODING_ENABLED, true)
                .putProperty(CHECKSUM_ALGORITHM, CRC32)
        );

        SyncSignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("x-amz-content-sha256"))
            .hasValue("STREAMING-AWS4-HMAC-SHA256-PAYLOAD-TRAILER");
        assertThat(signedRequest.request().firstMatchingHeader(Header.CONTENT_LENGTH)).isNotPresent();
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-decoded-content-length")).hasValue("20");
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-trailer")).hasValue("x-amz-checksum-crc32");
    }

    @Test
    public void sign_WithPayloadSigningFalseAndChunkEncodingTrueAndTrailer_DelegatesToAwsChunkedPayloadSigner() {
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> httpRequest
                .putHeader(Header.CONTENT_LENGTH, "20"),
            signRequest -> signRequest
                .putProperty(PAYLOAD_SIGNING_ENABLED, false)
                .putProperty(CHUNK_ENCODING_ENABLED, true)
                .putProperty(CHECKSUM_ALGORITHM, CRC32)
        );

        SyncSignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("x-amz-content-sha256"))
            .hasValue("STREAMING-UNSIGNED-PAYLOAD-TRAILER");
        assertThat(signedRequest.request().firstMatchingHeader(Header.CONTENT_LENGTH)).isNotPresent();
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-decoded-content-length")).hasValue("20");
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-trailer")).hasValue("x-amz-checksum-crc32");
    }

    @Test
    public void sign_WithPayloadSigningFalseAndChunkEncodingTrueWithoutTrailer_Throws() {
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
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
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> {
            },
            signRequest -> signRequest.putProperty(CHECKSUM_ALGORITHM, CRC32)
        );

        SyncSignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("x-amz-checksum-crc32")).isPresent();
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-content-sha256")).isPresent();
    }

    @Test
    public void sign_withChecksumAlgorithmAndPayloadSigningFalse_DelegatesToChecksummerWithThatChecksumAlgorithm() {
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> {
            },
            signRequest -> signRequest
                .putProperty(CHECKSUM_ALGORITHM, CRC32)
                .putProperty(PAYLOAD_SIGNING_ENABLED, false)
        );

        SyncSignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("x-amz-checksum-crc32")).isPresent();
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-content-sha256")).hasValue("UNSIGNED-PAYLOAD");
    }
}
