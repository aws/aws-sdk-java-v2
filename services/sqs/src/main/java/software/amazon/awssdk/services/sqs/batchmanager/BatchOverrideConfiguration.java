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
import java.util.Collections;
import java.util.List;
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
    private final Long maxBatchSizeBytes;
    private final Integer visibilityTimeoutSeconds;
    private final Integer longPollWaitTimeoutSeconds;
    private final Integer minReceiveWaitTimeMs;
    private final List<String> receiveAttributeNames;
    private final List<String> receiveMessageAttributeNames;
    private final Boolean adaptivePrefetching;
    private final Boolean flushOnShutdown;

    public BatchOverrideConfiguration(Builder builder) {
        this.maxBatchItems = Validate.isPositiveOrNull(builder.maxBatchItems, "maxBatchItems");
        this.maxBatchKeys = Validate.isPositiveOrNull(builder.maxBatchKeys, "maxBatchKeys");
        this.maxBufferSize = Validate.isPositiveOrNull(builder.maxBufferSize, "maxBufferSize");
        this.maxBatchOpenInMs = Validate.isPositiveOrNull(builder.maxBatchOpenInMs, "maxBatchOpenInMs");
        this.maxBatchSizeBytes = Validate.isPositiveOrNull(builder.maxBatchSizeBytes, "maxBatchSizeBytes");
        this.visibilityTimeoutSeconds = Validate.isPositiveOrNull(builder.visibilityTimeoutSeconds,
                                                                  "visibilityTimeoutSeconds");
        this.longPollWaitTimeoutSeconds = Validate.isPositiveOrNull(builder.longPollWaitTimeoutSeconds,
                                                                    "longPollWaitTimeoutSeconds");
        this.minReceiveWaitTimeMs = Validate.isPositiveOrNull(builder.minReceiveWaitTimeMs, "minReceiveWaitTimeMs");
        this.receiveAttributeNames = builder.receiveAttributeNames;
        this.receiveMessageAttributeNames = builder.receiveMessageAttributeNames;
        this.adaptivePrefetching = builder.adaptivePrefetching;
        this.flushOnShutdown = builder.flushOnShutdown;
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

    public Optional<Long> maxBatchSizeBytes() {
        return Optional.ofNullable(maxBatchSizeBytes);
    }

    public Optional<Integer> visibilityTimeoutSeconds() {
        return Optional.ofNullable(visibilityTimeoutSeconds);
    }

    public Optional<Integer> longPollWaitTimeoutSeconds() {
        return Optional.ofNullable(longPollWaitTimeoutSeconds);
    }

    public Optional<Integer> minReceiveWaitTimeMs() {
        return Optional.ofNullable(minReceiveWaitTimeMs);
    }

    public Optional<List<String>> receiveAttributeNames() {
        return Optional.ofNullable(receiveAttributeNames);
    }

    public Optional<List<String>> receiveMessageAttributeNames() {
        return Optional.ofNullable(receiveMessageAttributeNames);
    }

    public Optional<Boolean> adaptivePrefetching() {
        return Optional.ofNullable(adaptivePrefetching);
    }

    public Optional<Boolean> flushOnShutdown() {
        return Optional.ofNullable(flushOnShutdown);
    }

    @Override
    public Builder toBuilder() {
        return new Builder().maxBatchItems(maxBatchItems)
                            .maxBatchKeys(maxBatchKeys)
                            .maxBufferSize(maxBufferSize)
                            .maxBatchOpenInMs(maxBatchOpenInMs)
                            .maxBatchSizeBytes(maxBatchSizeBytes)
                            .visibilityTimeoutSeconds(visibilityTimeoutSeconds)
                            .longPollWaitTimeoutSeconds(longPollWaitTimeoutSeconds)
                            .minReceiveWaitTimeMs(minReceiveWaitTimeMs)
                            .receiveAttributeNames(receiveAttributeNames)
                            .receiveMessageAttributeNames(receiveMessageAttributeNames)
                            .adaptivePrefetching(adaptivePrefetching)
                            .flushOnShutdown(flushOnShutdown);
    }

    @Override
    public String toString() {
        return ToString.builder("BatchOverrideConfiguration")
                       .add("maxBatchItems", maxBatchItems)
                       .add("maxBatchKeys", maxBatchKeys)
                       .add("maxBufferSize", maxBufferSize)
                       .add("maxBatchOpenInMs", maxBatchOpenInMs.toMillis())
                       .add("maxBatchSizeBytes", maxBatchSizeBytes)
                       .add("visibilityTimeoutSeconds", visibilityTimeoutSeconds)
                       .add("longPollWaitTimeoutSeconds", longPollWaitTimeoutSeconds)
                       .add("minReceiveWaitTimeMs", minReceiveWaitTimeMs)
                       .add("receiveAttributeNames", receiveAttributeNames)
                       .add("receiveMessageAttributeNames", receiveMessageAttributeNames)
                       .add("adaptivePrefetching", adaptivePrefetching)
                       .add("flushOnShutdown", flushOnShutdown)
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

        if (maxBatchOpenInMs != null ? !maxBatchOpenInMs.equals(that.maxBatchOpenInMs) : that.maxBatchOpenInMs != null) {
            return false;
        }

        if (maxBatchSizeBytes != null ? !maxBatchSizeBytes.equals(that.maxBatchSizeBytes) : that.maxBatchSizeBytes != null) {
            return false;
        }
        if (visibilityTimeoutSeconds != null ? !visibilityTimeoutSeconds.equals(that.visibilityTimeoutSeconds)
                                             : that.visibilityTimeoutSeconds != null) {
            return false;
        }
        if (longPollWaitTimeoutSeconds != null ? !longPollWaitTimeoutSeconds.equals(that.longPollWaitTimeoutSeconds)
                                               : that.longPollWaitTimeoutSeconds != null) {
            return false;
        }
        if (minReceiveWaitTimeMs != null ? !minReceiveWaitTimeMs.equals(that.minReceiveWaitTimeMs)
                                         : that.minReceiveWaitTimeMs != null) {
            return false;
        }
        if (receiveAttributeNames != null ? !receiveAttributeNames.equals(that.receiveAttributeNames)
                                          : that.receiveAttributeNames != null) {
            return false;
        }
        if (receiveMessageAttributeNames != null ? !receiveMessageAttributeNames.equals(that.receiveMessageAttributeNames)
                                                 : that.receiveMessageAttributeNames != null) {
            return false;
        }
        if (adaptivePrefetching != null ? !adaptivePrefetching.equals(that.adaptivePrefetching)
                                        : that.adaptivePrefetching != null) {
            return false;
        }
        return flushOnShutdown != null ? flushOnShutdown.equals(that.flushOnShutdown) : that.flushOnShutdown == null;
    }

    @Override
    public int hashCode() {
        int result = maxBatchItems != null ? maxBatchItems.hashCode() : 0;
        result = 31 * result + (maxBatchKeys != null ? maxBatchKeys.hashCode() : 0);
        result = 31 * result + (maxBufferSize != null ? maxBufferSize.hashCode() : 0);
        result = 31 * result + (maxBatchOpenInMs != null ? maxBatchOpenInMs.hashCode() : 0);
        result = 31 * result + (maxBatchSizeBytes != null ? maxBatchSizeBytes.hashCode() : 0);
        result = 31 * result + (visibilityTimeoutSeconds != null ? visibilityTimeoutSeconds.hashCode() : 0);
        result = 31 * result + (longPollWaitTimeoutSeconds != null ? longPollWaitTimeoutSeconds.hashCode() : 0);
        result = 31 * result + (minReceiveWaitTimeMs != null ? minReceiveWaitTimeMs.hashCode() : 0);
        result = 31 * result + (receiveAttributeNames != null ? receiveAttributeNames.hashCode() : 0);
        result = 31 * result + (receiveMessageAttributeNames != null ? receiveMessageAttributeNames.hashCode() : 0);
        result = 31 * result + (adaptivePrefetching != null ? adaptivePrefetching.hashCode() : 0);
        result = 31 * result + (flushOnShutdown != null ? flushOnShutdown.hashCode() : 0);
        return result;
    }

    public static final class Builder implements CopyableBuilder<Builder, BatchOverrideConfiguration> {

        private Integer maxBatchItems;
        private Integer maxBatchKeys;
        private Integer maxBufferSize;
        private Duration maxBatchOpenInMs;
        private Long maxBatchSizeBytes;
        private Integer visibilityTimeoutSeconds;
        private Integer longPollWaitTimeoutSeconds;
        private Integer minReceiveWaitTimeMs;
        private List<String> receiveAttributeNames = Collections.emptyList();
        private List<String> receiveMessageAttributeNames = Collections.emptyList();
        private Boolean adaptivePrefetching;
        private Boolean flushOnShutdown;

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


        public Builder maxBatchSizeBytes(Long maxBatchSizeBytes) {
            this.maxBatchSizeBytes = maxBatchSizeBytes;
            return this;
        }

        public Builder visibilityTimeoutSeconds(Integer visibilityTimeoutSeconds) {
            this.visibilityTimeoutSeconds = visibilityTimeoutSeconds;
            return this;
        }

        public Builder longPollWaitTimeoutSeconds(Integer longPollWaitTimeoutSeconds) {
            this.longPollWaitTimeoutSeconds = longPollWaitTimeoutSeconds;
            return this;
        }

        public Builder minReceiveWaitTimeMs(Integer minReceiveWaitTimeMs) {
            this.minReceiveWaitTimeMs = minReceiveWaitTimeMs;
            return this;
        }

        public Builder receiveAttributeNames(List<String> receiveAttributeNames) {
            this.receiveAttributeNames = receiveAttributeNames != null ? receiveAttributeNames
                                                                       : Collections.emptyList();
            return this;
        }

        public Builder receiveMessageAttributeNames(List<String> receiveMessageAttributeNames) {
            this.receiveMessageAttributeNames = receiveMessageAttributeNames != null ? receiveMessageAttributeNames
                                                                                     : Collections.emptyList();
            return this;
        }

        public Builder adaptivePrefetching(Boolean adaptivePrefetching) {
            this.adaptivePrefetching = adaptivePrefetching;
            return this;
        }

        public Builder flushOnShutdown(Boolean flushOnShutdown) {
            this.flushOnShutdown = flushOnShutdown;
            return this;
        }

        public BatchOverrideConfiguration build() {
            return new BatchOverrideConfiguration(this);
        }
    }
}
