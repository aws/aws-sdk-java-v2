package software.amazon.awssdk.http.nio.netty.internal;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;

import java.time.ZonedDateTime;

import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKeys.REQUEST_CONTEXT_KEY;

public class DongiePeekingHandler extends ChannelDuplexHandler {
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        RequestContext requestContext = ctx.channel().attr(REQUEST_CONTEXT_KEY).get();
        log(String.format("Request Thread %d: write event with msg %s", requestContext.requestThreadId(), msg));
        ctx.write(msg, promise);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        RequestContext requestContext = ctx.channel().attr(REQUEST_CONTEXT_KEY).get();
        log(String.format("Request Thread %d: read event with msg %s", requestContext.requestThreadId(), msg));
        ctx.fireChannelRead(msg);
    }

    private static void log(String msg) {
        System.out.printf("[%s]: %s%n", ZonedDateTime.now(), msg);
    }
}
