/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKeys.CHANNEL_POOL_RECORD;
import static software.amazon.awssdk.http.nio.netty.internal.utils.NettyUtils.doInEventLoop;

import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.channel.pool.ChannelPool;
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import java.util.ArrayList;
import software.amazon.awssdk.http.nio.netty.internal.utils.BetterFixedChannelPool;

/**
 * {@link ChannelPool} implementation that handles multiplexed streams. Child channels are created
 * for each HTTP/2 stream using {@link Http2StreamChannelBootstrap} with the parent channel being
 * the actual socket channel. This implementation assumes that all connections have the same setting
 * for MAX_CONCURRENT_STREAMS. Concurrent requests are load balanced across all available connections,
 * when the max concurrency for a connection is reached then a new connection will be opened.
 *
 * <p>
 * <b>Note:</b> This enforces no max concurrency. Relies on being wrapped with a {@link BetterFixedChannelPool}
 * to enforce max concurrency which gives a bunch of other good features like timeouts, max pending acquires, etc.
 * </p>
 */
public class Http2MultiplexedChannelPool implements ChannelPool {

    private final EventLoop eventLoop;
    private final ChannelPool connectionPool;
    private final long maxConcurrencyPerConnection;
    private final ArrayList<MultiplexedChannelRecord> connections;

    /**
     * @param connectionPool Connection pool for parent channels (i.e. the socket channel).
     * @param eventLoop Event loop to run all tasks in.
     * @param maxConcurrencyPerConnection Max concurrent streams per HTTP/2 connection.
     */
    Http2MultiplexedChannelPool(ChannelPool connectionPool,
                                EventLoop eventLoop,
                                long maxConcurrencyPerConnection) {
        this.connectionPool = connectionPool;
        this.eventLoop = eventLoop;
        this.maxConcurrencyPerConnection = maxConcurrencyPerConnection;
        // Customers that want an unbounded connection pool may set max concurrency to something like
        // Long.MAX_VALUE so we just stick with the initial ArrayList capacity and grow from there.
        this.connections = new ArrayList<>();
    }

    @Override
    public Future<Channel> acquire() {
        return acquire(new DefaultPromise<>(eventLoop));
    }

    @Override
    public Future<Channel> acquire(Promise<Channel> promise) {
        doInEventLoop(eventLoop, () -> acquire0(promise), promise);
        return promise;
    }

    private Future<Channel> acquire0(Promise<Channel> promise) {
        for (MultiplexedChannelRecord connection : connections) {
            if (connection.availableStreams() > 0) {
                connection.acquire(promise);
                return promise;
            }
        }
        // No available streams, establish new connection and add it to list
        connections.add(new MultiplexedChannelRecord(connectionPool.acquire(), maxConcurrencyPerConnection)
                            .acquire(promise));
        return promise;
    }

    @Override
    public Future<Void> release(Channel childChannel) {
        return release(childChannel, new DefaultPromise<>(eventLoop));
    }

    @Override
    public Future<Void> release(Channel childChannel, Promise<Void> promise) {
        doInEventLoop(eventLoop, () -> release0(childChannel, promise), promise);
        return promise;
    }

    private void release0(Channel childChannel, Promise<Void> promise) {
        Channel parentChannel = childChannel.parent();
        MultiplexedChannelRecord channelRecord = parentChannel.attr(CHANNEL_POOL_RECORD).get();
        if (!parentChannel.isActive()) {
            connections.remove(channelRecord);
            parentChannel.close();
            connectionPool.release(parentChannel);
        }
        channelRecord.release();
        childChannel.close();
        promise.setSuccess(null);
    }

    @Override
    public void close() {
        doInEventLoop(eventLoop, connectionPool::close);
    }

}
