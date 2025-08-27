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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Responsible for storing batch entries and providing operations to access and extract them.
 */
@SdkInternalApi
class RequestBatchStorage<RequestT, ResponseT> {
    private final Map<String, BatchingExecutionContext<RequestT, ResponseT>> idToBatchContext;
    private final int maxBufferSize;
    private final ReentrantLock lock = new ReentrantLock();

    RequestBatchStorage(int maxBufferSize) {
        this.idToBatchContext = new ConcurrentHashMap<>();
        this.maxBufferSize = maxBufferSize;
    }

    public ReentrantLock getLock() {
        return lock;
    }

    public void put(String id, BatchingExecutionContext<RequestT, ResponseT> context) {
        lock.lock();
        try {
            if (idToBatchContext.size() == maxBufferSize) {
                throw new IllegalStateException("Reached MaxBufferSize of: " + maxBufferSize);
            }
            idToBatchContext.put(id, context);
        } finally {
            lock.unlock();
        }
    }

    public Map<String, BatchingExecutionContext<RequestT, ResponseT>> getAllEntries() {
        // No need for locking here as we're returning an unmodifiable view
        return Collections.unmodifiableMap(idToBatchContext);
    }

    public boolean isEmpty() {
        // ConcurrentHashMap's isEmpty is thread-safe
        return idToBatchContext.isEmpty();
    }

    public Map<String, BatchingExecutionContext<RequestT, ResponseT>> extractEntries(int maxEntries,
                                                                                     BatchEntryIdGenerator idGenerator) {
        Map<String, BatchingExecutionContext<RequestT, ResponseT>> extractedEntries =
            new ConcurrentHashMap<>(Math.min(maxEntries, idToBatchContext.size()));

        String nextEntry;
        int count = 0;

        while (count < maxEntries && idGenerator.hasNextBatchEntry(idToBatchContext)) {
            nextEntry = idGenerator.nextBatchEntry();
            BatchingExecutionContext<RequestT, ResponseT> context = idToBatchContext.get(nextEntry);
            if (context != null) {
                extractedEntries.put(nextEntry, context);
                idToBatchContext.remove(nextEntry);
                count++;
            }
        }

        return extractedEntries;
    }

    public Collection<CompletableFuture<ResponseT>> getAllResponses() {
        // Using ConcurrentHashMap's thread-safe iteration
        return idToBatchContext.values()
                               .stream()
                               .map(BatchingExecutionContext::response)
                               .collect(Collectors.toList());
    }

    public void clear() {
        lock.lock();
        try {
            idToBatchContext.clear();
        } finally {
            lock.unlock();
        }
    }
}
