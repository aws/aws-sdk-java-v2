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

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.junit.After;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException.ErrorType;
import software.amazon.awssdk.ResponseMetadata;
import software.amazon.awssdk.services.sqs.model.AddPermissionRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.ListDeadLetterSourceQueuesRequest;
import software.amazon.awssdk.services.sqs.model.ListDeadLetterSourceQueuesResponse;
import software.amazon.awssdk.services.sqs.model.ListQueuesRequest;
import software.amazon.awssdk.services.sqs.model.ListQueuesResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiptHandleIsInvalidException;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.RemovePermissionRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest;

public class SqsOperationsIntegrationTest extends IntegrationTestBase {

    private static final String ATTRIBUTE_VALUE = "42";
    private static final String ATTRIBUTE_NAME = "VisibilityTimeout";
    private static final String SPECIAL_CHARS = "%20%25~!@#$^&*(){}[]_-+\\<>/?";
    private static final String MESSAGE_BODY = "foobarbazbar" + SPECIAL_CHARS;

    private final String queueName = getUniqueQueueName();
    private final String deadLetterQueueName = "DLQ-" + queueName;

    private String queueUrl;
    private String deadLetterQueueUrl;

    @After
    public void tearDown() {
        sqsSync.deleteQueue(DeleteQueueRequest.builder().queueUrl(queueUrl).build());
        sqsSync.deleteQueue(DeleteQueueRequest.builder().queueUrl(deadLetterQueueUrl).build());
    }

    /**
     * Tests that each SQS operation can be called correctly, and that the result data is correctly
     * unmarshalled.
     */
    @Test
    public void testSqsOperations() throws Exception {
        runCreateQueueTest();
        runGetQueueUrlTest();
        runListQueuesTest();
        runSetQueueAttributesTest();
        runGetQueueAttributesTest();
        runAddPermissionTest();
        runRemovePermissionTest();
        runSendMessageTest();
        ReceiveMessageResponse receiveMessageResult = runReceiveMessageTest();
        runSendMessageBatch();
        String receiptHandle = runChangeMessageVisibilityTest(receiveMessageResult);
        runDeleteMessageTest(receiptHandle);
        runDlqTests();
        runDeleteMessageWithInvalidReceiptTest();
    }

    private void runCreateQueueTest() {
        CreateQueueRequest createQueueRequest = CreateQueueRequest.builder().queueName(queueName).build();
        CreateQueueResponse createQueueResult = sqsSync.createQueue(createQueueRequest);
        queueUrl = createQueueResult.queueUrl();
        assertNotEmpty(queueUrl);
    }

    private void runGetQueueUrlTest() {
        GetQueueUrlResponse queueUrlResult = sqsSync.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build());
        assertEquals(queueUrl, queueUrlResult.queueUrl());
    }

    private void runListQueuesTest() {
        ResponseMetadata responseMetadata;
        ListQueuesRequest listQueuesRequest = ListQueuesRequest.builder().queueNamePrefix(queueName).build();
        ListQueuesResponse listQueuesResult = sqsSync.listQueues(listQueuesRequest);
        assertEquals(1, listQueuesResult.queueUrls().size());
        assertEquals(queueUrl, listQueuesResult.queueUrls().get(0));
    }

    private void runSetQueueAttributesTest() {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(ATTRIBUTE_NAME, ATTRIBUTE_VALUE);
        SetQueueAttributesRequest setQueueAttributesRequest = SetQueueAttributesRequest.builder()
                .queueUrl(queueUrl)
                .attributes(attributes)
                .build();
        sqsSync.setQueueAttributes(setQueueAttributesRequest);

    }

    private void runGetQueueAttributesTest() throws InterruptedException {
        Thread.sleep(1000 * 10);
        GetQueueAttributesResponse queueAttributesResult = sqsSync.getQueueAttributes(GetQueueAttributesRequest.builder()
                                                                                              .queueUrl(queueUrl)
                                                                                              .attributeNames(new String[]{
                                                                                                      ATTRIBUTE_NAME}).build());
        assertEquals(1, queueAttributesResult.attributes().size());
        Map<String, String> attributes2 = queueAttributesResult.attributes();
        assertEquals(1, attributes2.size());
        assertNotNull(attributes2.get(ATTRIBUTE_NAME));
    }

    private void runAddPermissionTest() {
        sqsSync.addPermission(AddPermissionRequest.builder().actions(new String[]{"SendMessage", "DeleteMessage"})
                                        .awsAccountIds(getAccountId()).label("foo-label")
                                        .queueUrl(queueUrl).build());
    }

    private void runRemovePermissionTest() throws InterruptedException {
        Thread.sleep(1000 * 2);
        sqsSync.removePermission(RemovePermissionRequest.builder().label("foo-label").queueUrl(queueUrl).build());
    }

    private void runSendMessageTest() {
        for (int i = 0; i < 10; i++) {
            SendMessageResponse sendMessageResult = sqsSync.sendMessage(SendMessageRequest.builder().delaySeconds(1)
                                                                                .messageBody(MESSAGE_BODY)
                                                                                .queueUrl(queueUrl).build());
            assertNotEmpty(sendMessageResult.messageId());
            assertNotEmpty(sendMessageResult.md5OfMessageBody());
        }
    }

    private ReceiveMessageResponse runReceiveMessageTest() {
        ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .waitTimeSeconds(5)
                .maxNumberOfMessages(new Integer(8))
                .attributeNames(new String[]{"SenderId",
                        "SentTimestamp", "All"})
                .build();
        ReceiveMessageResponse receiveMessageResult = sqsSync.receiveMessage(receiveMessageRequest);
        assertThat(receiveMessageResult.messages(), not(empty()));
        Message message = receiveMessageResult.messages().get(0);
        assertEquals(MESSAGE_BODY, message.body());
        assertNotEmpty(message.md5OfBody());
        assertNotEmpty(message.messageId());
        assertNotEmpty(message.receiptHandle());
        assertThat(message.attributes().size(), greaterThan(3));

        for (Iterator<Entry<String, String>> iterator = message.attributes().entrySet().iterator(); iterator
                .hasNext(); ) {
            Entry<String, String> entry = iterator.next();
            assertNotEmpty((entry.getKey()));
            assertNotEmpty((entry.getValue()));
        }
        return receiveMessageResult;
    }

    private void runSendMessageBatch() {
        SendMessageBatchResponse sendMessageBatchResult = sqsSync.sendMessageBatch(SendMessageBatchRequest.builder()
                .queueUrl(queueUrl).entries(
                        SendMessageBatchRequestEntry.builder().id("1").messageBody("1" + SPECIAL_CHARS).build(),
                        SendMessageBatchRequestEntry.builder().id("2").messageBody("2" + SPECIAL_CHARS).build(),
                        SendMessageBatchRequestEntry.builder().id("3").messageBody("3" + SPECIAL_CHARS).build(),
                        SendMessageBatchRequestEntry.builder().id("4").messageBody("4" + SPECIAL_CHARS).build(),
                        SendMessageBatchRequestEntry.builder().id("5").messageBody("5" + SPECIAL_CHARS).build())
                .build());
        assertNotNull(sendMessageBatchResult.failed());
        assertThat(sendMessageBatchResult.successful().size(), greaterThan(0));
        assertNotNull(sendMessageBatchResult.successful().get(0).id());
        assertNotNull(sendMessageBatchResult.successful().get(0).md5OfMessageBody());
        assertNotNull(sendMessageBatchResult.successful().get(0).messageId());
    }

    private String runChangeMessageVisibilityTest(ReceiveMessageResponse receiveMessageResult) {
        String receiptHandle = (receiveMessageResult.messages().get(0)).receiptHandle();
        sqsSync.changeMessageVisibility(ChangeMessageVisibilityRequest.builder().queueUrl(queueUrl)
                                                  .receiptHandle(receiptHandle).visibilityTimeout(new Integer(123)).build());
        return receiptHandle;
    }

    private void runDeleteMessageTest(String receiptHandle) {
        sqsSync.deleteMessage(DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(receiptHandle).build());
    }

    private void runDlqTests() throws InterruptedException {
        CreateQueueResponse createDLQResult = sqsSync.createQueue(CreateQueueRequest.builder()
                                                                          .queueName(deadLetterQueueName).build());
        deadLetterQueueUrl = createDLQResult.queueUrl();
        // We have to get the ARN for the DLQ in order to set it on the redrive policy
        GetQueueAttributesResponse deadLetterQueueAttributes = sqsSync.getQueueAttributes(
                GetQueueAttributesRequest.builder().queueUrl(deadLetterQueueUrl).attributeNames(Arrays.asList(QueueAttributeName.QueueArn.toString())).build());

        assertNotNull(deadLetterQueueUrl);
        // Configure the DLQ
        final String deadLetterConfigAttributeName = "RedrivePolicy";
        final String deadLetterConfigAttributeValue = "{\"maxReceiveCount\" : 5, \"deadLetterTargetArn\" : \""
                                                      + deadLetterQueueAttributes.attributes()
                                                              .get(QueueAttributeName.QueueArn.toString()) + "\"}";
        sqsSync.setQueueAttributes(SetQueueAttributesRequest.builder().queueUrl(queueUrl).attributes(
                Collections.singletonMap(deadLetterConfigAttributeName, deadLetterConfigAttributeValue)).build());
        // List the DLQ
        Thread.sleep(1000 * 10);
        ListDeadLetterSourceQueuesResponse listDeadLetterSourceQueuesResult = sqsSync
                .listDeadLetterSourceQueues(ListDeadLetterSourceQueuesRequest.builder().queueUrl(deadLetterQueueUrl).build());
        assertThat(listDeadLetterSourceQueuesResult.queueUrls(), contains(queueUrl));
    }

    private void runDeleteMessageWithInvalidReceiptTest() {
        try {
            sqsSync.deleteMessage(DeleteMessageRequest.builder().queueUrl(
                    queueUrl).receiptHandle(
                    "alkdjfadfaldkjfdjkfldjfkjdljdljfljdjfldjfljflsjdf").build());
            fail("Expected an AmazonServiceException, but wasn't thrown");
        } catch (ReceiptHandleIsInvalidException e) {
            assertEquals("ReceiptHandleIsInvalid", e.getErrorCode());
            assertEquals(ErrorType.Client, e.getErrorType());
            assertNotEmpty(e.getMessage());
            assertNotEmpty(e.getRequestId());
            assertEquals("SQSClient", e.getServiceName());
            assertThat(e.getStatusCode(), allOf(greaterThanOrEqualTo(400), lessThan(500)));
        }
    }
}
