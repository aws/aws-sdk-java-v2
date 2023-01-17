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

import static software.amazon.awssdk.http.nio.netty.internal.utils.NettyUtils.consumeOrPropagate;

import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.nio.netty.internal.http2.HttpOrHttp2ChannelPool;
import software.amazon.awssdk.http.nio.netty.internal.utils.NettyUtils;
import software.amazon.awssdk.metrics.MetricCollector;

/**
 * A {@link SdkChannelPool} that wraps and delegates to another {@link SdkChannelPool} while invoking {@link ChannelPoolListener}s
 * for important events that occur.
 * <p>
 * {@link ChannelPoolListener} is similar to Netty's {@link ChannelPoolHandler} interface, but by invoking them as part of a
 * {@link SdkChannelPool} wrapper, we are given more control over when they are invoked. For example, {@link
 * HttpOrHttp2ChannelPool} may choose not to release HTTP/2 stream channels to the lowest-level pool (and instead store the
 * channels in its own pool), but by instrumenting listeners that sit on top of this layer, we are still given visibility into
 * these events occurring.
 */
@SdkInternalApi
public final class ListenerInvokingChannelPool implements SdkChannelPool {
    private final SdkChannelPool delegatePool;
    private final Supplier<Promise<Channel>> promiseFactory;
    private final List<ChannelPoolListener> listeners;

    /**
     * Listener which is called for various actions performed on a {@link SdkChannelPool}. All listener events are guaranteed to
     * be invoked as part of the {@link Channel}'s {@link EventLoop}.
     */
    @SdkInternalApi
    public interface ChannelPoolListener {

        /**
         * Called <b>after</b> a {@link Channel} was acquired by calling {@link SdkChannelPool#acquire()} or {@link
         * SdkChannelPool#acquire(Promise)}.
         * <p>
         * This method will be called by the {@link EventLoop} of the {@link Channel}.
         */
        default void channelAcquired(Channel channel) {
        }

        /**
         * Called <b>before</b> a {@link Channel} is released by calling {@link SdkChannelPool#release(Channel)} or {@link
         * SdkChannelPool#release(Channel, Promise)}.
         * <p>
         * This method will be called by the {@link EventLoop} of the {@link Channel}.
         */
        default void channelReleased(Channel channel) {
        }
    }

    public ListenerInvokingChannelPool(EventLoopGroup eventLoopGroup,
                                       SdkChannelPool delegatePool,
                                       List<ChannelPoolListener> listeners) {
        this(() -> eventLoopGroup.next().newPromise(), delegatePool, listeners);
    }

    public ListenerInvokingChannelPool(Supplier<Promise<Channel>> promiseFactory,
                                       SdkChannelPool delegatePool,
                                       List<ChannelPoolListener> listeners) {
        this.delegatePool = delegatePool;
        this.promiseFactory = promiseFactory;
        this.listeners = listeners;
    }

    @Override
    public Future<Channel> acquire() {
        return acquire(promiseFactory.get());
    }

    @Override
    public Future<Channel> acquire(Promise<Channel> returnFuture) {
        delegatePool.acquire(promiseFactory.get())
                    .addListener(consumeOrPropagate(returnFuture, channel -> {
                        NettyUtils.doInEventLoop(channel.eventLoop(), () -> {
                            invokeChannelAcquired(channel);
                            returnFuture.trySuccess(channel);
                        }, returnFuture);
                    }));
        return returnFuture;
    }

    private void invokeChannelAcquired(Channel channel) {
        listeners.forEach(listener -> listener.channelAcquired(channel));
    }

    @Override
    public Future<Void> release(Channel channel) {
        return release(channel, channel.eventLoop().newPromise());
    }

    @Override
    public Future<Void> release(Channel channel, Promise<Void> returnFuture) {
        NettyUtils.doInEventLoop(channel.eventLoop(), () -> {
            invokeChannelReleased(channel);
            delegatePool.release(channel, returnFuture);
        }, returnFuture);
        return returnFuture;
    }

    private void invokeChannelReleased(Channel channel) {
        listeners.forEach(listener -> listener.channelReleased(channel));
    }


    @Override
    public void close() {
        delegatePool.close();
    }

    @Override
    public CompletableFuture<Void> collectChannelPoolMetrics(MetricCollector metrics) {
        return delegatePool.collectChannelPoolMetrics(metrics);
    }
}
