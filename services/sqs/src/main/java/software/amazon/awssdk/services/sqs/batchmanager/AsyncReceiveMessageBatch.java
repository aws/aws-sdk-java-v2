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

package software.amazon.awssdk.services.sqs.batchmanager;


import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.internal.batchmanager.ResponseBatchConfiguration;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.utils.Logger;

/**
 * The {@code AsyncReceiveMessageBatch} class forms a {@link  ReceiveMessageRequest} request based on configuration settings,
 * collects messages from an AWS SQS queue, and handles exceptions during the process.
 * <p>
 * It manages message visibility timeout by tracking the visibility deadline and expiring messages if not processed in time,
 * ensuring unhandled messages return to the queue for reprocessing.
 * <p>
 * Additionally, the class supports clearing messages in the batch and changing their visibility as needed.
 */

@SdkInternalApi
public class AsyncReceiveMessageBatch {

    private static final Logger log = Logger.loggerFor(AsyncReceiveMessageBatch.class);

    private final ScheduledExecutorService scheduledExecutorService;
    private final String queueUrl;
    private final SqsAsyncClient asyncClient;
    private final Duration visibilityTimeout;
    private final ResponseBatchConfiguration config;
    private final AtomicBoolean open = new AtomicBoolean(false);
    private volatile Throwable exception;
    private List<Message> messages;
    private long visibilityDeadlineNano;

    public AsyncReceiveMessageBatch(ScheduledExecutorService scheduledExecutorService, String queueUrl,
                                    SqsAsyncClient asyncClient, Duration visibilityTimeout, ResponseBatchConfiguration config) {
        this.scheduledExecutorService = scheduledExecutorService;
        this.queueUrl = queueUrl;
        this.asyncClient = asyncClient;
        this.visibilityTimeout = visibilityTimeout;
        this.config = config;
    }

    public CompletableFuture<AsyncReceiveMessageBatch> asyncReceiveMessage() {
        ReceiveMessageRequest.Builder request =
            ReceiveMessageRequest.builder()
                                 .queueUrl(queueUrl)
                                 .maxNumberOfMessages(config.maxBatchItems())
                                 .messageAttributeNames(config.receiveMessageAttributeNames())
                                 .messageAttributeNames(config.receiveMessageAttributeNames());

        request.visibilityTimeout((int) this.visibilityTimeout.get(ChronoUnit.SECONDS));

        if (config.longPoll()) {
            request.waitTimeSeconds(config.longPollWaitTimeoutSeconds());
        }
        try {
            return asyncClient.receiveMessage(request.build())
                              .handle((response, throwable) -> {
                                  if (throwable != null) {
                                      setException(throwable);
                                  } else {
                                      messages = new ArrayList<>(response.messages());
                                  }
                                  open.set(true);
                                  return this;
                              });
        } finally {
            visibilityDeadlineNano = System.nanoTime() + visibilityTimeout.toNanos();
        }

    }

    public boolean isEmpty() {
        return messages == null || messages.isEmpty();
    }

    public Throwable getException() {
        checkIfOpen();
        return exception;
    }

    public void setException(Throwable exception) {
        this.exception = exception;
    }

    public Message removeMessage() {
        checkIfOpen();
        if (isExpired()) {
            clear();
            return null;
        }
        return messages.isEmpty() ? null : messages.remove(messages.size() - 1);
    }

    public boolean isExpired() {
        return System.nanoTime() > visibilityDeadlineNano;
    }

    public void clear() {
        if (!isEmpty()) {
            Optional.ofNullable(nackMessages())
                    .ifPresent(future -> future.whenComplete((r, t) -> {
                        // Logging an error is sufficient here as this is an asynchronous cleanup activity,
                        // and there are no dependent tasks waiting for its completion.
                        if (t != null) {
                            log.error(() -> "Could not change visibility for queue " + queueUrl, t);
                        }
                    }));
            messages.clear();
        }
    }

    private CompletableFuture<ChangeMessageVisibilityBatchResponse> nackMessages() {
        if (messages == null || messages.isEmpty()) {
            return null;
        }

        List<ChangeMessageVisibilityBatchRequestEntry> entries =
            IntStream.range(0, messages.size())
                     .mapToObj(i -> ChangeMessageVisibilityBatchRequestEntry.builder()
                                                                            .id(String.valueOf(i))
                                                                            .receiptHandle(messages.get(i).receiptHandle())
                                                                            .visibilityTimeout(0)
                                                                            .build())
                     .collect(Collectors.toList());

        ChangeMessageVisibilityBatchRequest batchRequest = ChangeMessageVisibilityBatchRequest.builder()
                                                                                              .queueUrl(queueUrl)
                                                                                              .entries(entries)
                                                                                              .build();

        return asyncClient.changeMessageVisibilityBatch(batchRequest);
    }

    private void checkIfOpen() {
        if (!open.get()) {
            throw new IllegalStateException("Batch is not open");
        }
    }

    public Integer messagesSize() {
        return messages != null ? messages.size() : 0;
    }
}