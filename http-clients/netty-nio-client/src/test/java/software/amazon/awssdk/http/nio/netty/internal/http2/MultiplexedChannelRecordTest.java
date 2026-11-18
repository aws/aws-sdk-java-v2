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
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey;
import software.amazon.awssdk.http.nio.netty.internal.MockChannel;
import software.amazon.awssdk.http.nio.netty.internal.UnusedChannelExceptionHandler;

public class MultiplexedChannelRecordTest {
    private EventLoopGroup loopGroup;
    private MockChannel channel;

    @BeforeEach
    public void setup() throws Exception {
        loopGroup = new NioEventLoopGroup(4);
        channel = new MockChannel();
    }

    @AfterEach
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

        record.closeChildChannels(new ClosedChannelException());

        record.acquireClaimedStream(channelPromise);

        assertThatThrownBy(() -> channelPromise.get()).hasCauseInstanceOf(IOException.class);
    }

    @Test
    public void closeChildChannels_shouldDeliverException() throws Exception {
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

    @ParameterizedTest(name = "{0} with {1} open streams")
    @CsvSource({"CLOSE_TO_NEW_STREAMS, 0", "CLOSE_TO_NEW_STREAMS, 2", "GO_AWAY, 0", "GO_AWAY, 2"})
    public void retire_whenStreamsDrain_shouldCloseConnectionAfterLastStream(Retirement retirement, int openStreams) {
        EmbeddedChannel channel = newHttp2Channel();
        MultiplexedChannelRecord record = new MultiplexedChannelRecord(channel, 2, null);
        record.closeAndReleaseChild(acquireStream(channel, record));
        channel.runPendingTasks();
        List<Channel> streams = new ArrayList<>();
        for (int i = 0; i < openStreams; i++) {
            streams.add(acquireStream(channel, record));
        }

        retirement.retire(record);
        channel.runPendingTasks();

        for (Channel stream : streams) {
            assertThat(channel.isOpen()).as("connection open while a stream remains").isTrue();
            record.closeAndReleaseChild(stream);
            channel.runPendingTasks();
        }
        assertThat(channel.isOpen()).as("connection open after its last stream ended").isFalse();
    }

    @Test
    public void acquireStream_whenRetiredAfterClaimBeforeStreamOpens_shouldFailWithIoErrorReasonAndCloseConnection() {
        EmbeddedChannel channel = newHttp2Channel();
        MultiplexedChannelRecord record = new MultiplexedChannelRecord(channel, 1, null);
        RetireOnFirstListenerPromise streamPromise = new RetireOnFirstListenerPromise(channel, record);

        assertThat(record.acquireStream(streamPromise)).isTrue();

        assertThat(streamPromise.connectionOpenAfterDrainedCheck).as("connection open while the claim is outstanding").isTrue();
        assertThat(streamPromise.cause()).isInstanceOf(IOException.class)
                                         .hasMessageContaining("I/O error on another stream")
                                         .hasMessageNotContaining("GOAWAY");
        assertThat(channel.isOpen()).as("connection open after the failed claim returned").isFalse();
    }

    @Test
    public void acquireClaimedStream_whenTwoGoAwaysReceived_shouldFailWithLatestLastStreamId() {
        EmbeddedChannel channel = newHttp2Channel();
        MultiplexedChannelRecord record = new MultiplexedChannelRecord(channel, 2, null);
        acquireStream(channel, record);
        record.handleGoAway(Integer.MAX_VALUE, new GoAwayException(0, "test"));
        record.handleGoAway(3, new GoAwayException(0, "test"));
        channel.runPendingTasks();

        Promise<Channel> streamPromise = channel.eventLoop().newPromise();
        record.acquireClaimedStream(streamPromise);

        assertThat(streamPromise.cause()).isInstanceOf(IOException.class)
                                         .hasMessageContaining("received GOAWAY with Last Stream ID 3.");
    }

    @Test
    public void closeIfIdle_retiredRecordWithNoStreams_shouldCloseConnection() {
        EmbeddedChannel channel = newHttp2Channel();
        MultiplexedChannelRecord record = new MultiplexedChannelRecord(channel, 1, Duration.ofMillis(1000));
        record.closeAndReleaseChild(acquireStream(channel, record));
        channel.runPendingTasks();
        // Make the 1 second idle task due.
        channel.advanceTimeBy(1, TimeUnit.SECONDS);

        record.closeToNewStreams();
        // Run only the due idle task, so it runs before the drained check that the retire queued.
        channel.runScheduledPendingTasks();

        assertThat(channel.isOpen()).as("connection open after the idle task ran").isFalse();
    }

    @Test
    public void retire_whenRepeatedAfterLastStream_shouldInvokeCloseCallbackOnce() {
        EmbeddedChannel channel = newHttp2Channel();
        List<Channel> closedConnections = new ArrayList<>();
        MultiplexedChannelRecord record = new MultiplexedChannelRecord(channel, 2, null, closedConnections::add);
        Channel stream = acquireStream(channel, record);
        record.closeToNewStreams();

        record.closeAndReleaseChild(stream);
        record.closeToNewStreams();
        record.handleGoAway(3, new GoAwayException(0, "test"));
        channel.runPendingTasks();

        assertThat(closedConnections).containsExactly(channel);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Retirement.class)
    public void getMetrics_whenRetiredWithOpenStream_shouldReportNoAvailableStreams(Retirement retirement) throws Exception {
        EmbeddedChannel channel = newHttp2Channel();
        MultiplexedChannelRecord record = new MultiplexedChannelRecord(channel, 2, null);
        acquireStream(channel, record);
        assertThat(record.getMetrics().get(5, TimeUnit.SECONDS).getAvailableStreams()).as("before retire").isEqualTo(1);

        retirement.retire(record);
        channel.runPendingTasks();

        assertThat(channel.isOpen()).as("connection open while a stream remains").isTrue();
        assertThat(record.getMetrics().get(5, TimeUnit.SECONDS).getAvailableStreams()).as("after retire").isZero();
    }

    private static Channel acquireStream(EmbeddedChannel channel, MultiplexedChannelRecord record) {
        Promise<Channel> streamPromise = channel.eventLoop().newPromise();
        assertThat(record.acquireStream(streamPromise)).isTrue();
        channel.runPendingTasks();
        assertThat(streamPromise.isSuccess()).isTrue();
        return streamPromise.getNow();
    }

    private enum Retirement {
        CLOSE_TO_NEW_STREAMS(MultiplexedChannelRecord::closeToNewStreams),
        // The test streams have not written HEADERS, so their stream IDs are below 3 and the GOAWAY leaves them open.
        GO_AWAY(record -> record.handleGoAway(3, new GoAwayException(0, "test")));

        private final Consumer<MultiplexedChannelRecord> action;

        Retirement(Consumer<MultiplexedChannelRecord> action) {
            this.action = action;
        }

        void retire(MultiplexedChannelRecord record) {
            action.accept(record);
        }
    }

    /**
     * {@link MultiplexedChannelRecord#acquireStream} claims a stream, adds a listener to this promise, and then runs the stream
     * open task. On the first listener, this promise retires the record and runs the drained check that the retire queued, so
     * both run after the claim and before the stream open task. The claim is still taken, so the drained check does not close
     * the connection; only the claim's return can.
     */
    private static final class RetireOnFirstListenerPromise extends DefaultPromise<Channel> {
        private final EmbeddedChannel connection;
        private final MultiplexedChannelRecord record;
        private boolean retired;
        private boolean connectionOpenAfterDrainedCheck;

        RetireOnFirstListenerPromise(EmbeddedChannel connection, MultiplexedChannelRecord record) {
            super(connection.eventLoop());
            this.connection = connection;
            this.record = record;
        }

        @Override
        public Promise<Channel> addListener(GenericFutureListener<? extends Future<? super Channel>> listener) {
            if (!retired) {
                retired = true;
                // EmbeddedEventLoop.inEventLoop() is always true, so doInEventLoop runs closeToNewStreams inline.
                record.closeToNewStreams();
                connection.runPendingTasks();
                connectionOpenAfterDrainedCheck = connection.isOpen();
            }
            return super.addListener(listener);
        }
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
