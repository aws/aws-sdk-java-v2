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

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.IN_USE;

import io.netty.channel.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InUseTrackingChannelPoolListenerTest {

    Channel mockChannel;
    InUseTrackingChannelPoolListener handler;

    @BeforeEach
    void setUp() throws Exception {
        mockChannel = new MockChannel();
        handler = InUseTrackingChannelPoolListener.create();
    }

    @Test
    void channelAcquired() {
        mockChannel.attr(IN_USE).set(false);
        handler.channelAcquired(mockChannel);
        assertThat(mockChannel.attr(IN_USE).get()).isTrue();
    }

    @Test
    void channelReleased() {
        mockChannel.attr(IN_USE).set(true);
        handler.channelReleased(mockChannel);
        assertThat(mockChannel.attr(IN_USE).get()).isFalse();
    }
}