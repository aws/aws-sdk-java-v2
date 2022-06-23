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

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;
import software.amazon.awssdk.utils.Validate;

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
     * Whether we are currently refreshing the supplier. This is used to make sure only one caller is blocking at a time.
     */
    private final AtomicBoolean currentlyRefreshing = new AtomicBoolean(false);

    /**
     * How frequently to automatically refresh the supplier in the background.
     */
    private final Duration asyncRefreshFrequency;

    /**
     * Single threaded executor to asynchronous refresh the value.
     */
    private final ScheduledExecutorService executor;

    /**
     * Create a non-blocking prefetch strategy that uses the provided value for the name of the background thread that will be
     * performing the update.
     */
    public NonBlocking(String asyncThreadName) {
        this(asyncThreadName, Duration.ofMinutes(1));
    }

    @SdkTestInternalApi
    NonBlocking(String asyncThreadName, Duration asyncRefreshFrequency) {
        this.executor = newExecutor(asyncThreadName);
        this.asyncRefreshFrequency = asyncRefreshFrequency;
    }

    private static ScheduledExecutorService newExecutor(String asyncThreadName) {
        Validate.paramNotBlank(asyncThreadName, "asyncThreadName");
        return new ScheduledThreadPoolExecutor(1, new ThreadFactoryBuilder().daemonThreads(true)
                                                                            .threadNamePrefix(asyncThreadName)
                                                                            .build());
    }

    @Override
    public void initializeCachedSupplier(CachedSupplier<?> cachedSupplier) {
        scheduleRefresh(cachedSupplier);
    }

    private void scheduleRefresh(CachedSupplier<?> cachedSupplier) {
        executor.schedule(() -> {
            try {
                cachedSupplier.get();
            } finally {
                scheduleRefresh(cachedSupplier);
            }
        }, asyncRefreshFrequency.toMillis(), MILLISECONDS);
    }

    @Override
    public void prefetch(Runnable valueUpdater) {
        // Only run one async refresh at a time.
        if (currentlyRefreshing.compareAndSet(false, true)) {
            try {
                executor.submit(() -> {
                    try {
                        valueUpdater.run();
                    } catch (RuntimeException e) {
                        log.warn(() -> "Exception occurred in AWS SDK background task.", e);
                    } finally {
                        currentlyRefreshing.set(false);
                    }
                });
            } catch (Throwable e) {
                currentlyRefreshing.set(false);
                throw e;
            }
        }
    }

    @Override
    public void close() {
        executor.shutdown();
    }
}
