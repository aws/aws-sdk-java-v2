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
import static org.mockito.Mockito.verify;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.PING_TRACKER;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.DefaultHttp2PingFrame;
import io.netty.util.concurrent.ScheduledFuture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.http.nio.netty.internal.MockChannel;

@RunWith(MockitoJUnitRunner.class)
public class Http2PingFrameHandlerTest {
    private Http2PingFrameHandler handler = Http2PingFrameHandler.create();

    @Mock
    private ChannelHandlerContext context;

    private MockChannel mockChannel;

    @Mock
    private ScheduledFuture<?> future;

    private PingTracker pingTracker;

    @Before
    public void setup() throws Exception {
        mockChannel = new MockChannel();
        pingTracker = new PingTracker(() -> future);
        pingTracker.start();
        Mockito.when(context.channel()).thenReturn(mockChannel);
        mockChannel.attr(PING_TRACKER).set(pingTracker);
    }

    @After
    public void tearDown() {
        mockChannel.close();
    }

    @Test
    public void pingAckReceived_shouldUpdatePingStatus() throws Exception {
        handler.channelRead(context, new DefaultHttp2PingFrame(0, true));

        verify(future).cancel(false);

        assertThat((mockChannel.attr(PING_TRACKER).get())).isNull();
    }

    @Test
    public void pingReceived_shouldForwardFrame() throws Exception {
        DefaultHttp2PingFrame pingFrame = new DefaultHttp2PingFrame(0, false);
        handler.channelRead(context, pingFrame);

        verify(context).fireChannelRead(pingFrame);
        assertThat(mockChannel.attr(PING_TRACKER).get()).isEqualTo(pingTracker);
    }

}
