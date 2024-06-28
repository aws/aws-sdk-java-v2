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

package software.amazon.awssdk.services.sqs.batchmanager;

import java.time.Duration;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Configuration values for the BatchManager Implementation.  All values are optional, and the default values will be used
 * if they are not specified.
 */
@SdkPublicApi
public final class BatchOverrideConfiguration implements ToCopyableBuilder<BatchOverrideConfiguration.Builder,
    BatchOverrideConfiguration> {

    private final Integer maxBatchItems;
    private final Integer maxBatchKeys;
    private final Integer maxBufferSize;
    private final Duration maxBatchOpenInMs;

    public BatchOverrideConfiguration(Builder builder) {
        //TODO : Add defaults based on QueueBufferConfig.java of V1 Default values.
        this.maxBatchItems = Validate.isPositiveOrNull(builder.maxBatchItems, "maxBatchItems");
        this.maxBatchOpenInMs = Validate.isPositiveOrNull(builder.maxBatchOpenInMs, "maxBachOpenInMs");
        this.maxBatchKeys = Validate.isPositiveOrNull(builder.maxBatchKeys, "maxBatchKeys");
        this.maxBufferSize = Validate.isPositiveOrNull(builder.maxBufferSize, "maxBufferSize");
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return the optional maximum number of messages that are batched together in a single request.
     */
    public Optional<Integer> maxBatchItems() {
        return Optional.ofNullable(maxBatchItems);
    }

    /**
     * @return the optional maximum number of batchKeys to keep track of.
     */
    public Optional<Integer> maxBatchKeys() {
        return Optional.ofNullable(maxBatchKeys);
    }

    /**
     * @return the maximum number of items to allow to be buffered for each batchKey.
     */
    public Optional<Integer> maxBufferSize() {
        return Optional.ofNullable(maxBufferSize);
    }

    /**
     * @return the optional maximum amount of time (in milliseconds) that an outgoing call waits to be batched with messages of
     * the same type.
     */
    public Optional<Duration> maxBatchOpenInMs() {
        return Optional.ofNullable(maxBatchOpenInMs);
    }

    @Override
    public Builder toBuilder() {
        return new Builder().maxBatchItems(maxBatchItems)
                            .maxBatchKeys(maxBatchKeys)
                            .maxBufferSize(maxBufferSize)
                            .maxBatchOpenInMs(maxBatchOpenInMs);
    }

    @Override
    public String toString() {
        return ToString.builder("BatchOverrideConfiguration")
                       .add("maxBatchItems", maxBatchItems)
                       .add("maxBatchKeys", maxBatchKeys)
                       .add("maxBufferSize", maxBufferSize)
                       .add("maxBatchOpenInMs", maxBatchOpenInMs.toMillis())
                       .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BatchOverrideConfiguration that = (BatchOverrideConfiguration) o;

        if (maxBatchItems != null ? !maxBatchItems.equals(that.maxBatchItems) : that.maxBatchItems != null) {
            return false;
        }
        if (maxBatchKeys != null ? !maxBatchKeys.equals(that.maxBatchKeys) : that.maxBatchKeys != null) {
            return false;
        }
        if (maxBufferSize != null ? !maxBufferSize.equals(that.maxBufferSize) : that.maxBufferSize != null) {
            return false;
        }
        return maxBatchOpenInMs != null ? maxBatchOpenInMs.equals(that.maxBatchOpenInMs) : that.maxBatchOpenInMs == null;
    }

    @Override
    public int hashCode() {
        int result = maxBatchItems != null ? maxBatchItems.hashCode() : 0;
        result = 31 * result + (maxBatchKeys != null ? maxBatchKeys.hashCode() : 0);
        result = 31 * result + (maxBufferSize != null ? maxBufferSize.hashCode() : 0);
        result = 31 * result + (maxBatchOpenInMs != null ? maxBatchOpenInMs.hashCode() : 0);
        return result;
    }

    public static final class Builder implements CopyableBuilder<Builder, BatchOverrideConfiguration> {

        private Integer maxBatchItems;
        private Integer maxBatchKeys;
        private Integer maxBufferSize;
        private Duration maxBatchOpenInMs;

        private Builder() {
        }

        /**
         * Define the maximum number of messages that are batched together in a single request.
         * //TODO : QueueBufferConfig.java Default value of 10.
         *
         * @param maxBatchItems The new maxBatchItems value.
         * @return This object for method chaining.
         */
        public Builder maxBatchItems(Integer maxBatchItems) {
            this.maxBatchItems = maxBatchItems;
            return this;
        }

        /**
         * Define the maximum number of batchKeys to keep track of. A batchKey determines which requests are batched together
         * and is calculated by the client based on the information in a request.
         * <p>
         * Ex. SQS determines a batchKey based on a request's queueUrl in combination with its overrideConfiguration, so
         * requests with the same queueUrl and overrideConfiguration will have the same batchKey and be batched together.
         *
         * @param maxBatchKeys the new maxBatchKeys value.
         * @return This object for method chaining.
         */
        public Builder maxBatchKeys(Integer maxBatchKeys) {
            this.maxBatchKeys = maxBatchKeys;
            return this;
        }

        /**
         * Define the maximum number of items to allow to be buffered for each batchKey.
         *
         * @param maxBufferSize the new maxBufferSize value.
         * @return This object for method chaining.
         */
        public Builder maxBufferSize(Integer maxBufferSize) {
            this.maxBufferSize = maxBufferSize;
            return this;
        }

        /**
         * The maximum amount of time (in milliseconds) that an outgoing call waits for other requests before sending out a batch
         * request.
         * TODO : Decide if Ms needs to be added to the name in surface API review meeting
         * @param maxBatchOpenInMs The new maxBatchOpenInMs value.
         * @return This object for method chaining.
         */
        public Builder maxBatchOpenInMs(Duration maxBatchOpenInMs) {
            this.maxBatchOpenInMs = maxBatchOpenInMs;
            return this;
        }

        public BatchOverrideConfiguration build() {
            return new BatchOverrideConfiguration(this);
        }
    }
}
