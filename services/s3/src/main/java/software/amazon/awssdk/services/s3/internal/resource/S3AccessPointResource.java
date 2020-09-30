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
 * An {@link S3Resource} that represents an S3 access point.
 */
@SdkInternalApi
public final class S3AccessPointResource
    implements S3Resource, ToCopyableBuilder<S3AccessPointResource.Builder, S3AccessPointResource> {

    private static final S3ResourceType S3_RESOURCE_TYPE = S3ResourceType.ACCESS_POINT;

    private final String partition;
    private final String region;
    private final String accountId;
    private final String accessPointName;
    private final S3Resource parentS3Resource;

    private S3AccessPointResource(Builder b) {
        this.accessPointName = Validate.paramNotBlank(b.accessPointName, "accessPointName");
        if (b.parentS3Resource == null) {
            this.parentS3Resource = null;
            this.partition = Validate.paramNotBlank(b.partition, "partition");
            this.region = Validate.paramNotBlank(b.region, "region");
            this.accountId = Validate.paramNotBlank(b.accountId, "accountId");
        } else {
            this.parentS3Resource = validateParentS3Resource(b.parentS3Resource);
            Validate.isTrue(b.partition == null, "partition cannot be set on builder if it has parent resource");
            Validate.isTrue(b.region == null, "region cannot be set on builder if it has parent resource");
            Validate.isTrue(b.accountId == null, "accountId cannot be set on builder if it has parent resource");
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
     * Gets the resource type for this access point.
     * @return This will always return "access_point".
     */
    @Override
    public String type() {
        return S3_RESOURCE_TYPE.toString();
    }

    @Override
    public Optional<S3Resource> parentS3Resource() {
        return Optional.ofNullable(parentS3Resource);
    }

    /**
     * Gets the AWS partition name associated with this access point (e.g.: 'aws').
     * @return the name of the partition.
     */
    @Override
    public Optional<String> partition() {
        return Optional.ofNullable(this.partition);
    }

    /**
     * Gets the AWS region name associated with this bucket (e.g.: 'us-east-1').
     * @return the name of the region.
     */
    @Override
    public Optional<String> region() {
        return Optional.ofNullable(this.region);
    }

    /**
     * Gets the AWS account ID associated with this bucket.
     * @return the AWS account ID.
     */
    @Override
    public Optional<String> accountId() {
        return Optional.ofNullable(this.accountId);
    }

    /**
     * Gets the name of the access point.
     * @return the name of the access point.
     */
    public String accessPointName() {
        return this.accessPointName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        S3AccessPointResource that = (S3AccessPointResource) o;

        if (partition != null ? ! partition.equals(that.partition) : that.partition != null) {
            return false;
        }
        if (region != null ? ! region.equals(that.region) : that.region != null) {
            return false;
        }
        if (accountId != null ? ! accountId.equals(that.accountId) : that.accountId != null) {
            return false;
        }

        if (parentS3Resource != null ? ! parentS3Resource.equals(that.parentS3Resource) : that.parentS3Resource != null) {
            return false;
        }
        return accessPointName.equals(that.accessPointName);
    }

    @Override
    public int hashCode() {
        int result = partition != null ? partition.hashCode() : 0;
        result = 31 * result + (region != null ? region.hashCode() : 0);
        result = 31 * result + (accountId != null ? accountId.hashCode() : 0);
        result = 31 * result + accessPointName.hashCode();
        result = 31 * result + (parentS3Resource != null ? parentS3Resource.hashCode() : 0);
        return result;
    }

    @Override
    public Builder toBuilder() {
        return builder()
            .partition(partition)
            .region(region)
            .accountId(accountId)
            .accessPointName(accessPointName);
    }

    private S3Resource validateParentS3Resource(S3Resource parentS3Resource) {
        if (!S3ResourceType.OUTPOST.toString().equals(parentS3Resource.type())) {
            throw new IllegalArgumentException("Invalid 'parentS3Resource' type. An S3 access point resource must be " +
                                               "associated with an outpost parent resource.");
        }
        return parentS3Resource;
    }

    /**
     * A builder for {@link S3AccessPointResource} objects.
     */
    public static final class Builder implements CopyableBuilder<Builder, S3AccessPointResource> {
        private String partition;
        private String region;
        private String accountId;
        private String accessPointName;
        private S3Resource parentS3Resource;

        private Builder() {
        }

        public void setPartition(String partition) {
            partition(partition);
        }

        /**
         * The AWS partition associated with the access point.
         */
        public Builder partition(String partition) {
            this.partition = partition;
            return this;
        }

        public void setRegion(String region) {
            region(region);
        }

        /**
         * The AWS region associated with the access point.
         */
        public Builder region(String region) {
            this.region = region;
            return this;
        }

        public void setAccountId(String accountId) {
            accountId(accountId);
        }

        /**
         * The AWS account ID associated with the access point.
         */
        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public void setAccessPointName(String accessPointName) {
            accessPointName(accessPointName);
        }

        /**
         * The name of the S3 access point.
         */
        public Builder accessPointName(String accessPointName) {
            this.accessPointName = accessPointName;
            return this;
        }

        /**
         * The S3 resource this access point is associated with (contained within). Only {@link S3OutpostResource}
         * is a valid parent resource types.
         */
        public Builder parentS3Resource(S3Resource parentS3Resource) {
            this.parentS3Resource = parentS3Resource;
            return this;
        }

        /**
         * Builds an instance of {@link S3AccessPointResource}.
         */
        @Override
        public S3AccessPointResource build() {
            return new S3AccessPointResource(this);
        }
    }
}
