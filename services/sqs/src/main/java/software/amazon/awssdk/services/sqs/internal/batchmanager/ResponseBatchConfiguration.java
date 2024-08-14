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
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.sqs.batchmanager.BatchOverrideConfiguration;

@SdkInternalApi
public final class ResponseBatchConfiguration {

    public static final boolean LONG_POLL_DEFAULT = true;
    public static final Duration VISIBILITY_TIMEOUT_SECONDS_DEFAULT = null;
    public static final Duration LONG_POLL_WAIT_TIMEOUT_DEFAULT = Duration.ofSeconds(20);
    public static final Duration MIN_RECEIVE_WAIT_TIME_MS_DEFAULT = Duration.ofMillis(50);
    public static final List<String> RECEIVE_MESSAGE_ATTRIBUTE_NAMES_DEFAULT = Collections.emptyList();
    public static final List<String> RECEIVE_ATTRIBUTE_NAMES_DEFAULT = Collections.emptyList();
    public static final boolean ADAPTIVE_PREFETCHING_DEFAULT = false;
    public static final int MAX_BATCH_ITEMS_DEFAULT = 10;
    public static final int MAX_INFLIGHT_RECEIVE_BATCHES_DEFAULT = 10;
    public static final int MAX_DONE_RECEIVE_BATCHES_DEFAULT = 10;

    private final Duration visibilityTimeout;
    private final Duration longPollWaitTimeout;
    private final Duration minReceiveWaitTime;
    private final List<String> receiveAttributeNames;
    private final List<String> receiveMessageAttributeNames;
    private final Boolean adaptivePrefetching;
    private final Integer maxBatchItems;
    private final Integer maxInflightReceiveBatches;
    private final Integer maxDoneReceiveBatches;

    public ResponseBatchConfiguration(BatchOverrideConfiguration overrideConfiguration) {
        Optional<BatchOverrideConfiguration> configuration = Optional.ofNullable(overrideConfiguration);
        this.visibilityTimeout = configuration.flatMap(BatchOverrideConfiguration::visibilityTimeout)
                                              .orElse(VISIBILITY_TIMEOUT_SECONDS_DEFAULT);
        this.longPollWaitTimeout = configuration.flatMap(BatchOverrideConfiguration::longPollWaitTimeout)
                                                .orElse(LONG_POLL_WAIT_TIMEOUT_DEFAULT);
        this.minReceiveWaitTime = configuration.flatMap(BatchOverrideConfiguration::minReceiveWaitTime)
                                               .orElse(MIN_RECEIVE_WAIT_TIME_MS_DEFAULT);
        this.receiveAttributeNames = configuration.flatMap(BatchOverrideConfiguration::receiveAttributeNames)
                                                  .orElse(RECEIVE_ATTRIBUTE_NAMES_DEFAULT);
        this.receiveMessageAttributeNames = configuration.flatMap(BatchOverrideConfiguration::receiveMessageAttributeNames)
                                                         .orElse(RECEIVE_MESSAGE_ATTRIBUTE_NAMES_DEFAULT);
        this.adaptivePrefetching = configuration.flatMap(BatchOverrideConfiguration::adaptivePrefetching)
                                                .orElse(ADAPTIVE_PREFETCHING_DEFAULT);
        this.maxBatchItems = configuration.flatMap(BatchOverrideConfiguration::maxBatchItems)
                                          .orElse(MAX_BATCH_ITEMS_DEFAULT);
        this.maxInflightReceiveBatches = configuration.flatMap(BatchOverrideConfiguration::maxInflightReceiveBatches)
                                                      .orElse(MAX_INFLIGHT_RECEIVE_BATCHES_DEFAULT);
        this.maxDoneReceiveBatches = configuration.flatMap(BatchOverrideConfiguration::maxDoneReceiveBatches)
                                                  .orElse(MAX_DONE_RECEIVE_BATCHES_DEFAULT);
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

    public List<String> receiveAttributeNames() {
        return receiveAttributeNames;
    }

    public List<String> receiveMessageAttributeNames() {
        return receiveMessageAttributeNames;
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