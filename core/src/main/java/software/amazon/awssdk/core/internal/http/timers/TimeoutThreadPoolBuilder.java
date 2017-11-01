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

package software.amazon.awssdk.core.internal.http.timers;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Utility class to build the {@link ScheduledThreadPoolExecutor} for the request timeout and client
 * execution timeout features
 */
@SdkInternalApi
public final class TimeoutThreadPoolBuilder {

    private TimeoutThreadPoolBuilder() {
    }

    /**
     * Creates a {@link ScheduledThreadPoolExecutor} with custom name for the threads.
     *
     * @param name the prefix to add to the thread name in ThreadFactory.
     * @return The default thread pool for request timeout and client execution timeout features.
     */
    public static ScheduledThreadPoolExecutor buildDefaultTimeoutThreadPool(final String name) {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(5, getThreadFactory(name));
        executor.setRemoveOnCancelPolicy(true);
        executor.setKeepAliveTime(5, TimeUnit.SECONDS);
        executor.allowCoreThreadTimeOut(true);

        return executor;
    }

    private static ThreadFactory getThreadFactory(final String name) {
        return new ThreadFactory() {
            private int threadCount = 1;

            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                if (name != null) {
                    thread.setName(name + "-" + threadCount++);
                }
                thread.setPriority(Thread.MAX_PRIORITY);
                return thread;
            }
        };
    }
}
