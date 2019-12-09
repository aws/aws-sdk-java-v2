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

package software.amazon.awssdk.http.nio.netty.fault;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES;
import static software.amazon.awssdk.http.nio.netty.internal.NettyConfiguration.HTTP2_CONNECTION_PING_TIMEOUT_SECONDS;

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
import io.netty.handler.codec.http2.DefaultHttp2PingFrame;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Frame;
import io.netty.handler.codec.http2.Http2FrameCodec;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.Http2GoAwayFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.codec.http2.Http2PingFrame;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.reactivex.Flowable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.http.nio.netty.EmptyPublisher;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.SdkEventLoopGroup;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.Logger;


/**
 * Testing the scenario where the server fails to respond
 */
public class ServerNotRespondingTest {
    private static final Logger LOGGER = Logger.loggerFor(ServerNotRespondingTest.class);
    private SdkAsyncHttpClient netty;
    private Server server;

    @Before
    public void setup() throws Exception {
        server = new Server();
        server.init();

        netty = NettyNioAsyncHttpClient.builder()
                                       .readTimeout(Duration.ofMillis(500))
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
    public void connectionNotRespond_pingAck_shouldReuse() throws InterruptedException {
        server.ackPingOnFirstChannel = true;
        // The first request picks up a non-responding channel and should fail. Channel 1
        CompletableFuture<Void> firstRequest = sendGetRequest();
        assertThatThrownBy(() -> firstRequest.join()).hasCauseInstanceOf(IOException.class);

        server.failOnFirstChannel = false;

        // Make sure PING is acked
        Thread.sleep(1000);

        // The second request should reuse channel 1 because it acks PING.
        Void secondRequest = sendGetRequest().join();

        assertThat(server.h2ConnectionCount.get()).isEqualTo(1);
    }

    private CompletableFuture<Void> sendGetRequest() {
        AsyncExecuteRequest req = AsyncExecuteRequest.builder()
                                                     .responseHandler(new SdkAsyncHttpResponseHandler() {
                                                         private SdkHttpResponse headers;

                                                         @Override
                                                         public void onHeaders(SdkHttpResponse headers) {
                                                             this.headers = headers;
                                                         }

                                                         @Override
                                                         public void onStream(Publisher<ByteBuffer> stream) {
                                                             Flowable.fromPublisher(stream).forEach(b -> {
                                                             });
                                                         }

                                                         @Override
                                                         public void onError(Throwable error) {
                                                         }
                                                     })
                                                     .request(SdkHttpFullRequest.builder()
                                                                                .method(SdkHttpMethod.GET)
                                                                                .protocol("https")
                                                                                .host("localhost")
                                                                                .port(server.port())
                                                                                .build())
                                                     .requestContentPublisher(new EmptyPublisher())
                                                     .build();

        return netty.execute(req);
    }


    private static class Server extends ChannelInitializer<Channel> {
        private ServerBootstrap bootstrap;
        private ServerSocketChannel serverSock;
        private String[] channelIds = new String[5];
        private final NioEventLoopGroup group = new NioEventLoopGroup();
        private SslContext sslCtx;
        private AtomicInteger h2ConnectionCount = new AtomicInteger(0);
        private boolean ackPingOnFirstChannel = false;
        private boolean failOnFirstChannel = true;

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
            ch.pipeline().addFirst(new LoggingHandler(LogLevel.DEBUG));
            LOGGER.debug(() -> "init channel " + ch);
            h2ConnectionCount.incrementAndGet();

            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast(sslCtx.newHandler(ch.alloc()));


            Http2FrameCodec http2Codec = Http2FrameCodecBuilder.forServer()
                                                               //Disable auto ack ping
                                                               .autoAckPingFrame(false)
                                                               .initialSettings(Http2Settings.defaultSettings().maxConcurrentStreams(2))
                                                               .frameLogger(new Http2FrameLogger(LogLevel.DEBUG, "WIRE"))
                                                               .build();

            Http2MultiplexHandler http2Handler = new Http2MultiplexHandler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel ch) throws Exception {
                    ch.pipeline().addLast(new MightNotRespondStreamFrameHandler());
                }
            });

            pipeline.addLast(http2Codec);
            pipeline.addLast(new MightNotRespondPingFrameHandler());
            pipeline.addLast(new VerifyGoAwayFrameHandler());
            pipeline.addLast(http2Handler);
        }

        public void shutdown() throws InterruptedException {
            group.shutdownGracefully().await();
            serverSock.close();
        }

        public int port() {
            return serverSock.localAddress().getPort();
        }

        public final class MightNotRespondPingFrameHandler extends SimpleChannelInboundHandler<Http2PingFrame> {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, Http2PingFrame msg) {
                if (channelIds[0].equals(ctx.channel().id().asShortText()) && !ackPingOnFirstChannel) {
                    // Not respond if this is the first request
                    LOGGER.info(() -> "yolo" + ctx.channel());
                } else {
                    ctx.writeAndFlush(new DefaultHttp2PingFrame(msg.content(), true));
                }
            }
        }

        public final class VerifyGoAwayFrameHandler extends SimpleChannelInboundHandler<Http2GoAwayFrame> {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, Http2GoAwayFrame msg) {
                LOGGER.info(() -> "goaway" + ctx.channel());
                h2ConnectionCount.decrementAndGet();
                msg.release();
            }
        }

        private class MightNotRespondStreamFrameHandler extends SimpleChannelInboundHandler<Http2Frame> {

            @Override
            protected void channelRead0(ChannelHandlerContext ctx, Http2Frame frame) {
                if (frame instanceof Http2DataFrame) {
                    // Not respond if this is channel 1
                    if (channelIds[0].equals(ctx.channel().parent().id().asShortText()) && failOnFirstChannel) {
                        LOGGER.info(() -> "This is the first request, not responding" + ctx.channel());
                    } else {
                        DefaultHttp2DataFrame dataFrame = new DefaultHttp2DataFrame(false);
                        try {
                            LOGGER.info(() -> "return empty data "  + ctx.channel() + " frame " + frame.getClass());
                            Http2Headers headers = new DefaultHttp2Headers().status(OK.codeAsText());
                            ctx.write(dataFrame);
                            ctx.write(new DefaultHttp2HeadersFrame(headers, true));
                            ctx.flush();
                        } finally {
                            dataFrame.release();
                        }
                    }
                }
            }
        }
    }
}
