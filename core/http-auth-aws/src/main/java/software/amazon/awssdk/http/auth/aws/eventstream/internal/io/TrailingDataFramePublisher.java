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

package software.amazon.awssdk.http.auth.aws.eventstream.internal.io;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * A publisher which wraps a publisher and appends a trailing frame once the wrapped publisher is finished.
 */
@SdkInternalApi
public final class TrailingDataFramePublisher implements Publisher<ByteBuffer> {

    private final Publisher<ByteBuffer> trailingPublisher;

    public TrailingDataFramePublisher(Publisher<ByteBuffer> publisher) {
        this.trailingPublisher = subscriber -> {
            Subscriber<ByteBuffer> adaptedSubscriber = new SubscriberAdapter(subscriber);
            publisher.subscribe(adaptedSubscriber);
        };
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
        trailingPublisher.subscribe(subscriber);
    }

    static class SubscriberAdapter implements Subscriber<ByteBuffer> {
        private final AtomicBoolean upstreamDone = new AtomicBoolean(false);
        private final AtomicLong downstreamDemand = new AtomicLong();
        private final Object lock = new Object();
        private final Subscriber<? super ByteBuffer> delegate;
        private volatile boolean sentTrailingFrame = false;

        SubscriberAdapter(Subscriber<? super ByteBuffer> actual) {
            this.delegate = actual;
        }

        @Override
        public void onSubscribe(Subscription s) {
            delegate.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    if (n <= 0) {
                        onError(new IllegalArgumentException("n > 0 required but it was " + n));
                    }

                    downstreamDemand.getAndAdd(n);

                    if (upstreamDone.get()) {
                        sendTrailingEmptyFrame();
                    } else {
                        s.request(n);
                    }
                }

                @Override
                public void cancel() {
                    s.cancel();
                }
            });
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            downstreamDemand.decrementAndGet();
            delegate.onNext(byteBuffer);
        }

        @Override
        public void onError(Throwable t) {
            upstreamDone.compareAndSet(false, true);
            delegate.onError(t);
        }

        @Override
        public void onComplete() {
            upstreamDone.compareAndSet(false, true);

            if (downstreamDemand.get() > 0) {
                sendTrailingEmptyFrame();
            }
        }

        private void sendTrailingEmptyFrame() {
            // when upstream complete, send a trailing empty frame
            synchronized (lock) {
                if (!sentTrailingFrame) {
                    sentTrailingFrame = true;
                    delegate.onNext(ByteBuffer.wrap(new byte[] {}));
                    delegate.onComplete();
                }
            }
        }
    }
}
