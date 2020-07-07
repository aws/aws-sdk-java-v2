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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Promise;
import java.util.concurrent.CompletableFuture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.utils.AttributeMap;

public class NettyRequestExecutorTest {

    private SdkChannelPool mockChannelPool;

    private EventLoopGroup eventLoopGroup;

    private NettyRequestExecutor nettyRequestExecutor;

    private RequestContext requestContext;

    @Before
    public void setup() {
        mockChannelPool = mock(SdkChannelPool.class);

        eventLoopGroup = new NioEventLoopGroup();

        requestContext = new RequestContext(mockChannelPool,
                                            eventLoopGroup,
                                            AsyncExecuteRequest.builder().build(),
                                            new NettyConfiguration(AttributeMap.empty()));
        nettyRequestExecutor = new NettyRequestExecutor(requestContext);
    }

    @After
    public void teardown() throws InterruptedException {
        eventLoopGroup.shutdownGracefully().await();
    }

    @Test
    public void cancelExecuteFuture_channelNotAcquired_failsAcquirePromise() {
        ArgumentCaptor<Promise> acquireCaptor = ArgumentCaptor.forClass(Promise.class);
        when(mockChannelPool.acquire(acquireCaptor.capture())).thenAnswer((Answer<Promise>) invocationOnMock -> {
            return invocationOnMock.getArgumentAt(0, Promise.class);
        });

        CompletableFuture<Void> executeFuture = nettyRequestExecutor.execute();

        executeFuture.cancel(true);

        assertThat(acquireCaptor.getValue().isDone()).isTrue();
        assertThat(acquireCaptor.getValue().isSuccess()).isFalse();
    }

    @Test
    public void cancelExecuteFuture_channelAcquired_submitsRunnable() {
        EventLoop mockEventLoop = mock(EventLoop.class);
        Channel mockChannel = mock(Channel.class);
        when(mockChannel.eventLoop()).thenReturn(mockEventLoop);

        when(mockChannelPool.acquire(any(Promise.class))).thenAnswer((Answer<Promise>) invocationOnMock -> {
            Promise p = invocationOnMock.getArgumentAt(0, Promise.class);
            p.setSuccess(mockChannel);
            return p;
        });

        CompletableFuture<Void> executeFuture = nettyRequestExecutor.execute();

        executeFuture.cancel(true);

        verify(mockEventLoop).submit(any(Runnable.class));
    }
}
