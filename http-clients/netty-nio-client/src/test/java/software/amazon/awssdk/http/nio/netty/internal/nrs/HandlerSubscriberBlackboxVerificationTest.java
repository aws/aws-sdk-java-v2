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

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.SubscriberBlackboxVerification;
import org.reactivestreams.tck.TestEnvironment;

/**
 * This class contains source imported from https://github.com/playframework/netty-reactive-streams,
 * licensed under the Apache License 2.0, available at the time of the fork (1/31/2020) here:
 * https://github.com/playframework/netty-reactive-streams/blob/master/LICENSE.txt
 *
 * All original source licensed under the Apache License 2.0 by playframework. All modifications are
 * licensed under the Apache License 2.0 by Amazon Web Services.
 */
public class HandlerSubscriberBlackboxVerificationTest extends SubscriberBlackboxVerification<Long> {

    public HandlerSubscriberBlackboxVerificationTest() {
        super(new TestEnvironment());
    }

    @Override
    public Subscriber<Long> createSubscriber() {
        // Embedded channel requires at least one handler when it's created, but HandlerSubscriber
        // needs the channels event loop in order to be created, so start with a dummy, then replace.
        ChannelHandler dummy = new ChannelDuplexHandler();
        EmbeddedChannel channel = new EmbeddedChannel(dummy);
        HandlerSubscriber<Long> subscriber = new HandlerSubscriber<>(channel.eventLoop(), 2, 4);
        channel.pipeline().replace(dummy, "subscriber", subscriber);

        return new SubscriberWithChannel<>(channel, subscriber);
    }

    @Override
    public Long createElement(int element) {
        return (long) element;
    }

    @Override
    public void triggerRequest(Subscriber<? super Long> subscriber) {
        EmbeddedChannel channel = ((SubscriberWithChannel) subscriber).channel;

        channel.runPendingTasks();
        while (channel.readOutbound() != null) {
            channel.runPendingTasks();
        }
        channel.runPendingTasks();
    }

    /**
     * Delegate subscriber that makes the embedded channel available so we can talk to it to trigger a request.
     */
    private static class SubscriberWithChannel<T> implements Subscriber<T> {
        final EmbeddedChannel channel;
        final HandlerSubscriber<T> subscriber;

        public SubscriberWithChannel(EmbeddedChannel channel, HandlerSubscriber<T> subscriber) {
            this.channel = channel;
            this.subscriber = subscriber;
        }

        public void onSubscribe(Subscription s) {
            subscriber.onSubscribe(s);
        }

        public void onNext(T t) {
            subscriber.onNext(t);
        }

        public void onError(Throwable t) {
            subscriber.onError(t);
        }

        public void onComplete() {
            subscriber.onComplete();
        }
    }
}
