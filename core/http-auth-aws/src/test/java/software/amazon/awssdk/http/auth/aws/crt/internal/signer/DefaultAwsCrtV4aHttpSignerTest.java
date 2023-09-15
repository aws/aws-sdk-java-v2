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

package software.amazon.awssdk.http.auth.aws.crt.internal.signer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static software.amazon.awssdk.checksums.DefaultChecksumAlgorithm.CRC32;
import static software.amazon.awssdk.crt.auth.signing.AwsSigningConfig.AwsSignatureType.HTTP_REQUEST_VIA_HEADERS;
import static software.amazon.awssdk.crt.auth.signing.AwsSigningConfig.AwsSignatureType.HTTP_REQUEST_VIA_QUERY_PARAMS;
import static software.amazon.awssdk.crt.auth.signing.AwsSigningConfig.AwsSignedBodyValue.STREAMING_AWS4_ECDSA_P256_SHA256_PAYLOAD;
import static software.amazon.awssdk.crt.auth.signing.AwsSigningConfig.AwsSignedBodyValue.STREAMING_AWS4_ECDSA_P256_SHA256_PAYLOAD_TRAILER;
import static software.amazon.awssdk.crt.auth.signing.AwsSigningConfig.AwsSignedBodyValue.STREAMING_UNSIGNED_PAYLOAD_TRAILER;
import static software.amazon.awssdk.crt.auth.signing.AwsSigningConfig.AwsSignedBodyValue.UNSIGNED_PAYLOAD;
import static software.amazon.awssdk.crt.auth.signing.AwsSigningConfig.AwsSigningAlgorithm.SIGV4_ASYMMETRIC;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner.CHECKSUM_ALGORITHM;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4aHttpSigner.AUTH_LOCATION;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4aHttpSigner.AuthLocation;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4aHttpSigner.CHUNK_ENCODING_ENABLED;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4aHttpSigner.EXPIRATION_DURATION;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4aHttpSigner.PAYLOAD_SIGNING_ENABLED;
import static software.amazon.awssdk.http.auth.aws.TestUtils.AnonymousCredentialsIdentity;
import static software.amazon.awssdk.http.auth.aws.crt.TestUtils.generateBasicRequest;
import static software.amazon.awssdk.http.auth.aws.crt.internal.util.CrtUtils.toCredentials;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.crt.auth.signing.AwsSigningConfig;
import software.amazon.awssdk.http.Header;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;


/**
 * Functional tests for the Sigv4a signer. These tests call the CRT native signer code.
 */
public class DefaultAwsCrtV4aHttpSignerTest {

    DefaultAwsCrtV4aHttpSigner signer = new DefaultAwsCrtV4aHttpSigner();

    @Test
    public void sign_withBasicRequest_shouldSignWithHeaders() {
        AwsCredentialsIdentity credentials =
            AwsCredentialsIdentity.create("AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");
        SignRequest<AwsCredentialsIdentity> request = generateBasicRequest(
            credentials,
            httpRequest -> httpRequest.port(443),
            signRequest -> {
            }
        );

        AwsSigningConfig expectedSigningConfig = new AwsSigningConfig();
        expectedSigningConfig.setCredentials(toCredentials(request.identity()));
        expectedSigningConfig.setService("demo");
        expectedSigningConfig.setRegion("aws-global");
        expectedSigningConfig.setAlgorithm(SIGV4_ASYMMETRIC);
        expectedSigningConfig.setTime(1596476903000L);
        expectedSigningConfig.setUseDoubleUriEncode(true);
        expectedSigningConfig.setShouldNormalizeUriPath(true);
        expectedSigningConfig.setSignatureType(HTTP_REQUEST_VIA_HEADERS);

        SignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("Host")).hasValue("demo.us-east-1.amazonaws.com");
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Date")).hasValue("20200803T174823Z");
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Region-Set")).hasValue("aws-global");
        assertThat(signedRequest.request().firstMatchingHeader("Authorization")).isPresent();

    }

    @Test
    public void sign_withQuery_shouldSignWithQueryParams() {
        AwsCredentialsIdentity credentials =
            AwsCredentialsIdentity.create("AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");
        SignRequest<AwsCredentialsIdentity> request = generateBasicRequest(
            credentials,
            httpRequest -> httpRequest.port(443),
            signRequest ->
                signRequest.putProperty(AUTH_LOCATION, AuthLocation.QUERY_STRING)
        );

        AwsSigningConfig expectedSigningConfig = new AwsSigningConfig();
        expectedSigningConfig.setCredentials(toCredentials(request.identity()));
        expectedSigningConfig.setService("demo");
        expectedSigningConfig.setRegion("aws-global");
        expectedSigningConfig.setAlgorithm(SIGV4_ASYMMETRIC);
        expectedSigningConfig.setTime(1596476903000L);
        expectedSigningConfig.setUseDoubleUriEncode(true);
        expectedSigningConfig.setShouldNormalizeUriPath(true);
        expectedSigningConfig.setSignatureType(HTTP_REQUEST_VIA_QUERY_PARAMS);

        SignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingRawQueryParameter("X-Amz-Algorithm"))
            .hasValue("AWS4-ECDSA-P256-SHA256");
        assertThat(signedRequest.request().firstMatchingRawQueryParameter("X-Amz-Credential"))
            .hasValue("AKIDEXAMPLE/20200803/demo/aws4_request");
        assertThat(signedRequest.request().firstMatchingRawQueryParameter("X-Amz-Date")).hasValue("20200803T174823Z");
        assertThat(signedRequest.request().firstMatchingRawQueryParameter("X-Amz-SignedHeaders"))
            .hasValue("host;x-amz-archive-description");
        assertThat(signedRequest.request().firstMatchingRawQueryParameter("X-Amz-Region-Set")).hasValue("aws-global");
        assertThat(signedRequest.request().firstMatchingRawQueryParameter("X-Amz-Signature")).isPresent();
    }

    @Test
    public void sign_withQueryAndExpiration_shouldSignWithQueryParamsAndExpire() {
        AwsCredentialsIdentity credentials =
            AwsCredentialsIdentity.create("AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");
        SignRequest<AwsCredentialsIdentity> request = generateBasicRequest(
            credentials,
            httpRequest -> httpRequest.port(443),
            signRequest -> signRequest
                .putProperty(AUTH_LOCATION, AuthLocation.QUERY_STRING)
                .putProperty(EXPIRATION_DURATION, Duration.ofSeconds(1))
        );

        AwsSigningConfig expectedSigningConfig = new AwsSigningConfig();
        expectedSigningConfig.setCredentials(toCredentials(request.identity()));
        expectedSigningConfig.setService("demo");
        expectedSigningConfig.setRegion("aws-global");
        expectedSigningConfig.setAlgorithm(SIGV4_ASYMMETRIC);
        expectedSigningConfig.setTime(1596476903000L);
        expectedSigningConfig.setUseDoubleUriEncode(true);
        expectedSigningConfig.setShouldNormalizeUriPath(true);
        expectedSigningConfig.setSignatureType(HTTP_REQUEST_VIA_QUERY_PARAMS);
        expectedSigningConfig.setExpirationInSeconds(1);

        SignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingRawQueryParameter("X-Amz-Algorithm"))
            .hasValue("AWS4-ECDSA-P256-SHA256");
        assertThat(signedRequest.request().firstMatchingRawQueryParameter("X-Amz-Credential"))
            .hasValue("AKIDEXAMPLE/20200803/demo/aws4_request");
        assertThat(signedRequest.request().firstMatchingRawQueryParameter("X-Amz-Date")).hasValue("20200803T174823Z");
        assertThat(signedRequest.request().firstMatchingRawQueryParameter("X-Amz-SignedHeaders"))
            .hasValue("host;x-amz-archive-description");
        assertThat(signedRequest.request().firstMatchingRawQueryParameter("X-Amz-Region-Set")).hasValue("aws-global");
        assertThat(signedRequest.request().firstMatchingRawQueryParameter("X-Amz-Signature")).isPresent();
        assertThat(signedRequest.request().firstMatchingRawQueryParameter("X-Amz-Expires")).hasValue("1");
    }

    @Test
    public void sign_withUnsignedPayload_shouldNotSignPayload() {
        AwsCredentialsIdentity credentials =
            AwsCredentialsIdentity.create("AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");
        SignRequest<AwsCredentialsIdentity> request = generateBasicRequest(
            credentials,
            httpRequest -> {
            },
            signRequest -> signRequest
                .putProperty(PAYLOAD_SIGNING_ENABLED, false)
        );

        AwsSigningConfig expectedSigningConfig = new AwsSigningConfig();
        expectedSigningConfig.setCredentials(toCredentials(request.identity()));
        expectedSigningConfig.setService("demo");
        expectedSigningConfig.setRegion("aws-global");
        expectedSigningConfig.setAlgorithm(SIGV4_ASYMMETRIC);
        expectedSigningConfig.setTime(1596476903000L);
        expectedSigningConfig.setUseDoubleUriEncode(true);
        expectedSigningConfig.setShouldNormalizeUriPath(true);
        expectedSigningConfig.setSignatureType(HTTP_REQUEST_VIA_HEADERS);
        expectedSigningConfig.setSignedBodyValue(UNSIGNED_PAYLOAD);

        SignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("Host")).hasValue("demo.us-east-1.amazonaws.com");
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Date")).hasValue("20200803T174823Z");
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Region-Set")).hasValue("aws-global");
        assertThat(signedRequest.request().firstMatchingHeader("Authorization")).isPresent();
    }

    @Test
    public void sign_withAnonymousCredentials_shouldNotSign() {
        AwsCredentialsIdentity credentials = new AnonymousCredentialsIdentity();
        SignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            credentials,
            httpRequest -> {
            },
            signRequest -> {
            }
        );

        SignedRequest signedRequest = signer.sign(request);

        assertNull(signedRequest.request().headers().get("Authorization"));
    }

    @Test
    public void signAsync_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class,
                     () -> signer.signAsync((AsyncSignRequest<? extends AwsCredentialsIdentity>) null)
        );
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
                               .hasValue(STREAMING_AWS4_ECDSA_P256_SHA256_PAYLOAD);
        assertThat(signedRequest.request().firstMatchingHeader(Header.CONTENT_LENGTH)).isNotPresent();
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
                               .hasValue(STREAMING_AWS4_ECDSA_P256_SHA256_PAYLOAD_TRAILER);
        assertThat(signedRequest.request().firstMatchingHeader(Header.CONTENT_LENGTH)).isNotPresent();
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
                               .hasValue(STREAMING_UNSIGNED_PAYLOAD_TRAILER);
        assertThat(signedRequest.request().firstMatchingHeader(Header.CONTENT_LENGTH)).isNotPresent();
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
}
