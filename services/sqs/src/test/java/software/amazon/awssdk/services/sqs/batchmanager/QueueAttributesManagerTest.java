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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.internal.batchmanager.QueueAttributesManager;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

public class QueueAttributesManagerTest {

    @Mock
    private SqsAsyncClient sqsClient;

    private QueueAttributesManager queueAttributesManager;

    private final String queueUrl = "testQueueUrl";

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        queueAttributesManager = new QueueAttributesManager(sqsClient, queueUrl);
    }

    @Test
    public void testGetReceiveMessageTimeoutWithRequestWaitTime() throws Exception {
        ReceiveMessageRequest request = ReceiveMessageRequest.builder().waitTimeSeconds(15).build();
        Duration configuredWaitTime = Duration.ofSeconds(5);

        CompletableFuture<Duration> result = queueAttributesManager.getReceiveMessageTimeout(request, configuredWaitTime);
        assertEquals(Duration.ofSeconds(15), result.get(3, TimeUnit.SECONDS));
    }

    @Test
    public void testGetReceiveMessageTimeoutFromSQS() throws Exception {
        mockGetQueueAttributesResponse("10", "30");

        ReceiveMessageRequest request = ReceiveMessageRequest.builder().build();
        Duration configuredWaitTime = Duration.ofSeconds(5);

        CompletableFuture<Duration> result = queueAttributesManager.getReceiveMessageTimeout(request, configuredWaitTime);
        assertEquals(Duration.ofSeconds(10), result.get(3, TimeUnit.SECONDS));
    }

    @Test
    public void testGetVisibilityTimeout() throws Exception {
        mockGetQueueAttributesResponse("10", "30");

        CompletableFuture<Duration> result = queueAttributesManager.getVisibilityTimeout();
        assertEquals(Duration.ofSeconds(30), result.get(3, TimeUnit.SECONDS));
    }

    @Test
    public void testConcurrentFetchQueueAttributes() throws Exception {
        mockGetQueueAttributesResponse("10", "30");

        CompletableFuture<Duration> future1 = queueAttributesManager.getReceiveMessageTimeout(
            ReceiveMessageRequest.builder().build(), Duration.ofSeconds(5));
        CompletableFuture<Duration> future2 = queueAttributesManager.getVisibilityTimeout();

        CompletableFuture.allOf(future1, future2).join();

        assertEquals(Duration.ofSeconds(10), future1.get(3, TimeUnit.SECONDS));
        assertEquals(Duration.ofSeconds(30), future2.get(3, TimeUnit.SECONDS));

        // Verify that the SQS client call was only made once
        verify(sqsClient, times(1)).getQueueAttributes(any(GetQueueAttributesRequest.class));
    }

    @Test
    public void testFetchQueueAttributesException() throws Exception {
        CompletableFuture<GetQueueAttributesResponse> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(QueueDoesNotExistException.builder().message("SQS error").build());

        when(sqsClient.getQueueAttributes(any(GetQueueAttributesRequest.class))).thenReturn(failedFuture);

        ReceiveMessageRequest request = ReceiveMessageRequest.builder().build();
        Duration configuredWaitTime = Duration.ofSeconds(5);

        CompletableFuture<Duration> future = queueAttributesManager.getReceiveMessageTimeout(request, configuredWaitTime);


        ExecutionException exception = assertThrows(ExecutionException.class, future::get);
        assertTrue(exception.getCause() instanceof QueueDoesNotExistException);
        assertEquals("SQS error", exception.getCause().getMessage());

        // Verify that the SQS client call was made
        verify(sqsClient, times(1)).getQueueAttributes(any(GetQueueAttributesRequest.class));

        // Next call should try to fetch again
        future = queueAttributesManager.getReceiveMessageTimeout(request, configuredWaitTime);
        exception = assertThrows(ExecutionException.class, future::get);
        assertTrue(exception.getCause() instanceof QueueDoesNotExistException);
        assertEquals("SQS error", exception.getCause().getMessage());

        // Verify that the SQS client call was made again
        verify(sqsClient, times(2)).getQueueAttributes(any(GetQueueAttributesRequest.class));
    }


    private void mockGetQueueAttributesResponse(String receiveMessageWaitTimeSeconds, String visibilityTimeout) {
        Map<QueueAttributeName, String> attributes = new HashMap<>();
        attributes.put(QueueAttributeName.RECEIVE_MESSAGE_WAIT_TIME_SECONDS, receiveMessageWaitTimeSeconds);
        attributes.put(QueueAttributeName.VISIBILITY_TIMEOUT, visibilityTimeout);

        GetQueueAttributesResponse response = GetQueueAttributesResponse.builder()
                                                                        .attributes(attributes)
                                                                        .build();

        when(sqsClient.getQueueAttributes(any(GetQueueAttributesRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(response));
    }
}
