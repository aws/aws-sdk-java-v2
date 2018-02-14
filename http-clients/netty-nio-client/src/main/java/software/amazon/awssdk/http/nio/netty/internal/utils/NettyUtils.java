/*
 * Copyright 2012-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package software.amazon.awssdk.http.nio.netty.internal.utils;

import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;

public class NettyUtils {

    /**
     * Create a {@link GenericFutureListener} that will notify the provided {@link Promise} on success and failure.
     *
     * @param channelPromise Promise to notify.
     * @return GenericFutureListener
     */
    public static <T> GenericFutureListener<Future<T>> createPromiseNotifyingListener(Promise<T> channelPromise) {
        return future -> {
            if (future.isSuccess()) {
                channelPromise.setSuccess(future.getNow());
            } else {
                channelPromise.setFailure(future.cause());
            }
        };
    }

    /**
     * Runs a task in the given {@link EventExecutor}. Runs immediately if the current thread is in the
     * eventExecutor.
     *
     * @param eventExecutor Executor to run task in.
     * @param runnable Task to run.
     */
    public static void doInEventLoop(EventExecutor eventExecutor, Runnable runnable) {
        if (eventExecutor.inEventLoop()) {
            runnable.run();
        } else {
            eventExecutor.submit(runnable);
        }
    }
}
