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

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.LastHttpContent;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Simple handler that swallows the next read object if it is an instance of
 * {@code LastHttpContent}, then removes itself from the pipeline.
 * <p>
 * This handler is sharable.
 */
@SdkInternalApi
@ChannelHandler.Sharable
final class LastHttpContentSwallower extends SimpleChannelInboundHandler<HttpObject> {
    private static final LastHttpContentSwallower INSTANCE = new LastHttpContentSwallower();

    private LastHttpContentSwallower() {
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject obj) {
        if (obj instanceof LastHttpContent) {
            // Queue another read to make up for the one we just ignored
            ctx.read();
        } else {
            ctx.fireChannelRead(obj);
        }
        // Remove self from pipeline since we only care about potentially
        // ignoring the very first message
        ctx.pipeline().remove(this);
    }

    public static LastHttpContentSwallower getInstance() {
        return INSTANCE;
    }
}
