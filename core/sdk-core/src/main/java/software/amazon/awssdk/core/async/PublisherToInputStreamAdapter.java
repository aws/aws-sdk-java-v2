/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.async;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkProtectedApi;

@SdkProtectedApi
public final class PublisherToInputStreamAdapter {
    public InputStream adapt(Publisher<ByteBuffer> publisher) {
        return new StreamImpl(publisher);
    }

    private static class StreamImpl extends InputStream implements Subscriber<ByteBuffer> {
        /** The current state of this stream */
        enum State {
            /** Initial state */
            NEED_SUBSCRIPTION,

            /** Waiting for the subscription */
            WAITING_SUBSCRIPTION,

            /** The current buffer is empty and we need more data from the publisher */
            NEED_DATA,

            /** Requested more data and waiting for it to arrive */
            WAITING_DATA,

            /** Currently reading from the buffer */
            READING_BUFFER,

            /** The stream is in error. This is a terminal state */
            ERROR,

            /** Reached the end of the stream. This is a terminal state */
            EOF
        }

        private final BlockingQueue<Event> events = new LinkedBlockingQueue<>();
        private final Publisher<ByteBuffer> publisher;
        private Subscription subscription;

        private State currentState = State.NEED_SUBSCRIPTION;
        private ByteBuffer buf;
        private Throwable error;

        StreamImpl(Publisher<ByteBuffer> publisher) {
            this.publisher = publisher;
        }

        @Override
        public int read() throws IOException {
            while (true) {
                switch (currentState) {
                    case EOF:
                        return -1;
                    case ERROR:
                        throw new IOException("Error getting data from Publisher", error);
                    case READING_BUFFER:
                        if (!bufEmpty()) {
                            return (int) buf.get() & 0xFF;
                        } else {
                            transitionState();
                        }
                        break;
                    default:
                        transitionState();
                }
            }
        }

        @Override
        public void close() {
            subscription.cancel();
            currentState = State.EOF;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            events.add(new OnSubscribeEvent(subscription));
        }

        @Override
        public void onNext(ByteBuffer data) {
            events.add(new OnNextEvent(data));
        }

        @Override
        public void onError(Throwable throwable) {
            events.add(new OnErrorEvent(throwable));
        }

        @Override
        public void onComplete() {
            events.add(new OnCompleteEvent());
        }

        private void transitionState() {
            // perform the action necessary to transition out of the current
            // state
            switch (currentState) {
                case READING_BUFFER:
                    if (bufEmpty()) {
                        currentState = State.NEED_DATA;
                    }
                    return;
                case NEED_DATA:
                    subscription.request(1);
                    currentState = State.WAITING_DATA;
                    return;
                case NEED_SUBSCRIPTION:
                    publisher.subscribe(this);
                    break;
                default:
                    break;
            }

            try {
                Event ev = events.take();
                if (ev instanceof OnSubscribeEvent) {
                    OnSubscribeEvent onSubEv = (OnSubscribeEvent) ev;
                    subscription = onSubEv.subscription;
                    currentState = State.NEED_DATA;
                } else if (ev instanceof OnNextEvent) {
                    OnNextEvent onNextEv = (OnNextEvent) ev;
                    buf = onNextEv.data;
                    currentState = State.READING_BUFFER;
                } else if (ev instanceof OnCompleteEvent) {
                    currentState = State.EOF;
                } else if (ev instanceof OnErrorEvent) {
                    OnErrorEvent onErrorEv = (OnErrorEvent) ev;
                    error = onErrorEv.error;
                    currentState = State.ERROR;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                subscription.cancel();
                error = e;
                currentState = State.ERROR;
            }
        }

        private boolean bufEmpty() {
            return (buf == null || buf.remaining() == 0);
        }

        private interface Event {
        }

        private static class OnNextEvent implements Event {
            private final ByteBuffer data;

            OnNextEvent(ByteBuffer data) {
                this.data = data;
            }
        }

        private static class OnErrorEvent implements Event {
            private final Throwable error;

            OnErrorEvent(Throwable error) {
                this.error = error;
            }
        }

        private static class OnCompleteEvent implements Event {
        }

        private static class OnSubscribeEvent implements Event {
            private final Subscription subscription;

            OnSubscribeEvent(Subscription subscription) {
                this.subscription = subscription;
            }
        }
    }
}
