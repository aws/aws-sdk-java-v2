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
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Long-term AWS credentials, without a session token. This class has been superseded by {@link AwsCredentialsIdentity}.
 *
 * <p>
 * To avoid unnecessary churn this class has not been marked as deprecated, but it's recommended to use
 * {@link AwsCredentialsIdentity} when defining generic credential providers because it provides the same functionality with
 * considerably fewer dependencies.
 */
@Immutable
@SdkPublicApi
public final class AwsBasicCredentials implements AwsCredentials,
                                                  ToCopyableBuilder<AwsBasicCredentials.Builder, AwsBasicCredentials> {
    private final String accessKeyId;
    private final String secretAccessKey;
    private final boolean validateCredentials;
    private final String providerName;
    private final String accountId;

    private AwsBasicCredentials(Builder builder) {
        this.accessKeyId = trimToNull(builder.accessKeyId);
        this.secretAccessKey = trimToNull(builder.secretAccessKey);
        this.validateCredentials = builder.validateCredentials;
        this.providerName = builder.providerName;
        this.accountId = builder.accountId;

        if (builder.validateCredentials) {
            Validate.notNull(this.accessKeyId, "Access key ID cannot be blank.");
            Validate.notNull(this.secretAccessKey, "Secret access key cannot be blank.");
        }
    }

    /**
     * Create a builder for AWS credentials.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Constructs a new credentials object, with the specified AWS access key and AWS secret key. To specify your AWS account
     * ID, use {@link #builder()}.
     *
     * @param accessKeyId The AWS access key, used to identify the user interacting with the service.
     * @param secretAccessKey The AWS secret access key, used to authenticate the user interacting with the service.
     */
    public static AwsBasicCredentials create(String accessKeyId, String secretAccessKey) {
        return builder().accessKeyId(accessKeyId)
                        .secretAccessKey(secretAccessKey)
                        .build();
    }

    /**
     * The AWS access key, used to identify the user interacting with services.
     */
    @Override
    public String accessKeyId() {
        return accessKeyId;
    }

    /**
     * The AWS secret access key, used to authenticate the user interacting with services.
     */
    @Override
    public String secretAccessKey() {
        return secretAccessKey;
    }

    /**
     * (Optional) The name of the identity provider that created this credential identity. This value should only be
     * specified by standard providers. If you're creating your own identity or provider, you should not configure this
     * value.
     */
    @Override
    public Optional<String> providerName() {
        return Optional.ofNullable(providerName);
    }

    /**
     * (Optional) The AWS account id associated with this credential identity. Specifying this value may improve performance
     * or availability for some services.
     */
    @Override
    public Optional<String> accountId() {
        return Optional.ofNullable(accountId);
    }

    @Override
    public String toString() {
        return ToString.builder("AwsCredentials")
                       .add("accessKeyId", accessKeyId)
                       .add("providerName", providerName)
                       .add("accountId", accountId)
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
               Objects.equals(secretAccessKey, that.secretAccessKey) &&
               Objects.equals(accountId, that.accountId().orElse(null));
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + Objects.hashCode(accessKeyId());
        hashCode = 31 * hashCode + Objects.hashCode(secretAccessKey());
        hashCode = 31 * hashCode + Objects.hashCode(accountId);
        return hashCode;
    }

    @Override
    public Builder toBuilder() {
        return builder().accessKeyId(accessKeyId)
                        .secretAccessKey(secretAccessKey)
                        .accountId(accountId)
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
        private String accountId;
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
         * (Optional) The AWS account id associated with this credential identity. Specifying this value may improve performance
         * or availability for some services.
         */
        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        /**
         * (Optional) The name of the identity provider that created this credential identity. This value should only be
         * specified by standard providers. If you're creating your own identity or provider, you should not configure this
         * value.
         */
        public Builder providerName(String providerName) {
            this.providerName = providerName;
            return this;
        }

        /**
         * Whether this class should verify that accessKeyId and secretAccessKey are not null.
         * Used internally by the SDK to create "anonymous" AWS credentials.
         */
        @SdkInternalApi
        Builder validateCredentials(Boolean validateCredentials) {
            this.validateCredentials = validateCredentials;
            return this;
        }

        public AwsBasicCredentials build() {
            return new AwsBasicCredentials(this);
        }
    }
}
