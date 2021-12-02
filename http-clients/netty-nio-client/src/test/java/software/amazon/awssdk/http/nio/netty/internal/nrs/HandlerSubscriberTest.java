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
 *
 * Original source licensed under the Apache License 2.0 by playframework.
 */

package software.amazon.awssdk.http.nio.netty.internal.nrs;

import static org.assertj.core.api.Assertions.assertThat;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.concurrent.AbstractEventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.internal.ObjectUtil;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Subscription;

/**
 * This class contains source imported from https://github.com/playframework/netty-reactive-streams,
 * licensed under the Apache License 2.0, available at the time of the fork (1/31/2020) here:
 * https://github.com/playframework/netty-reactive-streams/blob/master/LICENSE.txt
 *
 * All original source licensed under the Apache License 2.0 by playframework. All modifications are
 * licensed under the Apache License 2.0 by Amazon Web Services.
 */
public class HandlerSubscriberTest {
    private EmbeddedChannel channel;
    private CustomEmbeddedEventLoop eventLoop;
    private HandlerSubscriber<HttpContent> handler;

    @Before
    public void setup() throws Exception {
        channel = new CustomEmbeddedChannel();
        eventLoop = new CustomEmbeddedEventLoop();
        eventLoop.register(channel).syncUninterruptibly();

        handler = new HandlerSubscriber<>(eventLoop);
        channel.pipeline().addLast(handler);
    }

    @After
    public void teardown() {
        channel.close();
    }

    /**
     * Ensures that onNext invocations against the {@link HandlerSubscriber} do not order things based on which thread is calling
     * onNext.
     */
    @Test
    public void onNextWritesInProperOrderFromAnyThread() {
        HttpContent front = emptyHttpRequest();
        HttpContent back = emptyHttpRequest();

        handler.onSubscribe(doNothingSubscription());
        eventLoop.inEventLoop(false);
        handler.onNext(front);
        eventLoop.inEventLoop(true);
        handler.onNext(back);

        eventLoop.runTasks();

        Queue<Object> outboundMessages = channel.outboundMessages();

        assertThat(outboundMessages).hasSize(2);
        assertThat(outboundMessages.poll()).isSameAs(front);
        assertThat(outboundMessages.poll()).isSameAs(back);
    }

    private DefaultFullHttpRequest emptyHttpRequest() {
        return new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "http://fake.com");
    }

    private Subscription doNothingSubscription() {
        return new Subscription() {
            @Override
            public void request(long n) { }

            @Override
            public void cancel() { }
        };
    }

    private  static class CustomEmbeddedChannel extends EmbeddedChannel {
        private CustomEmbeddedChannel() {
            super(false, false);
        }

        @Override
        protected boolean isCompatible(EventLoop loop) {
            return loop instanceof CustomEmbeddedEventLoop;
        }
    }

    private static class CustomEmbeddedEventLoop extends AbstractEventExecutor implements EventLoop {
        private final Queue<Runnable> tasks = new ArrayDeque<>(2);
        private volatile boolean inEventLoop = true;

        @Override
        public EventLoopGroup parent() {
            return (EventLoopGroup) super.parent();
        }

        @Override
        public EventLoop next() {
            return (EventLoop) super.next();
        }

        @Override
        public void execute(Runnable runnable) {
            tasks.add(runnable);
        }

        public void runTasks() {
            for (;;) {
                Runnable task = tasks.poll();
                if (task == null) {
                    break;
                }

                task.run();
            }
        }

        @Override
        public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Future<?> terminationFuture() {
            throw new UnsupportedOperationException();
        }

        @Override
        @Deprecated
        public void shutdown() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isShuttingDown() {
            return false;
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return false;
        }

        @Override
        public ChannelFuture register(Channel channel) {
            return register(new DefaultChannelPromise(channel, this));
        }

        @Override
        public ChannelFuture register(ChannelPromise promise) {
            ObjectUtil.checkNotNull(promise, "promise");
            promise.channel().unsafe().register(this, promise);
            return promise;
        }

        @Deprecated
        @Override
        public ChannelFuture register(Channel channel, ChannelPromise promise) {
            channel.unsafe().register(this, promise);
            return promise;
        }

        public void inEventLoop(boolean inEventLoop) {
            this.inEventLoop = inEventLoop;
        }

        @Override
        public boolean inEventLoop() {
            return inEventLoop;
        }

        @Override
        public boolean inEventLoop(Thread thread) {
            return inEventLoop;
        }
    }
}