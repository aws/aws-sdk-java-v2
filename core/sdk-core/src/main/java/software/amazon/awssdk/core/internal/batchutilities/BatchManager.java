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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;

/**
 * Implementation of a generic buffer for automatic request batching.
 * @param <T> the type of an outgoing request.
 * @param <U> the type of an outgoing response.
 * @param <V> the type of an outgoing batch response.
 */
@SdkInternalApi
public class BatchManager<T, U, V> {
    // Just a number from the cloudwatch metric publisher for now. Not sure if I should choose a different max task queue size?
    private static final int MAXIMUM_TASK_QUEUE_SIZE = 128;

    private static final Logger log = Logger.loggerFor(BatchManager.class);
    private final BatchingMap<T> batchGroupIdToIdToRequest;
    private final BatchingMap<CompletableFuture<U>> batchGroupIdToIdToResponse;
    private final Map<String, ScheduledFlush> scheduledFlushTasks;
    private final Map<String, AtomicInteger> currentIds;
    private final BatchAndSendFunction<T, V> batchingFunction;
    private final UnpackBatchResponseFunction<V, U> unpackResponseFunction;
    private final ScheduledExecutorService scheduledExecutor;
    private final ThreadFactory threadFactory;
    private final ExecutorService executor;
    private final Duration maxBatchOpenInMs;
    private final int maxBatchItems;

    public BatchManager(int maxBatchItems, Duration maxBatchOpenInMs,
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
        this.threadFactory = new ThreadFactoryBuilder().threadNamePrefix("batch-buffer").build();
        this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);
        this.executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                                               new ArrayBlockingQueue<>(MAXIMUM_TASK_QUEUE_SIZE),
                                               threadFactory);
    }

    public CompletableFuture<U> sendRequest(T request, String batchGroupId) {
        CompletableFuture<U> response = new CompletableFuture<>();
        AtomicInteger currentId = currentIds.computeIfAbsent(batchGroupId, k -> new AtomicInteger(0));
        String id = Integer.toString(getCurrentIdAndIncrement(currentId));
        batchGroupIdToIdToResponse.getNestedMap(batchGroupId)
                                  .put(id, response);
        batchGroupIdToIdToRequest.getNestedMap(batchGroupId)
                                 .put(id, request);

        if (batchGroupIdToIdToRequest.get(batchGroupId).size() < maxBatchItems || checkIfScheduledFlush(batchGroupId)) {
            scheduledFlushTasks.computeIfAbsent(batchGroupId, k -> scheduleBufferFlush(batchGroupId, maxBatchOpenInMs.toMillis(),
                                                                     scheduledExecutor));
        } else {
            if (scheduledFlushTasks.containsKey(batchGroupId)) {
                // "reset" the flush task timer by cancelling scheduled task then restarting it.
                ScheduledFlush scheduledFuture = scheduledFlushTasks.get(batchGroupId);
                scheduledFuture.cancel();
                if (scheduledFuture.hasExecuted()) {
                    scheduledFlushTasks.put(batchGroupId, scheduleBufferFlush(batchGroupId, maxBatchOpenInMs.toMillis(),
                                                                             scheduledExecutor));
                    return response;
                }
            }
            scheduledFlushTasks.put(batchGroupId, scheduleBufferFlush(batchGroupId, 0, maxBatchOpenInMs.toMillis(),
                                                                     scheduledExecutor));
        }
        return response;
    }

    private Future<?> flushBuffer(String batchGroupId) {
        return executor.submit(() -> internalFlushBuffer(batchGroupId));
    }

    // Flushes the buffer for the given batchGroupId and fills in the response map with the returned responses.
    // Returns exception in completableFuture if batchingFunction.apply throws an exception.
    private void internalFlushBuffer(String batchGroupId) {
        Map<String, T> requestBuffer = batchGroupIdToIdToRequest.get(batchGroupId);
        Map<String, T> requestBufferCopy = new HashMap<>(requestBuffer);
        if (requestBufferCopy.isEmpty()) {
            return;
        }

        List<IdentifiedRequest<T>> requestEntryList = new ArrayList<>();
        Iterator<Map.Entry<String, T>> requestIterator = requestBufferCopy.entrySet().iterator();
        for (int i = 0; i < maxBatchItems && requestIterator.hasNext(); i++) {
            Map.Entry<String, T> entry = requestIterator.next();
            requestEntryList.add(new IdentifiedRequest<>(entry.getKey(), entry.getValue()));
            requestBuffer.remove(entry.getKey());
        }
        batchingFunction.batchAndSend(requestEntryList, batchGroupId)
                               .whenComplete((result, ex) -> handleAndCompleteResponses(batchGroupId, result, ex));
    }

    private void handleAndCompleteResponses(String batchGroupId, V batchResult, Throwable exception) {
        if (exception != null) {
            batchGroupIdToIdToResponse.get(batchGroupId)
                                      .values()
                                      .forEach(responseFuture -> responseFuture.completeExceptionally(exception));
        } else {
            List<IdentifiedResponse<U>> identifiedResponses = unpackResponseFunction.unpackBatchResponse(batchResult);
            for (IdentifiedResponse<U> identifiedResponse : identifiedResponses) {
                String id = identifiedResponse.getId();
                U response = identifiedResponse.getResponse();
                batchGroupIdToIdToResponse.get(batchGroupId)
                                          .get(id)
                                          .complete(response);
                batchGroupIdToIdToResponse.get(batchGroupId)
                                          .remove(id);
            }
        }
    }

    private ScheduledFlush scheduleBufferFlush(String batchGroupId, long timeOutInMs,
                                               ScheduledExecutorService scheduledExecutor) {
        return scheduleBufferFlush(batchGroupId, timeOutInMs, timeOutInMs, scheduledExecutor);
    }

    private ScheduledFlush scheduleBufferFlush(String batchGroupId, long initialDelay, long timeOutInMs,
                                               ScheduledExecutorService scheduledExecutor) {
        CancellableFlush flushTask = new CancellableFlush(() -> flushBuffer(batchGroupId));
        ScheduledFuture<?> scheduledFuture = scheduledExecutor.scheduleAtFixedRate(() -> {
            flushTask.reset();
            flushTask.run();
        }, initialDelay, timeOutInMs, TimeUnit.MILLISECONDS);
        return new ScheduledFlush(flushTask, scheduledFuture);
    }

    // Returns true if a flush is currently being executed.
    private boolean checkIfScheduledFlush(String batchGroupId) {
        if (scheduledFlushTasks.containsKey(batchGroupId)) {
            return scheduledFlushTasks.get(batchGroupId).hasExecuted();
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
            log.warn(() -> "Interrupted during BatchBuffer shutdown" + e);
        } catch (ExecutionException e) {
            log.warn(() -> "Failed during graceful metric publisher shutdown." + e);
        } catch (TimeoutException e) {
            log.warn(() -> "Timed out during graceful metric publisher shutdown." + e);
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
