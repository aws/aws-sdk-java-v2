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
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.batchmanager.BatchManager;
import software.amazon.awssdk.core.batchmanager.BatchOverrideConfiguration;
import software.amazon.awssdk.core.internal.batchmanager.BatchAndSend;
import software.amazon.awssdk.core.internal.batchmanager.BatchKeyMapper;
import software.amazon.awssdk.core.internal.batchmanager.BatchResponseMapper;
import software.amazon.awssdk.core.internal.batchmanager.IdentifiableMessage;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.batchmanager.SqsBatchManager;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResultEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@SdkInternalApi
public class DefaultSqsBatchManager implements SqsBatchManager {

    private SqsClient client;
    private BatchManager<SendMessageRequest, SendMessageResponse, SendMessageBatchResponse> batchManager;

    // TODO: These functions were just copied over from the SQS integration test. Should I move these to a separate file or
    //  into a separate wrapper class or is it fine to keep these as private lambdas here?
    private final BatchAndSend<SendMessageRequest, SendMessageBatchResponse> batchingFunction =
        (identifiedRequests, destination) -> {
            List<SendMessageBatchRequestEntry> entries = new ArrayList<>(identifiedRequests.size());
            identifiedRequests.forEach(identifiedRequest -> {
                String id = identifiedRequest.id();
                SendMessageRequest request = identifiedRequest.message();
                entries.add(createMessageBatchRequestEntry(id, request));
            });
            SendMessageBatchRequest batchRequest = SendMessageBatchRequest.builder()
                                                                          .queueUrl(destination)
                                                                          .entries(entries)
                                                                          .build();
            return CompletableFuture.supplyAsync(() -> client.sendMessageBatch(batchRequest));
        };

    private final BatchResponseMapper<SendMessageBatchResponse, SendMessageResponse> mapResponsesFunction =
        sendMessageBatchResponse -> {
            List<IdentifiableMessage<SendMessageResponse>> mappedResponses = new ArrayList<>();
            sendMessageBatchResponse.successful()
                                    .forEach(batchResponseEntry -> {
                                        String key = batchResponseEntry.id();
                                        SendMessageResponse response = createSendMessageResponse(batchResponseEntry);
                                        mappedResponses.add(new IdentifiableMessage<>(key, response));
                                    });
            // Add failed responses once I figure out how to create sendMessageResponse items.
            return mappedResponses;
        };

    private final BatchKeyMapper<SendMessageRequest> getBatchGroupIdFunction =
        request -> {
            if (request.overrideConfiguration().isPresent()) {
                return request.queueUrl() + request.overrideConfiguration().get();
            } else {
                return request.queueUrl();
            }
        };

    private DefaultSqsBatchManager(DefaultBuilder builder) {
        this.client = builder.client;
        SqsBatchConfiguration config = new SqsBatchConfiguration(builder.overrideConfiguration);
        BatchOverrideConfiguration overrideConfiguration = BatchOverrideConfiguration.builder()
                                                                                     .maxBatchItems(config.maxBatchItems())
                                                                                     .maxBatchOpenInMs(config.maxBatchOpenInMs())
                                                                                     .build();
        this.batchManager = BatchManager.builder(SendMessageRequest.class, SendMessageResponse.class,
                                                 SendMessageBatchResponse.class)
                                        .overrideConfiguration(overrideConfiguration)
                                        .batchingFunction(batchingFunction)
                                        .mapResponsesFunction(mapResponsesFunction)
                                        .batchKeyMapperFunction(getBatchGroupIdFunction)
                                        .build();
    }

    private SendMessageBatchRequestEntry createMessageBatchRequestEntry(String id, SendMessageRequest request) {
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

    private SendMessageResponse createSendMessageResponse(SendMessageBatchResultEntry successfulEntry) {
        return SendMessageResponse.builder()
                                  .md5OfMessageAttributes(successfulEntry.md5OfMessageAttributes())
                                  .md5OfMessageBody(successfulEntry.md5OfMessageBody())
                                  .md5OfMessageSystemAttributes(successfulEntry.md5OfMessageSystemAttributes())
                                  .messageId(successfulEntry.messageId())
                                  .sequenceNumber(successfulEntry.sequenceNumber())
                                  .build();
    }

    @Override
    public CompletableFuture<SendMessageResponse> sendMessage(SendMessageRequest message) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        batchManager.close();
    }

    public static SqsBatchManager.Builder builder() {
        return new DefaultBuilder();
    }

    public static final class DefaultBuilder implements Builder {
        private BatchOverrideConfiguration overrideConfiguration;
        private SqsClient client;

        private DefaultBuilder() {
        }

        @Override
        public Builder overrideConfiguration(BatchOverrideConfiguration overrideConfiguration) {
            this.overrideConfiguration = overrideConfiguration;
            return this;
        }

        @Override
        public Builder client(SqsClient client) {
            this.client = client;
            return this;
        }

        public SqsBatchManager build() {
            return new DefaultSqsBatchManager(this);
        }
    }
}
