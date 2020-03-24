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

package software.amazon.awssdk.services.s3.internal.resource;

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * An {@link S3Resource} that represents an S3 object.
 */
@SdkInternalApi
public final class S3ObjectResource
    implements S3Resource, ToCopyableBuilder<S3ObjectResource.Builder, S3ObjectResource> {

    private static final S3ResourceType S3_RESOURCE_TYPE = S3ResourceType.OBJECT;

    private final String partition;
    private final String region;
    private final String accountId;
    private final String bucketName;
    private final String key;

    private S3ObjectResource(Builder b) {
        this.bucketName = Validate.paramNotBlank(b.bucketName, "bucketName");
        this.key = Validate.paramNotBlank(b.key, "key");
        this.partition = Validate.paramNotBlank(b.partition, "partition");
        this.region = b.region;
        this.accountId = b.accountId;
    }

    /**
     * Get a new builder for this class.
     * @return A newly initialized instance of a builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets the resource type for this S3 object.
     * @return This will always return "object".
     */
    @Override
    public String type() {
        return S3_RESOURCE_TYPE.toString();
    }

    /**
     * Gets the AWS partition name associated with the S3 object (e.g.: 'aws').
     * @return the name of the partition.
     */
    @Override
    public Optional<String> partition() {
        return Optional.ofNullable(this.partition);
    }

    /**
     * Gets the AWS region name associated with the S3 object (e.g.: 'us-east-1').
     * @return the name of the region or null if the region has not been specified (e.g. the resource is in the
     * global namespace).
     */
    @Override
    public Optional<String> region() {
        return Optional.ofNullable(this.region);
    }

    /**
     * Gets the AWS account ID associated with the S3 object if it has been specified.
     * @return the optional AWS account ID or empty if the account ID has not been specified.
     */
    @Override
    public Optional<String> accountId() {
        return Optional.ofNullable(this.accountId);
    }

    /**
     * Gets the name of the bucket associated with the S3 object.
     * @return the name of the bucket associated with the S3 object.
     */
    public String bucketName() {
        return this.bucketName;
    }

    /**
     * Gets the key of the S3 object.
     * @return the key of the S3 object.
     */
    public String key() {
        return this.key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        S3ObjectResource that = (S3ObjectResource) o;

        if (partition != null ? ! partition.equals(that.partition) : that.partition != null) {
            return false;
        }
        if (region != null ? ! region.equals(that.region) : that.region != null) {
            return false;
        }
        if (accountId != null ? ! accountId.equals(that.accountId) : that.accountId != null) {
            return false;
        }
        if (! bucketName.equals(that.bucketName)) {
            return false;
        }
        return key.equals(that.key);
    }

    @Override
    public int hashCode() {
        int result = partition != null ? partition.hashCode() : 0;
        result = 31 * result + (region != null ? region.hashCode() : 0);
        result = 31 * result + (accountId != null ? accountId.hashCode() : 0);
        result = 31 * result + bucketName.hashCode();
        result = 31 * result + key.hashCode();
        return result;
    }

    @Override
    public Builder toBuilder() {
        return builder()
            .partition(partition)
            .region(region)
            .accountId(accountId)
            .bucketName(bucketName)
            .key(key);
    }

    /**
     * A builder for {@link S3ObjectResource} objects.
     */
    public static final class Builder implements CopyableBuilder<Builder, S3ObjectResource> {
        private String partition;
        private String region;
        private String accountId;
        private String bucketName;
        private String key;

        private Builder() {
        }

        public void setPartition(String partition) {
            partition(partition);
        }

        /**
         * The AWS partition associated with the S3 object.
         */
        public Builder partition(String partition) {
            this.partition = partition;
            return this;
        }

        public void setRegion(String region) {
            region(region);
        }

        /**
         * The AWS region associated with the S3 object. This property is optional.
         */
        public Builder region(String region) {
            this.region = region;
            return this;
        }

        public void setAccountId(String accountId) {
            accountId(accountId);
        }

        /**
         * The AWS account ID associated with the S3 object. This property is optional.
         */
        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public void setBucketName(String bucketName) {
            bucketName(bucketName);
        }

        /**
         * The name of the S3 bucket associated with this S3 object.
         */
        public Builder bucketName(String bucketName) {
            this.bucketName = bucketName;
            return this;
        }

        public void setKey(String key) {
            key(key);
        }

        /**
         * The key of the S3 object.
         */
        public Builder key(String key) {
            this.key = key;
            return this;
        }

        /**
         * Builds an instance of {@link S3BucketResource}.
         */
        @Override
        public S3ObjectResource build() {
            return new S3ObjectResource(this);
        }
    }
}
