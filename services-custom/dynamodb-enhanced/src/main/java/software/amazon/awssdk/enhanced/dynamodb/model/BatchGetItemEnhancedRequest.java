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
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;

/**
 * Defines parameters used for the batchGetItem() operation (such as
 * {@link DynamoDbEnhancedClient#batchGetItem(BatchGetItemEnhancedRequest)}).
 * <p>
 * A request contains references to keys and tables organized into one {@link ReadBatch} object per queried table.
 */
@SdkPublicApi
@ThreadSafe
public final class BatchGetItemEnhancedRequest {

    private final List<ReadBatch> readBatches;
    private final String returnConsumedCapacity;
    private final AwsRequestOverrideConfiguration overrideConfiguration;

    private BatchGetItemEnhancedRequest(Builder builder) {
        this.readBatches = getListIfExist(builder.readBatches);
        this.returnConsumedCapacity = builder.returnConsumedCapacity;
        this.overrideConfiguration = builder.overrideConfiguration;
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
        return new Builder().readBatches(readBatches)
                            .returnConsumedCapacity(this.returnConsumedCapacity)
                            .overrideConfiguration(this.overrideConfiguration);
    }

    /**
     * Returns the collection of {@link ReadBatch} in this request object.
     */
    public Collection<ReadBatch> readBatches() {
        return readBatches;
    }

    /**
     * Whether to return the capacity consumed by this operation.
     *
     * @see GetItemRequest#returnConsumedCapacity()
     */
    public ReturnConsumedCapacity returnConsumedCapacity() {
        return ReturnConsumedCapacity.fromValue(returnConsumedCapacity);
    }

    /**
     * Whether to return the capacity consumed by this operation.
     * <p>
     * Similar to {@link #returnConsumedCapacity()} but return the value as a string. This is useful in situations where the
     * value is not defined in {@link ReturnConsumedCapacity}.
     */
    public String returnConsumedCapacityAsString() {
        return returnConsumedCapacity;
    }

    /**
     * Returns the override configuration to apply to the low-level {@link BatchGetItemRequest}.
     * <p>
     * This can be used to customize the request, such as adding custom headers, MetricPublisher or AwsCredentialsProvider.
     * </p>
     *
     * @return the {@link AwsRequestOverrideConfiguration} to apply to the underlying service call.
     */
    public AwsRequestOverrideConfiguration overrideConfiguration() {
        return overrideConfiguration;
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

        return Objects.equals(this.readBatches, that.readBatches) &&
               Objects.equals(this.returnConsumedCapacity, that.returnConsumedCapacity) &&
               Objects.equals(this.overrideConfiguration, that.overrideConfiguration);
    }

    @Override
    public int hashCode() {
        int hc = readBatches != null ? readBatches.hashCode() : 0;
        hc = 31 * hc + (returnConsumedCapacity != null ? returnConsumedCapacity.hashCode() : 0);
        hc = 31 * hc + (overrideConfiguration != null ? overrideConfiguration.hashCode() : 0);
        return hc;
    }

    private static List<ReadBatch> getListIfExist(List<ReadBatch> readBatches) {
        return readBatches != null ? Collections.unmodifiableList(readBatches) : null;
    }

    /**
     * A builder that is used to create a request with the desired parameters.
     */
    @NotThreadSafe
    public static final class Builder {
        private List<ReadBatch> readBatches;
        private String returnConsumedCapacity;
        private AwsRequestOverrideConfiguration overrideConfiguration;

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

        /**
         * Whether to return the capacity consumed by this operation.
         *
         * @see BatchGetItemRequest.Builder#returnConsumedCapacity(ReturnConsumedCapacity)
         * @return a builder of this type
         */
        public Builder returnConsumedCapacity(ReturnConsumedCapacity returnConsumedCapacity) {
            this.returnConsumedCapacity = returnConsumedCapacity == null ? null : returnConsumedCapacity.toString();
            return this;
        }

        /**
         * Whether to return the capacity consumed by this operation.
         *
         * @see BatchGetItemRequest.Builder#returnConsumedCapacity(String)
         * @return a builder of this type
         */
        public Builder returnConsumedCapacity(String returnConsumedCapacity) {
            this.returnConsumedCapacity = returnConsumedCapacity;
            return this;
        }

        /**
         * Sets the override configuration to apply to the low-level {@link BatchGetItemRequest}.
         *
         * @see BatchGetItemRequest.Builder#overrideConfiguration(AwsRequestOverrideConfiguration) 
         * @return a builder of this type
         */
        public Builder overrideConfiguration(AwsRequestOverrideConfiguration overrideConfiguration) {
            this.overrideConfiguration = overrideConfiguration;
            return this;
        }

        /**
         * Sets the override configuration to apply to the low-level {@link BatchGetItemRequest}.
         *
         * @see BatchGetItemRequest.Builder#overrideConfiguration(Consumer)
         * @return a builder of this type
         */
        public Builder overrideConfiguration(Consumer<AwsRequestOverrideConfiguration.Builder> overrideConfigurationBuilder) {
            AwsRequestOverrideConfiguration.Builder builder = AwsRequestOverrideConfiguration.builder();
            overrideConfigurationBuilder.accept(builder);
            this.overrideConfiguration = builder.build();
            return this;
        }

        public BatchGetItemEnhancedRequest build() {
            return new BatchGetItemEnhancedRequest(this);
        }
    }
}
