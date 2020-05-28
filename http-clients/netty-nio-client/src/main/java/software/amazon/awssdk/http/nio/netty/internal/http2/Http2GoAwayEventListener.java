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
import io.netty.handler.codec.http2.Http2ConnectionAdapter;
import io.netty.handler.codec.http2.Http2GoAwayFrame;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey;
import software.amazon.awssdk.utils.Logger;

/**
 * Handles {@link Http2GoAwayFrame}s sent on a connection. This will pass the frame along to the connection's 
 * {@link Http2MultiplexedChannelPool#handleGoAway(Channel, int, GoAwayException)}.
 */
@SdkInternalApi
public final class Http2GoAwayEventListener extends Http2ConnectionAdapter {
    private static final Logger log = Logger.loggerFor(Http2GoAwayEventListener.class);

    private final Channel parentChannel;

    public Http2GoAwayEventListener(Channel parentChannel) {
        this.parentChannel = parentChannel;
    }


    @Override
    public void onGoAwayReceived(int lastStreamId, long errorCode, ByteBuf debugData) {
        Http2MultiplexedChannelPool channelPool = parentChannel.attr(ChannelAttributeKey.HTTP2_MULTIPLEXED_CHANNEL_POOL).get();
        GoAwayException exception = new GoAwayException(errorCode, debugData.retain());
        if (channelPool != null) {
            channelPool.handleGoAway(parentChannel, lastStreamId, exception);
        } else {
            log.warn(() -> "GOAWAY received on a connection (" + parentChannel + ") not associated with any multiplexed "
                           + "channel pool.");
            parentChannel.pipeline().fireExceptionCaught(exception);
        }
    }
}
