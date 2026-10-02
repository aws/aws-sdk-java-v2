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

import java.time.Duration;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;

/**
 * Publishes CRT metric collections away from CRT native callback threads.
 *
 * <p>Each active publisher has an independent bounded, serial lane. This preserves the order in which that publisher's
 * collections are accepted and prevents a slow publisher from delaying other publishers. The number of lanes is bounded:
 * an idle lane is evicted when capacity is reached, or the new publisher's collection is dropped if every lane is busy.
 * When an existing publisher's lane is full, only that publisher's collection is dropped.
 */
@SdkInternalApi
final class S3CrtMetricPublisherDispatcher implements SdkAutoCloseable {
    private static final Logger log = Logger.loggerFor(S3CrtMetricPublisherDispatcher.class);
    private static final int DEFAULT_QUEUE_CAPACITY = 1_024;
    private static final int DEFAULT_MAX_PUBLISHER_LANES = 64;
    private static final Duration DEFAULT_CLOSE_TIMEOUT = Duration.ofSeconds(10);
    private static final long IDLE_THREAD_TIMEOUT_SECONDS = 30;
    private static final ThreadFactory CLOSE_THREAD_FACTORY =
        new ThreadFactoryBuilder()
            .daemonThreads(true)
            .threadNamePrefix("s3-crt-metric-publisher-cleanup")
            .build();

    private final Object lock = new Object();
    private final Map<MetricPublisher, PublisherLane> publisherLanes = new IdentityHashMap<>();
    private final int queueCapacity;
    private final int maxPublisherLanes;
    private final Duration closeTimeout;
    private final AtomicLong unavailableLaneDropCount = new AtomicLong();
    private final AtomicBoolean asyncCloseStarted = new AtomicBoolean();
    private boolean closed;

    S3CrtMetricPublisherDispatcher() {
        this(DEFAULT_QUEUE_CAPACITY, DEFAULT_MAX_PUBLISHER_LANES, DEFAULT_CLOSE_TIMEOUT);
    }

    @SdkTestInternalApi
    S3CrtMetricPublisherDispatcher(int queueCapacity, Duration closeTimeout) {
        this(queueCapacity, DEFAULT_MAX_PUBLISHER_LANES, closeTimeout);
    }

    @SdkTestInternalApi
    S3CrtMetricPublisherDispatcher(int queueCapacity, int maxPublisherLanes, Duration closeTimeout) {
        if (queueCapacity <= 0) {
            throw new IllegalArgumentException("queueCapacity must be greater than zero");
        }
        if (maxPublisherLanes <= 0) {
            throw new IllegalArgumentException("maxPublisherLanes must be greater than zero");
        }
        if (closeTimeout.isNegative()) {
            throw new IllegalArgumentException("closeTimeout must not be negative");
        }
        this.queueCapacity = queueCapacity;
        this.maxPublisherLanes = maxPublisherLanes;
        this.closeTimeout = closeTimeout;
    }

    /**
     * Dispatch the collection to each publisher. The synchronized section only establishes a consistent acceptance order
     * across concurrent CRT callbacks and performs non-blocking queue insertion. Publisher code is never invoked while
     * holding the lock.
     */
    void dispatch(MetricCollection collection, List<MetricPublisher> publishers) {
        synchronized (lock) {
            if (closed) {
                log.warn(() -> "Dropping CRT S3 metric collection because the metric dispatcher is closed.");
                return;
            }

            for (MetricPublisher publisher : publishers) {
                PublisherLane lane = publisherLanes.get(publisher);
                if (lane == null) {
                    lane = createPublisherLane(publisher);
                }
                if (lane != null) {
                    lane.offer(collection);
                }
            }
        }
    }

    private PublisherLane createPublisherLane(MetricPublisher publisher) {
        if (publisherLanes.size() >= maxPublisherLanes) {
            PublisherLane idleLane = publisherLanes.values().stream()
                                                    .filter(PublisherLane::isIdle)
                                                    .findFirst()
                                                    .orElse(null);
            if (idleLane == null) {
                recordUnavailableLaneDrop();
                return null;
            }
            publisherLanes.remove(idleLane.publisher());
            idleLane.shutdown();
        }

        PublisherLane lane = newPublisherLane(publisher);
        publisherLanes.put(publisher, lane);
        return lane;
    }

    private void recordUnavailableLaneDrop() {
        long dropped = unavailableLaneDropCount.incrementAndGet();
        if (isPowerOfTwo(dropped)) {
            log.warn(() -> "Dropped CRT S3 metric collections because all " + maxPublisherLanes
                           + " publisher lanes are busy. Total dropped: " + dropped + ".");
        }
    }

    @SdkTestInternalApi
    long droppedCollectionCount(MetricPublisher publisher) {
        synchronized (lock) {
            PublisherLane lane = publisherLanes.get(publisher);
            return lane == null ? 0 : lane.droppedCollectionCount();
        }
    }

    @SdkTestInternalApi
    int publisherLaneCount() {
        synchronized (lock) {
            return publisherLanes.size();
        }
    }

    @SdkTestInternalApi
    long unavailableLaneDropCount() {
        return unavailableLaneDropCount.get();
    }

    void closeAsync() {
        if (asyncCloseStarted.compareAndSet(false, true)) {
            CLOSE_THREAD_FACTORY.newThread(this::close).start();
        }
    }

    @Override
    public void close() {
        List<PublisherLane> lanes;
        synchronized (lock) {
            if (closed) {
                return;
            }
            closed = true;
            lanes = new ArrayList<>(publisherLanes.values());
            lanes.forEach(PublisherLane::shutdown);
        }

        long deadline = System.nanoTime() + closeTimeout.toNanos();
        boolean interrupted = false;
        for (PublisherLane lane : lanes) {
            long remainingNanos = Math.max(0, deadline - System.nanoTime());
            try {
                if (!lane.awaitTermination(remainingNanos, TimeUnit.NANOSECONDS)) {
                    lane.forceShutdown();
                }
            } catch (InterruptedException e) {
                interrupted = true;
                break;
            }
        }

        if (interrupted) {
            lanes.forEach(PublisherLane::forceShutdown);
            Thread.currentThread().interrupt();
            log.warn(() -> "Interrupted while draining CRT S3 metric publisher queues.");
        } else {
            lanes.stream().filter(lane -> !lane.isTerminated()).forEach(PublisherLane::forceShutdown);
        }
    }

    private PublisherLane newPublisherLane(MetricPublisher publisher) {
        return new PublisherLane(publisher, queueCapacity);
    }

    private final class PublisherLane {
        private final MetricPublisher publisher;
        private final ThreadPoolExecutor executor;
        private final AtomicLong droppedCollectionCount = new AtomicLong();
        private final AtomicBoolean forceShutdown = new AtomicBoolean();
        private int pendingCollections;

        private PublisherLane(MetricPublisher publisher, int queueCapacity) {
            this.publisher = publisher;
            this.executor = new ThreadPoolExecutor(
                0, 1,
                IDLE_THREAD_TIMEOUT_SECONDS, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                new ThreadFactoryBuilder()
                    .daemonThreads(true)
                    .threadNamePrefix("s3-crt-metric-publisher")
                    .build());
        }

        private void offer(MetricCollection collection) {
            pendingCollections++;
            try {
                executor.execute(() -> publish(collection));
            } catch (RejectedExecutionException e) {
                pendingCollections--;
                recordDroppedCollection();
            }
        }

        private void publish(MetricCollection collection) {
            try {
                publisher.publish(collection);
            } catch (RuntimeException e) {
                log.warn(() -> "Failed to publish CRT S3 metrics through " + publisher.getClass().getName(), e);
            } finally {
                synchronized (lock) {
                    pendingCollections--;
                }
            }
        }

        private MetricPublisher publisher() {
            return publisher;
        }

        private boolean isIdle() {
            return pendingCollections == 0;
        }

        private void shutdown() {
            executor.shutdown();
        }

        private boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return executor.awaitTermination(timeout, unit);
        }

        private boolean isTerminated() {
            return executor.isTerminated();
        }

        private void forceShutdown() {
            if (!forceShutdown.compareAndSet(false, true)) {
                return;
            }
            int queuedCollectionCount = executor.shutdownNow().size();
            if (queuedCollectionCount > 0) {
                droppedCollectionCount.addAndGet(queuedCollectionCount);
            }
            log.warn(() -> "Timed out draining CRT S3 metrics for publisher " + publisher.getClass().getName()
                           + ". Undelivered queued collections: " + queuedCollectionCount + ".");
        }

        private void recordDroppedCollection() {
            long dropped = droppedCollectionCount.incrementAndGet();
            if (isPowerOfTwo(dropped)) {
                log.warn(() -> "Dropped CRT S3 metric collections because the queue for publisher "
                               + publisher.getClass().getName() + " is full. Total dropped: " + dropped + ".");
            }
        }

        private long droppedCollectionCount() {
            return droppedCollectionCount.get();
        }

    }

    private static boolean isPowerOfTwo(long value) {
        return (value & (value - 1)) == 0;
    }
}
