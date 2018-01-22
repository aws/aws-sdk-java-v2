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

import static io.netty.handler.logging.LogLevel.INFO;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http2.DefaultHttp2Connection;
import io.netty.handler.codec.http2.DelegatingDecompressorFrameListener;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandlerBuilder;
import io.netty.handler.ssl.SslContext;
import software.amazon.awssdk.http.nio.netty.internal.utils.LoggingHandler;

/**
 * Configures the client pipeline to support HTTP/2 frames.
 */
public class Http2ChannelPoolHandler extends ChannelInitializer<SocketChannel> {

    private static final Http2FrameLogger logger = new Http2FrameLogger(INFO, Http2ChannelPoolHandler.class);

    private final SslContext sslCtx;

    public Http2ChannelPoolHandler(SslContext sslCtx) {
        this.sslCtx = sslCtx;
    }

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
//        Http2Connection connection = new DefaultHttp2Connection(false);
//        ch.pipeline().addLast(
//            new HttpToHttp2ConnectionHandlerBuilder()
//                .frameListener(new DelegatingDecompressorFrameListener(
//                    connection,
//                    new SdkHttp2FrameListener()))
//                .frameLogger(logger)
//                .connection(connection)
//                .build());
//        if (sslCtx != null) {
//            ch.pipeline().addLast(sslCtx.newHandler(ch.alloc()));
//        }
//        ch.pipeline().addFirst(new LoggingHandler(s -> System.out.println(s.get())));
    }

}


