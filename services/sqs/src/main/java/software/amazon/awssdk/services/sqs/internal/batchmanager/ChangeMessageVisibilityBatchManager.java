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
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchResponse;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchResultEntry;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;
import software.amazon.awssdk.utils.Either;

@SdkInternalApi
public class ChangeMessageVisibilityBatchManager extends RequestBatchManager<ChangeMessageVisibilityRequest,
    ChangeMessageVisibilityResponse,
    ChangeMessageVisibilityBatchResponse> {

    private final SqsAsyncClient sqsAsyncClient;

    protected ChangeMessageVisibilityBatchManager(RequestBatchConfiguration overrideConfiguration,
                                                  ScheduledExecutorService scheduledExecutor,
                                                  SqsAsyncClient sqsAsyncClient) {
        super(overrideConfiguration, scheduledExecutor);
        this.sqsAsyncClient = sqsAsyncClient;
    }

    private static ChangeMessageVisibilityBatchRequest createChangeMessageVisibilityBatchRequest(
        List<IdentifiableMessage<ChangeMessageVisibilityRequest>> identifiedRequests, String batchKey) {

        List<ChangeMessageVisibilityBatchRequestEntry> entries =
            identifiedRequests.stream()
                              .map(identifiedRequest -> createChangeMessageVisibilityBatchRequestEntry(
                                  identifiedRequest.id(),
                                  identifiedRequest.message()))
                              .collect(Collectors.toList());

        // All requests have the same overrideConfiguration, so it's sufficient to retrieve it from the first request.
        Optional<AwsRequestOverrideConfiguration> overrideConfiguration = identifiedRequests.get(0)
                                                                                            .message()
                                                                                            .overrideConfiguration();

        return overrideConfiguration
            .map(config -> ChangeMessageVisibilityBatchRequest.builder()
                                                              .queueUrl(batchKey)
                                                              .overrideConfiguration(config.toBuilder()
                                                                                           .applyMutation(USER_AGENT_APPLIER)
                                                                                           .build())
                                                              .entries(entries)
                                                              .build())
            .orElseGet(() -> ChangeMessageVisibilityBatchRequest.builder()
                                                                .queueUrl(batchKey)
                                                                .overrideConfiguration(o -> o
                                                                    .applyMutation(USER_AGENT_APPLIER)
                                                                    .build())
                                                                .entries(entries)
                                                                .build());
    }

    private static ChangeMessageVisibilityBatchRequestEntry createChangeMessageVisibilityBatchRequestEntry(
        String id,
        ChangeMessageVisibilityRequest request) {
        return ChangeMessageVisibilityBatchRequestEntry.builder().id(id).receiptHandle(request.receiptHandle())
                                                       .visibilityTimeout(request.visibilityTimeout()).build();
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
        return new IdentifiableMessage<>(key, response);
    }

    private static IdentifiableMessage<Throwable> changeMessageVisibilityCreateThrowable(BatchResultErrorEntry failedEntry) {
        String key = failedEntry.id();
        AwsErrorDetails errorDetailsBuilder = AwsErrorDetails.builder().errorCode(failedEntry.code())
                                                             .errorMessage(failedEntry.message()).build();
        Throwable response = SqsException.builder().awsErrorDetails(errorDetailsBuilder).build();
        return new IdentifiableMessage<>(key, response);
    }

    @Override
    protected CompletableFuture<ChangeMessageVisibilityBatchResponse> batchAndSend(
        List<IdentifiableMessage<ChangeMessageVisibilityRequest>> identifiedRequests, String batchKey) {
        ChangeMessageVisibilityBatchRequest batchRequest = createChangeMessageVisibilityBatchRequest(identifiedRequests,
                                                                                                     batchKey);
        return sqsAsyncClient.changeMessageVisibilityBatch(batchRequest);
    }

    @Override
    protected String getBatchKey(ChangeMessageVisibilityRequest request) {
        return  request.overrideConfiguration().map(overrideConfig -> request.queueUrl() + overrideConfig.hashCode())
                       .orElseGet(request::queueUrl);
    }

    @Override
    protected List<Either<IdentifiableMessage<ChangeMessageVisibilityResponse>,
        IdentifiableMessage<Throwable>>> mapBatchResponse(ChangeMessageVisibilityBatchResponse batchResponse) {

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

    }
}