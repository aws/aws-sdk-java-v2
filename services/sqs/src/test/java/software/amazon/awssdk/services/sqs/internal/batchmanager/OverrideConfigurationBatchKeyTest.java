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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.services.sqs.internal.batchmanager.SqsBatchKeyTest.configWith;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.batchmanager.BatchOverrideConfiguration;
import software.amazon.awssdk.services.sqs.batchmanager.SqsAsyncBatchManager;
import software.amazon.awssdk.services.sqs.internal.batchmanager.SqsBatchKeyTest.CollidingCredentialsProvider;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;

/**
 * Verifies that the write batch managers group requests by equal queue URL and equal override configuration, not by a
 * hash of the override configuration, and that the batch request carries the caller's queue URL.
 */
class OverrideConfigurationBatchKeyTest {

    private static final String QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/123456789012/q";
    private static final String OTHER_QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/123456789012/other";
    private static final int VERIFY_TIMEOUT_MS = 5_000;

    private ScheduledExecutorService executor;
    private SqsAsyncClient client;
    private SqsAsyncBatchManager batchManager;

    private AwsRequestOverrideConfiguration configA;
    private AwsRequestOverrideConfiguration configB;

    @BeforeEach
    void setUp() {
        executor = Executors.newScheduledThreadPool(2);
        client = mock(SqsAsyncClient.class);
        when(client.sendMessageBatch(any(SendMessageBatchRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(SendMessageBatchResponse.builder().build()));
        when(client.deleteMessageBatch(any(DeleteMessageBatchRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(DeleteMessageBatchResponse.builder().build()));
        when(client.changeMessageVisibilityBatch(any(ChangeMessageVisibilityBatchRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(ChangeMessageVisibilityBatchResponse.builder().build()));

        // A batch of two is flushed at once. A batch of one is flushed by the scheduled flush.
        batchManager = SqsAsyncBatchManager.builder()
                                           .client(client)
                                           .scheduledExecutor(executor)
                                           .overrideConfiguration(BatchOverrideConfiguration.builder()
                                                                                            .maxBatchSize(2)
                                                                                            .sendRequestFrequency(
                                                                                                Duration.ofMillis(100))
                                                                                            .build())
                                           .build();

        configA = configWith(new CollidingCredentialsProvider("secret-a"));
        configB = configWith(new CollidingCredentialsProvider("secret-b"));
        assertThat(configA.hashCode()).isEqualTo(configB.hashCode());
        assertThat(configA).isNotEqualTo(configB);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    @Timeout(20)
    void deleteMessage_collidingUnequalOverrides_areSentInSeparateBatches() {
        batchManager.deleteMessage(r -> r.queueUrl(QUEUE_URL).receiptHandle("rh-a").overrideConfiguration(configA));
        batchManager.deleteMessage(r -> r.queueUrl(QUEUE_URL).receiptHandle("rh-b").overrideConfiguration(configB));

        ArgumentCaptor<DeleteMessageBatchRequest> captor = ArgumentCaptor.forClass(DeleteMessageBatchRequest.class);
        verify(client, timeout(VERIFY_TIMEOUT_MS).times(2)).deleteMessageBatch(captor.capture());
        verify(client, after(300).times(2)).deleteMessageBatch(any(DeleteMessageBatchRequest.class));

        List<DeleteMessageBatchRequest> batches = captor.getAllValues();
        for (DeleteMessageBatchRequest batch : batches) {
            assertThat(batch.queueUrl()).isEqualTo(QUEUE_URL);
            assertThat(batch.entries()).hasSize(1);
            String receiptHandle = batch.entries().get(0).receiptHandle();
            AwsRequestOverrideConfiguration expected = "rh-a".equals(receiptHandle) ? configA : configB;
            assertThat(credentialsOf(batch.overrideConfiguration())).isSameAs(credentialsOf(Optional.of(expected)));
            assertHasAbmUserAgent(batch.overrideConfiguration());
        }
        assertThat(batches.stream().map(b -> b.entries().get(0).receiptHandle()).collect(Collectors.toList()))
            .containsExactlyInAnyOrder("rh-a", "rh-b");
    }

    @Test
    @Timeout(20)
    void sendMessage_collidingUnequalOverrides_areSentInSeparateBatches() {
        batchManager.sendMessage(r -> r.queueUrl(QUEUE_URL).messageBody("body-a").overrideConfiguration(configA));
        batchManager.sendMessage(r -> r.queueUrl(QUEUE_URL).messageBody("body-b").overrideConfiguration(configB));

        ArgumentCaptor<SendMessageBatchRequest> captor = ArgumentCaptor.forClass(SendMessageBatchRequest.class);
        verify(client, timeout(VERIFY_TIMEOUT_MS).times(2)).sendMessageBatch(captor.capture());
        verify(client, after(300).times(2)).sendMessageBatch(any(SendMessageBatchRequest.class));

        List<SendMessageBatchRequest> batches = captor.getAllValues();
        for (SendMessageBatchRequest batch : batches) {
            assertThat(batch.queueUrl()).isEqualTo(QUEUE_URL);
            assertThat(batch.entries()).hasSize(1);
            String body = batch.entries().get(0).messageBody();
            AwsRequestOverrideConfiguration expected = "body-a".equals(body) ? configA : configB;
            assertThat(credentialsOf(batch.overrideConfiguration())).isSameAs(credentialsOf(Optional.of(expected)));
            assertHasAbmUserAgent(batch.overrideConfiguration());
        }
        assertThat(batches.stream().map(b -> b.entries().get(0).messageBody()).collect(Collectors.toList()))
            .containsExactlyInAnyOrder("body-a", "body-b");
    }

    @Test
    @Timeout(20)
    void changeMessageVisibility_collidingUnequalOverrides_areSentInSeparateBatches() {
        batchManager.changeMessageVisibility(r -> r.queueUrl(QUEUE_URL).receiptHandle("rh-a").visibilityTimeout(30)
                                                   .overrideConfiguration(configA));
        batchManager.changeMessageVisibility(r -> r.queueUrl(QUEUE_URL).receiptHandle("rh-b").visibilityTimeout(30)
                                                   .overrideConfiguration(configB));

        ArgumentCaptor<ChangeMessageVisibilityBatchRequest> captor =
            ArgumentCaptor.forClass(ChangeMessageVisibilityBatchRequest.class);
        verify(client, timeout(VERIFY_TIMEOUT_MS).times(2)).changeMessageVisibilityBatch(captor.capture());
        verify(client, after(300).times(2)).changeMessageVisibilityBatch(any(ChangeMessageVisibilityBatchRequest.class));

        List<ChangeMessageVisibilityBatchRequest> batches = captor.getAllValues();
        for (ChangeMessageVisibilityBatchRequest batch : batches) {
            assertThat(batch.queueUrl()).isEqualTo(QUEUE_URL);
            assertThat(batch.entries()).hasSize(1);
            String receiptHandle = batch.entries().get(0).receiptHandle();
            AwsRequestOverrideConfiguration expected = "rh-a".equals(receiptHandle) ? configA : configB;
            assertThat(credentialsOf(batch.overrideConfiguration())).isSameAs(credentialsOf(Optional.of(expected)));
            assertHasAbmUserAgent(batch.overrideConfiguration());
        }
        assertThat(batches.stream().map(b -> b.entries().get(0).receiptHandle()).collect(Collectors.toList()))
            .containsExactlyInAnyOrder("rh-a", "rh-b");
    }

    @Test
    @Timeout(20)
    void deleteMessage_equalOverrides_areSentInOneBatchWithOriginalQueueUrl() {
        batchManager.deleteMessage(r -> r.queueUrl(QUEUE_URL).receiptHandle("rh-1").overrideConfiguration(configA));
        batchManager.deleteMessage(r -> r.queueUrl(QUEUE_URL).receiptHandle("rh-2")
                                         .overrideConfiguration(configA.toBuilder().build()));

        ArgumentCaptor<DeleteMessageBatchRequest> captor = ArgumentCaptor.forClass(DeleteMessageBatchRequest.class);
        verify(client, timeout(VERIFY_TIMEOUT_MS).times(1)).deleteMessageBatch(captor.capture());
        verify(client, after(300).times(1)).deleteMessageBatch(any(DeleteMessageBatchRequest.class));

        DeleteMessageBatchRequest batch = captor.getValue();
        assertThat(batch.queueUrl()).isEqualTo(QUEUE_URL);
        assertThat(batch.entries()).hasSize(2);
        assertThat(credentialsOf(batch.overrideConfiguration())).isSameAs(credentialsOf(Optional.of(configA)));
        assertHasAbmUserAgent(batch.overrideConfiguration());
    }

    @Test
    @Timeout(20)
    void sendMessage_equalOverrides_areSentInOneBatchWithOriginalQueueUrl() {
        batchManager.sendMessage(r -> r.queueUrl(QUEUE_URL).messageBody("m1").overrideConfiguration(configA));
        batchManager.sendMessage(r -> r.queueUrl(QUEUE_URL).messageBody("m2")
                                       .overrideConfiguration(configA.toBuilder().build()));

        ArgumentCaptor<SendMessageBatchRequest> captor = ArgumentCaptor.forClass(SendMessageBatchRequest.class);
        verify(client, timeout(VERIFY_TIMEOUT_MS).times(1)).sendMessageBatch(captor.capture());
        verify(client, after(300).times(1)).sendMessageBatch(any(SendMessageBatchRequest.class));

        SendMessageBatchRequest batch = captor.getValue();
        assertThat(batch.queueUrl()).isEqualTo(QUEUE_URL);
        assertThat(batch.entries()).hasSize(2);
        assertThat(credentialsOf(batch.overrideConfiguration())).isSameAs(credentialsOf(Optional.of(configA)));
        assertHasAbmUserAgent(batch.overrideConfiguration());
    }

    @Test
    @Timeout(20)
    void changeMessageVisibility_equalOverrides_areSentInOneBatchWithOriginalQueueUrl() {
        batchManager.changeMessageVisibility(r -> r.queueUrl(QUEUE_URL).receiptHandle("rh-1").visibilityTimeout(30)
                                                   .overrideConfiguration(configA));
        batchManager.changeMessageVisibility(r -> r.queueUrl(QUEUE_URL).receiptHandle("rh-2").visibilityTimeout(30)
                                                   .overrideConfiguration(configA.toBuilder().build()));

        ArgumentCaptor<ChangeMessageVisibilityBatchRequest> captor =
            ArgumentCaptor.forClass(ChangeMessageVisibilityBatchRequest.class);
        verify(client, timeout(VERIFY_TIMEOUT_MS).times(1)).changeMessageVisibilityBatch(captor.capture());
        verify(client, after(300).times(1)).changeMessageVisibilityBatch(any(ChangeMessageVisibilityBatchRequest.class));

        ChangeMessageVisibilityBatchRequest batch = captor.getValue();
        assertThat(batch.queueUrl()).isEqualTo(QUEUE_URL);
        assertThat(batch.entries()).hasSize(2);
        assertThat(credentialsOf(batch.overrideConfiguration())).isSameAs(credentialsOf(Optional.of(configA)));
        assertHasAbmUserAgent(batch.overrideConfiguration());
    }

    @Test
    @Timeout(20)
    void deleteMessage_differentQueueUrlsSameOverride_areSentInSeparateBatches() {
        batchManager.deleteMessage(r -> r.queueUrl(QUEUE_URL).receiptHandle("rh-1").overrideConfiguration(configA));
        batchManager.deleteMessage(r -> r.queueUrl(OTHER_QUEUE_URL).receiptHandle("rh-2")
                                         .overrideConfiguration(configA));

        ArgumentCaptor<DeleteMessageBatchRequest> captor = ArgumentCaptor.forClass(DeleteMessageBatchRequest.class);
        verify(client, timeout(VERIFY_TIMEOUT_MS).times(2)).deleteMessageBatch(captor.capture());
        verify(client, after(300).times(2)).deleteMessageBatch(any(DeleteMessageBatchRequest.class));

        List<DeleteMessageBatchRequest> batches = captor.getAllValues();
        for (DeleteMessageBatchRequest batch : batches) {
            assertThat(batch.entries()).hasSize(1);
            String expectedUrl = "rh-1".equals(batch.entries().get(0).receiptHandle()) ? QUEUE_URL : OTHER_QUEUE_URL;
            assertThat(batch.queueUrl()).isEqualTo(expectedUrl);
            assertHasAbmUserAgent(batch.overrideConfiguration());
        }
        assertThat(batches.stream().map(DeleteMessageBatchRequest::queueUrl).collect(Collectors.toList()))
            .containsExactlyInAnyOrder(QUEUE_URL, OTHER_QUEUE_URL);
    }

    @Test
    @Timeout(20)
    void sendMessage_differentQueueUrlsSameOverride_areSentInSeparateBatches() {
        batchManager.sendMessage(r -> r.queueUrl(QUEUE_URL).messageBody("m1").overrideConfiguration(configA));
        batchManager.sendMessage(r -> r.queueUrl(OTHER_QUEUE_URL).messageBody("m2").overrideConfiguration(configA));

        ArgumentCaptor<SendMessageBatchRequest> captor = ArgumentCaptor.forClass(SendMessageBatchRequest.class);
        verify(client, timeout(VERIFY_TIMEOUT_MS).times(2)).sendMessageBatch(captor.capture());
        verify(client, after(300).times(2)).sendMessageBatch(any(SendMessageBatchRequest.class));

        List<SendMessageBatchRequest> batches = captor.getAllValues();
        for (SendMessageBatchRequest batch : batches) {
            assertThat(batch.entries()).hasSize(1);
            String expectedUrl = "m1".equals(batch.entries().get(0).messageBody()) ? QUEUE_URL : OTHER_QUEUE_URL;
            assertThat(batch.queueUrl()).isEqualTo(expectedUrl);
            assertHasAbmUserAgent(batch.overrideConfiguration());
        }
        assertThat(batches.stream().map(SendMessageBatchRequest::queueUrl).collect(Collectors.toList()))
            .containsExactlyInAnyOrder(QUEUE_URL, OTHER_QUEUE_URL);
    }

    @Test
    @Timeout(20)
    void changeMessageVisibility_differentQueueUrlsSameOverride_areSentInSeparateBatches() {
        batchManager.changeMessageVisibility(r -> r.queueUrl(QUEUE_URL).receiptHandle("rh-1").visibilityTimeout(30)
                                                   .overrideConfiguration(configA));
        batchManager.changeMessageVisibility(r -> r.queueUrl(OTHER_QUEUE_URL).receiptHandle("rh-2")
                                                   .visibilityTimeout(30)
                                                   .overrideConfiguration(configA));

        ArgumentCaptor<ChangeMessageVisibilityBatchRequest> captor =
            ArgumentCaptor.forClass(ChangeMessageVisibilityBatchRequest.class);
        verify(client, timeout(VERIFY_TIMEOUT_MS).times(2)).changeMessageVisibilityBatch(captor.capture());
        verify(client, after(300).times(2)).changeMessageVisibilityBatch(any(ChangeMessageVisibilityBatchRequest.class));

        List<ChangeMessageVisibilityBatchRequest> batches = captor.getAllValues();
        for (ChangeMessageVisibilityBatchRequest batch : batches) {
            assertThat(batch.entries()).hasSize(1);
            String expectedUrl = "rh-1".equals(batch.entries().get(0).receiptHandle()) ? QUEUE_URL : OTHER_QUEUE_URL;
            assertThat(batch.queueUrl()).isEqualTo(expectedUrl);
            assertHasAbmUserAgent(batch.overrideConfiguration());
        }
        assertThat(batches.stream().map(ChangeMessageVisibilityBatchRequest::queueUrl).collect(Collectors.toList()))
            .containsExactlyInAnyOrder(QUEUE_URL, OTHER_QUEUE_URL);
    }

    @Test
    @Timeout(20)
    void deleteMessage_noOverrides_areSentInOneBatchWithOriginalQueueUrl() {
        batchManager.deleteMessage(r -> r.queueUrl(QUEUE_URL).receiptHandle("rh-1"));
        batchManager.deleteMessage(r -> r.queueUrl(QUEUE_URL).receiptHandle("rh-2"));

        ArgumentCaptor<DeleteMessageBatchRequest> captor = ArgumentCaptor.forClass(DeleteMessageBatchRequest.class);
        verify(client, timeout(VERIFY_TIMEOUT_MS).times(1)).deleteMessageBatch(captor.capture());

        DeleteMessageBatchRequest batch = captor.getValue();
        assertThat(batch.queueUrl()).isEqualTo(QUEUE_URL);
        assertThat(batch.entries()).hasSize(2);
        assertThat(credentialsOf(batch.overrideConfiguration())).isNull();
    }

    private static void assertHasAbmUserAgent(Optional<AwsRequestOverrideConfiguration> overrideConfiguration) {
        assertThat(overrideConfiguration).isPresent();
        assertThat(overrideConfiguration.get().apiNames())
            .contains(ApiName.builder().name("hll").version("abm").build());
    }

    private static IdentityProvider<? extends AwsCredentialsIdentity> credentialsOf(
        Optional<AwsRequestOverrideConfiguration> overrideConfiguration) {
        return overrideConfiguration.flatMap(AwsRequestOverrideConfiguration::credentialsIdentityProvider).orElse(null);
    }
}
