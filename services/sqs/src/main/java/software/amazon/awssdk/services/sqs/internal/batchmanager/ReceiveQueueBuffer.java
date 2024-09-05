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

import static software.amazon.awssdk.services.sqs.internal.batchmanager.ResponseBatchConfiguration.MAX_SUPPORTED_SQS_RECEIVE_MSG;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.utils.SdkAutoCloseable;

@SdkInternalApi
public class ReceiveQueueBuffer implements SdkAutoCloseable {

    private final ScheduledExecutorService executor;
    private final SqsAsyncClient sqsClient;
    private final ResponseBatchConfiguration config;
    private final String queueUrl;
    private final  QueueAttributesManager queueAttributesManager;

    private final Queue<ReceiveSqsMessageHelper> finishedTasks = new ConcurrentLinkedQueue<>();
    private final Queue<FutureRequestWrapper> futures = new ConcurrentLinkedQueue<>();

    private final AtomicInteger inflightReceiveMessageBatches = new AtomicInteger(0);
    private final AtomicBoolean shutDown = new AtomicBoolean(false);

    private final AtomicBoolean processingFutures = new AtomicBoolean(false);

    private ReceiveQueueBuffer(Builder builder) {
        this.executor = builder.executor;
        this.sqsClient = builder.sqsClient;
        this.config = builder.config;
        this.queueUrl = builder.queueUrl;
        this.queueAttributesManager = builder.queueAttributesManager;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void receiveMessage(CompletableFuture<ReceiveMessageResponse> receiveMessageFuture, int numMessages) {
        futures.add(new FutureRequestWrapper(receiveMessageFuture, numMessages));
        satisfyFuturesFromBuffer();
        spawnMoreReceiveTasks();
    }

    public boolean isShutDown() {
        return shutDown.get();
    }

    @Override
    public void close() {
        if (this.shutDown.compareAndSet(false, true)) {
            while (!finishedTasks.isEmpty()) {
                ReceiveSqsMessageHelper batch = finishedTasks.poll();
                if (inflightReceiveMessageBatches.get() > 0) {
                    inflightReceiveMessageBatches.decrementAndGet();
                }
                if (batch != null) {
                    batch.clear();
                }
            }
            futures.forEach(futureWrapper -> {
                if (!futureWrapper.getFuture().isDone()) {
                    futureWrapper.getFuture().completeExceptionally(new CancellationException("Shutdown in progress"));
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

        queueAttributesManager.getVisibilityTimeout().thenAccept(visibilityTimeout -> {
            int max = Math.max(config.maxInflightReceiveBatches(), 1);
            int toSpawn = max - inflightReceiveMessageBatches.get();
            if (toSpawn > 0) {
                ReceiveSqsMessageHelper receiveSqsMessageHelper = new ReceiveSqsMessageHelper(
                    queueUrl, sqsClient, visibilityTimeout, config);
                inflightReceiveMessageBatches.incrementAndGet();
                receiveSqsMessageHelper.asyncReceiveMessage()
                                       .whenComplete((response, exception) -> reportBatchFinished(response));
            }
        });
    }

    private int determineDesiredBatches() {
        int desiredBatches = Math.max(config.maxDoneReceiveBatches(), 1);
        int totalRequested = futures.stream()
                                    .mapToInt(FutureRequestWrapper::getRequestedSize)
                                    .sum();
        int batchesNeededToFulfillFutures = (int) Math.ceil((float) totalRequested / MAX_SUPPORTED_SQS_RECEIVE_MSG);
        desiredBatches = Math.min(batchesNeededToFulfillFutures, desiredBatches);

        return desiredBatches;
    }

    private void fulfillFuture(FutureRequestWrapper futureWrapper) {
        ReceiveSqsMessageHelper peekedMessage = finishedTasks.peek();
        List<Message> messages = new LinkedList<>();
        Throwable exception = peekedMessage.getException();
        int numRetrieved = 0;
        boolean batchDone = false;

        if (exception != null) {
            futureWrapper.getFuture().completeExceptionally(exception);
            finishedTasks.poll();
            return;
        }

        while (numRetrieved < futureWrapper.getRequestedSize()) {
            Message msg = peekedMessage.removeMessage();
            if (msg != null) {
                messages.add(msg);
                ++numRetrieved;
            } else {
                batchDone = true;
                break;
            }
        }
        batchDone = batchDone || peekedMessage.isEmpty();
        if (batchDone) {
            finishedTasks.poll();
        }
        futureWrapper.getFuture().complete(ReceiveMessageResponse.builder().messages(messages).build());
    }

    private void satisfyFuturesFromBuffer() {
        if (!processingFutures.compareAndSet(false, true)) {
            return;
        }
        try {
            do {
                futures.removeIf(future -> {
                    if (future.getFuture().isDone()) {
                        return true;
                    }
                    if (!finishedTasks.isEmpty()) {
                        fulfillFuture(future);
                        return true;
                    }
                    return false;
                });
            } while (!futures.isEmpty() && !finishedTasks.isEmpty());
        } finally {
            processingFutures.set(false);
        }
    }

    private void reportBatchFinished(ReceiveSqsMessageHelper batch) {
        finishedTasks.offer(batch);
        inflightReceiveMessageBatches.decrementAndGet();
        satisfyFuturesFromBuffer();
        spawnMoreReceiveTasks();
    }

    private static class FutureRequestWrapper {
        private final CompletableFuture<ReceiveMessageResponse> future;
        private final int requestedSize;

        FutureRequestWrapper(CompletableFuture<ReceiveMessageResponse> future, int requestedSize) {
            this.future = future;
            this.requestedSize = requestedSize;
        }

        public CompletableFuture<ReceiveMessageResponse> getFuture() {
            return future;
        }

        public int getRequestedSize() {
            return requestedSize;
        }
    }

    public static class Builder {
        private ScheduledExecutorService executor;
        private SqsAsyncClient sqsClient;
        private ResponseBatchConfiguration config;
        private String queueUrl;
        private QueueAttributesManager queueAttributesManager;

        public Builder executor(ScheduledExecutorService executor) {
            this.executor = executor;
            return this;
        }

        public Builder sqsClient(SqsAsyncClient sqsClient) {
            this.sqsClient = sqsClient;
            return this;
        }

        public Builder config(ResponseBatchConfiguration config) {
            this.config = config;
            return this;
        }

        public Builder queueUrl(String queueUrl) {
            this.queueUrl = queueUrl;
            return this;
        }

        public Builder queueAttributesManager(QueueAttributesManager queueAttributesManager) {
            this.queueAttributesManager = queueAttributesManager;
            return this;
        }

        public ReceiveQueueBuffer build() {
            return new ReceiveQueueBuffer(this);
        }
    }
}
