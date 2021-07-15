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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.xml.bind.DatatypeConverter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.core.internal.batchutilities.BatchAndSendFunction;
import software.amazon.awssdk.core.internal.batchutilities.BatchBuffer;
import software.amazon.awssdk.core.internal.batchutilities.IdentifiedResponse;
import software.amazon.awssdk.core.internal.batchutilities.UnpackBatchResponseFunction;
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

    BatchAndSendFunction<SendMessageRequest, SendMessageBatchResponse> batchingFunction =
        (identifiedRequests, queueUrl) -> {
            List<SendMessageBatchRequestEntry> entries = new ArrayList<>(identifiedRequests.size());
            identifiedRequests.forEach(identifiedRequest -> {
                String id = identifiedRequest.getId();
                SendMessageRequest request = identifiedRequest.getRequest();
                entries.add(createMessageBatchRequestEntry(id, request));
            });
            SendMessageBatchRequest batchRequest = SendMessageBatchRequest.builder()
                                                                          .queueUrl(queueUrl)
                                                                          .entries(entries)
                                                                          .build();
            return CompletableFuture.supplyAsync(() -> client.sendMessageBatch(batchRequest));
        };

    UnpackBatchResponseFunction<SendMessageBatchResponse, SendMessageResponse> unpackResponseFunction =
        sendMessageBatchResponse -> {
            List<IdentifiedResponse<SendMessageResponse>> mappedResponses = new ArrayList<>();
            sendMessageBatchResponse.successful()
                                    .forEach(batchResponseEntry -> {
                                        String key = batchResponseEntry.id();
                                        SendMessageResponse response = createSendMessageResponse(batchResponseEntry);
                                        mappedResponses.add(new IdentifiedResponse<>(key, response));
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
    public void sendTenMessageTest() {
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
        for (int i = 0; i < requests.length; i++) {
            String requestBody = requests[i].messageBody();
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                md.update(requestBody.getBytes());
                byte[] digest = md.digest();
                String myHash = DatatypeConverter.printHexBinary(digest).toLowerCase();
                Assert.assertEquals(myHash, responses.get(i).join().md5OfMessageBody());
            } catch (NoSuchAlgorithmException e) {
                System.out.println("No MD5 algorithm.");
            }
        }
    }

    @Test
    public void sendTwentyMessageTest() {
        SendMessageRequest[] requests = new SendMessageRequest[20];
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
        for (int i = 0; i < requests.length; i++) {
            String requestBody = requests[i].messageBody();
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                md.update(requestBody.getBytes());
                byte[] digest = md.digest();
                String myHash = DatatypeConverter.printHexBinary(digest).toLowerCase();
                Assert.assertEquals(myHash, responses.get(i).join().md5OfMessageBody());
            } catch (NoSuchAlgorithmException e) {
                System.out.println("No MD5 algorithm.");
            }
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

    private SendMessageResponse createSendMessageResponse(SendMessageBatchResultEntry successfulEntry) {
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
