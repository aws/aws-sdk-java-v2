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
 * Defines parameters used for the batchGetItem() operation (such as
 * {@link DynamoDbEnhancedClient#batchGetItem(BatchGetItemEnhancedRequest)}).
 * <p>
 * A request contains references to keys and tables organized into one {@link ReadBatch} object per queried table.
 */
@SdkPublicApi
public final class BatchGetItemEnhancedRequest {

    private final List<ReadBatch> readBatches;

    private BatchGetItemEnhancedRequest(Builder builder) {
        this.readBatches = getListIfExist(builder.readBatches);
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
        return new Builder().readBatches(readBatches);
    }

    /**
     * Returns the collection of {@link ReadBatch} in this request object.
     */
    public Collection<ReadBatch> readBatches() {
        return readBatches;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BatchGetItemEnhancedRequest that = (BatchGetItemEnhancedRequest) o;

        return readBatches != null ? readBatches.equals(that.readBatches) : that.readBatches == null;
    }

    @Override
    public int hashCode() {
        return readBatches != null ? readBatches.hashCode() : 0;
    }

    private static List<ReadBatch> getListIfExist(List<ReadBatch> readBatches) {
        return readBatches != null ? Collections.unmodifiableList(readBatches) : null;
    }

    /**
     * A builder that is used to create a request with the desired parameters.
     */
    public static final class Builder {
        private List<ReadBatch> readBatches;

        private Builder() {
        }

        /**
         * Sets a collection of read batches to use in the batchGetItem operation.
         *
         * @param readBatches the collection of read batches
         * @return a builder of this type
         */
        public Builder readBatches(Collection<ReadBatch> readBatches) {
            this.readBatches = readBatches != null ? new ArrayList<>(readBatches) : null;
            return this;
        }

        /**
         * Sets one or more read batches to use in the batchGetItem operation.
         *
         * @param readBatches one or more {@link ReadBatch}, separated by comma.
         * @return a builder of this type
         */
        public Builder readBatches(ReadBatch... readBatches) {
            this.readBatches = Arrays.asList(readBatches);
            return this;
        }

        /**
         * Adds a read batch to the collection of batches on this builder.
         * If this is the first batch, the method creates a new list.
         *
         * @param readBatch a single read batch
         * @return a builder of this type
         */
        public Builder addReadBatch(ReadBatch readBatch) {
            if (readBatches == null) {
                readBatches = new ArrayList<>();
            }
            readBatches.add(readBatch);
            return this;
        }

        public BatchGetItemEnhancedRequest build() {
            return new BatchGetItemEnhancedRequest(this);
        }
    }

}
