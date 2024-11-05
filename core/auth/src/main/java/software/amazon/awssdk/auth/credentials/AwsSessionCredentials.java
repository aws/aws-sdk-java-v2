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

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Temporary AWS credentials, used for accessing AWS services. This interface has been superseded by
 * {@link AwsSessionCredentialsIdentity}.
 *
 * <p>
 * To avoid unnecessary churn this class has not been marked as deprecated, but it's recommended to use
 * {@link AwsSessionCredentialsIdentity} when defining generic credential providers because it provides the same functionality
 * with considerably fewer dependencies.
 */
@Immutable
@SdkPublicApi
public final class AwsSessionCredentials implements AwsCredentials, AwsSessionCredentialsIdentity,
                                                    ToCopyableBuilder<AwsSessionCredentials.Builder, AwsSessionCredentials> {
    private final String accessKeyId;
    private final String secretAccessKey;
    private final String sessionToken;

    private final String accountId;
    private final Instant expirationTime;
    private final String providerName;

    private AwsSessionCredentials(Builder builder) {
        this.accessKeyId = Validate.paramNotNull(builder.accessKeyId, "accessKey");
        this.secretAccessKey = Validate.paramNotNull(builder.secretAccessKey, "secretKey");
        this.sessionToken = Validate.paramNotNull(builder.sessionToken, "sessionToken");
        this.accountId = builder.accountId;
        this.expirationTime = builder.expirationTime;
        this.providerName = builder.providerName;
    }

    /**
     * Returns a builder for this object.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Constructs a new session credentials object, with the specified AWS access key, AWS secret key and AWS session token.
     *
     * @param accessKey The AWS access key, used to identify the user interacting with AWS.
     * @param secretKey The AWS secret access key, used to authenticate the user interacting with AWS.
     * @param sessionToken The AWS session token, retrieved from an AWS token service, used for authenticating that this user has
     * received temporary permission to access some resource.
     */
    public static AwsSessionCredentials create(String accessKey, String secretKey, String sessionToken) {
        return builder().accessKeyId(accessKey).secretAccessKey(secretKey).sessionToken(sessionToken).build();
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
     * (Optional) The time after which this identity is no longer valid. When not specified, the identity may
     * still expire at some unknown time in the future.
     */
    @Override
    public Optional<Instant> expirationTime() {
        return Optional.ofNullable(expirationTime);
    }

    /**
     * The AWS session token, used to authenticate that this user has received temporary permission to access some
     * resource.
     */
    @Override
    public String sessionToken() {
        return sessionToken;
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
        return ToString.builder("AwsSessionCredentials")
                       .add("accessKeyId", accessKeyId())
                       .add("accountId", accountId)
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

        AwsSessionCredentials that = (AwsSessionCredentials) o;
        return Objects.equals(accessKeyId, that.accessKeyId) &&
               Objects.equals(secretAccessKey, that.secretAccessKey) &&
               Objects.equals(sessionToken, that.sessionToken) &&
               Objects.equals(accountId, that.accountId().orElse(null)) &&
               Objects.equals(expirationTime, that.expirationTime().orElse(null));
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + Objects.hashCode(accessKeyId());
        hashCode = 31 * hashCode + Objects.hashCode(secretAccessKey());
        hashCode = 31 * hashCode + Objects.hashCode(sessionToken());
        hashCode = 31 * hashCode + Objects.hashCode(accountId);
        hashCode = 31 * hashCode + Objects.hashCode(expirationTime);
        return hashCode;
    }

    @Override
    public Builder toBuilder() {
        return builder().accessKeyId(accessKeyId)
                        .secretAccessKey(secretAccessKey)
                        .sessionToken(sessionToken)
                        .accountId(accountId)
                        .expirationTime(expirationTime)
                        .providerName(providerName);
    }

    @Override
    public AwsSessionCredentials copy(Consumer<? super Builder> modifier) {
        return ToCopyableBuilder.super.copy(modifier);
    }

    /**
     * A builder for creating an instance of {@link AwsSessionCredentials}. This can be created with the static
     * {@link #builder()} method.
     */
    public static final class Builder implements CopyableBuilder<AwsSessionCredentials.Builder, AwsSessionCredentials> {
        private String accessKeyId;
        private String secretAccessKey;
        private String sessionToken;
        private String accountId;
        private Instant expirationTime;
        private String providerName;

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
         * The AWS session token, used to authenticate that this user has received temporary permission to access some
         * resource.
         */
        public Builder sessionToken(String sessionToken) {
            this.sessionToken = sessionToken;
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
         * (Optional) The time after which this identity is no longer valid. When not specified, the identity may
         * still expire at some unknown time in the future.
         */
        public Builder expirationTime(Instant expirationTime) {
            this.expirationTime = expirationTime;
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

        public AwsSessionCredentials build() {
            return new AwsSessionCredentials(this);
        }
    }
}
