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

package software.amazon.awssdk.http.nio.netty.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.ChannelPool;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import software.amazon.awssdk.http.HttpMetric;
import software.amazon.awssdk.metrics.MetricCollector;

public class IdleConnectionCountingChannelPoolTest {
    private EventLoopGroup eventLoopGroup;
    private ChannelPool delegatePool;
    private IdleConnectionCountingChannelPool idleCountingPool;

    @Before
    public void setup() {
        delegatePool = mock(ChannelPool.class);
        eventLoopGroup = new NioEventLoopGroup(4);
        idleCountingPool = new IdleConnectionCountingChannelPool(eventLoopGroup.next(), delegatePool);
    }

    @After
    public void teardown() {
        eventLoopGroup.shutdownGracefully();
    }

    @Test(timeout = 5_000)
    public void acquiresAndReleasesOfNewChannelsIncreaseCount() throws InterruptedException {
        stubDelegatePoolAcquires(createSuccessfulAcquire(), createSuccessfulAcquire());
        stubDelegatePoolReleasesForSuccess();

        assertThat(getIdleConnectionCount()).isEqualTo(0);

        Channel firstChannel = idleCountingPool.acquire().await().getNow();
        assertThat(getIdleConnectionCount()).isEqualTo(0);

        Channel secondChannel = idleCountingPool.acquire().await().getNow();
        assertThat(getIdleConnectionCount()).isEqualTo(0);

        idleCountingPool.release(firstChannel).await();
        assertThat(getIdleConnectionCount()).isEqualTo(1);

        idleCountingPool.release(secondChannel).await();
        assertThat(getIdleConnectionCount()).isEqualTo(2);
    }

    @Test(timeout = 5_000)
    public void channelsClosedInTheDelegatePoolAreNotCounted() throws InterruptedException {
        stubDelegatePoolAcquires(createSuccessfulAcquire());
        stubDelegatePoolReleasesForSuccess();

        assertThat(getIdleConnectionCount()).isEqualTo(0);

        Channel channel = idleCountingPool.acquire().await().getNow();
        assertThat(getIdleConnectionCount()).isEqualTo(0);

        idleCountingPool.release(channel).await();
        assertThat(getIdleConnectionCount()).isEqualTo(1);

        channel.close().await();
        assertThat(getIdleConnectionCount()).isEqualTo(0);
    }

    @Test(timeout = 5_000)
    public void channelsClosedWhenCheckedOutAreNotCounted() throws InterruptedException {
        stubDelegatePoolAcquires(createSuccessfulAcquire());
        stubDelegatePoolReleasesForSuccess();

        assertThat(getIdleConnectionCount()).isEqualTo(0);

        Channel channel = idleCountingPool.acquire().await().getNow();
        assertThat(getIdleConnectionCount()).isEqualTo(0);

        channel.close().await();
        assertThat(getIdleConnectionCount()).isEqualTo(0);

        idleCountingPool.release(channel).await();
        assertThat(getIdleConnectionCount()).isEqualTo(0);
    }

    @Test
    public void checkingOutAnIdleChannelIsCountedCorrectly() throws InterruptedException {
        Future<Channel> successfulAcquire = createSuccessfulAcquire();
        stubDelegatePoolAcquires(successfulAcquire, successfulAcquire);
        stubDelegatePoolReleasesForSuccess();

        assertThat(getIdleConnectionCount()).isEqualTo(0);

        Channel channel1 = idleCountingPool.acquire().await().getNow();
        assertThat(getIdleConnectionCount()).isEqualTo(0);

        idleCountingPool.release(channel1).await();
        assertThat(getIdleConnectionCount()).isEqualTo(1);

        Channel channel2 = idleCountingPool.acquire().await().getNow();
        assertThat(getIdleConnectionCount()).isEqualTo(0);
        assertThat(channel1).isEqualTo(channel2);
    }

    @Test
    public void stochastic_rapidAcquireReleaseIsCalculatedCorrectly() throws InterruptedException {
        Future<Channel> successfulAcquire = createSuccessfulAcquire();
        Channel expectedChannel = successfulAcquire.getNow();
        stubDelegatePoolAcquires(successfulAcquire);
        stubDelegatePoolReleasesForSuccess();

        for (int i = 0; i < 1000; ++i) {
            Channel channel = idleCountingPool.acquire().await().getNow();
            assertThat(channel).isEqualTo(expectedChannel);
            assertThat(getIdleConnectionCount()).isEqualTo(0);
            idleCountingPool.release(channel).await();
            assertThat(getIdleConnectionCount()).isEqualTo(1);
        }
    }

    @Test
    public void stochastic_rapidAcquireReleaseCloseIsCalculatedCorrectly() throws InterruptedException {
        stubDelegatePoolAcquiresForSuccess();
        stubDelegatePoolReleasesForSuccess();

        for (int i = 0; i < 1000; ++i) {
            Channel channel = idleCountingPool.acquire().await().getNow();
            assertThat(getIdleConnectionCount()).isEqualTo(0);
            idleCountingPool.release(channel).await();
            assertThat(getIdleConnectionCount()).isEqualTo(1);
            channel.close().await();
            assertThat(getIdleConnectionCount()).isEqualTo(0);
        }
    }

    @Test
    public void stochastic_rapidAcquireCloseReleaseIsCalculatedCorrectly() throws InterruptedException {
        stubDelegatePoolAcquiresForSuccess();
        stubDelegatePoolReleasesForSuccess();

        for (int i = 0; i < 1000; ++i) {
            Channel channel = idleCountingPool.acquire().await().getNow();
            assertThat(getIdleConnectionCount()).isEqualTo(0);
            channel.close().await();
            assertThat(getIdleConnectionCount()).isEqualTo(0);
            idleCountingPool.release(channel).await();
            assertThat(getIdleConnectionCount()).isEqualTo(0);
        }
    }

    private int getIdleConnectionCount() {
        MetricCollector metricCollector = MetricCollector.create("test");
        idleCountingPool.collectChannelPoolMetrics(metricCollector).join();
        return metricCollector.collect().metricValues(HttpMetric.AVAILABLE_CONCURRENCY).get(0);
    }

    @SafeVarargs
    private final void stubDelegatePoolAcquires(Future<Channel> result, Future<Channel>... extraResults) {
        Mockito.when(delegatePool.acquire(any())).thenReturn(result, extraResults);
    }

    private void stubDelegatePoolAcquiresForSuccess() {
        Mockito.when(delegatePool.acquire(any())).thenAnswer(a -> createSuccessfulAcquire());
    }

    private void stubDelegatePoolReleasesForSuccess() {
        Mockito.when(delegatePool.release(any())).thenAnswer((Answer<Future<Channel>>) invocation -> {
            Channel channel = invocation.getArgumentAt(0, Channel.class);
            Promise<Channel> result = channel.eventLoop().newPromise();
            return result.setSuccess(channel);
        });
    }

    private Future<Channel> createSuccessfulAcquire() {
        try {
            EventLoop eventLoop = this.eventLoopGroup.next();

            Promise<Channel> channelPromise = eventLoop.newPromise();
            MockChannel channel = new MockChannel();
            eventLoop.register(channel);
            channelPromise.setSuccess(channel);

            return channelPromise;
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}