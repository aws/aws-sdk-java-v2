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

package software.amazon.awssdk.http;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;


import java.util.ArrayList;
import java.util.List;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.TEXT_PLAIN;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


/**
 * Testing the scenario where h1 server sends 5xx errors.
 */
public abstract class H1ServerErrorTestBase {
    private Server server;

    protected abstract SdkAsyncHttpClient getTestClient();

    public void setup() throws Exception {
        server = new Server();
        server.init();
    }

    public void teardown() throws InterruptedException {
        if (server != null) {
            server.shutdown();
        }
        server = null;
    }

    public void connectionReceive500_shouldNotReuseConnection() {
        server.return500OnFirstRequest = true;

        TestUtils.sendGetRequest(server.port(), getTestClient()).join();
        TestUtils.sendGetRequest(server.port(), getTestClient()).join();
        assertThat(server.channels.size()).isEqualTo(2);
    }

    public void connectionReceive200_shouldReuseConnection() {
        server.return500OnFirstRequest = false;

        TestUtils.sendGetRequest(server.port(), getTestClient()).join();
        TestUtils.sendGetRequest(server.port(), getTestClient()).join();
        assertThat(server.channels.size()).isEqualTo(1);
    }

    private static class Server extends ChannelInitializer<SocketChannel> {
        private static final byte[] CONTENT = "helloworld".getBytes();
        private ServerBootstrap bootstrap;
        private ServerSocketChannel serverSock;
        private List<SocketChannel> channels = new ArrayList<>();
        private final NioEventLoopGroup group = new NioEventLoopGroup();
        private SslContext sslCtx;
        private boolean return500OnFirstRequest;

        public void init() throws Exception {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();

            bootstrap = new ServerBootstrap()
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.DEBUG))
                .group(group)
                .childHandler(this);

            serverSock = (ServerSocketChannel) bootstrap.bind(0).sync().channel();
        }

        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            channels.add(ch);
            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast(sslCtx.newHandler(ch.alloc()));
            pipeline.addLast(new HttpServerCodec());
            pipeline.addLast(new MightReturn500ChannelHandler());
        }

        public void shutdown() throws InterruptedException {
            group.shutdownGracefully().await();
        }

        public int port() {
            return serverSock.localAddress().getPort();
        }

        private class MightReturn500ChannelHandler extends ChannelDuplexHandler {

            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) {
                if (msg instanceof HttpRequest) {
                    HttpResponseStatus status;
                    if (ctx.channel().equals(channels.get(0)) && return500OnFirstRequest) {
                        status = INTERNAL_SERVER_ERROR;
                    } else {
                        status = OK;
                    }

                    FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status,
                                                                            Unpooled.wrappedBuffer(CONTENT));

                    response.headers()
                            .set(CONTENT_TYPE, TEXT_PLAIN)
                            .setInt(CONTENT_LENGTH, response.content().readableBytes());
                    ctx.writeAndFlush(response);
                }
            }
        }
    }
}
