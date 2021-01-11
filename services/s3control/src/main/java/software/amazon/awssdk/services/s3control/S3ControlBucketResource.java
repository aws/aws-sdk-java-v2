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

package software.amazon.awssdk.services.s3control;

import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.s3.internal.resource.S3OutpostResource;
import software.amazon.awssdk.services.s3.internal.resource.S3Resource;
import software.amazon.awssdk.services.s3.internal.resource.S3ResourceType;
import software.amazon.awssdk.services.s3control.internal.S3ControlResourceType;
import software.amazon.awssdk.utils.Validate;

/**
 * An {@link S3Resource} that represents an bucket.
 */
@SdkInternalApi
public final class S3ControlBucketResource implements S3Resource {

    private final String partition;
    private final String region;
    private final String accountId;
    private final String bucketName;
    private final S3Resource parentS3Resource;

    private S3ControlBucketResource(Builder b) {
        this.bucketName = Validate.notBlank(b.bucketName, "bucketName");
        if (b.parentS3Resource == null) {
            this.parentS3Resource = null;
            this.partition = b.partition;
            this.region = b.region;
            this.accountId = b.accountId;
        } else {
            this.parentS3Resource = validateParentS3Resource(b.parentS3Resource);
            Validate.isNull(b.partition, "partition cannot be set on builder if it has parent resource");
            Validate.isNull(b.region, "region cannot be set on builder if it has parent resource");
            Validate.isNull(b.accountId, "accountId cannot be set on builder if it has parent resource");
            this.partition = parentS3Resource.partition().orElse(null);
            this.region = parentS3Resource.region().orElse(null);
            this.accountId = parentS3Resource.accountId().orElse(null);
        }
    }

    /**
     * Get a new builder for this class.
     * @return A newly initialized instance of a builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets the resource type for this bucket.
     * @return This will always return "bucket_name".
     */
    @Override
    public String type() {
        return S3ControlResourceType.BUCKET.toString();
    }

    /**
     * Gets the AWS partition name associated with this bucket (e.g.: 'aws').
     * @return the name of the partition.
     */
    @Override
    public Optional<String> partition() {
        return Optional.of(this.partition);
    }

    /**
     * Gets the AWS region name associated with this bucket (e.g.: 'us-east-1').
     * @return the name of the region or null if the region has not been specified (e.g. the resource is in the
     * global namespace).
     */
    @Override
    public Optional<String> region() {
        return Optional.of(this.region);
    }

    /**
     * Gets the AWS account ID associated with this bucket.
     * @return the AWS account ID or null if the account ID has not been specified.
     */
    @Override
    public Optional<String> accountId() {
        return Optional.of(this.accountId);
    }

    /**
     * Gets the name of the bucket.
     * @return the name of the bucket.
     */
    public String bucketName() {
        return this.bucketName;
    }

    /**
     * Gets the optional parent s3 resource
     * @return the parent s3 resource if exists, otherwise null
     */
    @Override
    public Optional<S3Resource> parentS3Resource() {
        return Optional.ofNullable(parentS3Resource);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        S3ControlBucketResource that = (S3ControlBucketResource) o;

        if (!Objects.equals(partition, that.partition)) {
            return false;
        }
        if (!Objects.equals(region, that.region)) {
            return false;
        }
        if (!Objects.equals(accountId, that.accountId)) {
            return false;
        }
        if (!bucketName.equals(that.bucketName)) {
            return false;
        }
        return Objects.equals(parentS3Resource, that.parentS3Resource);
    }

    @Override
    public int hashCode() {
        int result = partition != null ? partition.hashCode() : 0;
        result = 31 * result + (region != null ? region.hashCode() : 0);
        result = 31 * result + (accountId != null ? accountId.hashCode() : 0);
        result = 31 * result + bucketName.hashCode();
        result = 31 * result + (parentS3Resource != null ? parentS3Resource.hashCode() : 0);
        return result;
    }

    /**
     * A builder for {@link S3ControlBucketResource} objects.
     */
    public static final class Builder {
        private String partition;
        private String region;
        private String accountId;
        private String bucketName;
        private S3Resource parentS3Resource;

        private Builder() {
        }

        /**
         * The AWS partition associated with the bucket.
         */
        public Builder partition(String partition) {
            this.partition = partition;
            return this;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        /**
         * The AWS region associated with the bucket. This property is optional.
         */
        public Builder region(String region) {
            this.region = region;
            return this;
        }

        /**
         * The AWS account ID associated with the bucket. This property is optional.
         */
        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        /**
         * The name of the S3 bucket.
         */
        public Builder bucketName(String bucketName) {
            this.bucketName = bucketName;
            return this;
        }

        /**
         * The S3 resource this access point is associated with (contained within). Only {@link S3OutpostResource} and
         * is a valid parent resource types.
         */
        public Builder parentS3Resource(S3Resource parentS3Resource) {
            this.parentS3Resource = parentS3Resource;
            return this;
        }

        /**
         * Builds an instance of {@link S3ControlBucketResource}.
         */
        public S3ControlBucketResource build() {
            return new S3ControlBucketResource(this);
        }
    }

    private S3Resource validateParentS3Resource(S3Resource parentS3Resource) {
        if (!S3ResourceType.OUTPOST.toString().equals(parentS3Resource.type())) {
            throw new IllegalArgumentException("Invalid 'parentS3Resource' type. An S3 bucket resource must be " +
                                               "associated with an outpost parent resource.");
        }
        return parentS3Resource;
    }
}
