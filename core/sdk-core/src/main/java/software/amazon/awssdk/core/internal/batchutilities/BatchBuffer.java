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

@SdkInternalApi
public final class BatchBuffer<RequestT, ResponseT> {
    private final Map<String, BatchingExecutionContext<RequestT, ResponseT>> idToBatchContext;
    private final AtomicInteger numRequests;

    /**
     * Batch entries in a batch request require a unique ID so nextId keeps track of the ID to assign to the next
     * BatchingExecutionContext. For simplicity, the ID is just an integer that is incremented everytime a new request and
     * response pair is received.
     */
    private final AtomicInteger nextId;

    /**
     * The scheduled flush tasks associated with this batchBuffer.
     */
    private ScheduledFlush scheduledFlush;

    public BatchBuffer(ScheduledFlush scheduledFlush) {
        this.idToBatchContext = new ConcurrentHashMap<>();
        this.numRequests = new AtomicInteger(0);
        this.nextId = new AtomicInteger(0);
        this.scheduledFlush = scheduledFlush;
    }

    public int size() {
        return idToBatchContext.size();
    }

    public int requestSize() {
        return numRequests.get();
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

    public BatchingExecutionContext<RequestT, ResponseT> put(RequestT request, CompletableFuture<ResponseT> response) {
        numRequests.getAndIncrement();
        String id = BatchUtils.getAndIncrementId(nextId);
        return idToBatchContext.put(id, new BatchingExecutionContext<>(request, response));
    }

    public void putScheduledFlush(ScheduledFlush scheduledFlush) {
        this.scheduledFlush = scheduledFlush;
    }

    public void cancelScheduledFlush() {
        scheduledFlush.cancel();
    }

    public void removeRequest(String key) {
        if (idToBatchContext.get(key).removeRequest()) {
            numRequests.getAndDecrement();
        }
    }

    public BatchingExecutionContext<RequestT, ResponseT> remove(String key) {
        return idToBatchContext.remove(key);
    }

    public Collection<BatchingExecutionContext<RequestT, ResponseT>> values() {
        return idToBatchContext.values();
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
        numRequests.set(0);
        idToBatchContext.clear();
    }

    public void forEach(BiConsumer<String, BatchingExecutionContext<RequestT, ResponseT>> action) {
        idToBatchContext.forEach(action);
    }
}
