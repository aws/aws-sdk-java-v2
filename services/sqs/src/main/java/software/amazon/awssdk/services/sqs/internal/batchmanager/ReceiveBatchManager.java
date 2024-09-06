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

import static software.amazon.awssdk.services.sqs.internal.batchmanager.ResponseBatchConfiguration.MAX_SUPPORTED_SQS_RECEIVE_MSG;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.utils.SdkAutoCloseable;

@SdkInternalApi
public class ReceiveBatchManager implements SdkAutoCloseable {

    private final SqsAsyncClient sqsClient;
    private final ScheduledExecutorService executor;
    private final ResponseBatchConfiguration config;
    private final String queueUrl;
    private final ReceiveQueueBuffer receiveQueueBuffer;
    private final QueueAttributesManager queueAttributesManager;

    public ReceiveBatchManager(SqsAsyncClient sqsClient, ScheduledExecutorService executor, ResponseBatchConfiguration config,
                               String queueUrl) {
        this.sqsClient = sqsClient;
        this.executor = executor;
        this.config = config;
        this.queueUrl = queueUrl;
        this.queueAttributesManager = new QueueAttributesManager(sqsClient, queueUrl);
        this.receiveQueueBuffer = ReceiveQueueBuffer.builder()
                                                    .executor(executor)
                                                    .sqsClient(sqsClient)
                                                    .config(config)
                                                    .queueUrl(queueUrl)
                                                    .queueAttributesManager(queueAttributesManager).build();
    }

    public CompletableFuture<ReceiveMessageResponse> processRequest(ReceiveMessageRequest rq) {
        if (receiveQueueBuffer.isShutDown()) {
            throw new IllegalStateException("The client has been shut down.");
        }
        int numMessages = rq.maxNumberOfMessages() != null ? rq.maxNumberOfMessages() : MAX_SUPPORTED_SQS_RECEIVE_MSG;

        return queueAttributesManager.getReceiveMessageTimeout(rq, config.messageMinWaitDuration()).thenCompose(waitTimeMs -> {
            CompletableFuture<ReceiveMessageResponse> receiveMessageFuture = new CompletableFuture<>();
            receiveQueueBuffer.receiveMessage(receiveMessageFuture, numMessages);
            executor.schedule(() -> receiveMessageFuture.complete(ReceiveMessageResponse.builder().build()),
                              waitTimeMs.toMillis(),
                              TimeUnit.MILLISECONDS);
            return receiveMessageFuture;

        });
    }

    @Override
    public void close() {
        receiveQueueBuffer.close();
    }
}
