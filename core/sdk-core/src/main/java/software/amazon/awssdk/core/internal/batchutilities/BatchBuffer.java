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

    // Maps destination (ex. queueUrl) to list of individual requests
    private final BatchingMap<T> batchGroupIdToIdToRequest;
    private final BatchingMap<CompletableFuture<U>> batchGroupIdToIdToResponse;
    private final Map<String, ScheduledFuture<?>> scheduledFlushTasks;
    private final BatchAndSendFunction<T, V> batchingFunction;
    private final UnpackBatchResponseFunction<V, U> unpackResponseFunction;
    private final ScheduledExecutorService scheduledExecutor;
    private final Duration maxBatchOpenInMs;
    private final int maxBatchItems;
    private final AtomicInteger currentId;

    public BatchBuffer(int maxBatchItems, Duration maxBatchOpenInMs,
                       BatchAndSendFunction<T, V> batchingFunction,
                       UnpackBatchResponseFunction<V, U> unpackResponseFunction) {
        this.batchGroupIdToIdToRequest = new BatchingMap<>();
        this.batchGroupIdToIdToResponse = new BatchingMap<>();
        this.scheduledFlushTasks = new ConcurrentHashMap<>();
        this.currentId = new AtomicInteger(Integer.MIN_VALUE);
        this.maxBatchItems = maxBatchItems;
        this.maxBatchOpenInMs = maxBatchOpenInMs;
        this.batchingFunction = batchingFunction;
        this.unpackResponseFunction = unpackResponseFunction;
        ThreadFactory threadFactory = new ThreadFactoryBuilder().threadNamePrefix("batch-buffer").build();
        this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);
    }

    public CompletableFuture<U> sendRequest(T request, String destination) {
        CompletableFuture<U> response = new CompletableFuture<>();
        String id = Integer.toString(currentId.getAndIncrement());
        batchGroupIdToIdToResponse.getNestedMap(destination)
                                  .put(id, response);
        batchGroupIdToIdToRequest.getNestedMap(destination)
                                 .put(id, request);

        if (batchGroupIdToIdToRequest.getNestedMap(destination).size() < maxBatchItems) {
            if (!scheduledFlushTasks.containsKey(destination)) {
                scheduledFlushTasks.put(destination, scheduleBufferFlush(destination, maxBatchOpenInMs.toMillis(),
                                                                         scheduledExecutor));
            }
        } else {
            if (scheduledFlushTasks.containsKey(destination)) {
                // "reset" the flush task timer by cancelling scheduled task then restarting it.
                ScheduledFuture<?> scheduledFuture = scheduledFlushTasks.get(destination);
                scheduledFuture.cancel(true);
                if (scheduledFuture.getDelay(TimeUnit.MILLISECONDS) <= 0) {
                    scheduledFlushTasks.put(destination, scheduleBufferFlush(destination, maxBatchOpenInMs.toMillis(),
                                                                             scheduledExecutor));
                    return response;
                }
                scheduledFlushTasks.put(destination, scheduleBufferFlush(destination, maxBatchOpenInMs.toMillis(),
                                                                         scheduledExecutor));
            }
            flushBuffer(destination);
        }
        return response;
    }

    // Flushes the buffer for the given destination and fills in the response map with the returned responses.
    // Returns exception in completableFuture if batchingFunction.apply throws an exception.
    private void flushBuffer(String destination) {
        HashMap<String, T> requestBuffer = new HashMap<>(batchGroupIdToIdToRequest.getNestedMap(destination));
        if (requestBuffer.isEmpty()) {
            return;
        }

        List<IdentifiedRequest<T>> requestEntryList = new ArrayList<>();
        requestBuffer.forEach((key, value) -> requestEntryList.add(new IdentifiedRequest<>(key, value)));
        batchingFunction.batchAndSend(requestEntryList, destination)
                        .whenComplete((result, ex) -> handleAndCompleteResponses(destination, result, ex));
    }

    private void handleAndCompleteResponses(String destination, V batchResult, Throwable exception) {
        if (exception != null) {
            batchGroupIdToIdToResponse.getNestedMap(destination)
                                      .values()
                                      .forEach(responseFuture -> responseFuture.completeExceptionally(exception));
        } else {
            List<IdentifiedResponse<U>> identifiedResponses = unpackResponseFunction.unpackBatchResponse(batchResult);
            Map<String, T> requestQueue = batchGroupIdToIdToRequest.getNestedMap(destination);
            for (IdentifiedResponse<U> identifiedResponse : identifiedResponses) {
                String id = identifiedResponse.getId();
                U response = identifiedResponse.getResponse();
                batchGroupIdToIdToResponse.getNestedMap(destination)
                                          .get(id)
                                          .complete(response);
                batchGroupIdToIdToResponse.getNestedMap(destination)
                                          .remove(id);
                requestQueue.remove(id);
            }
        }
    }

    private ScheduledFuture<?> scheduleBufferFlush(String destination, long timeOutInMs,
                                               ScheduledExecutorService scheduledExecutor) {
        return scheduledExecutor.scheduleAtFixedRate(() -> flushBuffer(destination),
                                                     timeOutInMs,
                                                     timeOutInMs,
                                                     TimeUnit.MILLISECONDS);
    }

    public void close() {
        try {
            scheduledExecutor.shutdownNow();
            scheduledFlushTasks.forEach((key, value) -> value.cancel(false));
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

}
