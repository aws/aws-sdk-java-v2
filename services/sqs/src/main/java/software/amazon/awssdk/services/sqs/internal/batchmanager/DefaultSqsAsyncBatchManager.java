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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.batchmanager.BatchOverrideConfiguration;
import software.amazon.awssdk.services.sqs.batchmanager.SqsAsyncBatchManager;
import software.amazon.awssdk.services.sqs.internal.batchmanager.core.BatchManager;
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
    private final  ScheduledExecutorService scheduledExecutor;
    private final  BatchOverrideConfiguration overrideConfiguration;

    private final BatchManager<SendMessageRequest, SendMessageResponse, SendMessageBatchResponse> sendMessageBatchManager;

    private final BatchManager<DeleteMessageRequest, DeleteMessageResponse, DeleteMessageBatchResponse> deleteMessageBatchManager;

    private final BatchManager<ChangeMessageVisibilityRequest, ChangeMessageVisibilityResponse,
            ChangeMessageVisibilityBatchResponse> changeMessageVisibilityBatchManager;
    private final BatchManager<ReceiveMessageRequest, ReceiveMessageResponse,
        ReceiveMessageResponse> receiveMessageBatchManager;

    private DefaultSqsAsyncBatchManager(DefaultBuilder builder) {
        this.client = Validate.notNull(builder.client, "client cannot be null");
        this.scheduledExecutor = Validate.notNull(builder.scheduledExecutor, "scheduledExecutor cannot be null");
        // TODO : create overrideConfiguration with Default values if null
        this.overrideConfiguration = builder.overrideConfiguration;

        sendMessageBatchManager = null;
        deleteMessageBatchManager = null;
        changeMessageVisibilityBatchManager = null;
        receiveMessageBatchManager = null;
    }

    @Override
    public CompletableFuture<SendMessageResponse> sendMessage(SendMessageRequest request) {
        return SqsAsyncBatchManager.super.sendMessage(request);
    }

    @Override
    public CompletableFuture<DeleteMessageResponse> deleteMessage(DeleteMessageRequest request) {
        return SqsAsyncBatchManager.super.deleteMessage(request);
    }

    @Override
    public CompletableFuture<ChangeMessageVisibilityResponse> changeMessageVisibility(ChangeMessageVisibilityRequest request) {
        return SqsAsyncBatchManager.super.changeMessageVisibility(request);
    }

    @Override
    public CompletableFuture<ReceiveMessageResponse> receiveMessage(ReceiveMessageRequest request) {
        return SqsAsyncBatchManager.super.receiveMessage(request);
    }

    public static SqsAsyncBatchManager.Builder builder() {
        return new DefaultBuilder();
    }

    @Override
    public void close() {
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
