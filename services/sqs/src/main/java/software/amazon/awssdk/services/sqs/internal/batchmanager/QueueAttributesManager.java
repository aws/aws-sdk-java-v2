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


import static software.amazon.awssdk.services.sqs.internal.batchmanager.RequestBatchManager.USER_AGENT_APPLIER;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Validate;


/**
 * The {@code QueueAttributesManager} class is responsible for managing and retrieving specific attributes
 * of an AWS SQS queue, such as message wait time and visibility timeout. It efficiently caches these attributes
 * to minimize redundant API calls to SQS, ensuring that the attributes are fetched only once and reused in subsequent requests.
 *
 * <p>This class uses an {@link AtomicReference} to maintain the state of the attribute map, allowing concurrent access
 * and handling cases where the fetching of attributes may fail. If an error occurs during the retrieval of attributes,
 * the state is reset to allow for a fresh attempt in subsequent calls.</p>
 *
 * <p>The class provides methods to get the visibility timeout and calculate the message receive timeout, which
 * are asynchronously retrieved and processed using {@link CompletableFuture}. These methods handle cancellation
 * scenarios by cancelling the SQS request if the calling future is cancelled.</p>
 *
 * <p>This class is intended for internal use and is marked with the {@link SdkInternalApi} annotation.</p>
 */
@SdkInternalApi
public final class QueueAttributesManager {

    private static final List<QueueAttributeName> QUEUE_ATTRIBUTE_NAMES =
        Arrays.asList(QueueAttributeName.RECEIVE_MESSAGE_WAIT_TIME_SECONDS,
                      QueueAttributeName.VISIBILITY_TIMEOUT);
    private final SqsAsyncClient sqsClient;
    private final String queueUrl;
    private final AtomicReference<CompletableFuture<Map<QueueAttributeName, String>>> queueAttributeMap = new AtomicReference<>();

    public QueueAttributesManager(SqsAsyncClient sqsClient, String queueUrl) {
        this.sqsClient = sqsClient;
        this.queueUrl = queueUrl;
    }

    /**
     * Retrieves the received message timeout based on the provided request and queue attributes.
     *
     * @param rq                 The receive message request
     * @param configuredWaitTime The configured minimum wait time
     * @return CompletableFuture with the calculated receive message timeout in milliseconds
     */
    public CompletableFuture<Duration> getReceiveMessageTimeout(ReceiveMessageRequest rq, Duration configuredWaitTime) {
        Integer waitTimeSeconds = rq.waitTimeSeconds();
        if (waitTimeSeconds != null) {
            long waitTimeMillis = TimeUnit.SECONDS.toMillis(waitTimeSeconds);
            return CompletableFuture.completedFuture(Duration.ofMillis(Math.max(configuredWaitTime.toMillis(), waitTimeMillis)));
        }

        CompletableFuture<Map<QueueAttributeName, String>> attributeFuture = getAttributeMap();
        CompletableFuture<Duration> resultFuture = attributeFuture.thenApply(attributes -> {
            String waitTimeSecondsStr = attributes.get(QueueAttributeName.RECEIVE_MESSAGE_WAIT_TIME_SECONDS);
            long waitTimeFromSqsMillis = TimeUnit.SECONDS.toMillis(Long.parseLong(waitTimeSecondsStr));
            return Duration.ofMillis(Math.max(configuredWaitTime.toMillis(), waitTimeFromSqsMillis));
        });

        return CompletableFutureUtils.forwardExceptionTo(resultFuture, attributeFuture);
    }

    /**
     * Retrieves the visibility timeout for the queue.
     *
     * @return CompletableFuture with the visibility timeout in nanoseconds
     */
    public CompletableFuture<Duration> getVisibilityTimeout() {
        CompletableFuture<Map<QueueAttributeName, String>> attributeFuture = getAttributeMap();
        CompletableFuture<Duration> resultFuture = attributeFuture.thenApply(attributes -> {
            String visibilityTimeoutStr = attributes.get(QueueAttributeName.VISIBILITY_TIMEOUT);
            return Duration.ofSeconds(Integer.parseInt(visibilityTimeoutStr));
        });

        return CompletableFutureUtils.forwardExceptionTo(resultFuture, attributeFuture);
    }

    /**
     * Retrieves the queue attributes based on the predefined attribute names.
     *
     * @return CompletableFuture with the map of attribute names and their values.
     */
    private CompletableFuture<Map<QueueAttributeName, String>> getAttributeMap() {
        CompletableFuture<Map<QueueAttributeName, String>> future = queueAttributeMap.get();

        if (future == null || future.isCompletedExceptionally()) {
            CompletableFuture<Map<QueueAttributeName, String>> newFuture = new CompletableFuture<>();

            if (queueAttributeMap.compareAndSet(future, newFuture)) {
                fetchQueueAttributes().whenComplete((r, t) -> {
                    if (t != null) {
                        newFuture.completeExceptionally(t);
                    } else {
                        newFuture.complete(r);
                    }
                });
                return newFuture;
            } else {
                newFuture.cancel(true);
                return queueAttributeMap.get();
            }
        }
        return future;
    }

    /**
     * Fetches the queue attributes from SQS and completes the provided future with the result.
     *
     * @return CompletableFuture with the map of attribute names and values.
     */
    private CompletableFuture<Map<QueueAttributeName, String>> fetchQueueAttributes() {
        GetQueueAttributesRequest request = GetQueueAttributesRequest.builder()
                                                                     .queueUrl(queueUrl)
                                                                     .attributeNames(QUEUE_ATTRIBUTE_NAMES)
                                                                     .overrideConfiguration(o -> o
                                                                         .applyMutation(USER_AGENT_APPLIER))
                                                                     .build();

        return sqsClient.getQueueAttributes(request)
                        .thenApply(response -> {
                            Map<QueueAttributeName, String> attributes = response.attributes();
                            Validate.notNull(attributes.get(QueueAttributeName.RECEIVE_MESSAGE_WAIT_TIME_SECONDS),
                                             QueueAttributeName.RECEIVE_MESSAGE_WAIT_TIME_SECONDS
                                             + " attribute is null in SQS.");
                            Validate.notNull(attributes.get(QueueAttributeName.VISIBILITY_TIMEOUT),
                                             QueueAttributeName.VISIBILITY_TIMEOUT + " attribute is null in SQS.");
                            return attributes.entrySet().stream()
                                             .filter(entry -> QUEUE_ATTRIBUTE_NAMES.contains(entry.getKey()))
                                             .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                        });
    }
}
