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

import static software.amazon.awssdk.http.nio.netty.internal.utils.ChannelUtils.getAttribute;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.ssl.SslCloseCompletionEvent;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Handles {@link SslCloseCompletionEvent}s that are sent whenever an SSL channel
 * goes inactive. This most commonly occurs on a tls_close sent by the server. Channels
 * in this state can't be reused so they must be closed.
 */
@SdkInternalApi
@ChannelHandler.Sharable
public final class SslCloseCompletionEventHandler extends ChannelInboundHandlerAdapter {

    private static final SslCloseCompletionEventHandler INSTANCE = new SslCloseCompletionEventHandler();

    private SslCloseCompletionEventHandler() {
    }

    /**
     * {@inheritDoc}
     *
     * Close the channel if the event is {@link SslCloseCompletionEvent} and the channel is unused.
     * If the channel is being used, it will be closed in {@link ResponseHandler}
     *
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        boolean channelInUse = getAttribute(ctx.channel(), ChannelAttributeKey.IN_USE).orElse(false);

        if (!channelInUse && evt instanceof SslCloseCompletionEvent) {
            ctx.close();
        } else {
            ctx.fireUserEventTriggered(evt);
        }
    }

    public static SslCloseCompletionEventHandler getInstance() {
        return INSTANCE;
    }
}
