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

package software.amazon.awssdk.enhanced.dynamodb.model;

import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.utils.Validate;

/**
 * Enhanced model representation of a 'global secondary index' of a DynamoDb table. This is optionally used with the
 * 'createTable' operation in the enhanced client.
 */
@SdkPublicApi
public final class EnhancedGlobalSecondaryIndex {
    private final String indexName;
    private final Projection projection;
    private final ProvisionedThroughput provisionedThroughput;

    private EnhancedGlobalSecondaryIndex(Builder builder) {
        this.indexName = Validate.paramNotBlank(builder.indexName, "indexName");
        this.projection = builder.projection;
        this.provisionedThroughput = builder.provisionedThroughput;
    }

    /**
     * Creates a newly initialized builder for an {@link EnhancedLocalSecondaryIndex}
     * @return A new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a builder initialized with the attributes of an existing {@link EnhancedLocalSecondaryIndex}
     * @return A new builder
     */
    public Builder toBuilder() {
        return builder().indexName(indexName)
                        .projection(projection)
                        .provisionedThroughput(provisionedThroughput);
    }

    /**
     * The name of the global secondary index
     */
    public String indexName() {
        return indexName;
    }

    /**
     * The attribute projection setting for this global secondary index.
     */
    public Projection projection() {
        return projection;
    }

    /**
     * The provisioned throughput setting for this global secondary index.
     */
    public ProvisionedThroughput provisionedThroughput() {
        return provisionedThroughput;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EnhancedGlobalSecondaryIndex that = (EnhancedGlobalSecondaryIndex) o;

        if (indexName != null ? ! indexName.equals(that.indexName) : that.indexName != null) {
            return false;
        }
        if (projection != null ? ! projection.equals(that.projection) : that.projection != null) {
            return false;
        }
        return provisionedThroughput != null ? provisionedThroughput.equals(that.provisionedThroughput) :
            that.provisionedThroughput == null;
    }

    @Override
    public int hashCode() {
        int result = indexName != null ? indexName.hashCode() : 0;
        result = 31 * result + (projection != null ? projection.hashCode() : 0);
        result = 31 * result + (provisionedThroughput != null ? provisionedThroughput.hashCode() : 0);
        return result;
    }

    /**
     * A builder for {@link EnhancedGlobalSecondaryIndex}
     */
    public static final class Builder {
        private String indexName;
        private Projection projection;
        private ProvisionedThroughput provisionedThroughput;

        private Builder() {
        }

        /**
         * The name of the global secondary index
         */
        public Builder indexName(String indexName) {
            this.indexName = indexName;
            return this;
        }

        /**
         * The attribute projection setting for this global secondary index.
         */
        public Builder projection(Projection projection) {
            this.projection = projection;
            return this;
        }

        /**
         * The attribute projection setting for this global secondary index.
         */
        public Builder projection(Consumer<Projection.Builder> projection) {
            Projection.Builder builder = Projection.builder();
            projection.accept(builder);
            return projection(builder.build());
        }

        /**
         * The provisioned throughput setting for this global secondary index.
         */
        public Builder provisionedThroughput(ProvisionedThroughput provisionedThroughput) {
            this.provisionedThroughput = provisionedThroughput;
            return this;
        }

        /**
         * The provisioned throughput setting for this global secondary index.
         */
        public Builder provisionedThroughput(Consumer<ProvisionedThroughput.Builder> provisionedThroughput) {
            ProvisionedThroughput.Builder builder = ProvisionedThroughput.builder();
            provisionedThroughput.accept(builder);
            return provisionedThroughput(builder.build());
        }

        /**
         * Builds a {@link EnhancedGlobalSecondaryIndex} based on the values stored in this builder
         */
        public EnhancedGlobalSecondaryIndex build() {
            return new EnhancedGlobalSecondaryIndex(this);
        }
    }
}
