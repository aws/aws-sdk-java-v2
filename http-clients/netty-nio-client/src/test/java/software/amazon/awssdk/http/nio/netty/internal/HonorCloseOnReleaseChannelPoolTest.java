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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.netty.channel.Channel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.ChannelPool;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.verification.Times;

public class HonorCloseOnReleaseChannelPoolTest {
    @Test
    public void releaseDoesntCloseIfNotFlagged() throws Exception {
        ChannelPool channelPool = Mockito.mock(ChannelPool.class);

        MockChannel channel = new MockChannel();
        channel.attr(ChannelAttributeKey.CLOSE_ON_RELEASE).set(false);

        new HonorCloseOnReleaseChannelPool(channelPool).release(channel);
        channel.runAllPendingTasks();

        assertThat(channel.isOpen()).isTrue();
        Mockito.verify(channelPool, new Times(0)).release(any());
        Mockito.verify(channelPool, new Times(1)).release(any(), any());
    }

    @Test
    public void releaseClosesIfFlagged() throws Exception {
        ChannelPool channelPool = Mockito.mock(ChannelPool.class);

        MockChannel channel = new MockChannel();
        channel.attr(ChannelAttributeKey.CLOSE_ON_RELEASE).set(true);

        new HonorCloseOnReleaseChannelPool(channelPool).release(channel);
        channel.runAllPendingTasks();

        assertThat(channel.isOpen()).isFalse();
        Mockito.verify(channelPool, new Times(0)).release(any());
        Mockito.verify(channelPool, new Times(1)).release(any(), any());
    }

    @Test
    public void release_delegateReleaseFails_futureFailed() throws Exception {
        NioEventLoopGroup nioEventLoopGroup = new NioEventLoopGroup();
        try {
            ChannelPool mockDelegatePool = Mockito.mock(ChannelPool.class);
            MockChannel channel = new MockChannel();

            nioEventLoopGroup.register(channel);

            HonorCloseOnReleaseChannelPool channelPool = new HonorCloseOnReleaseChannelPool(mockDelegatePool);

            RuntimeException errorToThrow = new RuntimeException("failed!");
            when(mockDelegatePool.release(any(Channel.class), any(Promise.class))).thenThrow(errorToThrow);

            Promise<Void> promise = channel.eventLoop().newPromise();

            Future<Void> releasePromise = channelPool.release(channel, promise);
            try {
                releasePromise.get(1, TimeUnit.SECONDS);
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    throw e;
                }
                // ignore any other exception
            }
            assertThat(releasePromise.isSuccess()).isFalse();
            assertThat(releasePromise.cause()).isSameAs(errorToThrow);
        } finally {
            nioEventLoopGroup.shutdownGracefully();
        }
    }
}
