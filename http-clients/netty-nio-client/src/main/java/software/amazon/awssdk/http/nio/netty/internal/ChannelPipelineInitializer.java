/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.MAX_CONCURRENT_STREAMS;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.PROTOCOL_FUTURE;
import static software.amazon.awssdk.utils.StringUtils.lowerCase;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.pool.AbstractChannelPoolHandler;
import io.netty.channel.pool.ChannelPool;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http2.ForkedHttp2MultiplexCodecBuilder;
import io.netty.handler.codec.http2.Http2SettingsFrame;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.ssl.SslContext;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.nio.netty.internal.http2.MultiplexedChannelRecord;
import software.amazon.awssdk.http.nio.netty.internal.http2.SdkHttp2FrameLogger;

/**
 * Configures the client pipeline to support HTTP/2 frames with multiplexed streams.
 */
@SdkInternalApi
public class ChannelPipelineInitializer extends AbstractChannelPoolHandler {
    private final Protocol protocol;
    private final SslContext sslCtx;
    private final long clientMaxStreams;
    private final AtomicReference<ChannelPool> channelPoolRef;

    public ChannelPipelineInitializer(Protocol protocol,
                                      SslContext sslCtx,
                                      long clientMaxStreams,
                                      AtomicReference<ChannelPool> channelPoolRef) {
        this.protocol = protocol;
        this.sslCtx = sslCtx;
        this.clientMaxStreams = clientMaxStreams;
        this.channelPoolRef = channelPoolRef;
    }

    @Override
    public void channelCreated(Channel ch) throws Exception {
        ch.attr(PROTOCOL_FUTURE).set(new CompletableFuture<>());
        ChannelPipeline pipeline = ch.pipeline();
        if (sslCtx != null) {
            pipeline.addLast(sslCtx.newHandler(ch.alloc()));
        }

        if (protocol == Protocol.HTTP2) {
            configureHttp2(ch, pipeline);
        } else {
            configureHttp11(ch, pipeline);
        }
    }

    private void configureHttp2(Channel ch, ChannelPipeline pipeline) {
        pipeline.addLast(ForkedHttp2MultiplexCodecBuilder
                             .forClient(new NoOpChannelInitializer())
                             // TODO disable frame logging for performance
                             .frameLogger(new SdkHttp2FrameLogger(LogLevel.DEBUG))
                             .headerSensitivityDetector((name, value) -> lowerCase(name.toString()).equals("authorization"))
                             .build());

        pipeline.addLast(new SimpleChannelInboundHandler<Http2SettingsFrame>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, Http2SettingsFrame msg) throws Exception {
                Long serverMaxStreams = Optional.ofNullable(msg.settings().maxConcurrentStreams()).orElse(Long.MAX_VALUE);
                ch.attr(MAX_CONCURRENT_STREAMS).set(Math.min(clientMaxStreams, serverMaxStreams));
                ch.attr(PROTOCOL_FUTURE).get().complete(Protocol.HTTP2);
            }

            @Override
            public void channelUnregistered(ChannelHandlerContext ctx) {
                if (!ch.attr(PROTOCOL_FUTURE).get().isDone()) {
                    channelError(new IOException("The channel was closed before the protocol could be determined."), ch);
                }
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                channelError(cause, ch);
            }
        });
    }

    private void channelError(Throwable cause, Channel ch) {
        ch.attr(PROTOCOL_FUTURE).get().completeExceptionally(cause);
        MultiplexedChannelRecord record = ch.attr(ChannelAttributeKey.CHANNEL_POOL_RECORD).get();
        // Deliver the exception to any child channels registered to this connection.
        if (record != null) {
            record.shutdownChildChannels(cause);
        }
        // Channel status may still be active at this point even if it's not so queue up the close so that status is
        // accurately updated
        ch.eventLoop().submit(() -> {
            try {
                if (ch.isActive()) {
                    ch.close();
                }
            } finally {
                channelPoolRef.get().release(ch);
            }
        });
    }

    private void configureHttp11(Channel ch, ChannelPipeline pipeline) {
        pipeline.addLast(new HttpClientCodec());
        ch.attr(PROTOCOL_FUTURE).get().complete(Protocol.HTTP1_1);
    }

    private static class NoOpChannelInitializer extends ChannelInitializer<Channel> {
        @Override
        protected void initChannel(Channel ch) throws Exception {
        }
    }

}


