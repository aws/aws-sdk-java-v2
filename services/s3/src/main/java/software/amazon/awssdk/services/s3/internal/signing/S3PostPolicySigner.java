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

package software.amazon.awssdk.services.s3.internal.signing;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.auth.aws.internal.signer.CredentialScope;
import software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerUtils;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.services.s3.presigner.model.PostPolicyConditions;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * Signature Version 4 signing for S3 POST policies. The signing process base64-encodes the policy document UTF-8 bytes, then
 * computes an HMAC-SHA256 signature over that base64 text using the derived signing key (the same key derivation used for
 * HTTP SigV4).
 */
@SdkInternalApi
public final class S3PostPolicySigner {
    private static final String SIGNING_SERVICE = "s3";

    private S3PostPolicySigner() {
    }

    public static SignedPostPolicy sign(SignInput input) {
        Validate.paramNotNull(input, "input");
        AwsCredentialsIdentity credentials = Validate.paramNotNull(input.credentials(), "credentials");
        String region = Validate.paramNotBlank(input.region(), "region");
        CredentialScope credentialScope = new CredentialScope(region, SIGNING_SERVICE, input.signingInstant());

        String xAmzDate = SignerUtils.formatDateTime(input.signingInstant());
        String credentialScopeString = credentialScope.scope(credentials);

        PostPolicyDocument document = PostPolicyDocument.from(input.bucket(),
                                                             input.objectKey(),
                                                             input.policyExpiration(),
                                                             input.userConditions(),
                                                             input.userFields(),
                                                             credentialScopeString,
                                                             xAmzDate,
                                                             input.sessionToken());

        String policyJson = document.toJson();
        String base64Policy = Base64.getEncoder().encodeToString(policyJson.getBytes(StandardCharsets.UTF_8));

        String hexSignature = computePostPolicySignatureHex(base64Policy, credentials, credentialScope);

        return new SignedPostPolicy(policyJson, base64Policy, hexSignature, credentialScopeString, xAmzDate,
                                    input.sessionToken());
    }


    static String computePostPolicySignatureHex(String base64Policy,
                                                AwsCredentialsIdentity credentials,
                                                CredentialScope credentialScope) {
        byte[] signingKey = SignerUtils.deriveSigningKey(credentials, credentialScope);
        byte[] signatureBytes = SignerUtils.computeSignature(base64Policy, signingKey);
        return BinaryUtils.toHex(signatureBytes);
    }

    /**
     * Inputs required to build and sign a POST policy.
     */
    public static final class SignInput {
        private final AwsCredentialsIdentity credentials;
        private final String region;
        private final java.time.Instant signingInstant;
        private final java.time.Instant policyExpiration;
        private final String bucket;
        private final String objectKey;
        private final PostPolicyConditions userConditions;
        private final java.util.Map<String, String> userFields;
        private final String sessionToken;

        private SignInput(Builder builder) {
            this.credentials = Validate.paramNotNull(builder.credentials, "credentials");
            this.region = Validate.paramNotBlank(builder.region, "region");
            this.signingInstant = Validate.paramNotNull(builder.signingInstant, "signingInstant");
            this.policyExpiration = Validate.paramNotNull(builder.policyExpiration, "policyExpiration");
            this.bucket = Validate.paramNotBlank(builder.bucket, "bucket");
            this.objectKey = Validate.paramNotBlank(builder.objectKey, "objectKey");
            this.userConditions = builder.userConditions;
            this.userFields = java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(builder.userFields));
            this.sessionToken = builder.sessionToken;
        }

        public static Builder builder() {
            return new Builder();
        }

        public AwsCredentialsIdentity credentials() {
            return credentials;
        }

        public String region() {
            return region;
        }

        public java.time.Instant signingInstant() {
            return signingInstant;
        }

        public java.time.Instant policyExpiration() {
            return policyExpiration;
        }

        public String bucket() {
            return bucket;
        }

        public String objectKey() {
            return objectKey;
        }

        public PostPolicyConditions userConditions() {
            return userConditions;
        }

        public java.util.Map<String, String> userFields() {
            return userFields;
        }

        public String sessionToken() {
            return sessionToken;
        }

        public static final class Builder {
            private AwsCredentialsIdentity credentials;
            private String region;
            private java.time.Instant signingInstant;
            private java.time.Instant policyExpiration;
            private String bucket;
            private String objectKey;
            private PostPolicyConditions userConditions;
            private final java.util.Map<String, String> userFields = new java.util.LinkedHashMap<>();
            private String sessionToken;

            private Builder() {
            }

            public Builder credentials(AwsCredentialsIdentity credentials) {
                this.credentials = credentials;
                return this;
            }

            public Builder region(String region) {
                this.region = region;
                return this;
            }

            public Builder signingInstant(java.time.Instant signingInstant) {
                this.signingInstant = signingInstant;
                return this;
            }

            public Builder policyExpiration(java.time.Instant policyExpiration) {
                this.policyExpiration = policyExpiration;
                return this;
            }

            public Builder bucket(String bucket) {
                this.bucket = bucket;
                return this;
            }

            public Builder objectKey(String objectKey) {
                this.objectKey = objectKey;
                return this;
            }

            public Builder userConditions(PostPolicyConditions userConditions) {
                this.userConditions = userConditions;
                return this;
            }

            public Builder putUserField(String name, String value) {
                Objects.requireNonNull(name, "name");
                Objects.requireNonNull(value, "value");
                userFields.put(name, value);
                return this;
            }

            public Builder userFields(java.util.Map<String, String> userFields) {
                this.userFields.clear();
                if (userFields != null) {
                    this.userFields.putAll(userFields);
                }
                return this;
            }

            public Builder sessionToken(String sessionToken) {
                this.sessionToken = sessionToken;
                return this;
            }

            public SignInput build() {
                return new SignInput(this);
            }
        }
    }

    /**
     * Signed POST policy outputs.
     */
    public static final class SignedPostPolicy {
        private final String policyJson;
        private final String base64Policy;
        private final String hexSignature;
        private final String xAmzCredential;
        private final String xAmzDate;
        private final String sessionToken;

        private SignedPostPolicy(String policyJson,
                                 String base64Policy,
                                 String hexSignature,
                                 String xAmzCredential,
                                 String xAmzDate,
                                 String sessionToken) {
            this.policyJson = policyJson;
            this.base64Policy = base64Policy;
            this.hexSignature = hexSignature;
            this.xAmzCredential = xAmzCredential;
            this.xAmzDate = xAmzDate;
            this.sessionToken = sessionToken;
        }

        public String policyJson() {
            return policyJson;
        }

        public String base64Policy() {
            return base64Policy;
        }

        public String hexSignature() {
            return hexSignature;
        }

        public String xAmzCredential() {
            return xAmzCredential;
        }

        public String xAmzDate() {
            return xAmzDate;
        }

        public String sessionToken() {
            return sessionToken;
        }
    }
}
