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

package software.amazon.awssdk.http;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.codec.http2.Http2StreamFrame;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.AttributeKey;
import io.reactivex.Flowable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;

/**
 * Reusable suite for HTTP/2 stream errors on an async HTTP client. A read timeout on one HTTP/2 stream must not end the
 * other streams on its connection. The connection takes no new streams and closes after its last stream ends. Closing the
 * client fails the streams still in flight with an IOException. Uses TLS with ALPN, the default for H2 service clients.
 *
 * <p>Each async HTTP client module subclasses this and provides its client via {@link #createH2Client(int, Duration)}. A
 * client that does not support a tested behavior overrides that test with an empty body.
 */
@EnabledIf("alpnSupported")
public abstract class SdkAsyncHttpClientH2StreamErrorTestSuite {
    private static final String REQUEST_ID = "x-request-id";
    private static final int FRAME_COUNT = 20;
    private static final int FRAME_BYTES = 10;
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(1);
    private static final Duration WAIT = Duration.ofSeconds(5);

    private final Server server = new Server();
    private SdkAsyncHttpClient client;

    /**
     * Creates a client that uses HTTP/2 over TLS with ALPN, trusts all certificates, and allows at least 10 streams per
     * connection.
     *
     * @param eventLoopThreads the number of event loop threads the client uses
     * @param readTimeout the read timeout, for clients that support one
     */
    protected abstract SdkAsyncHttpClient createH2Client(int eventLoopThreads, Duration readTimeout);

    /**
     * Returns the exception the underlying HTTP library raises when a stream hits the read timeout.
     */
    protected abstract Class<? extends Exception> expectedReadTimeoutCause();

    @BeforeEach
    public void setup() throws Exception {
        server.start();
    }

    @AfterEach
    public void teardown() {
        if (client != null) {
            client.close();
        }
        server.shutdown();
    }

    @ParameterizedTest(name = "eventLoopThreads={0}")
    @ValueSource(ints = {1, 3})
    public void execute_readTimeoutOnOneStream_shouldCompleteSiblingStreams(int eventLoopThreads) {
        List<Call> siblings = streamSiblingsAndTimeOutStall(eventLoopThreads);

        Call afterTimeout = send("/quick", "after-timeout");

        assertThat(afterTimeout.execute).succeedsWithin(WAIT);
        assertFullBodies(siblings);
        assertThat(server.requestConnections).as("connection index that served each request")
                                             .containsOnly(entry("warmup", 1), entry("sibling-0", 1), entry("sibling-1", 1),
                                                           entry("sibling-2", 1), entry("stall", 1), entry("after-timeout", 2));
    }

    @ParameterizedTest(name = "eventLoopThreads={0}")
    @ValueSource(ints = {1, 3})
    public void execute_readTimeoutOnOneStream_shouldCloseConnectionAfterLastSibling(int eventLoopThreads) {
        List<Call> siblings = streamSiblingsAndTimeOutStall(eventLoopThreads);

        assertFullBodies(siblings);
        long lastSiblingDone = siblings.stream().mapToLong(c -> c.bodyCompletedNanos).max().getAsLong();
        assertThat(server.closedNanos(1)).as("connection 1 closed")
                                         .succeedsWithin(Duration.ofSeconds(2))
                                         .satisfies(closed -> assertThat(closed).isBetween(
                                             lastSiblingDone, lastSiblingDone + TimeUnit.SECONDS.toNanos(2)));
    }

    @Test
    public void close_withStreamsInFlight_shouldFailStreamsWithIoException() {
        client = createH2Client(3, READ_TIMEOUT);
        assertThat(send("/quick", "warmup").execute).succeedsWithin(WAIT);
        List<Call> streams = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            streams.add(send("/stream", "stream-" + i));
        }
        streams.forEach(s -> assertThat(s.firstFrameReceived).succeedsWithin(WAIT));

        client.close();
        client = null;

        for (Call stream : streams) {
            assertThat(stream.execute).as("%s, %s body bytes received", stream.requestId, stream.bodyBytes)
                                      .failsWithin(WAIT)
                                      .withThrowableOfType(ExecutionException.class)
                                      .withCauseInstanceOf(IOException.class)
                                      .withMessageContaining("An error occurred on the connection")
                                      .withMessageContaining("The HTTP client closed the connection");
            assertThat(stream.bodyError).as("%s body error", stream.requestId)
                                        .isInstanceOf(IOException.class)
                                        .hasMessageContaining("An error occurred on the connection");
            assertThat(stream.bodyCompletedNanos).as("%s body completed", stream.requestId).isZero();
        }
    }

    /**
     * Opens connection 1 with one request, starts three streaming siblings on it, then a stalled request that hits the
     * read timeout. Returns the siblings, still streaming.
     */
    private List<Call> streamSiblingsAndTimeOutStall(int eventLoopThreads) {
        client = createH2Client(eventLoopThreads, READ_TIMEOUT);
        // One request first: concurrent first requests can each open a connection.
        assertThat(send("/quick", "warmup").execute).succeedsWithin(WAIT);
        List<Call> siblings = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            siblings.add(send("/stream", "sibling-" + i));
        }
        siblings.forEach(s -> assertThat(server.firstDataSent(s.requestId)).succeedsWithin(WAIT));

        Call stall = send("/stall", "stall");

        assertThat(stall.execute).failsWithin(WAIT)
                                 .withThrowableOfType(ExecutionException.class)
                                 .withMessageContaining("Read timed out")
                                 .withRootCauseInstanceOf(expectedReadTimeoutCause());
        return siblings;
    }

    private static void assertFullBodies(List<Call> siblings) {
        for (Call sibling : siblings) {
            assertThat(sibling.execute).as(sibling.requestId).succeedsWithin(WAIT);
            assertThat(sibling.bodyBytes).as("%s body bytes", sibling.requestId).hasValue(FRAME_COUNT * FRAME_BYTES);
        }
    }

    private Call send(String path, String requestId) {
        Call call = new Call(requestId);
        SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                                                       .method(SdkHttpMethod.GET)
                                                       .protocol("https")
                                                       .host("127.0.0.1")
                                                       .port(server.port())
                                                       .encodedPath(path)
                                                       .putHeader(REQUEST_ID, requestId)
                                                       .build();
        call.execute = client.execute(AsyncExecuteRequest.builder()
                                                         .request(request)
                                                         .requestContentPublisher(new EmptyPublisher())
                                                         .responseHandler(call)
                                                         .build());
        return call;
    }

    private static boolean alpnSupported() {
        return SslProvider.isAlpnSupported(SslProvider.JDK);
    }

    /**
     * Counts body bytes, not onNext calls, because a client may coalesce DATA frames.
     */
    private static final class Call implements SdkAsyncHttpResponseHandler {
        private final String requestId;
        private final AtomicLong bodyBytes = new AtomicLong();
        private final CompletableFuture<Void> firstFrameReceived = new CompletableFuture<>();
        private volatile long bodyCompletedNanos;
        private volatile Throwable bodyError;
        private CompletableFuture<Void> execute;

        private Call(String requestId) {
            this.requestId = requestId;
        }

        @Override
        public void onHeaders(SdkHttpResponse headers) {
        }

        @Override
        public void onStream(Publisher<ByteBuffer> stream) {
            Flowable.fromPublisher(stream).subscribe(this::onBody,
                                                     t -> bodyError = t,
                                                     () -> bodyCompletedNanos = System.nanoTime());
        }

        private void onBody(ByteBuffer b) {
            if (bodyBytes.addAndGet(b.remaining()) >= FRAME_BYTES) {
                firstFrameReceived.complete(null);
            }
        }

        @Override
        public void onError(Throwable error) {
        }
    }

    /**
     * Answers {@code /quick} with one DATA frame, {@code /stream} with {@value #FRAME_COUNT} DATA frames 100 ms apart and no
     * content-length, and {@code /stall} with nothing. Records which connection served each request and when each
     * connection closed.
     */
    private static final class Server extends ChannelInitializer<Channel> {
        private static final AttributeKey<Integer> CONNECTION_INDEX =
            AttributeKey.valueOf(SdkAsyncHttpClientH2StreamErrorTestSuite.class.getName() + ".CONNECTION_INDEX");

        private final NioEventLoopGroup group = new NioEventLoopGroup();
        private final AtomicInteger connectionCount = new AtomicInteger();
        private final Map<Integer, CompletableFuture<Long>> closedNanos = new ConcurrentHashMap<>();
        private final Map<String, Integer> requestConnections = new ConcurrentHashMap<>();
        private final Map<String, CompletableFuture<Void>> firstDataSent = new ConcurrentHashMap<>();
        private SslContext sslContext;
        private ServerSocketChannel serverSocket;

        void start() throws Exception {
            SelfSignedCertificate certificate = new SelfSignedCertificate();
            sslContext = SslContextBuilder.forServer(certificate.certificate(), certificate.privateKey())
                                          .applicationProtocolConfig(new ApplicationProtocolConfig(
                                              ApplicationProtocolConfig.Protocol.ALPN,
                                              ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                                              ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                                              ApplicationProtocolNames.HTTP_2))
                                          .build();
            serverSocket = (ServerSocketChannel) new ServerBootstrap().channel(NioServerSocketChannel.class)
                                                                      .group(group)
                                                                      .childHandler(this)
                                                                      .bind("127.0.0.1", 0).sync().channel();
        }

        void shutdown() {
            if (serverSocket != null) {
                serverSocket.close().awaitUninterruptibly(5, TimeUnit.SECONDS);
            }
            group.shutdownGracefully(0, 1, TimeUnit.SECONDS).awaitUninterruptibly(10, TimeUnit.SECONDS);
        }

        int port() {
            return serverSocket.localAddress().getPort();
        }

        CompletableFuture<Long> closedNanos(int connectionIndex) {
            return closedNanos.computeIfAbsent(connectionIndex, i -> new CompletableFuture<>());
        }

        CompletableFuture<Void> firstDataSent(String requestId) {
            return firstDataSent.computeIfAbsent(requestId, id -> new CompletableFuture<>());
        }

        @Override
        protected void initChannel(Channel ch) {
            int index = connectionCount.incrementAndGet();
            ch.attr(CONNECTION_INDEX).set(index);
            ch.closeFuture().addListener(f -> closedNanos(index).complete(System.nanoTime()));
            ch.pipeline().addLast(sslContext.newHandler(ch.alloc()),
                                  Http2FrameCodecBuilder.forServer().build(),
                                  new Http2MultiplexHandler(new ChannelInitializer<Channel>() {
                                      @Override
                                      protected void initChannel(Channel stream) {
                                          stream.pipeline().addLast(new StreamHandler());
                                      }
                                  }));
        }

        private final class StreamHandler extends SimpleChannelInboundHandler<Http2StreamFrame> {
            private Http2Headers request;

            @Override
            protected void channelRead0(ChannelHandlerContext ctx, Http2StreamFrame frame) {
                boolean requestEnded = false;
                if (frame instanceof Http2HeadersFrame) {
                    request = ((Http2HeadersFrame) frame).headers();
                    requestConnections.put(requestId(), ctx.channel().parent().attr(CONNECTION_INDEX).get());
                    requestEnded = ((Http2HeadersFrame) frame).isEndStream();
                } else if (frame instanceof Http2DataFrame) {
                    requestEnded = ((Http2DataFrame) frame).isEndStream();
                }
                if (!requestEnded) {
                    return;
                }

                String path = request.path().toString();
                if ("/quick".equals(path)) {
                    ctx.write(new DefaultHttp2HeadersFrame(new DefaultHttp2Headers().status(OK.codeAsText())));
                    ctx.writeAndFlush(dataFrame(true));
                } else if ("/stream".equals(path)) {
                    ctx.write(new DefaultHttp2HeadersFrame(new DefaultHttp2Headers().status(OK.codeAsText())));
                    ctx.writeAndFlush(dataFrame(false)).addListener(f -> firstDataSent(requestId()).complete(null));
                    for (int i = 1; i < FRAME_COUNT; i++) {
                        boolean last = i == FRAME_COUNT - 1;
                        ctx.channel().eventLoop().schedule(() -> ctx.writeAndFlush(dataFrame(last)),
                                                           i * 100L, TimeUnit.MILLISECONDS);
                    }
                }
            }

            private String requestId() {
                return String.valueOf(request.get(REQUEST_ID));
            }

            private DefaultHttp2DataFrame dataFrame(boolean endStream) {
                return new DefaultHttp2DataFrame(Unpooled.wrappedBuffer(new byte[FRAME_BYTES]), endStream);
            }
        }
    }
}
