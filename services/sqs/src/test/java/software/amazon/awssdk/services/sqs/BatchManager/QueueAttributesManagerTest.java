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

package software.amazon.awssdk.services.sqs.BatchManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.internal.batchmanager.QueueAttributesManager;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

class QueueAttributesManagerTest {

    private final String queueUrl = "some-queue-url";
    @Mock
    private SqsAsyncClient sqsClient;
    @InjectMocks
    private QueueAttributesManager visibilityTimeoutManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        visibilityTimeoutManager = new QueueAttributesManager(sqsClient, queueUrl, Duration.ofMillis(500));
    }

    @Test
    void getReceiveMessageTimeout_validResponse() throws Exception {
        Map<QueueAttributeName, String> attributes = new HashMap<>();
        attributes.put(QueueAttributeName.RECEIVE_MESSAGE_WAIT_TIME_SECONDS, "10");

        GetQueueAttributesResponse response = GetQueueAttributesResponse.builder()
                                                                        .attributes(attributes)
                                                                        .build();

        when(sqsClient.getQueueAttributes(any(GetQueueAttributesRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(response));

        ReceiveMessageRequest request = ReceiveMessageRequest.builder().build();
        CompletableFuture<Duration> timeoutFuture = visibilityTimeoutManager.getReceiveMessageTimeout(request);
        Duration timeout = timeoutFuture.get(1, TimeUnit.SECONDS);

        assertEquals(Duration.ofSeconds(10), timeout);  // 10 seconds in milliseconds
        verify(sqsClient, times(1)).getQueueAttributes(any(GetQueueAttributesRequest.class));
    }

    @Test
    void getReceiveMessageTimeout_usesDefaultValue_when_WaitTimeInServiceIsLow() throws Exception {
        Map<QueueAttributeName, String> attributes = new HashMap<>();
        attributes.put(QueueAttributeName.RECEIVE_MESSAGE_WAIT_TIME_SECONDS, "0");

        GetQueueAttributesResponse response = GetQueueAttributesResponse.builder()
                                                                        .attributes(attributes)
                                                                        .build();

        when(sqsClient.getQueueAttributes(any(GetQueueAttributesRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(response));

        ReceiveMessageRequest request = ReceiveMessageRequest.builder().build();
        CompletableFuture<Duration> timeoutFuture = visibilityTimeoutManager.getReceiveMessageTimeout(request);
        Duration timeout = timeoutFuture.get(1, TimeUnit.SECONDS);

        assertEquals(Duration.ofMillis(500), timeout);  // 10 seconds in milliseconds
        verify(sqsClient, times(1)).getQueueAttributes(any(GetQueueAttributesRequest.class));
    }

    @Test
    void getReceiveMessageTimeout_invalidResponse() {
        GetQueueAttributesResponse response = GetQueueAttributesResponse.builder()
                                                                        .attributes(new HashMap<>())
                                                                        .build();

        when(sqsClient.getQueueAttributes(any(GetQueueAttributesRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(response));

        ReceiveMessageRequest request = ReceiveMessageRequest.builder().build();
        CompletableFuture<Duration> timeoutFuture = visibilityTimeoutManager.getReceiveMessageTimeout(request);

        ExecutionException exception = assertThrows(ExecutionException.class, () -> timeoutFuture.get(1, TimeUnit.SECONDS));
        assertTrue(exception.getCause() instanceof NullPointerException);
        assertEquals("ReceiveMessageWaitTimeSeconds attribute is null in sqs.", exception.getCause().getMessage());

        verify(sqsClient, times(1)).getQueueAttributes(any(GetQueueAttributesRequest.class));
    }

    @Test
    void getReceiveMessageTimeout_exception(){
        CompletableFuture<GetQueueAttributesResponse> exceptionFuture = new CompletableFuture<>();
        exceptionFuture.completeExceptionally(QueueDoesNotExistException.builder().message("queue not exist").build());

        when(sqsClient.getQueueAttributes(any(GetQueueAttributesRequest.class)))
            .thenReturn(exceptionFuture);

        ReceiveMessageRequest request = ReceiveMessageRequest.builder().build();
        CompletableFuture<Duration> timeoutResult = visibilityTimeoutManager.getReceiveMessageTimeout(request);

        ExecutionException exception = assertThrows(ExecutionException.class, () -> timeoutResult.get(1, TimeUnit.SECONDS));
        assertTrue(exception.getCause() instanceof QueueDoesNotExistException);

        verify(sqsClient, times(1)).getQueueAttributes(any(GetQueueAttributesRequest.class));
    }

    @Test
    void getReceiveMessageTimeout_calledOnce() throws Exception {
        Map<QueueAttributeName, String> attributes = new HashMap<>();
        attributes.put(QueueAttributeName.RECEIVE_MESSAGE_WAIT_TIME_SECONDS, "10");

        GetQueueAttributesResponse response = GetQueueAttributesResponse.builder()
                                                                        .attributes(attributes)
                                                                        .build();

        when(sqsClient.getQueueAttributes(any(GetQueueAttributesRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(response));

        ReceiveMessageRequest request = ReceiveMessageRequest.builder().build();

        visibilityTimeoutManager.getReceiveMessageTimeout(request).get(1, TimeUnit.SECONDS);
        visibilityTimeoutManager.getReceiveMessageTimeout(request).get(1, TimeUnit.SECONDS);

        verify(sqsClient, times(1)).getQueueAttributes(any(GetQueueAttributesRequest.class));
    }

    @Test
    void concurrencyTest() throws Exception {
        Map<QueueAttributeName, String> attributes = new HashMap<>();
        attributes.put(QueueAttributeName.RECEIVE_MESSAGE_WAIT_TIME_SECONDS, "10");

        GetQueueAttributesResponse response = GetQueueAttributesResponse.builder()
                                                                        .attributes(attributes)
                                                                        .build();

        when(sqsClient.getQueueAttributes(any(GetQueueAttributesRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(response));

        ReceiveMessageRequest request = ReceiveMessageRequest.builder().build();
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        List<CompletableFuture<Duration>> futures = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    return visibilityTimeoutManager.getReceiveMessageTimeout(request).get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executorService));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(10, TimeUnit.SECONDS);

        for (CompletableFuture<Duration> future : futures) {
            assertEquals(Duration.ofSeconds(10), future.get());
        }

        verify(sqsClient, times(1)).getQueueAttributes(any(GetQueueAttributesRequest.class));
    }


    @Test
    void getVisibilityTimeout_validResponse() throws Exception {
        Map<QueueAttributeName, String> attributes = new HashMap<>();
        attributes.put(QueueAttributeName.VISIBILITY_TIMEOUT, "30");

        GetQueueAttributesResponse response = GetQueueAttributesResponse.builder()
                                                                        .attributes(attributes)
                                                                        .build();

        when(sqsClient.getQueueAttributes(any(GetQueueAttributesRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(response));

        CompletableFuture<Duration> timeoutFuture = visibilityTimeoutManager.getVisibilityTimeout();
        Duration timeout = timeoutFuture.get(1, TimeUnit.SECONDS);

        assertEquals(Duration.ofSeconds(30), timeout);
        verify(sqsClient, times(1)).getQueueAttributes(any(GetQueueAttributesRequest.class));
    }


    @Test
    void getVisibilityTimeout_validResponse_withLowValueUsesThatLowValue() throws Exception {
        Map<QueueAttributeName, String> attributes = new HashMap<>();
        attributes.put(QueueAttributeName.VISIBILITY_TIMEOUT, "0");

        GetQueueAttributesResponse response = GetQueueAttributesResponse.builder()
                                                                        .attributes(attributes)
                                                                        .build();

        when(sqsClient.getQueueAttributes(any(GetQueueAttributesRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(response));

        CompletableFuture<Duration> timeoutFuture = visibilityTimeoutManager.getVisibilityTimeout();
        Duration timeout = timeoutFuture.get(1, TimeUnit.SECONDS);

        assertEquals(Duration.ofSeconds(0), timeout);
        verify(sqsClient, times(1)).getQueueAttributes(any(GetQueueAttributesRequest.class));
    }

    @Test
    void getVisibilityTimeout_invalidResponse() throws Exception {
        GetQueueAttributesResponse response = GetQueueAttributesResponse.builder()
                                                                        .attributes(new HashMap<>())
                                                                        .build();

        when(sqsClient.getQueueAttributes(any(GetQueueAttributesRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(response));

        CompletableFuture<Duration> timeoutFuture = visibilityTimeoutManager.getVisibilityTimeout();

        ExecutionException exception = assertThrows(ExecutionException.class, () -> timeoutFuture.get(1, TimeUnit.SECONDS));
        assertTrue(exception.getCause() instanceof NullPointerException);
        assertEquals("VisibilityTimeout attribute is null in sqs.", exception.getCause().getMessage());
        // Expecting NullPointerException because the
        // attribute is missing

        verify(sqsClient, times(1)).getQueueAttributes(any(GetQueueAttributesRequest.class));
    }


    @Test
    void getVisibilityTimeout_exception() throws Exception {
        CompletableFuture<GetQueueAttributesResponse> exceptionFuture = new CompletableFuture<>();
        exceptionFuture.completeExceptionally(QueueDoesNotExistException.builder().message("queue not exist").build());

        when(sqsClient.getQueueAttributes(any(GetQueueAttributesRequest.class)))
            .thenReturn(exceptionFuture);

        CompletableFuture<Duration> timeoutResult = visibilityTimeoutManager.getVisibilityTimeout();

        ExecutionException exception = assertThrows(ExecutionException.class, () -> timeoutResult.get(1, TimeUnit.SECONDS));
        assertTrue(exception.getCause() instanceof QueueDoesNotExistException);

        verify(sqsClient, times(1)).getQueueAttributes(any(GetQueueAttributesRequest.class));
    }

    @Test
    void getVisibilityTimeout_calledOnce() throws Exception {
        Map<QueueAttributeName, String> attributes = new HashMap<>();
        attributes.put(QueueAttributeName.VISIBILITY_TIMEOUT, "30");

        GetQueueAttributesResponse response = GetQueueAttributesResponse.builder()
                                                                        .attributes(attributes)
                                                                        .build();

        when(sqsClient.getQueueAttributes(any(GetQueueAttributesRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(response));

        visibilityTimeoutManager.getVisibilityTimeout().get(1, TimeUnit.SECONDS);
        visibilityTimeoutManager.getVisibilityTimeout().get(1, TimeUnit.SECONDS);

        verify(sqsClient, times(1)).getQueueAttributes(any(GetQueueAttributesRequest.class));
    }

    @Test
    void concurrency_getVisibilityTimeout() throws Exception {
        Map<QueueAttributeName, String> attributes = new HashMap<>();
        attributes.put(QueueAttributeName.VISIBILITY_TIMEOUT, "30");

        GetQueueAttributesResponse response = GetQueueAttributesResponse.builder()
                                                                        .attributes(attributes)
                                                                        .build();

        when(sqsClient.getQueueAttributes(any(GetQueueAttributesRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(response));

        ExecutorService executorService = Executors.newFixedThreadPool(10);

        List<CompletableFuture<Duration>> futures = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    return visibilityTimeoutManager.getVisibilityTimeout().get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executorService));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(10, TimeUnit.SECONDS);

        for (CompletableFuture<Duration> future : futures) {
            assertEquals(Duration.ofSeconds(30), future.get());
        }

        verify(sqsClient, times(1)).getQueueAttributes(any(GetQueueAttributesRequest.class));
    }
}
