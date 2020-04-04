
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

package software.amazon.awssdk.http.nio.netty.internal.http2;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.PING_TRACKER;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.timeout.ReadTimeoutException;
import java.io.IOException;
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
public class Http2StreamExceptionHandlerTest {

    private static final NioEventLoopGroup GROUP = new NioEventLoopGroup(1);
    private Http2StreamExceptionHandler handler;

    @Mock
    private ChannelHandlerContext context;

    @Mock
    private Channel mockParentChannel;

    private MockChannel embeddedParentChannel;

    @Mock
    private Channel streamChannel;

    private TestVerifyExceptionHandler verifyExceptionHandler;


    @Before
    public void setup() throws Exception {
        embeddedParentChannel = new MockChannel();
        verifyExceptionHandler = new TestVerifyExceptionHandler();
        embeddedParentChannel.pipeline().addLast(verifyExceptionHandler);
        when(context.channel()).thenReturn(streamChannel);
        handler = Http2StreamExceptionHandler.create();
        when(context.executor()).thenReturn(GROUP.next());
    }


    @After
    public void tearDown() {
        embeddedParentChannel.close().awaitUninterruptibly();
        Mockito.reset(streamChannel, context, mockParentChannel);
    }

    @AfterClass
    public static void teardown() {
        GROUP.shutdownGracefully().awaitUninterruptibly();
    }

    @Test
    public void timeoutException_shouldFireExceptionAndPropagateException() {
        when(streamChannel.parent()).thenReturn(embeddedParentChannel);
        handler.exceptionCaught(context, ReadTimeoutException.INSTANCE);

        assertThat(verifyExceptionHandler.exceptionCaught).isExactlyInstanceOf(Http2ConnectionTerminatingException.class);
        verify(context).fireExceptionCaught(ReadTimeoutException.INSTANCE);
    }

    @Test
    public void ioException_shouldFireExceptionAndPropagateException() {
        IOException ioException = new IOException("yolo");
        when(streamChannel.parent()).thenReturn(embeddedParentChannel);
        handler.exceptionCaught(context, ioException);

        assertThat(verifyExceptionHandler.exceptionCaught).isExactlyInstanceOf(Http2ConnectionTerminatingException.class);
        verify(context).fireExceptionCaught(ioException);
    }

    @Test
    public void otherException_shouldJustPropagateException() {
        when(streamChannel.parent()).thenReturn(embeddedParentChannel);

        RuntimeException otherException = new RuntimeException("test");
        handler.exceptionCaught(context, otherException);

        assertThat(embeddedParentChannel.attr(PING_TRACKER).get()).isNull();

        verify(context).fireExceptionCaught(otherException);
        assertThat(verifyExceptionHandler.exceptionCaught).isNull();
    }

    private static final class TestVerifyExceptionHandler extends ChannelInboundHandlerAdapter {
        private Throwable exceptionCaught;
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            exceptionCaught = cause;
        }
    }
}