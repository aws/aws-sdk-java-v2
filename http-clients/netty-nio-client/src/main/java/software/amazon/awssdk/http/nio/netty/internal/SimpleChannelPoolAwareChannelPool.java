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

package software.amazon.awssdk.http.nio.netty.internal;

import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelPool;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
final class SimpleChannelPoolAwareChannelPool implements ChannelPool {
    private final ChannelPool delegate;
    private final BetterSimpleChannelPool simpleChannelPool;

    SimpleChannelPoolAwareChannelPool(ChannelPool delegate, BetterSimpleChannelPool simpleChannelPool) {
        this.delegate = delegate;
        this.simpleChannelPool = simpleChannelPool;
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
        return delegate.release(channel);
    }

    @Override
    public Future<Void> release(Channel channel, Promise<Void> promise) {
        return delegate.release(channel, promise);
    }

    @Override
    public void close() {
        delegate.close();
    }

    public BetterSimpleChannelPool underlyingSimpleChannelPool() {
        return simpleChannelPool;
    }

}
