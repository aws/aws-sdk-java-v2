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
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class DefaultAwsCredentialsIdentity implements AwsCredentialsIdentity {

    private final String accessKeyId;
    private final String secretAccessKey;

    private DefaultAwsCredentialsIdentity(Builder builder) {
        this.accessKeyId = builder.accessKeyId;
        this.secretAccessKey = builder.secretAccessKey;

        Validate.paramNotNull(accessKeyId, "accessKeyId");
        Validate.paramNotNull(secretAccessKey, "secretAccessKey");
    }

    public static AwsCredentialsIdentity.Builder builder() {
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
    public String toString() {
        return ToString.builder("AwsCredentialsIdentity")
                       .add("accessKeyId", accessKeyId)
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
        AwsCredentialsIdentity that = (AwsCredentialsIdentity) o;
        return Objects.equals(accessKeyId, that.accessKeyId()) &&
               Objects.equals(secretAccessKey, that.secretAccessKey());
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + Objects.hashCode(accessKeyId);
        hashCode = 31 * hashCode + Objects.hashCode(secretAccessKey);
        return hashCode;
    }

    private static final class Builder implements AwsCredentialsIdentity.Builder {
        private String accessKeyId;
        private String secretAccessKey;

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
        public AwsCredentialsIdentity build() {
            return new DefaultAwsCredentialsIdentity(this);
        }
    }
}
