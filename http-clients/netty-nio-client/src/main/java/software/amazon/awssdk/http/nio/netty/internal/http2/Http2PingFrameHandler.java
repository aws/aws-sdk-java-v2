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

package software.amazon.awssdk.http.nio.netty.internal.http2;

import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.PING_TRACKER;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http2.Http2PingFrame;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.nio.netty.internal.utils.ChannelUtils;
import software.amazon.awssdk.utils.Logger;

@SdkInternalApi
@ChannelHandler.Sharable
public final class Http2PingFrameHandler extends SimpleChannelInboundHandler<Http2PingFrame> {
    private static final Logger log = Logger.loggerFor(Http2PingFrameHandler.class);
    private static final Http2PingFrameHandler INSTANCE = new Http2PingFrameHandler();

    private Http2PingFrameHandler() {
    }

    public static Http2PingFrameHandler create() {
        return INSTANCE;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http2PingFrame frame) {
        if (frame.ack()) {
            Channel channel = ctx.channel();
            log.debug(() -> "Received PING ACK from channel " + channel);
            ChannelUtils.getAttribute(channel, PING_TRACKER).ifPresent(tracker -> {
                tracker.cancel();
                channel.attr(PING_TRACKER).set(null);
            });
        } else {
            ctx.fireChannelRead(frame);
        }
    }

}