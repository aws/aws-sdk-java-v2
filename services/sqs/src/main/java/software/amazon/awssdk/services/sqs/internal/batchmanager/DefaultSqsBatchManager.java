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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
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
    private boolean createdExecutor;
    private final ExecutorService executor;
    private final BatchManager<SendMessageRequest, SendMessageResponse, SendMessageBatchResponse> sendMessageBatchManager;
    private final BatchManager<DeleteMessageRequest, DeleteMessageResponse, DeleteMessageBatchResponse> deleteMessageBatchManager;
    private final BatchManager<ChangeMessageVisibilityRequest, ChangeMessageVisibilityResponse,
        ChangeMessageVisibilityBatchResponse> changeVisibilityBatchManager;

    // TODO: When generating executor, do it like it is done here in
    private DefaultSqsBatchManager(DefaultBuilder builder) {
        this.client = builder.client;
        SqsBatchConfiguration config = new SqsBatchConfiguration(builder.overrideConfiguration);
        BatchOverrideConfiguration overrideConfiguration = BatchOverrideConfiguration.builder()
                                                                                     .maxBatchItems(config.maxBatchItems())
                                                                                     .maxBatchOpenInMs(config.maxBatchOpenInMs())
                                                                                     .build();
        ScheduledExecutorService scheduledExecutor = builder.scheduledExecutor;
        ThreadFactory threadFactory = new ThreadFactoryBuilder().threadNamePrefix("DefaultSqsBatchManager").build();
        if (builder.executor == null) {
            this.executor = createDefaultExecutor(threadFactory);
            this.createdExecutor = true;
        } else {
            this.executor = builder.executor;
            this.createdExecutor = false;
        }
        this.sendMessageBatchManager = BatchManager.builder(SendMessageRequest.class, SendMessageResponse.class,
                                                            SendMessageBatchResponse.class)
                                        .batchFunction(sendMessageBatchFunction(client, executor))
                                        .responseMapper(sendMessageResponseMapper())
                                        .batchKeyMapper(sendMessageBatchKeyMapper())
                                        .overrideConfiguration(overrideConfiguration)
                                        .scheduledExecutor(scheduledExecutor)
                                        .build();
        this.deleteMessageBatchManager = BatchManager.builder(DeleteMessageRequest.class, DeleteMessageResponse.class,
                                                              DeleteMessageBatchResponse.class)
                                                     .batchFunction(deleteMessageBatchFunction(client, executor))
                                                     .responseMapper(deleteMessageResponseMapper())
                                                     .batchKeyMapper(deleteMessageBatchKeyMapper())
                                                     .overrideConfiguration(overrideConfiguration)
                                                     .scheduledExecutor(scheduledExecutor)
                                                     .build();
        this.changeVisibilityBatchManager = BatchManager.builder(ChangeMessageVisibilityRequest.class,
                                                                 ChangeMessageVisibilityResponse.class,
                                                                 ChangeMessageVisibilityBatchResponse.class)
                                                        .batchFunction(changeVisibilityBatchFunction(client, executor))
                                                        .responseMapper(changeVisibilityResponseMapper())
                                                        .batchKeyMapper(changeVisibilityBatchKeyMapper())
                                                        .overrideConfiguration(overrideConfiguration)
                                                        .scheduledExecutor(scheduledExecutor)
                                                        .build();
    }

    @SdkTestInternalApi
    public DefaultSqsBatchManager(SqsClient client, ExecutorService executor, boolean createdExecutor,
                                  BatchManager<SendMessageRequest, SendMessageResponse, SendMessageBatchResponse>
                                      sendMessageBatchManager,
                                  BatchManager<DeleteMessageRequest, DeleteMessageResponse, DeleteMessageBatchResponse>
                                          deleteMessageBatchManager,
                                  BatchManager<ChangeMessageVisibilityRequest, ChangeMessageVisibilityResponse,
                                      ChangeMessageVisibilityBatchResponse> changeVisibilityBatchManager) {
        this.client = client;
        this.executor = executor;
        this.createdExecutor = createdExecutor;
        this.sendMessageBatchManager = sendMessageBatchManager;
        this.deleteMessageBatchManager = deleteMessageBatchManager;
        this.changeVisibilityBatchManager = changeVisibilityBatchManager;
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
        changeVisibilityBatchManager.close();
        if (createdExecutor) {
            executor.shutdownNow();
        }
    }

    private ExecutorService createDefaultExecutor(ThreadFactory threadFactory) {
        int processors = Runtime.getRuntime().availableProcessors();
        int corePoolSize = Math.max(8, processors);
        int maxPoolSize = Math.max(64, processors * 2);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(corePoolSize, maxPoolSize,
                                                             10, TimeUnit.SECONDS,
                                                             new LinkedBlockingQueue<>(1_000),
                                                             threadFactory);
        // Allow idle core threads to time out
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    public static SqsBatchManager.Builder builder() {
        return new DefaultBuilder();
    }

    public static final class DefaultBuilder implements Builder {
        private BatchOverrideConfiguration overrideConfiguration;
        private SqsClient client;
        private ScheduledExecutorService scheduledExecutor;
        private ExecutorService executor;

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

        @Override
        public Builder scheduledExecutor(ScheduledExecutorService scheduledExecutor) {
            this.scheduledExecutor = scheduledExecutor;
            return this;
        }

        @Override
        public Builder executor(ExecutorService executor) {
            this.executor = executor;
            return this;
        }

        public SqsBatchManager build() {
            return new DefaultSqsBatchManager(this);
        }
    }
}
