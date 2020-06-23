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

package software.amazon.awssdk.http.nio.netty.internal.utils;

import io.netty.channel.EventLoop;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.SucceededFuture;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Logger;

@SdkInternalApi
public final class NettyUtils {
    /**
     * Completed succeed future.
     */
    public static final SucceededFuture<?> SUCCEEDED_FUTURE = new SucceededFuture<>(null, null);

    private static final Logger log = Logger.loggerFor(NettyUtils.class);

    private NettyUtils() {
    }

    /**
     * Creates a {@link BiConsumer} that notifies the promise of any failures either via the {@link Throwable} passed into the
     * BiConsumer of as a result of running the successFunction.
     *
     * @param successFunction Function called to process the successful result and map it into the result to notify the promise
     * with.
     * @param promise Promise to notify of success or failure.
     * @param <SuccessT> Success type.
     * @param <PromiseT> Type being fulfilled by the promise.
     * @return BiConsumer that can be used in a {@link CompletableFuture#whenComplete(BiConsumer)} method.
     */
    public static <SuccessT, PromiseT> BiConsumer<SuccessT, ? super Throwable> promiseNotifyingBiConsumer(
        Function<SuccessT, PromiseT> successFunction, Promise<PromiseT> promise) {
        return (success, fail) -> {
            if (fail != null) {
                promise.setFailure(fail);
            } else {
                try {
                    promise.setSuccess(successFunction.apply(success));
                } catch (Throwable e) {
                    promise.setFailure(e);
                }
            }
        };
    }

    /**
     * Creates a {@link BiConsumer} that notifies the promise of any failures either via the throwable passed into the BiConsumer
     * or as a result of running the successConsumer. This assumes that the successConsumer will notify the promise when it
     * completes successfully.
     *
     * @param successConsumer BiConsumer to call if the result is successful. Promise is also passed and must be notified on
     * success.
     * @param promise Promise to notify.
     * @param <SuccessT> Success type.
     * @param <PromiseT> Type being fulfilled by the Promise.
     * @return BiConsumer that can be used in a {@link CompletableFuture#whenComplete(BiConsumer)} method.
     */
    public static <SuccessT, PromiseT> BiConsumer<SuccessT, ? super Throwable> asyncPromiseNotifyingBiConsumer(
        BiConsumer<SuccessT, Promise<PromiseT>> successConsumer, Promise<PromiseT> promise) {
        return (success, fail) -> {
            if (fail != null) {
                promise.setFailure(fail);
            } else {
                try {
                    successConsumer.accept(success, promise);
                } catch (Throwable e) {
                    // If the successConsumer fails synchronously then we can notify the promise. If it fails asynchronously
                    // it's up to the successConsumer to notify.
                    promise.setFailure(e);
                }
            }
        };
    }

    /**
     * Create a {@link GenericFutureListener} that will notify the provided {@link Promise} on success and failure.
     *
     * @param channelPromise Promise to notify.
     * @return GenericFutureListener
     */
    public static <T> GenericFutureListener<Future<T>> promiseNotifyingListener(Promise<T> channelPromise) {
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

    /**
     * Runs a task in the given {@link EventExecutor}. Runs immediately if the current thread is in the
     * eventExecutor. Notifies the given {@link Promise} if a failure occurs.
     *
     * @param eventExecutor Executor to run task in.
     * @param runnable Task to run.
     * @param promise Promise to notify if failure occurs.
     */
    public static void doInEventLoop(EventExecutor eventExecutor, Runnable runnable, Promise<?> promise) {
        try {
            if (eventExecutor.inEventLoop()) {
                runnable.run();
            } else {
                eventExecutor.submit(() -> {
                    try {
                        runnable.run();
                    } catch (Throwable e) {
                        promise.setFailure(e);
                    }
                });
            }
        } catch (Throwable e) {
            promise.setFailure(e);
        }
    }

    public static void warnIfNotInEventLoop(EventLoop loop) {
        assert loop.inEventLoop();
        if (!loop.inEventLoop()) {
            Exception exception =
                new IllegalStateException("Execution is not in the expected event loop. Please report this issue to the "
                                          + "AWS SDK for Java team on GitHub, because it could result in race conditions.");
            log.warn(() -> "Execution is happening outside of the expected event loop.", exception);
        }
    }

    /**
     * @return an {@code AttributeKey} for {@code attr}. This returns an existing instance if it was previously created.
     */
    public static <T> AttributeKey<T> getOrCreateAttributeKey(String attr) {
        if (AttributeKey.exists(attr)) {
            return AttributeKey.valueOf(attr);
        }
        //CHECKSTYLE:OFF - This is the only place allowed to call AttributeKey.newInstance()
        return AttributeKey.newInstance(attr);
        //CHECKSTYLE:ON
    }
}
