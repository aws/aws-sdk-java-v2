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

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.pool.ChannelPool;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.nio.netty.internal.utils.NettyClientLogger;

/**
 * This {@code ChannelPool} ensures that the channel has completed the TLS negotiation before giving it back to the caller. Note
 * that it's possible for a channel to have multiple {@link SslHandler} instances its pipeline, for example if it's using a
 * proxy over HTTPS. This pool explicitly looks for the handler of the given name.
 * <p>
 * If TLS setup fails, the channel will be closed and released back to the wrapped pool and the future will be failed.
 */
@SdkInternalApi
public class TlsHandshakeEnsuringChannelPool implements ChannelPool {
    private static final NettyClientLogger LOGGER = NettyClientLogger.getLogger(TlsHandshakeEnsuringChannelPool.class);
    private final EventLoopGroup eventLoopGroup;
    private final String sslHandlerName;
    private final ChannelPool delegate;

    public TlsHandshakeEnsuringChannelPool(EventLoopGroup eventLoopGroup, String sslHandlerName, ChannelPool delegate) {
        this.eventLoopGroup = eventLoopGroup;
        this.sslHandlerName = sslHandlerName;
        this.delegate = delegate;
    }

    @Override
    public Future<Channel> acquire() {
        return acquire(eventLoopGroup.next().newPromise());
    }

    @Override
    public Future<Channel> acquire(Promise<Channel> promise) {
        Promise<Channel> delegatePromise = eventLoopGroup.next().newPromise();

        delegate.acquire(delegatePromise).addListener((GenericFutureListener<Future<Channel>>)
                                                       f -> tlsHandshakeListener(f, promise));

        return promise;
    }

    @Override
    public Future<Void> release(Channel channel) {
        return delegate.release(channel);
    }

    @Override
    public Future<Void> release(Channel channel, Promise<Void> promise) {
        return delegate.release(channel, promise);
    }

    @Override
    public void close() {
        delegate.close();
    }

    private void tlsHandshakeListener(Future<Channel> f, Promise<Channel> promise) throws ExecutionException,
                                                                                                 InterruptedException {
        if (!f.isSuccess()) {
            promise.tryFailure(f.cause());
            return;
        }

        Channel channel = f.get();
        Optional<Future<Channel>> handshakeFuture = sslHandshakeFuture(channel);

        // Future won't be present if this channel isn't establishing a TLS connection
        if (!handshakeFuture.isPresent()) {
            promise.trySuccess(channel);
            return;
        }

        // Channel is using TLS, wait for handshake to complete to give the channel to the caller
        handshakeFuture.get().addListener((GenericFutureListener<Future<Channel>>) handshake -> {
            if (handshake.isSuccess()) {
                promise.trySuccess(handshake.getNow());
            } else {
                LOGGER.debug(channel, () -> "Failed TLS connection setup. Channel will be closed.", handshake.cause());
                channel.close();
                delegate.release(channel);
                IOException error = new IOException("Failed TLS connection setup.", handshake.cause());
                promise.tryFailure(error);
            }
        });
    }

    private Optional<Future<Channel>> sslHandshakeFuture(Channel ch) {
        ChannelHandler handlerByName = ch.pipeline().get(sslHandlerName);
        if (!(handlerByName instanceof SslHandler)) {
            return Optional.empty();
        }

        SslHandler sslHandler = (SslHandler) handlerByName;

        return Optional.of(sslHandler.handshakeFuture());
    }
}
