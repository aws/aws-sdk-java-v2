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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.DefaultChannelPromise;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.ChannelPool;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslCloseCompletionEvent;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Promise;
import java.io.IOException;
import java.net.URI;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.http.nio.netty.ProxyAuthScheme;

/**
 * Unit tests for {@link ProxyTunnelInitHandler}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ProxyTunnelInitHandlerTest {
    private static final NioEventLoopGroup GROUP = new NioEventLoopGroup(1);

    private static final URI REMOTE_HOST = URI.create("https://s3.amazonaws.com:1234");
    private static final String PROXY_USER = "myuser";
    private static final String PROXY_PASSWORD = "mypassword";

    @Mock
    private ChannelHandlerContext mockCtx;

    @Mock
    private Channel mockChannel;

    @Mock
    private ChannelPipeline mockPipeline;

    @Mock
    private ChannelPool mockChannelPool;

    @Before
    public void methodSetup() {
        when(mockCtx.channel()).thenReturn(mockChannel);
        when(mockCtx.pipeline()).thenReturn(mockPipeline);
        when(mockChannel.writeAndFlush(any())).thenReturn(new DefaultChannelPromise(mockChannel, GROUP.next()));
    }

    @AfterClass
    public static void teardown() {
        GROUP.shutdownGracefully().awaitUninterruptibly();
    }

    @Test
    public void addedToPipeline_addsCodec() {
        HttpClientCodec codec = new HttpClientCodec();
        Supplier<HttpClientCodec> codecSupplier = () -> codec;
        when(mockCtx.name()).thenReturn("foo");

        ProxyTunnelInitHandler handler = new ProxyTunnelInitHandler(mockChannelPool, null, null, null, REMOTE_HOST, null,
                                                                    codecSupplier);
        handler.handlerAdded(mockCtx);

        verify(mockPipeline).addBefore(eq("foo"), eq(null), eq(codec));
    }

    @Test
    public void successfulProxyResponse_completesFuture() {
        Promise<Channel> promise = GROUP.next().newPromise();
        ProxyTunnelInitHandler handler = new ProxyTunnelInitHandler(mockChannelPool, REMOTE_HOST, promise);
        successResponse(handler);

        assertThat(promise.awaitUninterruptibly().getNow()).isEqualTo(mockChannel);
    }

    @Test
    public void successfulProxyResponse_removesSelfAndCodec() {
        when(mockPipeline.get(HttpClientCodec.class)).thenReturn(new HttpClientCodec());

        Promise<Channel> promise = GROUP.next().newPromise();
        ProxyTunnelInitHandler handler = new ProxyTunnelInitHandler(mockChannelPool, REMOTE_HOST, promise);
        successResponse(handler);

        handler.handlerRemoved(mockCtx);

        verify(mockPipeline).remove(eq(handler));
        verify(mockPipeline).remove(eq(HttpClientCodec.class));
    }

    @Test
    public void successfulProxyResponse_doesNotRemoveSslHandler() {
        Promise<Channel> promise = GROUP.next().newPromise();
        ProxyTunnelInitHandler handler = new ProxyTunnelInitHandler(mockChannelPool, REMOTE_HOST, promise);
        successResponse(handler);

        verify(mockPipeline, never()).get(eq(SslHandler.class));
        verify(mockPipeline, never()).remove(eq(SslHandler.class));
    }

    @Test
    public void unexpectedMessage_failsPromise() {
        Promise<Channel> promise = GROUP.next().newPromise();
        ProxyTunnelInitHandler handler = new ProxyTunnelInitHandler(mockChannelPool, REMOTE_HOST, promise);
        handler.channelRead(mockCtx, new Object());

        assertThat(promise.awaitUninterruptibly().isSuccess()).isFalse();
    }

    @Test
    public void unsuccessfulResponse_failsPromise() {
        Promise<Channel> promise = GROUP.next().newPromise();
        ProxyTunnelInitHandler handler = new ProxyTunnelInitHandler(mockChannelPool, REMOTE_HOST, promise);

        DefaultHttpResponse resp = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN);
        handler.channelRead(mockCtx, resp);

        assertThat(promise.awaitUninterruptibly().isSuccess()).isFalse();
    }

    @Test
    public void requestWriteFails_failsPromise() {
        DefaultChannelPromise writePromise = new DefaultChannelPromise(mockChannel, GROUP.next());
        writePromise.setFailure(new IOException("boom"));
        when(mockChannel.writeAndFlush(any())).thenReturn(writePromise);

        Promise<Channel> promise = GROUP.next().newPromise();
        ProxyTunnelInitHandler handler = new ProxyTunnelInitHandler(mockChannelPool, REMOTE_HOST, promise);
        handler.handlerAdded(mockCtx);

        assertThat(promise.awaitUninterruptibly().isSuccess()).isFalse();
    }

    @Test
    public void channelInactive_shouldFailPromise() throws Exception {
        Promise<Channel> promise = GROUP.next().newPromise();
        ProxyTunnelInitHandler handler = new ProxyTunnelInitHandler(mockChannelPool, REMOTE_HOST, promise);
        SslCloseCompletionEvent event = new SslCloseCompletionEvent(new RuntimeException(""));
        handler.channelInactive(mockCtx);

        assertThat(promise.awaitUninterruptibly().isSuccess()).isFalse();
        verify(mockCtx).close();
    }

    @Test
    public void unexpectedExceptionThrown_shouldFailPromise() throws Exception {
        Promise<Channel> promise = GROUP.next().newPromise();
        ProxyTunnelInitHandler handler = new ProxyTunnelInitHandler(mockChannelPool, REMOTE_HOST, promise);
        handler.exceptionCaught(mockCtx, new RuntimeException("exception"));

        assertThat(promise.awaitUninterruptibly().isSuccess()).isFalse();
        verify(mockCtx).close();
    }

    @Test
    public void handlerRemoved_removesCodec() {
        HttpClientCodec codec = new HttpClientCodec();
        when(mockPipeline.get(eq(HttpClientCodec.class))).thenReturn(codec);

        Promise<Channel> promise = GROUP.next().newPromise();
        ProxyTunnelInitHandler handler = new ProxyTunnelInitHandler(mockChannelPool, REMOTE_HOST, promise);

        handler.handlerRemoved(mockCtx);

        verify(mockPipeline).remove(eq(HttpClientCodec.class));
    }

    @Test
    public void handlerAdded_writesRequest_withoutAuth() {
        Promise<Channel> promise = GROUP.next().newPromise();
        ProxyTunnelInitHandler handler = new ProxyTunnelInitHandler(mockChannelPool, REMOTE_HOST, promise);
        handler.handlerAdded(mockCtx);

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockChannel).writeAndFlush(requestCaptor.capture());

        String uri = REMOTE_HOST.getHost() + ":" + REMOTE_HOST.getPort();
        HttpRequest expectedRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.CONNECT, uri,
                                                                 Unpooled.EMPTY_BUFFER);
        expectedRequest.headers().add(HttpHeaderNames.HOST, uri);

        assertThat(requestCaptor.getValue()).isEqualTo(expectedRequest);
    }

    @Test
    public void handlerAdded_writesRequest_withAuth() {
        Promise<Channel> promise = GROUP.next().newPromise();
        ProxyTunnelInitHandler handler = new ProxyTunnelInitHandler(mockChannelPool, PROXY_USER, PROXY_PASSWORD, REMOTE_HOST, promise);
        handler.handlerAdded(mockCtx);

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockChannel).writeAndFlush(requestCaptor.capture());

        String uri = REMOTE_HOST.getHost() + ":" + REMOTE_HOST.getPort();
        HttpRequest expectedRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.CONNECT, uri,
                Unpooled.EMPTY_BUFFER);
        expectedRequest.headers().add(HttpHeaderNames.HOST, uri);

        String authB64 = Base64.getEncoder().encodeToString(String.format("%s:%s", PROXY_USER, PROXY_PASSWORD).getBytes(CharsetUtil.UTF_8));
        expectedRequest.headers().add(HttpHeaderNames.PROXY_AUTHORIZATION, String.format("Basic %s", authB64));

        assertThat(requestCaptor.getValue()).isEqualTo(expectedRequest);
    }

    @Test
    public void handlerAdded_authParamsGeneratorThrows_failsFuture() {
        ProxyAuthGenerator authGenerator = mock(ProxyAuthGenerator.class);
        when(authGenerator.generateAuthParams(any(URI.class))).thenThrow(new RuntimeException("auth generator error"));

        Promise<Channel> promise = GROUP.next().newPromise();
        ProxyTunnelInitHandler handler = new ProxyTunnelInitHandler(mockChannelPool, URI.create("https://amazon.com"),
                                                                    authGenerator,
                                                                    REMOTE_HOST,
                                                                    promise);
        handler.handlerAdded(mockCtx);

        assertThatThrownBy(promise::get).hasMessageContaining("Unable to send CONNECT request to proxy")
                                        .hasRootCauseMessage("auth generator error");
    }

    @Test
    public void handlerAdded_authParamsCompleteAsynchronously_writesRequestOnceParamsAvailable() throws Exception {
        CompletableFuture<String> authParams = new CompletableFuture<>();
        ProxyAuthGenerator authGenerator = mock(ProxyAuthGenerator.class);
        when(authGenerator.scheme()).thenReturn(ProxyAuthScheme.NEGOTIATE);
        when(authGenerator.generateAuthParams(any(URI.class))).thenReturn(authParams);

        EventExecutor executor = GROUP.next();
        when(mockCtx.executor()).thenReturn(executor);

        Promise<Channel> promise = GROUP.next().newPromise();
        ProxyTunnelInitHandler handler = new ProxyTunnelInitHandler(mockChannelPool, URI.create("https://proxy.com"),
                                                                    authGenerator, REMOTE_HOST, promise);
        handler.handlerAdded(mockCtx);

        // The calling thread must not have waited for the auth params, so nothing is written yet.
        verify(mockChannel, never()).writeAndFlush(any());

        authParams.complete("token");
        // Drain the executor so the queued continuation has run.
        executor.submit(() -> { }).get();

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockChannel).writeAndFlush(requestCaptor.capture());
        assertThat(requestCaptor.getValue().headers().get(HttpHeaderNames.PROXY_AUTHORIZATION)).isEqualTo("Negotiate token");
    }

    @Test
    public void handlerAdded_authParamsFailAsynchronously_failsFuture() throws Exception {
        CompletableFuture<String> authParams = new CompletableFuture<>();
        ProxyAuthGenerator authGenerator = mock(ProxyAuthGenerator.class);
        when(authGenerator.generateAuthParams(any(URI.class))).thenReturn(authParams);

        EventExecutor executor = GROUP.next();
        when(mockCtx.executor()).thenReturn(executor);

        Promise<Channel> promise = GROUP.next().newPromise();
        ProxyTunnelInitHandler handler = new ProxyTunnelInitHandler(mockChannelPool, URI.create("https://proxy.com"),
                                                                    authGenerator, REMOTE_HOST, promise);
        handler.handlerAdded(mockCtx);

        authParams.completeExceptionally(new RuntimeException("auth generator error"));
        executor.submit(() -> { }).get();

        verify(mockChannel, never()).writeAndFlush(any());
        assertThatThrownBy(promise::get).hasMessageContaining("Unable to send CONNECT request to proxy")
                                        .hasRootCauseMessage("auth generator error");
    }

    private void successResponse(ProxyTunnelInitHandler handler) {
        DefaultHttpResponse resp = new DefaultHttpResponse(HttpVersion.HTTP_1_1,  HttpResponseStatus.OK);
        handler.channelRead(mockCtx, resp);
    }
}
