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

import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Contains common cookies used by Amazon CloudFront.
 */
@Immutable
@ThreadSafe
@SdkPublicApi
public final class CloudFrontSignedCookie implements ToCopyableBuilder<CloudFrontSignedCookie.Builder, CloudFrontSignedCookie> {
    private static final String KEY_PAIR_ID_KEY = "CloudFront-Key-Pair-Id";
    private static final String SIGNATURE_KEY = "CloudFront-Signature";
    private static final String EXPIRES_KEY = "CloudFront-Expires";
    private static final String POLICY_KEY = "CloudFront-Policy";
    private final String keyPairIdVal;
    private final String signatureVal;
    private final String expiresVal;
    private final String policyVal;
    private final boolean isCustom;

    private CloudFrontSignedCookie(DefaultBuilder builder) {
        this.keyPairIdVal = builder.keyPairIdVal;
        this.signatureVal = builder.signatureVal;
        this.expiresVal = builder.expiresVal;
        this.policyVal = builder.policyVal;
        this.isCustom = builder.isCustom;
    }

    /**
     * Creates a builder for {@link CloudFrontSignedCookie}.
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }

    @Override
    public Builder toBuilder() {
        return new DefaultBuilder(this);
    }

    /**
     * Returns the key pair ID key
     */
    public String keyPairIdKey() {
        return KEY_PAIR_ID_KEY;
    }

    /**
     * Returns the signature key
     */
    public String signatureKey() {
        return SIGNATURE_KEY;
    }

    /**
     * Returns the expires key
     */
    public String expiresKey() {
        if (this.isCustom) {
            throw SdkClientException.create("This is a cookie with a custom policy, use policy instead of expires.");
        }
        return EXPIRES_KEY;
    }

    /**
     * Returns the policy key
     */
    public String policyKey() {
        if (!this.isCustom) {
            throw SdkClientException.create("This is a cookie with a canned policy, use expires instead of policy");
        }
        return POLICY_KEY;
    }

    /**
     * Returns the key pair ID value
     */
    public String keyPairIdVal() {
        return keyPairIdVal;
    }

    /**
     * Returns the signature value
     */
    public String signatureVal() {
        return signatureVal;
    }

    /**
     * Returns the expires value
     */
    public String expiresVal() {
        if (this.isCustom) {
            throw SdkClientException.create("This is a cookie with a custom policy, use policy instead of expires.");
        }
        return expiresVal;
    }

    /**
     * Returns the policy value
     */
    public String policyVal() {
        if (!this.isCustom) {
            throw SdkClientException.create("This is a cookie with a canned policy, use expires instead of policy");
        }
        return policyVal;
    }

    /**
     * Returns true if this is a cookie with custom policy, else false for a cookie with canned policy
     */
    public boolean isCustom() {
        return isCustom;
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
                    throw SdkClientException.create("This is a cookie with a custom policy, use policy instead of expires.");
                }
                return EXPIRES_KEY + "=" + expiresVal;
            case "Policy":
                if (!this.isCustom) {
                    throw SdkClientException.create("This is a cookie with a canned policy, use expires instead of policy");
                }
                return POLICY_KEY + "=" + policyVal;
            default:
                throw SdkClientException.create("Did not provide a valid cookie type");
        }
    }

    @NotThreadSafe
    @SdkPublicApi
    public interface Builder extends CopyableBuilder<CloudFrontSignedCookie.Builder, CloudFrontSignedCookie> {

        /**
         * Configure the key pair ID value
         */
        Builder keyPairId(String keyPairId);

        /**
         * Configure the signature value
         */
        Builder signature(String signature);

        /**
         * Configure the expiration value
         */
        Builder expires(String expires);

        /**
         * Configure the policy value
         */
        Builder policy(String policy);

        /**
         * Configure the value to indicate the type of cookie policy - custom or canned
         */
        Builder isCustom(Boolean isCustom);
    }

    @SdkInternalApi
    private static final class DefaultBuilder implements Builder {
        private String keyPairIdVal;
        private String signatureVal;
        private String expiresVal;
        private String policyVal;
        private boolean isCustom;

        private DefaultBuilder() {
        }

        private DefaultBuilder(CloudFrontSignedCookie cloudFrontSignedCookie) {
            this.keyPairIdVal = cloudFrontSignedCookie.keyPairIdVal;
            this.signatureVal = cloudFrontSignedCookie.signatureVal;
            this.expiresVal = cloudFrontSignedCookie.expiresVal;
            this.policyVal = cloudFrontSignedCookie.policyVal;
            this.isCustom = cloudFrontSignedCookie.isCustom;
        }

        @Override
        public Builder keyPairId(String keyPairId) {
            this.keyPairIdVal = keyPairId;
            return this;
        }

        @Override
        public Builder signature(String signature) {
            this.signatureVal = signature;
            return this;
        }

        @Override
        public Builder expires(String expires) {
            this.expiresVal = expires;
            return this;
        }

        @Override
        public Builder policy(String policy) {
            this.policyVal = policy;
            return this;
        }

        @Override
        public Builder isCustom(Boolean isCustom) {
            this.isCustom = isCustom;
            return this;
        }

        @Override
        public CloudFrontSignedCookie build() {
            return new CloudFrontSignedCookie(this);
        }
    }

}
