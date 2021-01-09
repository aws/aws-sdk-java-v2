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

package software.amazon.awssdk.http.nio.netty.internal.http2;

import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.MAX_CONCURRENT_STREAMS;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.PROTOCOL_FUTURE;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.pool.ChannelPool;
import io.netty.handler.codec.http2.Http2SettingsFrame;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.Protocol;

/**
 * Configure channel based on the {@link Http2SettingsFrame} received from server
 */
@SdkInternalApi
public final class Http2SettingsFrameHandler extends SimpleChannelInboundHandler<Http2SettingsFrame> {

    private Channel channel;
    private final long clientMaxStreams;
    private AtomicReference<ChannelPool> channelPoolRef;

    public Http2SettingsFrameHandler(Channel channel, long clientMaxStreams, AtomicReference<ChannelPool> channelPoolRef) {
        this.channel = channel;
        this.clientMaxStreams = clientMaxStreams;
        this.channelPoolRef = channelPoolRef;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http2SettingsFrame msg) {
        Long serverMaxStreams = Optional.ofNullable(msg.settings().maxConcurrentStreams()).orElse(Long.MAX_VALUE);
        channel.attr(MAX_CONCURRENT_STREAMS).set(Math.min(clientMaxStreams, serverMaxStreams));
        channel.attr(PROTOCOL_FUTURE).get().complete(Protocol.HTTP2);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) {
        if (!channel.attr(PROTOCOL_FUTURE).get().isDone()) {
            channelError(new IOException("The channel was closed before the protocol could be determined."), channel, ctx);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        channelError(cause, channel, ctx);
    }

    private void channelError(Throwable cause, Channel ch, ChannelHandlerContext ctx) {
        ch.attr(PROTOCOL_FUTURE).get().completeExceptionally(cause);
        ctx.fireExceptionCaught(cause);

        // Channel status may still be active at this point even if it's not so queue up the close so that status is
        // accurately updated
        ch.eventLoop().submit(() -> {
            try {
                if (ch.isActive()) {
                    ch.close();
                }
            } finally {
                channelPoolRef.get().release(ch);
            }
        });
    }
}
