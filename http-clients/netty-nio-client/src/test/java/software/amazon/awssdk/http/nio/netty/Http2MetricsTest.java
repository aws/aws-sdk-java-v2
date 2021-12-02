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

package software.amazon.awssdk.http.nio.netty;

import static org.assertj.core.api.Assertions.assertThat;

import io.netty.bootstrap.ServerBootstrap;
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
import io.netty.handler.codec.http2.Http2StreamFrame;
import io.netty.util.ReferenceCountUtil;
import java.net.URI;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.http.EmptyPublisher;
import software.amazon.awssdk.http.Http2Metric;
import software.amazon.awssdk.http.HttpMetric;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricCollector;

public class Http2MetricsTest {
    private static final int H2_DEFAULT_WINDOW_SIZE = 65535;
    private static final int SERVER_MAX_CONCURRENT_STREAMS = 2;
    private static final int SERVER_INITIAL_WINDOW_SIZE = 65535 * 2;

    private static final TestHttp2Server SERVER = new TestHttp2Server();

    @BeforeClass
    public static void setup() throws InterruptedException {
        SERVER.start();
    }

    @AfterClass
    public static void teardown() throws InterruptedException {
        SERVER.stop();
    }

    @Test
    public void maxClientStreamsLowerThanServerMaxStreamsReportClientMaxStreams() {
        try (SdkAsyncHttpClient client = NettyNioAsyncHttpClient.builder()
                                                                .protocol(Protocol.HTTP2)
                                                                .maxConcurrency(10)
                                                                .http2Configuration(c -> c.maxStreams(1L)
                                                                                          .initialWindowSize(65535 * 3))
                                                                .build()) {
            MetricCollector metricCollector = MetricCollector.create("test");
            client.execute(createExecuteRequest(metricCollector)).join();
            MetricCollection metrics = metricCollector.collect();

            assertThat(metrics.metricValues(HttpMetric.HTTP_CLIENT_NAME)).containsExactly("NettyNio");
            assertThat(metrics.metricValues(HttpMetric.MAX_CONCURRENCY)).containsExactly(10);
            assertThat(metrics.metricValues(HttpMetric.LEASED_CONCURRENCY).get(0)).isBetween(0, 1);
            assertThat(metrics.metricValues(HttpMetric.PENDING_CONCURRENCY_ACQUIRES).get(0)).isBetween(0, 1);
            assertThat(metrics.metricValues(HttpMetric.AVAILABLE_CONCURRENCY)).containsExactly(0);
            // The stream window doesn't get initialized with the connection
            // initial setting and the update appears to be asynchronous so
            // this may be the default window size just based on when the
            // stream window was queried or if this is the first time the
            // stream is used (i.e. not previously pooled)
            assertThat(metrics.metricValues(Http2Metric.LOCAL_STREAM_WINDOW_SIZE_IN_BYTES).get(0)).isIn(H2_DEFAULT_WINDOW_SIZE, 65535 * 3);
            assertThat(metrics.metricValues(Http2Metric.REMOTE_STREAM_WINDOW_SIZE_IN_BYTES)).containsExactly(SERVER_INITIAL_WINDOW_SIZE);
        }
    }

    @Test
    public void maxClientStreamsHigherThanServerMaxStreamsReportServerMaxStreams() {
        try (SdkAsyncHttpClient client = NettyNioAsyncHttpClient.builder()
                                                                .protocol(Protocol.HTTP2)
                                                                .maxConcurrency(10)
                                                                .http2Configuration(c -> c.maxStreams(3L)
                                                                                          .initialWindowSize(65535 * 3))
                                                                .build()) {
            MetricCollector metricCollector = MetricCollector.create("test");
            client.execute(createExecuteRequest(metricCollector)).join();
            MetricCollection metrics = metricCollector.collect();

            assertThat(metrics.metricValues(HttpMetric.HTTP_CLIENT_NAME)).containsExactly("NettyNio");
            assertThat(metrics.metricValues(HttpMetric.MAX_CONCURRENCY)).containsExactly(10);
            assertThat(metrics.metricValues(HttpMetric.LEASED_CONCURRENCY).get(0)).isBetween(0, 1);
            assertThat(metrics.metricValues(HttpMetric.PENDING_CONCURRENCY_ACQUIRES).get(0)).isBetween(0, 1);
            assertThat(metrics.metricValues(HttpMetric.AVAILABLE_CONCURRENCY).get(0)).isIn(0, 2, 3);
            // The stream window doesn't get initialized with the connection
            // initial setting and the update appears to be asynchronous so
            // this may be the default window size just based on when the
            // stream window was queried or if this is the first time the
            // stream is used (i.e. not previously pooled)
            assertThat(metrics.metricValues(Http2Metric.LOCAL_STREAM_WINDOW_SIZE_IN_BYTES).get(0)).isIn(H2_DEFAULT_WINDOW_SIZE, 65535 * 3);
            assertThat(metrics.metricValues(Http2Metric.REMOTE_STREAM_WINDOW_SIZE_IN_BYTES)).containsExactly(SERVER_INITIAL_WINDOW_SIZE);
        }
    }

    private AsyncExecuteRequest createExecuteRequest(MetricCollector metricCollector)  {
        URI uri = URI.create("http://localhost:" + SERVER.port());
        SdkHttpRequest request = createRequest(uri);
        return AsyncExecuteRequest.builder()
                                  .request(request)
                                  .requestContentPublisher(new EmptyPublisher())
                                  .responseHandler(new RecordingResponseHandler())
                                  .metricCollector(metricCollector)
                                  .build();
    }

    private SdkHttpFullRequest createRequest(URI uri) {
        return SdkHttpFullRequest.builder()
                                 .uri(uri)
                                 .method(SdkHttpMethod.GET)
                                 .encodedPath("/")
                                 .putHeader("Host", uri.getHost())
                                 .putHeader("Content-Length", "0")
                                 .build();
    }

    private static final class TestHttp2Server extends ChannelInitializer<SocketChannel> {
        private ServerBootstrap bootstrap;
        private ServerSocketChannel channel;

        private TestHttp2Server() {
        }

        public void start() throws InterruptedException {
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

        public void stop() throws InterruptedException {
            channel.close().await();
        }

        @Override
        protected void initChannel(SocketChannel ch) {
            Http2FrameCodec codec = Http2FrameCodecBuilder.forServer()
                                                          .initialSettings(new Http2Settings()
                                                                               .maxConcurrentStreams(SERVER_MAX_CONCURRENT_STREAMS)
                                                                               .initialWindowSize(SERVER_INITIAL_WINDOW_SIZE))
                                                          .build();
            ch.pipeline().addLast(codec);
            ch.pipeline().addLast(new SuccessfulHandler());
        }
    }

    private static class SuccessfulHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (!(msg instanceof Http2Frame)) {
                ctx.fireChannelRead(msg);
                return;
            }
            ReferenceCountUtil.release(msg);

            boolean isEnd = isEndFrame(msg);
            if (isEnd) {
                ctx.writeAndFlush(new DefaultHttp2HeadersFrame(new DefaultHttp2Headers().status("204"), true)
                                      .stream(((Http2StreamFrame) msg).stream()));
            }
        }

        private boolean isEndFrame(Object msg) {
            if (msg instanceof Http2HeadersFrame) {
                return ((Http2HeadersFrame) msg).isEndStream();
            }

            if (msg instanceof Http2DataFrame) {
                return ((Http2DataFrame) msg).isEndStream();
            }

            return false;
        }
    }
}
