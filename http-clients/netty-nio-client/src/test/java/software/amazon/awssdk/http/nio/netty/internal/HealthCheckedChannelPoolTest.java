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
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.CONNECTION_ACQUIRE_TIMEOUT;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.KEEP_ALIVE;

import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.util.Attribute;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.ScheduledFuture;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.OngoingStubbing;
import software.amazon.awssdk.utils.AttributeMap;

public class HealthCheckedChannelPoolTest {
    private EventLoopGroup eventLoopGroup = Mockito.mock(EventLoopGroup.class);
    private EventLoop eventLoop = Mockito.mock(EventLoop.class);
    private SdkChannelPool downstreamChannelPool = Mockito.mock(SdkChannelPool.class);
    private List<Channel> channels = new ArrayList<>();
    private ScheduledFuture<?> scheduledFuture = Mockito.mock(ScheduledFuture.class);
    private Attribute<Boolean> attribute = mock(Attribute.class);

    private static final NettyConfiguration NETTY_CONFIGURATION =
            new NettyConfiguration(AttributeMap.builder()
                                               .put(CONNECTION_ACQUIRE_TIMEOUT, Duration.ofMillis(10))
                                               .build());

    private HealthCheckedChannelPool channelPool = new HealthCheckedChannelPool(eventLoopGroup,
                                                                                NETTY_CONFIGURATION,
                                                                                downstreamChannelPool);

    @Before
    public void reset() {
        Mockito.reset(eventLoopGroup, eventLoop, downstreamChannelPool, scheduledFuture, attribute);
        channels.clear();

        Mockito.when(eventLoopGroup.next()).thenReturn(eventLoop);
        Mockito.when(eventLoop.newPromise())
               .thenAnswer((Answer<Promise<Object>>) i -> new DefaultPromise<>(GlobalEventExecutor.INSTANCE));
    }

    @Test
    public void acquireCanMakeJustOneCall() throws Exception {
        stubForIgnoredTimeout();
        stubAcquireHealthySequence(true);

        Future<Channel> acquire = channelPool.acquire();

        acquire.get(5, TimeUnit.SECONDS);

        assertThat(acquire.isDone()).isTrue();
        assertThat(acquire.isSuccess()).isTrue();
        assertThat(acquire.getNow()).isEqualTo(channels.get(0));

        Mockito.verify(downstreamChannelPool, Mockito.times(1)).acquire(any());
    }

    @Test
    public void acquireCanMakeManyCalls() throws Exception {
        stubForIgnoredTimeout();
        stubAcquireHealthySequence(false, false, false, false, true);

        Future<Channel> acquire = channelPool.acquire();

        acquire.get(5, TimeUnit.SECONDS);

        assertThat(acquire.isDone()).isTrue();
        assertThat(acquire.isSuccess()).isTrue();
        assertThat(acquire.getNow()).isEqualTo(channels.get(4));

        Mockito.verify(downstreamChannelPool, Mockito.times(5)).acquire(any());
    }

    @Test
    public void acquireActiveAndKeepAliveTrue_shouldAcquireOnce() throws Exception {
        stubForIgnoredTimeout();
        stubAcquireActiveAndKeepAlive();

        Future<Channel> acquire = channelPool.acquire();

        acquire.get(5, TimeUnit.SECONDS);

        assertThat(acquire.isDone()).isTrue();
        assertThat(acquire.isSuccess()).isTrue();
        assertThat(acquire.getNow()).isEqualTo(channels.get(0));

        Mockito.verify(downstreamChannelPool, Mockito.times(1)).acquire(any());
    }


    @Test
    public void acquire_firstChannelKeepAliveFalse_shouldAcquireAnother() throws Exception {
        stubForIgnoredTimeout();
        stubAcquireTwiceFirstTimeNotKeepAlive();

        Future<Channel> acquire = channelPool.acquire();

        acquire.get(5, TimeUnit.SECONDS);

        assertThat(acquire.isDone()).isTrue();
        assertThat(acquire.isSuccess()).isTrue();
        assertThat(acquire.getNow()).isEqualTo(channels.get(1));

        Mockito.verify(downstreamChannelPool, Mockito.times(2)).acquire(any());
    }

    @Test
    public void badDownstreamAcquiresCausesException() throws Exception {
        stubForIgnoredTimeout();
        stubBadDownstreamAcquire();

        Future<Channel> acquire = channelPool.acquire();

        try {
            acquire.get(5, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            // Expected
        }

        assertThat(acquire.isDone()).isTrue();
        assertThat(acquire.isSuccess()).isFalse();
        assertThat(acquire.cause()).isInstanceOf(IOException.class);

        Mockito.verify(downstreamChannelPool, Mockito.times(1)).acquire(any());
    }

    @Test
    public void slowAcquireTimesOut() throws Exception {
        stubIncompleteDownstreamAcquire();

        Mockito.when(eventLoopGroup.schedule(Mockito.any(Runnable.class), Mockito.eq(10), Mockito.eq(TimeUnit.MILLISECONDS)))
               .thenAnswer(i -> scheduledFuture);

        Future<Channel> acquire = channelPool.acquire();

        ArgumentCaptor<Runnable> timeoutTask = ArgumentCaptor.forClass(Runnable.class);
        Mockito.verify(eventLoopGroup).schedule(timeoutTask.capture(), anyLong(), any());
        timeoutTask.getValue().run();

        try {
            acquire.get(5, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            // Expected
        }

        assertThat(acquire.isDone()).isTrue();
        assertThat(acquire.isSuccess()).isFalse();
        assertThat(acquire.cause()).isInstanceOf(TimeoutException.class);

        Mockito.verify(downstreamChannelPool, Mockito.times(1)).acquire(any());
    }

    @Test
    public void releaseHealthyDoesNotClose() {
        Channel channel = Mockito.mock(Channel.class);
        Mockito.when(channel.isActive()).thenReturn(true);
        stubKeepAliveAttribute(channel, null);

        channelPool.release(channel);

        Mockito.verify(channel, never()).close();
        Mockito.verify(downstreamChannelPool, times(1)).release(channel);
    }

    @Test
    public void releaseHealthyCloses() {
        Channel channel = Mockito.mock(Channel.class);
        Mockito.when(channel.isActive()).thenReturn(false);
        stubKeepAliveAttribute(channel, null);
        channelPool.release(channel);

        Mockito.verify(channel, times(1)).close();
        Mockito.verify(downstreamChannelPool, times(1)).release(channel);
    }

    public void stubAcquireHealthySequence(Boolean... acquireHealthySequence) {
        OngoingStubbing<Future<Channel>> stubbing = Mockito.when(downstreamChannelPool.acquire(any()));
        for (boolean shouldAcquireBeHealthy : acquireHealthySequence) {
            stubbing = stubbing.thenAnswer(invocation -> {
                Promise<Channel> promise = invocation.getArgumentAt(0, Promise.class);
                Channel channel = Mockito.mock(Channel.class);
                Mockito.when(channel.isActive()).thenReturn(shouldAcquireBeHealthy);
                stubKeepAliveAttribute(channel, null);
                channels.add(channel);
                promise.setSuccess(channel);
                return promise;
            });
        }
    }

    private void stubAcquireActiveAndKeepAlive() {
        OngoingStubbing<Future<Channel>> stubbing = Mockito.when(downstreamChannelPool.acquire(any()));
        stubbing = stubbing.thenAnswer(invocation -> {
            Promise<Channel> promise = invocation.getArgumentAt(0, Promise.class);
            Channel channel = Mockito.mock(Channel.class);
            Mockito.when(channel.isActive()).thenReturn(true);

            stubKeepAliveAttribute(channel, true);

            channels.add(channel);
            promise.setSuccess(channel);
            return promise;
        });
    }

    private void stubKeepAliveAttribute(Channel channel, Boolean isKeepAlive) {
        Mockito.when(channel.attr(KEEP_ALIVE)).thenReturn(attribute);
        when(attribute.get()).thenReturn(isKeepAlive);
    }

    public void stubBadDownstreamAcquire() {
        Mockito.when(downstreamChannelPool.acquire(any())).thenAnswer(invocation -> {
            Promise<Channel> promise = invocation.getArgumentAt(0, Promise.class);
            promise.setFailure(new IOException());
            return promise;
        });
    }

    public void stubIncompleteDownstreamAcquire() {
        Mockito.when(downstreamChannelPool.acquire(any())).thenAnswer(invocation -> invocation.getArgumentAt(0, Promise.class));
    }

    public void stubForIgnoredTimeout() {
        Mockito.when(eventLoopGroup.schedule(any(Runnable.class), anyLong(), any()))
               .thenAnswer(i -> scheduledFuture);
    }

    private void stubAcquireTwiceFirstTimeNotKeepAlive() {
        OngoingStubbing<Future<Channel>> stubbing = Mockito.when(downstreamChannelPool.acquire(any()));
        stubbing = stubbing.thenAnswer(invocation -> {
            Promise<Channel> promise = invocation.getArgumentAt(0, Promise.class);
            Channel channel = Mockito.mock(Channel.class);
            stubKeepAliveAttribute(channel, false);
            Mockito.when(channel.isActive()).thenReturn(true);
            channels.add(channel);
            promise.setSuccess(channel);
            return promise;
        });

        stubbing.thenAnswer(invocation -> {
            Promise<Channel> promise = invocation.getArgumentAt(0, Promise.class);
            Channel channel = Mockito.mock(Channel.class);
            Mockito.when(channel.isActive()).thenReturn(true);
            channels.add(channel);
            promise.setSuccess(channel);
            stubKeepAliveAttribute(channel, true);
            return promise;
        });
    }
}