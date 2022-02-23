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
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.ChannelPool;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.nio.netty.TlsHandshakeEnsuringChannelPool;

public class TlsHandshakeEnsuringChannelPoolTest {
    private static final String SSL_HANDLER_NAME = "TestSslHandler";
    private static EventLoopGroup eventLoopGroup;

    private ChannelPool delegatePool;

    @BeforeAll
    public static void setup() {
        eventLoopGroup = new NioEventLoopGroup();
    }

    @BeforeEach
    public void methodSetup() {
        delegatePool = mock(ChannelPool.class);
    }

    @AfterAll
    public static void teardown() {
        eventLoopGroup.shutdownGracefully();
    }

    @Test
    public void acquire_delegateFutureFails_failsReturnedFuture() {
        Promise<Channel> acquirePromise = eventLoopGroup.next().newPromise();
        IOException cause = new IOException("Could not open socket!");
        acquirePromise.setFailure(cause);
        when(delegatePool.acquire(any(Promise.class))).thenReturn(acquirePromise);

        TlsHandshakeEnsuringChannelPool pool = new TlsHandshakeEnsuringChannelPool(eventLoopGroup,
                                                                                   SSL_HANDLER_NAME,
                                                                                   delegatePool);

        Future<Channel> acquire = pool.acquire();
        waitForFuture(acquire);

        assertThat(acquire.isSuccess()).isFalse();
        assertThat(acquire.cause()).isSameAs(cause);
    }

    @Test
    public void acquire_channelDoesNotUseTls_completesReturnedFuture() {
        Promise<Channel> acquirePromise = eventLoopGroup.next().newPromise();
        EmbeddedChannel channel = new EmbeddedChannel();
        acquirePromise.setSuccess(channel);
        when(delegatePool.acquire(any(Promise.class))).thenReturn(acquirePromise);

        TlsHandshakeEnsuringChannelPool pool = new TlsHandshakeEnsuringChannelPool(eventLoopGroup,
                                                                                   SSL_HANDLER_NAME,
                                                                                   delegatePool);

        Future<Channel> acquire = pool.acquire();
        waitForFuture(acquire);

        assertThat(acquire.isSuccess()).isTrue();
        assertThat(acquire.getNow()).isSameAs(channel);
    }

    @Test
    public void acquire_tlsHandshakeFails_failsReturnedFuture() {
        Promise<Channel> handshakePromise = eventLoopGroup.next().newPromise();
        IOException handshakeFailure = new IOException("Failed TLS connection setup.");
        handshakePromise.setFailure(handshakeFailure);
        SslHandler mockSslHandler = mock(SslHandler.class);
        when(mockSslHandler.handshakeFuture()).thenReturn(handshakePromise);

        EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(SSL_HANDLER_NAME, mockSslHandler);
        Promise<Channel> acquirePromise = eventLoopGroup.next().newPromise();
        acquirePromise.setSuccess(channel);
        when(delegatePool.acquire(any(Promise.class))).thenReturn(acquirePromise);

        TlsHandshakeEnsuringChannelPool pool = new TlsHandshakeEnsuringChannelPool(eventLoopGroup,
                                                                                   SSL_HANDLER_NAME,
                                                                                   delegatePool);

        Future<Channel> acquire = pool.acquire();
        waitForFuture(acquire);

        assertThat(acquire.isSuccess()).isFalse();
        assertThat(acquire.cause()).hasCause(handshakeFailure);
    }

    @Test
    public void acquire_tlsHandshakeSucceeds_completesReturnedFuture() {
        EmbeddedChannel channel = new EmbeddedChannel();

        Promise<Channel> handshakePromise = eventLoopGroup.next().newPromise();
        handshakePromise.setSuccess(channel);
        SslHandler mockSslHandler = mock(SslHandler.class);
        when(mockSslHandler.handshakeFuture()).thenReturn(handshakePromise);


        channel.pipeline().addLast(SSL_HANDLER_NAME, mockSslHandler);
        Promise<Channel> acquirePromise = eventLoopGroup.next().newPromise();
        acquirePromise.setSuccess(channel);
        when(delegatePool.acquire(any(Promise.class))).thenReturn(acquirePromise);

        TlsHandshakeEnsuringChannelPool pool = new TlsHandshakeEnsuringChannelPool(eventLoopGroup,
                                                                                   SSL_HANDLER_NAME,
                                                                                   delegatePool);

        Future<Channel> acquire = pool.acquire();
        waitForFuture(acquire);

        assertThat(acquire.isSuccess()).isTrue();
        assertThat(acquire.getNow()).isSameAs(channel);
    }

    @Test
    public void acquire_tlsHandshakeFails_closesAndReleasesChannel() {
        Promise<Channel> handshakePromise = eventLoopGroup.next().newPromise();

        IOException handshakeFailure = new IOException("Failed TLS connection setup.");
        handshakePromise.setFailure(handshakeFailure);
        SslHandler mockSslHandler = mock(SslHandler.class);
        when(mockSslHandler.handshakeFuture()).thenReturn(handshakePromise);

        ChannelPipeline mockPipeline = mock(ChannelPipeline.class);
        when(mockPipeline.get(SSL_HANDLER_NAME)).thenReturn(mockSslHandler);

        Channel channel = mock(Channel.class);
        when(channel.pipeline()).thenReturn(mockPipeline);

        Promise<Channel> acquirePromise = eventLoopGroup.next().newPromise();
        acquirePromise.setSuccess(channel);
        when(delegatePool.acquire(any(Promise.class))).thenReturn(acquirePromise);

        TlsHandshakeEnsuringChannelPool pool = new TlsHandshakeEnsuringChannelPool(eventLoopGroup,
                                                                                   SSL_HANDLER_NAME,
                                                                                   delegatePool);

        Future<Channel> acquire = pool.acquire();
        waitForFuture(acquire);

        verify(channel).close();
        verify(delegatePool).release(channel);
    }

    @Test
    public void acquire_findsHandlerByName() {
        Channel channel = mock(Channel.class);

        Promise<Channel> handshakePromise = eventLoopGroup.next().newPromise();
        handshakePromise.setSuccess(channel);

        SslHandler mockSslHandler = mock(SslHandler.class);
        when(mockSslHandler.handshakeFuture()).thenReturn(handshakePromise);

        ChannelPipeline channelPipeline = mock(ChannelPipeline.class);
        when(channel.pipeline()).thenReturn(channelPipeline);

        when(channelPipeline.get(SSL_HANDLER_NAME)).thenReturn(mockSslHandler);

        Promise<Channel> acquirePromise = eventLoopGroup.next().newPromise();
        acquirePromise.setSuccess(channel);
        when(delegatePool.acquire(any(Promise.class))).thenReturn(acquirePromise);

        TlsHandshakeEnsuringChannelPool pool = new TlsHandshakeEnsuringChannelPool(eventLoopGroup,
                                                                                   SSL_HANDLER_NAME,
                                                                                   delegatePool);

        Future<Channel> acquire = pool.acquire();
        waitForFuture(acquire);

        verify(channelPipeline).get(SSL_HANDLER_NAME);
        verifyNoMoreInteractions(channelPipeline);
    }

    private static void waitForFuture(Future<?> f) {
        try {
            f.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }

        if (!f.isDone()) {
            fail("Pool future did not complete within 5 seconds.");
        }
    }
}
