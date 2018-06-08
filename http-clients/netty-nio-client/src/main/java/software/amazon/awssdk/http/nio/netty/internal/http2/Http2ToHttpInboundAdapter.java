/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2Frame;
import io.netty.handler.codec.http2.Http2GoAwayFrame;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.Http2ResetFrame;
import io.netty.handler.codec.http2.HttpConversionUtil;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import software.amazon.awssdk.utils.BinaryUtils;

/**
 * Converts {@link Http2Frame}s to {@link HttpObject}s. Ignores the majority of {@link Http2Frame}s like PING
 * or SETTINGS.
 */
public class Http2ToHttpInboundAdapter extends SimpleChannelInboundHandler<Http2Frame> {

    public Http2ToHttpInboundAdapter() {
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http2Frame frame) throws Exception {
        if (frame instanceof Http2DataFrame) {
            onDataRead((Http2DataFrame) frame, ctx);
        } else if (frame instanceof Http2HeadersFrame) {
            onHeadersRead((Http2HeadersFrame) frame, ctx);
            ctx.channel().read();
        } else if (frame instanceof Http2ResetFrame) {
            onRstStreamRead((Http2ResetFrame) frame, ctx);
        } else if (frame instanceof Http2GoAwayFrame) {
            onGoAwayRead((Http2GoAwayFrame) frame, ctx);
        } else {
            ctx.channel().parent().read();
        }
    }

    private void onHeadersRead(Http2HeadersFrame headersFrame, ChannelHandlerContext ctx) throws Http2Exception {
        ctx.fireChannelRead(HttpConversionUtil.toHttpResponse(headersFrame.stream().id(), headersFrame.headers(), true));
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

    private void onGoAwayRead(Http2GoAwayFrame goAwayFrame, ChannelHandlerContext ctx) throws Http2Exception {
        ctx.fireExceptionCaught(new GoawayException(goAwayFrame.errorCode(), goAwayFrame.content()));
    }

    private void onRstStreamRead(Http2ResetFrame resetFrame, ChannelHandlerContext ctx) throws Http2Exception {
        ctx.fireExceptionCaught(new Http2ResetException(resetFrame.errorCode()));
    }

    public static class Http2ResetException extends IOException {

        Http2ResetException(long errorCode) {
            super(String.format("Connection reset. Error - %s(%d)", Http2Error.valueOf(errorCode).name(), errorCode));
        }
    }

    /**
     * Exception thrown when a GOAWAY frame is sent by the service.
     */
    private static class GoawayException extends Throwable {

        private final long errorCode;
        private final byte[] debugData;

        GoawayException(long errorCode, ByteBuf debugData) {
            this.errorCode = errorCode;
            this.debugData = BinaryUtils.copyBytesFrom(debugData.nioBuffer());
        }

        @Override
        public String getMessage() {
            return String.format("GOAWAY received. Error Code = %d, Debug Data = %s",
                                 errorCode, new String(debugData, StandardCharsets.UTF_8));
        }
    }
}
