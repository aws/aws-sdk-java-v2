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

package software.amazon.awssdk.services.cloudfront.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignerUtils.buildCannedPolicy;
import static software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignerUtils.Protocol;
import static software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignerUtils.buildCustomPolicy;
import static software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignerUtils.loadPrivateKey;
import static software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignerUtils.makeBytesUrlSafe;
import static software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignerUtils.makeStringUrlSafe;
import static software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignedUrl.buildCustomPolicyForSignedUrl;
import static software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignedUrl.getSignedUrlWithCannedPolicy;
import static software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignedUrl.getSignedUrlWithCustomPolicy;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CloudFrontSignerTest {

    protected static KeyPairGenerator kpg;
    protected static KeyPair keyPair;
    protected static File keyFile;

    @BeforeAll
    static void setUp() throws NoSuchAlgorithmException, IOException {
        initKeys();
        writeKeys();
    }

    @AfterAll
    static void tearDown() {
        keyFile.deleteOnExit();
    }

    static void initKeys() throws NoSuchAlgorithmException {
        kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        keyPair = kpg.generateKeyPair();
    }

    static void writeKeys() throws IOException {
        Base64.Encoder encoder = Base64.getEncoder();
        keyFile = new File("key.pem");
        FileWriter writer = new FileWriter(keyFile);
        writer.write("-----BEGIN PRIVATE KEY-----\n");
        writer.write(encoder.encodeToString(keyPair.getPrivate().getEncoded()));
        writer.write("\n-----END PRIVATE KEY-----\n");
        writer.close();
    }

    @Test
    void makeBytesUrlSafe_shouldWork() {
        String expectedB64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/AAA=";
        String expectedEnc = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-~AAA_";
        byte[] input = Base64.getDecoder().decode(expectedB64);
        String b64 = Base64.getEncoder().encodeToString(input);
        assertThat(expectedB64).isEqualTo(b64);
        String encoded = makeBytesUrlSafe(input);
        assertThat(expectedEnc).isEqualTo(encoded);
    }

    @Test
    void buildCustomPolicyForSignedUrl_shouldWork() {
        String expected = "{\"Statement\": [{\"Resource\":\"resourcePath\",\"Condition\":{\"DateLessThan\":{\"AWS:EpochTime\":1704067200},\"IpAddress\":{\"AWS:SourceIp\":\"limitToIpAddressCIDR\"},\"DateGreaterThan\":{\"AWS:EpochTime\":1640995200}}}]}";
        ZonedDateTime activeDate = ZonedDateTime.of(2022, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        ZonedDateTime expirationDate = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        String policy = buildCustomPolicyForSignedUrl("resourcePath", activeDate, expirationDate, "limitToIpAddressCIDR");
        assertThat(expected).isEqualTo(policy);
    }

    @Test
    void buildCannedPolicy_shouldWork() {
        String expected = "{\"Statement\":[{\"Resource\":\"resourceUrlOrPath\",\"Condition\":{\"DateLessThan\":{\"AWS:EpochTime\":1704067200}}}]}";
        ZonedDateTime expirationDate = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        String policy = buildCannedPolicy("resourceUrlOrPath", expirationDate);
        assertThat(expected).isEqualTo(policy);
    }

    @Test
    void buildCustomPolicy_shouldWork() {
        String expected = "{\"Statement\": [{\"Resource\":\"resourcePath\",\"Condition\":{\"DateLessThan\":{\"AWS:EpochTime\":1704067200},\"IpAddress\":{\"AWS:SourceIp\":\"1.2.3.4\"},\"DateGreaterThan\":{\"AWS:EpochTime\":1640995200}}}]}";
        ZonedDateTime activeDate = ZonedDateTime.of(2022, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        ZonedDateTime expirationDate = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        String ipAddress = "1.2.3.4";
        String policy = buildCustomPolicy("resourcePath", activeDate, expirationDate, ipAddress);
        assertThat(expected).isEqualTo(policy);
    }

    @Test
    void getSignedURLWithCannedPolicy_shouldWork() throws Exception {

        ZonedDateTime expirationDate = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));

        {
            String signedUrl = getSignedUrlWithCannedPolicy(Protocol.HTTPS, "distributionDomain",
                                                                                "s3ObjectKey", keyFile, "keyPairId", expirationDate);
            String signature = signedUrl.substring(signedUrl.indexOf("&Signature"), signedUrl.indexOf("&Key-Pair-Id"));
            String expected = "https://distributionDomain/s3ObjectKey?Expires=1704067200"
                              + signature
                              + "&Key-Pair-Id=keyPairId";
            assertThat(expected).isEqualTo(signedUrl);
        }
        {
            PrivateKey pk =  loadPrivateKey(keyFile);
            String signedUrl = getSignedUrlWithCannedPolicy("https://distributionDomain/s3ObjectKey", pk,"keyPairId", expirationDate);
            String signature = signedUrl.substring(signedUrl.indexOf("&Signature"), signedUrl.indexOf("&Key-Pair-Id"));
            String expected = "https://distributionDomain/s3ObjectKey?Expires=1704067200"
                              + signature
                              + "&Key-Pair-Id=keyPairId";
            assertThat(expected).isEqualTo(signedUrl);
        }
    }

    @Test
    void getSignedURLWithCustomPolicy_shouldWork() throws Exception {
        ZonedDateTime activeDate = ZonedDateTime.of(2022, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        ZonedDateTime expirationDate = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        String ipRange = "1.2.3.4";
        {
            String signedUrl = getSignedUrlWithCustomPolicy(CloudFrontSignerUtils.Protocol.HTTPS, "distributionDomain","s3ObjectKey", keyFile, "keyPairId", activeDate, expirationDate, ipRange);
            String signature = signedUrl.substring(signedUrl.indexOf("&Signature"), signedUrl.indexOf("&Key-Pair-Id"));
            String expected = "https://distributionDomain/s3ObjectKey?Policy"
                              + "=eyJTdGF0ZW1lbnQiOiBbeyJSZXNvdXJjZSI6Imh0dHBzOi8vZGlzdHJpYnV0aW9uRG9tYWluL3MzT2JqZWN0S2V5IiwiQ29uZGl0aW9uIjp7IkRhdGVMZXNzVGhhbiI6eyJBV1M6RXBvY2hUaW1lIjoxNzA0MDY3MjAwfSwiSXBBZGRyZXNzIjp7IkFXUzpTb3VyY2VJcCI6IjEuMi4zLjQifSwiRGF0ZUdyZWF0ZXJUaGFuIjp7IkFXUzpFcG9jaFRpbWUiOjE2NDA5OTUyMDB9fX1dfQ__"
                              + signature
                              + "&Key-Pair-Id=keyPairId";
            assertThat(expected).isEqualTo(signedUrl);
        }
        {
            PrivateKey pk =  loadPrivateKey(keyFile);
            String policy = buildCustomPolicyForSignedUrl("https://distributionDomain/s3ObjectKey", activeDate, expirationDate, ipRange);
            String signedUrl = getSignedUrlWithCustomPolicy("https://distributionDomain/s3ObjectKey", pk, "keyPairId", policy);
            String signature = signedUrl.substring(signedUrl.indexOf("&Signature"), signedUrl.indexOf("&Key-Pair-Id"));
            String expected = "https://distributionDomain/s3ObjectKey?Policy"
                              + "=eyJTdGF0ZW1lbnQiOiBbeyJSZXNvdXJjZSI6Imh0dHBzOi8vZGlzdHJpYnV0aW9uRG9tYWluL3MzT2JqZWN0S2V5IiwiQ29uZGl0aW9uIjp7IkRhdGVMZXNzVGhhbiI6eyJBV1M6RXBvY2hUaW1lIjoxNzA0MDY3MjAwfSwiSXBBZGRyZXNzIjp7IkFXUzpTb3VyY2VJcCI6IjEuMi4zLjQifSwiRGF0ZUdyZWF0ZXJUaGFuIjp7IkFXUzpFcG9jaFRpbWUiOjE2NDA5OTUyMDB9fX1dfQ__"
                              + signature
                              + "&Key-Pair-Id=keyPairId";
            assertThat(expected).isEqualTo(signedUrl);
        }
    }

    @Test
    void buildCustomPolicyForSignedUrl_omits_IpAddress_when_param_is_null() {
        String expectedPolicy = "{\"Statement\": [{\"Resource\":\"resourcePath\",\"Condition\":{\"DateLessThan\":{\"AWS:EpochTime\":1704067200},\"DateGreaterThan\":{\"AWS:EpochTime\":1640995200}}}]}";
        String resourcePath = "resourcePath";
        ZonedDateTime activeDate = ZonedDateTime.of(2022, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        ZonedDateTime expirationDate = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        String generatedPolicy = buildCustomPolicyForSignedUrl(resourcePath, activeDate, expirationDate, null);
        assertThat(expectedPolicy).isEqualTo(generatedPolicy);
    }

    @Test
    void getSignedURLWithCustomPolicy_omits_IpAddress_when_param_is_null() throws Exception {
        String expectedPolicy = "{\"Statement\": [{\"Resource\":\"https://distributionDomain/s3ObjectKey\",\"Condition\":{\"DateLessThan\":{\"AWS:EpochTime\":1704067200},\"DateGreaterThan\":{\"AWS:EpochTime\":1640995200}}}]}";
        ZonedDateTime activeDate = ZonedDateTime.of(2022, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        ZonedDateTime expirationDate = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        String urlSafePolicy = makeStringUrlSafe(expectedPolicy);
        String signedUrl = getSignedUrlWithCustomPolicy(Protocol.HTTPS, "distributionDomain", "s3ObjectKey", keyFile, "keyPairId", activeDate, expirationDate, null);
        assertThat(signedUrl).contains("Policy=" + urlSafePolicy + "&");
    }

}
