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


import static software.amazon.awssdk.services.sqs.internal.batchmanager.RequestBatchManager.USER_AGENT_APPLIER;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.NumericUtils;

/**
 * The {@code ReceiveSqsMessageHelper} class forms a {@link  ReceiveMessageRequest} request based on configuration settings,
 * collects messages from an AWS SQS queue, and handles exceptions during the process.
 * <p>
 * It manages message visibility timeout by tracking the visibility deadline and expiring messages if not processed in time,
 * ensuring unhandled messages return to the queue for reprocessing.
 * <p>
 * Additionally, the class supports clearing messages in the batch and changing their visibility as needed.
 */

@SdkInternalApi
public class ReceiveSqsMessageHelper {

    private static final Logger log = Logger.loggerFor(ReceiveSqsMessageHelper.class);
    private final String queueUrl;
    private final SqsAsyncClient asyncClient;
    private final Duration visibilityTimeout;
    private final ResponseBatchConfiguration config;
    private volatile Throwable exception;
    private Queue<Message> messages = new ConcurrentLinkedQueue<>();
    private volatile long visibilityDeadlineNano;

    public ReceiveSqsMessageHelper(String queueUrl,
                                   SqsAsyncClient asyncClient,
                                   Duration visibilityTimeout,
                                   ResponseBatchConfiguration config) {
        this.queueUrl = queueUrl;
        this.asyncClient = asyncClient;
        this.visibilityTimeout = visibilityTimeout;
        this.config = config;
    }

    public CompletableFuture<ReceiveSqsMessageHelper> asyncReceiveMessage() {
        ReceiveMessageRequest.Builder request =
            ReceiveMessageRequest.builder()
                                 .queueUrl(queueUrl)
                                 .maxNumberOfMessages(config.maxBatchItems())
                                 .overrideConfiguration(o -> o.applyMutation(USER_AGENT_APPLIER));

        if (!CollectionUtils.isNullOrEmpty(config.messageSystemAttributeNames())) {
            request.messageSystemAttributeNames(config.messageSystemAttributeNames());
        }

        if (!CollectionUtils.isNullOrEmpty(config.receiveMessageAttributeNames())) {
            request.messageAttributeNames(config.receiveMessageAttributeNames());
        }

        request.visibilityTimeout(NumericUtils.saturatedCast(this.visibilityTimeout.getSeconds()));

        try {
            return asyncClient.receiveMessage(request.build())
                              .handle((response, throwable) -> {
                                  if (throwable != null) {
                                      this.exception = throwable;
                                  } else {
                                      messages.addAll(response.messages());
                                  }
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
        return exception;
    }

    public Message removeMessage() {
        if (isExpired()) {
            clear();
            return null;
        }
        return messages.poll();
    }

    private boolean isExpired() {
        return System.nanoTime() > visibilityDeadlineNano;
    }


    public void clear() {
        if (!isEmpty()) {
            CompletableFuture<ChangeMessageVisibilityBatchResponse> nackedMessages = nackMessages();
            if (nackedMessages != null) {
                nackedMessages.exceptionally(throwable -> {
                    log.warn(() -> String.format(
                        "Failed to reset the visibility timeout of unprocessed messages for queueUrl: %s. "
                        + "As a result, these unprocessed messages will remain invisible in the queue for the "
                        + "duration of the visibility timeout (%s).",
                        queueUrl, visibilityTimeout
                    ), throwable);

                    return null;
                });
            }
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
                                                                            .receiptHandle(Objects.requireNonNull(messages.poll())
                                                                                                  .receiptHandle())
                                                                            .visibilityTimeout(0)
                                                                            .build())
                     .collect(Collectors.toList());

        ChangeMessageVisibilityBatchRequest batchRequest =
            ChangeMessageVisibilityBatchRequest.builder()
                                               .queueUrl(queueUrl)
                                               .entries(entries)
                                               .overrideConfiguration(o -> o.applyMutation(USER_AGENT_APPLIER))
                                               .build();

        return asyncClient.changeMessageVisibilityBatch(batchRequest);
    }

    /**
     * messages.size() is expensive since it is ConcurrentLinkedQueue.
     * Thus, its used only for testing the results and not used in any internal classes.
     */
    @SdkTestInternalApi
    public Integer messagesSize() {
        return messages != null ? messages.size() : 0;
    }
}