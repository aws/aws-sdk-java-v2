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

package software.amazon.awssdk.http.nio.netty.internal;

import static javax.net.ssl.SSLEngineResult.HandshakeStatus.NEED_TASK;
import static javax.net.ssl.SSLEngineResult.HandshakeStatus.NEED_UNWRAP;
import static javax.net.ssl.SSLEngineResult.HandshakeStatus.NEED_WRAP;
import static javax.net.ssl.SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.reactivex.Flowable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.http.EmptyPublisher;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.SdkEventLoopGroup;
import software.amazon.awssdk.http.nio.netty.fault.DelegatingSslEngine;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.Logger;

/**
 * Test client behavior when TLS setup fails at different points in the handshake process.
 */
public class TlsHandshakeFailureClientTest {
    private static final Logger LOGGER = Logger.loggerFor(TlsHandshakeFailureClientTest.class);
    private Server server;
    private SdkAsyncHttpClient netty;

    private enum TlsSetupFailTime {
        BEFORE_HANDSHAKE,
        DURING_HANDSHAKE,
        NEVER
    }

    private static List<TestCase> testCases() {
        List<TestCase> testCases = new ArrayList<>();

        for (String tlsVersion : versionsToTest()) {
            testCases.add(new TestCase(tlsVersion,
                                       TlsSetupFailTime.BEFORE_HANDSHAKE, IOException.class,
                                       "Failed TLS connection setup."));
            testCases.add(new TestCase(tlsVersion,
                                       TlsSetupFailTime.DURING_HANDSHAKE, IOException.class,
                                       "Failed TLS connection setup."));
            testCases.add(new TestCase(tlsVersion, TlsSetupFailTime.NEVER, null, null));
        }

        return testCases;
    }

    private static List<String> versionsToTest() {
        List<String> versions = new ArrayList<>();
        versions.add("TLSv1.2");

        if (SslProvider.isTlsv13Supported(SslProvider.JDK)) {
            versions.add("TLSv1.3");
        }

        return versions;
    }

    @AfterEach
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

    @ParameterizedTest
    @MethodSource("testCases")
    public void testTlsHandshake(TestCase testCase) throws Exception {
        server = new Server(testCase.tlsVersion);
        server.init(testCase.tlsSetupFailTime);

        netty = NettyNioAsyncHttpClient.builder()
                                       .readTimeout(Duration.ofMillis(500))
                                       .eventLoopGroup(SdkEventLoopGroup.builder().numberOfThreads(3).build())
                                       .protocol(Protocol.HTTP1_1)
                                       .buildWithDefaults(AttributeMap.builder().put(TRUST_ALL_CERTIFICATES, true).build());

        CompletableFuture<Void> requestFuture = sendGetRequest();
        if (server.tlsSetupFailTime == TlsSetupFailTime.NEVER) {
            assertThatNoException().isThrownBy(requestFuture::join);
        } else {
            assertThatThrownBy(requestFuture::join)
                .hasCauseInstanceOf(testCase.errorClass)
                .hasMessageContaining(testCase.errorMessage);
        }
    }

    private static class TestCase {
        private final String tlsVersion;
        private final TlsSetupFailTime tlsSetupFailTime;
        private final Class<? extends Throwable> errorClass;
        private final String errorMessage;

        private TestCase(String tlsVersion, TlsSetupFailTime tlsSetupFailTime, Class<? extends Throwable> errorClass,
                        String errorMessage) {
            this.tlsVersion = tlsVersion;
            this.tlsSetupFailTime = tlsSetupFailTime;
            this.errorClass = errorClass;
            this.errorMessage = errorMessage;
        }

        @Override
        public String toString() {
            return "TestCase{" +
                   "tlsVersion='" + tlsVersion + '\'' +
                   ", tlsSetupFailTime=" + tlsSetupFailTime +
                   ", errorClass=" + errorClass +
                   ", errorMessage='" + errorMessage + '\'' +
                   '}';
        }
    }

    public CompletableFuture<Void> sendGetRequest() {
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
        private final NioEventLoopGroup group = new NioEventLoopGroup();
        private final String tlsVersion;
        private TlsSetupFailTime tlsSetupFailTime;
        private ServerBootstrap bootstrap;
        private ServerSocketChannel serverSock;
        private SslContext sslCtx;

        private Server(String tlsVersion) {
            this.tlsVersion = tlsVersion;
        }

        private void init(TlsSetupFailTime tlsSetupFailTime) throws Exception {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();

            bootstrap = new ServerBootstrap()
                .channel(NioServerSocketChannel.class)
                .group(group)
                .childHandler(this);

            serverSock = (ServerSocketChannel) bootstrap.bind(0).sync().channel();
            this.tlsSetupFailTime = tlsSetupFailTime;
        }

        public int port() {
            return serverSock.localAddress().getPort();
        }

        public void shutdown() throws InterruptedException {
            group.shutdownGracefully().await();
            serverSock.close();
        }

        @Override
        protected void initChannel(Channel ch) {
            LOGGER.info(() -> "Initializing channel " + ch);


            ChannelPipeline pipeline = ch.pipeline();
            SSLEngine sslEngine = sslCtx.newEngine(ch.alloc());
            sslEngine.setEnabledProtocols(new String[]{ tlsVersion });
            pipeline.addLast(new SslHandler(new FaultInjectionSslEngine(sslEngine, ch), false));
            pipeline.addLast(new HttpServerCodec());
            pipeline.addLast(new FaultInjectionHttpHandler());

            LOGGER.info(() -> "Channel initialized " + ch);
        }

        private class FaultInjectionSslEngine extends DelegatingSslEngine {
            private final Channel channel;
            private volatile boolean closed = false;

            public FaultInjectionSslEngine(SSLEngine delegate, Channel channel) {
                super(delegate);
                this.channel = channel;
            }

            @Override
            public SSLEngineResult unwrap(ByteBuffer src, ByteBuffer[] dsts, int offset, int length) throws SSLException {
                handleBeforeFailures();
                return super.unwrap(src, dsts, offset, length);
            }

            @Override
            public SSLEngineResult wrap(ByteBuffer[] srcs, int offset, int length, ByteBuffer dst) throws SSLException {
                handleBeforeFailures();
                return super.wrap(srcs, offset, length, dst);
            }

            private void handleBeforeFailures() {
                if (getHandshakeStatus() == NOT_HANDSHAKING && tlsSetupFailTime == TlsSetupFailTime.BEFORE_HANDSHAKE) {
                    closeChannel("Closing channel before handshake " + channel);
                }

                if ((getHandshakeStatus() == NEED_WRAP || getHandshakeStatus() == NEED_TASK || getHandshakeStatus() == NEED_UNWRAP)
                    && tlsSetupFailTime == TlsSetupFailTime.DURING_HANDSHAKE) {
                    closeChannel("Closing channel during handshake " + channel);
                }
            }

            private void closeChannel(String message) {
                if (!closed) {
                    LOGGER.info(() -> message);
                    closed = true;
                    channel.close();
                }
            }
        }

        private class FaultInjectionHttpHandler extends SimpleChannelInboundHandler<Object> {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
                LOGGER.info(() -> "Reading " + msg);
                if (msg instanceof LastHttpContent) {
                    writeResponse(ctx);
                }
            }

            private void writeResponse(ChannelHandlerContext ctx) {
                int responseLength = 1;
                HttpHeaders headers = new DefaultHttpHeaders().add("Content-Length", responseLength);
                ctx.writeAndFlush(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, headers)).addListener(x -> {
                    ByteBuf payload = ctx.alloc().buffer(responseLength);
                    payload.writeBytes(new byte[responseLength]);
                    ctx.writeAndFlush(new DefaultHttpContent(payload)).addListener(ChannelFutureListener.CLOSE);
                });
            }
        }
    }
}
