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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.BatchOverrideConfiguration;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.Validate;

/**
 * Implementation of a generic buffer for automatic request batching.
 * @param <RequestT> the type of an outgoing request.
 * @param <ResponseT> the type of an outgoing response.
 * @param <BatchResponseT> the type of an outgoing batch response.
 */
@SdkInternalApi
public final class BatchManager<RequestT, ResponseT, BatchResponseT> implements SdkAutoCloseable {

    private static final Logger log = Logger.loggerFor(BatchManager.class);
    private final int maxBatchItems;
    private final Duration maxBatchOpenInMs;

    /**
     * A nested map that keeps track of customer requests and the corresponding responses to be completed. Requests are batched
     * together according to a batchKey that is calculated from the request by the service client.
     */
    private final BatchingMap<RequestT, ResponseT> requestsAndResponsesMaps;

    /**
     * Takes a list of identified requests in addition to a destination and batches the requests into a batch request.
     * It then sends the batch request and returns a CompletableFuture of the response.
     */
    private final BatchAndSend<RequestT, BatchResponseT> batchingFunction;

    /**
     * Unpacks the batch response, then transforms individual entries to the appropriate response type. Each entry's batch ID
     * is mapped to the individual response entry.
     */
    private final BatchResponseMapper<BatchResponseT, ResponseT> mapResponsesFunction;

    /**
     * Takes a request and extracts a batchKey as determined by the caller.
     */
    private final BatchKeyMapper<RequestT> batchKeyMapperFunction;

    /**
     * A scheduled executor that periodically schedules {@link #flushBuffer}.
     */
    private final ScheduledExecutorService scheduledExecutor;

    private BatchManager(Builder<RequestT, ResponseT, BatchResponseT> builder) {
        BatchOverrideConfiguration overrideConfiguration = Validate.notNull(builder.overrideConfiguration, "Null override"
                                                                                                           + "Configuration");
        this.requestsAndResponsesMaps = new BatchingMap<>();
        this.maxBatchItems = overrideConfiguration.maxBatchItems();
        this.maxBatchOpenInMs = overrideConfiguration.maxBatchOpenInMs();
        this.batchingFunction = Validate.notNull(builder.batchingFunction, "Null batchingFunction");
        this.mapResponsesFunction = Validate.notNull(builder.mapResponsesFunction, "Null mapResponsesFunction");
        this.batchKeyMapperFunction = Validate.notNull(builder.batchKeyMapperFunction, "Null batchKeyMapperFunction");
        this.scheduledExecutor = overrideConfiguration.scheduledExecutor();
    }

    public static <RequestT, ResponseT, BatchResponseT> Builder<RequestT, ResponseT, BatchResponseT> builder() {
        return new Builder<>();
    }

    /**
     * Buffers outgoing requests on the client and sends them as batch requests to the service. Requests are batched together
     * according to a batchKey and are sent periodically to the service as determined by {@link #maxBatchOpenInMs}. If the
     * number of requests for a batchKey reaches or exceeds {@link #maxBatchItems}, then the requests are immediately flushed
     * and the timeout on the periodic flush is reset.
     * By default, messages are batched according to a service's maximum size for a batch request. These settings can be
     * customized via the configuration.
     *
     * @param request the outgoing request.
     * @return a CompletableFuture of the corresponding response.
     */
    public CompletableFuture<ResponseT> sendRequest(RequestT request) {
        CompletableFuture<ResponseT> response = new CompletableFuture<>();
        try {
            String batchKey = batchKeyMapperFunction.getBatchKey(request);
            BatchBuffer<RequestT, ResponseT> requestBuffer =
                requestsAndResponsesMaps.batchBufferByKey(batchKey,
                                                          () -> scheduleBufferFlush(batchKey, maxBatchOpenInMs.toMillis(),
                                                                                          scheduledExecutor));
            requestBuffer.put(request, response);

            int requestsNum = requestsAndResponsesMaps.get(batchKey).requestSize();
            if (requestsNum > maxBatchItems) {
                manualFlushBuffer(batchKey);
            }
        } catch (Exception e) {
            response.completeExceptionally(e);
        }
        return response;
    }

    private void manualFlushBuffer(String batchKey) {
        ScheduledFlush scheduledFuture = requestsAndResponsesMaps.getScheduledFlush(batchKey);
        // Only cancel a periodic scheduled flush.
        if (!scheduledFuture.isManual()) {
            scheduledFuture.cancel();
        }
        requestsAndResponsesMaps.putScheduledFlush(batchKey, scheduleBufferFlush(batchKey, 0, maxBatchOpenInMs.toMillis(), true,
                                                                                 scheduledExecutor));
    }

    // Flushes the buffer for the given batchKey and fills in the response map with the returned responses.
    // Returns exception in completableFuture if batchingFunction.apply throws an exception.
    private void flushBuffer(String batchKey) {
        BatchBuffer<RequestT, ResponseT> requestBuffer = requestsAndResponsesMaps.get(batchKey);
        if (!requestBuffer.hasRequests()) {
            return;
        }

        List<IdentifiableRequest<RequestT>> requestEntryList = new ArrayList<>();
        Iterator<Map.Entry<String, BatchingExecutionContext<RequestT, ResponseT>>> requestIterator = requestBuffer.entrySet()
                                                                                                                  .iterator();
        while (requestEntryList.size() < maxBatchItems && requestIterator.hasNext()) {
            Map.Entry<String, BatchingExecutionContext<RequestT, ResponseT>> entry = requestIterator.next();
            RequestT request = entry.getValue().request();
            if (request != null) {
                requestEntryList.add(new IdentifiableRequest<>(entry.getKey(), request));
                requestBuffer.removeRequest(entry.getKey());
            }
        }
        if (!requestEntryList.isEmpty()) {
            batchingFunction.batchAndSend(requestEntryList, batchKey)
                            .whenComplete((result, ex) -> handleAndCompleteResponses(batchKey, result, ex));
        }
    }

    private void handleAndCompleteResponses(String batchKey, BatchResponseT batchResult, Throwable exception) {
        if (exception != null) {
            requestsAndResponsesMaps.get(batchKey)
                                    .entrySet()
                                    .forEach(entry -> {
                                        entry.getValue().response().completeExceptionally(exception);
                                        requestsAndResponsesMaps.get(batchKey)
                                                                .remove(entry.getKey());
                                    });
        } else {
            List<IdentifiableResponse<ResponseT>> identifiedResponses = mapResponsesFunction.mapBatchResponse(batchResult);
            for (IdentifiableResponse<ResponseT> identifiedResponse : identifiedResponses) {
                String id = identifiedResponse.id();
                ResponseT response = identifiedResponse.response();
                requestsAndResponsesMaps.get(batchKey)
                                        .getResponse(id)
                                        .complete(response);
                requestsAndResponsesMaps.get(batchKey)
                                        .remove(id);
            }
        }
    }

    private ScheduledFlush scheduleBufferFlush(String batchKey, long timeOutInMs,
                                               ScheduledExecutorService scheduledExecutor) {
        return scheduleBufferFlush(batchKey, timeOutInMs, timeOutInMs, false, scheduledExecutor);
    }

    private ScheduledFlush scheduleBufferFlush(String batchKey, long initialDelay, long timeOutInMs, boolean isManual,
                                               ScheduledExecutorService scheduledExecutor) {
        CancellableFlush flushTask = new CancellableFlush(() -> flushBuffer(batchKey), isManual);
        ScheduledFuture<?> scheduledFuture = scheduledExecutor.scheduleAtFixedRate(() -> {
            flushTask.run();
            flushTask.reset();
        }, initialDelay, timeOutInMs, TimeUnit.MILLISECONDS);
        return new ScheduledFlush(flushTask, scheduledFuture);
    }

    public void close() {
        try {
            requestsAndResponsesMaps.forEach((key, batchBuffer) -> batchBuffer.cancelScheduledFlush());
            requestsAndResponsesMaps.forEach((key, value) -> flushBuffer(key));
            for (BatchBuffer<RequestT, ResponseT> idToResponse : requestsAndResponsesMaps.values()) {
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
        }
    }

    public static final class Builder<RequestT, ResponseT, BatchResponseT> {

        private BatchOverrideConfiguration overrideConfiguration;
        private BatchAndSend<RequestT, BatchResponseT> batchingFunction;
        private BatchResponseMapper<BatchResponseT, ResponseT> mapResponsesFunction;
        private BatchKeyMapper<RequestT> batchKeyMapperFunction;

        private Builder() {
        }

        public Builder<RequestT, ResponseT, BatchResponseT> overrideConfiguration(BatchOverrideConfiguration
                                                                                      overrideConfiguration) {
            this.overrideConfiguration = overrideConfiguration;
            return this;
        }

        public Builder<RequestT, ResponseT, BatchResponseT> batchingFunction(BatchAndSend<RequestT, BatchResponseT>
                                                                                 batchingFunction) {
            this.batchingFunction = batchingFunction;
            return this;
        }

        public Builder<RequestT, ResponseT, BatchResponseT> mapResponsesFunction(
            BatchResponseMapper<BatchResponseT, ResponseT> mapResponsesFunction) {
            this.mapResponsesFunction = mapResponsesFunction;
            return this;
        }

        public Builder<RequestT, ResponseT, BatchResponseT> batchKeyMapperFunction(BatchKeyMapper<RequestT>
                                                                                     batchKeyMapperFunction) {
            this.batchKeyMapperFunction = batchKeyMapperFunction;
            return this;
        }

        public BatchManager<RequestT, ResponseT, BatchResponseT> build() {
            return new BatchManager<>(this);
        }
    }
}
