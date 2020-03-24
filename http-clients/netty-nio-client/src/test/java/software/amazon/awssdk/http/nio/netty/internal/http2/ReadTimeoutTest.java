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
import io.netty.handler.codec.http2.DefaultHttp2WindowUpdateFrame;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Frame;
import io.netty.handler.codec.http2.Http2FrameCodec;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.util.ReferenceCountUtil;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.After;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;

public class ReadTimeoutTest {
    private static final int N_FRAMES = 10;
    private TestH2Server testServer;
    private SdkAsyncHttpClient netty;

    @After
    public void methodTeardown() throws InterruptedException {
        if (testServer != null) {
            testServer.shutdown();
        }
        testServer = null;

        if (netty != null) {
            netty.close();
        }
        netty = null;
    }

    @Test
    public void readTimeoutActivatedAfterRequestFullyWritten() throws InterruptedException {
        testServer = new TestH2Server(StreamHandler::new);
        testServer.init();

        // Set a very short read timeout, shorter than it will take to transfer
        // the body
        netty = NettyNioAsyncHttpClient.builder()
                .protocol(Protocol.HTTP2)
                .readTimeout(Duration.ofMillis(500))
                .build();

        SdkHttpFullRequest sdkRequest = SdkHttpFullRequest.builder()
                .method(SdkHttpMethod.PUT)
                .protocol("http")
                .host("localhost")
                .port(testServer.port())
                .build();

        // at 10 frames, should take approximately 3-4 seconds for the server
        // to receive given that it sleeps for 500ms between data frames and
        // sleeps for most of them
        byte[] data = new byte[16384 * N_FRAMES];

        Publisher<ByteBuffer> dataPublisher = Flowable.just(ByteBuffer.wrap(data));

        AsyncExecuteRequest executeRequest = AsyncExecuteRequest.builder()
                .request(sdkRequest)
                .responseHandler(new SdkAsyncHttpResponseHandler() {
                    @Override
                    public void onHeaders(SdkHttpResponse headers) {
                    }

                    @Override
                    public void onStream(Publisher<ByteBuffer> stream) {
                        Flowable.fromPublisher(stream).forEach(s -> {});
                    }

                    @Override
                    public void onError(Throwable error) {
                    }
                })
                .requestContentPublisher(new SdkHttpContentPublisher() {
                    @Override
                    public Optional<Long> contentLength() {
                        return Optional.of((long) data.length);
                    }

                    @Override
                    public void subscribe(Subscriber<? super ByteBuffer> s) {
                        dataPublisher.subscribe(s);
                    }
                })
                .build();

        netty.execute(executeRequest).join();
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
                    .autoAckPingFrame(true)
                    .initialSettings(new Http2Settings()
                        .initialWindowSize(16384)
                        .maxFrameSize(16384)
                        .maxConcurrentStreams(5))
                    .build();

            ch.pipeline().addLast(codec);
            ch.pipeline().addLast(handlerSupplier.get());
        }
    }

    private static class StreamHandler extends ChannelInboundHandlerAdapter {
        private int sleeps = N_FRAMES - 3;

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (!(msg instanceof Http2Frame)) {
                ctx.fireChannelRead(msg);
                return;
            }

            Http2Frame frame = (Http2Frame) msg;
            if (frame instanceof Http2DataFrame) {
                Http2DataFrame dataFrame = (Http2DataFrame) frame;
                ReferenceCountUtil.release(frame);
                if (dataFrame.isEndStream()) {
                    Http2HeadersFrame respHeaders = new DefaultHttp2HeadersFrame(
                            new DefaultHttp2Headers().status("204"), true)
                            .stream(dataFrame.stream());
                    ctx.writeAndFlush(respHeaders);
                }

                if (sleeps > 0) {
                    --sleeps;
                    // Simulate a server that's slow to read data. Since our
                    // window size is equal to the max frame size, the client
                    // shouldn't be able to send more data until we update our
                    // window
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ie) {
                    }
                }
                ctx.writeAndFlush(new DefaultHttp2WindowUpdateFrame(dataFrame.initialFlowControlledBytes())
                        .stream(dataFrame.stream()));
            }
        }
    }
}
