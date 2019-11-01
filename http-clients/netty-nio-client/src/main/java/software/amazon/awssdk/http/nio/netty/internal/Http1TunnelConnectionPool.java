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

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoop;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

import java.net.URI;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Connection pool that knows how to establish a tunnel using the HTTP CONNECT method.
 */
@SdkInternalApi
public class Http1TunnelConnectionPool implements ChannelPool {
    static final AttributeKey<Boolean> TUNNEL_ESTABLISHED_KEY = AttributeKey.newInstance(
            "aws.http.nio.netty.async.Http1TunnelConnectionPool.tunnelEstablished");

    private static final Logger log = Logger.loggerFor(Http1TunnelConnectionPool.class);

    private final EventLoop eventLoop;
    private final ChannelPool delegate;
    private final SslContext sslContext;
    private final URI proxyAddress;
    private final URI remoteAddress;
    private final ChannelPoolHandler handler;
    private final InitHandlerSupplier initHandlerSupplier;

    public Http1TunnelConnectionPool(EventLoop eventLoop, ChannelPool delegate, SslContext sslContext,
                                     URI proxyAddress, URI remoteAddress, ChannelPoolHandler handler) {
        this(eventLoop, delegate, sslContext, proxyAddress, remoteAddress, handler, ProxyTunnelInitHandler::new);

    }

    @SdkTestInternalApi
    Http1TunnelConnectionPool(EventLoop eventLoop, ChannelPool delegate, SslContext sslContext,
                              URI proxyAddress, URI remoteAddress, ChannelPoolHandler handler,
                              InitHandlerSupplier initHandlerSupplier) {
        this.eventLoop = eventLoop;
        this.delegate = delegate;
        this.sslContext = sslContext;
        this.proxyAddress = proxyAddress;
        this.remoteAddress = remoteAddress;
        this.handler = handler;
        this.initHandlerSupplier = initHandlerSupplier;
    }

    @Override
    public Future<Channel> acquire() {
        return acquire(eventLoop.newPromise());
    }

    @Override
    public Future<Channel> acquire(Promise<Channel> promise) {
        delegate.acquire(eventLoop.newPromise()).addListener((Future<Channel> f) -> {
            if (f.isSuccess()) {
                setupChannel(f.getNow(), promise);
            } else {
                promise.setFailure(f.cause());
            }
        });
        return promise;
    }

    @Override
    public Future<Void> release(Channel channel) {
        return release(channel, eventLoop.newPromise());
    }

    @Override
    public Future<Void> release(Channel channel, Promise<Void> promise) {
        return delegate.release(channel, promise);
    }

    @Override
    public void close() {
        delegate.close();
    }

    private void setupChannel(Channel ch, Promise<Channel> acquirePromise) {
        if (isTunnelEstablished(ch)) {
            log.debug(() -> String.format("Tunnel already established for %s", ch.id().asShortText()));
            acquirePromise.setSuccess(ch);
            return;
        }

        log.debug(() -> String.format("Tunnel not yet established for channel %s. Establishing tunnel now.",
                ch.id().asShortText()));

        Promise<Channel> tunnelEstablishedPromise = eventLoop.newPromise();

        SslHandler sslHandler = createSslHandlerIfNeeded(ch.alloc());
        if (sslHandler != null) {
            ch.pipeline().addLast(sslHandler);
        }
        ch.pipeline().addLast(initHandlerSupplier.newInitHandler(delegate, remoteAddress, tunnelEstablishedPromise));

        tunnelEstablishedPromise.addListener((Future<Channel> f) -> {
            if (f.isSuccess()) {
                Channel tunnel = f.getNow();
                handler.channelCreated(tunnel);
                tunnel.attr(TUNNEL_ESTABLISHED_KEY).set(true);
                acquirePromise.setSuccess(tunnel);
            } else {
                ch.close();
                delegate.release(ch);

                Throwable cause = f.cause();
                log.error(() -> String.format("Unable to establish tunnel for channel %s", ch.id().asShortText()), cause);
                acquirePromise.setFailure(cause);
            }
        });
    }

    private SslHandler createSslHandlerIfNeeded(ByteBufAllocator alloc) {
        if (sslContext == null) {
            return null;
        }

        String scheme = proxyAddress.getScheme();

        if (!"https".equals(StringUtils.lowerCase(scheme))) {
            return null;
        }

        return sslContext.newHandler(alloc, proxyAddress.getHost(), proxyAddress.getPort());
    }

    private static boolean isTunnelEstablished(Channel ch) {
        Boolean established = ch.attr(TUNNEL_ESTABLISHED_KEY).get();
        return Boolean.TRUE.equals(established);
    }

    @SdkTestInternalApi
    @FunctionalInterface
    interface InitHandlerSupplier {
        ChannelHandler newInitHandler(ChannelPool sourcePool, URI remoteAddress, Promise<Channel> tunnelInitFuture);
    }
}
