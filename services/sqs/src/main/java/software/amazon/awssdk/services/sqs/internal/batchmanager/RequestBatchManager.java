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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private final Duration shutdownTimeout;
    private final BatchingMap<RequestT, ResponseT> requestsAndResponsesMaps;
    private final ScheduledExecutorService scheduledExecutor;
    private final Set<CompletableFuture<BatchResponseT>> pendingBatchResponses ;
    private final Set<CompletableFuture<ResponseT>> pendingResponses ;
    private final AtomicBoolean closed = new AtomicBoolean(false);


    protected RequestBatchManager(RequestBatchConfiguration overrideConfiguration,
                                  ScheduledExecutorService scheduledExecutor) {
        batchConfiguration = overrideConfiguration;
        this.maxBatchItems = batchConfiguration.maxBatchItems();
        this.sendRequestFrequency = batchConfiguration.sendRequestFrequency();
        this.shutdownTimeout = batchConfiguration.shutdownTimeout();
        this.scheduledExecutor = Validate.notNull(scheduledExecutor, "Null scheduledExecutor");
        pendingBatchResponses = ConcurrentHashMap.newKeySet();
        pendingResponses = ConcurrentHashMap.newKeySet();
        this.requestsAndResponsesMaps = new BatchingMap<>(overrideConfiguration);

    }

    public CompletableFuture<ResponseT> batchRequest(RequestT request) {
        CompletableFuture<ResponseT> response = new CompletableFuture<>();
        if (closed.get()) {
            response.completeExceptionally(new IllegalStateException("The client has been shut down."));
            return response;
        }
        pendingResponses.add(response);
        response.whenComplete((r, t) -> pendingResponses.remove(response));

        try {
            BatchKey batchKey = getBatchKey(request);
            // Handle potential byte size overflow only if there are request in map and if feature enabled
            if (requestsAndResponsesMaps.contains(batchKey) && batchConfiguration.maxBatchBytesSize() > 0) {
                Optional.of(requestsAndResponsesMaps.extractBatchIfSizeExceeded(batchKey, request))
                        .filter(extractedEntries -> !extractedEntries.isEmpty())
                        .ifPresent(extractedEntries -> manualFlushBuffer(batchKey, extractedEntries));
            }

            // Add request and response to the map, scheduling a flush if necessary
            requestsAndResponsesMaps.put(batchKey,
                                         () -> scheduleBufferFlush(batchKey,
                                                                   sendRequestFrequency.toMillis(),
                                                                   scheduledExecutor),
                                         request,
                                         response);

            // Immediately flush if the batch is full
            Optional.of(requestsAndResponsesMaps.extractBatchIfReady(batchKey))
                    .filter(extractedEntries -> !extractedEntries.isEmpty())
                    .ifPresent(extractedEntries -> manualFlushBuffer(batchKey, extractedEntries));

        } catch (Exception e) {
            response.completeExceptionally(e);
        }

        return response;
    }

    protected abstract CompletableFuture<BatchResponseT> batchAndSend(List<IdentifiableMessage<RequestT>> identifiedRequests,
                                                                      BatchKey batchKey);

    /**
     * Returns the key that identifies the buffer this request is batched in. Requests that must not be signed or dispatched
     * together must map to keys that are not {@link BatchKey#equals(Object)}, and the returned key - not any individual
     * buffered request - is the source of truth for the queue URL and the request override configuration the batch is sent
     * with.
     */
    protected abstract BatchKey getBatchKey(RequestT request);

    protected abstract List<Either<IdentifiableMessage<ResponseT>,
        IdentifiableMessage<Throwable>>> mapBatchResponse(BatchResponseT batchResponse);

    /**
     * Returns the request override configuration a batch for the given key must be sent with: the configuration that defines
     * the group, with the automatic-batching-manager user agent appended.
     */
    protected static AwsRequestOverrideConfiguration batchOverrideConfiguration(BatchKey batchKey) {
        return batchKey.overrideConfiguration()
                       .map(overrideConfig -> overrideConfig.toBuilder().applyMutation(USER_AGENT_APPLIER).build())
                       .orElseGet(() -> AwsRequestOverrideConfiguration.builder()
                                                                       .applyMutation(USER_AGENT_APPLIER)
                                                                       .build());
    }

    private void manualFlushBuffer(BatchKey batchKey,
                                   Map<String, BatchingExecutionContext<RequestT, ResponseT>> flushableRequests) {
        flushBuffer(batchKey, flushableRequests);
        requestsAndResponsesMaps.cancelAndReplaceScheduledFlush(batchKey,
                                                                scheduleBufferFlush(batchKey,
                                                                       sendRequestFrequency.toMillis(),
                                                                       scheduledExecutor));
    }

    private void flushBuffer(BatchKey batchKey, Map<String, BatchingExecutionContext<RequestT, ResponseT>> flushableRequests) {
        List<IdentifiableMessage<RequestT>> requestEntries = new ArrayList<>();
        flushableRequests.forEach((contextId, batchExecutionContext) ->
                                      requestEntries.add(new IdentifiableMessage<>(contextId, batchExecutionContext.request())));
        if (!requestEntries.isEmpty()) {
            CompletableFuture<BatchResponseT> pendingBatchingRequest = batchAndSend(requestEntries, batchKey);
            pendingBatchResponses.add(pendingBatchingRequest);
            pendingBatchingRequest.whenComplete((result, ex) -> {
                handleAndCompleteResponses(result, ex, flushableRequests);
                pendingBatchResponses.remove(pendingBatchingRequest);
            });
        }
    }

    private void handleAndCompleteResponses(BatchResponseT batchResult, Throwable exception,
                                            Map<String, BatchingExecutionContext<RequestT, ResponseT>> requests) {
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

    private ScheduledFuture<?> scheduleBufferFlush(BatchKey batchKey, long timeOutInMs,
                                                   ScheduledExecutorService scheduledExecutor) {
        return scheduledExecutor.scheduleAtFixedRate(() -> performScheduledFlush(batchKey), timeOutInMs, timeOutInMs,
                                                     TimeUnit.MILLISECONDS);
    }

    private void performScheduledFlush(BatchKey batchKey) {
        Map<String, BatchingExecutionContext<RequestT, ResponseT>> extractedEntries =
            requestsAndResponsesMaps.extractEntriesForScheduledFlush(batchKey, maxBatchItems);
        if (!extractedEntries.isEmpty()) {
            flushBuffer(batchKey, extractedEntries);
        }
    }

    /**
     * Phase 1 of shutdown, non-blocking. Marks the manager closed, dispatches every buffered batch (each exactly
     * once), and returns a future that completes when all in-flight sends dispatched here have completed, so the
     * caller can wait on it. Does not block and does not cancel; a second call is a no-op that returns an
     * already-completed future.
     */
    protected CompletableFuture<Void> closeAndDispatch() {
        if (!closed.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(null);
        }
        drainBuffers();
        List<CompletableFuture<ResponseT>> snapshot = new ArrayList<>(pendingResponses);
        return CompletableFuture.allOf(snapshot.toArray(new CompletableFuture[0]));
    }

    /**
     * Phase 2 of shutdown. Cancels any requests still pending after the timeout, surfacing
     * {@link java.util.concurrent.CancellationException} to their callers, and releases the buffers. Idempotent.
     */
    protected void cancelPending() {
        pendingResponses.forEach(future -> future.cancel(true));
        pendingBatchResponses.forEach(future -> future.cancel(true));
        requestsAndResponsesMaps.clear();
    }

    protected Duration shutdownTimeout() {
        return shutdownTimeout;
    }

    private void drainBuffers() {
        requestsAndResponsesMaps.forEach((batchKey, batchBuffer) -> {
            requestsAndResponsesMaps.cancelScheduledFlush(batchKey);
            Map<String, BatchingExecutionContext<RequestT, ResponseT>> extractedEntries =
                requestsAndResponsesMaps.extractEntriesForScheduledFlush(batchKey, maxBatchItems);
            while (!extractedEntries.isEmpty()) {
                flushBuffer(batchKey, extractedEntries);
                extractedEntries = requestsAndResponsesMaps.extractEntriesForScheduledFlush(batchKey, maxBatchItems);
            }
        });
    }

}