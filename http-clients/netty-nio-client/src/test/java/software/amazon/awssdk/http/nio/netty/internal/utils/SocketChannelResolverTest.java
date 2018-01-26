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

package software.amazon.awssdk.http.nio.netty.internal.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;
import static software.amazon.awssdk.http.nio.netty.internal.utils.SocketChannelResolver.resolveSocketChannelClass;

import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.socket.oio.OioSocketChannel;
import org.junit.Test;
import software.amazon.awssdk.http.nio.netty.internal.DelegatingEventLoopGroup;

public class SocketChannelResolverTest {

    @Test
    public void canDetectForStandardNioEventLoopGroup() {
        assertThat(resolveSocketChannelClass(new NioEventLoopGroup())).isEqualTo(NioSocketChannel.class);
    }

    @Test
    public void canDetectEpollEventLoopGroup() {
        assumeTrue(Epoll.isAvailable());
        assertThat(resolveSocketChannelClass(new EpollEventLoopGroup())).isEqualTo(EpollSocketChannel.class);
    }

    @Test
    public void worksWithDelegateEventLoopGroups() {
        assertThat(resolveSocketChannelClass(new DelegatingEventLoopGroup(new NioEventLoopGroup()) {})).isEqualTo(NioSocketChannel.class);
    }

    @Test
    public void worksWithOioEventLoopGroup() {
        assertThat(resolveSocketChannelClass(new OioEventLoopGroup())).isEqualTo(OioSocketChannel.class);
    }
}