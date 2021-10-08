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

import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.EXECUTION_ID_KEY;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.REQUEST_CONTEXT_KEY;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.io.IOException;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Logger;

/**
 * Closes the channel if the execution future has been cancelled.
 */
@SdkInternalApi
@ChannelHandler.Sharable
public final class FutureCancelHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOG = Logger.loggerFor(FutureCancelHandler.class);
    private static final FutureCancelHandler INSTANCE = new FutureCancelHandler();

    private FutureCancelHandler() {
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
        if (!(e instanceof FutureCancelledException)) {
            ctx.fireExceptionCaught(e);
            return;
        }

        FutureCancelledException cancelledException = (FutureCancelledException) e;

        if (currentRequestCancelled(ctx, cancelledException)) {
            RequestContext requestContext = ctx.channel().attr(REQUEST_CONTEXT_KEY).get();
            requestContext.handler().onError(e);
            ctx.fireExceptionCaught(new IOException("Request cancelled"));
            ctx.close();
            requestContext.channelPool().release(ctx.channel());
        } else {
            LOG.debug(() -> String.format("Received a cancellation exception but it did not match the current execution ID. "
                                          + "Exception's execution ID is %d, but the current ID on the channel is %d. Exception"
                                          + " is being ignored.",
                                          cancelledException.getExecutionId(),
                                          executionId(ctx)));
        }
    }

    public static FutureCancelHandler getInstance() {
        return INSTANCE;
    }

    private boolean currentRequestCancelled(ChannelHandlerContext ctx, FutureCancelledException e) {
        return e.getExecutionId() == executionId(ctx);
    }

    private Long executionId(ChannelHandlerContext ctx) {
        return ctx.channel().attr(EXECUTION_ID_KEY).get();
    }
}
