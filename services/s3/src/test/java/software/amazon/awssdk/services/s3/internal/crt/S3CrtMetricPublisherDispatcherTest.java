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

package software.amazon.awssdk.services.s3.internal.crt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.MetricPublisher;

public class S3CrtMetricPublisherDispatcherTest {

    @Test
    public void dispatch_blockingPublisher_doesNotBlockCallingThread() throws Exception {
        S3CrtMetricPublisherDispatcher dispatcher =
            new S3CrtMetricPublisherDispatcher(4, Duration.ofSeconds(1));
        BlockingPublisher publisher = new BlockingPublisher();
        AtomicReference<Thread> dispatchThread = new AtomicReference<>();

        try {
            CompletableFuture<Void> dispatch =
                CompletableFuture.runAsync(() -> {
                    dispatchThread.set(Thread.currentThread());
                    dispatcher.dispatch(collection("one"), Collections.singletonList(publisher));
                });

            dispatch.get(1, TimeUnit.SECONDS);
            assertThat(publisher.started.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(publisher.publishThread.get()).isNotSameAs(dispatchThread.get());
            assertThat(publisher.publishThread.get().isDaemon()).isTrue();
        } finally {
            publisher.release.countDown();
            dispatcher.close();
        }
    }

    @Test
    public void dispatch_blockingPublisher_delaysOtherPublisherWithoutBlockingCaller() throws Exception {
        S3CrtMetricPublisherDispatcher dispatcher =
            new S3CrtMetricPublisherDispatcher(4, Duration.ofSeconds(1));
        BlockingPublisher blockingPublisher = new BlockingPublisher();
        CapturingPublisher healthyPublisher = new CapturingPublisher(1);

        try {
            dispatcher.dispatch(collection("one"), Arrays.asList(blockingPublisher, healthyPublisher));

            assertThat(blockingPublisher.started.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(healthyPublisher.completed.await(50, TimeUnit.MILLISECONDS)).isFalse();

            blockingPublisher.release.countDown();
            assertThat(healthyPublisher.completed.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(healthyPublisher.collectionNames).containsExactly("one");
        } finally {
            blockingPublisher.release.countDown();
            dispatcher.close();
        }
    }

    @Test
    public void dispatch_multiplePublishers_preservesGlobalAcceptanceOrder() throws Exception {
        S3CrtMetricPublisherDispatcher dispatcher =
            new S3CrtMetricPublisherDispatcher(4, Duration.ofSeconds(1));
        List<String> publications = new CopyOnWriteArrayList<>();
        CountDownLatch completed = new CountDownLatch(6);
        MetricPublisher firstPublisher = recordingPublisher("first", publications, completed);
        MetricPublisher secondPublisher = recordingPublisher("second", publications, completed);
        List<MetricPublisher> publishers = Arrays.asList(firstPublisher, secondPublisher);

        try {
            dispatcher.dispatch(collection("one"), publishers);
            dispatcher.dispatch(collection("two"), publishers);
            dispatcher.dispatch(collection("three"), publishers);

            assertThat(completed.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(publications).containsExactly("first-one", "second-one",
                                                     "first-two", "second-two",
                                                     "first-three", "second-three");
        } finally {
            dispatcher.close();
        }
    }

    @Test
    public void dispatch_fullQueue_dropsWholeCollection() throws Exception {
        S3CrtMetricPublisherDispatcher dispatcher =
            new S3CrtMetricPublisherDispatcher(1, Duration.ofSeconds(1));
        BlockingPublisher blockingPublisher = new BlockingPublisher();
        CapturingPublisher healthyPublisher = new CapturingPublisher(2);
        List<MetricPublisher> publishers = Arrays.asList(blockingPublisher, healthyPublisher);

        try {
            dispatcher.dispatch(collection("one"), publishers);
            assertThat(blockingPublisher.started.await(1, TimeUnit.SECONDS)).isTrue();

            dispatcher.dispatch(collection("two"), publishers);
            dispatcher.dispatch(collection("three"), publishers);

            blockingPublisher.release.countDown();

            assertThat(healthyPublisher.completed.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(healthyPublisher.collectionNames).containsExactly("one", "two");
        } finally {
            blockingPublisher.release.countDown();
            dispatcher.close();
        }
    }

    @Test
    public void flushPendingMetrics_fullQueue_retriesUntilMarkerIsAccepted() throws Exception {
        S3CrtMetricPublisherDispatcher dispatcher =
            new S3CrtMetricPublisherDispatcher(1, Duration.ofSeconds(2));
        BlockingPublisher blockingPublisher = new BlockingPublisher();
        MetricPublisher queuedPublisher = mock(MetricPublisher.class);
        AtomicReference<Thread> flushThread = new AtomicReference<>();

        CompletableFuture<Void> flushFuture = null;
        try {
            dispatcher.dispatch(collection("one"), Collections.singletonList(blockingPublisher));
            assertThat(blockingPublisher.started.await(1, TimeUnit.SECONDS)).isTrue();
            dispatcher.dispatch(collection("two"), Collections.singletonList(queuedPublisher));

            flushFuture = CompletableFuture.runAsync(() -> {
                flushThread.set(Thread.currentThread());
                dispatcher.flushPendingMetrics();
            });

            assertThat(waitForThreadState(flushThread, Thread.State.TIMED_WAITING, Duration.ofSeconds(1))).isTrue();
            assertThat(flushFuture.isDone()).isFalse();

            blockingPublisher.release.countDown();

            flushFuture.get(1, TimeUnit.SECONDS);
            verify(queuedPublisher).publish(collectionNamed("two"));
        } finally {
            blockingPublisher.release.countDown();
            dispatcher.close();
        }
    }

    @Test
    public void dispatch_publisherThrows_doesNotAffectRemainingPublishersAndWorkerContinues() throws Exception {
        S3CrtMetricPublisherDispatcher dispatcher =
            new S3CrtMetricPublisherDispatcher(4, Duration.ofSeconds(1));
        ThrowingFirstPublisher throwingPublisher = new ThrowingFirstPublisher();
        CapturingPublisher healthyPublisher = new CapturingPublisher(2);
        List<MetricPublisher> publishers = Arrays.asList(throwingPublisher, healthyPublisher);

        try {
            dispatcher.dispatch(collection("one"), publishers);
            dispatcher.dispatch(collection("two"), publishers);

            assertThat(throwingPublisher.secondCallCompleted.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(healthyPublisher.completed.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(healthyPublisher.collectionNames).containsExactly("one", "two");
        } finally {
            dispatcher.close();
        }
    }

    @Test
    public void close_drainsQueuedCollectionsWithoutClosingPublishers() throws Exception {
        S3CrtMetricPublisherDispatcher dispatcher =
            new S3CrtMetricPublisherDispatcher(4, Duration.ofSeconds(2));
        BlockingPublisher blockingPublisher = new BlockingPublisher();
        MetricPublisher publisherSpy = mock(MetricPublisher.class);

        dispatcher.dispatch(collection("one"), Collections.singletonList(blockingPublisher));
        assertThat(blockingPublisher.started.await(1, TimeUnit.SECONDS)).isTrue();
        dispatcher.dispatch(collection("two"), Collections.singletonList(publisherSpy));

        CompletableFuture<Void> closeFuture = CompletableFuture.runAsync(dispatcher::close);
        blockingPublisher.release.countDown();

        closeFuture.get(1, TimeUnit.SECONDS);
        verify(publisherSpy).publish(collectionNamed("two"));
        verify(publisherSpy, never()).close();
    }

    @Test
    public void close_blockedPublisher_returnsAfterTimeoutAndDropsQueuedCollections() throws Exception {
        S3CrtMetricPublisherDispatcher dispatcher =
            new S3CrtMetricPublisherDispatcher(1, Duration.ofMillis(50));
        BlockingPublisher blockingPublisher = new BlockingPublisher();
        MetricPublisher queuedPublisher = mock(MetricPublisher.class);

        try {
            dispatcher.dispatch(collection("one"), Collections.singletonList(blockingPublisher));
            assertThat(blockingPublisher.started.await(1, TimeUnit.SECONDS)).isTrue();
            dispatcher.dispatch(collection("two"), Collections.singletonList(queuedPublisher));

            long startNanos = System.nanoTime();
            dispatcher.close();
            long closeDurationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

            assertThat(closeDurationMillis).isLessThan(1_000);
            verify(queuedPublisher, never()).publish(org.mockito.ArgumentMatchers.any());
        } finally {
            blockingPublisher.release.countDown();
            dispatcher.close();
        }
    }

    @Test
    public void startAsyncClose_blockedPublisher_doesNotBlockCallingThread() throws Exception {
        S3CrtMetricPublisherDispatcher dispatcher =
            new S3CrtMetricPublisherDispatcher(1, Duration.ofSeconds(1));
        BlockingPublisher publisher = new BlockingPublisher();

        try {
            dispatcher.dispatch(collection("one"), Collections.singletonList(publisher));
            assertThat(publisher.started.await(1, TimeUnit.SECONDS)).isTrue();

            CompletableFuture.runAsync(dispatcher::startAsyncClose).get(1, TimeUnit.SECONDS);
        } finally {
            publisher.release.countDown();
            dispatcher.close();
        }
    }

    @Test
    public void dispatch_closedDispatcher_dropsWithoutInvokingPublisher() {
        S3CrtMetricPublisherDispatcher dispatcher =
            new S3CrtMetricPublisherDispatcher(1, Duration.ofSeconds(1));
        MetricPublisher publisher = mock(MetricPublisher.class);

        dispatcher.close();
        dispatcher.dispatch(collection("one"), Collections.singletonList(publisher));

        verify(publisher, never()).publish(org.mockito.ArgumentMatchers.any());
    }

    private static MetricPublisher recordingPublisher(String publisherName,
                                                       List<String> publications,
                                                       CountDownLatch completed) {
        return new MetricPublisher() {
            @Override
            public void publish(MetricCollection metricCollection) {
                publications.add(publisherName + "-" + metricCollection.name());
                completed.countDown();
            }

            @Override
            public void close() {
            }
        };
    }

    private static MetricCollection collection(String name) {
        return MetricCollector.create(name).collect();
    }

    private static MetricCollection collectionNamed(String name) {
        return org.mockito.ArgumentMatchers.argThat(collection -> collection != null && name.equals(collection.name()));
    }

    private static boolean waitForThreadState(AtomicReference<Thread> threadReference,
                                              Thread.State state,
                                              Duration timeout) throws InterruptedException {
        long deadlineNanos = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadlineNanos) {
            Thread thread = threadReference.get();
            if (thread != null && thread.getState() == state) {
                return true;
            }
            TimeUnit.MILLISECONDS.sleep(1);
        }
        return false;
    }

    private static final class BlockingPublisher implements MetricPublisher {
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final AtomicReference<Thread> publishThread = new AtomicReference<>();

        @Override
        public void publish(MetricCollection metricCollection) {
            publishThread.set(Thread.currentThread());
            started.countDown();
            boolean released = false;
            while (!released) {
                try {
                    release.await();
                    released = true;
                } catch (InterruptedException ignored) {
                    // Deliberately model a publisher that does not respond to interruption.
                }
            }
        }

        @Override
        public void close() {
        }
    }

    private static final class CapturingPublisher implements MetricPublisher {
        private final List<String> collectionNames = new CopyOnWriteArrayList<>();
        private final CountDownLatch completed;

        private CapturingPublisher(int expectedCollections) {
            this.completed = new CountDownLatch(expectedCollections);
        }

        @Override
        public void publish(MetricCollection metricCollection) {
            collectionNames.add(metricCollection.name());
            completed.countDown();
        }

        @Override
        public void close() {
        }
    }

    private static final class ThrowingFirstPublisher implements MetricPublisher {
        private final AtomicInteger calls = new AtomicInteger();
        private final CountDownLatch secondCallCompleted = new CountDownLatch(1);

        @Override
        public void publish(MetricCollection metricCollection) {
            if (calls.incrementAndGet() == 1) {
                throw new IllegalStateException("expected");
            }
            secondCallCompleted.countDown();
        }

        @Override
        public void close() {
        }
    }
}
