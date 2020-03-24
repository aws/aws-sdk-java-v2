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

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import java.net.InetSocketAddress;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.nio.netty.SdkEventLoopGroup;

/**
 * The primary purpose of this Bootstrap provider is to ensure that all Bootstraps created by it are 'unresolved'
 * InetSocketAddress. This is to prevent Netty from caching the resolved address of a host and then re-using it in
 * subsequent connection attempts, and instead deferring to the JVM to handle address resolution and caching.
 */
@SdkInternalApi
public class BootstrapProvider {
    private final SdkEventLoopGroup sdkEventLoopGroup;
    private final NettyConfiguration nettyConfiguration;
    private final SdkChannelOptions sdkChannelOptions;


    BootstrapProvider(SdkEventLoopGroup sdkEventLoopGroup,
                      NettyConfiguration nettyConfiguration,
                      SdkChannelOptions sdkChannelOptions) {
        this.sdkEventLoopGroup = sdkEventLoopGroup;
        this.nettyConfiguration = nettyConfiguration;
        this.sdkChannelOptions = sdkChannelOptions;
    }

    /**
     * Creates a Bootstrap for a specific host and port with an unresolved InetSocketAddress as the remoteAddress.
     * @param host The unresolved remote hostname
     * @param port The remote port
     * @return A newly created Bootstrap using the configuration this provider was initialized with, and having an
     * unresolved remote address.
     */
    public Bootstrap createBootstrap(String host, int port) {
        Bootstrap bootstrap =
            new Bootstrap()
                .group(sdkEventLoopGroup.eventLoopGroup())
                .channelFactory(sdkEventLoopGroup.channelFactory())
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, nettyConfiguration.connectTimeoutMillis())
                .remoteAddress(InetSocketAddress.createUnresolved(host, port));
        sdkChannelOptions.channelOptions().forEach(bootstrap::option);

        return bootstrap;
    }
}
