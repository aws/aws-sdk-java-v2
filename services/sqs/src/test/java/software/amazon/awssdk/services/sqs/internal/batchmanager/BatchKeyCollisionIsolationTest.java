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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.batchmanager.BatchOverrideConfiguration;
import software.amazon.awssdk.services.sqs.batchmanager.SqsAsyncBatchManager;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchResponse;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchResultEntry;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchResultEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResultEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

/**
 * Buffered requests must only be batched - and therefore signed and dispatched - together when their queue URL and request
 * override configuration match by equality. These tests drive the real write batch managers over a mock
 * {@link SqsAsyncClient} and inspect the outgoing batch requests. Lives in the internal package to reach the package-private
 * {@link DefaultSqsAsyncBatchManager.DefaultBuilder#shutdownTimeout} test seam, like
 * {@link DefaultSqsAsyncBatchManagerTest}.
 */
class BatchKeyCollisionIsolationTest {

    private static final String QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/123456789012/a";

    private ScheduledExecutorService executor;
    private FixedHashCredentialsProvider provider1;
    private FixedHashCredentialsProvider provider2;
    private AwsRequestOverrideConfiguration config1;
    private AwsRequestOverrideConfiguration config2;

    @BeforeEach
    void setUp() {
        executor = Executors.newScheduledThreadPool(2);
        provider1 = new FixedHashCredentialsProvider("akid1");
        provider2 = new FixedHashCredentialsProvider("akid2");
        config1 = AwsRequestOverrideConfiguration.builder().credentialsProvider(provider1).build();
        config2 = AwsRequestOverrideConfiguration.builder().credentialsProvider(provider2).build();
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    /**
     * The two configurations must collide on {@code hashCode} while being non-equal, otherwise these tests would no longer
     * exercise the identity-confusion bug at all.
     */
    private void assertConfigsCollideButDiffer() {
        assertThat(config1.hashCode()).isEqualTo(config2.hashCode());
        assertThat(config1).isNotEqualTo(config2);
    }

    private SqsAsyncBatchManager batchManager(SqsAsyncClient client) {
        DefaultSqsAsyncBatchManager.DefaultBuilder builder =
            (DefaultSqsAsyncBatchManager.DefaultBuilder) DefaultSqsAsyncBatchManager.builder();
        builder.client(client);
        builder.scheduledExecutor(executor);
        // maxBatchSize 10 so a colliding pair really would be merged into one call on the buggy path, and an hour-long send
        // frequency so the periodic flush can never split a batch mid-test: the only flush is the close() drain.
        builder.overrideConfiguration(BatchOverrideConfiguration.builder()
                                                                .maxBatchSize(10)
                                                                .sendRequestFrequency(Duration.ofHours(1))
                                                                .build());
        builder.shutdownTimeout(Duration.ofMillis(500));
        return builder.build();
    }

    private static void answerChangeMessageVisibility(SqsAsyncClient client) {
        when(client.changeMessageVisibilityBatch(any(ChangeMessageVisibilityBatchRequest.class))).thenAnswer(invocation -> {
            ChangeMessageVisibilityBatchRequest request = invocation.getArgument(0);
            return CompletableFuture.completedFuture(
                ChangeMessageVisibilityBatchResponse.builder()
                                                    .successful(request.entries()
                                                                       .stream()
                                                                       .map(entry -> ChangeMessageVisibilityBatchResultEntry
                                                                           .builder().id(entry.id()).build())
                                                                       .collect(Collectors.toList()))
                                                    .build());
        });
    }

    private static void answerDeleteMessage(SqsAsyncClient client) {
        when(client.deleteMessageBatch(any(DeleteMessageBatchRequest.class))).thenAnswer(invocation -> {
            DeleteMessageBatchRequest request = invocation.getArgument(0);
            return CompletableFuture.completedFuture(
                DeleteMessageBatchResponse.builder()
                                          .successful(request.entries()
                                                             .stream()
                                                             .map(entry -> DeleteMessageBatchResultEntry.builder()
                                                                                                        .id(entry.id())
                                                                                                        .build())
                                                             .collect(Collectors.toList()))
                                          .build());
        });
    }

    /**
     * Echoes back a message id derived from the entry that was actually sent, so each caller's future can be checked against
     * its own submission.
     */
    private static void answerSendMessage(SqsAsyncClient client) {
        when(client.sendMessageBatch(any(SendMessageBatchRequest.class))).thenAnswer(invocation -> {
            SendMessageBatchRequest request = invocation.getArgument(0);
            return CompletableFuture.completedFuture(
                SendMessageBatchResponse.builder()
                                        .successful(request.entries()
                                                           .stream()
                                                           .map(entry -> SendMessageBatchResultEntry
                                                               .builder()
                                                               .id(entry.id())
                                                               .messageId(entry.id() + "-" + entry.messageBody())
                                                               .build())
                                                           .collect(Collectors.toList()))
                                        .build());
        });
    }

    private static AwsCredentialsProvider credentialsProviderOf(AwsRequestOverrideConfiguration overrideConfiguration) {
        return (AwsCredentialsProvider) overrideConfiguration.credentialsIdentityProvider()
                                                            .orElseThrow(() -> new AssertionError("no credentials provider"));
    }

    @Test
    @Timeout(20)
    void changeMessageVisibility_collidingOverrideConfigHashes_notMerged() {
        assertConfigsCollideButDiffer();
        SqsAsyncClient client = mock(SqsAsyncClient.class);
        answerChangeMessageVisibility(client);

        SqsAsyncBatchManager batchManager = batchManager(client);
        batchManager.changeMessageVisibility(r -> r.queueUrl(QUEUE_URL)
                                                  .receiptHandle("rh1")
                                                  .visibilityTimeout(30)
                                                  .overrideConfiguration(config1));
        batchManager.changeMessageVisibility(r -> r.queueUrl(QUEUE_URL)
                                                  .receiptHandle("rh2")
                                                  .visibilityTimeout(30)
                                                  .overrideConfiguration(config2));
        batchManager.close();

        ArgumentCaptor<ChangeMessageVisibilityBatchRequest> captor =
            ArgumentCaptor.forClass(ChangeMessageVisibilityBatchRequest.class);
        verify(client, times(2)).changeMessageVisibilityBatch(captor.capture());

        for (ChangeMessageVisibilityBatchRequest request : captor.getAllValues()) {
            assertThat(request.queueUrl()).isEqualTo(QUEUE_URL);
            assertThat(request.entries()).hasSize(1);
            AwsRequestOverrideConfiguration overrideConfiguration = request.overrideConfiguration().get();
            if ("rh1".equals(request.entries().get(0).receiptHandle())) {
                assertThat(credentialsProviderOf(overrideConfiguration)).isSameAs(provider1);
            } else {
                assertThat(request.entries().get(0).receiptHandle()).isEqualTo("rh2");
                assertThat(credentialsProviderOf(overrideConfiguration)).isSameAs(provider2);
            }
        }
    }

    @Test
    @Timeout(20)
    void deleteMessage_collidingOverrideConfigHashes_notMerged() {
        assertConfigsCollideButDiffer();
        SqsAsyncClient client = mock(SqsAsyncClient.class);
        answerDeleteMessage(client);

        SqsAsyncBatchManager batchManager = batchManager(client);
        batchManager.deleteMessage(r -> r.queueUrl(QUEUE_URL).receiptHandle("rh1").overrideConfiguration(config1));
        batchManager.deleteMessage(r -> r.queueUrl(QUEUE_URL).receiptHandle("rh2").overrideConfiguration(config2));
        batchManager.close();

        ArgumentCaptor<DeleteMessageBatchRequest> captor = ArgumentCaptor.forClass(DeleteMessageBatchRequest.class);
        verify(client, times(2)).deleteMessageBatch(captor.capture());

        for (DeleteMessageBatchRequest request : captor.getAllValues()) {
            assertThat(request.queueUrl()).isEqualTo(QUEUE_URL);
            assertThat(request.entries()).hasSize(1);
            AwsRequestOverrideConfiguration overrideConfiguration = request.overrideConfiguration().get();
            if ("rh1".equals(request.entries().get(0).receiptHandle())) {
                assertThat(credentialsProviderOf(overrideConfiguration)).isSameAs(provider1);
            } else {
                assertThat(request.entries().get(0).receiptHandle()).isEqualTo("rh2");
                assertThat(credentialsProviderOf(overrideConfiguration)).isSameAs(provider2);
            }
        }
    }

    @Test
    @Timeout(20)
    void sendMessage_collidingOverrideConfigHashes_notMerged_andEachCallerGetsItsOwnResponse() {
        assertConfigsCollideButDiffer();
        SqsAsyncClient client = mock(SqsAsyncClient.class);
        answerSendMessage(client);

        SqsAsyncBatchManager batchManager = batchManager(client);
        CompletableFuture<SendMessageResponse> response1 =
            batchManager.sendMessage(r -> r.queueUrl(QUEUE_URL).messageBody("body1").overrideConfiguration(config1));
        CompletableFuture<SendMessageResponse> response2 =
            batchManager.sendMessage(r -> r.queueUrl(QUEUE_URL).messageBody("body2").overrideConfiguration(config2));
        batchManager.close();

        ArgumentCaptor<SendMessageBatchRequest> captor = ArgumentCaptor.forClass(SendMessageBatchRequest.class);
        verify(client, times(2)).sendMessageBatch(captor.capture());

        for (SendMessageBatchRequest request : captor.getAllValues()) {
            assertThat(request.queueUrl()).isEqualTo(QUEUE_URL);
            assertThat(request.entries()).hasSize(1);
            AwsRequestOverrideConfiguration overrideConfiguration = request.overrideConfiguration().get();
            if ("body1".equals(request.entries().get(0).messageBody())) {
                assertThat(credentialsProviderOf(overrideConfiguration)).isSameAs(provider1);
            } else {
                assertThat(request.entries().get(0).messageBody()).isEqualTo("body2");
                assertThat(credentialsProviderOf(overrideConfiguration)).isSameAs(provider2);
            }
        }

        // Both split buffers number their single entry "0", so this also pins that the two in-flight batches cannot
        // cross-complete each other's caller futures.
        assertThat(response1.join().messageId()).endsWith("-body1");
        assertThat(response2.join().messageId()).endsWith("-body2");
        assertThat(response1.join().messageId()).isNotEqualTo(response2.join().messageId());
    }

    @Test
    @Timeout(20)
    void equalOverrideConfigs_stillBatchedTogether() {
        SqsAsyncClient client = mock(SqsAsyncClient.class);
        answerDeleteMessage(client);

        AwsRequestOverrideConfiguration first = AwsRequestOverrideConfiguration.builder()
                                                                              .credentialsProvider(provider1)
                                                                              .build();
        AwsRequestOverrideConfiguration second = AwsRequestOverrideConfiguration.builder()
                                                                               .credentialsProvider(provider1)
                                                                               .build();
        assertThat(second).isEqualTo(first);

        SqsAsyncBatchManager batchManager = batchManager(client);
        batchManager.deleteMessage(r -> r.queueUrl(QUEUE_URL).receiptHandle("rh1").overrideConfiguration(first));
        batchManager.deleteMessage(r -> r.queueUrl(QUEUE_URL).receiptHandle("rh2").overrideConfiguration(second));
        batchManager.close();

        ArgumentCaptor<DeleteMessageBatchRequest> captor = ArgumentCaptor.forClass(DeleteMessageBatchRequest.class);
        verify(client, times(1)).deleteMessageBatch(captor.capture());

        DeleteMessageBatchRequest request = captor.getValue();
        assertThat(request.queueUrl()).isEqualTo(QUEUE_URL);
        assertThat(request.entries()).hasSize(2);
        assertThat(credentialsProviderOf(request.overrideConfiguration().get())).isSameAs(provider1);
    }

    @Test
    @Timeout(20)
    void noOverrideConfig_batchTargetsRealQueueUrlAndCarriesUserAgentApiName() {
        SqsAsyncClient client = mock(SqsAsyncClient.class);
        answerDeleteMessage(client);

        SqsAsyncBatchManager batchManager = batchManager(client);
        batchManager.deleteMessage(r -> r.queueUrl(QUEUE_URL).receiptHandle("rh1"));
        batchManager.close();

        ArgumentCaptor<DeleteMessageBatchRequest> captor = ArgumentCaptor.forClass(DeleteMessageBatchRequest.class);
        verify(client, times(1)).deleteMessageBatch(captor.capture());

        DeleteMessageBatchRequest request = captor.getValue();
        assertThat(request.queueUrl()).isEqualTo(QUEUE_URL);
        List<ApiName> apiNames = request.overrideConfiguration().get().apiNames();
        assertThat(apiNames).anySatisfy(apiName -> {
            assertThat(apiName.name()).isEqualTo("hll");
            assertThat(apiName.version()).isEqualTo("abm");
        });
    }

    @Test
    @Timeout(20)
    void aliasedQueueUrlAndOverrideConfig_neverShareABatch() {
        SqsAsyncClient client = mock(SqsAsyncClient.class);
        answerChangeMessageVisibility(client);

        String queueA = QUEUE_URL;
        // The queue URL the replaced string key (queueUrl + overrideConfig.hashCode()) collapsed queue A's key onto.
        String queueB = queueA + config1.hashCode();

        SqsAsyncBatchManager batchManager = batchManager(client);
        batchManager.changeMessageVisibility(r -> r.queueUrl(queueA)
                                                  .receiptHandle("rh-a")
                                                  .visibilityTimeout(30)
                                                  .overrideConfiguration(config1));
        batchManager.changeMessageVisibility(r -> r.queueUrl(queueB)
                                                  .receiptHandle("rh-b")
                                                  .visibilityTimeout(30));
        batchManager.close();

        ArgumentCaptor<ChangeMessageVisibilityBatchRequest> captor =
            ArgumentCaptor.forClass(ChangeMessageVisibilityBatchRequest.class);
        verify(client, times(2)).changeMessageVisibilityBatch(captor.capture());

        for (ChangeMessageVisibilityBatchRequest request : captor.getAllValues()) {
            assertThat(request.entries()).hasSize(1);
            if ("rh-a".equals(request.entries().get(0).receiptHandle())) {
                assertThat(request.queueUrl()).isEqualTo(queueA);
                assertThat(credentialsProviderOf(request.overrideConfiguration().get())).isSameAs(provider1);
            } else {
                assertThat(request.entries().get(0).receiptHandle()).isEqualTo("rh-b");
                assertThat(request.queueUrl()).isEqualTo(queueB);
                assertThat(request.overrideConfiguration().get().credentialsIdentityProvider()).isEmpty();
            }
        }
    }

    /**
     * A credentials provider with a constant hash code and identity equality: two instances make two request override
     * configurations that are not equal but do share a hash code, which is the collision the batch key must not merge.
     */
    private static final class FixedHashCredentialsProvider implements AwsCredentialsProvider {
        private final String accessKeyId;

        private FixedHashCredentialsProvider(String accessKeyId) {
            this.accessKeyId = accessKeyId;
        }

        @Override
        public AwsCredentials resolveCredentials() {
            return AwsBasicCredentials.create(accessKeyId, "secret");
        }

        @Override
        public int hashCode() {
            return 42;
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj;
        }
    }
}
