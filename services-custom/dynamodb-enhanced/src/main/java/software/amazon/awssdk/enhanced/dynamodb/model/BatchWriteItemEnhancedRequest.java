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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;

/**
 * Defines parameters used for the batchWriteItem() operation (such as
 * {@link DynamoDbEnhancedClient#batchWriteItem(BatchWriteItemEnhancedRequest)}).
 * <p>
 * A request contains references to keys for delete actions and items for put actions,
 * organized into one {@link WriteBatch} object per accessed table.
 */
@SdkPublicApi
public final class BatchWriteItemEnhancedRequest {

    private final List<WriteBatch> writeBatches;

    private BatchWriteItemEnhancedRequest(Builder builder) {
        this.writeBatches = getListIfExist(builder.writeBatches);
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
        return new Builder().writeBatches(writeBatches);
    }

    /**
     * Returns the collection of {@link WriteBatch} in this request object.
     */
    public Collection<WriteBatch> writeBatches() {
        return writeBatches;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BatchWriteItemEnhancedRequest that = (BatchWriteItemEnhancedRequest) o;

        return writeBatches != null ? writeBatches.equals(that.writeBatches) : that.writeBatches == null;
    }

    @Override
    public int hashCode() {
        return writeBatches != null ? writeBatches.hashCode() : 0;
    }

    private static List<WriteBatch> getListIfExist(List<WriteBatch> writeBatches) {
        return writeBatches != null ? Collections.unmodifiableList(writeBatches) : null;
    }

    /**
     * A builder that is used to create a request with the desired parameters.
     */
    public static final class Builder {
        private List<WriteBatch> writeBatches;

        private Builder() {
        }

        /**
         * Sets a collection of write batches to use in the batchWriteItem operation.
         *
         * @param writeBatches the collection of write batches
         * @return a builder of this type
         */
        public Builder writeBatches(Collection<WriteBatch> writeBatches) {
            this.writeBatches = writeBatches != null ? new ArrayList<>(writeBatches) : null;
            return this;
        }

        /**
         * Sets one or more write batches to use in the batchWriteItem operation.
         *
         * @param writeBatches one or more {@link WriteBatch}, separated by comma.
         * @return a builder of this type
         */
        public Builder writeBatches(WriteBatch... writeBatches) {
            this.writeBatches = Arrays.asList(writeBatches);
            return this;
        }

        /**
         * Adds a write batch to the collection of batches on this builder.
         * If this is the first batch, the method creates a new list.
         *
         * @param writeBatch a single write batch
         * @return a builder of this type
         */
        public Builder addWriteBatch(WriteBatch writeBatch) {
            if (writeBatches == null) {
                writeBatches = new ArrayList<>();
            }
            writeBatches.add(writeBatch);
            return this;
        }

        public BatchWriteItemEnhancedRequest build() {
            return new BatchWriteItemEnhancedRequest(this);
        }
    }

}
