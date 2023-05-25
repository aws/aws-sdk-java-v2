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

package software.amazon.awssdk.identity.spi.internal;

import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class DefaultAwsSessionCredentialsIdentity implements AwsSessionCredentialsIdentity {

    private final String accessKeyId;
    private final String secretAccessKey;
    private final String sessionToken;
    private final String accountId;


    private DefaultAwsSessionCredentialsIdentity(Builder builder) {
        this.accessKeyId = Validate.paramNotNull(builder.accessKeyId, "accessKeyId");
        this.secretAccessKey = Validate.paramNotNull(builder.secretAccessKey, "secretAccessKey");
        this.sessionToken = Validate.paramNotNull(builder.sessionToken, "sessionToken");
        this.accountId = builder.accountId;
    }

    public static AwsSessionCredentialsIdentity.Builder builder() {
        return new Builder();
    }

    @Override
    public String accessKeyId() {
        return accessKeyId;
    }

    @Override
    public String secretAccessKey() {
        return secretAccessKey;
    }

    @Override
    public Optional<String> accountId() {
        return Optional.ofNullable(accountId);
    }

    @Override
    public String sessionToken() {
        return sessionToken;
    }

    @Override
    public String toString() {
        return ToString.builder("AwsSessionCredentialsIdentity")
                       .add("accessKeyId", accessKeyId)
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
        AwsSessionCredentialsIdentity that = (AwsSessionCredentialsIdentity) o;
        return Objects.equals(accessKeyId, that.accessKeyId()) &&
               Objects.equals(secretAccessKey, that.secretAccessKey()) &&
               Objects.equals(sessionToken, that.sessionToken()) &&
               Objects.equals(accountId, that.accountId().orElse(null));
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + Objects.hashCode(accessKeyId);
        hashCode = 31 * hashCode + Objects.hashCode(secretAccessKey);
        hashCode = 31 * hashCode + Objects.hashCode(sessionToken);
        hashCode = 31 * hashCode + Objects.hashCode(accountId);
        return hashCode;
    }

    private static final class Builder implements AwsSessionCredentialsIdentity.Builder {
        private String accessKeyId;
        private String secretAccessKey;
        private String sessionToken;
        private String accountId;

        private Builder() {
        }

        @Override
        public Builder accessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
            return this;
        }

        @Override
        public Builder secretAccessKey(String secretAccessKey) {
            this.secretAccessKey = secretAccessKey;
            return this;
        }

        @Override
        public Builder sessionToken(String sessionToken) {
            this.sessionToken = sessionToken;
            return this;
        }


        @Override
        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        @Override
        public AwsSessionCredentialsIdentity build() {
            return new DefaultAwsSessionCredentialsIdentity(this);
        }
    }
}
