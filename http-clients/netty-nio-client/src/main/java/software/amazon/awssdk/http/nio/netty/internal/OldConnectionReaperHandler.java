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

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * A handler that will close channels after they have reached their time-to-live, regardless of usage.
 *
 * Channels that are not in use will be closed immediately, and channels that are in use will be closed when they are next
 * released to the underlying connection pool (via {@link ChannelAttributeKey#CLOSE_ON_RELEASE}).
 */
@SdkInternalApi
public class OldConnectionReaperHandler extends ChannelDuplexHandler {
    private static final Logger log = Logger.loggerFor(OldConnectionReaperHandler.class);
    private final int connectionTtlMillis;

    private ScheduledFuture<?> channelKiller;

    public OldConnectionReaperHandler(int connectionTtlMillis) {
        Validate.isPositive(connectionTtlMillis, "connectionTtlMillis");
        this.connectionTtlMillis = connectionTtlMillis;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        initialize(ctx);
        super.handlerAdded(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        initialize(ctx);
        super.channelActive(ctx);
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        initialize(ctx);
        super.channelRegistered(ctx);
    }

    private void initialize(ChannelHandlerContext ctx) {
        if (channelKiller == null) {
            channelKiller = ctx.channel().eventLoop().schedule(() -> closeChannel(ctx),
                                                               connectionTtlMillis,
                                                               TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        destroy();
    }

    private void destroy() {
        if (channelKiller != null) {
            channelKiller.cancel(false);
            channelKiller = null;
        }
    }

    private void closeChannel(ChannelHandlerContext ctx) {
        assert ctx.channel().eventLoop().inEventLoop();

        if (ctx.channel().isOpen()) {
            if (Boolean.FALSE.equals(ctx.channel().attr(ChannelAttributeKey.IN_USE).get())) {
                log.debug(() -> "Closing unused connection (" + ctx.channel().id() + ") because it has reached its maximum " +
                                "time to live of " + connectionTtlMillis + " milliseconds.");
                ctx.close();
            } else {
                log.debug(() -> "Connection (" + ctx.channel().id() + ") will be closed during its next release, because it " +
                                "has reached its maximum time to live of " + connectionTtlMillis + " milliseconds.");
                ctx.channel().attr(ChannelAttributeKey.CLOSE_ON_RELEASE).set(true);
            }
        }

        channelKiller = null;
    }
}
