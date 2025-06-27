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

import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.batchmanager.SqsAsyncBatchManager;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

/**
 * Tests SQS message sending with improved memory management.
 */
@Ignore
public class SqsSendMessageApp {
    private static final String QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/248213382692/myTestQueue0";
    private static final int MESSAGE_SIZE = 12_000;
    private static final int MESSAGE_COUNT = 270000;
    private static final int DELAY_MS = 30;
    private static final int BATCH_SIZE = 1000;

    @Test
    void testBatchSize() throws Exception {
        // Create SQS client and batch manager
        SqsAsyncClient sqsAsyncClient = SqsAsyncClient.builder().build();
        SqsAsyncBatchManager batchManager = sqsAsyncClient.batchManager();

        // Create message template
        String messageBody = createLargeString('a', MESSAGE_SIZE);
        SendMessageRequest messageTemplate = SendMessageRequest.builder()
                                                               .queueUrl(QUEUE_URL)
                                                               .messageBody(messageBody)
                                                               .build();


        while (true) {
            batchManager.sendMessage(messageTemplate).whenComplete((response, error) -> {
                if (error != null) {
                    System.err.println("Error sending message: " + error.getMessage());
                } else {
                    System.out.println("Message sent successfully: " + response.messageId());
                }
            });

            Thread.sleep(DELAY_MS);
        }
    }

    /**
     * Creates a string of specified length filled with the given character.
     */
    private String createLargeString(char ch, int length) {
        char[] chars = new char[length];
        java.util.Arrays.fill(chars, ch);
        return new String(chars);
    }
}
