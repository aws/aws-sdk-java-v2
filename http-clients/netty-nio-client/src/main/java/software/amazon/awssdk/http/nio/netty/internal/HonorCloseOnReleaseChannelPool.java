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

import static software.amazon.awssdk.http.nio.netty.internal.utils.NettyUtils.doInEventLoop;

import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelPool;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Logger;

/**
 * Wrap a channel pool so that {@link ChannelAttributeKey#CLOSE_ON_RELEASE} is honored when a channel is released to the
 * underlying pool.
 *
 * When a channel is released and {@link ChannelAttributeKey#CLOSE_ON_RELEASE} is true on the channel, the channel will be closed
 * before it is released to the underlying pool.
 */
@SdkInternalApi
public class HonorCloseOnReleaseChannelPool implements ChannelPool {
    private static final Logger log = Logger.loggerFor(HonorCloseOnReleaseChannelPool.class);
    private final ChannelPool delegatePool;

    public HonorCloseOnReleaseChannelPool(ChannelPool delegatePool) {
        this.delegatePool = delegatePool;
    }

    @Override
    public Future<Channel> acquire() {
        return delegatePool.acquire();
    }

    @Override
    public Future<Channel> acquire(Promise<Channel> promise) {
        return delegatePool.acquire(promise);
    }

    @Override
    public Future<Void> release(Channel channel) {
        return release(channel, channel.eventLoop().newPromise());
    }

    @Override
    public Future<Void> release(Channel channel, Promise<Void> promise) {
        doInEventLoop(channel.eventLoop(), () -> {
            boolean shouldCloseOnRelease = Boolean.TRUE.equals(channel.attr(ChannelAttributeKey.CLOSE_ON_RELEASE).get());

            if (shouldCloseOnRelease && channel.isOpen() && !channel.eventLoop().isShuttingDown()) {
                log.debug(() -> "Closing connection (" + channel.id() + "), instead of releasing it.");
                channel.close();
            }

            delegatePool.release(channel, promise);
        });
        return promise;
    }

    @Override
    public void close() {
        delegatePool.close();
    }
}
