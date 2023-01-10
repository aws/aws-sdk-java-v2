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

import java.net.URI;
import java.security.InvalidKeyException;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.cloudfront.cookie.CookiesForCannedPolicy;
import software.amazon.awssdk.services.cloudfront.cookie.CookiesForCustomPolicy;
import software.amazon.awssdk.services.cloudfront.internal.cookie.DefaultCookiesForCannedPolicy;
import software.amazon.awssdk.services.cloudfront.internal.cookie.DefaultCookiesForCustomPolicy;
import software.amazon.awssdk.services.cloudfront.internal.url.DefaultSignedUrl;
import software.amazon.awssdk.services.cloudfront.internal.utils.SigningUtils;
import software.amazon.awssdk.services.cloudfront.model.CannedSignerRequest;
import software.amazon.awssdk.services.cloudfront.model.CustomSignerRequest;
import software.amazon.awssdk.services.cloudfront.url.SignedUrl;

/**
 *
 * Utilities for working with CloudFront distributions
 * <p>
 * To securely serve private content by using CloudFront, you can require that users access your private content by using
 * special CloudFront signed URLs or signed cookies. You then develop your application either to create and distribute signed
 * URLs to authenticated users or to send Set-Cookie headers that set signed cookies for authenticated users.
 * <p>
 * Signed URLs take precedence over signed cookies. If you use both signed URLs and signed cookies to control access to the
 * same files and a viewer uses a signed URL to request a file, CloudFront determines whether to return the file to the
 * viewer based only on the signed URL.
 *
 */
@Immutable
@ThreadSafe
@SdkPublicApi
public final class CloudFrontUtilities {

    private static final String KEY_PAIR_ID_KEY = "CloudFront-Key-Pair-Id";
    private static final String SIGNATURE_KEY = "CloudFront-Signature";
    private static final String EXPIRES_KEY = "CloudFront-Expires";
    private static final String POLICY_KEY = "CloudFront-Policy";

    private CloudFrontUtilities() {
    }

    public static CloudFrontUtilities create() {
        return new CloudFrontUtilities();
    }

    /**
     * Returns a signed URL with a canned policy that grants universal access to
     * private content until a given date.
     * For more information, see <a href=
     * "https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/private-content-creating-signed-url-canned-policy.html"
     * >Creating a signed URL using a canned policy</a>.
     *
     * <p>
     * This is a convenience which creates an instance of the {@link CannedSignerRequest.Builder} avoiding the need to
     * create one manually via {@link CannedSignerRequest#builder()}
     *
     * @param request
     *            A {@link Consumer} that will call methods on {@link CannedSignerRequest.Builder} to create a request.
     * @return A signed URL that will permit access to a specific distribution
     *         and S3 object.
     *
     * <p><b>Example Usage</b>
     * <p>
     * {@snippet :
     *     //Generates signed URL String with canned policy, valid for 7 days
     *     CloudFrontUtilities utilities = CloudFrontUtilities.create();
     *
     *     Instant expirationDate = Instant.now().plus(Duration.ofDays(7));
     *     String resourceUrl = "https://d111111abcdef8.cloudfront.net/s3ObjectKey";
     *     String keyPairId = "myKeyPairId";
     *     PrivateKey privateKey = myPrivateKey;
     *
     *     SignedUrl signedUrl = utilities.getSignedUrlWithCannedPolicy(r -> r.resourceUrl(resourceUrl)
     *                                                                        .privateKey(privateKey)
     *                                                                        .keyPairId(keyPairId)
     *                                                                        .expirationDate(expirationDate));
     *     String url = signedUrl.url();
     * }
     */
    public SignedUrl getSignedUrlWithCannedPolicy(Consumer<CannedSignerRequest.Builder> request) {
        return getSignedUrlWithCannedPolicy(CannedSignerRequest.builder().applyMutation(request).build());
    }

    /**
     * Returns a signed URL with a canned policy that grants universal access to
     * private content until a given date.
     * For more information, see <a href=
     * "https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/private-content-creating-signed-url-canned-policy.html"
     * >Creating a signed URL using a canned policy</a>.
     *
     * @param request
     *            A {@link CannedSignerRequest} configured with the following values:
     *            resourceUrl, privateKey, keyPairId, expirationDate
     * @return A signed URL that will permit access to a specific distribution
     *         and S3 object.
     *
     * <p><b>Example Usage</b>
     * <p>
     * {@snippet :
     *     //Generates signed URL String with canned policy, valid for 7 days
     *     CloudFrontUtilities utilities = CloudFrontUtilities.create();
     *
     *     Instant expirationDate = Instant.now().plus(Duration.ofDays(7));
     *     String resourceUrl = "https://d111111abcdef8.cloudfront.net/s3ObjectKey";
     *     String keyPairId = "myKeyPairId";
     *     Path keyFile = myKeyFile;
     *
     *     CannedSignerRequest cannedRequest = CannedSignerRequest.builder()
     *                                                            .resourceUrl(resourceUrl)
     *                                                            .privateKey(keyFile)
     *                                                            .keyPairId(keyPairId)
     *                                                            .expirationDate(expirationDate)
     *                                                            .build();
     *     SignedUrl signedUrl = utilities.getSignedUrlWithCannedPolicy(cannedRequest);
     *     String url = signedUrl.url();
     * }
     */
    public SignedUrl getSignedUrlWithCannedPolicy(CannedSignerRequest request) {
        try {
            String resourceUrl = request.resourceUrl();
            String cannedPolicy = SigningUtils.buildCannedPolicy(resourceUrl, request.expirationDate());
            byte[] signatureBytes = SigningUtils.signWithSha1Rsa(cannedPolicy.getBytes(UTF_8), request.privateKey());
            String urlSafeSignature = SigningUtils.makeBytesUrlSafe(signatureBytes);
            URI uri = URI.create(resourceUrl);
            String protocol = uri.getScheme();
            String domain = uri.getHost();
            String encodedPath = uri.getPath()
                                 + (uri.getQuery() != null ? "?" + uri.getQuery() + "&" : "?")
                                 + "Expires=" + request.expirationDate().getEpochSecond()
                                 + "&Signature=" + urlSafeSignature
                                 + "&Key-Pair-Id=" + request.keyPairId();
            return DefaultSignedUrl.builder().protocol(protocol).domain(domain).encodedPath(encodedPath)
                                   .url(protocol + "://" + domain + encodedPath).build();
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
     * <p>
     * This is a convenience which creates an instance of the {@link CustomSignerRequest.Builder} avoiding the need to
     * create one manually via {@link CustomSignerRequest#builder()}
     *
     * @param request
     *            A {@link Consumer} that will call methods on {@link CustomSignerRequest.Builder} to create a request.
     * @return A signed URL that will permit access to distribution and S3
     *         objects as specified in the policy document.
     *
     * <p><b>Example Usage</b>
     * <p>
     * {@snippet :
     *     //Generates signed URL String with custom policy, with an access window that begins in 2 days and ends in 7 days,
     *     //for a specified IP range
     *     CloudFrontUtilities utilities = CloudFrontUtilities.create();
     *
     *     Instant expirationDate = Instant.now().plus(Duration.ofDays(7));
     *     String resourceUrl = "https://d111111abcdef8.cloudfront.net/s3ObjectKey";
     *     String keyPairId = "myKeyPairId";
     *     PrivateKey privateKey = myPrivateKey;
     *     Instant activeDate = Instant.now().plus(Duration.ofDays(2));
     *     String ipRange = "192.168.0.1/24";
     *
     *     SignedUrl signedUrl = utilities.getSignedUrlWithCustomPolicy(r -> r.resourceUrl(resourceUrl)
     *                                                                        .privateKey(privateKey)
     *                                                                        .keyPairId(keyPairId)
     *                                                                        .expirationDate(expirationDate)
     *                                                                        .activeDate(activeDate)
     *                                                                        .ipRange(ipRange));
     *     String url = signedUrl.url();
     * }
     */
    public SignedUrl getSignedUrlWithCustomPolicy(Consumer<CustomSignerRequest.Builder> request) {
        return getSignedUrlWithCustomPolicy(CustomSignerRequest.builder().applyMutation(request).build());
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
     *            A {@link CustomSignerRequest} configured with the following values:
     *            resourceUrl, privateKey, keyPairId, expirationDate, activeDate (optional), ipRange (optional)
     * @return A signed URL that will permit access to distribution and S3
     *         objects as specified in the policy document.
     *
     * <p><b>Example Usage</b>
     * <p>
     * {@snippet :
     *     //Generates signed URL String with custom policy, with an access window that begins in 2 days and ends in 7 days,
     *     //for a specified IP range
     *     CloudFrontUtilities utilities = CloudFrontUtilities.create();
     *
     *     Instant expirationDate = Instant.now().plus(Duration.ofDays(7));
     *     String resourceUrl = "https://d111111abcdef8.cloudfront.net/s3ObjectKey";
     *     String keyPairId = "myKeyPairId";
     *     Path keyFile = myKeyFile;
     *     Instant activeDate = Instant.now().plus(Duration.ofDays(2));
     *     String ipRange = "192.168.0.1/24";
     *
     *     CustomSignerRequest customRequest = CustomSignerRequest.builder()
     *                                                            .resourceUrl(resourceUrl)
     *                                                            .privateKey(keyFile)
     *                                                            .keyPairId(keyPairId)
     *                                                            .expirationDate(expirationDate)
     *                                                            .activeDate(activeDate)
     *                                                            .ipRange(ipRange)
     *                                                            .build();
     *     SignedUrl signedUrl = utilities.getSignedUrlWithCustomPolicy(customRequest);
     *     String url = signedUrl.url();
     * }
     */
    public SignedUrl getSignedUrlWithCustomPolicy(CustomSignerRequest request) {
        try {
            String resourceUrl = request.resourceUrl();
            String policy = SigningUtils.buildCustomPolicyForSignedUrl(request.resourceUrl(), request.activeDate(),
                                                                       request.expirationDate(), request.ipRange());
            byte[] signatureBytes = SigningUtils.signWithSha1Rsa(policy.getBytes(UTF_8), request.privateKey());
            String urlSafePolicy = SigningUtils.makeStringUrlSafe(policy);
            String urlSafeSignature = SigningUtils.makeBytesUrlSafe(signatureBytes);
            URI uri = URI.create(resourceUrl);
            String protocol = uri.getScheme();
            String domain = uri.getHost();
            String encodedPath = uri.getPath()
                                 + (uri.getQuery() != null ? "?" + uri.getQuery() + "&" : "?")
                                 + "Policy=" + urlSafePolicy
                                 + "&Signature=" + urlSafeSignature
                                 + "&Key-Pair-Id=" + request.keyPairId();
            return DefaultSignedUrl.builder().protocol(protocol).domain(domain).encodedPath(encodedPath)
                                   .url(protocol + "://" + domain + encodedPath).build();
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
     * <p>
     * This is a convenience which creates an instance of the {@link CannedSignerRequest.Builder} avoiding the need to
     * create one manually via {@link CannedSignerRequest#builder()}
     *
     * @param request
     *            A {@link Consumer} that will call methods on {@link CannedSignerRequest.Builder} to create a request.
     * @return The signed cookies with canned policy.
     *
     * <p><b>Example Usage</b>
     * <p>
     * {@snippet :
     *     //Generates signed Cookie for canned policy, valid for 7 days
     *     CloudFrontUtilities utilities = CloudFrontUtilities.create();
     *
     *     Instant expirationDate = Instant.now().plus(Duration.ofDays(7));
     *     String resourceUrl = "https://d111111abcdef8.cloudfront.net/s3ObjectKey";
     *     String keyPairId = "myKeyPairId";
     *     PrivateKey privateKey = myPrivateKey;
     *
     *     CookiesForCannedPolicy cookies = utilities.getSignedCookiesForCannedPolicy(r -> r.resourceUrl(resourceUrl)
     *                                                                                      .privateKey(privateKey)
     *                                                                                      .keyPairId(keyPairId)
     *                                                                                      .expirationDate(expirationDate));
     *     // Generates Set-Cookie header values to send to the viewer to allow access
     *     String signatureHeaderValue = cookies.signatureHeaderValue();
     *     String keyPairIdHeaderValue = cookies.keyPairIdHeaderValue();
     *     String expiresHeaderValue = cookies.expiresHeaderValue();
     * }
     */
    public CookiesForCannedPolicy getCookiesForCannedPolicy(Consumer<CannedSignerRequest.Builder> request) {
        return getCookiesForCannedPolicy(CannedSignerRequest.builder().applyMutation(request).build());
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
     *            A {@link CannedSignerRequest} configured with the following values:
     *            resourceUrl, privateKey, keyPairId, expirationDate
     * @return The signed cookies with canned policy.
     *
     * <p><b>Example Usage</b>
     * <p>
     * {@snippet :
     *     //Generates signed Cookie for canned policy, valid for 7 days
     *     CloudFrontUtilities utilities = CloudFrontUtilities.create();
     *
     *     Instant expirationDate = Instant.now().plus(Duration.ofDays(7));
     *     String resourceUrl = "https://d111111abcdef8.cloudfront.net/s3ObjectKey";
     *     String keyPairId = "myKeyPairId";
     *     Path keyFile = myKeyFile;
     *
     *     CannedSignerRequest cannedRequest = CannedSignerRequest.builder()
     *                                                            .resourceUrl(resourceUrl)
     *                                                            .privateKey(keyFile)
     *                                                            .keyPairId(keyPairId)
     *                                                            .expirationDate(expirationDate)
     *                                                            .build();
     *     CookiesForCannedPolicy cookies = utilities.getCookiesForCannedPolicy(cannedRequest);
     *     // Generates Set-Cookie header values to send to the viewer to allow access
     *     String signatureHeaderValue = cookies.signatureHeaderValue();
     *     String keyPairIdHeaderValue = cookies.keyPairIdHeaderValue();
     *     String expiresHeaderValue = cookies.expiresHeaderValue();
     * }
     */
    public CookiesForCannedPolicy getCookiesForCannedPolicy(CannedSignerRequest request) {
        try {
            String cannedPolicy = SigningUtils.buildCannedPolicy(request.resourceUrl(), request.expirationDate());
            byte[] signatureBytes = SigningUtils.signWithSha1Rsa(cannedPolicy.getBytes(UTF_8), request.privateKey());
            String urlSafeSignature = SigningUtils.makeBytesUrlSafe(signatureBytes);
            String expiry = String.valueOf(request.expirationDate().getEpochSecond());
            return DefaultCookiesForCannedPolicy.builder()
                                                .resourceUrl(request.resourceUrl())
                                                .keyPairIdHeaderValue(KEY_PAIR_ID_KEY + "=" + request.keyPairId())
                                                .signatureHeaderValue(SIGNATURE_KEY + "=" + urlSafeSignature)
                                                .expiresHeaderValue(EXPIRES_KEY + "=" + expiry).build();
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
     * <p>
     * This is a convenience which creates an instance of the {@link CustomSignerRequest.Builder} avoiding the need to
     * create one manually via {@link CustomSignerRequest#builder()}
     *
     * @param request
     *            A {@link Consumer} that will call methods on {@link CustomSignerRequest.Builder} to create a request.
     * @return The signed cookies with custom policy.
     *
     * <p><b>Example Usage</b>
     * <p>
     * {@snippet :
     *     //Generates signed Cookie for custom policy, with an access window that begins in 2 days and ends in 7 days,
     *     //for a specified IP range
     *     CloudFrontUtilities utilities = CloudFrontUtilities.create();
     *
     *     Instant expirationDate = Instant.now().plus(Duration.ofDays(7));
     *     String resourceUrl = "https://d111111abcdef8.cloudfront.net/s3ObjectKey";
     *     String keyPairId = "myKeyPairId";
     *     PrivateKey privateKey = myPrivateKey;
     *     Instant activeDate = Instant.now().plus(Duration.ofDays(2));
     *     String ipRange = "192.168.0.1/24";
     *
     *     CookiesForCustomPolicy cookies = utilities.getCookiesForCustomPolicy(r -> r.resourceUrl(resourceUrl)
     *                                                                                .privateKey(privateKey)
     *                                                                                .keyPairId(keyPairId)
     *                                                                                .expirationDate(expirationDate)
     *                                                                                .activeDate(activeDate)
     *                                                                                .ipRange(ipRange));
     *     // Generates Set-Cookie header values to send to the viewer to allow access
     *     String signatureHeaderValue = cookies.signatureHeaderValue();
     *     String keyPairIdHeaderValue = cookies.keyPairIdHeaderValue();
     *     String policyHeaderValue = cookies.policyHeaderValue();
     * }
     */
    public CookiesForCustomPolicy getCookiesForCustomPolicy(Consumer<CustomSignerRequest.Builder> request) {
        return getCookiesForCustomPolicy(CustomSignerRequest.builder().applyMutation(request).build());
    }

    /**
     * Returns signed cookies that provides tailored access to private content based on an access time window and an ip range.
     * For more information, see <a href=
     * "https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/private-content-setting-signed-cookie-custom-policy.html"
     * >Setting signed cookies using a custom policy</a>.
     *
     * @param request
     *            A {@link CustomSignerRequest} configured with the following values:
     *            resourceUrl, privateKey, keyPairId, expirationDate, activeDate (optional), ipRange (optional)
     * @return The signed cookies with custom policy.
     *
     * <p><b>Example Usage</b>
     * <p>
     * {@snippet :
     *     //Generates signed Cookie for custom policy, with an access window that begins in 2 days and ends in 7 days,
     *     //for a specified IP range
     *     CloudFrontUtilities utilities = CloudFrontUtilities.create();
     *
     *     Instant expirationDate = Instant.now().plus(Duration.ofDays(7));
     *     String resourceUrl = "https://d111111abcdef8.cloudfront.net/s3ObjectKey";
     *     String keyPairId = "myKeyPairId";
     *     Path keyFile = myKeyFile;
     *     Instant activeDate = Instant.now().plus(Duration.ofDays(2));
     *     String ipRange = "192.168.0.1/24";
     *
     *     CustomSignerRequest customRequest = CustomSignerRequest.builder()
     *                                                            .resourceUrl(resourceUrl)
     *                                                            .privateKey(keyFile)
     *                                                            .keyPairId(keyFile)
     *                                                            .expirationDate(expirationDate)
     *                                                            .activeDate(activeDate)
     *                                                            .ipRange(ipRange)
     *                                                            .build();
     *     CookiesForCustomPolicy cookies = utilities.getCookiesForCustomPolicy(customRequest);
     *     // Generates Set-Cookie header values to send to the viewer to allow access
     *     String signatureHeaderValue = cookies.signatureHeaderValue();
     *     String keyPairIdHeaderValue = cookies.keyPairIdHeaderValue();
     *     String policyHeaderValue = cookies.policyHeaderValue();
     * }
     */
    public CookiesForCustomPolicy getCookiesForCustomPolicy(CustomSignerRequest request) {
        try {
            String policy = SigningUtils.buildCustomPolicy(request.resourceUrl(), request.activeDate(), request.expirationDate(),
                                                           request.ipRange());
            byte[] signatureBytes = SigningUtils.signWithSha1Rsa(policy.getBytes(UTF_8), request.privateKey());
            String urlSafePolicy = SigningUtils.makeStringUrlSafe(policy);
            String urlSafeSignature = SigningUtils.makeBytesUrlSafe(signatureBytes);
            return DefaultCookiesForCustomPolicy.builder()
                                                .resourceUrl(request.resourceUrl())
                                                .keyPairIdHeaderValue(KEY_PAIR_ID_KEY + "=" + request.keyPairId())
                                                .signatureHeaderValue(SIGNATURE_KEY + "=" + urlSafeSignature)
                                                .policyHeaderValue(POLICY_KEY + "=" + urlSafePolicy).build();
        } catch (InvalidKeyException e) {
            throw SdkClientException.create("Could not sign custom policy cookie", e);
        }
    }

}
