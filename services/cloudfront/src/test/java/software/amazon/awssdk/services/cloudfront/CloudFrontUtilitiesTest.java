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
import software.amazon.awssdk.services.cloudfront.internal.utils.SigningUtils;
import software.amazon.awssdk.services.cloudfront.model.CannedSignerRequest;
import software.amazon.awssdk.services.cloudfront.model.CustomSignerRequest;
import software.amazon.awssdk.services.cloudfront.url.SignedUrl;


class CloudFrontUtilitiesTest {
    private static final String resourceUrl = "https://distributionDomain-cloudfront.net/s3ObjectKey";
    private static KeyPairGenerator kpg;
    private static KeyPair keyPair;
    private static File keyFile;
    private static Path keyFilePath;
    private static CloudFrontUtilities cloudFrontUtilities;

    @BeforeAll
    static void setUp() throws Exception {
        initKeys();
        cloudFrontUtilities = CloudFrontClient.create().utilities();
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
    void getSignedURLWithCannedPolicy_shouldWork() throws Exception {

        Instant expirationDate = LocalDate.of(2024, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        CannedSignerRequest request = CannedSignerRequest.builder()
                                                         .resourceUrl(resourceUrl)
                                                         .privateKey(SigningUtils.loadPrivateKey(keyFilePath))
                                                         .keyPairId("keyPairId")
                                                         .expirationDate(expirationDate).build();
        SignedUrl signedUrl = cloudFrontUtilities.getSignedUrlWithCannedPolicy(request);
        String signature = signedUrl.url().substring(signedUrl.url().indexOf("&Signature"), signedUrl.url().indexOf("&Key-Pair-Id"));
        String expected = "https://distributionDomain-cloudfront.net/s3ObjectKey?Expires=1704067200"
                          + signature
                          + "&Key-Pair-Id=keyPairId";
        assertThat(expected).isEqualTo(signedUrl.url());
    }

    @Test
    void getSignedURLWithCustomPolicy_shouldWork() {
        Instant activeDate = LocalDate.of(2022, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        Instant expirationDate = LocalDate.of(2024, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        String ipRange = "1.2.3.4";
        CustomSignerRequest request = CustomSignerRequest.builder()
                                                         .resourceUrl(resourceUrl)
                                                         .privateKey(keyPair.getPrivate())
                                                         .keyPairId("keyPairId")
                                                         .expirationDate(expirationDate)
                                                         .activeDate(activeDate)
                                                         .ipRange(ipRange).build();
        SignedUrl signedUrl = cloudFrontUtilities.getSignedUrlWithCustomPolicy(request);
        String signature = signedUrl.url().substring(signedUrl.url().indexOf("&Signature"), signedUrl.url().indexOf("&Key-Pair-Id"));
        String expected = "https://distributionDomain-cloudfront.net/s3ObjectKey?Policy"
                          + "=eyJTdGF0ZW1lbnQiOiBbeyJSZXNvdXJjZSI6Imh0dHBzOi8vZGlzdHJpYnV0aW9uRG9tYWluLWNsb3VkZnJvbnQubmV0L3MzT2JqZWN0S2V5IiwiQ29uZGl0aW9uIjp7IkRhdGVMZXNzVGhhbiI6eyJBV1M6RXBvY2hUaW1lIjoxNzA0MDY3MjAwfSwiSXBBZGRyZXNzIjp7IkFXUzpTb3VyY2VJcCI6IjEuMi4zLjQifSwiRGF0ZUdyZWF0ZXJUaGFuIjp7IkFXUzpFcG9jaFRpbWUiOjE2NDA5OTUyMDB9fX1dfQ__"
                          + signature
                          + "&Key-Pair-Id=keyPairId";
        assertThat(expected).isEqualTo(signedUrl.url());
    }

}
