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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.CONNECTION_ACQUIRE_TIMEOUT;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.MAX_PENDING_CONNECTION_ACQUIRES;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.REAP_IDLE_CONNECTIONS;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.PROTOCOL_FUTURE;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.http.HttpMetric;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.nio.netty.internal.MockChannel;
import software.amazon.awssdk.http.nio.netty.internal.NettyConfiguration;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Tests for {@link HttpOrHttp2ChannelPool}.
 */
@RunWith(MockitoJUnitRunner.class)
public class HttpOrHttp2ChannelPoolTest {
    private static EventLoopGroup eventLoopGroup;

    @Mock
    private ChannelPool mockDelegatePool;

    private HttpOrHttp2ChannelPool httpOrHttp2ChannelPool;

    @BeforeClass
    public static void setup() {
        eventLoopGroup = new NioEventLoopGroup(1);
    }

    @AfterClass
    public static void teardown() throws InterruptedException {
        eventLoopGroup.shutdownGracefully().await();
    }

    @Before
    public void methodSetup() {
        httpOrHttp2ChannelPool = new HttpOrHttp2ChannelPool(mockDelegatePool,
                                                            eventLoopGroup,
                                                            4,
                                                            new NettyConfiguration(AttributeMap.builder()
                                                                    .put(CONNECTION_ACQUIRE_TIMEOUT, Duration.ofSeconds(1))
                                                                    .put(MAX_PENDING_CONNECTION_ACQUIRES, 5)
                                                                    .put(REAP_IDLE_CONNECTIONS, false)
                                                                    .build()));
    }

    @Test
    public void protocolConfigNotStarted_closeSucceeds() {
        httpOrHttp2ChannelPool.close();
    }

    @Test(timeout = 5_000)
    public void invalidProtocolConfig_shouldFailPromise() throws Exception {
        HttpOrHttp2ChannelPool invalidChannelPool = new HttpOrHttp2ChannelPool(mockDelegatePool,
                                                            eventLoopGroup,
                                                            4,
                                                            new NettyConfiguration(AttributeMap.builder()
                                                                                               .put(CONNECTION_ACQUIRE_TIMEOUT, Duration.ofSeconds(1))
                                                                                               .put(MAX_PENDING_CONNECTION_ACQUIRES, 0)
                                                                                               .build()));

        Promise<Channel> acquirePromise = eventLoopGroup.next().newPromise();
        when(mockDelegatePool.acquire()).thenReturn(acquirePromise);

        Thread.sleep(500);

        Channel channel = new MockChannel();
        eventLoopGroup.register(channel);

        channel.attr(PROTOCOL_FUTURE).set(CompletableFuture.completedFuture(Protocol.HTTP1_1));

        acquirePromise.setSuccess(channel);

        Future<Channel> p = invalidChannelPool.acquire();
        assertThat(p.await().cause().getMessage()).contains("maxPendingAcquires: 0 (expected: >= 1)");
        verify(mockDelegatePool).release(channel);
        assertThat(channel.isOpen()).isFalse();
    }

    @Test
    public void protocolConfigNotStarted_closeClosesDelegatePool() throws InterruptedException {
        httpOrHttp2ChannelPool.close();
        Thread.sleep(500);
        verify(mockDelegatePool).close();
    }

    @Test(timeout = 5_000)
    public void poolClosed_acquireFails() throws InterruptedException {
        httpOrHttp2ChannelPool.close();
        Thread.sleep(500);
        Future<Channel> p = httpOrHttp2ChannelPool.acquire(eventLoopGroup.next().newPromise());
        assertThat(p.await().cause().getMessage()).contains("pool is closed");
    }

    @Test(timeout = 5_000)
    public void protocolConfigInProgress_poolClosed_closesChannelAndDelegatePoolOnAcquireSuccess() throws InterruptedException {
        Promise<Channel> acquirePromise = eventLoopGroup.next().newPromise();
        when(mockDelegatePool.acquire()).thenReturn(acquirePromise);

        // initiate the configuration
        httpOrHttp2ChannelPool.acquire();

        // close the pool before the config can complete (we haven't completed acquirePromise yet)
        httpOrHttp2ChannelPool.close();

        Thread.sleep(500);

        Channel channel = new NioSocketChannel();
        eventLoopGroup.register(channel);
        try {
            channel.attr(PROTOCOL_FUTURE).set(CompletableFuture.completedFuture(Protocol.HTTP1_1));

            acquirePromise.setSuccess(channel);

            assertThat(channel.closeFuture().await().isDone()).isTrue();
            Thread.sleep(500);
            verify(mockDelegatePool).release(eq(channel));
            verify(mockDelegatePool).close();
        } finally {
            channel.close();
        }
    }

    @Test
    public void protocolConfigInProgress_poolClosed_delegatePoolClosedOnAcquireFailure() throws InterruptedException {
        Promise<Channel> acquirePromise = eventLoopGroup.next().newPromise();
        when(mockDelegatePool.acquire()).thenReturn(acquirePromise);

        // initiate the configuration
        httpOrHttp2ChannelPool.acquire();

        // close the pool before the config can complete (we haven't completed acquirePromise yet)
        httpOrHttp2ChannelPool.close();

        Thread.sleep(500);

        acquirePromise.setFailure(new RuntimeException("Some failure"));

        Thread.sleep(500);
        verify(mockDelegatePool).close();
    }

    @Test(timeout = 5_000)
    public void protocolConfigComplete_poolClosed_closesDelegatePool() throws InterruptedException {
        Promise<Channel> acquirePromise = eventLoopGroup.next().newPromise();
        when(mockDelegatePool.acquire()).thenReturn(acquirePromise);

        // initiate the configuration
        httpOrHttp2ChannelPool.acquire();

        Thread.sleep(500);

        Channel channel = new NioSocketChannel();
        eventLoopGroup.register(channel);
        try {
            channel.attr(PROTOCOL_FUTURE).set(CompletableFuture.completedFuture(Protocol.HTTP1_1));

            // this should complete the protocol config
            acquirePromise.setSuccess(channel);

            Thread.sleep(500);

            // close the pool
            httpOrHttp2ChannelPool.close();

            Thread.sleep(500);
            verify(mockDelegatePool).close();
        } finally {
            channel.close();
        }
    }

    @Test(timeout = 5_000)
    public void incompleteProtocolFutureDelaysMetricsDelegationAndForwardsFailures() throws InterruptedException {
        Promise<Channel> acquirePromise = eventLoopGroup.next().newPromise();
        when(mockDelegatePool.acquire()).thenReturn(acquirePromise);

        // startConnection
        httpOrHttp2ChannelPool.acquire();

        // query for metrics before the config can complete (we haven't completed acquirePromise yet)
        CompletableFuture<Void> metrics = httpOrHttp2ChannelPool.collectChannelPoolMetrics(MetricCollector.create("test"));

        Thread.sleep(500);

        assertThat(metrics.isDone()).isFalse();
        acquirePromise.setFailure(new RuntimeException("Some failure"));

        Thread.sleep(500);

        assertThat(metrics.isCompletedExceptionally()).isTrue();
    }

    @Test(timeout = 5_000)
    public void incompleteProtocolFutureDelaysMetricsDelegationAndForwardsSuccessForHttp1() throws Exception {
        incompleteProtocolFutureDelaysMetricsDelegationAndForwardsSuccessForProtocol(Protocol.HTTP1_1);
    }

    @Test(timeout = 5_000)
    public void incompleteProtocolFutureDelaysMetricsDelegationAndForwardsSuccessForHttp2() throws Exception {
        incompleteProtocolFutureDelaysMetricsDelegationAndForwardsSuccessForProtocol(Protocol.HTTP2);
    }

    public void incompleteProtocolFutureDelaysMetricsDelegationAndForwardsSuccessForProtocol(Protocol protocol) throws Exception {
        Promise<Channel> acquirePromise = eventLoopGroup.next().newPromise();
        when(mockDelegatePool.acquire()).thenReturn(acquirePromise);

        // startConnection
        httpOrHttp2ChannelPool.acquire();

        // query for metrics before the config can complete (we haven't completed acquirePromise yet)
        MetricCollector metricCollector = MetricCollector.create("foo");
        CompletableFuture<Void> metricsFuture = httpOrHttp2ChannelPool.collectChannelPoolMetrics(metricCollector);

        Thread.sleep(500);

        assertThat(metricsFuture.isDone()).isFalse();

        Channel channel = new MockChannel();
        eventLoopGroup.register(channel);
        channel.attr(PROTOCOL_FUTURE).set(CompletableFuture.completedFuture(protocol));
        acquirePromise.setSuccess(channel);

        metricsFuture.join();
        MetricCollection metrics = metricCollector.collect();

        assertThat(metrics.metricValues(HttpMetric.PENDING_CONCURRENCY_ACQUIRES).get(0)).isEqualTo(0);
        assertThat(metrics.metricValues(HttpMetric.MAX_CONCURRENCY).get(0)).isEqualTo(4);
        assertThat(metrics.metricValues(HttpMetric.AVAILABLE_CONCURRENCY).get(0)).isBetween(0, 1);
        assertThat(metrics.metricValues(HttpMetric.LEASED_CONCURRENCY).get(0)).isBetween(0, 1);
    }
}
