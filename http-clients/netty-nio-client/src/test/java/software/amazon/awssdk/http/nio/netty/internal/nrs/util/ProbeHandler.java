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

package software.amazon.awssdk.http.nio.netty.internal.nrs.util;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.SubscriberWhiteboxVerification;

/**
 * This class contains source imported from https://github.com/playframework/netty-reactive-streams,
 * licensed under the Apache License 2.0, available at the time of the fork (1/31/2020) here:
 * https://github.com/playframework/netty-reactive-streams/blob/master/LICENSE.txt
 *
 * All original source licensed under the Apache License 2.0 by playframework. All modifications are
 * licensed under the Apache License 2.0 by Amazon Web Services.
 */
public class ProbeHandler<T> extends ChannelDuplexHandler implements SubscriberWhiteboxVerification.SubscriberPuppet {

    private static final int NO_CONTEXT = 0;
    private static final int RUN = 1;
    private static final int CANCEL = 2;

    private final SubscriberWhiteboxVerification.WhiteboxSubscriberProbe<T> probe;
    private final Class<T> clazz;
    private final Queue<WriteEvent> queue = new LinkedList<>();
    private final AtomicInteger state = new AtomicInteger(NO_CONTEXT);
    private volatile ChannelHandlerContext ctx;
    // Netty doesn't provide a way to send errors out, so we capture whether it was an error or complete here
    private volatile Throwable receivedError = null;

    public ProbeHandler(SubscriberWhiteboxVerification.WhiteboxSubscriberProbe<T> probe, Class<T> clazz) {
        this.probe = probe;
        this.clazz = clazz;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
        if (!state.compareAndSet(NO_CONTEXT, RUN)) {
            ctx.fireChannelInactive();
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        queue.add(new WriteEvent(clazz.cast(msg), promise));
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise future) throws Exception {
        if (receivedError == null) {
            probe.registerOnComplete();
        } else {
            probe.registerOnError(receivedError);
        }
    }

    @Override
    public void flush(ChannelHandlerContext ctx) throws Exception {
        while (!queue.isEmpty()) {
            WriteEvent event = queue.remove();
            probe.registerOnNext(event.msg);
            event.future.setSuccess();
        }
    }

    @Override
    public void triggerRequest(long elements) {
        // No need, the channel automatically requests
    }

    @Override
    public void signalCancel() {
        if (!state.compareAndSet(NO_CONTEXT, CANCEL)) {
            ctx.fireChannelInactive();
        }
    }

    private class WriteEvent {
        final T msg;
        final ChannelPromise future;

        private WriteEvent(T msg, ChannelPromise future) {
            this.msg = msg;
            this.future = future;
        }
    }

    public Subscriber<T> wrap(final Subscriber<T> subscriber) {
        return new Subscriber<T>() {
            public void onSubscribe(Subscription s) {
                probe.registerOnSubscribe(ProbeHandler.this);
                subscriber.onSubscribe(s);
            }
            public void onNext(T t) {
                subscriber.onNext(t);
            }
            public void onError(Throwable t) {
                receivedError = t;
                subscriber.onError(t);
            }
            public void onComplete() {
                subscriber.onComplete();
            }
        };
    }
}
