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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
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
import software.amazon.awssdk.core.internal.batchmanager.BatchAndSend;
import software.amazon.awssdk.core.internal.batchmanager.BatchKeyMapper;
import software.amazon.awssdk.core.internal.batchmanager.BatchManager;
import software.amazon.awssdk.core.BatchOverrideConfiguration;
import software.amazon.awssdk.core.internal.batchmanager.BatchResponseMapper;
import software.amazon.awssdk.core.internal.batchmanager.IdentifiableMessage;
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

public class BatchManagerSqsIntegrationTest extends IntegrationTestBase{

    private static final int DEFAULT_MAX_BATCH_OPEN = 200;
    private static final Logger log = Logger.loggerFor(BatchManagerSqsIntegrationTest.class);
    private SqsClient client;
    private BatchManager<SendMessageRequest, SendMessageResponse, SendMessageBatchResponse> batchManager;
    private ScheduledExecutorService scheduledExecutor;
    private String defaultQueueUrl;

    @Before
    public void setUp() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().threadNamePrefix("batch-buffer").build();
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);

        client = createSqsSyncClient();
        defaultQueueUrl = client.createQueue(CreateQueueRequest.builder().queueName("myQueue0").build()).queueUrl();
        BatchOverrideConfiguration overrideConfiguration = BatchOverrideConfiguration.builder()
                                                                                     .maxBatchItems(10)
                                                                                     .maxBatchOpenInMs(Duration.ofMillis(DEFAULT_MAX_BATCH_OPEN))
                                                                                     .scheduledExecutor(scheduledExecutor)
                                                                                     .build();
        batchManager = BatchManager.<SendMessageRequest, SendMessageResponse, SendMessageBatchResponse> builder()
                                   .overrideConfiguration(overrideConfiguration)
                                   .batchingFunction(batchingFunction)
                                   .mapResponsesFunction(mapResponsesFunction)
                                   .batchKeyMapperFunction(getBatchGroupIdFunction)
                                   .build();
    }

    @After
    public void tearDown() {
        batchManager.close();
        scheduledExecutor.shutdownNow();
        client.close();
    }

    @Test
    public void sendTenMessage() {
        Map<String, SendMessageRequest> requests = createRequestsOfSize(10);
        Map<String, CompletableFuture<SendMessageResponse>> responses = createAndSendResponses(requests);
        checkAllResponses(requests, responses);
    }

    @Test
    public void sendTwentyMessage() {
        Map<String, SendMessageRequest> requests = createRequestsOfSize(20);
        Map<String, CompletableFuture<SendMessageResponse>> responses = createAndSendResponses(requests);
        checkAllResponses(requests, responses);
    }

    @Test
    public void scheduleSendFiveRequests() {
        Map<String, SendMessageRequest> requests = createRequestsOfSize(5);

        long startTime = System.nanoTime();
        Map<String, CompletableFuture<SendMessageResponse>> responses = createAndSendResponses(requests);
        CompletableFuture.allOf(responses.values().toArray(new CompletableFuture[0])).join();
        long endTime = System.nanoTime();

        Assert.assertTrue(Duration.ofNanos(endTime - startTime).toMillis() > DEFAULT_MAX_BATCH_OPEN);
        checkAllResponses(requests, responses);
    }

    @Test
    public void scheduleTwoBatches() {
        Map<String, SendMessageRequest> requests = createRequestsOfSize(10);

        long startTime = System.nanoTime();
        Map<String, CompletableFuture<SendMessageResponse>> responses = createAndSendResponses(0, 5, requests);
        waitForTime(DEFAULT_MAX_BATCH_OPEN + 10);
        responses.putAll(createAndSendResponses(5, 5, requests));
        CompletableFuture.allOf(responses.values().toArray(new CompletableFuture[0])).join();
        long endTime = System.nanoTime();

        Assert.assertEquals(10, responses.size());
        Assert.assertTrue(Duration.ofNanos(endTime - startTime).toMillis() > DEFAULT_MAX_BATCH_OPEN + 100);
        checkAllResponses(requests, responses);
    }

    @Test
    public void sendTenMessagesWithEachThread() {
        int numThreads = 10;
        int numMessages = 10;
        Map<String, SendMessageRequest> requests = createRequestsOfSize(numThreads*numMessages);
        ConcurrentHashMap<String, CompletableFuture<SendMessageResponse>> responses = new ConcurrentHashMap<>();
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

        List<CompletableFuture<Map<String, CompletableFuture<SendMessageResponse>>>> sendRequestFutures =
            createThreadsAndSendMessages(numThreads, numMessages, requests, responses, executorService);
        checkThreadedResponses(requests, responses, sendRequestFutures);
    }

    @Test
    public void scheduleFiveMessagesWithEachThreadToDifferentLocations() {
        int numThreads = 10;
        int numMessages = 5;
        Map<String, SendMessageRequest> requests = createRequestsOfSizeToDiffDestinations(numThreads, numMessages);
        ConcurrentHashMap<String, CompletableFuture<SendMessageResponse>> responses = new ConcurrentHashMap<>();
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

        List<CompletableFuture<Map<String, CompletableFuture<SendMessageResponse>>>> sendRequestFutures =
            createThreadsAndSendMessages(numThreads, numMessages, requests, responses, executorService);
        checkThreadedResponses(requests, responses, sendRequestFutures);
    }

    // Sometimes it passes a null identifiedRequests;
    BatchAndSend<SendMessageRequest, SendMessageBatchResponse> batchingFunction =
        (identifiedRequests, destination) -> {
            List<SendMessageBatchRequestEntry> entries = new ArrayList<>(identifiedRequests.size());
            identifiedRequests.forEach(identifiedRequest -> {
                String id = identifiedRequest.id();
                SendMessageRequest request = identifiedRequest.message();
                entries.add(createMessageBatchRequestEntry(id, request));
            });
            SendMessageBatchRequest batchRequest = SendMessageBatchRequest.builder()
                                                                          .queueUrl(destination)
                                                                          .entries(entries)
                                                                          .build();
            return CompletableFuture.supplyAsync(() -> client.sendMessageBatch(batchRequest));
        };

    BatchResponseMapper<SendMessageBatchResponse, SendMessageResponse> mapResponsesFunction =
        sendMessageBatchResponse -> {
            List<IdentifiableMessage<SendMessageResponse>> mappedResponses = new ArrayList<>();
            sendMessageBatchResponse.successful()
                                    .forEach(batchResponseEntry -> {
                                        String key = batchResponseEntry.id();
                                        SendMessageResponse response = createSendMessageResponse(batchResponseEntry);
                                        mappedResponses.add(new IdentifiableMessage<>(key, response));
                                    });
            // Add failed responses once I figure out how to create sendMessageResponse items.
            return mappedResponses;
        };

    private static final BatchKeyMapper<SendMessageRequest> getBatchGroupIdFunction =
        request -> {
            if (request.overrideConfiguration().isPresent()) {
                return request.queueUrl() + request.overrideConfiguration().get();
            } else {
                return request.queueUrl();
            }
        };

    private List<CompletableFuture<Map<String, CompletableFuture<SendMessageResponse>>>> createThreadsAndSendMessages(
        int numThreads, int numMessages, Map<String, SendMessageRequest> requests,
        ConcurrentHashMap<String, CompletableFuture<SendMessageResponse>> responses, ExecutorService executorService) {
        List<CompletableFuture<Map<String, CompletableFuture<SendMessageResponse>>>> executions = new ArrayList<>();
        for (int i = 0; i < numThreads; i++) {
            executions.add(sendRequestToDestination(i*numMessages, numMessages, requests, responses, executorService));
        }
        return executions;
    }

    private CompletableFuture<Map<String, CompletableFuture<SendMessageResponse>>> sendRequestToDestination(
        int startingId, int numMessages, Map<String, SendMessageRequest> requests,
        ConcurrentHashMap<String, CompletableFuture<SendMessageResponse>> responses, ExecutorService executorService) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, CompletableFuture<SendMessageResponse>> newResponses = createAndSendResponses(startingId, numMessages,
                                                                                                      requests);
            responses.putAll(newResponses);
            return newResponses;
        }, executorService);
    }

    private void checkThreadedResponses(Map<String, SendMessageRequest> requests,
                                        ConcurrentHashMap<String, CompletableFuture<SendMessageResponse>> responses,
                                        List<CompletableFuture<Map<String, CompletableFuture<SendMessageResponse>>>> sendRequestFutures) {
        for (CompletableFuture<Map<String, CompletableFuture<SendMessageResponse>>> sendRequestFuture : sendRequestFutures) {
            try {
                CompletableFuture.allOf(sendRequestFuture.get(300, TimeUnit.MILLISECONDS)
                                                         .values()
                                                         .toArray(new CompletableFuture[0]))
                                 .join();
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                log.error(() -> String.valueOf(e));
            }
        }
        checkAllResponses(requests, responses);
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

    private void checkAllResponses(Map<String, SendMessageRequest> requests,
                                   Map<String, CompletableFuture<SendMessageResponse>> responses) {

        Assert.assertEquals(responses.size(), requests.size());
        byte[] expectedMd5;
        for (int i = 0; i < responses.size(); i++) {
            String key = Integer.toString(i);
            String requestBody = requests.get(key).messageBody();
            expectedMd5 = Md5Utils.computeMD5Hash(requestBody.getBytes(StandardCharsets.UTF_8));
            String expectedHash = BinaryUtils.toHex(expectedMd5);
            Assert.assertEquals(expectedHash, responses.get(key).join().md5OfMessageBody());
        }
    }

    private Map<String, SendMessageRequest> createRequestsOfSize(int size) {
        Map<String, SendMessageRequest> requests = new HashMap<>();
        for (int i = 0; i < size; i++) {
            requests.put(Integer.toString(i), SendMessageRequest.builder()
                                                                .messageBody(Integer.toString(i))
                                                                .queueUrl(defaultQueueUrl)
                                                                .build());
        }
        return requests;
    }

    private Map<String, SendMessageRequest> createRequestsOfSizeToDiffDestinations(int numDestinations, int destinationSize) {
        String myQueueUrl;
        Map<String, SendMessageRequest> requests = new HashMap<>();
        for (int i = 0; i < numDestinations; i++) {
            for (int j = 0; j < destinationSize; j++) {
                String key = Integer.toString(i*destinationSize + j);
                myQueueUrl = client.createQueue(CreateQueueRequest.builder().queueName("myQueue" + i).build()).queueUrl();
                requests.put(key, SendMessageRequest.builder()
                                                    .messageBody(Integer.toString(i))
                                                    .queueUrl(myQueueUrl)
                                                    .build());
            }
        }
        return requests;
    }

    private Map<String, CompletableFuture<SendMessageResponse>> createAndSendResponses(Map<String, SendMessageRequest> requests) {
        return createAndSendResponses(0, requests.size(), requests);
    }

    private Map<String, CompletableFuture<SendMessageResponse>> createAndSendResponses(int startingId, int size,
                                                                                Map<String, SendMessageRequest>  requests) {
        Map<String, CompletableFuture<SendMessageResponse>> responses = new HashMap<>();
        for (int i = startingId; i < startingId + size; i++) {
            String key = Integer.toString(i);
            SendMessageRequest request = requests.get(key);
            responses.put(key, batchManager.sendRequest(request));
        }
        return responses;
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
