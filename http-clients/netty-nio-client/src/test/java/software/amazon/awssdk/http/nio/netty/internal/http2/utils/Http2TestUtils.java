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

package software.amazon.awssdk.http.nio.netty.internal.http2.utils;


import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http2.Http2FrameCodec;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.logging.LogLevel;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey;

public final class Http2TestUtils {
    public static final int INITIAL_WINDOW_SIZE = 1_048_576;

    public static EmbeddedChannel newHttp2Channel() {
        return newHttp2Channel(new NoOpHandler());
    }

    public static EmbeddedChannel newHttp2Channel(ChannelHandler channelHandler) {
        Http2FrameCodec http2FrameCodec = Http2FrameCodecBuilder.forClient().initialSettings(
            Http2Settings.defaultSettings().initialWindowSize(INITIAL_WINDOW_SIZE))
                                                                .frameLogger(new Http2FrameLogger(LogLevel.DEBUG)).build();
        EmbeddedChannel channel = new EmbeddedChannel(http2FrameCodec,
                                                      new Http2MultiplexHandler(channelHandler));

        channel.attr(ChannelAttributeKey.HTTP2_CONNECTION).set(http2FrameCodec.connection());
        channel.attr(ChannelAttributeKey.HTTP2_INITIAL_WINDOW_SIZE).set(INITIAL_WINDOW_SIZE);
        channel.attr(ChannelAttributeKey.PROTOCOL_FUTURE).set(CompletableFuture.completedFuture(Protocol.HTTP2));
        return channel;
    }

    private static class NoOpHandler extends ChannelInitializer<Channel> {
        @Override
        protected void initChannel(Channel ch) {
        }
    }
}
