/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import software.amazon.awssdk.utils.ExecutorUtils;

/**
 * A {@link CachedSupplier.PrefetchStrategy} that will run a single thread in the background to update the value. A call to
 * prefetch on this strategy will never return.
 *
 * Multiple calls to {@link #prefetch(Runnable)} will still only result in one background task performing the update.
 */
public class NonBlocking implements CachedSupplier.PrefetchStrategy {
    /**
     * Whether we are currently refreshing the supplier. This is used to make sure only one caller is blocking at a time.
     */
    private final AtomicBoolean currentlyRefreshing = new AtomicBoolean(false);

    /**
     * Single threaded executor to asynchronous refresh the value.
     */
    private final ExecutorService executor;

    /**
     * Create a non-blocking prefetch strategy that uses the provided value for the name of the background thread that will be
     * performing the update.
     */
    public NonBlocking(String asyncThreadName) {
        this.executor = ExecutorUtils.newSingleDaemonThreadExecutor(1, asyncThreadName);
    }

    @Override
    public void prefetch(Runnable valueUpdater) {
        // Only run one async refresh at a time.
        if (currentlyRefreshing.compareAndSet(false, true)) {
            try {
                executor.submit(() -> {
                    try {
                        valueUpdater.run();
                    } finally {
                        currentlyRefreshing.set(false);
                    }
                });
            } catch (RuntimeException e) {
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
