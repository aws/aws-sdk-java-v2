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

package software.amazon.awssdk.protocol.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES;

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
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Frame;
import io.netty.handler.codec.http2.Http2FrameCodec;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.reactivex.Flowable;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.SdkEventLoopGroup;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.DefaultRetryStrategy;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.model.EmptyModeledException;
import software.amazon.awssdk.services.protocolrestjson.model.EventStream;
import software.amazon.awssdk.services.protocolrestjson.model.EventStreamOperationResponse;
import software.amazon.awssdk.services.protocolrestjson.model.EventStreamOperationResponseHandler;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.eventstream.HeaderValue;
import software.amazon.eventstream.Message;

public class EventStreamExceptionUnmarshallingTest {
    private static final AwsCredentialsProvider TEST_CREDENTIALS = StaticCredentialsProvider.create(
        AwsBasicCredentials.create("akid", "skid"));
    private static Server server;
    private static ProtocolRestJsonAsyncClient client;

    @BeforeAll
    public static void setup() throws Exception {
        server = new Server();
        server.init();

        SdkAsyncHttpClient netty = NettyNioAsyncHttpClient.builder()
                                                          .eventLoopGroup(SdkEventLoopGroup.builder().numberOfThreads(3).build())
                                                          .protocol(Protocol.HTTP2)
                                                          .buildWithDefaults(AttributeMap.builder()
                                                                                         .put(TRUST_ALL_CERTIFICATES,
                                                                                              true)
                                                                                         .build());

        client = ProtocolRestJsonAsyncClient.builder()
                                            .overrideConfiguration(o -> o.retryStrategy(DefaultRetryStrategy.doNotRetry()))
                                            .endpointOverride(URI.create("https://localhost:" + server.port()))
                                            .region(Region.US_WEST_2)
                                            .credentialsProvider(TEST_CREDENTIALS)
                                            .httpClient(netty)
                                            .build();
    }

    @AfterAll
    public static void teardown() throws InterruptedException {
        client.close();
        server.shutdown();
    }

    @Test
    public void eventstreamOperation_errorMemberEvent_unmarshalledToCorrectClass() {
        AtomicReference<Throwable> eventstreamException = new AtomicReference<>();

        CompletableFuture<Void> operationFuture = client.eventStreamOperation(r -> {
        }, Flowable.empty(), new EventStreamOperationResponseHandler() {
            @Override
            public void responseReceived(EventStreamOperationResponse response) {
            }

            @Override
            public void onEventStream(SdkPublisher<EventStream> publisher) {
                publisher.subscribe(new Subscriber<EventStream>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        s.request(Long.MAX_VALUE);
                    }

                    @Override
                    public void onNext(EventStream eventStream) {
                    }

                    @Override
                    public void onError(Throwable t) {
                        eventstreamException.set(t);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
            }

            @Override
            public void exceptionOccurred(Throwable throwable) {
            }

            @Override
            public void complete() {

            }
        });

        assertThatThrownBy(operationFuture::join).hasCauseInstanceOf(EmptyModeledException.class);
        assertThat(eventstreamException.get()).isInstanceOf(EmptyModeledException.class);
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

            Http2FrameCodec http2Codec = Http2FrameCodecBuilder.forServer()
                                                               .autoAckPingFrame(true)
                                                               .initialSettings(Http2Settings.defaultSettings().maxConcurrentStreams(1))
                                                               .build();

            Http2MultiplexHandler http2Handler = new Http2MultiplexHandler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel ch) throws Exception {
                    ch.pipeline().addLast(new ResponseHandler());
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

        private class ResponseHandler extends SimpleChannelInboundHandler<Http2Frame> {

            @Override
            protected void channelRead0(ChannelHandlerContext ctx, Http2Frame frame) {
                if ("DATA".equals(frame.name())) {
                    Http2DataFrame data = (Http2DataFrame) frame;

                    if (data.isEndStream()) {
                        Http2Headers headers = new DefaultHttp2Headers();
                        headers.status(HttpResponseStatus.OK.codeAsText());
                        ctx.writeAndFlush(new DefaultHttp2HeadersFrame(headers));

                        Map<String, HeaderValue> eventHeaders = new HashMap<>();
                        eventHeaders.put(":message-type", HeaderValue.fromString("exception"));
                        eventHeaders.put(":exception-type", HeaderValue.fromString("errorOne"));
                        Message errorEvent = new Message(eventHeaders, "{}".getBytes(StandardCharsets.UTF_8));
                        DefaultHttp2DataFrame responseData =
                            new DefaultHttp2DataFrame(Unpooled.wrappedBuffer(errorEvent.toByteBuffer()), true);
                        ctx.writeAndFlush(responseData);
                        ctx.close();
                    }
                }
            }
        }
    }
}
