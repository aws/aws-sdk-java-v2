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

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
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
     * The semaphore around concurrent background refreshes, enforcing the {@link #MAX_CONCURRENT_REFRESHES}.
     */
    private static final Semaphore CONCURRENT_REFRESH_LEASES = new Semaphore(MAX_CONCURRENT_REFRESHES);

    /**
     * Thread used to kick off refreshes during the prefetch window. This does not do the actual refreshing. That's left for
     * the {@link #EXECUTOR}.
     */
    private static final ScheduledThreadPoolExecutor SCHEDULER =
        new ScheduledThreadPoolExecutor(1, new ThreadFactoryBuilder().threadNamePrefix("sdk-cache-scheduler")
                                                                     .daemonThreads(true)
                                                                     .build());

    /**
     * Threads used to do the actual work of refreshing the values (because the cached supplier might block, so we don't
     * want the work to be done by a small thread pool). This executor is created as unbounded, but we start complaining and
     * skipping refreshes when there are more than {@link #MAX_CONCURRENT_REFRESHES} running.
     */
    private static final ThreadPoolExecutor EXECUTOR =
        new ThreadPoolExecutor(1, Integer.MAX_VALUE,
                               60L, TimeUnit.SECONDS,
                               new SynchronousQueue<>(),
                               new ThreadFactoryBuilder().threadNamePrefix("sdk-cache")
                                                         .daemonThreads(true)
                                                         .build());

    /**
     * An incrementing number, used to uniquely identify an instance of NonBlocking in the {@link #asyncThreadName}.
     */
    private static final AtomicLong INSTANCE_NUMBER = new AtomicLong(0);

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
        this.asyncThreadName = asyncThreadName + "-" + INSTANCE_NUMBER.getAndIncrement();
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
        schedulePrefetch(result);
        return result;
    }

    private void schedulePrefetch(RefreshResult<?> result) {
        if (shutdown || result.staleTime() == null || result.prefetchTime() == null) {
            return;
        }

        Duration timeUntilPrefetch = Duration.between(Instant.now(), result.prefetchTime());
        if (timeUntilPrefetch.isNegative() || timeUntilPrefetch.toDays() > 7) {
            log.debug(() -> "Skipping background refresh because the prefetch time is in the past or too far in the future: " +
                            result.prefetchTime());
            return;
        }

        Instant backgroundRefreshTime = result.prefetchTime().plusSeconds(1);
        Duration timeUntilBackgroundRefresh = timeUntilPrefetch.plusSeconds(1);

        log.debug(() -> "Scheduling refresh attempt for " + backgroundRefreshTime + " (in " +
                        timeUntilBackgroundRefresh.toMillis() + " ms)");

        ScheduledFuture<?> scheduledTask = SCHEDULER.schedule(() -> {
            runWithInstanceThreadName(() -> {
                log.debug(() -> "Executing refresh attempt scheduled for " + backgroundRefreshTime);

                // If the supplier has already been prefetched, this will just be a cache hit.
                tryRunBackgroundTask(cachedSupplier::get);
            });
        }, timeUntilBackgroundRefresh.toMillis(), TimeUnit.MILLISECONDS);

        updateTask(scheduledTask);

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

    public void tryRunBackgroundTask(Runnable runnable, Runnable runOnCompletion) {
        if (!CONCURRENT_REFRESH_LEASES.tryAcquire()) {
            log.warn(() -> "Skipping a background refresh task because there are too many other tasks running.");
            runOnCompletion.run();
            return;
        }

        try {
            EXECUTOR.submit(() -> {
                runWithInstanceThreadName(() -> {
                    try {
                        runnable.run();
                    } catch (Throwable t) {
                        log.warn(() -> "Exception occurred in AWS SDK background task.", t);
                    } finally {
                        CONCURRENT_REFRESH_LEASES.release();
                        runOnCompletion.run();
                    }
                });
            });
        } catch (Throwable t) {
            log.warn(() -> "Exception occurred when submitting AWS SDK background task.", t);
            CONCURRENT_REFRESH_LEASES.release();
            runOnCompletion.run();
        }
    }

    public void runWithInstanceThreadName(Runnable runnable) {
        String baseThreadName = Thread.currentThread().getName();
        try {
            Thread.currentThread().setName(baseThreadName + "-" + asyncThreadName);
            runnable.run();
        } finally {
            Thread.currentThread().setName(baseThreadName);
        }
    }
}
