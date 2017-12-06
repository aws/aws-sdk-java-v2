/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.typesafe.netty.http.HttpStreamsClientHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.pool.AbstractChannelPoolHandler;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.http.nio.netty.internal.utils.ChannelUtils;
import software.amazon.awssdk.http.nio.netty.internal.utils.LoggingHandler;
import software.amazon.awssdk.utils.Logger;

public class ChannelPipelineInitializer extends AbstractChannelPoolHandler {
    private static final Logger log = Logger.loggerFor(ChannelPipelineInitializer.class);

    private final SslContext sslContext;
    private final ChannelHandler[] handlers;

    public ChannelPipelineInitializer(SslContext sslContext) {
        this.sslContext = sslContext;

        List<ChannelHandler> tmpHandlers = new ArrayList<>();
        if (log.isLoggingLevelEnabled("debug")) {
            tmpHandlers.add(new LoggingHandler(log::debug));
        }

        handlers = tmpHandlers.toArray(new ChannelHandler[0]);
    }

    @Override
    public void channelCreated(Channel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();

        if (sslContext != null) {
            SslHandler handler = sslContext.newHandler(ch.alloc());
            p.addLast(handler);
            handler.handshakeFuture().addListener(future -> {
                if (!future.isSuccess()) {
                    log.error(() -> "SSL handshake failed.", future.cause());
                }
            });
        }

        p.addLast(new HttpClientCodec());
        p.addLast(handlers);
        // Disabling auto-read is needed for backpressure to work
        ch.config().setOption(ChannelOption.AUTO_READ, false);
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
}
