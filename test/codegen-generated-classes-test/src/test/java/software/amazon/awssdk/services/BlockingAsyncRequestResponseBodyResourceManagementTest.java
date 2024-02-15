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

package software.amazon.awssdk.services;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.TEXT_PLAIN;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.assertj.core.api.Assertions.assertThat;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.model.StreamingOutputOperationRequest;
import software.amazon.awssdk.services.protocolrestjson.model.StreamingOutputOperationResponse;

@Timeout(10)
public class BlockingAsyncRequestResponseBodyResourceManagementTest {
    private ProtocolRestJsonAsyncClient client;
    private Server server;


    @AfterEach
    void tearDownPerTest() throws InterruptedException {
        server.shutdown();
        server = null;
        client.close();;

    }

    @BeforeEach
    void setUpPerTest() throws Exception {
        server = new Server();
        server.init();

        client = ProtocolRestJsonAsyncClient.builder()
                                            .region(Region.US_WEST_2)
                                            .credentialsProvider(AnonymousCredentialsProvider.create())
                                            .endpointOverride(URI.create("http://localhost:" + server.port()))
                                            .overrideConfiguration(o -> o.retryPolicy(RetryPolicy.none()))
                                            .build();
    }


    @Test
    void blockingResponseTransformer_abort_shouldCloseUnderlyingConnection() throws IOException {
        verifyConnection(r -> r.abort());
    }

    @Test
    void blockingResponseTransformer_close_shouldCloseUnderlyingConnection() throws IOException {
        Consumer<ResponseInputStream<StreamingOutputOperationResponse>> closeInputStream = closeInputStraem();
        verifyConnection(closeInputStream);
    }


    private static Consumer<ResponseInputStream<StreamingOutputOperationResponse>> closeInputStraem() {
        Consumer<ResponseInputStream<StreamingOutputOperationResponse>> closeInputStream = r -> {
            try {
                r.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
        return closeInputStream;
    }


    void verifyConnection(Consumer<ResponseInputStream<StreamingOutputOperationResponse>> consumer) throws IOException {

        CompletableFuture<ResponseInputStream<StreamingOutputOperationResponse>> responseFuture =
            client.streamingOutputOperation(StreamingOutputOperationRequest.builder().build(),
                                                 AsyncResponseTransformer.toBlockingInputStream());
        ResponseInputStream<StreamingOutputOperationResponse> responseStream = responseFuture.join();


        responseStream.read();
        consumer.accept(responseStream);

        try {
            client.headOperation().join();
        } catch (Exception exception) {
            // Doesn't matter if the request succeeds or not
        }

        // Total of 2 connections got established.
        assertThat(server.channels.size()).isEqualTo(2);
    }

    private static class Server extends ChannelInitializer<Channel> {
        private static final byte[] CONTENT = ("{  "
                                               + "\"foo\": " + RandomStringUtils.randomAscii(1024 * 1024)
                                               + "}").getBytes(StandardCharsets.UTF_8);
        private ServerBootstrap bootstrap;
        private ServerSocketChannel serverSock;
        private List<Channel> channels = new ArrayList<>();
        private final NioEventLoopGroup group = new NioEventLoopGroup(3);

        public void init() throws Exception {
            bootstrap = new ServerBootstrap()
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .group(group)
                .childHandler(this);

            serverSock = (ServerSocketChannel) bootstrap.bind(0).sync().channel();
        }

        public void shutdown() throws InterruptedException {
            group.shutdownGracefully().await();
            serverSock.close();
        }

        public int port() {
            return serverSock.localAddress().getPort();
        }

        @Override
        protected void initChannel(Channel ch) {
            channels.add(ch);
            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast(new HttpServerCodec());
            pipeline.addLast(new BehaviorTestChannelHandler());
        }


        private class BehaviorTestChannelHandler extends ChannelDuplexHandler {

            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) {

                if (!(msg instanceof HttpRequest)) {
                    return;
                }

                HttpMethod method = ((HttpRequest) msg).method();

                if (Objects.equals(method, HttpMethod.HEAD)) {
                    DefaultHttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, OK);
                    ctx.writeAndFlush(response);
                    return;
                }

                FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, OK,
                                                                        Unpooled.wrappedBuffer(CONTENT));

                response.headers()
                        .set(CONTENT_TYPE, TEXT_PLAIN)
                        .setInt(CONTENT_LENGTH, response.content().readableBytes());

                ctx.writeAndFlush(response);
            }
        }
    }
}
