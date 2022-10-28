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

import static software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignerUtils.UTF8;
import static software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignerUtils.buildCannedPolicy;
import static software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignerUtils.buildCustomPolicy;
import static software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignerUtils.generateResourceUrl;
import static software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignerUtils.loadPrivateKey;
import static software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignerUtils.makeBytesUrlSafe;
import static software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignerUtils.makeStringUrlSafe;
import static software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignerUtils.signWithSha1Rsa;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.time.ZonedDateTime;
import java.util.Map;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignerUtils.Protocol;

@Immutable
@SdkPublicApi
public final class CloudFrontSignedCookie {

    private CloudFrontSignedCookie() {
    }

    /**
     * Returns signed cookies that grants universal access to private content until a given date.
     * For more information, see <a href=
     * "https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/private-content-setting-signed-cookie-canned-policy.html"
     * >Setting signed cookies using a canned policy</a>.
     *
     * @param protocol           The protocol used to access content using signed cookies.
     * @param distributionDomain The domain name of the distribution.
     * @param resourcePath       The path for the resource.
     * @param privateKeyFile     The private key file in DER format
     * @param keyPairId          The key pair id corresponding to the private key file given.
     * @param expirationDate     The expiration date till which content can be accessed using the generated cookies.
     * @return The signed cookies.
     */
    public static CookiesForCannedPolicy getCookiesForCannedPolicy(Protocol protocol,
                                                                   String distributionDomain,
                                                                   String resourcePath,
                                                                   File privateKeyFile,
                                                                   String keyPairId,
                                                                   ZonedDateTime expirationDate) throws InvalidKeySpecException,
                                                                                                        IOException {
        PrivateKey privateKey = loadPrivateKey(privateKeyFile);
        String resourceUrl = generateResourceUrl(protocol, distributionDomain, resourcePath);
        return getCookiesForCannedPolicy(resourceUrl, privateKey, keyPairId, expirationDate);
    }

    /**
     * Generate signed cookies that allows access to a specific distribution and
     * resource path by applying a access restrictions from a "canned" (simplified)
     * policy document.
     * For more information, see <a href=
     * "https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/private-content-setting-signed-cookie-canned-policy.html"
     * >Setting signed cookies using a canned policy</a>.
     *
     * @param resourceUrl       The URL or path that uniquely identifies a resource within a
     *                          distribution. For standard distributions the resource URL will
     *                          be <tt>"http://" + distributionName + "/" + path</tt>
     *                          (may also include URL parameters. For distributions with the
     *                          HTTPS required protocol, the resource URL must start with
     *                          <tt>"https://"</tt>.
     * @param privateKey        The RSA private key data that corresponding to the certificate keypair identified by keyPairId.
     * @param keyPairId         Identifier of a public/private certificate keypair already configured in your Amazon WebServices
     *                          account
     * @param expirationDate    The expiration date till which content can be accessed using the generated cookies.
     * @return The signed cookies.
     */
    public static CookiesForCannedPolicy getCookiesForCannedPolicy(String resourceUrl,
                                                                   PrivateKey privateKey,
                                                                   String keyPairId,
                                                                   ZonedDateTime expirationDate) {
        try {
            String cannedPolicy = buildCannedPolicy(resourceUrl, expirationDate);
            byte[] signatureBytes = signWithSha1Rsa(cannedPolicy.getBytes(UTF8), privateKey);
            String urlSafeSignature = makeBytesUrlSafe(signatureBytes);
            // Create the cookies
            CookiesForCannedPolicy cookies = new CookiesForCannedPolicy();
            cookies.setExpires(String.valueOf(expirationDate.toEpochSecond()));
            cookies.setSignature(urlSafeSignature);
            cookies.setKeyPairId(keyPairId);
            return cookies;
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
     * @param protocol           The protocol used to access content using signed cookies.
     * @param distributionDomain The domain name of the distribution.
     * @param resourcePath       The path for the resource.
     * @param privateKeyFile     Your private key file. RSA private key (.der) are supported.
     * @param keyPairId          The key pair id corresponding to the private key file given.
     * @param activeDate         The date from which content can be accessed using the generated cookies.
     * @param expirationDate     The expiration date till which content can be accessed using the generated cookies.
     * @param ipRange            The allowed IP address range of the client making the GET request,
     *                           in CIDR form (e.g. 192.168.0.1/24).
     * @return The signed cookies.
     */
    public static CookiesForCustomPolicy getCookiesForCustomPolicy(Protocol protocol,
                                                                   String distributionDomain,
                                                                   String resourcePath,
                                                                   File privateKeyFile,
                                                                   String keyPairId,
                                                                   ZonedDateTime activeDate,
                                                                   ZonedDateTime expirationDate,
                                                                   String ipRange) throws InvalidKeySpecException, IOException {
        PrivateKey privateKey = loadPrivateKey(privateKeyFile);
        String resourceUrl = generateResourceUrl(protocol, distributionDomain, resourcePath);
        return getCookiesForCustomPolicy(resourceUrl, privateKey, keyPairId, activeDate, expirationDate, ipRange);
    }

    /**
     * Returns signed cookies that provides tailored access to private content based on an access time window and an ip range.
     * For more information, see <a href=
     * "https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/private-content-setting-signed-cookie-custom-policy.html"
     * >Setting signed cookies using a custom policy</a>.
     *
     * @param resourceUrl       The URL or path for resource within a distribution.
     * @param privateKey        Your private key file. RSA private key (.der) are supported.
     * @param keyPairId         The key pair id corresponding to the private key file given.
     * @param activeDate        The date from which content can be accessed using the generated cookies.
     * @param expirationDate    The expiration date till which content can be accessed using the generated cookies.
     * @param ipRange           The allowed IP address range of the client making the GET request,
     *                          in CIDR form (e.g. 192.168.0.1/24).
     * @return The signed cookies.
     */
    public static CookiesForCustomPolicy getCookiesForCustomPolicy(String resourceUrl,
                                                                   PrivateKey privateKey,
                                                                   String keyPairId,
                                                                   ZonedDateTime activeDate,
                                                                   ZonedDateTime expirationDate,
                                                                   String ipRange) {
        try {
            String policy = buildCustomPolicy(resourceUrl, activeDate, expirationDate, ipRange);
            byte[] signatureBytes = signWithSha1Rsa(policy.getBytes(UTF8), privateKey);
            String urlSafePolicy = makeStringUrlSafe(policy);
            String urlSafeSignature = makeBytesUrlSafe(signatureBytes);
            // Create the cookies
            CookiesForCustomPolicy cookies = new CookiesForCustomPolicy();
            cookies.setPolicy(urlSafePolicy);
            cookies.setSignature(urlSafeSignature);
            cookies.setKeyPairId(keyPairId);
            return cookies;
        } catch (InvalidKeyException e) {
            throw SdkClientException.create("Could not sign custom policy cookie", e);
        }
    }

    /**
     * Contains common cookies used by Amazon CloudFront.
     */
    public static class SignedCookies {
        /**
         * The active CloudFront key pair Id for the key pair (Trusted Signer) that you are using to generate the signature.
         * For more information, see <a href="http://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/private-content-trusted-signers.html">
         * Specifying the AWS Accounts That Can Create Signed URLs and Signed Cookies (Trusted Signers)</a>
         * in the <i>Amazon CloudFront User Guide</i>.
         */
        protected Map.Entry<String, String> keyPairId;

        protected static class CookieKeyValuePair implements Map.Entry<String, String> {
            private final String key;
            private String value;

            public CookieKeyValuePair(String key, String value) {
                this.key = key;
                this.value = value;
            }

            @Override
            public String getKey() {
                return key;
            }

            @Override
            public String getValue() {
                return value;
            }

            @Override
            public String setValue(String value) {
                String originalValue = this.value;
                this.value = value;
                return originalValue;
            }
        }

        /**
         * The hashed and signed version of the policy.
         */
        protected Map.Entry<String, String> signature;

        public Map.Entry<String, String> getKeyPairId() {
            return keyPairId;
        }

        public void setKeyPairId(String keyPairId) {
            this.keyPairId = new CookieKeyValuePair("CloudFront-Key-Pair-Id", keyPairId);
        }

        public Map.Entry<String, String> getSignature() {
            return signature;
        }

        public void setSignature(String signature) {
            this.signature = new CookieKeyValuePair("CloudFront-Signature", signature);
        }
    }

    /**
     * Contains the cookies used to access restricted content from CloudFront using a canned policy
     */
    public static class CookiesForCannedPolicy extends SignedCookies {
        // Date and time in Unix time format (in seconds) and Coordinated Universal Time (UTC).
        Map.Entry<String, String> expires;

        public Map.Entry<String, String> getExpires() {
            return expires;
        }

        public void setExpires(String expires) {
            this.expires = new CookieKeyValuePair("CloudFront-Expires", expires);
        }
    }

    /**
     * Contains the cookies used to access restricted content from CloudFront using a custom policy.
     */
    public static class CookiesForCustomPolicy extends SignedCookies {
        // Base64 encoded version of the custom policy.
        Map.Entry<String, String> policy;

        public Map.Entry<String, String> getPolicy() {
            return policy;
        }

        public void setPolicy(String policy) {
            this.policy = new CookieKeyValuePair("CloudFront-Policy", policy);
        }
    }
}
