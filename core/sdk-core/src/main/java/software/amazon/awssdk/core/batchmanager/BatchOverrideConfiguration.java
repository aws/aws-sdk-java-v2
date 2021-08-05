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

package software.amazon.awssdk.core.batchmanager;

import java.time.Duration;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.internal.batchmanager.DefaultBatchManager;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Configuration values for the {@link DefaultBatchManager}. All values are optional, and the default values will be used
 * if they are not specified.
 */
@SdkPublicApi
public final class BatchOverrideConfiguration implements ToCopyableBuilder<BatchOverrideConfiguration.Builder,
    BatchOverrideConfiguration> {

    private final Integer maxBatchItems;
    private final Duration maxBatchOpenInMs;

    public BatchOverrideConfiguration(Builder builder) {
        this.maxBatchItems = Validate.isPositiveOrNull(builder.maxBatchItems, "maxBatchItems");
        this.maxBatchOpenInMs = Validate.isPositiveOrNull(builder.maxBatchOpenInMs, "maxBachOpenInMs");
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
     * @return the optional maximum amount of time (in milliseconds) that an outgoing call waits to be batched with messages of
     * the same type.
     */
    public Optional<Duration> maxBatchOpenInMs() {
        return Optional.ofNullable(maxBatchOpenInMs);
    }

    @Override
    public Builder toBuilder() {
        return new Builder().maxBatchItems(maxBatchItems)
                            .maxBatchOpenInMs(maxBatchOpenInMs);
    }

    @Override
    public String toString() {
        return ToString.builder("BatchOverrideConfiguration")
                       .add("maxBatchItems", maxBatchItems)
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
        return maxBatchOpenInMs != null ? maxBatchOpenInMs.equals(that.maxBatchOpenInMs) : that.maxBatchOpenInMs == null;
    }

    @Override
    public int hashCode() {
        int result = maxBatchItems != null ? maxBatchItems.hashCode() : 0;
        result = 31 * result + (maxBatchOpenInMs != null ? maxBatchOpenInMs.hashCode() : 0);
        return result;
    }

    public static final class Builder implements CopyableBuilder<Builder, BatchOverrideConfiguration> {

        private Integer maxBatchItems;
        private Duration maxBatchOpenInMs;

        private Builder() {
        }

        /**
         * Define the the maximum number of messages that are batched together in a single request.
         *
         * @param maxBatchItems The new maxBatchItems value.
         * @return This object for method chaining.
         */
        public Builder maxBatchItems(Integer maxBatchItems) {
            this.maxBatchItems = maxBatchItems;
            return this;
        }

        /**
         * The maximum amount of time (in milliseconds) that an outgoing call waits for other requests before sending out a batch
         * request.
         *
         * @param maxBatchOpenInMs
         * @return
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
