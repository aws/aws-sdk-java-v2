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

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey;
import software.amazon.awssdk.http.nio.netty.internal.MockChannel;
import software.amazon.awssdk.http.nio.netty.internal.UnusedChannelExceptionHandler;

public class MultiplexedChannelRecordTest {
    private EventLoopGroup loopGroup;
    private MockChannel channel;

    @Before
    public void setup() throws Exception {
        loopGroup = new NioEventLoopGroup(4);
        channel = new MockChannel();
    }

    @After
    public void teardown() {
        loopGroup.shutdownGracefully().awaitUninterruptibly();
        channel.close();
    }

    @Test
    public void nullIdleTimeoutSeemsToDisableReaping() throws InterruptedException {
        EmbeddedChannel channel = newHttp2Channel();
        MultiplexedChannelRecord record = new MultiplexedChannelRecord(channel, 1, null);

        Promise<Channel> streamPromise = channel.eventLoop().newPromise();
        record.acquireStream(streamPromise);

        channel.runPendingTasks();

        assertThat(streamPromise.isSuccess()).isTrue();
        assertThat(channel.isOpen()).isTrue();

        record.closeAndReleaseChild(streamPromise.getNow());

        assertThat(channel.isOpen()).isTrue();

        Thread.sleep(1_000);
        channel.runPendingTasks();

        assertThat(channel.isOpen()).isTrue();
    }

    @Test
    public void recordsWithoutReservedStreamsAreClosedAfterTimeout() throws InterruptedException {
        int idleTimeoutMillis = 1000;
        EmbeddedChannel channel = newHttp2Channel();
        MultiplexedChannelRecord record = new MultiplexedChannelRecord(channel, 1, Duration.ofMillis(idleTimeoutMillis));

        Promise<Channel> streamPromise = channel.eventLoop().newPromise();
        record.acquireStream(streamPromise);

        channel.runPendingTasks();

        assertThat(streamPromise.isSuccess()).isTrue();
        assertThat(channel.isOpen()).isTrue();

        record.closeAndReleaseChild(streamPromise.getNow());

        assertThat(channel.isOpen()).isTrue();

        Thread.sleep(idleTimeoutMillis * 2);
        channel.runPendingTasks();

        assertThat(channel.isOpen()).isFalse();
    }

    @Test
    public void recordsWithReservedStreamsAreNotClosedAfterTimeout() throws InterruptedException {
        int idleTimeoutMillis = 1000;
        EmbeddedChannel channel = newHttp2Channel();
        MultiplexedChannelRecord record = new MultiplexedChannelRecord(channel, 2, Duration.ofMillis(idleTimeoutMillis));

        Promise<Channel> streamPromise = channel.eventLoop().newPromise();
        Promise<Channel> streamPromise2 = channel.eventLoop().newPromise();
        record.acquireStream(streamPromise);
        record.acquireStream(streamPromise2);

        channel.runPendingTasks();

        assertThat(streamPromise.isSuccess()).isTrue();
        assertThat(streamPromise2.isSuccess()).isTrue();
        assertThat(channel.isOpen()).isTrue();

        record.closeAndReleaseChild(streamPromise.getNow());

        assertThat(channel.isOpen()).isTrue();

        Thread.sleep(idleTimeoutMillis * 2);
        channel.runPendingTasks();

        assertThat(channel.isOpen()).isTrue();
    }

    @Test
    public void acquireRequestResetsCloseTimer() throws InterruptedException {
        int idleTimeoutMillis = 1000;
        EmbeddedChannel channel = newHttp2Channel();
        MultiplexedChannelRecord record = new MultiplexedChannelRecord(channel, 2, Duration.ofMillis(idleTimeoutMillis));

        for (int i = 0; i < 20; ++i) {
            Thread.sleep(idleTimeoutMillis / 10);
            channel.runPendingTasks();

            Promise<Channel> streamPromise = channel.eventLoop().newPromise();
            assertThat(record.acquireStream(streamPromise)).isTrue();
            channel.runPendingTasks();

            assertThat(streamPromise.isSuccess()).isTrue();
            assertThat(channel.isOpen()).isTrue();

            record.closeAndReleaseChild(streamPromise.getNow());
            channel.runPendingTasks();
        }

        assertThat(channel.isOpen()).isTrue();

        Thread.sleep(idleTimeoutMillis * 2);
        channel.runPendingTasks();

        assertThat(channel.isOpen()).isFalse();
    }

    @Test
    public void idleTimerDoesNotApplyBeforeFirstChannelIsCreated() throws InterruptedException {
        int idleTimeoutMillis = 1000;
        EmbeddedChannel channel = newHttp2Channel();
        MultiplexedChannelRecord record = new MultiplexedChannelRecord(channel, 2, Duration.ofMillis(idleTimeoutMillis));

        Thread.sleep(idleTimeoutMillis * 2);
        channel.runPendingTasks();

        assertThat(channel.isOpen()).isTrue();
    }

    @Test
    public void availableStream0_reusableShouldBeFalse() {
        loopGroup.register(channel).awaitUninterruptibly();
        Promise<Channel> channelPromise = new DefaultPromise<>(loopGroup.next());
        channelPromise.setSuccess(channel);

        MultiplexedChannelRecord record = new MultiplexedChannelRecord(channel, 0, Duration.ofSeconds(10));

        assertThat(record.acquireStream(null)).isFalse();
    }

    @Test
    public void acquireClaimedConnection_channelClosed_shouldThrowIOException() {
        loopGroup.register(channel).awaitUninterruptibly();
        Promise<Channel> channelPromise = new DefaultPromise<>(loopGroup.next());

        MultiplexedChannelRecord record = new MultiplexedChannelRecord(channel, 1, Duration.ofSeconds(10));

        record.closeChildChannels();

        record.acquireClaimedStream(channelPromise);

        assertThatThrownBy(() -> channelPromise.get()).hasCauseInstanceOf(IOException.class);
    }

    @Test
    public void closeChildChannels_shouldDeliverException() throws ExecutionException, InterruptedException {
        EmbeddedChannel channel = newHttp2Channel();
        loopGroup.register(channel).awaitUninterruptibly();
        Promise<Channel> channelPromise = new DefaultPromise<>(loopGroup.next());
        channelPromise.setSuccess(channel);

        MultiplexedChannelRecord record = new MultiplexedChannelRecord(channel, 2, Duration.ofSeconds(10));

        Promise<Channel> streamPromise = channel.eventLoop().newPromise();
        record.acquireStream(streamPromise);

        channel.runPendingTasks();
        Channel childChannel = streamPromise.get();
        VerifyExceptionHandler verifyExceptionHandler = new VerifyExceptionHandler();
        childChannel.pipeline().addFirst(verifyExceptionHandler);

        IOException ioException = new IOException("foobar");
        record.closeChildChannels(ioException);

        assertThat(childChannel.pipeline().get(UnusedChannelExceptionHandler.class)).isNotNull();

        assertThat(verifyExceptionHandler.exceptionCaught).hasStackTraceContaining("foobar")
                                                          .hasRootCauseInstanceOf(IOException.class);

        // should be closed by UnusedChannelExceptionHandler
        assertThat(childChannel.isOpen()).isFalse();
    }

    @Test
    public void closeToNewStreams_AcquireStreamShouldReturnFalse() {
        MultiplexedChannelRecord record = new MultiplexedChannelRecord(channel, 2, Duration.ofSeconds(10));
        Promise<Channel> streamPromise = channel.eventLoop().newPromise();
        assertThat(record.acquireStream(streamPromise)).isTrue();

        record.closeToNewStreams();
        assertThat(record.acquireStream(streamPromise)).isFalse();
    }

    private static final class VerifyExceptionHandler extends ChannelInboundHandlerAdapter {
        private Throwable exceptionCaught;
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            exceptionCaught = cause;
            ctx.fireExceptionCaught(cause);
        }
    }

    private EmbeddedChannel newHttp2Channel() {
        EmbeddedChannel channel = new EmbeddedChannel(Http2FrameCodecBuilder.forClient().build(),
                                                      new Http2MultiplexHandler(new NoOpHandler()));
        channel.attr(ChannelAttributeKey.PROTOCOL_FUTURE).set(CompletableFuture.completedFuture(Protocol.HTTP2));
        return channel;
    }

    private static class NoOpHandler extends ChannelInitializer<Channel> {
        @Override
        protected void initChannel(Channel ch) { }
    }
}
