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

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.batchmanager.BatchOverrideConfiguration;
import software.amazon.awssdk.services.sqs.batchmanager.SqsAsyncBatchManager;
import software.amazon.awssdk.services.sqs.internal.batchmanager.core.RequestBatchManager;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchResponse;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class DefaultSqsAsyncBatchManager implements SqsAsyncBatchManager {
    // TODO : update the validation here while implementing this class in next PR
    private final SqsAsyncClient client;

    private final RequestBatchManager<SendMessageRequest, SendMessageResponse, SendMessageBatchResponse> sendMessageBatchManager;

    private final RequestBatchManager<DeleteMessageRequest, DeleteMessageResponse, DeleteMessageBatchResponse> deleteMessageBatchManager;

    private final RequestBatchManager<ChangeMessageVisibilityRequest, ChangeMessageVisibilityResponse,
            ChangeMessageVisibilityBatchResponse> changeMessageVisibilityBatchManager;

    private DefaultSqsAsyncBatchManager(DefaultBuilder builder) {
        this.client = Validate.notNull(builder.client, "client cannot be null");

        ScheduledExecutorService scheduledExecutor = builder.scheduledExecutor;

        this.sendMessageBatchManager = RequestBatchManager
            .builder(SendMessageRequest.class, SendMessageResponse.class, SendMessageBatchResponse.class)
            .batchFunction(SqsBatchFunctions.sendMessageBatchAsyncFunction(client))
            .responseMapper(SqsBatchFunctions.sendMessageResponseMapper())
            .batchKeyMapper(SqsBatchFunctions.sendMessageBatchKeyMapper())
            .overrideConfiguration(sendMessageConfig(builder.overrideConfiguration)).scheduledExecutor(scheduledExecutor)
            .build();
        this.deleteMessageBatchManager = RequestBatchManager
            .builder(DeleteMessageRequest.class, DeleteMessageResponse.class, DeleteMessageBatchResponse.class)
            .batchFunction(SqsBatchFunctions.deleteMessageBatchAsyncFunction(client))
            .responseMapper(SqsBatchFunctions.deleteMessageResponseMapper())
            .batchKeyMapper(SqsBatchFunctions.deleteMessageBatchKeyMapper())
            .overrideConfiguration(deleteMessageConfig(builder.overrideConfiguration)).scheduledExecutor(scheduledExecutor)
            .build();
        this.changeMessageVisibilityBatchManager = RequestBatchManager
            .builder(ChangeMessageVisibilityRequest.class, ChangeMessageVisibilityResponse.class,
                     ChangeMessageVisibilityBatchResponse.class)
            .batchFunction(SqsBatchFunctions.changeMessageVisibilityBatchAsyncFunction(client))
            .responseMapper(SqsBatchFunctions.changeMessageVisibilityResponseMapper())
            .batchKeyMapper(SqsBatchFunctions.changeMessageVisibilityBatchKeyMapper())
            .overrideConfiguration(changeMessageVisibilityConfig(builder.overrideConfiguration))
            .scheduledExecutor(scheduledExecutor).build();

        //TODO : this will be updated while implementing the Receive Message Batch Manager
    }


    @SdkTestInternalApi
    public DefaultSqsAsyncBatchManager(
        SqsAsyncClient client,
        RequestBatchManager<SendMessageRequest, SendMessageResponse, SendMessageBatchResponse> sendMessageBatchManager,
        RequestBatchManager<DeleteMessageRequest, DeleteMessageResponse, DeleteMessageBatchResponse> deleteMessageBatchManager,
        RequestBatchManager<ChangeMessageVisibilityRequest, ChangeMessageVisibilityResponse,
            ChangeMessageVisibilityBatchResponse> changeMessageVisibilityBatchManager) {
        this.sendMessageBatchManager = sendMessageBatchManager;
        this.deleteMessageBatchManager = deleteMessageBatchManager;
        this.changeMessageVisibilityBatchManager = changeMessageVisibilityBatchManager;
        this.client = client;
    }

    @Override
    public CompletableFuture<SendMessageResponse> sendMessage(SendMessageRequest request) {
        return sendMessageBatchManager.batchRequest(request);
    }

    @Override
    public CompletableFuture<DeleteMessageResponse> deleteMessage(DeleteMessageRequest request) {
        return deleteMessageBatchManager.batchRequest(request);
    }

    @Override
    public CompletableFuture<ChangeMessageVisibilityResponse> changeMessageVisibility(ChangeMessageVisibilityRequest request) {
        return changeMessageVisibilityBatchManager.batchRequest(request);
    }

    @Override
    public CompletableFuture<ReceiveMessageResponse> receiveMessage(ReceiveMessageRequest request) {
        return null;
    }

    public static SqsAsyncBatchManager.Builder builder() {
        return new DefaultBuilder();
    }

    @Override
    public void close() {
        sendMessageBatchManager.close();
        deleteMessageBatchManager.close();
        changeMessageVisibilityBatchManager.close();
    }

    private BatchOverrideConfiguration createConfig(BatchOverrideConfiguration overrideConfiguration) {
        BatchOverrideConfiguration.Builder config = BatchOverrideConfiguration.builder();
        if (overrideConfiguration == null) {
            config.maxBatchItems(10);
            config.maxBatchOpenInMs(Duration.ofMillis(200));
        } else {
            config.maxBatchItems(overrideConfiguration.maxBatchItems().orElse(10));
            config.maxBatchOpenInMs(overrideConfiguration.maxBatchOpenInMs().orElse(Duration.ofMillis(200)));
        }
        return config.build();
    }

    private BatchOverrideConfiguration sendMessageConfig(BatchOverrideConfiguration overrideConfiguration) {
        return createConfig(overrideConfiguration);
    }

    private BatchOverrideConfiguration deleteMessageConfig(BatchOverrideConfiguration overrideConfiguration) {
        return createConfig(overrideConfiguration);
    }

    private BatchOverrideConfiguration changeMessageVisibilityConfig(BatchOverrideConfiguration overrideConfiguration) {
        return createConfig(overrideConfiguration);
    }

    public static final class DefaultBuilder implements SqsAsyncBatchManager.Builder {
        private SqsAsyncClient client;
        private BatchOverrideConfiguration overrideConfiguration;
        private ScheduledExecutorService scheduledExecutor;

        private DefaultBuilder() {
        }

        @Override
        public SqsAsyncBatchManager.Builder overrideConfiguration(BatchOverrideConfiguration overrideConfiguration) {
            this.overrideConfiguration = overrideConfiguration;
            return this;
        }

        @Override
        public SqsAsyncBatchManager.Builder client(SqsAsyncClient client) {
            this.client = client;
            return this;
        }

        @Override
        public SqsAsyncBatchManager.Builder scheduledExecutor(ScheduledExecutorService scheduledExecutor) {
            this.scheduledExecutor = scheduledExecutor;
            return this;
        }

        @Override
        public SqsAsyncBatchManager build() {
            return new DefaultSqsAsyncBatchManager(this);
        }
    }
}
