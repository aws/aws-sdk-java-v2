/*
 * Copyright 2014 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License, version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package software.amazon.awssdk.http.nio.netty.h2;

import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKeys.MAX_CONCURRENT_STREAMS;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKeys.PROTOCOL_FUTURE;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.pool.AbstractChannelPoolHandler;
import io.netty.handler.codec.http2.Http2MultiplexCodecBuilder;
import io.netty.handler.codec.http2.Http2SettingsFrame;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.netty.handler.ssl.SslContext;
import java.util.concurrent.CompletableFuture;

/**
 * Configures the client pipeline to support HTTP/2 frames with multiplexed streams.
 */
public class Http2MultiplexInitializer extends AbstractChannelPoolHandler {

    private final SslContext sslCtx;
    private final long maxStreams;

    public Http2MultiplexInitializer(H2MetricsCollector metricsCollector, SslContext sslCtx, long maxStreams) {
        this.sslCtx = sslCtx;
        this.maxStreams = maxStreams;
    }

    @Override
    public void channelCreated(Channel ch) throws Exception {
        ch.attr(PROTOCOL_FUTURE).set(new CompletableFuture<>());
        if (sslCtx != null) {
            configureSsl(ch);
        } else {
            // TODO http
            ch.attr(PROTOCOL_FUTURE).get().complete(ApplicationProtocolNames.HTTP_1_1);
        }
    }

    private void configureSsl(Channel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        // We must wait for the handshake to finish and the protocol to be negotiated before configuring
        // the HTTP/2 components of the pipeline.
        pipeline.addLast(sslCtx.newHandler(ch.alloc()));
        pipeline.addLast(new ApplicationProtocolNegotiationHandler(ApplicationProtocolNames.HTTP_1_1) {
            @Override
            protected void configurePipeline(ChannelHandlerContext ctx, String protocol) {
                System.out.println("Negotiated protocol = " + protocol);
                if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
                    ch.pipeline().addLast(Http2MultiplexCodecBuilder
                                              .forClient(new NoOpChannelInitializer())
                                              .frameLogger(new SdkHttp2FrameLogger(LogLevel.DEBUG))
                                              .propagateSettings(true)
                                              .headerSensitivityDetector((name, value) -> {
                                                  String lowerName = name.toString().toLowerCase();
                                                  return lowerName.equals("authorization") || lowerName.equals("amz-sdk-invocation-id");
                                              })
                                              .build());
                    ch.pipeline().addLast(new SimpleChannelInboundHandler<Http2SettingsFrame>() {
                        @Override
                        protected void channelRead0(ChannelHandlerContext ctx, Http2SettingsFrame msg) throws Exception {
                            // TODO fix this
                            // ch.attr(MAX_CONCURRENT_STREAMS).set(msg.settings().maxConcurrentStreams());
                            ch.attr(MAX_CONCURRENT_STREAMS).set(maxStreams);
                            ch.attr(PROTOCOL_FUTURE).get().complete(ApplicationProtocolNames.HTTP_2);
                        }
                    });
                } else {
                    // TODO configure pipeline for H1
                    ch.attr(PROTOCOL_FUTURE).get().complete(ApplicationProtocolNames.HTTP_1_1);
                }
            }
        });
    }

    private static class NoOpChannelInitializer extends ChannelInitializer<Channel> {
        @Override
        protected void initChannel(Channel ch) throws Exception {
        }
    }

}


