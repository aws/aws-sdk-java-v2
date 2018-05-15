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

package software.amazon.awssdk.http.nio.netty.h2;

import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKeys.MAX_CONCURRENT_STREAMS;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKeys.PROTOCOL_FUTURE;

import com.typesafe.netty.http.HttpStreamsClientHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.pool.AbstractChannelPoolHandler;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http2.Http2MultiplexCodecBuilder;
import io.netty.handler.codec.http2.Http2SettingsFrame;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.nio.netty.internal.ResponseHandler;
import software.amazon.awssdk.http.nio.netty.internal.utils.ChannelUtils;

/**
 * Configures the client pipeline to support HTTP/2 frames with multiplexed streams.
 */
public class Http2MultiplexInitializer extends AbstractChannelPoolHandler {

    private final Protocol protocol;
    private final SslContext sslCtx;
    private final long clientMaxStreams;

    Http2MultiplexInitializer(Protocol protocol,
                              SslContext sslCtx,
                              long clientMaxStreams) {
        this.protocol = protocol;
        this.sslCtx = sslCtx;
        this.clientMaxStreams = clientMaxStreams;
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

    @Override
    public void channelReleased(Channel ch) throws Exception {
        // Remove any existing handlers from the pipeline from the previous request.
        ChannelUtils.removeIfExists(ch.pipeline(),
                                    HttpStreamsClientHandler.class,
                                    ResponseHandler.class,
                                    ReadTimeoutHandler.class,
                                    WriteTimeoutHandler.class);
    }

    private void configureHttp2(Channel ch, ChannelPipeline pipeline) {
        pipeline.addLast(Http2MultiplexCodecBuilder
                             .forClient(new NoOpChannelInitializer())
                             // TODO disable frame logging for performance
                             .frameLogger(new SdkHttp2FrameLogger(LogLevel.DEBUG))
                             .headerSensitivityDetector((name, value) -> {
                                 String lowerName = name.toString().toLowerCase();
                                 return lowerName.equals("authorization") || lowerName.equals("amz-sdk-invocation-id");
                             })
                             .build());
        pipeline.addLast(new SimpleChannelInboundHandler<Http2SettingsFrame>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, Http2SettingsFrame msg) throws Exception {
                Long serverMaxStreams = Optional.ofNullable(msg.settings().maxConcurrentStreams()).orElse(Long.MAX_VALUE);
                ch.attr(MAX_CONCURRENT_STREAMS).set(Math.min(clientMaxStreams, serverMaxStreams));
                ch.attr(PROTOCOL_FUTURE).get().complete(ApplicationProtocolNames.HTTP_2);
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                ch.attr(PROTOCOL_FUTURE).get().completeExceptionally(cause);
            }
        });
    }

    private void configureHttp11(Channel ch, ChannelPipeline pipeline) {
        pipeline.addLast(new HttpClientCodec());
        // Disabling auto-read is needed for backpressure to work
        ch.config().setOption(ChannelOption.AUTO_READ, false);
        ch.attr(PROTOCOL_FUTURE).get().complete(ApplicationProtocolNames.HTTP_1_1);
    }

    private static class NoOpChannelInitializer extends ChannelInitializer<Channel> {
        @Override
        protected void initChannel(Channel ch) throws Exception {
        }
    }

}


