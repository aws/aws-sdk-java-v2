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
import static software.amazon.awssdk.http.nio.netty.internal.utils.NettyUtils.doInEventLoop;

import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.channel.pool.ChannelPool;
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import java.util.ArrayList;
import java.util.Collection;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
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
@SdkInternalApi
public class Http2MultiplexedChannelPool implements ChannelPool {

    private final EventLoop eventLoop;
    private final ChannelPool connectionPool;
    private final long maxConcurrencyPerConnection;
    private final ArrayList<MultiplexedChannelRecord> connections;
    private boolean closed = false;

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

    @SdkTestInternalApi
    Http2MultiplexedChannelPool(ChannelPool connectionPool,
                                EventLoop eventLoop,
                                long maxConcurrencyPerConnection,
                                Collection<MultiplexedChannelRecord> connections) {
        this.connectionPool = connectionPool;
        this.eventLoop = eventLoop;
        this.maxConcurrencyPerConnection = maxConcurrencyPerConnection;
        this.connections = new ArrayList<>(connections);
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
        if (closed) {
            return promise.setFailure(new IllegalStateException("Channel pool is closed!"));
        }

        for (MultiplexedChannelRecord connection : connections) {
            if (connection.availableStreams() > 0) {
                connection.acquire(promise);
                return promise;
            }
        }
        // No available streams, establish new connection and add it to list
        connections.add(new MultiplexedChannelRecord(connectionPool.acquire(),
                                                     maxConcurrencyPerConnection,
                                                     this::releaseParentChannel)
                            .acquire(promise));
        return promise;
    }

    /**
     * Releases parent channel on failure and cleans up record from connections list.
     *
     * @param parentChannel Channel to release. May be null if no channel is established.
     * @param record Record to cleanup.
     */
    private void releaseParentChannel(Channel parentChannel, MultiplexedChannelRecord record) {
        doInEventLoop(eventLoop, () -> releaseParentChannel0(parentChannel, record));
    }

    private void releaseParentChannel0(Channel parentChannel, MultiplexedChannelRecord record) {
        if (parentChannel != null) {
            try {
                parentChannel.close();
            } finally {
                connectionPool.release(parentChannel);
            }
        }
        connections.remove(record);
    }

    @Override
    public Future<Void> release(Channel childChannel) {
        return release(childChannel, new DefaultPromise<>(eventLoop));
    }

    @Override
    public Future<Void> release(Channel channel, Promise<Void> promise) {
        doInEventLoop(eventLoop, () -> release0(channel, promise), promise);
        return promise;
    }

    private void release0(Channel channel, Promise<Void> promise) {
        if (channel.parent() == null) {
            // This is the socket channel, close and release from underlying connection pool
            try {
                releaseParentChannel(channel);
            } finally {
                // This channel doesn't technically belong to this pool as it was never acquired directly
                promise.setFailure(new IllegalArgumentException("Channel does not belong to this pool"));
            }
        } else {
            Channel parentChannel = channel.parent();
            MultiplexedChannelRecord channelRecord = parentChannel.attr(CHANNEL_POOL_RECORD).get();
            channelRecord.release(channel);
            channel.close();
            promise.setSuccess(null);
        }
    }

    private void releaseParentChannel(Channel parentChannel) {
        MultiplexedChannelRecord channelRecord = parentChannel.attr(CHANNEL_POOL_RECORD).get();
        connections.remove(channelRecord);
        parentChannel.close();
        connectionPool.release(parentChannel);
    }

    @Override
    public void close() {
        try {
            setClosedFlag().await();
            for (MultiplexedChannelRecord c : connections) {
                Future<Channel> f = c.getConnectionFuture();
                f.await();
                if (f.isSuccess()) {
                    connectionPool.release(f.getNow()).await();
                }
            }
            connectionPool.close();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ie);
        }
    }

    private Promise<Void> setClosedFlag() {
        Promise<Void> closedFuture = eventLoop.newPromise();
        doInEventLoop(eventLoop, () -> {
            closed = true;
            closedFuture.setSuccess(null);
        });
        return closedFuture;
    }
}
