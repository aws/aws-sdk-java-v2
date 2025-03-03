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
import static software.amazon.awssdk.checksums.DefaultChecksumAlgorithm.CRC32C;
import static software.amazon.awssdk.checksums.DefaultChecksumAlgorithm.CRC64NVME;
import static software.amazon.awssdk.checksums.DefaultChecksumAlgorithm.SHA1;
import static software.amazon.awssdk.checksums.DefaultChecksumAlgorithm.SHA256;
import static software.amazon.awssdk.crt.auth.signing.AwsSigningConfig.AwsSignatureType.HTTP_REQUEST_VIA_HEADERS;
import static software.amazon.awssdk.crt.auth.signing.AwsSigningConfig.AwsSignatureType.HTTP_REQUEST_VIA_QUERY_PARAMS;
import static software.amazon.awssdk.crt.auth.signing.AwsSigningConfig.AwsSignedBodyValue.STREAMING_AWS4_ECDSA_P256_SHA256_PAYLOAD;
import static software.amazon.awssdk.crt.auth.signing.AwsSigningConfig.AwsSignedBodyValue.STREAMING_AWS4_ECDSA_P256_SHA256_PAYLOAD_TRAILER;
import static software.amazon.awssdk.crt.auth.signing.AwsSigningConfig.AwsSignedBodyValue.STREAMING_UNSIGNED_PAYLOAD_TRAILER;
import static software.amazon.awssdk.crt.auth.signing.AwsSigningConfig.AwsSignedBodyValue.UNSIGNED_PAYLOAD;
import static software.amazon.awssdk.crt.auth.signing.AwsSigningConfig.AwsSigningAlgorithm.SIGV4_ASYMMETRIC;
import static software.amazon.awssdk.http.auth.aws.TestUtils.AnonymousCredentialsIdentity;
import static software.amazon.awssdk.http.auth.aws.crt.TestUtils.generateBasicRequest;
import static software.amazon.awssdk.http.auth.aws.crt.internal.util.CrtUtils.toCredentials;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.ChecksumUtil.readAll;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner.CHECKSUM_ALGORITHM;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner.SERVICE_SIGNING_NAME;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4aHttpSigner.AUTH_LOCATION;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4aHttpSigner.AuthLocation;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4aHttpSigner.CHUNK_ENCODING_ENABLED;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4aHttpSigner.EXPIRATION_DURATION;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4aHttpSigner.PAYLOAD_SIGNING_ENABLED;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4aHttpSigner.REGION_SET;
import static software.amazon.awssdk.http.auth.spi.signer.HttpSigner.SIGNING_CLOCK;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.checksums.spi.ChecksumAlgorithm;
import software.amazon.awssdk.crt.auth.signing.AwsSigningConfig;
import software.amazon.awssdk.http.Header;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.TestUtils;
import software.amazon.awssdk.http.auth.aws.signer.RegionSet;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.utils.ImmutableMap;


/**
 * Functional tests for the Sigv4a signer. These tests call the CRT native signer code.
 */
public class DefaultAwsCrtV4aHttpSignerTest {

    DefaultAwsCrtV4aHttpSigner signer = new DefaultAwsCrtV4aHttpSigner();

    private static final Map<ChecksumAlgorithm, String> ALGORITHM_TO_VALUE = ImmutableMap.<ChecksumAlgorithm, String>builder()
                                                                              .put(CRC32, "i9aeUg==")
                                                                              .put(CRC32C, "crUfeA==")
                                                                              .put(SHA1, "e1AsOh9IyGCa4hLN+2Od7jlnP14=")
                                                                              .put(SHA256,
                                                                                   "ZOyIygCyaOW6GjVnihtTFtIS9PNmskdyMlNKiuyjfzw=")
                                                                              .put(CRC64NVME, "OOJZ0D8xKts=")
                                                                              .build();

    public static Stream<Map.Entry<ChecksumAlgorithm, String>> checksumAlgorithmToValueParams() {
        return ALGORITHM_TO_VALUE.entrySet().stream();
    }

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
    void sign_requestWithQueryEncodedParamValue_shouldEncodedValue() {
        AwsCredentialsIdentity credentials =
            AwsCredentialsIdentity.create("AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");
        SignRequest<AwsCredentialsIdentity> request =
            SignRequest.builder(credentials)
                       .request(SdkHttpRequest.builder()
                                              .method(SdkHttpMethod.POST)
                                              .port(443)
                                              .putHeader("x-amz-archive-description", "test  test")
                                              .putHeader("Host", "demo.us-east-1.amazonaws.com")
                                              .encodedPath("/")
                                              .uri(URI.create("https://demo.us-east-1.amazonaws.com"))
                                              .appendRawQueryParameter("goodParam1", "123")
                                              .appendRawQueryParameter("badParam", "abc&xyz")
                                              .appendRawQueryParameter("goodParam2", "abc")
                                              .build())
                       .payload(() -> new ByteArrayInputStream("{\"TableName\": \"foo\"}".getBytes()))
                       .putProperty(REGION_SET, RegionSet.create("aws-global"))
                       .putProperty(SERVICE_SIGNING_NAME, "demo")
                       .putProperty(SIGNING_CLOCK, new TestUtils.TickingClock(Instant.ofEpochMilli(1596476903000L)))
                       .putProperty(AUTH_LOCATION, AuthLocation.QUERY_STRING)
                       .build();

        SignedRequest signedRequest = signer.sign(request);
        Map<String, List<String>> queryParam = signedRequest.request().rawQueryParameters();
        assertThat(queryParam).doesNotContainKey("xyz");
        assertThat(queryParam).containsKeys("goodParam1", "badParam", "goodParam2");

        assertThat(signedRequest.request().encodedQueryParameters())
            .isPresent()
            .get()
            .matches(str -> str.contains("badParam=abc%26xyz"));

        assertThat(signedRequest.request().firstMatchingRawQueryParameter("goodParam1"))
            .hasValue("123");
        assertThat(signedRequest.request().firstMatchingRawQueryParameter("badParam"))
            .hasValue("abc&xyz");
        assertThat(signedRequest.request().firstMatchingRawQueryParameter("goodParam2"))
            .hasValue("abc");

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
        assertThat(signedRequest.request().firstMatchingHeader(Header.CONTENT_LENGTH)).hasValue("353");
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-decoded-content-length")).hasValue("20");

        // Ensures that CRT runs correctly and without throwing an exception
        readAll(signedRequest.payload().get().newStream());
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
        assertThat(signedRequest.request().firstMatchingHeader(Header.CONTENT_LENGTH)).hasValue("554");
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-decoded-content-length")).hasValue("20");
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-trailer")).hasValue("x-amz-checksum-crc32");

        // Ensures that CRT runs correctly and without throwing an exception
        readAll(signedRequest.payload().get().newStream());
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
        assertThat(signedRequest.request().firstMatchingHeader(Header.CONTENT_LENGTH)).hasValue("62");
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-decoded-content-length")).hasValue("20");
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-trailer")).hasValue("x-amz-checksum-crc32");

        // Ensures that CRT runs correctly and without throwing an exception
        readAll(signedRequest.payload().get().newStream());
    }

    @Test
    public void sign_WithPayloadSigningFalseAndChunkEncodingTrueWithoutTrailer_DelegatesToUnsignedPayload() {
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

    @ParameterizedTest
    @MethodSource("checksumAlgorithmToValueParams")
    public void sign_checksumAlgorithmPresent_shouldAddChecksumHeader(Map.Entry<ChecksumAlgorithm, String> checksumToValue) {
        ChecksumAlgorithm checksumAlgorithm = checksumToValue.getKey();
        SignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> {
            },
            signRequest -> signRequest.putProperty(CHECKSUM_ALGORITHM, checksumAlgorithm)
        );

        SignedRequest signedRequest = signer.sign(request);
       assertThat(signedRequest.request().firstMatchingHeader("x-amz-checksum-" + checksumAlgorithm.algorithmId()
                                                                                                   .toLowerCase(Locale.US)))
           .contains(checksumToValue.getValue());
    }

    @Test
    public void sign_checksumValueProvided_shouldNotOverrideChecksumHeader() {
        SignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
                httpRequest -> httpRequest
                    .putHeader("x-amz-checksum-crc32", "some value"),
            signRequest -> signRequest.putProperty(CHECKSUM_ALGORITHM, CRC32)
        );

        SignedRequest signedRequest = signer.sign(request);
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-checksum-crc32"))
            .contains("some value");
    }
}
