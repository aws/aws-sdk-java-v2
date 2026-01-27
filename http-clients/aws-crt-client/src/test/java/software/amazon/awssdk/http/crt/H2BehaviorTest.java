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
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.utils.AttributeMap;

public class H2BehaviorTest {
    private SdkAsyncHttpClient crt;
    private H2Server server;

    @BeforeEach
    public void setup() throws Exception {
        server = new H2Server(true);
        server.init();
        crt = AwsCrtAsyncHttpClient.builder()
                                   .buildWithDefaults(AttributeMap.builder()
                                                                  .put(TRUST_ALL_CERTIFICATES, true)
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
    public void sendH2Request_overTls() throws Exception {
        CompletableFuture<?> request = sendGetRequest(server.port(), crt);
        request.join();
    }

    @Test
    public void sendH2Request_overPlaintext_usesPriorKnowledge() throws Exception {
        H2Server h2cServer = new H2Server(false);
        h2cServer.init();
        try (SdkAsyncHttpClient h2cClient = AwsCrtAsyncHttpClient.builder()
                                                                  .buildWithDefaults(AttributeMap.builder()
                                                                                                 .put(PROTOCOL, Protocol.HTTP2)
                                                                                                 .build())) {
            CompletableFuture<?> request = sendGetRequest(h2cServer.port(), h2cClient, false);
            request.join();
        } finally {
            h2cServer.shutdown();
        }
    }

    private static class H2Server extends ChannelInitializer<Channel> {
        private final boolean useTls;
        private final NioEventLoopGroup group = new NioEventLoopGroup();
        private ServerBootstrap bootstrap;
        private ServerSocketChannel serverSock;
        private SslContext sslCtx;

        H2Server(boolean useTls) {
            this.useTls = useTls;
        }

        void init() throws Exception {
            if (useTls) {
                SelfSignedCertificate ssc = new SelfSignedCertificate();
                sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
                                          .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                                          .sslProvider(SslProvider.JDK)
                                          .applicationProtocolConfig(new ApplicationProtocolConfig(
                                              ApplicationProtocolConfig.Protocol.ALPN,
                                              ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                                              ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                                              ApplicationProtocolNames.HTTP_2))
                                          .build();
            }

            bootstrap = new ServerBootstrap()
                .channel(NioServerSocketChannel.class)
                .group(group)
                .childHandler(this);

            serverSock = (ServerSocketChannel) bootstrap.bind(0).sync().channel();
        }

        @Override
        protected void initChannel(Channel ch) {
            ChannelPipeline pipeline = ch.pipeline();

            if (useTls) {
                pipeline.addLast(sslCtx.newHandler(ch.alloc()));
            }

            pipeline.addLast(Http2FrameCodecBuilder.forServer()
                                                   .autoAckSettingsFrame(true)
                                                   .autoAckPingFrame(true)
                                                   .initialSettings(Http2Settings.defaultSettings())
                                                   .frameLogger(new Http2FrameLogger(LogLevel.DEBUG))
                                                   .build());

            pipeline.addLast(new Http2MultiplexHandler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel ch) {
                    ch.pipeline().addLast(new SimpleChannelInboundHandler<Http2Frame>() {
                        @Override
                        protected void channelRead0(ChannelHandlerContext ctx, Http2Frame frame) {
                            if (frame instanceof Http2DataFrame) {
                                Http2Headers headers = new DefaultHttp2Headers().status(OK.codeAsText());
                                ctx.write(new DefaultHttp2HeadersFrame(headers, false));
                                ctx.write(new DefaultHttp2DataFrame(true));
                                ctx.flush();
                            }
                        }
                    });
                }
            }));
        }

        void shutdown() throws InterruptedException {
            group.shutdownGracefully().await();
            serverSock.close();
        }

        int port() {
            return serverSock.localAddress().getPort();
        }
    }
}
