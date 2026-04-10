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

import static software.amazon.awssdk.http.nio.netty.internal.utils.NettyUtils.errorMessageWithChannelDiagnostics;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import java.io.IOException;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Handles writer idle events from IdleStateHandler to detect idle body write gaps.
 */
@SdkInternalApi
public final class WriteIdleTimeoutHandler extends ChannelDuplexHandler {
    private final long timeoutMillis;
    private boolean closed;

    public WriteIdleTimeoutHandler(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.WRITER_IDLE) {
                writeTimeout(ctx);
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    private void writeTimeout(ChannelHandlerContext ctx) {
        if (!closed) {
            String message = String.format("No data was written to the request body within %dms. "
                                           + "This can occur if the request body publisher is not producing data, "
                                           + "for example when using AsyncRequestBody.fromInputStream() with an "
                                           + "executor that has fewer threads than concurrent requests. "
                                           + "Verify that the request body publisher is sending data or "
                                           + "investigate why it may be blocked.", timeoutMillis);
            ctx.fireExceptionCaught(new IOException(errorMessageWithChannelDiagnostics(ctx.channel(), message)));
            ctx.close();
            closed = true;
        }
    }
}
