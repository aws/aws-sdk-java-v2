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

package software.amazon.awssdk.core;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.internal.batchmanager.BatchManager;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Configuration values for the {@link BatchManager}. All values are optional, and the default values will be used
 * if they are not specified.
 */
@SdkPublicApi
public final class BatchOverrideConfiguration implements ToCopyableBuilder<BatchOverrideConfiguration.Builder,
    BatchOverrideConfiguration> {

    private final Integer maxBatchItems;
    private final Duration maxBatchOpenInMs;
    private final ScheduledExecutorService scheduledExecutor;

    public BatchOverrideConfiguration(Builder builder) {
        Validate.notNull(builder.maxBatchItems, "maxBatchItems cannot be null");
        this.maxBatchItems = Validate.isPositive(builder.maxBatchItems, "maxBatchItems");
        Validate.notNull(builder.maxBatchOpenInMs, "maxBatchOpenInMs cannot be null");
        this.maxBatchOpenInMs = Validate.isPositive(builder.maxBatchOpenInMs, "maxBachOpenInMs");
        this.scheduledExecutor = Validate.notNull(builder.scheduledExecutor, "scheduledExecutor cannot be null");
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return the optional maximum number of messages that are batched together in a single request.
     */
    public Integer maxBatchItems() {
        return maxBatchItems;
    }

    /**
     * @return the optional maximum amount of time (in milliseconds) that an outgoing call waits to be batched with messages of
     * the same type.
     */
    public Duration maxBatchOpenInMs() {
        return maxBatchOpenInMs;
    }

    public ScheduledExecutorService scheduledExecutor() {
        return scheduledExecutor;
    }

    @Override
    public Builder toBuilder() {
        return new Builder().maxBatchItems(maxBatchItems)
                            .maxBatchOpenInMs(maxBatchOpenInMs)
                            .scheduledExecutor(scheduledExecutor);
    }

    @Override
    public String toString() {
        return ToString.builder("BatchOverrideConfiguration")
                       .add("maxBatchItems", maxBatchItems)
                       .add("maxBatchOpenInMs", maxBatchOpenInMs.toMillis())
                       .build();
    }

    public static final class Builder implements CopyableBuilder<Builder, BatchOverrideConfiguration> {

        private Integer maxBatchItems;
        private Duration maxBatchOpenInMs;
        private ScheduledExecutorService scheduledExecutor;

        private Builder() {
        }

        public Builder maxBatchItems(Integer maxBatchItems) {
            this.maxBatchItems = maxBatchItems;
            return this;
        }

        public Builder maxBatchOpenInMs(Duration maxBatchOpenInMs) {
            this.maxBatchOpenInMs = maxBatchOpenInMs;
            return this;
        }

        public Builder scheduledExecutor(ScheduledExecutorService scheduledExecutor) {
            this.scheduledExecutor = scheduledExecutor;
            return this;
        }

        public BatchOverrideConfiguration build() {
            return new BatchOverrideConfiguration(this);
        }
    }
}
