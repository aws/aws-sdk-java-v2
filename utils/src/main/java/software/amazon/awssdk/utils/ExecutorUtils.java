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

package software.amazon.awssdk.utils;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Utilities that make it easier to create, use and destroy {@link ExecutorService}s.
 */
@SdkProtectedApi
public final class ExecutorUtils {
    private ExecutorUtils() {
    }

    /**
     * Create a bounded-queue executor with one thread for performing background tasks. The thread in the service is marked as a
     * daemon thread.
     */
    public static ExecutorService newSingleDaemonThreadExecutor(int queueCapacity, String threadNameFormat) {
        return new ThreadPoolExecutor(0, 1, 5, SECONDS,
                                      new LinkedBlockingQueue<>(queueCapacity),
                                      new ThreadFactoryBuilder().daemonThreads(true).threadNamePrefix(threadNameFormat).build());
    }

    /**
     * Wrap an executor in a type that cannot be closed, or shut down.
     */
    public static Executor unmanagedExecutor(Executor executor) {
        return new UnmanagedExecutor(executor);
    }

    private static class UnmanagedExecutor implements Executor {
        private final Executor executor;

        private UnmanagedExecutor(Executor executor) {
            this.executor = executor;
        }

        @Override
        public void execute(Runnable command) {
            executor.execute(command);
        }
    }
}
