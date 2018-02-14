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
package software.amazon.awssdk.http.nio.netty.h2;

import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelPool;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.ThrowableUtil;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * {@link ChannelPool} implementation that takes another {@link ChannelPool} implementation and enforce a maximum
 * number of concurrent connections.
 */
public class BetterFixedChannelPool implements ChannelPool {
    private static final IllegalStateException FULL_EXCEPTION = ThrowableUtil.unknownStackTrace(
        new IllegalStateException("Too many outstanding acquire operations"),
        BetterFixedChannelPool.class, "acquire0(...)");
    private static final TimeoutException TIMEOUT_EXCEPTION = ThrowableUtil.unknownStackTrace(
        new TimeoutException("Acquire operation took longer then configured maximum time"),
        BetterFixedChannelPool.class, "<init>(...)");
    static final IllegalStateException POOL_CLOSED_ON_RELEASE_EXCEPTION = ThrowableUtil.unknownStackTrace(
        new IllegalStateException("BetterFixedChannelPooled was closed"),
        BetterFixedChannelPool.class, "release(...)");
    static final IllegalStateException POOL_CLOSED_ON_ACQUIRE_EXCEPTION = ThrowableUtil.unknownStackTrace(
        new IllegalStateException("BetterFixedChannelPooled was closed"),
        BetterFixedChannelPool.class, "acquire0(...)");

    public enum AcquireTimeoutAction {
        /**
         * Create a new connection when the timeout is detected.
         */
        NEW,

        /**
         * Fail the {@link Future} of the acquire call with a {@link TimeoutException}.
         */
        FAIL
    }

    private final EventExecutor executor;
    private final long acquireTimeoutNanos;
    private final Runnable timeoutTask;
    private final ChannelPool delegateChannelPool;

    // There is no need to worry about synchronization as everything that modified the queue or counts is done
    // by the above EventExecutor.
    private final Queue<AcquireTask> pendingAcquireQueue = new ArrayDeque<AcquireTask>();
    private final int maxConnections;
    private final int maxPendingAcquires;
    private int acquiredChannelCount;
    private int pendingAcquireCount;
    private boolean closed;


    private BetterFixedChannelPool(Builder builder) {
        if (builder.maxConnections < 1) {
            throw new IllegalArgumentException("maxConnections: " + builder.maxConnections + " (expected: >= 1)");
        }
        if (builder.maxPendingAcquires < 1) {
            throw new IllegalArgumentException("maxPendingAcquires: " + builder.maxPendingAcquires + " (expected: >= 1)");
        }
        this.delegateChannelPool = builder.channelPool;
        this.executor = builder.executor;
        if (builder.action == null && builder.acquireTimeoutMillis == -1) {
            timeoutTask = null;
            acquireTimeoutNanos = -1;
        } else if (builder.action == null && builder.acquireTimeoutMillis != -1) {
            throw new NullPointerException("action");
        } else if (builder.action != null && builder.acquireTimeoutMillis < 0) {
            throw new IllegalArgumentException("acquireTimeoutMillis: " + builder.acquireTimeoutMillis + " (expected: >= 0)");
        } else {
            acquireTimeoutNanos = TimeUnit.MILLISECONDS.toNanos(builder.acquireTimeoutMillis);
            switch (builder.action) {
                case FAIL:
                    timeoutTask = new TimeoutTask() {
                        @Override
                        public void onTimeout(AcquireTask task) {
                            // Fail the promise as we timed out.
                            task.promise.setFailure(TIMEOUT_EXCEPTION);
                        }
                    };
                    break;
                case NEW:
                    timeoutTask = new TimeoutTask() {
                        @Override
                        public void onTimeout(AcquireTask task) {
                            // Increment the acquire count and delegate to super to actually acquire a Channel which will
                            // create a new connection.
                            task.acquired();

                            delegateChannelPool.acquire(task.promise);
                        }
                    };
                    break;
                default:
                    throw new Error();
            }
        }
        this.maxConnections = builder.maxConnections;
        this.maxPendingAcquires = builder.maxPendingAcquires;
    }

    @Override
    public Future<Channel> acquire() {
        return acquire(new DefaultPromise<>(executor));
    }

    @Override
    public Future<Channel> acquire(final Promise<Channel> promise) {
        try {
            if (executor.inEventLoop()) {
                acquire0(promise);
            } else {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        acquire0(promise);
                    }
                });
            }
        } catch (Throwable cause) {
            promise.setFailure(cause);
        }
        return promise;
    }

    private void acquire0(final Promise<Channel> promise) {
        assert executor.inEventLoop();

        if (closed) {
            promise.setFailure(POOL_CLOSED_ON_ACQUIRE_EXCEPTION);
            return;
        }
        if (acquiredChannelCount < maxConnections) {
            assert acquiredChannelCount >= 0;

            // We need to create a new promise as we need to ensure the AcquireListener runs in the correct
            // EventLoop
            Promise<Channel> p = executor.newPromise();
            AcquireListener l = new AcquireListener(promise);
            l.acquired();
            p.addListener(l);
            delegateChannelPool.acquire(p);
        } else {
            if (pendingAcquireCount >= maxPendingAcquires) {
                promise.setFailure(FULL_EXCEPTION);
            } else {
                AcquireTask task = new AcquireTask(promise);
                if (pendingAcquireQueue.offer(task)) {
                    ++pendingAcquireCount;

                    if (timeoutTask != null) {
                        task.timeoutFuture = executor.schedule(timeoutTask, acquireTimeoutNanos, TimeUnit.NANOSECONDS);
                    }
                } else {
                    promise.setFailure(FULL_EXCEPTION);
                }
            }

            assert pendingAcquireCount > 0;
        }
    }

    @Override
    public Future<Void> release(Channel channel) {
        return release(channel, new DefaultPromise<>(executor));
    }

    @Override
    public Future<Void> release(final Channel channel, final Promise<Void> promise) {
        ObjectUtil.checkNotNull(promise, "promise");
        final Promise<Void> p = executor.newPromise();
        delegateChannelPool.release(channel, p.addListener(new FutureListener<Void>() {

            @Override
            public void operationComplete(Future<Void> future) throws Exception {
                assert executor.inEventLoop();

                if (closed) {
                    // Since the pool is closed, we have no choice but to close the channel
                    channel.close();
                    promise.setFailure(POOL_CLOSED_ON_RELEASE_EXCEPTION);
                    return;
                }

                if (future.isSuccess()) {
                    decrementAndRunTaskQueue();
                    promise.setSuccess(null);
                } else {
                    Throwable cause = future.cause();
                    // Check if the exception was not because of we passed the Channel to the wrong pool.
                    if (!(cause instanceof IllegalArgumentException)) {
                        decrementAndRunTaskQueue();
                    }
                    promise.setFailure(future.cause());
                }
            }
        }));
        return promise;
    }

    private void decrementAndRunTaskQueue() {
        --acquiredChannelCount;

        // We should never have a negative value.
        assert acquiredChannelCount >= 0;

        // Run the pending acquire tasks before notify the original promise so if the user would
        // try to acquire again from the ChannelFutureListener and the pendingAcquireCount is >=
        // maxPendingAcquires we may be able to run some pending tasks first and so allow to add
        // more.
        runTaskQueue();
    }

    private void runTaskQueue() {
        while (acquiredChannelCount < maxConnections) {
            AcquireTask task = pendingAcquireQueue.poll();
            if (task == null) {
                break;
            }

            // Cancel the timeout if one was scheduled
            ScheduledFuture<?> timeoutFuture = task.timeoutFuture;
            if (timeoutFuture != null) {
                timeoutFuture.cancel(false);
            }

            --pendingAcquireCount;
            task.acquired();

            delegateChannelPool.acquire(task.promise);
        }

        // We should never have a negative value.
        assert pendingAcquireCount >= 0;
        assert acquiredChannelCount >= 0;
    }

    // AcquireTask extends AcquireListener to reduce object creations and so GC pressure
    private final class AcquireTask extends AcquireListener {
        final Promise<Channel> promise;
        final long expireNanoTime = System.nanoTime() + acquireTimeoutNanos;
        ScheduledFuture<?> timeoutFuture;

        public AcquireTask(Promise<Channel> promise) {
            super(promise);
            // We need to create a new promise as we need to ensure the AcquireListener runs in the correct
            // EventLoop.
            this.promise = executor.<Channel>newPromise().addListener(this);
        }
    }

    private abstract class TimeoutTask implements Runnable {
        @Override
        public final void run() {
            assert executor.inEventLoop();
            long nanoTime = System.nanoTime();
            for (; ; ) {
                AcquireTask task = pendingAcquireQueue.peek();
                // Compare nanoTime as descripted in the javadocs of System.nanoTime()
                //
                // See https://docs.oracle.com/javase/7/docs/api/java/lang/System.html#nanoTime()
                // See https://github.com/netty/netty/issues/3705
                if (task == null || nanoTime - task.expireNanoTime < 0) {
                    break;
                }
                pendingAcquireQueue.remove();

                --pendingAcquireCount;
                onTimeout(task);
            }
        }

        public abstract void onTimeout(AcquireTask task);
    }

    private class AcquireListener implements FutureListener<Channel> {
        private final Promise<Channel> originalPromise;
        protected boolean acquired;

        AcquireListener(Promise<Channel> originalPromise) {
            this.originalPromise = originalPromise;
        }

        @Override
        public void operationComplete(Future<Channel> future) throws Exception {
            assert executor.inEventLoop();

            if (closed) {
                if (future.isSuccess()) {
                    // Since the pool is closed, we have no choice but to close the channel
                    future.getNow().close();
                }
                originalPromise.setFailure(POOL_CLOSED_ON_ACQUIRE_EXCEPTION);
                return;
            }

            if (future.isSuccess()) {
                originalPromise.setSuccess(future.getNow());
            } else {
                if (acquired) {
                    decrementAndRunTaskQueue();
                } else {
                    runTaskQueue();
                }

                originalPromise.setFailure(future.cause());
            }
        }

        public void acquired() {
            if (acquired) {
                return;
            }
            acquiredChannelCount++;
            acquired = true;
        }
    }

    @Override
    public void close() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (!closed) {
                    closed = true;
                    for (; ; ) {
                        AcquireTask task = pendingAcquireQueue.poll();
                        if (task == null) {
                            break;
                        }
                        ScheduledFuture<?> f = task.timeoutFuture;
                        if (f != null) {
                            f.cancel(false);
                        }
                        task.promise.setFailure(new ClosedChannelException());
                    }
                    acquiredChannelCount = 0;
                    pendingAcquireCount = 0;
                    delegateChannelPool.close();
                }
            }
        });
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private ChannelPool channelPool;
        private EventExecutor executor;
        private AcquireTimeoutAction action;
        private long acquireTimeoutMillis;
        private int maxConnections;
        private int maxPendingAcquires;

        private Builder() {
        }

        public Builder channelPool(ChannelPool channelPool) {
            this.channelPool = channelPool;
            return this;
        }

        public Builder executor(EventExecutor executor) {
            this.executor = executor;
            return this;
        }

        public Builder acquireTimeoutAction(AcquireTimeoutAction action) {
            this.action = action;
            return this;
        }

        public Builder acquireTimeoutMillis(long acquireTimeoutMillis) {
            this.acquireTimeoutMillis = acquireTimeoutMillis;
            return this;
        }

        public Builder maxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
            return this;
        }

        public Builder maxPendingAcquires(int maxPendingAcquires) {
            this.maxPendingAcquires = maxPendingAcquires;
            return this;
        }

        public BetterFixedChannelPool build() {
            return new BetterFixedChannelPool(this);
        }
    }
}
