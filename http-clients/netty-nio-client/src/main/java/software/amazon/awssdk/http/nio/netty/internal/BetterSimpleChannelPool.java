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

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.pool.SimpleChannelPool;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Extension of {@link SimpleChannelPool} to add an asynchronous close method
 * and to allow multiple {@link ChannelPoolHandler}s to be registered.
 */
@SdkInternalApi
public final class BetterSimpleChannelPool extends SimpleChannelPool {
    private final CompletableFuture<Boolean> closeFuture;

    BetterSimpleChannelPool(Bootstrap bootstrap, List<ChannelPoolHandler> handlers) {
        super(bootstrap, combine(handlers));
        closeFuture = new CompletableFuture<>();
    }

    @Override
    public void close() {
        super.close();
        closeFuture.complete(true);
    }

    CompletableFuture<Boolean> closeFuture() {
        return closeFuture;
    }

    /**
     * {@link SimpleChannelPool}'s constructors only accept a single {@link ChannelPoolHandler}.
     * This method combines multiple provided handlers into a single handler.
     */
    private static ChannelPoolHandler combine(List<ChannelPoolHandler> handlers) {
        return new ChannelPoolHandler() {
            @Override
            public void channelCreated(Channel ch) throws Exception {
                for (ChannelPoolHandler handler : handlers) {
                    handler.channelCreated(ch);
                }
            }

            @Override
            public void channelAcquired(Channel ch) throws Exception {
                for (ChannelPoolHandler handler : handlers) {
                    handler.channelAcquired(ch);
                }
            }

            @Override
            public void channelReleased(Channel ch) throws Exception {
                for (ChannelPoolHandler handler : handlers) {
                    handler.channelReleased(ch);
                }
            }
        };
    }
}