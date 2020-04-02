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

import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.LAST_HTTP_CONTENT_RECEIVED_KEY;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.LastHttpContent;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Logger;

/**
 * Marks {@code ChannelAttributeKey.LAST_HTTP_CONTENT_RECEIVED_KEY} if {@link LastHttpContent} is received.
 */
@SdkInternalApi
@ChannelHandler.Sharable
public final class LastHttpContentHandler extends ChannelInboundHandlerAdapter {
    private static final LastHttpContentHandler INSTANCE = new LastHttpContentHandler();
    private static final Logger logger = Logger.loggerFor(LastHttpContent.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof LastHttpContent) {
            logger.debug(() -> "Received LastHttpContent " + ctx.channel());
            ctx.channel().attr(LAST_HTTP_CONTENT_RECEIVED_KEY).set(true);
        }

        ctx.fireChannelRead(msg);
    }

    public static LastHttpContentHandler create() {
        return INSTANCE;
    }
}
