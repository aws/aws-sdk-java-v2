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

/**
 * Enables auto read on idle channels so that any data that a service sends while it's idling can be handled.
 */
public class AutoReadEnableChannelPoolListener implements ListenerInvokingChannelPool.ChannelPoolListener {
    private static final AutoReadEnableChannelPoolListener INSTANCE = new AutoReadEnableChannelPoolListener();

    @Override
    public void channelReleased(Channel channel) {
        channel.config().setAutoRead(true);
    }

    public static AutoReadEnableChannelPoolListener create() {
        return INSTANCE;
    }
}
