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

package software.amazon.awssdk.http.nio.netty.internal.http2;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http2.DefaultHttp2PingFrame;
import io.netty.handler.codec.http2.Http2PingFrame;
import io.netty.util.concurrent.ScheduledFuture;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey;
import software.amazon.awssdk.http.nio.netty.internal.utils.NettyClientLogger;
import software.amazon.awssdk.utils.Validate;

/**
 * Attached to a {@link Channel} to periodically check the health of HTTP2 connections via PING frames.
 *
 * If a channel is found to be unhealthy, this will invoke {@link ChannelPipeline#fireExceptionCaught(Throwable)}.
 */
@SdkInternalApi
public class Http2PingHandler extends SimpleChannelInboundHandler<Http2PingFrame> {
    private static final NettyClientLogger log = NettyClientLogger.getLogger(Http2PingHandler.class);
    private static final Http2PingFrame DEFAULT_PING_FRAME = new DefaultHttp2PingFrame(0);

    /**
     * Time limit in ms for delays that results in a warning message being printed.
     */
    private final long delayWarningTimeLimitMs;

    private final long pingTimeoutMillis;

    private ScheduledFuture<?> periodicPing;
    private long lastPingSendTime = 0;
    private long lastPingAckTime = 0;

    public Http2PingHandler(int pingTimeoutMillis) {
        this.pingTimeoutMillis = pingTimeoutMillis;
        delayWarningTimeLimitMs = Math.min(100, pingTimeoutMillis / 10);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        CompletableFuture<Protocol> protocolFuture = ctx.channel().attr(ChannelAttributeKey.PROTOCOL_FUTURE).get();
        Validate.validState(protocolFuture != null, "Protocol future must be initialized before handler is added.");
        protocolFuture.thenAccept(p -> start(p, ctx));
    }

    private void start(Protocol protocol, ChannelHandlerContext ctx) {
        if (protocol == Protocol.HTTP2 && periodicPing == null) {
            periodicPing = ctx.channel()
                              .eventLoop()
                              .schedule(() -> doPeriodicPing(ctx.channel()), 0, MILLISECONDS);
        }
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        stop();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        stop();
        ctx.fireChannelInactive();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http2PingFrame frame) {
        log.debug(ctx.channel(), () -> "Received PING from channel, ack=" + frame.ack());
        if (frame.ack()) {
            lastPingAckTime = System.currentTimeMillis();
        } else {
            ctx.fireChannelRead(frame);
        }
    }

    private void doPeriodicPing(Channel channel) {
        if (lastPingAckTime <= lastPingSendTime - pingTimeoutMillis) {
            log.warn(channel, () -> "PING timeout occurred");
            long timeSinceLastPingSend = System.currentTimeMillis() - lastPingSendTime;
            channelIsUnhealthy(channel, new PingFailedException("Server did not respond to PING after " +
                                                                timeSinceLastPingSend + "ms (limit: " +
                                                                pingTimeoutMillis + "ms)"));
        } else {
            log.debug(channel, () -> "Sending HTTP2/PING frame");
            long scheduleTime = lastPingSendTime == 0 ? 0 : System.currentTimeMillis() - lastPingSendTime;
            if (scheduleTime - pingTimeoutMillis > delayWarningTimeLimitMs) {
                log.warn(channel, () -> "PING timer scheduled after " + scheduleTime + "ms");
            }
            sendPing(channel);
        }
    }

    private void sendPing(Channel channel) {
        long writeMs = System.currentTimeMillis();
        channel.writeAndFlush(DEFAULT_PING_FRAME).addListener(res -> {
            if (!res.isSuccess()) {
                log.debug(channel, () -> "Failed to write and flush PING frame to connection", res.cause());
                channelIsUnhealthy(channel, new PingFailedException("Failed to send PING to the service", res.cause()));
            } else {
                log.debug(channel, () -> "Successfully flushed PING frame to connection");
                lastPingSendTime = System.currentTimeMillis();
                long flushTime = lastPingSendTime - writeMs;
                if (flushTime > delayWarningTimeLimitMs) {
                    log.warn(channel, () -> "Flushing PING frame took " + flushTime + "ms");
                }
                // ping frame was flushed to the socket, schedule to send the next ping now to avoid premature timeout.
                //
                // Scenario we want to avoid is shown below (NOTE: ptm - pingTimeoutMillis, Wx - Write Ping x,
                // Fx - Flush Ping x, Rx - Receive ack x, T -Timeout)
                // When timer is scheduled periodically, even though ack2 comes < ptm after being flushed to the socket, we
                // will still timeout at 2ptm.
                // 0          1ptm       2ptm       3ptm
                // |----------|----------|----------|-------> time
                // W1F1 R1    W2      F2 T  R2
                // When timer is scheduled after flushing, we allow time for ack to come back and don't prematurely timeout.
                // 0            ptm1               ptm2
                // |-|----------|------|----------|-----------> time
                // W1F1 R1      W2     F2   R2    W3
                periodicPing = channel.eventLoop().schedule(() -> doPeriodicPing(channel), pingTimeoutMillis, MILLISECONDS);
            }
        });
    }

    private void channelIsUnhealthy(Channel channel, PingFailedException exception) {
        stop();
        channel.pipeline().fireExceptionCaught(exception);
    }

    private void stop() {
        if (periodicPing != null) {
            periodicPing.cancel(false);
            periodicPing = null;
        }
    }
}
