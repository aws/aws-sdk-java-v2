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
import static software.amazon.awssdk.http.nio.netty.internal.NettyConfiguration.HTTP2_CONNECTION_PING_TIMEOUT_SECONDS;
import static software.amazon.awssdk.http.nio.netty.internal.utils.NettyUtils.doInEventLoop;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http2.DefaultHttp2PingFrame;
import io.netty.handler.codec.http2.Http2PingFrame;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.WriteTimeoutException;
import io.netty.util.concurrent.ScheduledFuture;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Logger;

@ChannelHandler.Sharable
@SdkInternalApi
public final class Http2FrameExceptionHandler extends ChannelInboundHandlerAdapter {
    private static final Http2FrameExceptionHandler INSTANCE = new Http2FrameExceptionHandler();
    private static final Http2PingFrame DEFAULT_PING_FRAME = new DefaultHttp2PingFrame(0);
    private static final Logger log = Logger.loggerFor(Http2FrameExceptionHandler.class);

    private Http2FrameExceptionHandler() {
    }

    public static Http2FrameExceptionHandler create() {
        return INSTANCE;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if ((cause instanceof ReadTimeoutException || cause instanceof WriteTimeoutException) && ctx.channel().parent() != null) {
            Channel parent = ctx.channel().parent();
            doInEventLoop(parent.eventLoop(), () -> healthCheckIfNotAlready(parent));
        }

        ctx.fireExceptionCaught(cause);
    }

    private void healthCheckIfNotAlready(Channel parent) {

        // No need to do another health check if there is a PING in flight
        if (parent.attr(PING_TRACKER).get() != null) {
            return;
        }

        log.debug(() -> "Read/WriteTimeoutException occurred, sending PING frame to " + parent);
        Supplier<ScheduledFuture<?>> pingTimerFutureSupplier =  () ->
            parent.eventLoop().schedule(() -> timeoutHealthCheck(parent),
                                        HTTP2_CONNECTION_PING_TIMEOUT_SECONDS,
                                        TimeUnit.SECONDS);

        PingTracker pingTracker = new PingTracker(pingTimerFutureSupplier);
        parent.attr(PING_TRACKER).set(pingTracker);

        parent.writeAndFlush(DEFAULT_PING_FRAME).addListener(res -> {
            if (!res.isSuccess()) {
                log.debug(() -> "Failed to write and flush PING frame to connection", res.cause());

                pingTracker.cancel();
                closeH2Connection(parent, PingFailedException.PING_WRITE_FAILED_INSTANCE);
            }
        });
        pingTracker.start();
    }

    private void timeoutHealthCheck(Channel parentChannel) {
        log.debug(() -> String.format("Has not received PING ACK within %s seconds, closing the connection %s",
                                      HTTP2_CONNECTION_PING_TIMEOUT_SECONDS, parentChannel));
        closeH2Connection(parentChannel, PingFailedException.PING_NOT_ACK_INSTANCE);
    }

    private void closeH2Connection(Channel parent, PingFailedException exception) {
        parent.pipeline().fireExceptionCaught(exception);
    }

    static final class PingFailedException extends IOException {
        static final PingFailedException PING_WRITE_FAILED_INSTANCE =
            new PingFailedException("Failed to send PING to the service");

        static final PingFailedException PING_NOT_ACK_INSTANCE =
            new PingFailedException(String.format("Failed to receive PING ACK from the service within %s seconds",
                                                  HTTP2_CONNECTION_PING_TIMEOUT_SECONDS));

        private PingFailedException(String msg) {
            super(msg);
        }
    }
}
