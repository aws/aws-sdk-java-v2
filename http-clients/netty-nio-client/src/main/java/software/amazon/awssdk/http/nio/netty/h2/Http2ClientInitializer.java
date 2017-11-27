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

import io.netty.channel.Channel;
import io.netty.channel.pool.AbstractChannelPoolHandler;
import io.netty.handler.codec.http2.DefaultHttp2Connection;
import io.netty.handler.codec.http2.DelegatingDecompressorFrameListener;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandlerBuilder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.ssl.SslContext;

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
        Http2Connection connection = new DefaultHttp2Connection(false);
        if (sslCtx != null) {
            ch.pipeline().addLast(sslCtx.newHandler(ch.alloc()));
        }
        ch.pipeline().addLast(
            new HttpToHttp2ConnectionHandlerBuilder()
                .frameListener(new DelegatingDecompressorFrameListener(
                    connection,
                    new SdkHttp2FrameListener()))
                .frameLogger(new SdkHttp2FrameLogger(LogLevel.DEBUG))
                .connection(connection)
                .build());

    }

}


