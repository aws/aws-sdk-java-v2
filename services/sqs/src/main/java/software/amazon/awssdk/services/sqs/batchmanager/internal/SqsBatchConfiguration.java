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

package software.amazon.awssdk.services.sqs.batchmanager.internal;

import java.time.Duration;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.batchmanager.BatchOverrideConfiguration;

@SdkInternalApi
public final class SqsBatchConfiguration {
    private static final int DEFAULT_MAX_BATCH_ITEMS = 10;
    private static final Duration DEFAULT_MAX_BATCH_OPEN_IN_MS = Duration.ofMillis(200);
    private final Integer maxBatchItems;
    private final Duration maxBatchOpenInMs;

    public SqsBatchConfiguration(BatchOverrideConfiguration overrideConfiguration) {
        if (overrideConfiguration == null) {
            this.maxBatchItems = DEFAULT_MAX_BATCH_ITEMS;
            this.maxBatchOpenInMs = DEFAULT_MAX_BATCH_OPEN_IN_MS;
        } else {
            this.maxBatchItems = overrideConfiguration.maxBatchItems().orElse(DEFAULT_MAX_BATCH_ITEMS);
            this.maxBatchOpenInMs = overrideConfiguration.maxBatchOpenInMs().orElse(DEFAULT_MAX_BATCH_OPEN_IN_MS);
        }
    }

    public Duration maxBatchOpenInMs() {
        return maxBatchOpenInMs;
    }

    public int maxBatchItems() {
        return maxBatchItems;
    }
}
