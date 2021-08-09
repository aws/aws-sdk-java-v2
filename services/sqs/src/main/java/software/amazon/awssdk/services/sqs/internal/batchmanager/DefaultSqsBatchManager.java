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

import static software.amazon.awssdk.services.sqs.internal.batchmanager.SqsBatchFunctions.changeVisibilityBatchFunction;
import static software.amazon.awssdk.services.sqs.internal.batchmanager.SqsBatchFunctions.changeVisibilityBatchKeyMapper;
import static software.amazon.awssdk.services.sqs.internal.batchmanager.SqsBatchFunctions.changeVisibilityResponseMapper;
import static software.amazon.awssdk.services.sqs.internal.batchmanager.SqsBatchFunctions.deleteMessageBatchFunction;
import static software.amazon.awssdk.services.sqs.internal.batchmanager.SqsBatchFunctions.deleteMessageBatchKeyMapper;
import static software.amazon.awssdk.services.sqs.internal.batchmanager.SqsBatchFunctions.deleteMessageResponseMapper;
import static software.amazon.awssdk.services.sqs.internal.batchmanager.SqsBatchFunctions.sendMessageBatchFunction;
import static software.amazon.awssdk.services.sqs.internal.batchmanager.SqsBatchFunctions.sendMessageBatchKeyMapper;
import static software.amazon.awssdk.services.sqs.internal.batchmanager.SqsBatchFunctions.sendMessageResponseMapper;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.batchmanager.BatchManager;
import software.amazon.awssdk.core.batchmanager.BatchOverrideConfiguration;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.batchmanager.SqsBatchManager;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchResponse;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;

@SdkInternalApi
public final class DefaultSqsBatchManager implements SqsBatchManager {

    private final SqsClient client;
    private final BatchManager<SendMessageRequest, SendMessageResponse, SendMessageBatchResponse> sendMessageBatchManager;
    private final BatchManager<DeleteMessageRequest, DeleteMessageResponse, DeleteMessageBatchResponse> deleteMessageBatchManager;
    private BatchManager<ChangeMessageVisibilityRequest, ChangeMessageVisibilityResponse,
        ChangeMessageVisibilityBatchResponse> changeVisibilityBatchManager;

    private DefaultSqsBatchManager(DefaultBuilder builder) {
        this.client = builder.client;
        SqsBatchConfiguration config = new SqsBatchConfiguration(builder.overrideConfiguration);
        BatchOverrideConfiguration overrideConfiguration = BatchOverrideConfiguration.builder()
                                                                                     .maxBatchItems(config.maxBatchItems())
                                                                                     .maxBatchOpenInMs(config.maxBatchOpenInMs())
                                                                                     .build();

        //TODO: Pass the client's scheduledExecutor after we add the scheduledExecutor into the client. Right now just creating it
        // here
        ThreadFactory threadFactory = new ThreadFactoryBuilder().threadNamePrefix("SqsBatchManager").build();
        ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);

        this.sendMessageBatchManager = BatchManager.builder(SendMessageRequest.class, SendMessageResponse.class,
                                                            SendMessageBatchResponse.class)
                                        .batchFunction(sendMessageBatchFunction(client))
                                        .responseMapper(sendMessageResponseMapper())
                                        .batchKeyMapper(sendMessageBatchKeyMapper())
                                        .overrideConfiguration(overrideConfiguration)
                                        .scheduledExecutor(scheduledExecutor)
                                        .build();

        this.deleteMessageBatchManager = BatchManager.builder(DeleteMessageRequest.class, DeleteMessageResponse.class,
                                                              DeleteMessageBatchResponse.class)
                                                     .batchFunction(deleteMessageBatchFunction(client))
                                                     .responseMapper(deleteMessageResponseMapper())
                                                     .batchKeyMapper(deleteMessageBatchKeyMapper())
                                                     .overrideConfiguration(overrideConfiguration)
                                                     .scheduledExecutor(scheduledExecutor)
                                                     .build();

        this.changeVisibilityBatchManager = BatchManager.builder(ChangeMessageVisibilityRequest.class,
                                                                 ChangeMessageVisibilityResponse.class,
                                                                 ChangeMessageVisibilityBatchResponse.class)
                                                        .batchFunction(changeVisibilityBatchFunction(client))
                                                        .responseMapper(changeVisibilityResponseMapper())
                                                        .batchKeyMapper(changeVisibilityBatchKeyMapper())
                                                        .overrideConfiguration(overrideConfiguration)
                                                        .scheduledExecutor(scheduledExecutor)
                                                        .build();
    }

    @Override
    public CompletableFuture<SendMessageResponse> sendMessage(SendMessageRequest message) {
        return sendMessageBatchManager.sendRequest(message);
    }

    @Override
    public CompletableFuture<DeleteMessageResponse> deleteMessage(DeleteMessageRequest deleteRequest) {
        return deleteMessageBatchManager.sendRequest(deleteRequest);
    }

    @Override
    public CompletableFuture<ChangeMessageVisibilityResponse> changeMessageVisibility(ChangeMessageVisibilityRequest
                                                                                              changeRequest) {
        return changeVisibilityBatchManager.sendRequest(changeRequest);
    }

    @Override
    public void close() {
        sendMessageBatchManager.close();
        deleteMessageBatchManager.close();
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
