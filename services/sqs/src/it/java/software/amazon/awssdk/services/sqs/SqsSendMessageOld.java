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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.services.sqs.batchmanager.SqsAsyncBatchManager;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

/**
 * Tests SQS message sending with size monitoring.
 */
@Ignore
public class SqsSendMessageOld {

    String QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/248213382692/myTestQueue0";
    private static final int MESSAGE_SIZE = 12_000;
    private static final int MESSAGE_COUNT = 270000;
    private static final int DELAY_MS = 30;

    @Test
    void testBatchSize() throws Exception {
        ExecutionInterceptor captureMessageSizeInterceptor = new CaptureMessageSizeInterceptor();

        SqsAsyncClient sqsAsyncClient = SqsAsyncClient.builder()
                                                      // .overrideConfiguration(o -> o.addExecutionInterceptor(captureMessageSizeInterceptor))
                                                      .build();

        String messageBody = createLargeString('a', MESSAGE_SIZE);
        SqsAsyncBatchManager sqsAsyncBatchManager = sqsAsyncClient.batchManager();

        SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                                                                  .queueUrl(QUEUE_URL)
                                                                  .messageBody(messageBody)
                                                                  .delaySeconds(20)
                                                                  .build();

        List<CompletableFuture<SendMessageResponse>> futures = sendMessages(
            sqsAsyncBatchManager, sendMessageRequest, MESSAGE_COUNT);

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).join();

        System.out.println("All messages sent successfully");
    }

    /**
     * Sends multiple messages with a delay between each.
     *
     * @param batchManager The batch manager to use
     * @param messageRequest The message request template
     * @param count Number of messages to send
     * @return List of futures for the send operations
     * @throws InterruptedException If thread is interrupted during sleep
     */
    private List<CompletableFuture<SendMessageResponse>> sendMessages(
        SqsAsyncBatchManager batchManager,
        SendMessageRequest messageRequest,
        int count) throws InterruptedException {

        List<CompletableFuture<SendMessageResponse>> futures = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            CompletableFuture<SendMessageResponse> future = batchManager.sendMessage(messageRequest)
                                                                        .whenComplete((response, error) -> {
                                                                            if (error != null) {
                                                                                error.printStackTrace();
                                                                            } else {
                                                                                System.out.println("Message sent with ID: " + response.messageId());
                                                                            }
                                                                        });

            futures.add(future);

            if (i < count - 1) {
                Thread.sleep(DELAY_MS);
            }
        }

        return futures;
    }

    /**
     * Creates a string of specified length filled with the given character.
     *
     * @param ch Character to fill the string with
     * @param length Length of the string to create
     * @return The generated string
     */
    private String createLargeString(char ch, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ch);
        }
        return sb.toString();
    }

    /**
     * Interceptor that captures and logs message sizes in batch requests.
     */
    static class CaptureMessageSizeInterceptor implements ExecutionInterceptor {
        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            if (context.request() instanceof SendMessageBatchRequest) {
                SendMessageBatchRequest batchRequest = (SendMessageBatchRequest) context.request();

                System.out.println("Batch contains " + batchRequest.entries().size() + " messages");

                int totalMessageBodySize = 0;
                for (SendMessageBatchRequestEntry entry : batchRequest.entries()) {
                    int messageSize = entry.messageBody().length();
                    totalMessageBodySize += messageSize;
                    System.out.println("Message body size: " + messageSize + " bytes");
                }

                System.out.println("Total message bodies size: " + totalMessageBodySize + " bytes");
            }
        }
    }
}
