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
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import java.util.Base64;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.cloudfront.internal.auth.Pem;
import software.amazon.awssdk.services.cloudfront.internal.auth.Rsa;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.StringUtils;

@Immutable
@ThreadSafe
@SdkPublicApi
public final class CloudFrontUtilities {

    private CloudFrontUtilities() {
    }

    /**
     * Returns a signed URL with a canned policy that grants universal access to
     * private content until a given date.
     * For more information, see <a href=
     * "https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/private-content-creating-signed-url-canned-policy.html"
     * >Creating a signed URL using a canned policy</a>.
     *
     * @param protocol
     *            The protocol of the URL - HTTP or HTTPS
     * @param distributionDomain
     *            The domain name of the distribution
     * @param s3ObjectKey
     *            The s3 key of the object
     * @param privateKeyFile
     *            The private key file. RSA private key (.pem) and pkcs8 (.der)
     *            files are supported.
     * @param keyPairId
     *            Identifier of a public/private certificate keypair already
     *            configured in your Amazon Web Services account.
     * @param expirationDate
     *            The expiration date of the signed URL in UTC
     * @return The signed URL.
     */
    public static String getSignedUrlWithCannedPolicy(Protocol protocol,
                                                      String distributionDomain,
                                                      String s3ObjectKey,
                                                      File privateKeyFile,
                                                      String keyPairId,
                                                      Instant expirationDate) throws InvalidKeySpecException, IOException {
        PrivateKey privateKey = loadPrivateKey(privateKeyFile);
        String resourceUrl = generateResourceUrl(protocol, distributionDomain, s3ObjectKey);
        return getSignedUrlWithCannedPolicy(resourceUrl, privateKey, keyPairId, expirationDate);
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
     *            <tt>"https://"</tt>
     * @param privateKey
     *            The private key data that corresponds to the keypair
     *            identified by keyPairId
     * @param keyPairId
     *            Identifier of a public/private certificate keypair already
     *            configured in your Amazon Web Services account.
     * @param expirationDate
     *            The UTC time and date when the signed URL will expire.
     * @return A signed URL that will permit access to a specific distribution
     *         and S3 object.
     */
    public static String getSignedUrlWithCannedPolicy(String resourceUrl,
                                                      PrivateKey privateKey,
                                                      String keyPairId,
                                                      Instant expirationDate) {
        try {
            String cannedPolicy = buildCannedPolicy(resourceUrl, expirationDate);
            byte[] signatureBytes = signWithSha1Rsa(cannedPolicy.getBytes(UTF_8), privateKey);
            String urlSafeSignature = makeBytesUrlSafe(signatureBytes);
            return resourceUrl
                   + (resourceUrl.indexOf('?') >= 0 ? "&" : "?")
                   + "Expires=" + expirationDate.getEpochSecond()
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
     *            The protocol of the URL - HTTP or HTTPS
     * @param distributionDomain
     *            The domain name of the distribution
     * @param s3ObjectKey
     *            The s3 key of the object
     * @param privateKeyFile
     *            Your private key file. RSA private key (.pem) and pkcs8 (.der)
     *            files are supported.
     * @param keyPairId
     *            Identifier of a public/private certificate keypair already
     *            configured in your Amazon Web Services account.
     * @param activeDate
     *            The beginning valid date of the signed URL in UTC
     * @param expirationDate
     *            The expiration date of the signed URL in UTC
     * @param ipRange
     *            The allowed IP address range of the client making the GET
     *            request, in CIDR form (e.g. 192.168.0.1/24).
     * @return The signed URL.
     */
    public static String getSignedUrlWithCustomPolicy(Protocol protocol,
                                                      String distributionDomain,
                                                      String s3ObjectKey,
                                                      File privateKeyFile,
                                                      String keyPairId,
                                                      Instant activeDate,
                                                      Instant expirationDate,
                                                      String ipRange) throws InvalidKeySpecException, IOException {
        PrivateKey privateKey = loadPrivateKey(privateKeyFile);
        String resourceUrl = generateResourceUrl(protocol, distributionDomain, s3ObjectKey);
        String policy = buildCustomPolicyForSignedUrl(resourceUrl, activeDate, expirationDate, ipRange);
        return getSignedUrlWithCustomPolicy(resourceUrl, privateKey, keyPairId, policy);
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
     *            <tt>"https://"</tt>
     * @param privateKey
     *            The private key data that corresponds to the keypair
     *            identified by keyPairId
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
    public static String getSignedUrlWithCustomPolicy(String resourceUrl,
                                                      PrivateKey privateKey,
                                                      String keyPairId,
                                                      String policy) {
        try {
            byte[] signatureBytes = signWithSha1Rsa(policy.getBytes(UTF_8), privateKey);
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
     * Returns signed cookies that grants universal access to private content until a given date.
     * For more information, see <a href=
     * "https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/private-content-setting-signed-cookie-canned-policy.html"
     * >Setting signed cookies using a canned policy</a>.
     *
     * @param protocol           The protocol used to access content using signed cookies - HTTP or HTTPS
     * @param distributionDomain The domain name of the distribution.
     * @param resourcePath       The path for the resource.
     * @param privateKeyFile     The private key file in DER format
     * @param keyPairId          Identifier of a public/private certificate keypair already configured in your Amazon Web
     *                           Services account.
     * @param expirationDate     The expiration date till which content can be accessed using the generated cookies.
     * @return The signed cookies.
     */
    public static SignedCookie getCookiesForCannedPolicy(Protocol protocol,
                                                         String distributionDomain,
                                                         String resourcePath,
                                                         File privateKeyFile,
                                                         String keyPairId,
                                                         Instant expirationDate) throws InvalidKeySpecException, IOException {
        PrivateKey privateKey = loadPrivateKey(privateKeyFile);
        String resourceUrl = generateResourceUrl(protocol, distributionDomain, resourcePath);
        return getCookiesForCannedPolicy(resourceUrl, privateKey, keyPairId, expirationDate);
    }

    /**
     * Generate signed cookies that allows access to a specific distribution and
     * resource path by applying an access restrictions from a "canned" (simplified)
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
    public static SignedCookie getCookiesForCannedPolicy(String resourceUrl,
                                                         PrivateKey privateKey,
                                                         String keyPairId,
                                                         Instant expirationDate) {
        try {
            String cannedPolicy = buildCannedPolicy(resourceUrl, expirationDate);
            byte[] signatureBytes = signWithSha1Rsa(cannedPolicy.getBytes(UTF_8), privateKey);
            String urlSafeSignature = makeBytesUrlSafe(signatureBytes);
            String expiry = String.valueOf(expirationDate.getEpochSecond());
            return SignedCookie.builder()
                               .isCustom(false).keyPairId(keyPairId).signature(urlSafeSignature).expires(expiry).build();
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
     * @param protocol           The protocol used to access content using signed cookies - HTTP or HTTPS
     * @param distributionDomain The domain name of the distribution.
     * @param resourcePath       The path for the resource.
     * @param privateKeyFile     Your private key file. RSA private key (.der) are supported.
     * @param keyPairId          Identifier of a public/private certificate keypair already configured in your Amazon Web
     *                           Services account.
     * @param activeDate         The date from which content can be accessed using the generated cookies.
     * @param expirationDate     The expiration date till which content can be accessed using the generated cookies.
     * @param ipRange            The allowed IP address range of the client making the GET request,
     *                           in CIDR form (e.g. 192.168.0.1/24).
     * @return The signed cookies.
     */
    public static SignedCookie getCookiesForCustomPolicy(Protocol protocol,
                                                         String distributionDomain,
                                                         String resourcePath,
                                                         File privateKeyFile,
                                                         String keyPairId,
                                                         Instant activeDate,
                                                         Instant expirationDate,
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
     * @param keyPairId          Identifier of a public/private certificate keypair already configured in your Amazon Web
     *                           Services account.
     * @param activeDate        The date from which content can be accessed using the generated cookies.
     * @param expirationDate    The expiration date till which content can be accessed using the generated cookies.
     * @param ipRange           The allowed IP address range of the client making the GET request,
     *                          in CIDR form (e.g. 192.168.0.1/24).
     * @return The signed cookies.
     */
    public static SignedCookie getCookiesForCustomPolicy(String resourceUrl,
                                                         PrivateKey privateKey,
                                                         String keyPairId,
                                                         Instant activeDate,
                                                         Instant expirationDate,
                                                         String ipRange) {
        try {
            String policy = buildCustomPolicy(resourceUrl, activeDate, expirationDate, ipRange);
            byte[] signatureBytes = signWithSha1Rsa(policy.getBytes(UTF_8), privateKey);
            String urlSafePolicy = makeStringUrlSafe(policy);
            String urlSafeSignature = makeBytesUrlSafe(signatureBytes);
            return SignedCookie.builder()
                               .isCustom(true).keyPairId(keyPairId).signature(urlSafeSignature).policy(urlSafePolicy).build();
        } catch (InvalidKeyException e) {
            throw SdkClientException.create("Could not sign custom policy cookie", e);
        }
    }

    /**
     * Contains common cookies used by Amazon CloudFront.
     */
    @SdkPublicApi
    public static final class SignedCookie {
        private static final String KEY_PAIR_ID_KEY = "CloudFront-Key-Pair-Id";
        private static final String SIGNATURE_KEY = "CloudFront-Signature";
        private static final String EXPIRES_KEY = "CloudFront-Expires";
        private static final String POLICY_KEY = "CloudFront-Policy";
        private final String keyPairIdVal;
        private final String signatureVal;
        private final String expiresVal;
        private final String policyVal;
        private final boolean isCustom;

        private SignedCookie(Builder builder) {
            this.keyPairIdVal = builder.keyPairIdVal;
            this.signatureVal = builder.signatureVal;
            this.expiresVal = builder.expiresVal;
            this.policyVal = builder.policyVal;
            this.isCustom = builder.isCustom;
        }

        /**
         * Creates a builder for {@link SignedCookie}.
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Returns the cookie header value for the specified cookie type.
         *
         * @param type The type of the cookie header value to retrieve
         * @return The cookie header value to pass into an HTTP request
         */
        public String cookieHeaderValue(String type) {
            switch (type) {
                case "KeyPairId":
                    return KEY_PAIR_ID_KEY + "=" + keyPairIdVal;
                case "Signature":
                    return SIGNATURE_KEY + "=" + signatureVal;
                case "Expires":
                    if (this.isCustom) {
                        throw SdkClientException.create("This is a custom cookie, use policy instead of expires.");
                    }
                    return EXPIRES_KEY + "=" + expiresVal;
                case "Policy":
                    if (!this.isCustom) {
                        throw SdkClientException.create("This is a canned cookie, use expires instead of policy");
                    }
                    return POLICY_KEY + "=" + policyVal;
                default:
                    throw SdkClientException.create("Did not provide a valid cookie type");
            }
        }

        /**
         * Builder class to construct {@link SignedCookie} object
         */
        public static final class Builder {
            private String keyPairIdVal;
            private String signatureVal;
            private String expiresVal;
            private String policyVal;
            private boolean isCustom;

            private Builder() {
            }

            /**
             * Sets the key pair ID value
             *
             * @return This object for method chaining
             */
            public Builder keyPairId(String keyPairId) {
                this.keyPairIdVal = keyPairId;
                return this;
            }

            /**
             * Sets the signature value
             *
             * @return This object for method chaining
             */
            public Builder signature(String signature) {
                this.signatureVal = signature;
                return this;
            }

            /**
             * Sets the expiration value
             *
             * @return This object for method chaining
             */
            public Builder expires(String expires) {
                this.expiresVal = expires;
                return this;
            }

            /**
             * Sets the policy value
             *
             * @return This object for method chaining
             */
            public Builder policy(String policy) {
                this.policyVal = policy;
                return this;
            }

            /**
             * Sets the value to indicate the type of cookie - custom or canned
             *
             * @return This object for method chaining
             */
            public Builder isCustom(Boolean isCustom) {
                this.isCustom = isCustom;
                return this;
            }

            /**
             * Construct a {@link SignedCookie} object.
             */
            public SignedCookie build() {
                return new SignedCookie(this);
            }
        }

        /**
         * Returns the key pair id key
         */
        public String getKeyPairIdKey() {
            return KEY_PAIR_ID_KEY;
        }

        /**
         * Returns the signature key
         */
        public String getSignatureKey() {
            return SIGNATURE_KEY;
        }

        /**
         * Returns the expires key
         */
        public String getExpiresKey() {
            if (this.isCustom) {
                throw SdkClientException.create("This is a custom cookie, use policy instead of expires.");
            }
            return EXPIRES_KEY;
        }

        /**
         * Returns the policy key
         */
        public String getPolicyKey() {
            if (!this.isCustom) {
                throw SdkClientException.create("This is a canned cookie, use expires instead of policy");
            }
            return POLICY_KEY;
        }

        /**
         * Returns the key pair id value
         */
        public String getKeyPairIdVal() {
            return keyPairIdVal;
        }

        /**
         * Returns the signature value
         */
        public String getSignatureVal() {
            return signatureVal;
        }

        /**
         * Returns the expires value
         */
        public String getExpiresVal() {
            if (this.isCustom) {
                throw SdkClientException.create("This is a custom cookie, use policy instead of expires.");
            }
            return expiresVal;
        }

        /**
         * Returns the policy value
         */
        public String getPolicyVal() {
            if (!this.isCustom) {
                throw SdkClientException.create("This is a canned cookie, use expires instead of policy");
            }
            return policyVal;
        }

        /**
         * Returns true if custom cookie, else false for canned cookie
         */
        public boolean isCustom() {
            return isCustom;
        }
    }

    /**
     * Enumeration of protocols for signed URLs - HTTP, HTTPS
     */
    public enum Protocol {
        HTTP, HTTPS;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    /**
     * Extracts the encoded path from a signed URL to be provided to a HttpRequest
     *
     * @param signedUrl
     *          The generated signed URL
     * @param s3ObjectKey
     *          The s3 key of the object
     * @return The encoded path to be provided to a HttpRequest
     */
    public static String extractEncodedPath(String signedUrl, String s3ObjectKey) {
        return signedUrl.substring(signedUrl.indexOf(s3ObjectKey));
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
     * @param limitToIpAddressCidr
     *            An optional range of client IP addresses that will be allowed
     *            to access the distribution, specified as a CIDR range. If
     *            null, the CIDR will be omitted and any client will be
     *            permitted.
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
     * Creates a private key from the file given, either in RSA private key
     * (.pem) or pkcs8 (.der) format. Other formats will cause an exception to
     * be thrown.
     */
    private static PrivateKey loadPrivateKey(File privateKeyFile) throws InvalidKeySpecException, IOException {
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
    private static String buildCannedPolicy(String resourceUrl, Instant expirationDate) {
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
    private static String buildCustomPolicy(String resourceUrl, Instant activeDate, Instant expirationDate,
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
    private static String makeBytesUrlSafe(byte[] bytes) {
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
    private static String makeStringUrlSafe(String str) {
        return makeBytesUrlSafe(str.getBytes(UTF_8));
    }

    /**
     * Signs the data given with the private key given, using the SHA1withRSA
     * algorithm provided by bouncy castle.
     */
    private static byte[] signWithSha1Rsa(byte[] dataToSign, PrivateKey privateKey) throws InvalidKeyException {
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
