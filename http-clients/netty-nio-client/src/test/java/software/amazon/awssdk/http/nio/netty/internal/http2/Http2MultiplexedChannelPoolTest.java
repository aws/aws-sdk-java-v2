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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.MAX_CONCURRENT_STREAMS;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.PING_TRACKER;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.PROTOCOL_FUTURE;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.FailedFuture;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.ScheduledFuture;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.nio.netty.internal.MockChannel;

/**
 * Tests for {@link Http2MultiplexedChannelPool}.
 */
public class Http2MultiplexedChannelPoolTest {
    private static EventLoopGroup loopGroup;

    @BeforeClass
    public static void setup() {
        loopGroup = new NioEventLoopGroup(4);
    }

    @AfterClass
    public static void teardown() {
        loopGroup.shutdownGracefully().awaitUninterruptibly();
    }

    @Test
    public void failedConnectionAcquireNotifiesPromise() throws InterruptedException {
        IOException exception = new IOException();
        ChannelPool connectionPool = mock(ChannelPool.class);
        when(connectionPool.acquire()).thenReturn(new FailedFuture<>(loopGroup.next(), exception));

        ChannelPool pool = new Http2MultiplexedChannelPool(connectionPool, loopGroup.next());

        Future<Channel> acquirePromise = pool.acquire().await();
        assertThat(acquirePromise.isSuccess()).isFalse();
        assertThat(acquirePromise.cause()).isEqualTo(exception);
    }

    @Test
    public void releaseParentChannelIfReleasingLastChildChannelOnGoAwayChannel() throws InterruptedException {
        SocketChannel channel = new NioSocketChannel();
        try {
            loopGroup.register(channel).awaitUninterruptibly();

            ChannelPool connectionPool = mock(ChannelPool.class);
            ArgumentCaptor<Promise> releasePromise = ArgumentCaptor.forClass(Promise.class);
            when(connectionPool.release(eq(channel), releasePromise.capture())).thenAnswer(invocation -> {
                Promise<?> promise = releasePromise.getValue();
                promise.setSuccess(null);
                return promise;
            });

            MultiplexedChannelRecord record = new MultiplexedChannelRecord(channel, 8);
            Http2MultiplexedChannelPool h2Pool = new Http2MultiplexedChannelPool(connectionPool, loopGroup,
                                                                                 Collections.singleton(record));

            h2Pool.close();

            InOrder inOrder = Mockito.inOrder(connectionPool);
            inOrder.verify(connectionPool).release(eq(channel), isA(Promise.class));
            inOrder.verify(connectionPool).close();
        } finally {
            channel.close().awaitUninterruptibly();
        }
    }

    @Test
    public void acquireAfterCloseFails() throws InterruptedException {
        ChannelPool connectionPool = mock(ChannelPool.class);
        Http2MultiplexedChannelPool h2Pool = new Http2MultiplexedChannelPool(connectionPool, loopGroup.next());

        h2Pool.close();

        Future<Channel> acquireResult = h2Pool.acquire().await();
        assertThat(acquireResult.isSuccess()).isFalse();
        assertThat(acquireResult.cause()).isInstanceOf(IOException.class);
    }

    @Test
    public void closeWaitsForConnectionToBeReleasedBeforeClosingConnectionPool() {
        SocketChannel channel = new NioSocketChannel();
        try {
            loopGroup.register(channel).awaitUninterruptibly();

            ChannelPool connectionPool = mock(ChannelPool.class);
            ArgumentCaptor<Promise> releasePromise = ArgumentCaptor.forClass(Promise.class);
            when(connectionPool.release(eq(channel), releasePromise.capture())).thenAnswer(invocation -> {
                Promise<?> promise = releasePromise.getValue();
                promise.setSuccess(null);
                return promise;
            });

            MultiplexedChannelRecord record = new MultiplexedChannelRecord(channel, 8);
            Http2MultiplexedChannelPool h2Pool = new Http2MultiplexedChannelPool(connectionPool, loopGroup,
                                                                                 Collections.singleton(record));

            h2Pool.close();

            InOrder inOrder = Mockito.inOrder(connectionPool);
            inOrder.verify(connectionPool).release(eq(channel), isA(Promise.class));
            inOrder.verify(connectionPool).close();
        } finally {
            channel.close().awaitUninterruptibly();
        }
    }

    @Test
    public void acquire_shouldAcquireAgainIfExistingNotReusable() throws Exception {
        Channel channel = new MockChannel();

        try {
            ChannelPool connectionPool = Mockito.mock(ChannelPool.class);
            channel.attr(PING_TRACKER).set(new PingTracker(() -> Mockito.mock(ScheduledFuture.class)));

            loopGroup.register(channel).awaitUninterruptibly();
            Promise<Channel> channelPromise = new DefaultPromise<>(loopGroup.next());
            channelPromise.setSuccess(channel);

            Mockito.when(connectionPool.acquire()).thenReturn(channelPromise);

            Http2MultiplexedChannelPool h2Pool = new Http2MultiplexedChannelPool(connectionPool, loopGroup, Collections.emptySet());

            h2Pool.acquire().awaitUninterruptibly();

            channel.attr(PING_TRACKER).set(new PingTracker(() -> Mockito.mock(ScheduledFuture.class)));

            h2Pool.acquire().awaitUninterruptibly();

            Mockito.verify(connectionPool, Mockito.times(2)).acquire();
        } finally {
            channel.close();
        }
    }

    @Test(timeout = 5_000)
    public void interruptDuringClosePreservesFlag() throws InterruptedException {
        SocketChannel channel = new NioSocketChannel();
        try {
            loopGroup.register(channel).awaitUninterruptibly();
            Promise<Channel> channelPromise = new DefaultPromise<>(loopGroup.next());
            channelPromise.setSuccess(channel);

            ChannelPool connectionPool = mock(ChannelPool.class);
            Promise<Void> releasePromise = Mockito.spy(new DefaultPromise<>(loopGroup.next()));

            when(connectionPool.release(eq(channel))).thenReturn(releasePromise);

            MultiplexedChannelRecord record = new MultiplexedChannelRecord(channel, 8);
            Http2MultiplexedChannelPool h2Pool = new Http2MultiplexedChannelPool(connectionPool, loopGroup,
                                                                                 Collections.singleton(record));

            CompletableFuture<Boolean> interrupteFlagPreserved = new CompletableFuture<>();

            Thread t = new Thread(() -> {
                try {
                    h2Pool.close();
                } catch (Exception e) {
                    if (e.getCause() instanceof InterruptedException && Thread.currentThread().isInterrupted()) {
                        interrupteFlagPreserved.complete(true);
                    }
                }
            });

            t.start();
            t.interrupt();
            t.join();
            assertThat(interrupteFlagPreserved.join()).isTrue();
        } finally {
            channel.close().awaitUninterruptibly();
        }
    }
}
