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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.services.sqs.internal.batchmanager.RequestBatchManager.USER_AGENT_APPLIER;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.internal.batchmanager.ReceiveSqsMessageHelper;
import software.amazon.awssdk.services.sqs.internal.batchmanager.ResponseBatchConfiguration;
import software.amazon.awssdk.services.sqs.model.BatchResultErrorEntry;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.testutils.LogCaptor;

@ExtendWith(MockitoExtension.class)
class ReceiveSqsMessageHelperTest {

    private static final String QUEUE_URL = "test-queue-url";
    private final Duration visibilityTimeout = Duration.ofSeconds(30);
    @Mock
    private ScheduledExecutorService scheduledExecutorService;
    @Mock
    private SqsAsyncClient sqsClient;
    private ResponseBatchConfiguration config;
    private ReceiveSqsMessageHelper receiveSqsMessageHelper;

    @BeforeEach
    void setUp() {

        config = ResponseBatchConfiguration.builder()
                                           .receiveMessageAttributeNames(Arrays.asList(
                                               "attribute1", "attribute2"))
                                           .visibilityTimeout(Duration.ofSeconds(20))
                                           .build();
        receiveSqsMessageHelper = new ReceiveSqsMessageHelper(QUEUE_URL, sqsClient, visibilityTimeout, config);
    }

    @AfterEach
    void clear(){
        receiveSqsMessageHelper.clear();
    }

    @Test
    void asyncReceiveMessageSuccess() throws Exception {
        ReceiveMessageResponse response = ReceiveMessageResponse.builder()
                                                                .messages(Message.builder().body("Message 1").build())
                                                                .build();

        CompletableFuture<ReceiveMessageResponse> futureResponse = new CompletableFuture<>();
        futureResponse.complete(response);
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(futureResponse);

        CompletableFuture<ReceiveSqsMessageHelper> result = receiveSqsMessageHelper.asyncReceiveMessage();
        assertTrue(result.isDone());
        assertNull(result.get().getException());
        assertFalse(result.get().isEmpty());
        assertEquals(1, result.get().messagesSize());
    }

    @Test
    void multipleMessageGetsAdded() throws Exception {
        ReceiveMessageResponse response = generateMessageResponse(10);

        CompletableFuture<ReceiveMessageResponse> futureResponse = new CompletableFuture<>();
        futureResponse.complete(response);
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(futureResponse);

        CompletableFuture<ReceiveSqsMessageHelper> result = receiveSqsMessageHelper.asyncReceiveMessage();
        assertEquals(10, result.get(1, TimeUnit.SECONDS).messagesSize());
        assertTrue(result.isDone());
        assertNull(result.get().getException());
        assertFalse(result.get().isEmpty());
    }

    @Test
    void asyncReceiveMessageFailure() throws Exception {
        // Mocking receiveMessage to throw an exception
        CompletableFuture<ReceiveMessageResponse> futureResponse = new CompletableFuture<>();
        futureResponse.completeExceptionally(new RuntimeException("SQS error"));
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(futureResponse);

        // Call asyncReceiveMessage and expect it to handle the exception
        CompletableFuture<ReceiveSqsMessageHelper> result = receiveSqsMessageHelper.asyncReceiveMessage();

        // Verify that the CompletableFuture is completed exceptionally
        ReceiveSqsMessageHelper messageBatch = result.get(2, TimeUnit.SECONDS);

        // Verify the exception in the ReceiveSqsMessageHelper
        assertNotNull(messageBatch.getException());
        assertEquals("SQS error", receiveSqsMessageHelper.getException().getMessage());
        assertTrue(receiveSqsMessageHelper.isEmpty());
    }

    @Test
    void emptyResponseReceivedFromSQS() throws Exception {
        ReceiveMessageResponse response = ReceiveMessageResponse.builder()
                                                                .messages(Collections.emptyList())
                                                                .build();
        CompletableFuture<ReceiveMessageResponse> futureResponse = new CompletableFuture<>();
        futureResponse.complete(response);
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(futureResponse);

        receiveSqsMessageHelper.asyncReceiveMessage().get(3, TimeUnit.SECONDS);
        assertTrue(receiveSqsMessageHelper.isEmpty());
    }

    @Test
    void concurrencyTestForRemoveMessage() throws Exception {
        // Mocking receiveMessage to return 10 messages
        ReceiveMessageResponse response = generateMessageResponse(10);
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(CompletableFuture.completedFuture(response));

        // Calling asyncReceiveMessage to initialize messages
        receiveSqsMessageHelper.asyncReceiveMessage().get(3, TimeUnit.SECONDS);

        // Verify initial state
        assertEquals(10, receiveSqsMessageHelper.messagesSize());

        // Create threads to call removeMessage concurrently
        List<Thread> threads = new ArrayList<>();
        AtomicInteger successfulRemovals = new AtomicInteger(0);
        for (int i = 0; i < 10; i++) {
            threads.add(new Thread(() -> {
                Message message = receiveSqsMessageHelper.removeMessage();
                if (message != null) {
                    int messageNumber = Integer.parseInt(message.body().split(" ")[1]);
                    assertTrue(messageNumber >= 0 && messageNumber < 10);
                    successfulRemovals.incrementAndGet();
                }
            }));
        }
        // Start all threads
        threads.forEach(Thread::start);
        // Wait for all threads to finish
        for (Thread thread : threads) {
            thread.join();
        }
        // Verify final state
        assertEquals(10, successfulRemovals.get());
        assertEquals(0, receiveSqsMessageHelper.messagesSize());
        assertNull(receiveSqsMessageHelper.removeMessage());
        assertTrue(receiveSqsMessageHelper.isEmpty());
    }

    private ReceiveMessageResponse generateMessageResponse(int count) {
        List<Message> messages = IntStream.range(0, count)
                                          .mapToObj(i -> Message.builder().body("Message " + i).receiptHandle("handle" + i).build())
                                          .collect(Collectors.toList());
        return ReceiveMessageResponse.builder().messages(messages).build();
    }

    @Test
    void changeMessageVisibilityBatchIsCalledOnClearing() throws Exception {
        Message message1 = Message.builder().body("Message 1").receiptHandle("handle1").build();
        Message message2 = Message.builder().body("Message 2").receiptHandle("handle2").build();
        List<Message> messages = Arrays.asList(message1, message2);

        ReceiveMessageResponse response = ReceiveMessageResponse.builder()
                                                                .messages(messages)
                                                                .build();
        CompletableFuture<ReceiveMessageResponse> futureResponse = new CompletableFuture<>();
        futureResponse.complete(response);
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(futureResponse);
        when(sqsClient.changeMessageVisibilityBatch(any(ChangeMessageVisibilityBatchRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(ChangeMessageVisibilityBatchResponse.builder().build()));

        receiveSqsMessageHelper.asyncReceiveMessage().get(3, TimeUnit.SECONDS);
        receiveSqsMessageHelper.clear();

        assertTrue(receiveSqsMessageHelper.isEmpty());
        verify(sqsClient, times(1)).changeMessageVisibilityBatch(any(ChangeMessageVisibilityBatchRequest.class));
    }

    @Test
    void immediateMessageProcessingWithoutExpiry() throws Exception {
        ReceiveMessageResponse response = ReceiveMessageResponse.builder()
                                                                .messages(Message.builder().body("Message 1").build())
                                                                .build();

        CompletableFuture<ReceiveMessageResponse> futureResponse = new CompletableFuture<>();
        futureResponse.complete(response);
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(futureResponse);

        CompletableFuture<ReceiveSqsMessageHelper> completableFuture = receiveSqsMessageHelper.asyncReceiveMessage();
        ReceiveSqsMessageHelper receiveSqsMessageHelper1 = completableFuture.get(2, TimeUnit.SECONDS);
        Message message = receiveSqsMessageHelper1.removeMessage();
        assertEquals(message, Message.builder().body("Message 1").build());
    }

    @Test
    void expiredBatchesClearsItself() throws Exception {
        // Test setup: creating a new instance of ReceiveSqsMessageHelper
        ReceiveSqsMessageHelper batch = new ReceiveSqsMessageHelper("queueUrl", sqsClient
            , Duration.ofNanos(1), config);

        // Mocking receiveMessage to return a single message to open the batch
        ReceiveMessageResponse response = generateMessageResponse(10);
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(CompletableFuture.completedFuture(response));

        CompletableFuture<ReceiveSqsMessageHelper> result = batch.asyncReceiveMessage();
        ReceiveSqsMessageHelper messageBatch = result.get(1, TimeUnit.SECONDS);
        Thread.sleep(10);
        assertNull(messageBatch.removeMessage());
        assertTrue(messageBatch.isEmpty());
        verify(sqsClient, times(1))
            .changeMessageVisibilityBatch(any(ChangeMessageVisibilityBatchRequest.class));

    }

    @Test
    void asyncReceiveMessageArgs() throws Exception {

        ResponseBatchConfiguration batchOverrideConfig = ResponseBatchConfiguration.builder()

                                                                                   .receiveMessageAttributeNames(Arrays.asList(
                                                                                       "custom1", "custom2"))
                                                                                   .messageSystemAttributeNames(Arrays.asList(
                                                                                       MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT))
                                                                                   .visibilityTimeout(Duration.ofSeconds(9))
                                                                                   .messageMinWaitDuration(Duration.ofMillis(200))
                                                                                   .build();

        ReceiveSqsMessageHelper batch = new ReceiveSqsMessageHelper(
            QUEUE_URL, sqsClient, Duration.ofSeconds(9), batchOverrideConfig);


        // Mocking receiveMessage to return a single message
        ReceiveMessageResponse response = generateMessageResponse(1);
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(response));

        // Call asyncReceiveMessage
        batch.asyncReceiveMessage().get(3, TimeUnit.SECONDS);

        // Verify that receiveMessage was called with the correct arguments
        ReceiveMessageRequest expectedRequest =
            ReceiveMessageRequest.builder()
                                 .queueUrl(QUEUE_URL)
                                 .maxNumberOfMessages(10)
                                 .messageAttributeNames("custom1", "custom2")
                                 .messageSystemAttributeNames(Arrays.asList(
                                     MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT))
                                 .visibilityTimeout(9)
                                 .overrideConfiguration(o -> o.applyMutation(USER_AGENT_APPLIER))
                                 .build();

        verify(sqsClient, times(1)).receiveMessage(eq(expectedRequest));
    }

    @Test
    void clear_whenChangeVisibilityReportsFailedEntries_logsDebugWithCount() throws Exception {
        receiveThreeMessages();
        ChangeMessageVisibilityBatchResponse partialFailure =
            ChangeMessageVisibilityBatchResponse.builder()
                                                .failed(BatchResultErrorEntry.builder().id("0").code("Error").build(),
                                                        BatchResultErrorEntry.builder().id("2").code("Error").build())
                                                .build();
        when(sqsClient.changeMessageVisibilityBatch(any(ChangeMessageVisibilityBatchRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(partialFailure));

        try (LogCaptor logCaptor = LogCaptor.create(Level.DEBUG)) {
            assertThatCode(receiveSqsMessageHelper::clear).doesNotThrowAnyException();

            assertThat(logCaptor.loggedEvents()).anySatisfy(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.DEBUG);
                assertThat(event.getMessage().getFormattedMessage())
                    .contains("Failed to reset the visibility timeout of 2 unprocessed messages for queueUrl: "
                              + QUEUE_URL);
            });
        }
    }

    @Test
    void clear_whenChangeVisibilityThrowsSynchronously_logsWarnAndDoesNotThrow() throws Exception {
        receiveThreeMessages();
        when(sqsClient.changeMessageVisibilityBatch(any(ChangeMessageVisibilityBatchRequest.class)))
            .thenThrow(new IllegalStateException("client closed"));

        assertNackFailureLoggedWithoutThrowing();
        assertTrue(receiveSqsMessageHelper.isEmpty());
    }

    @Test
    void clear_whenRacingRemoveMessage_handlesEveryMessageOnce() throws Exception {
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(generateMessageResponse(10)));
        List<ChangeMessageVisibilityBatchRequest> nacks = Collections.synchronizedList(new ArrayList<>());
        // Lenient because clear() never calls SQS if removeMessage takes every message.
        lenient().when(sqsClient.changeMessageVisibilityBatch(any(ChangeMessageVisibilityBatchRequest.class)))
                 .thenAnswer(invocation -> {
            nacks.add(invocation.getArgument(0));
            return CompletableFuture.completedFuture(ChangeMessageVisibilityBatchResponse.builder().build());
        });
        ExecutorService racers = Executors.newFixedThreadPool(2);
        try {
            for (int round = 0; round < 500; round++) {
                nacks.clear();
                ReceiveSqsMessageHelper helper = new ReceiveSqsMessageHelper(QUEUE_URL, sqsClient, visibilityTimeout, config);
                helper.asyncReceiveMessage().get(1, TimeUnit.SECONDS);
                CountDownLatch start = new CountDownLatch(1);
                Future<Integer> removed = racers.submit(() -> {
                    start.await();
                    int count = 0;
                    while (helper.removeMessage() != null) {
                        count++;
                    }
                    return count;
                });
                Future<?> cleared = racers.submit(() -> {
                    start.await();
                    helper.clear();
                    return null;
                });
                start.countDown();

                cleared.get(5, TimeUnit.SECONDS);
                int nacked = nacks.stream().mapToInt(r -> r.entries().size()).sum();
                assertThat(removed.get(5, TimeUnit.SECONDS) + nacked).isEqualTo(10);
            }
        } finally {
            racers.shutdownNow();
        }
    }

    private void receiveThreeMessages() throws Exception {
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(generateMessageResponse(3)));
        receiveSqsMessageHelper.asyncReceiveMessage().get(3, TimeUnit.SECONDS);
    }

    private void assertNackFailureLoggedWithoutThrowing() {
        try (LogCaptor logCaptor = LogCaptor.create(Level.WARN)) {
            assertThatCode(receiveSqsMessageHelper::clear).doesNotThrowAnyException();

            assertThat(logCaptor.loggedEvents()).anySatisfy(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getMessage().getFormattedMessage())
                    .contains("Failed to reset the visibility timeout of unprocessed messages for queueUrl: " + QUEUE_URL);
            });
        }
    }

}
