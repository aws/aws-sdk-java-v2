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

package software.amazon.awssdk.http.nio.netty.internal.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.http.HttpMetric;
import software.amazon.awssdk.http.nio.netty.internal.MockChannel;
import software.amazon.awssdk.http.nio.netty.internal.SdkChannelPool;
import software.amazon.awssdk.http.nio.netty.internal.utils.BetterFixedChannelPool.AcquireTimeoutAction;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.utils.CompletableFutureUtils;

public class BetterFixedChannelPoolTest {
    private static EventLoopGroup eventLoopGroup;

    private BetterFixedChannelPool channelPool;
    private SdkChannelPool delegatePool;

    @BeforeClass
    public static void setupClass() {
        eventLoopGroup = new NioEventLoopGroup(1);
    }

    @AfterClass
    public static void teardownClass() throws InterruptedException {
        eventLoopGroup.shutdownGracefully().await();
    }

    @Before
    public void setup() {
        delegatePool = mock(SdkChannelPool.class);

        channelPool = BetterFixedChannelPool.builder()
                                            .channelPool(delegatePool)
                                            .maxConnections(2)
                                            .maxPendingAcquires(2)
                                            .acquireTimeoutAction(AcquireTimeoutAction.FAIL)
                                            .acquireTimeoutMillis(10_000)
                                            .executor(eventLoopGroup.next())
                                            .build();
    }

    @After
    public void teardown() {
        channelPool.close();
    }

    @Test
    public void delegateChannelPoolMetricFailureIsReported() {
        Throwable t = new Throwable();
        Mockito.when(delegatePool.collectChannelPoolMetrics(any())).thenReturn(CompletableFutureUtils.failedFuture(t));

        CompletableFuture<Void> result = channelPool.collectChannelPoolMetrics(MetricCollector.create("test"));
        waitForCompletion(result);
        assertThat(result).hasFailedWithThrowableThat().isEqualTo(t);
    }

    @Test(timeout = 5_000)
    public void metricCollectionHasCorrectValuesAfterAcquiresAndReleases() throws Exception {
        List<Promise<Channel>> acquirePromises = Collections.synchronizedList(new ArrayList<>());
        Mockito.when(delegatePool.acquire(isA(Promise.class))).thenAnswer(i -> {
            Promise<Channel> promise = eventLoopGroup.next().newPromise();
            acquirePromises.add(promise);
            return promise;
        });

        List<Promise<Channel>> releasePromises = Collections.synchronizedList(new ArrayList<>());
        Mockito.when(delegatePool.release(isA(Channel.class), isA(Promise.class))).thenAnswer(i -> {
            Promise promise = i.getArgumentAt(1, Promise.class);
            releasePromises.add(promise);
            return promise;
        });

        Mockito.when(delegatePool.collectChannelPoolMetrics(any())).thenReturn(CompletableFuture.completedFuture(null));

        assertConnectionsCheckedOutAndPending(0, 0);

        channelPool.acquire();
        completePromise(acquirePromises, 0);
        assertConnectionsCheckedOutAndPending(1, 0);

        channelPool.acquire();
        completePromise(acquirePromises, 1);
        assertConnectionsCheckedOutAndPending(2, 0);

        channelPool.acquire();
        assertConnectionsCheckedOutAndPending(2, 1);

        channelPool.acquire();
        assertConnectionsCheckedOutAndPending(2, 2);

        Future<Channel> f = channelPool.acquire();
        assertConnectionsCheckedOutAndPending(2, 2);
        assertThat(f.isSuccess()).isFalse();
        assertThat(f.cause()).isInstanceOf(IllegalStateException.class);

        channelPool.release(acquirePromises.get(1).getNow());
        assertConnectionsCheckedOutAndPending(2, 2);

        completePromise(releasePromises, 0);
        completePromise(acquirePromises, 2);
        assertConnectionsCheckedOutAndPending(2, 1);

        channelPool.release(acquirePromises.get(2).getNow());
        completePromise(releasePromises, 1);
        completePromise(acquirePromises, 3);
        assertConnectionsCheckedOutAndPending(2, 0);

        channelPool.release(acquirePromises.get(0).getNow());
        completePromise(releasePromises, 2);
        assertConnectionsCheckedOutAndPending(1, 0);

        channelPool.release(acquirePromises.get(3).getNow());
        completePromise(releasePromises, 3);
        assertConnectionsCheckedOutAndPending(0, 0);
    }

    private void completePromise(List<Promise<Channel>> promises, int promiseIndex) throws Exception {
        waitForPromise(promises, promiseIndex);

        MockChannel channel = new MockChannel();
        eventLoopGroup.next().register(channel);
        promises.get(promiseIndex).setSuccess(channel);
    }

    private void waitForPromise(List<Promise<Channel>> promises, int promiseIndex) throws Exception {
        while (promises.size() < promiseIndex + 1) {
            Thread.sleep(1);
        }
    }

    private void assertConnectionsCheckedOutAndPending(int checkedOut, int pending) {
        MetricCollector metricCollector = MetricCollector.create("foo");
        waitForCompletion(channelPool.collectChannelPoolMetrics(metricCollector));

        MetricCollection metrics = metricCollector.collect();

        assertThat(metrics.metricValues(HttpMetric.MAX_CONCURRENCY)).containsExactly(2);
        assertThat(metrics.metricValues(HttpMetric.LEASED_CONCURRENCY)).containsExactly(checkedOut);
        assertThat(metrics.metricValues(HttpMetric.PENDING_CONCURRENCY_ACQUIRES)).containsExactly(pending);
    }

    private void waitForCompletion(CompletableFuture<Void> future) {
        try {
            future.get(5, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            return;
        } catch (InterruptedException | TimeoutException e) {
            throw new Error(e);
        }
    }
}