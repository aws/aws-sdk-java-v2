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
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.SdkEventLoopGroup;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Tests that the write idle timeout handler detects when no request body data is written
 * and proactively closes the connection.
 */
public class WriteIdleTimeoutTest {

    private SdkAsyncHttpClient netty;
    private Server server;

    @BeforeEach
    public void setup() throws Exception {
        server = new Server();
        server.init();

        netty = NettyNioAsyncHttpClient.builder()
                                       .writeTimeout(Duration.ofMillis(500))
                                       .readTimeout(Duration.ofSeconds(5))
                                       .eventLoopGroup(SdkEventLoopGroup.builder().numberOfThreads(2).build())
                                       .protocol(Protocol.HTTP1_1)
                                       .buildWithDefaults(AttributeMap.builder()
                                                                      .put(TRUST_ALL_CERTIFICATES, true)
                                                                      .build());
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
     * A request body publisher that subscribes but never produces any data, simulating
     * the thread starvation scenario from the customer issue.
     */
    @Test
    public void stalledBodyPublisher_shouldTriggerWriteIdleTimeout() throws InterruptedException, TimeoutException {
        SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                                                       .method(SdkHttpMethod.PUT)
                                                       .protocol("https")
                                                       .host("localhost")
                                                       .port(server.port())
                                                       .putHeader("Content-Length", "1024")
                                                       .build();

        CompletableFuture<Void> future = sendRequest(request, new NeverWritesContentPublisher(1024));

        assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS))
            .hasCauseInstanceOf(IOException.class)
            .hasStackTraceContaining("No data was written to the request body");
    }

    private CompletableFuture<Void> sendRequest(SdkHttpFullRequest request, SdkHttpContentPublisher contentPublisher) {
        return netty.execute(AsyncExecuteRequest.builder()
                                                .responseHandler(new SdkAsyncHttpResponseHandler() {
                                                    @Override
                                                    public void onHeaders(SdkHttpResponse headers) {
                                                    }

                                                    @Override
                                                    public void onStream(Publisher<ByteBuffer> stream) {
                                                    }

                                                    @Override
                                                    public void onError(Throwable error) {
                                                    }
                                                })
                                                .request(request)
                                                .requestContentPublisher(contentPublisher)
                                                .build());
    }

    /**
     * A content publisher that accepts a subscription but never calls onNext/onComplete,
     * simulating a stalled body write.
     */
    private static class NeverWritesContentPublisher implements SdkHttpContentPublisher {
        private final long contentLength;

        NeverWritesContentPublisher(long contentLength) {
            this.contentLength = contentLength;
        }

        @Override
        public Optional<Long> contentLength() {
            return Optional.of(contentLength);
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> s) {
            // Request subscription but never produce data
            s.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    // intentionally do nothing
                }

                @Override
                public void cancel() {
                }
            });
        }
    }

    private static class Server extends ChannelInitializer<Channel> {
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
            pipeline.addLast(sslCtx.newHandler(ch.alloc()));
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
