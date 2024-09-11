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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.utils.Either;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public abstract class RequestBatchManager<RequestT, ResponseT, BatchResponseT> {


    // abm stands for Automatic Batching Manager
    public static final Consumer<AwsRequestOverrideConfiguration.Builder> USER_AGENT_APPLIER =
        b -> b.addApiName(ApiName.builder().version("abm").name("hll").build());

    protected final RequestBatchConfiguration batchConfiguration ;

    private final int maxBatchItems;
    private final Duration sendRequestFrequency;
    private final BatchingMap<RequestT, ResponseT> requestsAndResponsesMaps;
    private final ScheduledExecutorService scheduledExecutor;
    private final Set<CompletableFuture<BatchResponseT>> pendingBatchResponses ;
    private final Set<CompletableFuture<ResponseT>> pendingResponses ;


    protected RequestBatchManager(RequestBatchConfiguration overrideConfiguration,
                                  ScheduledExecutorService scheduledExecutor) {
        batchConfiguration = overrideConfiguration;
        this.maxBatchItems = batchConfiguration.maxBatchItems();
        this.sendRequestFrequency = batchConfiguration.sendRequestFrequency();
        this.scheduledExecutor = Validate.notNull(scheduledExecutor, "Null scheduledExecutor");
        pendingBatchResponses = Collections.newSetFromMap(new ConcurrentHashMap<>());
        pendingResponses = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.requestsAndResponsesMaps = new BatchingMap<>(overrideConfiguration);

    }

    public CompletableFuture<ResponseT> batchRequest(RequestT request) {
        CompletableFuture<ResponseT> response = new CompletableFuture<>();
        pendingResponses.add(response);

        try {
            String batchKey = getBatchKey(request);
            // Handle potential byte size overflow only if there are request in map and if feature enabled
            if (requestsAndResponsesMaps.contains(batchKey) && batchConfiguration.maxBatchBytesSize() > 0) {
                Optional.of(requestsAndResponsesMaps.flushableRequestsOnByteLimitBeforeAdd(batchKey, request))
                        .filter(flushableRequests -> !flushableRequests.isEmpty())
                        .ifPresent(flushableRequests -> manualFlushBuffer(batchKey, flushableRequests));
            }

            // Add request and response to the map, scheduling a flush if necessary
            requestsAndResponsesMaps.put(batchKey,
                                         () -> scheduleBufferFlush(batchKey,
                                                                   sendRequestFrequency.toMillis(),
                                                                   scheduledExecutor),
                                         request,
                                         response);

            // Immediately flush if the batch is full
            Optional.of(requestsAndResponsesMaps.flushableRequests(batchKey))
                    .filter(flushableRequests -> !flushableRequests.isEmpty())
                    .ifPresent(flushableRequests -> manualFlushBuffer(batchKey, flushableRequests));

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

    private void manualFlushBuffer(String batchKey,
                                   Map<String, BatchingExecutionContext<RequestT, ResponseT>> flushableRequests) {
        requestsAndResponsesMaps.cancelScheduledFlush(batchKey);
        flushBuffer(batchKey, flushableRequests);
        requestsAndResponsesMaps.putScheduledFlush(batchKey,
                                                   scheduleBufferFlush(batchKey,
                                                                       sendRequestFrequency.toMillis(),
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
                requestsAndResponsesMaps.flushableRequests(batchKey);

            while (!flushableRequests.isEmpty()) {
                flushBuffer(batchKey, flushableRequests);
            }

        });
        pendingBatchResponses.forEach(future -> future.cancel(true));
        pendingResponses.forEach(future -> future.cancel(true));
        requestsAndResponsesMaps.clear();
    }

}