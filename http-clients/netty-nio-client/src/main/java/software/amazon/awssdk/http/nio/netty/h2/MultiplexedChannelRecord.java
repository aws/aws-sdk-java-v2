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

import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKeys.CHANNEL_POOL_RECORD;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKeys.PROTOCOL_FUTURE;
import static software.amazon.awssdk.utils.NumericUtils.saturatedCast;

import io.netty.channel.Channel;
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import java.util.concurrent.atomic.AtomicLong;
import software.amazon.awssdk.http.nio.netty.internal.utils.NettyUtils;

/**
 * Contains a {@link Future} for the actual socket channel and tracks available
 * streams based on the MAX_CONCURRENT_STREAMS setting for the connection.
 */
public final class MultiplexedChannelRecord implements Comparable<MultiplexedChannelRecord> {

    private final Future<Channel> connectionFuture;
    private final AtomicLong availableStreams;

    private volatile Channel connection;

    MultiplexedChannelRecord(Future<Channel> connectionFuture, long maxConcurrencyPerConnection) {
        this.connectionFuture = connectionFuture;
        this.availableStreams = new AtomicLong(maxConcurrencyPerConnection);
    }

    MultiplexedChannelRecord acquire(Promise<Channel> channelPromise) {
        availableStreams.decrementAndGet();
        if (connection != null) {
            createChildChannel(channelPromise, connection);
        } else {
            connectionFuture.addListener((GenericFutureListener<Future<Channel>>) future -> {
                connection = future.getNow();
                createChildChannel(channelPromise, connection);

            });
        }
        return this;
    }

    /**
     * Bootstraps a child stream channel from the parent socket channel. Done in parent channel event loop.
     *
     * @param channelPromise Promise to notify when channel is available.
     * @param parentChannel Parent socket channel.
     */
    private void createChildChannel(Promise<Channel> channelPromise, Channel parentChannel) {
        if (parentChannel.eventLoop().inEventLoop()) {
            createChildChannel0(channelPromise, parentChannel);
        } else {
            parentChannel.eventLoop().submit(() -> createChildChannel0(channelPromise, parentChannel));
        }

    }

    private void createChildChannel0(Promise<Channel> channelPromise, Channel parentChannel) {
        // Set a reference to the MultiplexedChannelRecord so we can get it when a channel is released back to the pool
        parentChannel.attr(CHANNEL_POOL_RECORD).set(this);
        // Once protocol future is notified then parent pipeline is configured and ready to go
        parentChannel.attr(PROTOCOL_FUTURE).get()
                     .thenAccept(s -> {
                         // Open stream child channel and fulfill promise with it.
                         new Http2StreamChannelBootstrap(parentChannel)
                             .open()
                             .addListener(NettyUtils.createPromiseNotifyingListener(channelPromise));
                     });
    }

    MultiplexedChannelRecord release() {
        availableStreams.incrementAndGet();
        return this;
    }

    long availableStreams() {
        return availableStreams.get();
    }

    @Override
    public int compareTo(MultiplexedChannelRecord o) {
        // TODO test this
        return saturatedCast(availableStreams.get() - o.availableStreams.get());
    }

}
