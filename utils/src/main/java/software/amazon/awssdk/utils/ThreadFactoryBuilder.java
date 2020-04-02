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

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;

/**
 * A builder for creating a thread factory. This allows changing the behavior of the created thread factory.
 */
@SdkProtectedApi
public class ThreadFactoryBuilder {

    /**
     * To prevent thread names from becoming too long we limit to 4 digits for the pool number before wrapping back
     * around.
     */
    private static final int POOL_NUMBER_MAX = 10_000;

    private static final AtomicLong POOL_NUMBER = new AtomicLong(0);

    private String threadNamePrefix = "aws-java-sdk";
    private Boolean daemonThreads = true;

    /**
     * The name prefix for threads created by this thread factory. The prefix will be appended with a number unique to the thread
     * factory and a number unique to the thread.
     *
     * For example, "aws-java-sdk-thread" could become "aws-java-sdk-thread-3-4".
     *
     * By default, this is "aws-java-sdk-thread".
     */
    public ThreadFactoryBuilder threadNamePrefix(String threadNamePrefix) {
        this.threadNamePrefix = threadNamePrefix;
        return this;
    }

    /**
     * Whether the threads created by the factory should be daemon threads. By default this is true - we shouldn't be holding up
     * the customer's JVM shutdown unless we're absolutely sure we want to.
     */
    public ThreadFactoryBuilder daemonThreads(Boolean daemonThreads) {
        this.daemonThreads = daemonThreads;
        return this;
    }

    /**
     * Test API to reset pool count for reliable assertions.
     */
    @SdkTestInternalApi
    static void resetPoolNumber() {
        POOL_NUMBER.set(0);
    }

    /**
     * Create the {@link ThreadFactory} with the configuration currently applied to this builder.
     */
    public ThreadFactory build() {
        String threadNamePrefixWithPoolNumber = threadNamePrefix + "-" + POOL_NUMBER.getAndIncrement() % POOL_NUMBER_MAX;

        ThreadFactory result = new NamedThreadFactory(Executors.defaultThreadFactory(), threadNamePrefixWithPoolNumber);

        if (daemonThreads) {
            result = new DaemonThreadFactory(result);
        }

        return result;
    }
}
