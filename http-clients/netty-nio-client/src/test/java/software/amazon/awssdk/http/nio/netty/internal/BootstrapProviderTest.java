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
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import io.netty.bootstrap.Bootstrap;
import io.netty.resolver.AddressResolver;
import io.netty.resolver.AddressResolverGroup;
import software.amazon.awssdk.http.nio.netty.SdkEventLoopGroup;

@RunWith(MockitoJUnitRunner.class)
public class BootstrapProviderTest {
    private final BootstrapProvider bootstrapProvider =
        new BootstrapProvider(SdkEventLoopGroup.builder().build(),
                              new NettyConfiguration(GLOBAL_HTTP_DEFAULTS),
                              new SdkChannelOptions());

    // IMPORTANT: This unit test asserts that the bootstrap provider creates bootstraps using 'unresolved
    // InetSocketAddress'. If this test is replaced or removed, perhaps due to a different implementation of
    // SocketAddress, a different test must be created that ensures that the hostname will be resolved on every
    // connection attempt and not cached between connection attempts.
    @Test
    public void createBootstrap_usesUnresolvedInetSocketAddress() {
        Bootstrap bootstrap = bootstrapProvider.createBootstrap("some-awesome-service-1234.amazonaws.com", 443);

        SocketAddress socketAddress = bootstrap.config().remoteAddress();

        assertThat(socketAddress).isInstanceOf(InetSocketAddress.class);
        InetSocketAddress inetSocketAddress = (InetSocketAddress)socketAddress;

        assertThat(inetSocketAddress.isUnresolved()).isTrue();
    }
}