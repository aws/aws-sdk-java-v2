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

package software.amazon.awssdk.http.nio.netty.internal.http2;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.TimeoutException;
import java.io.IOException;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.nio.netty.internal.utils.NettyClientLogger;

/**
 * Exception Handler for errors on the Http2 streams.
 */
@ChannelHandler.Sharable
@SdkInternalApi
public final class Http2StreamExceptionHandler extends ChannelInboundHandlerAdapter {
    private static final NettyClientLogger log = NettyClientLogger.getLogger(Http2StreamExceptionHandler.class);
    private static final Http2StreamExceptionHandler INSTANCE = new Http2StreamExceptionHandler();

    private Http2StreamExceptionHandler() {
    }

    public static Http2StreamExceptionHandler create() {
        return INSTANCE;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Channel parent = ctx.channel().parent();
        if (isIoError(cause) && parent != null) {
            MultiplexedChannelRecord record = parent.attr(Http2MultiplexedChannelPool.MULTIPLEXED_CHANNEL).get();
            if (record != null) {
                log.debug(parent, () -> "An I/O error occurred on HTTP/2 stream " + ctx.channel()
                                        + ". Connection " + parent + " will take no new streams.");
                record.closeToNewStreams();
            }
        }
        ctx.fireExceptionCaught(cause);
    }

    private boolean isIoError(Throwable cause) {
        return cause instanceof TimeoutException || cause instanceof IOException;
    }
}
