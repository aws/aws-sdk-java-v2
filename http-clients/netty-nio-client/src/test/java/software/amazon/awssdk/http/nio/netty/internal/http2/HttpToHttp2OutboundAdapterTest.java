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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelPromise;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http2.HttpConversionUtil;
import java.util.List;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HttpToHttp2OutboundAdapterTest {
    private static final NioEventLoopGroup EVENT_LOOP_GROUP = new NioEventLoopGroup(1);

    @Mock
    public ChannelHandlerContext ctx;

    @Mock
    public Channel channel;

    @AfterClass
    public static void classTeardown() {
        EVENT_LOOP_GROUP.shutdownGracefully();
    }

    @Test
    public void aggregatesWritePromises() {
        when(ctx.executor()).thenReturn(EVENT_LOOP_GROUP.next());
        when(ctx.channel()).thenReturn(channel);

        HttpToHttp2OutboundAdapter adapter = new HttpToHttp2OutboundAdapter();
        ChannelPromise writePromise = new DefaultChannelPromise(channel, EVENT_LOOP_GROUP.next());

        writeRequest(adapter, writePromise);

        ArgumentCaptor<ChannelPromise> writePromiseCaptor = ArgumentCaptor.forClass(ChannelPromise.class);
        verify(ctx, atLeastOnce()).write(any(Object.class), writePromiseCaptor.capture());

        List<ChannelPromise> writePromises = writePromiseCaptor.getAllValues();

        assertThat(writePromise.isDone()).isFalse();

        writePromises.forEach(ChannelPromise::setSuccess);

        assertThat(writePromise.isDone()).isTrue();
    }

    private void writeRequest(HttpToHttp2OutboundAdapter adapter, ChannelPromise promise) {
        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.PUT, "/", Unpooled.wrappedBuffer(new byte[16]));
        request.headers().add(HttpConversionUtil.ExtensionHeaderNames.SCHEME.text(), "http");
        adapter.write(ctx, request, promise);
    }
}
