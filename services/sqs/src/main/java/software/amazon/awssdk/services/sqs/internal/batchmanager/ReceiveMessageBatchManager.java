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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * Routes batchable receive requests to one {@link ReceiveBatchManager} (a receive buffer) per queue URL.
 *
 * <p>At most {@link RequestBatchConfiguration#DEFAULT_MAX_BATCH_KEYS} buffers are tracked. At the limit, a request for a
 * new queue evicts the buffer that has been idle (no caller request in progress) the longest. If every buffer is in use,
 * the request fails with an {@link IllegalStateException}. A buffer whose queue-attribute lookup failed is removed once
 * it is idle, so queue URLs that don't exist or can't be read don't keep a slot.
 *
 * <p>One private lock guards the buffer map, the idle set and the per-entry state. Buffers are closed and requests
 * are processed outside the lock.
 */
@SdkInternalApi
public class ReceiveMessageBatchManager implements SdkAutoCloseable {

    private static final Logger log = Logger.loggerFor(ReceiveMessageBatchManager.class);

    private final SqsAsyncClient sqsClient;
    private final ScheduledExecutorService executor;
    private final ResponseBatchConfiguration config;
    private final int maxBuffers;
    private final Object lock = new Object();
    // guarded by lock
    private final Map<String, Entry> buffers = new HashMap<>();
    // guarded by lock; entries with inUse == 0, oldest first
    private final Set<Entry> idle = new LinkedHashSet<>();
    // written only under lock
    private volatile boolean closed;

    public ReceiveMessageBatchManager(SqsAsyncClient sqsClient,
                                      ScheduledExecutorService executor,
                                      ResponseBatchConfiguration config) {
        this(sqsClient, executor, config, RequestBatchConfiguration.DEFAULT_MAX_BATCH_KEYS);
    }

    @SdkTestInternalApi
    ReceiveMessageBatchManager(SqsAsyncClient sqsClient,
                               ScheduledExecutorService executor,
                               ResponseBatchConfiguration config,
                               int maxBuffers) {
        this.sqsClient = sqsClient;
        this.executor = executor;
        this.config = config;
        this.maxBuffers = maxBuffers;
    }

    public CompletableFuture<ReceiveMessageResponse> batchRequest(ReceiveMessageRequest request) {
        if (closed) {
            return failedFuture(new IllegalStateException("The client has been shut down."));
        }
        String ineligibleReason = checkBatchingEligibility(request);
        if (ineligibleReason != null) {
            log.debug(() -> String.format("Batching skipped. Reason: %s", ineligibleReason));
            return failedOnThrow(() -> sqsClient.receiveMessage(request));
        }
        Admission admission;
        try {
            admission = admit(request.queueUrl());
        } catch (RuntimeException e) {
            return failedFuture(e);
        }
        if (admission.evicted != null) {
            closeQuietly(admission.evicted.manager);
        }
        Entry admitted = admission.entry;
        CompletableFuture<ReceiveMessageResponse> response = failedOnThrow(() -> admitted.manager.processRequest(request));
        response.whenComplete((r, t) -> release(admitted));
        return response;
    }

    /**
     * Returns the entry for the queue, marked in use. Throws if the manager is closed, if the limit is reached and every
     * buffer is in use, or if the buffer can't be created.
     */
    private Admission admit(String queueUrl) {
        synchronized (lock) {
            if (closed) {
                throw new IllegalStateException("The client has been shut down.");
            }
            Entry entry = buffers.get(queueUrl);
            if (entry != null) {
                idle.remove(entry);
                entry.inUse++;
                return new Admission(entry, null);
            }
            return addEntry(queueUrl);
        }
    }

    /**
     * Adds an in-use entry for a queue that has none. At the limit, it also removes the oldest idle entry, which the caller
     * closes after releasing the lock. Caller holds lock.
     */
    private Admission addEntry(String queueUrl) {
        boolean atLimit = buffers.size() >= maxBuffers;
        if (atLimit && idle.isEmpty()) {
            throw new IllegalStateException(
                "Reached the maximum of " + maxBuffers + " receive buffers, and every buffer is in use.");
        }
        // Create before evicting, so a failed creation never costs an idle queue its buffer.
        ReceiveBatchManager manager = createReceiveBatchManager(queueUrl);
        Entry evicted = atLimit ? removeOldestIdle() : null;
        Entry entry = new Entry(queueUrl, manager);
        entry.inUse++;
        buffers.put(queueUrl, entry);
        return new Admission(entry, evicted);
    }

    @SdkTestInternalApi
    protected ReceiveBatchManager createReceiveBatchManager(String queueUrl) {
        return new ReceiveBatchManager(sqsClient, executor, config, queueUrl);
    }

    @SdkTestInternalApi
    int batchKeyCount() {
        synchronized (lock) {
            return buffers.size();
        }
    }

    @SdkTestInternalApi
    int idleCount() {
        synchronized (lock) {
            return idle.size();
        }
    }

    @Override
    public void close() {
        List<Entry> all;
        synchronized (lock) {
            if (closed) {
                return;
            }
            closed = true;
            all = new ArrayList<>(buffers.values());
            buffers.clear();
            idle.clear();
        }
        all.forEach(e -> closeQuietly(e.manager));
    }

    private void release(Entry entry) {
        synchronized (lock) {
            entry.inUse--;
            if (entry.inUse == 0 && buffers.get(entry.queueUrl) == entry) {
                idle.add(entry);
            }
        }
        CompletableFuture<?> lookup = entry.manager.attributeLookup();
        if (lookup == null) {
            return;
        }
        if (lookup.isDone()) {
            if (lookup.isCompletedExceptionally()) {
                removeIfIdle(entry);
            }
        } else if (watchOnce(entry, lookup)) {
            lookup.whenComplete((r, t) -> {
                if (t != null) {
                    removeIfIdle(entry);
                }
            });
        }
    }

    private boolean watchOnce(Entry entry, CompletableFuture<?> lookup) {
        synchronized (lock) {
            if (entry.watchedLookup == lookup) {
                return false;
            }
            entry.watchedLookup = lookup;
            return true;
        }
    }

    private void removeIfIdle(Entry entry) {
        boolean removed;
        synchronized (lock) {
            removed = entry.inUse == 0 && buffers.remove(entry.queueUrl, entry);
            if (removed) {
                idle.remove(entry);
            }
        }
        if (removed) {
            log.debug(() -> "Removed the receive buffer for queueUrl: " + entry.queueUrl
                            + " because its queue attribute lookup failed.");
            closeQuietly(entry.manager);
        }
    }

    /**
     * Removes and returns the entry that has been idle the longest, or null if no entry is idle. Caller holds lock.
     */
    private Entry removeOldestIdle() {
        Iterator<Entry> it = idle.iterator();
        if (!it.hasNext()) {
            return null;
        }
        Entry oldest = it.next();
        it.remove();
        buffers.remove(oldest.queueUrl);
        return oldest;
    }

    private static void closeQuietly(ReceiveBatchManager manager) {
        try {
            manager.close();
        } catch (RuntimeException e) {
            log.warn(() -> "Failed to close a receive buffer. Its prefetched messages reappear after their visibility timeout.",
                     e);
        }
    }

    private static <T> CompletableFuture<T> failedOnThrow(Supplier<CompletableFuture<T>> call) {
        try {
            return call.get();
        } catch (RuntimeException e) {
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

    /**
     * The entry a request was admitted to, and the entry evicted to make room for it, if any.
     */
    private static final class Admission {
        private final Entry entry;
        private final Entry evicted;

        Admission(Entry entry, Entry evicted) {
            this.entry = entry;
            this.evicted = evicted;
        }
    }

    /**
     * A tracked receive buffer. Uses identity equality, which the idle set relies on.
     */
    private static final class Entry {
        private final String queueUrl;
        private final ReceiveBatchManager manager;
        // guarded by the outer manager's lock
        private int inUse;
        // guarded by the outer manager's lock
        private CompletableFuture<?> watchedLookup;

        Entry(String queueUrl, ReceiveBatchManager manager) {
            this.queueUrl = queueUrl;
            this.manager = manager;
        }
    }
}
