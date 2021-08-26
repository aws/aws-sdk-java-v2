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
import static software.amazon.awssdk.services.sqs.batchmanager.internal.SqsBatchFunctions.changeMessageVisibilityBatchKeyMapper;
import static software.amazon.awssdk.services.sqs.batchmanager.internal.SqsBatchFunctions.changeMessageVisibilityResponseMapper;
import static software.amazon.awssdk.services.sqs.batchmanager.internal.SqsBatchFunctions.deleteMessageBatchKeyMapper;
import static software.amazon.awssdk.services.sqs.batchmanager.internal.SqsBatchFunctions.deleteMessageResponseMapper;
import static software.amazon.awssdk.services.sqs.batchmanager.internal.SqsBatchFunctions.sendMessageBatchKeyMapper;
import static software.amazon.awssdk.services.sqs.batchmanager.internal.SqsBatchFunctions.sendMessageResponseMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.AwsResponseMetadata;
import software.amazon.awssdk.core.batchmanager.IdentifiableMessage;
import software.amazon.awssdk.http.SdkHttpResponse;
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
import software.amazon.awssdk.utils.Either;
import software.amazon.awssdk.utils.Md5Utils;

public class SqsBatchFunctionsTest {

    @Test
    public void sendMessageResponseMapper_mapResponsesCorrectly() {
        String id1 = "1";
        String id2 = "2";
        String messageBody1 = getMd5Hash("1");
        String messageBody2 = getMd5Hash("2");
        SendMessageBatchResultEntry entry1 = createSendMessageBatchEntry(id1, messageBody1);
        SendMessageBatchResultEntry entry2 = createSendMessageBatchEntry(id2, messageBody2);
        AwsResponseMetadata responseMetadata = createAwsResponseMetadata();
        SdkHttpResponse httpResponse = createSdkHttpResponse();
        SendMessageBatchResponse batchResponse = createSendMessageBatchResponse(responseMetadata, httpResponse, entry1, entry2);
        List<Either<IdentifiableMessage<SendMessageResponse>, IdentifiableMessage<Throwable>>> mappedResponses =
            sendMessageResponseMapper().mapBatchResponse(batchResponse);

        IdentifiableMessage<SendMessageResponse> response1 = mappedResponses.get(0).left().get();
        IdentifiableMessage<SendMessageResponse> response2 = mappedResponses.get(1).left().get();
        assertThat(response1.id()).isEqualTo(id1);
        assertThat(response1.message().md5OfMessageBody()).isEqualTo(messageBody1);
        assertThat(response1.message().sdkHttpResponse()).isEqualTo(httpResponse);
        assertThat(response1.message().responseMetadata().requestId()).isEqualTo(responseMetadata.requestId());
        assertThat(response2.id()).isEqualTo(id2);
        assertThat(response2.message().md5OfMessageBody()).isEqualTo(messageBody2);
        assertThat(response2.message().sdkHttpResponse()).isEqualTo(httpResponse);
        assertThat(response2.message().responseMetadata().requestId()).isEqualTo(responseMetadata.requestId());
    }

    @Test
    public void sendMessageBatchKeyMapperWithoutOverrideConfig_hasSameBatchKey() {
        String queueUrl = "myQueue";
        SendMessageRequest request1 = SendMessageRequest.builder().queueUrl(queueUrl).build();
        SendMessageRequest request2 = SendMessageRequest.builder().queueUrl(queueUrl).build();
        String batchKey1 = sendMessageBatchKeyMapper().getBatchKey(request1);
        String batchKey2 = sendMessageBatchKeyMapper().getBatchKey(request2);

        assertThat(batchKey1).isEqualTo(queueUrl);
        assertThat(batchKey2).isEqualTo(queueUrl);
        assertThat(batchKey1).isEqualTo(batchKey2);
    }

    @Test
    public void sendMessageBatchKeyMapperWithOverrideConfig_hasSameBatchKey() {
        String queueUrl = "myQueue";
        SendMessageRequest request1 = createSendMessageRequestWithOverrideConfig(queueUrl, 10);
        SendMessageRequest request2 = createSendMessageRequestWithOverrideConfig(queueUrl, 10);
        String batchKey1 = sendMessageBatchKeyMapper().getBatchKey(request1);
        String batchKey2 = sendMessageBatchKeyMapper().getBatchKey(request2);
        assertThat(batchKey1).isEqualTo(batchKey2);
    }

    @Test
    public void sendMessageBatchKeyMapperWithDifferentOverrideConfig_hasDifferentBatchKey() {
        String queueUrl = "myQueue";
        SendMessageRequest request1 = createSendMessageRequestWithOverrideConfig(queueUrl, 10);
        SendMessageRequest request2 = createSendMessageRequestWithOverrideConfig(queueUrl, 20);
        String batchKey1 = sendMessageBatchKeyMapper().getBatchKey(request1);
        String batchKey2 = sendMessageBatchKeyMapper().getBatchKey(request2);
        assertThat(batchKey1).isNotEqualTo(batchKey2);
    }

    @Test
    public void deleteMessageResponseMapper_mapResponsesCorrectly() {
        String id1 = "1";
        String id2 = "2";
        DeleteMessageBatchResultEntry entry1 = createDeleteMessageBatchEntry(id1);
        DeleteMessageBatchResultEntry entry2 = createDeleteMessageBatchEntry(id2);
        AwsResponseMetadata responseMetadata = createAwsResponseMetadata();
        SdkHttpResponse httpResponse = createSdkHttpResponse();
        DeleteMessageBatchResponse batchResponse = createDeleteMessageBatchResponse(responseMetadata, httpResponse, entry1,
                                                                                    entry2);
        List<Either<IdentifiableMessage<DeleteMessageResponse>, IdentifiableMessage<Throwable>>> mappedResponses =
            deleteMessageResponseMapper().mapBatchResponse(batchResponse);

        IdentifiableMessage<DeleteMessageResponse> response1 = mappedResponses.get(0).left().get();
        IdentifiableMessage<DeleteMessageResponse> response2 = mappedResponses.get(1).left().get();
        assertThat(response1.id()).isEqualTo(id1);
        assertThat(response1.message().sdkHttpResponse()).isEqualTo(httpResponse);
        assertThat(response1.message().responseMetadata().requestId()).isEqualTo(responseMetadata.requestId());
        assertThat(response2.id()).isEqualTo(id2);
        assertThat(response2.message().sdkHttpResponse()).isEqualTo(httpResponse);
        assertThat(response2.message().responseMetadata().requestId()).isEqualTo(responseMetadata.requestId());
    }

    @Test
    public void deleteMessageBatchKeyMapperWithoutOverrideConfig_hasSameBatchKey() {
        String queueUrl = "myQueue";
        DeleteMessageRequest request1 = DeleteMessageRequest.builder().queueUrl(queueUrl).build();
        DeleteMessageRequest request2 = DeleteMessageRequest.builder().queueUrl(queueUrl).build();
        String batchKey1 = deleteMessageBatchKeyMapper().getBatchKey(request1);
        String batchKey2 = deleteMessageBatchKeyMapper().getBatchKey(request2);

        assertThat(batchKey1).isEqualTo(queueUrl);
        assertThat(batchKey2).isEqualTo(queueUrl);
        assertThat(batchKey1).isEqualTo(batchKey2);
    }

    @Test
    public void deleteMessageBatchKeyMapperWithOverrideConfig_hasSameBatchKey() {
        String queueUrl = "myQueue";
        DeleteMessageRequest request1 = createDeleteMessageRequestWithOverrideConfig(queueUrl, 10);
        DeleteMessageRequest request2 = createDeleteMessageRequestWithOverrideConfig(queueUrl, 10);
        String batchKey1 = deleteMessageBatchKeyMapper().getBatchKey(request1);
        String batchKey2 = deleteMessageBatchKeyMapper().getBatchKey(request2);
        assertThat(batchKey1).isEqualTo(batchKey2);
    }

    @Test
    public void deleteMessageBatchKeyMapperWithDifferentOverrideConfig_hasDifferentBatchKey() {
        String queueUrl = "myQueue";
        DeleteMessageRequest request1 = createDeleteMessageRequestWithOverrideConfig(queueUrl, 10);
        DeleteMessageRequest request2 = createDeleteMessageRequestWithOverrideConfig(queueUrl, 20);
        String batchKey1 = deleteMessageBatchKeyMapper().getBatchKey(request1);
        String batchKey2 = deleteMessageBatchKeyMapper().getBatchKey(request2);
        assertThat(batchKey1).isNotEqualTo(batchKey2);
    }

    @Test
    public void changeVisibilityResponseMapper_mapResponsesCorrectly() {
        String id1 = "1";
        String id2 = "2";
        ChangeMessageVisibilityBatchResultEntry entry1 = createChangeVisibilityBatchEntry(id1);
        ChangeMessageVisibilityBatchResultEntry entry2 = createChangeVisibilityBatchEntry(id2);
        AwsResponseMetadata responseMetadata = createAwsResponseMetadata();
        SdkHttpResponse httpResponse = createSdkHttpResponse();
        ChangeMessageVisibilityBatchResponse batchResponse = createChangeVisibilityBatchResponse(responseMetadata, httpResponse,
                                                                                                 entry1, entry2);
        List<Either<IdentifiableMessage<ChangeMessageVisibilityResponse>, IdentifiableMessage<Throwable>>> mappedResponses =
            changeMessageVisibilityResponseMapper().mapBatchResponse(batchResponse);

        IdentifiableMessage<ChangeMessageVisibilityResponse> response1 = mappedResponses.get(0).left().get();
        IdentifiableMessage<ChangeMessageVisibilityResponse> response2 = mappedResponses.get(1).left().get();
        assertThat(response1.id()).isEqualTo(id1);
        assertThat(response1.message().sdkHttpResponse()).isEqualTo(httpResponse);
        assertThat(response1.message().responseMetadata().requestId()).isEqualTo(responseMetadata.requestId());
        assertThat(response2.id()).isEqualTo(id2);
        assertThat(response2.message().sdkHttpResponse()).isEqualTo(httpResponse);
        assertThat(response2.message().responseMetadata().requestId()).isEqualTo(responseMetadata.requestId());
    }

    @Test
    public void changeVisibilityBatchKeyMapperWithoutOverrideConfig_hasSameBatchKey() {
        String queueUrl = "myQueue";
        ChangeMessageVisibilityRequest request1 = ChangeMessageVisibilityRequest.builder().queueUrl(queueUrl).build();
        ChangeMessageVisibilityRequest request2 = ChangeMessageVisibilityRequest.builder().queueUrl(queueUrl).build();
        String batchKey1 = changeMessageVisibilityBatchKeyMapper().getBatchKey(request1);
        String batchKey2 = changeMessageVisibilityBatchKeyMapper().getBatchKey(request2);

        assertThat(batchKey1).isEqualTo(queueUrl);
        assertThat(batchKey2).isEqualTo(queueUrl);
        assertThat(batchKey1).isEqualTo(batchKey2);
    }

    @Test
    public void changeVisibilityBatchKeyMapperWithOverrideConfig_hasSameBatchKey() {
        String queueUrl = "myQueue";
        ChangeMessageVisibilityRequest request1 = createChangeVisibilityRequestWithOverrideConfig(queueUrl, 10);
        ChangeMessageVisibilityRequest request2 = createChangeVisibilityRequestWithOverrideConfig(queueUrl, 10);
        String batchKey1 = changeMessageVisibilityBatchKeyMapper().getBatchKey(request1);
        String batchKey2 = changeMessageVisibilityBatchKeyMapper().getBatchKey(request2);
        assertThat(batchKey1).isEqualTo(batchKey2);
    }

    @Test
    public void changeVisibilityBatchKeyMapperWithDifferentOverrideConfig_hasDifferentBatchKey() {
        String queueUrl = "myQueue";
        ChangeMessageVisibilityRequest request1 = createChangeVisibilityRequestWithOverrideConfig(queueUrl, 10);
        ChangeMessageVisibilityRequest request2 = createChangeVisibilityRequestWithOverrideConfig(queueUrl, 20);
        String batchKey1 = changeMessageVisibilityBatchKeyMapper().getBatchKey(request1);
        String batchKey2 = changeMessageVisibilityBatchKeyMapper().getBatchKey(request2);
        assertThat(batchKey1).isNotEqualTo(batchKey2);
    }

    private SendMessageRequest createSendMessageRequestWithOverrideConfig(String queueUrl, int millis) {
        AwsRequestOverrideConfiguration overrideConfiguration = createOverrideConfig(millis);
        return SendMessageRequest.builder()
                                 .queueUrl(queueUrl)
                                 .overrideConfiguration(overrideConfiguration)
                                 .build();
    }

    private DeleteMessageRequest createDeleteMessageRequestWithOverrideConfig(String queueUrl, int millis) {
        AwsRequestOverrideConfiguration overrideConfiguration = createOverrideConfig(millis);
        return DeleteMessageRequest.builder()
                                   .queueUrl(queueUrl)
                                   .overrideConfiguration(overrideConfiguration)
                                   .build();
    }

    private ChangeMessageVisibilityRequest createChangeVisibilityRequestWithOverrideConfig(String queueUrl, int millis) {
        AwsRequestOverrideConfiguration overrideConfiguration = createOverrideConfig(millis);
        return ChangeMessageVisibilityRequest.builder()
                                             .queueUrl(queueUrl)
                                             .overrideConfiguration(overrideConfiguration)
                                             .build();
    }

    private SendMessageBatchResponse createSendMessageBatchResponse(AwsResponseMetadata responseMetadata,
                                                                    SdkHttpResponse httpResponse,
                                                                    SendMessageBatchResultEntry... entries) {
        return (SendMessageBatchResponse) SendMessageBatchResponse.builder()
                                                                  .successful(entries)
                                                                  .responseMetadata(responseMetadata)
                                                                  .sdkHttpResponse(httpResponse)
                                                                  .build();
    }

    private DeleteMessageBatchResponse createDeleteMessageBatchResponse(AwsResponseMetadata responseMetadata,
                                                                        SdkHttpResponse httpResponse,
                                                                        DeleteMessageBatchResultEntry... entries) {
        return (DeleteMessageBatchResponse) DeleteMessageBatchResponse.builder()
                                                                      .successful(entries)
                                                                      .responseMetadata(responseMetadata)
                                                                      .sdkHttpResponse(httpResponse)
                                                                      .build();
    }

    private ChangeMessageVisibilityBatchResponse createChangeVisibilityBatchResponse(AwsResponseMetadata responseMetadata,
                                                                                     SdkHttpResponse httpResponse,
                                                                                     ChangeMessageVisibilityBatchResultEntry...
                                                                                         entries) {
        return (ChangeMessageVisibilityBatchResponse) ChangeMessageVisibilityBatchResponse.builder()
                                                                                          .successful(entries)
                                                                                          .responseMetadata(responseMetadata)
                                                                                          .sdkHttpResponse(httpResponse)
                                                                                          .build();
    }

    private AwsResponseMetadata createAwsResponseMetadata() {
        Map<String, String> metadataMap = new HashMap<>();
        metadataMap.put("data", "metadata");
        return new AwsResponseMetadata(metadataMap) {};
    }

    private SdkHttpResponse createSdkHttpResponse() {
        return SdkHttpResponse.builder().putHeader("content", "content").build();
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
