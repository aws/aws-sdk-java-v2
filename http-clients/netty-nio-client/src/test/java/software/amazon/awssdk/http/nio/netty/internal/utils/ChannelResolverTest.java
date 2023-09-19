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

package software.amazon.awssdk.http.nio.netty.internal.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.http.nio.netty.internal.utils.ChannelResolver.resolveDatagramChannelFactory;
import static software.amazon.awssdk.http.nio.netty.internal.utils.ChannelResolver.resolveSocketChannelFactory;

import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.socket.oio.OioDatagramChannel;
import io.netty.channel.socket.oio.OioSocketChannel;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.nio.netty.internal.DelegatingEventLoopGroup;

public class ChannelResolverTest {

    @Test
    public void canDetectFactoryForStandardNioEventLoopGroup() {
        assertThat(resolveSocketChannelFactory(new NioEventLoopGroup()).newChannel()).isInstanceOf(NioSocketChannel.class);
        assertThat(resolveDatagramChannelFactory(new NioEventLoopGroup()).newChannel()).isInstanceOf(NioDatagramChannel.class);
    }

    @Test
    public void canDetectEpollEventLoopGroupFactory() {
        Assumptions.assumeTrue(Epoll.isAvailable());
        assertThat(resolveSocketChannelFactory(new EpollEventLoopGroup()).newChannel()).isInstanceOf(EpollSocketChannel.class);
        assertThat(resolveDatagramChannelFactory(new EpollEventLoopGroup()).newChannel()).isInstanceOf(EpollDatagramChannel.class);
    }

    @Test
    public void worksWithDelegateEventLoopGroupsFactory() {
        assertThat(resolveSocketChannelFactory(new DelegatingEventLoopGroup(new NioEventLoopGroup()) {}).newChannel()).isInstanceOf(NioSocketChannel.class);
        assertThat(resolveDatagramChannelFactory(new DelegatingEventLoopGroup(new NioEventLoopGroup()) {}).newChannel()).isInstanceOf(NioDatagramChannel.class);
    }

    @Test
    public void worksWithOioEventLoopGroupFactory() {
        assertThat(resolveSocketChannelFactory(new OioEventLoopGroup()).newChannel()).isInstanceOf(OioSocketChannel.class);
        assertThat(resolveDatagramChannelFactory(new OioEventLoopGroup()).newChannel()).isInstanceOf(OioDatagramChannel.class);
    }
}
