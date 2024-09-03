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
import software.amazon.awssdk.services.sqs.batchmanager.BatchOverrideConfiguration;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;

@SdkInternalApi
public final class ResponseBatchConfiguration {

    public static final boolean LONG_POLL_DEFAULT = true;
    public static final Duration VISIBILITY_TIMEOUT_SECONDS_DEFAULT = null;
    public static final Duration LONG_POLL_WAIT_TIMEOUT_DEFAULT = Duration.ofSeconds(20);
    public static final Duration MIN_RECEIVE_WAIT_TIME_MS_DEFAULT = Duration.ofMillis(300);
    public static final List<String> RECEIVE_MESSAGE_ATTRIBUTE_NAMES_DEFAULT = Collections.emptyList();
    public static final List<MessageSystemAttributeName> MESSAGE_SYSTEM_ATTRIBUTE_NAMES_DEFAULT = Collections.emptyList();
    public static final boolean ADAPTIVE_PREFETCHING_DEFAULT = false;
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
    private final Duration longPollWaitTimeout;
    private final Duration minReceiveWaitTime;
    private final List<MessageSystemAttributeName> messageSystemAttributeValues;
    private final List<String> receiveMessageAttributeNames;
    private final Boolean adaptivePrefetching;
    private final Integer maxBatchItems;
    private final Integer maxInflightReceiveBatches;
    private final Integer maxDoneReceiveBatches;

    public ResponseBatchConfiguration(BatchOverrideConfiguration overrideConfiguration) {
        this.visibilityTimeout = overrideConfiguration != null && overrideConfiguration.visibilityTimeout() != null
                                 ? overrideConfiguration.visibilityTimeout()
                                 : VISIBILITY_TIMEOUT_SECONDS_DEFAULT;

        this.longPollWaitTimeout = overrideConfiguration != null && overrideConfiguration.longPollWaitTimeout() != null
                                   ? overrideConfiguration.longPollWaitTimeout()
                                   : LONG_POLL_WAIT_TIMEOUT_DEFAULT;

        this.minReceiveWaitTime = overrideConfiguration != null && overrideConfiguration.minReceiveWaitTime() != null
                                  ? overrideConfiguration.minReceiveWaitTime()
                                  : MIN_RECEIVE_WAIT_TIME_MS_DEFAULT;

        this.messageSystemAttributeValues = overrideConfiguration != null
                                            && overrideConfiguration.messageSystemAttributeName() != null
                                            && !overrideConfiguration.messageSystemAttributeName().isEmpty()
                                            ? overrideConfiguration.messageSystemAttributeName()
                                            : MESSAGE_SYSTEM_ATTRIBUTE_NAMES_DEFAULT;

        this.receiveMessageAttributeNames = overrideConfiguration != null
                                            && overrideConfiguration.receiveMessageAttributeNames() != null
                                            && !overrideConfiguration.receiveMessageAttributeNames().isEmpty()
                                            ? overrideConfiguration.receiveMessageAttributeNames()
                                            : RECEIVE_MESSAGE_ATTRIBUTE_NAMES_DEFAULT;

        this.adaptivePrefetching = overrideConfiguration != null && overrideConfiguration.adaptivePrefetching() != null
                                   ? overrideConfiguration.adaptivePrefetching()
                                   : ADAPTIVE_PREFETCHING_DEFAULT;

        this.maxBatchItems = overrideConfiguration != null && overrideConfiguration.maxBatchItems() != null
                             ? overrideConfiguration.maxBatchItems()
                             : MAX_SUPPORTED_SQS_RECEIVE_MSG;

        this.maxInflightReceiveBatches = overrideConfiguration != null
                                         && overrideConfiguration.maxInflightReceiveBatches() != null
                                         ? overrideConfiguration.maxInflightReceiveBatches()
                                         : MAX_INFLIGHT_RECEIVE_BATCHES_DEFAULT;

        this.maxDoneReceiveBatches = overrideConfiguration != null && overrideConfiguration.maxDoneReceiveBatches() != null
                                     ? overrideConfiguration.maxDoneReceiveBatches()
                                     : MAX_DONE_RECEIVE_BATCHES_DEFAULT;
    }



    public Duration visibilityTimeout() {
        return visibilityTimeout;
    }

    public Duration longPollWaitTimeout() {
        return longPollWaitTimeout;
    }

    public Duration minReceiveWaitTime() {
        return minReceiveWaitTime;
    }

    public List<MessageSystemAttributeName> messageSystemAttributeNames() {
        return Collections.unmodifiableList(messageSystemAttributeValues);
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
}