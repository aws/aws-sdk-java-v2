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

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.CLOSE;
import static io.netty.handler.codec.http.HttpHeaderValues.TEXT_PLAIN;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES;

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
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Test;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.http.EmptyPublisher;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.SdkEventLoopGroup;
import software.amazon.awssdk.utils.AttributeMap;


public class ResponseCompletionTest {
    private SdkAsyncHttpClient netty;
    private Server server;

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
    public void connectionCloseAfterResponse_shouldNotReuseConnection() throws Exception {
        server = new Server();
        server.init();

        netty = NettyNioAsyncHttpClient.builder()
                                       .eventLoopGroup(SdkEventLoopGroup.builder().numberOfThreads(2).build())
                                       .protocol(Protocol.HTTP1_1)
                                       .buildWithDefaults(AttributeMap.builder().put(TRUST_ALL_CERTIFICATES, true).build());

        sendGetRequest().join();
        sendGetRequest().join();

        assertThat(server.channels.size()).isEqualTo(2);
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


    private static class Server extends ChannelInitializer<SocketChannel> {
        private static final byte[] CONTENT = RandomStringUtils.randomAscii(7000).getBytes();
        private ServerBootstrap bootstrap;
        private ServerSocketChannel serverSock;
        private List<SocketChannel> channels = new ArrayList<>();
        private final NioEventLoopGroup group = new NioEventLoopGroup();
        private SslContext sslCtx;

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
            pipeline.addLast(new AlwaysCloseConnectionChannelHandler());
        }

        public void shutdown() throws InterruptedException {
            group.shutdownGracefully().await();
        }

        public int port() {
            return serverSock.localAddress().getPort();
        }

        private static class AlwaysCloseConnectionChannelHandler extends ChannelDuplexHandler {

            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) {
                if (msg instanceof HttpRequest) {
                    FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, OK,
                                                                            Unpooled.wrappedBuffer(CONTENT));

                    response.headers()
                            .set(CONTENT_TYPE, TEXT_PLAIN)
                            .set(CONNECTION, CLOSE)
                            .setInt(CONTENT_LENGTH, response.content().readableBytes());
                    ctx.writeAndFlush(response).addListener(i -> ctx.channel().close());
                }
            }
        }
    }
}
