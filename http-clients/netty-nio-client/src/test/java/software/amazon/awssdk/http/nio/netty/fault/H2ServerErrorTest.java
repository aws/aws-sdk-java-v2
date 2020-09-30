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

package software.amazon.awssdk.http.nio.netty.fault;

import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES;
import static software.amazon.awssdk.http.HttpTestUtils.sendGetRequest;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Frame;
import io.netty.handler.codec.http2.Http2FrameCodec;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.SdkEventLoopGroup;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.Logger;

/**
 * Testing the scenario where h2 server sends 5xx errors.
 */
public class H2ServerErrorTest {
    private static final Logger LOGGER = Logger.loggerFor(ServerNotRespondingTest.class);
    private SdkAsyncHttpClient netty;
    private Server server;

    @Before
    public void setup() throws Exception {
        server = new Server();
        server.init();

        netty = NettyNioAsyncHttpClient.builder()
                                       .eventLoopGroup(SdkEventLoopGroup.builder().numberOfThreads(3).build())
                                       .protocol(Protocol.HTTP2)
                                       .buildWithDefaults(AttributeMap.builder().put(TRUST_ALL_CERTIFICATES, true).build());
    }

    @After
    public void teardown() throws InterruptedException {
        if (server != null) {
            server.shutdown();
        }
        server = null;

        if (netty != null) {
            netty.close();
        }
        netty = null;
    }

    @Test
    public void serviceReturn500_newRequestShouldUseNewConnection() {
        server.return500OnFirstRequest = true;
        CompletableFuture<Void> firstRequest = sendGetRequest(server.port(), netty);
        firstRequest.join();

        sendGetRequest(server.port(), netty).join();
        assertThat(server.h2ConnectionCount.get()).isEqualTo(2);
    }

    @Test
    public void serviceReturn200_newRequestShouldReuseNewConnection() {
        server.return500OnFirstRequest = false;
        CompletableFuture<Void> firstRequest = sendGetRequest(server.port(), netty);
        firstRequest.join();

        sendGetRequest(server.port(), netty).join();
        assertThat(server.h2ConnectionCount.get()).isEqualTo(1);
    }

    private static class Server extends ChannelInitializer<Channel> {
        private ServerBootstrap bootstrap;
        private ServerSocketChannel serverSock;
        private String[] channelIds = new String[5];
        private final NioEventLoopGroup group = new NioEventLoopGroup();
        private SslContext sslCtx;
        private boolean return500OnFirstRequest;
        private AtomicInteger h2ConnectionCount = new AtomicInteger(0);

        void init() throws Exception {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();

            bootstrap = new ServerBootstrap()
                .channel(NioServerSocketChannel.class)
                .group(group)
                .childHandler(this);

            serverSock = (ServerSocketChannel) bootstrap.bind(0).sync().channel();
        }

        @Override
        protected void initChannel(Channel ch) {
            channelIds[h2ConnectionCount.get()] = ch.id().asShortText();
            LOGGER.debug(() -> "init channel " + ch);
            h2ConnectionCount.incrementAndGet();

            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast(sslCtx.newHandler(ch.alloc()));


            Http2FrameCodec http2Codec = Http2FrameCodecBuilder.forServer()
                                                               .autoAckPingFrame(true)
                                                               .initialSettings(Http2Settings.defaultSettings().maxConcurrentStreams(1))
                                                               .build();

            Http2MultiplexHandler http2Handler = new Http2MultiplexHandler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel ch) throws Exception {
                    ch.pipeline().addLast(new MightReturn500StreamFrameHandler());
                }
            });

            pipeline.addLast(http2Codec);
            pipeline.addLast(http2Handler);
        }

        public void shutdown() throws InterruptedException {
            group.shutdownGracefully().await();
            serverSock.close();
        }

        public int port() {
            return serverSock.localAddress().getPort();
        }

        private class MightReturn500StreamFrameHandler extends SimpleChannelInboundHandler<Http2Frame> {

            @Override
            protected void channelRead0(ChannelHandlerContext ctx, Http2Frame frame) {
                if (frame instanceof Http2DataFrame) {
                    DefaultHttp2DataFrame dataFrame = new DefaultHttp2DataFrame(true);

                    // returning 500 this is channel 1
                    if (channelIds[0].equals(ctx.channel().parent().id().asShortText()) && return500OnFirstRequest) {
                        LOGGER.info(() -> "This is the first request, returning 500" + ctx.channel());
                        Http2Headers headers = new DefaultHttp2Headers().status(INTERNAL_SERVER_ERROR.codeAsText());
                        ctx.write(new DefaultHttp2HeadersFrame(headers, false));
                        ctx.write(new DefaultHttp2DataFrame(true));
                        ctx.flush();
                    } else {
                        LOGGER.info(() -> "return empty data " + ctx.channel() + " frame " + frame.getClass());
                        Http2Headers headers = new DefaultHttp2Headers().status(OK.codeAsText());
                        ctx.write(new DefaultHttp2HeadersFrame(headers, false));
                        ctx.write(dataFrame);
                        ctx.flush();
                    }
                }
            }
        }
    }

}
