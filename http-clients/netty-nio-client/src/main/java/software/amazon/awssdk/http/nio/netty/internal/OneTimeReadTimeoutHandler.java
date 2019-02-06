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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.ReferenceCountUtil;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Handler to add an one-time {@link ReadTimeoutHandler} to the pipeline and remove it afterwards.
 */
@SdkInternalApi
public final class OneTimeReadTimeoutHandler extends SimpleChannelInboundHandler {

    private static final String READ_TIMEOUT_HANDLER_NAME = "RemoveAfterReadTimeoutHandler";
    private final long readTimeoutMillis;
    private final TimeUnit timeUnit;

    OneTimeReadTimeoutHandler(long readTimeoutMillis, TimeUnit timeUnit) {
        this.readTimeoutMillis = readTimeoutMillis;
        this.timeUnit = timeUnit;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) {
        ReferenceCountUtil.retain(msg);
        ctx.pipeline().addFirst(READ_TIMEOUT_HANDLER_NAME, new ReadTimeoutHandler(readTimeoutMillis, timeUnit));
        ctx.fireChannelRead(msg);

        ctx.pipeline().remove(READ_TIMEOUT_HANDLER_NAME);
        ctx.pipeline().remove(this);
    }
}
