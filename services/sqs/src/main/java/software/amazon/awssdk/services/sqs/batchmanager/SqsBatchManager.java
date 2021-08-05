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
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.utils.SdkAutoCloseable;

@SdkPublicApi
public interface SqsBatchManager extends SdkAutoCloseable {

    /**
     * Buffers outgoing SendMessageRequests on the client and sends them as a SendMessageBatchRequest to SQS. Requests are
     * batched together according to a batchKey and are sent periodically to SQS. If the number of requests for a batchKey
     * reaches or exceeds the configured max items, then the requests are immediately flushed and the timeout on the periodic
     * flush is reset.
     * By default, messages are batched with a maximum batch size of 10. These settings can be customized via the configuration.
     *
     * @param message the outgoing SendMessageRequest.
     * @return a CompletableFuture of the corresponding SendMessageResponse.
     */
    CompletableFuture<SendMessageResponse> sendMessage(SendMessageRequest message);

    /**
     * Create a builder that can be used to configure and create a {@link SqsBatchManager}.
     * @return a builder
     */
    static Builder builder() {
        throw new UnsupportedOperationException();
    }

    /**
     * Create an instance of {@link SqsBatchManager} with the default configuration.
     *
     * @return an instance of {@link SqsBatchManager}
     */
    static SqsBatchManager create() {
        throw new UnsupportedOperationException();
    }

    interface Builder {

        /**
         * Defines overrides to the default BatchManager configuration
         *
         * @param overrideConfiguration the override configuration to set
         * @return a reference to this object so that method calls can be chained together.
         */
        Builder overrideConfiguration(BatchOverrideConfiguration overrideConfiguration);

        SqsBatchManager build();
    }
}
