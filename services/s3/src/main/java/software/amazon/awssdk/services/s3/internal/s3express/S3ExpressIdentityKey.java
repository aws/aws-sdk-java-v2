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

package software.amazon.awssdk.services.s3.internal.s3express;

import java.util.Objects;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * The true keys used in the identity cache are
 * - bucket
 * - identity (AwsCredentials)
 *
 */
@SdkInternalApi
public final class S3ExpressIdentityKey implements ToCopyableBuilder<S3ExpressIdentityKey.Builder, S3ExpressIdentityKey> {

    private final String bucket;
    private final SdkClient client;
    private final AwsCredentialsIdentity identity;

    public S3ExpressIdentityKey(Builder builder) {
        this.bucket = Validate.notNull(builder.bucket, "Bucket must not be null");
        this.client = Validate.notNull(builder.client, "Client must not be null");
        this.identity = Validate.notNull(builder.identity, "Identity must not be null");
    }

    public static Builder builder() {
        return new Builder();
    }

    public String bucket() {
        return bucket;
    }

    public SdkClient client() {
        return client;
    }

    public AwsCredentialsIdentity identity() {
        return identity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        S3ExpressIdentityKey that = (S3ExpressIdentityKey) o;

        return Objects.equals(bucket, that.bucket) &&
               Objects.equals(identity, that.identity);
    }

    @Override
    public int hashCode() {
        int result = bucket != null ? bucket.hashCode() : 0;
        result = 31 * result + (identity != null ? identity.hashCode() : 0);
        return result;
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    public static class Builder implements CopyableBuilder<Builder, S3ExpressIdentityKey> {
        String bucket;
        SdkClient client;
        AwsCredentialsIdentity identity;

        public Builder() {
        }

        public Builder(S3ExpressIdentityKey s3ExpressIdentityKey) {
            this.bucket = s3ExpressIdentityKey.bucket;
            this.client = s3ExpressIdentityKey.client;
            this.identity = s3ExpressIdentityKey.identity;
        }

        public Builder bucket(String bucket) {
            this.bucket = bucket;
            return this;
        }

        public Builder client(SdkClient client) {
            this.client = client;
            return this;
        }

        public Builder identity(AwsCredentialsIdentity identity) {
            this.identity = identity;
            return this;
        }

        @Override
        public S3ExpressIdentityKey build() {
            return new S3ExpressIdentityKey(this);
        }
    }
}
