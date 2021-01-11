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

import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelPool;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.metrics.MetricCollector;

/**
 * Simple decorator {@link ChannelPool} that attempts to complete the promise
 * given to {@link #acquire(Promise)} with the channel acquired from the underlying
 * pool. If it fails (because the promise is already done), the acquired channel
 * is closed then released back to the delegate.
 */
@SdkInternalApi
public final class CancellableAcquireChannelPool implements SdkChannelPool {
    private final EventExecutor executor;
    private final SdkChannelPool delegatePool;

    public CancellableAcquireChannelPool(EventExecutor executor, SdkChannelPool delegatePool) {
        this.executor = executor;
        this.delegatePool = delegatePool;
    }

    @Override
    public Future<Channel> acquire() {
        return this.acquire(executor.newPromise());
    }

    @Override
    public Future<Channel> acquire(Promise<Channel> acquirePromise) {
        Future<Channel> channelFuture = delegatePool.acquire(executor.newPromise());
        channelFuture.addListener((Future<Channel> f) -> {
            if (f.isSuccess()) {
                Channel ch = f.getNow();
                if (!acquirePromise.trySuccess(ch)) {
                    ch.close().addListener(closeFuture -> delegatePool.release(ch));
                }
            } else {
                acquirePromise.tryFailure(f.cause());
            }
        });
        return acquirePromise;
    }

    @Override
    public Future<Void> release(Channel channel) {
        return delegatePool.release(channel);
    }

    @Override
    public Future<Void> release(Channel channel, Promise<Void> promise) {
        return delegatePool.release(channel, promise);
    }

    @Override
    public void close() {
        delegatePool.close();
    }

    @Override
    public CompletableFuture<Void> collectChannelPoolMetrics(MetricCollector metrics) {
        return delegatePool.collectChannelPoolMetrics(metrics);
    }
}
