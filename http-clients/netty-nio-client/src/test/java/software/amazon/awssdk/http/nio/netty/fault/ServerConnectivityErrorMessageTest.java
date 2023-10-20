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

import static javax.net.ssl.SSLEngineResult.HandshakeStatus.NEED_TASK;
import static javax.net.ssl.SSLEngineResult.HandshakeStatus.NEED_UNWRAP;
import static javax.net.ssl.SSLEngineResult.HandshakeStatus.NEED_WRAP;
import static javax.net.ssl.SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;
import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.http.EmptyPublisher;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.SimpleHttpContentPublisher;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.SdkEventLoopGroup;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.Logger;

public class ServerConnectivityErrorMessageTest {
    private static final Logger LOGGER = Logger.loggerFor(ServerConnectivityErrorMessageTest.class);
    private SdkAsyncHttpClient netty;
    private Server server;

    public static Collection<TestCase> testCases() {
        return Arrays.asList(new TestCase(CloseTime.DURING_INIT, "The connection was closed during the request."),
                             new TestCase(CloseTime.BEFORE_SSL_HANDSHAKE, "The connection was closed during the request."),
                             new TestCase(CloseTime.DURING_SSL_HANDSHAKE, "The connection was closed during the request."),
                             new TestCase(CloseTime.BEFORE_REQUEST_PAYLOAD, "The connection was closed during the request."),
                             new TestCase(CloseTime.DURING_REQUEST_PAYLOAD, "The connection was closed during the request."),
                             new TestCase(CloseTime.BEFORE_RESPONSE_HEADERS, "The connection was closed during the request."),
                             new TestCase(CloseTime.BEFORE_RESPONSE_PAYLOAD, "Response had content-length"),
                             new TestCase(CloseTime.DURING_RESPONSE_PAYLOAD, "Response had content-length"));
    }

    public static Collection<TestCase> testCasesForHttpContinueResponse() {
        return Arrays.asList(new TestCase(CloseTime.DURING_INIT, "The connection was closed during the request."),
                             new TestCase(CloseTime.BEFORE_SSL_HANDSHAKE, "The connection was closed during the request."),
                             new TestCase(CloseTime.DURING_SSL_HANDSHAKE, "The connection was closed during the request."),
                             new TestCase(CloseTime.BEFORE_REQUEST_PAYLOAD, "The connection was closed during the request."),
                             new TestCase(CloseTime.DURING_REQUEST_PAYLOAD, "The connection was closed during the request."),
                             new TestCase(CloseTime.BEFORE_RESPONSE_HEADERS, "The connection was closed during the request."),
                             new TestCase(CloseTime.BEFORE_RESPONSE_PAYLOAD, "The connection was closed during the request."),
                             new TestCase(CloseTime.DURING_RESPONSE_PAYLOAD, "The connection was closed during the request."));
    }

    @AfterEach
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

    @ParameterizedTest
    @MethodSource("testCases")
    void closeTimeHasCorrectMessage(TestCase testCase) throws Exception {
        server = new Server(ServerConfig.builder().httpResponseStatus(HttpResponseStatus.OK).build());
        setupTestCase(testCase);
        server.closeTime = testCase.closeTime;
        assertThat(captureException(RequestParams.builder()
                                                 .httpMethod(SdkHttpMethod.GET)
                                                 .contentPublisher(new EmptyPublisher())
                                                 .build()))
            .hasMessageContaining(testCase.errorMessageSubstring);
    }

    @ParameterizedTest
    @MethodSource("testCasesForHttpContinueResponse")
    void closeTimeHasCorrectMessageWith100ContinueResponse(TestCase testCase) throws Exception {
        server = new Server(ServerConfig.builder().httpResponseStatus(HttpResponseStatus.CONTINUE).build());
        setupTestCase(testCase);
        server.closeTime = testCase.closeTime;
        assertThat(captureException(RequestParams.builder()
                                                 .httpMethod(SdkHttpMethod.PUT)
                                                 .addHeaderKeyValue(HttpHeaderNames.EXPECT.toString(),
                                                                    Arrays.asList(HttpHeaderValues.CONTINUE.toString()))
                                                 .contentPublisher(new SimpleHttpContentPublisher("reqBody".getBytes(StandardCharsets.UTF_8)))
                                                 .build()));
    }

    public void setupTestCase(TestCase testCase) throws Exception {
        server.init(testCase.closeTime);

        netty = NettyNioAsyncHttpClient.builder()
                                       .readTimeout(Duration.ofMillis(500))
                                       .eventLoopGroup(SdkEventLoopGroup.builder().numberOfThreads(3).build())
                                       .protocol(Protocol.HTTP1_1)
                                       .buildWithDefaults(AttributeMap.builder().put(TRUST_ALL_CERTIFICATES, true).build());
    }

    private Throwable captureException(RequestParams requestParams) {
        try {
            sendCustomRequest(requestParams).get(10, TimeUnit.SECONDS);
        } catch (InterruptedException | TimeoutException e) {
            throw new Error(e);
        } catch (ExecutionException e) {
            return e.getCause();
        }

        throw new AssertionError("Call did not fail as expected.");
    }

    private CompletableFuture<Void> sendCustomRequest(RequestParams requestParams) {
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
                                                                                .method(requestParams.httpMethod())
                                                                                .protocol("https")
                                                                                .host("localhost")
                                                                                .headers(requestParams.headersMap())
                                                                                .port(server.port())
                                                                                .build())
                                                     .requestContentPublisher(requestParams.contentPublisher())
                                                     .build();

        return netty.execute(req);
    }

    private static class TestCase {
        private CloseTime closeTime;
        private String errorMessageSubstring;

        private TestCase(CloseTime closeTime, String errorMessageSubstring) {
            this.closeTime = closeTime;
            this.errorMessageSubstring = errorMessageSubstring;
        }

        @Override
        public String toString() {
            return "Closure " + closeTime;
        }
    }

    private enum CloseTime {
        DURING_INIT,

        BEFORE_SSL_HANDSHAKE,
        DURING_SSL_HANDSHAKE,

        BEFORE_REQUEST_PAYLOAD,
        DURING_REQUEST_PAYLOAD,

        BEFORE_RESPONSE_HEADERS,
        BEFORE_RESPONSE_PAYLOAD,
        DURING_RESPONSE_PAYLOAD
    }

    private static class ServerConfig {
        private final HttpResponseStatus httpResponseStatus;
        public static Builder builder(){
            return new Builder();
        }
        private ServerConfig(Builder builder) {
            this.httpResponseStatus = builder.httpResponseStatus;
        }
        public static class Builder {
            private HttpResponseStatus httpResponseStatus;
            public Builder httpResponseStatus(HttpResponseStatus httpResponseStatus){
                this.httpResponseStatus = httpResponseStatus;
                return this;
            }
            public ServerConfig build() {
                return new ServerConfig(this);
            }
        }
    }

    private static class RequestParams{
        private final SdkHttpMethod httpMethod;
        private final SdkHttpContentPublisher contentPublisher;
        private final Map<String, List<String>> headersMap;

        public RequestParams(SdkHttpMethod httpMethod, SdkHttpContentPublisher contentPublisher,
                             Map<String, List<String>> headersMap) {
            this.httpMethod = httpMethod;
            this.contentPublisher = contentPublisher;
            this.headersMap = headersMap;
        }

        public SdkHttpMethod httpMethod() {
            return httpMethod;
        }

        public Map<String, List<String>> headersMap() {
            return headersMap;
        }

        public SdkHttpContentPublisher contentPublisher() {
            return contentPublisher;
        }

        public static Builder builder(){
            return new Builder();
        }
        private static class Builder{
            private  SdkHttpMethod httpMethod;
            private  SdkHttpContentPublisher contentPublisher;
            private Map<String, List<String>> headersMap = new HashMap<>();

            public Builder httpMethod(SdkHttpMethod httpMethod) {
                this.httpMethod = httpMethod;
                return this;
            }

            public Builder contentPublisher(SdkHttpContentPublisher contentPublisher) {
                this.contentPublisher = contentPublisher;
                return this;
            }

            public Builder addHeaderKeyValue(String headerName, List<String> headerValues) {
                headersMap.put(headerName, headerValues);
                return this;
            }

            public RequestParams build(){
                return new RequestParams(httpMethod, contentPublisher, headersMap);

            }
        }
    }

    private static class Server extends ChannelInitializer<Channel> {
        private final NioEventLoopGroup group = new NioEventLoopGroup();
        private CloseTime closeTime;
        private ServerBootstrap bootstrap;
        private ServerSocketChannel serverSock;
        private SslContext sslCtx;
        private ServerConfig serverConfig;

        public Server(ServerConfig serverConfig) {
            this.serverConfig = serverConfig;
        }

        private void init(CloseTime closeTime) throws Exception {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();

            bootstrap = new ServerBootstrap()
                .channel(NioServerSocketChannel.class)
                .group(group)
                .childHandler(this);

            serverSock = (ServerSocketChannel) bootstrap.bind(0).sync().channel();
            this.closeTime = closeTime;
        }

        public int port() {
            return serverSock.localAddress().getPort();
        }

        public void shutdown() throws InterruptedException {
            group.shutdownGracefully().await();
            serverSock.close();
        }

        @Override
        protected void initChannel(Channel ch) {
            LOGGER.info(() -> "Initializing channel " + ch);

            if (closeTime == CloseTime.DURING_INIT) {
                LOGGER.info(() -> "Closing channel during initialization " + ch);
                ch.close();
                return;
            }

            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast(new SslHandler(new FaultInjectionSslEngine(sslCtx.newEngine(ch.alloc()), ch), false));
            pipeline.addLast(new HttpServerCodec());
            FaultInjectionHttpHandler faultInjectionHttpHandler = new FaultInjectionHttpHandler();
            faultInjectionHttpHandler.setHttpResponseStatus(serverConfig.httpResponseStatus);
            pipeline.addLast(faultInjectionHttpHandler);

            LOGGER.info(() -> "Channel initialized " + ch);
        }

        private class FaultInjectionSslEngine extends DelegatingSslEngine {
            private final Channel channel;
            private volatile boolean closed = false;

            public FaultInjectionSslEngine(SSLEngine delegate, Channel channel) {
                super(delegate);
                this.channel = channel;
            }

            @Override
            public SSLEngineResult unwrap(ByteBuffer src, ByteBuffer[] dsts, int offset, int length) throws SSLException {
                handleBeforeFailures();
                return super.unwrap(src, dsts, offset, length);
            }

            @Override
            public SSLEngineResult wrap(ByteBuffer[] srcs, int offset, int length, ByteBuffer dst) throws SSLException {
                handleBeforeFailures();
                return super.wrap(srcs, offset, length, dst);
            }

            private void handleBeforeFailures() {
                if (getHandshakeStatus() == NOT_HANDSHAKING && closeTime == CloseTime.BEFORE_SSL_HANDSHAKE) {
                    closeChannel("Closing channel before handshake " + channel);
                }

                if ((getHandshakeStatus() == NEED_WRAP || getHandshakeStatus() == NEED_TASK || getHandshakeStatus() == NEED_UNWRAP)
                    && closeTime == CloseTime.DURING_SSL_HANDSHAKE) {
                    closeChannel("Closing channel during handshake " + channel);
                }
            }

            private void closeChannel(String message) {
                if (!closed) {
                    LOGGER.info(() -> message);
                    closed = true;
                    channel.close();
                }
            }
        }

        private class FaultInjectionHttpHandler extends SimpleChannelInboundHandler<Object> {

            private HttpResponseStatus httpResponseStatus = HttpResponseStatus.OK;

            public void setHttpResponseStatus(HttpResponseStatus httpResponseStatus) {
                this.httpResponseStatus = httpResponseStatus;
            }

            @Override
            protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
                LOGGER.info(() -> "Reading " + msg);

                if (msg instanceof HttpRequest) {
                    if (closeTime == CloseTime.BEFORE_REQUEST_PAYLOAD) {
                        LOGGER.info(() -> "Closing channel before request payload " + ctx.channel());
                        ctx.channel().disconnect();
                        return;
                    }
                }

                if (msg instanceof HttpContent) {
                    if (closeTime == CloseTime.DURING_REQUEST_PAYLOAD) {
                        LOGGER.info(() -> "Closing channel during request payload handling " + ctx.channel());
                        ctx.close();
                        return;
                    }

                    if (msg instanceof LastHttpContent) {
                        if (closeTime == CloseTime.BEFORE_RESPONSE_HEADERS) {
                            LOGGER.info(() -> "Closing channel before response headers " + ctx.channel());
                            ctx.close();
                            return;
                        }

                        writeResponse(ctx);
                    }
                }
            }

            private void writeResponse(ChannelHandlerContext ctx) {
                int responseLength = 10 * 1024 * 1024; // 10 MB
                HttpHeaders headers = new DefaultHttpHeaders().add("Content-Length", responseLength);
                ctx.writeAndFlush(new DefaultHttpResponse(HttpVersion.HTTP_1_1, this.httpResponseStatus, headers)).addListener(x -> {
                    if (closeTime == CloseTime.BEFORE_RESPONSE_PAYLOAD) {
                        LOGGER.info(() -> "Closing channel before response payload " + ctx.channel());
                        ctx.close();
                        return;
                    }

                    ByteBuf firstPartOfResponsePayload = ctx.alloc().buffer(1);
                    firstPartOfResponsePayload.writeByte(0);
                    ctx.writeAndFlush(new DefaultHttpContent(firstPartOfResponsePayload)).addListener(x2 -> {
                        if (closeTime == CloseTime.DURING_RESPONSE_PAYLOAD) {
                            LOGGER.info(() -> "Closing channel during response payload handling " + ctx.channel());
                            ctx.close();
                            return;
                        }

                        ByteBuf lastPartOfResponsePayload = ctx.alloc().buffer(responseLength - 1);
                        lastPartOfResponsePayload.writeBytes(new byte[responseLength - 1]);
                        ctx.writeAndFlush(new DefaultLastHttpContent(lastPartOfResponsePayload))
                           .addListener(ChannelFutureListener.CLOSE);
                    });
                });
            }
        }
    }
}
