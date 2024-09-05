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

import java.io.IOException;
import java.time.Duration;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RequestBatchManagerTest {

    @Mock
    private CustomClient mockClient;

    private static ScheduledExecutorService scheduledExecutor;

    public static BatchResponse batchedResponse(int count, String responseMessage) {
        List<BatchResponseEntry> entries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            entries.add(new BatchResponseEntry(String.valueOf(i), responseMessage + i));
        }
        return new BatchResponse(entries);
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        scheduledExecutor = Executors.newScheduledThreadPool(1);
    }



    @Test
    void batchRequest_OnlyOneInBatch_successful() throws Exception {
        String request = "testRequest:1";
        String batchKey = "testRequest";
        CompletableFuture<BatchResponse> batchResponseFuture = CompletableFuture.completedFuture(batchedResponse(1,
                                                                                                                 "testResponse"));

        when(mockClient.sendBatchAsync(any(), eq(batchKey))).thenReturn(batchResponseFuture);

        SampleBatchManager batchManager =
        new SampleBatchManager(BatchOverrideConfiguration.builder().maxBatchSize(1).build(), scheduledExecutor, mockClient);
        CompletableFuture<String> response = batchManager.batchRequest(request);
        assertEquals("testResponse0", response.get(1, TimeUnit.SECONDS));
    }

    @Test
    void batchRequest_TwoBatchesMessagesSplitInTwoCalls_successful() throws Exception {
        String request1 = "testRequest:0";
        String batchKey1 = "testRequest";
        String request2 = "testRequest:1";
        CompletableFuture<BatchResponse> batchResponseFuture = CompletableFuture.completedFuture(batchedResponse(2,
                                                                                                                 "testResponse"));
        when(mockClient.sendBatchAsync(any(), eq(batchKey1))).thenReturn(batchResponseFuture);
        SampleBatchManager batchManager=
            new SampleBatchManager(BatchOverrideConfiguration.builder().maxBatchSize(2)
                                                             .sendRequestFrequency(Duration.ofHours(1)).build(),
                                   scheduledExecutor, mockClient);
        CompletableFuture<String> response1 = batchManager.batchRequest(request1);
        CompletableFuture<String> response2 = batchManager.batchRequest(request2);
        assertEquals("testResponse0", response1.get(1, TimeUnit.SECONDS));
        assertEquals("testResponse1", response2.get(1, TimeUnit.SECONDS));

    }

    @Test
    void batchRequest_TwoBatchesWithDifferentKey_successful() throws Exception {
        String KEY_ONE = "testRequestOne";
        String KEY_TWO = "testRequestTwo";

        CompletableFuture<BatchResponse> batchResponseFutureOne = CompletableFuture.completedFuture(batchedResponse(2, KEY_ONE));
        CompletableFuture<BatchResponse> batchResponseFutureTwo = CompletableFuture.completedFuture(batchedResponse(2, KEY_TWO));

        when(mockClient.sendBatchAsync(any(), eq(KEY_ONE))).thenReturn(batchResponseFutureOne);
        when(mockClient.sendBatchAsync(any(), eq(KEY_TWO))).thenReturn(batchResponseFutureTwo);

        SampleBatchManager batchManager=
            new SampleBatchManager(BatchOverrideConfiguration.builder().maxBatchSize(2).sendRequestFrequency(Duration.ofHours(1)).build(), scheduledExecutor, mockClient);
        CompletableFuture<String> response1 = batchManager.batchRequest(KEY_ONE + ":0");
        CompletableFuture<String> response2 = batchManager.batchRequest(KEY_TWO + ":0");
        CompletableFuture<String> response3 = batchManager.batchRequest(KEY_ONE + ":1");
        CompletableFuture<String> response4 = batchManager.batchRequest(KEY_TWO + ":1");

        assertEquals("testRequestOne0", response1.get(1, TimeUnit.SECONDS));
        assertEquals("testRequestTwo0", response2.get(1, TimeUnit.SECONDS));
        assertEquals("testRequestOne1", response3.get(1, TimeUnit.SECONDS));
        assertEquals("testRequestTwo1", response4.get(1, TimeUnit.SECONDS));
    }

    @Test
    void batchRequest_WithErrorInResponse_throwsException() throws Exception {
        String request = "testRequest:1";
        String batchKey = "testRequest";
        CompletableFuture<BatchResponse> batchResponseFuture = new CompletableFuture<>();
        batchResponseFuture.completeExceptionally(new RuntimeException("testException"));

        when(mockClient.sendBatchAsync(any(), eq(batchKey))).thenReturn(batchResponseFuture);
        SampleBatchManager batchManager=
            new SampleBatchManager(BatchOverrideConfiguration.builder().build(), scheduledExecutor, mockClient);
        CompletableFuture<String> response = batchManager.batchRequest(request);

        assertThrows(ExecutionException.class, () -> response.get(1, TimeUnit.SECONDS));
    }

    @Test
    void batchRequest_WithNetworkError_throwsException() throws Exception {
        String request = "testRequest:1";
        String batchKey = "testRequest";
        CompletableFuture<BatchResponse> batchResponseFuture = new CompletableFuture<>();
        batchResponseFuture.completeExceptionally(new RuntimeException("Network error"));

        when(mockClient.sendBatchAsync(any(), eq(batchKey))).thenReturn(batchResponseFuture);

        SampleBatchManager batchManager=
            new SampleBatchManager(BatchOverrideConfiguration.builder().build(), scheduledExecutor, mockClient);
        CompletableFuture<String> response = batchManager.batchRequest(request);

        assertThrows(ExecutionException.class, () -> response.get(1, TimeUnit.SECONDS));
    }

    @Test
    void close_FlushesAllBatches() throws Exception {
        String request1 = "testRequest:0";
        String batchKey = "testRequest";
        String request2 = "testRequest:1";
        CompletableFuture<BatchResponse> batchResponseFuture = CompletableFuture.completedFuture(batchedResponse(2,
                                                                                                                 "testResponse"));

        when(mockClient.sendBatchAsync(any(), eq(batchKey))).thenReturn(batchResponseFuture);

        SampleBatchManager batchManager=
            new SampleBatchManager(BatchOverrideConfiguration.builder().maxBatchSize(2).sendRequestFrequency(Duration.ofHours(1)).build(), scheduledExecutor, mockClient);

        CompletableFuture<String> response1 = batchManager.batchRequest(request1);
        CompletableFuture<String> response2 = batchManager.batchRequest(request2);
        // Even though the mock returns results immediately, since this is asynchronous execution, the test environment may take
        // additional time due to the Scheduled Executors execution on that machine.
        Thread.sleep(200);
        batchManager.close();

        assertEquals("testResponse0", response1.get(1, TimeUnit.SECONDS));

        assertEquals("testResponse1", response2.get(1, TimeUnit.SECONDS));
    }


    @Test
    void batchRequest_ClosedWhenWaitingForResponse() throws Exception {
        String request = "testRequest:1";
        String batchKey = "testRequest";
        CompletableFuture<BatchResponse> batchResponseFuture = new CompletableFuture<>();

        // Simulate successful response with delay
        scheduledExecutor.schedule(() -> batchResponseFuture.complete(batchedResponse(1, "testResponse")),
                                   10, TimeUnit.HOURS);

        when(mockClient.sendBatchAsync(any(), eq(batchKey))).thenReturn(batchResponseFuture);

        SampleBatchManager batchManager =
            new SampleBatchManager(BatchOverrideConfiguration.builder().maxBatchSize(1).build(), scheduledExecutor, mockClient);
        CompletableFuture<String> response = batchManager.batchRequest(request);

        batchManager.close();
        assertThrows(CancellationException.class, () -> response.join());

    }


    @Test
    void batchRequest_MoreThanBufferSize_Fails() throws Exception {
        final int MAX_QUEUES_THRESHOLD = 10000;

        // Generate unique keys up to MAX_QUEUES_THRESHOLD
        List<String> keys = IntStream.range(0, MAX_QUEUES_THRESHOLD)
                                     .mapToObj(i -> String.format("testRequest%d:%d", i, i))
                                     .collect(Collectors.toList());

        // Create mock responses for all keys
        keys.forEach(key ->
                         when(mockClient.sendBatchAsync(any(), eq(key)))
                             .thenReturn(CompletableFuture.completedFuture(batchedResponse(2, key)))
        );

        SampleBatchManager batchManager = new SampleBatchManager(
            BatchOverrideConfiguration.builder()
                                      .maxBatchSize(2)
                                      .sendRequestFrequency(Duration.ofHours(1))
                                      .build(),
            scheduledExecutor,
            mockClient
        );

        List<CompletableFuture<String>> responses = new ArrayList<>();
        for (String key : keys) {
            responses.add(batchManager.batchRequest(key));
        }

        CompletableFuture<String> extraResponse = batchManager.batchRequest(String.format("testRequest%d:%d", MAX_QUEUES_THRESHOLD, MAX_QUEUES_THRESHOLD));
        ExecutionException exception = assertThrows(ExecutionException.class, () -> extraResponse.get(1, TimeUnit.SECONDS));
        assertEquals(String.format("java.lang.IllegalStateException: Reached MaxBatchKeys of: %d", MAX_QUEUES_THRESHOLD), exception.getCause().toString());
    }

    @AfterAll
    public static void teardown() throws IOException {
        if (scheduledExecutor != null) {
            scheduledExecutor.shutdownNow();
        }
    }

}