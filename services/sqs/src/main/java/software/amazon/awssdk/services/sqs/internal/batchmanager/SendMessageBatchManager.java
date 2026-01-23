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
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResultEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;
import software.amazon.awssdk.utils.Either;

@SdkInternalApi
public class SendMessageBatchManager extends RequestBatchManager<SendMessageRequest,
    SendMessageResponse,
    SendMessageBatchResponse> {

    private final SqsAsyncClient asyncClient;

    protected SendMessageBatchManager(RequestBatchConfiguration overrideConfiguration,
                                      ScheduledExecutorService scheduledExecutor,
                                      SqsAsyncClient asyncClient) {
        super(overrideConfiguration, scheduledExecutor);
        this.asyncClient = asyncClient;
    }

    private static IdentifiableMessage<Throwable> sendMessageCreateThrowable(BatchResultErrorEntry failedEntry) {
        String key = failedEntry.id();
        AwsErrorDetails errorDetailsBuilder = AwsErrorDetails.builder()
                                                             .errorCode(failedEntry.code())
                                                             .errorMessage(failedEntry.message())
                                                             .build();
        Throwable response = SqsException.builder()
                                         .awsErrorDetails(errorDetailsBuilder)
                                         .build();
        return new IdentifiableMessage<>(key, response);
    }

    private static IdentifiableMessage<SendMessageResponse> createSendMessageResponse(
        SendMessageBatchResultEntry successfulEntry, SendMessageBatchResponse batchResponse) {
        String key = successfulEntry.id();
        SendMessageResponse.Builder builder = SendMessageResponse.builder()
                                                                 .md5OfMessageBody(successfulEntry.md5OfMessageBody())
                                                                 .md5OfMessageAttributes(successfulEntry.md5OfMessageAttributes())
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
        SendMessageResponse response = builder.build();
        return new IdentifiableMessage<>(key, response);
    }

    private static SendMessageBatchRequest createSendMessageBatchRequest(
        List<IdentifiableMessage<SendMessageRequest>> identifiedRequests, String batchKey) {

        List<SendMessageBatchRequestEntry> entries =
            identifiedRequests.stream()
                              .map(identifiedRequest -> createSendMessageBatchRequestEntry(identifiedRequest.id(),
                                                                                           identifiedRequest.message()))
                              .collect(Collectors.toList());

        // All requests must have the same overrideConfiguration, so retrieve it from the first request.
        Optional<AwsRequestOverrideConfiguration> overrideConfiguration = identifiedRequests.get(0)
                                                                                            .message()
                                                                                            .overrideConfiguration();

        return overrideConfiguration
            .map(overrideConfig -> SendMessageBatchRequest.builder()
                                                          .queueUrl(batchKey)
                                                          .overrideConfiguration(overrideConfig.toBuilder()
                                                                                               .applyMutation(USER_AGENT_APPLIER)
                                                                                               .build())
                                                          .entries(entries)
                                                          .build())
            .orElseGet(() -> SendMessageBatchRequest.builder()
                                                    .queueUrl(batchKey)
                                                    .overrideConfiguration(o -> o.applyMutation(USER_AGENT_APPLIER))
                                                    .entries(entries)
                                                    .build());
    }

    private static SendMessageBatchRequestEntry createSendMessageBatchRequestEntry(String id, SendMessageRequest request) {
        return SendMessageBatchRequestEntry.builder()
                                           .id(id)
                                           .messageBody(request.messageBody())
                                           .delaySeconds(request.delaySeconds())
                                           .messageAttributes(request.messageAttributes())
                                           .messageSystemAttributesWithStrings(request.messageSystemAttributesAsStrings())
                                           .messageDeduplicationId(request.messageDeduplicationId())
                                           .messageGroupId(request.messageGroupId())
                                           .build();
    }

    @Override
    protected CompletableFuture<SendMessageBatchResponse> batchAndSend(List<IdentifiableMessage<SendMessageRequest>>
                                                                           identifiedRequests, String batchKey) {
        SendMessageBatchRequest batchRequest = createSendMessageBatchRequest(identifiedRequests, batchKey);
        return asyncClient.sendMessageBatch(batchRequest);
    }

    @Override
    protected String getBatchKey(SendMessageRequest request) {
        return request.overrideConfiguration().map(overrideConfig -> request.queueUrl() + overrideConfig.hashCode())
                      .orElseGet(request::queueUrl);
    }

    @Override
    protected List<Either<IdentifiableMessage<SendMessageResponse>,
        IdentifiableMessage<Throwable>>> mapBatchResponse(SendMessageBatchResponse batchResponse) {
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
    }

}
