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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import io.netty.handler.codec.http2.DefaultHttp2GoAwayFrame;
import io.netty.handler.codec.http2.DefaultHttp2ResetFrame;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2Frame;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.Http2HeadersFrame;
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
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Tests HTTP/2 error scenarios: RST_STREAM and GOAWAY frames.
 */
public class H2ErrorTest {
    private SdkAsyncHttpClient client;

    @BeforeEach
    public void setup() {
        client = AwsCrtAsyncHttpClient.builder()
                                      .buildWithDefaults(AttributeMap.builder()
                                                                     .put(TRUST_ALL_CERTIFICATES, true)
                                                                     .put(PROTOCOL, Protocol.HTTP2)
                                                                     .build());
    }

    @AfterEach
    public void teardown() {
        if (client != null) {
            client.close();
        }
    }

    @Test
    public void serverSendsRstStream_shouldThrowIOException() throws Exception {
        H2ErrorServer server = new H2ErrorServer(ErrorType.RST_STREAM);
        server.init();
        try {
            CompletableFuture<?> request = sendGetRequest(server.port(), client);
            assertThatThrownBy(request::join)
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(IOException.class)
                .hasMessageContaining("RST_STREAM");
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void serverSendsGoAway_shouldThrowIOException() throws Exception {
        H2ErrorServer server = new H2ErrorServer(ErrorType.GOAWAY);
        server.init();
        try {
            CompletableFuture<?> request = sendGetRequest(server.port(), client);
            assertThatThrownBy(request::join)
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(IOException.class)
                .hasMessageContaining("connection has closed");
        } finally {
            server.shutdown();
        }
    }

    private enum ErrorType {
        RST_STREAM,
        GOAWAY
    }

    private static class H2ErrorServer extends ChannelInitializer<Channel> {
        private final ErrorType errorType;
        private final NioEventLoopGroup group = new NioEventLoopGroup();
        private ServerBootstrap bootstrap;
        private ServerSocketChannel serverSock;
        private SslContext sslCtx;

        H2ErrorServer(ErrorType errorType) {
            this.errorType = errorType;
        }

        void init() throws Exception {
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

            bootstrap = new ServerBootstrap()
                .channel(NioServerSocketChannel.class)
                .group(group)
                .childHandler(this);

            serverSock = (ServerSocketChannel) bootstrap.bind(0).sync().channel();
        }

        @Override
        protected void initChannel(Channel ch) {
            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast(sslCtx.newHandler(ch.alloc()));
            pipeline.addLast(Http2FrameCodecBuilder.forServer()
                                                   .autoAckSettingsFrame(true)
                                                   .autoAckPingFrame(true)
                                                   .initialSettings(Http2Settings.defaultSettings())
                                                   .frameLogger(new Http2FrameLogger(LogLevel.DEBUG))
                                                   .build());

            pipeline.addLast(new Http2MultiplexHandler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel ch) {
                    ch.pipeline().addLast(new ErrorFrameHandler(errorType));
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

        private static class ErrorFrameHandler extends SimpleChannelInboundHandler<Http2Frame> {
            private final ErrorType errorType;

            ErrorFrameHandler(ErrorType errorType) {
                this.errorType = errorType;
            }

            @Override
            protected void channelRead0(ChannelHandlerContext ctx, Http2Frame frame) {
                if (frame instanceof Http2HeadersFrame || frame instanceof Http2DataFrame) {
                    switch (errorType) {
                        case RST_STREAM:
                            ctx.writeAndFlush(new DefaultHttp2ResetFrame(Http2Error.INTERNAL_ERROR));
                            break;
                        case GOAWAY:
                            ctx.channel().parent().writeAndFlush(new DefaultHttp2GoAwayFrame(Http2Error.INTERNAL_ERROR));
                            break;
                    }
                }
            }
        }
    }
}
