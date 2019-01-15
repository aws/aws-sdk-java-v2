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

package software.amazon.awssdk.http.nio.netty.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.util.concurrent.TimeUnit;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OneTimeReadTimeoutHandlerTest {

    private static final long TIMEOUT_IN_MILLIS = 1000;
    private static OneTimeReadTimeoutHandler handler;

    @Mock
    private ChannelHandlerContext context;

    @Mock
    private ChannelPipeline channelPipeline;

    @Mock
    private Object object;

    @BeforeClass
    public static void setup() {
        handler = new OneTimeReadTimeoutHandler(TIMEOUT_IN_MILLIS, TimeUnit.MILLISECONDS);
    }

    @Test
    public void channelRead_shouldAddReadTimeoutHandlerBeforeRead() {
        ArgumentCaptor<ReadTimeoutHandler> argumentCaptor = ArgumentCaptor.forClass(ReadTimeoutHandler.class);
        ArgumentCaptor<String> handlerNameCaptor = ArgumentCaptor.forClass(String.class);

        when(context.pipeline()).thenReturn(channelPipeline);

        handler.channelRead0(context, object);

        verify(channelPipeline, times(1)).addFirst(handlerNameCaptor.capture(),
                                                   argumentCaptor.capture());

        verify(context, times(1)).fireChannelRead(object);

        ReadTimeoutHandler readTimeoutHandler = argumentCaptor.getValue();
        assertThat(readTimeoutHandler.getReaderIdleTimeInMillis()).isEqualTo(TIMEOUT_IN_MILLIS);

        verify(channelPipeline, times(1)).remove(handlerNameCaptor.getValue());

        verify(channelPipeline, times(1)).remove(handler);

    }
}
