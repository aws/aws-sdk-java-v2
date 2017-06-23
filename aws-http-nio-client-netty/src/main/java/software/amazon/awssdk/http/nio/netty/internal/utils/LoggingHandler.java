/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http.nio.netty.internal.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import java.net.SocketAddress;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Sharable
public final class LoggingHandler extends ChannelDuplexHandler {

    private final Consumer<Supplier<String>> logger;

    public LoggingHandler(Consumer<Supplier<String>> logger) {
        this.logger = logger;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        log(() -> format(ctx, "CHANNEL_REGISTERED"));
        ctx.fireChannelRegistered();
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        log(() -> format(ctx, "CHANNEL_UNREGISTERED"));
        ctx.fireChannelUnregistered();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log(() -> format(ctx, "CHANNEL_ACTIVE"));
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log(() -> format(ctx, "CHANNEL_INACTIVE"));
        ctx.fireChannelInactive();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log(() -> format(ctx, "(inbound) RECEIVED", msg));
        ctx.fireChannelRead(msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        log(() -> format(ctx, "(inbound) READ_COMPLETE"));
        ctx.fireChannelReadComplete();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        log(() -> format(ctx, "USER_EVENT_TRIGGERED", evt));
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        log(() -> format(ctx, "CHANNEL_WRITABILITY_CHANGED"));
        ctx.fireChannelWritabilityChanged();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log(() -> format(ctx, "EXCEPTION", cause));
        ctx.fireExceptionCaught(cause);
    }

    @Override
    public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws Exception {
        log(() -> format(ctx, "BIND", localAddress));
        ctx.bind(localAddress, promise);
    }

    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remote, SocketAddress local, ChannelPromise p) throws Exception {
        log(() -> format(ctx, "CONNECT", remote, local));
        ctx.connect(remote, local, p);
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        log(() -> format(ctx, "DISCONNECT"));
        ctx.disconnect(promise);
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        log(() -> format(ctx, "CLOSE"));
        ctx.close(promise);
    }

    @Override
    public void deregister(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        log(() -> format(ctx, "DEREGISTER"));
        ctx.deregister(promise);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        log(() -> format(ctx, "(outbound) WRITE", msg));
        ctx.write(msg, promise);
    }

    @Override
    public void flush(ChannelHandlerContext ctx) throws Exception {
        log(() -> format(ctx, "(outbound) FLUSH"));
        ctx.flush();
    }

    private String format(ChannelHandlerContext ctx, String event) {
        return ctx.channel() + " " + event;
    }

    private String format(ChannelHandlerContext ctx, String event, Object obj) {
        StringBuilder sb = new StringBuilder(ctx.channel().toString()).append(" ").append(event);
        if (obj instanceof ByteBuf) {
            ByteBuf buf = (ByteBuf) obj;
            sb.append(" ").append(buf.readableBytes()).append(" bytes\n").append(ByteBufUtil.prettyHexDump(buf));
        } else if (obj instanceof ByteBufHolder) {
            ByteBufHolder holder = (ByteBufHolder) obj;
            sb.append(" ")
                .append(holder.content().readableBytes())
                .append(" bytes\n")
                .append(String.valueOf(obj))
                .append("\n")
                .append(ByteBufUtil.prettyHexDump(holder.content()));
        } else {
            sb.append("\n").append(String.valueOf(obj));
        }

        return sb.toString();
    }

    private String format(ChannelHandlerContext ctx, String event, Object first, Object second) {
        if (second == null) {
            return format(ctx, event, first);
        }
        return ctx.channel().toString() + " " + event + ":" + String.valueOf(first) + "," + String.valueOf(second);
    }

    private void log(Supplier<String> msg) {
        logger.accept(msg);
    }
}
