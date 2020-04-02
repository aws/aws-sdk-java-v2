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

package software.amazon.awssdk.http.nio.netty.internal.nrs;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.internal.TypeParameterMatcher;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Publisher for a Netty Handler.
 *
 * This publisher supports only one subscriber.
 *
 * All interactions with the subscriber are done from the handlers executor, hence, they provide the same happens before
 * semantics that Netty provides.
 *
 * The handler publishes all messages that match the type as specified by the passed in class. Any non matching messages
 * are forwarded to the next handler.
 *
 * The publisher will signal complete if it receives a channel inactive event.
 *
 * The publisher will release any messages that it drops (for example, messages that are buffered when the subscriber
 * cancels), but other than that, it does not release any messages.  It is up to the subscriber to release messages.
 *
 * If the subscriber cancels, the publisher will send a close event up the channel pipeline.
 *
 * All errors will short circuit the buffer, and cause publisher to immediately call the subscribers onError method,
 * dropping the buffer.
 *
 * The publisher can be subscribed to or placed in a handler chain in any order.
 *
 * This class contains source imported from https://github.com/playframework/netty-reactive-streams,
 * licensed under the Apache License 2.0, available at the time of the fork (1/31/2020) here:
 * https://github.com/playframework/netty-reactive-streams/blob/master/LICENSE.txt
 */
@SdkInternalApi
public class HandlerPublisher<T> extends ChannelDuplexHandler implements Publisher<T> {
    /**
     * Used for buffering a completion signal.
     */
    private static final Object COMPLETE = new Object() {
        @Override
        public String toString() {
            return "COMPLETE";
        }
    };

    private final EventExecutor executor;
    private final TypeParameterMatcher matcher;

    private final Queue<Object> buffer = new LinkedList<>();

    /**
     * Whether a subscriber has been provided. This is used to detect whether two subscribers are subscribing
     * simultaneously.
     */
    private final AtomicBoolean hasSubscriber = new AtomicBoolean();

    private State state = HandlerPublisher.State.NO_SUBSCRIBER_OR_CONTEXT;

    private volatile Subscriber<? super T> subscriber;
    private ChannelHandlerContext ctx;
    private long outstandingDemand = 0;
    private Throwable noSubscriberError;

    /**
     * Create a handler publisher.
     *
     * The supplied executor must be the same event loop as the event loop that this handler is eventually registered
     * with, if not, an exception will be thrown when the handler is registered.
     *
     * @param executor The executor to execute asynchronous events from the subscriber on.
     * @param subscriberMessageType The type of message this publisher accepts.
     */
    public HandlerPublisher(EventExecutor executor, Class<? extends T> subscriberMessageType) {
        this.executor = executor;
        this.matcher = TypeParameterMatcher.get(subscriberMessageType);
    }

    /**
     * Returns {@code true} if the given message should be handled. If {@code false} it will be passed to the next
     * {@link ChannelInboundHandler} in the {@link ChannelPipeline}.
     *
     * @param msg The message to check.
     * @return True if the message should be accepted.
     */
    protected boolean acceptInboundMessage(Object msg) throws Exception {
        return matcher.match(msg);
    }

    /**
     * Override to handle when a subscriber cancels the subscription.
     *
     * By default, this method will simply close the channel.
     */
    protected void cancelled() {
        ctx.close();
    }

    /**
     * Override to intercept when demand is requested.
     *
     * By default, a channel read is invoked.
     */
    protected void requestDemand() {
        ctx.read();
    }

    enum State {
        /**
         * Initial state. There's no subscriber, and no context.
         */
        NO_SUBSCRIBER_OR_CONTEXT,

        /**
         * A subscriber has been provided, but no context has been provided.
         */
        NO_CONTEXT,

        /**
         * A context has been provided, but no subscriber has been provided.
         */
        NO_SUBSCRIBER,

        /**
         * An error has been received, but there's no subscriber to receive it.
         */
        NO_SUBSCRIBER_ERROR,

        /**
         * There is no demand, and we have nothing buffered.
         */
        IDLE,

        /**
         * There is no demand, and we're buffering elements.
         */
        BUFFERING,

        /**
         * We have nothing buffered, but there is demand.
         */
        DEMANDING,

        /**
         * The stream is complete, however there are still elements buffered for which no demand has come from the subscriber.
         */
        DRAINING,

        /**
         * We're done, in the terminal state.
         */
        DONE
    }

    @Override
    public void subscribe(final Subscriber<? super T> subscriber) {
        if (subscriber == null) {
            throw new NullPointerException("Null subscriber");
        }

        if (!hasSubscriber.compareAndSet(false, true)) {
            // Must call onSubscribe first.
            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                }

                @Override
                public void cancel() {
                }
            });
            subscriber.onError(new IllegalStateException("This publisher only supports one subscriber"));
        } else {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    provideSubscriber(subscriber);
                }
            });
        }
    }

    private void provideSubscriber(Subscriber<? super T> subscriber) {
        this.subscriber = subscriber;
        switch (state) {
            case NO_SUBSCRIBER_OR_CONTEXT:
                state = HandlerPublisher.State.NO_CONTEXT;
                break;
            case NO_SUBSCRIBER:
                if (buffer.isEmpty()) {
                    state = HandlerPublisher.State.IDLE;
                } else {
                    state = HandlerPublisher.State.BUFFERING;
                }
                subscriber.onSubscribe(new ChannelSubscription());
                break;
            case DRAINING:
                subscriber.onSubscribe(new ChannelSubscription());
                break;
            case NO_SUBSCRIBER_ERROR:
                cleanup();
                state = HandlerPublisher.State.DONE;
                subscriber.onSubscribe(new ChannelSubscription());
                subscriber.onError(noSubscriberError);
                break;
            default:
                // Do nothing
        }
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        // If the channel is not yet registered, then it's not safe to invoke any methods on it, eg read() or close()
        // So don't provide the context until it is registered.
        if (ctx.channel().isRegistered()) {
            provideChannelContext(ctx);
        }
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        provideChannelContext(ctx);
        ctx.fireChannelRegistered();
    }

    private void provideChannelContext(ChannelHandlerContext ctx) {
        switch (state) {
            case NO_SUBSCRIBER_OR_CONTEXT:
                verifyRegisteredWithRightExecutor(ctx);
                this.ctx = ctx;
                // It's set, we don't have a subscriber
                state = HandlerPublisher.State.NO_SUBSCRIBER;
                break;
            case NO_CONTEXT:
                verifyRegisteredWithRightExecutor(ctx);
                this.ctx = ctx;
                state = HandlerPublisher.State.IDLE;
                subscriber.onSubscribe(new ChannelSubscription());
                break;
            default:
                // Ignore, this could be invoked twice by both handlerAdded and channelRegistered.
        }
    }

    private void verifyRegisteredWithRightExecutor(ChannelHandlerContext ctx) {
        if (!executor.inEventLoop()) {
            throw new IllegalArgumentException("Channel handler MUST be registered with the same EventExecutor that it is "
                                               + "created with.");
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // If we subscribed before the channel was active, then our read would have been ignored.
        if (state == HandlerPublisher.State.DEMANDING) {
            requestDemand();
        }
        ctx.fireChannelActive();
    }

    private void receivedDemand(long demand) {
        switch (state) {
            case BUFFERING:
            case DRAINING:
                if (addDemand(demand)) {
                    flushBuffer();
                }
                break;

            case DEMANDING:
                addDemand(demand);
                break;

            case IDLE:
                if (addDemand(demand)) {
                    // Important to change state to demanding before doing a read, in case we get a synchronous
                    // read back.
                    state = HandlerPublisher.State.DEMANDING;
                    requestDemand();
                }
                break;
            default:

        }
    }

    private boolean addDemand(long demand) {

        if (demand <= 0) {
            illegalDemand();
            return false;
        } else {
            if (outstandingDemand < Long.MAX_VALUE) {
                outstandingDemand += demand;
                if (outstandingDemand < 0) {
                    outstandingDemand = Long.MAX_VALUE;
                }
            }
            return true;
        }
    }

    private void illegalDemand() {
        cleanup();
        subscriber.onError(new IllegalArgumentException("Request for 0 or negative elements in violation of Section 3.9 "
                                                        + "of the Reactive Streams specification"));
        ctx.close();
        state = HandlerPublisher.State.DONE;
    }

    private void flushBuffer() {
        while (!buffer.isEmpty() && (outstandingDemand > 0 || outstandingDemand == Long.MAX_VALUE)) {
            publishMessage(buffer.remove());
        }
        if (buffer.isEmpty()) {
            if (outstandingDemand > 0) {
                if (state == HandlerPublisher.State.BUFFERING) {
                    state = HandlerPublisher.State.DEMANDING;
                } // otherwise we're draining
                requestDemand();
            } else if (state == HandlerPublisher.State.BUFFERING) {
                state = HandlerPublisher.State.IDLE;
            }
        }
    }

    private void receivedCancel() {
        switch (state) {
            case BUFFERING:
            case DEMANDING:
            case IDLE:
                cancelled();
                state = HandlerPublisher.State.DONE;
                break;
            case DRAINING:
                state = HandlerPublisher.State.DONE;
                break;
            default:
                // ignore
        }
        cleanup();
        subscriber = null;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) throws Exception {
        if (acceptInboundMessage(message)) {
            switch (state) {
                case IDLE:
                    buffer.add(message);
                    state = HandlerPublisher.State.BUFFERING;
                    break;
                case NO_SUBSCRIBER:
                case BUFFERING:
                    buffer.add(message);
                    break;
                case DEMANDING:
                    publishMessage(message);
                    break;
                case DRAINING:
                case DONE:
                    ReferenceCountUtil.release(message);
                    break;
                case NO_CONTEXT:
                case NO_SUBSCRIBER_OR_CONTEXT:
                    throw new IllegalStateException("Message received before added to the channel context");
                default:
                    // Ignore
            }
        } else {
            ctx.fireChannelRead(message);
        }
    }

    private void publishMessage(Object message) {
        if (COMPLETE.equals(message)) {
            subscriber.onComplete();
            state = HandlerPublisher.State.DONE;
        } else {
            @SuppressWarnings("unchecked")
            T next = (T) message;
            subscriber.onNext(next);
            if (outstandingDemand < Long.MAX_VALUE) {
                outstandingDemand--;
                if (outstandingDemand == 0 && state != HandlerPublisher.State.DRAINING) {
                    if (buffer.isEmpty()) {
                        state = HandlerPublisher.State.IDLE;
                    } else {
                        state = HandlerPublisher.State.BUFFERING;
                    }
                }
            }
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        if (state == HandlerPublisher.State.DEMANDING) {
            requestDemand();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        complete();
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        complete();
    }

    private void complete() {
        switch (state) {
            case NO_SUBSCRIBER:
            case BUFFERING:
                buffer.add(COMPLETE);
                state = HandlerPublisher.State.DRAINING;
                break;
            case DEMANDING:
            case IDLE:
                subscriber.onComplete();
                state = HandlerPublisher.State.DONE;
                break;
            case NO_SUBSCRIBER_ERROR:
                // Ignore, we're already going to complete the stream with an error
                // when the subscriber subscribes.
                break;
            default:
                // Ignore
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        switch (state) {
            case NO_SUBSCRIBER:
                noSubscriberError = cause;
                state = HandlerPublisher.State.NO_SUBSCRIBER_ERROR;
                cleanup();
                break;
            case BUFFERING:
            case DEMANDING:
            case IDLE:
            case DRAINING:
                state = HandlerPublisher.State.DONE;
                cleanup();
                subscriber.onError(cause);
                break;
            default:
                // Ignore
        }
    }

    /**
     * Release all elements from the buffer.
     */
    private void cleanup() {
        while (!buffer.isEmpty()) {
            ReferenceCountUtil.release(buffer.remove());
        }
    }

    private class ChannelSubscription implements Subscription {
        @Override
        public void request(final long demand) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    receivedDemand(demand);
                }
            });
        }

        @Override
        public void cancel() {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    receivedCancel();
                }
            });
        }
    }
}
