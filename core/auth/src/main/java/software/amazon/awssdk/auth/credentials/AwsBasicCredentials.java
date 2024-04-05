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

package software.amazon.awssdk.auth.credentials;

import static software.amazon.awssdk.utils.StringUtils.trimToNull;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Provides access to the AWS credentials used for accessing services: AWS access key ID and secret access key. These
 * credentials are used to securely sign requests to services (e.g., AWS services) that use them for authentication.
 *
 * <p>For more details on AWS access keys, see:
 * <a href="https://docs.aws.amazon.com/general/latest/gr/aws-sec-cred-types.html#access-keys-and-secret-access-keys">
 * https://docs.aws.amazon.com/general/latest/gr/aws-sec-cred-types.html#access-keys-and-secret-access-keys</a></p>
 *
 * @see AwsCredentialsProvider
 */
@Immutable
@SdkPublicApi
public final class AwsBasicCredentials implements AwsCredentials,
                                                  ToCopyableBuilder<AwsBasicCredentials.Builder, AwsBasicCredentials> {
    /**
     * A set of AWS credentials without an access key or secret access key, indicating that anonymous access should be used.
     */
    // TODO(sra-identity-and-auth): Check if this static member can be removed after cleanup
    @SdkInternalApi
    static final AwsBasicCredentials ANONYMOUS_CREDENTIALS = builder().validateCredentials(false).build();

    private final String accessKeyId;
    private final String secretAccessKey;
    private final boolean validateCredentials;
    private final String providerName;

    private AwsBasicCredentials(Builder builder) {
        this.accessKeyId = trimToNull(builder.accessKeyId);
        this.secretAccessKey = trimToNull(builder.secretAccessKey);
        this.validateCredentials = builder.validateCredentials;
        this.providerName = builder.providerName;

        if (builder.validateCredentials) {
            Validate.notNull(this.accessKeyId, "Access key ID cannot be blank.");
            Validate.notNull(this.secretAccessKey, "Secret access key cannot be blank.");
        }
    }

    /**
     * Constructs a new credentials object, with the specified AWS access key and AWS secret key.
     *
     * @param accessKeyId The AWS access key, used to identify the user interacting with AWS.
     * @param secretAccessKey The AWS secret access key, used to authenticate the user interacting with AWS.
     */
    protected AwsBasicCredentials(String accessKeyId, String secretAccessKey) {
        this.accessKeyId = trimToNull(accessKeyId);
        this.secretAccessKey = trimToNull(secretAccessKey);
        this.validateCredentials = false;
        this.providerName = null;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Constructs a new credentials object, with the specified AWS access key and AWS secret key.
     *
     * @param accessKeyId The AWS access key, used to identify the user interacting with AWS.
     * @param secretAccessKey The AWS secret access key, used to authenticate the user interacting with AWS.
     * */
    public static AwsBasicCredentials create(String accessKeyId, String secretAccessKey) {
        return builder().accessKeyId(accessKeyId)
                        .secretAccessKey(secretAccessKey)
                        .build();
    }

    /**
     * Retrieve the AWS access key, used to identify the user interacting with AWS.
     */
    @Override
    public String accessKeyId() {
        return accessKeyId;
    }

    /**
     * Retrieve the AWS secret access key, used to authenticate the user interacting with AWS.
     */
    @Override
    public String secretAccessKey() {
        return secretAccessKey;
    }

    /**
     * The name of the identity provider that created this credential identity.
     */
    @Override
    public Optional<String> providerName() {
        return Optional.ofNullable(providerName);
    }

    @Override
    public String toString() {
        return ToString.builder("AwsCredentials")
                       .add("accessKeyId", accessKeyId)
                       .add("providerName", providerName)
                       .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AwsBasicCredentials that = (AwsBasicCredentials) o;
        return Objects.equals(accessKeyId, that.accessKeyId) &&
               Objects.equals(secretAccessKey, that.secretAccessKey);
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + Objects.hashCode(accessKeyId());
        hashCode = 31 * hashCode + Objects.hashCode(secretAccessKey());
        return hashCode;
    }

    @Override
    public Builder toBuilder() {
        return builder().accessKeyId(accessKeyId)
                        .secretAccessKey(secretAccessKey)
                        .validateCredentials(validateCredentials)
                        .providerName(providerName);
    }

    @Override
    public AwsBasicCredentials copy(Consumer<? super AwsBasicCredentials.Builder> modifier) {
        return ToCopyableBuilder.super.copy(modifier);
    }

    /**
     * A builder for creating an instance of {@link AwsBasicCredentials}. This can be created with the static
     * {@link #builder()} method.
     */
    public static final class Builder implements CopyableBuilder<AwsBasicCredentials.Builder, AwsBasicCredentials> {
        private String accessKeyId;
        private String secretAccessKey;
        private String providerName;
        private boolean validateCredentials = true;

        private Builder() {
        }

        /**
         * The AWS access key, used to identify the user interacting with services.
         */
        public Builder accessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
            return this;
        }

        /**
         * The AWS secret access key, used to authenticate the user interacting with services.
         */
        public Builder secretAccessKey(String secretAccessKey) {
            this.secretAccessKey = secretAccessKey;
            return this;
        }

        /**
         * The name of the identity provider that created this credential identity.
         */
        public Builder providerName(String providerName) {
            this.providerName = providerName;
            return this;
        }

        /**
         * Whether this class should verify that accessKeyId and secretAccessKey are not null.
         * Used internally by the SDK for legacy reasons.
         */
        @SdkInternalApi
        public Builder validateCredentials(Boolean validateCredentials) {
            this.validateCredentials = validateCredentials;
            return this;
        }

        public AwsBasicCredentials build() {
            return new AwsBasicCredentials(this);
        }
    }
}
