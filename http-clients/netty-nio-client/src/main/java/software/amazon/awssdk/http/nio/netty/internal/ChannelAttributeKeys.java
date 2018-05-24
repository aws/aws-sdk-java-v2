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

package software.amazon.awssdk.http.nio.netty.internal;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.nio.netty.internal.http2.MultiplexedChannelRecord;

/**
 * Keys for attributes attached via {@link io.netty.channel.Channel#attr(AttributeKey)}.
 */
public final class ChannelAttributeKeys {

    /**
     * Future that when a protocol (http/1.1 or h2) has been selected.
     */
    public static final AttributeKey<CompletableFuture<Protocol>> PROTOCOL_FUTURE = AttributeKey.newInstance("protocolFuture");

    /**
     * Reference to {@link MultiplexedChannelRecord} which stores information about leased streams for a multiplexed connection.
     */
    public static final AttributeKey<MultiplexedChannelRecord> CHANNEL_POOL_RECORD =
        AttributeKey.newInstance("channelPoolRecord");

    /**
     * Value of the MAX_CONCURRENT_STREAMS from the server's SETTING frame.
     */
    public static final AttributeKey<Long> MAX_CONCURRENT_STREAMS = AttributeKey.newInstance("maxConcurrentStreams");

    /**
     * Attribute key for {@link RequestContext}.
     */
    static final AttributeKey<RequestContext> REQUEST_CONTEXT_KEY = AttributeKey.newInstance("requestContext");

    static final AttributeKey<Subscriber<? super ByteBuffer>> SUBSCRIBER_KEY = AttributeKey.newInstance("subscriber");

    static final AttributeKey<Boolean> RESPONSE_COMPLETE_KEY = AttributeKey.newInstance("responseComplete");

    private ChannelAttributeKeys() {
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
            .attr(ChannelAttributeKeys.PROTOCOL_FUTURE).get().join();
    }
}
