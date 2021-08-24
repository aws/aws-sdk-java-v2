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

package software.amazon.awssdk.services.batchmanagertest.batchmanager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.batchmanager.BatchAndSend;
import software.amazon.awssdk.core.batchmanager.BatchKeyMapper;
import software.amazon.awssdk.core.batchmanager.BatchResponseMapper;
import software.amazon.awssdk.core.batchmanager.IdentifiableMessage;
import software.amazon.awssdk.services.batchmanagertest.BatchManagerTestAsyncClient;
import software.amazon.awssdk.services.batchmanagertest.BatchManagerTestClient;
import software.amazon.awssdk.services.batchmanagertest.model.BatchErrorEntry;
import software.amazon.awssdk.services.batchmanagertest.model.BatchManagerTestException;
import software.amazon.awssdk.services.batchmanagertest.model.DeleteRequestBatchRequest;
import software.amazon.awssdk.services.batchmanagertest.model.DeleteRequestBatchRequestEntry;
import software.amazon.awssdk.services.batchmanagertest.model.DeleteRequestBatchResponse;
import software.amazon.awssdk.services.batchmanagertest.model.DeleteRequestBatchResultEntry;
import software.amazon.awssdk.services.batchmanagertest.model.DeleteRequestRequest;
import software.amazon.awssdk.services.batchmanagertest.model.DeleteRequestResponse;
import software.amazon.awssdk.services.batchmanagertest.model.SendRequestBatchRequest;
import software.amazon.awssdk.services.batchmanagertest.model.SendRequestBatchRequestEntry;
import software.amazon.awssdk.services.batchmanagertest.model.SendRequestBatchResponse;
import software.amazon.awssdk.services.batchmanagertest.model.SendRequestBatchResultEntry;
import software.amazon.awssdk.services.batchmanagertest.model.SendRequestRequest;
import software.amazon.awssdk.services.batchmanagertest.model.SendRequestResponse;
import software.amazon.awssdk.utils.Either;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class BatchManagerTestBatchFunctions {
    private BatchManagerTestBatchFunctions() {
    }

    public static BatchAndSend<SendRequestRequest, SendRequestBatchResponse> sendRequestBatchFunction(
        BatchManagerTestClient client, Executor executor) {
        return (identifiedRequests, batchKey) -> {
            SendRequestBatchRequest batchRequest = createSendRequestBatchRequest(identifiedRequests, batchKey);
            return CompletableFuture.supplyAsync(() -> client.sendRequestBatch(batchRequest), executor);
        };
    }

    public static BatchAndSend<SendRequestRequest, SendRequestBatchResponse> sendRequestAsyncBatchFunction(
        BatchManagerTestAsyncClient client) {
        return (identifiedRequests, batchKey) -> {
            SendRequestBatchRequest batchRequest = createSendRequestBatchRequest(identifiedRequests, batchKey);
            return client.sendRequestBatch(batchRequest);
        };
    }

    private static SendRequestBatchRequest createSendRequestBatchRequest(
        List<IdentifiableMessage<SendRequestRequest>> identifiedRequests, String batchKey) {
        List<SendRequestBatchRequestEntry> entries = identifiedRequests
            .stream()
            .map(identifiedRequest -> createSendRequestBatchRequestEntry(identifiedRequest.id(), identifiedRequest.message()))
            .collect(Collectors.toList());
        // Since requests are batched together according to a combination of their queueUrl and overrideConfiguration,
        // all requests must have the same overrideConfiguration so it is sufficient to retrieve it from the first
        // request.
        Optional<AwsRequestOverrideConfiguration> overrideConfiguration = identifiedRequests.get(0).message()
                                                                                            .overrideConfiguration();
        return overrideConfiguration.map(
            overrideConfig -> SendRequestBatchRequest.builder().destination(batchKey).overrideConfiguration(overrideConfig)
                                                     .entries(entries).build()).orElse(
            SendRequestBatchRequest.builder().destination(batchKey).entries(entries).build());
    }

    private static SendRequestBatchRequestEntry createSendRequestBatchRequestEntry(String id, SendRequestRequest request) {
        return SendRequestBatchRequestEntry.builder().build();
    }

    public static BatchResponseMapper<SendRequestBatchResponse, SendRequestResponse> sendRequestResponseMapper() {
        return batchResponse -> {
            List<Either<IdentifiableMessage<SendRequestResponse>, IdentifiableMessage<Throwable>>> mappedResponses = new ArrayList<>();
            batchResponse.successfulEntries().forEach(batchResponseEntry -> {
                IdentifiableMessage<SendRequestResponse> response = createSendRequestResponse(batchResponseEntry, batchResponse);
                mappedResponses.add(Either.left(response));
            });
            batchResponse.failedEntries().forEach(batchResponseEntry -> {
                IdentifiableMessage<Throwable> response = createThrowable(batchResponseEntry);
                mappedResponses.add(Either.right(response));
            });
            return mappedResponses;
        };
    }

    private static IdentifiableMessage<SendRequestResponse> createSendRequestResponse(
        SendRequestBatchResultEntry successfulEntry, SendRequestBatchResponse batchResponse) {
        String key = successfulEntry.id();
        SendRequestResponse.Builder builder = SendRequestResponse.builder();
        if (batchResponse.responseMetadata() != null) {
            builder.responseMetadata(batchResponse.responseMetadata());
        }
        if (batchResponse.sdkHttpResponse() != null) {
            builder.sdkHttpResponse(batchResponse.sdkHttpResponse());
        }
        SendRequestResponse response = builder.build();
        return new IdentifiableMessage<SendRequestResponse>(key, response);
    }

    public static BatchKeyMapper<SendRequestRequest> sendRequestBatchKeyMapper() {
        return request -> request.overrideConfiguration()
                                 .map(overrideConfig -> request.destination() + overrideConfig.hashCode()).orElse(request.destination());
    }

    public static BatchAndSend<DeleteRequestRequest, DeleteRequestBatchResponse> deleteRequestBatchFunction(
        BatchManagerTestClient client, Executor executor) {
        return (identifiedRequests, batchKey) -> {
            DeleteRequestBatchRequest batchRequest = createDeleteRequestBatchRequest(identifiedRequests, batchKey);
            return CompletableFuture.supplyAsync(() -> client.deleteRequestBatch(batchRequest), executor);
        };
    }

    public static BatchAndSend<DeleteRequestRequest, DeleteRequestBatchResponse> deleteRequestAsyncBatchFunction(
        BatchManagerTestAsyncClient client) {
        return (identifiedRequests, batchKey) -> {
            DeleteRequestBatchRequest batchRequest = createDeleteRequestBatchRequest(identifiedRequests, batchKey);
            return client.deleteRequestBatch(batchRequest);
        };
    }

    private static DeleteRequestBatchRequest createDeleteRequestBatchRequest(
        List<IdentifiableMessage<DeleteRequestRequest>> identifiedRequests, String batchKey) {
        List<DeleteRequestBatchRequestEntry> entries = identifiedRequests
            .stream()
            .map(identifiedRequest -> createDeleteRequestBatchRequestEntry(identifiedRequest.id(),
                                                                           identifiedRequest.message())).collect(Collectors.toList());
        // Since requests are batched together according to a combination of their queueUrl and overrideConfiguration,
        // all requests must have the same overrideConfiguration so it is sufficient to retrieve it from the first
        // request.
        Optional<AwsRequestOverrideConfiguration> overrideConfiguration = identifiedRequests.get(0).message()
                                                                                            .overrideConfiguration();
        return overrideConfiguration.map(
            overrideConfig -> DeleteRequestBatchRequest.builder().destination(batchKey).overrideConfiguration(overrideConfig)
                                                       .entries(entries).build()).orElse(
            DeleteRequestBatchRequest.builder().destination(batchKey).entries(entries).build());
    }

    private static DeleteRequestBatchRequestEntry createDeleteRequestBatchRequestEntry(String id, DeleteRequestRequest request) {
        return DeleteRequestBatchRequestEntry.builder().build();
    }

    public static BatchResponseMapper<DeleteRequestBatchResponse, DeleteRequestResponse> deleteRequestResponseMapper() {
        return batchResponse -> {
            List<Either<IdentifiableMessage<DeleteRequestResponse>, IdentifiableMessage<Throwable>>> mappedResponses = new ArrayList<>();
            batchResponse.successfulEntries().forEach(
                batchResponseEntry -> {
                    IdentifiableMessage<DeleteRequestResponse> response = createDeleteRequestResponse(batchResponseEntry,
                                                                                                      batchResponse);
                    mappedResponses.add(Either.left(response));
                });
            batchResponse.failedEntries().forEach(batchResponseEntry -> {
                IdentifiableMessage<Throwable> response = createThrowable(batchResponseEntry);
                mappedResponses.add(Either.right(response));
            });
            return mappedResponses;
        };
    }

    private static IdentifiableMessage<DeleteRequestResponse> createDeleteRequestResponse(
        DeleteRequestBatchResultEntry successfulEntry, DeleteRequestBatchResponse batchResponse) {
        String key = successfulEntry.id();
        DeleteRequestResponse.Builder builder = DeleteRequestResponse.builder();
        if (batchResponse.responseMetadata() != null) {
            builder.responseMetadata(batchResponse.responseMetadata());
        }
        if (batchResponse.sdkHttpResponse() != null) {
            builder.sdkHttpResponse(batchResponse.sdkHttpResponse());
        }
        DeleteRequestResponse response = builder.build();
        return new IdentifiableMessage<DeleteRequestResponse>(key, response);
    }

    public static BatchKeyMapper<DeleteRequestRequest> deleteRequestBatchKeyMapper() {
        return request -> request.overrideConfiguration()
                                 .map(overrideConfig -> request.destination() + request.someOtherMethod() + overrideConfig.hashCode())
                                 .orElse(request.destination() + request.someOtherMethod());
    }

    private static IdentifiableMessage<Throwable> createThrowable(BatchErrorEntry failedEntry) {
        String key = failedEntry.id();
        BatchManagerTestException.Builder builder = BatchManagerTestException.builder();
        builder.statusCode(Integer.parseInt(failedEntry.errorCode()));
        Throwable response = builder.build();
        return new IdentifiableMessage<Throwable>(key, response);
    }
}
