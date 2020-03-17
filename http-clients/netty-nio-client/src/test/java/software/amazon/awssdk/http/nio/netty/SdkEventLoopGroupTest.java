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

package software.amazon.awssdk.http.nio.netty;

import static org.assertj.core.api.Assertions.assertThat;

import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.junit.Test;

public class SdkEventLoopGroupTest {

    @Test
    public void creatingUsingBuilder() {
        SdkEventLoopGroup sdkEventLoopGroup = SdkEventLoopGroup.builder().numberOfThreads(1).build();
        assertThat(sdkEventLoopGroup.channelFactory()).isNotNull();
        assertThat(sdkEventLoopGroup.eventLoopGroup()).isNotNull();
    }

    @Test
    public void creatingUsingStaticMethod() {
        SdkEventLoopGroup sdkEventLoopGroup = SdkEventLoopGroup.create(new NioEventLoopGroup(), NioSocketChannel::new);
        assertThat(sdkEventLoopGroup.channelFactory()).isNotNull();
        assertThat(sdkEventLoopGroup.eventLoopGroup()).isNotNull();
    }

    @Test
    public void notProvidingChannelFactory_channelFactoryResolved() {
        SdkEventLoopGroup sdkEventLoopGroup = SdkEventLoopGroup.create(new NioEventLoopGroup());

        assertThat(sdkEventLoopGroup.channelFactory()).isNotNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void notProvidingChannelFactory_unknownEventLoopGroup() {
        SdkEventLoopGroup.create(new DefaultEventLoopGroup());
    }
}
