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


import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Determines when a batch should be flushed based on various criteria.
 */
@SdkInternalApi
class FlushPolicy<RequestT> {
    private final int maxBatchItems;
    private final int maxBatchSizeInBytes;

    FlushPolicy(int maxBatchItems, int maxBatchSizeInBytes) {
        this.maxBatchItems = maxBatchItems;
        this.maxBatchSizeInBytes = maxBatchSizeInBytes;
    }

    public int getMaxBatchItems() {
        return maxBatchItems;
    }

    // Updated method signature to use the same generic types
    public <ResponseT> boolean shouldFlush(Map<String, BatchingExecutionContext<RequestT, ResponseT>> entries) {
        return isBatchSizeLimitReached(entries) || isByteSizeThresholdCrossed(entries, 0);
    }

    // Updated method signature to use the same generic types
    public <ResponseT> boolean shouldFlushBeforeAdd(
        Map<String, BatchingExecutionContext<RequestT, ResponseT>> entries,
        RequestT incomingRequest) {
        if (maxBatchSizeInBytes > 0 && !entries.isEmpty()) {
            int incomingRequestBytes = RequestPayloadCalculator.calculateMessageSize(incomingRequest).orElse(0);
            return isByteSizeThresholdCrossed(entries, incomingRequestBytes);
        }
        return false;
    }

    // Updated method signature to use the same generic types
    private <ResponseT> boolean isBatchSizeLimitReached(
        Map<String, BatchingExecutionContext<RequestT, ResponseT>> entries) {
        return entries.size() >= maxBatchItems;
    }

    // Updated method signature to use the same generic types
    private <ResponseT> boolean isByteSizeThresholdCrossed(
        Map<String, BatchingExecutionContext<RequestT, ResponseT>> entries,
        int additionalBytes) {
        if (maxBatchSizeInBytes < 0) {
            return false;
        }

        int totalPayloadSize = entries.values().stream()
                                      .map(BatchingExecutionContext::responsePayloadByteSize)
                                      .mapToInt(opt -> opt.orElse(0))
                                      .sum() + additionalBytes;

        return totalPayloadSize > maxBatchSizeInBytes;
    }
}
