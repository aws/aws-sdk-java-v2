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

package software.amazon.awssdk.services.sqs;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.core.internal.batchutilities.BatchBuffer;
import software.amazon.awssdk.services.sqs.model.BatchResultErrorEntry;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResultEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

public class BatchBufferSqsTest {

    private SqsClient client;
    private BatchBuffer<SendMessageRequest, SendMessageResponse, SendMessageBatchResponse> buffer;
    private String queueUrl;

    BiFunction<Map<String, SendMessageRequest>, String, CompletableFuture<SendMessageBatchResponse>> batchingFunction =
        (requestEntryMap, queueUrl) -> {
                List<SendMessageBatchRequestEntry> entries = new ArrayList<>(requestEntryMap.size());
                requestEntryMap.forEach((key, value) -> entries.add(createMessageBatchRequestEntry(key, value)));
                SendMessageBatchRequest batchRequest = SendMessageBatchRequest.builder()
                                                                              .queueUrl(queueUrl)
                                                                              .entries(entries)
                                                                              .build();
                return CompletableFuture.supplyAsync(() -> client.sendMessageBatch(batchRequest));
        };

    Function<SendMessageBatchResponse, Map<String, SendMessageResponse>> unpackResponseFunction =
        sendMessageBatchResponse -> {
            Map<String, SendMessageResponse> mappedResponses = new HashMap<>();
            sendMessageBatchResponse.successful()
                                    .stream()
                                    .forEach(batchResponseEntry -> {
                                        String key = batchResponseEntry.id();
                                        SendMessageResponse response = createSendMessageResponse(key, batchResponseEntry);
                                        mappedResponses.put(key, response);
                                    });
            // Add failed responses once I figure out how to crate sendMessageResponse items.
            return mappedResponses;
        };

    @Before
    public void beforeEachBufferTest() {
        client = SqsClient.create();
        queueUrl = client.createQueue(CreateQueueRequest.builder().queueName("myQueue").build()).queueUrl();
        buffer = new BatchBuffer<>(10, Duration.ofMillis(200), batchingFunction, unpackResponseFunction);
    }

    @After
    public void afterEachBufferTest() {
        client.close();
    }

    @Test
    public void sendMessageTest() {
        SendMessageRequest[] requests = new SendMessageRequest[10];
        for (int i = 0; i < requests.length; i++) {
            requests[i] = SendMessageRequest.builder()
                                            .messageBody(Integer.toString(i))
                                            .queueUrl(queueUrl)
                                            .build();
        }
        List<CompletableFuture<SendMessageResponse>> responses = new ArrayList<>();
        for (SendMessageRequest request : requests) {
            responses.add(buffer.sendRequest(request, queueUrl));
        }
        CompletableFuture.allOf(responses.toArray(new CompletableFuture[0])).join();
        for (CompletableFuture<SendMessageResponse> response: responses) {
            System.out.println("message ID:" + response.join().messageId());
        }
    }


    private SendMessageBatchRequestEntry createMessageBatchRequestEntry(String id, SendMessageRequest request) {
        return SendMessageBatchRequestEntry.builder()
                                           .id(id)
                                           .messageBody(request.messageBody())
                                           .delaySeconds(request.delaySeconds())
                                           .messageAttributes(request.messageAttributes())
                                           .messageDeduplicationId(request.messageDeduplicationId())
                                           .messageGroupId(request.messageGroupId())
                                           .messageSystemAttributes(request.messageSystemAttributes())
                                           .build();
    }

    private SendMessageResponse createSendMessageResponse(String id, SendMessageBatchResultEntry successfulEntry) {
        return SendMessageResponse.builder()
                                  .md5OfMessageAttributes(successfulEntry.md5OfMessageAttributes())
                                  .md5OfMessageBody(successfulEntry.md5OfMessageBody())
                                  .md5OfMessageSystemAttributes(successfulEntry.md5OfMessageSystemAttributes())
                                  .messageId(successfulEntry.messageId())
                                  .sequenceNumber(successfulEntry.sequenceNumber())
                                  .build();
    }

    private SendMessageResponse createSendMessageResponse(String id, BatchResultErrorEntry failedEntry) {
        return SendMessageResponse.builder()

                                  .build();
    }
}
