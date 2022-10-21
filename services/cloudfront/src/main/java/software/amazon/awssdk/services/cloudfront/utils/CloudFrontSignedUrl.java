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

import java.time.ZonedDateTime;
import software.amazon.awssdk.annotations.SdkInternalApi;

import static software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignerUtils.UTF8;
import static software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignerUtils.buildCannedPolicy;
import static software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignerUtils.buildCustomPolicy;
import static software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignerUtils.generateResourceUrl;
import static software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignerUtils.loadPrivateKey;
import static software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignerUtils.makeBytesUrlSafe;
import static software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignerUtils.makeStringUrlSafe;
import static software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignerUtils.signWithSha1RSA;
import software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignerUtils.Protocol;
import software.amazon.awssdk.core.exception.SdkClientException;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;

@SdkInternalApi
public final class CloudFrontSignedUrl {

    private CloudFrontSignedUrl(){
    }

    /**
     * Returns a signed URL with a canned policy that grants universal access to
     * private content until a given date.
     * For more information, see <a href=
     * "https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/private-content-creating-signed-url-canned-policy.html"
     * >Creating a signed URL using a canned policy</a>.
     *
     * @param protocol
     *            The protocol of the URL
     * @param distributionDomain
     *            The domain name of the distribution
     * @param s3ObjectKey
     *            The s3 key of the object, or the name of the stream for rtmp
     * @param privateKeyFile
     *            The private key file. RSA private key (.pem) and pkcs8 (.der)
     *            files are supported.
     * @param keyPairId
     *            The key pair id corresponding to the private key file given
     * @param expirationDate
     *            The expiration date of the signed URL in UTC
     * @return The signed URL.
     */
    public static String getSignedURLWithCannedPolicy(Protocol protocol,
                                                      String distributionDomain,
                                                      String s3ObjectKey,
                                                      File privateKeyFile,
                                                      String keyPairId,
                                                      ZonedDateTime expirationDate) throws InvalidKeySpecException, IOException {
        PrivateKey privateKey = loadPrivateKey(privateKeyFile);
        String resourceUrl = generateResourceUrl(protocol, distributionDomain, s3ObjectKey);
        return getSignedURLWithCannedPolicy(resourceUrl, privateKey, keyPairId, expirationDate);
    }

    /**
     * Generate a signed URL that allows access to a specific distribution and
     * S3 object by applying a access restrictions from a "canned" (simplified)
     * policy document.
     * For more information, see <a href=
     * "https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/private-content-creating-signed-url-canned-policy.html"
     * >Creating a signed URL using a canned policy</a>.
     *
     * @param resourceUrl
     *            The URL or path that uniquely identifies a resource within a
     *            distribution. For standard distributions the resource URL will
     *            be <tt>"http://" + distributionName + "/" + objectKey</tt>
     *            (may also include URL parameters. For distributions with the
     *            HTTPS required protocol, the resource URL must start with
     *            <tt>"https://"</tt>. RTMP resources do not take the form of a
     *            URL, and instead the resource path is nothing but the stream's
     *            name.
     * @param privateKey
     *            The private key data that corresponding to the keypair
     *            identified by keyPairId
     * @param keyPairId
     *            Identifier of a public/private certificate keypair already
     *            configured in your Amazon Web Services account.
     * @param expirationDate
     *            The UTC time and date when the signed URL will expire.
     * @return A signed URL that will permit access to a specific distribution
     *         and S3 object.
     */

    public static String getSignedURLWithCannedPolicy(String resourceUrl,
                                                      PrivateKey privateKey,
                                                      String keyPairId,
                                                      ZonedDateTime expirationDate) {
        try {
            String cannedPolicy = buildCannedPolicy(resourceUrl, expirationDate);
            byte[] signatureBytes = signWithSha1RSA(cannedPolicy.getBytes(UTF8), privateKey);
            String urlSafeSignature = makeBytesUrlSafe(signatureBytes);
            return resourceUrl
                   + (resourceUrl.indexOf('?') >= 0 ? "&" : "?")
                   + "Expires=" + expirationDate.toEpochSecond()
                   + "&Signature=" + urlSafeSignature
                   + "&Key-Pair-Id=" + keyPairId;
        } catch (InvalidKeyException e) {
            throw SdkClientException.create("Could not sign url", e);
        }
    }

    /**
     * Returns a signed URL that provides tailored access to private content
     * based on an access time window and an ip range.
     * For more information, see <a href=
     * "https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/private-content-creating-signed-url-custom-policy.html"
     * >Creating a signed URL using a custom policy</a>.
     *
     * @param protocol
     *            The protocol of the URL
     * @param distributionDomain
     *            The domain name of the distribution
     * @param s3ObjectKey
     *            The s3 key of the object, or the name of the stream for rtmp
     * @param privateKeyFile
     *            Your private key file. RSA private key (.pem) and pkcs8 (.der)
     *            files are supported.
     * @param keyPairId
     *            The key pair id corresponding to the private key file given
     * @param activeDate
     *            The beginning valid date of the signed URL in UTC
     * @param expirationDate
     *            The expiration date of the signed URL in UTC
     * @param ipRange
     *            The allowed IP address range of the client making the GET
     *            request, in CIDR form (e.g. 192.168.0.1/24).
     * @return The signed URL.
     */
    public static String getSignedURLWithCustomPolicy(Protocol protocol,
                                                      String distributionDomain,
                                                      String s3ObjectKey,
                                                      File privateKeyFile,
                                                      String keyPairId,
                                                      ZonedDateTime activeDate,
                                                      ZonedDateTime expirationDate,
                                                      String ipRange) throws InvalidKeySpecException, IOException {
        PrivateKey privateKey = loadPrivateKey(privateKeyFile);
        String resourceUrl = generateResourceUrl(protocol, distributionDomain, s3ObjectKey);
        String policy = buildCustomPolicyForSignedUrl(resourceUrl, activeDate, expirationDate, ipRange);
        return getSignedURLWithCustomPolicy(resourceUrl, privateKey, keyPairId, policy);
    }

    /**
     * Generate a signed URL that allows access to distribution and S3 objects
     * by applying access restrictions specified in a custom policy document.
     * For more information, see <a href=
     * "https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/private-content-creating-signed-url-custom-policy.html"
     * >Creating a signed URL using a custom policy</a>.
     *
     * @param resourceUrl
     *            The URL or path that uniquely identifies a resource within a
     *            distribution. For standard distributions the resource URL will
     *            be <tt>"http://" + distributionName + "/" + objectKey</tt>
     *            (may also include URL parameters. For distributions with the
     *            HTTPS required protocol, the resource URL must start with
     *            <tt>"https://"</tt>. RTMP resources do not take the form of a
     *            URL, and instead the resource path is nothing but the stream's
     *            name.
     * @param privateKey
     *            The RSA private key data that corresponding to the certificate
     *            keypair identified by keyPairId.
     * @param keyPairId
     *            Identifier of a public/private certificate keypair already
     *            configured in your Amazon Web Services account.
     * @param policy
     *            A policy document that describes the access permissions that
     *            will be applied by the signed URL. To generate a custom policy
     *            use buildCustomPolicyForSignedUrl
     * @return A signed URL that will permit access to distribution and S3
     *         objects as specified in the policy document.
     */
    public static String getSignedURLWithCustomPolicy(String resourceUrl,
                                                      PrivateKey privateKey,
                                                      String keyPairId,
                                                      String policy) {
        try {
            byte[] signatureBytes = signWithSha1RSA(policy.getBytes(UTF8), privateKey);
            String urlSafePolicy = makeStringUrlSafe(policy);
            String urlSafeSignature = makeBytesUrlSafe(signatureBytes);
            return resourceUrl
                   + (resourceUrl.indexOf('?') >= 0 ? "&" : "?")
                   + "Policy=" + urlSafePolicy
                   + "&Signature=" + urlSafeSignature
                   + "&Key-Pair-Id=" + keyPairId;
        } catch (InvalidKeyException e) {
            throw SdkClientException.create("Could not sign url", e);
        }
    }

    /**
     * Generate a policy document that describes custom access permissions to
     * apply via a private distribution's signed URL.
     *
     * @param resourceUrl
     *            An optional HTTP/S or RTMP resource path that restricts which
     *            distribution and S3 objects will be accessible in a signed
     *            URL. For standard distributions the resource URL will be
     *            <tt>"http://" + distributionName + "/" + objectKey</tt> (may
     *            also include URL parameters. For distributions with the HTTPS
     *            required protocol, the resource URL must start with
     *            <tt>"https://"</tt>. RTMP resources do not take the form of a
     *            URL, and instead the resource path is nothing but the stream's
     *            name. The '*' and '?' characters can be used as a wildcards to
     *            allow multi-character or single-character matches
     *            respectively:
     *            <ul>
     *            <li><tt>*</tt> : All distributions/objects will be accessible</li>
     *            <li><tt>a1b2c3d4e5f6g7.cloudfront.net/*</tt> : All objects
     *            within the distribution a1b2c3d4e5f6g7 will be accessible</li>
     *            <li><tt>a1b2c3d4e5f6g7.cloudfront.net/path/to/object.txt</tt>
     *            : Only the S3 object named <tt>path/to/object.txt</tt> in the
     *            distribution a1b2c3d4e5f6g7 will be accessible.</li>
     *            </ul>
     *            If this parameter is null the policy will permit access to all
     *            distributions and S3 objects associated with the certificate
     *            keypair used to generate the signed URL.
     * @param activeDate
     *            An optional UTC time and date when the signed URL will become
     *            active. If null, the signed URL will be active as soon as it
     *            is created.
     * @param expirationDate
     *            The UTC time and date when the signed URL will expire. REQUIRED.
     * @param limitToIpAddressCIDR
     *            An optional range of client IP addresses that will be allowed
     *            to access the distribution, specified as a CIDR range. If
     *            null, the CIDR will be omitted and any client will be
     *            permitted.
     * @return A policy document describing the access permission to apply when
     *         generating a signed URL.
     */
    public static String buildCustomPolicyForSignedUrl(String resourceUrl,
                                                       ZonedDateTime activeDate,
                                                       ZonedDateTime expirationDate,
                                                       String limitToIpAddressCIDR) {
        if (expirationDate == null) {
            throw SdkClientException.create("Expiration date must be provided to sign CloudFront URLs");
        }
        if (resourceUrl == null) {
            resourceUrl = "*";
        }
        return buildCustomPolicy(resourceUrl, activeDate, expirationDate, limitToIpAddressCIDR);
    }
}
