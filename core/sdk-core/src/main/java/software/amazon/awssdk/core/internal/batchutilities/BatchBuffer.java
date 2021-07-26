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
public class BatchBuffer<RequestT, ResponseT> {
    private final Map<String, BatchContext<RequestT, ResponseT>> idToBatchContext;
    private final AtomicInteger numRequests;

    public BatchBuffer() {
        this.idToBatchContext = new ConcurrentHashMap<>();
        this.numRequests = new AtomicInteger(0);
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

    public Map<String, BatchContext<RequestT, ResponseT>> getUnderlyingMap() {
        return idToBatchContext;
    }

    public BatchContext<RequestT, ResponseT> put(String key, RequestT request, CompletableFuture<ResponseT> response) {
        numRequests.getAndIncrement();
        return idToBatchContext.put(key, new BatchContext<>(request, response));
    }

    public RequestT removeRequest(String key) {
        numRequests.getAndIncrement();
        return idToBatchContext.get(key).removeRequest();
    }

    public BatchContext<RequestT, ResponseT> remove(String key) {
        return idToBatchContext.remove(key);
    }

    public Collection<BatchContext<RequestT, ResponseT>> values() {
        return idToBatchContext.values();
    }

    public Collection<CompletableFuture<ResponseT>> responses() {
        return idToBatchContext.values()
                               .stream()
                               .map(BatchContext::response)
                               .collect(Collectors.toList());
    }

    public Set<Map.Entry<String, BatchContext<RequestT, ResponseT>>> entrySet() {
        return idToBatchContext.entrySet();
    }

    public void clear() {
        numRequests.set(0);
        idToBatchContext.clear();
    }

    public void forEach(BiConsumer<String, BatchContext<RequestT, ResponseT>> action) {
        idToBatchContext.forEach(action);
    }
}
