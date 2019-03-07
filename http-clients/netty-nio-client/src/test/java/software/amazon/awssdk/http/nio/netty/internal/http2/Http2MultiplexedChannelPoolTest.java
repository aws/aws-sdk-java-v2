/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http.nio.netty.internal.http2;

import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.ChannelPool;
import io.netty.util.concurrent.Future;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link Http2MultiplexedChannelPool}.
 */
@RunWith(MockitoJUnitRunner.class)
public class Http2MultiplexedChannelPoolTest {
    private static EventLoopGroup loopGroup;
    private static EventLoop eventLoop;

    @Mock
    private ChannelPool mockDelegatePool;

    @BeforeClass
    public static void setup() {
        loopGroup = new NioEventLoopGroup(1);
        eventLoop = loopGroup.next();
    }

    @AfterClass
    public static void teardown() {
        loopGroup.shutdownGracefully().awaitUninterruptibly();
    }

    @Test
    public void acquireAfterCloseFails() throws InterruptedException {
        Http2MultiplexedChannelPool h2Pool = new Http2MultiplexedChannelPool(mockDelegatePool, loopGroup.next(), 2, Collections.emptyList());

        h2Pool.close();

        assertThat(h2Pool.acquire().await().isSuccess()).isFalse();
    }

    @Test
    public void closeReleasesAllOpenConnections_connectionFuturesSuccessful() throws InterruptedException {
        Channel mockConnection1 = mock(Channel.class);
        Channel mockConnection2 = mock(Channel.class);

        Future<Channel> c1Future = eventLoop.newSucceededFuture(mockConnection1);
        Future<Channel> c2Future = eventLoop.newSucceededFuture(mockConnection2);

        MultiplexedChannelRecord r1 = new MultiplexedChannelRecord(c1Future, mockConnection1, 1, (c, r) -> {});
        MultiplexedChannelRecord r2 = new MultiplexedChannelRecord(c2Future, mockConnection2, 1, (c, r) -> {});

        List<MultiplexedChannelRecord> connections = Stream.of(r1, r2).collect(Collectors.toList());

        when(mockDelegatePool.release(any())).thenReturn(eventLoop.newSucceededFuture(null));

        Http2MultiplexedChannelPool h2Pool = new Http2MultiplexedChannelPool(mockDelegatePool, eventLoop, 2, connections);

        h2Pool.close();

        // Allow time for close logic to run in the loop
        Thread.sleep(500);

        verify(mockDelegatePool).release(eq(mockConnection1));
        verify(mockDelegatePool).release(eq(mockConnection2));
        verify(mockDelegatePool).close();
    }

    @Test
    public void closeReleasesAllOpenConnections_hasFailedConnectionFuture() throws InterruptedException {
        Channel mockConnection1 = mock(Channel.class);

        Future<Channel> c1Future = eventLoop.newSucceededFuture(mockConnection1);
        Future<Channel> c2Future = eventLoop.newFailedFuture(new IOException("Timeout"));

        MultiplexedChannelRecord r1 = new MultiplexedChannelRecord(c1Future, mockConnection1, 1, (c, r) -> {});
        MultiplexedChannelRecord r2 = new MultiplexedChannelRecord(c2Future, null, 1, (c, r) -> {});

        List<MultiplexedChannelRecord> connections = Stream.of(r1, r2).collect(Collectors.toList());

        when(mockDelegatePool.release(eq(mockConnection1))).thenReturn(eventLoop.newSucceededFuture(null));

        Http2MultiplexedChannelPool h2Pool = new Http2MultiplexedChannelPool(mockDelegatePool, eventLoop, 2, connections);

        h2Pool.close();

        // Allow time for close logic to run in the loop
        Thread.sleep(500);

        verify(mockDelegatePool).release(eq(mockConnection1));
        verify(mockDelegatePool).close();
    }

    @Test
    public void closeReleasesAllOpenConnections_hasAllFailedFutures() throws InterruptedException {
        Future<Channel> c1Future = eventLoop.newFailedFuture(new IOException("Timeout"));
        Future<Channel> c2Future = eventLoop.newFailedFuture(new IOException("Timeout"));

        MultiplexedChannelRecord r1 = new MultiplexedChannelRecord(c1Future, null, 1, (c, r) -> {});
        MultiplexedChannelRecord r2 = new MultiplexedChannelRecord(c2Future, null, 1, (c, r) -> {});

        List<MultiplexedChannelRecord> connections = Stream.of(r1, r2).collect(Collectors.toList());

        Http2MultiplexedChannelPool h2Pool = new Http2MultiplexedChannelPool(mockDelegatePool, eventLoop, 2, connections);

        h2Pool.close();

        // Allow time for close logic to run in the loop
        Thread.sleep(500);

        verify(mockDelegatePool, times(0)).release(any(Channel.class));
        verify(mockDelegatePool).close();
    }

    @Test
    public void closeReleasesAllOpenConnections_connectionFuturesSuccessful_hasFailedRelease() throws InterruptedException {
        Channel mockConnection1 = mock(Channel.class);
        Channel mockConnection2 = mock(Channel.class);

        Future<Channel> c1Future = eventLoop.newSucceededFuture(mockConnection1);
        Future<Channel> c2Future = eventLoop.newSucceededFuture(mockConnection2);

        MultiplexedChannelRecord r1 = new MultiplexedChannelRecord(c1Future, mockConnection1, 1, (c, r) -> {});
        MultiplexedChannelRecord r2 = new MultiplexedChannelRecord(c2Future, mockConnection2, 1, (c, r) -> {});

        List<MultiplexedChannelRecord> connections = Stream.of(r1, r2).collect(Collectors.toList());

        when(mockDelegatePool.release(eq(mockConnection1))).thenReturn(eventLoop.newSucceededFuture(null));
        when(mockDelegatePool.release(eq(mockConnection2))).thenReturn(eventLoop.newFailedFuture(new RuntimeException("Couldn't release channel")));

        Http2MultiplexedChannelPool h2Pool = new Http2MultiplexedChannelPool(mockDelegatePool, eventLoop, 2, connections);

        h2Pool.close();

        // Allow time for close logic to run in the loop
        Thread.sleep(500);

        verify(mockDelegatePool).release(eq(mockConnection1));
        verify(mockDelegatePool).release(eq(mockConnection2));
        verify(mockDelegatePool).close();
    }

    @Test
    public void closeReleasesAllOpenConnections_connectionFuturesSuccessful_releasesFailed() throws InterruptedException {
        Channel mockConnection1 = mock(Channel.class);
        Channel mockConnection2 = mock(Channel.class);

        Future<Channel> c1Future = eventLoop.newSucceededFuture(mockConnection1);
        Future<Channel> c2Future = eventLoop.newSucceededFuture(mockConnection2);

        MultiplexedChannelRecord r1 = new MultiplexedChannelRecord(c1Future, mockConnection1, 1, (c, r) -> {});
        MultiplexedChannelRecord r2 = new MultiplexedChannelRecord(c2Future, mockConnection2, 1, (c, r) -> {});

        List<MultiplexedChannelRecord> connections = Stream.of(r1, r2).collect(Collectors.toList());

        when(mockDelegatePool.release(eq(mockConnection1))).thenReturn(eventLoop.newFailedFuture(new RuntimeException("Couldn't release channel")));
        when(mockDelegatePool.release(eq(mockConnection2))).thenReturn(eventLoop.newFailedFuture(new RuntimeException("Couldn't release channel")));

        Http2MultiplexedChannelPool h2Pool = new Http2MultiplexedChannelPool(mockDelegatePool, eventLoop, 2, connections);

        h2Pool.close();

        // Allow time for close logic to run in the loop
        Thread.sleep(500);

        verify(mockDelegatePool).release(eq(mockConnection1));
        verify(mockDelegatePool).release(eq(mockConnection2));
        verify(mockDelegatePool).close();
    }

    @Test
    public void close_noOpenConnections_closesDelegatePool() throws InterruptedException {
        Http2MultiplexedChannelPool h2Pool = new Http2MultiplexedChannelPool(mockDelegatePool, eventLoop, 2, Collections.emptyList());

        h2Pool.close();

        // Allow time for close logic to run in the loop
        Thread.sleep(500);

        verify(mockDelegatePool).close();
    }

    @Test
    public void closeIsIdempotent() throws InterruptedException {
        Http2MultiplexedChannelPool h2Pool = new Http2MultiplexedChannelPool(mockDelegatePool, eventLoop, 2, Collections.emptyList());

        for (int i = 0; i < 8; ++i) {
            h2Pool.close();
        }

        // Allow time for close logic to run in the loop
        Thread.sleep(500);

        verify(mockDelegatePool, times(1)).close();
    }
}
