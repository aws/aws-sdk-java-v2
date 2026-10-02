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
        Thread callingThread = Thread.currentThread();

        try {
            CompletableFuture<Void> dispatch =
                CompletableFuture.runAsync(() -> dispatcher.dispatch(collection("one"),
                                                                      Collections.singletonList(publisher)));

            dispatch.get(1, TimeUnit.SECONDS);
            assertThat(publisher.started.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(publisher.publishThread.get()).isNotSameAs(callingThread);
        } finally {
            publisher.release.countDown();
            dispatcher.close();
        }
    }

    @Test
    public void dispatch_blockingPublisher_doesNotDelayOtherPublisher() throws Exception {
        S3CrtMetricPublisherDispatcher dispatcher =
            new S3CrtMetricPublisherDispatcher(4, Duration.ofSeconds(1));
        BlockingPublisher blockingPublisher = new BlockingPublisher();
        CapturingPublisher healthyPublisher = new CapturingPublisher(1);

        try {
            dispatcher.dispatch(collection("one"), Arrays.asList(blockingPublisher, healthyPublisher));

            assertThat(blockingPublisher.started.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(healthyPublisher.completed.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(healthyPublisher.collectionNames).containsExactly("one");
        } finally {
            blockingPublisher.release.countDown();
            dispatcher.close();
        }
    }

    @Test
    public void dispatch_preservesOrderWithinEachPublisher() throws Exception {
        S3CrtMetricPublisherDispatcher dispatcher =
            new S3CrtMetricPublisherDispatcher(4, Duration.ofSeconds(1));
        CapturingPublisher firstPublisher = new CapturingPublisher(3);
        CapturingPublisher secondPublisher = new CapturingPublisher(3);

        try {
            List<MetricPublisher> publishers = Arrays.asList(firstPublisher, secondPublisher);
            dispatcher.dispatch(collection("one"), publishers);
            dispatcher.dispatch(collection("two"), publishers);
            dispatcher.dispatch(collection("three"), publishers);

            assertThat(firstPublisher.completed.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(secondPublisher.completed.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(firstPublisher.collectionNames).containsExactly("one", "two", "three");
            assertThat(secondPublisher.collectionNames).containsExactly("one", "two", "three");
        } finally {
            dispatcher.close();
        }
    }

    @Test
    public void dispatch_fullQueue_dropsOnlyForSaturatedPublisher() throws Exception {
        S3CrtMetricPublisherDispatcher dispatcher =
            new S3CrtMetricPublisherDispatcher(1, Duration.ofSeconds(1));
        BlockingPublisher blockingPublisher = new BlockingPublisher();
        CapturingPublisher healthyPublisher = new CapturingPublisher(3);
        List<MetricPublisher> publishers = Arrays.asList(blockingPublisher, healthyPublisher);

        try {
            dispatcher.dispatch(collection("one"), publishers);
            assertThat(blockingPublisher.started.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(healthyPublisher.awaitCollectionCount(1, Duration.ofSeconds(1))).isTrue();

            dispatcher.dispatch(collection("two"), publishers);
            assertThat(healthyPublisher.awaitCollectionCount(2, Duration.ofSeconds(1))).isTrue();
            dispatcher.dispatch(collection("three"), publishers);

            assertThat(healthyPublisher.completed.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(healthyPublisher.collectionNames).containsExactly("one", "two", "three");
            assertThat(dispatcher.droppedCollectionCount(blockingPublisher)).isEqualTo(1);
            assertThat(dispatcher.droppedCollectionCount(healthyPublisher)).isZero();
        } finally {
            blockingPublisher.release.countDown();
            dispatcher.close();
        }
    }

    @Test
    public void dispatch_publisherThrows_laneContinues() throws Exception {
        S3CrtMetricPublisherDispatcher dispatcher =
            new S3CrtMetricPublisherDispatcher(4, Duration.ofSeconds(1));
        ThrowingFirstPublisher publisher = new ThrowingFirstPublisher();

        try {
            dispatcher.dispatch(collection("one"), Collections.singletonList(publisher));
            dispatcher.dispatch(collection("two"), Collections.singletonList(publisher));

            assertThat(publisher.secondCallCompleted.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(publisher.calls).hasValue(2);
        } finally {
            dispatcher.close();
        }
    }

    @Test
    public void close_drainsQueuedCollections_withoutClosingPublisher() throws Exception {
        S3CrtMetricPublisherDispatcher dispatcher =
            new S3CrtMetricPublisherDispatcher(4, Duration.ofSeconds(2));
        BlockingPublisher publisher = new BlockingPublisher();
        MetricPublisher publisherSpy = mock(MetricPublisher.class);

        dispatcher.dispatch(collection("one"), Collections.singletonList(publisher));
        assertThat(publisher.started.await(1, TimeUnit.SECONDS)).isTrue();
        dispatcher.dispatch(collection("two"), Collections.singletonList(publisherSpy));

        CompletableFuture<Void> closeFuture = CompletableFuture.runAsync(dispatcher::close);
        verify(publisherSpy, org.mockito.Mockito.timeout(1_000)).publish(collectionNamed("two"));
        publisher.release.countDown();

        closeFuture.get(1, TimeUnit.SECONDS);
        verify(publisherSpy, never()).close();
    }

    @Test
    public void close_blockedPublisher_returnsAfterTimeoutAndCountsQueuedDrops() throws Exception {
        S3CrtMetricPublisherDispatcher dispatcher =
            new S3CrtMetricPublisherDispatcher(1, Duration.ofMillis(50));
        BlockingPublisher publisher = new BlockingPublisher();

        dispatcher.dispatch(collection("one"), Collections.singletonList(publisher));
        assertThat(publisher.started.await(1, TimeUnit.SECONDS)).isTrue();
        dispatcher.dispatch(collection("two"), Collections.singletonList(publisher));

        long startNanos = System.nanoTime();
        dispatcher.close();
        long closeDurationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

        publisher.release.countDown();
        assertThat(closeDurationMillis).isLessThan(1_000);
        assertThat(dispatcher.droppedCollectionCount(publisher)).isEqualTo(1);
    }

    @Test
    public void closeAsync_blockedPublisher_doesNotBlockCallingThread() throws Exception {
        S3CrtMetricPublisherDispatcher dispatcher =
            new S3CrtMetricPublisherDispatcher(1, Duration.ofSeconds(1));
        BlockingPublisher publisher = new BlockingPublisher();

        dispatcher.dispatch(collection("one"), Collections.singletonList(publisher));
        assertThat(publisher.started.await(1, TimeUnit.SECONDS)).isTrue();

        long startNanos = System.nanoTime();
        dispatcher.closeAsync();
        long closeDurationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

        publisher.release.countDown();
        assertThat(closeDurationMillis).isLessThan(100);
    }

    @Test
    public void dispatch_laneLimitReached_evictsIdleLane() throws Exception {
        S3CrtMetricPublisherDispatcher dispatcher =
            new S3CrtMetricPublisherDispatcher(1, 2, Duration.ofSeconds(1));
        CapturingPublisher firstPublisher = new CapturingPublisher(1);
        CapturingPublisher secondPublisher = new CapturingPublisher(1);
        CapturingPublisher thirdPublisher = new CapturingPublisher(1);

        try {
            dispatcher.dispatch(collection("one"), Collections.singletonList(firstPublisher));
            dispatcher.dispatch(collection("two"), Collections.singletonList(secondPublisher));
            assertThat(firstPublisher.completed.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(secondPublisher.completed.await(1, TimeUnit.SECONDS)).isTrue();

            dispatcher.dispatch(collection("three"), Collections.singletonList(thirdPublisher));

            assertThat(thirdPublisher.completed.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(dispatcher.publisherLaneCount()).isEqualTo(2);
            assertThat(dispatcher.unavailableLaneDropCount()).isZero();
        } finally {
            dispatcher.close();
        }
    }

    @Test
    public void dispatch_laneLimitReachedWithBusyLanes_dropsNewPublisherCollection() throws Exception {
        S3CrtMetricPublisherDispatcher dispatcher =
            new S3CrtMetricPublisherDispatcher(1, 2, Duration.ofSeconds(1));
        BlockingPublisher firstPublisher = new BlockingPublisher();
        BlockingPublisher secondPublisher = new BlockingPublisher();
        MetricPublisher rejectedPublisher = mock(MetricPublisher.class);

        try {
            dispatcher.dispatch(collection("one"), Collections.singletonList(firstPublisher));
            dispatcher.dispatch(collection("two"), Collections.singletonList(secondPublisher));
            assertThat(firstPublisher.started.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(secondPublisher.started.await(1, TimeUnit.SECONDS)).isTrue();

            dispatcher.dispatch(collection("three"), Collections.singletonList(rejectedPublisher));

            verify(rejectedPublisher, never()).publish(org.mockito.ArgumentMatchers.any());
            assertThat(dispatcher.publisherLaneCount()).isEqualTo(2);
            assertThat(dispatcher.unavailableLaneDropCount()).isEqualTo(1);
        } finally {
            firstPublisher.release.countDown();
            secondPublisher.release.countDown();
            dispatcher.close();
        }
    }

    private static MetricCollection collection(String name) {
        return MetricCollector.create(name).collect();
    }

    private static MetricCollection collectionNamed(String name) {
        return org.mockito.ArgumentMatchers.argThat(collection -> collection != null && name.equals(collection.name()));
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
        private final Object collectionAdded = new Object();

        private CapturingPublisher(int expectedCollections) {
            this.completed = new CountDownLatch(expectedCollections);
        }

        @Override
        public void publish(MetricCollection metricCollection) {
            collectionNames.add(metricCollection.name());
            completed.countDown();
            synchronized (collectionAdded) {
                collectionAdded.notifyAll();
            }
        }

        @Override
        public void close() {
        }

        private boolean awaitCollectionCount(int expectedCount, Duration timeout) throws InterruptedException {
            long deadline = System.nanoTime() + timeout.toNanos();
            synchronized (collectionAdded) {
                while (collectionNames.size() < expectedCount) {
                    long remainingNanos = deadline - System.nanoTime();
                    if (remainingNanos <= 0) {
                        return false;
                    }
                    TimeUnit.NANOSECONDS.timedWait(collectionAdded, remainingNanos);
                }
                return true;
            }
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
