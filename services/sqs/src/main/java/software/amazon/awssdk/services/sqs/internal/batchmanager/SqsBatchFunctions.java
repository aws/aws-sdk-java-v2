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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.internal.batchmanager.BatchAndSend;
import software.amazon.awssdk.core.internal.batchmanager.BatchKeyMapper;
import software.amazon.awssdk.core.internal.batchmanager.BatchResponseMapper;
import software.amazon.awssdk.core.internal.batchmanager.IdentifiableMessage;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;
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

@SdkInternalApi
public final class SqsBatchFunctions {

    private SqsBatchFunctions() {
    }

    public static BatchAndSend<SendMessageRequest, SendMessageBatchResponse> sendMessageBatchFunction(SqsClient client,
                                                                                                      ExecutorService executor) {
        return (identifiedRequests, batchKey) -> {
            SendMessageBatchRequest batchRequest = createSendMessageBatchRequest(identifiedRequests, batchKey);
            return CompletableFuture.supplyAsync(() -> client.sendMessageBatch(batchRequest), executor);
        };
    }

    public static BatchAndSend<SendMessageRequest, SendMessageBatchResponse> sendMessageBatchAsyncFunction(SqsAsyncClient
                                                                                                               client) {
        return (identifiedRequests, batchKey) -> {
            SendMessageBatchRequest batchRequest = createSendMessageBatchRequest(identifiedRequests, batchKey);
            return client.sendMessageBatch(batchRequest);
        };
    }

    public static SendMessageBatchRequest createSendMessageBatchRequest(
        List<IdentifiableMessage<SendMessageRequest>> identifiedRequests, String batchKey) {
        List<SendMessageBatchRequestEntry> entries =
            identifiedRequests.stream()
                              .map(identifiedRequest -> createSendMessageBatchRequestEntry(identifiedRequest.id(),
                                                                                           identifiedRequest.message()))
                              .collect(Collectors.toList());

        // Since requests are batched together according to a combination of their queueUrl and overrideConfiguration, all
        // requests must have the same overrideConfiguration so it is sufficient to retrieve it from the first request.
        Optional<AwsRequestOverrideConfiguration> overrideConfiguration = identifiedRequests.get(0)
                                                                                            .message()
                                                                                            .overrideConfiguration();
        return overrideConfiguration.map(overrideConfig -> SendMessageBatchRequest.builder()
                                                                                  .queueUrl(batchKey)
                                                                                  .overrideConfiguration(overrideConfig)
                                                                                  .entries(entries)
                                                                                  .build())
                                    .orElse(SendMessageBatchRequest.builder()
                                                                   .queueUrl(batchKey)
                                                                   .entries(entries)
                                                                   .build());
    }

    public static BatchResponseMapper<SendMessageBatchResponse, SendMessageResponse> sendMessageResponseMapper() {
        return sendMessageBatchResponse -> {
            List<IdentifiableMessage<SendMessageResponse>> mappedResponses = new ArrayList<>();
            sendMessageBatchResponse.successful()
                                    .forEach(batchResponseEntry -> {
                                        String key = batchResponseEntry.id();
                                        SendMessageResponse response = createSendMessageResponse(batchResponseEntry,
                                                                                                 sendMessageBatchResponse);
                                        mappedResponses.add(new IdentifiableMessage<>(key, response));
                                    });
            sendMessageBatchResponse.failed()
                                    .forEach(batchResponseEntry -> {
                                        String key = batchResponseEntry.id();
                                        SendMessageResponse response = createSendMessageResponse();
                                        mappedResponses.add(new IdentifiableMessage<>(key, response));
                                    });
            return mappedResponses;
        };
    }

    // TODO: The BatchKeyMappers for SQS is not set in stone. Could batch requests some other way.
    public static BatchKeyMapper<SendMessageRequest> sendMessageBatchKeyMapper() {
        return request -> request.overrideConfiguration()
                                 .map(overrideConfig -> request.queueUrl() + overrideConfig.hashCode())
                                 .orElse(request.queueUrl());
    }

    public static BatchAndSend<DeleteMessageRequest, DeleteMessageBatchResponse> deleteMessageBatchFunction(
        SqsClient client, ExecutorService executor) {
        return (identifiedRequests, batchKey) -> {
            DeleteMessageBatchRequest batchRequest = createDeleteMessageBatchRequest(identifiedRequests, batchKey);
            return CompletableFuture.supplyAsync(() -> client.deleteMessageBatch(batchRequest), executor);
        };
    }

    public static BatchAndSend<DeleteMessageRequest, DeleteMessageBatchResponse> deleteMessageBatchAsyncFunction(
        SqsAsyncClient client) {
        return (identifiedRequests, batchKey) -> {
            DeleteMessageBatchRequest batchRequest = createDeleteMessageBatchRequest(identifiedRequests, batchKey);
            return client.deleteMessageBatch(batchRequest);
        };
    }

    public static DeleteMessageBatchRequest createDeleteMessageBatchRequest(
        List<IdentifiableMessage<DeleteMessageRequest>> identifiedRequests, String batchKey) {
        List<DeleteMessageBatchRequestEntry> entries =
            identifiedRequests.stream()
                              .map(identifiedRequest -> createDeleteMessageBatchRequestEntry(identifiedRequest.id(),
                                                                                             identifiedRequest.message()))
                              .collect(Collectors.toList());

        // Since requests are batched together according to a combination of their queueUrl and overrideConfiguration, all
        // requests must have the same overrideConfiguration so it is sufficient to retrieve it from the first request.
        Optional<AwsRequestOverrideConfiguration> overrideConfiguration = identifiedRequests.get(0)
                                                                                            .message()
                                                                                            .overrideConfiguration();
        return overrideConfiguration.map(overrideConfig -> DeleteMessageBatchRequest.builder()
                                                                                    .queueUrl(batchKey)
                                                                                    .overrideConfiguration(overrideConfig)
                                                                                    .entries(entries)
                                                                                    .build())
                                    .orElse(DeleteMessageBatchRequest.builder()
                                                                     .queueUrl(batchKey)
                                                                     .entries(entries)
                                                                     .build());

    }

    public static BatchResponseMapper<DeleteMessageBatchResponse, DeleteMessageResponse> deleteMessageResponseMapper() {
        return deleteMessageBatchResponse -> {
            List<IdentifiableMessage<DeleteMessageResponse>> mappedResponses = new ArrayList<>();
            deleteMessageBatchResponse.successful()
                                      .forEach(batchResponseEntry -> {
                                          String key = batchResponseEntry.id();
                                          DeleteMessageResponse response =
                                              createDeleteMessageResponse(deleteMessageBatchResponse);
                                          mappedResponses.add(new IdentifiableMessage<>(key, response));
                                      });
            deleteMessageBatchResponse.failed()
                                      .forEach(batchResponseEntry -> {
                                          String key = batchResponseEntry.id();
                                          DeleteMessageResponse response =
                                              createDeleteMessageResponse(deleteMessageBatchResponse);
                                          mappedResponses.add(new IdentifiableMessage<>(key, response));
                                      });
            return mappedResponses;
        };
    }

    public static BatchKeyMapper<DeleteMessageRequest> deleteMessageBatchKeyMapper() {
        return request -> request.overrideConfiguration()
                                 .map(overrideConfig -> request.queueUrl() + overrideConfig.hashCode())
                                 .orElse(request.queueUrl());
    }

    public static BatchAndSend<ChangeMessageVisibilityRequest, ChangeMessageVisibilityBatchResponse>
        changeVisibilityBatchFunction(SqsClient client, ExecutorService executor) {
        return (identifiedRequests, batchKey) -> {
            ChangeMessageVisibilityBatchRequest batchRequest = createChangeVisibilityBatchRequest(identifiedRequests, batchKey);
            return CompletableFuture.supplyAsync(() -> client.changeMessageVisibilityBatch(batchRequest), executor);
        };
    }

    public static BatchAndSend<ChangeMessageVisibilityRequest, ChangeMessageVisibilityBatchResponse>
        changeVisibilityBatchAsyncFunction(SqsAsyncClient client) {
        return (identifiedRequests, batchKey) -> {
            ChangeMessageVisibilityBatchRequest batchRequest = createChangeVisibilityBatchRequest(identifiedRequests, batchKey);
            return client.changeMessageVisibilityBatch(batchRequest);
        };
    }

    public static ChangeMessageVisibilityBatchRequest createChangeVisibilityBatchRequest(
        List<IdentifiableMessage<ChangeMessageVisibilityRequest>> identifiedRequests, String batchKey) {
        List<ChangeMessageVisibilityBatchRequestEntry> entries =
            identifiedRequests.stream()
                              .map(identifiedRequest -> createChangVisibilityBatchRequestEntry(identifiedRequest.id(),
                                                                                               identifiedRequest.message()))
                              .collect(Collectors.toList());

        // Since requests are batched together according to a combination of their queueUrl and overrideConfiguration, all
        // requests must have the same overrideConfiguration so it is sufficient to retrieve it from the first request.
        Optional<AwsRequestOverrideConfiguration> overrideConfiguration = identifiedRequests.get(0)
                                                                                            .message()
                                                                                            .overrideConfiguration();
        return overrideConfiguration.map(overrideConfig ->
                                             ChangeMessageVisibilityBatchRequest.builder()
                                                                                .queueUrl(batchKey)
                                                                                .overrideConfiguration(overrideConfig)
                                                                                .entries(entries)
                                                                                .build())
                                    .orElse(ChangeMessageVisibilityBatchRequest.builder()
                                                                               .queueUrl(batchKey)
                                                                               .entries(entries)
                                                                               .build());
    }

    public static BatchResponseMapper<ChangeMessageVisibilityBatchResponse, ChangeMessageVisibilityResponse>
        changeVisibilityResponseMapper() {
        return changeMessageVisibilityResponses -> {
            List<IdentifiableMessage<ChangeMessageVisibilityResponse>> mappedResponses = new ArrayList<>();
            changeMessageVisibilityResponses.successful()
                                            .forEach(batchResponseEntry -> {
                                                String key = batchResponseEntry.id();
                                                ChangeMessageVisibilityResponse response =
                                                    createChangeVisibilityResponse(changeMessageVisibilityResponses);
                                                mappedResponses.add(new IdentifiableMessage<>(key, response));
                                            });
            changeMessageVisibilityResponses.failed()
                                            .forEach(batchResponseEntry -> {
                                                String key = batchResponseEntry.id();
                                                ChangeMessageVisibilityResponse response =
                                                    createChangeVisibilityResponse(changeMessageVisibilityResponses);
                                                mappedResponses.add(new IdentifiableMessage<>(key, response));
                                            });
            return mappedResponses;
        };
    }

    public static BatchKeyMapper<ChangeMessageVisibilityRequest> changeVisibilityBatchKeyMapper() {
        return request -> request.overrideConfiguration()
                                 .map(overrideConfig -> request.queueUrl() + overrideConfig.hashCode())
                                 .orElse(request.queueUrl());
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

    private static SendMessageResponse createSendMessageResponse(SendMessageBatchResultEntry successfulEntry,
                                                                 SendMessageBatchResponse batchResponse) {
        SendMessageResponse.Builder builder = SendMessageResponse.builder()
                                                                 .md5OfMessageAttributes(successfulEntry.md5OfMessageAttributes())
                                                                 .md5OfMessageBody(successfulEntry.md5OfMessageBody())
                                                                 .md5OfMessageSystemAttributes(
                                                                     successfulEntry.md5OfMessageSystemAttributes())
                                                                 .messageId(successfulEntry.messageId())
                                                                 .sequenceNumber(successfulEntry.sequenceNumber());
        if (batchResponse.responseMetadata() != null) {
            builder.responseMetadata(batchResponse.responseMetadata());
        }

        if (batchResponse.sdkHttpResponse() != null) {
            builder.sdkHttpResponse(batchResponse.sdkHttpResponse());
        }
        return builder.build();
    }

    private static SendMessageResponse createSendMessageResponse() {
        // TODO: Return an exception instead of filling a sendMessageResponse with the error message.
        return SendMessageResponse.builder().build();
    }

    private static DeleteMessageBatchRequestEntry createDeleteMessageBatchRequestEntry(String id, DeleteMessageRequest request) {
        return DeleteMessageBatchRequestEntry.builder()
                                             .id(id)
                                             .receiptHandle(request.receiptHandle())
                                             .build();
    }

    private static DeleteMessageResponse createDeleteMessageResponse(DeleteMessageBatchResponse batchResponse) {
        DeleteMessageResponse.Builder builder = DeleteMessageResponse.builder();

        if (batchResponse.responseMetadata() != null) {
            builder.responseMetadata(batchResponse.responseMetadata());
        }

        if (batchResponse.sdkHttpResponse() != null) {
            builder.sdkHttpResponse(batchResponse.sdkHttpResponse());
        }
        return builder.build();
    }

    private static ChangeMessageVisibilityBatchRequestEntry createChangVisibilityBatchRequestEntry(String id,
                                                                                         ChangeMessageVisibilityRequest request) {
        return ChangeMessageVisibilityBatchRequestEntry.builder()
                                                       .id(id)
                                                       .receiptHandle(request.receiptHandle())
                                                       .visibilityTimeout(request.visibilityTimeout())
                                                       .build();
    }

    private static ChangeMessageVisibilityResponse createChangeVisibilityResponse(ChangeMessageVisibilityBatchResponse
                                                                                      batchResponse) {
        ChangeMessageVisibilityResponse.Builder builder = ChangeMessageVisibilityResponse.builder();

        if (batchResponse.responseMetadata() != null) {
            builder.responseMetadata(batchResponse.responseMetadata());
        }

        if (batchResponse.sdkHttpResponse() != null) {
            builder.sdkHttpResponse(batchResponse.sdkHttpResponse());
        }
        return builder.build();
    }
}
