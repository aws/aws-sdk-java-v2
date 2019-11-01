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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http2.Http2GoAwayFrame;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey;

/**
 * Handles {@link Http2GoAwayFrame}s sent on a connection. This will pass the frame along to the connection's 
 * {@link MultiplexedChannelRecord#goAway(Http2GoAwayFrame)} method.
 */
@SdkInternalApi
public class Http2GoAwayFrameHandler extends SimpleChannelInboundHandler<Http2GoAwayFrame> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http2GoAwayFrame frame)
    {
        MultiplexedChannelRecord parentConnection = ctx.channel().attr(ChannelAttributeKey.CHANNEL_POOL_RECORD).get();
        if (parentConnection != null) {
            parentConnection.goAway(frame);
        }
    }
}
