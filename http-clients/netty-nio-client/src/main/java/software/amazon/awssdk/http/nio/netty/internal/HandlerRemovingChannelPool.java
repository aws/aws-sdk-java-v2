/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static software.amazon.awssdk.http.nio.netty.internal.utils.ChannelUtils.removeIfExists;

import com.typesafe.netty.http.HttpStreamsClientHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.pool.ChannelPool;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

/**
 * Removes any per request {@link ChannelHandler} from the pipeline prior to releasing
 * it to the pool.
 */
class HandlerRemovingChannelPool implements ChannelPool {

    private final ChannelPool delegate;

    HandlerRemovingChannelPool(ChannelPool delegate) {
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
        removeIfExists(channel.pipeline(),
                       HttpStreamsClientHandler.class,
                       ResponseHandler.class,
                       ReadTimeoutHandler.class,
                       WriteTimeoutHandler.class);
    }
}
