/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.SslCloseCompletionEvent;
import java.nio.channels.ClosedChannelException;
import org.junit.Before;
import org.junit.Test;

public class SslCloseCompletionEventHandlerTest {

    private ChannelHandlerContext ctx;

    @Before
    public void setup() {
        ctx = mock(ChannelHandlerContext.class);
    }

    @Test
    public void userEventTriggered_ClosesChannel() {
        SslCloseCompletionEventHandler handler = new SslCloseCompletionEventHandler();
        handler.userEventTriggered(ctx, new SslCloseCompletionEvent(new ClosedChannelException()));

        verify(ctx).close();
    }

    @Test
    public void userEventTriggered_StaticVariable_ClosesChannel() {
        SslCloseCompletionEventHandler handler = new SslCloseCompletionEventHandler();
        handler.userEventTriggered(ctx, SslCloseCompletionEvent.SUCCESS);

        verify(ctx).close();
    }
}
