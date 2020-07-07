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

import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.IN_USE;
import static software.amazon.awssdk.http.nio.netty.internal.utils.ChannelUtils.removeIfExists;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.nio.netty.internal.http2.FlushOnReadHandler;
import software.amazon.awssdk.http.nio.netty.internal.nrs.HttpStreamsClientHandler;
import software.amazon.awssdk.metrics.MetricCollector;

/**
 * Removes any per request {@link ChannelHandler} from the pipeline prior to releasing
 * it to the pool.
 */
@SdkInternalApi
public class HandlerRemovingChannelPool implements SdkChannelPool {

    private final SdkChannelPool delegate;

    public HandlerRemovingChannelPool(SdkChannelPool delegate) {
        this.delegate = delegate;
    }

    @Override
    public Future<Channel> acquire() {
        return delegate.acquire();
    }

    @Override
    public Future<Channel> acquire(Promise<Channel> promise) {
        return delegate.acquire(promise);
    }

    @Override
    public Future<Void> release(Channel channel) {
        removePerRequestHandlers(channel);
        return delegate.release(channel);
    }

    @Override
    public Future<Void> release(Channel channel, Promise<Void> promise) {
        removePerRequestHandlers(channel);
        return delegate.release(channel, promise);
    }

    @Override
    public void close() {
        delegate.close();
    }

    private void removePerRequestHandlers(Channel channel) {
        channel.attr(IN_USE).set(false);

        // Only remove per request handler if the channel is registered
        // or open since DefaultChannelPipeline would remove handlers if
        // channel is closed and unregistered
        // See DefaultChannelPipeline.java#L1403
        if (channel.isOpen() || channel.isRegistered()) {
            removeIfExists(channel.pipeline(),
                           HttpStreamsClientHandler.class,
                           LastHttpContentHandler.class,
                           FlushOnReadHandler.class,
                           ResponseHandler.class,
                           ReadTimeoutHandler.class,
                           WriteTimeoutHandler.class);
        }
    }

    @Override
    public CompletableFuture<Void> collectChannelPoolMetrics(MetricCollector metrics) {
        return delegate.collectChannelPoolMetrics(metrics);
    }
}
