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

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;

/**
 * Implementation of a generic buffer for automatic request batching.
 * @param <T> the type of an outgoing request.
 * @param <U> the type of an outgoing response.
 * @param <V> the type of an outgoing batch response.
 */
@SdkInternalApi
public class BatchBuffer<T, U, V> {

    private final BatchingMap<T> batchGroupIdToIdToRequest;
    private final BatchingMap<CompletableFuture<U>> batchGroupIdToIdToResponse;
    private final Map<String, ScheduledFlush> scheduledFlushTasks;
    private final Map<String, AtomicInteger> currentIds;
    private final BatchAndSendFunction<T, V> batchingFunction;
    private final UnpackBatchResponseFunction<V, U> unpackResponseFunction;
    private final ScheduledExecutorService scheduledExecutor;
    private final Duration maxBatchOpenInMs;
    private final int maxBatchItems;

    public BatchBuffer(int maxBatchItems, Duration maxBatchOpenInMs,
                       BatchAndSendFunction<T, V> batchingFunction,
                       UnpackBatchResponseFunction<V, U> unpackResponseFunction) {
        this.batchGroupIdToIdToRequest = new BatchingMap<>();
        this.batchGroupIdToIdToResponse = new BatchingMap<>();
        this.scheduledFlushTasks = new ConcurrentHashMap<>();
        this.currentIds = new ConcurrentHashMap<>();
        this.maxBatchItems = maxBatchItems;
        this.maxBatchOpenInMs = maxBatchOpenInMs;
        this.batchingFunction = batchingFunction;
        this.unpackResponseFunction = unpackResponseFunction;
        ThreadFactory threadFactory = new ThreadFactoryBuilder().threadNamePrefix("batch-buffer").build();
        this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);
    }

    public CompletableFuture<U> sendRequest(T request, String destination) {
        CompletableFuture<U> response = new CompletableFuture<>();
        AtomicInteger currentId = currentIds.computeIfAbsent(destination, k -> new AtomicInteger(0));
        String id = Integer.toString(getCurrentIdAndIncrement(currentId));
        batchGroupIdToIdToResponse.getNestedMap(destination)
                                  .put(id, response);
        batchGroupIdToIdToRequest.getNestedMap(destination)
                                 .put(id, request);
        if (batchGroupIdToIdToRequest.get(destination).size() < maxBatchItems || checkIfScheduledFlush(destination)) {
            if (!scheduledFlushTasks.containsKey(destination)) {
                scheduledFlushTasks.put(destination, scheduleBufferFlush(destination, maxBatchOpenInMs.toMillis(),
                                                                         scheduledExecutor));
            }
        } else {
            if (scheduledFlushTasks.containsKey(destination)) {
                // "reset" the flush task timer by cancelling scheduled task then restarting it.
                ScheduledFlush scheduledFuture = scheduledFlushTasks.get(destination);
                scheduledFuture.cancel();
                if (scheduledFuture.hasExecuted()) {
                    scheduledFlushTasks.put(destination, scheduleBufferFlush(destination, maxBatchOpenInMs.toMillis(),
                                                                             scheduledExecutor));
                    return response;
                }
            }
            //Can think of a manual flush as a scheduled flush that doesn't have an initial delay.
            scheduledFlushTasks.put(destination, scheduleBufferFlush(destination, 0, maxBatchOpenInMs.toMillis(),
                                                                     scheduledExecutor));
        }
        return response;
    }

    // Flushes the buffer for the given destination and fills in the response map with the returned responses.
    // Returns exception in completableFuture if batchingFunction.apply throws an exception.
    private CompletableFuture<V> flushBuffer(String destination) {
        Map<String, T> requestBuffer = batchGroupIdToIdToRequest.get(destination);
        Map<String, T> requestBufferCopy = new HashMap<>(requestBuffer);
        if (requestBufferCopy.isEmpty()) {
            return null;
        }

        List<IdentifiedRequest<T>> requestEntryList = new ArrayList<>();
        Iterator<Map.Entry<String, T>> requestIterator = requestBufferCopy.entrySet().iterator();
        for (int i = 0; i < maxBatchItems && requestIterator.hasNext(); i++) {
            Map.Entry<String, T> entry = requestIterator.next();
            requestEntryList.add(new IdentifiedRequest<>(entry.getKey(), entry.getValue()));
            requestBuffer.remove(entry.getKey());
        }

        return batchingFunction.batchAndSend(requestEntryList, destination)
                               .whenComplete((result, ex) -> handleAndCompleteResponses(destination, result, ex));
    }

    private void handleAndCompleteResponses(String destination, V batchResult, Throwable exception) {
        if (exception != null) {
            batchGroupIdToIdToResponse.get(destination)
                                      .values()
                                      .forEach(responseFuture -> responseFuture.completeExceptionally(exception));
        } else {
            List<IdentifiedResponse<U>> identifiedResponses = unpackResponseFunction.unpackBatchResponse(batchResult);
            for (IdentifiedResponse<U> identifiedResponse : identifiedResponses) {
                String id = identifiedResponse.getId();
                U response = identifiedResponse.getResponse();
                batchGroupIdToIdToResponse.get(destination)
                                          .get(id)
                                          .complete(response);
                batchGroupIdToIdToResponse.get(destination)
                                          .remove(id);
            }
        }
    }

    private ScheduledFlush scheduleBufferFlush(String destination, long timeOutInMs,
                                               ScheduledExecutorService scheduledExecutor) {
        return scheduleBufferFlush(destination, timeOutInMs, timeOutInMs, scheduledExecutor);
    }

    private ScheduledFlush scheduleBufferFlush(String destination, long initialDelay, long timeOutInMs,
                                               ScheduledExecutorService scheduledExecutor) {
        CancellableFlush flushTask = new CancellableFlush(() -> flushBuffer(destination));
        ScheduledFuture<?> scheduledFuture = scheduledExecutor.scheduleAtFixedRate(flushTask,
                                                                                   initialDelay,
                                                                                   timeOutInMs,
                                                                                   TimeUnit.MILLISECONDS);
        return new ScheduledFlush(flushTask, scheduledFuture);
    }

    // Returns true if a flush is currently being executed.
    private boolean checkIfScheduledFlush(String destination) {
        if (scheduledFlushTasks.containsKey(destination)) {
            return scheduledFlushTasks.get(destination).hasExecuted();
        }
        return false;
    }

    public void close() {
        try {
            scheduledExecutor.shutdownNow();
            scheduledFlushTasks.forEach((key, value) -> value.cancel());
            batchGroupIdToIdToRequest.forEach((key, value) -> flushBuffer(key));
            for (Map<String, CompletableFuture<U>> idToResponse : batchGroupIdToIdToResponse.values()) {
                CompletableFuture.allOf(idToResponse.values().toArray(new CompletableFuture[0]))
                                 .get(60, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Interrupted during BatchBuffer shutdown" + e);
        } catch (ExecutionException e) {
            System.err.println("Failed during graceful metric publisher shutdown." + e);
        } catch (TimeoutException e) {
            System.err.println("Timed out during graceful metric publisher shutdown." + e);
        } finally {
            scheduledExecutor.shutdownNow();
        }
    }

    private synchronized int getCurrentIdAndIncrement(AtomicInteger currentId) {
        int id = currentId.getAndIncrement();
        if (id < 0) {
            currentId.set(1);
            id = 0;
        }
        return id;
    }
}
