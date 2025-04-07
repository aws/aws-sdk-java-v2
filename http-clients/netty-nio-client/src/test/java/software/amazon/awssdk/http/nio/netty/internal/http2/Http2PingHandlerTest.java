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

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http2.DefaultHttp2PingFrame;
import io.netty.handler.codec.http2.Http2PingFrame;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey;
import software.amazon.awssdk.http.nio.netty.internal.utils.NettyClientLogger;

public class Http2PingHandlerTest {
    private static final NettyClientLogger log = NettyClientLogger.getLogger(Http2PingHandler.class);

    private static final int FAST_CHECKER_DURATION_MILLIS = 100;

    private Http2PingHandler fastChecker;
    private Http2PingHandler slowChecker;

    @BeforeEach
    public void setup() throws Exception {
        this.fastChecker = new Http2PingHandler(FAST_CHECKER_DURATION_MILLIS);
        this.slowChecker = new Http2PingHandler(30 * 1_000);
    }

    @Test
    public void register_withoutProtocol_Fails() {
        EmbeddedChannel channel = new EmbeddedChannel(slowChecker);
        assertThat(channel.pipeline().get(Http2PingHandler.class)).isNull();
    }

    @Test
    public void register_withIncompleteProtocol_doesNotPing() {
        EmbeddedChannel channel = createChannelWithoutProtocol(fastChecker);
        channel.runPendingTasks();

        DefaultHttp2PingFrame sentFrame = channel.readOutbound();

        assertThat(sentFrame).isNull();
    }

    @Test
    public void register_withHttp1Protocol_doesNotPing() {
        EmbeddedChannel channel = createHttp1Channel(fastChecker);
        channel.runPendingTasks();

        DefaultHttp2PingFrame sentFrame = channel.readOutbound();

        assertThat(sentFrame).isNull();
    }

    @Test
    public void register_WithHttp2Protocol_pingsImmediately() {
        EmbeddedChannel channel = createHttp2Channel(slowChecker);
        channel.runPendingTasks();

        DefaultHttp2PingFrame sentFrame = channel.readOutbound();

        assertThat(sentFrame).isNotNull();
        assertThat(sentFrame.ack()).isFalse();
    }

    @Test
    public void unregister_stopsRunning() throws InterruptedException {
        EmbeddedChannel channel = createHttp2Channel(fastChecker);
        channel.pipeline().remove(Http2PingHandler.class);

        // Flush out any tasks that happened before we closed
        channel.runPendingTasks();

        while (channel.readOutbound() != null) {
            // Discard
        }

        Thread.sleep(FAST_CHECKER_DURATION_MILLIS);

        DefaultHttp2PingFrame sentFrame = channel.readOutbound();

        assertThat(sentFrame).isNull();
    }

    @Test
    public void schedulingDelayDoesNotCausePingTimeout() throws InterruptedException {
        PipelineExceptionCatcher catcher = new PipelineExceptionCatcher();
        PingResponder pingResponder = new PingResponder();
        EmbeddedChannel channel = createHttp2Channel(fastChecker, catcher, pingResponder);

        pingResponder.setCallback(() -> channel.writeInbound(new DefaultHttp2PingFrame(0, true)),
                                  (long)(FAST_CHECKER_DURATION_MILLIS / 10) /* send ack 10ms after getting ping */);

        channel.runPendingTasks();

        // cause a scheduling delay for the timer to run
        Thread.sleep(FAST_CHECKER_DURATION_MILLIS * 2);
        channel.runPendingTasks();

        assertThat(catcher.caughtExceptions).hasSize(0);
    }

    @Test
    public void respondedToPingsResultInNoAction() {
        PipelineExceptionCatcher catcher = new PipelineExceptionCatcher();
        EmbeddedChannel channel = createHttp2Channel(fastChecker, catcher);

        channel.eventLoop().scheduleAtFixedRate(() -> channel.writeInbound(new DefaultHttp2PingFrame(0, true)),
                                                0, FAST_CHECKER_DURATION_MILLIS, TimeUnit.MILLISECONDS);

        Instant runEnd = Instant.now().plus(1, SECONDS);
        while (Instant.now().isBefore(runEnd)) {
            channel.runPendingTasks();
        }

        assertThat(catcher.caughtExceptions).isEmpty();
    }

    @Test
    public void nonAckPingsResultInOneChannelException() {
        PipelineExceptionCatcher catcher = new PipelineExceptionCatcher();
        EmbeddedChannel channel = createHttp2Channel(fastChecker, catcher);

        channel.eventLoop().scheduleAtFixedRate(() -> channel.writeInbound(new DefaultHttp2PingFrame(0, false)),
                                                0, FAST_CHECKER_DURATION_MILLIS, TimeUnit.MILLISECONDS);

        Instant runEnd = Instant.now().plus(1, SECONDS);
        while (Instant.now().isBefore(runEnd)) {
            channel.runPendingTasks();
        }

        assertThat(catcher.caughtExceptions).hasSize(1);
        assertThat(catcher.caughtExceptions.get(0)).isInstanceOf(IOException.class);
    }

    @Test
    public void failedWriteResultsInOneChannelException() throws InterruptedException {
        PipelineExceptionCatcher catcher = new PipelineExceptionCatcher();
        EmbeddedChannel channel = createHttp2Channel(fastChecker, catcher, new FailingWriter());
        channel.runPendingTasks();
        assertThat(catcher.caughtExceptions).hasSize(1);
        assertThat(catcher.caughtExceptions.get(0)).isInstanceOf(IOException.class);
    }

    @Test
    public void ackPingsAreNotForwardedToOtherHandlers() throws InterruptedException {
        PingReadCatcher catcher = new PingReadCatcher();
        EmbeddedChannel channel = createHttp2Channel(fastChecker, catcher);
        channel.writeInbound(new DefaultHttp2PingFrame(0, true));

        channel.runPendingTasks();

        assertThat(catcher.caughtPings).isEmpty();
    }

    private static EmbeddedChannel createChannelWithoutProtocol(ChannelHandler... handlers) {
        EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(ChannelAttributeKey.PROTOCOL_FUTURE).set(new CompletableFuture<>());
        channel.pipeline().addLast(handlers);
        return channel;
    }

    private static EmbeddedChannel createHttp1Channel(ChannelHandler... handlers) {
        EmbeddedChannel channel = createChannelWithoutProtocol(handlers);
        channel.attr(ChannelAttributeKey.PROTOCOL_FUTURE).get().complete(Protocol.HTTP1_1);
        return channel;
    }

    private static EmbeddedChannel createHttp2Channel(ChannelHandler... handlers) {
        EmbeddedChannel channel = createChannelWithoutProtocol(handlers);
        channel.attr(ChannelAttributeKey.PROTOCOL_FUTURE).get().complete(Protocol.HTTP2);
        return channel;
    }

    @Test
    public void nonAckPingsAreForwardedToOtherHandlers() throws InterruptedException {
        PingReadCatcher catcher = new PingReadCatcher();
        EmbeddedChannel channel = createHttp2Channel(fastChecker, catcher);
        channel.writeInbound(new DefaultHttp2PingFrame(0, false));

        channel.runPendingTasks();

        assertThat(catcher.caughtPings).hasSize(1);
    }

    @Test
    public void channelInactive_shouldCancelTaskAndForwardToOtherHandlers() {
        EmbeddedChannel channel = createHttp2Channel(fastChecker);
        ChannelHandlerContext context = Mockito.mock(ChannelHandlerContext.class);
        fastChecker.channelInactive(context);
        Mockito.verify(context).fireChannelInactive();

        channel.writeInbound(new DefaultHttp2PingFrame(0, false));
        assertThat(channel.runScheduledPendingTasks()).isEqualTo(-1L);
    }

    @Test
    public void delayedPingFlushDoesntTerminateConnectionPrematurely() {
        Logger.getLogger("").setLevel(Level.ALL);

        PipelineExceptionCatcher catcher = new PipelineExceptionCatcher();
        PingResponder pingResponder = new PingResponder();
        DelayingWriter delayingWriter = new DelayingWriter((long)(FAST_CHECKER_DURATION_MILLIS * 1.5));

        EmbeddedChannel channel = createHttp2Channel(fastChecker, catcher, pingResponder, delayingWriter);

        pingResponder.setCallback(() -> channel.writeInbound(new DefaultHttp2PingFrame(0, true)),
                                  5 /* send ack in 10 ms after getting ping*/);

        Instant runEnd = Instant.now().plus(1, SECONDS);
        while (Instant.now().isBefore(runEnd)) {
            channel.runPendingTasks();
            assertThat(catcher.caughtExceptions).isEmpty();
        }
    }

    @Test
    public void delayedPingAckTerminatesConnection() {
        Logger.getLogger("").setLevel(Level.ALL);

        PipelineExceptionCatcher catcher = new PipelineExceptionCatcher();
        PingResponder pingResponder = new PingResponder();
        DelayingWriter delayingWriter = new DelayingWriter((long)(FAST_CHECKER_DURATION_MILLIS * 1.5));

        EmbeddedChannel channel = createHttp2Channel(fastChecker, catcher, pingResponder, delayingWriter);

        pingResponder.setCallback(() -> channel.writeInbound(new DefaultHttp2PingFrame(0, true)),
                                  (long)(FAST_CHECKER_DURATION_MILLIS * 1.5) /* send a late ack to trigger timeout */);

        Instant runEnd = Instant.now().plus(1, SECONDS);
        while (Instant.now().isBefore(runEnd)) {
            channel.runPendingTasks();
            if (!catcher.caughtExceptions.isEmpty()) {
                break;
            }
        }

        assertThat(catcher.caughtExceptions).hasSize(1);
        assertThat(catcher.caughtExceptions.get(0)).isInstanceOf(PingFailedException.class);
    }

    private static final class PingReadCatcher extends SimpleChannelInboundHandler<Http2PingFrame> {
        private final List<Http2PingFrame> caughtPings = Collections.synchronizedList(new ArrayList<>());

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Http2PingFrame msg) {
            caughtPings.add(msg);
        }
    }

    private static final class PipelineExceptionCatcher extends ChannelInboundHandlerAdapter {
        private final List<Throwable> caughtExceptions = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            caughtExceptions.add(cause);
            super.exceptionCaught(ctx, cause);
        }
    }

    private static final class FailingWriter extends ChannelOutboundHandlerAdapter {
        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
            promise.setFailure(new IOException("Failed!"));
        }
    }

    private static final class PingResponder extends ChannelOutboundHandlerAdapter {
        private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        private Runnable respondCallback;
        private long callbackDelayMillis;

        void setCallback(Runnable respondCallback, long delay) {
            this.respondCallback = respondCallback;
            this.callbackDelayMillis = delay;
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
            if (msg instanceof Http2PingFrame) {
                log.debug(ctx.channel(), () -> "OutgoingPingCatcher Received ping " + msg);
                scheduler.schedule(respondCallback, callbackDelayMillis, TimeUnit.MILLISECONDS);
            }
            ctx.write(msg, promise);
        }
    }

    private static final class DelayingWriter extends ChannelOutboundHandlerAdapter {
        private final long sleepMillis;

        DelayingWriter(long sleepMillis) {
            this.sleepMillis = sleepMillis;
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
            log.debug(ctx.channel(), () -> " Writing " + msg + " delayed by " + sleepMillis);
            try {
                Thread.sleep(sleepMillis);
            } catch (InterruptedException e) {

            }
            log.debug(ctx.channel(), () -> " Continuing write of " + msg);

            ctx.write(msg, promise);
        }
    }
}