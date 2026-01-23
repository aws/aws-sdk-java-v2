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


import static software.amazon.awssdk.services.sqs.internal.batchmanager.ResponseBatchConfiguration.MAX_SEND_MESSAGE_PAYLOAD_SIZE_BYTES;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.batchmanager.BatchOverrideConfiguration;
import software.amazon.awssdk.services.sqs.batchmanager.SqsAsyncBatchManager;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class DefaultSqsAsyncBatchManager implements SqsAsyncBatchManager {
    private final SqsAsyncClient client;

    private final SendMessageBatchManager sendMessageBatchManager;

    private final DeleteMessageBatchManager deleteMessageBatchManager;

    private final ChangeMessageVisibilityBatchManager changeMessageVisibilityBatchManager;

    private final ReceiveMessageBatchManager receiveMessageBatchManager;

    private DefaultSqsAsyncBatchManager(DefaultBuilder builder) {
        this.client = Validate.notNull(builder.client, "client cannot be null");
        ScheduledExecutorService scheduledExecutor  = Validate.notNull(builder.scheduledExecutor,
                                                                       "scheduledExecutor cannot be null");
        this.sendMessageBatchManager =
            new SendMessageBatchManager(
                RequestBatchConfiguration.builder(builder.overrideConfiguration)
                                         .maxBatchBytesSize(MAX_SEND_MESSAGE_PAYLOAD_SIZE_BYTES)
                                         .build(),
                scheduledExecutor,
                client
            );

        this.deleteMessageBatchManager =
            new DeleteMessageBatchManager(
                RequestBatchConfiguration.builder(builder.overrideConfiguration).build(),
                scheduledExecutor,
                client
            );

        this.changeMessageVisibilityBatchManager =
            new ChangeMessageVisibilityBatchManager(
                RequestBatchConfiguration.builder(builder.overrideConfiguration).build(),
                scheduledExecutor,
                client
            );

        this.receiveMessageBatchManager =
            new ReceiveMessageBatchManager(client,
                                           scheduledExecutor,
                                           ResponseBatchConfiguration.builder(builder.overrideConfiguration).build());
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
        return this.receiveMessageBatchManager.batchRequest(request);
    }

    public static SqsAsyncBatchManager.Builder builder() {
        return new DefaultBuilder();
    }

    @Override
    public void close() {
        sendMessageBatchManager.close();
        deleteMessageBatchManager.close();
        changeMessageVisibilityBatchManager.close();
        receiveMessageBatchManager.close();
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
