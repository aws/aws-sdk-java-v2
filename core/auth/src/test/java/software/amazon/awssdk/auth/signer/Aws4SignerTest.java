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

package software.amazon.awssdk.auth.signer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.signer.internal.Aws4SignerUtils;
import software.amazon.awssdk.auth.signer.internal.SignerConstant;
import software.amazon.awssdk.auth.signer.params.SignerChecksumParams;
import software.amazon.awssdk.auth.signer.internal.SignerTestUtils;
import software.amazon.awssdk.core.checksums.Algorithm;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;

/**
 * Unit tests for the {@link Aws4Signer}.
 */
@RunWith(MockitoJUnitRunner.class)
public class Aws4SignerTest {

    private static final String AWS_4_HMAC_SHA_256_AUTHORIZATION = "AWS4-HMAC-SHA256 Credential=access/19810216/us-east-1/demo/aws4_request, ";
    private static final String SIGNER_HEADER_WITH_CHECKSUMS_IN_HEADER = "SignedHeaders=host;x-amz-archive-description;x-amz-date;x-amzn-header-crc, ";
    private static final String SIGNER_HEADER_WITH_CHECKSUMS_IN_TRAILER = "SignedHeaders=host;x-amz-archive-description;x-amz-date;x-amz-trailer, ";

    private Aws4Signer signer = Aws4Signer.create();

    @Mock
    private Clock signingOverrideClock;

    SdkHttpFullRequest.Builder request;

    AwsBasicCredentials credentials;

    @Before
    public void setupCase() {
        mockClock();
        credentials = AwsBasicCredentials.create("access", "secret");
        request = SdkHttpFullRequest.builder()
                .contentStreamProvider(() -> new ByteArrayInputStream("abc".getBytes()))
                .method(SdkHttpMethod.POST)
                .putHeader("Host", "demo.us-east-1.amazonaws.com")
                .putHeader("x-amz-archive-description", "test  test")
                .encodedPath("/")
                .uri(URI.create("http://demo.us-east-1.amazonaws.com"));
    }

    @Test
    public void testSigning() throws Exception {
        final String expectedAuthorizationHeaderWithoutSha256Header =
            AWS_4_HMAC_SHA_256_AUTHORIZATION +
            "SignedHeaders=host;x-amz-archive-description;x-amz-date, " +
            "Signature=77fe7c02927966018667f21d1dc3dfad9057e58401cbb9ed64f1b7868288e35a";

        final String expectedAuthorizationHeaderWithSha256Header =
            AWS_4_HMAC_SHA_256_AUTHORIZATION +
            "SignedHeaders=host;x-amz-archive-description;x-amz-date;x-amz-sha256, " +
            "Signature=e73e20539446307a5dc71252dbd5b97e861f1d1267456abda3ebd8d57e519951";


        AwsBasicCredentials credentials = AwsBasicCredentials.create("access", "secret");
        // Test request without 'x-amz-sha256' header
        SdkHttpFullRequest.Builder request = generateBasicRequest();

        SdkHttpFullRequest signed = SignerTestUtils.signRequest(signer, request.build(), credentials,
                                                                "demo", signingOverrideClock, "us-east-1");
        assertThat(signed.firstMatchingHeader("Authorization"))
                .hasValue(expectedAuthorizationHeaderWithoutSha256Header);


        // Test request with 'x-amz-sha256' header
        request = generateBasicRequest();
        request.putHeader("x-amz-sha256", "required");

        signed = SignerTestUtils.signRequest(signer, request.build(), credentials, "demo", signingOverrideClock, "us-east-1");
        assertThat(signed.firstMatchingHeader("Authorization")).hasValue(expectedAuthorizationHeaderWithSha256Header);
    }

    @Test
    public void queryParamsWithNullValuesAreStillSignedWithTrailingEquals() throws Exception {
        final String expectedAuthorizationHeaderWithoutSha256Header =
            AWS_4_HMAC_SHA_256_AUTHORIZATION  +
            "SignedHeaders=host;x-amz-archive-description;x-amz-date, " +
            "Signature=c45a3ff1f028e83017f3812c06b4440f0b3240264258f6e18cd683b816990ba4";

        AwsBasicCredentials credentials = AwsBasicCredentials.create("access", "secret");
        // Test request without 'x-amz-sha256' header
        SdkHttpFullRequest.Builder request = generateBasicRequest().putRawQueryParameter("Foo", (String) null);

        SdkHttpFullRequest signed = SignerTestUtils.signRequest(signer, request.build(), credentials,
                                                                "demo", signingOverrideClock, "us-east-1");
        assertThat(signed.firstMatchingHeader("Authorization")).hasValue(expectedAuthorizationHeaderWithoutSha256Header);
    }

    @Test
    public void testPresigning() throws Exception {
        final String expectedAmzSignature = "bf7ae1c2f266d347e290a2aee7b126d38b8a695149d003b9fab2ed1eb6d6ebda";
        final String expectedAmzCredentials = "access/19810216/us-east-1/demo/aws4_request";
        final String expectedAmzHeader = "19810216T063000Z";
        final String expectedAmzExpires = "604800";

        AwsBasicCredentials credentials = AwsBasicCredentials.create("access", "secret");
        // Test request without 'x-amz-sha256' header

        SdkHttpFullRequest request = generateBasicRequest().build();

        SdkHttpFullRequest signed = SignerTestUtils.presignRequest(signer, request, credentials, null, "demo",
                                                                   signingOverrideClock, "us-east-1");
        assertEquals(expectedAmzSignature, signed.rawQueryParameters().get("X-Amz-Signature").get(0));
        assertEquals(expectedAmzCredentials, signed.rawQueryParameters().get("X-Amz-Credential").get(0));
        assertEquals(expectedAmzHeader, signed.rawQueryParameters().get("X-Amz-Date").get(0));
        assertEquals(expectedAmzExpires, signed.rawQueryParameters().get("X-Amz-Expires").get(0));
    }

    /**
     * Tests that if passed anonymous credentials, signer will not generate a signature.
     */
    @Test
    public void testAnonymous() throws Exception {
        AwsCredentials credentials = AnonymousCredentialsProvider.create().resolveCredentials();
        SdkHttpFullRequest request = generateBasicRequest().build();

        SignerTestUtils.signRequest(signer, request, credentials, "demo", signingOverrideClock, "us-east-1");

        assertNull(request.headers().get("Authorization"));
    }

    /**
     * x-amzn-trace-id should not be signed as it may be mutated by proxies or load balancers.
     */
    @Test
    public void xAmznTraceId_NotSigned() throws Exception {
        AwsBasicCredentials credentials = AwsBasicCredentials.create("akid", "skid");
        SdkHttpFullRequest.Builder request = generateBasicRequest();
        request.putHeader("X-Amzn-Trace-Id", " Root=1-584b150a-708479cb060007ffbf3ee1da;Parent=36d3dbcfd150aac9;Sampled=1");

        SdkHttpFullRequest actual = SignerTestUtils.signRequest(signer, request.build(), credentials, "demo", signingOverrideClock, "us-east-1");

        assertThat(actual.firstMatchingHeader("Authorization"))
                .hasValue("AWS4-HMAC-SHA256 Credential=akid/19810216/us-east-1/demo/aws4_request, " +
                          "SignedHeaders=host;x-amz-archive-description;x-amz-date, " +
                          "Signature=581d0042389009a28d461124138f1fe8eeb8daed87611d2a2b47fd3d68d81d73");
    }

    /**
     * Multi-value headers should be comma separated.
     */
    @Test
    public void canonicalizedHeaderString_multiValueHeaders_areCommaSeparated() throws Exception {
        AwsBasicCredentials credentials = AwsBasicCredentials.create("akid", "skid");
        SdkHttpFullRequest.Builder request = generateBasicRequest();
        request.appendHeader("foo","bar");
        request.appendHeader("foo","baz");

        SdkHttpFullRequest actual = SignerTestUtils.signRequest(signer, request.build(), credentials, "demo", signingOverrideClock, "us-east-1");

        // We cannot easily test the canonical header string value, but the below signature asserts that it contains:
        // foo:bar,baz
        assertThat(actual.firstMatchingHeader("Authorization"))
            .hasValue("AWS4-HMAC-SHA256 Credential=akid/19810216/us-east-1/demo/aws4_request, " 
                      + "SignedHeaders=foo;host;x-amz-archive-description;x-amz-date, " 
                      + "Signature=1253bc1751048ea299e688cbe07a2224292e5cc606a079cb40459ad987793c19");
    }

    /**
     * Canonical headers should remove excess white space before and after values, and convert sequential spaces to a single 
     * space.
     */
    @Test
    public void canonicalizedHeaderString_valuesWithExtraWhitespace_areTrimmed() throws Exception {
        AwsBasicCredentials credentials = AwsBasicCredentials.create("akid", "skid");
        SdkHttpFullRequest.Builder request = generateBasicRequest();
        request.putHeader("My-header1","    a   b   c  ");
        request.putHeader("My-Header2","    \"a   b   c\"  ");

        SdkHttpFullRequest actual = SignerTestUtils.signRequest(signer, request.build(), credentials, "demo", signingOverrideClock, "us-east-1");

        // We cannot easily test the canonical header string value, but the below signature asserts that it contains:
        // my-header1:a b c
        // my-header2:"a b c"
        assertThat(actual.firstMatchingHeader("Authorization"))
            .hasValue("AWS4-HMAC-SHA256 Credential=akid/19810216/us-east-1/demo/aws4_request, " 
                      + "SignedHeaders=host;my-header1;my-header2;x-amz-archive-description;x-amz-date, " 
                      + "Signature=6d3520e3397e7aba593d8ebd8361fc4405e90aed71bc4c7a09dcacb6f72460b9");
    }

    /**
     * Query strings with empty keys should not be included in the canonical string.
     */
    @Test
    public void canonicalizedQueryString_keyWithEmptyNames_doNotGetSigned() throws Exception {
        AwsBasicCredentials credentials = AwsBasicCredentials.create("akid", "skid");
        SdkHttpFullRequest.Builder request = generateBasicRequest();
        request.putRawQueryParameter("", (String) null);

        SdkHttpFullRequest actual = SignerTestUtils.signRequest(signer, request.build(), credentials, "demo", signingOverrideClock, "us-east-1");

        assertThat(actual.firstMatchingHeader("Authorization"))
            .hasValue("AWS4-HMAC-SHA256 Credential=akid/19810216/us-east-1/demo/aws4_request, "
                      + "SignedHeaders=host;x-amz-archive-description;x-amz-date, "
                      + "Signature=581d0042389009a28d461124138f1fe8eeb8daed87611d2a2b47fd3d68d81d73");
    }

    private SdkHttpFullRequest.Builder generateBasicRequest() {
        return SdkHttpFullRequest.builder()
                                 .contentStreamProvider(() -> new ByteArrayInputStream("{\"TableName\": \"foo\"}".getBytes()))
                                 .method(SdkHttpMethod.POST)
                                 .putHeader("Host", "demo.us-east-1.amazonaws.com")
                                 .putHeader("x-amz-archive-description", "test  test")
                                 .encodedPath("/")
                                 .uri(URI.create("http://demo.us-east-1.amazonaws.com"));
    }

    private void mockClock() {
        Calendar c = new GregorianCalendar();
        c.set(1981, 1, 16, 6, 30, 0);
        c.setTimeZone(TimeZone.getTimeZone("UTC"));

        when(signingOverrideClock.millis()).thenReturn(c.getTimeInMillis());
    }

    private String getOldTimeStamp(Date date) {
        final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        dateTimeFormat.setTimeZone(new SimpleTimeZone(0, "UTC"));
        return dateTimeFormat.format(date);
    }

    @Test
    public void getTimeStamp() {
        Date now = new Date();
        String timeStamp = Aws4SignerUtils.formatTimestamp(now.getTime());
        String old = getOldTimeStamp(now);
        assertEquals(old, timeStamp);
    }

    private String getOldDateStamp(Date date) {
        final SimpleDateFormat dateStampFormat = new SimpleDateFormat("yyyyMMdd");
        dateStampFormat.setTimeZone(new SimpleTimeZone(0, "UTC"));
        return dateStampFormat.format(date);
    }

    @Test
    public void getDateStamp() {
        Date now = new Date();
        String dateStamp = Aws4SignerUtils.formatDateStamp(now.getTime());
        String old = getOldDateStamp(now);
        assertEquals(old, dateStamp);
    }
    @Test
    public void signing_with_Crc32Checksum_WithOut_x_amz_sha25_header() throws Exception {
        //Note here x_amz_sha25_header is not present in SignedHeaders
        String expectedAuthorization = AWS_4_HMAC_SHA_256_AUTHORIZATION + SIGNER_HEADER_WITH_CHECKSUMS_IN_HEADER
                                       + "Signature=c1804802dc623d1689e7d0a7f9f5caee3588cc8d3df4495425129dbd52965d1f";

        final SignerChecksumParams signerChecksumParams = SignerChecksumParams.builder()
                                                                              .algorithm(Algorithm.CRC32)
                                                                              .checksumHeaderName("x-amzn-header-crc")
                                                                              .isStreamingRequest(false)
                .build();
        SdkHttpFullRequest signed = SignerTestUtils.signRequest(signer, request.contentStreamProvider(
                () -> new ByteArrayInputStream("{\"TableName\": \"foo\"}".getBytes(StandardCharsets.UTF_8))
                ).build(), credentials,
                "demo", signingOverrideClock, "us-east-1", signerChecksumParams);
        assertThat(signed.firstMatchingHeader("x-amzn-header-crc").get()).contains("oL+a/g==");
        assertThat(signed.firstMatchingHeader(SignerConstant.X_AMZ_CONTENT_SHA256)).isNotPresent();
        assertThat(signed.firstMatchingHeader("Authorization")).hasValue(expectedAuthorization);
    }

    @Test
    public void signing_with_Crc32Checksum_with_streaming_input_request() throws Exception {
        //Note here x_amz_sha25_header is not present in SignedHeaders
        String expectedAuthorization = AWS_4_HMAC_SHA_256_AUTHORIZATION + SIGNER_HEADER_WITH_CHECKSUMS_IN_HEADER
                                       + "Signature=c1804802dc623d1689e7d0a7f9f5caee3588cc8d3df4495425129dbd52965d1f";
        final SignerChecksumParams signerChecksumParams = SignerChecksumParams.builder()
                                                                              .algorithm(Algorithm.CRC32)
                                                                              .checksumHeaderName("x-amzn-header-crc")
                                                                              .isStreamingRequest(true)
                                                                              .build();
        SdkHttpFullRequest signed = SignerTestUtils.signRequest(signer, request.contentStreamProvider(
                                                                    () -> new ByteArrayInputStream("{\"TableName\": \"foo\"}".getBytes(StandardCharsets.UTF_8))
                                                                ).build(), credentials,
                                                                "demo", signingOverrideClock, "us-east-1", signerChecksumParams);
        assertThat(signed.firstMatchingHeader("x-amzn-header-crc").get()).contains("oL+a/g==");
        assertThat(signed.firstMatchingHeader(SignerConstant.X_AMZ_CONTENT_SHA256)).isNotPresent();
        assertThat(signed.firstMatchingHeader("Authorization")).hasValue(expectedAuthorization);
    }


    @Test
    public void signing_with_Crc32Checksum_with_x_amz_sha25_header_preset() throws Exception {
        //Note here x_amz_sha25_header is  present in SignedHeaders, we make sure checksum is calculated even in this case.
        String expectedAuthorization = AWS_4_HMAC_SHA_256_AUTHORIZATION
            + "SignedHeaders=host;x-amz-archive-description;x-amz-content-sha256;x-amz-date;x-amzn-header-crc, "
            + "Signature=bc931232666f226854cdd9c9962dc03d791cf4024f5ca032fab996c1d15e4a5d";
        final SignerChecksumParams signerChecksumParams = SignerChecksumParams.builder()
                                                                              .algorithm(Algorithm.CRC32)
                                                                              .checksumHeaderName("x-amzn-header-crc")
                                                                              .isStreamingRequest(true).build();
        request = generateBasicRequest();
        // presetting of the header
        request.putHeader(SignerConstant.X_AMZ_CONTENT_SHA256, "required");
        SdkHttpFullRequest signed = SignerTestUtils.signRequest(signer, request.build(), credentials,
                "demo", signingOverrideClock, "us-east-1", signerChecksumParams);
        assertThat(signed.firstMatchingHeader("Authorization")).hasValue(expectedAuthorization);
        assertThat(signed.firstMatchingHeader("x-amzn-header-crc").get()).contains("oL+a/g==");
        assertThat(signed.firstMatchingHeader(SignerConstant.X_AMZ_CONTENT_SHA256)).isPresent();
    }

    @Test
    public void signing_with_NoHttpChecksum_As_No_impact_on_Signature() throws Exception {
        //Note here x_amz_sha25_header is not present in SignedHeaders
        String expectedAuthorization =
            AWS_4_HMAC_SHA_256_AUTHORIZATION +
            "SignedHeaders=host;x-amz-archive-description;x-amz-date, " +
            "Signature=77fe7c02927966018667f21d1dc3dfad9057e58401cbb9ed64f1b7868288e35a";
        SdkHttpFullRequest signed = SignerTestUtils.signRequest(signer, request.contentStreamProvider(
                () -> new ByteArrayInputStream("{\"TableName\": \"foo\"}".getBytes(StandardCharsets.UTF_8))
                ).build(), credentials,
                "demo", signingOverrideClock, "us-east-1", null);
        assertThat(signed.firstMatchingHeader("Authorization")).hasValue(expectedAuthorization);
        assertThat(signed.firstMatchingHeader("x-amzn-header-crc")).isNotPresent();
    }

    @Test
    public void signing_with_Crc32Checksum_with_header_already_present() throws Exception {

        String expectedAuthorization = AWS_4_HMAC_SHA_256_AUTHORIZATION + SIGNER_HEADER_WITH_CHECKSUMS_IN_HEADER
                                       + "Signature=f6fad563460f2ac50fe2ab5f5f5d77a787e357897ac6e9bb116ff12d30f45589";

        final SignerChecksumParams signerChecksumParams = SignerChecksumParams.builder()
                                                                              .algorithm(Algorithm.CRC32)
                                                                              .checksumHeaderName("x-amzn-header-crc")
                                                                              .isStreamingRequest(false)
                                                                              .build();
        SdkHttpFullRequest signed = SignerTestUtils.signRequest(signer, request.contentStreamProvider(
                                                                    () -> new ByteArrayInputStream("{\"TableName\": \"foo\"}".getBytes(StandardCharsets.UTF_8))
                                                                )
                                                       .appendHeader("x-amzn-header-crc", "preCalculatedChecksum")
                                                                               .build(), credentials,
                                                                "demo", signingOverrideClock, "us-east-1", signerChecksumParams);
        assertThat(signed.firstMatchingHeader("x-amzn-header-crc")).hasValue("preCalculatedChecksum");
        assertThat(signed.firstMatchingHeader(SignerConstant.X_AMZ_CONTENT_SHA256)).isNotPresent();
        assertThat(signed.firstMatchingHeader("Authorization")).hasValue(expectedAuthorization);
    }

    @Test
    public void signing_with_Crc32Checksum_with__trailer_header_already_present() throws Exception {
        String expectedAuthorization = AWS_4_HMAC_SHA_256_AUTHORIZATION + SIGNER_HEADER_WITH_CHECKSUMS_IN_TRAILER
                                       + "Signature=3436c4bc175d31e87a591802e64756cebf2d1c6c2054d26ca3dc91bdd3de303e";

        final SignerChecksumParams signerChecksumParams = SignerChecksumParams.builder()
                                                                              .algorithm(Algorithm.CRC32)
                                                                              .checksumHeaderName("x-amzn-header-crc")
                                                                              .isStreamingRequest(false)
                                                                              .build();
        SdkHttpFullRequest signed = SignerTestUtils.signRequest(
            signer, request.contentStreamProvider(() -> new ByteArrayInputStream(("{\"TableName"
                                                                                  + "\": "
                                                                                  + "\"foo\"}").getBytes(StandardCharsets.UTF_8)))
                           .appendHeader("x-amz-trailer", "x-amzn-header-crc")
                           .build(), credentials,
            "demo", signingOverrideClock, "us-east-1", signerChecksumParams);
        assertThat(signed.firstMatchingHeader("x-amzn-header-crc")).isNotPresent();
        assertThat(signed.firstMatchingHeader("x-amz-trailer")).contains("x-amzn-header-crc");
        assertThat(signed.firstMatchingHeader(SignerConstant.X_AMZ_CONTENT_SHA256)).isNotPresent();
        assertThat(signed.firstMatchingHeader("Authorization")).hasValue(expectedAuthorization);
    }
}