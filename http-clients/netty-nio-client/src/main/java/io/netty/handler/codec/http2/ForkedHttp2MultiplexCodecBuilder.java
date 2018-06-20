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

package io.netty.handler.codec.http2;

import static io.netty.util.internal.ObjectUtil.checkNotNull;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;

public class ForkedHttp2MultiplexCodecBuilder
     extends AbstractHttp2ConnectionHandlerBuilder<ForkedHttp2MultiplexCodec, ForkedHttp2MultiplexCodecBuilder> {

    final ChannelHandler childHandler;

    ForkedHttp2MultiplexCodecBuilder(boolean server, ChannelHandler childHandler) {
        server(server);
        this.childHandler = checkSharable(checkNotNull(childHandler, "childHandler"));
    }

    private static ChannelHandler checkSharable(ChannelHandler handler) {
        if ((handler instanceof ChannelHandlerAdapter && !((ChannelHandlerAdapter) handler).isSharable()) &&
            !handler.getClass().isAnnotationPresent(ChannelHandler.Sharable.class)) {
            throw new IllegalArgumentException("The handler must be Sharable");
        }
        return handler;
    }

    /**
     * Creates a builder for a HTTP/2 client.
     *
     * @param childHandler the handler added to channels for remotely-created streams. It must be
     *     {@link ChannelHandler.Sharable}.
     */
    public static ForkedHttp2MultiplexCodecBuilder forClient(ChannelHandler childHandler) {
        return new ForkedHttp2MultiplexCodecBuilder(false, childHandler);
    }

    /**
     * Creates a builder for a HTTP/2 server.
     *
     * @param childHandler the handler added to channels for remotely-created streams. It must be
     *     {@link ChannelHandler.Sharable}.
     */
    public static ForkedHttp2MultiplexCodecBuilder forServer(ChannelHandler childHandler) {
        return new ForkedHttp2MultiplexCodecBuilder(true, childHandler);
    }

    @Override
    public Http2Settings initialSettings() {
        return super.initialSettings();
    }

    @Override
    public ForkedHttp2MultiplexCodecBuilder initialSettings(Http2Settings settings) {
        return super.initialSettings(settings);
    }

    @Override
    public long gracefulShutdownTimeoutMillis() {
        return super.gracefulShutdownTimeoutMillis();
    }

    @Override
    public ForkedHttp2MultiplexCodecBuilder gracefulShutdownTimeoutMillis(long gracefulShutdownTimeoutMillis) {
        return super.gracefulShutdownTimeoutMillis(gracefulShutdownTimeoutMillis);
    }

    @Override
    public boolean isServer() {
        return super.isServer();
    }

    @Override
    public int maxReservedStreams() {
        return super.maxReservedStreams();
    }

    @Override
    public ForkedHttp2MultiplexCodecBuilder maxReservedStreams(int maxReservedStreams) {
        return super.maxReservedStreams(maxReservedStreams);
    }

    @Override
    public boolean isValidateHeaders() {
        return super.isValidateHeaders();
    }

    @Override
    public ForkedHttp2MultiplexCodecBuilder validateHeaders(boolean validateHeaders) {
        return super.validateHeaders(validateHeaders);
    }

    @Override
    public Http2FrameLogger frameLogger() {
        return super.frameLogger();
    }

    @Override
    public ForkedHttp2MultiplexCodecBuilder frameLogger(Http2FrameLogger frameLogger) {
        return super.frameLogger(frameLogger);
    }

    @Override
    public boolean encoderEnforceMaxConcurrentStreams() {
        return super.encoderEnforceMaxConcurrentStreams();
    }

    @Override
    public ForkedHttp2MultiplexCodecBuilder encoderEnforceMaxConcurrentStreams(boolean encoderEnforceMaxConcurrentStreams) {
        return super.encoderEnforceMaxConcurrentStreams(encoderEnforceMaxConcurrentStreams);
    }

    @Override
    public Http2HeadersEncoder.SensitivityDetector headerSensitivityDetector() {
        return super.headerSensitivityDetector();
    }

    @Override
    public ForkedHttp2MultiplexCodecBuilder headerSensitivityDetector(
        Http2HeadersEncoder.SensitivityDetector headerSensitivityDetector) {
        return super.headerSensitivityDetector(headerSensitivityDetector);
    }

    @Override
    public ForkedHttp2MultiplexCodecBuilder encoderIgnoreMaxHeaderListSize(boolean ignoreMaxHeaderListSize) {
        return super.encoderIgnoreMaxHeaderListSize(ignoreMaxHeaderListSize);
    }

    @Override
    public ForkedHttp2MultiplexCodecBuilder initialHuffmanDecodeCapacity(int initialHuffmanDecodeCapacity) {
        return super.initialHuffmanDecodeCapacity(initialHuffmanDecodeCapacity);
    }

    @Override
    public ForkedHttp2MultiplexCodec build() {
        return super.build();
    }

    @Override
    protected ForkedHttp2MultiplexCodec build(
        Http2ConnectionDecoder decoder, Http2ConnectionEncoder encoder, Http2Settings initialSettings) {
        return new ForkedHttp2MultiplexCodec(encoder, decoder, initialSettings, childHandler);
    }
}
