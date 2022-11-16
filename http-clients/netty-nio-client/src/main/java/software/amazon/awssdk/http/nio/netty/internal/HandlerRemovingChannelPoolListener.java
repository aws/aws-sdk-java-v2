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

import static software.amazon.awssdk.http.nio.netty.internal.utils.ChannelUtils.removeIfExists;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.nio.netty.internal.ListenerInvokingChannelPool.ChannelPoolListener;
import software.amazon.awssdk.http.nio.netty.internal.http2.FlushOnReadHandler;
import software.amazon.awssdk.http.nio.netty.internal.nrs.HttpStreamsClientHandler;

/**
 * Removes any per-request {@link ChannelHandler} from the pipeline when releasing it to the pool.
 */
@SdkInternalApi
public final class HandlerRemovingChannelPoolListener implements ChannelPoolListener {

    private static final HandlerRemovingChannelPoolListener INSTANCE = new HandlerRemovingChannelPoolListener();

    private HandlerRemovingChannelPoolListener() {
    }

    public static HandlerRemovingChannelPoolListener create() {
        return INSTANCE;
    }

    @Override
    public void channelReleased(Channel channel) {
        // Only remove per request handler if the channel is registered
        // or open since DefaultChannelPipeline would remove handlers if
        // channel is closed and unregistered
        if (channel.isOpen() || channel.isRegistered()) {
            removeIfExists(channel.pipeline(),
                           HttpStreamsClientHandler.class,
                           FlushOnReadHandler.class,
                           ResponseHandler.class,
                           ReadTimeoutHandler.class,
                           WriteTimeoutHandler.class);
        }
    }
}
