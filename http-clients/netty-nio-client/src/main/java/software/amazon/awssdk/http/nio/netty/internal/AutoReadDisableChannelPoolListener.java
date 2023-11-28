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
import io.netty.channel.ChannelHandler;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Disables auto read on in-use channels to allow upper layers to take care of flow control.
 */
@SdkInternalApi
@ChannelHandler.Sharable
public final class AutoReadDisableChannelPoolListener implements ListenerInvokingChannelPool.ChannelPoolListener {
    private static final AutoReadDisableChannelPoolListener INSTANCE = new AutoReadDisableChannelPoolListener();

    private AutoReadDisableChannelPoolListener() {
    }

    @Override
    public void channelAcquired(Channel channel) {
        channel.config().setAutoRead(false);
    }

    public static AutoReadDisableChannelPoolListener create() {
        return INSTANCE;
    }
}
