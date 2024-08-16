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
    public static final int DEFAULT_MAX_BATCH_KEYS = 100;
    public static final int DEFAULT_MAX_BUFFER_SIZE = 500;
    public static final Duration DEFAULT_MAX_BATCH_OPEN_IN_MS = Duration.ofMillis(200);

    private final Integer maxBatchItems;
    private final Integer maxBatchKeys;
    private final Integer maxBufferSize;
    private final Duration maxBatchOpenDuration;

    public RequestBatchConfiguration(BatchOverrideConfiguration overrideConfiguration) {
        if (overrideConfiguration == null) {
            this.maxBatchItems = DEFAULT_MAX_BATCH_ITEMS;
            this.maxBatchKeys = DEFAULT_MAX_BATCH_KEYS;
            this.maxBufferSize = DEFAULT_MAX_BUFFER_SIZE;
            this.maxBatchOpenDuration = DEFAULT_MAX_BATCH_OPEN_IN_MS;
        } else {
            this.maxBatchItems = overrideConfiguration.maxBatchItems() != null
                                 ? overrideConfiguration.maxBatchItems()
                                 : DEFAULT_MAX_BATCH_ITEMS;
            this.maxBatchKeys = overrideConfiguration.maxBatchKeys() != null
                                ? overrideConfiguration.maxBatchKeys()
                                : DEFAULT_MAX_BATCH_KEYS;
            this.maxBufferSize = overrideConfiguration.maxBufferSize() != null
                                 ? overrideConfiguration.maxBufferSize()
                                 : DEFAULT_MAX_BUFFER_SIZE;
            this.maxBatchOpenDuration = overrideConfiguration.maxBatchOpenDuration() != null
                                        ? overrideConfiguration.maxBatchOpenDuration()
                                        : DEFAULT_MAX_BATCH_OPEN_IN_MS;
        }
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
}
