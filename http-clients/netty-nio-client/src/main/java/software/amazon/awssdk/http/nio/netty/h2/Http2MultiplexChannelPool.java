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
import static software.amazon.awssdk.utils.NumericUtils.saturatedCast;

import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.channel.pool.ChannelPool;
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import java.util.PriorityQueue;

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
public class Http2MultiplexChannelPool implements ChannelPool {

    private final EventLoop eventLoop;
    private final ChannelPool connectionPool;
    private final long maxConcurrencyPerConnection;
    private final PriorityQueue<MultiplexedChannelRecord> connectionQueue;

    Http2MultiplexChannelPool(ChannelPool connectionPool,
                              EventLoop eventLoop,
                              long maxConcurrency,
                              long maxConcurrencyPerConnection) {
        this.connectionPool = connectionPool;
        this.eventLoop = eventLoop;
        this.maxConcurrencyPerConnection = maxConcurrencyPerConnection;
        // Calculate the max conns needed to meet the potential concurrency.
        int maxConns = Math.max(saturatedCast(maxConcurrency / maxConcurrencyPerConnection), 1);
        this.connectionQueue = new PriorityQueue<>(maxConns);
    }

    @Override
    public Future<Channel> acquire() {
        return acquire(new DefaultPromise<>(eventLoop));
    }

    @Override
    public Future<Channel> acquire(Promise<Channel> promise) {
        try {
            if (eventLoop.inEventLoop()) {
                acquire0(promise);
            } else {
                eventLoop.submit(() -> acquire0(promise));
            }
        } catch (Exception e) {
            promise.setFailure(e);
        }
        return promise;
    }

    private Future<Channel> acquire0(Promise<Channel> promise) {
        if (connectionQueue.peek() == null) {
            connectionQueue.add(new MultiplexedChannelRecord(connectionPool.acquire(), maxConcurrencyPerConnection)
                                    .acquire(promise));
        } else {
            MultiplexedChannelRecord multiplexedChannelRecord = connectionQueue.poll();
            multiplexedChannelRecord.acquire(promise);
            // If we still have available streams on the connection then add it back to the queue to be acquired again
            if (multiplexedChannelRecord.availableStreams() > 0) {
                connectionQueue.add(multiplexedChannelRecord);
            }
        }
        return promise;
    }

    @Override
    public Future<Void> release(Channel childChannel) {
        return release(childChannel, new DefaultPromise<>(eventLoop));
    }

    @Override
    public Future<Void> release(Channel childChannel, Promise<Void> promise) {
        try {
            if (eventLoop.inEventLoop()) {
                release0(childChannel, promise);
            } else {
                eventLoop.submit(() -> release0(childChannel, promise));
            }
        } catch (Exception e) {
            promise.setFailure(e);
        }
        return promise;
    }

    private void release0(Channel childChannel, Promise<Void> promise) {
        Channel parentChannel = childChannel.parent();
        MultiplexedChannelRecord multiplexedChannelRecord = parentChannel.attr(CHANNEL_POOL_RECORD).get().release();
        // If this release brings the available streams back up to one add the connection back to the queue so it
        // may be acquired again.
        if (multiplexedChannelRecord.availableStreams() == 1) {
            connectionQueue.add(multiplexedChannelRecord);
        }
        // TODO do we need to close child stream?
        childChannel.close();
        promise.setSuccess(null);
    }

    @Override
    public void close() {
        connectionPool.close();
    }

}
