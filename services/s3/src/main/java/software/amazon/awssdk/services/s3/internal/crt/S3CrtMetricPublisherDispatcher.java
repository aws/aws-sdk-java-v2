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
 * <p>Collections are submitted without blocking to a single bounded queue and published in acceptance order by one
 * daemon worker. A slow or blocked publisher can delay the other publishers configured for the client, but it cannot
 * block a CRT callback thread or cause the queue to grow without bound.
 */
@SdkInternalApi
final class S3CrtMetricPublisherDispatcher implements SdkAutoCloseable {
    private static final Logger log = Logger.loggerFor(S3CrtMetricPublisherDispatcher.class);
    private static final int DEFAULT_QUEUE_CAPACITY = 128;
    private static final Duration DEFAULT_CLOSE_TIMEOUT = Duration.ofSeconds(5);
    private static final ThreadFactory CLOSE_THREAD_FACTORY =
        new ThreadFactoryBuilder()
            .daemonThreads(true)
            .threadNamePrefix("s3-crt-metric-publisher-cleanup")
            .build();

    private final Object lock = new Object();
    private final ThreadPoolExecutor executor;
    private final int queueCapacity;
    private final Duration closeTimeout;
    private final AtomicLong queueFullDropCount = new AtomicLong();
    private final AtomicLong closedDispatcherDropCount = new AtomicLong();
    private final AtomicBoolean asyncCloseStarted = new AtomicBoolean();
    private boolean closed;

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
                .build());
    }

    /**
     * Dispatches one collection to all configured publishers as a single queue task. Synchronization establishes one
     * acceptance order across concurrent CRT callbacks and coordinates submission with close. Publisher code is never
     * invoked while holding the lock.
     */
    void dispatch(MetricCollection collection, List<MetricPublisher> publishers) {
        synchronized (lock) {
            if (closed) {
                recordClosedDispatcherDrop();
                return;
            }

            try {
                executor.execute(() -> publish(collection, publishers));
            } catch (RejectedExecutionException e) {
                recordQueueFullDrop();
            }
        }
    }

    private void publish(MetricCollection collection, List<MetricPublisher> publishers) {
        for (MetricPublisher publisher : publishers) {
            try {
                publisher.publish(collection);
            } catch (RuntimeException e) {
                log.warn(() -> "Failed to publish CRT S3 metrics through " + publisher.getClass().getName(), e);
            }
        }
    }

    private void recordQueueFullDrop() {
        long dropped = queueFullDropCount.incrementAndGet();
        if (isPowerOfTwo(dropped)) {
            log.warn(() -> "Dropped CRT S3 metric collections because the metric publisher queue is full. "
                           + "Queue capacity: " + queueCapacity + ", total dropped: " + dropped + ".");
        }
    }

    private void recordClosedDispatcherDrop() {
        long dropped = closedDispatcherDropCount.incrementAndGet();
        if (isPowerOfTwo(dropped)) {
            log.warn(() -> "Dropped CRT S3 metric collections because the metric publisher dispatcher is closed. "
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
        synchronized (lock) {
            if (closed) {
                return;
            }
            closed = true;
            executor.shutdown();
        }

        boolean terminated = false;
        try {
            terminated = executor.awaitTermination(closeTimeout.toNanos(), TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            int queuedCollectionCount = forceShutdown();
            Thread.currentThread().interrupt();
            log.warn(() -> "Interrupted while draining CRT S3 metric publisher queue. Undelivered queued collections: "
                           + queuedCollectionCount + ".");
            return;
        }

        if (!terminated) {
            int queuedCollectionCount = forceShutdown();
            log.warn(() -> "Timed out draining CRT S3 metric publisher queue. Undelivered queued collections: "
                           + queuedCollectionCount + ".");
        }
    }

    private int forceShutdown() {
        return executor.shutdownNow().size();
    }

    private static boolean isPowerOfTwo(long value) {
        return (value & (value - 1)) == 0;
    }
}
