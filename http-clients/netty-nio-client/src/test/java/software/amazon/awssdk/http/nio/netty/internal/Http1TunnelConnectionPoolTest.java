/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.http.nio.netty.internal.Http1TunnelConnectionPool.TUNNEL_ESTABLISHED_KEY;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.handler.ssl.ApplicationProtocolNegotiator;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.Attribute;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSessionContext;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Unit tests for {@link Http1TunnelConnectionPool}.
 */
@RunWith(MockitoJUnitRunner.class)
public class Http1TunnelConnectionPoolTest {
    private static final NioEventLoopGroup GROUP = new NioEventLoopGroup(1);

    private static final URI HTTP_PROXY_ADDRESS = URI.create("http://localhost:1234");

    private static final URI HTTPS_PROXY_ADDRESS = URI.create("https://localhost:5678");

    private static final URI REMOTE_ADDRESS = URI.create("https://s3.amazonaws.com:5678");

    @Mock
    private ChannelPool delegatePool;

    @Mock
    private ChannelPoolHandler mockHandler;

    @Mock
    public Channel mockChannel;

    @Mock
    public ChannelPipeline mockPipeline;

    @Mock
    public Attribute mockAttr;

    @Mock
    public ChannelHandlerContext mockCtx;

    @Mock
    public ChannelId mockId;

    @Before
    public void methodSetup() {
        Future<Channel> channelFuture = GROUP.next().newSucceededFuture(mockChannel);
        when(delegatePool.acquire(any(Promise.class))).thenReturn(channelFuture);

        when(mockCtx.channel()).thenReturn(mockChannel);
        when(mockCtx.pipeline()).thenReturn(mockPipeline);

        when(mockChannel.attr(eq(TUNNEL_ESTABLISHED_KEY))).thenReturn(mockAttr);
        when(mockChannel.id()).thenReturn(mockId);
        when(mockChannel.pipeline()).thenReturn(mockPipeline);
    }

    @AfterClass
    public static void teardown() {
        GROUP.shutdownGracefully().awaitUninterruptibly();
    }

    @Test
    public void tunnelAlreadyEstablished_doesNotAddInitHandler() {
        Http1TunnelConnectionPool tunnelPool = new Http1TunnelConnectionPool(GROUP.next(), delegatePool, null,
                HTTP_PROXY_ADDRESS, REMOTE_ADDRESS, mockHandler);

        when(mockAttr.get()).thenReturn(true);

        tunnelPool.acquire().awaitUninterruptibly();

        verify(mockPipeline, never()).addLast(anyObject());
    }

    @Test(timeout = 1000)
    public void tunnelNotEstablished_addsInitHandler() throws InterruptedException {
        Http1TunnelConnectionPool tunnelPool = new Http1TunnelConnectionPool(GROUP.next(), delegatePool, null,
                HTTP_PROXY_ADDRESS, REMOTE_ADDRESS, mockHandler);

        when(mockAttr.get()).thenReturn(false);

        CountDownLatch latch = new CountDownLatch(1);
        when(mockPipeline.addLast(any(ChannelHandler.class))).thenAnswer(i -> {
           latch.countDown();
           return mockPipeline;
        });
        tunnelPool.acquire();
        latch.await();
        verify(mockPipeline, times(1)).addLast(any(ProxyTunnelInitHandler.class));
    }

    @Test
    public void tunnelInitFails_acquireFutureFails() {
        Http1TunnelConnectionPool.InitHandlerSupplier supplier = (srcPool, remoteAddr, initFuture) -> {
            initFuture.setFailure(new IOException("boom"));
            return mock(ChannelHandler.class);
        };

        Http1TunnelConnectionPool tunnelPool = new Http1TunnelConnectionPool(GROUP.next(), delegatePool, null,
                HTTP_PROXY_ADDRESS, REMOTE_ADDRESS, mockHandler, supplier);

        Future<Channel> acquireFuture = tunnelPool.acquire();

        assertThat(acquireFuture.awaitUninterruptibly().cause()).hasMessage("boom");
    }

    @Test
    public void tunnelInitSucceeds_acquireFutureSucceeds() {
        Http1TunnelConnectionPool.InitHandlerSupplier supplier = (srcPool, remoteAddr, initFuture) -> {
            initFuture.setSuccess(mockChannel);
            return mock(ChannelHandler.class);
        };

        Http1TunnelConnectionPool tunnelPool = new Http1TunnelConnectionPool(GROUP.next(), delegatePool, null,
                HTTP_PROXY_ADDRESS, REMOTE_ADDRESS, mockHandler, supplier);

        Future<Channel> acquireFuture = tunnelPool.acquire();

        assertThat(acquireFuture.awaitUninterruptibly().getNow()).isEqualTo(mockChannel);
    }

    @Test
    public void acquireFromDelegatePoolFails_failsFuture() {
        Http1TunnelConnectionPool tunnelPool = new Http1TunnelConnectionPool(GROUP.next(), delegatePool, null,
                HTTP_PROXY_ADDRESS, REMOTE_ADDRESS, mockHandler);

        when(delegatePool.acquire(any(Promise.class))).thenReturn(GROUP.next().newFailedFuture(new IOException("boom")));

        Future<Channel> acquireFuture = tunnelPool.acquire();

        assertThat(acquireFuture.awaitUninterruptibly().cause()).hasMessage("boom");
    }

    @Test
    public void sslContextProvided_andProxyUsingHttps_addsSslHandler() {
        SslHandler mockSslHandler = mock(SslHandler.class);
        TestSslContext mockSslCtx = new TestSslContext(mockSslHandler);

        Http1TunnelConnectionPool.InitHandlerSupplier supplier = (srcPool, remoteAddr, initFuture) -> {
            initFuture.setSuccess(mockChannel);
            return mock(ChannelHandler.class);
        };

        Http1TunnelConnectionPool tunnelPool = new Http1TunnelConnectionPool(GROUP.next(), delegatePool, mockSslCtx,
                HTTPS_PROXY_ADDRESS, REMOTE_ADDRESS, mockHandler, supplier);

        tunnelPool.acquire().awaitUninterruptibly();

        ArgumentCaptor<ChannelHandler> handlersCaptor = ArgumentCaptor.forClass(ChannelHandler.class);
        verify(mockPipeline, times(2)).addLast(handlersCaptor.capture());

        assertThat(handlersCaptor.getAllValues().get(0)).isEqualTo(mockSslHandler);
    }

    @Test
    public void sslContextProvided_andProxyNotUsingHttps_doesNotAddSslHandler() {
        SslHandler mockSslHandler = mock(SslHandler.class);
        TestSslContext mockSslCtx = new TestSslContext(mockSslHandler);

        Http1TunnelConnectionPool.InitHandlerSupplier supplier = (srcPool, remoteAddr, initFuture) -> {
            initFuture.setSuccess(mockChannel);
            return mock(ChannelHandler.class);
        };

        Http1TunnelConnectionPool tunnelPool = new Http1TunnelConnectionPool(GROUP.next(), delegatePool, mockSslCtx,
                HTTP_PROXY_ADDRESS, REMOTE_ADDRESS, mockHandler, supplier);

        tunnelPool.acquire().awaitUninterruptibly();

        ArgumentCaptor<ChannelHandler> handlersCaptor = ArgumentCaptor.forClass(ChannelHandler.class);
        verify(mockPipeline).addLast(handlersCaptor.capture());

        assertThat(handlersCaptor.getAllValues().get(0)).isNotInstanceOf(SslHandler.class);
    }

    @Test
    public void release_releasedToDelegatePool() {
        Http1TunnelConnectionPool tunnelPool = new Http1TunnelConnectionPool(GROUP.next(), delegatePool, null,
                HTTP_PROXY_ADDRESS, REMOTE_ADDRESS, mockHandler);
        tunnelPool.release(mockChannel);
        verify(delegatePool).release(eq(mockChannel), any(Promise.class));
    }

    @Test
    public void release_withGivenPromise_releasedToDelegatePool() {
        Http1TunnelConnectionPool tunnelPool = new Http1TunnelConnectionPool(GROUP.next(), delegatePool, null,
                HTTP_PROXY_ADDRESS, REMOTE_ADDRESS, mockHandler);
        Promise mockPromise = mock(Promise.class);
        tunnelPool.release(mockChannel, mockPromise);
        verify(delegatePool).release(eq(mockChannel), eq(mockPromise));
    }

    @Test
    public void close_closesDelegatePool() {
        Http1TunnelConnectionPool tunnelPool = new Http1TunnelConnectionPool(GROUP.next(), delegatePool, null,
                HTTP_PROXY_ADDRESS, REMOTE_ADDRESS, mockHandler);
        tunnelPool.close();
        verify(delegatePool).close();
    }

    private static class TestSslContext extends SslContext {
        private final SslHandler handler;

        protected TestSslContext(SslHandler handler) {
            this.handler = handler;
        }

        @Override
        public boolean isClient() {
            return false;
        }

        @Override
        public List<String> cipherSuites() {
            return null;
        }

        @Override
        public long sessionCacheSize() {
            return 0;
        }

        @Override
        public long sessionTimeout() {
            return 0;
        }

        @Override
        public ApplicationProtocolNegotiator applicationProtocolNegotiator() {
            return null;
        }

        @Override
        public SSLEngine newEngine(ByteBufAllocator alloc) {
            return null;
        }

        @Override
        public SSLEngine newEngine(ByteBufAllocator alloc, String peerHost, int peerPort) {
            return null;
        }

        @Override
        public SSLSessionContext sessionContext() {
            return null;
        }

        @Override
        public SslHandler newHandler(ByteBufAllocator alloc, String host, int port, boolean startTls) {
            return handler;
        }
    }
}
