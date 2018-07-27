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
import io.netty.handler.codec.http2.Http2Flags;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.logging.LogLevel;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.BinaryUtils;

@SdkInternalApi
public class SdkHttp2FrameLogger extends Http2FrameLogger {

    private static final Logger log = LoggerFactory.getLogger(SdkHttp2FrameLogger.class);

    public SdkHttp2FrameLogger(LogLevel level) {
        super(level);
    }

    @Override
    public void logWindowsUpdate(Direction direction, ChannelHandlerContext ctx, int streamId, int windowSizeIncrement) {
        log("{} WINDOW_UPDATE: streamId={} windowSizeIncrement={}", direction.name(), streamId, windowSizeIncrement);
    }

    @Override
    public void logGoAway(Direction direction, ChannelHandlerContext ctx, int lastStreamId, long errorCode, ByteBuf debugData) {
        log("{} GO_AWAY: lastStreamId={} errorCode={} length={}\n{}", direction.name(), lastStreamId, errorCode,
            debugData.readableBytes(), dataToString(direction, debugData));
    }

    @Override
    public void logSettings(Direction direction, ChannelHandlerContext ctx, Http2Settings settings) {
        log("{} SETTINGS: ack=false settings={}", direction.name(), settings);
    }

    @Override
    public void logPing(Direction direction, ChannelHandlerContext ctx, long data) {
        log("{} PING: ack=false length={}", direction.name(), data);
    }

    @Override
    public void logPingAck(Direction direction, ChannelHandlerContext ctx, long data) {
        log("{} PING: ack=true length={}\n{}", direction.name(), data);
    }

    @Override
    public void logPriority(Direction direction, ChannelHandlerContext ctx, int streamId,
                            int streamDependency, short weight, boolean exclusive) {
        log("{} PRIORITY: streamId={} streamDependency={} weight={} exclusive={}",
            direction.name(), streamId, streamDependency, weight, exclusive);
    }

    @Override
    public void logRstStream(Direction direction, ChannelHandlerContext ctx, int streamId, long errorCode) {
        log("{} RST_STREAM: streamId={} errorCode={}", direction.name(), streamId, errorCode);
    }

    @Override
    public void logUnknownFrame(Direction direction, ChannelHandlerContext ctx, byte frameType,
                                int streamId, Http2Flags flags, ByteBuf data) {
        log("{} UNKNOWN: frameType={} streamId={} flags={} length={}\n{}",
            direction.name(), frameType & 255, streamId, flags.value(), data.readableBytes(), dataToString(direction, data));
    }

    @Override
    public void logPushPromise(Direction direction, ChannelHandlerContext ctx, int streamId,
                               int promisedStreamId, Http2Headers headers, int padding) {
        log("{} PUSH_PROMISE: streamId={} promisedStreamId={} padding={}\n{}",
            direction.name(), streamId, promisedStreamId, padding, formatHeaders(direction, headers));
    }

    @Override
    public void logSettingsAck(Direction direction, ChannelHandlerContext ctx) {
        log("{} SETTINGS: ack=true", direction.name());
    }

    @Override
    public void logHeaders(Direction direction, ChannelHandlerContext ctx, int streamId,
                           Http2Headers headers, int padding, boolean endStream) {
        log("{} HEADERS: streamId={} padding={} endStream={}\n{}",
            direction.name(), streamId, padding, endStream,
            formatHeaders(direction, headers));
    }

    @Override
    public void logHeaders(Direction direction, ChannelHandlerContext ctx, int streamId, Http2Headers headers,
                           int streamDependency, short weight, boolean exclusive, int padding, boolean endStream) {
        log("{} HEADERS: streamId={} streamDependency={} weight={} exclusive={} padding={} endStream={}\n{}",
            direction.name(), streamId, streamDependency, weight, exclusive, padding, endStream,
            formatHeaders(direction, headers));

    }

    private String formatHeaders(Direction direction, Http2Headers headers) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(headers.iterator(), Spliterator.ORDERED), false)
                            .map(h -> String.format("%s %s: %s", indentArrow(direction), h.getKey(), h.getValue()))
                            .collect(Collectors.joining("\n"));
    }

    private String indentArrow(Direction direction) {
        if (direction == Direction.INBOUND) {
            return "\t\t<<";
        } else {
            return "\t\t>>";
        }
    }

    @Override
    public void logData(Direction direction, ChannelHandlerContext ctx, int streamId,
                        ByteBuf data, int padding, boolean endStream) {
        if (log.isTraceEnabled()) {
            log.trace("{} DATA: streamId={} padding={} endStream={} length={}\n{}",
                      direction, streamId, padding, endStream, data.nioBuffer().remaining(),
                      dataToString(direction, data));
        } else {
            log("{} DATA: streamId={} padding={} endStream={} length={}\n",
                direction, streamId, padding, endStream, data.nioBuffer().remaining());
        }
    }

    private void log(String msg, Object... args) {
        log.debug(msg, args);
    }

    private String dataToString(Direction direction, ByteBuf data) {
        return indentArrow(direction) + " " +
               new String(BinaryUtils.copyBytesFrom(data.nioBuffer()), StandardCharsets.UTF_8);
    }

    /**
     * @return Http2FrameLogger if debug logs are enabled, otherwise empty {@link Optional}.
     */
    public static Optional<Http2FrameLogger> frameLogger() {
        return log.isDebugEnabled() ? Optional.of(new SdkHttp2FrameLogger(LogLevel.DEBUG)) : Optional.empty();
    }
}
