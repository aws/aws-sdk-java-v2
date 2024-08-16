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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.internal.batchmanager.ReceiveMessageCompletableFuture;
import software.amazon.awssdk.services.sqs.internal.batchmanager.ReceiveQueueBuffer;
import software.amazon.awssdk.services.sqs.internal.batchmanager.QueueAttributesManager;
import software.amazon.awssdk.services.sqs.internal.batchmanager.ResponseBatchConfiguration;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;
import software.amazon.awssdk.testutils.LogCaptor;


@ExtendWith(MockitoExtension.class)
public class ReceiveQueueBufferTest {

    @Mock private SqsAsyncClient sqsClient;

    private QueueAttributesManager queueAttributesManager;

    private ScheduledExecutorService executor;


    @BeforeEach
    public void setUp() {
        executor = Executors.newScheduledThreadPool(Integer.MAX_VALUE);
        Map<QueueAttributeName, String> attributes = new HashMap<>();
        attributes.put(QueueAttributeName.VISIBILITY_TIMEOUT, "30");
        attributes.put(QueueAttributeName.RECEIVE_MESSAGE_WAIT_TIME_SECONDS, "10");

        GetQueueAttributesResponse response = GetQueueAttributesResponse.builder()
                                                                        .attributes(attributes)
                                                                        .build();

        when(sqsClient.getQueueAttributes(any(GetQueueAttributesRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(response));
        queueAttributesManager = new QueueAttributesManager(sqsClient,"queueUrl");
    }

    @AfterEach
    public void clear() {

        executor.shutdownNow();

    }

    private ResponseBatchConfiguration createConfig(int maxBatchItems, boolean adaptivePrefetching,
                                                    int maxInflightReceiveBatches, int maxDoneReceiveBatches,
                                                    Duration minReceiveWaitTime) {
        return new ResponseBatchConfiguration(BatchOverrideConfiguration.builder()
                                                                        .maxBatchItems(maxBatchItems)
                                                                        .adaptivePrefetching(adaptivePrefetching)
                                                                        .maxInflightReceiveBatches(maxInflightReceiveBatches)
                                                                        .maxDoneReceiveBatches(maxDoneReceiveBatches)
                                                                        .receiveMessageAttributeNames(Collections.emptyList())
                                                                        .visibilityTimeout(Duration.ofSeconds(2))
                                                                        .minReceiveWaitTime(minReceiveWaitTime)
                                                                        .build());
    }

    @Test
    public void testReceiveMessageSuccessful() throws Exception {
        // Mock response
        ReceiveMessageResponse response = generateMessageResponse(10);
        ReceiveQueueBuffer receiveQueueBuffer = receiveQueueBuffer();
        ReceiveMessageRequest receiveMessageRequest;
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(CompletableFuture.completedFuture(response));

        // Create future and call receiveMessage

        ReceiveMessageCompletableFuture receiveMessageFuture = new ReceiveMessageCompletableFuture(10, Duration.ofSeconds(1));
        receiveQueueBuffer.receiveMessage(receiveMessageFuture);
        ReceiveMessageResponse receiveMessageResponse = receiveMessageFuture.responseCompletableFuture().get(2, TimeUnit.SECONDS);
        // Verify future completion
        assertTrue(receiveMessageFuture.responseCompletableFuture().isDone());
        assertEquals(10, receiveMessageResponse.messages().size());
    }

    private ReceiveQueueBuffer receiveQueueBuffer() {
        return new ReceiveQueueBuffer(executor, sqsClient, createConfig(10, true, 2, 1,
                                                                        Duration.ofMillis(50)), "queueUrl", queueAttributesManager);
    }

    @Test
    public void testReceiveMessageSuccessful_customRequestSize() throws Exception {
        // Mock response
        ReceiveMessageResponse response = generateMessageResponse(10);
        ReceiveQueueBuffer receiveQueueBuffer = receiveQueueBuffer();
        ReceiveMessageRequest receiveMessageRequest;
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(CompletableFuture.completedFuture(response));

        // Create future and call receiveMessage

        ReceiveMessageCompletableFuture receiveMessageFuture = new ReceiveMessageCompletableFuture(2, Duration.ofSeconds(1));
        receiveQueueBuffer.receiveMessage(receiveMessageFuture);
        ReceiveMessageResponse receiveMessageResponse = receiveMessageFuture.responseCompletableFuture().get(2, TimeUnit.SECONDS);
        // Verify future completion
        assertTrue(receiveMessageFuture.responseCompletableFuture().isDone());
        assertEquals(2, receiveMessageResponse.messages().size());
    }

    @Test
    public void multipleReceiveMessagesWithDifferentBatchSizes() throws Exception {
        ReceiveQueueBuffer receiveQueueBuffer = new ReceiveQueueBuffer(executor, sqsClient, createConfig(10, false, 10, 10,
                                                                                                         Duration.ofSeconds(1)),
                                                                       "queueUrl", queueAttributesManager);
        // Mock response

        CompletableFuture<ReceiveMessageResponse> delayedResponse = new CompletableFuture<>();


        executor.schedule(() ->  delayedResponse.complete(generateMessageResponse(5)), 500, TimeUnit.MILLISECONDS);

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(generateMessageResponse(10)))
            .thenReturn(delayedResponse);

        // Create futures and call receiveMessage
        ReceiveMessageCompletableFuture future1 = new ReceiveMessageCompletableFuture(10, Duration.ofSeconds(2));
        ReceiveMessageCompletableFuture future2 = new ReceiveMessageCompletableFuture(5, Duration.ofSeconds(2));
        receiveQueueBuffer.receiveMessage(future1);
        receiveQueueBuffer.receiveMessage(future2);


        ReceiveMessageResponse receiveMessageResponse1 = future1.responseCompletableFuture().get(1, TimeUnit.SECONDS);
        assertEquals(10, receiveMessageResponse1.messages().size());

        ReceiveMessageResponse receiveMessageResponse2 = future2.responseCompletableFuture().get(1, TimeUnit.SECONDS);
        assertEquals(5, receiveMessageResponse2.messages().size());
        // Verify future completions
        assertTrue(future2.responseCompletableFuture().isDone());
        assertTrue(future1.responseCompletableFuture().isDone());

    }

    @Test
    public void numberOfBatchesSpawned() throws Exception {
        // Mock response
        ReceiveMessageResponse response = generateMessageResponse(10);
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(CompletableFuture.completedFuture(response));

        // Create futures and call receiveMessage
        ReceiveQueueBuffer receiveQueueBuffer = receiveQueueBuffer();
        ReceiveMessageCompletableFuture future1 = new ReceiveMessageCompletableFuture(10, Duration.ofSeconds(1));
        ReceiveMessageCompletableFuture future2 = new ReceiveMessageCompletableFuture(10, Duration.ofSeconds(1));
        receiveQueueBuffer.receiveMessage(future1);
        receiveQueueBuffer.receiveMessage(future2);

        future1.responseCompletableFuture().get(1, TimeUnit.SECONDS);
        future2.responseCompletableFuture().get(1, TimeUnit.SECONDS);
        // small sleep just to make sure any scheduled task complete
        Thread.sleep(300);
        // Verify that two batches were spawned
        verify(sqsClient, atLeast(2)).receiveMessage(any(ReceiveMessageRequest.class));
    }


    @Test
    public void testReceiveMessageWithAdaptivePrefetchingTrue() throws Exception {
        // Mock response
        int MAX_BATCH_ITEMS = 10;
        ReceiveMessageResponse response = generateMessageResponse(MAX_BATCH_ITEMS);
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(response));

        // Create receiveQueueBuffer with adaptive prefetching
        ReceiveQueueBuffer receiveQueueBuffer = new ReceiveQueueBuffer(executor, sqsClient, createConfig(MAX_BATCH_ITEMS, true,
                                                                                                         2, Integer.MAX_VALUE, Duration.ofMillis(50)),
                                                                       "queueUrl", queueAttributesManager);

        // Create and send multiple ReceiveMessageCompletableFuture instances using a loop
        List<ReceiveMessageCompletableFuture> futures = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            ReceiveMessageCompletableFuture future = new ReceiveMessageCompletableFuture(1, Duration.ofSeconds(1));
            futures.add(future);
            receiveQueueBuffer.receiveMessage(future);
            Thread.sleep(10);

        }

        // Join all futures to ensure they complete
        for (ReceiveMessageCompletableFuture future : futures) {
            future.responseCompletableFuture().get(2, TimeUnit.SECONDS);
        }

        // Verify that the receiveMessage method was called the expected number of times
        verify(sqsClient, atMost(30)).receiveMessage(any(ReceiveMessageRequest.class));
        verify(sqsClient, atLeast(3)).receiveMessage(any(ReceiveMessageRequest.class));

    }

    @Test
    public void testReceiveMessageWithAdaptivePrefetchingFalse() throws Exception {
        // Mock response
        int MAX_BATCH_ITEMS = 10;
        ReceiveMessageResponse response = generateMessageResponse(MAX_BATCH_ITEMS);
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(CompletableFuture.completedFuture(response));


        // Create receiveQueueBuffer with adaptive prefetching
        ReceiveQueueBuffer receiveQueueBuffer = new ReceiveQueueBuffer(executor, sqsClient, createConfig(MAX_BATCH_ITEMS, false
            , 2, Integer.MAX_VALUE, Duration.ofMillis(50)), "queueUrl", queueAttributesManager);

        // Create and send multiple ReceiveMessageCompletableFuture instances using a loop
        List<ReceiveMessageCompletableFuture> futures = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            ReceiveMessageCompletableFuture future = new ReceiveMessageCompletableFuture(1, Duration.ofSeconds(1));
            futures.add(future);
            receiveQueueBuffer.receiveMessage(future);
            Thread.sleep(10);
        }

        // Join all futures to ensure they complete
        for (ReceiveMessageCompletableFuture future : futures) {
            future.responseCompletableFuture().get(2, TimeUnit.SECONDS);
        }

        verify(sqsClient, atLeast(300)).receiveMessage(any(ReceiveMessageRequest.class));
    }


    @Test
    public void testReceiveMessageWithAdaptivePrefetchingFalse_followsMaxDoneRecieveBatches() throws Exception {
        // Mock response
        int MAX_BATCH_ITEMS = 10;
        ReceiveMessageResponse response = generateMessageResponse(MAX_BATCH_ITEMS);
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(CompletableFuture.completedFuture(response));

        // Create receiveQueueBuffer with adaptive prefetching
        int MAX_DONE_RECEIVE_BATCH = 1;
        ReceiveQueueBuffer receiveQueueBuffer = new ReceiveQueueBuffer(executor, sqsClient, createConfig(MAX_BATCH_ITEMS, false
            , 2, MAX_DONE_RECEIVE_BATCH, Duration.ofMillis(50)), "queueUrl", queueAttributesManager);

        // Create and send multiple ReceiveMessageCompletableFuture instances using a loop
        List<ReceiveMessageCompletableFuture> futures = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            ReceiveMessageCompletableFuture future = new ReceiveMessageCompletableFuture(1, Duration.ofSeconds(1));
            futures.add(future);
            Thread.sleep(10);
            receiveQueueBuffer.receiveMessage(future);
        }

        // Join all futures to ensure they complete
        for (ReceiveMessageCompletableFuture future : futures) {
            future.responseCompletableFuture().get(2, TimeUnit.SECONDS);
        }

        verify(sqsClient, atMost(MAX_BATCH_ITEMS)).receiveMessage(any(ReceiveMessageRequest.class));
    }


    @Test
    public void receiveMessageShutDown() throws Exception {
        ReceiveQueueBuffer receiveQueueBuffer = receiveQueueBuffer();

        // Create future and call receiveMessage
        ReceiveMessageCompletableFuture future = new ReceiveMessageCompletableFuture(10, Duration.ofSeconds(1));
        receiveQueueBuffer.receiveMessage(future);


        // Shutdown receiveQueueBuffer
        receiveQueueBuffer.shutdown( );

        // Verify that the future is completed exceptionally
        assertTrue(future.responseCompletableFuture().isCompletedExceptionally());
    }

    @Test
    public void testConcurrentExecutionWithResponses() throws Exception {
        ReceiveQueueBuffer receiveQueueBuffer = receiveQueueBuffer();

        // Mock response
        ReceiveMessageResponse response = generateMessageResponse(4);
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(response));

        // Create futures and call receiveMessage concurrently
        List<ReceiveMessageCompletableFuture> futures = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ReceiveMessageCompletableFuture future = new ReceiveMessageCompletableFuture(5, Duration.ofSeconds(1));
            futures.add(future);
            new Thread(() -> receiveQueueBuffer.receiveMessage(future)).start();
        }

        // Wait for all futures to complete
        for (ReceiveMessageCompletableFuture future : futures) {
            future.responseCompletableFuture().get(2, TimeUnit.SECONDS);
        }

        // Verify all futures completed successfully and collect all messages
        int totalMessages = 0;
        for (ReceiveMessageCompletableFuture future : futures) {
            assertTrue(future.responseCompletableFuture().isDone());
            totalMessages += future.responseCompletableFuture().get().messages().size();
        }

        // Since each Mocked response we mock 4 responses thus 10*4 = 40 expected messages for 10 futures
        assertEquals(40, totalMessages);
    }

    @Test
    public void receiveMessageErrorHandlingForSimpleError() throws Exception {
        ReceiveQueueBuffer receiveQueueBuffer = receiveQueueBuffer();

        // Mock response with exception
        CompletableFuture<ReceiveMessageResponse> futureResponse = new CompletableFuture<>();
        futureResponse.completeExceptionally(new RuntimeException("SQS error"));
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(futureResponse);

        // Create future and call receiveMessage
        ReceiveMessageCompletableFuture future = new ReceiveMessageCompletableFuture(10, Duration.ofSeconds(1));
        receiveQueueBuffer.receiveMessage(future);

        // Use assertThrows to expect an ExecutionException
        ExecutionException thrown = assertThrows(ExecutionException.class, () -> {
            future.responseCompletableFuture().get(1, TimeUnit.SECONDS);
        });

        // Assert the cause and cause message
        assertTrue(thrown.getCause() instanceof RuntimeException);
        assertEquals("SQS error", thrown.getCause().getMessage());

        // Verify that the future is completed exceptionally
        assertTrue(future.responseCompletableFuture().isCompletedExceptionally());
    }


    @Test
    public void receiveMessageErrorHandlingWhenErrorFollowSuccess() throws Exception {
        ReceiveQueueBuffer receiveQueueBuffer = receiveQueueBuffer();

        // Mock responses
        CompletableFuture<ReceiveMessageResponse> errorResponse = new CompletableFuture<>();
        errorResponse.completeExceptionally(new RuntimeException("SQS error"));
        CompletableFuture<ReceiveMessageResponse> successResponse = CompletableFuture.completedFuture(generateMessageResponse(3));

        // Mock the SqsAsyncClient to return responses in the specified order
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
            .thenReturn(errorResponse)
            .thenReturn(successResponse)
            .thenReturn(errorResponse);

        // Create futures and call receiveMessage
        ReceiveMessageCompletableFuture erredFuture = new ReceiveMessageCompletableFuture(10, Duration.ofSeconds(1));
        ReceiveMessageCompletableFuture successFuture = new ReceiveMessageCompletableFuture(10, Duration.ofSeconds(1));
        ReceiveMessageCompletableFuture erredTwoFuture = new ReceiveMessageCompletableFuture(10, Duration.ofSeconds(1));

        receiveQueueBuffer.receiveMessage(erredFuture);
        receiveQueueBuffer.receiveMessage(successFuture);
        receiveQueueBuffer.receiveMessage(erredTwoFuture);

        // Use assertThrows to expect an ExecutionException for the first error response
        ExecutionException thrown = assertThrows(ExecutionException.class, () -> {
            erredFuture.responseCompletableFuture().get(1, TimeUnit.SECONDS);
        });

        // Assert the cause and cause message
        assertTrue(thrown.getCause() instanceof RuntimeException);
        assertEquals("SQS error", thrown.getCause().getMessage());

        // Verify that the future is completed exceptionally
        assertTrue(erredFuture.responseCompletableFuture().isCompletedExceptionally());

        // Verify the successful future
        ReceiveMessageResponse successMessages = successFuture.responseCompletableFuture().get(1, TimeUnit.SECONDS);
        assertEquals(3, successMessages.messages().size());
        assertTrue(successFuture.responseCompletableFuture().isDone());

        // Use assertThrows to expect an ExecutionException for the second error response
        ExecutionException thrownSecond = assertThrows(ExecutionException.class, () -> {
            erredTwoFuture.responseCompletableFuture().get(1, TimeUnit.SECONDS);
        });

        // Assert the cause and cause message for the second error
        assertTrue(thrownSecond.getCause() instanceof RuntimeException);
        assertEquals("SQS error", thrownSecond.getCause().getMessage());

        // Verify that the second error future is completed exceptionally
        assertTrue(erredTwoFuture.responseCompletableFuture().isCompletedExceptionally());
    }

    @Test
    public void testShutdownExceptionallyCompletesAllIncompleteFutures() throws Exception {
        // Initialize ReceiveQueueBuffer with required configuration
        ReceiveQueueBuffer receiveQueueBuffer = receiveQueueBuffer();

        // Mock SQS response and visibility timeout configuration
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(generateMessageResponse(10)))
            .thenReturn(new CompletableFuture<>()); // Incomplete future for later use


        // Create and complete a successful ReceiveMessageCompletableFuture
        ReceiveMessageCompletableFuture successFuture = new ReceiveMessageCompletableFuture(10, Duration.ofHours(1));
        receiveQueueBuffer.receiveMessage(successFuture);
        successFuture.responseCompletableFuture().get(3, TimeUnit.SECONDS);

        // Create multiple ReceiveMessageCompletableFuture instances
        List<ReceiveMessageCompletableFuture> futures = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ReceiveMessageCompletableFuture future = new ReceiveMessageCompletableFuture(10, Duration.ofHours(1));
            futures.add(future);
            receiveQueueBuffer.receiveMessage(future);
        }

        // Shutdown the queue buffer and assert no exceptions are thrown
        assertDoesNotThrow(() -> receiveQueueBuffer.shutdown());

        // Verify that each future completes exceptionally with CancellationException
        for (ReceiveMessageCompletableFuture future : futures) {
            CancellationException thrown = assertThrows(CancellationException.class, () -> {
                future.responseCompletableFuture().get(1, TimeUnit.SECONDS);
            });
            assertEquals("Shutdown in progress", thrown.getMessage());
        }
    }


    @Test
    public void visibilityTimeOutErrorsAreLogged() throws Exception {
        // Initialize ReceiveQueueBuffer with required configuration
        ReceiveQueueBuffer receiveQueueBuffer = receiveQueueBuffer();

        // Mock SQS response and visibility timeout configuration
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(generateMessageResponse(10)));


        // Mock changeMessageVisibilityBatch to throw an exception
        CompletableFuture<ChangeMessageVisibilityBatchResponse> futureResponse = new CompletableFuture<>();
        futureResponse.completeExceptionally(SqsException.builder().message("SQS error").build());
        when(sqsClient.changeMessageVisibilityBatch(any(ChangeMessageVisibilityBatchRequest.class)))
            .thenReturn(futureResponse);

        // Create and complete a successful ReceiveMessageCompletableFuture
        ReceiveMessageCompletableFuture successFuture = new ReceiveMessageCompletableFuture(2, Duration.ofSeconds(20));
        receiveQueueBuffer.receiveMessage(successFuture);
        // Making sure response is recieved so that we have unpicked message for which visibility time needs to be updated
        Thread.sleep(1000);
        try (
            LogCaptor logCaptor = LogCaptor.create(Level.DEBUG)) {
            assertDoesNotThrow(() -> receiveQueueBuffer.shutdown());
            assertThat(logCaptor.loggedEvents()).anySatisfy(logEvent -> {
                assertThat(logEvent.getLevel()).isEqualTo(Level.ERROR);
                assertThat(logEvent.getMessage().getFormattedMessage())
                    .contains("Could not change visibility for queue queueUrl");
            });
        }

    }


    private ReceiveMessageResponse generateMessageResponse(int count) {
        List<Message> messages = IntStream.range(0, count).mapToObj(i -> Message.builder().body("Message " + i).receiptHandle(
            "handle" + i).build()).collect(Collectors.toList());
        return ReceiveMessageResponse.builder().messages(messages).build();
    }


}
