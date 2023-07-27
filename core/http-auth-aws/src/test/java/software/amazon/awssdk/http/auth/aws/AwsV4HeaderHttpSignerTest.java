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

package software.amazon.awssdk.http.auth.aws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static software.amazon.awssdk.http.auth.aws.AwsV4HttpSigner.AUTH_LOCATION;
import static software.amazon.awssdk.http.auth.aws.AwsV4HttpSigner.REGION_NAME;
import static software.amazon.awssdk.http.auth.aws.AwsV4HttpSigner.SERVICE_SIGNING_NAME;
import static software.amazon.awssdk.http.auth.aws.TestUtils.generateBasicAsyncRequest;
import static software.amazon.awssdk.http.auth.aws.TestUtils.generateBasicRequest;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.auth.aws.TestUtils.AnonymousCredentialsIdentity;
import software.amazon.awssdk.http.auth.aws.internal.signer.AwsV4HeaderHttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.BaseAwsV4HttpSigner;
import software.amazon.awssdk.http.auth.aws.util.SignerConstant;
import software.amazon.awssdk.http.auth.spi.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;

/**
 * This test suite also tests much of the base V4 signer implementation, and validates behaviors such as header/query-param
 * processing, and so on.
 */
class AwsV4HeaderHttpSignerTest {

    private static final String AWS_4_HMAC_SHA_256_AUTHORIZATION =
        "AWS4-HMAC-SHA256 Credential=access/19810216/us-east-1/demo/aws4_request, ";
    private static final String AWS_4_HMAC_SHA_256_AKID_AUTHORIZATION = "AWS4-HMAC-SHA256 Credential=akid/19810216/us-east-1" +
        "/demo/aws4_request, ";

    private static final String SIGNER_HEADER_WITH_CHECKSUMS_IN_HEADER =
        "SignedHeaders=host;x-amz-archive-description;x-amz-date;x-amzn-header-crc, ";

    private static final String SIGNER_HEADER_WITH_CHECKSUMS_IN_TRAILER =
        "SignedHeaders=host;x-amz-archive-description;x-amz-date;x-amz-trailer, ";

    private static final BaseAwsV4HttpSigner<?> signer = new AwsV4HeaderHttpSigner(
        BaseAwsV4HttpSigner.create()
    );

    @Test
    public void sign_withoutSHA256Header_shouldSign() {
        final String expectedAuthorizationHeaderWithoutSha256Header =
            AWS_4_HMAC_SHA_256_AUTHORIZATION +
                "SignedHeaders=host;x-amz-archive-description;x-amz-date, " +
                "Signature=77fe7c02927966018667f21d1dc3dfad9057e58401cbb9ed64f1b7868288e35a";

        // Test request without 'x-amz-sha256' header
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> {
            },
            signRequest -> signRequest.putProperty(AUTH_LOCATION, "Header")
        );

        SyncSignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("Host")).hasValue(request.request().host());
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-archive-description")).hasValue(
            request.request().firstMatchingHeader("x-amz-archive-description").get());
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Date")).hasValue("19810216T063000Z");
        assertThat(signedRequest.request().firstMatchingHeader("Authorization"))
            .hasValue(expectedAuthorizationHeaderWithoutSha256Header);
    }

    @Test
    public void sign_withSHA256Header_shouldSignAndHaveHeader() {
        final String expectedAuthorizationHeaderWithSha256Header =
            AWS_4_HMAC_SHA_256_AUTHORIZATION +
                "SignedHeaders=host;x-amz-archive-description;x-amz-date;x-amz-sha256, " +
                "Signature=e73e20539446307a5dc71252dbd5b97e861f1d1267456abda3ebd8d57e519951";

        // Test request with 'x-amz-sha256' header
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> httpRequest.putHeader("x-amz-sha256", "required"),
            signRequest -> {
            }
        );

        SyncSignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("Host")).hasValue(request.request().host());
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-archive-description")).hasValue(
            request.request().firstMatchingHeader("x-amz-archive-description").get());
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Date")).hasValue("19810216T063000Z");
        assertThat(signedRequest.request().firstMatchingHeader("Authorization"))
            .hasValue(expectedAuthorizationHeaderWithSha256Header);
    }

    @Test
    public void sign_withNullQueryParam_shouldStillSignTrailingEquals() {
        final String expectedAuthorizationHeaderWithoutSha256Header =
            AWS_4_HMAC_SHA_256_AUTHORIZATION +
                "SignedHeaders=host;x-amz-archive-description;x-amz-date, " +
                "Signature=c45a3ff1f028e83017f3812c06b4440f0b3240264258f6e18cd683b816990ba4";

        // Test request without 'x-amz-sha256' header
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> httpRequest.putRawQueryParameter("Foo", (String) null),
            signRequest -> {
            }
        );

        SyncSignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("Host")).hasValue(request.request().host());
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-archive-description")).hasValue(
            request.request().firstMatchingHeader("x-amz-archive-description").get());
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Date")).hasValue("19810216T063000Z");
        assertThat(signedRequest.request().firstMatchingHeader("Authorization"))
            .hasValue(expectedAuthorizationHeaderWithoutSha256Header);
    }

    /**
     * Tests that if passed anonymous credentials, signer will not generate a signature.
     */
    @Test
    public void sign_withAnonymousCredentials_shouldNotSign() {
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            new AnonymousCredentialsIdentity(),
            httpRequest -> {
            },
            signRequest -> {
            }
        );

        SyncSignedRequest signedRequest = signer.sign(request);

        assertNull(signedRequest.request().headers().get("Authorization"));
    }

    @Test
    public void sign_withTraceHeader_shouldNotSignTraceHeader() {
        final String expectedAuthorizationHeader =
            AWS_4_HMAC_SHA_256_AKID_AUTHORIZATION +
                "SignedHeaders=host;x-amz-archive-description;x-amz-date, " +
                "Signature=581d0042389009a28d461124138f1fe8eeb8daed87611d2a2b47fd3d68d81d73";

        // Test request without 'x-amz-sha256' header
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("akid", "skid"),
            httpRequest -> httpRequest
                .putHeader("X-Amzn-Trace-Id", " Root=1-584b150a-708479cb060007ffbf3ee1da;Parent=36d3dbcfd150aac9;Sampled=1"),
            signRequest -> {
            }
        );

        SyncSignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("Host")).hasValue(request.request().host());
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-archive-description")).hasValue(
            request.request().firstMatchingHeader("x-amz-archive-description").get());
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Date")).hasValue("19810216T063000Z");
        assertThat(signedRequest.request().firstMatchingHeader("Authorization"))
            .hasValue(expectedAuthorizationHeader);
    }

    /**
     * Multi-value headers should be comma separated.
     */
    @Test
    public void sign_withMultiValueHeaders_shouldBeSignedAsCommaSeparated() {
        final String expectedAuthorizationHeader =
            AWS_4_HMAC_SHA_256_AKID_AUTHORIZATION +
                "SignedHeaders=foo;host;x-amz-archive-description;x-amz-date, " +
                "Signature=1253bc1751048ea299e688cbe07a2224292e5cc606a079cb40459ad987793c19";

        // Test request without 'x-amz-sha256' header
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("akid", "skid"),
            httpRequest -> httpRequest
                .appendHeader("foo", "bar")
                .appendHeader("foo", "baz"),
            signRequest -> {
            }
        );

        SyncSignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("Host")).hasValue(request.request().host());
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-archive-description")).hasValue(
            request.request().firstMatchingHeader("x-amz-archive-description").get());
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Date")).hasValue("19810216T063000Z");
        assertThat(signedRequest.request().firstMatchingHeader("Authorization"))
            .hasValue(expectedAuthorizationHeader);
    }

    /**
     * Canonical headers should remove excess white space before and after values, and convert sequential spaces to a single
     * space.
     */
    @Test
    public void sign_withHeaderStringWithExtraWhitespace_shouldBeSignedWithoutWhitespace() {
        final String expectedAuthorizationHeader =
            AWS_4_HMAC_SHA_256_AKID_AUTHORIZATION +
                "SignedHeaders=host;my-header1;my-header2;x-amz-archive-description;x-amz-date, " +
                "Signature=6d3520e3397e7aba593d8ebd8361fc4405e90aed71bc4c7a09dcacb6f72460b9";

        // Test request without 'x-amz-sha256' header
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("akid", "skid"),
            httpRequest -> httpRequest
                .putHeader("My-header1", "    a   b   c  ")
                .putHeader("My-Header2", "    \"a   b   c\"  "),
            signRequest -> {
            }
        );

        SyncSignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("Host")).hasValue(request.request().host());
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-archive-description")).hasValue(
            request.request().firstMatchingHeader("x-amz-archive-description").get());
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Date")).hasValue("19810216T063000Z");
        assertThat(signedRequest.request().firstMatchingHeader("Authorization"))
            .hasValue(expectedAuthorizationHeader);
    }

    /**
     * Query strings with empty keys should not be included in the canonical string.
     */
    @Test
    public void sign_withQueryStringKeysWithEmptyNames_shouldNotSignEmptyNameKeys() {
        final String expectedAuthorizationHeader =
            AWS_4_HMAC_SHA_256_AKID_AUTHORIZATION +
                "SignedHeaders=host;x-amz-archive-description;x-amz-date, " +
                "Signature=581d0042389009a28d461124138f1fe8eeb8daed87611d2a2b47fd3d68d81d73";

        // Test request without 'x-amz-sha256' header
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("akid", "skid"),
            httpRequest -> httpRequest.putRawQueryParameter("", (String) null),
            signRequest -> {
            }
        );

        SyncSignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("Host")).hasValue(request.request().host());
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-archive-description")).hasValue(
            request.request().firstMatchingHeader("x-amz-archive-description").get());
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Date")).hasValue("19810216T063000Z");
        assertThat(signedRequest.request().firstMatchingHeader("Authorization"))
            .hasValue(expectedAuthorizationHeader);
    }

    @Test
    public void sign_withCrc32Checksum_and_withoutSHA256Header_shouldSignWithCrc32Checksum() {
        // Note here x_amz_sha25_header is not present in SignedHeaders
        String expectedAuthorizationHeader = AWS_4_HMAC_SHA_256_AUTHORIZATION + SIGNER_HEADER_WITH_CHECKSUMS_IN_HEADER
            + "Signature=c1804802dc623d1689e7d0a7f9f5caee3588cc8d3df4495425129dbd52965d1f";

        // Test request without 'x-amz-sha256' header
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> httpRequest.putHeader("x-amzn-header-crc", "oL+a/g=="),
            signRequest -> {
            }
        );

        SyncSignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("Host")).hasValue(request.request().host());
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-archive-description")).hasValue(
            request.request().firstMatchingHeader("x-amz-archive-description").get());
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Date")).hasValue("19810216T063000Z");
        assertThat(signedRequest.request().firstMatchingHeader("Authorization"))
            .hasValue(expectedAuthorizationHeader);
        assertThat(signedRequest.request().firstMatchingHeader("x-amzn-header-crc").get()).contains("oL+a/g==");
        assertThat(signedRequest.request().firstMatchingHeader(SignerConstant.X_AMZ_CONTENT_SHA256)).isNotPresent();
    }

    @Test
    public void sign_withCrc32Checksum_and_withSHA256Header_shouldSignWithCrc32ChecksumAndHaveHeader() {
        //Note here x_amz_sha25_header is  present in SignedHeaders, we make sure checksum is calculated even in this case.
        String expectedAuthorizationHeader = AWS_4_HMAC_SHA_256_AUTHORIZATION
            + "SignedHeaders=host;x-amz-archive-description;x-amz-content-sha256;x-amz-date;x-amzn-header-crc, "
            + "Signature=bc931232666f226854cdd9c9962dc03d791cf4024f5ca032fab996c1d15e4a5d";

        // Test request without 'x-amz-sha256' header
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> httpRequest
                .putHeader(SignerConstant.X_AMZ_CONTENT_SHA256, "required")
                .putHeader("x-amzn-header-crc", "oL+a/g=="),
            signRequest -> {
            }
        );


        SyncSignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("Host")).hasValue(request.request().host());
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-archive-description")).hasValue(
            request.request().firstMatchingHeader("x-amz-archive-description").get());
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Date")).hasValue("19810216T063000Z");
        assertThat(signedRequest.request().firstMatchingHeader("Authorization"))
            .hasValue(expectedAuthorizationHeader);
        assertThat(signedRequest.request().firstMatchingHeader("x-amzn-header-crc").get()).contains("oL+a/g==");
        assertThat(signedRequest.request().firstMatchingHeader(SignerConstant.X_AMZ_CONTENT_SHA256)).isPresent();
    }

    @Test
    public void sign_withCrc32Checksum_and_withPrexistingHeader_shouldSignWithCrc32ChecksumAndNotOverwriteHeader() {
        String expectedAuthorizationHeader = AWS_4_HMAC_SHA_256_AUTHORIZATION + SIGNER_HEADER_WITH_CHECKSUMS_IN_HEADER
            + "Signature=f6fad563460f2ac50fe2ab5f5f5d77a787e357897ac6e9bb116ff12d30f45589";

        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> httpRequest.appendHeader("x-amzn-header-crc", "preCalculatedChecksum"),
            signRequest -> {}
        );

        SyncSignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("Host")).hasValue(request.request().host());
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-archive-description")).hasValue(
            request.request().firstMatchingHeader("x-amz-archive-description").get());
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Date")).hasValue("19810216T063000Z");
        assertThat(signedRequest.request().firstMatchingHeader("Authorization"))
            .hasValue(expectedAuthorizationHeader);
        assertThat(signedRequest.request().firstMatchingHeader("x-amzn-header-crc")).hasValue("preCalculatedChecksum");
        assertThat(signedRequest.request().firstMatchingHeader(SignerConstant.X_AMZ_CONTENT_SHA256)).isNotPresent();
    }

    @Test
    public void sign_withCrc32Checksum_and_withTrailinggHeader_shouldSignWithCrc32ChecksumAndTrailerHeaderContainsCrc32Header() {
        String expectedAuthorizationHeader = AWS_4_HMAC_SHA_256_AUTHORIZATION + SIGNER_HEADER_WITH_CHECKSUMS_IN_TRAILER
            + "Signature=3436c4bc175d31e87a591802e64756cebf2d1c6c2054d26ca3dc91bdd3de303e";

        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> httpRequest.appendHeader("x-amz-trailer", "x-amzn-header-crc"),
            signRequest -> {}
        );

        SyncSignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("Host")).hasValue(request.request().host());
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-archive-description")).hasValue(
            request.request().firstMatchingHeader("x-amz-archive-description").get());
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Date")).hasValue("19810216T063000Z");
        assertThat(signedRequest.request().firstMatchingHeader("x-amzn-header-crc")).isNotPresent();
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-trailer")).contains("x-amzn-header-crc");
        assertThat(signedRequest.request().firstMatchingHeader(SignerConstant.X_AMZ_CONTENT_SHA256)).isNotPresent();
        assertThat(signedRequest.request().firstMatchingHeader("Authorization")).hasValue(expectedAuthorizationHeader);
    }

    @Test
    public void sign_withoutRegionNameProperty_throws() {
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> {
            },
            signRequest -> signRequest
                .putProperty(REGION_NAME, null)
        );

        NullPointerException exception =
            assertThrows(NullPointerException.class, () -> signer.sign(request));

        assertThat(exception.getMessage()).contains("must not be null");
    }

    @Test
    public void sign_withoutServiceSigningNameProperty_throws() {
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> {
            },
            signRequest -> signRequest
                .putProperty(SERVICE_SIGNING_NAME, null)
        );

        NullPointerException exception =
            assertThrows(NullPointerException.class, () -> signer.sign(request));

        assertThat(exception.getMessage()).contains("must not be null");
    }

    @Test
    public void sign_withSessionCredentials_shouldSignAndAddTokenHeader() {
        final String expectedAuthorizationHeaderWithoutSha256Header =
            AWS_4_HMAC_SHA_256_AUTHORIZATION +
                "SignedHeaders=host;x-amz-archive-description;x-amz-date;x-amz-security-token, " +
                "Signature=f44a6c23e168c186a982f20322655a932659db5b75b0917f55eb4519a8e7169e";

        // Test request without 'x-amz-sha256' header
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsSessionCredentialsIdentity.create("access", "secret", "token"),
            httpRequest -> {
            },
            signRequest -> {
            }
        );

        SyncSignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("Host")).hasValue(request.request().host());
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-archive-description")).hasValue(
            request.request().firstMatchingHeader("x-amz-archive-description").get());
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Date")).hasValue("19810216T063000Z");
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Security-Token")).hasValue("token");
        assertThat(signedRequest.request().firstMatchingHeader("Authorization"))
            .hasValue(expectedAuthorizationHeaderWithoutSha256Header);
    }

    @Test
    public void signAsync_withoutSHA256Header_shouldSign() {
        final String expectedAuthorizationHeaderWithoutSha256Header =
            AWS_4_HMAC_SHA_256_AUTHORIZATION +
                "SignedHeaders=host;x-amz-archive-description;x-amz-date, " +
                "Signature=77fe7c02927966018667f21d1dc3dfad9057e58401cbb9ed64f1b7868288e35a";

        // Test request without 'x-amz-sha256' header
        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> {
            },
            signRequest -> {
            }
        );

        AsyncSignedRequest signedRequest = signer.signAsync(request);

        assertThat(signedRequest.request().firstMatchingHeader("Host")).hasValue(request.request().host());
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-archive-description")).hasValue(
            request.request().firstMatchingHeader("x-amz-archive-description").get());
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Date")).hasValue("19810216T063000Z");
        assertThat(signedRequest.request().firstMatchingHeader("Authorization"))
            .hasValue(expectedAuthorizationHeaderWithoutSha256Header);
    }

    @Test
    public void signAsync_withSHA256Header_shouldSignAndHaveHeader() {
        final String expectedAuthorizationHeaderWithSha256Header =
            AWS_4_HMAC_SHA_256_AUTHORIZATION +
                "SignedHeaders=host;x-amz-archive-description;x-amz-date;x-amz-sha256, " +
                "Signature=e73e20539446307a5dc71252dbd5b97e861f1d1267456abda3ebd8d57e519951";

        // Test request with 'x-amz-sha256' header
        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> httpRequest.putHeader("x-amz-sha256", "required"),
            signRequest -> {
            }
        );

        AsyncSignedRequest signedRequest = signer.signAsync(request);

        assertThat(signedRequest.request().firstMatchingHeader("Host")).hasValue(request.request().host());
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-archive-description")).hasValue(
            request.request().firstMatchingHeader("x-amz-archive-description").get());
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Date")).hasValue("19810216T063000Z");
        assertThat(signedRequest.request().firstMatchingHeader("Authorization"))
            .hasValue(expectedAuthorizationHeaderWithSha256Header);
    }

    @Test
    public void signAsync_withSessionCredentials_shouldSignAndAddTokenHeader() {
        final String expectedAuthorizationHeaderWithoutSha256Header =
            AWS_4_HMAC_SHA_256_AUTHORIZATION +
                "SignedHeaders=host;x-amz-archive-description;x-amz-date;x-amz-security-token, " +
                "Signature=f44a6c23e168c186a982f20322655a932659db5b75b0917f55eb4519a8e7169e";

        // Test request without 'x-amz-sha256' header
        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            AwsSessionCredentialsIdentity.create("access", "secret", "token"),
            httpRequest -> {
            },
            signRequest -> {
            }
        );

        AsyncSignedRequest signedRequest = signer.signAsync(request);

        assertThat(signedRequest.request().firstMatchingHeader("Host")).hasValue(request.request().host());
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-archive-description")).hasValue(
            request.request().firstMatchingHeader("x-amz-archive-description").get());
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Date")).hasValue("19810216T063000Z");
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Security-Token")).hasValue("token");
        assertThat(signedRequest.request().firstMatchingHeader("Authorization"))
            .hasValue(expectedAuthorizationHeaderWithoutSha256Header);
    }

    @Test
    public void signAsync_withoutRegionNameProperty_throws() {
        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> {
            },
            signRequest -> signRequest
                .putProperty(REGION_NAME, null)
        );

        NullPointerException exception =
            assertThrows(NullPointerException.class, () -> signer.signAsync(request));

        assertThat(exception.getMessage()).contains("must not be null");
    }

    @Test
    public void signAsync_withoutServiceSigningNameProperty_throws() {
        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> {
            },
            signRequest -> signRequest
                .putProperty(SERVICE_SIGNING_NAME, null)
        );

        NullPointerException exception =
            assertThrows(NullPointerException.class, () -> signer.signAsync(request));

        assertThat(exception.getMessage()).contains("must not be null");
    }

    // TODO: Move to a mega signer test
    @Test
    public void sign_withoutAuthLocation_shouldSignWithHeader() {
        final String expectedAuthorizationHeaderWithoutSha256Header =
            AWS_4_HMAC_SHA_256_AUTHORIZATION +
                "SignedHeaders=host;x-amz-archive-description;x-amz-date, " +
                "Signature=77fe7c02927966018667f21d1dc3dfad9057e58401cbb9ed64f1b7868288e35a";

        // Test request without 'x-amz-sha256' header
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> {
            },
            signRequest -> {
            }
        );

        SyncSignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("Authorization"))
            .hasValue(expectedAuthorizationHeaderWithoutSha256Header);
    }

    // TODO: Move to a mega signer test
    @Test
    public void signAsync_withoutAuthLocation_shouldSignWithHeader() {
        final String expectedAuthorizationHeaderWithoutSha256Header =
            AWS_4_HMAC_SHA_256_AUTHORIZATION +
                "SignedHeaders=host;x-amz-archive-description;x-amz-date, " +
                "Signature=77fe7c02927966018667f21d1dc3dfad9057e58401cbb9ed64f1b7868288e35a";

        // Test request without 'x-amz-sha256' header
        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> {
            },
            signRequest -> {
            }
        );

        AsyncSignedRequest signedRequest = signer.signAsync(request);

        assertThat(signedRequest.request().firstMatchingHeader("Authorization"))
            .hasValue(expectedAuthorizationHeaderWithoutSha256Header);
    }
}
