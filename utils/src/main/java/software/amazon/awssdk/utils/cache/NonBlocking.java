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

package software.amazon.awssdk.utils.cache;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;

/**
 * A {@link CachedSupplier.PrefetchStrategy} that will run a single thread in the background to update the value. A call to
 * prefetch on this strategy will never return.
 *
 * Multiple calls to {@link #prefetch(Runnable)} will still only result in one background task performing the update.
 */
@SdkProtectedApi
public class NonBlocking implements CachedSupplier.PrefetchStrategy {
    private static final Logger log = Logger.loggerFor(NonBlocking.class);

    /**
     * The maximum number of concurrent refreshes before we start logging warnings and skipping refreshes.
     */
    private static final int MAX_CONCURRENT_REFRESHES = 100;

    /**
     * The {@link Random} instance used for calculating jitter of the background prefetches.
     */
    private static final Random JITTER_RANDOM = new Random();

    /**
     * Thread used to kick off refreshes during the prefetch window. This does not do the actual refreshing. That's left for
     * the {@link #EXECUTOR}.
     */
    private static final ScheduledThreadPoolExecutor SCHEDULER =
        new ScheduledThreadPoolExecutor(1, new ThreadFactoryBuilder().daemonThreads(true).build());

    /**
     * Threads used to do the actual work of refreshing the values (because the cached supplier might block, so we don't
     * want the work to be done by a small thread pool). This executor is created as unbounded, but we start complaining and
     * skipping refreshes when there are more than {@link #MAX_CONCURRENT_REFRESHES} running.
     */
    private static final ThreadPoolExecutor EXECUTOR =
        new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                               60L, TimeUnit.SECONDS,
                               new SynchronousQueue<>(),
                               new ThreadFactoryBuilder().daemonThreads(true).build());

    /**
     * An incrementing number, used to uniquely identify an instance of NonBlocking in the {@link #asyncThreadName}.
     */
    private static final AtomicLong THREAD_NUMBER = new AtomicLong(0);

    /**
     * Whether we are currently refreshing the supplier. This is used to make sure only one caller is blocking at a time.
     */
    private final AtomicBoolean currentlyPrefetching = new AtomicBoolean(false);

    /**
     * Name of the thread refreshing the cache for this strategy.
     */
    private final String asyncThreadName;

    /**
     * The refresh task currently scheduled for this non-blocking instance. We ensure that no more than one task is scheduled
     * per instance.
     */
    private final AtomicReference<ScheduledFuture<?>> refreshTask = new AtomicReference<>();

    /**
     * The minimum amount of time allowed between async refreshes, primarily adjustable for testing purposes.
     */
    private final Duration minimumRefreshFrequency;

    /**
     * Whether this strategy has been shutdown (and should stop doing background refreshes)
     */
    private volatile boolean shutdown = false;

    /**
     * The cached supplier using this non-blocking instance.
     */
    private volatile CachedSupplier<?> cachedSupplier;

    static {
        // Ensure that cancelling a task actually removes it from the queue.
        SCHEDULER.setRemoveOnCancelPolicy(true);
    }

    /**
     * Create a non-blocking prefetch strategy that uses the provided value for the name of the background thread that will be
     * performing the update.
     */
    public NonBlocking(String asyncThreadName) {
        this(asyncThreadName, Duration.ofSeconds(60));
    }

    @SdkTestInternalApi
    NonBlocking(String asyncThreadName, Duration minimumRefreshFrequency) {
        this.asyncThreadName = asyncThreadName + "-" + THREAD_NUMBER.getAndIncrement();
        this.minimumRefreshFrequency = minimumRefreshFrequency;
    }

    @SdkTestInternalApi
    static ThreadPoolExecutor executor() {
        return EXECUTOR;
    }

    @Override
    public void initializeCachedSupplier(CachedSupplier<?> cachedSupplier) {
        this.cachedSupplier = cachedSupplier;
    }

    @Override
    public void prefetch(Runnable valueUpdater) {
        // Only run one async prefetch at a time.
        if (currentlyPrefetching.compareAndSet(false, true)) {
            tryRunBackgroundTask(valueUpdater, () -> currentlyPrefetching.set(false));
        }
    }

    @Override
    public <T> RefreshResult<T> fetch(Supplier<RefreshResult<T>> supplier) {
        RefreshResult<T> result = supplier.get();
        if (result.staleTime() == null || result.prefetchTime() == null) {
            return result;
        }

        getRefreshTime(result).ifPresent(this::schedulePrefetch);
        return result;
    }

    private Optional<Instant> getRefreshTime(RefreshResult<?> result) {
        Instant minStart = Instant.now().plus(minimumRefreshFrequency);
        Instant rangeStart = result.prefetchTime().isBefore(minStart) ? minStart : result.prefetchTime();

        if (Duration.between(Instant.now(), rangeStart).toDays() > 7) {
            log.debug(() -> "Skipping background refresh because the prefetch time is too far in the future: " + rangeStart);
            return Optional.empty();
        }

        Instant maxEnd = rangeStart.plus(1, HOURS);
        Instant rangeEnd = result.staleTime().isAfter(maxEnd) ? maxEnd : result.staleTime().minus(1, MINUTES);

        if (rangeEnd.isBefore(rangeStart)) {
            return Optional.of(rangeStart);
        }

        return Optional.of(randomTimeBetween(rangeStart, rangeEnd));
    }

    private Instant randomTimeBetween(Instant rangeStart, Instant rangeEnd) {
        Duration timeBetween = Duration.between(rangeStart, rangeEnd);
        return rangeStart.plusMillis(Math.abs(JITTER_RANDOM.nextLong() % timeBetween.toMillis()));
    }

    private void schedulePrefetch(Instant refreshTime) {
        if (shutdown) {
            return;
        }

        Duration waitTime = Duration.between(Instant.now(), refreshTime);
        log.debug(() -> "Scheduling refresh attempt for " + refreshTime + " (in " + waitTime.toMillis() + " ms)");
        updateTask(SCHEDULER.schedule(() -> {
            Thread.currentThread().setName(asyncThreadName + "-scheduler");
            log.debug(() -> "Executing refresh attempt scheduled for " + refreshTime);

            // If the supplier has already been prefetched, this will just be a cache hit.
            tryRunBackgroundTask(cachedSupplier::get);
        }, waitTime.toMillis(), TimeUnit.MILLISECONDS));

        if (shutdown) {
            updateTask(null);
        }
    }

    @Override
    public void close() {
        shutdown = true;
        updateTask(null);
    }

    public void updateTask(ScheduledFuture<?> newTask) {
        ScheduledFuture<?> currentTask;
        do {
            currentTask = refreshTask.get();
            if (currentTask != null && !currentTask.isDone()) {
                currentTask.cancel(false);
            }
        } while (!refreshTask.compareAndSet(currentTask, newTask));
    }

    public void tryRunBackgroundTask(Runnable runnable) {
        tryRunBackgroundTask(runnable, () -> {
        });
    }

    public void tryRunBackgroundTask(Runnable runnable, Runnable finallyRunnable) {
        try {
            if (EXECUTOR.getActiveCount() > MAX_CONCURRENT_REFRESHES) {
                log.warn(() -> "Skipping a background refresh task because there are too many other tasks running.");
                return;
            }

            EXECUTOR.submit(() -> {
                try {
                    Thread.currentThread().setName(asyncThreadName);
                    runnable.run();
                } catch (Throwable t) {
                    log.warn(() -> "Exception occurred in AWS SDK background task.", t);
                } finally {
                    finallyRunnable.run();
                }
            });
        } catch (Throwable t) {
            log.warn(() -> "Exception occurred when submitting AWS SDK background task.", t);
        } finally {
            finallyRunnable.run();
        }
    }
}
