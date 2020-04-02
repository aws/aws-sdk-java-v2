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

import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.HTTP2_CONNECTION;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.HTTP2_INITIAL_WINDOW_SIZE;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.PROTOCOL_FUTURE;
import static software.amazon.awssdk.http.nio.netty.internal.NettyConfiguration.HTTP2_CONNECTION_PING_TIMEOUT_SECONDS;
import static software.amazon.awssdk.utils.NumericUtils.saturatedCast;
import static software.amazon.awssdk.utils.StringUtils.lowerCase;

import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.pool.AbstractChannelPoolHandler;
import io.netty.channel.pool.ChannelPool;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http2.Http2FrameCodec;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslProvider;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.nio.netty.internal.http2.Http2GoAwayEventListener;
import software.amazon.awssdk.http.nio.netty.internal.http2.Http2PingHandler;
import software.amazon.awssdk.http.nio.netty.internal.http2.Http2SettingsFrameHandler;

/**
 * ChannelPoolHandler to configure the client pipeline.
 */
@SdkInternalApi
public final class ChannelPipelineInitializer extends AbstractChannelPoolHandler {
    private final Protocol protocol;
    private final SslContext sslCtx;
    private final SslProvider sslProvider;
    private final long clientMaxStreams;
    private final int clientInitialWindowSize;
    private final Duration healthCheckPingPeriod;
    private final AtomicReference<ChannelPool> channelPoolRef;
    private final NettyConfiguration configuration;
    private final URI poolKey;

    public ChannelPipelineInitializer(Protocol protocol,
                                      SslContext sslCtx,
                                      SslProvider sslProvider,
                                      long clientMaxStreams,
                                      int clientInitialWindowSize,
                                      Duration healthCheckPingPeriod,
                                      AtomicReference<ChannelPool> channelPoolRef,
                                      NettyConfiguration configuration,
                                      URI poolKey) {
        this.protocol = protocol;
        this.sslCtx = sslCtx;
        this.sslProvider = sslProvider;
        this.clientMaxStreams = clientMaxStreams;
        this.clientInitialWindowSize = clientInitialWindowSize;
        this.healthCheckPingPeriod = healthCheckPingPeriod;
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

            // Use unpooled allocator to avoid increased heap memory usage from Netty 4.1.43.
            // See https://github.com/netty/netty/issues/9768
            if (sslProvider == SslProvider.JDK) {
                ch.config().setOption(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT);
            }
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

        // Only add it for h1 channel because it does not apply to
        // h2 connection channel. It will be attached
        // to stream channels when they are created.
        if (protocol == Protocol.HTTP1_1) {
            pipeline.addLast(UnusedChannelExceptionHandler.getInstance());
        }

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
        // Using Http2FrameCodecBuilder and Http2MultiplexHandler based on 4.1.37 release notes
        // https://netty.io/news/2019/06/28/4-1-37-Final.html
        Http2FrameCodec codec =
            Http2FrameCodecBuilder.forClient()
                                  .headerSensitivityDetector((name, value) -> lowerCase(name.toString()).equals("authorization"))
                                  .initialSettings(Http2Settings.defaultSettings().initialWindowSize(clientInitialWindowSize))
                                  .frameLogger(new Http2FrameLogger(LogLevel.DEBUG))
                                  .build();

        // Connection listeners have higher priority than handlers, in the eyes of the Http2FrameCodec. The Http2FrameCodec will
        // close any connections when a GOAWAY is received, but we'd like to send a "GOAWAY happened" exception instead of just
        // closing the connection. Because of this, we use a go-away listener instead of a handler, so that we can send the
        // exception before the Http2FrameCodec closes the connection itself.
        codec.connection().addListener(new Http2GoAwayEventListener(ch));

        pipeline.addLast(codec);
        ch.attr(HTTP2_CONNECTION).set(codec.connection());

        ch.attr(HTTP2_INITIAL_WINDOW_SIZE).set(clientInitialWindowSize);
        pipeline.addLast(new Http2MultiplexHandler(new NoOpChannelInitializer()));
        pipeline.addLast(new Http2SettingsFrameHandler(ch, clientMaxStreams, channelPoolRef));
        if (healthCheckPingPeriod == null) {
            pipeline.addLast(new Http2PingHandler(HTTP2_CONNECTION_PING_TIMEOUT_SECONDS * 1_000));
        } else if (healthCheckPingPeriod.toMillis() > 0) {
            pipeline.addLast(new Http2PingHandler(saturatedCast(healthCheckPingPeriod.toMillis())));
        }
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


