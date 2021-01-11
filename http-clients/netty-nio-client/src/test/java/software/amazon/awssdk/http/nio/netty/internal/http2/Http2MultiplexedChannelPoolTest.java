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
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.HTTP2_CONNECTION;
import static software.amazon.awssdk.http.nio.netty.internal.http2.utils.Http2TestUtils.newHttp2Channel;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2LocalFlowController;
import io.netty.handler.codec.http2.Http2Stream;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.FailedFuture;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import software.amazon.awssdk.http.HttpMetric;
import software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricCollector;

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

        ChannelPool pool = new Http2MultiplexedChannelPool(connectionPool, loopGroup.next(), null);

        Future<Channel> acquirePromise = pool.acquire().await();
        assertThat(acquirePromise.isSuccess()).isFalse();
        assertThat(acquirePromise.cause()).isEqualTo(exception);
    }

    @Test
    public void releaseParentChannelIfReleasingLastChildChannelOnGoAwayChannel() {
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

            MultiplexedChannelRecord record = new MultiplexedChannelRecord(channel, 8, null);
            Http2MultiplexedChannelPool h2Pool = new Http2MultiplexedChannelPool(connectionPool, loopGroup,
                                                                                 Collections.singleton(record), null);

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
        Http2MultiplexedChannelPool h2Pool = new Http2MultiplexedChannelPool(connectionPool, loopGroup.next(), null);

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

            MultiplexedChannelRecord record = new MultiplexedChannelRecord(channel, 8, null);
            Http2MultiplexedChannelPool h2Pool = new Http2MultiplexedChannelPool(connectionPool, loopGroup,
                                                                                 Collections.singleton(record), null);

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
        Channel channel = new EmbeddedChannel();

        try {
            ChannelPool connectionPool = Mockito.mock(ChannelPool.class);

            loopGroup.register(channel).awaitUninterruptibly();
            Promise<Channel> channelPromise = new DefaultPromise<>(loopGroup.next());
            channelPromise.setSuccess(channel);

            Mockito.when(connectionPool.acquire()).thenReturn(channelPromise);

            Http2MultiplexedChannelPool h2Pool = new Http2MultiplexedChannelPool(connectionPool, loopGroup,
                                                                                 Collections.emptySet(), null);

            h2Pool.acquire().awaitUninterruptibly();
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

            MultiplexedChannelRecord record = new MultiplexedChannelRecord(channel, 8, null);
            Http2MultiplexedChannelPool h2Pool = new Http2MultiplexedChannelPool(connectionPool, loopGroup,
                                                                                 Collections.singleton(record), null);

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

    @Test
    public void acquire_shouldExpandConnectionWindowSizeProportionally() {
        int maxConcurrentStream = 3;
        EmbeddedChannel channel = newHttp2Channel();
        channel.attr(ChannelAttributeKey.MAX_CONCURRENT_STREAMS).set((long) maxConcurrentStream);

        try {
            ChannelPool connectionPool = Mockito.mock(ChannelPool.class);

            loopGroup.register(channel).awaitUninterruptibly();
            Promise<Channel> channelPromise = new DefaultPromise<>(loopGroup.next());
            channelPromise.setSuccess(channel);

            Mockito.when(connectionPool.acquire()).thenReturn(channelPromise);

            Http2MultiplexedChannelPool h2Pool = new Http2MultiplexedChannelPool(connectionPool, loopGroup,
                                                                                 Collections.emptySet(), null);

            Future<Channel> acquire = h2Pool.acquire();
            acquire.awaitUninterruptibly();
            channel.runPendingTasks();

            Http2Connection http2Connection = channel.attr(HTTP2_CONNECTION).get();
            Http2LocalFlowController flowController =
                http2Connection.local().flowController();

            System.out.println(flowController.initialWindowSize());
            Http2Stream connectionStream = http2Connection.stream(0);

            // 1_048_576 (initial configured window size), 65535 (configured initial window size)
            // (1048576 - 65535) *2 + 65535 = 2031617
            assertThat(flowController.windowSize(connectionStream)).isEqualTo(2031617);

            // 2031617 + 1048576 (configured initial window size) = 3080193
            assertThat(flowController.initialWindowSize(connectionStream)).isEqualTo(3080193);

            // acquire again
            h2Pool.acquire().awaitUninterruptibly();
            channel.runPendingTasks();

            // 3080193 + 1048576 (configured initial window size) = 4128769
            assertThat(flowController.initialWindowSize(connectionStream)).isEqualTo(4128769);

            Mockito.verify(connectionPool, Mockito.times(1)).acquire();
        } finally {
            channel.close();
        }
    }

    @Test
    public void metricsShouldSumAllChildChannels() throws InterruptedException {
        int maxConcurrentStream = 2;
        EmbeddedChannel channel1 = newHttp2Channel();
        EmbeddedChannel channel2 = newHttp2Channel();
        channel1.attr(ChannelAttributeKey.MAX_CONCURRENT_STREAMS).set((long) maxConcurrentStream);
        channel2.attr(ChannelAttributeKey.MAX_CONCURRENT_STREAMS).set((long) maxConcurrentStream);

        try {
            ChannelPool connectionPool = Mockito.mock(ChannelPool.class);

            loopGroup.register(channel1).awaitUninterruptibly();
            loopGroup.register(channel2).awaitUninterruptibly();
            Promise<Channel> channel1Promise = new DefaultPromise<>(loopGroup.next());
            Promise<Channel> channel2Promise = new DefaultPromise<>(loopGroup.next());
            channel1Promise.setSuccess(channel1);
            channel2Promise.setSuccess(channel2);

            Mockito.when(connectionPool.acquire()).thenReturn(channel1Promise, channel2Promise);

            Http2MultiplexedChannelPool h2Pool = new Http2MultiplexedChannelPool(connectionPool,
                                                                                 Http2MultiplexedChannelPoolTest.loopGroup,
                                                                                 Collections.emptySet(), null);
            MetricCollection metrics;

            metrics = getMetrics(h2Pool);
            assertThat(metrics.metricValues(HttpMetric.AVAILABLE_CONCURRENCY)).containsExactly(0);

            doAcquire(channel1, channel2, h2Pool);

            metrics = getMetrics(h2Pool);
            assertThat(metrics.metricValues(HttpMetric.AVAILABLE_CONCURRENCY)).containsExactly(1);

            doAcquire(channel1, channel2, h2Pool);

            metrics = getMetrics(h2Pool);
            assertThat(metrics.metricValues(HttpMetric.AVAILABLE_CONCURRENCY)).containsExactly(0);

            doAcquire(channel1, channel2, h2Pool);

            metrics = getMetrics(h2Pool);
            assertThat(metrics.metricValues(HttpMetric.AVAILABLE_CONCURRENCY)).containsExactly(1);

            Channel lastAcquire = doAcquire(channel1, channel2, h2Pool);

            metrics = getMetrics(h2Pool);
            assertThat(metrics.metricValues(HttpMetric.AVAILABLE_CONCURRENCY)).containsExactly(0);

            lastAcquire.close();
            h2Pool.release(lastAcquire).awaitUninterruptibly();

            metrics = getMetrics(h2Pool);
            assertThat(metrics.metricValues(HttpMetric.AVAILABLE_CONCURRENCY)).containsExactly(1);

            channel1.close();
            h2Pool.release(channel1);

            metrics = getMetrics(h2Pool);
            assertThat(metrics.metricValues(HttpMetric.AVAILABLE_CONCURRENCY)).containsExactly(1);

            channel2.close();

            metrics = getMetrics(h2Pool);
            assertThat(metrics.metricValues(HttpMetric.AVAILABLE_CONCURRENCY)).containsExactly(0);
        } finally {
            channel1.close();
            channel2.close();
        }
    }

    private Channel doAcquire(EmbeddedChannel channel1, EmbeddedChannel channel2, Http2MultiplexedChannelPool h2Pool) {
        Future<Channel> acquire = h2Pool.acquire();
        acquire.awaitUninterruptibly();
        runPendingTasks(channel1, channel2);
        return acquire.getNow();
    }

    private void runPendingTasks(EmbeddedChannel channel1, EmbeddedChannel channel2) {
        channel1.runPendingTasks();
        channel2.runPendingTasks();
    }

    private MetricCollection getMetrics(Http2MultiplexedChannelPool h2Pool) {
        MetricCollector metricCollector = MetricCollector.create("test");
        h2Pool.collectChannelPoolMetrics(metricCollector);
        return metricCollector.collect();
    }
}
