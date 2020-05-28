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

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2Frame;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.Http2ResetFrame;
import io.netty.handler.codec.http2.HttpConversionUtil;
import java.io.IOException;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.HttpStatusFamily;
import software.amazon.awssdk.utils.Logger;

/**
 * Converts {@link Http2Frame}s to {@link HttpObject}s. Ignores the majority of {@link Http2Frame}s like PING
 * or SETTINGS.
 */
@SdkInternalApi
public class Http2ToHttpInboundAdapter extends SimpleChannelInboundHandler<Http2Frame> {
    private static final Logger log = Logger.loggerFor(Http2ToHttpInboundAdapter.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http2Frame frame) throws Exception {
        if (frame instanceof Http2DataFrame) {
            onDataRead((Http2DataFrame) frame, ctx);
        } else if (frame instanceof Http2HeadersFrame) {
            onHeadersRead((Http2HeadersFrame) frame, ctx);
            ctx.channel().read();
        } else if (frame instanceof Http2ResetFrame) {
            onRstStreamRead((Http2ResetFrame) frame, ctx);
        } else {
            // TODO this is related to the inbound window update bug. Revisit
            ctx.channel().parent().read();
        }
    }

    private void onHeadersRead(Http2HeadersFrame headersFrame, ChannelHandlerContext ctx) throws Http2Exception {

        HttpResponse httpResponse = HttpConversionUtil.toHttpResponse(headersFrame.stream().id(), headersFrame.headers(), true);
        ctx.fireChannelRead(httpResponse);

        if (HttpStatusFamily.of(httpResponse.status().code()) == HttpStatusFamily.SERVER_ERROR) {
            fireConnectionExceptionForServerError(ctx);
        }
    }

    private void fireConnectionExceptionForServerError(ChannelHandlerContext ctx) {
        if (ctx.channel().parent() != null) {
            Channel parent = ctx.channel().parent();
            log.debug(() -> "A 5xx server error occurred on an Http2 stream, notifying the connection channel " + ctx.channel());
            parent.pipeline().fireExceptionCaught(new Http2ConnectionTerminatingException("A 5xx server error occurred on an "
                                                                                          + "Http2 stream " + ctx.channel()));
        }
    }

    private void onDataRead(Http2DataFrame dataFrame, ChannelHandlerContext ctx) throws Http2Exception {
        ByteBuf data = dataFrame.content();
        data.retain();
        if (!dataFrame.isEndStream()) {
            ctx.fireChannelRead(new DefaultHttpContent(data));
        } else {
            ctx.fireChannelRead(new DefaultLastHttpContent(data));
        }
    }

    private void onRstStreamRead(Http2ResetFrame resetFrame, ChannelHandlerContext ctx) throws Http2Exception {
        ctx.fireExceptionCaught(new Http2ResetException(resetFrame.errorCode()));
    }

    public static final class Http2ResetException extends IOException {

        Http2ResetException(long errorCode) {
            super(String.format("Connection reset. Error - %s(%d)", Http2Error.valueOf(errorCode).name(), errorCode));
        }
    }
}
