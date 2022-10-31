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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.time.ZonedDateTime;
import java.util.Base64;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.cloudfront.auth.Pem;
import software.amazon.awssdk.services.cloudfront.auth.Rsa;
import software.amazon.awssdk.utils.StringUtils;

@Immutable
@SdkPublicApi
public final class CloudFrontSignerUtils {

    public static final Charset UTF8 = StandardCharsets.UTF_8;

    private CloudFrontSignerUtils() {
    }

    /**
     * Enumeration of protocols for presigned URLs
     */
    public enum Protocol {
        HTTP, HTTPS, RTMP;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    /**
     * Returns a "canned" policy for the given parameters.
     * For more information, see
     * <a href =
     * "https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/private-content-creating-signed-url-canned-policy.html"
     * >Creating a signed URL using a canned policy</a>
     * or
     * <a href=
     * "https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/private-content-setting-signed-cookie-canned-policy.html"
     * >Setting signed cookies using a canned policy</a>.
     */
    public static String buildCannedPolicy(String resourceUrl, ZonedDateTime expirationDate) {
        return "{\"Statement\":[{\"Resource\":\""
               + resourceUrl
               + "\",\"Condition\":{\"DateLessThan\":{\"AWS:EpochTime\":"
               + expirationDate.toEpochSecond()
               + "}}}]}";
    }

    /**
     * Returns a custom policy for the given parameters.
     * For more information, see <a href=
     * "https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/private-content-creating-signed-url-custom-policy.html"
     * >Creating a signed URL using a custom policy</a>
     * or
     * <a href=
     * "https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/private-content-setting-signed-cookie-custom-policy.html"
     * >Setting signed cookies using a custom policy</a>.
     */
    public static String buildCustomPolicy(String resourceUrl, ZonedDateTime activeDate, ZonedDateTime expirationDate,
                                           String ipAddress) {
        return "{\"Statement\": [{"
               + "\"Resource\":\""
               + resourceUrl
               + "\""
               + ",\"Condition\":{"
               + "\"DateLessThan\":{\"AWS:EpochTime\":"
               + expirationDate.toEpochSecond()
               + "}"
               + (ipAddress == null
                  ? ""
                  : ",\"IpAddress\":{\"AWS:SourceIp\":\"" + ipAddress + "\"}"
               )
               + (activeDate == null
                  ? ""
                  : ",\"DateGreaterThan\":{\"AWS:EpochTime\":" + activeDate.toEpochSecond() + "}"
               )
               + "}}]}";
    }

    /**
     * Converts the given data to be safe for use in signed URLs for a private
     * distribution by using specialized Base64 encoding.
     */
    public static String makeBytesUrlSafe(byte[] bytes) {
        byte[] encoded = Base64.getEncoder().encode(bytes);

        for (int i = 0; i < encoded.length; i++) {
            switch (encoded[i]) {
                case '+':
                    encoded[i] = '-';
                    continue;
                case '=':
                    encoded[i] = '_';
                    continue;
                case '/':
                    encoded[i] = '~';
                    continue;
                default:
            }
        }
        return new String(encoded, UTF8);
    }

    /**
     * Converts the given string to be safe for use in signed URLs for a private
     * distribution.
     */
    public static String makeStringUrlSafe(String str) {
        return makeBytesUrlSafe(str.getBytes(UTF8));
    }

    /**
     * Returns the resource path for the given distribution, object, and
     * protocol.
     */
    public static String generateResourceUrl(Protocol protocol, String distributionDomain, String resourcePath) {
        return protocol == Protocol.HTTP || protocol == Protocol.HTTPS
               ? protocol + "://" + distributionDomain + "/" + resourcePath
               : resourcePath;
    }

    /**
     * Signs the data given with the private key given, using the SHA1withRSA
     * algorithm provided by bouncy castle.
     */
    public static byte[] signWithSha1Rsa(byte[] dataToSign, PrivateKey privateKey) throws InvalidKeyException {
        try {
            Signature signature = Signature.getInstance("SHA1withRSA");
            SecureRandom random = new SecureRandom();
            signature.initSign(privateKey, random);
            signature.update(dataToSign);
            return signature.sign();
        } catch (NoSuchAlgorithmException | SignatureException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Creates a private key from the file given, either in RSA private key
     * (.pem) or pkcs8 (.der) format. Other formats will cause an exception to
     * be thrown.
     */
    public static PrivateKey loadPrivateKey(File privateKeyFile) throws InvalidKeySpecException, IOException {
        Path path = privateKeyFile.toPath();
        if (StringUtils.lowerCase(privateKeyFile.getAbsolutePath()).endsWith(".pem")) {
            try (InputStream is = Files.newInputStream(path)) {
                return Pem.readPrivateKey(is);
            }
        }
        if (StringUtils.lowerCase(privateKeyFile.getAbsolutePath()).endsWith(".der")) {
            try (InputStream is = Files.newInputStream(path)) {
                return Rsa.privateKeyFromPkcs8(toByteArray(is));
            }
        }
        throw SdkClientException.create("Unsupported file type for private key");
    }

    private static byte[] toByteArray(InputStream is) throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] b = new byte[1024 * 4];
            int n = 0;
            while ((n = is.read(b)) != -1) {
                output.write(b, 0, n);
            }
            return output.toByteArray();
        }
    }
}
