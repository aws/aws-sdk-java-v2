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

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Outer map maps a batchKey (ex. queueUrl, overrideConfig etc.) to a {@link RequestBatchBuffer}
 *
 * @param <RequestT> the type of an outgoing response
 */
@SdkInternalApi
public final class BatchingMap<RequestT, ResponseT> {

    private final int maxBatchKeys;
    private final int maxBatchBytesSize;
    private final int maxBatchSize;
    private final int maxBufferSize;
    private final ConcurrentMap<String, RequestBatchBuffer<RequestT, ResponseT>> batchContextMap;
    private final AtomicInteger keyCount = new AtomicInteger();

    public BatchingMap(RequestBatchConfiguration overrideConfiguration) {
        this.batchContextMap = new ConcurrentHashMap<>();
        this.maxBatchKeys = overrideConfiguration.maxBatchKeys();
        this.maxBatchBytesSize = overrideConfiguration.maxBatchBytesSize();
        this.maxBatchSize = overrideConfiguration.maxBatchItems();
        this.maxBufferSize = overrideConfiguration.maxBufferSize();
    }

    public void put(String batchKey, Supplier<ScheduledFuture<?>> scheduleFlush, RequestT request,
                    CompletableFuture<ResponseT> response) throws IllegalStateException {
        // The put runs inside the per-key compute, so it is serialized with evictIfEmpty for the same key.
        batchContextMap.compute(batchKey, (k, buffer) -> {
            if (buffer != null) {
                buffer.put(request, response);
                return buffer;
            }
            reserveKeySlot();
            RequestBatchBuffer<RequestT, ResponseT> newBuffer = null;
            try {
                newBuffer = new RequestBatchBuffer<>(scheduleFlush.get(), maxBatchSize, maxBatchBytesSize, maxBufferSize);
                newBuffer.put(request, response);
                return newBuffer;
            } catch (RuntimeException e) {
                if (newBuffer != null) {
                    newBuffer.cancelScheduledFlush();
                }
                keyCount.decrementAndGet();
                throw e;
            }
        });
    }

    private void reserveKeySlot() {
        int previous = keyCount.getAndUpdate(c -> c < maxBatchKeys ? c + 1 : c);
        if (previous >= maxBatchKeys) {
            throw new IllegalStateException("Reached MaxBatchKeys of: " + maxBatchKeys);
        }
    }

    /**
     * Removes the buffer for the given key and cancels its scheduled flush if the buffer holds no requests.
     */
    public void evictIfEmpty(String batchKey) {
        batchContextMap.computeIfPresent(batchKey, (k, buffer) -> {
            if (buffer.isEmpty()) {
                buffer.cancelScheduledFlush();
                keyCount.decrementAndGet();
                return null;
            }
            return buffer;
        });
    }

    public boolean contains(String batchKey) {
        return batchContextMap.containsKey(batchKey);
    }

    public void cancelAndReplaceScheduledFlush(String batchKey, Supplier<ScheduledFuture<?>> scheduleFlush) {
        batchContextMap.computeIfPresent(batchKey, (k, buffer) -> {
            buffer.cancelAndReplaceScheduledFlush(scheduleFlush.get());
            return buffer;
        });
    }

    public void forEach(BiConsumer<String, RequestBatchBuffer<RequestT, ResponseT>> action) {
        batchContextMap.forEach(action);
    }

    public Map<String, BatchingExecutionContext<RequestT, ResponseT>> extractBatchIfReady(String batchKey) {
        RequestBatchBuffer<RequestT, ResponseT> buffer = batchContextMap.get(batchKey);
        return buffer == null ? Collections.emptyMap() : buffer.extractBatchIfReady();
    }

    public Map<String, BatchingExecutionContext<RequestT, ResponseT>> extractBatchIfSizeExceeded(String batchKey,
                                                                                                            RequestT request) {
        RequestBatchBuffer<RequestT, ResponseT> buffer = batchContextMap.get(batchKey);
        return buffer == null ? Collections.emptyMap() : buffer.extractBatchIfSizeExceeded(request);
    }

    public Map<String, BatchingExecutionContext<RequestT, ResponseT>> extractEntriesForScheduledFlush(String batchKey,
                                                                                                 int maxBatchItems) {
        RequestBatchBuffer<RequestT, ResponseT> buffer = batchContextMap.get(batchKey);
        return buffer == null ? Collections.emptyMap() : buffer.extractEntriesForScheduledFlush(maxBatchItems);
    }

    public void cancelScheduledFlush(String batchKey) {
        RequestBatchBuffer<RequestT, ResponseT> buffer = batchContextMap.get(batchKey);
        if (buffer != null) {
            buffer.cancelScheduledFlush();
        }
    }

    public void clear() {
        for (Map.Entry<String, RequestBatchBuffer<RequestT, ResponseT>> entry : batchContextMap.entrySet()) {
            RequestBatchBuffer<RequestT, ResponseT> buffer = entry.getValue();
            if (batchContextMap.remove(entry.getKey(), buffer)) {
                buffer.cancelScheduledFlush();
                buffer.clear();
                keyCount.decrementAndGet();
            }
        }
    }
}
