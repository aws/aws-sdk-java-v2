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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.WRITE_TIMEOUT;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.IN_USE;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.PROTOCOL_FUTURE;

import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.DefaultChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Promise;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
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

        AttributeMap attributeMap = AttributeMap.builder()
            .put(WRITE_TIMEOUT, Duration.ofSeconds(3))
            .build();

        requestContext = new RequestContext(mockChannelPool,
                                            eventLoopGroup,
                                            AsyncExecuteRequest.builder().request(SdkHttpFullRequest.builder()
                                                                                                    .method(SdkHttpMethod.GET)
                                                                                                    .host("amazonaws.com")
                                                                                                    .protocol("https")
                                                                                                    .build())
                                                               .build(),
                                            new NettyConfiguration(attributeMap));
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
    public void cancelExecuteFuture_channelAcquired_submitsRunnable() throws InterruptedException {
        EventLoop mockEventLoop = mock(EventLoop.class);
        Channel mockChannel = mock(Channel.class);
        ChannelPipeline mockPipeline = mock(ChannelPipeline.class);

        when(mockChannel.pipeline()).thenReturn(mockPipeline);
        when(mockChannel.eventLoop()).thenReturn(mockEventLoop);
        when(mockChannel.isActive()).thenReturn(true);

        Attribute<Boolean> mockInUseAttr = mock(Attribute.class);
        when(mockInUseAttr.get()).thenReturn(Boolean.TRUE);

        CompletableFuture<Protocol> protocolFuture = CompletableFuture.completedFuture(Protocol.HTTP1_1);
        Attribute<CompletableFuture<Protocol>> mockProtocolFutureAttr = mock(Attribute.class);
        when(mockProtocolFutureAttr.get()).thenReturn(protocolFuture);

        when(mockChannel.attr(any(AttributeKey.class))).thenAnswer(i -> {
            AttributeKey argumentAt = i.getArgumentAt(0, AttributeKey.class);
            if (argumentAt == IN_USE) {
                return mockInUseAttr;
            }
            if (argumentAt == PROTOCOL_FUTURE) {
                return mockProtocolFutureAttr;
            }
            return mock(Attribute.class);
        });

        when(mockChannel.writeAndFlush(any(Object.class))).thenReturn(new DefaultChannelPromise(mockChannel));

        ChannelConfig mockChannelConfig = mock(ChannelConfig.class);

        when(mockChannel.config()).thenReturn(mockChannelConfig);

        CountDownLatch submitLatch = new CountDownLatch(1);
        when(mockEventLoop.submit(any(Runnable.class))).thenAnswer(i -> {
            i.getArgumentAt(0, Runnable.class).run();
            // Need to wait until the first submit() happens which sets up the channel before cancelling the future.
            submitLatch.countDown();
            return null;
        });

        when(mockChannelPool.acquire(any(Promise.class))).thenAnswer((Answer<Promise>) invocationOnMock -> {
            Promise p = invocationOnMock.getArgumentAt(0, Promise.class);
            p.setSuccess(mockChannel);
            return p;
        });

        CountDownLatch exceptionFiredLatch = new CountDownLatch(1);
        when(mockPipeline.fireExceptionCaught(any(FutureCancelledException.class))).thenAnswer(i -> {
            exceptionFiredLatch.countDown();
            return mockPipeline;
        });

        CompletableFuture<Void> executeFuture = nettyRequestExecutor.execute();
        submitLatch.await(1, TimeUnit.SECONDS);
        executeFuture.cancel(true);
        exceptionFiredLatch.await(1, TimeUnit.SECONDS);

        verify(mockEventLoop, times(2)).submit(any(Runnable.class));
    }
}
