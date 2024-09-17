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
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.BatchResultErrorEntry;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchResultEntry;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;
import software.amazon.awssdk.utils.Either;

@SdkInternalApi
public class DeleteMessageBatchManager extends RequestBatchManager<DeleteMessageRequest, DeleteMessageResponse,
    DeleteMessageBatchResponse> {

    private final SqsAsyncClient sqsAsyncClient;

    protected DeleteMessageBatchManager(RequestBatchConfiguration overrideConfiguration,
                                        ScheduledExecutorService scheduledExecutor,
                                        SqsAsyncClient sqsAsyncClient) {
        super(overrideConfiguration, scheduledExecutor);
        this.sqsAsyncClient = sqsAsyncClient;
    }

    private static DeleteMessageBatchRequest createDeleteMessageBatchRequest(
        List<IdentifiableMessage<DeleteMessageRequest>> identifiedRequests, String batchKey) {

        List<DeleteMessageBatchRequestEntry> entries = identifiedRequests
            .stream()
            .map(identifiedRequest -> createDeleteMessageBatchRequestEntry(
                identifiedRequest.id(), identifiedRequest.message()
            ))
            .collect(Collectors.toList());

        // Since requests are batched together according to a combination of their queueUrl and overrideConfiguration,
        // all requests must have the same overrideConfiguration, so it is sufficient to retrieve it from the first request.
        Optional<AwsRequestOverrideConfiguration> overrideConfiguration = identifiedRequests.get(0).message()
                                                                                            .overrideConfiguration();

        return overrideConfiguration.map(
            overrideConfig -> DeleteMessageBatchRequest.builder()
                                                       .queueUrl(batchKey)
                                                       .overrideConfiguration(
                                                           overrideConfig.toBuilder()
                                                                         .applyMutation(USER_AGENT_APPLIER)
                                                                         .build()
                                                       )
                                                       .entries(entries)
                                                       .build()
        ).orElseGet(
            () -> DeleteMessageBatchRequest.builder()
                                           .queueUrl(batchKey)
                                           .overrideConfiguration(o ->
                                                                      o.applyMutation(USER_AGENT_APPLIER).build()
                                           )
                                           .entries(entries)
                                           .build()
        );
    }

    private static DeleteMessageBatchRequestEntry createDeleteMessageBatchRequestEntry(String id, DeleteMessageRequest request) {
        return DeleteMessageBatchRequestEntry.builder().id(id).receiptHandle(request.receiptHandle()).build();
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
        return new IdentifiableMessage<>(key, response);
    }

    private static IdentifiableMessage<Throwable> deleteMessageCreateThrowable(BatchResultErrorEntry failedEntry) {
        String key = failedEntry.id();
        AwsErrorDetails errorDetailsBuilder = AwsErrorDetails.builder().errorCode(failedEntry.code())
                                                             .errorMessage(failedEntry.message()).build();
        Throwable response = SqsException.builder().awsErrorDetails(errorDetailsBuilder).build();
        return new IdentifiableMessage<>(key, response);
    }

    @Override
    protected CompletableFuture<DeleteMessageBatchResponse> batchAndSend(
        List<IdentifiableMessage<DeleteMessageRequest>> identifiedRequests, String batchKey) {
        DeleteMessageBatchRequest batchRequest = createDeleteMessageBatchRequest(identifiedRequests, batchKey);
        return sqsAsyncClient.deleteMessageBatch(batchRequest);
    }

    @Override
    protected String getBatchKey(DeleteMessageRequest request) {
        return request.overrideConfiguration().map(overrideConfig -> request.queueUrl() + overrideConfig.hashCode())
                      .orElse(request.queueUrl());
    }

    @Override
    protected List<Either<IdentifiableMessage<DeleteMessageResponse>,
        IdentifiableMessage<Throwable>>> mapBatchResponse(DeleteMessageBatchResponse batchResponse) {

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
    }

}
