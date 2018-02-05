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
package software.amazon.awssdk.http.nio.netty.h2;


import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2Frame;
import io.netty.handler.codec.http2.Http2FrameListener;
import io.netty.handler.codec.http2.Http2GoAwayFrame;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.Http2PingFrame;
import io.netty.handler.codec.http2.Http2ResetFrame;
import io.netty.handler.codec.http2.Http2SettingsFrame;
import io.netty.handler.codec.http2.Http2UnknownFrame;
import io.netty.handler.codec.http2.Http2WindowUpdateFrame;

/**
 * Calls appropriate method on {@link Http2FrameListener} given a {@link Http2Frame}.
 */
public class SdkHttp2FrameVisitor {

    private final Http2FrameListener frameListener;

    public SdkHttp2FrameVisitor(Http2FrameListener frameListener) {
        this.frameListener = frameListener;
    }

    public void visit(Http2Frame frame, ChannelHandlerContext ctx) throws Http2Exception {
        if (frame instanceof Http2DataFrame) {
            onDataRead((Http2DataFrame) frame, ctx);
        } else if (frame instanceof Http2HeadersFrame) {
            onHeadersRead((Http2HeadersFrame) frame, ctx);
        } else if (frame instanceof Http2ResetFrame) {
            onRstStreamRead((Http2ResetFrame) frame, ctx);
        } else if (frame instanceof Http2GoAwayFrame) {
            onGoAwayRead((Http2GoAwayFrame) frame, ctx);
        } else if (frame instanceof Http2PingFrame) {
            onPingRead((Http2PingFrame) frame, ctx);
        } else if (frame instanceof Http2WindowUpdateFrame) {
            onWindowUpdateRead((Http2WindowUpdateFrame) frame, ctx);
        } else if (frame instanceof Http2UnknownFrame) {
            onUnknownFrame((Http2UnknownFrame) frame, ctx);
        } else if (frame instanceof Http2SettingsFrame) {
            // TODO SETTINGS and SETTINGS ACK
        }
        // TODO PRI
        // TODO PUSH PROMISE
    }

    private void onHeadersRead(Http2HeadersFrame headersFrame, ChannelHandlerContext ctx) throws Http2Exception {
        frameListener.onHeadersRead(ctx, headersFrame.stream().id(), headersFrame.headers(), headersFrame.padding(), headersFrame.isEndStream());
    }

    private void onDataRead(Http2DataFrame dataFrame, ChannelHandlerContext ctx) throws Http2Exception {
        frameListener.onDataRead(ctx, dataFrame.stream().id(), dataFrame.content(), dataFrame.padding(), dataFrame.isEndStream());
    }

    private void onWindowUpdateRead(Http2WindowUpdateFrame windowUpdateFrame, ChannelHandlerContext ctx) throws Http2Exception {
        frameListener.onWindowUpdateRead(ctx, windowUpdateFrame.stream().id(), windowUpdateFrame.windowSizeIncrement());
    }

    private void onPingRead(Http2PingFrame pingFrame, ChannelHandlerContext ctx) throws Http2Exception {
        if (pingFrame.ack()) {
            frameListener.onPingAckRead(ctx, pingFrame.content());
        } else {
            frameListener.onPingRead(ctx, pingFrame.content());
        }
    }

    private void onGoAwayRead(Http2GoAwayFrame goAwayFrame, ChannelHandlerContext ctx) throws Http2Exception {
        frameListener.onGoAwayRead(ctx, goAwayFrame.lastStreamId(), goAwayFrame.errorCode(), goAwayFrame.content());
    }

    private void onRstStreamRead(Http2ResetFrame resetFrame, ChannelHandlerContext ctx) throws Http2Exception {
        frameListener.onRstStreamRead(ctx, resetFrame.stream().id(), resetFrame.errorCode());
    }

    private void onUnknownFrame(Http2UnknownFrame unknownFrame, ChannelHandlerContext ctx) throws Http2Exception {
        frameListener.onUnknownFrame(ctx, unknownFrame.frameType(), unknownFrame.stream().id(), unknownFrame.flags(),
                                     unknownFrame.content());
    }
}
