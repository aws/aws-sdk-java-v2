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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.testutils.LogCaptor;

/**
 * Tests the receive buffer limit with stub buffers. In the internal package for the package-private test hooks.
 */
@Timeout(value = 30, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class ReceiveMessageBatchManagerLimitTest {

    private final SqsAsyncClient sqsClient = mock(SqsAsyncClient.class);
    private final ResponseBatchConfiguration config = ResponseBatchConfiguration.builder().build();
    private final Map<String, StubReceiveBatchManager> stubs = new ConcurrentHashMap<>();
    private final List<StubReceiveBatchManager> created = new ArrayList<>();
    private ScheduledExecutorService executor;

    @BeforeEach
    void setUp() {
        executor = Executors.newScheduledThreadPool(4);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void batchRequest_whenAtLimit_evictsBufferThatBecameIdleFirst() {
        ReceiveMessageBatchManager manager = manager(3, url -> new StubReceiveBatchManager(url, false));
        CompletableFuture<ReceiveMessageResponse> first = manager.batchRequest(request("a"));
        CompletableFuture<ReceiveMessageResponse> second = manager.batchRequest(request("b"));
        manager.batchRequest(request("c"));
        stubFor("b").completePending();
        stubFor("a").completePending();
        assertThat(first).isDone();
        assertThat(second).isDone();

        manager.batchRequest(request("d"));

        assertThat(stubFor("b").closed).isTrue();
        assertThat(stubFor("a").closed).isFalse();
        assertThat(manager.batchKeyCount()).isEqualTo(3);
        assertThat(manager.idleCount()).isEqualTo(1);
    }

    @Test
    void batchRequest_whenIdleBufferIsReused_isNotEvictedWhileInUse() {
        ReceiveMessageBatchManager manager = manager(2, url -> new StubReceiveBatchManager(url, true));
        manager.batchRequest(request("reused")).join();
        manager.batchRequest(request("idle")).join();
        stubFor("reused").respondImmediately = false;
        manager.batchRequest(request("reused"));

        manager.batchRequest(request("new")).join();

        assertThat(stubFor("reused").closed).isFalse();
        assertThat(stubFor("idle").closed).isTrue();
    }

    @Test
    void batchRequest_whenEveryBufferIsInUse_failsWithoutCallingSqs() {
        ReceiveMessageBatchManager manager = manager(2, url -> new StubReceiveBatchManager(url, false));
        manager.batchRequest(request("a"));
        manager.batchRequest(request("b"));

        for (int i = 1; i <= 3; i++) {
            assertThatThrownBy(manager.batchRequest(request("extra-" + i))::join)
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Reached the maximum of 2 receive buffers, and every buffer is in use.");
        }
        assertThat(manager.batchKeyCount()).isEqualTo(2);
        verifyNoInteractions(sqsClient);

        stubFor("a").completePending();
        manager.batchRequest(request("after-release"));

        assertThat(stubFor("a").closed).isTrue();
        assertThat(manager.batchKeyCount()).isEqualTo(2);
    }

    @Test
    void batchRequest_whenQueueAttributesFailToLoad_failsAndNextRequestLoadsAgain() {
        RuntimeException failure = new RuntimeException("queue does not exist");
        CompletableFuture<Void> lookup = new CompletableFuture<>();
        lookup.completeExceptionally(failure);
        ReceiveMessageBatchManager manager = manager(1, url -> new StubReceiveBatchManager(url, true, lookup));

        assertThatThrownBy(manager.batchRequest(request("missing"))::join).hasCause(failure);
        assertThatThrownBy(manager.batchRequest(request("missing"))::join).hasCause(failure);

        assertThat(created).hasSize(2);
        assertThat(created).allSatisfy(stub -> assertThat(stub.processed).hasValue(0));
        assertThat(manager.batchKeyCount()).isZero();
    }

    @Test
    void batchRequest_whenFirstRequestsForQueueOverlap_shareOneLookup() {
        ReceiveMessageBatchManager manager =
            manager(2, url -> new StubReceiveBatchManager(url, true, new CompletableFuture<>()));
        List<CompletableFuture<ReceiveMessageResponse>> responses = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            responses.add(manager.batchRequest(request("q")));
        }
        assertThat(created).hasSize(1);

        created.get(0).lookup.complete(null);

        assertThat(responses).allSatisfy(response -> assertThat(response).isCompleted());
        assertThat(created.get(0).processed).hasValue(5);
        assertThat(manager.batchKeyCount()).isEqualTo(1);
        assertThat(manager.idleCount()).isEqualTo(1);
    }

    @Test
    void batchRequest_whenSharedLookupWaiterRunsAfterEviction_loadsNewBuffer() {
        CompletableFuture<Void> lookup = new CompletableFuture<>();
        ReceiveMessageBatchManager manager =
            manager(1, url -> new StubReceiveBatchManager(url, true,
                                                          url.equals("q") ? lookup : CompletableFuture.completedFuture(null)));
        CompletableFuture<ReceiveMessageResponse> first = manager.batchRequest(request("q"));
        CompletableFuture<ReceiveMessageResponse> second = manager.batchRequest(request("q"));
        // The first waiter to finish evicts q before the other waiter adds it.
        AtomicBoolean evicted = new AtomicBoolean();
        Runnable evictQ = () -> {
            if (evicted.compareAndSet(false, true)) {
                manager.batchRequest(request("other"));
            }
        };
        first.thenRun(evictQ);
        second.thenRun(evictQ);

        lookup.complete(null);

        assertThat(created.get(0).closed).isTrue();
        assertThat(first).isCompleted();
        assertThat(second).isCompleted();
        assertThat(manager.batchRequest(request("q"))).isCompleted();
    }

    @Test
    void batchRequest_whenEvictedQueueReturns_loadsNewBuffer() {
        ReceiveMessageBatchManager manager = manager(1, url -> new StubReceiveBatchManager(url, true));
        manager.batchRequest(request("a")).join();
        manager.batchRequest(request("b")).join();

        assertThat(manager.batchRequest(request("a"))).isCompleted();

        assertThat(created).hasSize(3);
        assertThat(created.get(2).closed).isFalse();
    }

    @Test
    void batchRequest_whenCreateReceiveBatchManagerThrowsAtLimit_keepsIdleBuffer() {
        Error failure = new NoClassDefFoundError("create failed");
        AtomicBoolean failCreate = new AtomicBoolean(true);
        ReceiveMessageBatchManager manager = manager(1, url -> {
            if (url.equals("bad") && failCreate.getAndSet(false)) {
                throw failure;
            }
            return new StubReceiveBatchManager(url, true);
        });
        manager.batchRequest(request("idle")).join();

        assertThatThrownBy(manager.batchRequest(request("bad"))::join).hasCause(failure);

        assertThat(stubFor("idle").closed).isFalse();
        assertThat(manager.batchKeyCount()).isEqualTo(1);
        assertThat(manager.idleCount()).isEqualTo(1);
        assertThat(manager.batchRequest(request("bad"))).isCompleted();
    }

    @Test
    void batchRequest_whenProcessRequestThrows_returnsFailedFutureAndReleasesEntry() {
        Error failure = new NoClassDefFoundError("process failed");
        ReceiveMessageBatchManager manager = manager(2, url -> new StubReceiveBatchManager(url, true));
        manager.batchRequest(request("q")).join();
        stubFor("q").processFailure = failure;

        CompletableFuture<ReceiveMessageResponse> response = manager.batchRequest(request("q"));

        assertThatThrownBy(response::join).hasCause(failure);
        assertThat(manager.batchKeyCount()).isEqualTo(1);
        assertThat(manager.idleCount()).isEqualTo(1);
    }

    @Test
    void batchRequest_whenEvictedBufferCloseThrows_logsDebugAndRunsAdmittedRequest() {
        ReceiveMessageBatchManager manager = manager(1, url -> new StubReceiveBatchManager(url, true));
        manager.batchRequest(request("idle")).join();
        stubFor("idle").closeFailure = new RuntimeException("close failed");

        try (LogCaptor logCaptor = LogCaptor.create(Level.DEBUG)) {
            CompletableFuture<ReceiveMessageResponse> response = manager.batchRequest(request("new"));

            assertThat(response).isCompleted();
            assertThat(logCaptor.loggedEvents()).anySatisfy(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.DEBUG);
                assertThat(event.getThrown()).hasMessage("close failed");
            });
        }
        assertThat(manager.batchKeyCount()).isEqualTo(1);
        assertThat(manager.idleCount()).isEqualTo(1);
    }

    @Test
    void close_whenOneBufferCloseThrows_closesOthersAndLaterCallsFail() {
        ReceiveMessageBatchManager manager = manager(3, url -> new StubReceiveBatchManager(url, true));
        manager.batchRequest(request("a")).join();
        manager.batchRequest(request("b")).join();
        manager.batchRequest(request("c")).join();
        stubFor("b").closeFailure = new RuntimeException("close failed");

        try (LogCaptor logCaptor = LogCaptor.create(Level.DEBUG)) {
            manager.close();

            assertThat(logCaptor.loggedEvents())
                .anySatisfy(event -> assertThat(event.getThrown()).hasMessage("close failed"));
        }
        assertThat(stubFor("a").closed).isTrue();
        assertThat(stubFor("c").closed).isTrue();
        assertThat(manager.batchKeyCount()).isZero();
        assertThat(manager.idleCount()).isZero();

        assertThatThrownBy(manager.batchRequest(request("a"))::join)
            .hasCauseInstanceOf(IllegalStateException.class)
            .hasMessageContaining("The client has been shut down.");
        ReceiveMessageRequest ineligible = request("a").toBuilder().visibilityTimeout(30).build();
        assertThatThrownBy(manager.batchRequest(ineligible)::join)
            .hasCauseInstanceOf(IllegalStateException.class)
            .hasMessageContaining("The client has been shut down.");
        verifyNoInteractions(sqsClient);
    }

    @Test
    void batchRequest_whenRequestCompletesAfterClose_doesNotReturnEntryToIdle() {
        ReceiveMessageBatchManager manager = manager(2, url -> new StubReceiveBatchManager(url, false));
        CompletableFuture<ReceiveMessageResponse> response = manager.batchRequest(request("q"));
        manager.close();

        stubFor("q").completePending();

        assertThat(response).isCompleted();
        assertThat(manager.batchKeyCount()).isZero();
        assertThat(manager.idleCount()).isZero();
    }

    @Test
    void batchRequest_whenManyThreadsUseFewBuffers_neverExceedsLimitAndIdleMatchesUnusedEntries() throws Exception {
        int limit = 4;
        int threads = 16;
        ReceiveMessageBatchManager manager = manager(limit, url -> new StubReceiveBatchManager(url, executor));
        ExecutorService callers = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger maxSeen = new AtomicInteger();
        List<CompletableFuture<ReceiveMessageResponse>> responses = new ArrayList<>();
        List<Future<?>> callerTasks = new ArrayList<>();
        try {
            for (int t = 0; t < threads; t++) {
                callerTasks.add(callers.submit(() -> {
                    start.await();
                    for (int i = 0; i < 500; i++) {
                        String url = "q-" + ThreadLocalRandom.current().nextInt(12);
                        CompletableFuture<ReceiveMessageResponse> response = manager.batchRequest(request(url));
                        synchronized (responses) {
                            responses.add(response);
                        }
                        maxSeen.accumulateAndGet(manager.batchKeyCount(), Math::max);
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> task : callerTasks) {
                task.get(30, TimeUnit.SECONDS);
            }
        } finally {
            callers.shutdownNow();
        }

        awaitTrue(() -> responses.stream().allMatch(CompletableFuture::isDone));
        // Polls because release runs in a whenComplete callback that can run after the response is already done.
        awaitTrue(() -> manager.idleCount() == manager.batchKeyCount());

        assertThat(maxSeen.get()).isLessThanOrEqualTo(limit);
        assertThat(manager.batchKeyCount()).isLessThanOrEqualTo(limit);
        assertThat(manager.idleCount()).isEqualTo(manager.batchKeyCount());
        synchronized (created) {
            assertThat(created.stream().filter(stub -> stub.processed.get() > 0 && !stub.closed).count())
                .isEqualTo(manager.batchKeyCount());
        }
    }

    @Test
    void close_whenRacingRequestsForNewQueues_leavesNoEntriesAndClosesEveryBuffer() throws Exception {
        ExecutorService callers = Executors.newFixedThreadPool(9);
        try {
            for (int round = 0; round < 30; round++) {
                synchronized (created) {
                    created.clear();
                }
                ReceiveMessageBatchManager manager = manager(1000, url -> new StubReceiveBatchManager(url, true));
                CountDownLatch start = new CountDownLatch(1);
                List<Future<?>> requesters = new ArrayList<>();
                for (int t = 0; t < 8; t++) {
                    String prefix = "q-" + round + "-" + t + "-";
                    requesters.add(callers.submit(() -> {
                        start.await();
                        for (int i = 0; i < 50; i++) {
                            manager.batchRequest(request(prefix + i));
                        }
                        return null;
                    }));
                }
                Future<?> closer = callers.submit(() -> {
                    start.await();
                    manager.close();
                    return null;
                });
                start.countDown();
                closer.get(10, TimeUnit.SECONDS);
                for (Future<?> requester : requesters) {
                    requester.get(10, TimeUnit.SECONDS);
                }

                assertThat(manager.batchKeyCount()).isZero();
                synchronized (created) {
                    assertThat(created).filteredOn(stub -> stub.processed.get() > 0)
                                       .allSatisfy(stub -> assertThat(stub.closed).isTrue());
                }
            }
        } finally {
            callers.shutdownNow();
        }
    }

    @Test
    void batchRequest_whenIneligibleRequestAndClientThrowsSynchronously_returnsFailedFuture() {
        RuntimeException failure = new RuntimeException("client failed");
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenThrow(failure);
        ReceiveMessageBatchManager manager = manager(2, url -> new StubReceiveBatchManager(url, true));

        CompletableFuture<ReceiveMessageResponse> response =
            manager.batchRequest(request("q").toBuilder().visibilityTimeout(30).build());

        assertThatThrownBy(response::join).hasCause(failure);
    }

    private ReceiveMessageBatchManager manager(int maxBuffers, Function<String, StubReceiveBatchManager> factory) {
        return new ReceiveMessageBatchManager(sqsClient, executor, config, maxBuffers) {
            @Override
            protected ReceiveBatchManager createReceiveBatchManager(String queueUrl) {
                StubReceiveBatchManager stub = factory.apply(queueUrl);
                synchronized (created) {
                    created.add(stub);
                }
                stubs.put(queueUrl, stub);
                return stub;
            }
        };
    }

    private StubReceiveBatchManager stubFor(String queueUrl) {
        return stubs.get(queueUrl);
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

    private final class StubReceiveBatchManager extends ReceiveBatchManager {
        private final List<CompletableFuture<ReceiveMessageResponse>> pending = new ArrayList<>();
        private final AtomicInteger processed = new AtomicInteger();
        private final CompletableFuture<Void> lookup;
        private final ScheduledExecutorService completer;
        private volatile boolean respondImmediately;
        private volatile Error processFailure;
        private volatile RuntimeException closeFailure;
        private volatile boolean closed;

        StubReceiveBatchManager(String queueUrl, boolean respondImmediately) {
            this(queueUrl, respondImmediately, CompletableFuture.completedFuture(null));
        }

        StubReceiveBatchManager(String queueUrl, boolean respondImmediately, CompletableFuture<Void> lookup) {
            super(sqsClient, executor, config, queueUrl);
            this.respondImmediately = respondImmediately;
            this.lookup = lookup;
            this.completer = null;
        }

        StubReceiveBatchManager(String queueUrl, ScheduledExecutorService completer) {
            super(sqsClient, executor, config, queueUrl);
            this.lookup = CompletableFuture.completedFuture(null);
            this.completer = completer;
        }

        @Override
        public CompletableFuture<ReceiveMessageResponse> processRequest(ReceiveMessageRequest rq) {
            processed.incrementAndGet();
            if (closed) {
                throw new IllegalStateException("The client has been shut down.");
            }
            if (processFailure != null) {
                throw processFailure;
            }
            if (respondImmediately) {
                return CompletableFuture.completedFuture(ReceiveMessageResponse.builder().build());
            }
            CompletableFuture<ReceiveMessageResponse> response = new CompletableFuture<>();
            if (completer != null) {
                long delayMicros = ThreadLocalRandom.current().nextLong(500);
                completer.schedule(() -> response.complete(ReceiveMessageResponse.builder().build()),
                                   delayMicros, TimeUnit.MICROSECONDS);
                return response;
            }
            synchronized (pending) {
                pending.add(response);
            }
            return response;
        }

        @Override
        CompletableFuture<Void> loadQueueAttributes() {
            return lookup;
        }

        @Override
        public void close() {
            closed = true;
            if (closeFailure != null) {
                throw closeFailure;
            }
        }

        void completePending() {
            List<CompletableFuture<ReceiveMessageResponse>> toComplete;
            synchronized (pending) {
                toComplete = new ArrayList<>(pending);
                pending.clear();
            }
            toComplete.forEach(f -> f.complete(ReceiveMessageResponse.builder().build()));
        }
    }
}
