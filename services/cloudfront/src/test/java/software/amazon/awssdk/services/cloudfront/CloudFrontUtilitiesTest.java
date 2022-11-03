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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.cloudfront.model.CloudFrontSignerRequest;


class CloudFrontUtilitiesTest {
    private static final String resourceUrl = "https://distributionDomain/s3ObjectKey";
    private static KeyPairGenerator kpg;
    private static KeyPair keyPair;
    private static File keyFile;

    @BeforeAll
    static void setUp() throws NoSuchAlgorithmException, IOException {
        initKeys();
    }

    @AfterAll
    static void tearDown() {
        keyFile.deleteOnExit();
    }

    static void initKeys() throws NoSuchAlgorithmException, IOException {
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
    }

    @Test
    void getSignedURLWithCannedPolicy_shouldWork() throws Exception {

        Instant expirationDate = LocalDate.of(2024, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        CloudFrontSignerRequest request = CloudFrontSignerRequest.builder()
                                                                 .resourceUrl(resourceUrl)
                                                                 .privateKey(CloudFrontUtilities.loadPrivateKey(keyFile))
                                                                 .keyPairId("keyPairId")
                                                                 .expirationDate(expirationDate).build();
        String signedUrl = CloudFrontUtilities.getSignedUrlWithCannedPolicy(request);
        String signature = signedUrl.substring(signedUrl.indexOf("&Signature"), signedUrl.indexOf("&Key-Pair-Id"));
        String expected = "https://distributionDomain/s3ObjectKey?Expires=1704067200"
                          + signature
                          + "&Key-Pair-Id=keyPairId";
        assertThat(expected).isEqualTo(signedUrl);
    }

    @Test
    void getSignedURLWithCustomPolicy_shouldWork() {
        Instant activeDate = LocalDate.of(2022, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        Instant expirationDate = LocalDate.of(2024, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        String ipRange = "1.2.3.4";
        CloudFrontSignerRequest request = CloudFrontSignerRequest.builder()
                                                                 .resourceUrl(resourceUrl)
                                                                 .privateKey(keyPair.getPrivate())
                                                                 .keyPairId("keyPairId")
                                                                 .expirationDate(expirationDate)
                                                                 .activeDate(activeDate)
                                                                 .ipRange(ipRange).build();
        String signedUrl = CloudFrontUtilities.getSignedUrlWithCustomPolicy(request);
        String signature = signedUrl.substring(signedUrl.indexOf("&Signature"), signedUrl.indexOf("&Key-Pair-Id"));
        String expected = "https://distributionDomain/s3ObjectKey?Policy"
                          + "=eyJTdGF0ZW1lbnQiOiBbeyJSZXNvdXJjZSI6Imh0dHBzOi8vZGlzdHJpYnV0aW9uRG9tYWluL3MzT2JqZWN0S2V5IiwiQ29uZGl0aW9uIjp7IkRhdGVMZXNzVGhhbiI6eyJBV1M6RXBvY2hUaW1lIjoxNzA0MDY3MjAwfSwiSXBBZGRyZXNzIjp7IkFXUzpTb3VyY2VJcCI6IjEuMi4zLjQifSwiRGF0ZUdyZWF0ZXJUaGFuIjp7IkFXUzpFcG9jaFRpbWUiOjE2NDA5OTUyMDB9fX1dfQ__"
                          + signature
                          + "&Key-Pair-Id=keyPairId";
        assertThat(expected).isEqualTo(signedUrl);
    }

    @Test
    void extractEncodedPath_shouldWork() {
        String s3ObjectKey = "s3ObjectKey";
        Instant expirationDate = LocalDate.of(2024, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        CloudFrontSignerRequest request = CloudFrontSignerRequest.builder()
                                                                 .resourceUrl(resourceUrl)
                                                                 .privateKey(keyPair.getPrivate())
                                                                 .keyPairId("keyPairId")
                                                                 .expirationDate(expirationDate).build();
        String signedUrl = CloudFrontUtilities.getSignedUrlWithCannedPolicy(request);
        String encodedPath = CloudFrontUtilities.extractEncodedPath(signedUrl, s3ObjectKey);
        String signature = signedUrl.substring(signedUrl.indexOf("&Signature"), signedUrl.indexOf("&Key-Pair-Id"));
        String expected = "s3ObjectKey?Expires=1704067200"
                          + signature
                          + "&Key-Pair-Id=keyPairId";
        assertThat(encodedPath).isEqualTo(expected);
    }

    @Test
    void buildCustomPolicyForSignedUrl_shouldWork() {
        String expected = "{\"Statement\": [{\"Resource\":\"resourcePath\",\"Condition\":{\"DateLessThan\":{\"AWS:EpochTime\":1704067200},\"IpAddress\":{\"AWS:SourceIp\":\"limitToIpAddressCIDR\"},\"DateGreaterThan\":{\"AWS:EpochTime\":1640995200}}}]}";
        Instant activeDate = LocalDate.of(2022, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        Instant expirationDate = LocalDate.of(2024, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        String policy = CloudFrontUtilities.buildCustomPolicyForSignedUrl("resourcePath", activeDate, expirationDate, "limitToIpAddressCIDR");
        assertThat(expected).isEqualTo(policy);
    }

}
