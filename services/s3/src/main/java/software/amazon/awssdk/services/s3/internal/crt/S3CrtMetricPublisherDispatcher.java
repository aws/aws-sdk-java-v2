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
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
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
 * <p>Collections are submitted without blocking to a single bounded queue and published in acceptance order by one
 * daemon worker. A slow or blocked publisher can delay the other publishers configured for the client, but it cannot
 * block a CRT callback thread or cause the queue to grow without bound.
 */
@SdkInternalApi
final class S3CrtMetricPublisherDispatcher implements SdkAutoCloseable {
    private static final Logger log = Logger.loggerFor(S3CrtMetricPublisherDispatcher.class);
    // Uses CloudWatchMetricPublisher's 128-entry queue as an existing SDK precedent. This limits each client
    // to retaining at most 128 queued metric collections awaiting publication.
    private static final int DEFAULT_QUEUE_CAPACITY = 128;
    // Allows time for queued metrics to be published while preventing a slow publisher from delaying shutdown
    // indefinitely.
    private static final Duration DEFAULT_CLOSE_TIMEOUT = Duration.ofSeconds(5);
    // Delay between attempts to enqueue a flush marker when the queue is full; retries stop when
    // DEFAULT_CLOSE_TIMEOUT expires.
    private static final long FLUSH_RETRY_DELAY_NANOS = TimeUnit.MILLISECONDS.toNanos(10);
    private static final ThreadFactory CLOSE_THREAD_FACTORY =
        new ThreadFactoryBuilder()
            .daemonThreads(true)
            .threadNamePrefix("s3-crt-metric-publisher-cleanup")
            .build();

    private final ThreadPoolExecutor executor;
    private final int queueCapacity;
    private final Duration closeTimeout;
    private final AtomicLong queueFullDropCount = new AtomicLong();
    private final AtomicLong closedDispatcherDropCount = new AtomicLong();
    private final AtomicBoolean dispatchAttempted = new AtomicBoolean();
    private final AtomicBoolean asyncCloseStarted = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();

    S3CrtMetricPublisherDispatcher() {
        this(DEFAULT_QUEUE_CAPACITY, DEFAULT_CLOSE_TIMEOUT);
    }

    @SdkTestInternalApi
    S3CrtMetricPublisherDispatcher(int queueCapacity, Duration closeTimeout) {
        if (queueCapacity <= 0) {
            throw new IllegalArgumentException("queueCapacity must be greater than zero");
        }
        if (closeTimeout.isNegative()) {
            throw new IllegalArgumentException("closeTimeout must not be negative");
        }
        this.queueCapacity = queueCapacity;
        this.closeTimeout = closeTimeout;
        this.executor = new ThreadPoolExecutor(
            1, 1,
            0, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(queueCapacity),
            new ThreadFactoryBuilder()
                .daemonThreads(true)
                .threadNamePrefix("s3-crt-metric-publisher")
                .build(),
            this::handleRejectedTask);
    }

    /**
     * Dispatches one collection to all configured publishers as a single queue task. Queue admission is non-blocking,
     * and publisher code is only invoked by the executor worker.
     */
    void dispatch(MetricCollection collection, List<MetricPublisher> publishers) {
        dispatchAttempted.set(true);
        executor.execute(new PublishTask(collection, publishers));
    }

    /**
     * Waits for collections accepted before the marker task to be published, up to the configured close timeout.
     */
    void flushPendingMetrics() {
        if (!dispatchAttempted.get() || executor.isShutdown()) {
            return;
        }

        long deadlineNanos = System.nanoTime() + closeTimeout.toNanos();
        FlushMarkerTask flushMarker = submitFlushMarker(deadlineNanos);
        if (flushMarker == null) {
            return;
        }

        long remainingNanos = deadlineNanos - System.nanoTime();
        if (remainingNanos <= 0) {
            logFlushTimeout();
            return;
        }

        try {
            if (!flushMarker.await(remainingNanos)) {
                logFlushTimeout();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.debug(() -> "Interrupted while waiting for queued CRT S3 metrics to be published.");
        }
    }

    private void publish(MetricCollection collection, List<MetricPublisher> publishers) {
        for (MetricPublisher publisher : publishers) {
            try {
                publisher.publish(collection);
            } catch (RuntimeException e) {
                log.debug(() -> "Failed to publish CRT S3 metrics through " + publisher.getClass().getName(), e);
            }
        }
    }

    private FlushMarkerTask submitFlushMarker(long deadlineNanos) {
        while (!executor.isShutdown()) {
            long remainingNanos = deadlineNanos - System.nanoTime();
            if (remainingNanos <= 0) {
                logFlushTimeout();
                return null;
            }

            FlushMarkerTask flushMarker = new FlushMarkerTask();
            executor.execute(flushMarker);
            if (!flushMarker.rejected()) {
                return flushMarker;
            }

            try {
                TimeUnit.NANOSECONDS.sleep(Math.min(remainingNanos, FLUSH_RETRY_DELAY_NANOS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.debug(() -> "Interrupted while waiting to flush queued CRT S3 metrics.");
                return null;
            }
        }
        return null;
    }

    private void handleRejectedTask(Runnable task, ThreadPoolExecutor pool) {
        if (task instanceof FlushMarkerTask) {
            ((FlushMarkerTask) task).reject();
        } else if (pool.isShutdown()) {
            recordClosedDispatcherDrop();
        } else {
            recordQueueFullDrop();
        }
    }

    private void logFlushTimeout() {
        log.debug(() -> "Timed out waiting for queued CRT S3 metrics to be published.");
    }

    private void recordQueueFullDrop() {
        long dropped = queueFullDropCount.incrementAndGet();
        if (isPowerOfTwo(dropped)) {
            log.debug(() -> "Dropped CRT S3 metric collections because the metric publisher queue is full. "
                            + "Queue capacity: " + queueCapacity + ", total dropped: " + dropped + ".");
        }
    }

    private void recordClosedDispatcherDrop() {
        long dropped = closedDispatcherDropCount.incrementAndGet();
        if (isPowerOfTwo(dropped)) {
            log.debug(() -> "Dropped CRT S3 metric collections because the metric publisher dispatcher is closed. "
                            + "Total dropped after close: " + dropped + ".");
        }
    }

    void startAsyncClose() {
        if (asyncCloseStarted.compareAndSet(false, true)) {
            CLOSE_THREAD_FACTORY.newThread(this::close).start();
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        executor.shutdown();

        boolean terminated = false;
        try {
            terminated = executor.awaitTermination(closeTimeout.toNanos(), TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            int queuedCollectionCount = forceShutdown();
            Thread.currentThread().interrupt();
            log.debug(() -> "Interrupted while draining CRT S3 metric publisher queue. Undelivered queued collections: "
                            + queuedCollectionCount + ".");
            return;
        }

        if (!terminated) {
            int queuedCollectionCount = forceShutdown();
            log.debug(() -> "Timed out draining CRT S3 metric publisher queue. Undelivered queued collections: "
                            + queuedCollectionCount + ".");
        }
    }

    private int forceShutdown() {
        return (int) executor.shutdownNow()
                             .stream()
                             .filter(PublishTask.class::isInstance)
                             .count();
    }

    private static boolean isPowerOfTwo(long value) {
        return (value & (value - 1)) == 0;
    }

    private final class PublishTask implements Runnable {
        private final MetricCollection collection;
        private final List<MetricPublisher> publishers;

        private PublishTask(MetricCollection collection, List<MetricPublisher> publishers) {
            this.collection = collection;
            this.publishers = publishers;
        }

        @Override
        public void run() {
            publish(collection, publishers);
        }
    }

    private static final class FlushMarkerTask implements Runnable {
        private final CountDownLatch completion = new CountDownLatch(1);
        private volatile boolean rejected;

        @Override
        public void run() {
            completion.countDown();
        }

        private void reject() {
            rejected = true;
        }

        private boolean rejected() {
            return rejected;
        }

        private boolean await(long timeoutNanos) throws InterruptedException {
            return completion.await(timeoutNanos, TimeUnit.NANOSECONDS);
        }
    }
}
