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

import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKeys.PROTOCOL_FUTURE;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKeys.REQUEST_CONTEXT_KEY;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKeys.RESPONSE_COMPLETE_KEY;

import com.typesafe.netty.http.HttpStreamsClientHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.pool.AbstractChannelPoolHandler;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http2.DefaultHttp2Connection;
import io.netty.handler.codec.http2.DelegatingDecompressorFrameListener;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandler;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandlerBuilder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.http.nio.netty.internal.ResponseHandler;

/**
 * Configures the client pipeline to support HTTP/2 frames.
 */
public class Http2ClientInitializer extends AbstractChannelPoolHandler {

    private final SslContext sslCtx;

    public Http2ClientInitializer(SslContext sslCtx) {
        this.sslCtx = sslCtx;
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

    @Override
    public void channelReleased(Channel channel) throws Exception {
        channel.attr(REQUEST_CONTEXT_KEY).set(null);
        channel.attr(RESPONSE_COMPLETE_KEY).set(false);
        removeIfExists(channel, HttpStreamsClientHandler.class, ResponseHandler.class,
                       ReadTimeoutHandler.class, WriteTimeoutHandler.class);
    }

    /**
     * Removes the handler from the pipeline if present.
     *
     * @param channel Channel to remove handlers from.
     * @param handlers Handlers to remove, identified by class.
     */
    @SafeVarargs
    private final void removeIfExists(Channel channel, Class<? extends ChannelHandler>... handlers) {
        for (Class<? extends ChannelHandler> handler : handlers) {
            if (channel.pipeline().get(handler) != null) {
                channel.pipeline().remove(handler);
            }
        }
    }

    private HttpToHttp2ConnectionHandler createH2ConnectionHandler() {
        Http2Connection connection = new DefaultHttp2Connection(false);
        return new HttpToHttp2ConnectionHandlerBuilder()
            .frameListener(new DelegatingDecompressorFrameListener(connection, new SdkHttp2FrameListener()))
            // TODO static log level
            .frameLogger(new SdkHttp2FrameLogger(LogLevel.DEBUG))
            .connection(connection)
            .build();
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
                    System.out.println("Configuring pipeline for H2");
                    ctx.pipeline().addLast(createH2ConnectionHandler());
                    ch.attr(PROTOCOL_FUTURE).get().complete(ApplicationProtocolNames.HTTP_2);
                } else {
                    System.out.println("Configuring pipeline for H1");
                    ctx.pipeline().addLast(new HttpClientCodec());
                    ch.attr(PROTOCOL_FUTURE).get().complete(ApplicationProtocolNames.HTTP_1_1);
                    // TODO these are request level handlers
                    //                    ctx.pipeline().addLast(new HttpStreamsClientHandler());
                    //                    ctx.pipeline().addLast(new ResponseHandler());
                }
                //                else {
                //                    // TODO what happens here, does future get notified?
                //                    throw new IllegalArgumentException("Unsupported protocol negotiated: " + protocol);
                //                }
            }
        });
    }

}


