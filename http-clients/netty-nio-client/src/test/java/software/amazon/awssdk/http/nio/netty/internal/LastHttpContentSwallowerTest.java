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

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.LastHttpContent;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link LastHttpContentSwallower}.
 */
public class LastHttpContentSwallowerTest {
    private final LastHttpContentSwallower lastHttpContentSwallower = LastHttpContentSwallower.getInstance();
    private ChannelHandlerContext mockCtx;
    private ChannelPipeline mockPipeline;

    @Before
    public void setUp() {
        mockCtx = mock(ChannelHandlerContext.class);
        mockPipeline = mock(ChannelPipeline.class);
        when(mockCtx.pipeline()).thenReturn(mockPipeline);
    }

    @Test
    public void testOtherHttpObjectRead_removesSelfFromPipeline() {
        HttpObject contentObject = mock(HttpContent.class);
        lastHttpContentSwallower.channelRead0(mockCtx, contentObject);
        verify(mockPipeline).remove(eq(lastHttpContentSwallower));
    }

    @Test
    public void testLastHttpContentRead_removesSelfFromPipeline() {
        LastHttpContent lastContent = mock(LastHttpContent.class);
        lastHttpContentSwallower.channelRead0(mockCtx, lastContent);
        verify(mockPipeline).remove(eq(lastHttpContentSwallower));
    }

    @Test
    public void testLastHttpContentRead_swallowsObject() {
        LastHttpContent lastContent = mock(LastHttpContent.class);
        lastHttpContentSwallower.channelRead0(mockCtx, lastContent);
        verify(mockCtx, times(0)).fireChannelRead(eq(lastContent));
    }

    @Test
    public void testOtherHttpObjectRead_doesNotSwallowObject() {
        HttpContent content = mock(HttpContent.class);
        lastHttpContentSwallower.channelRead0(mockCtx, content);
        verify(mockCtx).fireChannelRead(eq(content));
    }

    @Test
    public void testCallsReadAfterSwallowingContent() {
        LastHttpContent lastContent = mock(LastHttpContent.class);
        lastHttpContentSwallower.channelRead0(mockCtx, lastContent);
        verify(mockCtx).read();
    }
}
