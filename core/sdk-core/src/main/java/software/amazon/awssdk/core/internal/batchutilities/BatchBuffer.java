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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;
import software.amazon.awssdk.utils.Validate;

/**
 * Implementation of a generic buffer for automatic request batching.
 * @param <T> the type of an outgoing request.
 * @param <U> the type of an outgoing response.
 * @param <V> the type of an outgoing batch response.
 */
@SdkInternalApi
public class BatchBuffer<T, U, V> {

    // Maps destination (ex. queueUrl) to list of individual requests
    private final Map<String, ConcurrentLinkedQueue<T>> destinationRequestMap;
    // Outer map maps destination to nested map. Inner map maps batch id to future response.
    private final Map<String, Map<String, CompletableFuture<U>>> destinationResponseMap;
    private final ScheduledExecutorService scheduledExecutor;
    private final Duration maxBatchOpenInMs;
    private final int maxBatchItems;
    private AtomicInteger currentId;
    private boolean scheduled = false;
    private ScheduledFlush scheduledFlushTask = null;
    private final BatchAndSendFunction<T, V> batchingFunction;
    private final UnpackBatchResponseFunction<V, U> unpackResponseFunction;

    public BatchBuffer(int maxBatchItems, Duration maxBatchOpenInMs,
                       BatchAndSendFunction<T, V> batchingFunction,
                       UnpackBatchResponseFunction<V, U> unpackResponseFunction) {
        this.destinationRequestMap = new HashMap<>();
        this.destinationResponseMap = new HashMap<>();
        this.currentId = new AtomicInteger(Integer.MIN_VALUE);
        this.maxBatchItems = maxBatchItems;
        this.maxBatchOpenInMs = maxBatchOpenInMs;
        this.batchingFunction = batchingFunction;
        this.unpackResponseFunction = unpackResponseFunction;
        ThreadFactory threadFactory = new ThreadFactoryBuilder().threadNamePrefix("batch-buffer").build();
        this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);
    }

    public CompletableFuture<U> sendRequest(T request, String destination) {
        destinationRequestMap.computeIfAbsent(destination, k -> new ConcurrentLinkedQueue<>())
                             .add(request);
        CompletableFuture<U> response = new CompletableFuture<>();
        destinationResponseMap.computeIfAbsent(destination, k -> new HashMap<>())
                              .put(Integer.toString(currentId.getAndIncrement()), response);

        if (destinationRequestMap.get(destination).size() < maxBatchItems) {
            if (!scheduled) {
                scheduledFlushTask = scheduleBufferFlush(destination, maxBatchOpenInMs.toMillis(), scheduledExecutor);
                scheduled = true;
            }
        } else {
            if (scheduled) {
                scheduledFlushTask.cancel();
                scheduled = false;
            }
            // Flush was not cancelled in time. Return the Completable Future added in the response map.
            // Response should have been mapped back to it.
            if (scheduledFlushTask.hasExecuted()) {
                return response;
            }
            flushBuffer(destination);
            destinationRequestMap.get(destination).clear();
        }
        return response;
    }

    // Flushes the buffer for the given destination and fills in the response map with the returned responses.
    // Returns exception in completableFuture if batchingFunction.apply throws an exception.
    private void flushBuffer(String destination) {
        ConcurrentLinkedQueue<T> requestBuffer = destinationRequestMap.get(destination);
        List<IdentifiedRequest<T>> requestEntryMap = new ArrayList<>();
        int batchId = currentId.get() - requestBuffer.size();
        Iterator<T> iterator = requestBuffer.iterator();
        for (int i = 0; iterator.hasNext() && i < maxBatchItems; i++) {
            T request = iterator.next();
            requestEntryMap.add(new IdentifiedRequest<>(Integer.toString(batchId + i), request));
        }

        // Map returned responses back to the destinationResponseMap.
        batchingFunction.batchAndSend(requestEntryMap, destination)
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                destinationResponseMap.get(destination)
                                                      .values()
                                                      .forEach(responseFuture -> responseFuture.completeExceptionally(ex));
                            } else {
                                List<IdentifiedResponse<U>> identifiedResponses = unpackResponseFunction
                                                                                    .unpackBatchResponse(result);
                                identifiedResponses.forEach(identifiedResponse -> {
                                    String id = identifiedResponse.getId();
                                    U response = identifiedResponse.getResponse();
                                    destinationResponseMap.get(destination)
                                                          .get(id)
                                                          .complete(response);
                                    destinationResponseMap.get(destination)
                                                          .remove(id);
                                });
                            }
                        });
    }

    // Helper methods and classes to handle scheduling and cancelling
    private ScheduledFlush scheduleBufferFlush(String destination, long timeOutInMs,
                                               ScheduledExecutorService scheduledExecutor) {
       CancellableFlush flushTask = new CancellableFlush(destination);
       ScheduledFuture<?> scheduledFuture = scheduledExecutor.schedule(flushTask,
                                                                       timeOutInMs,
                                                                       TimeUnit.MILLISECONDS);
       return new ScheduledFlush(flushTask, scheduledFuture);
    }

    private class ScheduledFlush {

        private final ScheduledFuture<?> future;
        private final CancellableFlush cancellableFlush;

        public ScheduledFlush(CancellableFlush cancellableFlush, ScheduledFuture<?> future) {
            this.cancellableFlush = Validate.paramNotNull(cancellableFlush, "cancellableFlush");
            this.future = Validate.paramNotNull(future, "scheduledFuture");
        }

        public void cancel() {
            future.cancel(false);
            cancellableFlush.cancel();
        }

        public boolean hasExecuted() {
            return cancellableFlush.hasExecuted();
        }
    }

    private class CancellableFlush implements Runnable {

        private final String destination;
        private final Object lock = new Object();
        private boolean hasExecuted = false;
        private boolean isCancelled = false;

        private CancellableFlush(String destination) {
            this.destination = destination;
        }

        @Override
        public void run() {
            synchronized (this.lock) {
                ConcurrentLinkedQueue<T> destinationBuffer = destinationRequestMap.get(destination);
                // Might need to modify this. flushBuffer() still completes the futures. Instead, we should only complete the
                // futures if the flushBuffer was not cancelled.
                flushBuffer(destination);
                if (!isCancelled) {
                    destinationBuffer.clear();
                    hasExecuted = true;
                }
            }
        }

        public void cancel() {
            synchronized (this.lock) {
                isCancelled = true;
            }
        }

        public boolean hasExecuted() {
            synchronized (this.lock) {
                return hasExecuted;
            }
        }
    }
}
