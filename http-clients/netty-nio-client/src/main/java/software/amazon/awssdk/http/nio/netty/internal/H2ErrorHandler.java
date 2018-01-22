/*
 * Copyright 2012-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package software.amazon.awssdk.http.nio.netty.internal;

import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKeys.REQUEST_CONTEXT_KEY;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKeys.RESPONSE_COMPLETE_KEY;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.utils.FunctionalUtils.UnsafeRunnable;

public class H2ErrorHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(ResponseHandler.class);

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        RequestContext requestContext = ctx.channel().attr(REQUEST_CONTEXT_KEY).get();
        log.error("Exception processing request: {}", requestContext.sdkRequest(), cause);
        runAndLogError("SdkHttpResponseHandler threw an exception",
                       () -> {
                           requestContext.handler().exceptionOccurred(cause);
                           requestContext.handler().complete();
                       });
        runAndLogError("Could not release channel back to the pool", () -> closeAndRelease(ctx));
    }

    /**
     * Runs a given {@link UnsafeRunnable} and logs an error without throwing.
     *
     * @param errorMsg Message to log with exception thrown.
     * @param runnable Action to perform.
     */
    private static void runAndLogError(String errorMsg, UnsafeRunnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.error(errorMsg, e);
        }
    }

    /**
     * Close the channel and release it back into the pool.
     *
     * @param ctx Context for channel
     */
    private static void closeAndRelease(ChannelHandlerContext ctx) {
        RequestContext requestContext = ctx.channel().attr(REQUEST_CONTEXT_KEY).get();
        ctx.channel().close()
           .addListener(channelFuture -> requestContext.channelPool().release(ctx.channel()));
    }

    @Override
    public void channelInactive(ChannelHandlerContext handlerCtx) throws Exception {
        RequestContext requestCtx = handlerCtx.channel().attr(REQUEST_CONTEXT_KEY).get();
        boolean responseCompleted = handlerCtx.channel().attr(RESPONSE_COMPLETE_KEY).get();
        if (!responseCompleted) {
            runAndLogError("SdkHttpResponseHandler threw an exception when calling exceptionOccurred",
                           () -> requestCtx.handler().exceptionOccurred(new IOException("Server failed to send complete response")));
            runAndLogError("Could not release channel",
                           () -> requestCtx.channelPool().release(handlerCtx.channel()));
        }
    }
}
