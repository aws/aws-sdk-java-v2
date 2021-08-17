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

package software.amazon.awssdk.core.internal.batchmanager;

import java.time.Duration;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.batchmanager.BatchOverrideConfiguration;

@SdkInternalApi
public final class BatchConfiguration {

    // TODO: Update these default values.
    private static final int DEFAULT_MAX_BATCH_ITEMS = 5;
    private static final int DEFAULT_MAX_BATCH_KEYS = 100;
    private static final int DEFAULT_MAX_BUFFER_SIZE = 500;
    private static final Duration DEFAULT_MAX_BATCH_OPEN_IN_MS = Duration.ofMillis(200);

    private final Integer maxBatchItems;
    private final Integer maxBatchKeys;
    private final Integer maxBufferSize;
    private final Duration maxBatchOpenInMs;

    public BatchConfiguration(BatchOverrideConfiguration overrideConfiguration) {
        Optional<BatchOverrideConfiguration> configuration = Optional.ofNullable(overrideConfiguration);
        this.maxBatchItems = configuration.flatMap(BatchOverrideConfiguration::maxBatchItems).orElse(DEFAULT_MAX_BATCH_ITEMS);
        this.maxBatchKeys = configuration.flatMap(BatchOverrideConfiguration::maxBatchKeys).orElse(DEFAULT_MAX_BATCH_KEYS);
        this.maxBufferSize = configuration.flatMap(BatchOverrideConfiguration::maxBufferSize).orElse(DEFAULT_MAX_BUFFER_SIZE);
        this.maxBatchOpenInMs = configuration.flatMap(BatchOverrideConfiguration::maxBatchOpenInMs)
                                             .orElse(DEFAULT_MAX_BATCH_OPEN_IN_MS);
    }

    public Duration maxBatchOpenInMs() {
        return maxBatchOpenInMs;
    }

    public int maxBatchItems() {
        return maxBatchItems;
    }

    public int getMaxBatchKeys() {
        return maxBatchKeys;
    }

    public int getMaxBufferSize() {
        return maxBufferSize;
    }
}
