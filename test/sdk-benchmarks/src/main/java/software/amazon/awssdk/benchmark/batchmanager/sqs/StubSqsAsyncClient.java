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

package software.amazon.awssdk.benchmark.batchmanager.sqs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

/**
 * An {@link SqsAsyncClient} that behaves like a queue with a deep backlog. Every ReceiveMessage returns the number of
 * messages requested (up to 10). Responses are the same prebuilt objects every time, so results don't depend on
 * payload generation or randomness.
 *
 * <p>Responses complete on a separate service thread pool, not on the caller's thread, the same way a real async HTTP
 * client completes them on its event loop. A latency of 0 hands off without delay; a positive latency adds a fixed
 * delay per call to model the SQS round trip.
 */
final class StubSqsAsyncClient implements SqsAsyncClient {
    private static final int MAX_MESSAGES = 10;
    private static final String VISIBILITY_TIMEOUT_SECONDS = "30";
    private static final String RECEIVE_WAIT_TIME_SECONDS = "0";

    private final long latencyMicros;
    private final ScheduledExecutorService serviceThreads;
    private final ReceiveMessageResponse[] responsesBySize;
    private final GetQueueAttributesResponse queueAttributes;

    StubSqsAsyncClient(long latencyMicros, int serviceThreadCount) {
        this.latencyMicros = latencyMicros;
        this.serviceThreads = Executors.newScheduledThreadPool(serviceThreadCount, r -> {
            Thread t = new Thread(r, "stub-sqs-service");
            t.setDaemon(true);
            return t;
        });
        this.responsesBySize = buildResponses();
        this.queueAttributes = buildQueueAttributes();
    }

    @Override
    public CompletableFuture<GetQueueAttributesResponse> getQueueAttributes(GetQueueAttributesRequest request) {
        return respond(queueAttributes);
    }

    @Override
    public CompletableFuture<ReceiveMessageResponse> receiveMessage(ReceiveMessageRequest request) {
        Integer requested = request.maxNumberOfMessages();
        int size = requested == null ? 1 : Math.min(requested, MAX_MESSAGES);
        return respond(responsesBySize[size]);
    }

    @Override
    public CompletableFuture<ChangeMessageVisibilityBatchResponse> changeMessageVisibilityBatch(
        ChangeMessageVisibilityBatchRequest request) {
        return respond(ChangeMessageVisibilityBatchResponse.builder().build());
    }

    @Override
    public String serviceName() {
        return SERVICE_NAME;
    }

    @Override
    public void close() {
        serviceThreads.shutdownNow();
    }

    private <T> CompletableFuture<T> respond(T response) {
        CompletableFuture<T> future = new CompletableFuture<>();
        if (latencyMicros <= 0) {
            serviceThreads.execute(() -> future.complete(response));
        } else {
            serviceThreads.schedule(() -> future.complete(response), latencyMicros, TimeUnit.MICROSECONDS);
        }
        return future;
    }

    private static ReceiveMessageResponse[] buildResponses() {
        ReceiveMessageResponse[] responses = new ReceiveMessageResponse[MAX_MESSAGES + 1];
        for (int size = 0; size <= MAX_MESSAGES; size++) {
            List<Message> messages = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                messages.add(Message.builder()
                                    .messageId("message-" + i)
                                    .receiptHandle("receipt-handle-" + i)
                                    .body("message-body-" + i)
                                    .build());
            }
            responses[size] = ReceiveMessageResponse.builder().messages(messages).build();
        }
        return responses;
    }

    private static GetQueueAttributesResponse buildQueueAttributes() {
        Map<QueueAttributeName, String> attributes = new HashMap<>();
        attributes.put(QueueAttributeName.VISIBILITY_TIMEOUT, VISIBILITY_TIMEOUT_SECONDS);
        attributes.put(QueueAttributeName.RECEIVE_MESSAGE_WAIT_TIME_SECONDS, RECEIVE_WAIT_TIME_SECONDS);
        return GetQueueAttributesResponse.builder().attributes(attributes).build();
    }
}
