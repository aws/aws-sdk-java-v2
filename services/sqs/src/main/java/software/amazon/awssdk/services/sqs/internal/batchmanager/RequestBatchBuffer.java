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
import java.util.concurrent.ScheduledFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Main facade class that coordinates the batch buffer functionality.
 */
@SdkInternalApi
public final class RequestBatchBuffer<RequestT, ResponseT> {
    private final RequestBatchStorage<RequestT, ResponseT> requestBatchStorage;
    private final FlushPolicy<RequestT> flushPolicy;
    private final FlushScheduler flushScheduler;
    private final BatchEntryIdGenerator idGenerator;

    private RequestBatchBuffer(Builder<RequestT, ResponseT> builder) {
        this.requestBatchStorage = new RequestBatchStorage<>(builder.maxBufferSize);
        this.flushPolicy = new FlushPolicy<>(builder.maxBatchItems, builder.maxBatchSizeInBytes);
        this.flushScheduler = new FlushScheduler(builder.scheduledFlush);
        this.idGenerator = new BatchEntryIdGenerator();
    }

    public static <RequestT, ResponseT> Builder<RequestT, ResponseT> builder() {
        return new Builder<>();
    }

    /**
     * Returns entries that should be flushed before adding a new request based on byte size constraints.
     */
    public Map<String, BatchingExecutionContext<RequestT, ResponseT>> getFlushableBatchIfSizeExceeded(RequestT request) {
        requestBatchStorage.getLock().lock();
        try {
            if (flushPolicy.shouldFlushBeforeAdd(requestBatchStorage.getAllEntries(), request)) {
                return requestBatchStorage.extractEntries(flushPolicy.getMaxBatchItems(), idGenerator);
            }
            return Collections.emptyMap();
        } finally {
            requestBatchStorage.getLock().unlock();
        }
    }

    /**
     * Returns entries that should be flushed due to scheduled flush.
     */
    public Map<String, BatchingExecutionContext<RequestT, ResponseT>> extractEntriesForScheduledFlush(int maxBatchItems) {
        requestBatchStorage.getLock().lock();
        try {
            if (!requestBatchStorage.isEmpty()) {
                return requestBatchStorage.extractEntries(maxBatchItems, idGenerator);
            }
            return Collections.emptyMap();
        } finally {
            requestBatchStorage.getLock().unlock();
        }
    }

    /**
     * Returns entries that should be flushed based on current buffer state.
     */
    public Map<String, BatchingExecutionContext<RequestT, ResponseT>> extractBatchIfNeeded() {
        requestBatchStorage.getLock().lock();
        try {
            if (flushPolicy.shouldFlush(requestBatchStorage.getAllEntries())) {
                return requestBatchStorage.extractEntries(flushPolicy.getMaxBatchItems(), idGenerator);
            }
            return Collections.emptyMap();
        } finally {
            requestBatchStorage.getLock().unlock();
        }
    }


    /**
     * Adds a request to the buffer.
     */
    public void put(RequestT request, CompletableFuture<ResponseT> response) {
        String id = idGenerator.nextId();
        requestBatchStorage.put(id, new BatchingExecutionContext<>(request, response));
    }

    /**
     * Updates the scheduled flush task.
     */
    public void putScheduledFlush(ScheduledFuture<?> scheduledFlush) {
        flushScheduler.updateScheduledFlush(scheduledFlush);
    }

    /**
     * Cancels the scheduled flush task.
     */
    public void cancelScheduledFlush() {
        flushScheduler.cancelScheduledFlush();
    }

    /**
     * Returns all response futures in the buffer.
     */
    public Collection<CompletableFuture<ResponseT>> responses() {
        return requestBatchStorage.getAllResponses();
    }

    /**
     * Clears all entries from the buffer.
     */
    public void clear() {
        requestBatchStorage.clear();
    }

    /**
     * Builder for RequestBatchBuffer.
     */
    public static final class Builder<RequestT, ResponseT> {
        private ScheduledFuture<?> scheduledFlush;
        private int maxBatchItems;
        private int maxBatchSizeInBytes;
        private int maxBufferSize;

        public Builder<RequestT, ResponseT> scheduledFlush(ScheduledFuture<?> scheduledFlush) {
            this.scheduledFlush = scheduledFlush;
            return this;
        }

        public Builder<RequestT, ResponseT> maxBatchItems(int maxBatchItems) {
            this.maxBatchItems = maxBatchItems;
            return this;
        }

        public Builder<RequestT, ResponseT> maxBatchSizeInBytes(int maxBatchSizeInBytes) {
            this.maxBatchSizeInBytes = maxBatchSizeInBytes;
            return this;
        }

        public Builder<RequestT, ResponseT> maxBufferSize(int maxBufferSize) {
            this.maxBufferSize = maxBufferSize;
            return this;
        }

        public RequestBatchBuffer<RequestT, ResponseT> build() {
            return new RequestBatchBuffer<>(this);
        }
    }
}
