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
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.socket.oio.OioDatagramChannel;
import io.netty.channel.socket.oio.OioSocketChannel;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

public class SdkEventLoopGroupTest {

    @Test
    public void creatingUsingBuilder() {
        SdkEventLoopGroup sdkEventLoopGroup = SdkEventLoopGroup.builder().numberOfThreads(1).build();
        assertThat(sdkEventLoopGroup.channelFactory()).isNotNull();
        assertThat(sdkEventLoopGroup.datagramChannelFactory()).isNotNull();
        assertThat(sdkEventLoopGroup.eventLoopGroup()).isNotNull();
    }

    @Test
    public void creatingUsingStaticMethod_A() {
        SdkEventLoopGroup sdkEventLoopGroup = SdkEventLoopGroup.create(new NioEventLoopGroup(), NioSocketChannel::new);
        assertThat(sdkEventLoopGroup.channelFactory()).isNotNull();
        assertThat(sdkEventLoopGroup.datagramChannelFactory().newChannel()).isInstanceOf(NioDatagramChannel.class);
        assertThat(sdkEventLoopGroup.eventLoopGroup()).isNotNull();
    }

    @Test
    public void creatingUsingStaticMethod_B() {
        SdkEventLoopGroup sdkEventLoopGroup = SdkEventLoopGroup.create(new OioEventLoopGroup(), OioSocketChannel::new);
        assertThat(sdkEventLoopGroup.channelFactory()).isNotNull();
        assertThat(sdkEventLoopGroup.datagramChannelFactory()).isNotNull();
        assertThat(sdkEventLoopGroup.datagramChannelFactory().newChannel()).isInstanceOf(OioDatagramChannel.class);
        assertThat(sdkEventLoopGroup.eventLoopGroup()).isNotNull();
    }

    @Test
    public void notProvidingChannelFactory_channelFactoryResolved() {
        SdkEventLoopGroup sdkEventLoopGroup = SdkEventLoopGroup.create(new NioEventLoopGroup());

        assertThat(sdkEventLoopGroup.channelFactory()).isNotNull();
        assertThat(sdkEventLoopGroup.datagramChannelFactory().newChannel()).isInstanceOf(NioDatagramChannel.class);
    }

    @Test
    public void multiThreadIoEventLoopGroup_nioIoHandler_withoutChannelFactory() {
        SdkEventLoopGroup sdkEventLoopGroup =
            SdkEventLoopGroup.create(new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory()));

        assertThat(sdkEventLoopGroup.channelFactory().newChannel()).isInstanceOf(NioSocketChannel.class);
        assertThat(sdkEventLoopGroup.datagramChannelFactory().newChannel()).isInstanceOf(NioDatagramChannel.class);
    }

    @Test
    public void multiThreadIoEventLoopGroupWithNonNioIoHandler_withoutChannelFactory_createsNioChannels() {
        Assumptions.assumeTrue(Epoll.isAvailable());

        SdkEventLoopGroup sdkEventLoopGroup =
            SdkEventLoopGroup.create(new MultiThreadIoEventLoopGroup(EpollIoHandler.newFactory()));

        assertThat(sdkEventLoopGroup.channelFactory().newChannel()).isInstanceOf(NioSocketChannel.class);
        assertThat(sdkEventLoopGroup.datagramChannelFactory().newChannel()).isInstanceOf(NioDatagramChannel.class);
    }

    @Test
    public void multiThreadIoEventLoopGroupWithNonNioIoHandler_withChannelsFactories_properlySetsChannelTypes() {
        Assumptions.assumeTrue(Epoll.isAvailable());

        SdkEventLoopGroup sdkEventLoopGroup =
            SdkEventLoopGroup.create(new MultiThreadIoEventLoopGroup(EpollIoHandler.newFactory()),
                                     EpollSocketChannel::new,
                                     EpollDatagramChannel::new);
        assertThat(sdkEventLoopGroup.channelFactory()).isNotNull();
        assertThat(sdkEventLoopGroup.channelFactory().newChannel()).isInstanceOf(EpollSocketChannel.class);
        assertThat(sdkEventLoopGroup.datagramChannelFactory().newChannel()).isInstanceOf(EpollDatagramChannel.class);
        assertThat(sdkEventLoopGroup.eventLoopGroup()).isNotNull();
    }

    @Test
    public void notProvidingChannelFactory_unknownEventLoopGroup() {
        assertThrows(IllegalArgumentException.class, () -> SdkEventLoopGroup.create(new DefaultEventLoopGroup()));
    }
}
