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

import static software.amazon.awssdk.utils.CompletableFutureUtils.failedFuture;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * Sends batchable receive requests to the queue's buffer and the rest directly to SQS. A queue gets no buffer until its
 * attributes load, so queue URLs that don't exist never take a slot.
 */
@SdkInternalApi
public class ReceiveMessageBatchManager implements SdkAutoCloseable {

    private static final Logger log = Logger.loggerFor(ReceiveMessageBatchManager.class);

    private final SqsAsyncClient sqsClient;
    private final ScheduledExecutorService executor;
    private final ResponseBatchConfiguration config;
    private final ReceiveBufferMap buffers;
    // In-flight attribute loads, shared by concurrent first requests for a queue.
    private final ConcurrentMap<String, CompletableFuture<ReceiveBufferMap.Entry>> loading = new ConcurrentHashMap<>();

    public ReceiveMessageBatchManager(SqsAsyncClient sqsClient,
                                      ScheduledExecutorService executor,
                                      ResponseBatchConfiguration config) {
        this(sqsClient, executor, config, RequestBatchConfiguration.DEFAULT_MAX_BATCH_KEYS);
    }

    ReceiveMessageBatchManager(SqsAsyncClient sqsClient,
                               ScheduledExecutorService executor,
                               ResponseBatchConfiguration config,
                               int maxBuffers) {
        this.sqsClient = sqsClient;
        this.executor = executor;
        this.config = config;
        this.buffers = new ReceiveBufferMap(maxBuffers);
    }

    public CompletableFuture<ReceiveMessageResponse> batchRequest(ReceiveMessageRequest request) {
        if (buffers.isClosed()) {
            return failedFuture(new IllegalStateException("The client has been shut down."));
        }
        String ineligibleReason = checkBatchingEligibility(request);
        if (ineligibleReason != null) {
            log.debug(() -> String.format("Batching skipped. Reason: %s", ineligibleReason));
            return failedOnThrow(() -> sqsClient.receiveMessage(request));
        }
        String queueUrl = request.queueUrl();
        return failedOnThrow(() -> {
            ReceiveBufferMap.Entry entry = buffers.acquire(queueUrl);
            if (entry != null) {
                return process(entry, request);
            }
            CompletableFuture<ReceiveBufferMap.Entry> loaded = load(queueUrl);
            return loaded.thenCompose(candidate -> {
                ReceiveBufferMap.Entry added = buffers.add(candidate);
                if (added == null) {
                    // Evicted before this request joined it, so drop the stale lookup and load the queue again.
                    loading.remove(queueUrl, loaded);
                    return batchRequest(request);
                }
                return process(added, request);
            });
        });
    }

    private CompletableFuture<ReceiveBufferMap.Entry> load(String queueUrl) {
        CompletableFuture<ReceiveBufferMap.Entry> created = new CompletableFuture<>();
        CompletableFuture<ReceiveBufferMap.Entry> inFlight = loading.putIfAbsent(queueUrl, created);
        if (inFlight != null) {
            return inFlight;
        }
        // Outside the map operation, because the SDK call resolves credentials on this thread.
        try {
            ReceiveBatchManager manager = createReceiveBatchManager(queueUrl);
            manager.loadQueueAttributes().whenComplete((loaded, t) -> {
                if (t != null) {
                    created.completeExceptionally(t);
                } else {
                    created.complete(new ReceiveBufferMap.Entry(queueUrl, manager));
                }
                // Remove after completing, so late arrivals share this result.
                loading.remove(queueUrl, created);
            });
        } catch (Throwable e) {
            created.completeExceptionally(e);
            loading.remove(queueUrl, created);
        }
        return created;
    }

    private CompletableFuture<ReceiveMessageResponse> process(ReceiveBufferMap.Entry entry, ReceiveMessageRequest request) {
        CompletableFuture<ReceiveMessageResponse> response = failedOnThrow(() -> entry.manager().processRequest(request));
        response.whenComplete((r, t) -> buffers.release(entry));
        return response;
    }

    protected ReceiveBatchManager createReceiveBatchManager(String queueUrl) {
        return new ReceiveBatchManager(sqsClient, executor, config, queueUrl);
    }

    int batchKeyCount() {
        return buffers.size();
    }

    int idleCount() {
        return buffers.idleCount();
    }

    @Override
    public void close() {
        buffers.close();
    }

    private static <T> CompletableFuture<T> failedOnThrow(Supplier<CompletableFuture<T>> call) {
        try {
            return call.get();
        } catch (Throwable e) {
            return failedFuture(e);
        }
    }

    private String checkBatchingEligibility(ReceiveMessageRequest rq) {
        if (!hasCompatibleAttributes(rq)) {
            return "Incompatible attributes.";
        }
        if (rq.visibilityTimeout() != null) {
            return "Visibility timeout is set.";
        }
        if (!isBufferingEnabled()) {
            return "Buffering is disabled.";
        }
        if (rq.overrideConfiguration().isPresent()) {
            return "Request has override configurations.";
        }
        return null;
    }

    private boolean hasCompatibleAttributes(ReceiveMessageRequest rq) {
        return !rq.hasAttributeNames()
               && hasCompatibleSystemAttributes(rq)
               && hasCompatibleMessageAttributes(rq);
    }

    private boolean hasCompatibleSystemAttributes(ReceiveMessageRequest rq) {
        return !rq.hasMessageSystemAttributeNames()
               || config.messageSystemAttributeNames().equals(rq.messageSystemAttributeNames());
    }

    private boolean hasCompatibleMessageAttributes(ReceiveMessageRequest rq) {
        return !rq.hasMessageAttributeNames()
               || config.receiveMessageAttributeNames().equals(rq.messageAttributeNames());
    }

    private boolean isBufferingEnabled() {
        return config.maxInflightReceiveBatches() > 0 && config.maxDoneReceiveBatches() > 0;
    }
}
