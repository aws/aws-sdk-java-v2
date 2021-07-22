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

package software.amazon.awssdk.core.internal.batchutilities;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.testng.Assert;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;

public class BatchBufferTest {

    private static final Logger log = Logger.loggerFor(BatchBufferTest.class);
    private BatchManager<String, String, List<RequestWithId>> buffer;
    private ScheduledExecutorService scheduledExecutor;
    private String destination;

    @Before
    public void setUp() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().threadNamePrefix("batch-buffer").build();
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);
        buffer = new BatchManager<>(10, Duration.ofMillis(200), scheduledExecutor,
                                    batchingFunction, unpackResponseFunction);
        destination = "testDestination";
    }

    @After
    public void tearDown() {
        buffer.close();
        scheduledExecutor.shutdownNow();
    }

    @Test
    public void sendTenRequests() {
        Map<String, String> requests = createRequestsOfSize(10);
        Map<String, CompletableFuture<String>> responses = createAndSendResponses(requests);
        checkAllResponses(requests, responses);
    }

    @Test
    public void sendTwentyRequests() {
        Map<String, String> requests = createRequestsOfSize(20);
        Map<String, CompletableFuture<String>> responses = createAndSendResponses(requests);
        checkAllResponses(requests, responses);
    }

    @Test
    public void scheduleSendFiveRequests() {
        Map<String, String> requests = createRequestsOfSize(5);

        long startTime = System.nanoTime();
        Map<String, CompletableFuture<String>> responses = createAndSendResponses(requests);
        CompletableFuture.allOf(responses.values().toArray(new CompletableFuture[0])).join();
        long endTime = System.nanoTime();

        Assert.assertTrue(Duration.ofNanos(endTime - startTime).toMillis() > 200);
        checkAllResponses(requests, responses);
    }

    @Test
    public void cancelScheduledBatch() {
        Map<String, String> requests = createRequestsOfSize(10);

        long startTime = System.nanoTime();
        Map<String, CompletableFuture<String>> responses = createAndSendResponses(0, 5, requests, destination);
        waitForTime(195);
        responses.putAll(createAndSendResponses(5, 5, requests, destination));
        CompletableFuture.allOf(responses.values().toArray(new CompletableFuture[0])).join();
        long endTime = System.nanoTime();

        Assert.assertTrue(Duration.ofNanos(endTime - startTime).toMillis() < 400);
        checkAllResponses(requests, responses);
    }

    @Test
    public void scheduleTwoBatches() {
        Map<String, String> requests = createRequestsOfSize(10);

        long startTime = System.nanoTime();
        Map<String, CompletableFuture<String>> responses = createAndSendResponses(0, 5, requests, destination);
        waitForTime(250);
        responses.putAll(createAndSendResponses(5, 5, requests, destination));
        CompletableFuture.allOf(responses.values().toArray(new CompletableFuture[0])).join();
        long endTime = System.nanoTime();

        Assert.assertEquals(responses.size(), 10);
        Assert.assertTrue(Duration.ofNanos(endTime - startTime).toMillis() > 400);
        checkAllResponses(requests, responses);
    }

    @Test
    public void sendTenMessagesWithEachThreadToSameDestination() {
        int numThreads = 10;
        Map<String, String> requests = createRequestsOfSize(10*numThreads);
        ConcurrentHashMap<String, CompletableFuture<String>> responses = new ConcurrentHashMap<>();
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        CompletionService<Map<String, CompletableFuture<String>>> completionService =
            new ExecutorCompletionService<>(executorService);

        createThreadsAndSendMessages(numThreads, 10, destination, requests, responses, completionService);
        checkThreadedResponses(numThreads, requests, responses, completionService);
    }

    @Test
    public void scheduleFiveMessagesWithEachThreadToDifferentDestinations() {
        int numThreads = 10;
        Map<String, String> requests = createRequestsOfSize(5*numThreads);
        ConcurrentHashMap<String, CompletableFuture<String>> responses = new ConcurrentHashMap<>();
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        CompletionService<Map<String, CompletableFuture<String>>> completionService =
            new ExecutorCompletionService<>(executorService);

        createThreadsAndSendMessages(numThreads, 5, null, requests, responses, completionService);
        checkThreadedResponses(numThreads, requests, responses, completionService);
    }

    private static final BatchAndSendFunction<String, List<RequestWithId>> batchingFunction =
        (identifiableRequests, destination) -> {
            List<RequestWithId> entries = new ArrayList<>(identifiableRequests.size());
            identifiableRequests.forEach(identifiableRequest -> {
                String id = identifiableRequest.id();
                String request = identifiableRequest.request();
                entries.add(new RequestWithId(id, request));
            });
            return CompletableFuture.supplyAsync(() -> {
                waitForTime(150);
                return entries;
            });
        };

    private static final BatchResponseMapperFunction<List<RequestWithId>, String> unpackResponseFunction =
        requestBatchResponse -> {
            List<IdentifiableResponse<String>> identifiableResponses = new ArrayList<>();
            for (RequestWithId requestWithId : requestBatchResponse) {
                identifiableResponses.add(new IdentifiableResponse<>(requestWithId.getId(), requestWithId.getMessage()));
            }
            return identifiableResponses;
        };

    //Object to mimic a batch request entry
    private static class RequestWithId {

        private final String id;
        private final String message;

        public RequestWithId(String id, String message) {
            this.id = id;
            this.message = message;
        }

        public String getId() {
            return id;
        }

        public String getMessage() {
            return message;
        }
    }

    private void checkThreadedResponses(int numThreads, Map<String, String> requests,
                                        ConcurrentHashMap<String, CompletableFuture<String>> responses,
                                        CompletionService<Map<String, CompletableFuture<String>>> completionService) {
        for (int i = 0; i < numThreads; i++) {
            try {
                CompletableFuture.allOf(completionService.take()
                                                         .get(300, TimeUnit.MILLISECONDS)
                                                         .values()
                                                         .toArray(new CompletableFuture[0]))
                                 .join();
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                log.warn(() -> String.valueOf(e));
            }
        }
        checkAllResponses(requests, responses);
    }

    private void createThreadsAndSendMessages(int numThreads, int numMessages, String destination, Map<String, String> requests,
                                              ConcurrentHashMap<String, CompletableFuture<String>> responses,
                                              CompletionService<Map<String, CompletableFuture<String>>> completionService) {
        String newDestination;
        for (int i = 0; i < numThreads; i++) {
            if (destination == null) {
                newDestination = Integer.toString(i);
            } else {
                newDestination = destination;
            }
            sendRequestToDestination(i*numMessages, numMessages, newDestination, requests, responses, completionService);
        }
    }

    private void sendRequestToDestination(int startingId, int numMessages, String destination, Map<String, String> requests,
                                          ConcurrentHashMap<String, CompletableFuture<String>> responses,
                                          CompletionService<Map<String, CompletableFuture<String>>> completionService) {
        completionService.submit(() -> {
            Map<String, CompletableFuture<String>> newResponses = createAndSendResponses(startingId, numMessages,
                                                                                         requests, destination);
            responses.putAll(newResponses);
            return newResponses;
        });
    }

    private static boolean waitForTime(int msToWait) {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            return countDownLatch.await(msToWait, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void checkAllResponses(Map<String, String> requests, Map<String, CompletableFuture<String>> responses) {
        Assert.assertEquals(responses.size(), requests.size());
        for (int i = 0; i < responses.size(); i++) {
            String key = Integer.toString(i);
            Assert.assertEquals(responses.get(key).join(), requests.get(key));
        }
    }

    private Map<String, String> createRequestsOfSize(int size) {
        Map<String, String> requests = new HashMap<>();
        for (int i = 0; i < size; i++) {
            requests.put(Integer.toString(i), "Message " + (i));
        }
        return requests;
    }

    private Map<String, CompletableFuture<String>> createAndSendResponses(Map<String, String> requests) {
        return createAndSendResponses(requests, destination);
    }

    private Map<String, CompletableFuture<String>> createAndSendResponses(Map<String, String> requests, String destination) {
        return createAndSendResponses(0, requests.size(), requests, destination);
    }

    private Map<String, CompletableFuture<String>> createAndSendResponses(int startingId, int size,
                                                                   Map<String, String> requests, String destination) {
        Map<String, CompletableFuture<String>> responses = new HashMap<>();
        for (int i = startingId; i < startingId + size; i++) {
            String key = Integer.toString(i);
            String request = requests.get(key);
            responses.put(key, buffer.sendRequest(request, destination));
        }
        return responses;
    }
}
