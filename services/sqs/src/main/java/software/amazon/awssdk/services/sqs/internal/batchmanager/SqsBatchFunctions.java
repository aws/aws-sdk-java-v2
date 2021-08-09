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

package software.amazon.awssdk.services.sqs.internal.batchmanager;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.internal.batchmanager.BatchAndSend;
import software.amazon.awssdk.core.internal.batchmanager.BatchKeyMapper;
import software.amazon.awssdk.core.internal.batchmanager.BatchResponseMapper;
import software.amazon.awssdk.core.internal.batchmanager.IdentifiableMessage;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.BatchResultErrorEntry;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchResponse;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResultEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.Md5Utils;

@SdkInternalApi
public final class SqsBatchFunctions {

    private SqsBatchFunctions() {
    }

    public static BatchAndSend<SendMessageRequest, SendMessageBatchResponse> sendMessageBatchFunction(SqsClient client) {
        return (identifiedRequests, destination) -> {
            List<SendMessageBatchRequestEntry> entries = new ArrayList<>(identifiedRequests.size());
            identifiedRequests.forEach(identifiedRequest -> {
                String id = identifiedRequest.id();
                SendMessageRequest request = identifiedRequest.message();
                entries.add(createSendMessageBatchRequestEntry(id, request));
            });
            // TODO: If the individual requests have an override configuration, should we pass it to the batch request builder as well?
            //  If so, how should we do it? Just take the override configuration of the first individual request (since theoretically
            //  they should all have the same override configuration).
            Optional<AwsRequestOverrideConfiguration> overrideConfiguration = identifiedRequests.get(0)
                                                                                                .message()
                                                                                                .overrideConfiguration();
            SendMessageBatchRequest batchRequest;
            if (overrideConfiguration.isPresent()) {
                batchRequest = SendMessageBatchRequest.builder()
                                                      .queueUrl(destination)
                                                      .overrideConfiguration(overrideConfiguration.get())
                                                      .entries(entries)
                                                      .build();
            } else {
                batchRequest = SendMessageBatchRequest.builder()
                                                      .queueUrl(destination)
                                                      .entries(entries)
                                                      .build();
            }
            // TODO: Pass client executor in supplyAsync once an executor is added into the client.
            return CompletableFuture.supplyAsync(() -> client.sendMessageBatch(batchRequest));
        };
    }

    public static BatchResponseMapper<SendMessageBatchResponse, SendMessageResponse> sendMessageResponseMapper() {
     return sendMessageBatchResponse -> {
         List<IdentifiableMessage<SendMessageResponse>> mappedResponses = new ArrayList<>();
         sendMessageBatchResponse.successful()
                                 .forEach(batchResponseEntry -> {
                                     String key = batchResponseEntry.id();
                                     SendMessageResponse response = createSendMessageResponse(batchResponseEntry);
                                     mappedResponses.add(new IdentifiableMessage<>(key, response));
                                 });
         sendMessageBatchResponse.failed()
                                 .forEach(batchResponseEntry -> {
                                     String key = batchResponseEntry.id();
                                     SendMessageResponse response = createSendMessageResponse(batchResponseEntry);
                                     mappedResponses.add(new IdentifiableMessage<>(key, response));
                                 });
         return mappedResponses;
     };
    }

    // TODO: This BatchKeyMapper for SQS is temporary. We have not decided on how to properly group batch requests
    public static BatchKeyMapper<SendMessageRequest> sendMessageBatchKeyMapper() {
        return request -> {
            if (request.overrideConfiguration().isPresent()) {
                return request.queueUrl() + request.overrideConfiguration().get();
            } else {
                return request.queueUrl();
            }
        };
    }

    public static BatchAndSend<DeleteMessageRequest, DeleteMessageBatchResponse> deleteMessageBatchFunction(SqsClient client) {
        return (identifiedRequests, destination) -> {
            List<DeleteMessageBatchRequestEntry> entries = new ArrayList<>(identifiedRequests.size());
            identifiedRequests.forEach(identifiedRequest -> {
                String id = identifiedRequest.id();
                DeleteMessageRequest request = identifiedRequest.message();
                entries.add(createDeleteMessageBatchRequestEntry(id, request));
            });
            // TODO: Fix overrideConfiguration.
            Optional<AwsRequestOverrideConfiguration> overrideConfiguration = identifiedRequests.get(0)
                                                                                                .message()
                                                                                                .overrideConfiguration();
            DeleteMessageBatchRequest batchRequest;
            if (overrideConfiguration.isPresent()) {
                batchRequest = DeleteMessageBatchRequest.builder()
                                                        .entries(entries)
                                                        .overrideConfiguration(overrideConfiguration.get())
                                                        .queueUrl(destination)
                                                        .build();
            } else {
                batchRequest = DeleteMessageBatchRequest.builder()
                                                        .entries(entries)
                                                        .queueUrl(destination)
                                                        .build();
            }
            // TODO: Pass client executor in supplyAsync once an executor is added into the client.
            return CompletableFuture.supplyAsync(() -> client.deleteMessageBatch(batchRequest));
        };
    }

    // TODO: How should we properly map a deleteMessageResponse? Seems like a DeleteMessageResponse builder doesn't really take
    //  any fields.
    public static BatchResponseMapper<DeleteMessageBatchResponse, DeleteMessageResponse> deleteMessageResponseMapper() {
        return deleteMessageBatchResponse -> {
            List<IdentifiableMessage<DeleteMessageResponse>> mappedResponses = new ArrayList<>();
            deleteMessageBatchResponse.successful()
                                      .forEach(batchResponseEntry -> {
                                          String key = batchResponseEntry.id();
                                          DeleteMessageResponse response = createDeleteMessageResponse();
                                          mappedResponses.add(new IdentifiableMessage<>(key, response));
                                      });
            deleteMessageBatchResponse.failed()
                                      .forEach(batchResponseEntry -> {
                                          String key = batchResponseEntry.id();
                                          DeleteMessageResponse response = createDeleteMessageResponse();
                                          mappedResponses.add(new IdentifiableMessage<>(key, response));
                                      });
            return mappedResponses;
        };
    }

    // TODO: Might be able to combine all batchKeyMapper functions if they are all the same. Only problem is they take
    //    //  different types as parameters so maybe not?
    public static BatchKeyMapper<DeleteMessageRequest> deleteMessageBatchKeyMapper() {
        return request -> {
            if (request.overrideConfiguration().isPresent()) {
                return request.queueUrl() + request.overrideConfiguration().get();
            } else {
                return request.queueUrl();
            }
        };
    }

    public static BatchAndSend<ChangeMessageVisibilityRequest, ChangeMessageVisibilityBatchResponse>
    changeVisibilityBatchFunction(SqsClient client) {
        return (identifiedRequests, destination) -> {
            List<ChangeMessageVisibilityBatchRequestEntry> entries = new ArrayList<>(identifiedRequests.size());
            identifiedRequests.forEach(identifiedRequest -> {
                String id = identifiedRequest.id();
                ChangeMessageVisibilityRequest request = identifiedRequest.message();
                entries.add(createChangVisibilityBatchRequestEntry(id, request));
            });
            // TODO: Fix overrideConfiguration.
            Optional<AwsRequestOverrideConfiguration> overrideConfiguration = identifiedRequests.get(0)
                                                                                                .message()
                                                                                                .overrideConfiguration();
            ChangeMessageVisibilityBatchRequest batchRequest;
            if (overrideConfiguration.isPresent()) {
                batchRequest = ChangeMessageVisibilityBatchRequest.builder()
                                                                  .entries(entries)
                                                                  .overrideConfiguration(overrideConfiguration.get())
                                                                  .queueUrl(destination)
                                                                  .build();
            } else {
                batchRequest = ChangeMessageVisibilityBatchRequest.builder()
                                                                  .entries(entries)
                                                                  .queueUrl(destination)
                                                                  .build();
            }
            // TODO: Pass client executor in supplyAsync once an executor is added into the client.
            return CompletableFuture.supplyAsync(() -> client.changeMessageVisibilityBatch(batchRequest));
        };
    }

    // TODO: Same problem with mapping ChangeMessageVisibilityResponse as with DeleteMessageResponse.
    public static BatchResponseMapper<ChangeMessageVisibilityBatchResponse, ChangeMessageVisibilityResponse>
    changeVisibilityResponseMapper() {
        return changeMessageVisibilityResponses -> {
            List<IdentifiableMessage<ChangeMessageVisibilityResponse>> mappedResponses = new ArrayList<>();
            changeMessageVisibilityResponses.successful()
                                            .forEach(batchResponseEntry -> {
                                                String key = batchResponseEntry.id();
                                                ChangeMessageVisibilityResponse response = createChangeVisibilityResponse();
                                                mappedResponses.add(new IdentifiableMessage<>(key, response));
                                            });
            changeMessageVisibilityResponses.failed()
                                            .forEach(batchResponseEntry -> {
                                                String key = batchResponseEntry.id();
                                                ChangeMessageVisibilityResponse response = createChangeVisibilityResponse();
                                                mappedResponses.add(new IdentifiableMessage<>(key, response));
                                            });
            return mappedResponses;
        };
    }

    // TODO: Might be able to combine all batchKeyMapper functions if they are all the same. Only problem is they take
    //  different types as parameters so maybe not?
    public static BatchKeyMapper<ChangeMessageVisibilityRequest> changeVisibilityBatchKeyMapper() {
        return request -> {
            if (request.overrideConfiguration().isPresent()) {
                return request.queueUrl() + request.overrideConfiguration().get();
            } else {
                return request.queueUrl();
            }
        };
    }

    private static SendMessageBatchRequestEntry createSendMessageBatchRequestEntry(String id, SendMessageRequest request) {
        return SendMessageBatchRequestEntry.builder()
                                           .id(id)
                                           .messageBody(request.messageBody())
                                           .delaySeconds(request.delaySeconds())
                                           .messageAttributes(request.messageAttributes())
                                           .messageDeduplicationId(request.messageDeduplicationId())
                                           .messageGroupId(request.messageGroupId())
                                           .messageSystemAttributes(request.messageSystemAttributes())
                                           .build();
    }

    private static SendMessageResponse createSendMessageResponse(SendMessageBatchResultEntry successfulEntry) {
        return SendMessageResponse.builder()
                                  .md5OfMessageAttributes(successfulEntry.md5OfMessageAttributes())
                                  .md5OfMessageBody(successfulEntry.md5OfMessageBody())
                                  .md5OfMessageSystemAttributes(successfulEntry.md5OfMessageSystemAttributes())
                                  .messageId(successfulEntry.messageId())
                                  .sequenceNumber(successfulEntry.sequenceNumber())
                                  .build();
    }

    private static SendMessageResponse createSendMessageResponse(BatchResultErrorEntry failedEntry) {
        String messageBody = String.format("%s: %s", failedEntry.code(), failedEntry.message());
        return SendMessageResponse.builder()
                                  .md5OfMessageBody(computeMd5Hash(messageBody))
                                  .build();
    }

    private static DeleteMessageBatchRequestEntry createDeleteMessageBatchRequestEntry(String id, DeleteMessageRequest request) {
        return DeleteMessageBatchRequestEntry.builder()
                                             .id(id)
                                             .receiptHandle(request.receiptHandle())
                                             .build();
    }

    private static ChangeMessageVisibilityBatchRequestEntry createChangVisibilityBatchRequestEntry(String id,
                                                                                         ChangeMessageVisibilityRequest request) {
        return ChangeMessageVisibilityBatchRequestEntry.builder()
                                                       .id(id)
                                                       .receiptHandle(request.receiptHandle())
                                                       .visibilityTimeout(request.visibilityTimeout())
                                                       .build();
    }

    private static DeleteMessageResponse createDeleteMessageResponse() {
        return DeleteMessageResponse.builder().build();
    }

    private static ChangeMessageVisibilityResponse createChangeVisibilityResponse() {
        return ChangeMessageVisibilityResponse.builder().build();
    }

    private static String computeMd5Hash(String message) {
        byte[] expectedMd5;
        expectedMd5 = Md5Utils.computeMD5Hash(message.getBytes(StandardCharsets.UTF_8));
        return BinaryUtils.toHex(expectedMd5);
    }
}
