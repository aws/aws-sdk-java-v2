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
import io.netty.handler.codec.http2.Http2GoAwayFrame;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import java.io.IOException;
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
public class MultiplexedChannelRecord {
    private final Future<Channel> connectionFuture;
    private final Map<ChannelId, Http2StreamChannel> childChannels;
    private final AtomicLong availableStreams;
    private final BiConsumer<Channel, MultiplexedChannelRecord> channelReleaser;

    private volatile Channel connection;
    private volatile boolean goAway = false;

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
            createChildChannel(channelPromise);
        } else {
            connectionFuture.addListener((GenericFutureListener<Future<Channel>>) future -> {
                if (future.isSuccess()) {
                    connection = future.getNow();
                    connection.attr(CHANNEL_POOL_RECORD).set(this);
                    createChildChannel(channelPromise);
                } else {
                    channelPromise.setFailure(future.cause());
                    channelReleaser.accept(connection, this);
                }
            });
        }
        return this;
    }

    /**
     * Handle a {@link Http2GoAwayFrame} on this connection, preventing new streams from being created on it, and closing any
     * streams newer than the last-stream-id on the go-away frame.
     */
    public void goAway(Http2GoAwayFrame frame) {
        this.goAway = true;
        GoAwayException exception = new GoAwayException(frame.errorCode(), frame.content());
        childChannels.entrySet().stream()
                     .map(Map.Entry::getValue)
                     .filter(cc -> cc.stream().id() > frame.lastStreamId())
                     .forEach(cc -> cc.eventLoop().execute(() -> shutdownChildChannel(cc, exception)));
    }

    /**
     * Delivers the exception to all registered child channels, and prohibits new streams being created on this connection.
     *
     * @param t Exception to deliver.
     */
    public void shutdownChildChannels(Throwable t) {
        this.goAway = true;
        doInEventLoop(connection.eventLoop(), () -> {
            for (Channel childChannel : childChannels.values()) {
                shutdownChildChannel(childChannel, t);
            }
        });
    }

    private void shutdownChildChannel(Channel childChannel, Throwable t) {
        childChannel.pipeline().fireExceptionCaught(t);
    }

    /**
     * Bootstraps a child stream channel from the parent socket channel. Done in parent channel event loop.
     *
     * @param channelPromise Promise to notify when channel is available.
     */
    private void createChildChannel(Promise<Channel> channelPromise) {
        doInEventLoop(connection.eventLoop(), () -> createChildChannel0(channelPromise), channelPromise);
    }

    private void createChildChannel0(Promise<Channel> channelPromise) {
        if (goAway) {
            channelPromise.tryFailure(new IOException("No streams are available on this connection."));
        } else {
            // Once protocol future is notified then parent pipeline is configured and ready to go
            connection.attr(PROTOCOL_FUTURE).get()
                      .whenComplete(asyncPromiseNotifyingBiConsumer(bootstrapChildChannel(), channelPromise));
        }
    }

    /**
     * Bootstraps the child stream channel and notifies the Promise on success or failure.
     *
     * @return BiConsumer that will bootstrap the child channel.
     */
    private BiConsumer<Protocol, Promise<Channel>> bootstrapChildChannel() {
        return (s, p) -> new Http2StreamChannelBootstrap(connection)
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
        return goAway ? 0 : availableStreams.get();
    }

}
