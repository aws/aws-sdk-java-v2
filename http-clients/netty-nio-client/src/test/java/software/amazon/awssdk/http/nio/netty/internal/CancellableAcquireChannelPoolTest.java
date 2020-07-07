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

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Promise;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link CancellableAcquireChannelPool}.
 */
@RunWith(MockitoJUnitRunner.class)
public class CancellableAcquireChannelPoolTest {
    private static final EventLoopGroup eventLoopGroup = new NioEventLoopGroup();

    private EventExecutor eventExecutor;

    @Mock
    private SdkChannelPool mockDelegatePool;

    private Channel channel;

    private CancellableAcquireChannelPool cancellableAcquireChannelPool;

    @Before
    public void setup() {
        channel = new NioSocketChannel();
        eventLoopGroup.register(channel);
        eventExecutor = eventLoopGroup.next();
        cancellableAcquireChannelPool = new CancellableAcquireChannelPool(eventExecutor, mockDelegatePool);
    }

    @After
    public void methodTeardown() {
        channel.close().awaitUninterruptibly();
    }

    @AfterClass
    public static void teardown() {
        eventLoopGroup.shutdownGracefully().awaitUninterruptibly();
    }

    @Test
    public void promiseCancelledBeforeAcquireComplete_closesAndReleasesChannel() throws InterruptedException {
        Promise<Channel> acquireFuture = eventExecutor.newPromise();
        acquireFuture.setFailure(new RuntimeException("Changed my mind!"));

        when(mockDelegatePool.acquire(any(Promise.class))).thenAnswer((Answer<Promise>) invocationOnMock -> {
            Promise p = invocationOnMock.getArgumentAt(0, Promise.class);
            p.setSuccess(channel);
            return p;
        });

        cancellableAcquireChannelPool.acquire(acquireFuture);

        Thread.sleep(500);
        verify(mockDelegatePool).release(eq(channel));
        assertThat(channel.closeFuture().isDone()).isTrue();
    }
}
