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

import static org.assertj.core.api.Assertions.assertThat;

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
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.sqs.batchmanager.SqsBatchManager;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityResponse;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Md5Utils;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;

public class BatchManagerSqsIntegrationTest extends IntegrationTestBase{

    private static final int DEFAULT_MAX_BATCH_OPEN = 200;
    private static final String TEST_QUEUE_PREFIX = "myTestQueue";
    private static final Logger log = Logger.loggerFor(BatchManagerSqsIntegrationTest.class);
    private static ScheduledExecutorService scheduledExecutor;
    private static ExecutorService executor;
    private static SqsClient client;
    private static String defaultQueueUrl;
    private SqsBatchManager batchManager;

    @BeforeClass
    public static void oneTimeSetUp() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().threadNamePrefix("SqsBatchManager").build();
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);
        executor = Executors.newSingleThreadExecutor();
        client = createSqsSyncClient();
        defaultQueueUrl = client.createQueue(CreateQueueRequest.builder().queueName(TEST_QUEUE_PREFIX + "0").build()).queueUrl();
    }

    @AfterClass
    public static void oneTimeTearDown() {
        deleteAllMessagesInQueue(defaultQueueUrl);
        client.close();
        scheduledExecutor.shutdownNow();
        executor.shutdownNow();
    }

    @Before
    public void setUp() {
        batchManager = SqsBatchManager.builder()
                                      .client(client)
                                      .scheduledExecutor(scheduledExecutor)
                                      .executor(executor)
                                      .build();
    }

    @After
    public void tearDown() {
        batchManager.close();
    }

    @Test
    public void sendTenMessages() {
        Map<String, SendMessageRequest> requests = createRequestsOfSize(10);
        Map<String, CompletableFuture<SendMessageResponse>> responses = sendSendMessageRequests(requests);
        checkAllResponses(requests, responses);
    }

    @Test
    public void sendTwentyMessages() {
        Map<String, SendMessageRequest> requests = createRequestsOfSize(20);
        Map<String, CompletableFuture<SendMessageResponse>> responses = sendSendMessageRequests(requests);
        checkAllResponses(requests, responses);
    }

    @Test
    public void scheduleSendFiveMessages() {
        Map<String, SendMessageRequest> requests = createRequestsOfSize(5);

        long startTime = System.nanoTime();
        Map<String, CompletableFuture<SendMessageResponse>> responses = sendSendMessageRequests(requests);
        CompletableFuture.allOf(responses.values().toArray(new CompletableFuture[0])).join();
        long endTime = System.nanoTime();

        assertThat(Duration.ofNanos(endTime - startTime).toMillis()).isGreaterThan(DEFAULT_MAX_BATCH_OPEN);
        checkAllResponses(requests, responses);
    }

    @Test
    public void scheduleTwoSendMessageBatches() {
        Map<String, SendMessageRequest> requests = createRequestsOfSize(10);

        long startTime = System.nanoTime();
        Map<String, CompletableFuture<SendMessageResponse>> responses = sendSendMessageRequests(0, 5, requests);
        waitForTime(DEFAULT_MAX_BATCH_OPEN + 10);
        responses.putAll(sendSendMessageRequests(5, 5, requests));
        CompletableFuture.allOf(responses.values().toArray(new CompletableFuture[0])).join();
        long endTime = System.nanoTime();

        assertThat(responses).hasSize(10);
        assertThat(Duration.ofNanos(endTime - startTime).toMillis()).isGreaterThan(DEFAULT_MAX_BATCH_OPEN + 100);
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

        for (int i = 1; i < numThreads; i++) {
            GetQueueUrlRequest queueUrlRequest = GetQueueUrlRequest.builder().queueName(TEST_QUEUE_PREFIX + i).build();
            String queueUrl = client.getQueueUrl(queueUrlRequest).queueUrl();
            deleteAllMessagesInQueue(queueUrl);
        }
    }

    @Test
    public void deleteTenMessages() {
        int numMessages = 10;
        String queueUrl = client.createQueue(CreateQueueRequest.builder().queueName(TEST_QUEUE_PREFIX + "10").build()).queueUrl();
        Map<String, SendMessageRequest> requests = createRequestsOfSizeToDest(numMessages, queueUrl);
        Map<String, CompletableFuture<SendMessageResponse>> responses = sendSendMessageRequests(requests);

        CompletableFuture.allOf(responses.values().toArray(new CompletableFuture[0])).join();
        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                                                                    .maxNumberOfMessages(numMessages)
                                                                    .queueUrl(queueUrl)
                                                                    .build();

        List<Message> messages = client.receiveMessage(receiveRequest).messages();
        Map<String, DeleteMessageRequest> deleteRequests;
        List<CompletableFuture<DeleteMessageResponse>> deleteResponses = new ArrayList<>();
        while (!messages.isEmpty()) {
            deleteRequests = createDeleteRequests(messages, queueUrl);
            deleteResponses.addAll(sendDeleteMessageRequests(deleteRequests).values());
            messages = client.receiveMessage(receiveRequest).messages();
        }
        deleteAllMessagesInQueue(queueUrl);
        assertThat(deleteResponses.size()).isEqualTo(numMessages);
    }

    @Test
    public void changeVisibilityTenMessages() {
        int numMessages = 10;
        String queueUrl = client.createQueue(CreateQueueRequest.builder().queueName(TEST_QUEUE_PREFIX + "11").build()).queueUrl();
        Map<String, SendMessageRequest> requests = createRequestsOfSizeToDest(numMessages, queueUrl);
        Map<String, CompletableFuture<SendMessageResponse>> responses = sendSendMessageRequests(requests);

        CompletableFuture.allOf(responses.values().toArray(new CompletableFuture[0])).join();
        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                                                                    .maxNumberOfMessages(numMessages)
                                                                    .queueUrl(queueUrl)
                                                                    .build();

        List<Message> messages = client.receiveMessage(receiveRequest).messages();
        Map<String, ChangeMessageVisibilityRequest> changeVisibilityRequests;
        List<CompletableFuture<ChangeMessageVisibilityResponse>> changeVisibilityResponses = new ArrayList<>();
        while (!messages.isEmpty()) {
            changeVisibilityRequests = createChangeVisibilityRequest(messages, queueUrl);
            changeVisibilityResponses.addAll(sendChangeVisibilityRequest(changeVisibilityRequests).values());
            messages = client.receiveMessage(receiveRequest).messages();
        }
        deleteAllMessagesInQueue(queueUrl);
        assertThat(changeVisibilityResponses.size()).isEqualTo(numMessages);
    }

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
            Map<String, CompletableFuture<SendMessageResponse>> newResponses = sendSendMessageRequests(startingId, numMessages,
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

        assertThat(responses).hasSameSizeAs(requests);
        byte[] expectedMd5;
        for (int i = 0; i < responses.size(); i++) {
            String key = Integer.toString(i);
            String requestBody = requests.get(key).messageBody();
            expectedMd5 = Md5Utils.computeMD5Hash(requestBody.getBytes(StandardCharsets.UTF_8));
            String expectedHash = BinaryUtils.toHex(expectedMd5);
            assertThat(responses.get(key).join().md5OfMessageBody()).isEqualTo(expectedHash);
        }
    }

    private Map<String, SendMessageRequest> createRequestsOfSize(int size) {
        return createRequestsOfSizeToDest(size, defaultQueueUrl);
    }

    private Map<String, SendMessageRequest> createRequestsOfSizeToDiffDestinations(int numDestinations, int destinationSize) {
        String myQueueUrl;
        Map<String, SendMessageRequest> requests = new HashMap<>();
        for (int i = 0; i < numDestinations; i++) {
            myQueueUrl = client.createQueue(CreateQueueRequest.builder().queueName(TEST_QUEUE_PREFIX + i).build()).queueUrl();
            for (int j = 0; j < destinationSize; j++) {
                String key = Integer.toString(i*destinationSize + j);
                requests.put(key, createSendMessageRequestToDestination(Integer.toString(i), myQueueUrl));
            }
        }
        return requests;
    }

    private Map<String, SendMessageRequest> createRequestsOfSizeToDest(int size, String queueUrl) {
        Map<String, SendMessageRequest> requests = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String key = Integer.toString(i);
            requests.put(key, createSendMessageRequestToDestination(key, queueUrl));
        }
        return requests;
    }

    private SendMessageRequest createSendMessageRequestToDestination(String messageBody, String queueUrl) {
        return SendMessageRequest.builder()
                                 .messageBody(messageBody)
                                 .queueUrl(queueUrl)
                                 .build();
    }

    private Map<String, CompletableFuture<SendMessageResponse>> sendSendMessageRequests(Map<String, SendMessageRequest> requests) {
        return sendSendMessageRequests(0, requests.size(), requests);
    }

    private Map<String, CompletableFuture<SendMessageResponse>> sendSendMessageRequests(int startingId, int size,
                                                                                Map<String, SendMessageRequest>  requests) {
        Map<String, CompletableFuture<SendMessageResponse>> responses = new HashMap<>();
        for (int i = startingId; i < startingId + size; i++) {
            String key = Integer.toString(i);
            SendMessageRequest request = requests.get(key);
            responses.put(key, batchManager.sendMessage(request));
        }
        return responses;
    }

    private static Map<String, DeleteMessageRequest> createDeleteRequests(List<Message> messages, String queueUrl) {
        Map<String, DeleteMessageRequest> requests = new HashMap<>();
        int i = 0;
        for (Message message : messages) {
            requests.put(Integer.toString(i), DeleteMessageRequest.builder()
                                                                  .receiptHandle(message.receiptHandle())
                                                                  .queueUrl(queueUrl)
                                                                  .build());
            i++;
        }
        return requests;
    }

    private Map<String, CompletableFuture<DeleteMessageResponse>> sendDeleteMessageRequests(Map<String,
        DeleteMessageRequest> requests) {
        Map<String, CompletableFuture<DeleteMessageResponse>> responses = new HashMap<>();
        for (int i = 0; i < requests.size(); i++) {
            String key = Integer.toString(i);
            DeleteMessageRequest request = requests.get(key);
            responses.put(key, batchManager.deleteMessage(request));
        }
        return responses;
    }

    private Map<String, ChangeMessageVisibilityRequest> createChangeVisibilityRequest(List<Message> messages, String queueUrl) {
        Map<String, ChangeMessageVisibilityRequest> requests = new HashMap<>();
        int i = 0;
        for (Message message : messages) {
            requests.put(Integer.toString(i), ChangeMessageVisibilityRequest.builder()
                                                                            .receiptHandle(message.receiptHandle())
                                                                            .queueUrl(queueUrl)
                                                                            .build());
            i++;
        }
        return requests;
    }

    private Map<String, CompletableFuture<ChangeMessageVisibilityResponse>> sendChangeVisibilityRequest(Map<String,
        ChangeMessageVisibilityRequest> requests) {
        Map<String, CompletableFuture<ChangeMessageVisibilityResponse>> responses = new HashMap<>();
        for (int i = 0; i < requests.size(); i++) {
            String key = Integer.toString(i);
            ChangeMessageVisibilityRequest request = requests.get(key);
            responses.put(key, batchManager.changeMessageVisibility(request));
        }
        return responses;
    }

    private static void deleteAllMessagesInQueue(String queueUrl) {
        PurgeQueueRequest purgeQueueRequest = PurgeQueueRequest.builder()
                                                               .queueUrl(queueUrl)
                                                               .build();
        client.purgeQueue(purgeQueueRequest);
    }
}
