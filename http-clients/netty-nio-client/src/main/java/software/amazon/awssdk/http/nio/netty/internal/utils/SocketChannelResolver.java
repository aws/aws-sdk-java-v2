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

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ReflectiveChannelFactory;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.nio.netty.internal.DelegatingEventLoopGroup;

@SdkInternalApi
public final class SocketChannelResolver {

    private static final Map<String, String> KNOWN_EL_GROUPS = new HashMap<>();

    static {
        KNOWN_EL_GROUPS.put("io.netty.channel.kqueue.KQueueEventLoopGroup", "io.netty.channel.kqueue.KQueueSocketChannel");
        KNOWN_EL_GROUPS.put("io.netty.channel.oio.OioEventLoopGroup", "io.netty.channel.socket.oio.OioSocketChannel");
    }

    private SocketChannelResolver() {
    }

    /**
     * Attempts to determine the {@link Channel} class that corresponds to the given
     * event loop group.
     *
     * @param eventLoopGroup the event loop group to determine the {@link Channel} for
     * @return A {@link Channel} class for the given event loop group.
     */
    public static Class<? extends Channel> resolveSocketChannelClass(EventLoopGroup eventLoopGroup) {
        if (eventLoopGroup instanceof DelegatingEventLoopGroup) {
            return resolveSocketChannelClass(((DelegatingEventLoopGroup) eventLoopGroup).getDelegate());
        }
        if (eventLoopGroup instanceof NioEventLoopGroup) {
            return NioSocketChannel.class;
        }
        if (eventLoopGroup instanceof EpollEventLoopGroup) {
            return EpollSocketChannel.class;
        }
        String socketFqcn = KNOWN_EL_GROUPS.get(eventLoopGroup.getClass().getName());
        if (socketFqcn == null) {
            throw new IllegalArgumentException("Unknown event loop group : " + eventLoopGroup.getClass());
        }
        return invokeSafely(() -> (Class<? extends Channel>) Class.forName(socketFqcn));
    }

    /**
     * Attempts to determine the {@link ChannelFactory} class that corresponds to the given
     * event loop group.
     *
     * @param eventLoopGroup the event loop group to determine the {@link ChannelFactory} for
     * @return A {@link ChannelFactory} instance for the given event loop group.
     */
    @SuppressWarnings("unchecked")
    public static ChannelFactory<? extends Channel> resolveSocketChannelFactory(EventLoopGroup eventLoopGroup) {
        if (eventLoopGroup instanceof DelegatingEventLoopGroup) {
            return resolveSocketChannelFactory(((DelegatingEventLoopGroup) eventLoopGroup).getDelegate());
        }

        if (eventLoopGroup instanceof NioEventLoopGroup) {
            return NioSocketChannel::new;
        }
        if (eventLoopGroup instanceof EpollEventLoopGroup) {
            return EpollSocketChannel::new;
        }

        String socketFqcn = KNOWN_EL_GROUPS.get(eventLoopGroup.getClass().getName());
        if (socketFqcn == null) {
            throw new IllegalArgumentException("Unknown event loop group : " + eventLoopGroup.getClass());
        }

        return invokeSafely(() -> new ReflectiveChannelFactory(Class.forName(socketFqcn)));
    }
}
