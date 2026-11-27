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

package software.amazon.awssdk.http.crt;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.codec.http2.Http2StreamFrame;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import software.amazon.awssdk.http.Protocol;

/**
 * Plaintext HTTP/1.1 or HTTP/2 (prior knowledge) test server that defers every response until the test calls
 * {@link #releaseResponses()}, and answers immediately after that, until the next {@link #reset()}.
 */
final class DeferredResponseServer implements AutoCloseable {
    private final NioEventLoopGroup group = new NioEventLoopGroup(1);
    private final AtomicInteger receivedRequests = new AtomicInteger();
    private final List<Runnable> deferredResponses = new ArrayList<>();
    private boolean released;
    private Channel serverChannel;

    static DeferredResponseServer start(Protocol protocol) throws InterruptedException {
        DeferredResponseServer server = new DeferredResponseServer();
        server.serverChannel = new ServerBootstrap()
            .group(server.group)
            .channel(NioServerSocketChannel.class)
            .childHandler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel ch) {
                    if (protocol == Protocol.HTTP2) {
                        ch.pipeline().addLast(Http2FrameCodecBuilder.forServer().build(),
                                              new Http2MultiplexHandler(server.h2StreamHandler()));
                    } else {
                        ch.pipeline().addLast(new HttpServerCodec(), new HttpObjectAggregator(8192), server.h1Handler());
                    }
                }
            })
            .bind(0).sync().channel();
        return server;
    }

    private SimpleChannelInboundHandler<FullHttpRequest> h1Handler() {
        return new SimpleChannelInboundHandler<FullHttpRequest>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
                respondOrDefer(ctx, () -> respondH1(ctx));
            }
        };
    }

    private ChannelInitializer<Channel> h2StreamHandler() {
        return new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel stream) {
                stream.pipeline().addLast(new SimpleChannelInboundHandler<Http2StreamFrame>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, Http2StreamFrame frame) {
                        if (isEndOfRequest(frame)) {
                            respondOrDefer(ctx, () -> respondH2(ctx));
                        }
                    }
                });
            }
        };
    }

    private synchronized void respondOrDefer(ChannelHandlerContext ctx, Runnable respond) {
        receivedRequests.incrementAndGet();
        if (released) {
            respond.run();
        } else {
            deferredResponses.add(() -> ctx.executor().execute(respond));
        }
    }

    synchronized void releaseResponses() {
        released = true;
        deferredResponses.forEach(Runnable::run);
        deferredResponses.clear();
    }

    synchronized void reset() {
        released = false;
        deferredResponses.clear();
        receivedRequests.set(0);
    }

    int receivedRequests() {
        return receivedRequests.get();
    }

    URI uri() {
        return URI.create("http://localhost:" + ((InetSocketAddress) serverChannel.localAddress()).getPort());
    }

    private static boolean isEndOfRequest(Http2StreamFrame frame) {
        return (frame instanceof Http2HeadersFrame && ((Http2HeadersFrame) frame).isEndStream())
               || (frame instanceof Http2DataFrame && ((Http2DataFrame) frame).isEndStream());
    }

    private static void respondH1(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK);
        HttpUtil.setContentLength(response, 0);
        ctx.writeAndFlush(response);
    }

    private static void respondH2(ChannelHandlerContext ctx) {
        ctx.write(new DefaultHttp2HeadersFrame(new DefaultHttp2Headers().status("200"), false));
        ctx.writeAndFlush(new DefaultHttp2DataFrame(true));
    }

    @Override
    public void close() throws InterruptedException {
        serverChannel.close().sync();
        group.shutdownGracefully(0, 1, TimeUnit.SECONDS).await();
    }
}
