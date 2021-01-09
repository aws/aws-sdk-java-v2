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

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import java.time.Duration;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
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
        handler = new OneTimeReadTimeoutHandler(Duration.ofMillis(TIMEOUT_IN_MILLIS));
    }

    @Test
    public void channelRead_removesSelf() throws Exception {
        when(context.pipeline()).thenReturn(channelPipeline);

        handler.channelRead(context, object);

        verify(channelPipeline, times(1)).remove(eq(handler));
        verify(context, times(1)).fireChannelRead(object);
    }
}
