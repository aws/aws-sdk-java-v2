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

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http2.DefaultHttp2PingFrame;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.nio.netty.internal.http2.Http2PingHandler;
import software.amazon.awssdk.http.nio.netty.internal.utils.NettyClientLogger;

public class IdleConnectionReaperHandlerTest {
    private static final NettyClientLogger log = NettyClientLogger.getLogger(IdleConnectionReaperHandlerTest.class);

    @Test
    public void channelSwitchFromInUseToNotInUse_shouldCloseOnIdleTimeout_EvenIfPingsAreGoingOn() {
        // pipelines are in the order of
        // Transport -> Head -> Handlers -> Tail -> Application
        IdleConnectionReaperHandler idleHandler = new IdleConnectionReaperHandler(1000); // timeout connection after 1 sec
        Http2PingHandler pingHandler = new Http2PingHandler(100); // ping every 100 ms.

        // create a channel with the following handlers
        // Transport -> Head -> pingHandler, idleHandler -> Tail -> Application
        // Even though pings are going on keeping the channel alive, idleHandler should detect idleness
        EmbeddedChannel channel = createHttp2Channel(pingHandler, idleHandler);
        channel.attr(ChannelAttributeKey.IN_USE).set(true);

        // Roughly 20 pings will be sent and acked... channel will be detected as idle but not timeout because it is
        // in use
        int count = 0;
        Instant runEnd = Instant.now().plus(2, SECONDS);
        while (Instant.now().isBefore(runEnd)) {
            channel.runPendingTasks();
            DefaultHttp2PingFrame sentFrame = channel.readOutbound();

            if (sentFrame != null) {
                assertThat(sentFrame.ack()).isFalse();
                channel.writeInbound(new DefaultHttp2PingFrame(0, true));
                count++;
            }
        }

        Assert.assertTrue(count >= 20); // atleast 20 must be sent
        Assert.assertFalse(channel.closeFuture().isDone());

        channel.attr(ChannelAttributeKey.IN_USE).set(false);

        // Roughly 20 more pings will be sent and acked... channel will be detected as idle and timeout and close
        runEnd = Instant.now().plus(2, SECONDS);
        while (Instant.now().isBefore(runEnd)) {
            channel.runPendingTasks();
            DefaultHttp2PingFrame sentFrame = channel.readOutbound();

            if (sentFrame != null) {
                assertThat(sentFrame.ack()).isFalse();
                channel.writeInbound(new DefaultHttp2PingFrame(0, true));
                count++;
            }
        }
        Assert.assertTrue(channel.closeFuture().isDone());
    }


    @Test
    public void channelNeverUsed_shouldCloseOnIdleTimeout_EvenIfPingsAreGoingOn() {
        // pipelines are in the order of
        // Transport -> Head -> Handlers -> Tail -> Application
        IdleConnectionReaperHandler idleHandler = new IdleConnectionReaperHandler(1000); // timeout connection after 1 sec
        Http2PingHandler pingHandler = new Http2PingHandler(100); // ping every 100 ms.

        // create a channel with the following handlers
        // Transport -> Head -> pingHandler, idleHandler -> Tail -> Application
        // Even though pings are going on keeping the channel alive, idleHandler should detect idleness
        EmbeddedChannel channel = createHttp2Channel(pingHandler, idleHandler);

        // Roughly 20 pings will be sent and acked... channel will be detected as idle and timeout and close
        // in use
        int count = 0;
        Instant runEnd = Instant.now().plus(2, SECONDS);
        while (Instant.now().isBefore(runEnd)) {
            channel.runPendingTasks();
            DefaultHttp2PingFrame sentFrame = channel.readOutbound();

            if (sentFrame != null) {
                assertThat(sentFrame.ack()).isFalse();
                channel.writeInbound(new DefaultHttp2PingFrame(0, true));
                count++;
            }
        }
        Assert.assertTrue(count == 10); // because connection closes after 10 pings are sent/rcvd
        Assert.assertTrue(channel.closeFuture().isDone());
    }

    private static EmbeddedChannel createChannelWithoutProtocol(ChannelHandler... handlers) {
        EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(ChannelAttributeKey.PROTOCOL_FUTURE).set(new CompletableFuture<>());
        channel.pipeline().addLast(handlers);
        return channel;
    }

    private static EmbeddedChannel createHttp2Channel(ChannelHandler... handlers) {
        EmbeddedChannel channel = createChannelWithoutProtocol(handlers);
        channel.attr(ChannelAttributeKey.PROTOCOL_FUTURE).get().complete(Protocol.HTTP2);
        return channel;
    }
}
