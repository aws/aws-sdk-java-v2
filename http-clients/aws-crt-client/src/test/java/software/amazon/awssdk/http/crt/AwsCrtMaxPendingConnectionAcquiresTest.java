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

package software.amazon.awssdk.http.crt;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.http.HttpTestUtils.createProvider;
import static software.amazon.awssdk.http.crt.CrtHttpClientTestUtils.createRequest;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.codec.http2.Http2StreamFrame;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.crt.http.HttpException;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.RecordingResponseHandler;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.IoUtils;

/**
 * With maxConcurrency(1) and maxPendingConnectionAcquires(1), one request holds the only connection (or stream, on HTTP/2),
 * one waits, and a third concurrent request is over the limit. On HTTP/1.1 the server delay keeps the first two outstanding
 * while the third is submitted; the HTTP/2 server holds its responses until the test releases them.
 */
public class AwsCrtMaxPendingConnectionAcquiresTest {
    private static final int SERVER_DELAY_MILLIS = 250;
    private static final int MAX_PENDING_ACQUISITIONS_EXCEEDED = 2094;

    @RegisterExtension
    static WireMockExtension mockServer = WireMockExtension.newInstance()
                                                           .options(wireMockConfig().dynamicPort())
                                                           .build();

    private static ThreadPoolExecutor executor;

    private URI uri;

    @BeforeAll
    static void setUpExecutor() {
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);
        executor.prestartAllCoreThreads();
    }

    @AfterAll
    static void tearDownExecutor() {
        executor.shutdownNow();
    }

    @BeforeEach
    void setUp() {
        uri = URI.create("http://localhost:" + mockServer.getPort());
        mockServer.stubFor(any(anyUrl()).willReturn(aResponse().withFixedDelay(SERVER_DELAY_MILLIS).withBody("OK")));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void asyncBuilderMaxPendingConnectionAcquires_nonPositive_throwsIllegalArgumentException(int value) {
        assertThatThrownBy(() -> AwsCrtAsyncHttpClient.builder().maxPendingConnectionAcquires(value))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("maxPendingConnectionAcquires");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void syncBuilderMaxPendingConnectionAcquires_nonPositive_throwsIllegalArgumentException(int value) {
        assertThatThrownBy(() -> AwsCrtHttpClient.builder().maxPendingConnectionAcquires(value))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("maxPendingConnectionAcquires");
    }

    @Test
    void builderMaxPendingConnectionAcquires_null_isAccepted() {
        assertThatCode(() -> AwsCrtAsyncHttpClient.builder().maxPendingConnectionAcquires(null)).doesNotThrowAnyException();
        assertThatCode(() -> AwsCrtHttpClient.builder().maxPendingConnectionAcquires(null)).doesNotThrowAnyException();
    }

    @Test
    void asyncExecute_overLimit_failsFutureWithNonRetryableHttpException() throws Exception {
        try (SdkAsyncHttpClient client = limitedAsyncClient()) {
            CompletableFuture<Void> first = execute(client, uri, new RecordingResponseHandler());
            CompletableFuture<Void> second = execute(client, uri, new RecordingResponseHandler());

            CompletableFuture<Void> rejected = execute(client, uri, new RecordingResponseHandler());

            assertThatThrownBy(() -> rejected.get(5, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .satisfies(e -> assertMaxPendingAcquisitionsExceeded(e.getCause()));
            CompletableFuture.allOf(first, second).get(5, TimeUnit.SECONDS);
        }
    }

    // Fails until the CRT HTTP/2 stream manager honors maxPendingConnectionAcquisitions: aws-crt 0.50.0 and earlier ignore it,
    // so the third request waits for a stream instead of being rejected, and the bounded get times out.
    @Test
    void asyncExecuteH2_overLimit_failsFutureWithNonRetryableHttpException() throws Exception {
        try (HeldResponseH2cServer server = HeldResponseH2cServer.start();
             SdkAsyncHttpClient client = AwsCrtAsyncHttpClient.builder()
                                                              .protocol(Protocol.HTTP2)
                                                              .maxConcurrency(1)
                                                              .maxPendingConnectionAcquires(1)
                                                              .build()) {
            URI h2Uri = URI.create("http://localhost:" + server.port());
            CompletableFuture<Void> first = execute(client, h2Uri, new RecordingResponseHandler());
            CompletableFuture<Void> second = execute(client, h2Uri, new RecordingResponseHandler());

            CompletableFuture<Void> rejected = execute(client, h2Uri, new RecordingResponseHandler());

            assertThatThrownBy(() -> rejected.get(500, TimeUnit.MILLISECONDS))
                .isInstanceOf(ExecutionException.class)
                .satisfies(e -> assertMaxPendingAcquisitionsExceeded(e.getCause()));
            server.releaseResponses();
            CompletableFuture.allOf(first, second).get(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void asyncExecute_overLimit_notifiesResponseHandlerOnError() throws Exception {
        try (SdkAsyncHttpClient client = limitedAsyncClient()) {
            CompletableFuture<Void> first = execute(client, uri, new RecordingResponseHandler());
            CompletableFuture<Void> second = execute(client, uri, new RecordingResponseHandler());
            RecordingResponseHandler rejectedHandler = new RecordingResponseHandler();

            execute(client, uri, rejectedHandler);

            assertThatThrownBy(() -> rejectedHandler.completeFuture().get(5, TimeUnit.SECONDS))
                .hasCauseInstanceOf(HttpException.class);
            CompletableFuture.allOf(first, second).get(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void asyncExecute_overLimit_doesNotSendRejectedRequest() throws Exception {
        try (SdkAsyncHttpClient client = limitedAsyncClient()) {
            CompletableFuture<Void> first = execute(client, uri, new RecordingResponseHandler());
            CompletableFuture<Void> second = execute(client, uri, new RecordingResponseHandler());
            CompletableFuture<Void> rejected = execute(client, uri, new RecordingResponseHandler());

            assertThatThrownBy(() -> rejected.get(5, TimeUnit.SECONDS)).isInstanceOf(Exception.class);
            CompletableFuture.allOf(first, second).get(5, TimeUnit.SECONDS);

            mockServer.verify(2, anyRequestedFor(anyUrl()));
        }
    }

    @Test
    void asyncExecute_limitNotSet_queuesAllRequests() throws Exception {
        try (SdkAsyncHttpClient client = AwsCrtAsyncHttpClient.builder().maxConcurrency(1).build()) {
            List<CompletableFuture<Void>> futures = IntStream.range(0, 3)
                                                             .mapToObj(i -> execute(client, uri, new RecordingResponseHandler()))
                                                             .collect(Collectors.toList());

            assertThatCode(() -> CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(10, TimeUnit.SECONDS))
                .doesNotThrowAnyException();
        }
    }

    @Test
    void asyncBuildWithDefaults_maxPendingConnectionAcquiresServiceDefault_isNotApplied() throws Exception {
        AttributeMap serviceDefaults = AttributeMap.builder()
                                                   .put(SdkHttpConfigurationOption.MAX_PENDING_CONNECTION_ACQUIRES, 1)
                                                   .build();
        try (SdkAsyncHttpClient client = AwsCrtAsyncHttpClient.builder().maxConcurrency(1).buildWithDefaults(serviceDefaults)) {
            List<CompletableFuture<Void>> futures = IntStream.range(0, 3)
                                                             .mapToObj(i -> execute(client, uri, new RecordingResponseHandler()))
                                                             .collect(Collectors.toList());

            assertThatCode(() -> CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(10, TimeUnit.SECONDS))
                .doesNotThrowAnyException();
        }
    }

    @Test
    void syncCall_overLimit_throwsNonRetryableHttpException() {
        try (SdkHttpClient client = AwsCrtHttpClient.builder().maxConcurrency(1).maxPendingConnectionAcquires(1).build()) {
            List<Throwable> failures = failuresOfConcurrentSyncCalls(client, 3);

            assertThat(failures).hasSize(1);
            assertMaxPendingAcquisitionsExceeded(failures.get(0));
        }
    }

    @Test
    void syncCall_limitNotSet_queuesAllRequests() {
        try (SdkHttpClient client = AwsCrtHttpClient.builder().maxConcurrency(1).build()) {
            assertThat(failuresOfConcurrentSyncCalls(client, 3)).isEmpty();
        }
    }

    private static SdkAsyncHttpClient limitedAsyncClient() {
        return AwsCrtAsyncHttpClient.builder().maxConcurrency(1).maxPendingConnectionAcquires(1).build();
    }

    private static void assertMaxPendingAcquisitionsExceeded(Throwable failure) {
        assertThat(failure).isNotInstanceOf(IOException.class)
                           .isInstanceOfSatisfying(HttpException.class,
                                                   e -> assertThat(e.getErrorCode()).isEqualTo(MAX_PENDING_ACQUISITIONS_EXCEEDED));
    }

    private static CompletableFuture<Void> execute(SdkAsyncHttpClient client, URI endpoint, RecordingResponseHandler handler) {
        return client.execute(AsyncExecuteRequest.builder()
                                                 .request(createRequest(endpoint))
                                                 .requestContentPublisher(createProvider(""))
                                                 .responseHandler(handler)
                                                 .build());
    }

    private List<Throwable> failuresOfConcurrentSyncCalls(SdkHttpClient client, int count) {
        HttpExecuteRequest request = HttpExecuteRequest.builder()
                                                       .request(createRequest(uri))
                                                       .contentStreamProvider(() -> new ByteArrayInputStream(new byte[0]))
                                                       .build();
        List<CompletableFuture<Throwable>> outcomes =
            IntStream.range(0, count)
                     .mapToObj(i -> CompletableFuture.runAsync(() -> invokeSafely(() -> client.prepareRequest(request).call()
                                                                                .responseBody()
                                                                                .ifPresent(body -> IoUtils.closeQuietly(body, null))),
                                                               executor)
                                                     .handle((r, t) -> t instanceof CompletionException ? t.getCause() : t))
                     .collect(Collectors.toList());
        return outcomes.stream()
                       .map(CompletableFuture::join)
                       .filter(t -> t != null)
                       .collect(Collectors.toList());
    }

    /**
     * Plaintext HTTP/2 (prior knowledge) server that holds every response until {@link #releaseResponses()} is called, and
     * answers immediately after that.
     */
    private static final class HeldResponseH2cServer implements AutoCloseable {
        private final NioEventLoopGroup group = new NioEventLoopGroup(1);
        private final List<ChannelHandlerContext> heldStreams = new ArrayList<>();
        private boolean released;
        private Channel serverChannel;

        static HeldResponseH2cServer start() throws InterruptedException {
            HeldResponseH2cServer server = new HeldResponseH2cServer();
            server.serverChannel = new ServerBootstrap()
                .group(server.group)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline().addLast(Http2FrameCodecBuilder.forServer().build(),
                                              new Http2MultiplexHandler(server.streamHandler()));
                    }
                })
                .bind(0).sync().channel();
            return server;
        }

        private ChannelInitializer<Channel> streamHandler() {
            return new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel stream) {
                    stream.pipeline().addLast(new SimpleChannelInboundHandler<Http2StreamFrame>() {
                        @Override
                        protected void channelRead0(ChannelHandlerContext ctx, Http2StreamFrame frame) {
                            if (isEndOfRequest(frame)) {
                                respondOrHold(ctx);
                            }
                        }
                    });
                }
            };
        }

        private synchronized void respondOrHold(ChannelHandlerContext ctx) {
            if (released) {
                respond(ctx);
            } else {
                heldStreams.add(ctx);
            }
        }

        synchronized void releaseResponses() {
            released = true;
            heldStreams.forEach(ctx -> ctx.executor().execute(() -> respond(ctx)));
            heldStreams.clear();
        }

        private static boolean isEndOfRequest(Http2StreamFrame frame) {
            return (frame instanceof Http2HeadersFrame && ((Http2HeadersFrame) frame).isEndStream())
                   || (frame instanceof Http2DataFrame && ((Http2DataFrame) frame).isEndStream());
        }

        private static void respond(ChannelHandlerContext ctx) {
            ctx.write(new DefaultHttp2HeadersFrame(new DefaultHttp2Headers().status("200"), false));
            ctx.writeAndFlush(new DefaultHttp2DataFrame(true));
        }

        int port() {
            return ((InetSocketAddress) serverChannel.localAddress()).getPort();
        }

        @Override
        public void close() throws InterruptedException {
            serverChannel.close().sync();
            group.shutdownGracefully(0, 1, TimeUnit.SECONDS).await();
        }
    }
}
