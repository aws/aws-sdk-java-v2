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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.sqs.batchmanager.BatchOverrideConfiguration;
import software.amazon.awssdk.utils.Either;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public abstract class RequestBatchManager<RequestT, ResponseT, BatchResponseT> {
    private final int maxBatchItems;
    private final Duration maxBatchOpenDuration;
    private final BatchingMap<RequestT, ResponseT> requestsAndResponsesMaps;
    private final ScheduledExecutorService scheduledExecutor;

    private final Set<CompletableFuture<BatchResponseT>> pendingBatchResponses ;
    private final Set<CompletableFuture<ResponseT>> pendingResponses ;

    protected RequestBatchManager(BatchOverrideConfiguration overrideConfiguration, ScheduledExecutorService scheduledExecutor) {
        RequestBatchConfiguration batchConfiguration = new RequestBatchConfiguration(overrideConfiguration);
        this.requestsAndResponsesMaps = new BatchingMap<>(batchConfiguration.maxBatchKeys(),
                                                          batchConfiguration.maxBufferSize());
        this.maxBatchItems = batchConfiguration.maxBatchItems();
        this.maxBatchOpenDuration = batchConfiguration.maxBatchOpenDuration();
        this.scheduledExecutor = Validate.notNull(scheduledExecutor, "Null scheduledExecutor");
        pendingBatchResponses = Collections.newSetFromMap(new ConcurrentHashMap<>());
        pendingResponses = Collections.newSetFromMap(new ConcurrentHashMap<>());
    }

    public CompletableFuture<ResponseT> batchRequest(RequestT request) {
        CompletableFuture<ResponseT> response = new CompletableFuture<>();
        pendingResponses.add(response);
        try {
            String batchKey = getBatchKey(request);
            requestsAndResponsesMaps.put(batchKey,
                                         () -> scheduleBufferFlush(batchKey, maxBatchOpenDuration.toMillis(), scheduledExecutor),
                                         request,
                                         response);
            flushBufferIfNeeded(batchKey);
        } catch (Exception e) {
            response.completeExceptionally(e);
        }
        return response;
    }

    protected abstract CompletableFuture<BatchResponseT> batchAndSend(List<IdentifiableMessage<RequestT>> identifiedRequests,
                                                                      String batchKey);

    protected abstract String getBatchKey(RequestT request);

    protected abstract List<Either<IdentifiableMessage<ResponseT>,
        IdentifiableMessage<Throwable>>> mapBatchResponse(BatchResponseT batchResponse);


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
        requestsAndResponsesMaps.putScheduledFlush(batchKey, scheduleBufferFlush(batchKey, maxBatchOpenDuration.toMillis(),
                                                                                 scheduledExecutor));
    }

    private void flushBuffer(String batchKey, Map<String, BatchingExecutionContext<RequestT, ResponseT>> flushableRequests) {
        List<IdentifiableMessage<RequestT>> requestEntries = new ArrayList<>();
        flushableRequests.forEach((contextId, batchExecutionContext) ->
                                      requestEntries.add(new IdentifiableMessage<>(contextId, batchExecutionContext.request())));
        if (!requestEntries.isEmpty()) {
            CompletableFuture<BatchResponseT> pendingBatchingRequest = batchAndSend(requestEntries, batchKey)
                .whenComplete((result, ex) -> handleAndCompleteResponses(result, ex, flushableRequests));

            pendingBatchResponses.add(pendingBatchingRequest);
        }
    }

    private void handleAndCompleteResponses(BatchResponseT batchResult, Throwable exception,
                                            Map<String, BatchingExecutionContext<RequestT, ResponseT>> requests) {
        requests.forEach((contextId, batchExecutionContext) ->  pendingResponses.add(batchExecutionContext.response()));
        if (exception != null) {
            requests.forEach((contextId, batchExecutionContext) -> batchExecutionContext.response()
                                                                                        .completeExceptionally(exception));
        } else {
            mapBatchResponse(batchResult)
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
        pendingBatchResponses.forEach(future -> future.cancel(true));
        pendingResponses.forEach(future -> future.cancel(true));
        requestsAndResponsesMaps.clear();
    }
}