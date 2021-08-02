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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Logger;

@SdkInternalApi
public final class BatchBuffer<RequestT, ResponseT> {
    private final Object flushLock = new Object();
    private static final Logger log = Logger.loggerFor(BatchBuffer.class);

    private final Map<String, BatchingExecutionContext<RequestT, ResponseT>> idToBatchContext;

    /**
     * Batch entries in a batch request require a unique ID so nextId keeps track of the ID to assign to the next
     * BatchingExecutionContext. For simplicity, the ID is just an integer that is incremented everytime a new request and
     * response pair is received.
     */
    private final AtomicInteger nextId;

    /**
     * Keeps track of the ID of the next entry to be added in a batch request. This ID does not necessarily correlate to a
     * request that already exists in the idToBatchContext map since it refers to the next entry (ex. if the last entry added
     * to idToBatchContext had an id of 22, nextBatchEntry will have a value of 23).
     */
    private final AtomicInteger nextBatchEntry;

    /**
     * The scheduled flush tasks associated with this batchBuffer.
     */
    private ScheduledFuture<?> scheduledFlush;

    public BatchBuffer(ScheduledFuture<?> scheduledFlush) {
        this.idToBatchContext = new ConcurrentHashMap<>();
        this.nextId = new AtomicInteger(0);
        this.nextBatchEntry = new AtomicInteger(0);
        this.scheduledFlush = scheduledFlush;
    }

    public LinkedHashMap<String, BatchingExecutionContext<RequestT, ResponseT>> canManualFlush(int maxBatchItems) {
        synchronized (flushLock) {
            if (idToBatchContext.size() >= maxBatchItems) {
                return extractFlushedEntries(maxBatchItems);
            }
            return null;
        }
    }

    public LinkedHashMap<String, BatchingExecutionContext<RequestT, ResponseT>> canScheduledFlush(int maxBatchItems) {
        synchronized (flushLock) {
            if (idToBatchContext.size() > 0) {
                return extractFlushedEntries(maxBatchItems);
            }
            return null;
        }
    }

    private LinkedHashMap<String, BatchingExecutionContext<RequestT, ResponseT>> extractFlushedEntries(int maxBatchItems) {
        LinkedHashMap<String, BatchingExecutionContext<RequestT, ResponseT>> requestEntries = new LinkedHashMap<>();
        String nextEntry;
        while (requestEntries.size() < maxBatchItems && (nextEntry = nextBatchEntry()) != null) {
            requestEntries.put(nextEntry, idToBatchContext.get(nextEntry));
            idToBatchContext.remove(nextEntry);
        }
        return requestEntries;
    }

    public RequestT getRequest(String key) {
        return idToBatchContext.get(key).request();
    }

    public CompletableFuture<ResponseT> getResponse(String key) {
        return idToBatchContext.get(key).response();
    }

    // TODO: Needs to be in a lock to maintain insertion order. Not sure if there is any other way to accomplish this. I tried to
    //  do this in a do while loop but it ended up being the same problem as before.
    public BatchingExecutionContext<RequestT, ResponseT> put(RequestT request, CompletableFuture<ResponseT> response) {
        synchronized (this) {
            String id = BatchUtils.getAndIncrementId(nextId);
            log.warn(() -> "Putting ID: " + id + ". From Thread: " + Thread.currentThread().getId());
            return idToBatchContext.put(id, new BatchingExecutionContext<>(request, response));
        }
    }

    private String nextBatchEntry() {
        int currentNextBatchEntry;
        int newNextBatchEntry;
        do {
            currentNextBatchEntry = nextBatchEntry.get();
            newNextBatchEntry = currentNextBatchEntry + 1;
            if (!idToBatchContext.containsKey(Integer.toString(currentNextBatchEntry))) {
                newNextBatchEntry = currentNextBatchEntry;
            }
        } while (!nextBatchEntry.compareAndSet(currentNextBatchEntry, newNextBatchEntry));

        if (currentNextBatchEntry != newNextBatchEntry) {
            return Integer.toString(currentNextBatchEntry);
        }
        // TODO: Debugging
        int finalCurrentId = currentNextBatchEntry;
        log.warn(() -> "Couldn't find nextBatchEntry" + finalCurrentId);
        return null;
    }

    public void putScheduledFlush(ScheduledFuture<?> scheduledFlush) {
        this.scheduledFlush = scheduledFlush;
    }

    public void cancelScheduledFlush() {
        scheduledFlush.cancel(false);
    }

    public Collection<CompletableFuture<ResponseT>> responses() {
        return idToBatchContext.values()
                               .stream()
                               .map(BatchingExecutionContext::response)
                               .collect(Collectors.toList());
    }

    public void clear() {
        idToBatchContext.clear();
    }

    // TODO: Only for debugging
    public void forEach(BiConsumer<String, BatchingExecutionContext<RequestT, ResponseT>> action) {
        idToBatchContext.forEach(action);
    }
}
