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

package software.amazon.awssdk.http.nio.netty.h2;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKeys.FIRST_BYTE_RECEIVED;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKeys.REQUEST_CONTEXT_KEY;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKeys.REQUEST_FINISH;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKeys.RESPONSE_COMPLETE_KEY;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2Flags;
import io.netty.handler.codec.http2.Http2FrameListener;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.util.Attribute;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.async.SdkHttpResponseHandler;
import software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKeys;
import software.amazon.awssdk.http.nio.netty.internal.RequestContext;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.FunctionalUtils.UnsafeRunnable;

public class SdkHttp2FrameListener implements Http2FrameListener {

    private static final Logger log = LoggerFactory.getLogger(SdkHttp2FrameListener.class);
    private final H2MetricsCollector metricsCollector;

    SdkHttp2FrameListener(H2MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }

    @Override
    public int onDataRead(ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding, boolean endOfStream)
            throws Http2Exception {
        int numBytes = data.nioBuffer().remaining();
        try {
            RequestContext requestContext = ctx.channel().attr(REQUEST_CONTEXT_KEY).get();
            if (!ctx.channel().attr(FIRST_BYTE_RECEIVED).get()) {
                ctx.channel().attr(FIRST_BYTE_RECEIVED).set(Boolean.TRUE);
                metricsCollector.putMetric("H2JavaSDK", "TimeToFirstByte",
                                           System.nanoTime() - ctx.channel().attr(REQUEST_FINISH).get());
            }
            SdkHttpResponseHandler<?> responseHandler = requestContext.handler();

            Attribute<Subscriber<? super ByteBuffer>> subscriberAttr = ctx.channel().attr(ChannelAttributeKeys.SUBSCRIBER_KEY);

            if (subscriberAttr.get() == null) {
                responseHandler.onStream(new H2Publisher(subscriberAttr));
            }
            // TODO backpressure
            subscriberAttr.get().onNext(copyToByteBuffer(data));
            if (endOfStream) {
                try {
                    subscriberAttr.get().onComplete();
                    responseHandler.complete();
                    ctx.channel().attr(RESPONSE_COMPLETE_KEY).get();
                    metricsCollector.putMetric("H2JavaSDK", "ResponseTime",
                                               System.nanoTime() - ctx.channel().attr(REQUEST_FINISH).get());
                } finally {
                    subscriberAttr.set(null);
                    runAndLogError("Could not release channel",
                        () -> requestContext.channelPool().release(ctx.channel()));
                }
            }
        } catch (Exception e) {
            log.error("Unable to read data frame", e);
        }
        // TODO we should return the number of bytes immediately processed. Any async processing of bytes should be notified
        // by the flow controller
        return numBytes;
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

    private static ByteBuffer copyToByteBuffer(ByteBuf byteBuf) {
        ByteBuffer bb = ByteBuffer.allocate(byteBuf.readableBytes());
        byteBuf.getBytes(byteBuf.readerIndex(), bb);
        bb.flip();
        return bb;
    }

    @Override
    public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int padding,
                              boolean endOfStream) throws Http2Exception {
        deliverHeaders(ctx, streamId, headers);
    }

    @Override
    public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int streamDependency, short weight,
                              boolean exclusive, int padding, boolean endOfStream) throws Http2Exception {
        deliverHeaders(ctx, streamId, headers);
    }

    private void deliverHeaders(ChannelHandlerContext ctx, int streamId, Http2Headers headers) throws Http2Exception {
        metricsCollector.putMetric("H2JavaSDK", "TimeToHeaders",
                                   System.nanoTime() - ctx.channel().attr(REQUEST_FINISH).get());
        HttpResponse response = HttpConversionUtil.toHttpResponse(streamId, headers, true);
        RequestContext requestContext = ctx.channel().attr(REQUEST_CONTEXT_KEY).get();
        SdkHttpResponseHandler<?> responseHandler = requestContext.handler();
        // TODO duplication
        responseHandler.headersReceived(SdkHttpFullResponse.builder()
                                                           .headers(fromNettyHeaders(response.headers()))
                                                           .statusCode(response.status().code())
                                                           .statusText(response.status().reasonPhrase())
                                                           .build());
    }

    private static Map<String, List<String>> fromNettyHeaders(HttpHeaders headers) {
        return headers.entries().stream()
                      .collect(groupingBy(Map.Entry::getKey,
                                          mapping(Map.Entry::getValue, Collectors.toList())));
    }

    @Override
    public void onPriorityRead(ChannelHandlerContext ctx, int streamId, int streamDependency, short weight,
                               boolean exclusive) throws Http2Exception {

        // TODO do we care about priority?
    }

    @Override
    public void onRstStreamRead(ChannelHandlerContext ctx, int streamId, long errorCode) throws Http2Exception {
        RequestContext requestContext = ctx.channel().attr(REQUEST_CONTEXT_KEY).get();
        requestContext.handler().exceptionOccurred(new Http2ResetException(errorCode));

        runAndLogError("Could not release channel",
            () -> requestContext.channelPool().release(ctx.channel()));
    }

    @Override
    public void onSettingsAckRead(ChannelHandlerContext ctx) throws Http2Exception {
    }

    @Override
    public void onSettingsRead(ChannelHandlerContext ctx, Http2Settings settings) throws Http2Exception {
        // Store max concurrent streams for multiplexing
    }

    @Override
    public void onPingRead(ChannelHandlerContext ctx, long data) throws Http2Exception {
        // Do we need to do anything with this? Send a ping ack or does Netty handle that?
    }

    @Override
    public void onPingAckRead(ChannelHandlerContext ctx, long data) throws Http2Exception {
    }

    @Override
    public void onPushPromiseRead(ChannelHandlerContext ctx, int streamId, int promisedStreamId, Http2Headers headers,
                                  int padding) throws Http2Exception {
        // Push promise out of scope
    }

    @Override
    public void onGoAwayRead(ChannelHandlerContext ctx, int lastStreamId, long errorCode, ByteBuf debugData) throws
                                                                                                             Http2Exception {
        RequestContext requestContext = ctx.channel().attr(REQUEST_CONTEXT_KEY).get();
        SdkHttpResponseHandler<?> responseHandler = requestContext.handler();
        if (responseHandler != null) {
            responseHandler.exceptionOccurred(new GoawayException(errorCode, debugData));
        }
        // TODO need to stop accepting new streams but allow current streams to complete. Connection should be closed
        // after all streams complete. Goaway will send number of highest stream eligible for processing so we
        // should kill any streams that happened to be created after that.
    }

    /**
     * TODO Do we need to handle this or will Netty return false for {@link Channel#isWritable()} and the reactive streams
     * handler can just stop pushing data?
     */
    @Override
    public void onWindowUpdateRead(ChannelHandlerContext ctx, int streamId, int windowSizeIncrement) throws Http2Exception {
    }

    @Override
    public void onUnknownFrame(ChannelHandlerContext ctx, byte frameType, int streamId, Http2Flags flags, ByteBuf payload)
            throws Http2Exception {
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

    private static class H2Publisher implements Publisher<ByteBuffer> {
        private final Attribute<Subscriber<? super ByteBuffer>> subscriberAttr;

        H2Publisher(Attribute<Subscriber<? super ByteBuffer>> subscriberAttr) {
            this.subscriberAttr = subscriberAttr;
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            subscriberAttr.set(subscriber);
            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long l) {
                }

                @Override
                public void cancel() {
                }
            });
        }
    }
}
