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

package software.amazon.awssdk.core.internal.batchutilities;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Logger;

@SdkInternalApi
public final class BatchBuffer<RequestT, ResponseT> {
    private static final Logger log = Logger.loggerFor(BatchBuffer.class);

    private final Map<String, BatchingExecutionContext<RequestT, ResponseT>> idToBatchContext;

    /**
     * Keeps track of the number of requests yet to be flushed. When manually flushing (ie the number of requests >=
     * maxBatchItems, numRequests is preemptively
     */
    private final AtomicInteger numRequests;

    // TODO: Might make sense to still have to separate numRequest counters (one for numRequests not finished sending, and one
    //  that preempte number of unflushed. Mainly useful for keeping track of when to run scheduled and manual flushes.
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
    private ScheduledFlush scheduledFlush;

    public BatchBuffer(ScheduledFlush scheduledFlush) {
        this.idToBatchContext = new ConcurrentHashMap<>();
        this.numRequests = new AtomicInteger(0);
        this.nextId = new AtomicInteger(0);
        this.nextBatchEntry = new AtomicInteger(0);
        this.scheduledFlush = scheduledFlush;
    }

    public int size() {
        return idToBatchContext.size();
    }

    public int requestSize() {
        return numRequests.get();
    }

    public int canManualFlush(int maxBatchItems) {
        return numRequests.getAndUpdate(num -> num < maxBatchItems ? num : num - maxBatchItems);
    }

    public int canScheduledFlush(int maxBatchItems) {
        return numRequests.getAndUpdate(num -> num < maxBatchItems ? 0 : num - maxBatchItems);
    }

    public void addNumRequests(int update) {
        numRequests.addAndGet(update);
    }

    public boolean hasRequests() {
        return numRequests.get() != 0;
    }

    public boolean hasResponses() {
        return !idToBatchContext.isEmpty();
    }

    public boolean containsKey(String key) {
        return idToBatchContext.containsKey(key);
    }

    public RequestT getRequest(String key) {
        return idToBatchContext.get(key).request();
    }

    public CompletableFuture<ResponseT> getResponse(String key) {
        return idToBatchContext.get(key).response();
    }

    public ScheduledFlush getScheduledFlush() {
        return scheduledFlush;
    }

    // TODO: Fix this somehow to maintain insertion order.
    public BatchingExecutionContext<RequestT, ResponseT> put(RequestT request, CompletableFuture<ResponseT> response) {
        synchronized (this) {
            String id = BatchUtils.getAndIncrementId(nextId);
            log.warn(() -> "Putting ID: " + id + ". From Thread: " + Thread.currentThread().getId());
            BatchingExecutionContext<RequestT, ResponseT> ret = idToBatchContext.put(id, new BatchingExecutionContext<>(request,
                                                                                                                   response));
            numRequests.getAndIncrement();
            return ret;
        }
    }

    public String nextBatchEntry() {
        int currentId;
        int newCurrentId;
        do {
            currentId = nextBatchEntry.get();
            newCurrentId = currentId + 1;
            if (!idToBatchContext.containsKey(Integer.toString(currentId))) {
                newCurrentId = currentId;
            }
        } while (!nextBatchEntry.compareAndSet(currentId, newCurrentId));

        if (currentId != newCurrentId) {
            return Integer.toString(currentId);
        }
        int finalCurrentId = currentId;
        log.warn(() -> "Couldn't find nextBatchEntry" + finalCurrentId);
        return null;
    }

    public void putScheduledFlush(ScheduledFlush scheduledFlush) {
        this.scheduledFlush = scheduledFlush;
    }

    public void cancelScheduledFlush() {
        scheduledFlush.cancel();
    }

    public BatchingExecutionContext<RequestT, ResponseT> remove(String key) {
        return idToBatchContext.remove(key);
    }

    public Collection<CompletableFuture<ResponseT>> responses() {
        return idToBatchContext.values()
                               .stream()
                               .map(BatchingExecutionContext::response)
                               .collect(Collectors.toList());
    }

    public Set<Map.Entry<String, BatchingExecutionContext<RequestT, ResponseT>>> entrySet() {
        return idToBatchContext.entrySet();
    }

    public void clear() {
        idToBatchContext.clear();
    }

    // Only for debugging
    public void forEach(BiConsumer<String, BatchingExecutionContext<RequestT, ResponseT>> action) {
        idToBatchContext.forEach(action);
    }
}
