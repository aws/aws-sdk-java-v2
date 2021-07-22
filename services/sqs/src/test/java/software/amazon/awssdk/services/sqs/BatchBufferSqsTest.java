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

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.core.internal.batchutilities.BatchAndSendFunction;
import software.amazon.awssdk.core.internal.batchutilities.BatchManager;
import software.amazon.awssdk.core.internal.batchutilities.IdentifiedResponse;
import software.amazon.awssdk.core.internal.batchutilities.UnpackBatchResponseFunction;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResultEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Md5Utils;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;

public class BatchBufferSqsTest {

    private static final Logger log = Logger.loggerFor(BatchBufferSqsTest.class);
    private SqsClient client;
    private BatchManager<SendMessageRequest, SendMessageResponse, SendMessageBatchResponse> buffer;
    private ScheduledExecutorService scheduledExecutor;
    private String queueUrl;

    BatchAndSendFunction<SendMessageRequest, SendMessageBatchResponse> batchingFunction =
        (identifiedRequests, destination) -> {
            List<SendMessageBatchRequestEntry> entries = new ArrayList<>(identifiedRequests.size());
            identifiedRequests.forEach(identifiedRequest -> {
                String id = identifiedRequest.getId();
                SendMessageRequest request = identifiedRequest.getRequest();
                entries.add(createMessageBatchRequestEntry(id, request));
            });
            SendMessageBatchRequest batchRequest = SendMessageBatchRequest.builder()
                                                                          .queueUrl(destination)
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
    public void setUp() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().threadNamePrefix("batch-buffer").build();
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);
        client = SqsClient.create();
        queueUrl = client.createQueue(CreateQueueRequest.builder().queueName("myQueue").build()).queueUrl();
        buffer = new BatchManager<>(10, Duration.ofMillis(200), scheduledExecutor,
                                    batchingFunction, unpackResponseFunction);
    }

    @After
    public void tearDown() {
        buffer.close();
        client.close();
    }

    @Test
    public void sendTenMessage() {
        SendMessageRequest[] requests = createRequestsOfSize(10);
        List<CompletableFuture<SendMessageResponse>> responses = createAndSendResponses(requests);
        checkAllResponses(requests, responses);
    }

    @Test
    public void sendTwentyMessage() {
        SendMessageRequest[] requests = createRequestsOfSize(20);
        List<CompletableFuture<SendMessageResponse>> responses = createAndSendResponses(requests);
        checkAllResponses(requests, responses);
    }

    @Test
    public void scheduleSendFiveRequests() {
        SendMessageRequest[] requests = createRequestsOfSize(5);

        long startTime = System.nanoTime();
        List<CompletableFuture<SendMessageResponse>> responses = createAndSendResponses(requests);
        CompletableFuture.allOf(responses.toArray(new CompletableFuture[0])).join();
        long endTime = System.nanoTime();

        Assert.assertTrue(Duration.ofNanos(endTime - startTime).toMillis() > 200);
        checkAllResponses(requests, responses);
    }

    @Test
    public void cancelScheduledBatch() {
        SendMessageRequest[] requests = createRequestsOfSize(10);
        SendMessageRequest[] requestsBatch1 = Arrays.copyOfRange(requests, 0, 5);
        SendMessageRequest[] requestsBatch2 = Arrays.copyOfRange(requests, 5, 10);

        long startTime = System.nanoTime();
        List<CompletableFuture<SendMessageResponse>> responses = createAndSendResponses(requestsBatch1);
        waitForTime(195);
        responses.addAll(createAndSendResponses(requestsBatch2));
        CompletableFuture.allOf(responses.toArray(new CompletableFuture[0])).join();
        long endTime = System.nanoTime();

        Assert.assertTrue(Duration.ofNanos(endTime - startTime).toMillis() < 400);
        checkAllResponses(requests, responses);
    }

    @Test
    public void scheduleTwoBatches() {
        SendMessageRequest[] requests = createRequestsOfSize(10);
        SendMessageRequest[] requestsBatch1 = Arrays.copyOfRange(requests, 0, 5);
        SendMessageRequest[] requestsBatch2 = Arrays.copyOfRange(requests, 5, 10);

        long startTime = System.nanoTime();
        List<CompletableFuture<SendMessageResponse>>  responses = createAndSendResponses(requestsBatch1);
        waitForTime(250);
        responses.addAll(createAndSendResponses(requestsBatch2));
        CompletableFuture.allOf(responses.toArray(new CompletableFuture[0])).join();
        long endTime = System.nanoTime();

        Assert.assertEquals(10, responses.size());
        Assert.assertTrue(Duration.ofNanos(endTime - startTime).toMillis() > 400);
        checkAllResponses(requests, responses);
    }

    @Test
    public void sendTenMessagesWithEachThread() {
        int numThreads = 10;
        SendMessageRequest[] requests = createRequestsOfSize(numThreads*10);
        ConcurrentLinkedQueue<CompletableFuture<SendMessageResponse>> responses = new ConcurrentLinkedQueue<>();
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        CompletionService<List<CompletableFuture<SendMessageResponse>>> completionService =
            new ExecutorCompletionService<>(executorService);

        createThreadsAndSendMessages(numThreads, 10, queueUrl, requests, responses, completionService);
        checkThreadedResponses(numThreads, 10, responses, completionService);
    }

    @Test
    public void scheduleFiveMessagesWithEachThreadToDifferentLocations() {
        int numThreads = 10;
        SendMessageRequest[] requests = createRequestsOfSize(numThreads*5);
        ConcurrentLinkedQueue<CompletableFuture<SendMessageResponse>> responses = new ConcurrentLinkedQueue<>();
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        CompletionService<List<CompletableFuture<SendMessageResponse>>> completionService =
            new ExecutorCompletionService<>(executorService);

        createThreadsAndSendMessages(numThreads, 5, null, requests, responses, completionService);
        checkThreadedResponses(numThreads, 5, responses, completionService);
    }

    private void createThreadsAndSendMessages(int numThreads, int numMessages, String destination,
                                              SendMessageRequest[] requests,
                                              ConcurrentLinkedQueue<CompletableFuture<SendMessageResponse>> responses,
                                              CompletionService<List<CompletableFuture<SendMessageResponse>>> completionService) {
        String newDestination;
        for (int i = 0; i < numThreads; i++) {
            SendMessageRequest[] requestsCopy = Arrays.copyOfRange(requests, i*numMessages, i*numMessages + numMessages);
            if (destination == null) {
                newDestination = client.createQueue(CreateQueueRequest.builder().queueName(Integer.toString(i)).build()).queueUrl();
            } else {
                newDestination = destination;
            }
            sendRequestToDestination(newDestination, requestsCopy, responses, completionService);
        }
    }

    private void sendRequestToDestination(String destination, SendMessageRequest[] requests,
                                          ConcurrentLinkedQueue<CompletableFuture<SendMessageResponse>> responses,
                                          CompletionService<List<CompletableFuture<SendMessageResponse>>> completionService) {
        completionService.submit(() -> {
            List<CompletableFuture<SendMessageResponse>> newResponses = createAndSendResponses(requests, destination);
            responses.addAll(newResponses);
            return newResponses;
        });
    }

    private void checkThreadedResponses(int numThreads, int numMessages,
                                        ConcurrentLinkedQueue<CompletableFuture<SendMessageResponse>> responses,
                                        CompletionService<List<CompletableFuture<SendMessageResponse>>> completionService) {
        for (int i = 0; i < numThreads; i++) {
            try {
                CompletableFuture.allOf(completionService.take()
                                                         .get(300, TimeUnit.MILLISECONDS)
                                                         .toArray(new CompletableFuture[0]))
                                 .join();
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                log.error(() -> String.valueOf(e));
            }
        }

        Iterator<CompletableFuture<SendMessageResponse>> responsesIterator = responses.iterator();
        for (int i = 0; responsesIterator.hasNext(); i++) {
            log.debug(() -> String.valueOf(responsesIterator.next().join()));
        }
        Assert.assertEquals(responses.size(), numThreads*numMessages);
    }

    private boolean waitForTime(int msToWait) {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            return countDownLatch.await(msToWait, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    private SendMessageRequest[] createRequestsOfSize(int size) {
        SendMessageRequest[] requests = new SendMessageRequest[size];
        for (int i = 0; i < size; i++) {
            requests[i] = SendMessageRequest.builder()
                                            .messageBody(Integer.toString(i))
                                            .build();
        }
        return requests;
    }

    private List<CompletableFuture<SendMessageResponse>> createAndSendResponses(SendMessageRequest[] requests) {
        return createAndSendResponses(requests, queueUrl);
    }

    private List<CompletableFuture<SendMessageResponse>> createAndSendResponses(SendMessageRequest[] requests,
                                                                                String destination) {
        List<CompletableFuture<SendMessageResponse>> responses = new ArrayList<>();
        for (SendMessageRequest request : requests) {
            responses.add(buffer.sendRequest(request, destination));
        }
        return responses;
    }

    private void checkAllResponses(SendMessageRequest[] requests, Collection<CompletableFuture<SendMessageResponse>> responses) {

        Assert.assertEquals(responses.size(), requests.length);
        byte[] expectedMd5;
        Iterator<CompletableFuture<SendMessageResponse>> responsesIterator = responses.iterator();
        for (int i = 0; responsesIterator.hasNext(); i++) {
            String requestBody = requests[i].messageBody();
            expectedMd5 = Md5Utils.computeMD5Hash(requestBody.getBytes(StandardCharsets.UTF_8));
            String expectedHash = BinaryUtils.toHex(expectedMd5);
            Assert.assertEquals(expectedHash, responsesIterator.next().join().md5OfMessageBody());
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
}
