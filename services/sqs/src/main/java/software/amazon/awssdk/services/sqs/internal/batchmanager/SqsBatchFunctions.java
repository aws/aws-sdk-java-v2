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
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.internal.batchmanager.core.BatchAndSend;
import software.amazon.awssdk.services.sqs.internal.batchmanager.core.BatchKeyMapper;
import software.amazon.awssdk.services.sqs.internal.batchmanager.core.BatchResponseMapper;
import software.amazon.awssdk.services.sqs.internal.batchmanager.core.IdentifiableMessage;
import software.amazon.awssdk.services.sqs.model.BatchResultErrorEntry;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchResponse;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchResultEntry;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchResultEntry;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResultEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;
import software.amazon.awssdk.utils.Either;

@SdkInternalApi
public final class SqsBatchFunctions {
    private SqsBatchFunctions() {
    }

    public static BatchAndSend<SendMessageRequest, SendMessageBatchResponse> sendMessageBatchFunction(SqsClient client,
                                                                                                      Executor executor) {
        return (identifiedRequests, batchKey) -> {
            SendMessageBatchRequest batchRequest = createSendMessageBatchRequest(identifiedRequests, batchKey);
            return CompletableFuture.supplyAsync(() -> client.sendMessageBatch(batchRequest), executor);
        };
    }

    public static BatchAndSend<SendMessageRequest,
        SendMessageBatchResponse> sendMessageBatchAsyncFunction(SqsAsyncClient client) {
        return (identifiedRequests, batchKey) -> {
            SendMessageBatchRequest batchRequest = createSendMessageBatchRequest(identifiedRequests, batchKey);
            return client.sendMessageBatch(batchRequest);
        };
    }

    private static SendMessageBatchRequest createSendMessageBatchRequest(
        List<IdentifiableMessage<SendMessageRequest>> identifiedRequests, String batchKey) {
        List<SendMessageBatchRequestEntry> entries = identifiedRequests
                .stream()
                .map(identifiedRequest -> createSendMessageBatchRequestEntry(identifiedRequest.id(), identifiedRequest.message()))
                .collect(Collectors.toList());
        // Since requests are batched together according to a combination of their queueUrl and overrideConfiguration,
        // all requests must have the same overrideConfiguration so it is sufficient to retrieve it from the first
        // request.
        Optional<AwsRequestOverrideConfiguration> overrideConfiguration = identifiedRequests.get(0).message()
                .overrideConfiguration();
        return overrideConfiguration.map(
                overrideConfig -> SendMessageBatchRequest.builder().queueUrl(batchKey).overrideConfiguration(overrideConfig)
                        .entries(entries).build()).orElse(
                SendMessageBatchRequest.builder().queueUrl(batchKey).entries(entries).build());
    }

    private static SendMessageBatchRequestEntry createSendMessageBatchRequestEntry(String id, SendMessageRequest request) {
        return SendMessageBatchRequestEntry.builder().id(id).messageBody(request.messageBody())
                .delaySeconds(request.delaySeconds()).messageAttributes(request.messageAttributes())
                .messageSystemAttributesWithStrings(request.messageSystemAttributesAsStrings())
                .messageDeduplicationId(request.messageDeduplicationId()).messageGroupId(request.messageGroupId()).build();
    }

    public static BatchResponseMapper<SendMessageBatchResponse, SendMessageResponse> sendMessageResponseMapper() {
        return batchResponse -> {
            List<Either<IdentifiableMessage<SendMessageResponse>, IdentifiableMessage<Throwable>>> mappedResponses =
                new ArrayList<>();
            batchResponse.successful().forEach(batchResponseEntry -> {
                IdentifiableMessage<SendMessageResponse> response = createSendMessageResponse(batchResponseEntry, batchResponse);
                mappedResponses.add(Either.left(response));
            });
            batchResponse.failed().forEach(batchResponseEntry -> {
                IdentifiableMessage<Throwable> response = sendMessageCreateThrowable(batchResponseEntry);
                mappedResponses.add(Either.right(response));
            });
            return mappedResponses;
        };
    }

    private static IdentifiableMessage<SendMessageResponse> createSendMessageResponse(
            SendMessageBatchResultEntry successfulEntry, SendMessageBatchResponse batchResponse) {
        String key = successfulEntry.id();
        SendMessageResponse.Builder builder = SendMessageResponse.builder().md5OfMessageBody(successfulEntry.md5OfMessageBody())
                .md5OfMessageAttributes(successfulEntry.md5OfMessageAttributes())
                .md5OfMessageSystemAttributes(successfulEntry.md5OfMessageSystemAttributes())
                .messageId(successfulEntry.messageId()).sequenceNumber(successfulEntry.sequenceNumber());
        if (batchResponse.responseMetadata() != null) {
            builder.responseMetadata(batchResponse.responseMetadata());
        }
        if (batchResponse.sdkHttpResponse() != null) {
            builder.sdkHttpResponse(batchResponse.sdkHttpResponse());
        }
        SendMessageResponse response = builder.build();
        return new IdentifiableMessage<SendMessageResponse>(key, response);
    }

    private static IdentifiableMessage<Throwable> sendMessageCreateThrowable(BatchResultErrorEntry failedEntry) {
        String key = failedEntry.id();
        AwsErrorDetails errorDetailsBuilder = AwsErrorDetails.builder().errorCode(failedEntry.code())
                .errorMessage(failedEntry.message()).build();
        Throwable response = SqsException.builder().awsErrorDetails(errorDetailsBuilder).build();
        return new IdentifiableMessage<Throwable>(key, response);
    }

    public static BatchKeyMapper<SendMessageRequest> sendMessageBatchKeyMapper() {
        return request -> request.overrideConfiguration().map(overrideConfig -> request.queueUrl() + overrideConfig.hashCode())
                .orElse(request.queueUrl());
    }

    public static BatchAndSend<DeleteMessageRequest, DeleteMessageBatchResponse> deleteMessageBatchFunction(SqsClient client,
            Executor executor) {
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

    private static DeleteMessageBatchRequest createDeleteMessageBatchRequest(
            List<IdentifiableMessage<DeleteMessageRequest>> identifiedRequests, String batchKey) {
        List<DeleteMessageBatchRequestEntry> entries = identifiedRequests
                .stream()
                .map(identifiedRequest -> createDeleteMessageBatchRequestEntry(identifiedRequest.id(),
                        identifiedRequest.message())).collect(Collectors.toList());
        // Since requests are batched together according to a combination of their queueUrl and overrideConfiguration,
        // all requests must have the same overrideConfiguration so it is sufficient to retrieve it from the first
        // request.
        Optional<AwsRequestOverrideConfiguration> overrideConfiguration = identifiedRequests.get(0).message()
                .overrideConfiguration();
        return overrideConfiguration.map(
                overrideConfig -> DeleteMessageBatchRequest.builder().queueUrl(batchKey).overrideConfiguration(overrideConfig)
                        .entries(entries).build()).orElse(
                DeleteMessageBatchRequest.builder().queueUrl(batchKey).entries(entries).build());
    }

    private static DeleteMessageBatchRequestEntry createDeleteMessageBatchRequestEntry(String id, DeleteMessageRequest request) {
        return DeleteMessageBatchRequestEntry.builder().id(id).receiptHandle(request.receiptHandle()).build();
    }

    public static BatchResponseMapper<DeleteMessageBatchResponse, DeleteMessageResponse> deleteMessageResponseMapper() {
        return batchResponse -> {
            List<Either<IdentifiableMessage<DeleteMessageResponse>, IdentifiableMessage<Throwable>>> mappedResponses =
                new ArrayList<>();
            batchResponse.successful().forEach(
                    batchResponseEntry -> {
                        IdentifiableMessage<DeleteMessageResponse> response = createDeleteMessageResponse(batchResponseEntry,
                                batchResponse);
                        mappedResponses.add(Either.left(response));
                    });
            batchResponse.failed().forEach(batchResponseEntry -> {
                IdentifiableMessage<Throwable> response = deleteMessageCreateThrowable(batchResponseEntry);
                mappedResponses.add(Either.right(response));
            });
            return mappedResponses;
        };
    }

    private static IdentifiableMessage<DeleteMessageResponse> createDeleteMessageResponse(
            DeleteMessageBatchResultEntry successfulEntry, DeleteMessageBatchResponse batchResponse) {
        String key = successfulEntry.id();
        DeleteMessageResponse.Builder builder = DeleteMessageResponse.builder();
        if (batchResponse.responseMetadata() != null) {
            builder.responseMetadata(batchResponse.responseMetadata());
        }
        if (batchResponse.sdkHttpResponse() != null) {
            builder.sdkHttpResponse(batchResponse.sdkHttpResponse());
        }
        DeleteMessageResponse response = builder.build();
        return new IdentifiableMessage<DeleteMessageResponse>(key, response);
    }

    private static IdentifiableMessage<Throwable> deleteMessageCreateThrowable(BatchResultErrorEntry failedEntry) {
        String key = failedEntry.id();
        AwsErrorDetails errorDetailsBuilder = AwsErrorDetails.builder().errorCode(failedEntry.code())
                .errorMessage(failedEntry.message()).build();
        Throwable response = SqsException.builder().awsErrorDetails(errorDetailsBuilder).build();
        return new IdentifiableMessage<Throwable>(key, response);
    }

    public static BatchKeyMapper<DeleteMessageRequest> deleteMessageBatchKeyMapper() {
        return request -> request.overrideConfiguration().map(overrideConfig -> request.queueUrl() + overrideConfig.hashCode())
                .orElse(request.queueUrl());
    }

    public static BatchAndSend<ChangeMessageVisibilityRequest,
        ChangeMessageVisibilityBatchResponse> changeMessageVisibilityBatchFunction(
            SqsClient client, Executor executor) {
        return (identifiedRequests, batchKey) -> {
            ChangeMessageVisibilityBatchRequest batchRequest = createChangeMessageVisibilityBatchRequest(identifiedRequests,
                    batchKey);
            return CompletableFuture.supplyAsync(() -> client.changeMessageVisibilityBatch(batchRequest), executor);
        };
    }

    public static BatchAndSend<ChangeMessageVisibilityRequest,
        ChangeMessageVisibilityBatchResponse> changeMessageVisibilityBatchAsyncFunction(
            SqsAsyncClient client) {
        return (identifiedRequests, batchKey) -> {
            ChangeMessageVisibilityBatchRequest batchRequest = createChangeMessageVisibilityBatchRequest(identifiedRequests,
                    batchKey);
            return client.changeMessageVisibilityBatch(batchRequest);
        };
    }

    private static ChangeMessageVisibilityBatchRequest createChangeMessageVisibilityBatchRequest(
            List<IdentifiableMessage<ChangeMessageVisibilityRequest>> identifiedRequests, String batchKey) {
        List<ChangeMessageVisibilityBatchRequestEntry> entries = identifiedRequests
                .stream()
                .map(identifiedRequest -> createChangeMessageVisibilityBatchRequestEntry(identifiedRequest.id(),
                        identifiedRequest.message())).collect(Collectors.toList());
        // Since requests are batched together according to a combination of their queueUrl and overrideConfiguration,
        // all requests must have the same overrideConfiguration so it is sufficient to retrieve it from the first
        // request.
        Optional<AwsRequestOverrideConfiguration> overrideConfiguration = identifiedRequests.get(0).message()
                .overrideConfiguration();
        return overrideConfiguration.map(
                overrideConfig -> ChangeMessageVisibilityBatchRequest.builder().queueUrl(batchKey)
                        .overrideConfiguration(overrideConfig).entries(entries).build()).orElse(
                ChangeMessageVisibilityBatchRequest.builder().queueUrl(batchKey).entries(entries).build());
    }

    private static ChangeMessageVisibilityBatchRequestEntry createChangeMessageVisibilityBatchRequestEntry(String id,
            ChangeMessageVisibilityRequest request) {
        return ChangeMessageVisibilityBatchRequestEntry.builder().id(id).receiptHandle(request.receiptHandle())
                .visibilityTimeout(request.visibilityTimeout()).build();
    }

    public static BatchResponseMapper<ChangeMessageVisibilityBatchResponse,
        ChangeMessageVisibilityResponse> changeMessageVisibilityResponseMapper() {
        return batchResponse -> {
            List<Either<IdentifiableMessage<ChangeMessageVisibilityResponse>, IdentifiableMessage<Throwable>>> mappedResponses =
                new ArrayList<>();
            batchResponse.successful().forEach(
                    batchResponseEntry -> {
                        IdentifiableMessage<ChangeMessageVisibilityResponse> response = createChangeMessageVisibilityResponse(
                                batchResponseEntry, batchResponse);
                        mappedResponses.add(Either.left(response));
                    });
            batchResponse.failed().forEach(batchResponseEntry -> {
                IdentifiableMessage<Throwable> response = changeMessageVisibilityCreateThrowable(batchResponseEntry);
                mappedResponses.add(Either.right(response));
            });
            return mappedResponses;
        };
    }

    private static IdentifiableMessage<ChangeMessageVisibilityResponse> createChangeMessageVisibilityResponse(
            ChangeMessageVisibilityBatchResultEntry successfulEntry, ChangeMessageVisibilityBatchResponse batchResponse) {
        String key = successfulEntry.id();
        ChangeMessageVisibilityResponse.Builder builder = ChangeMessageVisibilityResponse.builder();
        if (batchResponse.responseMetadata() != null) {
            builder.responseMetadata(batchResponse.responseMetadata());
        }
        if (batchResponse.sdkHttpResponse() != null) {
            builder.sdkHttpResponse(batchResponse.sdkHttpResponse());
        }
        ChangeMessageVisibilityResponse response = builder.build();
        return new IdentifiableMessage<ChangeMessageVisibilityResponse>(key, response);
    }

    private static IdentifiableMessage<Throwable> changeMessageVisibilityCreateThrowable(BatchResultErrorEntry failedEntry) {
        String key = failedEntry.id();
        AwsErrorDetails errorDetailsBuilder = AwsErrorDetails.builder().errorCode(failedEntry.code())
                .errorMessage(failedEntry.message()).build();
        Throwable response = SqsException.builder().awsErrorDetails(errorDetailsBuilder).build();
        return new IdentifiableMessage<Throwable>(key, response);
    }

    public static BatchKeyMapper<ChangeMessageVisibilityRequest> changeMessageVisibilityBatchKeyMapper() {
        return request -> request.overrideConfiguration().map(overrideConfig -> request.queueUrl() + overrideConfig.hashCode())
                .orElse(request.queueUrl());
    }
}
