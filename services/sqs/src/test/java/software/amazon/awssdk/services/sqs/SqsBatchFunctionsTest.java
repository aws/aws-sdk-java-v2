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

import static software.amazon.awssdk.services.sqs.internal.batchmanager.SqsBatchFunctions.changeVisibilityBatchKeyMapper;
import static software.amazon.awssdk.services.sqs.internal.batchmanager.SqsBatchFunctions.changeVisibilityResponseMapper;
import static software.amazon.awssdk.services.sqs.internal.batchmanager.SqsBatchFunctions.deleteMessageBatchKeyMapper;
import static software.amazon.awssdk.services.sqs.internal.batchmanager.SqsBatchFunctions.deleteMessageResponseMapper;
import static software.amazon.awssdk.services.sqs.internal.batchmanager.SqsBatchFunctions.sendMessageBatchKeyMapper;
import static software.amazon.awssdk.services.sqs.internal.batchmanager.SqsBatchFunctions.sendMessageResponseMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.internal.batchmanager.IdentifiableMessage;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchResponse;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchResultEntry;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchResultEntry;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResultEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.Md5Utils;

// TODO: Will refactor code in this test file for async tests.
public class SqsBatchFunctionsTest {

    @Test
    public void sendMessageResponseMapper_mapResponsesCorrectly() {
        String id1 = "1";
        String id2 = "2";
        String messageBody1 = getMd5Hash("1");
        String messageBody2 = getMd5Hash("2");
        SendMessageBatchResultEntry entry1 = createSendMessageBatchEntry(id1, messageBody1);
        SendMessageBatchResultEntry entry2 = createSendMessageBatchEntry(id2, messageBody2);
        SendMessageBatchResponse batchResponse = SendMessageBatchResponse.builder()
                                                                         .successful(entry1, entry2)
                                                                         .build();
        List<IdentifiableMessage<SendMessageResponse>> mappedResponses =
            sendMessageResponseMapper().mapBatchResponse(batchResponse);

        IdentifiableMessage<SendMessageResponse> response1 = mappedResponses.get(0);
        IdentifiableMessage<SendMessageResponse> response2 = mappedResponses.get(1);
        Assert.assertEquals(id1, response1.id());
        Assert.assertEquals(messageBody1, response1.message().md5OfMessageBody());
        Assert.assertEquals(id2, response2.id());
        Assert.assertEquals(messageBody2, response2.message().md5OfMessageBody());
    }

    @Test
    public void sendMessageBatchKeyMapperWithoutOverrideConfig_hasSameBatchKey() {
        String queueUrl = "myQueue";
        SendMessageRequest request1 = SendMessageRequest.builder().queueUrl(queueUrl).build();
        SendMessageRequest request2 = SendMessageRequest.builder().queueUrl(queueUrl).build();
        String batchKey1 = sendMessageBatchKeyMapper().getBatchKey(request1);
        String batchKey2 = sendMessageBatchKeyMapper().getBatchKey(request2);

        Assert.assertEquals(queueUrl, batchKey1);
        Assert.assertEquals(queueUrl, batchKey2);
        Assert.assertEquals(batchKey1, batchKey2);
    }

    @Test
    public void sendMessageBatchKeyMapperWithOverrideConfig_hasSameBatchKey() {
        String queueUrl = "myQueue";
        AwsRequestOverrideConfiguration overrideConfiguration1 = createOverrideConfig(10);
        AwsRequestOverrideConfiguration overrideConfiguration2 = createOverrideConfig(10);
        SendMessageRequest request1 = SendMessageRequest.builder()
                                                        .queueUrl(queueUrl)
                                                        .overrideConfiguration(overrideConfiguration1)
                                                        .build();
        SendMessageRequest request2 = SendMessageRequest.builder()
                                                        .queueUrl(queueUrl)
                                                        .overrideConfiguration(overrideConfiguration2)
                                                        .build();
        String batchKey1 = sendMessageBatchKeyMapper().getBatchKey(request1);
        String batchKey2 = sendMessageBatchKeyMapper().getBatchKey(request2);
        Assert.assertEquals(batchKey1, batchKey2);
    }

    @Test
    public void sendMessageBatchKeyMapperWithDifferentOverrideConfig_hasDifferentBatchKey() {
        String queueUrl = "myQueue";
        AwsRequestOverrideConfiguration overrideConfiguration1 = createOverrideConfig(10);
        AwsRequestOverrideConfiguration overrideConfiguration2 = createOverrideConfig(20);
        SendMessageRequest request1 = SendMessageRequest.builder()
                                                        .queueUrl(queueUrl)
                                                        .overrideConfiguration(overrideConfiguration1)
                                                        .build();
        SendMessageRequest request2 = SendMessageRequest.builder()
                                                        .queueUrl(queueUrl)
                                                        .overrideConfiguration(overrideConfiguration2)
                                                        .build();
        String batchKey1 = sendMessageBatchKeyMapper().getBatchKey(request1);
        String batchKey2 = sendMessageBatchKeyMapper().getBatchKey(request2);
        Assert.assertNotEquals(batchKey1, batchKey2);
    }

    @Test
    public void deleteMessageResponseMapper_mapResponsesCorrectly() {
        String id1 = "1";
        String id2 = "2";
        DeleteMessageBatchResultEntry entry1 = createDeleteMessageBatchEntry(id1);
        DeleteMessageBatchResultEntry entry2 = createDeleteMessageBatchEntry(id2);
        DeleteMessageBatchResponse batchResponse = DeleteMessageBatchResponse.builder().successful(entry1, entry2).build();
        List<IdentifiableMessage<DeleteMessageResponse>> mappedResponses =
            deleteMessageResponseMapper().mapBatchResponse(batchResponse);

        IdentifiableMessage<DeleteMessageResponse> response1 = mappedResponses.get(0);
        IdentifiableMessage<DeleteMessageResponse> response2 = mappedResponses.get(1);
        Assert.assertEquals(id1, response1.id());
        Assert.assertEquals(id2, response2.id());
    }

    @Test
    public void deleteMessageBatchKeyMapperWithoutOverrideConfig_hasSameBatchKey() {
        String queueUrl = "myQueue";
        DeleteMessageRequest request1 = DeleteMessageRequest.builder().queueUrl(queueUrl).build();
        DeleteMessageRequest request2 = DeleteMessageRequest.builder().queueUrl(queueUrl).build();
        String batchKey1 = deleteMessageBatchKeyMapper().getBatchKey(request1);
        String batchKey2 = deleteMessageBatchKeyMapper().getBatchKey(request2);

        Assert.assertEquals(queueUrl, batchKey1);
        Assert.assertEquals(queueUrl, batchKey2);
        Assert.assertEquals(batchKey1, batchKey2);
    }

    @Test
    public void deleteMessageBatchKeyMapperWithOverrideConfig_hasSameBatchKey() {
        String queueUrl = "myQueue";
        AwsRequestOverrideConfiguration overrideConfiguration1 = createOverrideConfig(10);
        AwsRequestOverrideConfiguration overrideConfiguration2 = createOverrideConfig(10);
        DeleteMessageRequest request1 = DeleteMessageRequest.builder()
                                                            .queueUrl(queueUrl)
                                                            .overrideConfiguration(overrideConfiguration1)
                                                            .build();
        DeleteMessageRequest request2 = DeleteMessageRequest.builder()
                                                            .queueUrl(queueUrl)
                                                            .overrideConfiguration(overrideConfiguration2)
                                                            .build();
        String batchKey1 = deleteMessageBatchKeyMapper().getBatchKey(request1);
        String batchKey2 = deleteMessageBatchKeyMapper().getBatchKey(request2);
        Assert.assertEquals(batchKey1, batchKey2);
    }

    @Test
    public void deleteMessageBatchKeyMapperWithDifferentOverrideConfig_hasDifferentBatchKey() {
        String queueUrl = "myQueue";
        AwsRequestOverrideConfiguration overrideConfiguration1 = createOverrideConfig(10);
        AwsRequestOverrideConfiguration overrideConfiguration2 = createOverrideConfig(20);
        DeleteMessageRequest request1 = DeleteMessageRequest.builder()
                                                            .queueUrl(queueUrl)
                                                            .overrideConfiguration(overrideConfiguration1)
                                                            .build();
        DeleteMessageRequest request2 = DeleteMessageRequest.builder()
                                                            .queueUrl(queueUrl)
                                                            .overrideConfiguration(overrideConfiguration2)
                                                            .build();
        String batchKey1 = deleteMessageBatchKeyMapper().getBatchKey(request1);
        String batchKey2 = deleteMessageBatchKeyMapper().getBatchKey(request2);
        Assert.assertNotEquals(batchKey1, batchKey2);
    }

    @Test
    public void changeVisibilityResponseMapper_mapResponsesCorrectly() {
        String id1 = "1";
        String id2 = "2";
        ChangeMessageVisibilityBatchResultEntry entry1 = createChangeVisibilityBatchEntry(id1);
        ChangeMessageVisibilityBatchResultEntry entry2 = createChangeVisibilityBatchEntry(id2);
        ChangeMessageVisibilityBatchResponse batchResponse = ChangeMessageVisibilityBatchResponse.builder()
                                                                                                 .successful(entry1, entry2)
                                                                                                 .build();
        List<IdentifiableMessage<ChangeMessageVisibilityResponse>> mappedResponses =
            changeVisibilityResponseMapper().mapBatchResponse(batchResponse);

        IdentifiableMessage<ChangeMessageVisibilityResponse> response1 = mappedResponses.get(0);
        IdentifiableMessage<ChangeMessageVisibilityResponse> response2 = mappedResponses.get(1);
        Assert.assertEquals(id1, response1.id());
        Assert.assertEquals(id2, response2.id());
    }

    @Test
    public void changeVisibilityBatchKeyMapperWithoutOverrideConfig_hasSameBatchKey() {
        String queueUrl = "myQueue";
        ChangeMessageVisibilityRequest request1 = ChangeMessageVisibilityRequest.builder().queueUrl(queueUrl).build();
        ChangeMessageVisibilityRequest request2 = ChangeMessageVisibilityRequest.builder().queueUrl(queueUrl).build();
        String batchKey1 = changeVisibilityBatchKeyMapper().getBatchKey(request1);
        String batchKey2 = changeVisibilityBatchKeyMapper().getBatchKey(request2);

        Assert.assertEquals(queueUrl, batchKey1);
        Assert.assertEquals(queueUrl, batchKey2);
        Assert.assertEquals(batchKey1, batchKey2);
    }

    @Test
    public void changeVisibilityBatchKeyMapperWithOverrideConfig_hasSameBatchKey() {
        String queueUrl = "myQueue";
        AwsRequestOverrideConfiguration overrideConfiguration1 = createOverrideConfig(10);
        AwsRequestOverrideConfiguration overrideConfiguration2 = createOverrideConfig(10);
        ChangeMessageVisibilityRequest request1 = ChangeMessageVisibilityRequest.builder()
                                                                                .queueUrl(queueUrl)
                                                                                .overrideConfiguration(overrideConfiguration1)
                                                                                .build();
        ChangeMessageVisibilityRequest request2 = ChangeMessageVisibilityRequest.builder()
                                                                                .queueUrl(queueUrl)
                                                                                .overrideConfiguration(overrideConfiguration2)
                                                                                .build();
        String batchKey1 = changeVisibilityBatchKeyMapper().getBatchKey(request1);
        String batchKey2 = changeVisibilityBatchKeyMapper().getBatchKey(request2);
        Assert.assertEquals(batchKey1, batchKey2);
    }

    @Test
    public void changeVisibilityBatchKeyMapperWithDifferentOverrideConfig_hasDifferentBatchKey() {
        String queueUrl = "myQueue";
        AwsRequestOverrideConfiguration overrideConfiguration1 = createOverrideConfig(10);
        AwsRequestOverrideConfiguration overrideConfiguration2 = createOverrideConfig(20);
        ChangeMessageVisibilityRequest request1 = ChangeMessageVisibilityRequest.builder()
                                                                                .queueUrl(queueUrl)
                                                                                .overrideConfiguration(overrideConfiguration1)
                                                                                .build();
        ChangeMessageVisibilityRequest request2 = ChangeMessageVisibilityRequest.builder()
                                                                                .queueUrl(queueUrl)
                                                                                .overrideConfiguration(overrideConfiguration2)
                                                                                .build();
        String batchKey1 = changeVisibilityBatchKeyMapper().getBatchKey(request1);
        String batchKey2 = changeVisibilityBatchKeyMapper().getBatchKey(request2);
        Assert.assertNotEquals(batchKey1, batchKey2);
    }

    private SendMessageBatchResultEntry createSendMessageBatchEntry(String id, String messageBody) {
        return SendMessageBatchResultEntry.builder()
                                          .id(id)
                                          .md5OfMessageBody(messageBody)
                                          .build();
    }

    private DeleteMessageBatchResultEntry createDeleteMessageBatchEntry(String id) {
        return DeleteMessageBatchResultEntry.builder().id(id).build();
    }

    private ChangeMessageVisibilityBatchResultEntry createChangeVisibilityBatchEntry(String id) {
        return ChangeMessageVisibilityBatchResultEntry.builder().id(id).build();
    }

    private AwsRequestOverrideConfiguration createOverrideConfig(int millis) {
        Duration apiCallTimeout = Duration.ofMillis(millis);
        return AwsRequestOverrideConfiguration.builder().apiCallTimeout(apiCallTimeout).build();
    }

    private String getMd5Hash(String message) {
        byte[] expectedMd5;
        expectedMd5 = Md5Utils.computeMD5Hash(message.getBytes(StandardCharsets.UTF_8));
        return BinaryUtils.toHex(expectedMd5);
    }
}
