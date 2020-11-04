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

package software.amazon.awssdk.http.nio.netty.internal.http2;

import static org.assertj.core.api.Assertions.assertThat;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Frame;
import io.netty.handler.codec.http2.Http2FrameCodec;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.codec.http2.Http2SettingsFrame;
import io.netty.util.ReferenceCountUtil;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.reactivestreams.Publisher;
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

public class WindowSizeTest {
    private static final int DEFAULT_INIT_WINDOW_SIZE = 1024 * 1024;

    private TestH2Server server;
    private SdkAsyncHttpClient netty;

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @After
    public void methodTeardown() throws InterruptedException {
        if (netty != null) {
            netty.close();
        }
        netty = null;

        if (server != null) {
            server.shutdown();
        }
        server = null;
    }

    @Test
    public void builderSetter_negativeValue_throws() {
        expected.expect(IllegalArgumentException.class);

        NettyNioAsyncHttpClient.builder()
                .http2Configuration(Http2Configuration.builder()
                        .initialWindowSize(-1)
                        .build())
                .build();
    }

    @Test
    public void builderSetter_0Value_throws() {
        expected.expect(IllegalArgumentException.class);

        NettyNioAsyncHttpClient.builder()
                .http2Configuration(Http2Configuration.builder()
                        .initialWindowSize(0)
                        .build())
                .build();
    }

    @Test
    public void builderSetter_explicitNullSet_usesDefaultValue() throws InterruptedException {
        expectCorrectWindowSizeValueTest(null, DEFAULT_INIT_WINDOW_SIZE);
    }

    @Test
    public void execute_customWindowValue_valueSentInSettings() throws InterruptedException {
        int windowSize = 128 * 1024 * 1024;
        expectCorrectWindowSizeValueTest(windowSize, windowSize);
    }

    @Test
    public void execute_noExplicitValueSet_sendsDefaultValueInSettings() throws InterruptedException {
        ConcurrentLinkedQueue<Http2Frame> receivedFrames = new ConcurrentLinkedQueue<>();

        server = new TestH2Server(() -> new StreamHandler(receivedFrames));

        server.init();

        netty = NettyNioAsyncHttpClient.builder()
                .protocol(Protocol.HTTP2)
                .build();

        AsyncExecuteRequest req = AsyncExecuteRequest.builder()
                .requestContentPublisher(new EmptyPublisher())
                .request(SdkHttpFullRequest.builder()
                        .method(SdkHttpMethod.GET)
                        .protocol("http")
                        .host("localhost")
                        .port(server.port())
                        .build())
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
                .build();

        netty.execute(req).join();

        List<Http2Settings> receivedSettings = receivedFrames.stream()
                .filter(f -> f instanceof Http2SettingsFrame)
                .map(f -> (Http2SettingsFrame) f)
                .map(Http2SettingsFrame::settings)
                .collect(Collectors.toList());

        assertThat(receivedSettings.size()).isGreaterThan(0);
        for (Http2Settings s : receivedSettings) {
            assertThat(s.initialWindowSize()).isEqualTo(DEFAULT_INIT_WINDOW_SIZE);
        }
    }

    private void expectCorrectWindowSizeValueTest(Integer builderSetterValue, int settingsFrameValue) throws InterruptedException {
        ConcurrentLinkedQueue<Http2Frame> receivedFrames = new ConcurrentLinkedQueue<>();

        server = new TestH2Server(() -> new StreamHandler(receivedFrames));

        server.init();

        netty = NettyNioAsyncHttpClient.builder()
                .protocol(Protocol.HTTP2)
                .http2Configuration(Http2Configuration.builder()
                        .initialWindowSize(builderSetterValue)
                        .build())
                .build();

        AsyncExecuteRequest req = AsyncExecuteRequest.builder()
                .requestContentPublisher(new EmptyPublisher())
                .request(SdkHttpFullRequest.builder()
                        .method(SdkHttpMethod.GET)
                        .protocol("http")
                        .host("localhost")
                        .port(server.port())
                        .build())
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
                .build();

        netty.execute(req).join();


        List<Http2Settings> receivedSettings = receivedFrames.stream()
                .filter(f -> f instanceof Http2SettingsFrame)
                .map(f -> (Http2SettingsFrame) f)
                .map(Http2SettingsFrame::settings)
                .collect(Collectors.toList());

        assertThat(receivedSettings.size()).isGreaterThan(0);
        for (Http2Settings s : receivedSettings) {
            assertThat(s.initialWindowSize()).isEqualTo(settingsFrameValue);
        }
    }

    private static final class TestH2Server extends ChannelInitializer<SocketChannel> {
        private final Supplier<ChannelHandler> handlerSupplier;

        private ServerBootstrap bootstrap;
        private ServerSocketChannel channel;

        private TestH2Server(Supplier<ChannelHandler> handlerSupplier) {
            this.handlerSupplier = handlerSupplier;
        }

        public void init() throws InterruptedException {
            bootstrap = new ServerBootstrap()
                    .channel(NioServerSocketChannel.class)
                    .group(new NioEventLoopGroup())
                    .childHandler(this)
                    .localAddress(0)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            channel = ((ServerSocketChannel) bootstrap.bind().await().channel());
        }

        public int port() {
            return channel.localAddress().getPort();
        }

        public void shutdown() throws InterruptedException {
            channel.close().await();
        }

        @Override
        protected void initChannel(SocketChannel ch) {
            Http2FrameCodec codec = Http2FrameCodecBuilder.forServer()
                    .initialSettings(new Http2Settings()
                            .maxConcurrentStreams(5))
                    .build();

            ch.pipeline().addLast(codec);
            ch.pipeline().addLast(handlerSupplier.get());
        }
    }

    private static class StreamHandler extends ChannelInboundHandlerAdapter {
        private final Queue<Http2Frame> receivedFrames;

        private StreamHandler(Queue<Http2Frame> receivedFrames) {
            this.receivedFrames = receivedFrames;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (!(msg instanceof Http2Frame)) {
                ctx.fireChannelRead(msg);
                return;
            }

            Http2Frame frame = (Http2Frame) msg;
            receivedFrames.add(frame);
            if (frame instanceof Http2DataFrame) {
                Http2DataFrame dataFrame = (Http2DataFrame) frame;
                if (dataFrame.isEndStream()) {
                    Http2HeadersFrame respHeaders = new DefaultHttp2HeadersFrame(
                            new DefaultHttp2Headers().status("204"), true)
                            .stream(dataFrame.stream());
                    ctx.writeAndFlush(respHeaders);
                }
            }
            ReferenceCountUtil.release(frame);
        }
    }
}
