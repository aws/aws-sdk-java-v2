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

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.SdkAutoCloseable;

@SdkInternalApi
public class ReceiveMessageBatchManager implements SdkAutoCloseable {

    private static final Logger log = Logger.loggerFor(ReceiveMessageBatchManager.class);

    private final SqsAsyncClient sqsClient;
    private final ScheduledExecutorService executor;
    private final ResponseBatchConfiguration config;
    private final Map<String, ReceiveBatchManager> receiveBatchManagerMap = new ConcurrentHashMap<>();

    public ReceiveMessageBatchManager(SqsAsyncClient sqsClient,
                                      ScheduledExecutorService executor,
                                      ResponseBatchConfiguration config) {
        this.sqsClient = sqsClient;
        this.executor = executor;
        this.config = config;


    }

    public CompletableFuture<ReceiveMessageResponse> batchRequest(ReceiveMessageRequest request) {
        String ineligibleReason = checkBatchingEligibility(request);
        if (ineligibleReason == null) {
            return receiveBatchManagerMap.computeIfAbsent(generateBatchKey(request), key -> createReceiveBatchManager(request))
                                         .processRequest(request);
        } else {
            log.debug(() -> String.format("Batching skipped. Reason: %s", ineligibleReason));
            return sqsClient.receiveMessage(request);
        }
    }

    /**
     * Generates a unique key for batch processing based on the queue URL and any override configuration.
     *
     * @param request The receive message request.
     * @return The generated batch key.
     */
    private String generateBatchKey(ReceiveMessageRequest request) {
        return request.overrideConfiguration()
                      .map(config -> request.queueUrl() + config.hashCode())
                      .orElse(request.queueUrl());
    }

    private ReceiveBatchManager createReceiveBatchManager(ReceiveMessageRequest request) {
        return new ReceiveBatchManager(sqsClient, executor, config, request.queueUrl());
    }

    @Override
    public void close() {
        receiveBatchManagerMap.values().forEach(ReceiveBatchManager::close);
    }

    private String checkBatchingEligibility(ReceiveMessageRequest rq) {
        if (!hasCompatibleAttributes(rq)) {
            return "Incompatible attributes.";
        }
        if (rq.visibilityTimeout() != null) {
            return "Visibility timeout is set.";
        }
        if (!isBufferingEnabled()) {
            return "Buffering is disabled.";
        }
        if (rq.overrideConfiguration().isPresent()) {
            return "Request has override configurations.";
        }
        return null;
    }

    private boolean hasCompatibleAttributes(ReceiveMessageRequest rq) {
        return !rq.hasAttributeNames()
               && hasCompatibleSystemAttributes(rq)
               && hasCompatibleMessageAttributes(rq);
    }

    private boolean hasCompatibleSystemAttributes(ReceiveMessageRequest rq) {
        return !rq.hasMessageSystemAttributeNames()
               || config.messageSystemAttributeNames().equals(rq.messageSystemAttributeNames());
    }

    private boolean hasCompatibleMessageAttributes(ReceiveMessageRequest rq) {
        return !rq.hasMessageAttributeNames()
               || config.receiveMessageAttributeNames().equals(rq.messageAttributeNames());
    }

    private boolean isBufferingEnabled() {
        return config.maxInflightReceiveBatches() > 0 && config.maxDoneReceiveBatches() > 0;
    }

}