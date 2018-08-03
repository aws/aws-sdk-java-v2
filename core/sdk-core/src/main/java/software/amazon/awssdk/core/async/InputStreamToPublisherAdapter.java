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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import software.amazon.awssdk.annotations.SdkProtectedApi;

@SdkProtectedApi
public final class InputStreamToPublisherAdapter {
    public Publisher<ByteBuffer> adapt(InputStream is) {
        return new PublisherImpl(is);
    }

    private static class PublisherImpl implements Publisher<ByteBuffer> {
        private final InputStream is;

        PublisherImpl(InputStream is) {
            this.is = is;
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            subscriber.onSubscribe(new SubscriptionImpl(is, subscriber));
        }
    }

    private static class SubscriptionImpl implements Subscription {
        /** The state of this Subscription */
        enum State {
            /** Currently publishing. This is the initial state. */
            PUBLISHING,

            /**
             * In the process of cancelling. The publisher should stop filling
             * any remaining demand as soon as possible.
             */
            CANCELLING,

            /**
             * The subscription is cancelled (either we completed of cancel()
             * was explicitly called). This is a terminal state.
             */
            CANCELLED
        }

        private final ExecutorService exec = Executors.newSingleThreadExecutor();
        private final BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>();
        private AtomicReference<State> currentState = new AtomicReference<>(State.PUBLISHING);
        private final InputStream is;
        private final Subscriber<? super ByteBuffer> subscriber;

        SubscriptionImpl(InputStream is, Subscriber<? super ByteBuffer> subscriber) {
            this.is = is;
            this.subscriber = subscriber;
            this.exec.execute(new TaskRunner());
        }

        @Override
        public void request(long demand) {
            if (demand < 0) {
                signalError(new IllegalArgumentException("Demand must be positive!"));
                return;
            }
            fillDemand(demand);
        }

        @Override
        public void cancel() {
            signalComplete();
        }

        private void fillDemand(long demand) {
            post(() -> {
                for (long i = 0; i < demand && currentState.get() == State.PUBLISHING; ++i) {
                    byte[] buf = new byte[8192];
                    try {
                        int read = is.read(buf);
                        if (read == -1) {
                            signalComplete();
                        } else {
                            subscriber.onNext(ByteBuffer.wrap(buf, 0, read));
                        }
                    } catch (IOException e) {
                        signalError(e);
                    }
                }
            });
        }

        private void signalComplete() {
            if (currentState.compareAndSet(State.PUBLISHING, State.CANCELLING)) {
                post(() -> {
                    currentState.set(State.CANCELLED);
                    subscriber.onComplete();
                    exec.shutdown();
                });
            }
        }

        private void signalError(Throwable t) {
            if (currentState.compareAndSet(State.PUBLISHING, State.CANCELLING)) {
                post(() -> {
                    currentState.set(State.CANCELLED);
                    subscriber.onError(t);
                    exec.shutdown();
                });
            }
        }

        private void post(final Runnable r) {
            taskQueue.add(r);
        }

        private class TaskRunner implements Runnable {
            @Override
            public void run() {
                while (currentState.get() != State.CANCELLED) {
                    try {
                        taskQueue.take().run();
                    } catch (InterruptedException ie) {
                        break;
                    }
                }
            }
        }
    }
}
