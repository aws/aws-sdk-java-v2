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

import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.PROTOCOL_FUTURE;
import static software.amazon.awssdk.utils.StringUtils.lowerCase;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.pool.AbstractChannelPoolHandler;
import io.netty.channel.pool.ChannelPool;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http2.ForkedHttp2MultiplexCodecBuilder;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.nio.netty.internal.http2.Http2SettingsFrameHandler;

/**
 * ChannelPoolHandler to configure the client pipeline.
 */
@SdkInternalApi
public final class ChannelPipelineInitializer extends AbstractChannelPoolHandler {
    private final Protocol protocol;
    private final SslContext sslCtx;
    private final long clientMaxStreams;
    private final AtomicReference<ChannelPool> channelPoolRef;
    private final NettyConfiguration configuration;
    private final URI poolKey;

    public ChannelPipelineInitializer(Protocol protocol,
                                      SslContext sslCtx,
                                      long clientMaxStreams,
                                      AtomicReference<ChannelPool> channelPoolRef,
                                      NettyConfiguration configuration,
                                      URI poolKey) {
        this.protocol = protocol;
        this.sslCtx = sslCtx;
        this.clientMaxStreams = clientMaxStreams;
        this.channelPoolRef = channelPoolRef;
        this.configuration = configuration;
        this.poolKey = poolKey;
    }

    @Override
    public void channelCreated(Channel ch) {
        ch.attr(PROTOCOL_FUTURE).set(new CompletableFuture<>());
        ChannelPipeline pipeline = ch.pipeline();
        if (sslCtx != null) {

            // Need to provide host and port to enable SNI
            // https://github.com/netty/netty/issues/3801#issuecomment-104274440
            SslHandler sslHandler = sslCtx.newHandler(ch.alloc(), poolKey.getHost(), poolKey.getPort());
            configureSslEngine(sslHandler.engine());

            pipeline.addLast(sslHandler);
            pipeline.addLast(SslCloseCompletionEventHandler.getInstance());
        }

        if (protocol == Protocol.HTTP2) {
            configureHttp2(ch, pipeline);
        } else {
            configureHttp11(ch, pipeline);
        }

        if (configuration.reapIdleConnections()) {
            pipeline.addLast(new IdleConnectionReaperHandler(configuration.idleTimeoutMillis()));
        }

        if (configuration.connectionTtlMillis() > 0) {
            pipeline.addLast(new OldConnectionReaperHandler(configuration.connectionTtlMillis()));
        }

        pipeline.addLast(FutureCancelHandler.getInstance());
        pipeline.addLast(UnusedChannelExceptionHandler.getInstance());
        pipeline.addLast(new LoggingHandler(LogLevel.DEBUG));
    }

    /**
     * Enable HostName verification.
     *
     * See https://netty.io/4.0/api/io/netty/handler/ssl/SslContext.html#newHandler-io.netty.buffer.ByteBufAllocator-java.lang
     * .String-int-
     *
     * @param sslEngine the sslEngine to configure
     */
    private void configureSslEngine(SSLEngine sslEngine) {
        SSLParameters sslParameters = sslEngine.getSSLParameters();
        sslParameters.setEndpointIdentificationAlgorithm("HTTPS");
        sslEngine.setSSLParameters(sslParameters);
    }

    private void configureHttp2(Channel ch, ChannelPipeline pipeline) {
        ForkedHttp2MultiplexCodecBuilder codecBuilder = ForkedHttp2MultiplexCodecBuilder
            .forClient(new NoOpChannelInitializer())
            .headerSensitivityDetector((name, value) -> lowerCase(name.toString()).equals("authorization"))
            .initialSettings(Http2Settings.defaultSettings().initialWindowSize(1_048_576));

        codecBuilder.frameLogger(new Http2FrameLogger(LogLevel.DEBUG));

        pipeline.addLast(codecBuilder.build());

        pipeline.addLast(new Http2SettingsFrameHandler(ch, clientMaxStreams, channelPoolRef));
    }

    private void configureHttp11(Channel ch, ChannelPipeline pipeline) {
        pipeline.addLast(new HttpClientCodec());
        ch.attr(PROTOCOL_FUTURE).get().complete(Protocol.HTTP1_1);
    }

    private static class NoOpChannelInitializer extends ChannelInitializer<Channel> {
        @Override
        protected void initChannel(Channel ch) {
        }
    }

}


