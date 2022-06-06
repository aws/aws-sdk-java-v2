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
import static software.amazon.awssdk.http.HttpTestUtils.sendGetRequest;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.PROTOCOL;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES;

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
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.crt.Log;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.Logger;

/**
 * Testing the scenario where h2 server sends 5xx errors.
 */
public class H2Test {
    private static final Logger LOGGER = Logger.loggerFor(H2Test.class);
    private SdkAsyncHttpClient crt;
    private Server server;

    static {
        Log.initLoggingToStdout(Log.LogLevel.Trace);
    }

    @BeforeEach
    public void setup() throws Exception {
        server = new Server();
        server.init();
        crt = AwsCrtAsyncHttpClient.builder()
                                   .buildWithDefaults(AttributeMap.builder().put(TRUST_ALL_CERTIFICATES, true)
                                                                  .put(PROTOCOL, Protocol.HTTP2)
                                                                  .build());
    }

    @AfterEach
    public void teardown() throws InterruptedException {
        if (server != null) {
            server.shutdown();
        }
        server = null;

        if (crt != null) {
            crt.close();
        }
        crt = null;
    }

    @Test
    public void sendH2Request() throws Exception {
        CompletableFuture<?> firstRequest = sendGetRequest(server.port(), crt);
        firstRequest.join();
    }

    private static class Server extends ChannelInitializer<Channel> {
        private ServerBootstrap bootstrap;
        private ServerSocketChannel serverSock;
        private String[] channelIds = new String[5];
        private final NioEventLoopGroup group = new NioEventLoopGroup();
        private SslContext sslCtx;
        private AtomicInteger h2ConnectionCount = new AtomicInteger(0);

        void init() throws Exception {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
                                      .sslProvider(SslProvider.JDK)
                                      .applicationProtocolConfig(new ApplicationProtocolConfig(
                                          ApplicationProtocolConfig.Protocol.ALPN,
                                          ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                                          ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                                          ApplicationProtocolNames.HTTP_2))
                                      .build();

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
                                                               .autoAckSettingsFrame(true)
                                                               .autoAckPingFrame(true)
                                                               .initialSettings(Http2Settings.defaultSettings().maxConcurrentStreams(1))
                                                               .frameLogger(new Http2FrameLogger(LogLevel.DEBUG))
                                                               .build();

            Http2MultiplexHandler http2Handler = new Http2MultiplexHandler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel ch) throws Exception {
                    ch.pipeline().addLast(new StreamFrameHandler());
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

        private class StreamFrameHandler extends SimpleChannelInboundHandler<Http2Frame> {

            @Override
            protected void channelRead0(ChannelHandlerContext ctx, Http2Frame frame) {
                if (frame instanceof Http2DataFrame) {
                    DefaultHttp2DataFrame dataFrame = new DefaultHttp2DataFrame(true);
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
