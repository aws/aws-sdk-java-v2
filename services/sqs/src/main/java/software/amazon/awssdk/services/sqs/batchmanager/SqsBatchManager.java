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
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.batchmanager.BatchOverrideConfiguration;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.internal.batchmanager.DefaultSqsBatchManager;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * Batch manager class that implements automatic batching features for a SQS sync client. This can be created using the static
 * {@link #builder()} method.
 */
@SdkPublicApi
public interface SqsBatchManager extends SdkAutoCloseable {

    /**
     * Buffers outgoing SendMessageRequests on the client and sends them as a SendMessageBatchRequest to SQS. Requests are
     * batched together according to a batchKey and are sent periodically to SQS. If the number of requests for a batchKey
     * reaches or exceeds the configured max items, then the requests are immediately flushed and the timeout on the periodic
     * flush is reset.
     * <p>
     * By default, messages are batched with a maximum batch size of 10. These settings can be customized via the configuration.
     *
     * @param message the outgoing SendMessageRequest.
     * @return a CompletableFuture of the corresponding SendMessageResponse.
     */
    CompletableFuture<SendMessageResponse> sendMessage(SendMessageRequest message);

    /**
     * Buffers outgoing ChangeMessageVisibilityRequests on the client and sends them as a ChangeMessageVisibilityBatchRequest to
     * SQS. Requests are batched together according to a batchKey and are sent periodically to SQS. If the number of requests
     * for a batchKey reaches or exceeds the configured max items, then the requests are immediately flushed and the timeout on
     * the periodic flush is reset.
     * <p>
     * By default, messages are batched with a maximum batch size of 10. These settings can be customized via the configuration.
     *
     * @param changeRequest the outgoing ChangeMessageVisibilityRequest.
     * @return a CompletableFuture of the corresponding ChangeMessageVisibilityResponse.
     */
    CompletableFuture<ChangeMessageVisibilityResponse> changeMessageVisibility(ChangeMessageVisibilityRequest changeRequest);

    /**
     * Buffers outgoing DeleteMessageRequests on the client and sends them as a DeleteMessageBatchRequest to SQS. Requests are
     * batched together according to a batchKey and are sent periodically to SQS. If the number of requests for a batchKey
     * reaches or exceeds the configured max items, then the requests are immediately flushed and the timeout on the periodic
     * flush is reset.
     * <p>
     * By default, messages are batched with a maximum batch size of 10. These settings can be customized via the configuration.
     *
     * @param deleteRequest the outgoing DeleteMessageRequest.
     * @return a CompletableFuture of the corresponding DeleteMessageResponse.
     */
    CompletableFuture<DeleteMessageResponse> deleteMessage(DeleteMessageRequest deleteRequest);

    /**
     * Create a builder that can be used to configure and create a {@link SqsBatchManager}.
     *
     * @return a builder
     */
    static Builder builder() {
        return DefaultSqsBatchManager.builder();
    }

    /**
     * Create an instance of {@link SqsBatchManager} with the default configuration.
     *
     * @return an instance of {@link SqsBatchManager}
     */
    static SqsBatchManager create() {
        return DefaultSqsBatchManager.builder().build();
    }

    interface Builder {

        /**
         * Defines overrides to the default BatchManager configuration
         *
         * @param overrideConfiguration the override configuration to set
         * @return a reference to this object so that method calls can be chained together.
         */
        Builder overrideConfiguration(BatchOverrideConfiguration overrideConfiguration);

        /**
         * Sets a custom {@link SqsClient} that will be used to poll the resource.
         * <p>
         * This SDK client must be closed by the caller when it is ready to be disposed. The SDK will not close the
         * client when the BatchManager is closed.
         *
         * @param client the client used to send and receive batch messages.
         * @return a reference to this object so that method calls can be chained together.
         */
        Builder client(SqsClient client);

        /**
         * Builds an instance of {@link SqsBatchManager} based on the configurations supplied to this builder.
         *
         * @return An initialized {@link SqsBatchManager}
         */
        SqsBatchManager build();
    }
}
