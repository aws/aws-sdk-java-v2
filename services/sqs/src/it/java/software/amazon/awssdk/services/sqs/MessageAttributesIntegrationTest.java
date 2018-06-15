/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static software.amazon.awssdk.testutils.SdkAsserts.assertNotEmpty;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.util.ImmutableMapParameter;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

/**
 * Integration tests for the SQS message attributes.
 */
public class MessageAttributesIntegrationTest extends IntegrationTestBase {

    private static final String MESSAGE_BODY = "message-body-" + System.currentTimeMillis();

    private String queueUrl;

    @Before
    public void setup() {
        queueUrl = createQueue(sqsAsync);
    }

    @After
    public void tearDown() throws Exception {
        sqsAsync.deleteQueue(DeleteQueueRequest.builder().queueUrl(queueUrl).build());
    }

    @Test
    public void sendMessage_InvalidMd5_ThrowsException() {
        try (SqsClient tamperingClient = SqsClient.builder()
                                                  .credentialsProvider(getCredentialsProvider())
                                                  .overrideConfiguration(ClientOverrideConfiguration
                                                                                 .builder()
                                                                                 .addExecutionInterceptor(
                                                                                         new TamperingInterceptor())
                                                                                 .build())
                                                  .build()) {
            tamperingClient.sendMessage(
                    SendMessageRequest.builder()
                                      .queueUrl(queueUrl)
                                      .messageBody(MESSAGE_BODY)
                                      .messageAttributes(createRandomAttributeValues(10))
                                      .build());
            fail("Expected SdkClientException");
        } catch (SdkClientException e) {
            assertThat(e.getMessage(), containsString("MD5 returned by SQS does not match"));
        }
    }

    public static class TamperingInterceptor implements ExecutionInterceptor {

        @Override
        public SdkResponse modifyResponse(Context.ModifyResponse context, ExecutionAttributes executionAttributes) {
            if (context.response() instanceof SendMessageResponse) {
                return ((SendMessageResponse) context.response()).toBuilder()
                                                                 .md5OfMessageBody("invalid-md5")
                                                                 .build();
            }
            return context.response();
        }
    }

    @Test
    public void sendMessage_WithMessageAttributes_ResultHasMd5OfMessageAttributes() {
        SendMessageResponse sendMessageResult = sendTestMessage();
        assertNotEmpty(sendMessageResult.md5OfMessageBody());
        assertNotEmpty(sendMessageResult.md5OfMessageAttributes());
    }

    /**
     * Makes sure we don't modify the state of ByteBuffer backed attributes in anyway internally
     * before returning the result to the customer. See https://github.com/aws/aws-sdk-java/pull/459
     * for reference
     */
    @Test
    public void receiveMessage_WithBinaryAttributeValue_DoesNotChangeStateOfByteBuffer() {
        byte[] bytes = new byte[]{1, 1, 1, 0, 0, 0};
        String byteBufferAttrName = "byte-buffer-attr";
        Map<String, MessageAttributeValue> attrs = ImmutableMapParameter.of(byteBufferAttrName,
                MessageAttributeValue.builder().dataType("Binary").binaryValue(ByteBuffer.wrap(bytes)).build());

        sqsAsync.sendMessage(SendMessageRequest.builder().queueUrl(queueUrl).messageBody("test")
                .messageAttributes(attrs)
                .build());
        // Long poll to make sure we get the message back
        List<Message> messages = sqsAsync.receiveMessage(
                ReceiveMessageRequest.builder().queueUrl(queueUrl).messageAttributeNames("All").waitTimeSeconds(20).build()).join()
                .messages();

        ByteBuffer actualByteBuffer = messages.get(0).messageAttributes().get(byteBufferAttrName).binaryValue();
        assertEquals(bytes.length, actualByteBuffer.remaining());
    }

    @Test
    public void receiveMessage_WithAllAttributesRequested_ReturnsAttributes() throws Exception {
        sendTestMessage();

        ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder().queueUrl(queueUrl).waitTimeSeconds(5)
                .visibilityTimeout(0).messageAttributeNames("All").build();
        ReceiveMessageResponse receiveMessageResult = sqsAsync.receiveMessage(receiveMessageRequest).join();

        assertFalse(receiveMessageResult.messages().isEmpty());
        Message message = receiveMessageResult.messages().get(0);
        assertEquals(MESSAGE_BODY, message.body());
        assertNotEmpty(message.md5OfBody());
        assertNotEmpty(message.md5OfMessageAttributes());
    }

    /**
     * Tests SQS operations that involve message attributes checksum.
     */
    @Test
    public void receiveMessage_WithNoAttributesRequested_DoesNotReturnAttributes() throws Exception {
        sendTestMessage();

        ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder().queueUrl(queueUrl).waitTimeSeconds(5)
                .visibilityTimeout(0).build();
        ReceiveMessageResponse receiveMessageResult = sqsAsync.receiveMessage(receiveMessageRequest).join();

        assertFalse(receiveMessageResult.messages().isEmpty());
        Message message = receiveMessageResult.messages().get(0);
        assertEquals(MESSAGE_BODY, message.body());
        assertNotEmpty(message.md5OfBody());
        assertNull(message.md5OfMessageAttributes());
    }

    @Test
    public void sendMessageBatch_WithMessageAttributes_ResultHasMd5OfMessageAttributes() {
        SendMessageBatchResponse sendMessageBatchResult = sqsAsync.sendMessageBatch(SendMessageBatchRequest.builder()
                .queueUrl(queueUrl)
                .entries(
                        SendMessageBatchRequestEntry.builder().id("1").messageBody(MESSAGE_BODY)
                                .messageAttributes(createRandomAttributeValues(1)).build(),
                        SendMessageBatchRequestEntry.builder().id("2").messageBody(MESSAGE_BODY)
                                .messageAttributes(createRandomAttributeValues(2)).build(),
                        SendMessageBatchRequestEntry.builder().id("3").messageBody(MESSAGE_BODY)
                                .messageAttributes(createRandomAttributeValues(3)).build(),
                        SendMessageBatchRequestEntry.builder().id("4").messageBody(MESSAGE_BODY)
                                .messageAttributes(createRandomAttributeValues(4)).build(),
                        SendMessageBatchRequestEntry.builder().id("5").messageBody(MESSAGE_BODY)
                                .messageAttributes(createRandomAttributeValues(5)).build())
                .build())
                .join();

        assertThat(sendMessageBatchResult.successful().size(), greaterThan(0));
        assertNotEmpty(sendMessageBatchResult.successful().get(0).id());
        assertNotEmpty(sendMessageBatchResult.successful().get(0).md5OfMessageBody());
        assertNotEmpty(sendMessageBatchResult.successful().get(0).md5OfMessageAttributes());
    }

    private SendMessageResponse sendTestMessage() {
        SendMessageResponse sendMessageResult = sqsAsync.sendMessage(SendMessageRequest.builder().queueUrl(queueUrl).messageBody(MESSAGE_BODY)
                .messageAttributes(createRandomAttributeValues(10)).build()).join();
        return sendMessageResult;
    }
}
