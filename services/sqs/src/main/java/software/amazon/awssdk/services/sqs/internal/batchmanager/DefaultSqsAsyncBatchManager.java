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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
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
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class DefaultSqsAsyncBatchManager implements SqsAsyncBatchManager {
    private static final Logger log = Logger.loggerFor(DefaultSqsAsyncBatchManager.class);

    private final SqsAsyncClient client;

    private final SendMessageBatchManager sendMessageBatchManager;

    private final DeleteMessageBatchManager deleteMessageBatchManager;

    private final ChangeMessageVisibilityBatchManager changeMessageVisibilityBatchManager;

    private final ReceiveMessageBatchManager receiveMessageBatchManager;

    private final List<RequestBatchManager<?, ?, ?>> requestBatchManagers;

    private final AtomicBoolean closed = new AtomicBoolean(false);

    private DefaultSqsAsyncBatchManager(DefaultBuilder builder) {
        this.client = Validate.notNull(builder.client, "client cannot be null");
        ScheduledExecutorService scheduledExecutor  = Validate.notNull(builder.scheduledExecutor,
                                                                       "scheduledExecutor cannot be null");
        this.sendMessageBatchManager =
            new SendMessageBatchManager(
                RequestBatchConfiguration.builder(builder.overrideConfiguration)
                                         .maxBatchBytesSize(MAX_SEND_MESSAGE_PAYLOAD_SIZE_BYTES)
                                         .shutdownTimeout(builder.shutdownTimeout)
                                         .build(),
                scheduledExecutor,
                client
            );

        this.deleteMessageBatchManager =
            new DeleteMessageBatchManager(
                RequestBatchConfiguration.builder(builder.overrideConfiguration)
                                         .shutdownTimeout(builder.shutdownTimeout)
                                         .build(),
                scheduledExecutor,
                client
            );

        this.changeMessageVisibilityBatchManager =
            new ChangeMessageVisibilityBatchManager(
                RequestBatchConfiguration.builder(builder.overrideConfiguration)
                                         .shutdownTimeout(builder.shutdownTimeout)
                                         .build(),
                scheduledExecutor,
                client
            );

        requestBatchManagers = new ArrayList<>(3);
        requestBatchManagers.add(sendMessageBatchManager);
        requestBatchManagers.add(deleteMessageBatchManager);
        requestBatchManagers.add(changeMessageVisibilityBatchManager);

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
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        List<CompletableFuture<Void>> futures =
            requestBatchManagers.stream().map(requestBatchManager -> requestBatchManager.closeAndDispatch())
                                                                    .collect(Collectors.toList());

        awaitQuietly(CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])),
                     sendMessageBatchManager.shutdownTimeout());

        requestBatchManagers.forEach(requestBatchManager -> requestBatchManager.cancelPending());
        receiveMessageBatchManager.close();
    }

    private static void awaitQuietly(CompletableFuture<?> pending, Duration timeout) {
        try {
            pending.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (TimeoutException e) {
            log.debug(() -> "Timed out waiting for in-flight batch sends to complete during close; "
                            + "cancelling outstanding requests.");
        } catch (ExecutionException e) {
            // A send failed; its caller future is already settled with the failure. Nothing to do here.
        }
    }

    public static final class DefaultBuilder implements SqsAsyncBatchManager.Builder {
        private SqsAsyncClient client;
        private BatchOverrideConfiguration overrideConfiguration;
        private ScheduledExecutorService scheduledExecutor;
        private Duration shutdownTimeout;

        private DefaultBuilder() {
        }

        @SdkTestInternalApi
        DefaultBuilder shutdownTimeout(Duration shutdownTimeout) {
            this.shutdownTimeout = shutdownTimeout;
            return this;
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
