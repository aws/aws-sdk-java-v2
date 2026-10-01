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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.batchmanager.BatchOverrideConfiguration;
import software.amazon.awssdk.services.sqs.batchmanager.SqsAsyncBatchManager;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResultEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

/**
 * Close/shutdown behavior of the real {@link DefaultSqsAsyncBatchManager} (and, through it, the real write batch
 * managers), driven over a mock {@link SqsAsyncClient} whose batch-send futures the test controls. Lives in the
 * internal package to reach the package-private {@link DefaultSqsAsyncBatchManager.DefaultBuilder#shutdownTimeout}
 * test seam so a short timeout can be injected.
 */
class DefaultSqsAsyncBatchManagerTest {

    private static final String QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/123456789012/q";

    private ScheduledExecutorService executor;

    @BeforeEach
    void setUp() {
        executor = Executors.newScheduledThreadPool(2);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    private SqsAsyncBatchManager batchManager(SqsAsyncClient client, int maxBatchSize, Duration sendFrequency,
                                              Duration shutdownTimeout) {
        DefaultSqsAsyncBatchManager.DefaultBuilder builder =
            (DefaultSqsAsyncBatchManager.DefaultBuilder) DefaultSqsAsyncBatchManager.builder();
        builder.client(client);
        builder.scheduledExecutor(executor);
        builder.overrideConfiguration(BatchOverrideConfiguration.builder()
                                                                .maxBatchSize(maxBatchSize)
                                                                .sendRequestFrequency(sendFrequency)
                                                                .build());
        builder.shutdownTimeout(shutdownTimeout);
        return builder.build();
    }

    @Test
    @Timeout(20)
    void close_boundsShutdownToOneSharedTimeout_notPerManager() {
        Duration timeout = Duration.ofMillis(400);
        SqsAsyncClient client = mock(SqsAsyncClient.class);
        when(client.sendMessageBatch(any(SendMessageBatchRequest.class)))
            .thenReturn(new CompletableFuture<SendMessageBatchResponse>());
        when(client.deleteMessageBatch(any(DeleteMessageBatchRequest.class)))
            .thenReturn(new CompletableFuture<DeleteMessageBatchResponse>());
        when(client.changeMessageVisibilityBatch(any(ChangeMessageVisibilityBatchRequest.class)))
            .thenReturn(new CompletableFuture<ChangeMessageVisibilityBatchResponse>());

        SqsAsyncBatchManager batchManager = batchManager(client, 10, Duration.ofHours(1), timeout);
        batchManager.sendMessage(r -> r.queueUrl(QUEUE_URL).messageBody("m"));
        batchManager.deleteMessage(r -> r.queueUrl(QUEUE_URL).receiptHandle("rh"));
        batchManager.changeMessageVisibility(r -> r.queueUrl(QUEUE_URL).receiptHandle("rh").visibilityTimeout(30));

        long start = System.nanoTime();
        batchManager.close();
        long closeMillis = (System.nanoTime() - start) / 1_000_000;

        assertThat(closeMillis).as("close() should wait about one timeout").isGreaterThanOrEqualTo(300);
        assertThat(closeMillis)
            .as("close() must be bounded by ONE shared timeout, not one per manager (3x would be ~%d ms)",
                3 * timeout.toMillis())
            .isLessThan(2 * timeout.toMillis());
    }

    @Test
    @Timeout(20)
    void close_flushesBufferedPartialBatchExactlyOnce() {
        SqsAsyncClient client = mock(SqsAsyncClient.class);
        when(client.sendMessageBatch(any(SendMessageBatchRequest.class)))
            .thenReturn(new CompletableFuture<SendMessageBatchResponse>());

        SqsAsyncBatchManager batchManager = batchManager(client, 10, Duration.ofHours(1), Duration.ofMillis(300));
        for (int i = 0; i < 5; i++) {
            int n = i;
            batchManager.sendMessage(r -> r.queueUrl(QUEUE_URL).messageBody("m" + n));
        }

        batchManager.close();

        verify(client, times(1)).sendMessageBatch(any(SendMessageBatchRequest.class));
    }

    @Test
    @Timeout(20)
    void close_flushesFullThenResidualPartialBatch_eachExactlyOnce() {
        SqsAsyncClient client = mock(SqsAsyncClient.class);
        when(client.sendMessageBatch(any(SendMessageBatchRequest.class)))
            .thenReturn(new CompletableFuture<SendMessageBatchResponse>());

        SqsAsyncBatchManager batchManager = batchManager(client, 10, Duration.ofHours(1), Duration.ofMillis(300));
        for (int i = 0; i < 15; i++) {
            int n = i;
            batchManager.sendMessage(r -> r.queueUrl(QUEUE_URL).messageBody("m" + n));
        }

        batchManager.close();

        // 10 auto-flush a full batch during sendMessage, the residual 5 are drained on close: two sends, each once.
        verify(client, times(2)).sendMessageBatch(any(SendMessageBatchRequest.class));
    }

    @Test
    @Timeout(20)
    void close_completesCallerWithRealResult_whenSendCompletesWithinTimeout() throws Exception {
        SqsAsyncClient client = mock(SqsAsyncClient.class);
        CompletableFuture<SendMessageBatchResponse> sendFuture = new CompletableFuture<>();
        when(client.sendMessageBatch(any(SendMessageBatchRequest.class))).thenReturn(sendFuture);

        SqsAsyncBatchManager batchManager = batchManager(client, 10, Duration.ofHours(1), Duration.ofSeconds(2));
        CompletableFuture<SendMessageResponse> response = batchManager.sendMessage(r -> r.queueUrl(QUEUE_URL).messageBody("m"));
        executor.schedule(() -> sendFuture.complete(
            SendMessageBatchResponse.builder()
                                    .successful(SendMessageBatchResultEntry.builder()
                                                                           .id("0")
                                                                           .messageId("msg-0")
                                                                           .md5OfMessageBody("d41d8cd98f00b204e9800998ecf8427e")
                                                                           .build())
                                    .build()),
                          100, TimeUnit.MILLISECONDS);

        batchManager.close();

        assertThat(response.get(1, TimeUnit.SECONDS).messageId()).isEqualTo("msg-0");
    }

    @Test
    @Timeout(20)
    void close_cancelsStragglerSend_thatDoesNotCompleteWithinTimeout() {
        SqsAsyncClient client = mock(SqsAsyncClient.class);
        when(client.sendMessageBatch(any(SendMessageBatchRequest.class)))
            .thenReturn(new CompletableFuture<SendMessageBatchResponse>());

        SqsAsyncBatchManager batchManager = batchManager(client, 10, Duration.ofHours(1), Duration.ofMillis(300));
        CompletableFuture<SendMessageResponse> response = batchManager.sendMessage(r -> r.queueUrl(QUEUE_URL).messageBody("m"));

        batchManager.close();

        assertThatThrownBy(response::join).isInstanceOf(CancellationException.class);
    }

    @Test
    @Timeout(20)
    void sendMessageAfterClose_completesExceptionallyWithIllegalStateException() {
        SqsAsyncClient client = mock(SqsAsyncClient.class);
        SqsAsyncBatchManager batchManager = batchManager(client, 10, Duration.ofHours(1), Duration.ofMillis(1));

        batchManager.close();
        batchManager.close(); // idempotent: second close is a no-op

        CompletableFuture<SendMessageResponse> response = batchManager.sendMessage(r -> r.queueUrl(QUEUE_URL).messageBody("m"));

        assertThatThrownBy(() -> response.get(1, TimeUnit.SECONDS)).hasCauseInstanceOf(IllegalStateException.class);
    }
}
