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

package software.amazon.awssdk.services.cloudfront.internal.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.utils.BinaryUtils;

class SigningUtilsTest {

    private static final Instant EXPIRATION = Instant.ofEpochSecond(1704067200);
    private static final Instant ACTIVE_DATE = Instant.ofEpochSecond(1640995200);
    private static final String VALID_URL = "https://d111111abcdef8.cloudfront.net/s3ObjectKey";
    private static final String VALID_IP = "192.168.1.0/24";

    @Test
    void buildCannedPolicy_withValidUrl_producesValidJson() {
        String policy = SigningUtils.buildCannedPolicy(VALID_URL, EXPIRATION);

        assertThat(policy).contains("\"Resource\":\"" + VALID_URL + "\"");
        assertThat(policy).contains("\"AWS:EpochTime\":" + EXPIRATION.getEpochSecond());
        // Verify it's valid JSON structure
        assertThat(policy).startsWith("{");
        assertThat(policy).endsWith("}");
    }

    @Test
    void buildCustomPolicy_withAllParameters_producesValidJson() {
        String policy = SigningUtils.buildCustomPolicy(VALID_URL, ACTIVE_DATE, EXPIRATION, VALID_IP);

        assertThat(policy).contains("\"Resource\":\"" + VALID_URL + "\"");
        assertThat(policy).contains("\"DateLessThan\"");
        assertThat(policy).contains("\"DateGreaterThan\"");
        assertThat(policy).contains("\"IpAddress\"");
        assertThat(policy).contains("\"AWS:SourceIp\":\"" + VALID_IP + "\"");
    }


    @Test
    void buildCannedPolicy_withDoubleQuoteInUrl_shouldRejectInput() {
        String maliciousUrl = "https://example.com/file\",\"Resource\":\"*\",\"x\":\"";

        assertThatThrownBy(() -> SigningUtils.buildCannedPolicy(maliciousUrl, EXPIRATION))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("contains invalid characters")
            .hasMessageContaining("resourceUrl");
    }

    @Test
    void buildCannedPolicy_withBackslashInUrl_shouldRejectInput() {
        String maliciousUrl = "https://example.com/file\\";

        assertThatThrownBy(() -> SigningUtils.buildCannedPolicy(maliciousUrl, EXPIRATION))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("contains invalid characters");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "https://example.com/file\u0000",  // null character
        "https://example.com/file\n",      // newline
        "https://example.com/file\r",      // carriage return
        "https://example.com/file\t"       // tab
    })
    void buildCannedPolicy_withControlCharactersInUrl_shouldRejectInput(String maliciousUrl) {
        assertThatThrownBy(() -> SigningUtils.buildCannedPolicy(maliciousUrl, EXPIRATION))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("contains invalid characters");
    }

    @Test
    void buildCustomPolicy_withDoubleQuoteInUrl_shouldRejectInput() {
        String maliciousUrl = "https://example.com/file\"";

        assertThatThrownBy(() -> SigningUtils.buildCustomPolicy(maliciousUrl, ACTIVE_DATE, EXPIRATION, VALID_IP))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("resourceUrl");
    }

    @Test
    void buildCustomPolicy_withDoubleQuoteInIpAddress_shouldRejectInput() {
        String maliciousIp = "192.168.1.0\",\"Resource\":\"*";

        assertThatThrownBy(() -> SigningUtils.buildCustomPolicy(VALID_URL, ACTIVE_DATE, EXPIRATION, maliciousIp))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ipAddress");
    }

    @Test
    void buildCustomPolicyForSignedUrl_withDoubleQuoteInUrl_shouldRejectInput() {
        String maliciousUrl = "https://example.com/file\"";

        assertThatThrownBy(() -> SigningUtils.buildCustomPolicyForSignedUrl(maliciousUrl, ACTIVE_DATE, EXPIRATION, VALID_IP))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("contains invalid characters");
    }

    @Test
    void buildCustomPolicyForSignedUrl_withDoubleQuoteInIpRange_shouldRejectInput() {
        String maliciousIp = "192.168.1.0\"";

        assertThatThrownBy(() -> SigningUtils.buildCustomPolicyForSignedUrl(VALID_URL, ACTIVE_DATE, EXPIRATION, maliciousIp))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("contains invalid characters");
    }

    // Null parameter validation tests

    @Test
    void buildCannedPolicy_withNullUrl_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> SigningUtils.buildCannedPolicy(null, EXPIRATION))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("resourceUrl");
    }

    @Test
    void buildCannedPolicy_withNullExpiration_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> SigningUtils.buildCannedPolicy(VALID_URL, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("expirationDate");
    }

    @Test
    void buildCustomPolicy_withNullUrl_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> SigningUtils.buildCustomPolicy(null, ACTIVE_DATE, EXPIRATION, VALID_IP))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("resourceUrl");
    }

    @Test
    void buildCustomPolicy_withNullExpiration_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> SigningUtils.buildCustomPolicy(VALID_URL, ACTIVE_DATE, null, VALID_IP))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("expirationDate");
    }

    // Valid edge cases that should still work

    @Test
    void buildCannedPolicy_withWildcard_shouldSucceed() {
        String policy = SigningUtils.buildCannedPolicy("*", EXPIRATION);
        assertThat(policy).contains("\"Resource\":\"*\"");
    }

    @Test
    void buildCannedPolicy_withWildcardInPath_shouldSucceed() {
        String url = "https://d111111abcdef8.cloudfront.net/*";
        String policy = SigningUtils.buildCannedPolicy(url, EXPIRATION);
        assertThat(policy).contains("\"Resource\":\"" + url + "\"");
    }

    @Test
    void buildCannedPolicy_withQueryParameters_shouldSucceed() {
        String url = "https://d111111abcdef8.cloudfront.net/file?param=value&other=123";
        String policy = SigningUtils.buildCannedPolicy(url, EXPIRATION);
        assertThat(policy).contains("\"Resource\":\"" + url + "\"");
    }

    @ParameterizedTest
    @ValueSource(strings = {"30847FFFFFFF", "308480000000"})
    void loadPrivateKey_pkcs1PemWithInvalidDerLength_throwsIllegalArgumentException(String derHex,
                                                                                    @TempDir Path tempDir)
        throws IOException {
        byte[] der = BinaryUtils.fromHex(derHex);
        String pem = "-----BEGIN RSA PRIVATE KEY-----\n"
                     + Base64.getEncoder().encodeToString(der)
                     + "\n-----END RSA PRIVATE KEY-----\n";
        Path keyFile = tempDir.resolve("evil.pem");
        Files.write(keyFile, pem.getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> SigningUtils.loadPrivateKey(keyFile))
            .isInstanceOf(IllegalArgumentException.class)
            .hasCauseInstanceOf(IOException.class);
    }
}
