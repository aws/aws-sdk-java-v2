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

package software.amazon.awssdk.services.sqs.batchmanager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.internal.batchmanager.DefaultSqsAsyncBatchManager;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * Batch manager for implementing automatic batching with an SQS async client. Create an instance using {@link #builder()}.
 * <p>
 * This manager buffers client requests and sends them in batches to the service, enhancing efficiency by reducing the number of
 * API requests. Requests are buffered until they reach a specified limit or a timeout occurs.
 */
@SdkPublicApi
public interface SqsAsyncBatchManager extends SdkAutoCloseable {

    /**
     * Creates a builder for configuring and creating an {@link SqsAsyncBatchManager}.
     *
     * @return A new builder.
     */
    static Builder builder() {
        return DefaultSqsAsyncBatchManager.builder();
    }

    /**
     * Buffers and batches {@link SendMessageRequest}s, sending them as a
     * {@link software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest}. Requests are grouped by queue URL and override
     * configuration, and sent when the batch size or timeout is reached.
     *
     * @param request The SendMessageRequest to be buffered.
     * @return CompletableFuture of the corresponding {@link SendMessageResponse}.
     */
    default CompletableFuture<SendMessageResponse> sendMessage(SendMessageRequest request) {
        throw new UnsupportedOperationException();
    }


    /**
     * Buffers and batches {@link SendMessageRequest}s using a {@link Consumer} to configure the request,
     * sending them as a {@link software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest}.
     * Requests are grouped by queue URL and override configuration, and sent when the batch size or timeout is reached.
     *
     * @param sendMessageRequest A {@link Consumer} to configure the SendMessageRequest to be buffered.
     * @return CompletableFuture of the corresponding {@link SendMessageResponse}.
     */
    default CompletableFuture<SendMessageResponse> sendMessage(Consumer<SendMessageRequest.Builder> sendMessageRequest) {
        return sendMessage(SendMessageRequest.builder().applyMutation(sendMessageRequest).build());
    }

    /**
     * Buffers and batches {@link DeleteMessageRequest}s, sending them as a
     * {@link software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest}. Requests are grouped by queue URL and override
     * configuration, and sent when the batch size or timeout is reached.
     *
     * @param request The DeleteMessageRequest to be buffered.
     * @return CompletableFuture of the corresponding {@link DeleteMessageResponse}.
     */
    default CompletableFuture<DeleteMessageResponse> deleteMessage(DeleteMessageRequest request) {
        throw new UnsupportedOperationException();
    }


    /**
     * Buffers and batches {@link DeleteMessageRequest}s using a {@link Consumer} to configure the request,
     * sending them as a {@link software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest}.
     * Requests are grouped by queue URL and override configuration, and sent when the batch size or timeout is reached.
     *
     * @param request A {@link Consumer} to configure the DeleteMessageRequest to be buffered.
     * @return CompletableFuture of the corresponding {@link DeleteMessageResponse}.
     */
    default CompletableFuture<DeleteMessageResponse> deleteMessage(Consumer<DeleteMessageRequest.Builder> request) {
        return deleteMessage(DeleteMessageRequest.builder().applyMutation(request).build());
    }

    /**
     * Buffers and batches {@link ChangeMessageVisibilityRequest}s, sending them as a
     * {@link software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequest}. Requests are grouped by queue URL
     * and override configuration, and sent when the batch size or timeout is reached.
     *
     * @param request The ChangeMessageVisibilityRequest to be buffered.
     * @return CompletableFuture of the corresponding {@link ChangeMessageVisibilityResponse}.
     */
    default CompletableFuture<ChangeMessageVisibilityResponse> changeMessageVisibility(ChangeMessageVisibilityRequest request) {
        throw new UnsupportedOperationException();
    }

    /**
     * Buffers and batches {@link ChangeMessageVisibilityRequest}s using a {@link Consumer} to configure the request,
     * sending them as a {@link software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequest}.
     * Requests are grouped by queue URL and override configuration, and sent when the batch size or timeout is reached.
     *
     * @param request A {@link Consumer} to configure the ChangeMessageVisibilityRequest to be buffered.
     * @return CompletableFuture of the corresponding {@link ChangeMessageVisibilityResponse}.
     */
    default CompletableFuture<ChangeMessageVisibilityResponse> changeMessageVisibility(
        Consumer<ChangeMessageVisibilityRequest.Builder> request) {
        return changeMessageVisibility(ChangeMessageVisibilityRequest.builder().applyMutation(request).build());
    }


    /**
     * Buffers and retrieves messages with {@link ReceiveMessageRequest}, with a maximum of 10 messages per request. Returns an
     * empty message if no messages are available in SQS.
     *
     * @param request The ReceiveMessageRequest.
     * @return CompletableFuture of the corresponding {@link ReceiveMessageResponse}.
     */
    default CompletableFuture<ReceiveMessageResponse> receiveMessage(ReceiveMessageRequest request) {
        throw new UnsupportedOperationException();
    }

    /**
     * Buffers and retrieves messages with {@link ReceiveMessageRequest} using a {@link Consumer} to configure the request,
     * with a maximum of 10 messages per request. Returns an empty message if no messages are available in SQS.
     *
     * @param request A {@link Consumer} to configure the ReceiveMessageRequest.
     * @return CompletableFuture of the corresponding {@link ReceiveMessageResponse}.
     */
    default CompletableFuture<ReceiveMessageResponse> receiveMessage(
        Consumer<ReceiveMessageRequest.Builder> request) {
        return receiveMessage(ReceiveMessageRequest.builder().applyMutation(request).build());
    }


    interface Builder {

        /**
         * Sets custom overrides for the BatchManager configuration.
         *
         * @param overrideConfiguration The configuration overrides.
         * @return This builder for method chaining.
         */
        Builder overrideConfiguration(BatchOverrideConfiguration overrideConfiguration);

        /**
         * Sets custom overrides for the BatchManager configuration using a {@link Consumer} to configure the overrides.
         *
         * @param overrideConfiguration A {@link Consumer} to configure the {@link BatchOverrideConfiguration}.
         * @return This builder for method chaining.
         */
        default Builder overrideConfiguration(Consumer<BatchOverrideConfiguration.Builder> overrideConfiguration) {
            return overrideConfiguration(BatchOverrideConfiguration.builder().applyMutation(overrideConfiguration).build());
        }

        /**
         * Sets a custom {@link software.amazon.awssdk.services.sqs.SqsClient} for polling resources. This client must be closed
         * by the caller.
         *
         * @param client The SqsAsyncClient to use.
         * @return This builder for method chaining.
         * @throws NullPointerException If client is null.
         */
        Builder client(SqsAsyncClient client);

        /**
         * Sets a custom {@link ScheduledExecutorService} for periodic buffer flushes. This executor must be closed by the
         * caller.
         *
         * @param scheduledExecutor The executor to use.
         * @return This builder for method chaining.
         */
        Builder scheduledExecutor(ScheduledExecutorService scheduledExecutor);

        /**
         * Builds an instance of {@link SqsAsyncBatchManager} based on the supplied configurations.
         *
         * @return An initialized SqsAsyncBatchManager.
         */
        SqsAsyncBatchManager build();
    }
}
