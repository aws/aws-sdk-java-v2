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

package software.amazon.awssdk.http.nio.netty.internal;

import static software.amazon.awssdk.http.nio.netty.internal.utils.NettyUtils.doInEventLoop;

import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelPool;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.HttpMetric;
import software.amazon.awssdk.http.nio.netty.internal.utils.NettyUtils;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.utils.Logger;

/**
 * A channel pool implementation that tracks the number of "idle" channels in an underlying channel pool.
 *
 * <p>Specifically, this pool counts the number of channels acquired and then released from/to the underlying channel pool. It
 * will monitor for the underlying channels to be closed, and will remove them from the "idle" count.
 */
@SdkInternalApi
public class IdleConnectionCountingChannelPool implements SdkChannelPool {
    private static final Logger log = Logger.loggerFor(IdleConnectionCountingChannelPool.class);

    /**
     * The idle channel state for a specific channel. This should only be accessed from the {@link #executor}.
     */
    private static final AttributeKey<ChannelIdleState> CHANNEL_STATE =
        NettyUtils.getOrCreateAttributeKey("IdleConnectionCountingChannelPool.CHANNEL_STATE");

    /**
     * The executor in which all updates to {@link #idleConnections} is performed.
     */
    private final EventExecutor executor;

    /**
     * The delegate pool to which all acquire and release calls are delegated.
     */
    private final ChannelPool delegatePool;

    /**
     * The number of idle connections in the underlying channel pool. This value is only valid if accessed from the
     * {@link #executor}.
     */
    private int idleConnections = 0;

    public IdleConnectionCountingChannelPool(EventExecutor executor, ChannelPool delegatePool) {
        this.executor = executor;
        this.delegatePool = delegatePool;
    }

    @Override
    public Future<Channel> acquire() {
        return acquire(executor.newPromise());
    }

    @Override
    public Future<Channel> acquire(Promise<Channel> promise) {
        Future<Channel> acquirePromise = delegatePool.acquire(executor.newPromise());
        acquirePromise.addListener(f -> {
            Throwable failure = acquirePromise.cause();
            if (failure != null) {
                promise.setFailure(failure);
            } else {
                Channel channel = acquirePromise.getNow();
                channelAcquired(channel);
                promise.setSuccess(channel);
            }
        });

        return promise;
    }

    @Override
    public Future<Void> release(Channel channel) {
        channelReleased(channel);
        return delegatePool.release(channel);
    }

    @Override
    public Future<Void> release(Channel channel, Promise<Void> promise) {
        channelReleased(channel);
        return delegatePool.release(channel, promise);
    }

    @Override
    public void close() {
        delegatePool.close();
    }

    @Override
    public CompletableFuture<Void> collectChannelPoolMetrics(MetricCollector metrics) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        doInEventLoop(executor, () -> {
            metrics.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, idleConnections);
            result.complete(null);
        });
        return result;
    }

    /**
     * Add a listener to the provided channel that will update the idle channel count when the channel is closed.
     */
    private void addUpdateIdleCountOnCloseListener(Channel channel) {
        channel.closeFuture().addListener(f -> channelClosed(channel));
    }

    /**
     * Invoked when a channel is acquired, marking it non-idle until it's closed or released.
     */
    private void channelAcquired(Channel channel) {
        doInEventLoop(executor, () -> {
            ChannelIdleState channelIdleState = getChannelIdleState(channel);

            if (channelIdleState == null) {
                addUpdateIdleCountOnCloseListener(channel);
                setChannelIdleState(channel, ChannelIdleState.NOT_IDLE);
            } else {
                switch (channelIdleState) {
                    case IDLE:
                        decrementIdleConnections();
                        setChannelIdleState(channel, ChannelIdleState.NOT_IDLE);
                        break;
                    case CLOSED:
                        break;
                    case NOT_IDLE:
                    default:
                        log.warn(() -> "Failed to update idle connection count metric on acquire, because the channel (" +
                                       channel + ") was in an unexpected state: " + channelIdleState);
                }
            }
        });
    }

    /**
     * Invoked when a channel is released, marking it idle until it's acquired.
     */
    private void channelReleased(Channel channel) {
        doInEventLoop(executor, () -> {
            ChannelIdleState channelIdleState = getChannelIdleState(channel);

            if (channelIdleState == null) {
                log.warn(() -> "Failed to update idle connection count metric on release, because the channel (" + channel +
                               ") was in an unexpected state: null");
            } else {
                switch (channelIdleState) {
                    case NOT_IDLE:
                        incrementIdleConnections();
                        setChannelIdleState(channel, ChannelIdleState.IDLE);
                        break;
                    case CLOSED:
                        break;
                    case IDLE:
                    default:
                        log.warn(() -> "Failed to update idle connection count metric on release, because the channel (" +
                                       channel + ") was in an unexpected state: " + channelIdleState);
                }
            }
        });
    }

    /**
     * Invoked when a channel is closed, ensure it is marked as non-idle.
     */
    private void channelClosed(Channel channel) {
        doInEventLoop(executor, () -> {
            ChannelIdleState channelIdleState = getChannelIdleState(channel);
            setChannelIdleState(channel, ChannelIdleState.CLOSED);

            if (channelIdleState != null) {
                switch (channelIdleState) {
                    case IDLE:
                        decrementIdleConnections();
                        break;
                    case NOT_IDLE:
                        break;
                    default:
                        log.warn(() -> "Failed to update idle connection count metric on close, because the channel (" + channel +
                                       ") was in an unexpected state: " + channelIdleState);
                }
            }
        });
    }

    private ChannelIdleState getChannelIdleState(Channel channel) {
        return channel.attr(CHANNEL_STATE).get();
    }

    private void setChannelIdleState(Channel channel, ChannelIdleState newState) {
        channel.attr(CHANNEL_STATE).set(newState);
    }

    /**
     * Decrement the idle connection count. This must be invoked from the {@link #executor}.
     */
    private void decrementIdleConnections() {
        --idleConnections;
        log.trace(() -> "Idle connection count decremented, now " + idleConnections);
    }

    /**
     * Increment the idle connection count. This must be invoked from the {@link #executor}.
     */
    private void incrementIdleConnections() {
        ++idleConnections;
        log.trace(() -> "Idle connection count incremented, now " + idleConnections);
    }

    /**
     * The idle state of a channel.
     */
    private enum ChannelIdleState {
        IDLE,
        NOT_IDLE,
        CLOSED
    }
}
