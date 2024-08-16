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

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

@SdkInternalApi
public class ReceiveQueueBuffer {

    private final ScheduledExecutorService executor;
    private final SqsAsyncClient sqsClient;
    private final ResponseBatchConfiguration config;
    private final String queueUrl;
    private final  QueueAttributesManager queueAttributesManager;

    private final Queue<ReceiveSqsMessageHelper> finishedTasks = new ConcurrentLinkedQueue<>();
    private final Queue<ReceiveMessageCompletableFuture> futures = new ConcurrentLinkedQueue<>();

    private final AtomicInteger inflightReceiveMessageBatches = new AtomicInteger(0);
    private final AtomicBoolean shutDown = new AtomicBoolean(false);

    private final AtomicBoolean processingFutures = new AtomicBoolean(false);


    public ReceiveQueueBuffer(ScheduledExecutorService executor, SqsAsyncClient sqsClient,
                              ResponseBatchConfiguration config, String queueUrl, QueueAttributesManager queueAttributesManager) {
        this.executor = executor;
        this.sqsClient = sqsClient;
        this.config = config;
        this.queueUrl = queueUrl;
        this.queueAttributesManager = queueAttributesManager;
    }

    public void receiveMessage(ReceiveMessageCompletableFuture receiveMessageFuture) {
        futures.add(receiveMessageFuture);
        satisfyFuturesFromBuffer();
        spawnMoreReceiveTasks();
    }

    public boolean isShutDown() {
        return shutDown.get();
    }

    public void shutdown() {
        if (this.shutDown.compareAndSet(false, true)) {
            // Clear all finished tasks
            while (!finishedTasks.isEmpty()) {
                ReceiveSqsMessageHelper batch = finishedTasks.poll();
                if (inflightReceiveMessageBatches.get() > 0) {
                    inflightReceiveMessageBatches.decrementAndGet();
                }
                if (batch != null) {
                    batch.clear();
                }
            }

            // Clear futures
            futures.forEach(future -> {
                if (!future.responseCompletableFuture().isDone()) {
                    future.setFailure(new CancellationException("Shutdown in progress"));
                }
            });
            futures.clear();
        }
    }


    private void spawnMoreReceiveTasks() {
        if (shutDown.get()) {
            return;
        }

        int desiredBatches = determineDesiredBatches();
        if (finishedTasks.size() >= desiredBatches) {
            return;
        }

        if (!finishedTasks.isEmpty() && (finishedTasks.size() + inflightReceiveMessageBatches.get()) >= desiredBatches) {
            return;
        }

        queueAttributesManager.getVisibilityTimeout().thenAcceptAsync(visibilityTimeout -> {
            int max = Math.max(config.maxInflightReceiveBatches(), 1);
            int toSpawn = max - inflightReceiveMessageBatches.get();
            if (toSpawn > 0) {
                ReceiveSqsMessageHelper receiveSqsMessageHelper = new ReceiveSqsMessageHelper(
                    queueUrl, sqsClient, visibilityTimeout, config);
                inflightReceiveMessageBatches.incrementAndGet();
                receiveSqsMessageHelper.asyncReceiveMessage()
                                       .whenComplete((response, exception) -> reportBatchFinished(response));
            }
        }, executor);
    }

    private int determineDesiredBatches() {
        int desiredBatches = Math.max(config.maxDoneReceiveBatches(), 1);

        if (config.adaptivePrefetching()) {
            int totalRequested = futures.stream()
                                        .mapToInt(ReceiveMessageCompletableFuture::requestedSize)
                                        .sum();
            int batchesNeededToFulfillFutures = (int) Math.ceil((float) totalRequested / config.maxBatchItems());
            desiredBatches = Math.min(batchesNeededToFulfillFutures, desiredBatches);
        }

        return desiredBatches;
    }

    private void fulfillFuture(ReceiveMessageCompletableFuture future, ReceiveSqsMessageHelper messageHelper) {
        List<Message> messages = new LinkedList<>();
        Throwable exception = messageHelper.getException();
        int numRetrieved = 0;
        boolean batchDone = false;

        if (exception != null) {
            future.setFailure(exception);
            finishedTasks.poll();
            return;
        }

        while (numRetrieved < future.requestedSize()) {
            Message msg = messageHelper.removeMessage();
            if (msg != null) {
                messages.add(msg);
                ++numRetrieved;
            } else {
                batchDone = true;
                break;
            }
        }
        batchDone = batchDone || messageHelper.isEmpty();
        if (batchDone) {
            finishedTasks.poll();
        }
        future.setSuccess(ReceiveMessageResponse.builder().messages(messages).build());
    }

    private void satisfyFuturesFromBuffer() {
        pruneExpiredTasks();
        if (!processingFutures.compareAndSet(false, true)) {
            return;
        }
        try {
            futures.forEach(future -> {
                if (!finishedTasks.isEmpty()) {
                    fulfillFuture(future, finishedTasks.peek());
                    futures.remove(future);
                }
            });
        } finally {
            processingFutures.set(false);
        }
    }

    private void pruneExpiredTasks() {
        futures.removeIf(ReceiveMessageCompletableFuture::isExpired);
    }

    private void reportBatchFinished(ReceiveSqsMessageHelper batch) {
        finishedTasks.offer(batch);
        inflightReceiveMessageBatches.decrementAndGet();
        satisfyFuturesFromBuffer();
        spawnMoreReceiveTasks();
    }
}
