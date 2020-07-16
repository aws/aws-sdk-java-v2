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

package software.amazon.awssdk.http.nio.netty.internal.http2;

import static java.util.stream.Collectors.toList;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.HTTP2_CONNECTION;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.HTTP2_INITIAL_WINDOW_SIZE;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.HTTP2_MULTIPLEXED_CHANNEL_POOL;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.MAX_CONCURRENT_STREAMS;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.PROTOCOL_FUTURE;
import static software.amazon.awssdk.http.nio.netty.internal.utils.NettyUtils.doInEventLoop;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.pool.ChannelPool;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2LocalFlowController;
import io.netty.handler.codec.http2.Http2Stream;
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.PromiseCombiner;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.http.HttpMetric;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.nio.netty.internal.SdkChannelPool;
import software.amazon.awssdk.http.nio.netty.internal.utils.BetterFixedChannelPool;
import software.amazon.awssdk.http.nio.netty.internal.utils.NettyUtils;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

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
public class Http2MultiplexedChannelPool implements SdkChannelPool {
    private static final Logger log = Logger.loggerFor(Http2MultiplexedChannelPool.class);

    /**
     * Reference to the {@link MultiplexedChannelRecord} on a channel.
     */
    private static final AttributeKey<MultiplexedChannelRecord> MULTIPLEXED_CHANNEL = NettyUtils.getOrCreateAttributeKey(
            "software.amazon.awssdk.http.nio.netty.internal.http2.Http2MultiplexedChannelPool.MULTIPLEXED_CHANNEL");

    /**
     * Whether a parent channel has been released yet. This guards against double-releasing to the delegate connection pool.
     */
    private static final AttributeKey<Boolean> RELEASED = NettyUtils.getOrCreateAttributeKey(
        "software.amazon.awssdk.http.nio.netty.internal.http2.Http2MultiplexedChannelPool.RELEASED");

    private final ChannelPool connectionPool;
    private final EventLoopGroup eventLoopGroup;
    private final Set<MultiplexedChannelRecord> connections;
    private final Duration idleConnectionTimeout;

    private AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * @param connectionPool Connection pool for parent channels (i.e. the socket channel).
     */
    Http2MultiplexedChannelPool(ChannelPool connectionPool,
                                EventLoopGroup eventLoopGroup,
                                Duration idleConnectionTimeout) {
        this.connectionPool = connectionPool;
        this.eventLoopGroup = eventLoopGroup;
        this.connections = ConcurrentHashMap.newKeySet();
        this.idleConnectionTimeout = idleConnectionTimeout;
    }

    @SdkTestInternalApi
    Http2MultiplexedChannelPool(ChannelPool connectionPool,
                                EventLoopGroup eventLoopGroup,
                                Set<MultiplexedChannelRecord> connections,
                                Duration idleConnectionTimeout) {
        this(connectionPool, eventLoopGroup, idleConnectionTimeout);
        this.connections.addAll(connections);
    }

    @Override
    public Future<Channel> acquire() {
        return acquire(eventLoopGroup.next().newPromise());
    }

    @Override
    public Future<Channel> acquire(Promise<Channel> promise) {
        if (closed.get()) {
            return promise.setFailure(new IOException("Channel pool is closed!"));
        }

        for (MultiplexedChannelRecord multiplexedChannel : connections) {
            if (acquireStreamOnInitializedConnection(multiplexedChannel, promise)) {
                return promise;
            }
        }

        // No available streams on existing connections, establish new connection and add it to list
        acquireStreamOnNewConnection(promise);
        return promise;
    }

    private void acquireStreamOnNewConnection(Promise<Channel> promise) {
        Future<Channel> newConnectionAcquire = connectionPool.acquire();

        newConnectionAcquire.addListener(f -> {
            if (!newConnectionAcquire.isSuccess()) {
                promise.setFailure(newConnectionAcquire.cause());
                return;
            }

            Channel parentChannel = newConnectionAcquire.getNow();
            try {
                parentChannel.attr(HTTP2_MULTIPLEXED_CHANNEL_POOL).set(this);

                // When the protocol future is completed on the new connection, we're ready for new streams to be added to it.
                parentChannel.attr(PROTOCOL_FUTURE).get()
                             .thenAccept(protocol -> acquireStreamOnFreshConnection(promise, parentChannel, protocol))
                             .exceptionally(throwable -> failAndCloseParent(promise, parentChannel, throwable));
            } catch (Throwable e) {
                failAndCloseParent(promise, parentChannel, e);
            }
        });
    }

    private void acquireStreamOnFreshConnection(Promise<Channel> promise, Channel parentChannel, Protocol protocol) {
        try {
            Long maxStreams = parentChannel.attr(MAX_CONCURRENT_STREAMS).get();

            Validate.isTrue(protocol == Protocol.HTTP2,
                            "Protocol negotiated on connection (%s) was expected to be HTTP/2, but it "
                            + "was %s.", parentChannel, Protocol.HTTP1_1);
            Validate.isTrue(maxStreams != null,
                            "HTTP/2 was negotiated on the connection (%s), but the maximum number of "
                            + "streams was not initialized.", parentChannel);
            Validate.isTrue(maxStreams > 0, "Maximum streams were not positive on channel (%s).", parentChannel);

            MultiplexedChannelRecord multiplexedChannel = new MultiplexedChannelRecord(parentChannel, maxStreams,
                                                                                       idleConnectionTimeout);
            parentChannel.attr(MULTIPLEXED_CHANNEL).set(multiplexedChannel);

            Promise<Channel> streamPromise = parentChannel.eventLoop().newPromise();

            if (!acquireStreamOnInitializedConnection(multiplexedChannel, streamPromise)) {
                failAndCloseParent(promise, parentChannel,
                                   new IOException("Connection was closed while creating a new stream."));
                return;
            }

            streamPromise.addListener(f -> {
                if (!streamPromise.isSuccess()) {
                    promise.setFailure(streamPromise.cause());
                    return;
                }

                Channel stream = streamPromise.getNow();
                cacheConnectionForFutureStreams(stream, multiplexedChannel, promise);
            });
        } catch (Throwable e) {
            failAndCloseParent(promise, parentChannel, e);
        }
    }

    private void cacheConnectionForFutureStreams(Channel stream,
                                                 MultiplexedChannelRecord multiplexedChannel,
                                                 Promise<Channel> promise) {
        Channel parentChannel = stream.parent();

        // Before we cache the connection, make sure that exceptions on the connection will remove it from the cache.
        parentChannel.pipeline().addLast(ReleaseOnExceptionHandler.INSTANCE);
        connections.add(multiplexedChannel);

        if (closed.get()) {
            // Whoops, we were closed while we were setting up. Make sure everything here is cleaned up properly.
            failAndCloseParent(promise, parentChannel,
                               new IOException("Connection pool was closed while creating a new stream."));
            return;
        }

        promise.setSuccess(stream);
    }

    /**
     * By default, connection window size is a constant value:
     * connectionWindowSize = 65535 + (configureInitialWindowSize - 65535) * 2.
     * See https://github.com/netty/netty/blob/5c458c9a98d4d3d0345e58495e017175156d624f/codec-http2/src/main/java/io/netty
     * /handler/codec/http2/Http2FrameCodec.java#L255
     * We should expand connection window so that the window size proportional to the number of concurrent streams within the
     * connection.
     * Note that when {@code WINDOW_UPDATE} will be sent depends on the processedWindow in DefaultHttp2LocalFlowController.
     */
    private void tryExpandConnectionWindow(Channel parentChannel) {
        doInEventLoop(parentChannel.eventLoop(), () -> {
            Http2Connection http2Connection = parentChannel.attr(HTTP2_CONNECTION).get();
            Integer initialWindowSize = parentChannel.attr(HTTP2_INITIAL_WINDOW_SIZE).get();

            Validate.notNull(http2Connection, "http2Connection should not be null on channel " + parentChannel);
            Validate.notNull(http2Connection, "initialWindowSize should not be null on channel " + parentChannel);

            Http2Stream connectionStream = http2Connection.connectionStream();
            log.debug(() -> "Expanding connection window size for " + parentChannel + " by " + initialWindowSize);
            try {
                Http2LocalFlowController localFlowController = http2Connection.local().flowController();
                localFlowController.incrementWindowSize(connectionStream, initialWindowSize);

            } catch (Http2Exception e) {
                log.warn(() -> "Failed to increment windowSize of connection " + parentChannel, e);
            }
        });
    }

    private Void failAndCloseParent(Promise<Channel> promise, Channel parentChannel, Throwable exception) {
        log.debug(() -> "Channel acquiring failed, closing connection " + parentChannel, exception);
        promise.setFailure(exception);
        closeAndReleaseParent(parentChannel);
        return null;
    }

    /**
     * Acquire a stream on a connection that has already been initialized. This will return false if the connection cannot have
     * any more streams allocated, and true if the stream can be allocated.
     *
     * This will NEVER complete the provided future when the return value is false. This will ALWAYS complete the provided
     * future when the return value is true.
     */
    private boolean acquireStreamOnInitializedConnection(MultiplexedChannelRecord channelRecord, Promise<Channel> promise) {
        Promise<Channel> acquirePromise = channelRecord.getConnection().eventLoop().newPromise();

        if (!channelRecord.acquireStream(acquirePromise)) {
            return false;
        }

        acquirePromise.addListener(f -> {
            try {
                if (!acquirePromise.isSuccess()) {
                    promise.setFailure(acquirePromise.cause());
                    return;
                }

                Channel channel = acquirePromise.getNow();
                channel.attr(HTTP2_MULTIPLEXED_CHANNEL_POOL).set(this);
                channel.attr(MULTIPLEXED_CHANNEL).set(channelRecord);
                promise.setSuccess(channel);

                tryExpandConnectionWindow(channel.parent());
            } catch (Exception e) {
                promise.setFailure(e);
            }
        });

        return true;
    }

    @Override
    public Future<Void> release(Channel childChannel) {
        return release(childChannel, childChannel.eventLoop().newPromise());
    }

    @Override
    public Future<Void> release(Channel childChannel, Promise<Void> promise) {
        if (childChannel.parent() == null) {
            // This isn't a child channel. Oddly enough, this is "expected" and is handled properly by the
            // BetterFixedChannelPool AS LONG AS we return an IllegalArgumentException via the promise.
            closeAndReleaseParent(childChannel);
            return promise.setFailure(new IllegalArgumentException("Channel (" + childChannel + ") is not a child channel."));
        }

        Channel parentChannel = childChannel.parent();
        MultiplexedChannelRecord multiplexedChannel = parentChannel.attr(MULTIPLEXED_CHANNEL).get();
        if (multiplexedChannel == null) {
            // This is a child channel, but there is no attached multiplexed channel, which there should be if it was from
            // this pool. Close it and log an error.
            Exception exception = new IOException("Channel (" + childChannel + ") is not associated with any channel records. "
                                                  + "It will be closed, but cannot be released within this pool.");
            log.error(exception::getMessage);
            childChannel.close();
            return promise.setFailure(exception);
        }

        multiplexedChannel.closeAndReleaseChild(childChannel);

        if (multiplexedChannel.canBeClosedAndReleased()) {
            // We just closed the last stream in a connection that has reached the end of its life.
            return closeAndReleaseParent(parentChannel, null, promise);
        }

        return promise.setSuccess(null);
    }

    private Future<Void> closeAndReleaseParent(Channel parentChannel) {
        return closeAndReleaseParent(parentChannel, null, parentChannel.eventLoop().newPromise());
    }

    private Future<Void> closeAndReleaseParent(Channel parentChannel, Throwable cause) {
        return closeAndReleaseParent(parentChannel, cause, parentChannel.eventLoop().newPromise());
    }

    private Future<Void> closeAndReleaseParent(Channel parentChannel, Throwable cause, Promise<Void> resultPromise) {
        if (parentChannel.parent() != null) {
            // This isn't a parent channel. Notify it that something is wrong.
            Exception exception = new IOException("Channel (" + parentChannel + ") is not a parent channel. It will be closed, "
                                                  + "but cannot be released within this pool.");
            log.error(exception::getMessage);
            parentChannel.close();
            return resultPromise.setFailure(exception);
        }

        MultiplexedChannelRecord multiplexedChannel = parentChannel.attr(MULTIPLEXED_CHANNEL).get();

        // We may not have a multiplexed channel if the parent channel hasn't been fully initialized.
        if (multiplexedChannel != null) {
            if (cause == null) {
                multiplexedChannel.closeChildChannels();
            } else {
                multiplexedChannel.closeChildChannels(cause);
            }
            connections.remove(multiplexedChannel);
        }

        parentChannel.close();
        if (parentChannel.attr(RELEASED).getAndSet(Boolean.TRUE) == null) {
            return connectionPool.release(parentChannel, resultPromise);
        }

        return resultPromise.setSuccess(null);
    }

    void handleGoAway(Channel parentChannel, int lastStreamId, GoAwayException exception) {
        log.debug(() -> "Received GOAWAY on " + parentChannel + " with lastStreamId of " + lastStreamId);
        try {
            MultiplexedChannelRecord multiplexedChannel = parentChannel.attr(MULTIPLEXED_CHANNEL).get();

            if (multiplexedChannel != null) {
                multiplexedChannel.handleGoAway(lastStreamId, exception);
            } else {
                // If we don't have a multiplexed channel, the parent channel hasn't been fully initialized. Close it now.
                closeAndReleaseParent(parentChannel, exception);
            }
        } catch (Exception e) {
            log.error(() -> "Failed to handle GOAWAY frame on channel " + parentChannel, e);
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            Future<?> closeCompleteFuture = doClose();

            try {
                if (!closeCompleteFuture.await(10, TimeUnit.SECONDS)) {
                    throw new RuntimeException("Event loop didn't close after 10 seconds.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }

            Throwable exception = closeCompleteFuture.cause();
            if (exception != null) {
                throw new RuntimeException("Failed to close channel pool.", exception);
            }
        }
    }

    private Future<?> doClose() {
        EventLoop closeEventLoop = eventLoopGroup.next();
        Promise<?> closeFinishedPromise = closeEventLoop.newPromise();

        doInEventLoop(closeEventLoop, () -> {
            Promise<Void> releaseAllChannelsPromise = closeEventLoop.newPromise();
            PromiseCombiner promiseCombiner = new PromiseCombiner(closeEventLoop);

            // Create a copy of the connections to remove while we close them, in case closing updates the original list.
            List<MultiplexedChannelRecord> channelsToRemove = new ArrayList<>(connections);
            for (MultiplexedChannelRecord channel : channelsToRemove) {
                promiseCombiner.add(closeAndReleaseParent(channel.getConnection()));
            }
            promiseCombiner.finish(releaseAllChannelsPromise);

            releaseAllChannelsPromise.addListener(f -> {
                connectionPool.close();
                closeFinishedPromise.setSuccess(null);
            });
        });

        return closeFinishedPromise;
    }

    @Override
    public CompletableFuture<Void> collectChannelPoolMetrics(MetricCollector metrics) {
        CompletableFuture<Void> result = new CompletableFuture<>();

        CompletableFuture<MultiplexedChannelRecord.Metrics> summedMetrics = new CompletableFuture<>();

        List<CompletableFuture<MultiplexedChannelRecord.Metrics>> channelMetrics =
            connections.stream()
                       .map(MultiplexedChannelRecord::getMetrics)
                       .collect(toList());

        accumulateMetrics(summedMetrics, channelMetrics);

        summedMetrics.whenComplete((m, t) -> {
            if (t != null) {
                result.completeExceptionally(t);
            } else {
                try {
                    metrics.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, Math.toIntExact(m.getAvailableStreams()));
                    result.complete(null);
                } catch (Exception e) {
                    result.completeExceptionally(e);
                }
            }
        });

        return result;
    }

    private void accumulateMetrics(CompletableFuture<MultiplexedChannelRecord.Metrics> result,
                                   List<CompletableFuture<MultiplexedChannelRecord.Metrics>> channelMetrics) {
        accumulateMetrics(result, channelMetrics, new MultiplexedChannelRecord.Metrics(), 0);
    }

    private void accumulateMetrics(CompletableFuture<MultiplexedChannelRecord.Metrics> result,
                                   List<CompletableFuture<MultiplexedChannelRecord.Metrics>> channelMetrics,
                                   MultiplexedChannelRecord.Metrics resultAccumulator,
                                   int index) {
        if (index >= channelMetrics.size()) {
            result.complete(resultAccumulator);
            return;
        }

        channelMetrics.get(index).whenComplete((m, t) -> {
            if (t != null) {
                result.completeExceptionally(t);
            } else {
                resultAccumulator.add(m);
                accumulateMetrics(result, channelMetrics, resultAccumulator, index + 1);
            }
        });
    }

    @Sharable
    private static final class ReleaseOnExceptionHandler extends ChannelDuplexHandler {
        private static final ReleaseOnExceptionHandler INSTANCE = new ReleaseOnExceptionHandler();

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            closeAndReleaseParent(ctx, new ClosedChannelException());
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if (cause instanceof Http2ConnectionTerminatingException) {
                closeConnectionToNewRequests(ctx, cause);
            } else {
                closeAndReleaseParent(ctx, cause);
            }
        }

        void closeConnectionToNewRequests(ChannelHandlerContext ctx, Throwable cause) {
            MultiplexedChannelRecord multiplexedChannel = ctx.channel().attr(MULTIPLEXED_CHANNEL).get();
            if (multiplexedChannel != null) {
                multiplexedChannel.closeToNewStreams();
            } else {
                closeAndReleaseParent(ctx, cause);
            }
        }

        private void closeAndReleaseParent(ChannelHandlerContext ctx, Throwable cause) {
            Http2MultiplexedChannelPool pool = ctx.channel().attr(HTTP2_MULTIPLEXED_CHANNEL_POOL).get();
            pool.closeAndReleaseParent(ctx.channel(), cause);
        }
    }
}
