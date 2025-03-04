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
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.StringJoiner;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.cloudfront.cookie.CookiesForCannedPolicy;
import software.amazon.awssdk.services.cloudfront.cookie.CookiesForCustomPolicy;
import software.amazon.awssdk.services.cloudfront.model.CannedSignerRequest;
import software.amazon.awssdk.services.cloudfront.model.CustomSignerRequest;
import software.amazon.awssdk.services.cloudfront.url.SignedUrl;


class CloudFrontUtilitiesTest {
    private static final String RESOURCE_URL = "https://d1npcfkc2mojrf.cloudfront.net/s3ObjectKey";
    private static final String RESOURCE_URL_WITH_PORT = "https://d1npcfkc2mojrf.cloudfront.net:65535/s3ObjectKey";
    private static KeyPairGenerator kpg;
    private static KeyPair keyPair;
    private static File keyFile;
    private static Path keyFilePath;
    private static CloudFrontUtilities cloudFrontUtilities;

    @BeforeAll
    static void setUp() throws Exception {
        initKeys();
        cloudFrontUtilities = CloudFrontUtilities.create();
    }

    @AfterAll
    static void tearDown() {
        keyFile.deleteOnExit();
    }

    static void initKeys() throws Exception {
        kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        keyPair = kpg.generateKeyPair();

        Base64.Encoder encoder = Base64.getEncoder();
        keyFile = new File("key.pem");
        FileWriter writer = new FileWriter(keyFile);
        writer.write("-----BEGIN PRIVATE KEY-----\n");
        writer.write(encoder.encodeToString(keyPair.getPrivate().getEncoded()));
        writer.write("\n-----END PRIVATE KEY-----\n");
        writer.close();
        keyFilePath = keyFile.toPath();
    }

    @Test
    void getSignedURLWithCannedPolicy_producesValidUrl() {
        Instant expirationDate = LocalDate.of(2024, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        SignedUrl signedUrl =
            cloudFrontUtilities.getSignedUrlWithCannedPolicy(r -> r
                .resourceUrl(RESOURCE_URL)
                .privateKey(keyPair.getPrivate())
                .keyPairId("keyPairId")
                .expirationDate(expirationDate));
        String url = signedUrl.url();
        String signature = url.substring(url.indexOf("&Signature"), url.indexOf("&Key-Pair-Id"));
        String expected = "https://d1npcfkc2mojrf.cloudfront.net/s3ObjectKey?Expires=1704067200"
                          + signature
                          + "&Key-Pair-Id=keyPairId";
        assertThat(expected).isEqualTo(url);
    }

    @Test
    void getSignedURLWithCannedPolicy_withQueryParams_producesValidUrl() {
        Instant expirationDate = LocalDate.of(2024, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        String resourceUrlWithQueryParams = "https://d1npcfkc2mojrf.cloudfront.net/s3ObjectKey?a=b&c=d";
        SignedUrl signedUrl =
            cloudFrontUtilities.getSignedUrlWithCannedPolicy(r -> r
                .resourceUrl(resourceUrlWithQueryParams)
                .privateKey(keyPair.getPrivate())
                .keyPairId("keyPairId")
                .expirationDate(expirationDate));
        String url = signedUrl.url();

        String signature = url.substring(url.indexOf("&Signature"), url.indexOf("&Key-Pair-Id"));
        String expected = "https://d1npcfkc2mojrf.cloudfront.net/s3ObjectKey?a=b&c=d&Expires=1704067200"
                          + signature
                          + "&Key-Pair-Id=keyPairId";
        assertThat(expected).isEqualTo(url);
    }

    @Test
    void getSignedURLWithCustomPolicy_producesValidUrl() throws Exception {
        Instant activeDate = LocalDate.of(2022, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        Instant expirationDate = LocalDate.of(2024, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        String ipRange = "1.2.3.4";
        SignedUrl signedUrl = cloudFrontUtilities.getSignedUrlWithCustomPolicy(r -> {
            try {
                r.resourceUrl(RESOURCE_URL)
                 .privateKey(keyFilePath)
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

    @Test
    void getSignedURLWithCustomPolicy_withQueryParams_producesValidUrl() {
        Instant activeDate = LocalDate.of(2022, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        Instant expirationDate = LocalDate.of(2024, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        String ipRange = "1.2.3.4";
        String resourceUrlWithQueryParams = "https://d1npcfkc2mojrf.cloudfront.net/s3ObjectKey?a=b&c=d";
        SignedUrl signedUrl =
            cloudFrontUtilities.getSignedUrlWithCustomPolicy(r -> r
                .resourceUrl(resourceUrlWithQueryParams)
                .privateKey(keyPair.getPrivate())
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

    @Test
    void getSignedURLWithCustomPolicy_withIpRangeOmitted_producesValidUrl() throws Exception {
        Instant activeDate = LocalDate.of(2022, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        Instant expirationDate = LocalDate.of(2024, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        CustomSignerRequest request = CustomSignerRequest.builder()
                                                         .resourceUrl(RESOURCE_URL)
                                                         .privateKey(keyFilePath)
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

    @Test
    void getSignedURLWithCustomPolicy_withActiveDateOmitted_producesValidUrl() throws Exception {
        Instant expirationDate = LocalDate.of(2024, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        String ipRange = "1.2.3.4";
        CustomSignerRequest request = CustomSignerRequest.builder()
                                                         .resourceUrl(RESOURCE_URL)
                                                         .privateKey(keyFilePath)
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

    @Test
    void getSignedURLWithCustomPolicy_withMissingExpirationDate_shouldThrowException() {
        NullPointerException exception = assertThrows(NullPointerException.class, () ->
            cloudFrontUtilities.getSignedUrlWithCustomPolicy(r -> r
                .resourceUrl(RESOURCE_URL)
                .privateKey(keyPair.getPrivate())
                .keyPairId("keyPairId"))
        );
        assertThat(exception.getMessage().contains("Expiration date must be provided to sign CloudFront URLs"));
    }

    @Test
    void getSignedURLWithCannedPolicy_withEncodedUrl_doesNotDecodeUrl() {
        String encodedUrl = "https://distributionDomain/s3ObjectKey/%40blob?v=1n1dm%2F01n1dm0";
        Instant expirationDate = LocalDate.of(2024, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        SignedUrl signedUrl =
            cloudFrontUtilities.getSignedUrlWithCannedPolicy(r -> r
                .resourceUrl(encodedUrl)
                .privateKey(keyPair.getPrivate())
                .keyPairId("keyPairId")
                .expirationDate(expirationDate));
        String url = signedUrl.url();
        String signature = url.substring(url.indexOf("&Signature"), url.indexOf("&Key-Pair-Id"));
        String expected = "https://distributionDomain/s3ObjectKey/%40blob?v=1n1dm%2F01n1dm0&Expires=1704067200"
                          + signature
                          + "&Key-Pair-Id=keyPairId";
        assertThat(expected).isEqualTo(url);
    }

    @Test
    void getSignedURLWithCustomPolicy_withEncodedUrl_doesNotDecodeUrl() {
        String encodedUrl = "https://distributionDomain/s3ObjectKey/%40blob?v=1n1dm%2F01n1dm0";
        Instant activeDate = LocalDate.of(2022, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        Instant expirationDate = LocalDate.of(2024, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        String ipRange = "1.2.3.4";
        SignedUrl signedUrl = cloudFrontUtilities.getSignedUrlWithCustomPolicy(r -> {
            try {
                r.resourceUrl(encodedUrl)
                 .privateKey(keyFilePath)
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

    @Test
    void getSignedURLWithCannedPolicy_withPortNumber_returnsPortNumber() {
        Instant expirationDate = LocalDate.of(2024, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        SignedUrl signedUrl =
            cloudFrontUtilities.getSignedUrlWithCannedPolicy(r -> r
                .resourceUrl(RESOURCE_URL_WITH_PORT)
                .privateKey(keyPair.getPrivate())
                .keyPairId("keyPairId")
                .expirationDate(expirationDate));
        assertThat(signedUrl.url()).contains("65535");
    }

    @Test
    void getSignedURLWithCustomPolicy_withPortNumber_returnsPortNumber() {
        Instant expirationDate = LocalDate.of(2024, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        SignedUrl signedUrl =
            cloudFrontUtilities.getSignedUrlWithCustomPolicy(r -> r
                .resourceUrl(RESOURCE_URL_WITH_PORT)
                .privateKey(keyPair.getPrivate())
                .keyPairId("keyPairId")
                .expirationDate(expirationDate));
        assertThat(signedUrl.url()).contains("65535");
    }

    @Test
    void getCookiesForCannedPolicy_producesValidCookies() throws Exception {
        Instant expirationDate = LocalDate.of(2024, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        CannedSignerRequest request = CannedSignerRequest.builder()
                                                         .resourceUrl(RESOURCE_URL)
                                                         .privateKey(keyFilePath)
                                                         .keyPairId("keyPairId")
                                                         .expirationDate(expirationDate)
                                                         .build();
        CookiesForCannedPolicy cookiesForCannedPolicy = cloudFrontUtilities.getCookiesForCannedPolicy(request);
        assertThat(cookiesForCannedPolicy.resourceUrl()).isEqualTo(RESOURCE_URL);
        assertThat(cookiesForCannedPolicy.keyPairIdHeaderValue()).isEqualTo("CloudFront-Key-Pair-Id=keyPairId");
    }

    @Test
    void getCookiesForCustomPolicy_producesValidCookies() throws Exception {
        Instant activeDate = LocalDate.of(2022, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        Instant expirationDate = LocalDate.of(2024, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        String ipRange = "1.2.3.4";
        CustomSignerRequest request = CustomSignerRequest.builder()
                                                         .resourceUrl(RESOURCE_URL)
                                                         .privateKey(keyFilePath)
                                                         .keyPairId("keyPairId")
                                                         .expirationDate(expirationDate)
                                                         .activeDate(activeDate)
                                                         .ipRange(ipRange)
                                                         .build();
        CookiesForCustomPolicy cookiesForCustomPolicy = cloudFrontUtilities.getCookiesForCustomPolicy(request);
        assertThat(cookiesForCustomPolicy.resourceUrl()).isEqualTo(RESOURCE_URL);
        assertThat(cookiesForCustomPolicy.keyPairIdHeaderValue()).isEqualTo("CloudFront-Key-Pair-Id=keyPairId");
    }

    @Test
    void getCookiesForCustomPolicy_withActiveDateAndIpRangeOmitted_producesValidCookies() {
        Instant expirationDate = LocalDate.of(2024, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        CustomSignerRequest request = CustomSignerRequest.builder()
                                                         .resourceUrl(RESOURCE_URL)
                                                         .privateKey(keyPair.getPrivate())
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
        String baseUrl = "https://d1234.cloudfront.net/images/photo.jpg";
        Instant expiration = Instant.now().plusSeconds(3600);
        CustomSignerRequest request = CustomSignerRequest.builder()
                                                         .resourceUrl(baseUrl)
                                                         .privateKey(keyPair.getPrivate())
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

        expectedPolicy.append("{\"Statement\": [{")
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
