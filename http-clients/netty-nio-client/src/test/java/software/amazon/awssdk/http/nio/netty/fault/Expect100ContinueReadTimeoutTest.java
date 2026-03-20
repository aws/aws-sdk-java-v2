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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.handler.timeout.ReadTimeoutException;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
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
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Regression test for the bug where {@code OneTimeReadTimeoutHandler} was added before {@code SslHandler} in the pipeline
 * via {@code addFirst}, causing TLS handshake {@code channelRead} events to prematurely remove it. This left no read timeout
 * handler active during the wait for a {@code 100 Continue} response, causing the request to hang indefinitely.
 *
 * <p>The server deliberately delays the TLS handshake to ensure it is still in progress when the client's
 * {@code writeRequest()} adds the timeout handler. Without the fix ({@code addBefore} instead of {@code addFirst}),
 * the handler gets removed by handshake data and the test times out.
 */
public class Expect100ContinueReadTimeoutTest {

    private SdkAsyncHttpClient netty;
    private Server server;

    @BeforeEach
    public void setup() throws Exception {
        server = new Server();
        server.init();

        netty = NettyNioAsyncHttpClient.builder()
                                       .readTimeout(Duration.ofMillis(500))
                                       .eventLoopGroup(SdkEventLoopGroup.builder().numberOfThreads(2).build())
                                       .protocol(Protocol.HTTP1_1)
                                       .buildWithDefaults(AttributeMap.builder().put(TRUST_ALL_CERTIFICATES, true).build());
    }

    @AfterEach
    public void teardown() throws InterruptedException {
        if (server != null) {
            server.shutdown();
        }
        if (netty != null) {
            netty.close();
        }
    }

    /**
     * Sends a PUT request with {@code Expect: 100-continue} to a TLS server that completes the handshake slowly
     * and then never responds. The read timeout must fire.
     *
     * <p>Without the fix, the {@code OneTimeReadTimeoutHandler} is removed by TLS handshake data and the request
     * hangs until the JUnit {@code @Timeout} kills it.
     */
    @Test
    public void expect100Continue_slowTlsHandshake_serverNotResponding_shouldTimeout() {
        SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                                                       .method(SdkHttpMethod.PUT)
                                                       .protocol("https")
                                                       .host("localhost")
                                                       .port(server.port())
                                                       .putHeader("Expect", "100-continue")
                                                       .putHeader("Content-Length", "1024")
                                                       .build();


        CompletableFuture<Void> future = sendRequest(request);
        assertThatThrownBy(() -> future.get(2, TimeUnit.SECONDS))
            .hasRootCauseInstanceOf(ReadTimeoutException.class);
    }

    private CompletableFuture<Void> sendRequest(SdkHttpFullRequest request) {
        return netty.execute(AsyncExecuteRequest.builder()
                                                .responseHandler(new SdkAsyncHttpResponseHandler() {
                                                    @Override
                                                    public void onHeaders(SdkHttpResponse headers) {
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
                                                .request(request)
                                                .requestContentPublisher(new EmptyPublisher())
                                                .build());
    }

    /**
     * A TLS server that delays the handshake and never sends an HTTP response.
     * The handshake delay ensures the client's {@code writeRequest()} executes while the handshake is still in progress,
     * which is the condition required to trigger the bug.
     */
    private static class Server extends ChannelInitializer<Channel> {
        private static final long HANDSHAKE_DELAY_MS = 200;

        private ServerBootstrap bootstrap;
        private ServerSocketChannel serverSock;
        private final NioEventLoopGroup group = new NioEventLoopGroup();
        private SslContext sslCtx;

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
            ChannelPipeline pipeline = ch.pipeline();
            SSLEngine engine = sslCtx.newEngine(ch.alloc());
            pipeline.addLast(new SslHandler(new SlowHandshakeSslEngine(engine)));
            pipeline.addLast(new HttpServerCodec());
            pipeline.addLast(new SilentHttpHandler());
        }

        void shutdown() throws InterruptedException {
            group.shutdownGracefully().await();
            serverSock.close();
        }

        int port() {
            return serverSock.localAddress().getPort();
        }

        /**
         * An SSLEngine wrapper that adds a delay to {@code wrap()} during the handshake,
         * slowing down the server's TLS responses so the client's handshake stays pending
         * while {@code writeRequest()} runs.
         */
        private static class SlowHandshakeSslEngine extends DelegatingSslEngine {
            SlowHandshakeSslEngine(SSLEngine delegate) {
                super(delegate);
            }

            @Override
            public SSLEngineResult wrap(ByteBuffer[] srcs, int offset, int length, ByteBuffer dst) throws SSLException {
                if (getHandshakeStatus() != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING
                    && getHandshakeStatus() != SSLEngineResult.HandshakeStatus.FINISHED) {
                    try {
                        Thread.sleep(HANDSHAKE_DELAY_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                return super.wrap(srcs, offset, length, dst);
            }
        }

        /** Receives the HTTP request but never responds. */
        private static class SilentHttpHandler extends SimpleChannelInboundHandler<HttpRequest> {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, HttpRequest msg) {
                // Intentionally do nothing — simulate server not responding to 100-continue
            }
        }
    }
}
