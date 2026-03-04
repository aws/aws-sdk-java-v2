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
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.protocols.jsoncore.JsonWriter;
import software.amazon.awssdk.services.cloudfront.internal.auth.Pem;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.Validate;

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
        Validate.notNull(resourceUrl, "resourceUrl must not be null");
        Validate.notNull(expirationDate, "expirationDate must not be null");
        validateInput(resourceUrl, "resourceUrl");

        JsonWriter writer = JsonWriter.create();
        writer.writeStartObject()
              .writeFieldName("Statement")
              .writeStartArray()
              .writeStartObject()
              .writeFieldName("Resource")
              .writeValue(resourceUrl)
              .writeFieldName("Condition")
              .writeStartObject()
              .writeFieldName("DateLessThan")
              .writeStartObject()
              .writeFieldName("AWS:EpochTime")
              .writeValue(expirationDate.getEpochSecond())
              .writeEndObject()
              .writeEndObject()
              .writeEndObject()
              .writeEndArray()
              .writeEndObject();
        return new String(writer.getBytes(), UTF_8);
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
        Validate.notNull(resourceUrl, "resourceUrl must not be null");
        Validate.notNull(expirationDate, "expirationDate must not be null");
        validateInput(resourceUrl, "resourceUrl");
        if (ipAddress != null) {
            validateInput(ipAddress, "ipAddress");
        }

        JsonWriter writer = JsonWriter.create();
        writer.writeStartObject()
              .writeFieldName("Statement")
              .writeStartArray()
              .writeStartObject()
              .writeFieldName("Resource")
              .writeValue(resourceUrl)
              .writeFieldName("Condition")
              .writeStartObject()
              .writeFieldName("DateLessThan")
              .writeStartObject()
              .writeFieldName("AWS:EpochTime")
              .writeValue(expirationDate.getEpochSecond())
              .writeEndObject();

        if (ipAddress != null) {
            writer.writeFieldName("IpAddress")
                  .writeStartObject()
                  .writeFieldName("AWS:SourceIp")
                  .writeValue(ipAddress)
                  .writeEndObject();
        }

        if (activeDate != null) {
            writer.writeFieldName("DateGreaterThan")
                  .writeStartObject()
                  .writeFieldName("AWS:EpochTime")
                  .writeValue(activeDate.getEpochSecond())
                  .writeEndObject();
        }

        writer.writeEndObject()
              .writeEndObject()
              .writeEndArray()
              .writeEndObject();

        return new String(writer.getBytes(), UTF_8);
    }

    /**
     * Validates that the input does not contain characters that could be used for JSON injection attacks.
     * Double quotes, backslashes, and control characters should never appear in valid CloudFront resource URLs
     * or IP addresses.
     *
     * @param input the input string to validate
     * @param paramName the parameter name for error messages
     * @throws IllegalArgumentException if the input contains invalid characters
     */
    private static void validateInput(String input, String paramName) {
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '"' || c == '\\' || Character.isISOControl(c)) {
                throw new IllegalArgumentException(
                    paramName + " contains invalid characters. The character '" + c + "' at position " + i +
                    " is not allowed. URLs and IP addresses should be properly encoded and must not contain " +
                    "double quotes, backslashes, or control characters.");
            }
        }
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
     * Signs the data given with the private key given, using the SHA1withECDSA
     * algorithm provided by bouncy castle.
     */
    public static byte[] signWithSha1ECDSA(byte[] dataToSign, PrivateKey privateKey) throws InvalidKeyException  {
        try {
            Signature signature = Signature.getInstance("SHA1withECDSA");
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

        Validate.notNull(expirationDate, "Expiration date must be provided to sign CloudFront URLs");

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
                return privateKeyFromPkcs8(IoUtils.toByteArray(is));
            }
        }
        throw SdkClientException.create("Unsupported file type for private key");
    }

    /**
     * Attempt to load a private key from PKCS8 DER
     */
    public static PrivateKey privateKeyFromPkcs8(byte[] derBytes) {
        EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(derBytes);
        try {
            return tryKeyLoadFromSpec(privateKeySpec);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        } catch (InvalidKeySpecException e) {
            throw new IllegalArgumentException("Invalid private key, unable to load as either RSA or ECDSA", e);
        }
    }

    /**
     * We don't have a way to determine which algorithm to use, so we try to load as RSA and EC
     */
    private static PrivateKey tryKeyLoadFromSpec(EncodedKeySpec privateKeySpec)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        try {
            return KeyFactory.getInstance("RSA").generatePrivate(privateKeySpec);
        } catch (InvalidKeySpecException rsaFail) {
            return KeyFactory.getInstance("EC").generatePrivate(privateKeySpec);
        }
    }
}
