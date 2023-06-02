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

package software.amazon.awssdk.http.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static software.amazon.awssdk.http.auth.internal.DefaultAwsV4HttpSigner.CHECKSUM_ALGORITHM;
import static software.amazon.awssdk.http.auth.internal.DefaultAwsV4HttpSigner.CHECKSUM_HEADER_NAME;
import static software.amazon.awssdk.http.auth.internal.DefaultAwsV4HttpSigner.DOUBLE_URL_ENCODE;
import static software.amazon.awssdk.http.auth.internal.DefaultAwsV4HttpSigner.NORMALIZE_PATH;
import static software.amazon.awssdk.http.auth.internal.DefaultAwsV4HttpSigner.REGION_NAME;
import static software.amazon.awssdk.http.auth.internal.DefaultAwsV4HttpSigner.REQUEST_SIGNING_DATE_TIME_MILLI;
import static software.amazon.awssdk.http.auth.internal.DefaultAwsV4HttpSigner.SERVICE_SIGNING_NAME;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.internal.util.SignerConstant;
import software.amazon.awssdk.http.auth.internal.checksums.Algorithm;
import software.amazon.awssdk.http.auth.spi.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignedHttpRequest;
import software.amazon.awssdk.http.auth.TestUtils.AnonymousCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.utils.async.SimplePublisher;

class AwsV4HttpSignerTest {

    private static final String AWS_4_HMAC_SHA_256_AUTHORIZATION = "AWS4-HMAC-SHA256 Credential=access/19810216/us-east-1/demo/aws4_request, ";
    private static final String AWS_4_HMAC_SHA_256_AKID_AUTHORIZATION = "AWS4-HMAC-SHA256 Credential=akid/19810216/us-east-1" +
        "/demo/aws4_request, ";

    private static final String SIGNER_HEADER_WITH_CHECKSUMS_IN_HEADER = "SignedHeaders=host;x-amz-archive-description;x-amz-date;x-amzn-header-crc, ";

    private static final String SIGNER_HEADER_WITH_CHECKSUMS_IN_TRAILER = "SignedHeaders=host;x-amz-archive-description;x-amz-date;x-amz-trailer, ";

    private static final AwsV4HttpSigner signer = AwsV4HttpSigner.create();

    @Test
    public void testSigning() {
        final String expectedAuthorizationHeaderWithoutSha256Header =
            AWS_4_HMAC_SHA_256_AUTHORIZATION +
                "SignedHeaders=host;x-amz-archive-description;x-amz-date, " +
                "Signature=77fe7c02927966018667f21d1dc3dfad9057e58401cbb9ed64f1b7868288e35a";

        // Test request without 'x-amz-sha256' header
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            SdkHttpRequest.builder(),
            SyncSignRequest.builder(AwsCredentialsIdentity.create("access", "secret"))
        );

        SyncSignedHttpRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("Authorization"))
            .hasValue(expectedAuthorizationHeaderWithoutSha256Header);
    }

    @Test
    public void testAsyncSigning() {
        final String expectedAuthorizationHeaderWithoutSha256Header =
            AWS_4_HMAC_SHA_256_AUTHORIZATION +
                "SignedHeaders=host;x-amz-archive-description;x-amz-date, " +
                "Signature=77fe7c02927966018667f21d1dc3dfad9057e58401cbb9ed64f1b7868288e35a";

        // Test request without 'x-amz-sha256' header
        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            SdkHttpRequest.builder(),
            AsyncSignRequest.builder(AwsCredentialsIdentity.create("access", "secret"))
        );

        AsyncSignedRequest signedRequest = signer.signAsync(request);

        assertThat(signedRequest.request().firstMatchingHeader("Authorization"))
            .hasValue(expectedAuthorizationHeaderWithoutSha256Header);
    }

    @Test
    public void testSigningWithHeader() {
        final String expectedAuthorizationHeaderWithSha256Header =
            AWS_4_HMAC_SHA_256_AUTHORIZATION +
                "SignedHeaders=host;x-amz-archive-description;x-amz-date;x-amz-sha256, " +
                "Signature=e73e20539446307a5dc71252dbd5b97e861f1d1267456abda3ebd8d57e519951";

        // Test request with 'x-amz-sha256' header
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            SdkHttpRequest.builder()
                .putHeader("x-amz-sha256", "required"),
            SyncSignRequest.builder(AwsCredentialsIdentity.create("access", "secret"))
        );

        SyncSignedHttpRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("Authorization"))
            .hasValue(expectedAuthorizationHeaderWithSha256Header);
    }

    @Test
    public void testAsyncSigningWithHeader() {
        final String expectedAuthorizationHeaderWithSha256Header =
            AWS_4_HMAC_SHA_256_AUTHORIZATION +
                "SignedHeaders=host;x-amz-archive-description;x-amz-date;x-amz-sha256, " +
                "Signature=e73e20539446307a5dc71252dbd5b97e861f1d1267456abda3ebd8d57e519951";

        // Test request with 'x-amz-sha256' header
        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            SdkHttpRequest.builder()
                .putHeader("x-amz-sha256", "required"),
            AsyncSignRequest.builder(AwsCredentialsIdentity.create("access", "secret"))
        );

        AsyncSignedRequest signedRequest = signer.signAsync(request);

        assertThat(signedRequest.request().firstMatchingHeader("Authorization"))
            .hasValue(expectedAuthorizationHeaderWithSha256Header);
    }

    @Test
    public void queryParamsWithNullValuesAreStillSignedWithTrailingEquals() {
        final String expectedAuthorizationHeaderWithoutSha256Header =
            AWS_4_HMAC_SHA_256_AUTHORIZATION  +
                "SignedHeaders=host;x-amz-archive-description;x-amz-date, " +
                "Signature=c45a3ff1f028e83017f3812c06b4440f0b3240264258f6e18cd683b816990ba4";

        // Test request without 'x-amz-sha256' header
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            SdkHttpRequest.builder()
                .putRawQueryParameter("Foo", (String) null),
            SyncSignRequest.builder(AwsCredentialsIdentity.create("access", "secret"))
        );

        SyncSignedHttpRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("Authorization"))
            .hasValue(expectedAuthorizationHeaderWithoutSha256Header);
    }

    /**
     * Tests that if passed anonymous credentials, signer will not generate a signature.
     */
    @Test
    public void testAnonymous() {
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            SdkHttpRequest.builder()
                .putRawQueryParameter("Foo", (String) null),
            SyncSignRequest.builder(new AnonymousCredentialsIdentity())
        );

        SyncSignedHttpRequest signedRequest = signer.sign(request);

        assertNull(signedRequest.request().headers().get("Authorization"));
    }

    @Test
    public void xAmznTraceId_NotSigned() {
        final String expectedAuthorizationHeader =
            AWS_4_HMAC_SHA_256_AKID_AUTHORIZATION  +
                "SignedHeaders=host;x-amz-archive-description;x-amz-date, " +
                "Signature=581d0042389009a28d461124138f1fe8eeb8daed87611d2a2b47fd3d68d81d73";

        // Test request without 'x-amz-sha256' header
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            SdkHttpRequest.builder()
                .putHeader("X-Amzn-Trace-Id", " Root=1-584b150a-708479cb060007ffbf3ee1da;Parent=36d3dbcfd150aac9;Sampled=1"),
            SyncSignRequest.builder(AwsCredentialsIdentity.create("akid", "skid"))
        );

        SyncSignedHttpRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("Authorization"))
            .hasValue(expectedAuthorizationHeader);
    }

    /**
     * Multi-value headers should be comma separated.
     */
    @Test
    public void canonicalizedHeaderString_multiValueHeaders_areCommaSeparated() {
        final String expectedAuthorizationHeader =
            AWS_4_HMAC_SHA_256_AKID_AUTHORIZATION  +
                "SignedHeaders=foo;host;x-amz-archive-description;x-amz-date, " +
                "Signature=1253bc1751048ea299e688cbe07a2224292e5cc606a079cb40459ad987793c19";

        // Test request without 'x-amz-sha256' header
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            SdkHttpRequest.builder()
                .appendHeader("foo","bar")
                .appendHeader("foo","baz"),
            SyncSignRequest.builder(AwsCredentialsIdentity.create("akid", "skid"))
        );

        SyncSignedHttpRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("Authorization"))
            .hasValue(expectedAuthorizationHeader);
    }

    /**
     * Canonical headers should remove excess white space before and after values, and convert sequential spaces to a single
     * space.
     */
    @Test
    public void canonicalizedHeaderString_valuesWithExtraWhitespace_areTrimmed() {
        final String expectedAuthorizationHeader =
            AWS_4_HMAC_SHA_256_AKID_AUTHORIZATION  +
                "SignedHeaders=host;my-header1;my-header2;x-amz-archive-description;x-amz-date, " +
                "Signature=6d3520e3397e7aba593d8ebd8361fc4405e90aed71bc4c7a09dcacb6f72460b9";

        // Test request without 'x-amz-sha256' header
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            SdkHttpRequest.builder()
                .putHeader("My-header1","    a   b   c  ")
                .putHeader("My-Header2","    \"a   b   c\"  "),
            SyncSignRequest.builder(AwsCredentialsIdentity.create("akid", "skid"))
        );

        SyncSignedHttpRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("Authorization"))
            .hasValue(expectedAuthorizationHeader);
    }

    /**
     * Query strings with empty keys should not be included in the canonical string.
     */
    @Test
    public void canonicalizedQueryString_keyWithEmptyNames_doNotGetSigned() {
        final String expectedAuthorizationHeader =
            AWS_4_HMAC_SHA_256_AKID_AUTHORIZATION  +
                "SignedHeaders=host;x-amz-archive-description;x-amz-date, " +
                "Signature=581d0042389009a28d461124138f1fe8eeb8daed87611d2a2b47fd3d68d81d73";

        // Test request without 'x-amz-sha256' header
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            SdkHttpRequest.builder()
                .putRawQueryParameter("", (String) null),
            SyncSignRequest.builder(AwsCredentialsIdentity.create("akid", "skid"))
        );

        SyncSignedHttpRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("Authorization"))
            .hasValue(expectedAuthorizationHeader);
    }

    @Test
    public void signing_with_Crc32Checksum_WithOut_x_amz_sha25_header() {
        // Note here x_amz_sha25_header is not present in SignedHeaders
        String expectedAuthorizationHeader = AWS_4_HMAC_SHA_256_AUTHORIZATION + SIGNER_HEADER_WITH_CHECKSUMS_IN_HEADER
            + "Signature=c1804802dc623d1689e7d0a7f9f5caee3588cc8d3df4495425129dbd52965d1f";

        // Test request without 'x-amz-sha256' header
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            SdkHttpRequest.builder(),
            SyncSignRequest.builder(AwsCredentialsIdentity.create("access", "secret"))
                .putProperty(CHECKSUM_HEADER_NAME, "x-amzn-header-crc")
                .putProperty(CHECKSUM_ALGORITHM, Algorithm.CRC32)
        );

        SyncSignedHttpRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("Authorization"))
            .hasValue(expectedAuthorizationHeader);
        assertThat(signedRequest.request().firstMatchingHeader("x-amzn-header-crc").get()).contains("oL+a/g==");
        assertThat(signedRequest.request().firstMatchingHeader(SignerConstant.X_AMZ_CONTENT_SHA256)).isNotPresent();
    }

    @Test
    public void signing_with_Crc32Checksum_with_x_amz_sha25_header_preset() {
        //Note here x_amz_sha25_header is  present in SignedHeaders, we make sure checksum is calculated even in this case.
        String expectedAuthorizationHeader = AWS_4_HMAC_SHA_256_AUTHORIZATION
            + "SignedHeaders=host;x-amz-archive-description;x-amz-content-sha256;x-amz-date;x-amzn-header-crc, "
            + "Signature=bc931232666f226854cdd9c9962dc03d791cf4024f5ca032fab996c1d15e4a5d";

        // Test request without 'x-amz-sha256' header
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            SdkHttpRequest.builder()
                .putHeader(SignerConstant.X_AMZ_CONTENT_SHA256, "required"),
            SyncSignRequest.builder(AwsCredentialsIdentity.create("access", "secret"))
                .putProperty(CHECKSUM_HEADER_NAME, "x-amzn-header-crc")
                .putProperty(CHECKSUM_ALGORITHM, Algorithm.CRC32)
        );

        SyncSignedHttpRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("Authorization"))
            .hasValue(expectedAuthorizationHeader);
        assertThat(signedRequest.request().firstMatchingHeader("x-amzn-header-crc").get()).contains("oL+a/g==");
        assertThat(signedRequest.request().firstMatchingHeader(SignerConstant.X_AMZ_CONTENT_SHA256)).isPresent();
    }

    @Test
    public void signing_with_Crc32Checksum_with_header_already_present() {
        String expectedAuthorizationHeader = AWS_4_HMAC_SHA_256_AUTHORIZATION + SIGNER_HEADER_WITH_CHECKSUMS_IN_HEADER
            + "Signature=f6fad563460f2ac50fe2ab5f5f5d77a787e357897ac6e9bb116ff12d30f45589";

        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            SdkHttpRequest.builder()
                .appendHeader("x-amzn-header-crc", "preCalculatedChecksum"),
            SyncSignRequest.builder(AwsCredentialsIdentity.create("access", "secret"))
                .putProperty(CHECKSUM_HEADER_NAME, "x-amzn-header-crc")
                .putProperty(CHECKSUM_ALGORITHM, Algorithm.CRC32)
        );

        SyncSignedHttpRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("Authorization"))
            .hasValue(expectedAuthorizationHeader);
        assertThat(signedRequest.request().firstMatchingHeader("x-amzn-header-crc")).hasValue("preCalculatedChecksum");
        assertThat(signedRequest.request().firstMatchingHeader(SignerConstant.X_AMZ_CONTENT_SHA256)).isNotPresent();
    }

    @Test
    public void signing_with_Crc32Checksum_with_trailer_header_already_present() {
        String expectedAuthorizationHeader = AWS_4_HMAC_SHA_256_AUTHORIZATION + SIGNER_HEADER_WITH_CHECKSUMS_IN_TRAILER
            + "Signature=3436c4bc175d31e87a591802e64756cebf2d1c6c2054d26ca3dc91bdd3de303e";

        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            SdkHttpRequest.builder()
                .appendHeader("x-amz-trailer", "x-amzn-header-crc"),
            SyncSignRequest.builder(AwsCredentialsIdentity.create("access", "secret"))
                .putProperty(CHECKSUM_HEADER_NAME, "x-amzn-header-crc")
                .putProperty(CHECKSUM_ALGORITHM, Algorithm.CRC32)
        );

        SyncSignedHttpRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("x-amzn-header-crc")).isNotPresent();
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-trailer")).contains("x-amzn-header-crc");
        assertThat(signedRequest.request().firstMatchingHeader(SignerConstant.X_AMZ_CONTENT_SHA256)).isNotPresent();
        assertThat(signedRequest.request().firstMatchingHeader("Authorization")).hasValue(expectedAuthorizationHeader);
    }

    // Helpers for tests
    private static SyncSignRequest<? extends AwsCredentialsIdentity> generateBasicRequest(
        SdkHttpRequest.Builder requestBuilder,
        SyncSignRequest.Builder<? extends AwsCredentialsIdentity> signRequestBuilder
    ) {
        return signRequestBuilder
            .request(requestBuilder
                .method(SdkHttpMethod.POST)
                .putHeader("Host", "demo.us-east-1.amazonaws.com")
                .putHeader("x-amz-archive-description", "test  test")
                .encodedPath("/")
                .uri(URI.create("http://demo.us-east-1.amazonaws.com"))
                .build())
            .payload(() -> new ByteArrayInputStream("{\"TableName\": \"foo\"}".getBytes()))
            .putProperty(REGION_NAME, "us-east-1")
            .putProperty(SERVICE_SIGNING_NAME, "demo")
            .putProperty(DOUBLE_URL_ENCODE, false)
            .putProperty(NORMALIZE_PATH, false)
            .putProperty(REQUEST_SIGNING_DATE_TIME_MILLI, 351153000968L)
            .build();
    }

    private static AsyncSignRequest<? extends AwsCredentialsIdentity> generateBasicAsyncRequest(
        SdkHttpRequest.Builder requestBuilder,
        AsyncSignRequest.Builder<? extends AwsCredentialsIdentity> signRequestBuilder
    ) {

        SimplePublisher<ByteBuffer> publisher = new SimplePublisher<>();

        publisher.send(ByteBuffer.wrap("{\"TableName\": \"foo\"}".getBytes()));
        publisher.complete();

        return signRequestBuilder
            .request(requestBuilder
                .method(SdkHttpMethod.POST)
                .putHeader("Host", "demo.us-east-1.amazonaws.com")
                .putHeader("x-amz-archive-description", "test  test")
                .encodedPath("/")
                .uri(URI.create("http://demo.us-east-1.amazonaws.com"))
                .build())
            .payload(publisher)
            .putProperty(REGION_NAME, "us-east-1")
            .putProperty(SERVICE_SIGNING_NAME, "demo")
            .putProperty(DOUBLE_URL_ENCODE, false)
            .putProperty(NORMALIZE_PATH, false)
            .putProperty(REQUEST_SIGNING_DATE_TIME_MILLI, 351153000968L)
            .build();
    }
}
