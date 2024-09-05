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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.internal.batchmanager.ReceiveBatchManager;
import software.amazon.awssdk.services.sqs.internal.batchmanager.ResponseBatchConfiguration;

import java.time.Duration;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReceiveBatchManagerTest {

    @Mock
    private SqsAsyncClient sqsClient;

    private ScheduledExecutorService executor;

    private ReceiveBatchManager receiveBatchManager;

    @BeforeEach
    public void setUp() {
        executor = Executors.newScheduledThreadPool(Integer.MAX_VALUE);

        // Mocking queue attributes
        Map<QueueAttributeName, String> attributes = new HashMap<>();
        attributes.put(QueueAttributeName.VISIBILITY_TIMEOUT, "30");
        attributes.put(QueueAttributeName.RECEIVE_MESSAGE_WAIT_TIME_SECONDS, "10");

        GetQueueAttributesResponse response = GetQueueAttributesResponse.builder()
                                                                        .attributes(attributes)
                                                                        .build();

        when(sqsClient.getQueueAttributes(any(GetQueueAttributesRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(response));
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    private ResponseBatchConfiguration createConfig(Duration minReceiveWaitTime) {
        return ResponseBatchConfiguration.builder()
                                         .receiveMessageAttributeNames(Collections.emptyList())
                                         .visibilityTimeout(Duration.ofSeconds(2))
                                         .messageMinWaitDuration(minReceiveWaitTime)
                                         .build();
    }

    private ReceiveMessageResponse generateMessageResponse(int count) {
        List<Message> messages = IntStream.range(0, count).mapToObj(i ->
                                                                        Message.builder().body("Message " + i).receiptHandle("handle" + i).build()
        ).collect(Collectors.toList());
        return ReceiveMessageResponse.builder().messages(messages).build();
    }

    @Test
    void testProcessRequestSuccessful() throws Exception {
        ReceiveMessageResponse response = generateMessageResponse(10);
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(response));

        receiveBatchManager = new ReceiveBatchManager(sqsClient, executor, createConfig( Duration.ofMillis(50)), "queueUrl");

        ReceiveMessageRequest request = ReceiveMessageRequest.builder().maxNumberOfMessages(10).build();
        CompletableFuture<ReceiveMessageResponse> futureResponse = receiveBatchManager.processRequest(request);
        ReceiveMessageResponse receiveMessageResponse = futureResponse.get(2, TimeUnit.SECONDS);

        assertTrue(futureResponse.isDone());
        assertEquals(10, receiveMessageResponse.messages().size());
    }

    @Test
    void testProcessRequestWithCustomMaxMessages() throws Exception {
        ReceiveMessageResponse response = generateMessageResponse(5);
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(response));

        receiveBatchManager = new ReceiveBatchManager(sqsClient, executor, createConfig(Duration.ofMillis(50)), "queueUrl");

        ReceiveMessageRequest request = ReceiveMessageRequest.builder().maxNumberOfMessages(5).build();
        CompletableFuture<ReceiveMessageResponse> futureResponse = receiveBatchManager.processRequest(request);
        ReceiveMessageResponse receiveMessageResponse = futureResponse.get(2, TimeUnit.SECONDS);

        assertTrue(futureResponse.isDone());
        assertEquals(5, receiveMessageResponse.messages().size());
    }

    @Test
    void testProcessRequestErrorHandling() throws Exception {
        CompletableFuture<ReceiveMessageResponse> futureResponse = new CompletableFuture<>();
        futureResponse.completeExceptionally(new RuntimeException("SQS error"));
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(futureResponse);

        receiveBatchManager = new ReceiveBatchManager(sqsClient, executor, createConfig(Duration.ofMillis(50)), "queueUrl");

        ReceiveMessageRequest request = ReceiveMessageRequest.builder().maxNumberOfMessages(10).build();

        CompletableFuture<ReceiveMessageResponse> future = receiveBatchManager.processRequest(request);

        ExecutionException thrown = assertThrows(ExecutionException.class, () -> {
            future.get(1, TimeUnit.SECONDS);
        });

        assertTrue(thrown.getCause() instanceof RuntimeException);
        assertEquals("SQS error", thrown.getCause().getMessage());
        assertTrue(future.isCompletedExceptionally());
    }

    @Test
    void testShutdown() throws Exception {

        receiveBatchManager = new ReceiveBatchManager(sqsClient, executor, createConfig(Duration.ofMillis(50)), "queueUrl");

        ReceiveMessageRequest request = ReceiveMessageRequest.builder().maxNumberOfMessages(10).build();
        CompletableFuture<ReceiveMessageResponse> futureResponse = receiveBatchManager.processRequest(request);

        receiveBatchManager.close();

        assertThrows(IllegalStateException.class, () -> receiveBatchManager.processRequest(request));

        assertTrue(futureResponse.isDone());
    }

    @Test
    void testProcessRequestMultipleMessages() throws Exception {
        ReceiveMessageResponse response = generateMessageResponse(10);
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(response));

        receiveBatchManager = new ReceiveBatchManager(sqsClient, executor, createConfig( Duration.ofMillis(50)), "queueUrl");

        List<ReceiveMessageRequest> requests = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            requests.add(ReceiveMessageRequest.builder().maxNumberOfMessages(10).build());
        }

        List<CompletableFuture<ReceiveMessageResponse>> futures = new ArrayList<>();
        for (ReceiveMessageRequest request : requests) {
            futures.add(receiveBatchManager.processRequest(request));
        }

        for (CompletableFuture<ReceiveMessageResponse> future : futures) {
            ReceiveMessageResponse receiveMessageResponse = future.get(2, TimeUnit.SECONDS);
            assertEquals(10, receiveMessageResponse.messages().size());
        }

        for (CompletableFuture<ReceiveMessageResponse> future : futures) {
            assertTrue(future.isDone());
        }
    }


    @Test
    void testProcessRequestWithQueueAttributes() throws Exception {
        // Prepare configuration with specific message attribute names
        ResponseBatchConfiguration configuration =  ResponseBatchConfiguration.builder()
                                      .receiveMessageAttributeNames(Arrays.asList("AttributeValue7", "AttributeValue9"))
                                      .build();

        // Mock response for receiveMessage
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(generateMessageResponse(10)));

        // Initialize ReceiveBatchManager
        receiveBatchManager = new ReceiveBatchManager(sqsClient, executor, configuration, "queueUrl");

        // Create ReceiveMessageRequest
        ReceiveMessageRequest request = ReceiveMessageRequest.builder().build();

        // Process request
        CompletableFuture<ReceiveMessageResponse> futureResponse = receiveBatchManager.processRequest(request);
        ReceiveMessageResponse receiveMessageResponse = futureResponse.get(2, TimeUnit.SECONDS);

        // Verify results
        assertTrue(futureResponse.isDone());
        assertEquals(10, receiveMessageResponse.messages().size());

        // Capture the argument passed to receiveMessage
        ArgumentCaptor<ReceiveMessageRequest> captor = ArgumentCaptor.forClass(ReceiveMessageRequest.class);
        verify(sqsClient).receiveMessage(captor.capture());
        ReceiveMessageRequest capturedRequest = captor.getValue();

        // Verify the messageAttributeNames in the captured request
        assertTrue(capturedRequest.messageAttributeNames().contains("AttributeValue7"));
        assertTrue(capturedRequest.messageAttributeNames().contains("AttributeValue9"));
    }

}
