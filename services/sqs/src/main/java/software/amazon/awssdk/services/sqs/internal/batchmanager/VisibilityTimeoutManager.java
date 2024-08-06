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

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.utils.Validate;


@SdkInternalApi
public class VisibilityTimeoutManager {

    private final SqsAsyncClient sqsClient;
    private final String queueUrl;
    private final AtomicReference<CompletableFuture<Duration>> defaultWaitTimeSecondsFuture = new AtomicReference<>();
    private final AtomicReference<CompletableFuture<Duration>> visibilityTimeoutSecondsFuture = new AtomicReference<>();
    private final Duration minReceiveWaitTime;

    public VisibilityTimeoutManager(SqsAsyncClient sqsClient, String queueUrl, Duration minReceiveWaitTime) {
        this.sqsClient = sqsClient;
        this.queueUrl = queueUrl;
        this.minReceiveWaitTime = minReceiveWaitTime;
    }

    /**
     * Retrieves the received message timeout based on the provided request and queue attributes.
     *
     * @param rq The receive message request
     * @return CompletableFuture with the calculated receive message timeout in milliseconds
     */
    public CompletableFuture<Duration> getReceiveMessageTimeout(ReceiveMessageRequest rq) {
        CompletableFuture<Duration> waitTimeFuture = defaultWaitTimeSecondsFuture.get();

        if (waitTimeFuture == null) {
            CompletableFuture<Duration> newWaitTimeFuture = new CompletableFuture<>();
            if (defaultWaitTimeSecondsFuture.compareAndSet(null, newWaitTimeFuture)) {
                fetchQueueWaitTime(newWaitTimeFuture);
            }
            waitTimeFuture = defaultWaitTimeSecondsFuture.get();
        }

        return waitTimeFuture.thenApply(waitTime -> calculateWaitTime(rq, waitTime));
    }

    /**
     * Fetches the queue wait time from SQS and completes the provided future with the result.
     *
     * @param newWaitTimeFuture The future to complete with the fetched wait time
     */
    private void fetchQueueWaitTime(CompletableFuture<Duration> newWaitTimeFuture) {
        GetQueueAttributesRequest request = GetQueueAttributesRequest.builder()
                                                                     .queueUrl(queueUrl)
                                                                     .attributeNames(
                                                                         QueueAttributeName.RECEIVE_MESSAGE_WAIT_TIME_SECONDS)
                                                                     .build();
        sqsClient.getQueueAttributes(request)
                 .thenApply(response -> {
                     String messageWaitTime =
                         Validate.notNull(response
                                              .attributes()
                                              .get(QueueAttributeName.RECEIVE_MESSAGE_WAIT_TIME_SECONDS),
                                          QueueAttributeName.RECEIVE_MESSAGE_WAIT_TIME_SECONDS +
                                          " attribute is null in sqs.");

                     return Duration.ofSeconds(Integer.parseInt(messageWaitTime));
                 })
                 .thenAccept(newWaitTimeFuture::complete)
                 .exceptionally(ex -> {
                     newWaitTimeFuture.completeExceptionally(ex);
                     return null;
                 });
    }

    /**
     * Calculates the wait time for receiving a message, ensuring it meets the minimum wait time.
     *
     * @param rq              The receive message request
     * @param defaultWaitTime The default wait time from the queue attributes
     * @return The calculated wait time in milliseconds
     */
    private Duration calculateWaitTime(ReceiveMessageRequest rq, Duration defaultWaitTime) {
        int waitTimeSeconds = (rq.waitTimeSeconds() != null) ? rq.waitTimeSeconds() : (int) defaultWaitTime.getSeconds();
        return Duration.ofMillis(Math.max(minReceiveWaitTime.toMillis(),
                                          TimeUnit.MILLISECONDS.convert(waitTimeSeconds, TimeUnit.SECONDS)));
    }

    /**
     * Retrieves the visibility timeout for the queue.
     *
     * @return CompletableFuture with the visibility timeout in nanoseconds
     */
    public CompletableFuture<Duration> getVisibilityTimeout() {
        CompletableFuture<Duration> timeoutFuture = visibilityTimeoutSecondsFuture.get();

        if (timeoutFuture == null) {
            CompletableFuture<Duration> newTimeoutFuture = new CompletableFuture<>();
            if (visibilityTimeoutSecondsFuture.compareAndSet(null, newTimeoutFuture)) {
                fetchVisibilityTimeout(newTimeoutFuture);
            }
            timeoutFuture = visibilityTimeoutSecondsFuture.get();
        }

        return timeoutFuture;
    }

    /**
     * Fetches the visibility timeout from SQS and completes the provided future with the result.
     *
     * @param newTimeoutFuture The future to complete with the fetched visibility timeout
     */
    private void fetchVisibilityTimeout(CompletableFuture<Duration> newTimeoutFuture) {
        GetQueueAttributesRequest request = GetQueueAttributesRequest.builder()
                                                                     .queueUrl(queueUrl)
                                                                     .attributeNames(QueueAttributeName.VISIBILITY_TIMEOUT)
                                                                     .build();
        sqsClient.getQueueAttributes(request)
                 .thenApply(response -> {
                     String visibilityTimeout =
                         Validate.notNull(response
                                              .attributes()
                                              .get(QueueAttributeName.VISIBILITY_TIMEOUT),
                                          QueueAttributeName.VISIBILITY_TIMEOUT +
                                          " attribute is null in sqs.");

                     return Duration.ofSeconds(Integer.parseInt(visibilityTimeout));
                 })
                 .thenAccept(newTimeoutFuture::complete)
                 .exceptionally(ex -> {
                     newTimeoutFuture.completeExceptionally(ex);
                     return null;
                 });
    }
}
