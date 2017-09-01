/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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


import static org.assertj.core.api.Java6Assertions.assertThat;

import io.netty.channel.EventLoopGroup;
import org.junit.Test;

public class SharedEventLoopGroupTest {

    @Test
    public void referenceCountIsInitiallyZero() {
        assertThat(SharedEventLoopGroup.referenceCount()).isEqualTo(0);
    }

    @Test
    public void referenceCountIsIncrementedOnGet() {
        EventLoopGroup group = SharedEventLoopGroup.get();
        assertThat(SharedEventLoopGroup.referenceCount()).isEqualTo(1);
        group.shutdownGracefully();
    }

    @Test
    public void referenceCountIsOnceDecrementedOnClose() {
        EventLoopGroup group = SharedEventLoopGroup.get();
        group.shutdownGracefully();
        assertThat(SharedEventLoopGroup.referenceCount()).isEqualTo(0);
        group.shutdownGracefully();
        assertThat(SharedEventLoopGroup.referenceCount()).isEqualTo(0);
    }

    @Test
    public void sharedEventLoopGroupIsDeallocatedWhenCountReachesZero() {
        DelegatingEventLoopGroup group1 = (DelegatingEventLoopGroup) SharedEventLoopGroup.get();
        DelegatingEventLoopGroup group2 = (DelegatingEventLoopGroup) SharedEventLoopGroup.get();
        assertThat(group1.getDelegate()).isEqualTo(group2.getDelegate());

        group1.shutdownGracefully();
        group2.shutdownGracefully();

        assertThat(group1.getDelegate().isShuttingDown()).isTrue();
    }
}
