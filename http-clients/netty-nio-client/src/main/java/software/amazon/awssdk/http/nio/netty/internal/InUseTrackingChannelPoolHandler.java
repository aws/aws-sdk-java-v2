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

import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.IN_USE;

import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelPoolHandler;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Marks {@link Channel}s as in-use when they are leased from the pool. An in-use channel is not eligible to be closed by {@link
 * IdleConnectionReaperHandler} or {@link OldConnectionReaperHandler}.
 */
@SdkInternalApi
public final class InUseTrackingChannelPoolHandler implements ChannelPoolHandler {

    private static final InUseTrackingChannelPoolHandler INSTANCE = new InUseTrackingChannelPoolHandler();

    private InUseTrackingChannelPoolHandler() {
    }

    public static InUseTrackingChannelPoolHandler create() {
        return INSTANCE;
    }

    @Override
    public void channelCreated(Channel ch) {
        // no-op
    }

    @Override
    public void channelAcquired(Channel ch) {
        ch.attr(IN_USE).set(true);
    }

    @Override
    public void channelReleased(Channel ch) {
        ch.attr(IN_USE).set(false);
    }
}
