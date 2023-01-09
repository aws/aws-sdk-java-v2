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

import static org.assertj.core.api.Assertions.assertThat;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.cloudfront.cookie.CookiesForCannedPolicy;
import software.amazon.awssdk.services.cloudfront.cookie.CookiesForCustomPolicy;
import software.amazon.awssdk.services.cloudfront.model.CannedSignerRequest;
import software.amazon.awssdk.services.cloudfront.model.CustomSignerRequest;
import software.amazon.awssdk.services.cloudfront.url.SignedUrl;


class CloudFrontUtilitiesTest {
    private static final String resourceUrl = "https://d1npcfkc2mojrf.cloudfront.net/s3ObjectKey";
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
                .resourceUrl(resourceUrl)
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
                r.resourceUrl(resourceUrl)
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
                                                         .resourceUrl(resourceUrl)
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
                                                         .resourceUrl(resourceUrl)
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
        SdkClientException exception = assertThrows(SdkClientException.class, () ->
            cloudFrontUtilities.getSignedUrlWithCustomPolicy(r -> r
                .resourceUrl(resourceUrl)
                .privateKey(keyPair.getPrivate())
                .keyPairId("keyPairId"))
        );
        assertThat(exception.getMessage().contains("Expiration date must be provided to sign CloudFront URLs"));
    }

    @Test
    void getCookiesForCannedPolicy_producesValidCookies() throws Exception {
        Instant expirationDate = LocalDate.of(2024, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        CannedSignerRequest request = CannedSignerRequest.builder()
                                                         .resourceUrl(resourceUrl)
                                                         .privateKey(keyFilePath)
                                                         .keyPairId("keyPairId")
                                                         .expirationDate(expirationDate)
                                                         .build();
        CookiesForCannedPolicy cookiesForCannedPolicy = cloudFrontUtilities.getCookiesForCannedPolicy(request);
        assertThat(cookiesForCannedPolicy.resourceUrl()).isEqualTo(resourceUrl);
        assertThat(cookiesForCannedPolicy.keyPairIdHeaderValue()).isEqualTo("CloudFront-Key-Pair-Id=keyPairId");
    }

    @Test
    void getCookiesForCustomPolicy_producesValidCookies() throws Exception {
        Instant activeDate = LocalDate.of(2022, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        Instant expirationDate = LocalDate.of(2024, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        String ipRange = "1.2.3.4";
        CustomSignerRequest request = CustomSignerRequest.builder()
                                                         .resourceUrl(resourceUrl)
                                                         .privateKey(keyFilePath)
                                                         .keyPairId("keyPairId")
                                                         .expirationDate(expirationDate)
                                                         .activeDate(activeDate)
                                                         .ipRange(ipRange)
                                                         .build();
        CookiesForCustomPolicy cookiesForCustomPolicy = cloudFrontUtilities.getCookiesForCustomPolicy(request);
        assertThat(cookiesForCustomPolicy.resourceUrl()).isEqualTo(resourceUrl);
        assertThat(cookiesForCustomPolicy.keyPairIdHeaderValue()).isEqualTo("CloudFront-Key-Pair-Id=keyPairId");
    }

    @Test
    void getCookiesForCustomPolicy_withActiveDateAndIpRangeOmitted_producesValidCookies() {
        Instant expirationDate = LocalDate.of(2024, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        CustomSignerRequest request = CustomSignerRequest.builder()
                                                         .resourceUrl(resourceUrl)
                                                         .privateKey(keyPair.getPrivate())
                                                         .keyPairId("keyPairId")
                                                         .expirationDate(expirationDate)
                                                         .build();
        CookiesForCustomPolicy cookiesForCustomPolicy = cloudFrontUtilities.getCookiesForCustomPolicy(request);
        assertThat(cookiesForCustomPolicy.resourceUrl()).isEqualTo(resourceUrl);
        assertThat(cookiesForCustomPolicy.keyPairIdHeaderValue()).isEqualTo("CloudFront-Key-Pair-Id=keyPairId");
    }

}
