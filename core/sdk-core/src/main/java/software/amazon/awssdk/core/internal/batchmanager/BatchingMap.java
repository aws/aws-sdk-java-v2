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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Logger;

/**
 * Outer map maps a batch group ID (ex. queueUrl, overrideConfig etc.) to a nested BatchingGroupMap map.
 * @param <RequestT> the type of an outgoing response
 */
@SdkInternalApi
public final class BatchingMap<RequestT, ResponseT> {

    private final Map<String, BatchBuffer<RequestT, ResponseT>> batchContextMap;

    public BatchingMap() {
        this.batchContextMap = new ConcurrentHashMap<>();
    }

    public void put(String batchKey, Supplier<ScheduledFlush> scheduleFlush, RequestT request,
                    CompletableFuture<ResponseT> response) {
        batchContextMap.computeIfAbsent(batchKey, k -> new BatchBuffer<>(scheduleFlush.get()))
                       .put(request, response);
    }

    public ScheduledFlush getScheduledFlush(String batchKey) {
        return batchContextMap.get(batchKey).getScheduledFlush();
    }

    public void putScheduledFlush(String key, ScheduledFlush scheduledFlush) {
        batchContextMap.get(key).putScheduledFlush(scheduledFlush);
    }

    public void forEach(BiConsumer<String, BatchBuffer<RequestT, ResponseT>> action) {
        batchContextMap.forEach(action);
    }

    //TODO: Doesn't seem to be used.
    public void clear() {
        for (Map.Entry<String, BatchBuffer<RequestT, ResponseT>> entry: batchContextMap.entrySet()) {
            String key = entry.getKey();
            entry.getValue().clear();
            batchContextMap.remove(key);
        }
    }

    public LinkedHashMap<String, BatchingExecutionContext<RequestT, ResponseT>> canManualFlush(String batchKey,
                                                                                               int maxBatchItems) {
        return batchContextMap.get(batchKey).canManualFlush(maxBatchItems);
    }

    public LinkedHashMap<String, BatchingExecutionContext<RequestT, ResponseT>> canScheduledFlush(String batchKey,
                                                                                                  int maxBatchItems) {
        return batchContextMap.get(batchKey).canScheduledFlush(maxBatchItems);
    }

    public void cancelScheduledFlush(String batchKey) {
        batchContextMap.get(batchKey).cancelScheduledFlush();
    }

    public void completeResponse(String batchKey, String responseId, ResponseT response) {
        batchContextMap.get(batchKey).getResponse(responseId).complete(response);
    }

    public void removeRequestAndResponse(String batchKey, String requestAndResponseId) {
        batchContextMap.get(batchKey).remove(requestAndResponseId);
    }

    public void waitForFlushesAndClear(Logger log) {
        try {
            for (BatchBuffer<RequestT, ResponseT> idToResponse : batchContextMap.values()) {
                CompletableFuture.allOf(idToResponse.responses().toArray(new CompletableFuture[0]))
                                 .get(60, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn(() -> "Interrupted during BatchBuffer shutdown" + e);
        } catch (ExecutionException e) {
            log.warn(() -> "Failed during graceful metric publisher shutdown." + e);
        } catch (TimeoutException e) {
            log.warn(() -> "Timed out during graceful metric publisher shutdown." + e);
        } finally {
            batchContextMap.clear();
        }
    }
}
