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
    private BatchManager<String, String, BatchResponse> batchManager;
    private ScheduledExecutorService scheduledExecutor;
    private String defaultDestination;

    @Before
    public void setUp() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().threadNamePrefix("batch-buffer").build();
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);
        BatchOverrideConfiguration overrideConfiguration = BatchOverrideConfiguration.builder()
                                                                                     .maxBatchItems(10)
                                                                                     .maxBatchOpenInMs(Duration.ofMillis(200))
                                                                                     .scheduledExecutor(scheduledExecutor)
                                                                                     .build();

        // TODO: read that it is bad practice to write down an explicit type argument like here, but not sure how else I can
        //  pass the types? It is only necessary since I need to provide the types for the functions.
        batchManager = BatchManager.<String, String, BatchResponse> builder()
                             .overrideConfiguration(overrideConfiguration)
                             .batchingFunction(batchingFunction)
                             .mapResponsesFunction(mapResponsesFunction)
                             .batchKeyMapperFunction(getBatchGroupIdFunction)
                             .build();

        defaultDestination = "dest0";
    }

    @After
    public void tearDown() {
        batchManager.close();
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
    public void sendRequestsLessThanMaxBatchItems_shouldSendAfterTimeout() {
        Map<String, String> requests = createRequestsOfSize(5);

        long startTime = System.nanoTime();
        Map<String, CompletableFuture<String>> responses = createAndSendResponses(requests);
        CompletableFuture.allOf(responses.values().toArray(new CompletableFuture[0])).join();
        long endTime = System.nanoTime();

        Assert.assertTrue(Duration.ofNanos(endTime - startTime).toMillis() > 200);
        checkAllResponses(requests, responses);
    }

    @Test
    public void scheduleTwoBatchesToSameDestination() {
        Map<String, String> requests = createRequestsOfSize(10);

        long startTime = System.nanoTime();
        Map<String, CompletableFuture<String>> responses = createAndSendResponses(0, 5, requests);
        waitForTime(210);
        responses.putAll(createAndSendResponses(5, 5, requests));
        CompletableFuture.allOf(responses.values().toArray(new CompletableFuture[0])).join();
        long endTime = System.nanoTime();

        Assert.assertEquals(responses.size(), 10);
        Assert.assertTrue(Duration.ofNanos(endTime - startTime).toMillis() > 300);
        checkAllResponses(requests, responses);
    }

    @Test
    public void scheduleTwoBatchesToDiffDestination() {
        Map<String, String> requests = createRequestsOfSizeToDiffDestinations(2, 5);

        long startTime = System.nanoTime();
        Map<String, CompletableFuture<String>> responses = createAndSendResponses(0, 5, requests);
        responses.putAll(createAndSendResponses(5, 5, requests));
        CompletableFuture.allOf(responses.values().toArray(new CompletableFuture[0])).join();
        long endTime = System.nanoTime();

        Assert.assertEquals(responses.size(), 10);
        Assert.assertTrue(Duration.ofNanos(endTime - startTime).toMillis() < 400);
        checkAllResponses(requests, responses);
    }

    @Test
    public void sendTenMessagesWithEachThreadToSameDestination() {
        int numThreads = 10;
        int numMessages = 10;
        Map<String, String> requests = createRequestsOfSize(numMessages*numThreads);
        ConcurrentHashMap<String, CompletableFuture<String>> responses = new ConcurrentHashMap<>();
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        CompletionService<Map<String, CompletableFuture<String>>> completionService =
            new ExecutorCompletionService<>(executorService);

        createThreadsAndSendMessages(numThreads, numMessages, requests, responses, completionService);
        checkThreadedResponses(numThreads, requests, responses, completionService);
        executorService.shutdownNow();
    }

    @Test
    public void scheduleFiveMessagesWithEachThreadToDifferentDestinations() {
        int numThreads = 10;
        int numMessages = 5;
        Map<String, String> requests = createRequestsOfSizeToDiffDestinations(numThreads, numMessages);
        ConcurrentHashMap<String, CompletableFuture<String>> responses = new ConcurrentHashMap<>();
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        CompletionService<Map<String, CompletableFuture<String>>> completionService =
            new ExecutorCompletionService<>(executorService);

        createThreadsAndSendMessages(numThreads, numMessages, requests, responses, completionService);
        checkThreadedResponses(numThreads, requests, responses, completionService);
        executorService.shutdownNow();
    }

    private static final BatchAndSend<String, BatchResponse> batchingFunction =
        (identifiableRequests, destination) -> {
            BatchResponse entries = new BatchResponse();
            identifiableRequests.forEach(identifiableRequest -> {
                String id = identifiableRequest.id();
                String request = identifiableRequest.request();
                entries.add(new MessageWithId(id, request));
            });
            return CompletableFuture.supplyAsync(() -> {
                waitForTime(150);
                return entries;
            });
        };

    private static final BatchResponseMapper<BatchResponse, String> mapResponsesFunction =
        requestBatchResponse -> {
            List<IdentifiableResponse<String>> identifiableResponses = new ArrayList<>();
            for (MessageWithId requestWithId : requestBatchResponse.getResponses()) {
                identifiableResponses.add(new IdentifiableResponse<>(requestWithId.getId(), requestWithId.getMessage()));
            }
            return identifiableResponses;
        };

    private static final BatchKeyMapper<String> getBatchGroupIdFunction = request -> request.substring(0, 5);

    private void createThreadsAndSendMessages(int numThreads, int numMessages, Map<String, String> requests,
                                              ConcurrentHashMap<String, CompletableFuture<String>> responses,
                                              CompletionService<Map<String, CompletableFuture<String>>> completionService) {
        for (int i = 0; i < numThreads; i++) {
            sendRequestToDestination(i*numMessages, numMessages, requests, responses, completionService);
        }
    }

    private void sendRequestToDestination(int startingId, int numMessages, Map<String, String> requests,
                                          ConcurrentHashMap<String, CompletableFuture<String>> responses,
                                          CompletionService<Map<String, CompletableFuture<String>>> completionService) {
        completionService.submit(() -> {
            Map<String, CompletableFuture<String>> newResponses = createAndSendResponses(startingId, numMessages, requests);
            responses.putAll(newResponses);
            return newResponses;
        });
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
                log.warn(() -> "Error with threaded response: " + e);
            }
        }
        checkAllResponses(requests, responses);
    }

    private static boolean waitForTime(int msToWait) {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            return countDownLatch.await(msToWait, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.warn(() -> String.valueOf(e));
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
            requests.put(Integer.toString(i), defaultDestination + " " + i);
        }
        return requests;
    }

    private Map<String, String> createRequestsOfSizeToDiffDestinations(int numDestinations, int destinationSize) {
        String destPrefix = "dest";
        Map<String, String> requests = new HashMap<>();
        for (int i = 0; i < numDestinations; i++) {
            for (int j = 0; j < destinationSize; j++) {
                String key = Integer.toString(i*destinationSize + j);
                requests.put(key, destPrefix + i + " " + j);
            }
        }
        return requests;
    }

    private Map<String, CompletableFuture<String>> createAndSendResponses(Map<String, String> requests) {
        return createAndSendResponses(0, requests.size(), requests);
    }

    private Map<String, CompletableFuture<String>> createAndSendResponses(int startingId, int size,
                                                                          Map<String, String> requests) {
        Map<String, CompletableFuture<String>> responses = new HashMap<>();
        for (int i = startingId; i < startingId + size; i++) {
            String key = Integer.toString(i);
            String request = requests.get(key);
            responses.put(key, batchManager.sendRequest(request));
        }
        return responses;
    }

    //Object to mimic a both a batch request and batch response entry
    private static class MessageWithId {

        private final String id;
        private final String message;

        public MessageWithId(String id, String message) {
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

    private static class BatchResponse {
        private final List<MessageWithId> responses;

        public BatchResponse() {
            responses = new ArrayList<>();
        }

        public void add(MessageWithId response) {
            responses.add(response);
        }

        public List<MessageWithId> getResponses() {
            return responses;
        }
    }
}
