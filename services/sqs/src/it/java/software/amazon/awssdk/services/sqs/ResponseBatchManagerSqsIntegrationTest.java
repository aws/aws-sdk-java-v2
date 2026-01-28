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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.services.sqs.batchmanager.SqsAsyncBatchManager;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ResponseBatchManagerSqsIntegrationTest extends IntegrationTestBase {
    private static final String TEST_QUEUE_PREFIX = "ResponseBatchManagerSqsIntegrationTest-";
    private static SqsAsyncClient client;
    private static String defaultQueueUrl;
    private static SqsAsyncBatchManager batchManager;

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        client = SqsAsyncClient.builder()
                               .credentialsProvider(getCredentialsProvider())
                               .build();
        defaultQueueUrl = client.createQueue(CreateQueueRequest.builder()
                                                               .queueName(TEST_QUEUE_PREFIX + UUID.randomUUID().toString())
                                                               .build())
                                .get(3, TimeUnit.SECONDS)
                                .queueUrl();
    }

    @AfterAll
    public static void tearDownAfterClass() {
        purgeQueue(defaultQueueUrl);
        client.deleteQueue(d -> d.queueUrl(defaultQueueUrl)).join();
        if(batchManager != null){
            batchManager.close();
        }
        client.close();
    }

    private static void purgeQueue(String queueUrl) {
        client.purgeQueue(PurgeQueueRequest.builder()
                                           .queueUrl(queueUrl)
                                           .build())
              .join();
    }

    private static void deleteMessages(ReceiveMessageResponse receiveMessageResponse, SqsAsyncBatchManager batchManager) {
        List<CompletableFuture<DeleteMessageResponse>> deleteFutures =
            receiveMessageResponse.messages().stream()
                                  .map(message -> batchManager.deleteMessage(r -> r.queueUrl(defaultQueueUrl)
                                                                                   .receiptHandle(message.receiptHandle())))
                                  .collect(Collectors.toList());
        CompletableFuture.allOf(deleteFutures.toArray(new CompletableFuture[0])).join();
    }

    private static void sendMessages(SqsAsyncBatchManager batchManager, String queueUrl, int numOfMessages) throws Exception {
        List<CompletableFuture<Void>> futures = IntStream.rangeClosed(1, numOfMessages)
                                                         .mapToObj(i -> batchManager.sendMessage(s -> s.queueUrl(queueUrl)
                                                                                                       .messageBody("Message " + i)
                                                                                                       .messageAttributes(messageAttributeValueMap()))
                                                                                    .thenAccept(response -> {
                                                                                    }))  // Removed logging
                                                         .collect(Collectors.toList());
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
    }

    private static Map<String, MessageAttributeValue> messageAttributeValueMap() {
        Map<String, MessageAttributeValue> attributes = new HashMap<>();
        attributes.put("keyOne", MessageAttributeValue.builder().stringValue("4").dataType("String").build());
        attributes.put("keyTwo", MessageAttributeValue.builder().stringValue("2").dataType("String").build());
        attributes.put("keyThree", MessageAttributeValue.builder().stringValue("3").dataType("String").build());
        attributes.put("keyFour", MessageAttributeValue.builder().stringValue("5").dataType("String").build());
        return attributes;
    }

    private void assertMessagesReceived(SqsAsyncBatchManager batchManager, int expectedMessageCount) throws Exception {
        List<Message> allMessages = new ArrayList<>();
        while (allMessages.size() < expectedMessageCount) {
            CompletableFuture<ReceiveMessageResponse> receiveMessageFuture =
                batchManager.receiveMessage(r -> r.queueUrl(defaultQueueUrl));
            ReceiveMessageResponse receiveMessageResponse = receiveMessageFuture.get(5, TimeUnit.SECONDS);
            if (!receiveMessageResponse.messages().isEmpty()) {
                allMessages.addAll(receiveMessageResponse.messages());
                deleteMessages(receiveMessageResponse, batchManager);
            }
        }
        assertThat(allMessages.size()).isEqualTo(expectedMessageCount);
    }

    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @Test
    void simpleReceiveMessagesWithDefaultConfiguration() throws Exception {
        batchManager = client.batchManager();
        sendMessages(batchManager, defaultQueueUrl, 10);
        assertMessagesReceived(batchManager, 10);

        List<Message> allMessages = batchManager.receiveMessage(r -> r.queueUrl(defaultQueueUrl))
                                                .get(5, TimeUnit.SECONDS)
                                                .messages();
        assertTrue(allMessages.stream().allMatch(m -> m.messageAttributes().isEmpty()), "Expected no message attributes.");
        assertTrue(allMessages.stream().allMatch(m -> m.attributes().isEmpty()), "Expected no system attributes.");
    }

    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @Test
    void simpleReceiveMessagesWithCustomConfigurations() throws Exception {
        batchManager = SqsAsyncBatchManager.builder()
                                           .client(client)
                                           .scheduledExecutor(Executors.newScheduledThreadPool(5))
                                           .overrideConfiguration(b -> b
                                               .receiveMessageMinWaitDuration(Duration.ofSeconds(10))
                                               .receiveMessageVisibilityTimeout(Duration.ofSeconds(1))
                                               .receiveMessageAttributeNames(Collections.singletonList("*"))
                                               .receiveMessageSystemAttributeNames(Collections.singletonList(MessageSystemAttributeName.ALL)))
                                           .build();

        sendMessages(batchManager, defaultQueueUrl, 10);
        assertMessagesReceived(batchManager, 10);

        List<Message> allMessages = batchManager.receiveMessage(r -> r.queueUrl(defaultQueueUrl))
                                                .get(5, TimeUnit.SECONDS)
                                                .messages();

        Map<String, MessageAttributeValue> expectedMessageAttributes = messageAttributeValueMap();
        allMessages.forEach(m -> {
            assertFalse(m.messageAttributes().isEmpty(), "Expected message attributes, but found none.");
            assertThat(m.messageAttributes()).isEqualTo(expectedMessageAttributes);
            assertFalse(m.attributes().isEmpty(), "Expected system attributes, but found none.");
            assertTrue(m.attributes().containsKey(MessageSystemAttributeName.SENDER_ID), "Expected SenderId, but missing.");
            assertTrue(m.attributes().containsKey(MessageSystemAttributeName.APPROXIMATE_FIRST_RECEIVE_TIMESTAMP), "Expected "
                                                                                                                   +
                                                                                                                   "ApproximateFirstReceiveTimestamp, but missing.");
        });
    }

    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @Test
    void requestLevelMaxMessageAndWaitTimeIsHonoured() throws Exception {
        batchManager = client.batchManager();

        sendMessages(batchManager, defaultQueueUrl, 10);

        List<Message> allMessages = new ArrayList<>();
        List<CompletableFuture<DeleteMessageResponse>> deleteFutures = new ArrayList<>();
        int expectedMessageCount = 10;

        while (allMessages.size() < expectedMessageCount) {
            CompletableFuture<ReceiveMessageResponse> future =
                batchManager.receiveMessage(r -> r.queueUrl(defaultQueueUrl)
                                                  .maxNumberOfMessages(1)
                                                  .waitTimeSeconds(2));

            // Process the received message and delete if non-empty
            future.whenComplete((response, throwable) -> {
                if (response != null && !response.messages().isEmpty()) {
                    allMessages.addAll(response.messages());
                    assertThat(response.messages().size()).isEqualTo(1);

                    // Collect deleteMessage futures for each message
                    response.messages().forEach(message -> {
                        CompletableFuture<DeleteMessageResponse> deleteFuture = batchManager.deleteMessage(d -> d.queueUrl(defaultQueueUrl)
                                                                                                                 .receiptHandle(message.receiptHandle()));
                        deleteFutures.add(deleteFuture);
                    });
                }
            }).join();  // Wait for each receiveMessage future to complete before continuing
        }

        // Wait for all deleteMessage futures to complete
        CompletableFuture<Void> allDeleteFutures = CompletableFuture.allOf(deleteFutures.toArray(new CompletableFuture[0]));
        allDeleteFutures.join();

        assertThat(allMessages.size()).isEqualTo(expectedMessageCount);
    }
}