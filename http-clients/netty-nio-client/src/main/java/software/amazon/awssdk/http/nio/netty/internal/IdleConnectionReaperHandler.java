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

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Logger;

/**
 * A handler that closes unused channels that have not had any traffic on them for a configurable amount of time.
 */
@SdkInternalApi
public class IdleConnectionReaperHandler extends IdleStateHandler {
    private static final Logger log = Logger.loggerFor(IdleConnectionReaperHandler.class);
    private final int maxIdleTimeMillis;

    public IdleConnectionReaperHandler(int maxIdleTimeMillis) {
        super(0, 0, maxIdleTimeMillis, TimeUnit.MILLISECONDS);
        this.maxIdleTimeMillis = maxIdleTimeMillis;
    }

    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent event) {
        assert ctx.channel().eventLoop().inEventLoop();

        boolean channelNotInUse = Boolean.FALSE.equals(ctx.channel().attr(ChannelAttributeKey.IN_USE).get());

        if (channelNotInUse && ctx.channel().isOpen()) {
            log.debug(() -> "Closing unused connection (" + ctx.channel().id() + ") because it has been idle for longer than " +
                            maxIdleTimeMillis + " milliseconds.");
            ctx.close();
        }
    }
}
