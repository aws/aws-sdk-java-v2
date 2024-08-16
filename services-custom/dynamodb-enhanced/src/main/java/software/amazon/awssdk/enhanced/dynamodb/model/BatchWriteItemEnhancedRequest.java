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
import java.util.Objects;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.ReturnItemCollectionMetrics;

/**
 * Defines parameters used for the batchWriteItem() operation (such as
 * {@link DynamoDbEnhancedClient#batchWriteItem(BatchWriteItemEnhancedRequest)}).
 * <p>
 * A request contains references to keys for delete actions and items for put actions, organized into one {@link WriteBatch}
 * object per accessed table.
 */
@SdkPublicApi
@ThreadSafe
public final class BatchWriteItemEnhancedRequest {

    private final List<WriteBatch> writeBatches;
    private final String returnConsumedCapacity;
    private final String returnItemCollectionMetrics;

    private BatchWriteItemEnhancedRequest(Builder builder) {
        this.writeBatches = getListIfExist(builder.writeBatches);
        this.returnConsumedCapacity = builder.returnConsumedCapacity;
        this.returnItemCollectionMetrics = builder.returnItemCollectionMetrics;
    }

    /**
     * Creates a newly initialized builder for a request object.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return a builder with all existing values set
     */
    public Builder toBuilder() {
        return builder().writeBatches(writeBatches).returnConsumedCapacity(returnConsumedCapacity).returnItemCollectionMetrics(returnItemCollectionMetrics);
    }

    /**
     * Whether to return the capacity consumed by this operation.
     *
     * @see BatchWriteItemEnhancedRequest#returnConsumedCapacity()
     */
    public ReturnConsumedCapacity returnConsumedCapacity() {
        return ReturnConsumedCapacity.fromValue(returnConsumedCapacity);
    }

    /**
     * Whether to return the capacity consumed by this operation.
     * <p>
     * Similar to {@link #returnConsumedCapacity()} but return the value as a string. This is useful in situations where the value
     * is not defined in {@link ReturnConsumedCapacity}.
     */
    public String returnConsumedCapacityAsString() {
        return returnConsumedCapacity;
    }

    /**
     * Whether to return the item collection metrics.
     *
     * @see BatchWriteItemRequest#returnItemCollectionMetrics()
     */
    public ReturnItemCollectionMetrics returnItemCollectionMetrics() {
        return ReturnItemCollectionMetrics.fromValue(returnItemCollectionMetrics);
    }

    /**
     * Whether to return the item collection metrics.
     * <p>
     * Similar to {@link #returnItemCollectionMetrics()} but return the value as a string. This is useful in situations where the
     * value is not defined in {@link ReturnItemCollectionMetrics}.
     */
    public String returnItemCollectionMetricsAsString() {
        return returnItemCollectionMetrics;
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
        return Objects.equals(writeBatches, that.writeBatches) && Objects.equals(returnConsumedCapacity,
                                                                                 that.returnConsumedCapacity) && Objects.equals(returnItemCollectionMetrics, that.returnItemCollectionMetrics);
    }

    @Override
    public int hashCode() {
        return Objects.hash(writeBatches, returnConsumedCapacity, returnItemCollectionMetrics);
    }

    private static List<WriteBatch> getListIfExist(List<WriteBatch> writeBatches) {
        return writeBatches != null ? Collections.unmodifiableList(writeBatches) : null;
    }

    /**
     * A builder that is used to create a request with the desired parameters.
     */
    @NotThreadSafe
    public static final class Builder {
        private List<WriteBatch> writeBatches;
        private String returnConsumedCapacity;
        private String returnItemCollectionMetrics;

        private Builder() {
        }

        /**
         * Whether to return the capacity consumed by this operation.
         *
         * @see Builder#returnConsumedCapacity(ReturnConsumedCapacity)
         */
        public Builder returnConsumedCapacity(ReturnConsumedCapacity returnConsumedCapacity) {
            this.returnConsumedCapacity = returnConsumedCapacity == null ? null : returnConsumedCapacity.toString();
            return this;
        }

        /**
         * Whether to return the capacity consumed by this operation.
         *
         * @see Builder#returnConsumedCapacity(String)
         */
        public Builder returnConsumedCapacity(String returnConsumedCapacity) {
            this.returnConsumedCapacity = returnConsumedCapacity;
            return this;
        }

        /**
         * Whether to return the item collection metrics.
         *
         * @see BatchWriteItemEnhancedRequest.Builder#returnItemCollectionMetrics(ReturnItemCollectionMetrics)
         */
        public BatchWriteItemEnhancedRequest.Builder returnItemCollectionMetrics(ReturnItemCollectionMetrics returnItemCollectionMetrics) {
            this.returnItemCollectionMetrics = returnItemCollectionMetrics == null ? null :
                                               returnItemCollectionMetrics.toString();
            return this;
        }

        /**
         * Whether to return the item collection metrics.
         *
         * @see BatchWriteItemEnhancedRequest.Builder#returnItemCollectionMetrics(String)
         */
        public BatchWriteItemEnhancedRequest.Builder returnItemCollectionMetrics(String returnItemCollectionMetrics) {
            this.returnItemCollectionMetrics = returnItemCollectionMetrics;
            return this;
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
         * Adds a write batch to the collection of batches on this builder. If this is the first batch, the method creates a new
         * list.
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
