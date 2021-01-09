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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
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
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.http.EmptyPublisher;
import software.amazon.awssdk.http.nio.netty.Http2Configuration;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.internal.http2.PingFailedException;

/**
 * Testing the scenario where the server never acks PING
 */
public class PingTimeoutTest {
    @Rule
    public ExpectedException expected = ExpectedException.none();

    private Server server;
    private SdkAsyncHttpClient netty;

    @Before
    public void methodSetup() throws Exception {
        server = new Server();
        server.init();
    }

    @After
    public void methodTeardown() throws InterruptedException {
        server.shutdown();

        if (netty != null) {
            netty.close();
        }

        netty = null;
    }

    @Test
    public void pingHealthCheck_null_shouldThrowExceptionAfter5Sec() {
        Instant a = Instant.now();
        assertThatThrownBy(() -> makeRequest(null).join())
            .hasMessageContaining("An error occurred on the connection")
            .hasCauseInstanceOf(IOException.class)
            .hasRootCauseInstanceOf(PingFailedException.class);
        assertThat(Duration.between(a, Instant.now())).isBetween(Duration.ofSeconds(5), Duration.ofSeconds(7));
    }

    @Test
    public void pingHealthCheck_10sec_shouldThrowExceptionAfter10Secs() {
        Instant a = Instant.now();
        assertThatThrownBy(() -> makeRequest(Duration.ofSeconds(10)).join()).hasCauseInstanceOf(IOException.class)
                                                                            .hasMessageContaining("An error occurred on the connection")
                                                                            .hasRootCauseInstanceOf(PingFailedException.class);
        assertThat(Duration.between(a, Instant.now())).isBetween(Duration.ofSeconds(10), Duration.ofSeconds(12));
    }

    @Test
    public void pingHealthCheck_0_disabled_shouldNotThrowException() throws Exception {
        expected.expect(TimeoutException.class);
        CompletableFuture<Void> requestFuture = makeRequest(Duration.ofMillis(0));
        try {
            requestFuture.get(8, TimeUnit.SECONDS);
        } finally {
            assertThat(requestFuture.isDone()).isFalse();
        }
    }

    private CompletableFuture<Void> makeRequest(Duration healthCheckPingPeriod) {
        netty = NettyNioAsyncHttpClient.builder()
                .protocol(Protocol.HTTP2)
                .http2Configuration(Http2Configuration.builder().healthCheckPingPeriod(healthCheckPingPeriod).build())
                .build();

        SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                .protocol("http")
                .host("localhost")
                .port(server.port())
                .method(SdkHttpMethod.GET)
                .build();

        AsyncExecuteRequest executeRequest = AsyncExecuteRequest.builder()
                .fullDuplex(false)
                .request(request)
                .requestContentPublisher(new EmptyPublisher())
                .responseHandler(new SdkAsyncHttpResponseHandler() {
                    @Override
                    public void onHeaders(SdkHttpResponse headers) {
                    }

                    @Override
                    public void onStream(Publisher<ByteBuffer> stream) {
                        stream.subscribe(new Subscriber<ByteBuffer>() {
                            @Override
                            public void onSubscribe(Subscription s) {
                                s.request(Integer.MAX_VALUE);
                            }

                            @Override
                            public void onNext(ByteBuffer byteBuffer) {
                            }

                            @Override
                            public void onError(Throwable t) {
                            }

                            @Override
                            public void onComplete() {
                            }
                        });
                    }

                    @Override
                    public void onError(Throwable error) {
                    }
                })
                .build();

        return netty.execute(executeRequest);
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
            h2ConnectionCount.incrementAndGet();

            ChannelPipeline pipeline = ch.pipeline();

            Http2FrameCodec http2Codec = Http2FrameCodecBuilder.forServer()
                    // simulate not sending goaway
                    .decoupleCloseAndGoAway(true)
                    .autoAckPingFrame(false)
                    .initialSettings(Http2Settings.defaultSettings().maxConcurrentStreams(2))
                    .frameLogger(new Http2FrameLogger(LogLevel.DEBUG, "WIRE"))
                    .build();

            Http2MultiplexHandler http2Handler = new Http2MultiplexHandler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel ch) {
                    ch.pipeline().addLast(new StreamHandler());
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
    }

    private static final class StreamHandler extends SimpleChannelInboundHandler<Http2Frame> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Http2Frame http2Frame) throws Exception {
            if (http2Frame instanceof Http2DataFrame) {
                Http2DataFrame dataFrame = (Http2DataFrame) http2Frame;
                if (dataFrame.isEndStream()) {
                    Http2Headers headers = new DefaultHttp2Headers().status("200");
                    ctx.writeAndFlush(new DefaultHttp2HeadersFrame(headers, false));
                    ctx.executor().scheduleAtFixedRate(() -> {
                        DefaultHttp2DataFrame respData = new DefaultHttp2DataFrame(Unpooled.wrappedBuffer("hello".getBytes()), false);
                        ctx.writeAndFlush(respData);
                    }, 0, 2, TimeUnit.SECONDS);
                }
            }
        }
    }
}
