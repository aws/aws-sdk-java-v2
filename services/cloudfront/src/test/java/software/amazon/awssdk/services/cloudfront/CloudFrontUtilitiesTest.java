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

package software.amazon.awssdk.services.cloudfront;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.StringJoiner;
import java.util.stream.Stream;
import junit.framework.TestCase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.cloudfront.cookie.CookiesForCannedPolicy;
import software.amazon.awssdk.services.cloudfront.cookie.CookiesForCustomPolicy;
import software.amazon.awssdk.services.cloudfront.internal.utils.SigningUtils;
import software.amazon.awssdk.services.cloudfront.model.CannedSignerRequest;
import software.amazon.awssdk.services.cloudfront.model.CustomSignerRequest;
import software.amazon.awssdk.services.cloudfront.url.SignedUrl;


class CloudFrontUtilitiesTest {
    private static final String RESOURCE_URL = "https://d1npcfkc2mojrf.cloudfront.net/s3ObjectKey";
    private static final String RESOURCE_URL_WITH_PORT = "https://d1npcfkc2mojrf.cloudfront.net:65535/s3ObjectKey";
    private static CloudFrontUtilities cloudFrontUtilities;

    @TempDir
    static Path tempDir;

    private static class KeyTestCase {
        final String name;
        final KeyPair keyPair;
        final Path keyFilePath;

        KeyTestCase(String name, KeyPair keyPair, Path keyFilePath) {
            this.name = name;
            this.keyPair = keyPair;
            this.keyFilePath = keyFilePath;
        }

        @Override
        public String toString() {
            return name;
        }

        static KeyTestCase createRsaTestCase() {
            try {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                kpg.initialize(2048);
                KeyPair keyPair = kpg.generateKeyPair();
                return new KeyTestCase("RSA", keyPair, writeKeyToFile("rsa", keyPair));
            } catch(NoSuchAlgorithmException | IOException e) {
                throw new RuntimeException(e);
            }
        }

        static KeyTestCase createECDSATestCase() {
            try {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
                kpg.initialize(new ECGenParameterSpec("secp256r1"));
                KeyPair keyPair = kpg.generateKeyPair();
                return new KeyTestCase("ECDSA", keyPair, writeKeyToFile("ec", keyPair));
            } catch(NoSuchAlgorithmException | IOException | InvalidAlgorithmParameterException e) {
                throw new RuntimeException(e);
            }
        }

        private static Path writeKeyToFile(String name, KeyPair keyPair) throws IOException {
            Path keyFilePath = tempDir.resolve(name + "_key.pem");
            try (Writer writer = Files.newBufferedWriter(keyFilePath)) {
                writer.write("-----BEGIN PRIVATE KEY-----\n");
                writer.write(Base64.getEncoder()
                                   .encodeToString(keyPair.getPrivate().getEncoded()));
                writer.write("\n-----END PRIVATE KEY-----\n");
            }
            return keyFilePath;
        }

    }

    static Stream<KeyTestCase> keyCases() throws Exception {
        return Stream.of(
            KeyTestCase.createRsaTestCase(),
            KeyTestCase.createECDSATestCase()
        );
    }

    @BeforeAll
    static void setUp() throws Exception {
        cloudFrontUtilities = CloudFrontUtilities.create();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("keyCases")
    void getSignedURLWithCannedPolicy_producesValidUrl(KeyTestCase testCase) {
        Instant expirationDate = LocalDate.of(2024, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        SignedUrl signedUrl =
            cloudFrontUtilities.getSignedUrlWithCannedPolicy(r -> r
                .resourceUrl(RESOURCE_URL)
                .privateKey(testCase.keyPair.getPrivate())
                .keyPairId("keyPairId")
                .expirationDate(expirationDate));
        String url = signedUrl.url();
        String signature = url.substring(url.indexOf("&Signature"), url.indexOf("&Key-Pair-Id"));
        String expected = "https://d1npcfkc2mojrf.cloudfront.net/s3ObjectKey?Expires=1704067200"
                          + signature
                          + "&Key-Pair-Id=keyPairId";
        assertThat(expected).isEqualTo(url);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("keyCases")
    void getSignedURLWithCannedPolicy_withQueryParams_producesValidUrl(KeyTestCase testCase) {
        Instant expirationDate = LocalDate.of(2024, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        String resourceUrlWithQueryParams = "https://d1npcfkc2mojrf.cloudfront.net/s3ObjectKey?a=b&c=d";
        SignedUrl signedUrl =
            cloudFrontUtilities.getSignedUrlWithCannedPolicy(r -> r
                .resourceUrl(resourceUrlWithQueryParams)
                .privateKey(testCase.keyPair.getPrivate())
                .keyPairId("keyPairId")
                .expirationDate(expirationDate));
        String url = signedUrl.url();

        String signature = url.substring(url.indexOf("&Signature"), url.indexOf("&Key-Pair-Id"));
        String expected = "https://d1npcfkc2mojrf.cloudfront.net/s3ObjectKey?a=b&c=d&Expires=1704067200"
                          + signature
                          + "&Key-Pair-Id=keyPairId";
        assertThat(expected).isEqualTo(url);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("keyCases")
    void getSignedURLWithCustomPolicy_producesValidUrl(KeyTestCase testCase) throws Exception {
        Instant activeDate = LocalDate.of(2022, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        Instant expirationDate = LocalDate.of(2024, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        String ipRange = "1.2.3.4";
        SignedUrl signedUrl = cloudFrontUtilities.getSignedUrlWithCustomPolicy(r -> {
            try {
                r.resourceUrl(RESOURCE_URL)
                 .privateKey(testCase.keyFilePath)
                 .keyPairId("keyPairId")
                 .expirationDate(expirationDate)
                 .activeDate(activeDate)
                 .ipRange(ipRange);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        String url = signedUrl.url();
        String policy = url.substring(url.indexOf("Policy=") + 7, url.indexOf("&Signature"));
        String signature = url.substring(url.indexOf("&Signature"), url.indexOf("&Key-Pair-Id"));
        String expected = "https://d1npcfkc2mojrf.cloudfront.net/s3ObjectKey?Policy="
                          + policy
                          + signature
                          + "&Key-Pair-Id=keyPairId";
        assertThat(expected).isEqualTo(url);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("keyCases")
    void getSignedURLWithCustomPolicy_withQueryParams_producesValidUrl(KeyTestCase testCase) throws Exception {
        Instant activeDate = LocalDate.of(2022, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        Instant expirationDate = LocalDate.of(2024, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        String ipRange = "1.2.3.4";
        String resourceUrlWithQueryParams = "https://d1npcfkc2mojrf.cloudfront.net/s3ObjectKey?a=b&c=d";
        SignedUrl signedUrl =
            cloudFrontUtilities.getSignedUrlWithCustomPolicy(r -> r
                .resourceUrl(resourceUrlWithQueryParams)
                .privateKey(testCase.keyPair.getPrivate())
                .keyPairId("keyPairId")
                .expirationDate(expirationDate)
                .activeDate(activeDate)
                .ipRange(ipRange));
        String url = signedUrl.url();
        String policy = url.substring(url.indexOf("Policy=") + 7, url.indexOf("&Signature"));
        String signature = url.substring(url.indexOf("&Signature"), url.indexOf("&Key-Pair-Id"));
        String expected = "https://d1npcfkc2mojrf.cloudfront.net/s3ObjectKey?a=b&c=d&Policy="
                          + policy
                          + signature
                          + "&Key-Pair-Id=keyPairId";
        assertThat(expected).isEqualTo(url);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("keyCases")
    void getSignedURLWithCustomPolicy_withIpRangeOmitted_producesValidUrl(KeyTestCase testCase) throws Exception {
        Instant activeDate = LocalDate.of(2022, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        Instant expirationDate = LocalDate.of(2024, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        CustomSignerRequest request = CustomSignerRequest.builder()
                                                         .resourceUrl(RESOURCE_URL)
                                                         .privateKey(testCase.keyFilePath)
                                                         .keyPairId("keyPairId")
                                                         .expirationDate(expirationDate)
                                                         .activeDate(activeDate)
                                                         .build();
        SignedUrl signedUrl = cloudFrontUtilities.getSignedUrlWithCustomPolicy(request);
        String url = signedUrl.url();
        String policy = url.substring(url.indexOf("Policy=") + 7, url.indexOf("&Signature"));
        String signature = url.substring(url.indexOf("&Signature"), url.indexOf("&Key-Pair-Id"));
        String expected = "https://d1npcfkc2mojrf.cloudfront.net/s3ObjectKey?Policy="
                          + policy
                          + signature
                          + "&Key-Pair-Id=keyPairId";
        assertThat(expected).isEqualTo(url);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("keyCases")
    void getSignedURLWithCustomPolicy_withActiveDateOmitted_producesValidUrl(KeyTestCase testCase) throws Exception {
        Instant expirationDate = LocalDate.of(2024, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        String ipRange = "1.2.3.4";
        CustomSignerRequest request = CustomSignerRequest.builder()
                                                         .resourceUrl(RESOURCE_URL)
                                                         .privateKey(testCase.keyFilePath)
                                                         .keyPairId("keyPairId")
                                                         .expirationDate(expirationDate)
                                                         .ipRange(ipRange)
                                                         .build();
        SignedUrl signedUrl = cloudFrontUtilities.getSignedUrlWithCustomPolicy(request);
        String url = signedUrl.url();
        String policy = url.substring(url.indexOf("Policy=") + 7, url.indexOf("&Signature"));
        String signature = url.substring(url.indexOf("&Signature"), url.indexOf("&Key-Pair-Id"));
        String expected = "https://d1npcfkc2mojrf.cloudfront.net/s3ObjectKey?Policy="
                          + policy
                          + signature
                          + "&Key-Pair-Id=keyPairId";
        assertThat(expected).isEqualTo(url);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("keyCases")
    void getSignedURLWithCustomPolicy_withMissingExpirationDate_shouldThrowException(KeyTestCase testCase) throws Exception {
        NullPointerException exception = assertThrows(NullPointerException.class, () ->
            cloudFrontUtilities.getSignedUrlWithCustomPolicy(r -> r
                .resourceUrl(RESOURCE_URL)
                .privateKey(testCase.keyPair.getPrivate())
                .keyPairId("keyPairId"))
        );
        assertThat(exception.getMessage().contains("Expiration date must be provided to sign CloudFront URLs"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("keyCases")
    void getSignedURLWithCannedPolicy_withEncodedUrl_doesNotDecodeUrl(KeyTestCase testCase) throws Exception {
        String encodedUrl = "https://distributionDomain/s3ObjectKey/%40blob?v=1n1dm%2F01n1dm0";
        Instant expirationDate = LocalDate.of(2024, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        SignedUrl signedUrl =
            cloudFrontUtilities.getSignedUrlWithCannedPolicy(r -> r
                .resourceUrl(encodedUrl)
                .privateKey(testCase.keyPair.getPrivate())
                .keyPairId("keyPairId")
                .expirationDate(expirationDate));
        String url = signedUrl.url();
        String signature = url.substring(url.indexOf("&Signature"), url.indexOf("&Key-Pair-Id"));
        String expected = "https://distributionDomain/s3ObjectKey/%40blob?v=1n1dm%2F01n1dm0&Expires=1704067200"
                          + signature
                          + "&Key-Pair-Id=keyPairId";
        assertThat(expected).isEqualTo(url);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("keyCases")
    void getSignedURLWithCustomPolicy_withEncodedUrl_doesNotDecodeUrl(KeyTestCase testCase) throws Exception {
        String encodedUrl = "https://distributionDomain/s3ObjectKey/%40blob?v=1n1dm%2F01n1dm0";
        Instant activeDate = LocalDate.of(2022, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        Instant expirationDate = LocalDate.of(2024, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        String ipRange = "1.2.3.4";
        SignedUrl signedUrl = cloudFrontUtilities.getSignedUrlWithCustomPolicy(r -> {
            try {
                r.resourceUrl(encodedUrl)
                 .privateKey(testCase.keyFilePath)
                 .keyPairId("keyPairId")
                 .expirationDate(expirationDate)
                 .activeDate(activeDate)
                 .ipRange(ipRange);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        String url = signedUrl.url();
        String policy = url.substring(url.indexOf("Policy=") + 7, url.indexOf("&Signature"));
        String signature = url.substring(url.indexOf("&Signature"), url.indexOf("&Key-Pair-Id"));
        String expected = "https://distributionDomain/s3ObjectKey/%40blob?v=1n1dm%2F01n1dm0&Policy="
                          + policy
                          + signature
                          + "&Key-Pair-Id=keyPairId";
        assertThat(expected).isEqualTo(url);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("keyCases")
    void getSignedURLWithCannedPolicy_withPortNumber_returnsPortNumber(KeyTestCase testCase) throws Exception {
        Instant expirationDate = LocalDate.of(2024, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        SignedUrl signedUrl =
            cloudFrontUtilities.getSignedUrlWithCannedPolicy(r -> r
                .resourceUrl(RESOURCE_URL_WITH_PORT)
                .privateKey(testCase.keyPair.getPrivate())
                .keyPairId("keyPairId")
                .expirationDate(expirationDate));
        assertThat(signedUrl.url()).contains("65535");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("keyCases")
    void getSignedURLWithCustomPolicy_withPortNumber_returnsPortNumber(KeyTestCase testCase) throws Exception {
        Instant expirationDate = LocalDate.of(2024, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        SignedUrl signedUrl =
            cloudFrontUtilities.getSignedUrlWithCustomPolicy(r -> r
                .resourceUrl(RESOURCE_URL_WITH_PORT)
                .privateKey(testCase.keyPair.getPrivate())
                .keyPairId("keyPairId")
                .expirationDate(expirationDate));
        assertThat(signedUrl.url()).contains("65535");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("keyCases")
    void getCookiesForCannedPolicy_producesValidCookies(KeyTestCase testCase) throws Exception {
        Instant expirationDate = LocalDate.of(2024, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        CannedSignerRequest request = CannedSignerRequest.builder()
                                                         .resourceUrl(RESOURCE_URL)
                                                         .privateKey(testCase.keyFilePath)
                                                         .keyPairId("keyPairId")
                                                         .expirationDate(expirationDate)
                                                         .build();
        CookiesForCannedPolicy cookiesForCannedPolicy = cloudFrontUtilities.getCookiesForCannedPolicy(request);
        assertThat(cookiesForCannedPolicy.resourceUrl()).isEqualTo(RESOURCE_URL);
        assertThat(cookiesForCannedPolicy.keyPairIdHeaderValue()).isEqualTo("CloudFront-Key-Pair-Id=keyPairId");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("keyCases")
    void getCookiesForCustomPolicy_producesValidCookies(KeyTestCase testCase) throws Exception {
        Instant activeDate = LocalDate.of(2022, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        Instant expirationDate = LocalDate.of(2024, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        String ipRange = "1.2.3.4";
        CustomSignerRequest request = CustomSignerRequest.builder()
                                                         .resourceUrl(RESOURCE_URL)
                                                         .privateKey(testCase.keyFilePath)
                                                         .keyPairId("keyPairId")
                                                         .expirationDate(expirationDate)
                                                         .activeDate(activeDate)
                                                         .ipRange(ipRange)
                                                         .build();
        CookiesForCustomPolicy cookiesForCustomPolicy = cloudFrontUtilities.getCookiesForCustomPolicy(request);
        assertThat(cookiesForCustomPolicy.resourceUrl()).isEqualTo(RESOURCE_URL);
        assertThat(cookiesForCustomPolicy.keyPairIdHeaderValue()).isEqualTo("CloudFront-Key-Pair-Id=keyPairId");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("keyCases")
    void getCookiesForCustomPolicy_withActiveDateAndIpRangeOmitted_producesValidCookies(KeyTestCase testCase) throws Exception {
        Instant expirationDate = LocalDate.of(2024, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        CustomSignerRequest request = CustomSignerRequest.builder()
                                                         .resourceUrl(RESOURCE_URL)
                                                         .privateKey(testCase.keyPair.getPrivate())
                                                         .keyPairId("keyPairId")
                                                         .expirationDate(expirationDate)
                                                         .build();
        CookiesForCustomPolicy cookiesForCustomPolicy = cloudFrontUtilities.getCookiesForCustomPolicy(request);
        assertThat(cookiesForCustomPolicy.resourceUrl()).isEqualTo(RESOURCE_URL);
        assertThat(cookiesForCustomPolicy.keyPairIdHeaderValue()).isEqualTo("CloudFront-Key-Pair-Id=keyPairId");
    }

    @ParameterizedTest
    @MethodSource("provideUrlPatternsAndExpectedResources")
    void getSignedURLWithCustomPolicy_policyResourceUrlShouldHandleVariousPatterns(
        String resourceUrlPattern, String expectedResource) {
        KeyTestCase testCase = KeyTestCase.createRsaTestCase();
        String baseUrl = "https://d1234.cloudfront.net/images/photo.jpg";
        Instant expiration = Instant.now().plusSeconds(3600);
        CustomSignerRequest request = CustomSignerRequest.builder()
                                                         .resourceUrl(baseUrl)
                                                         .privateKey(testCase.keyPair.getPrivate())
                                                         .keyPairId("keyPairId")
                                                         .resourceUrlPattern(resourceUrlPattern)
                                                         .expirationDate(expiration)
                                                         .build();

        SignedUrl signedUrl = cloudFrontUtilities.getSignedUrlWithCustomPolicy(request);

        // Extract and decode the policy
        String encodedPolicy = signedUrl.url().split("Policy=")[1].split("&")[0];
        String standardBase64 = encodedPolicy
            .replace('-', '+')
            .replace('_', '=')
            .replace('~', '/');
        String decodedPolicy = new String(Base64.getDecoder().decode(standardBase64), UTF_8);

        // Build expected policy
        StringBuilder expectedPolicy = new StringBuilder();
        StringJoiner conditions = new StringJoiner(",", "{", "}");
        conditions.add("\"DateLessThan\":{\"AWS:EpochTime\":" + expiration.getEpochSecond() + "}");

        expectedPolicy.append("{\"Statement\":[{")
                      .append("\"Resource\":\"").append(expectedResource).append("\",")
                      .append("\"Condition\":").append(conditions)
                      .append("}]}");

        assertThat(decodedPolicy.trim()).isEqualTo(expectedPolicy.toString().trim());
    }


    private static Stream<Arguments> provideUrlPatternsAndExpectedResources() {
        return Stream.of(
            Arguments.of("*", "*"),
            Arguments.of("https://d1234.cloudfront.net/*", "https://d1234.cloudfront.net/*"),
            Arguments.of("https://d1234.cloudfront.net/images/*", "https://d1234.cloudfront.net/images/*"),
            Arguments.of("https://d1234.cloudfront.net/*/photo.jpg", "https://d1234.cloudfront.net/*/photo.jpg"),
            Arguments.of("https://d1234.cloudfront.net/images/photo+with-plus.jpg",
                         "https://d1234.cloudfront.net/images/photo+with-plus.jpg"),
            Arguments.of("https://d1234.cloudfront.net/images/photo?param=value",
                         "https://d1234.cloudfront.net/images/photo?param=value"),
            Arguments.of("https://d1234.cloudfront.net/images/photo-ümlaut.jpg",
                         "https://d1234.cloudfront.net/images/photo-ümlaut.jpg"));
    }

    @Test
    void customSignerRequest_nullResourceUrlShouldThrow(){
        assertThatNullPointerException().isThrownBy(() -> CustomSignerRequest.builder().build())
                                        .withMessageContaining("resourceUrl must not be null");
    }
}
