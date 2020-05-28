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

import java.util.Arrays;
import java.util.Collection;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;

/**
 * Defines parameters used to create a DynamoDb table using the createTable() operation (such as
 * {@link DynamoDbTable#createTable(CreateTableEnhancedRequest)} or
 * {@link DynamoDbAsyncTable#createTable(CreateTableEnhancedRequest)}).
 * <p>
 * All parameters are optional.
 */
@SdkPublicApi
public final class CreateTableEnhancedRequest {
    private final ProvisionedThroughput provisionedThroughput;
    private final Collection<EnhancedLocalSecondaryIndex> localSecondaryIndices;
    private final Collection<EnhancedGlobalSecondaryIndex> globalSecondaryIndices;

    private CreateTableEnhancedRequest(Builder builder) {
        this.provisionedThroughput = builder.provisionedThroughput;
        this.localSecondaryIndices = builder.localSecondaryIndices;
        this.globalSecondaryIndices = builder.globalSecondaryIndices;
    }

    /**
     * Creates a newly initialized builder for a request object.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a builder initialized with all existing values on the request object.
     */
    public Builder toBuilder() {
        return builder().provisionedThroughput(provisionedThroughput)
                        .localSecondaryIndices(localSecondaryIndices)
                        .globalSecondaryIndices(globalSecondaryIndices);
    }

    /**
     * Returns the provisioned throughput value set on this request object, or null if it has not been set.
     */
    public ProvisionedThroughput provisionedThroughput() {
        return provisionedThroughput;
    }

    /**
     * Returns the local secondary index set on this request object, or null if it has not been set.
     */
    public Collection<EnhancedLocalSecondaryIndex> localSecondaryIndices() {
        return localSecondaryIndices;
    }

    /**
     * Returns the global secondary index set on this request object, or null if it has not been set.
     */
    public Collection<EnhancedGlobalSecondaryIndex> globalSecondaryIndices() {
        return globalSecondaryIndices;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CreateTableEnhancedRequest that = (CreateTableEnhancedRequest) o;

        if (provisionedThroughput != null ? ! provisionedThroughput.equals(that.provisionedThroughput) :
            that.provisionedThroughput != null) {
            return false;
        }
        if (localSecondaryIndices != null ? ! localSecondaryIndices.equals(that.localSecondaryIndices) :
            that.localSecondaryIndices != null) {
            return false;
        }
        return globalSecondaryIndices != null ? globalSecondaryIndices.equals(that.globalSecondaryIndices) :
            that.globalSecondaryIndices == null;
    }

    @Override
    public int hashCode() {
        int result = provisionedThroughput != null ? provisionedThroughput.hashCode() : 0;
        result = 31 * result + (localSecondaryIndices != null ? localSecondaryIndices.hashCode() : 0);
        result = 31 * result + (globalSecondaryIndices != null ? globalSecondaryIndices.hashCode() : 0);
        return result;
    }

    /**
     * A builder that is used to create a request with the desired parameters.
     */
    public static final class Builder {
        private ProvisionedThroughput provisionedThroughput;
        private Collection<EnhancedLocalSecondaryIndex> localSecondaryIndices;
        private Collection<EnhancedGlobalSecondaryIndex> globalSecondaryIndices;

        private Builder() {
        }

        /**
         * Sets the provisioned throughput for this table. Use this parameter to set the table's
         * read and write capacity units.
         * <p>
         * See the DynamoDb documentation for more information on default throughput values.
         */
        public Builder provisionedThroughput(ProvisionedThroughput provisionedThroughput) {
            this.provisionedThroughput = provisionedThroughput;
            return this;
        }

        /**
         * Defines a local secondary index for this table.
         * <p>
         * See {@link EnhancedLocalSecondaryIndex} for more information on creating and using a local secondary index.
         */
        public Builder localSecondaryIndices(Collection<EnhancedLocalSecondaryIndex> localSecondaryIndices) {
            this.localSecondaryIndices = localSecondaryIndices;
            return this;
        }

        /**
         * Defines a local secondary index for this table.
         * <p>
         * See {@link EnhancedLocalSecondaryIndex} for more information on creating and using a local secondary index.
         */
        public Builder localSecondaryIndices(EnhancedLocalSecondaryIndex... localSecondaryIndices) {
            this.localSecondaryIndices = Arrays.asList(localSecondaryIndices);
            return this;
        }

        /**
         * Defines a global secondary index for this table.
         * <p>
         * See {@link EnhancedGlobalSecondaryIndex} for more information on creating and using a global secondary index.
         */
        public Builder globalSecondaryIndices(Collection<EnhancedGlobalSecondaryIndex> globalSecondaryIndices) {
            this.globalSecondaryIndices = globalSecondaryIndices;
            return this;
        }

        /**
         * Defines a global secondary index for this table.
         * <p>
         * See {@link EnhancedGlobalSecondaryIndex} for more information on creating and using a global secondary index.
         */
        public Builder globalSecondaryIndices(EnhancedGlobalSecondaryIndex... globalSecondaryIndices) {
            this.globalSecondaryIndices = Arrays.asList(globalSecondaryIndices);
            return this;
        }

        public CreateTableEnhancedRequest build() {
            return new CreateTableEnhancedRequest(this);
        }
    }

}
