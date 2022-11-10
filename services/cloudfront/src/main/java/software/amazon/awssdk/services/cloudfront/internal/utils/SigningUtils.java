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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.time.Instant;
import java.util.Base64;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public class SigningUtils {

    private SigningUtils() {
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
    public static String buildCannedPolicy(String resourceUrl, Instant expirationDate) {
        return "{\"Statement\":[{\"Resource\":\""
               + resourceUrl
               + "\",\"Condition\":{\"DateLessThan\":{\"AWS:EpochTime\":"
               + expirationDate.getEpochSecond()
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
    public static String buildCustomPolicy(String resourceUrl, Instant activeDate, Instant expirationDate,
                                            String ipAddress) {
        return "{\"Statement\": [{"
               + "\"Resource\":\""
               + resourceUrl
               + "\""
               + ",\"Condition\":{"
               + "\"DateLessThan\":{\"AWS:EpochTime\":"
               + expirationDate.getEpochSecond()
               + "}"
               + (ipAddress == null
                  ? ""
                  : ",\"IpAddress\":{\"AWS:SourceIp\":\"" + ipAddress + "\"}"
               )
               + (activeDate == null
                  ? ""
                  : ",\"DateGreaterThan\":{\"AWS:EpochTime\":" + activeDate.getEpochSecond() + "}"
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
        return new String(encoded, UTF_8);
    }

    /**
     * Converts the given string to be safe for use in signed URLs for a private
     * distribution.
     */
    public static String makeStringUrlSafe(String str) {
        return makeBytesUrlSafe(str.getBytes(UTF_8));
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

}
