/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isIn;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.sqs.buffered.SqsBufferedAsyncClient;
import software.amazon.awssdk.services.sqs.buffered.QueueBufferConfig;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

/**
 * Tests async send/receive/delete operation on the Buffered client. Creates a bunch of messages on
 * a queue, receives and deletes those messages one by one and finally makes sure all sent messages
 * were received.
 */
public class BufferedSqsAsyncIntegrationTest extends IntegrationTestBase {

    private static final int MESSAGE_ATTRIBUTES_PER_MESSAGE = 10;
    private static final Map<String, MessageAttributeValue> ATTRIBUTES =
            createRandomAttributeValues(MESSAGE_ATTRIBUTES_PER_MESSAGE);
    private static final int NUM_MESSAGES = 50;
    private static final int NUM_OF_CONSUMERS = 5;

    private SqsBufferedAsyncClient buffSqs;
    private String queueUrl;

    @Before
    public void setup() {
        buffSqs = new SqsBufferedAsyncClient(createSqsAyncClient(),
                                                   new QueueBufferConfig().withLongPollWaitTimeoutSeconds(60));
        queueUrl = createQueue(buffSqs);
    }

    @After
    public void tearDown() throws Exception {
        buffSqs.deleteQueue(DeleteQueueRequest.builder().queueUrl(queueUrl).build());
        buffSqs.close();
    }

    @Test
    public void testAsyncOperations() throws Exception {
        List<Future<SendMessageResponse>> sendResults = new LinkedList<Future<SendMessageResponse>>();
        Set<String> messages = generateTestMessages(sendResults);

        waitForFutures(sendResults);
        System.out.println("All sending futures returned....");

        // Now start receiving messages and removing them from resultSet
        ExecutorService executor = Executors.newCachedThreadPool();
        waitForFutures(submitMessageConsumerTasks(messages, executor));

        // If we have successfully received all messsages resultSet should be empty
        assertThat(messages, empty());
        System.out.println("All receive threads exited....");
        executor.shutdown();
    }

    private List<Future<Void>> submitMessageConsumerTasks(Set<String> messages, ExecutorService executor) {
        List<Future<Void>> futures = new ArrayList<Future<Void>>(NUM_OF_CONSUMERS);
        for (int i = 0; i < NUM_OF_CONSUMERS; i++) {
            MessageConsumer consumer = new MessageConsumer(buffSqs, messages, queueUrl, ATTRIBUTES);
            futures.add(executor.submit(consumer));
        }
        return futures;
    }

    private <T> void waitForFutures(List<Future<T>> futures) throws InterruptedException, ExecutionException {
        for (Future<?> future : futures) {
            future.get();
        }
    }

    /**
     * Sends several test messages to SQS and returns a set of all message bodies sent
     */
    private Set<String> generateTestMessages(List<Future<SendMessageResponse>> sendResults) {
        Set<String> messages = Collections.synchronizedSet(new HashSet<String>());
        for (int i = 0; i < NUM_MESSAGES; i++) {
            String body = "test message " + i + "_" + System.currentTimeMillis();
            SendMessageRequest request = SendMessageRequest.builder().messageBody(body).queueUrl(queueUrl)
                                                                 .messageAttributes(ATTRIBUTES).build();

            sendResults.add(buffSqs.sendMessage(request));
            messages.add(body);
        }
        return messages;
    }

    private class MessageConsumer implements Callable<Void> {

        private static final int TIMEOUT_IN_SECONDS = 3 * 60;
        private SQSAsyncClient buffSqs;
        private Set<String> resultSet;
        private String url;
        private Map<String, MessageAttributeValue> expectedAttributes;

        public MessageConsumer(SQSAsyncClient paramSQS, Set<String> set, String paramUrl,
                               Map<String, MessageAttributeValue> expectedAttributes) {
            this.buffSqs = paramSQS;
            this.resultSet = set;
            this.url = paramUrl;
            this.expectedAttributes = expectedAttributes;
        }

        @Override
        public Void call() throws Exception {
            long operationStart = System.nanoTime();

            while (true) {
                List<Message> messages = recieveMessage();
                // It's possible for messages to be empty but resultSet to still have items. This
                // can happen if other messages have been received but not yet removed in resultSet.
                // This is okay because we assert that resultSet is empty when all consumers are
                // done
                if (resultSet.isEmpty() || messages.isEmpty()) {
                    return null;
                }
                assertThat(messages, hasSize(1));

                Message theMessage = messages.get(0);
                assertMessageIsValid(theMessage);

                resultSet.remove(theMessage.body());
                deleteMessage(messages.get(0));

                long totalRunningTime = System.nanoTime() - operationStart;

                if (TimeUnit.SECONDS.convert(totalRunningTime, TimeUnit.NANOSECONDS) > TIMEOUT_IN_SECONDS) {
                    throw new RuntimeException("Timed out waiting for the desired message to arrive");
                }

            }
        }

        private List<Message> recieveMessage() throws InterruptedException, ExecutionException {
            ReceiveMessageRequest recRequest = ReceiveMessageRequest.builder().maxNumberOfMessages(1).queueUrl(url)
                                                                          .messageAttributeNames("All")
                    .build();
            Future<ReceiveMessageResponse> future = buffSqs.receiveMessage(recRequest);
            List<Message> messages = future.get().messages();
            return messages;
        }

        private void assertMessageIsValid(Message theMessage) {
            assertNotNull(theMessage);
            assertNotNull(theMessage.md5OfMessageAttributes());
            assertNotNull(theMessage.messageAttributes());
            assertThat(theMessage.messageAttributes().entrySet(), everyItem(isIn(expectedAttributes.entrySet())));
            assertThat(theMessage.messageAttributes().entrySet(), hasSize(expectedAttributes.size()));
        }

        private void deleteMessage(Message theMessage) throws InterruptedException, ExecutionException {
            DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder().queueUrl(url).receiptHandle(
                    theMessage.receiptHandle()).build();
            buffSqs.deleteMessage(deleteRequest).get();
        }

    }
}
