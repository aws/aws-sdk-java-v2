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

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.pool.ChannelPool;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.concurrent.Promise;
import java.io.IOException;
import java.net.URI;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;

/**
 * Handler that initializes the HTTP tunnel.
 */
@SdkInternalApi
public final class ProxyTunnelInitHandler extends ChannelDuplexHandler {
    private final ChannelPool sourcePool;
    private final URI remoteHost;
    private final Promise<Channel> initPromise;
    private final Supplier<HttpClientCodec> httpCodecSupplier;

    public ProxyTunnelInitHandler(ChannelPool sourcePool, URI remoteHost, Promise<Channel> initPromise) {
        this(sourcePool, remoteHost, initPromise, HttpClientCodec::new);
    }

    @SdkTestInternalApi
    public ProxyTunnelInitHandler(ChannelPool sourcePool, URI remoteHost, Promise<Channel> initPromise,
                                  Supplier<HttpClientCodec> httpCodecSupplier) {
        this.sourcePool = sourcePool;
        this.remoteHost = remoteHost;
        this.initPromise = initPromise;
        this.httpCodecSupplier = httpCodecSupplier;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        ChannelPipeline pipeline = ctx.pipeline();
        pipeline.addBefore(ctx.name(), null, httpCodecSupplier.get());
        HttpRequest connectRequest = connectRequest();
        ctx.channel().writeAndFlush(connectRequest).addListener(f -> {
            if (!f.isSuccess()) {
                ctx.close();
                sourcePool.release(ctx.channel());
                initPromise.setFailure(new IOException("Unable to send CONNECT request to proxy", f.cause()));
            }
        });
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        if (ctx.pipeline().get(HttpClientCodec.class) != null) {
            ctx.pipeline().remove(HttpClientCodec.class);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) msg;
            if (response.status().code() == 200) {
                ctx.pipeline().remove(this);
                // Note: we leave the SslHandler here (if we added it)
                initPromise.setSuccess(ctx.channel());
                return;
            }
        }

        // Fail if we received any other type of message or we didn't get a 200 from the proxy
        ctx.pipeline().remove(this);
        ctx.close();
        sourcePool.release(ctx.channel());
        initPromise.setFailure(new IOException("Could not connect to proxy"));
    }

    private HttpRequest connectRequest() {
        String uri = getUri();
        HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.CONNECT, uri,
                                                         Unpooled.EMPTY_BUFFER, false);
        request.headers().add(HttpHeaderNames.HOST, uri);
        return request;
    }

    private String getUri() {
        return remoteHost.getHost() + ":" + remoteHost.getPort();
    }
}

