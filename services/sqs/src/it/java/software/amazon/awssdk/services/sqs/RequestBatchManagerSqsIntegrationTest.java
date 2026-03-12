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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import software.amazon.awssdk.services.sqs.batchmanager.SqsAsyncBatchManager;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Md5Utils;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RequestBatchManagerSqsIntegrationTest extends IntegrationTestBase {

    private static final int DEFAULT_MAX_BATCH_OPEN = 200;
    private static final String TEST_QUEUE_PREFIX = "myTestQueue";
    private static final Logger log = Logger.loggerFor(RequestBatchManagerSqsIntegrationTest.class);
    private static SqsAsyncClient client;
    private static String defaultQueueUrl;
    private SqsAsyncBatchManager batchManager;

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        client = createSqsAyncClient();


        defaultQueueUrl = client.createQueue(CreateQueueRequest.builder()
                                                               .queueName(TEST_QUEUE_PREFIX + "0")
                                                               .build())
                                .get(3, TimeUnit.SECONDS)
                                .queueUrl();
    }

    @AfterAll
    public static void tearDownAfterClass() {
        purgeQueue(defaultQueueUrl);
        client.close();
    }

    @BeforeEach
    public void setUp() {
        batchManager = client.batchManager();
    }

    @AfterEach
    public void tearDown() {
        batchManager.close();
    }

    @ParameterizedTest
    @MethodSource("provideQueueUrls")
    public void sendTenMessages(String queueUrl) {
        executeSendMessagesTest(10, queueUrl);
    }

    @ParameterizedTest
    @MethodSource("provideQueueUrls")
    public void sendTwentyMessages(String queueUrl) {
        executeSendMessagesTest(20, queueUrl);
    }

    @ParameterizedTest
    @MethodSource("provideQueueUrls")
    public void scheduleSendFiveMessages(String queueUrl) {
        long startTime = System.nanoTime();
        Map<String, SendMessageRequest> requests = createSendMessageRequests(5, queueUrl);
        Map<String, CompletableFuture<SendMessageResponse>> responses = sendMessages(requests);
        waitForAllResponses(responses);
        long endTime = System.nanoTime();

        assertThat(Duration.ofNanos(endTime - startTime).toMillis()).isGreaterThan(DEFAULT_MAX_BATCH_OPEN);
        verifyResponses(requests, responses);
    }

    @ParameterizedTest
    @MethodSource("provideQueueUrls")
    public void scheduleTwoSendMessageBatches(String queueUrl) {
        long startTime = System.nanoTime();
        Map<String, SendMessageRequest> requests = createSendMessageRequests(10, queueUrl);
        Map<String, CompletableFuture<SendMessageResponse>> responses = sendMessages(0, 5, requests);
        waitFor(DEFAULT_MAX_BATCH_OPEN + 10);
        responses.putAll(sendMessages(5, 5, requests));
        waitForAllResponses(responses);
        long endTime = System.nanoTime();

        assertThat(responses).hasSize(10);
        assertThat(Duration.ofNanos(endTime - startTime).toMillis()).isGreaterThan(DEFAULT_MAX_BATCH_OPEN + 100);
        verifyResponses(requests, responses);
    }

    @ParameterizedTest
    @MethodSource("provideMessageAndThreadCounts")
    public void sendMultipleMessagesWithMultiThread(int numMessages, int numThreads, String queueUrl) {
        executeConcurrentSendMessagesTest(numThreads, numMessages, queueUrl);
    }

    @ParameterizedTest
    @MethodSource("provideQueueUrls")
    public void scheduleFiveMessagesWithEachThreadToDifferentLocations(String queueUrl) throws Exception {
        int numThreads = 10;
        int numMessages = 5;

        Map<String, SendMessageRequest> requests = createSendMessageRequestsForDifferentDestinations(numThreads, numMessages);
        ConcurrentHashMap<String, CompletableFuture<SendMessageResponse>> responses = new ConcurrentHashMap<>();
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

        List<CompletableFuture<Map<String, CompletableFuture<SendMessageResponse>>>> sendRequestFutures =
            createThreadsAndSendMessages(numThreads, numMessages, requests, responses, executorService);

        checkThreadedResponses(requests, responses, sendRequestFutures);

        cleanupQueues(numThreads);
    }

    @ParameterizedTest
    @MethodSource("messageAndQueueProvider")
    public void deleteMessages(int numMessages, String queueUrl) throws Exception {
        executeDeleteMessagesTest(numMessages, queueUrl);
    }

    @ParameterizedTest
    @MethodSource("messageAndQueueProvider")
    public void changeVisibilityMessages(int numMessages, String queueUrl) throws Exception {
        executeChangeVisibilityTest(numMessages, queueUrl);
    }


    @ParameterizedTest
    @MethodSource("provideQueueUrls")
    void sendMessagesWhichCanExceed256KiBCollectively(String queueUrl) {

        String largeMessageBody = createLargeString('a', 256_000);

        List<CompletableFuture<SendMessageResponse>> futures = new ArrayList<>();

        // Send the large message 10 times and collect the futures
        for (int i = 0; i < 10; i++) {
            CompletableFuture<SendMessageResponse> future =
                batchManager.sendMessage(r -> r.queueUrl(queueUrl).messageBody(largeMessageBody));
            futures.add(future);
        }

        // Wait for all sendMessage futures to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allFutures.join();

        // Validate that all responses have a non-null messageId
        futures.forEach(future -> {
            SendMessageResponse response = future.join();
            assertThat(response.messageId()).isNotNull();
            assertThat(response.md5OfMessageBody()).isNotNull();
        });
    }


    private String createLargeString(char ch, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ch);
        }
        return sb.toString();
    }


    private static Stream<Arguments> provideQueueUrls() {
        return Stream.of(
            Arguments.of(defaultQueueUrl)
        );
    }

    private static Stream<Arguments> messageAndQueueProvider() {
        return Stream.of(
            Arguments.of(1, defaultQueueUrl),
            Arguments.of(5, defaultQueueUrl),
            Arguments.of(10, defaultQueueUrl)
        );
    }

    private static Stream<Arguments> provideMessageAndThreadCounts() {
        return Stream.of(
            Arguments.of(1, 1, defaultQueueUrl),
            Arguments.of(10, 1, defaultQueueUrl),
            Arguments.of(1, 10, defaultQueueUrl),
            Arguments.of(10, 10, defaultQueueUrl)
        );
    }

    private void executeSendMessagesTest(int numMessages, String queueUrl) {
        Map<String, SendMessageRequest> requests = createSendMessageRequests(numMessages, queueUrl);
        Map<String, CompletableFuture<SendMessageResponse>> responses = sendMessages(requests);
        verifyResponses(requests, responses);
    }

    private void executeConcurrentSendMessagesTest(int numThreads, int numMessages, String queueUrl) {
        Map<String, SendMessageRequest> requests = createSendMessageRequests(numThreads * numMessages, queueUrl);
        ConcurrentHashMap<String, CompletableFuture<SendMessageResponse>> responses = new ConcurrentHashMap<>();
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

        List<CompletableFuture<Map<String, CompletableFuture<SendMessageResponse>>>> sendRequestFutures =
            createThreadsAndSendMessages(numThreads, numMessages, requests, responses, executorService);

        checkThreadedResponses(requests, responses, sendRequestFutures);
    }

    private void executeDeleteMessagesTest(int numMessages, String queueUrl) throws Exception {
        Map<String, SendMessageRequest> requests = createSendMessageRequestsForDestination(numMessages, queueUrl);
        Map<String, CompletableFuture<SendMessageResponse>> responses = sendMessages(requests);
        waitForAllResponses(responses);

        List<Message> messages = receiveMessages(queueUrl, numMessages);
        deleteMessagesInBatches(messages, queueUrl);
        purgeQueue(queueUrl);

        assertThat(responses).hasSize(numMessages);
    }

    private void executeChangeVisibilityTest(int numMessages, String queueUrl) throws Exception {
        Map<String, SendMessageRequest> requests = createSendMessageRequestsForDestination(numMessages, queueUrl);
        Map<String, CompletableFuture<SendMessageResponse>> responses = sendMessages(requests);
        waitForAllResponses(responses);

        List<Message> messages = receiveMessages(queueUrl, numMessages);
        changeVisibilityForMessages(messages, queueUrl);
        assertThat(responses).hasSize(numMessages);
    }

    private Map<String, SendMessageRequest> createSendMessageRequests(int size, String queueUrl) {
        return createSendMessageRequestsForDestination(size, queueUrl);
    }

    private Map<String, SendMessageRequest> createSendMessageRequestsForDestination(int size, String queueUrl) {
        Map<String, SendMessageRequest> requests = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String key = Integer.toString(i);
            requests.put(key, createSendMessageRequestToDestination(key, queueUrl));
        }
        return requests;
    }

    private SendMessageRequest createSendMessageRequestToDestination(String messageBody, String queueUrl) {
        SendMessageRequest.Builder requestBuilder = SendMessageRequest.builder()
                                                                      .messageBody(messageBody)
                                                                      .queueUrl(queueUrl);
        // Check if the queue URL corresponds to a FIFO queue
        if (queueUrl.endsWith(".fifo")) {
            // Include a MessageGroupId for FIFO queues
            requestBuilder.messageGroupId("default-group");  // Use a consistent or meaningful group ID
        }
        return requestBuilder.build();
    }

    private Map<String, CompletableFuture<SendMessageResponse>> sendMessages(Map<String, SendMessageRequest> requests) {
        return sendMessages(0, requests.size(), requests);
    }

    private Map<String, CompletableFuture<SendMessageResponse>> sendMessages(int start, int size, Map<String, SendMessageRequest> requests) {
        Map<String, CompletableFuture<SendMessageResponse>> responses = new HashMap<>();
        for (int i = start; i < start + size; i++) {
            String key = Integer.toString(i);
            SendMessageRequest request = requests.get(key);
            responses.put(key, batchManager.sendMessage(request));
        }
        return responses;
    }

    private List<CompletableFuture<Map<String, CompletableFuture<SendMessageResponse>>>> createThreadsAndSendMessages(
        int numThreads, int numMessages, Map<String, SendMessageRequest> requests,
        ConcurrentHashMap<String, CompletableFuture<SendMessageResponse>> responses, ExecutorService executorService) {
        List<CompletableFuture<Map<String, CompletableFuture<SendMessageResponse>>>> executions = new ArrayList<>();
        for (int i = 0; i < numThreads; i++) {
            executions.add(sendMessagesToDestination(i * numMessages, numMessages, requests, responses, executorService));
        }
        return executions;
    }

    private CompletableFuture<Map<String, CompletableFuture<SendMessageResponse>>> sendMessagesToDestination(
        int start, int size, Map<String, SendMessageRequest> requests,
        ConcurrentHashMap<String, CompletableFuture<SendMessageResponse>> responses, ExecutorService executorService) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, CompletableFuture<SendMessageResponse>> newResponses = sendMessages(start, size, requests);
            responses.putAll(newResponses);
            return newResponses;
        }, executorService);
    }

    private void checkThreadedResponses(Map<String, SendMessageRequest> requests,
                                        ConcurrentHashMap<String, CompletableFuture<SendMessageResponse>> responses,
                                        List<CompletableFuture<Map<String, CompletableFuture<SendMessageResponse>>>> sendRequestFutures) {
        for (CompletableFuture<Map<String, CompletableFuture<SendMessageResponse>>> sendRequestFuture : sendRequestFutures) {
            try {
                waitForAllResponses(sendRequestFuture.get(300, TimeUnit.MILLISECONDS));
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                log.error(() -> String.valueOf(e));
            }
        }
        verifyResponses(requests, responses);
    }

    private void verifyResponses(Map<String, SendMessageRequest> requests, Map<String, CompletableFuture<SendMessageResponse>> responses) {
        assertThat(responses).hasSameSizeAs(requests);
        responses.forEach((key, future) -> {
            String expectedMd5 = BinaryUtils.toHex(Md5Utils.computeMD5Hash(requests.get(key).messageBody().getBytes(StandardCharsets.UTF_8)));
            assertThat(future.join().md5OfMessageBody()).isEqualTo(expectedMd5);
        });
    }

    private void waitForAllResponses(Map<String, CompletableFuture<SendMessageResponse>> responses) {
        CompletableFuture.allOf(responses.values().toArray(new CompletableFuture[0])).join();
    }

    private List<Message> receiveMessages(String queueUrl, int numMessages) throws Exception {
        return client.receiveMessage(ReceiveMessageRequest.builder()
                                                          .queueUrl(queueUrl)
                                                          .maxNumberOfMessages(numMessages)
                                                          .build())
                     .get(3, TimeUnit.SECONDS)
                     .messages();
    }

    private void deleteMessagesInBatches(List<Message> messages, String queueUrl) throws Exception {
        while (!messages.isEmpty()) {
            Map<String, DeleteMessageRequest> deleteRequests = createDeleteRequests(messages, queueUrl);
            sendDeleteMessages(deleteRequests);
            messages = receiveMessages(queueUrl, messages.size());
        }
    }

    private void changeVisibilityForMessages(List<Message> messages, String queueUrl) throws Exception {
        while (!messages.isEmpty()) {
            Map<String, ChangeMessageVisibilityRequest> visibilityRequests = createChangeVisibilityRequests(messages, queueUrl);
            sendChangeVisibilityRequests(visibilityRequests);
            messages = receiveMessages(queueUrl, messages.size());
        }
    }

    private Map<String, DeleteMessageRequest> createDeleteRequests(List<Message> messages, String queueUrl) {
        Map<String, DeleteMessageRequest> requests = new HashMap<>();
        for (int i = 0; i < messages.size(); i++) {
            requests.put(Integer.toString(i), DeleteMessageRequest.builder()
                                                                  .receiptHandle(messages.get(i).receiptHandle())
                                                                  .queueUrl(queueUrl)
                                                                  .build());
        }
        return requests;
    }

    private void sendDeleteMessages(Map<String, DeleteMessageRequest> deleteRequests) {
        deleteRequests.forEach((key, request) -> batchManager.deleteMessage(request));
    }

    private Map<String, ChangeMessageVisibilityRequest> createChangeVisibilityRequests(List<Message> messages, String queueUrl) {
        Map<String, ChangeMessageVisibilityRequest> requests = new HashMap<>();
        for (int i = 0; i < messages.size(); i++) {
            requests.put(Integer.toString(i), ChangeMessageVisibilityRequest.builder()
                                                                            .receiptHandle(messages.get(i).receiptHandle())
                                                                            .queueUrl(queueUrl)
                                                                            .build());
        }
        return requests;
    }

    private void sendChangeVisibilityRequests(Map<String, ChangeMessageVisibilityRequest> visibilityRequests) {
        visibilityRequests.forEach((key, request) -> batchManager.changeMessageVisibility(request));
    }

    private String createQueue(String queueName) throws Exception {
        return client.createQueue(CreateQueueRequest.builder()
                                                    .queueName(queueName)
                                                    .build())
                     .get(3, TimeUnit.SECONDS)
                     .queueUrl();
    }

    private void cleanupQueues(int numThreads) throws Exception {
        for (int i = 1; i < numThreads; i++) {
            String queueUrl = client.getQueueUrl(GetQueueUrlRequest.builder().queueName(TEST_QUEUE_PREFIX + i).build())
                                    .get(3, TimeUnit.SECONDS)
                                    .queueUrl();
            purgeQueue(queueUrl);
        }
    }

    private static void purgeQueue(String queueUrl) {
        client.purgeQueue(PurgeQueueRequest.builder()
                                           .queueUrl(queueUrl)
                                           .build());
    }

    private void waitFor(int milliseconds) {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            latch.await(milliseconds, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Map<String, SendMessageRequest> createSendMessageRequestsForDifferentDestinations(int numDestinations, int destinationSize) throws Exception {
        Map<String, SendMessageRequest> requests = new HashMap<>();
        for (int i = 0; i < numDestinations; i++) {
            // Create a new queue for each destination
            String queueUrl = createQueue(TEST_QUEUE_PREFIX + i);

            for (int j = 0; j < destinationSize; j++) {
                // Generate a unique key for each message
                String key = Integer.toString(i * destinationSize + j);
                // Create a SendMessageRequest for the specific destination (queue)
                requests.put(key, createSendMessageRequestToDestination(key, queueUrl));
            }
        }
        return requests;
    }
}