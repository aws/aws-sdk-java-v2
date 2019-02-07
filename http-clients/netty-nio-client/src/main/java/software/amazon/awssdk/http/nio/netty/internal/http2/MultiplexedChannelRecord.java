/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http.nio.netty.internal.http2;

import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.CHANNEL_POOL_RECORD;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.PROTOCOL_FUTURE;
import static software.amazon.awssdk.http.nio.netty.internal.utils.NettyUtils.asyncPromiseNotifyingBiConsumer;
import static software.amazon.awssdk.http.nio.netty.internal.utils.NettyUtils.doInEventLoop;
import static software.amazon.awssdk.http.nio.netty.internal.utils.NettyUtils.promiseNotifyingListener;
import static software.amazon.awssdk.utils.NumericUtils.saturatedCast;

import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import io.netty.handler.codec.http2.ForkedHttp2StreamChannelBootstrap;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.http.Protocol;

/**
 * Contains a {@link Future} for the actual socket channel and tracks available
 * streams based on the MAX_CONCURRENT_STREAMS setting for the connection.
 */
@SdkInternalApi
public final class MultiplexedChannelRecord {

    private final Future<Channel> connectionFuture;
    private final Map<ChannelId, Channel> childChannels;
    private final AtomicLong availableStreams;
    private final BiConsumer<Channel, MultiplexedChannelRecord> channelReleaser;

    private volatile Channel connection;

    /**
     * @param connectionFuture Future for parent socket channel.
     * @param maxConcurrencyPerConnection Max streams allowed per connection.
     * @param channelReleaser Method to release a channel and record on failure.
     */
    MultiplexedChannelRecord(Future<Channel> connectionFuture,
                             long maxConcurrencyPerConnection,
                             BiConsumer<Channel, MultiplexedChannelRecord> channelReleaser) {
        this.connectionFuture = connectionFuture;
        this.availableStreams = new AtomicLong(maxConcurrencyPerConnection);
        this.childChannels = new ConcurrentHashMap<>(saturatedCast(maxConcurrencyPerConnection));
        this.channelReleaser = channelReleaser;
    }

    @SdkTestInternalApi
    MultiplexedChannelRecord(Future<Channel> connectionFuture,
                             Channel connection,
                             long maxConcurrencyPerConnection,
                             BiConsumer<Channel, MultiplexedChannelRecord> channelReleaser) {
        this.connectionFuture = connectionFuture;
        this.childChannels = new ConcurrentHashMap<>(saturatedCast(maxConcurrencyPerConnection));
        this.availableStreams = new AtomicLong(maxConcurrencyPerConnection);
        this.channelReleaser = channelReleaser;
        this.connection = connection;
    }

    MultiplexedChannelRecord acquire(Promise<Channel> channelPromise) {
        availableStreams.decrementAndGet();
        if (connection != null) {
            createChildChannel(channelPromise, connection);
        } else {
            connectionFuture.addListener((GenericFutureListener<Future<Channel>>) future -> {
                if (future.isSuccess()) {
                    connection = future.getNow();
                    connection.attr(CHANNEL_POOL_RECORD).set(this);
                    createChildChannel(channelPromise, connection);
                } else {
                    channelPromise.setFailure(future.cause());
                    channelReleaser.accept(connection, this);
                }
            });
        }
        return this;
    }

    /**
     * Delivers the exception to all registered child channels.
     *
     * @param t Exception to deliver.
     */
    public void shutdownChildChannels(Throwable t) {
        for (Channel childChannel : childChannels.values()) {
            childChannel.pipeline().fireExceptionCaught(t);
        }
    }

    /**
     * Bootstraps a child stream channel from the parent socket channel. Done in parent channel event loop.
     *
     * @param channelPromise Promise to notify when channel is available.
     * @param parentChannel Parent socket channel.
     */
    private void createChildChannel(Promise<Channel> channelPromise, Channel parentChannel) {
        doInEventLoop(parentChannel.eventLoop(),
            () -> createChildChannel0(channelPromise, parentChannel),
                      channelPromise);
    }

    private void createChildChannel0(Promise<Channel> channelPromise, Channel parentChannel) {
        // Once protocol future is notified then parent pipeline is configured and ready to go
        parentChannel.attr(PROTOCOL_FUTURE).get()
                     .whenComplete(asyncPromiseNotifyingBiConsumer(bootstrapChildChannel(parentChannel), channelPromise));
    }

    /**
     * Bootstraps the child stream channel and notifies the Promise on success or failure.
     *
     * @param parentChannel Parent socket channel.
     * @return BiConsumer that will bootstrap the child channel.
     */
    private BiConsumer<Protocol, Promise<Channel>> bootstrapChildChannel(Channel parentChannel) {
        return (s, p) -> new ForkedHttp2StreamChannelBootstrap(parentChannel)
            .open()
            .addListener((GenericFutureListener<Future<Http2StreamChannel>>) future -> {
                if (future.isSuccess()) {
                    Http2StreamChannel channel = future.getNow();
                    childChannels.put(channel.id(), channel);
                } else {
                    if (!connection.isActive()) {
                        channelReleaser.accept(connection, this);
                    }
                    availableStreams.incrementAndGet();
                }
            })
            .addListener(promiseNotifyingListener(p));
    }

    void release(Channel channel) {
        availableStreams.incrementAndGet();
        childChannels.remove(channel.id());
    }

    public Future<Channel> getConnectionFuture() {
        return connectionFuture;
    }

    long availableStreams() {
        return availableStreams.get();
    }

}
