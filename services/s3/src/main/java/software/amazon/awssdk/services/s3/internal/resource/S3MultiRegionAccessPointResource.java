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
import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.services.s3.internal.signing.S3SigningUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * An {@link S3Resource} that represents an S3 global accesspoint (multi-region or MRAP) resource
 */
@SdkInternalApi
public final class S3MultiRegionAccessPointResource implements S3Resource {

    private static final String REGION_VALUE = "global";

    private final String partition;
    private final String region;
    private final String accountId;

    private S3MultiRegionAccessPointResource(Builder b) {
        this.partition = Validate.paramNotBlank(b.partition, "partition");
        this.region = Validate.paramNotBlank(b.region, "region");
        this.accountId = Validate.paramNotBlank(b.accountId, "accountId");
    }

    /**
     * Get a new builder for this class.
     * @return A newly initialized instance of a builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static boolean isMultiRegion(Arn arn) {
        return REGION_VALUE.equals(arn.region().orElse(null));
    }

    @Override
    public Optional<Signer> overrideSigner() {
        return Optional.of(S3SigningUtils.getSigV4aSigner());
    }

    /**
     * Gets the resource type for this access point.
     * @return This will always return {@link S3ResourceType#MULTI_REGION}.
     */
    @Override
    public String type() {
        return S3ResourceType.MULTI_REGION.toString();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        S3MultiRegionAccessPointResource that = (S3MultiRegionAccessPointResource) o;

        if (partition != null ? !partition.equals(that.partition) : that.partition != null) {
            return false;
        }
        if (region != null ? !region.equals(that.region) : that.region != null) {
            return false;
        }
        if (accountId != null ? !accountId.equals(that.accountId) : that.accountId != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = partition != null ? partition.hashCode() : 0;
        result = 31 * result + (region != null ? region.hashCode() : 0);
        result = 31 * result + (accountId != null ? accountId.hashCode() : 0);
        return result;
    }

    /**
     * A builder for {@link S3MultiRegionAccessPointResource} objects.
     */
    public static final class Builder {
        private String partition;
        private String region;
        private String accountId;

        private Builder() {
        }

        /**
         * The AWS partition associated with the access point.
         */
        public Builder partition(String partition) {
            this.partition = partition;
            return this;
        }

        /**
         * The AWS region associated with the access point.
         */
        public Builder region(String region) {
            this.region = region;
            return this;
        }

        /**
         * The AWS account ID associated with the access point.
         */
        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        /**
         * Builds an instance of {@link S3MultiRegionAccessPointResource}.
         */
        public S3MultiRegionAccessPointResource build() {
            return new S3MultiRegionAccessPointResource(this);
        }
    }
}
