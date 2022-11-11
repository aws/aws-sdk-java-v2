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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.cloudfront.cookie.CookiesForCannedPolicy;
import software.amazon.awssdk.services.cloudfront.cookie.CookiesForCustomPolicy;
import software.amazon.awssdk.services.cloudfront.internal.auth.Pem;
import software.amazon.awssdk.services.cloudfront.internal.auth.Rsa;
import software.amazon.awssdk.services.cloudfront.internal.cookie.DefaultCookiesForCannedPolicy;
import software.amazon.awssdk.services.cloudfront.internal.cookie.DefaultCookiesForCustomPolicy;
import software.amazon.awssdk.services.cloudfront.internal.url.DefaultSignedUrl;
import software.amazon.awssdk.services.cloudfront.internal.utils.SigningUtils;
import software.amazon.awssdk.services.cloudfront.model.CloudFrontSignerRequest;
import software.amazon.awssdk.services.cloudfront.url.SignedUrl;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.StringUtils;

/**
 *
 * Utilities for working with CloudFront distributions
 * <p>
 *     To securely serve private content by using CloudFront, you can require that users access your private content by using
 *     special CloudFront signed URLs or signed cookies. You then develop your application either to create and distribute signed
 *     URLs to authenticated users or to send Set-Cookie headers that set signed cookies for authenticated users.
 * </p>
 * <p>
 *     Signed URLs take precedence over signed cookies. If you use both signed URLs and signed cookies to control access to the
 *     same files and a viewer uses a signed URL to request a file, CloudFront determines whether to return the file to the
 *     viewer based only on the signed URL.
 * </p>
 *
 */
@Immutable
@ThreadSafe
@SdkPublicApi
public final class CloudFrontUtilities {

    private static final String CLOUDFRONT_NET = "cloudfront.net";

    private CloudFrontUtilities() {
    }

    /**
     * Returns a signed URL with a canned policy that grants universal access to
     * private content until a given date.
     * For more information, see <a href=
     * "https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/private-content-creating-signed-url-canned-policy.html"
     * >Creating a signed URL using a canned policy</a>.
     *
     * @param request
     *            A CloudFrontSignerRequest configured with the following values:
     *            resourceUrl, privateKey, keyPairId, expirationDate
     * @return A signed URL that will permit access to a specific distribution
     *         and S3 object.
     */
    public static SignedUrl getSignedUrlWithCannedPolicy(CloudFrontSignerRequest request) {
        try {
            String resourceUrl = request.resourceUrl();
            String cannedPolicy = SigningUtils.buildCannedPolicy(resourceUrl, request.expirationDate());
            byte[] signatureBytes = SigningUtils.signWithSha1Rsa(cannedPolicy.getBytes(UTF_8), request.privateKey());
            String urlSafeSignature = SigningUtils.makeBytesUrlSafe(signatureBytes);
            String protocol = resourceUrl.substring(0, resourceUrl.indexOf("://"));
            String domain = resourceUrl.substring(resourceUrl.indexOf("://") + 3, resourceUrl.indexOf(CLOUDFRONT_NET) + 14);
            String encodedPath = resourceUrl.substring(resourceUrl.indexOf(CLOUDFRONT_NET) + 15)
                   + (request.resourceUrl().indexOf('?') >= 0 ? "&" : "?")
                   + "Expires=" + request.expirationDate().getEpochSecond()
                   + "&Signature=" + urlSafeSignature
                   + "&Key-Pair-Id=" + request.keyPairId();
            return DefaultSignedUrl.builder().protocol(protocol).domain(domain).encodedPath(encodedPath).build();
        } catch (InvalidKeyException e) {
            throw SdkClientException.create("Could not sign url", e);
        }
    }

    /**
     * Returns a signed URL that provides tailored access to private content
     * based on an access time window and an ip range. The custom policy itself
     * is included as part of the signed URL (For a signed URL with canned
     * policy, there is no policy included in the signed URL).
     * For more information, see <a href=
     * "https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/private-content-creating-signed-url-custom-policy.html"
     * >Creating a signed URL using a custom policy</a>.
     *
     * @param request
     *            A CloudFrontSignerRequest configured with the following values:
     *            resourceUrl, privateKey, keyPairId, expirationDate, activeDate, ipRange
     * @return A signed URL that will permit access to distribution and S3
     *         objects as specified in the policy document.
     */
    public static SignedUrl getSignedUrlWithCustomPolicy(CloudFrontSignerRequest request) {
        try {
            String resourceUrl = request.resourceUrl();
            String policy = buildCustomPolicyForSignedUrl(request.resourceUrl(), request.activeDate(), request.expirationDate(),
                                                          request.ipRange());
            byte[] signatureBytes = SigningUtils.signWithSha1Rsa(policy.getBytes(UTF_8), request.privateKey());
            String urlSafePolicy = SigningUtils.makeStringUrlSafe(policy);
            String urlSafeSignature = SigningUtils.makeBytesUrlSafe(signatureBytes);
            String protocol = resourceUrl.substring(0, resourceUrl.indexOf("://"));
            String domain = resourceUrl.substring(resourceUrl.indexOf("://") + 3, resourceUrl.indexOf(CLOUDFRONT_NET) + 14);
            String encodedPath = resourceUrl.substring(resourceUrl.indexOf(CLOUDFRONT_NET) + 15)
                   + (request.resourceUrl().indexOf('?') >= 0 ? "&" : "?")
                   + "Policy=" + urlSafePolicy
                   + "&Signature=" + urlSafeSignature
                   + "&Key-Pair-Id=" + request.keyPairId();
            return DefaultSignedUrl.builder().protocol(protocol).domain(domain).encodedPath(encodedPath).build();
        } catch (InvalidKeyException e) {
            throw SdkClientException.create("Could not sign url", e);
        }
    }

    /**
     * Generate signed cookies that allows access to a specific distribution and
     * resource path by applying access restrictions from a "canned" (simplified)
     * policy document.
     * For more information, see <a href=
     * "https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/private-content-setting-signed-cookie-canned-policy.html"
     * >Setting signed cookies using a canned policy</a>.
     *
     * @param request
     *            A CloudFrontSignerRequest configured with the following values:
     *            resourceUrl, privateKey, keyPairId, expirationDate
     * @return The signed cookies with canned policy.
     */
    public static CookiesForCannedPolicy getCookiesForCannedPolicy(CloudFrontSignerRequest request) {
        try {
            String cannedPolicy = SigningUtils.buildCannedPolicy(request.resourceUrl(), request.expirationDate());
            byte[] signatureBytes = SigningUtils.signWithSha1Rsa(cannedPolicy.getBytes(UTF_8), request.privateKey());
            String urlSafeSignature = SigningUtils.makeBytesUrlSafe(signatureBytes);
            String expiry = String.valueOf(request.expirationDate().getEpochSecond());
            return DefaultCookiesForCannedPolicy.builder()
                                                .resourceUrl(request.resourceUrl())
                                                .keyPairId(request.keyPairId())
                                                .signature(urlSafeSignature)
                                                .expires(expiry).build();
        } catch (InvalidKeyException e) {
            throw SdkClientException.create("Could not sign canned policy cookie", e);
        }
    }

    /**
     * Returns signed cookies that provides tailored access to private content based on an access time window and an ip range.
     * For more information, see <a href=
     * "https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/private-content-setting-signed-cookie-custom-policy.html"
     * >Setting signed cookies using a custom policy</a>.
     *
     * @param request
     *            A CloudFrontSignerRequest configured with the following values:
     *            resourceUrl, privateKey, keyPairId, expirationDate, activeDate, ipRange
     * @return The signed cookies with custom policy.
     */
    public static CookiesForCustomPolicy getCookiesForCustomPolicy(CloudFrontSignerRequest request) {
        try {
            String policy = SigningUtils.buildCustomPolicy(request.resourceUrl(), request.activeDate(), request.expirationDate(),
                                                           request.ipRange());
            byte[] signatureBytes = SigningUtils.signWithSha1Rsa(policy.getBytes(UTF_8), request.privateKey());
            String urlSafePolicy = SigningUtils.makeStringUrlSafe(policy);
            String urlSafeSignature = SigningUtils.makeBytesUrlSafe(signatureBytes);
            return DefaultCookiesForCustomPolicy.builder()
                               .resourceUrl(request.resourceUrl())
                               .keyPairId(request.keyPairId())
                               .signature(urlSafeSignature)
                               .policy(urlSafePolicy).build();
        } catch (InvalidKeyException e) {
            throw SdkClientException.create("Could not sign custom policy cookie", e);
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
        return SigningUtils.buildCustomPolicy(resourceUrl, activeDate, expirationDate, limitToIpAddressCidr);
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
                return Rsa.privateKeyFromPkcs8(IoUtils.toByteArray(is));
            }
        }
        throw SdkClientException.create("Unsupported file type for private key");
    }



}
