/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities that make it easier to create, use and destroy {@link ExecutorService}s.
 */
public final class ExecutorUtils {
    private static Logger LOG = LoggerFactory.getLogger(ExecutorUtils.class);

    private ExecutorUtils() {}

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
     * Null-safely shut down the provided executor service.
     */
    public static void shutdown(ExecutorService executorService) {
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    /**
     * Null-safely shut down the provided executor service, waiting up to the provided max wait duration for it to complete.
     *
     * The first half of the max duration is used waiting for the threads in the executor service to finish draining the backlog
     * of tasks in the queue. If half of the max duration passes without the jobs finishing, the threads in the service are
     * interrupted, and the last half of the duration is spent waiting for the interrupted threads to terminate.
     *
     * If the threads still do not shut down after the maximum wait duration, a warning is logged.
     */
    public static void shutdownAndAwaitTermination(ExecutorService executorService, Duration maxWaitDuration) {
        if (executorService == null) {
            return;
        }

        long halfMaxWaitDurationInMillis = maxWaitDuration.toMillis() / 2;

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(halfMaxWaitDurationInMillis, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();

                if (!executorService.awaitTermination(halfMaxWaitDurationInMillis, TimeUnit.MILLISECONDS)) {
                    LOG.warn("Executor service did not shut down after {} ms.", maxWaitDuration.toMillis());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("Interrupted while waiting for the executor service to shut down.");
        }
    }
}
