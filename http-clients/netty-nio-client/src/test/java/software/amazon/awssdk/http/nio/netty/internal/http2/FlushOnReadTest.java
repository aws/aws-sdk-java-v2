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
import static org.mockito.Mockito.when;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FlushOnReadTest {

    @Mock
    private ChannelHandlerContext mockCtx;

    @Mock
    private Channel mockChannel;

    @Mock
    private Channel mockParentChannel;

    @Test
    public void read_forwardsReadBeforeParentFlush() {
        when(mockCtx.channel()).thenReturn(mockChannel);
        when(mockChannel.parent()).thenReturn(mockParentChannel);

        FlushOnReadHandler handler = FlushOnReadHandler.getInstance();

        handler.read(mockCtx);

        InOrder inOrder = Mockito.inOrder(mockCtx, mockParentChannel);

        inOrder.verify(mockCtx).read();
        inOrder.verify(mockParentChannel).flush();
    }

    @Test
    public void getInstance_returnsSingleton() {
        assertThat(FlushOnReadHandler.getInstance() == FlushOnReadHandler.getInstance()).isTrue();
    }
}
