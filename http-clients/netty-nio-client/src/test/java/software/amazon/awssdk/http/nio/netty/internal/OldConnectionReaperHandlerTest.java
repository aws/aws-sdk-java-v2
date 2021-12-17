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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;

import io.netty.channel.ChannelHandlerContext;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.verification.Times;

public class OldConnectionReaperHandlerTest {
    @Test
    @SuppressWarnings("unchecked")
    public void inUseChannelsAreFlaggedToBeClosed() throws Exception {
        // Given
        MockChannel channel = new MockChannel();
        channel.attr(ChannelAttributeKey.IN_USE).set(true);

        ChannelHandlerContext ctx = Mockito.mock(ChannelHandlerContext.class);
        Mockito.when(ctx.channel()).thenReturn(channel);

        // When
        new OldConnectionReaperHandler(1).handlerAdded(ctx);
        channel.runAllPendingTasks();

        // Then
        Mockito.verify(ctx, new Times(0)).close();
        Mockito.verify(ctx, new Times(0)).close(any());
        assertThat(channel.attr(ChannelAttributeKey.CLOSE_ON_RELEASE).get()).isTrue();
    }

    @Test
    public void notInUseChannelsAreClosed() throws Exception {
        // Given
        MockChannel channel = new MockChannel();
        channel.attr(ChannelAttributeKey.IN_USE).set(false);

        ChannelHandlerContext ctx = Mockito.mock(ChannelHandlerContext.class);
        Mockito.when(ctx.channel()).thenReturn(channel);

        // When
        new OldConnectionReaperHandler(1).handlerAdded(ctx);
        channel.runAllPendingTasks();

        // Then
        Mockito.verify(ctx, new Times(1)).close();
        Mockito.verify(ctx, new Times(0)).close(any());
    }
}
