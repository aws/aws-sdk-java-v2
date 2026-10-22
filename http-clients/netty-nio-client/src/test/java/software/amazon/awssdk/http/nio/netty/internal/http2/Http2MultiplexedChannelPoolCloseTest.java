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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.pool.AbstractChannelPoolHandler;
import io.netty.channel.pool.SimpleChannelPool;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Reproduces https://github.com/aws/aws-sdk-java-v2/issues/6863: closing the HTTP/2 pool must not block an event loop
 * when the underlying connection pool still holds an idle connection whose close completes asynchronously
 * (e.g. TLS close_notify being flushed).
 */
class Http2MultiplexedChannelPoolCloseTest {
    private EventLoopGroup singleThreadLoop;
    private Channel serverChannel;

    @BeforeEach
    void setUp() {
        // A single event loop so that the pool's close event loop is the same loop that owns the idle connection.
        singleThreadLoop = new DefaultEventLoopGroup(1);
    }

    @AfterEach
    void tearDown() {
        if (serverChannel != null) {
            serverChannel.close().awaitUninterruptibly();
        }
        singleThreadLoop.shutdownGracefully().awaitUninterruptibly();
    }

    @Test
    void close_idleConnectionWithAsynchronousClose_closesWithoutBlockingEventLoop() {
        SimpleChannelPool connectionPool = poolWithIdleConnection();
        Channel idleConnection = connectionPool.acquire().syncUninterruptibly().getNow();
        connectionPool.release(idleConnection).syncUninterruptibly();

        Http2MultiplexedChannelPool h2Pool =
            new Http2MultiplexedChannelPool(connectionPool, singleThreadLoop, Collections.emptySet(), null);

        assertThatCode(h2Pool::close).doesNotThrowAnyException();
        assertThat(idleConnection.closeFuture().awaitUninterruptibly().isSuccess()).isTrue();
    }

    @Test
    void close_calledFromConnectionEventLoop_closesWithoutBlockingEventLoop() throws Exception {
        // HttpOrHttp2ChannelPool calls close() on one of its event loops, which may own the idle connection.
        SimpleChannelPool connectionPool = poolWithIdleConnection();
        Channel idleConnection = connectionPool.acquire().syncUninterruptibly().getNow();
        connectionPool.release(idleConnection).syncUninterruptibly();

        Http2MultiplexedChannelPool h2Pool =
            new Http2MultiplexedChannelPool(connectionPool, singleThreadLoop, Collections.emptySet(), null);

        singleThreadLoop.submit(h2Pool::close).get(5, TimeUnit.SECONDS);
        assertThat(idleConnection.closeFuture().await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(idleConnection.closeFuture().isSuccess()).isTrue();
    }

    private SimpleChannelPool poolWithIdleConnection() {
        LocalAddress address = new LocalAddress("h2-pool-close-" + System.nanoTime());
        serverChannel = new ServerBootstrap().group(singleThreadLoop)
                                             .channel(LocalServerChannel.class)
                                             .childHandler(new ChannelInboundHandlerAdapter())
                                             .bind(address)
                                             .syncUninterruptibly()
                                             .channel();
        Bootstrap bootstrap = new Bootstrap().group(singleThreadLoop)
                                             .channel(LocalChannel.class)
                                             .remoteAddress(address);
        return new SimpleChannelPool(bootstrap, new AbstractChannelPoolHandler() {
            @Override
            public void channelCreated(Channel ch) {
                ch.pipeline().addLast(new DeferredCloseHandler());
            }
        });
    }

    /**
     * Completes the close on a later event loop iteration, like {@code SslHandler} does while flushing close_notify.
     */
    private static final class DeferredCloseHandler extends ChannelOutboundHandlerAdapter {
        @Override
        public void close(ChannelHandlerContext ctx, ChannelPromise promise) {
            ctx.executor().execute(() -> ctx.close(promise));
        }
    }
}
