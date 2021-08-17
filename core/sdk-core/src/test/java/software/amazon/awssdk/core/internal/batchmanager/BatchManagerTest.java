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

package software.amazon.awssdk.core.internal.batchmanager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.core.batchmanager.BatchAndSend;
import software.amazon.awssdk.core.batchmanager.BatchKeyMapper;
import software.amazon.awssdk.core.batchmanager.BatchManager;
import software.amazon.awssdk.core.batchmanager.BatchOverrideConfiguration;
import software.amazon.awssdk.core.batchmanager.BatchResponseMapper;
import software.amazon.awssdk.core.batchmanager.IdentifiableMessage;
import software.amazon.awssdk.utils.Either;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;

public class BatchManagerTest {

    private static final int DEFAULT_MAX_BATCH_OPEN = 200;
    private static final Logger log = Logger.loggerFor(BatchManagerTest.class);
    private BatchManager<String, String, BatchResponse> batchManager;
    private ScheduledExecutorService scheduledExecutor;
    private String defaultDestination;

    @Before
    public void setUp() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().threadNamePrefix("batch-buffer").build();
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);
        BatchOverrideConfiguration overrideConfiguration = BatchOverrideConfiguration.builder()
                                                                                     .maxBatchItems(10)
                                                                                     .maxBatchOpenInMs(Duration.ofMillis(DEFAULT_MAX_BATCH_OPEN))
                                                                                     .build();

        batchManager = BatchManager.builder(String.class, String.class, BatchResponse.class)
                                   .overrideConfiguration(overrideConfiguration)
                                   .scheduledExecutor(scheduledExecutor)
                                   .batchFunction(batchFunction)
                                   .responseMapper(responseMapper)
                                   .batchKeyMapper(batchKeyMapper)
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

        assertThat(Duration.ofNanos(endTime - startTime).toMillis()).isGreaterThan(DEFAULT_MAX_BATCH_OPEN);
        checkAllResponses(requests, responses);
    }

    @Test
    public void scheduleTwoBatchesToSameDestination() {
        Map<String, String> requests = createRequestsOfSize(10);

        long startTime = System.nanoTime();
        Map<String, CompletableFuture<String>> responses = createAndSendResponses(0, 5, requests);
        waitForTime(DEFAULT_MAX_BATCH_OPEN + 10);
        responses.putAll(createAndSendResponses(5, 5, requests));
        CompletableFuture.allOf(responses.values().toArray(new CompletableFuture[0])).join();
        long endTime = System.nanoTime();

        assertThat(responses).hasSize(10);
        assertThat(Duration.ofNanos(endTime - startTime).toMillis()).isGreaterThan(DEFAULT_MAX_BATCH_OPEN + 100);
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

        assertThat(responses).hasSize(10);
        assertThat(Duration.ofNanos(endTime - startTime).toMillis()).isLessThan(DEFAULT_MAX_BATCH_OPEN * 2);
        checkAllResponses(requests, responses);
    }

    @Test
    public void sendTenMessagesWithEachThreadToSameDestination() {
        int numThreads = 10;
        int numMessages = 10;
        Map<String, String> requests = createRequestsOfSize(numMessages*numThreads);
        ConcurrentHashMap<String, CompletableFuture<String>> responses = new ConcurrentHashMap<>();
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

        List<CompletableFuture<Map<String, CompletableFuture<String>>>> sendRequestFutures =
            createThreadsAndSendMessages(numThreads,  numMessages, requests, responses, executorService);
        checkThreadedResponses(requests, responses, sendRequestFutures);
        executorService.shutdownNow();
    }

    @Test
    public void scheduleFiveMessagesWithEachThreadToDifferentDestinations() {
        int numThreads = 10;
        int numMessages = 5;
        Map<String, String> requests = createRequestsOfSizeToDiffDestinations(numThreads, numMessages);
        ConcurrentHashMap<String, CompletableFuture<String>> responses = new ConcurrentHashMap<>();
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

        List<CompletableFuture<Map<String, CompletableFuture<String>>>> sendRequestFutures =
            createThreadsAndSendMessages(numThreads,  numMessages, requests, responses, executorService);
        checkThreadedResponses(requests, responses, sendRequestFutures);
        executorService.shutdownNow();
    }

    @Test
    public void periodicallySendMessagesWithTenThreadsToSameDestination() {
        int numThreads = 10;
        int numMessages = 10;
        Map<String, String> requests = createRequestsOfSize(numMessages*numThreads);
        ConcurrentHashMap<String, CompletableFuture<String>> responses = new ConcurrentHashMap<>();
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        List<CompletableFuture<Map<String, CompletableFuture<String>>>> executions = new ArrayList<>();
        for (int i = 0; i < numThreads; i++) {
            executions.add(sendRequestToDestinationWithDelay(i*numMessages, numMessages, requests, responses, executorService));
            waitForTime(75);
        }
        checkThreadedResponses(requests, responses, executions);
        executorService.shutdownNow();
    }

    @Test
    public void sentRequestsAllReturnExceptions(){
        BatchOverrideConfiguration overrideConfiguration = BatchOverrideConfiguration.builder()
                                                                                     .maxBatchItems(10)
                                                                                     .maxBatchOpenInMs(Duration.ofMillis(DEFAULT_MAX_BATCH_OPEN))
                                                                                     .build();
        BatchManager<String, String, BatchResponse> testBatchManager = BatchManager.builder(String.class, String.class, BatchResponse.class)
                                                                                   .overrideConfiguration(overrideConfiguration)
                                                                                   .scheduledExecutor(scheduledExecutor)
                                                                                   .batchFunction(exceptionBatchFunction)
                                                                                   .responseMapper(responseMapper)
                                                                                   .batchKeyMapper(batchKeyMapper)
                                                                                   .build();
        Map<String, String> requests = createRequestsOfSize(10);
        Map<String, CompletableFuture<String>> responses = new HashMap<>();
        for (int i = 0; i < requests.size(); i++) {
            String key = Integer.toString(i);
            String request = requests.get(key);
            responses.put(key, testBatchManager.sendRequest(request));
        }

        assertThat(requests).hasSameSizeAs(responses);
        for (int i = 0; i < responses.size(); i++) {
            String key = Integer.toString(i);
            CompletableFuture<String> completableResponse = responses.get(key);
            assertThatThrownBy(completableResponse::join).isInstanceOf(RuntimeException.class);
        }
    }

    @Test
    public void batchKeyLimitExceededReturnsIndexOutOfBoundsException() {
        BatchOverrideConfiguration overrideConfiguration = BatchOverrideConfiguration.builder()
                                                                                     .maxBatchKeys(1)
                                                                                     .build();
        BatchManager<String, String, BatchResponse> testBatchManager = BatchManager.builder(String.class, String.class, BatchResponse.class)
                                                                                   .overrideConfiguration(overrideConfiguration)
                                                                                   .scheduledExecutor(scheduledExecutor)
                                                                                   .batchFunction(batchFunction)
                                                                                   .responseMapper(responseMapper)
                                                                                   .batchKeyMapper(batchKeyMapper)
                                                                                   .build();

        Map<String, String> requests = new HashMap<>();
        requests.put(Integer.toString(0), "dest0 0");
        requests.put(Integer.toString(1), "dest1 0");
        requests.put(Integer.toString(2), "dest0 1");

        Map<String, CompletableFuture<String>> responses = new HashMap<>();
        for (int i = 0; i < requests.size(); i++) {
            String key = Integer.toString(i);
            String request = requests.get(key);
            responses.put(key, testBatchManager.sendRequest(request));
        }

        assertThat(responses.get("0").join()).isEqualTo(requests.get("0"));
        CompletableFuture<String> completableResponse = responses.get("1");
        assertThatThrownBy(completableResponse::join).hasCauseInstanceOf(IndexOutOfBoundsException.class)
                                                     .hasMessageContaining("MaxBatchKeys reached");
        assertThat(responses.get("2").join()).isEqualTo(requests.get("2"));
    }

    @Test
    public void batchBufferLimitExceededReturnsIndexOutOfBoundsException() {
        BatchOverrideConfiguration overrideConfiguration = BatchOverrideConfiguration.builder()
                                                                                     .maxBufferSize(1)
                                                                                     .build();
        BatchManager<String, String, BatchResponse> testBatchManager = BatchManager.builder(String.class, String.class, BatchResponse.class)
                                                                                   .overrideConfiguration(overrideConfiguration)
                                                                                   .scheduledExecutor(scheduledExecutor)
                                                                                   .batchFunction(batchFunction)
                                                                                   .responseMapper(responseMapper)
                                                                                   .batchKeyMapper(batchKeyMapper)
                                                                                   .build();

        Map<String, String> requests = createRequestsOfSize(2);
        Map<String, CompletableFuture<String>> responses = new HashMap<>();
        for (int i = 0; i < requests.size(); i++) {
            String key = Integer.toString(i);
            String request = requests.get(key);
            responses.put(key, testBatchManager.sendRequest(request));
        }

        assertThat(responses.get("0").join()).isEqualTo(requests.get("0"));
        CompletableFuture<String> completableResponse = responses.get("1");
        assertThatThrownBy(completableResponse::join).hasCauseInstanceOf(IndexOutOfBoundsException.class)
                                                     .hasMessageContaining("MaxBufferSize reached");
    }

    private static final BatchAndSend<String, BatchResponse> exceptionBatchFunction =
        (identifiableRequests, destination) -> CompletableFuture.supplyAsync(() -> {
            waitForTime(DEFAULT_MAX_BATCH_OPEN - 50);
            throw new RuntimeException("Throwing exception in test");
        });

    private static final BatchAndSend<String, BatchResponse> batchFunction =
        (identifiableRequests, destination) -> {
            BatchResponse entries = new BatchResponse();
            identifiableRequests.forEach(identifiableRequest -> {
                String id = identifiableRequest.id();
                String request = identifiableRequest.message();
                entries.add(new MessageWithId(id, request));
            });
            return CompletableFuture.supplyAsync(() -> {
                waitForTime(DEFAULT_MAX_BATCH_OPEN - 50);
                return entries;
            });
        };

    private static final BatchResponseMapper<BatchResponse, String> responseMapper =
        requestBatchResponse -> {
            List<Either<IdentifiableMessage<String>, IdentifiableMessage<Throwable>>> identifiableResponses = new ArrayList<>();
            for (MessageWithId requestWithId : requestBatchResponse.getResponses()) {
                IdentifiableMessage<String> response = new IdentifiableMessage<>(requestWithId.getId(),
                                                                                 requestWithId.getMessage());
                identifiableResponses.add(Either.left(response));
            }
            return identifiableResponses;
        };

    private static final BatchKeyMapper<String> batchKeyMapper = request -> request.substring(0, 5);

    private List<CompletableFuture<Map<String, CompletableFuture<String>>>> createThreadsAndSendMessages(
        int numThreads, int numMessages, Map<String, String> requests,
        ConcurrentHashMap<String, CompletableFuture<String>> responses, ExecutorService executorService) {
        List<CompletableFuture<Map<String, CompletableFuture<String>>>> executions = new ArrayList<>();
        for (int i = 0; i < numThreads; i++) {
            executions.add(sendRequestToDestination(i*numMessages, numMessages, requests, responses, executorService));
        }
        return executions;
    }

    private CompletableFuture<Map<String, CompletableFuture<String>>> sendRequestToDestination(
        int startingId, int numMessages, Map<String, String> requests,
        ConcurrentHashMap<String, CompletableFuture<String>> responses, ExecutorService executorService) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, CompletableFuture<String>> newResponses = createAndSendResponses(startingId, numMessages, requests);
            responses.putAll(newResponses);
            return newResponses;
        }, executorService);
    }


    private CompletableFuture<Map<String, CompletableFuture<String>>> sendRequestToDestinationWithDelay(
        int startingId, int numMessages, Map<String, String> requests,
        ConcurrentHashMap<String, CompletableFuture<String>> responses, ExecutorService executorService) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, CompletableFuture<String>> newResponses = createAndSendResponsesWithDelay(startingId, numMessages, requests);
            responses.putAll(newResponses);
            return newResponses;
        }, executorService);
    }

    private void checkThreadedResponses(Map<String, String> requests,
                                        ConcurrentHashMap<String, CompletableFuture<String>> responses,
                                        List<CompletableFuture<Map<String, CompletableFuture<String>>>> sentRequestsFutures) {
        for (CompletableFuture<Map<String, CompletableFuture<String>>> sentRequest : sentRequestsFutures) {
            try {
                CompletableFuture.allOf(sentRequest.get(300, TimeUnit.MILLISECONDS)
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
        assertThat(responses).hasSameSizeAs(requests);
        for (int i = 0; i < responses.size(); i++) {
            String key = Integer.toString(i);
            assertThat(responses.get(key).join()).isEqualTo(requests.get(key));
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

    private Map<String, CompletableFuture<String>> createAndSendResponsesWithDelay(int startingId, int size,
                                                                                   Map<String, String> requests) {
        Map<String, CompletableFuture<String>> responses = new HashMap<>();
        for (int i = startingId; i < startingId + size; i++) {
            String key = Integer.toString(i);
            String request = requests.get(key);
            responses.put(key, batchManager.sendRequest(request));
            waitForTime(100);
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
