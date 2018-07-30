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

package software.amazon.awssdk.http.nio.netty.internal;


import static org.assertj.core.api.Java6Assertions.assertThat;

import org.junit.Test;
import software.amazon.awssdk.http.nio.netty.SdkEventLoopGroup;

public class SharedSdkEventLoopGroupTest {

    @Test
    public void referenceCountIsInitiallyZero() {
        assertThat(SharedSdkEventLoopGroup.referenceCount()).isEqualTo(0);
    }

    @Test
    public void referenceCountIsIncrementedOnGet() {
        SdkEventLoopGroup group = SharedSdkEventLoopGroup.get();
        assertThat(SharedSdkEventLoopGroup.referenceCount()).isEqualTo(1);
        group.eventLoopGroup().shutdownGracefully();
    }

    @Test
    public void referenceCountIsOnceDecrementedOnClose() {
        SdkEventLoopGroup group = SharedSdkEventLoopGroup.get();
        group.eventLoopGroup().shutdownGracefully();
        assertThat(SharedSdkEventLoopGroup.referenceCount()).isEqualTo(0);
        group.eventLoopGroup().shutdownGracefully();
        assertThat(SharedSdkEventLoopGroup.referenceCount()).isEqualTo(0);
    }

    @Test
    public void sharedEventLoopGroupIsDeallocatedWhenCountReachesZero() {
        DelegatingEventLoopGroup group1 = (DelegatingEventLoopGroup) SharedSdkEventLoopGroup.get().eventLoopGroup();
        DelegatingEventLoopGroup group2 = (DelegatingEventLoopGroup) SharedSdkEventLoopGroup.get().eventLoopGroup();
        assertThat(group1.getDelegate()).isEqualTo(group2.getDelegate());

        group1.shutdownGracefully();
        group2.shutdownGracefully();

        assertThat(group1.getDelegate().isShuttingDown()).isTrue();
    }
}
