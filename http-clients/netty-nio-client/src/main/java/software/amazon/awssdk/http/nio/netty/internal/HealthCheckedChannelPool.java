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

import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.KEEP_ALIVE;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.pool.ChannelPool;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.ScheduledFuture;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.metrics.MetricCollector;

/**
 * An implementation of {@link ChannelPool} that validates the health of its connections.
 *
 * This wraps another {@code ChannelPool}, and verifies:
 * <ol>
 * <li>All connections acquired from the underlying channel pool are in the active state.</li>
 * <li>All connections released into the underlying pool that are not active, are closed before they are released.</li>
 * </ol>
 *
 * Acquisitions that fail due to an unhealthy underlying channel are retried until a healthy channel can be returned, or the
 * {@link NettyConfiguration#connectionAcquireTimeoutMillis()} timeout is reached.
 */
@SdkInternalApi
public class HealthCheckedChannelPool implements SdkChannelPool {
    private final EventLoopGroup eventLoopGroup;
    private final int acquireTimeoutMillis;
    private final SdkChannelPool delegate;

    public HealthCheckedChannelPool(EventLoopGroup eventLoopGroup,
                                    NettyConfiguration configuration,
                                    SdkChannelPool delegate) {
        this.eventLoopGroup = eventLoopGroup;
        this.acquireTimeoutMillis = configuration.connectionAcquireTimeoutMillis();
        this.delegate = delegate;
    }

    @Override
    public Future<Channel> acquire() {
        return acquire(eventLoopGroup.next().newPromise());
    }

    @Override
    public Future<Channel> acquire(Promise<Channel> resultFuture) {
        // Schedule a task to time out this acquisition, in case we can't acquire a channel fast enough.
        ScheduledFuture<?> timeoutFuture =
                eventLoopGroup.schedule(() -> timeoutAcquire(resultFuture), acquireTimeoutMillis, TimeUnit.MILLISECONDS);

        tryAcquire(resultFuture, timeoutFuture);
        return resultFuture;
    }

    /**
     * Time out the provided acquire future, if it hasn't already been completed.
     */
    private void timeoutAcquire(Promise<Channel> resultFuture) {
        resultFuture.tryFailure(new TimeoutException("Acquire operation took longer than " + acquireTimeoutMillis +
                                                     " milliseconds."));
    }

    /**
     * Try to acquire a channel from the underlying pool. This will keep retrying the acquisition until the provided result
     * future is completed.
     *
     * @param resultFuture The future that should be completed with the acquired channel. If this is completed external to this
     * function, this function will stop trying to acquire a channel.
     * @param timeoutFuture The future for the timeout task. This future will be cancelled when a channel is acquired.
     */
    private void tryAcquire(Promise<Channel> resultFuture, ScheduledFuture<?> timeoutFuture) {
        // Something else completed the future (probably a timeout). Stop trying to get a channel.
        if (resultFuture.isDone()) {
            return;
        }

        Promise<Channel> delegateFuture = eventLoopGroup.next().newPromise();
        delegate.acquire(delegateFuture);
        delegateFuture.addListener(f -> ensureAcquiredChannelIsHealthy(delegateFuture, resultFuture, timeoutFuture));
    }

    /**
     * Validate that the channel returned by the underlying channel pool is healthy. If so, complete the result future with the
     * channel returned by the underlying pool. If not, close the channel and try to get a different one.
     *
     * @param delegateFuture A completed promise as a result of invoking delegate.acquire().
     * @param resultFuture The future that should be completed with the healthy, acquired channel.
     * @param timeoutFuture The future for the timeout task. This future will be cancelled when a channel is acquired.
     */
    private void ensureAcquiredChannelIsHealthy(Promise<Channel> delegateFuture,
                                                Promise<Channel> resultFuture,
                                                ScheduledFuture<?> timeoutFuture) {
        // If our delegate failed to connect, forward down the failure. Don't try again.
        if (!delegateFuture.isSuccess()) {
            timeoutFuture.cancel(false);
            resultFuture.tryFailure(delegateFuture.cause());
            return;
        }

        // If our delegate gave us an unhealthy connection, close it and try to get a new one.
        Channel channel = delegateFuture.getNow();
        if (!isHealthy(channel)) {
            channel.close();
            delegate.release(channel);
            tryAcquire(resultFuture, timeoutFuture);
            return;
        }

        // Cancel the timeout (best effort), and return back the healthy channel.
        timeoutFuture.cancel(false);
        if (!resultFuture.trySuccess(channel)) {
            // If we couldn't give the channel to the result future (because it failed for some other reason),
            // just return it to the pool.
            release(channel);
        }
    }

    @Override
    public Future<Void> release(Channel channel) {
        closeIfUnhealthy(channel);
        return delegate.release(channel);
    }

    @Override
    public Future<Void> release(Channel channel, Promise<Void> promise) {
        closeIfUnhealthy(channel);
        return delegate.release(channel, promise);
    }

    @Override
    public void close() {
        delegate.close();
    }

    /**
     * Close the provided channel, if it's considered unhealthy.
     */
    private void closeIfUnhealthy(Channel channel) {
        if (!isHealthy(channel)) {
            channel.close();
        }
    }

    /**
     * Determine whether the provided channel is 'healthy' enough to use.
     */
    private boolean isHealthy(Channel channel) {
        // There might be cases where the channel is not reusable but still active at the moment
        // See https://github.com/aws/aws-sdk-java-v2/issues/1380
        if (channel.attr(KEEP_ALIVE).get() != null && !channel.attr(KEEP_ALIVE).get()) {
            return false;
        }

        return channel.isActive();
    }

    @Override
    public CompletableFuture<Void> collectChannelPoolMetrics(MetricCollector metrics) {
        return delegate.collectChannelPoolMetrics(metrics);
    }
}
