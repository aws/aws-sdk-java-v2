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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
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
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;

/**
 * Implementation of a generic buffer for automatic request batching.
 * @param <RequestT> the type of an outgoing request.
 * @param <ResponseT> the type of an outgoing response.
 * @param <BatchResponseT> the type of an outgoing batch response.
 */
@SdkInternalApi
public class BatchManager<RequestT, ResponseT, BatchResponseT> implements SdkAutoCloseable {
    // TODO: Just a number from the CloudwatchMetricPublisher for now. Should I choose a different max task queue size?
    private static final int MAXIMUM_TASK_QUEUE_SIZE = 128;

    private static final Logger log = Logger.loggerFor(BatchManager.class);
    private final int maxBatchItems;
    private final Duration maxBatchOpenInMs;
    private final BatchingMap<RequestT, ResponseT> requestsAndResponsesMaps;
    private final Map<String, ScheduledFlush> scheduledFlushTasks;
    private final Map<String, AtomicInteger> currentIds;
    private final BatchAndSendFunction<RequestT, BatchResponseT> batchingFunction;
    private final BatchResponseMapperFunction<BatchResponseT, ResponseT> mapResponsesFunction;

    /**
     * The executor that executes {@link #flushBuffer}
     */
    private final ExecutorService executor;

    /**
     * A scheduled executor that periodically schedules {@link #flushBuffer} on the {@link #executor} thread. Note: The scheduled
     * executor should never execute the flush task itself because that would modify the request and response maps which should
     * only ever be modified from the {@link #executor} thread.
     */
    private final ScheduledExecutorService scheduledExecutor;

    public BatchManager(int maxBatchItems, Duration maxBatchOpenInMs, ScheduledExecutorService scheduledExecutor,
                       BatchAndSendFunction<RequestT, BatchResponseT> batchingFunction,
                        BatchResponseMapperFunction<BatchResponseT, ResponseT> mapResponsesFunction) {
        this.requestsAndResponsesMaps = new BatchingMap<>();
        this.scheduledFlushTasks = new ConcurrentHashMap<>();
        this.currentIds = new ConcurrentHashMap<>();
        this.maxBatchItems = maxBatchItems;
        this.maxBatchOpenInMs = maxBatchOpenInMs;
        this.batchingFunction = batchingFunction;
        this.mapResponsesFunction = mapResponsesFunction;
        ThreadFactory threadFactory = new ThreadFactoryBuilder().threadNamePrefix("batch-buffer").build();
        this.scheduledExecutor = scheduledExecutor;
        this.executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                                               new ArrayBlockingQueue<>(MAXIMUM_TASK_QUEUE_SIZE),
                                               threadFactory);
    }

    public CompletableFuture<ResponseT> sendRequest(RequestT request, String batchGroupId) {
        CompletableFuture<ResponseT> response = new CompletableFuture<>();
        AtomicInteger currentId = currentIds.computeIfAbsent(batchGroupId, k -> new AtomicInteger(0));
        String id = Integer.toString(getCurrentIdAndIncrement(currentId));
        requestsAndResponsesMaps.getNestedMap(batchGroupId)
                                .put(id, request, response);

        if (requestsAndResponsesMaps.get(batchGroupId).requestSize() < maxBatchItems) {
            scheduledFlushTasks.computeIfAbsent(batchGroupId, k -> scheduleBufferFlush(batchGroupId, maxBatchOpenInMs.toMillis(),
                                                                     scheduledExecutor));
        } else {
            CompletableFuture<ResponseT> cancelledResponse = cancelScheduledFlushIfNeeded(response, batchGroupId);
            if (cancelledResponse != null) {
                return cancelledResponse;
            }
        }
        return response;
    }

    private CompletableFuture<ResponseT> cancelScheduledFlushIfNeeded(CompletableFuture<ResponseT> response,
                                                                      String batchGroupId) {
        if (scheduledFlushTasks.containsKey(batchGroupId)) {
            // "reset" the flush task timer by cancelling scheduled task then restarting it.
            ScheduledFlush scheduledFuture = scheduledFlushTasks.get(batchGroupId);
            scheduledFuture.cancel();
            // If scheduledFuture hasExecuted, do not perform a manual flush (initialDelay == 0), just return the response.
            if (scheduledFuture.hasExecuted()) {
                scheduledFlushTasks.put(batchGroupId, scheduleBufferFlush(batchGroupId, maxBatchOpenInMs.toMillis(),
                                                                          scheduledExecutor));
                return response;
            }
        }
        scheduledFlushTasks.put(batchGroupId, scheduleBufferFlush(batchGroupId, 0, maxBatchOpenInMs.toMillis(),
                                                                  scheduledExecutor));
        return null;
    }

    private Future<?> flushBuffer(String batchGroupId) {
        return executor.submit(() -> internalFlushBuffer(batchGroupId));
    }

    // Flushes the buffer for the given batchGroupId and fills in the response map with the returned responses.
    // Returns exception in completableFuture if batchingFunction.apply throws an exception.
    private void internalFlushBuffer(String batchGroupId) {
        BatchingGroupMap<RequestT, ResponseT> requestBuffer = requestsAndResponsesMaps.get(batchGroupId);
        BatchingGroupMap<RequestT, ResponseT> requestBufferCopy = new BatchingGroupMap<>(requestBuffer);
        if (!requestBufferCopy.hasRequests()) {
            return;
        }

        List<IdentifiableRequest<RequestT>> requestEntryList = new ArrayList<>();
        Iterator<Map.Entry<String, BatchContext<RequestT, ResponseT>>> requestIterator = requestBufferCopy.entrySet().iterator();
        while (requestEntryList.size() < maxBatchItems && requestIterator.hasNext()) {
            Map.Entry<String, BatchContext<RequestT, ResponseT>> entry = requestIterator.next();
            RequestT request = entry.getValue().request();
            if (request != null) {
                requestEntryList.add(new IdentifiableRequest<>(entry.getKey(), request));
                requestBuffer.removeRequest(entry.getKey());
            }
        }
        batchingFunction.batchAndSend(requestEntryList, batchGroupId)
                        .whenComplete((result, ex) -> handleAndCompleteResponses(batchGroupId, result, ex));
    }

    private void handleAndCompleteResponses(String batchGroupId, BatchResponseT batchResult, Throwable exception) {
        if (exception != null) {
            requestsAndResponsesMaps.get(batchGroupId)
                                    .values()
                                    .forEach(batchContext -> batchContext.response().completeExceptionally(exception));
        } else {
            List<IdentifiableResponse<ResponseT>> identifiedResponses = mapResponsesFunction.mapBatchResponse(batchResult);
            for (IdentifiableResponse<ResponseT> identifiedResponse : identifiedResponses) {
                String id = identifiedResponse.id();
                ResponseT response = identifiedResponse.response();
                requestsAndResponsesMaps.get(batchGroupId)
                                        .getResponse(id)
                                        .complete(response);
                requestsAndResponsesMaps.get(batchGroupId)
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

    public void close() {
        try {
            scheduledExecutor.shutdownNow();
            scheduledFlushTasks.forEach((key, value) -> value.cancel());
            requestsAndResponsesMaps.forEach((key, value) -> flushBuffer(key));
            for (BatchingGroupMap<RequestT, ResponseT> idToResponse : requestsAndResponsesMaps.values()) {
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
