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

package software.amazon.awssdk.utils.async;


import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.internal.async.EmptySubscription;

/**
 * Converts an {@link Iterable} to a {@link Publisher}.
 */
@SdkProtectedApi
public class IterablePublisher<T> implements Publisher<T> {

    private final Iterator<T> iterator;
    private Subscriber<? super T> subscriber;
    private AtomicBoolean isSendingData = new AtomicBoolean(false);
    private final AtomicLong outstandingDemand = new AtomicLong();

    public IterablePublisher(Iterable<T> iterable) {
        Validate.notNull(iterable, "iterable");
        this.iterator = iterable.iterator();
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
        if (subscriber != null) {
            s.onSubscribe(new NoOpSubscription(s));
            s.onError(new IllegalArgumentException("Only one subscription may be active at a time."));
        }

        this.subscriber = s;

        if (!iterator.hasNext()) {
            subscriber.onSubscribe(new EmptySubscription(s));
            return;
        }

        subscriber.onSubscribe(new IteratorSubscription());
    }

    private class IteratorSubscription implements Subscription {
        private volatile boolean done;

        @Override
        public void request(long newDemand) {
            if (newDemand <= 0) {
                subscriber.onError(new IllegalArgumentException("demand is not positive"));
            }

            outstandingDemand.updateAndGet(current -> {
                if (Long.MAX_VALUE - current < newDemand) {
                    return Long.MAX_VALUE;
                }

                return current + newDemand;
            });
            sendData();
        }

        private void sendData() {
            do {
                if (!isSendingData.compareAndSet(false, true)) {
                    return;
                }
                try {
                    doSendData();
                } finally {
                    isSendingData.set(false);
                }
            } while (shouldSendMoreData());
        }

        private boolean shouldSendMoreData() {
            if (done) {
                return false;
            }

            if (!iterator.hasNext()) {
                done = true;
                subscriber.onComplete();
                return false;
            }

            return outstandingDemand.get() > 0;
        }

        private void doSendData() {
            while (shouldSendMoreData()) {
                outstandingDemand.decrementAndGet();
                T next = iterator.next();
                if (next == null) {
                    done = true;
                    subscriber.onError(new IllegalArgumentException("Iterable returned null"));
                } else {
                    subscriber.onNext(next);
                }
            }
        }

        @Override
        public void cancel() {
            done = true;
            subscriber = null;
        }
    }
}
