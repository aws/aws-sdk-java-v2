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


import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.PING_TRACKER;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.ScheduledFuture;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.http.nio.netty.internal.MockChannel;

public class MultiplexedChannelRecordTest {

    private static EventLoopGroup loopGroup;
    private static Channel channel;

    @BeforeClass
    public static void setup() throws Exception {
        loopGroup = new NioEventLoopGroup(4);
        channel = new MockChannel();
    }

    @AfterClass
    public static void teardown() {
        loopGroup.shutdownGracefully().awaitUninterruptibly();
        channel.close();
    }

    @Test
    public void pingInflight_reusableShouldBeFalse() throws Exception {
        loopGroup.register(channel).awaitUninterruptibly();
        Promise<Channel> channelPromise = new DefaultPromise<>(loopGroup.next());
        channelPromise.setSuccess(channel);

        channel.attr(PING_TRACKER).set(new PingTracker(() -> Mockito.mock(ScheduledFuture.class)));
        MultiplexedChannelRecord record = new MultiplexedChannelRecord(channel, 8);

        assertThat(record.acquireStream(null)).isFalse();
    }

    @Test
    public void availableStream0_reusableShouldBeFalse() throws Exception {
        loopGroup.register(channel).awaitUninterruptibly();
        Promise<Channel> channelPromise = new DefaultPromise<>(loopGroup.next());
        channelPromise.setSuccess(channel);

        MultiplexedChannelRecord record = new MultiplexedChannelRecord(channel, 0);

        assertThat(record.acquireStream(null)).isFalse();
    }
}
