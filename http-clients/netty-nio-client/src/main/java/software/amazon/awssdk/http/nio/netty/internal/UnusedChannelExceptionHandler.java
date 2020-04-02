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
import io.netty.handler.timeout.TimeoutException;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Logger;

/**
 * A handler for exceptions occurring on channels not current in use (according to {@link ChannelAttributeKey#IN_USE}). This
 * does nothing if the channel is currently in use. If it's not currently in use, it will close the channel and log an
 * appropriate notification.
 *
 * This prevents spamming customer logs when errors (eg. connection resets) occur on an unused channel.
 */
@SdkInternalApi
@ChannelHandler.Sharable
public final class UnusedChannelExceptionHandler extends ChannelInboundHandlerAdapter {
    public static final UnusedChannelExceptionHandler INSTANCE = new UnusedChannelExceptionHandler();

    private static final Logger log = Logger.loggerFor(UnusedChannelExceptionHandler.class);

    private UnusedChannelExceptionHandler() {
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        boolean channelInUse = getAttribute(ctx.channel(), ChannelAttributeKey.IN_USE).orElse(false);

        if (channelInUse) {
            ctx.fireExceptionCaught(cause);
        } else {
            ctx.close();

            Optional<CompletableFuture<Void>> executeFuture = getAttribute(ctx.channel(), ChannelAttributeKey.EXECUTE_FUTURE_KEY);

            if (executeFuture.isPresent() && !executeFuture.get().isDone()) {
                log.error(() -> "An exception occurred on an channel (" + ctx.channel().id() + ") that was not in use, " +
                                "but was associated with a future that wasn't completed. This indicates a bug in the " +
                                "Java SDK, where a future was not completed while the channel was in use. The channel has " +
                                "been closed, and the future will be completed to prevent any ongoing issues.", cause);
                executeFuture.get().completeExceptionally(cause);
            } else if (isNettyIoException(cause) || hasNettyIoExceptionCause(cause)) {
                log.debug(() -> "An I/O exception (" + cause.getMessage() + ") occurred on a channel (" + ctx.channel().id() +
                                ") that was not in use. The channel has been closed. This is usually normal.");

            } else {
                log.warn(() -> "A non-I/O exception occurred on a channel (" + ctx.channel().id() + ") that was not in use. " +
                               "The channel has been closed to prevent any ongoing issues.", cause);
            }
        }
    }

    public static UnusedChannelExceptionHandler getInstance() {
        return INSTANCE;
    }

    private boolean isNettyIoException(Throwable cause) {
        return cause instanceof IOException || cause instanceof TimeoutException;
    }

    private boolean hasNettyIoExceptionCause(Throwable cause) {
        return cause.getCause() != null && isNettyIoException(cause.getCause());
    }
}
