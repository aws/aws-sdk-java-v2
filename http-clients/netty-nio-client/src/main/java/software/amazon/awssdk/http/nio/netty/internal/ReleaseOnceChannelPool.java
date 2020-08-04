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
import io.netty.channel.pool.FixedChannelPool;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.SucceededFuture;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.nio.netty.internal.http2.Http2MultiplexedChannelPool;
import software.amazon.awssdk.http.nio.netty.internal.utils.NettyUtils;
import software.amazon.awssdk.metrics.MetricCollector;

/**
 * Wrapper around a {@link ChannelPool} to protect it from having the same channel released twice. This can
 * cause issues in {@link FixedChannelPool} and {@link Http2MultiplexedChannelPool} which has a simple
 * mechanism to track leased connections.
 */
@SdkInternalApi
public class ReleaseOnceChannelPool implements SdkChannelPool {

    private static final AttributeKey<AtomicBoolean> IS_RELEASED = NettyUtils.getOrCreateAttributeKey(
            "software.amazon.awssdk.http.nio.netty.internal.http2.ReleaseOnceChannelPool.isReleased");

    private final SdkChannelPool delegate;

    public ReleaseOnceChannelPool(SdkChannelPool delegate) {
        this.delegate = delegate;
    }

    @Override
    public Future<Channel> acquire() {
        return delegate.acquire().addListener(onAcquire());
    }

    @Override
    public Future<Channel> acquire(Promise<Channel> promise) {
        return delegate.acquire(promise).addListener(onAcquire());
    }

    private GenericFutureListener<Future<Channel>> onAcquire() {
        return future -> {
            if (future.isSuccess()) {
                future.getNow().attr(IS_RELEASED).set(new AtomicBoolean(false));
            }
        };
    }

    @Override
    public Future<Void> release(Channel channel) {
        if (shouldRelease(channel)) {
            return delegate.release(channel);
        } else {
            return new SucceededFuture<>(channel.eventLoop(), null);
        }
    }

    @Override
    public Future<Void> release(Channel channel, Promise<Void> promise) {
        if (shouldRelease(channel)) {
            return delegate.release(channel, promise);
        } else {
            return promise.setSuccess(null);
        }
    }

    private boolean shouldRelease(Channel channel) {
        // IS_RELEASED may be null if this channel was not acquired by this pool. This can happen
        // for HTTP/2 when we release the parent socket channel
        return channel.attr(IS_RELEASED).get() == null
               || channel.attr(IS_RELEASED).get().compareAndSet(false, true);
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public CompletableFuture<Void> collectChannelPoolMetrics(MetricCollector metrics) {
        return delegate.collectChannelPoolMetrics(metrics);
    }
}
