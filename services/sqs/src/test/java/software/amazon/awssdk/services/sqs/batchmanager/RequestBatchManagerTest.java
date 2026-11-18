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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.sqs.internal.batchmanager.IdentifiableMessage;
import software.amazon.awssdk.services.sqs.internal.batchmanager.RequestBatchConfiguration;

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

    @Test
    void batchRequest_idleKeys_flushTimersCancelledAfterFlush() throws Exception {
        when(mockClient.sendBatchAsync(any(), any()))
            .thenAnswer(i -> CompletableFuture.completedFuture(batchedResponse(1, "testResponse")));
        CountingScheduler scheduler = new CountingScheduler();
        try {
            SampleBatchManager batchManager = new SampleBatchManager(
                BatchOverrideConfiguration.builder().sendRequestFrequency(Duration.ofMillis(50)).build(),
                scheduler, mockClient);

            List<CompletableFuture<String>> responses = new ArrayList<>();
            for (int i = 0; i < 200; i++) {
                responses.add(batchManager.batchRequest("idleKey" + i + ":" + i));
            }
            CompletableFuture.allOf(responses.toArray(new CompletableFuture[0])).get(10, TimeUnit.SECONDS);
            assertEquals(200, scheduler.scheduledCount());

            assertTrue(awaitNoLivePeriodicTasks(scheduler), "idle flush timers were not cancelled");
        } finally {
            scheduler.shutdownNow();
        }
    }

    @Test
    void batchRequest_maxBatchKeysReached_newKeyAcceptedAfterIdleKeysEvicted() throws Exception {
        int maxBatchKeys = 5;
        when(mockClient.sendBatchAsync(any(), any()))
            .thenAnswer(i -> CompletableFuture.completedFuture(batchedResponse(1, "testResponse")));
        CountingScheduler scheduler = new CountingScheduler();
        try {
            SampleBatchManager batchManager = new SampleBatchManager(
                RequestBatchConfiguration.builder()
                                         .maxBatchKeys(maxBatchKeys)
                                         .sendRequestFrequency(Duration.ofMillis(50))
                                         .build(),
                scheduler, mockClient);

            List<CompletableFuture<String>> responses = new ArrayList<>();
            for (int i = 0; i < maxBatchKeys; i++) {
                responses.add(batchManager.batchRequest("capKey" + i + ":" + i));
            }
            CompletableFuture.allOf(responses.toArray(new CompletableFuture[0])).get(10, TimeUnit.SECONDS);
            assertTrue(awaitNoLivePeriodicTasks(scheduler), "idle flush timers were not cancelled");

            CompletableFuture<String> fresh = batchManager.batchRequest("freshKey:0");
            assertEquals("testResponse0", fresh.get(5, TimeUnit.SECONDS));
        } finally {
            scheduler.shutdownNow();
        }
    }

    @Test
    void batchRequest_concurrentDistinctKeys_neverExceedsMaxBatchKeys() throws Exception {
        int maxBatchKeys = 50;
        int threads = 16;
        int keysPerThread = 100;
        CountingScheduler scheduler = new CountingScheduler();
        try {
            SampleBatchManager batchManager = new SampleBatchManager(
                RequestBatchConfiguration.builder()
                                         .maxBatchKeys(maxBatchKeys)
                                         .sendRequestFrequency(Duration.ofHours(1))
                                         .build(),
                scheduler, mockClient);

            List<CompletableFuture<String>> responses = Collections.synchronizedList(new ArrayList<>());
            CountDownLatch start = new CountDownLatch(1);
            List<Thread> workers = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                int thread = t;
                Thread worker = new Thread(() -> {
                    try {
                        start.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    for (int i = 0; i < keysPerThread; i++) {
                        responses.add(batchManager.batchRequest("concurrentKey" + thread + "_" + i + ":0"));
                    }
                });
                workers.add(worker);
                worker.start();
            }
            start.countDown();
            for (Thread worker : workers) {
                worker.join(TimeUnit.SECONDS.toMillis(30));
            }

            long accepted = responses.stream().filter(r -> !r.isCompletedExceptionally()).count();
            assertEquals(threads * keysPerThread, responses.size());
            assertEquals(maxBatchKeys, accepted);
            assertEquals(maxBatchKeys, scheduler.scheduledCount());
        } finally {
            scheduler.shutdownNow();
        }
    }

    @Test
    void batchRequest_sameKeyAcrossIdlePeriods_allResponsesComplete() throws Exception {
        when(mockClient.sendBatchAsync(any(), any())).thenAnswer(i -> {
            List<IdentifiableMessage<String>> requests = i.getArgument(0);
            return CompletableFuture.completedFuture(new BatchResponse(
                requests.stream()
                        .map(r -> new BatchResponseEntry(r.id(), "testResponse"))
                        .collect(Collectors.toList())));
        });
        CountingScheduler scheduler = new CountingScheduler();
        try {
            SampleBatchManager batchManager = new SampleBatchManager(
                BatchOverrideConfiguration.builder().sendRequestFrequency(Duration.ofMillis(20)).build(),
                scheduler, mockClient);

            for (int i = 0; i < 10; i++) {
                CompletableFuture<String> response = batchManager.batchRequest("repeatKey:" + i);
                assertEquals("testResponse", response.get(5, TimeUnit.SECONDS));
                Thread.sleep(60);
            }
            assertTrue(scheduler.scheduledCount() > 1, "idle key was never evicted between requests");
            assertTrue(awaitNoLivePeriodicTasks(scheduler), "idle flush timers were not cancelled");
        } finally {
            scheduler.shutdownNow();
        }
    }

    @Test
    void batchRequest_flushSchedulingRejected_keySlotReleased() throws Exception {
        int maxBatchKeys = 3;
        when(mockClient.sendBatchAsync(any(), any()))
            .thenAnswer(i -> CompletableFuture.completedFuture(batchedResponse(1, "testResponse")));
        CountingScheduler scheduler = new CountingScheduler(maxBatchKeys);
        try {
            SampleBatchManager batchManager = new SampleBatchManager(
                RequestBatchConfiguration.builder()
                                         .maxBatchKeys(maxBatchKeys)
                                         .sendRequestFrequency(Duration.ofHours(1))
                                         .build(),
                scheduler, mockClient);

            for (int i = 0; i < maxBatchKeys; i++) {
                CompletableFuture<String> rejected = batchManager.batchRequest("rejectedKey" + i + ":0");
                ExecutionException e = assertThrows(ExecutionException.class, () -> rejected.get(1, TimeUnit.SECONDS));
                assertInstanceOf(RejectedExecutionException.class, e.getCause());
            }

            List<CompletableFuture<String>> responses = new ArrayList<>();
            for (int i = 0; i < maxBatchKeys; i++) {
                responses.add(batchManager.batchRequest("acceptedKey" + i + ":0"));
            }
            for (CompletableFuture<String> response : responses) {
                assertFalse(response.isCompletedExceptionally(), "key slot was not released after a rejected schedule");
            }
            assertEquals(maxBatchKeys, scheduler.scheduledCount());
        } finally {
            scheduler.shutdownNow();
        }
    }

    private static boolean awaitNoLivePeriodicTasks(CountingScheduler scheduler) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (scheduler.livePeriodicTasks() == 0) {
                return true;
            }
            Thread.sleep(20);
        }
        return scheduler.livePeriodicTasks() == 0;
    }

    /**
     * Records every recurring flush task the batch manager schedules.
     */
    private static final class CountingScheduler extends ScheduledThreadPoolExecutor {
        private final List<ScheduledFuture<?>> periodic = Collections.synchronizedList(new ArrayList<>());

        private final AtomicInteger rejectionsLeft;

        CountingScheduler() {
            this(0);
        }

        /**
         * @param rejections how many of the first scheduleAtFixedRate calls throw RejectedExecutionException
         */
        CountingScheduler(int rejections) {
            super(2);
            this.rejectionsLeft = new AtomicInteger(rejections);
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
            if (rejectionsLeft.getAndUpdate(n -> n > 0 ? n - 1 : 0) > 0) {
                throw new RejectedExecutionException("rejected for test");
            }
            ScheduledFuture<?> future = super.scheduleAtFixedRate(command, initialDelay, period, unit);
            periodic.add(future);
            return future;
        }

        int scheduledCount() {
            return periodic.size();
        }

        int livePeriodicTasks() {
            synchronized (periodic) {
                return (int) periodic.stream().filter(f -> !f.isCancelled() && !f.isDone()).count();
            }
        }
    }

    @AfterAll
    public static void teardown() throws IOException {
        if (scheduledExecutor != null) {
            scheduledExecutor.shutdownNow();
        }
    }

}