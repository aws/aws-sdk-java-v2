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
import java.util.concurrent.locks.ReentrantLock;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Manages the generation of unique IDs for batch entries.
 */
@SdkInternalApi
class BatchEntryIdGenerator {
    private int nextId = 0;
    private int nextBatchEntry = 0;
    private final ReentrantLock idLock = new ReentrantLock();

    public String nextId() {
        idLock.lock();
        try {
            if (nextId == Integer.MAX_VALUE) {
                nextId = 0;
            }
            return Integer.toString(nextId++);
        } finally {
            idLock.unlock();
        }
    }

    public boolean hasNextBatchEntry(Map<String, ?> contextMap) {
        return contextMap.containsKey(Integer.toString(nextBatchEntry));
    }

    public String nextBatchEntry() {
        idLock.lock();
        try {
            if (nextBatchEntry == Integer.MAX_VALUE) {
                nextBatchEntry = 0;
            }
            return Integer.toString(nextBatchEntry++);
        } finally {
            idLock.unlock();
        }
    }
}
