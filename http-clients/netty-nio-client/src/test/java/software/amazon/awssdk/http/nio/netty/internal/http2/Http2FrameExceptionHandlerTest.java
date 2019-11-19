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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.PING_TRACKER;
import static software.amazon.awssdk.http.nio.netty.internal.NettyConfiguration.HTTP2_CONNECTION_PING_TIMEOUT_SECONDS;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.DefaultChannelPromise;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http2.DefaultHttp2PingFrame;
import io.netty.handler.codec.http2.Http2PingFrame;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.util.Attribute;
import io.netty.util.concurrent.ScheduledFuture;
import java.io.IOException;
import java.util.Queue;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.http.nio.netty.internal.MockChannel;

@RunWith(MockitoJUnitRunner.class)
public class Http2FrameExceptionHandlerTest {

    private static final NioEventLoopGroup GROUP = new NioEventLoopGroup(1);
    private Http2FrameExceptionHandler handler;

    @Mock
    private ChannelHandlerContext context;

    @Mock
    private Channel mockParentChannel;

    private MockChannel embeddedParentChannel;

    @Mock
    private Channel streamChannel;

    @Mock
    private ChannelPipeline mockParentChannelPipeline;

    @Mock
    private Attribute<PingTracker> attribute;

    @Mock
    private Attribute<MultiplexedChannelRecord> recordAttribute;

    @Mock
    private ScheduledFuture<?> future;

    @Before
    public void setup() throws Exception {
        embeddedParentChannel = new MockChannel();
        when(context.channel()).thenReturn(streamChannel);
        handler = Http2FrameExceptionHandler.create();
        when(context.executor()).thenReturn(GROUP.next());
    }

    @After
    public void tearDown() {
        embeddedParentChannel.close().awaitUninterruptibly();
        Mockito.reset(recordAttribute, attribute, streamChannel, context, mockParentChannel);
    }

    @AfterClass
    public static void teardown() {
        GROUP.shutdownGracefully().awaitUninterruptibly();
    }

    @Test
    public void timeoutException_shouldSendPingFrameAndPropagateException() throws InterruptedException {
        when(streamChannel.parent()).thenReturn(embeddedParentChannel);
        handler.exceptionCaught(context, ReadTimeoutException.INSTANCE);

        assertThat(embeddedParentChannel.attr(PING_TRACKER).get()).isNotNull();

        Queue<Object> objects = embeddedParentChannel.outboundMessages();

        assertThat(objects.size()).isEqualTo(1);
        assertThat(objects.poll()).isInstanceOf(DefaultHttp2PingFrame.class);

        verify(context).fireExceptionCaught(ReadTimeoutException.INSTANCE);
    }

    @Test
    public void timeoutException_pingTimedout_shouldCloseConnectionAndChildChannels() throws InterruptedException {
        stubMockParentChannel();

        DefaultChannelPromise writePromise = new DefaultChannelPromise(mockParentChannel, GROUP.next());
        writePromise.setSuccess();

        when(mockParentChannel.writeAndFlush(any(Http2PingFrame.class))).thenReturn(writePromise);

        handler.exceptionCaught(context, ReadTimeoutException.INSTANCE);

        // Wait for the ping to time out
        Thread.sleep(HTTP2_CONNECTION_PING_TIMEOUT_SECONDS * 1000 + 100);

        verify(mockParentChannelPipeline).fireExceptionCaught(Http2FrameExceptionHandler.PingFailedException.PING_NOT_ACK_INSTANCE);
        verify(context).fireExceptionCaught(ReadTimeoutException.INSTANCE);
    }

    @Test
    public void timeoutException_pingFailed_shouldCloseConnectionAndChildChannels() throws InterruptedException {
        stubMockParentChannel();

        DefaultChannelPromise writePromise = new DefaultChannelPromise(mockParentChannel, GROUP.next());
        writePromise.setFailure(new IOException("yolo"));

        when(mockParentChannel.writeAndFlush(any(Http2PingFrame.class))).thenReturn(writePromise);

        handler.exceptionCaught(context, ReadTimeoutException.INSTANCE);

        // Wait for the listener execution
        Thread.sleep(1000);

        verify(mockParentChannelPipeline).fireExceptionCaught(Http2FrameExceptionHandler.PingFailedException.PING_WRITE_FAILED_INSTANCE);
        verify(context).fireExceptionCaught(ReadTimeoutException.INSTANCE);
    }

    @Test
    public void otherException_shouldJustPropagateException() {
        when(streamChannel.parent()).thenReturn(embeddedParentChannel);

        RuntimeException otherException = new RuntimeException("test");
        handler.exceptionCaught(context, otherException);

        assertThat(embeddedParentChannel.attr(PING_TRACKER).get()).isNull();

        Queue<Object> objects = embeddedParentChannel.outboundMessages();

        assertThat(objects.size()).isEqualTo(0);

        verify(context).fireExceptionCaught(otherException);
    }

    @Test
    public void timeoutException_pingAlreadyInflight_shouldJustPropagateException() {
        embeddedParentChannel.attr(PING_TRACKER).set(new PingTracker(() -> future));
        when(streamChannel.parent()).thenReturn(embeddedParentChannel);
        handler.exceptionCaught(context, ReadTimeoutException.INSTANCE);

        Queue<Object> objects = embeddedParentChannel.outboundMessages();

        assertThat(objects.size()).isEqualTo(0);

        verify(context).fireExceptionCaught(ReadTimeoutException.INSTANCE);
    }

    private void stubMockParentChannel() {
        when(mockParentChannel.attr(PING_TRACKER)).thenReturn(attribute);
        when(mockParentChannel.eventLoop()).thenReturn(GROUP.next());
        when(mockParentChannel.pipeline()).thenReturn(mockParentChannelPipeline);
        when(streamChannel.parent()).thenReturn(mockParentChannel);
    }
}
