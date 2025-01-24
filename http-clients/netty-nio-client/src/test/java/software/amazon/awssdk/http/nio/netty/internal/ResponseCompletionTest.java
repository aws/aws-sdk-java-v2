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
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLEngine;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.http.EmptyPublisher;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.ProtocolNegotiation;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.SdkEventLoopGroup;
import software.amazon.awssdk.utils.AttributeMap;


public class ResponseCompletionTest {
    private SdkAsyncHttpClient netty;
    private Server server;

    @AfterEach
    public void teardown() throws InterruptedException {
        System.out.println();
        System.out.println("Teardown() !!!!!!!!!");
        System.out.println();

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
        System.setProperty("javax.net.debug", "ssl,handshake");

        /*System.setProperty("jdk.tls.client.protocols", "TLSv1.2");
        System.setProperty("jdk.tls.client.enableSessionTicketExtension", "false");
        System.setProperty("jdk.tls.client.enableSessionResumption", "false");
        System.setProperty("jdk.tls.client.sessionCacheSize", "0");*/

        server = new Server();
        server.init();

        netty = NettyNioAsyncHttpClient.builder()
                                       .eventLoopGroup(SdkEventLoopGroup.builder().numberOfThreads(2).build())
                                       .protocol(Protocol.HTTP1_1)
                                       // doesn't work
                                       //.maxConcurrency(1)
                                       // TODO
                                       .connectionAcquisitionTimeout(Duration.ofMinutes(1))
                                       .sslProvider(SslProvider.JDK)
                                       .protocolNegotiation(ProtocolNegotiation.ALPN)
                                       .buildWithDefaults(AttributeMap.builder().put(TRUST_ALL_CERTIFICATES, true).build());

        sendGetRequest().join();
        // TODO - hangs in this second request
        System.out.println();
        System.out.println("First request completed, sending second request");
        System.out.println();

        sendGetRequest().get(60, TimeUnit.SECONDS);

        System.out.println("Finished 2nd request");

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
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
                                      /*.applicationProtocolConfig(
                                          new ApplicationProtocolConfig(ApplicationProtocolConfig.Protocol.ALPN,
                                                                        ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                                                                        ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                                                                        ApplicationProtocolNames.HTTP_1_1))*/
                                      .build();

            bootstrap = new ServerBootstrap()
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.DEBUG))
                .group(group)
                .childHandler(this);

            serverSock = (ServerSocketChannel) bootstrap.bind(0).sync().channel();
        }

        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            System.out.println("Server :: Initializing channel: " + ch.id());
            channels.add(ch);
            ChannelPipeline pipeline = ch.pipeline();
            //pipeline.addLast(sslCtx.newHandler(ch.alloc()));

            SslHandler sslHandler = sslCtx.newHandler(ch.alloc());
            // TODO
            sslHandler.setHandshakeTimeout(60, TimeUnit.SECONDS);
            sslHandler.handshakeFuture().addListener(
                future -> {
                    if (future.isSuccess()) {
                        System.out.println("Server SSLHandler :: SSL handshake completed successfully for channel ID == " + ch.id());
                    } else {
                        System.out.println("Server SSLHandler :: SSL handshake failed for channel ID ==  " + ch.id()
                                           + " cause: " + future.cause());
                    }
                }
            );

            pipeline.addLast(sslHandler);

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
                    ctx.writeAndFlush(response)
                       .addListener(i -> ctx.channel().close());
                }
            }
        }
    }
}
