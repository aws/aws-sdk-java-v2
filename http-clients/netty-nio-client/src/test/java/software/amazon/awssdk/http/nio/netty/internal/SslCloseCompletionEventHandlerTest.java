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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.IN_USE;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.SslCloseCompletionEvent;
import java.nio.channels.ClosedChannelException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SslCloseCompletionEventHandlerTest {

    private ChannelHandlerContext ctx;
    private MockChannel channel;

    @Before
    public void setup() throws Exception {
        ctx = mock(ChannelHandlerContext.class);
        channel = new MockChannel();
        when(ctx.channel()).thenReturn(channel);
    }

    @After
    public void teardown() {
        channel.close();
    }

    @Test
    public void userEventTriggeredUnusedChannel_ClosesChannel() {
        SslCloseCompletionEventHandler handler = SslCloseCompletionEventHandler.getInstance();
        handler.userEventTriggered(ctx, new SslCloseCompletionEvent(new ClosedChannelException()));

        verify(ctx).close();
    }

    @Test
    public void userEventTriggered_StaticVariable_ClosesChannel() {
        SslCloseCompletionEventHandler handler = SslCloseCompletionEventHandler.getInstance();
        handler.userEventTriggered(ctx, SslCloseCompletionEvent.SUCCESS);

        verify(ctx).close();
    }

    @Test
    public void userEventTriggered_channelInUse_shouldForwardEvent() {
        SslCloseCompletionEventHandler handler = SslCloseCompletionEventHandler.getInstance();
        channel.attr(IN_USE).set(true);
        SslCloseCompletionEvent event = new SslCloseCompletionEvent(new ClosedChannelException());
        handler.userEventTriggered(ctx, event);

        verify(ctx).fireUserEventTriggered(event);
    }
}
