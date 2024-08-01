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

package software.amazon.awssdk.services.sqs.internal.batchmanager;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.sqs.batchmanager.BatchOverrideConfiguration;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public abstract class RequestBatchManager<RequestT, ResponseT, BatchResponseT> {
    private final int maxBatchItems;
    private final Duration maxBatchOpenInMs;
    private final BatchingMap<RequestT, ResponseT> requestsAndResponsesMaps;
    private final BatchAndSend<RequestT, BatchResponseT> batchFunction;
    private final BatchResponseMapper<BatchResponseT, ResponseT> responseMapper;
    private final BatchKeyMapper<RequestT> batchKeyMapper;
    private final ScheduledExecutorService scheduledExecutor;

    protected RequestBatchManager(DefaultBuilder builder) {
        BatchConfiguration batchConfiguration = new BatchConfiguration(builder.overrideConfiguration);
        this.requestsAndResponsesMaps = new BatchingMap<>(batchConfiguration.maxBatchKeys(),
                                                          batchConfiguration.maxBufferSize(),
                                                          RequestBatchBuffer::new);
        this.maxBatchItems = batchConfiguration.maxBatchItems();
        this.maxBatchOpenInMs = batchConfiguration.maxBatchOpenInMs();
        this.batchFunction = Validate.notNull(builder.batchFunction, "Null batchFunction");
        this.responseMapper = Validate.notNull(builder.responseMapper, "Null responseMapper");
        this.batchKeyMapper = Validate.notNull(builder.batchKeyMapper, "Null batchKeyMapper");
        this.scheduledExecutor = Validate.notNull(builder.scheduledExecutor, "Null scheduledExecutor");
    }

    public CompletableFuture<ResponseT> batchRequest(RequestT request) {
        CompletableFuture<ResponseT> response = new CompletableFuture<>();
        try {
            String batchKey = batchKeyMapper.getBatchKey(request);
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

    private void flushBuffer(String batchKey, Map<String, BatchingExecutionContext<RequestT, ResponseT>> flushableRequests) {
        List<IdentifiableMessage<RequestT>> requestEntries = new ArrayList<>();
        flushableRequests.forEach((contextId, batchExecutionContext) ->
                                      requestEntries.add(new IdentifiableMessage<>(contextId, batchExecutionContext.request())));
        if (!requestEntries.isEmpty()) {
            batchFunction.batchAndSend(requestEntries, batchKey)
                         .whenComplete((result, ex) -> handleAndCompleteResponses(result, ex, flushableRequests));
        }
    }

    private void handleAndCompleteResponses(BatchResponseT batchResult, Throwable exception,
                                            Map<String, BatchingExecutionContext<RequestT, ResponseT>> requests) {
        if (exception != null) {
            requests.forEach((contextId, batchExecutionContext) -> batchExecutionContext.response()
                                                                                        .completeExceptionally(exception));
        } else {
            responseMapper.mapBatchResponse(batchResult)
                          .forEach(
                              response -> response.map(actualResponse -> requests.get(actualResponse.id())
                                                                                 .response()
                                                                                 .complete(actualResponse.message()),
                                                       throwable -> requests.get(throwable.id())
                                                                            .response()
                                                                            .completeExceptionally(throwable.message())));
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

    public void close() {
        requestsAndResponsesMaps.forEach((batchKey, batchBuffer) -> {
            requestsAndResponsesMaps.cancelScheduledFlush(batchKey);
            Map<String, BatchingExecutionContext<RequestT, ResponseT>> flushableRequests =
                requestsAndResponsesMaps.flushableRequests(batchKey, maxBatchItems);

            while (!flushableRequests.isEmpty()) {
                flushBuffer(batchKey, flushableRequests);
            }
        });
        requestsAndResponsesMaps.clear();
    }

    public abstract static class DefaultBuilder<RequestT, ResponseT, BatchResponseT, B
        extends DefaultBuilder<RequestT, ResponseT, BatchResponseT, B>>
        implements BatchManagerBuilder<RequestT, ResponseT, BatchResponseT, B> {

        private BatchOverrideConfiguration overrideConfiguration;
        private ScheduledExecutorService scheduledExecutor;
        private BatchAndSend<RequestT, BatchResponseT> batchFunction;
        private BatchResponseMapper<BatchResponseT, ResponseT> responseMapper;
        private BatchKeyMapper<RequestT> batchKeyMapper;

        protected DefaultBuilder() {
        }

        @SuppressWarnings("unchecked")
        protected B self() {
            return (B) this;
        }

        @Override
        public B overrideConfiguration(BatchOverrideConfiguration overrideConfiguration) {
            this.overrideConfiguration = overrideConfiguration;
            return self();
        }

        @Override
        public B overrideConfiguration(Consumer<BatchOverrideConfiguration.Builder> overrideConfigurationConsumer) {
            BatchOverrideConfiguration.Builder builder = BatchOverrideConfiguration.builder();
            overrideConfigurationConsumer.accept(builder);
            this.overrideConfiguration = builder.build();
            return self();
        }

        @Override
        public B scheduledExecutor(ScheduledExecutorService scheduledExecutor) {
            this.scheduledExecutor = scheduledExecutor;
            return self();
        }

        @Override
        public B batchFunction(BatchAndSend<RequestT, BatchResponseT> batchFunction) {
            this.batchFunction = batchFunction;
            return self();
        }

        @Override
        public B responseMapper(BatchResponseMapper<BatchResponseT, ResponseT> responseMapper) {
            this.responseMapper = responseMapper;
            return self();
        }

        @Override
        public B batchKeyMapper(BatchKeyMapper<RequestT> batchKeyMapper) {
            this.batchKeyMapper = batchKeyMapper;
            return self();
        }

        public abstract RequestBatchManager<RequestT, ResponseT, BatchResponseT> build();
    }
}