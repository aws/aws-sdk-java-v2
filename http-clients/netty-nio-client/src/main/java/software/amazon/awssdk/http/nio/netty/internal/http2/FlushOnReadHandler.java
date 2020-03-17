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

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * This is an HTTP/2 related workaround for an issue where a WINDOW_UPDATE is
 * queued but not written to the socket, causing a read() on the channel to
 * hang if the remote endpoint thinks our inbound window is 0.
 */
@SdkInternalApi
@ChannelHandler.Sharable
public final class FlushOnReadHandler extends ChannelOutboundHandlerAdapter {
    private static final FlushOnReadHandler INSTANCE = new FlushOnReadHandler();

    private FlushOnReadHandler() {
    }

    @Override
    public void read(ChannelHandlerContext ctx) {
        //Note: order is important, we need to fire the read() event first
        // since it's what triggers the WINDOW_UPDATE frame write
        ctx.read();
        ctx.channel().parent().flush();
    }

    public static FlushOnReadHandler getInstance() {
        return INSTANCE;
    }
}
