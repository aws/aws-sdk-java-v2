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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.batchmanager.BatchManager;
import software.amazon.awssdk.core.batchmanager.BatchOverrideConfiguration;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class DefaultBatchManager<RequestT, ResponseT, BatchResponseT> implements BatchManager<RequestT, ResponseT,
    BatchResponseT> {

    private static final Logger log = Logger.loggerFor(DefaultBatchManager.class);
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

    private DefaultBatchManager(DefaultBuilder<RequestT, ResponseT, BatchResponseT> builder) {
        BatchConfiguration batchConfiguration = new BatchConfiguration(builder.overrideConfiguration);
        this.requestsAndResponsesMaps = new BatchingMap<>();
        this.maxBatchItems = batchConfiguration.maxBatchItems();
        this.maxBatchOpenInMs = batchConfiguration.maxBatchOpenInMs();
        this.batchingFunction = Validate.notNull(builder.batchFunction, "Null batchingFunction");
        this.mapResponsesFunction = Validate.notNull(builder.responseMapper, "Null mapResponsesFunction");
        this.batchKeyMapperFunction = Validate.notNull(builder.batchKeyMapper, "Null batchKeyMapperFunction");
        this.scheduledExecutor = builder.scheduledExecutor;
    }

    public static <RequestT, ResponseT, BatchResponseT> Builder<RequestT, ResponseT, BatchResponseT> builder() {
        return new DefaultBuilder<>();
    }

    @Override
    public CompletableFuture<ResponseT> sendRequest(RequestT request) {
        CompletableFuture<ResponseT> response = new CompletableFuture<>();
        try {
            String batchKey = batchKeyMapperFunction.getBatchKey(request);
            requestsAndResponsesMaps.put(batchKey,
                                         () -> scheduleBufferFlush(batchKey, maxBatchOpenInMs.toMillis(), scheduledExecutor),
                                         request,
                                         response);
            flushBufferIfNeeded(batchKey);
        } catch (Exception e) {
            response.completeExceptionally(e);
        }
        return response;
    }

    private void flushBufferIfNeeded(String batchKey) {
        Map<String, BatchingExecutionContext<RequestT, ResponseT>> flushableRequests =
            requestsAndResponsesMaps.flushableRequests(batchKey, maxBatchItems);
        if (!flushableRequests.isEmpty()) {
            manualFlushBuffer(batchKey, flushableRequests);
        }
    }

    private void manualFlushBuffer(String batchKey,
                                   Map<String, BatchingExecutionContext<RequestT, ResponseT>> flushableRequests) {
        requestsAndResponsesMaps.cancelScheduledFlush(batchKey);
        flushBuffer(batchKey, flushableRequests);
        requestsAndResponsesMaps.putScheduledFlush(batchKey, scheduleBufferFlush(batchKey, maxBatchOpenInMs.toMillis(),
                                                                                 scheduledExecutor));
    }

    // Flushes the buffer for the given batchKey and fills in the response map with the returned responses.
    // Returns exception in completableFuture if batchingFunction.apply throws an exception.
    private void flushBuffer(String batchKey, Map<String, BatchingExecutionContext<RequestT, ResponseT>> flushableRequests) {
        List<IdentifiableMessage<RequestT>> requestEntries = new ArrayList<>();
        flushableRequests.forEach((contextId, batchExecutionContext) ->
                                      requestEntries.add(new IdentifiableMessage<>(contextId, batchExecutionContext.request())));

        if (!requestEntries.isEmpty()) {
            batchingFunction.batchAndSend(requestEntries, batchKey)
                            .whenComplete((result, ex) -> handleAndCompleteResponses(result, ex, flushableRequests));
        }
    }

    private void handleAndCompleteResponses(BatchResponseT batchResult, Throwable exception,
                                            Map<String, BatchingExecutionContext<RequestT, ResponseT>> requests) {
        if (exception != null) {
            requests.forEach((contextId, batchExecutionContext) -> batchExecutionContext.response()
                                                                                        .completeExceptionally(exception));
        } else {
            List<IdentifiableMessage<ResponseT>> identifiedResponses = mapResponsesFunction.mapBatchResponse(batchResult);
            for (IdentifiableMessage<ResponseT> identifiedResponse : identifiedResponses) {
                String id = identifiedResponse.id();
                ResponseT response = identifiedResponse.message();
                requests.get(id)
                        .response()
                        .complete(response);
            }
        }
        requests.clear();
    }

    private ScheduledFuture<?> scheduleBufferFlush(String batchKey, long timeOutInMs,
                                           ScheduledExecutorService scheduledExecutor) {
        return scheduledExecutor.scheduleAtFixedRate(() -> performScheduledFlush(batchKey), timeOutInMs, timeOutInMs,
                                                     TimeUnit.MILLISECONDS);
    }

    private void performScheduledFlush(String batchKey) {
        Map<String, BatchingExecutionContext<RequestT, ResponseT>> flushableRequests =
            requestsAndResponsesMaps.flushableScheduledRequests(batchKey, maxBatchItems);
        if (!flushableRequests.isEmpty()) {
            flushBuffer(batchKey, flushableRequests);
        }
    }

    @Override
    public void close() {
        requestsAndResponsesMaps.forEach((batchKey, batchBuffer) -> {
            requestsAndResponsesMaps.cancelScheduledFlush(batchKey);
            Map<String, BatchingExecutionContext<RequestT, ResponseT>> flushableRequests =
                requestsAndResponsesMaps.flushableScheduledRequests(batchKey, maxBatchItems);

            while (!flushableRequests.isEmpty()) {
                flushBuffer(batchKey, flushableRequests);
            }
        });
        requestsAndResponsesMaps.waitForFlushesAndClear(log);
    }

    public static final class DefaultBuilder<RequestT, ResponseT, BatchResponseT> implements Builder<RequestT, ResponseT,
        BatchResponseT> {

        private BatchOverrideConfiguration overrideConfiguration;
        private ScheduledExecutorService scheduledExecutor;
        private BatchAndSend<RequestT, BatchResponseT> batchFunction;
        private BatchResponseMapper<BatchResponseT, ResponseT> responseMapper;
        private BatchKeyMapper<RequestT> batchKeyMapper;

        private DefaultBuilder() {
        }

        @Override
        public Builder<RequestT, ResponseT, BatchResponseT> overrideConfiguration(BatchOverrideConfiguration
                                                                                      overrideConfiguration) {
            this.overrideConfiguration = overrideConfiguration;
            return this;
        }

        @Override
        public Builder<RequestT, ResponseT, BatchResponseT> scheduledExecutor(ScheduledExecutorService scheduledExecutor) {
            this.scheduledExecutor = scheduledExecutor;
            return this;
        }

        @Override
        public Builder<RequestT, ResponseT, BatchResponseT> batchFunction(BatchAndSend<RequestT, BatchResponseT>
                                                                                 batchingFunction) {
            this.batchFunction = batchingFunction;
            return this;
        }

        @Override
        public Builder<RequestT, ResponseT, BatchResponseT> responseMapper(
            BatchResponseMapper<BatchResponseT, ResponseT> responseMapper) {
            this.responseMapper = responseMapper;
            return this;
        }

        @Override
        public Builder<RequestT, ResponseT, BatchResponseT> batchKeyMapper(BatchKeyMapper<RequestT> batchKeyMapper) {
            this.batchKeyMapper = batchKeyMapper;
            return this;
        }

        public DefaultBatchManager<RequestT, ResponseT, BatchResponseT> build() {
            return new DefaultBatchManager<>(this);
        }
    }
}
