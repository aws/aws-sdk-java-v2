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

package software.amazon.awssdk.http.nio.netty.internal.utils;

import static org.assertj.core.api.Assertions.assertThat;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

public class OrderedWriteChannelHandlerContextTest {
    @Test
    public void wrapOnlyHappensOnce() {
        EmbeddedChannel channel = new EmbeddedChannel(new NoOpHandler());
        ChannelHandlerContext ctx = channel.pipeline().context(NoOpHandler.class);
        ChannelHandlerContext wrapped = OrderedWriteChannelHandlerContext.wrap(ctx);
        ChannelHandlerContext wrapped2 = OrderedWriteChannelHandlerContext.wrap(wrapped);

        assertThat(ctx).isNotSameAs(wrapped);
        assertThat(wrapped).isSameAs(wrapped2);
    }

    private static class NoOpHandler implements ChannelHandler {
        @Override
        public void handlerAdded(ChannelHandlerContext ctx) throws Exception {

        }

        @Override
        public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {

        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

        }
    }

}