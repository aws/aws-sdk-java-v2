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

import io.netty.channel.Channel;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2FrameStream;
import io.netty.util.AttributeKey;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.nio.netty.internal.http2.Http2MultiplexedChannelPool;
import software.amazon.awssdk.http.nio.netty.internal.http2.PingTracker;
import software.amazon.awssdk.http.nio.netty.internal.utils.NettyUtils;

/**
 * Keys for attributes attached via {@link io.netty.channel.Channel#attr(AttributeKey)}.
 */
@SdkInternalApi
public final class ChannelAttributeKey {

    /**
     * Future that when a protocol (http/1.1 or h2) has been selected.
     */
    public static final AttributeKey<CompletableFuture<Protocol>> PROTOCOL_FUTURE = NettyUtils.getOrCreateAttributeKey(
        "aws.http.nio.netty.async.protocolFuture");

    /**
     * Reference to {@link Http2MultiplexedChannelPool} which stores information about leased streams for a multiplexed
     * connection.
     */
    public static final AttributeKey<Http2MultiplexedChannelPool> HTTP2_MULTIPLEXED_CHANNEL_POOL =
        NettyUtils.getOrCreateAttributeKey("aws.http.nio.netty.async.http2MultiplexedChannelPool");

    public static final AttributeKey<PingTracker> PING_TRACKER =
        NettyUtils.getOrCreateAttributeKey("aws.http.nio.netty.async.h2.pingTracker");

    public static final AttributeKey<Http2Connection> HTTP2_CONNECTION =
        NettyUtils.getOrCreateAttributeKey("aws.http.nio.netty.async.http2Connection");

    public static final AttributeKey<Integer> HTTP2_INITIAL_WINDOW_SIZE =
        NettyUtils.getOrCreateAttributeKey("aws.http.nio.netty.async.http2InitialWindowSize");

    /**
     * Value of the MAX_CONCURRENT_STREAMS from the server's SETTING frame.
     */
    public static final AttributeKey<Long> MAX_CONCURRENT_STREAMS = NettyUtils.getOrCreateAttributeKey(
        "aws.http.nio.netty.async.maxConcurrentStreams");

    /**
     * The {@link Http2FrameStream} associated with this stream channel. This is added to stream channels when they are created,
     * before they are fully initialized.
     */
    public static final AttributeKey<Http2FrameStream> HTTP2_FRAME_STREAM = NettyUtils.getOrCreateAttributeKey(
        "aws.http.nio.netty.async.http2FrameStream");

    /**
     * {@link AttributeKey} to keep track of whether we should close the connection after this request
     * has completed.
     */
    static final AttributeKey<Boolean> KEEP_ALIVE = NettyUtils.getOrCreateAttributeKey("aws.http.nio.netty.async.keepAlive");

    /**
     * Attribute key for {@link RequestContext}.
     */
    static final AttributeKey<RequestContext> REQUEST_CONTEXT_KEY = NettyUtils.getOrCreateAttributeKey(
        "aws.http.nio.netty.async.requestContext");

    static final AttributeKey<Subscriber<? super ByteBuffer>> SUBSCRIBER_KEY = NettyUtils.getOrCreateAttributeKey(
        "aws.http.nio.netty.async.subscriber");

    static final AttributeKey<Boolean> RESPONSE_COMPLETE_KEY = NettyUtils.getOrCreateAttributeKey(
        "aws.http.nio.netty.async.responseComplete");

    /**
     * {@link AttributeKey} to keep track of whether we have received the {@link LastHttpContent}.
     */
    static final AttributeKey<Boolean> LAST_HTTP_CONTENT_RECEIVED_KEY = NettyUtils.getOrCreateAttributeKey(
        "aws.http.nio.netty.async.lastHttpContentReceived");

    static final AttributeKey<CompletableFuture<Void>> EXECUTE_FUTURE_KEY = NettyUtils.getOrCreateAttributeKey(
            "aws.http.nio.netty.async.executeFuture");

    static final AttributeKey<Long> EXECUTION_ID_KEY = NettyUtils.getOrCreateAttributeKey(
            "aws.http.nio.netty.async.executionId");

    /**
     * Whether the channel is still in use
     */
    static final AttributeKey<Boolean> IN_USE = NettyUtils.getOrCreateAttributeKey("aws.http.nio.netty.async.inUse");

    /**
     * Whether the channel should be closed once it is released.
     */
    static final AttributeKey<Boolean> CLOSE_ON_RELEASE = NettyUtils.getOrCreateAttributeKey(
            "aws.http.nio.netty.async.closeOnRelease");

    private ChannelAttributeKey() {
    }

    /**
     * Gets the protocol of the channel assuming that it has already been negotiated.
     *
     * @param channel Channel to get protocol for.
     * @return Protocol of channel.
     */
    static Protocol getProtocolNow(Channel channel) {
        // For HTTP/2 the protocol future will be on the parent socket channel
        return (channel.parent() == null ? channel : channel.parent())
            .attr(ChannelAttributeKey.PROTOCOL_FUTURE).get().join();
    }
}
