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

package software.amazon.awssdk.services.sqs.internal.batchmanager;

import java.time.Duration;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.sqs.batchmanager.BatchOverrideConfiguration;

@SdkInternalApi
public final class RequestBatchConfiguration {

    public static final int DEFAULT_MAX_BATCH_ITEMS = 10;
    public static final int DEFAULT_MAX_BATCH_BYTES_SIZE = -1;
    public static final int DEFAULT_MAX_BATCH_KEYS = 1000;
    public static final int DEFAULT_MAX_BUFFER_SIZE = 500;
    public static final Duration DEFAULT_MAX_BATCH_OPEN_IN_MS = Duration.ofMillis(200);

    private final Integer maxBatchItems;
    private final Integer maxBatchKeys;
    private final Integer maxBufferSize;
    private final Duration maxBatchOpenDuration;
    private final Integer maxBatchBytesSize;

    private RequestBatchConfiguration(Builder builder) {

        this.maxBatchItems = builder.maxBatchItems != null ? builder.maxBatchItems : DEFAULT_MAX_BATCH_ITEMS;
        this.maxBatchKeys = builder.maxBatchKeys != null ? builder.maxBatchKeys : DEFAULT_MAX_BATCH_KEYS;
        this.maxBufferSize = builder.maxBufferSize != null ? builder.maxBufferSize : DEFAULT_MAX_BUFFER_SIZE;
        this.maxBatchOpenDuration = builder.maxBatchOpenDuration != null ? builder.maxBatchOpenDuration :
                                         DEFAULT_MAX_BATCH_OPEN_IN_MS;
        this.maxBatchBytesSize = builder.maxBatchBytesSize != null ? builder.maxBatchBytesSize : DEFAULT_MAX_BATCH_BYTES_SIZE;

    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(BatchOverrideConfiguration configuration) {
        if (configuration != null) {
            return new Builder()
                .maxBatchKeys(configuration.maxBatchKeys())
                .maxBatchItems(configuration.maxBatchItems())
                .maxBatchOpenDuration(configuration.maxBatchOpenDuration())
                .maxBufferSize(configuration.maxBufferSize());
        }
        return new Builder();
    }

    public Duration maxBatchOpenDuration() {
        return maxBatchOpenDuration;
    }

    public int maxBatchItems() {
        return maxBatchItems;
    }

    public int maxBatchKeys() {
        return maxBatchKeys;
    }

    public int maxBufferSize() {
        return maxBufferSize;
    }

    public int maxBatchBytesSize() {
        return maxBatchBytesSize;
    }

    public static final class Builder {

        private Integer maxBatchItems;
        private Integer maxBatchKeys;
        private Integer maxBufferSize;
        private Duration maxBatchOpenDuration;
        private Integer maxBatchBytesSize;

        private Builder() {
        }

        public Builder maxBatchItems(Integer maxBatchItems) {
            this.maxBatchItems = maxBatchItems;
            return this;
        }

        public Builder maxBatchKeys(Integer maxBatchKeys) {
            this.maxBatchKeys = maxBatchKeys;
            return this;
        }

        public Builder maxBufferSize(Integer maxBufferSize) {
            this.maxBufferSize = maxBufferSize;
            return this;
        }

        public Builder maxBatchOpenDuration(Duration maxBatchOpenDuration) {
            this.maxBatchOpenDuration = maxBatchOpenDuration;
            return this;
        }

        public Builder maxBatchBytesSize(Integer maxBatchBytesSize) {
            this.maxBatchBytesSize = maxBatchBytesSize;
            return this;
        }

        public RequestBatchConfiguration build() {
            return new RequestBatchConfiguration(this);
        }
    }

}