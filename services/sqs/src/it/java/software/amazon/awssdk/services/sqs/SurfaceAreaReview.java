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

package software.amazon.awssdk.services.sqs;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.services.sqs.batchmanager.SqsAsyncBatchManager;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityResponse;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

public class SurfaceAreaReview {


    private static SqsAsyncClient globalSqsAsyncClient = SqsAsyncClient.create();
    private static String QUEUE_URL = null;
    ;



    @Test
    void creatingUsingAsyncClient() throws Exception {
        if (QUEUE_URL == null) {
            createTestQueue();
        }
        ApiCaptureInterceptor apiCaptureInterceptor = new ApiCaptureInterceptor();
        SqsAsyncClient sqsAsyncClient =
            SqsAsyncClient.builder().overrideConfiguration(o -> o.addExecutionInterceptor(apiCaptureInterceptor)).build();


        //[REVIEW AREA  for batchManager from SqsAsyncClient]
        SqsAsyncBatchManager sqsAsyncBatchManager = sqsAsyncClient.batchManager();


        useCaseOfAPIs(sqsAsyncBatchManager);
        apiCaptureInterceptor.callsMadeMap.forEach((key, value) -> System.out.println("Key: " + key + ", Value: " + value));
        sqsAsyncBatchManager.close();
    }

    @Test
    void creatingCustomConfigBatchMangers() throws Exception {
        if (QUEUE_URL == null) {
            createTestQueue();
        }
        ApiCaptureInterceptor apiCaptureInterceptor = new ApiCaptureInterceptor();


        SqsAsyncClient sqsAsyncClient =
            SqsAsyncClient.builder().overrideConfiguration(o -> o.addExecutionInterceptor(apiCaptureInterceptor)).build();

        //[REVIEW AREA  for batchManager from SqsAsyncBatchManager Interface]

        SqsAsyncBatchManager sqsAsyncBatchManager =
            SqsAsyncBatchManager.builder()
                                .client(sqsAsyncClient)
                                .scheduledExecutor(Executors.newScheduledThreadPool(8))
                                .overrideConfiguration(b -> b.minReceiveWaitTime(Duration.ofMillis(300))
                                                             .maxDoneReceiveBatches(2)
                                                             .adaptivePrefetching(true)
                                                             .maxInflightReceiveBatches(2)
                                                             .maxBatchItems(10))
                                .build();


        useCaseOfAPIs(sqsAsyncBatchManager);
        System.out.println("\n apiCaptureInterceptor Results start");
        apiCaptureInterceptor.callsMadeMap.forEach((key, value) -> System.out.println("Key: " + key + ", Value: " + value));
        System.out.println("End \n ");
        sqsAsyncBatchManager.close();
    }

    @Test
    void noProjectInvolvedWhichMeansNoBatching() throws Exception {
        if (QUEUE_URL == null) {
            createTestQueue();
        }
        ApiCaptureInterceptor apiCaptureInterceptor = new ApiCaptureInterceptor();
        SqsAsyncClient sqsAsyncClient =
            SqsAsyncClient.builder().overrideConfiguration(o -> o.addExecutionInterceptor(apiCaptureInterceptor)).build();


        useCaseOfAPIsIfbatchManagerNotUsed(sqsAsyncClient);
        apiCaptureInterceptor.callsMadeMap.forEach((key, value) -> System.out.println("Key: " + key + ", Value: " + value));
        sqsAsyncClient.close();
    }

    private static void createTestQueue() throws Exception {
        CreateQueueResponse surfaceAreaReviewQueue =
            globalSqsAsyncClient.createQueue(r -> r.queueName("surfaceAreaReviewQueue")).get(5, TimeUnit.SECONDS);

        QUEUE_URL = surfaceAreaReviewQueue.queueUrl();

        System.out.println("QUEUE_URL " + QUEUE_URL);


    }

    /**
     * All the use case taken care here
     */
    private static void useCaseOfAPIs(SqsAsyncBatchManager sqsAsyncBatchManager) throws InterruptedException {
        //[REVIEW AREA  for simple sendMessage]
        sqsAsyncBatchManager.sendMessage(s -> s.queueUrl(QUEUE_URL).messageBody("Hi Simple"));
        sqsAsyncBatchManager.sendMessage(s -> s.queueUrl(QUEUE_URL).messageBody("Hello Simple"));


        //[REVIEW AREA  for CALL BACK using when complete]
        List<String> messages = new ArrayList<>();
        List<CompletableFuture<SendMessageResponse>> futures =
            IntStream.rangeClosed(0, 408)
                     .mapToObj(i ->
                                   sqsAsyncBatchManager.sendMessage(s -> s.queueUrl(QUEUE_URL).messageBody("Hi " + i))
                                                       .whenComplete((response, exception) -> {
                                                           if (exception == null) {
                                                               messages.add(response.messageId());
                                                           } else {
                                                               exception.printStackTrace();
                                                           }
                                                       })
                     )
                     .collect(Collectors.toList());


        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        System.out.println("Messages sent " + messages.size());


        System.out.println("Sleeping for some time to give message some time to reflect in Sqs servers");
        Thread.sleep(9000);


        List<CompletableFuture<ReceiveMessageResponse>> recieveFutures = new ArrayList<>();

        AtomicInteger messageCounter = new AtomicInteger();
        AtomicBoolean done = new AtomicBoolean(false);

        final Instant startTime = Instant.now();

        while (true){
            //[REVIEW AREA  for  receiveMessage]
            CompletableFuture<ReceiveMessageResponse> future = sqsAsyncBatchManager.receiveMessage(r -> r.queueUrl(QUEUE_URL)
                                                                                                   .maxNumberOfMessages(10)).whenComplete((r,e) -> {

                if(messageCounter.addAndGet(r.messages().size()) >= 400){
                    done.set(true);
                };
            });
            recieveFutures.add(future);
            Thread.sleep(50); // Sleep for 200ms

            if(done.get()){
                System.out.println("Recieve Done in millis " + (Instant.now().toEpochMilli() - startTime.toEpochMilli()));

                break;
            }

        }
        CompletableFuture<Void> allOf = CompletableFuture.allOf(recieveFutures.toArray(new CompletableFuture[0]));
        allOf.join();

        List<Message> recievedMessages = new ArrayList<>();
        recieveFutures.forEach(future -> future.thenAccept(response -> {
            // response.messages().forEach(message -> System.out.println(message.body()));
            recievedMessages.addAll(response.messages());
        }));


        System.out.println("Total messages recieved " + recievedMessages.size());


        List<CompletableFuture<ChangeMessageVisibilityResponse>> visibilityFutures =
            recievedMessages.stream()
                            .map(message ->
                                     //[REVIEW AREA  for  changeMessageVisibility]
                                     sqsAsyncBatchManager.changeMessageVisibility(c -> c
                                         .queueUrl(QUEUE_URL)
                                         .receiptHandle(message.receiptHandle())
                                         .visibilityTimeout(1)))
                            .collect(Collectors.toList());

        CompletableFuture<Void> changeVisibiltyFutures =
            CompletableFuture.allOf(visibilityFutures.toArray(new CompletableFuture[0]));

        changeVisibiltyFutures.join();

        visibilityFutures.forEach(future -> future.thenAccept(response -> {
        }));

        System.out.println("visibilityFutures changes for " + visibilityFutures.size());


        List<CompletableFuture<DeleteMessageResponse>> deleteMessageFutures = recievedMessages.stream()
                                                                                              .map(message ->
                                                                                                       //[REVIEW AREA  for
                                                                                                       // deleteMessage]
                                                                                                       sqsAsyncBatchManager.deleteMessage(c -> c.queueUrl(QUEUE_URL).receiptHandle(message.receiptHandle()))
                                                                                              )
                                                                                              .collect(Collectors.toList());

        CompletableFuture<Void> deleteFutures = CompletableFuture.allOf(deleteMessageFutures.toArray(new CompletableFuture[0]));
        deleteFutures.join();


        System.out.println("Total messages deleted " + deleteMessageFutures.size());
    }

    private static void useCaseOfAPIsIfbatchManagerNotUsed(SqsAsyncClient sqsAsyncClient) throws InterruptedException {
        //[REVIEW AREA  for simple sendMessage]
        sqsAsyncClient.sendMessage(s -> s.queueUrl(QUEUE_URL).messageBody("Hi Simple"));
        sqsAsyncClient.sendMessage(s -> s.queueUrl(QUEUE_URL).messageBody("Hello Simple"));


        //[REVIEW AREA  for CALL BACK using when complete]
        List<String> messages = new ArrayList<>();
        List<CompletableFuture<SendMessageResponse>> futures =
            IntStream.rangeClosed(0, 408)
                     .mapToObj(i ->
                                   sqsAsyncClient.sendMessage(s -> s.queueUrl(QUEUE_URL).messageBody("Hi " + i))
                                                 .whenComplete((response, exception) -> {
                                                     if (exception == null) {
                                                         messages.add(response.messageId());
                                                     } else {
                                                         exception.printStackTrace();
                                                     }
                                                 })
                     )
                     .collect(Collectors.toList());


        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        System.out.println("Messages sent " + messages.size());


        System.out.println("Sleeping for some time to give message some time to reflect in Sqs servers");
        Thread.sleep(9000);


        List<CompletableFuture<ReceiveMessageResponse>> recieveFutures = new ArrayList<>();

        AtomicInteger messageCounter = new AtomicInteger();
        AtomicBoolean done = new AtomicBoolean(false);

        final Instant startTime = Instant.now();

        while (true) {
            //[REVIEW AREA  for  receiveMessage]
            CompletableFuture<ReceiveMessageResponse> future = sqsAsyncClient.receiveMessage(r -> r.queueUrl(QUEUE_URL)
                                                                                                   .maxNumberOfMessages(10))
                                                                             .whenComplete((r,e) -> {

                    if(messageCounter.addAndGet(r.messages().size()) >= 400){
                        done.set(true);
                    };
            });
            recieveFutures.add(future);
            Thread.sleep(50); // Sleep for 200ms

            if(done.get()){
                System.out.println("Recieve Done in millis " + (Instant.now().toEpochMilli() - startTime.toEpochMilli()));

                break;
            }

        }

        CompletableFuture<Void> allOf = CompletableFuture.allOf(recieveFutures.toArray(new CompletableFuture[0]));
        allOf.join();

        List<Message> recievedMessages = new ArrayList<>();
        recieveFutures.forEach(future -> future.thenAccept(response -> {
            // response.messages().forEach(message -> System.out.println(message.body()));
            recievedMessages.addAll(response.messages());
        }));


        System.out.println("Total messages recieved " + recievedMessages.size());


        List<CompletableFuture<ChangeMessageVisibilityResponse>> visibilityFutures =
            recievedMessages.stream()
                            .map(message ->
                                     //[REVIEW AREA  for  changeMessageVisibility]
                                     sqsAsyncClient.changeMessageVisibility(c -> c
                                         .queueUrl(QUEUE_URL)
                                         .receiptHandle(message.receiptHandle())
                                         .visibilityTimeout(1)))
                            .collect(Collectors.toList());

        CompletableFuture<Void> changeVisibiltyFutures =
            CompletableFuture.allOf(visibilityFutures.toArray(new CompletableFuture[0]));

        changeVisibiltyFutures.join();

        visibilityFutures.forEach(future -> future.thenAccept(response -> {
        }));

        System.out.println("visibilityFutures changes for " + visibilityFutures.size());


        List<CompletableFuture<DeleteMessageResponse>> deleteMessageFutures = recievedMessages.stream()
                                                                                              .map(message ->
                                                                                                       //[REVIEW AREA  for
                                                                                                       // deleteMessage]
                                                                                                       sqsAsyncClient.deleteMessage(c -> c.queueUrl(QUEUE_URL).receiptHandle(message.receiptHandle()))
                                                                                              )
                                                                                              .collect(Collectors.toList());

        CompletableFuture<Void> deleteFutures = CompletableFuture.allOf(deleteMessageFutures.toArray(new CompletableFuture[0]));
        deleteFutures.join();


        System.out.println("Total messages deleted " + deleteMessageFutures.size());
    }

    // Appendix

    public class ApiCaptureInterceptor implements ExecutionInterceptor {

        // A thread-safe map to store the number of calls made for each API request type
        private final Map<String, Integer> callsMadeMap = new ConcurrentHashMap<>();

        @Override
        public void afterExecution(Context.AfterExecution context, ExecutionAttributes executionAttributes) {
            // Get the simple class name of the request
            String simpleName = context.request().getClass().getSimpleName();

            // Increment the counter for this API request type
            callsMadeMap.merge(simpleName, 1, Integer::sum);
        }

        // You may want to add methods to retrieve or print the map
        public Map<String, Integer> getCallsMadeMap() {
            return callsMadeMap;
        }
    }


}
