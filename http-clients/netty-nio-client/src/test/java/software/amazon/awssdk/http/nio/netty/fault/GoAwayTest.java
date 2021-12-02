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
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http2.DefaultHttp2FrameReader;
import io.netty.handler.codec.http2.DefaultHttp2FrameWriter;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2FrameAdapter;
import io.netty.handler.codec.http2.Http2FrameListener;
import io.netty.handler.codec.http2.Http2FrameReader;
import io.netty.handler.codec.http2.Http2FrameWriter;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.util.AttributeKey;
import io.reactivex.Flowable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
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
import software.amazon.awssdk.http.nio.netty.internal.http2.GoAwayException;

/**
 * Tests to ensure that the client behaves as expected when it receives GOAWAY messages.
 */
public class GoAwayTest {

    private SdkAsyncHttpClient netty;
    private SimpleEndpointDriver endpointDriver;

    @After
    public void teardown() throws InterruptedException {
        if (endpointDriver != null) {
            endpointDriver.shutdown();
        }
        endpointDriver = null;

        if (netty != null) {
            netty.close();
        }
        netty = null;
    }

    @Test
    public void goAwayCanCloseAllStreams() throws InterruptedException {
        Set<String> serverChannels = ConcurrentHashMap.newKeySet();

        CountDownLatch allRequestsReceived = new CountDownLatch(2);
        Supplier<Http2FrameListener> frameListenerSupplier = () -> new TestFrameListener() {
            @Override
            public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int padding, boolean endStream) {
                onHeadersReadDelegator(ctx, streamId);
            }

            @Override
            public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int streamDependency, short weight, boolean exclusive, int padding, boolean endStream) {
                onHeadersReadDelegator(ctx, streamId);
            }

            private void onHeadersReadDelegator(ChannelHandlerContext ctx, int streamId) {
                serverChannels.add(ctx.channel().id().asShortText());

                Http2Headers outboundHeaders = new DefaultHttp2Headers()
                    .status("200")
                    .add("content-type", "text/plain")
                    .addInt("content-length", 5);

                frameWriter().writeHeaders(ctx, streamId, outboundHeaders, 0, false, ctx.newPromise());
                ctx.flush();

                allRequestsReceived.countDown();
            }
        };

        endpointDriver = new SimpleEndpointDriver(frameListenerSupplier);
        endpointDriver.init();

        netty = NettyNioAsyncHttpClient.builder()
                                       .protocol(Protocol.HTTP2)
                                       .build();

        CompletableFuture<Void> request1 = sendGetRequest();
        CompletableFuture<Void> request2 = sendGetRequest();

        allRequestsReceived.await();

        endpointDriver.channels.forEach(ch -> {
            if (serverChannels.contains(ch.id().asShortText())) {
                endpointDriver.goAway(ch, 0);
            }
        });

        assertThatThrownBy(() -> request1.join())
            .hasMessageContaining("GOAWAY received from service")
            .hasCauseInstanceOf(GoAwayException.class);

        assertThatThrownBy(() -> request2.join())
            .hasMessageContaining("GOAWAY received from service")
            .hasCauseInstanceOf(GoAwayException.class);

        assertThat(endpointDriver.currentConnectionCount.get()).isEqualTo(0);
    }

    @Test
    public void execute_goAwayReceived_existingChannelsNotReused() throws InterruptedException {
        // Frame listener supplier for each connection
        Supplier<Http2FrameListener> frameListenerSupplier = () -> new TestFrameListener() {
            @Override
            public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int padding, boolean endStream) {
                onHeadersReadDelegator(ctx, streamId);
            }

            @Override
            public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int streamDependency, short weight, boolean exclusive, int padding, boolean endStream) {
                onHeadersReadDelegator(ctx, streamId);
            }

            private void onHeadersReadDelegator(ChannelHandlerContext ctx, int streamId) {
                frameWriter().writeHeaders(ctx, streamId, new DefaultHttp2Headers().add("content-length", "0").status("204"), 0, true, ctx.newPromise());
                ctx.flush();
            }
        };

        endpointDriver = new SimpleEndpointDriver(frameListenerSupplier);
        endpointDriver.init();

        netty = NettyNioAsyncHttpClient.builder()
                .eventLoopGroup(SdkEventLoopGroup.builder().numberOfThreads(1).build())
                .protocol(Protocol.HTTP2)
                .build();

        sendGetRequest().join();

        // Note: It's possible the initial request can cause the client to allocate more than 1 channel
        int initialChannelNum = endpointDriver.channels.size();

        // Send GOAWAY to all the currently open channels
        endpointDriver.channels.forEach(ch -> endpointDriver.goAway(ch, 1));

        // Need to give a chance for the streams to get closed
        Thread.sleep(1000);

        // Since the existing channels are now invalid, this request should cause a new channel to be opened
        sendGetRequest().join();

        assertThat(endpointDriver.channels).hasSize(initialChannelNum + 1);
    }

    // The client should not close streams that are less than the 'last stream
    // ID' given in the GOAWAY frame since it means they were processed fully
    @Test
    public void execute_goAwayReceived_lastStreamId_lowerStreamsNotClosed() throws InterruptedException {
        ConcurrentHashMap<String, Set<Integer>> channelToStreams = new ConcurrentHashMap<>();

        CompletableFuture<Void> stream3Received = new CompletableFuture<>();
        CountDownLatch allRequestsReceived = new CountDownLatch(2);
        byte[] getPayload = "go away!".getBytes(StandardCharsets.UTF_8);
        Supplier<Http2FrameListener> frameListenerSupplier = () -> new TestFrameListener() {
            @Override
            public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int padding, boolean endStream) {
                onHeadersReadDelegator(ctx, streamId);
            }

            @Override
            public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int streamDependency, short weight, boolean exclusive, int padding, boolean endStream) {
                onHeadersReadDelegator(ctx, streamId);
            }

            private void onHeadersReadDelegator(ChannelHandlerContext ctx, int streamId) {
                channelToStreams.computeIfAbsent(ctx.channel().id().asShortText(), (k) -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(streamId);

                if (streamId == 3) {
                    stream3Received.complete(null);
                }

                if (streamId < 5) {
                    Http2Headers outboundHeaders = new DefaultHttp2Headers()
                            .status("200")
                            .add("content-type", "text/plain")
                            .addInt("content-length", getPayload.length);

                    frameWriter().writeHeaders(ctx, streamId, outboundHeaders, 0, false, ctx.newPromise());
                    ctx.flush();
                }

                allRequestsReceived.countDown();
            }
        };

        endpointDriver = new SimpleEndpointDriver(frameListenerSupplier);
        endpointDriver.init();

        netty = NettyNioAsyncHttpClient.builder()
                .protocol(Protocol.HTTP2)
                .build();

        CompletableFuture<Void> stream3Cf = sendGetRequest();// stream ID 3

        // Wait for the request to be received just to ensure that it is given ID 3
        stream3Received.join();

        CompletableFuture<Void> stream5Cf = sendGetRequest();// stream ID 5

        allRequestsReceived.await(10, TimeUnit.SECONDS);

        // send the GOAWAY first, specifying that everything after 3 is not processed
        endpointDriver.channels.forEach(ch -> {
            Set<Integer> streams = channelToStreams.getOrDefault(ch.id().asShortText(), Collections.emptySet());
            if (streams.contains(3)) {
                endpointDriver.goAway(ch, 3);
            }
        });

        // now send the DATA for stream 3, which should still be valid
        endpointDriver.channels.forEach(ch -> {
            Set<Integer> streams = channelToStreams.getOrDefault(ch.id().asShortText(), Collections.emptySet());
            if (streams.contains(3)) {
                endpointDriver.data(ch, 3, getPayload);
            }
        });

        waitForFuture(stream3Cf);
        waitForFuture(stream5Cf);

        assertThat(stream3Cf.isCompletedExceptionally()).isFalse();
        assertThat(stream5Cf.isCompletedExceptionally()).isTrue();
        stream5Cf.exceptionally(e -> {
            assertThat(e).isInstanceOf(IOException.class);
            return null;
        });
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
                        // Consume the stream in order to complete request
                        Flowable.fromPublisher(stream).subscribe(b -> {}, t -> {});
                    }

                    @Override
                    public void onError(Throwable error) {
                    }
                })
                .request(SdkHttpFullRequest.builder()
                        .method(SdkHttpMethod.GET)
                        .protocol("http")
                        .host("localhost")
                        .port(endpointDriver.port())
                        .build())
                .requestContentPublisher(new EmptyPublisher())
                .build();

        return netty.execute(req);
    }

    private static void waitForFuture(CompletableFuture<?> cf) {
        try {
            cf.get(2, TimeUnit.SECONDS);
        } catch (ExecutionException | InterruptedException t) {
        } catch (TimeoutException t) {
            throw new RuntimeException("Future did not complete after 2 seconds.", t);
        }
    }

    // Minimal class to simulate an H2 endpoint
    private static class SimpleEndpointDriver extends ChannelInitializer<SocketChannel> {
        private List<SocketChannel> channels = new ArrayList<>();
        private final NioEventLoopGroup group = new NioEventLoopGroup();
        private final Supplier<Http2FrameListener> frameListenerSupplier;
        private ServerBootstrap bootstrap;
        private ServerSocketChannel serverSock;
        private AtomicInteger currentConnectionCount = new AtomicInteger(0);

        public SimpleEndpointDriver(Supplier<Http2FrameListener> frameListenerSupplier) {
            this.frameListenerSupplier = frameListenerSupplier;
        }

        public void init() throws InterruptedException {
            bootstrap = new ServerBootstrap()
                    .channel(NioServerSocketChannel.class)
                    .group(new NioEventLoopGroup())
                    .childHandler(this)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            serverSock = (ServerSocketChannel) bootstrap.bind(0).sync().channel();
        }

        public void shutdown() throws InterruptedException {
            group.shutdownGracefully().await();
        }

        public int port() {
            return serverSock.localAddress().getPort();
        }

        public void goAway(SocketChannel ch, int lastStreamId) {
            ByteBuf b = ch.alloc().buffer(9 + 8);

            // Frame header
            b.writeMedium(8); // Payload length
            b.writeByte(0x7); // Type = GOAWAY
            b.writeByte(0x0); // Flags
            b.writeInt(0); // 0 = connection frame

            // GOAWAY payload
            b.writeInt(lastStreamId);
            b.writeInt(0); // Error code

            ch.writeAndFlush(b);
        }

        public void data(SocketChannel ch, int streamId, byte[] payload) {
            ByteBuf b = ch.alloc().buffer(9 + payload.length);

            // Header
            b.writeMedium(payload.length); // Payload length
            b.writeByte(0); // Type = DATA
            b.writeByte(0x1); // 0x1 = EOF
            b.writeInt(streamId);

            // Payload
            b.writeBytes(payload);

            ch.writeAndFlush(b);
        }

        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            channels.add(ch);
            ch.pipeline().addLast(new Http2ConnHandler(this, frameListenerSupplier.get()));
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            currentConnectionCount.incrementAndGet();
            super.channelActive(ctx);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            currentConnectionCount.decrementAndGet();
            super.channelInactive(ctx);
        }
    }

    private abstract class TestFrameListener extends Http2FrameAdapter {
        private final Http2FrameWriter frameWriter = new DefaultHttp2FrameWriter();

        protected final Http2FrameWriter frameWriter() {
            return frameWriter;
        }

        @Override
        public void onSettingsRead(ChannelHandlerContext ctx, Http2Settings settings) {
            frameWriter().writeSettings(ctx, new Http2Settings(), ctx.newPromise());
            frameWriter().writeSettingsAck(ctx, ctx.newPromise());
            ctx.flush();
        }
    }

    private static class Http2ConnHandler extends ChannelDuplexHandler {
        // Prior knowledge preface
        private static final String PREFACE = "PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n";
        private static final AttributeKey<Boolean> H2_ESTABLISHED = AttributeKey.newInstance("h2-etablished");

        private final Http2FrameReader frameReader = new DefaultHttp2FrameReader();
        private final SimpleEndpointDriver simpleEndpointDriver;
        private final Http2FrameListener frameListener;

        public Http2ConnHandler(SimpleEndpointDriver simpleEndpointDriver, Http2FrameListener frameListener) {
            this.simpleEndpointDriver = simpleEndpointDriver;
            this.frameListener = frameListener;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            ByteBuf bb = (ByteBuf) msg;
            if (!isH2Established(ctx.channel())) {
                String prefaceString = bb.readCharSequence(24, StandardCharsets.UTF_8).toString();
                if (PREFACE.equals(prefaceString)) {
                    ctx.channel().attr(H2_ESTABLISHED).set(true);
                }
            }
            frameReader.readFrame(ctx, bb, frameListener);
        }

        private boolean isH2Established(Channel ch) {
            return Boolean.TRUE.equals(ch.attr(H2_ESTABLISHED).get());
        }
    }
}
