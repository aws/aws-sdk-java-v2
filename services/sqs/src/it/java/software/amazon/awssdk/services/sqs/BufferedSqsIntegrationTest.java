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

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.services.sqs.buffered.SqsBufferedAsyncClient;
import software.amazon.awssdk.services.sqs.buffered.QueueBufferConfig;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.util.StringUtils;

public class BufferedSqsIntegrationTest extends IntegrationTestBase {

    private static final int MAX_SIZE_MESSAGE = 260 * 1024 - 1;

    private SQSAsyncClient sqsClient;
    private QueueBufferConfig config;
    private SqsBufferedAsyncClient buffSqs;
    private String queueUrl;

    @Before
    public void setup() {
        config = new QueueBufferConfig();
        sqsClient = createSqsAyncClient();
        buffSqs = new SqsBufferedAsyncClient(sqsClient, config);
        queueUrl = createQueue(sqsClient);
    }

    @After
    public void tearDown() throws Exception {
        buffSqs.deleteQueue(DeleteQueueRequest.builder().queueUrl(queueUrl).build());
        buffSqs.close();
    }

    @Test
    public void receiveMessage_NoMessagesOnQueue_ReturnsEmptyListOfMessages() {
        assertEquals(0, buffSqs.receiveMessage(ReceiveMessageRequest.builder().queueUrl(queueUrl).build()).join().messages().size());
    }

    @Test
    public void receiveMessage_WhenAllBufferedBatchesExpire_FetchesNewBatchesFromSqs() throws InterruptedException {
        final int visiblityTimeoutSeconds = 2;
        config.withVisibilityTimeoutSeconds(visiblityTimeoutSeconds);

        List<SendMessageBatchRequestEntry> messages = new ArrayList<SendMessageBatchRequestEntry>();
        final int numOfTestMessages = 10;
        for (int messageNum = 1; messageNum <= numOfTestMessages; messageNum++) {
            messages.add(SendMessageBatchRequestEntry.builder().messageBody(String.valueOf(messageNum)).id("test-" + messageNum).build());
        }
        // Use the normal client so we don't have to wait for the buffered messages to be sent
        sqsClient.sendMessageBatch(SendMessageBatchRequest.builder().queueUrl(queueUrl).entries(messages).build());
        assertThat(buffSqs.receiveMessage(ReceiveMessageRequest.builder().queueUrl(queueUrl).build()).join().messages().size(), greaterThan(0));
        // Make sure they are expired by waiting twice the timeout
        Thread.sleep((visiblityTimeoutSeconds * 2) * 1000);
        assertThat(buffSqs.receiveMessage(ReceiveMessageRequest.builder().queueUrl(queueUrl).build()).join().messages().size(), greaterThan(0));
    }

    /**
     * Tests by trying to send a message of size larger than the allowed limit. Also tests to see if
     * an exception is thrown when the user tries to set the max size more than the allowed limit of
     * 256 KiB
     */
    @Test(expected = AmazonClientException.class)
    public void sendMessage_MaxSizeExceeded_ThrowsAmazonClientException() {

        final byte[] bytes = new byte[MAX_SIZE_MESSAGE];
        new Random().nextBytes(bytes);
        final String randomString = new String(bytes, StringUtils.UTF8);

        SendMessageRequest request = SendMessageRequest.builder()
                .messageBody(randomString)
                .queueUrl(queueUrl)
                .build();
        buffSqs.sendMessage(request);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setMaxBatchSizeOnQueueBuffer_WhenMaxSizeExceeded_ThrowsIllegalArgumentException() {
        QueueBufferConfig config = new QueueBufferConfig();
        config.setMaxBatchSizeBytes(MAX_SIZE_MESSAGE);
    }

    @Test
    public void receiveMessage_WhenMessagesAreOnTheQueueAndLongPollIsEnabled_ReturnsMessage() throws Exception {
        String body = "test message_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString();
        // Use the normal client so we don't have to wait for the buffered messages to be sent
        sqsClient.sendMessage(SendMessageRequest.builder().messageBody(body).queueUrl(queueUrl).build());
        long start = System.nanoTime();

        ReceiveMessageRequest receiveRq = ReceiveMessageRequest.builder().maxNumberOfMessages(1)
                                                                     .waitTimeSeconds(60).queueUrl(queueUrl)
                .build();
        List<Message> messages = buffSqs.receiveMessage(receiveRq).join().messages();
        assertThat(messages, hasSize(1));
        assertEquals(body, messages.get(0).body());

        long total = System.nanoTime() - start;

        if (TimeUnit.SECONDS.convert(total, TimeUnit.NANOSECONDS) > 60) {
            // we've waited for more than a minute for our message to
            // arrive. that's pretty bad.
            throw new Exception("Timed out waiting for the desired message to arrive");
        }
    }

}
