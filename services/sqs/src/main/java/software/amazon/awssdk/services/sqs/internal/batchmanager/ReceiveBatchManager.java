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

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.batchmanager.ReceiveMessageCompletableFuture;
import software.amazon.awssdk.services.sqs.batchmanager.ReceiveQueueBuffer;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

@SdkInternalApi
public class ReceiveBatchManager {

    private final SqsAsyncClient sqsClient;
    private final ScheduledExecutorService executor;
    private final ResponseBatchConfiguration config;
    private final String queueUrl;
    private final ReceiveQueueBuffer receiveQueueBuffer;
    private final QueueAttributesManager queueAttributesManager;

    public ReceiveBatchManager(SqsAsyncClient sqsClient,
                               ScheduledExecutorService executor,
                               ResponseBatchConfiguration config,
                               String queueUrl) {
        this.sqsClient = sqsClient;
        this.executor = executor;
        this.config = config;
        this.queueUrl = queueUrl;
        this.queueAttributesManager = new QueueAttributesManager(sqsClient, queueUrl);
        this.receiveQueueBuffer = new ReceiveQueueBuffer(executor, sqsClient, config, queueUrl, queueAttributesManager);
    }

    public CompletableFuture<ReceiveMessageResponse> processRequest(ReceiveMessageRequest rq) {
        if (receiveQueueBuffer.isShutDown()) {
            throw new IllegalStateException("The client has been shut down.");
        }
        int numMessages = Optional.ofNullable(rq.maxNumberOfMessages()).orElse(10);

        return queueAttributesManager.getReceiveMessageTimeout(rq, config.minReceiveWaitTime())
                                     .thenCompose(waitTimeMs -> {
                                         ReceiveMessageCompletableFuture receiveMessageFuture =
                                             new ReceiveMessageCompletableFuture(numMessages, waitTimeMs);
                                         receiveQueueBuffer.receiveMessage(receiveMessageFuture);
                                         receiveMessageFuture.startWaitTimer(executor);
                                         return receiveMessageFuture.responseCompletableFuture();
                                     });
    }

    public void shutdown() {
        receiveQueueBuffer.shutdown();
        executor.shutdown();
    }
}
