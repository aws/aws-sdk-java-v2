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

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.time.Instant;
import java.util.Base64;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.cloudfront.internal.auth.Pem;
import software.amazon.awssdk.services.cloudfront.internal.auth.Rsa;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.StringUtils;

@SdkInternalApi
public final class SigningUtils {

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

    /**
     * Generate a policy document that describes custom access permissions to
     * apply via a private distribution's signed URL.
     *
     * @param resourceUrl
     *            The HTTP/S resource path that restricts which distribution and
     *            S3 objects will be accessible in a signed URL, i.e.,
     *            <tt>"https://" + distributionName + "/" + objectKey</tt> (may
     *            also include URL parameters). The '*' and '?' characters can
     *            be used as a wildcards to allow multi-character or single-character
     *            matches respectively:
     *            <ul>
     *            <li><tt>*</tt> : All distributions/objects will be accessible</li>
     *            <li><tt>a1b2c3d4e5f6g7.cloudfront.net/*</tt> : All objects
     *            within the distribution a1b2c3d4e5f6g7 will be accessible</li>
     *            <li><tt>a1b2c3d4e5f6g7.cloudfront.net/path/to/object.txt</tt>
     *            : Only the S3 object named <tt>path/to/object.txt</tt> in the
     *            distribution a1b2c3d4e5f6g7 will be accessible.</li>
     *            </ul>
     * @param activeDate
     *            An optional UTC time and date when the signed URL will become
     *            active. If null, the signed URL will be active as soon as it
     *            is created.
     * @param expirationDate
     *            The UTC time and date when the signed URL will expire. REQUIRED.
     * @param limitToIpAddressCidr
     *            An optional range of client IP addresses that will be allowed
     *            to access the distribution, specified as an IPv4 CIDR range
     *            (IPv6 format is not supported). If null, the CIDR will be omitted
     *            and any client will be permitted.
     * @return A policy document describing the access permission to apply when
     *         generating a signed URL.
     */
    public static String buildCustomPolicyForSignedUrl(String resourceUrl,
                                                       Instant activeDate,
                                                       Instant expirationDate,
                                                       String limitToIpAddressCidr) {
        if (expirationDate == null) {
            throw SdkClientException.create("Expiration date must be provided to sign CloudFront URLs");
        }
        if (resourceUrl == null) {
            resourceUrl = "*";
        }
        return buildCustomPolicy(resourceUrl, activeDate, expirationDate, limitToIpAddressCidr);
    }

    /**
     * Creates a private key from the file given, either in pem or der format.
     * Other formats will cause an exception to be thrown.
     */
    public static PrivateKey loadPrivateKey(Path keyFile) throws Exception {
        if (StringUtils.lowerCase(keyFile.toString()).endsWith(".pem")) {
            try (InputStream is = Files.newInputStream(keyFile)) {
                return Pem.readPrivateKey(is);
            }
        }
        if (StringUtils.lowerCase(keyFile.toString()).endsWith(".der")) {
            try (InputStream is = Files.newInputStream(keyFile)) {
                return Rsa.privateKeyFromPkcs8(IoUtils.toByteArray(is));
            }
        }
        throw SdkClientException.create("Unsupported file type for private key");
    }

}
