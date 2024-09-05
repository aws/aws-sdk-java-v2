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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.internal.batchmanager.QueueAttributesManager;
import software.amazon.awssdk.services.sqs.internal.batchmanager.ReceiveQueueBuffer;
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
class ReceiveQueueBufferTest {

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
        queueAttributesManager = new QueueAttributesManager(sqsClient, "queueUrl");
    }

    @AfterEach
    public void clear() {


        executor.shutdownNow();

    }

    @Test
    void testReceiveMessageSuccessful() throws Exception {
        // Mock response
        ReceiveMessageResponse response = generateMessageResponse(10);
        ReceiveQueueBuffer receiveQueueBuffer = receiveQueueBuffer(ResponseBatchConfiguration.builder().build());
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(CompletableFuture.completedFuture(response));

        // Create future and call receiveMessage
        CompletableFuture<ReceiveMessageResponse> receiveMessageFuture = new CompletableFuture<>();
        receiveQueueBuffer.receiveMessage(receiveMessageFuture, 10);

        // Wait for the result and verify future completion
        ReceiveMessageResponse receiveMessageResponse = receiveMessageFuture.get(2, TimeUnit.SECONDS);
        assertTrue(receiveMessageFuture.isDone());
        assertEquals(10, receiveMessageResponse.messages().size());
    }

    private ReceiveQueueBuffer receiveQueueBuffer(ResponseBatchConfiguration configuration) {
        return ReceiveQueueBuffer.builder()
                                 .executor(executor)
                                 .sqsClient(sqsClient)
                                 .config(configuration)
                                 .queueUrl("queueUrl")
                                 .queueAttributesManager(queueAttributesManager)
                                 .build();
    }

    @Test
    void testReceiveMessageSuccessful_customRequestSize() throws Exception {
        // Mock response
        ReceiveMessageResponse response = generateMessageResponse(10);
        ReceiveQueueBuffer receiveQueueBuffer = receiveQueueBuffer(ResponseBatchConfiguration.builder().build());
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(CompletableFuture.completedFuture(response));

        // Create future and call receiveMessage
        CompletableFuture<ReceiveMessageResponse> receiveMessageFuture = new CompletableFuture<>();
        receiveQueueBuffer.receiveMessage(receiveMessageFuture, 2);

        // Wait for the result and verify future completion
        ReceiveMessageResponse receiveMessageResponse = receiveMessageFuture.get(2, TimeUnit.SECONDS);
        assertTrue(receiveMessageFuture.isDone());
        assertEquals(2, receiveMessageResponse.messages().size());
    }

    @Test
    void multipleReceiveMessagesWithDifferentBatchSizes() throws Exception {
        ReceiveQueueBuffer receiveQueueBuffer = receiveQueueBuffer(ResponseBatchConfiguration.builder().build());
        // Mock response
        CompletableFuture<ReceiveMessageResponse> delayedResponse = new CompletableFuture<>();
        executor.schedule(() -> delayedResponse.complete(generateMessageResponse(5)), 500, TimeUnit.MILLISECONDS);

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(generateMessageResponse(10)))
            .thenReturn(delayedResponse);

        // Create futures and call receiveMessage
        CompletableFuture<ReceiveMessageResponse> future1 = new CompletableFuture<>();
        CompletableFuture<ReceiveMessageResponse> future2 = new CompletableFuture<>();
        receiveQueueBuffer.receiveMessage(future1, 10);
        receiveQueueBuffer.receiveMessage(future2, 5);

        ReceiveMessageResponse receiveMessageResponse1 = future1.get(1, TimeUnit.SECONDS);
        assertEquals(10, receiveMessageResponse1.messages().size());

        ReceiveMessageResponse receiveMessageResponse2 = future2.get(1, TimeUnit.SECONDS);
        assertEquals(5, receiveMessageResponse2.messages().size());

        // Verify future completions
        assertTrue(future2.isDone());
        assertTrue(future1.isDone());
    }


    @Test
    void numberOfBatchesSpawned() throws Exception {
        // Mock response
        ReceiveMessageResponse response = generateMessageResponse(10);
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(CompletableFuture.completedFuture(response));

        // Create futures and call receiveMessage
        ReceiveQueueBuffer receiveQueueBuffer = receiveQueueBuffer(ResponseBatchConfiguration.builder().build());
        CompletableFuture<ReceiveMessageResponse> future1 = new CompletableFuture<>();
        CompletableFuture<ReceiveMessageResponse> future2 = new CompletableFuture<>();
        receiveQueueBuffer.receiveMessage(future1, 10);
        receiveQueueBuffer.receiveMessage(future2, 10);

        future1.get(1, TimeUnit.SECONDS);
        future2.get(1, TimeUnit.SECONDS);

        // Small sleep just to make sure any scheduled task completes
        Thread.sleep(300);

        // Verify that two batches were spawned
        verify(sqsClient, atLeast(2)).receiveMessage(any(ReceiveMessageRequest.class));
    }

    @Test
    void testReceiveMessageWithAdaptivePrefetchingOfReceieveMessageApiCalls() throws Exception {
        // Mock response
        ReceiveMessageResponse response = generateMessageResponse(10);
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(response));

        // Create receiveQueueBuffer with adaptive prefetching
        ReceiveQueueBuffer receiveQueueBuffer =
            receiveQueueBuffer(ResponseBatchConfiguration.builder().build());


        // Create and send multiple futures using a loop
        List<CompletableFuture<ReceiveMessageResponse>> futures = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            CompletableFuture<ReceiveMessageResponse> future = new CompletableFuture<>();
            futures.add(future);
            receiveQueueBuffer.receiveMessage(future, 1);
            Thread.sleep(10);
        }

        // Join all futures to ensure they complete
        for (CompletableFuture<ReceiveMessageResponse> future : futures) {
            future.get(2, TimeUnit.SECONDS);
        }

        // Verify that the receiveMessage method was called the expected number of times
        verify(sqsClient, atMost(30)).receiveMessage(any(ReceiveMessageRequest.class));
        verify(sqsClient, atLeast(3)).receiveMessage(any(ReceiveMessageRequest.class));
    }


    @Test
    void testReceiveMessageWithAdaptivePrefetchingForASingleCall() throws Exception {

        ReceiveMessageResponse response = generateMessageResponse(10);
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(response));

        // Create receiveQueueBuffer with adaptive prefetching
        ReceiveQueueBuffer receiveQueueBuffer = ReceiveQueueBuffer.builder()
                                                                  .executor(executor)
                                                                  .sqsClient(sqsClient)
                                                                  .config(ResponseBatchConfiguration.builder().build())
                                                                  .queueUrl("queueUrl")
                                                                  .queueAttributesManager(queueAttributesManager)
                                                                  .build();

        CompletableFuture<ReceiveMessageResponse> future = new CompletableFuture<>();
        receiveQueueBuffer.receiveMessage(future, 10);

        ReceiveMessageResponse receiveMessageResponse = future.get(1, TimeUnit.SECONDS);
        assertThat(receiveMessageResponse.messages()).hasSize(10);
        Thread.sleep(500);

        verify(sqsClient, times(1)).receiveMessage(any(ReceiveMessageRequest.class));
    }

    @Test
    void receiveMessageShutDown()  {
        ReceiveQueueBuffer receiveQueueBuffer = receiveQueueBuffer(ResponseBatchConfiguration.builder().build());

        // Create future and call receiveMessage
        CompletableFuture<ReceiveMessageResponse> future = new CompletableFuture<>();
        receiveQueueBuffer.receiveMessage(future, 10);

        // Shutdown receiveQueueBuffer
        receiveQueueBuffer.close();

        // Verify that the future is completed exceptionally
        assertTrue(future.isCompletedExceptionally());
    }

    @Test
    void testConcurrentExecutionWithResponses() throws Exception {
        ReceiveQueueBuffer receiveQueueBuffer = receiveQueueBuffer(ResponseBatchConfiguration.builder().build());

        // Mock response
        ReceiveMessageResponse response = generateMessageResponse(4);
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(response));

        // Create futures and call receiveMessage concurrently
        List<CompletableFuture<ReceiveMessageResponse>> futures = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            CompletableFuture<ReceiveMessageResponse> future = new CompletableFuture<>();
            futures.add(future);
            new Thread(() -> receiveQueueBuffer.receiveMessage(future, 5)).start();
        }

        // Wait for all futures to complete
        for (CompletableFuture<ReceiveMessageResponse> future : futures) {
            future.get(2, TimeUnit.SECONDS);
        }

        // Verify all futures completed successfully and collect all messages
        int totalMessages = 0;
        for (CompletableFuture<ReceiveMessageResponse> future : futures) {
            assertTrue(future.isDone());
            totalMessages += future.get().messages().size();
        }

        // Since each mocked response has 4 messages, we expect 10 * 4 = 40 messages for 10 futures
        assertEquals(40, totalMessages);
    }

    @Test
    void receiveMessageErrorHandlingForSimpleError() throws Exception {
        ReceiveQueueBuffer receiveQueueBuffer = receiveQueueBuffer(ResponseBatchConfiguration.builder().build());

        // Mock response with exception
        CompletableFuture<ReceiveMessageResponse> futureResponse = new CompletableFuture<>();
        futureResponse.completeExceptionally(new RuntimeException("SQS error"));
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(futureResponse);

        // Create future and call receiveMessage
        CompletableFuture<ReceiveMessageResponse> future = new CompletableFuture<>();
        receiveQueueBuffer.receiveMessage(future, 10);

        // Use assertThrows to expect an ExecutionException
        ExecutionException thrown = assertThrows(ExecutionException.class, () -> {
            future.get(1, TimeUnit.SECONDS);
        });

        // Assert the cause and cause message
        assertTrue(thrown.getCause() instanceof RuntimeException);
        assertEquals("SQS error", thrown.getCause().getMessage());

        // Verify that the future is completed exceptionally
        assertTrue(future.isCompletedExceptionally());
    }

    @Test
    void receiveMessageErrorHandlingWhenErrorFollowSuccess() throws Exception {
        ReceiveQueueBuffer receiveQueueBuffer = receiveQueueBuffer(ResponseBatchConfiguration.builder().build());

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
        CompletableFuture<ReceiveMessageResponse> erredFuture = new CompletableFuture<>();
        CompletableFuture<ReceiveMessageResponse> successFuture = new CompletableFuture<>();
        CompletableFuture<ReceiveMessageResponse> erredTwoFuture = new CompletableFuture<>();

        receiveQueueBuffer.receiveMessage(erredFuture, 10);
        receiveQueueBuffer.receiveMessage(successFuture, 10);
        receiveQueueBuffer.receiveMessage(erredTwoFuture, 10);

        // Use assertThrows to expect an ExecutionException for the first error response
        ExecutionException thrown = assertThrows(ExecutionException.class, () -> {
            erredFuture.get(1, TimeUnit.SECONDS);
        });

        // Assert the cause and cause message
        assertTrue(thrown.getCause() instanceof RuntimeException);
        assertEquals("SQS error", thrown.getCause().getMessage());

        // Verify that the future is completed exceptionally
        assertTrue(erredFuture.isCompletedExceptionally());

        // Verify the successful future
        ReceiveMessageResponse successMessages = successFuture.get(1, TimeUnit.SECONDS);
        assertEquals(3, successMessages.messages().size());
        assertTrue(successFuture.isDone());

        // Use assertThrows to expect an ExecutionException for the second error response
        ExecutionException thrownSecond = assertThrows(ExecutionException.class, () -> {
            erredTwoFuture.get(1, TimeUnit.SECONDS);
        });

        // Assert the cause and cause message for the second error
        assertTrue(thrownSecond.getCause() instanceof RuntimeException);
        assertEquals("SQS error", thrownSecond.getCause().getMessage());

        // Verify that the second error future is completed exceptionally
        assertTrue(erredTwoFuture.isCompletedExceptionally());
    }


    @Test
    void testShutdownExceptionallyCompletesAllIncompleteFutures() throws Exception {
        // Initialize ReceiveQueueBuffer with required configuration
        ReceiveQueueBuffer receiveQueueBuffer = receiveQueueBuffer(ResponseBatchConfiguration.builder().build());

        // Mock SQS response and visibility timeout configuration
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(generateMessageResponse(10)))
            .thenReturn(new CompletableFuture<>()); // Incomplete future for later use

        // Create and complete a successful future
        CompletableFuture<ReceiveMessageResponse> successFuture = new CompletableFuture<>();
        receiveQueueBuffer.receiveMessage(successFuture, 10);
        successFuture.get(3, TimeUnit.SECONDS);

        // Create multiple futures
        List<CompletableFuture<ReceiveMessageResponse>> futures = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            CompletableFuture<ReceiveMessageResponse> future = new CompletableFuture<>();
            futures.add(future);
            receiveQueueBuffer.receiveMessage(future, 10);
        }

        // Shutdown the queue buffer and assert no exceptions are thrown
        assertDoesNotThrow(() -> receiveQueueBuffer.close());

        // Verify that each future completes exceptionally with CancellationException
        for (CompletableFuture<ReceiveMessageResponse> future : futures) {
            CancellationException thrown = assertThrows(CancellationException.class, () -> {
                future.get(1, TimeUnit.SECONDS);
            });
            assertEquals("Shutdown in progress", thrown.getMessage());
        }
    }


    @Test
    void visibilityTimeOutErrorsAreLogged() throws Exception {
        // Initialize ReceiveQueueBuffer with required configuration
        ReceiveQueueBuffer receiveQueueBuffer = receiveQueueBuffer(ResponseBatchConfiguration.builder().build());

        // Mock SQS response and visibility timeout configuration
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(generateMessageResponse(10)));

        // Mock changeMessageVisibilityBatch to throw an exception
        CompletableFuture<ChangeMessageVisibilityBatchResponse> futureResponse = new CompletableFuture<>();
        futureResponse.completeExceptionally(SqsException.builder().message("SQS error").build());
        when(sqsClient.changeMessageVisibilityBatch(any(ChangeMessageVisibilityBatchRequest.class)))
            .thenReturn(futureResponse);

        // Create and complete a successful future
        CompletableFuture<ReceiveMessageResponse> successFuture = new CompletableFuture<>();
        receiveQueueBuffer.receiveMessage(successFuture, 2);

        // Making sure the response is received so that we have unpicked messages for which visibility time needs to be updated
        Thread.sleep(1000);

        try (LogCaptor logCaptor = LogCaptor.create(Level.DEBUG)) {
            // Shutdown the receiveQueueBuffer to trigger the visibility timeout errors
            assertDoesNotThrow(receiveQueueBuffer::close);

            // Verify that an error was logged for failing to change visibility timeout
            assertThat(logCaptor.loggedEvents()).anySatisfy(logEvent -> {
                assertThat(logEvent.getLevel()).isEqualTo(Level.WARN);
                assertThat(logEvent.getMessage().getFormattedMessage())
                    .contains("Failed to reset the visibility timeout of unprocessed messages for queueUrl: queueUrl. As a "
                              + "result,"
                              + " these unprocessed messages will remain invisible in the queue for the duration of the "
                              + "visibility"
                              + " timeout (PT30S)");
            });
        }
    }

    private ReceiveMessageResponse generateMessageResponse(int count) {
        List<Message> messages = IntStream.range(0, count)
                                          .mapToObj(i -> Message.builder().body("Message " + i).receiptHandle("handle" + i).build())
                                          .collect(Collectors.toList());
        return ReceiveMessageResponse.builder().messages(messages).build();
    }

}
