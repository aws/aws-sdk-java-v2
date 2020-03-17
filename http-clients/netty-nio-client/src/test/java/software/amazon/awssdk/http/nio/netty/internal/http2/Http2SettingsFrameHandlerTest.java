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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.HTTP2_MULTIPLEXED_CHANNEL_POOL;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.MAX_CONCURRENT_STREAMS;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.PROTOCOL_FUTURE;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.pool.ChannelPool;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.codec.http2.Http2SettingsFrame;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.nio.netty.internal.MockChannel;

@RunWith(MockitoJUnitRunner.class)
public class Http2SettingsFrameHandlerTest {

    private Http2SettingsFrameHandler handler;

    private MockChannel channel;

    private AtomicReference<ChannelPool> channelPoolRef;

    @Mock
    private ChannelHandlerContext context;

    @Mock
    private ChannelPool channelPool;

    private CompletableFuture<Protocol> protocolCompletableFuture;

    private long clientMaxStreams;

    @Before
    public void setup() throws Exception {
        clientMaxStreams = 1000L;
        protocolCompletableFuture = new CompletableFuture<>();
        channel = new MockChannel();

        channel.attr(MAX_CONCURRENT_STREAMS).set(null);
        channel.attr(PROTOCOL_FUTURE).set(protocolCompletableFuture);
        channelPoolRef = new AtomicReference<>(channelPool);

        handler = new Http2SettingsFrameHandler(channel, clientMaxStreams, channelPoolRef);
    }

    @Test
    public void channelRead_useServerMaxStreams() {
        long serverMaxStreams = 50L;
        Http2SettingsFrame http2SettingsFrame = http2SettingsFrame(serverMaxStreams);
        handler.channelRead0(context, http2SettingsFrame);

        assertThat(channel.attr(MAX_CONCURRENT_STREAMS).get()).isEqualTo(serverMaxStreams);
        assertThat(protocolCompletableFuture).isDone();
        assertThat(protocolCompletableFuture.join()).isEqualTo(Protocol.HTTP2);
    }

    @Test
    public void channelRead_useClientMaxStreams() {
        long serverMaxStreams = 10000L;
        Http2SettingsFrame http2SettingsFrame = http2SettingsFrame(serverMaxStreams);
        handler.channelRead0(context, http2SettingsFrame);

        assertThat(channel.attr(MAX_CONCURRENT_STREAMS).get()).isEqualTo(clientMaxStreams);
        assertThat(protocolCompletableFuture).isDone();
        assertThat(protocolCompletableFuture.join()).isEqualTo(Protocol.HTTP2);
    }

    @Test
    public void exceptionCaught_shouldHandleErrorCloseChannel() throws Exception {
        Throwable cause = new Throwable(new RuntimeException("BOOM"));
        handler.exceptionCaught(context, cause);
        verifyChannelError(cause.getClass());
    }

    @Test
    public void channelUnregistered_ProtocolFutureNotDone_ShouldRaiseError() throws InterruptedException {
        handler.channelUnregistered(context);
        verifyChannelError(IOException.class);
    }

    private void verifyChannelError(Class<? extends Throwable> cause) throws InterruptedException {
        channel.attr(HTTP2_MULTIPLEXED_CHANNEL_POOL).set(null);

        channel.runAllPendingTasks();

        assertThat(channel.isOpen()).isFalse();
        assertThat(protocolCompletableFuture).isDone();
        assertThatThrownBy(() -> protocolCompletableFuture.join()).hasCauseExactlyInstanceOf(cause);

        Mockito.verify(channelPool).release(channel);
    }


    private Http2SettingsFrame http2SettingsFrame(long serverMaxStreams) {
        return new Http2SettingsFrame() {
            @Override
            public Http2Settings settings() {
                Http2Settings http2Settings = new Http2Settings();
                http2Settings.maxConcurrentStreams(serverMaxStreams);
                return http2Settings;
            }

            @Override
            public String name() {
                return "test";
            }
        };
    }
}
