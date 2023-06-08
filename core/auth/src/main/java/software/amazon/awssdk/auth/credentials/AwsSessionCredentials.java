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
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * A special type of {@link AwsCredentials} that provides a session token to be used in service authentication. Session
 * tokens are typically provided by a token broker service, like AWS Security Token Service, and provide temporary access to an
 * AWS service.
 */
@Immutable
@SdkPublicApi
public final class AwsSessionCredentials implements AwsCredentials {

    private final String accessKeyId;
    private final String secretAccessKey;
    private final String sessionToken;

    private final Instant expirationTime;

    private AwsSessionCredentials(Builder builder) {
        this.accessKeyId = Validate.paramNotNull(builder.accessKeyId, "accessKey");
        this.secretAccessKey = Validate.paramNotNull(builder.secretAccessKey, "secretKey");
        this.sessionToken = Validate.paramNotNull(builder.sessionToken, "sessionToken");
        this.expirationTime = builder.expirationTime;
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
     * Retrieve the expiration time of these credentials, if it exists.
     */
    public Optional<Instant> expirationTime() {
        return Optional.ofNullable(expirationTime);
    }

    /**
     * Retrieve the AWS session token. This token is retrieved from an AWS token service, and is used for authenticating that this
     * user has received temporary permission to access some resource.
     */
    public String sessionToken() {
        return sessionToken;
    }

    @Override
    public String toString() {
        return ToString.builder("AwsSessionCredentials")
                       .add("accessKeyId", accessKeyId())
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
               Objects.equals(expirationTime, that.expirationTime().orElse(null));
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + Objects.hashCode(accessKeyId());
        hashCode = 31 * hashCode + Objects.hashCode(secretAccessKey());
        hashCode = 31 * hashCode + Objects.hashCode(sessionToken());
        hashCode = 31 * hashCode + Objects.hashCode(expirationTime);
        return hashCode;
    }

    /**
     * A builder for creating an instance of {@link AwsSessionCredentials}. This can be created with the static
     * {@link #builder()} method.
     */
    public static final class Builder {
        private String accessKeyId;
        private String secretAccessKey;
        private String sessionToken;
        private Instant expirationTime;

        /**
         * The AWS access key, used to identify the user interacting with services. Required.
         */
        public Builder accessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
            return this;
        }

        /**
         * The AWS secret access key, used to authenticate the user interacting with services. Required
         */
        public Builder secretAccessKey(String secretAccessKey) {
            this.secretAccessKey = secretAccessKey;
            return this;
        }

        /**
         * The AWS session token, retrieved from an AWS token service, used for authenticating that this user has
         * received temporary permission to access some resource. Required
         */
        public Builder sessionToken(String sessionToken) {
            this.sessionToken = sessionToken;
            return this;
        }

        /**
         * The time after which this identity will no longer be valid. If this is empty,
         * an expiration time is not known (but the identity may still expire at some
         * time in the future).
         */
        public Builder expirationTime(Instant expirationTime) {
            this.expirationTime = expirationTime;
            return this;
        }

        public AwsSessionCredentials build() {
            return new AwsSessionCredentials(this);
        }
    }
}
