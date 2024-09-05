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
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;

@SdkInternalApi
public final class ResponseBatchConfiguration {

    public static final Duration VISIBILITY_TIMEOUT_SECONDS_DEFAULT = null;
    public static final Duration MIN_RECEIVE_WAIT_TIME_MS_DEFAULT = Duration.ofMillis(50);
    public static final List<String> RECEIVE_MESSAGE_ATTRIBUTE_NAMES_DEFAULT = Collections.emptyList();
    public static final List<MessageSystemAttributeName> MESSAGE_SYSTEM_ATTRIBUTE_NAMES_DEFAULT = Collections.emptyList();
    public static final boolean ADAPTIVE_PREFETCHING_DEFAULT = true;
    public static final int MAX_INFLIGHT_RECEIVE_BATCHES_DEFAULT = 10;
    public static final int MAX_DONE_RECEIVE_BATCHES_DEFAULT = 10;

    public static final int MAX_SUPPORTED_SQS_RECEIVE_MSG = 10;

    public static final int MAX_SEND_MESSAGE_PAYLOAD_SIZE_BYTES = 262_144; // 256 KiB

    /**
     * <a href="https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-message-metadata.html#sqs-message-attributes">
     * AWS SQS Message Attributes Documentation</a>
     *
     * Rounding up max payload due to attribute maps.
     * This was not done in V1, thus an issue was reported where batch messages failed with payload size exceeding the maximum.
     */
    public static final int ATTRIBUTE_MAPS_PAYLOAD_BYTES = 16 * 1024; // 16 KiB

    private final Duration visibilityTimeout;
    private final Duration minReceiveWaitTime;
    private final List<MessageSystemAttributeName> messageSystemAttributeNames;
    private final List<String> receiveMessageAttributeNames;
    private final Boolean adaptivePrefetching;
    private final Integer maxBatchItems;
    private final Integer maxInflightReceiveBatches;
    private final Integer maxDoneReceiveBatches;

    private ResponseBatchConfiguration(Builder builder) {
        this.visibilityTimeout = builder.visibilityTimeout != null
                                 ? builder.visibilityTimeout
                                 : VISIBILITY_TIMEOUT_SECONDS_DEFAULT;

        this.minReceiveWaitTime = builder.minReceiveWaitTime != null
                                  ? builder.minReceiveWaitTime
                                  : MIN_RECEIVE_WAIT_TIME_MS_DEFAULT;

        this.messageSystemAttributeNames = builder.messageSystemAttributeNames != null
                                            ? builder.messageSystemAttributeNames
                                            : MESSAGE_SYSTEM_ATTRIBUTE_NAMES_DEFAULT;

        this.receiveMessageAttributeNames = builder.receiveMessageAttributeNames != null
                                            ? builder.receiveMessageAttributeNames
                                            : RECEIVE_MESSAGE_ATTRIBUTE_NAMES_DEFAULT;

        this.adaptivePrefetching = builder.adaptivePrefetching != null
                                   ? builder.adaptivePrefetching
                                   : ADAPTIVE_PREFETCHING_DEFAULT;

        this.maxBatchItems = builder.maxBatchItems != null
                             ? builder.maxBatchItems
                             : MAX_SUPPORTED_SQS_RECEIVE_MSG;

        this.maxInflightReceiveBatches = builder.maxInflightReceiveBatches != null
                                         ? builder.maxInflightReceiveBatches
                                         : MAX_INFLIGHT_RECEIVE_BATCHES_DEFAULT;

        this.maxDoneReceiveBatches = builder.maxDoneReceiveBatches != null
                                     ? builder.maxDoneReceiveBatches
                                     : MAX_DONE_RECEIVE_BATCHES_DEFAULT;
    }


    public Duration visibilityTimeout() {
        return visibilityTimeout;
    }

    public Duration minReceiveWaitTime() {
        return minReceiveWaitTime;
    }

    public List<MessageSystemAttributeName> messageSystemAttributeNames() {
        return Collections.unmodifiableList(messageSystemAttributeNames);
    }

    public List<String> receiveMessageAttributeNames() {
        return Collections.unmodifiableList(receiveMessageAttributeNames);
    }

    public boolean adaptivePrefetching() {
        return adaptivePrefetching;
    }

    public int maxBatchItems() {
        return maxBatchItems;
    }

    public int maxInflightReceiveBatches() {
        return maxInflightReceiveBatches;
    }

    public int maxDoneReceiveBatches() {
        return maxDoneReceiveBatches;
    }

    public static Builder builder() {
        return new Builder();
    }


    public static class Builder {
        private Duration visibilityTimeout;
        private Duration minReceiveWaitTime;
        private List<MessageSystemAttributeName> messageSystemAttributeNames;
        private List<String> receiveMessageAttributeNames;
        private Boolean adaptivePrefetching;
        private Integer maxBatchItems;
        private Integer maxInflightReceiveBatches;
        private Integer maxDoneReceiveBatches;

        public Builder visibilityTimeout(Duration visibilityTimeout) {
            this.visibilityTimeout = visibilityTimeout;
            return this;
        }

        public Builder minReceiveWaitTime(Duration minReceiveWaitTime) {
            this.minReceiveWaitTime = minReceiveWaitTime;
            return this;
        }

        public Builder messageSystemAttributeNames(List<MessageSystemAttributeName> messageSystemAttributeNames) {
            this.messageSystemAttributeNames = messageSystemAttributeNames;
            return this;
        }

        public Builder receiveMessageAttributeNames(List<String> receiveMessageAttributeNames) {
            this.receiveMessageAttributeNames = receiveMessageAttributeNames;
            return this;
        }

        public Builder adaptivePrefetching(Boolean adaptivePrefetching) {
            this.adaptivePrefetching = adaptivePrefetching;
            return this;
        }

        public Builder maxBatchItems(Integer maxBatchItems) {
            this.maxBatchItems = maxBatchItems;
            return this;
        }

        public Builder maxInflightReceiveBatches(Integer maxInflightReceiveBatches) {
            this.maxInflightReceiveBatches = maxInflightReceiveBatches;
            return this;
        }

        public Builder maxDoneReceiveBatches(Integer maxDoneReceiveBatches) {
            this.maxDoneReceiveBatches = maxDoneReceiveBatches;
            return this;
        }

        public ResponseBatchConfiguration build() {
            return new ResponseBatchConfiguration(this);
        }
    }
}