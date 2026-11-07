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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.utils.CompletableFutureUtils;

/**
 * Verifies receive buffer eviction and cleanup with real per-queue buffers, over an SQS stub whose received messages
 * carry the queue URL they came from. Lives in the internal package to reach the package-private test hooks of
 * {@link ReceiveMessageBatchManager}.
 */
@Timeout(value = 60, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class ReceiveBufferEvictionTest {

    private static final String MISSING_PREFIX = "missing-";

    private final EchoSqsClient sqs = new EchoSqsClient();
    private final ResponseBatchConfiguration config = ResponseBatchConfiguration.builder().build();
    private ScheduledExecutorService executor;
    private ReceiveMessageBatchManager manager;

    @BeforeEach
    void setUp() {
        executor = Executors.newScheduledThreadPool(4);
    }

    @AfterEach
    void tearDown() {
        if (manager != null) {
            manager.close();
        }
        executor.shutdownNow();
    }

    @Test
    void batchRequest_whenTwentyThousandDistinctQueues_tracksAtMostTenThousandBuffers() {
        manager = new ReceiveMessageBatchManager(sqs, executor, config);
        sqs.messagesPerReceive = 1;
        int maxSeen = 0;

        for (int i = 0; i < 20_000; i++) {
            String queueUrl = "q-" + i;
            ReceiveMessageResponse response = manager.batchRequest(request(queueUrl)).join();
            assertThat(response.messages()).allSatisfy(m -> assertThat(m.body()).startsWith(queueUrl + "#"));
            maxSeen = Math.max(maxSeen, manager.batchKeyCount());
        }

        assertThat(maxSeen).isEqualTo(RequestBatchConfiguration.DEFAULT_MAX_BATCH_KEYS);
        assertThat(manager.batchKeyCount()).isEqualTo(RequestBatchConfiguration.DEFAULT_MAX_BATCH_KEYS);
    }

    @Test
    void batchRequest_whenAtLimit_evictsOldestIdleBuffersAndReturnsTheirHeldMessages() {
        manager = new ReceiveMessageBatchManager(sqs, executor, config, 3);
        sqs.messagesPerReceive = 3;

        for (int i = 0; i < 6; i++) {
            String queueUrl = "q-" + i;
            ReceiveMessageResponse response = manager.batchRequest(request(queueUrl).toBuilder()
                                                                                    .maxNumberOfMessages(1)
                                                                                    .build()).join();
            assertThat(response.messages()).hasSize(1);
            assertThat(response.messages().get(0).body()).startsWith(queueUrl + "#");
        }

        assertThat(manager.batchKeyCount()).isEqualTo(3);
        assertThat(sqs.visibilityChanges).extracting(ChangeMessageVisibilityBatchRequest::queueUrl)
                                         .containsExactly("q-0", "q-1", "q-2");
        for (ChangeMessageVisibilityBatchRequest change : sqs.visibilityChanges) {
            assertThat(change.entries()).hasSize(2)
                                        .allSatisfy(entry -> {
                                            assertThat(entry.receiptHandle()).startsWith(change.queueUrl() + "#");
                                            assertThat(entry.visibilityTimeout()).isZero();
                                        });
        }
    }

    @Test
    void batchRequest_whenQueuesDoNotExist_removesEveryEntryAndEvictsNothing() {
        manager = new ReceiveMessageBatchManager(sqs, executor, config, 2);
        sqs.messagesPerReceive = 3;
        manager.batchRequest(request("legit").toBuilder().maxNumberOfMessages(1).build()).join();

        for (int i = 0; i < 1000; i++) {
            assertThatThrownBy(manager.batchRequest(request(MISSING_PREFIX + i))::join)
                .hasCauseInstanceOf(QueueDoesNotExistException.class);
        }

        assertThat(manager.batchKeyCount()).isEqualTo(1);
        assertThat(manager.idleCount()).isEqualTo(1);
        assertThat(sqs.visibilityChanges).isEmpty();
    }

    @Test
    void batchRequest_whenQueuesDoNotExistAndWaitTimeIsSet_removesEveryEntryAndEvictsNothing() throws Exception {
        manager = new ReceiveMessageBatchManager(sqs, executor, config);
        sqs.messagesPerReceive = 3;
        manager.batchRequest(request("legit").toBuilder().maxNumberOfMessages(1).build()).join();
        List<CompletableFuture<ReceiveMessageResponse>> responses = new ArrayList<>();

        for (int i = 0; i < 1000; i++) {
            responses.add(manager.batchRequest(request(MISSING_PREFIX + i).toBuilder().waitTimeSeconds(0).build()));
        }

        for (CompletableFuture<ReceiveMessageResponse> response : responses) {
            assertThat(response.get(5, TimeUnit.SECONDS).messages()).isEmpty();
        }
        awaitTrue(() -> manager.batchKeyCount() == 1);
        assertThat(manager.idleCount()).isEqualTo(1);
        assertThat(sqs.visibilityChanges).isEmpty();
    }

    @Test
    void batchRequest_whenGetQueueAttributesThrowsSynchronouslyOnce_nextReceiveCompletes() throws Exception {
        manager = new ReceiveMessageBatchManager(sqs, executor, config, 3);
        sqs.messagesPerReceive = 1;
        sqs.throwOnNextAttributeLookup.add("q");

        CompletableFuture<ReceiveMessageResponse> first = manager.batchRequest(request("q"));

        assertThatThrownBy(first::join).hasMessageContaining("synchronous lookup failure");
        ReceiveMessageResponse second = manager.batchRequest(request("q")).get(5, TimeUnit.SECONDS);
        assertThat(second.messages()).hasSize(1);
        assertThat(second.messages().get(0).body()).startsWith("q#");
        assertThat(manager.batchKeyCount()).isEqualTo(1);
        assertThat(manager.idleCount()).isEqualTo(1);
    }

    @Test
    void batchRequest_whenBufferEvictedWithReceiveInFlight_returnsItsMessagesWhenItCompletes() throws Exception {
        manager = new ReceiveMessageBatchManager(sqs, executor, config, 1);
        sqs.messagesPerReceive = 3;
        sqs.holdReceives.add("held");

        ReceiveMessageResponse heldResponse =
            manager.batchRequest(request("held").toBuilder().waitTimeSeconds(0).build()).get(5, TimeUnit.SECONDS);
        assertThat(heldResponse.messages()).isEmpty();
        awaitTrue(() -> manager.idleCount() == 1);

        manager.batchRequest(request("other").toBuilder().maxNumberOfMessages(3).build()).join();
        assertThat(sqs.visibilityChanges).isEmpty();

        sqs.releaseHeldReceives();

        assertThat(sqs.visibilityChanges).hasSize(1);
        ChangeMessageVisibilityBatchRequest change = sqs.visibilityChanges.get(0);
        assertThat(change.queueUrl()).isEqualTo("held");
        assertThat(change.entries()).extracting(ChangeMessageVisibilityBatchRequestEntry::receiptHandle)
                                    .hasSize(3)
                                    .allSatisfy(handle -> assertThat(handle).startsWith("held#"));
    }

    private static ReceiveMessageRequest request(String queueUrl) {
        return ReceiveMessageRequest.builder().queueUrl(queueUrl).build();
    }

    private static void awaitTrue(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!condition.getAsBoolean()) {
            assertThat(System.nanoTime()).as("condition not met within 10 seconds").isLessThan(deadline);
            Thread.sleep(10);
        }
    }

    /**
     * An SQS stub whose received message bodies and receipt handles start with the queue URL they came from. Queue URLs
     * starting with {@value #MISSING_PREFIX} don't exist.
     */
    private static final class EchoSqsClient implements SqsAsyncClient {
        private final AtomicLong sequence = new AtomicLong();
        private final Set<String> throwOnNextAttributeLookup = ConcurrentHashMap.newKeySet();
        private final Set<String> holdReceives = ConcurrentHashMap.newKeySet();
        private final List<Held> heldReceives = new CopyOnWriteArrayList<>();
        private final List<ChangeMessageVisibilityBatchRequest> visibilityChanges = new CopyOnWriteArrayList<>();
        private volatile int messagesPerReceive;

        @Override
        public CompletableFuture<GetQueueAttributesResponse> getQueueAttributes(GetQueueAttributesRequest request) {
            if (throwOnNextAttributeLookup.remove(request.queueUrl())) {
                throw new IllegalStateException("synchronous lookup failure");
            }
            if (request.queueUrl().startsWith(MISSING_PREFIX)) {
                return CompletableFutureUtils.failedFuture(QueueDoesNotExistException.builder()
                                                                                     .message("no such queue")
                                                                                     .build());
            }
            Map<QueueAttributeName, String> attributes = new HashMap<>();
            attributes.put(QueueAttributeName.RECEIVE_MESSAGE_WAIT_TIME_SECONDS, "0");
            attributes.put(QueueAttributeName.VISIBILITY_TIMEOUT, "30");
            return CompletableFuture.completedFuture(GetQueueAttributesResponse.builder().attributes(attributes).build());
        }

        @Override
        public CompletableFuture<ReceiveMessageResponse> receiveMessage(ReceiveMessageRequest request) {
            if (holdReceives.contains(request.queueUrl())) {
                CompletableFuture<ReceiveMessageResponse> held = new CompletableFuture<>();
                heldReceives.add(new Held(request.queueUrl(), held));
                return held;
            }
            return CompletableFuture.completedFuture(messagesFor(request.queueUrl()));
        }

        @Override
        public CompletableFuture<ChangeMessageVisibilityBatchResponse> changeMessageVisibilityBatch(
            ChangeMessageVisibilityBatchRequest request) {
            visibilityChanges.add(request);
            return CompletableFuture.completedFuture(ChangeMessageVisibilityBatchResponse.builder().build());
        }

        void releaseHeldReceives() {
            heldReceives.forEach(held -> held.response.complete(messagesFor(held.queueUrl)));
        }

        private ReceiveMessageResponse messagesFor(String queueUrl) {
            List<Message> messages = new ArrayList<>();
            for (int i = 0; i < messagesPerReceive; i++) {
                String id = queueUrl + "#" + sequence.incrementAndGet();
                messages.add(Message.builder().body(id).receiptHandle(id).build());
            }
            return ReceiveMessageResponse.builder().messages(messages).build();
        }

        @Override
        public String serviceName() {
            return SERVICE_NAME;
        }

        @Override
        public void close() {
        }

        private static final class Held {
            private final String queueUrl;
            private final CompletableFuture<ReceiveMessageResponse> response;

            Held(String queueUrl, CompletableFuture<ReceiveMessageResponse> response) {
                this.queueUrl = queueUrl;
                this.response = response;
            }
        }
    }
}
