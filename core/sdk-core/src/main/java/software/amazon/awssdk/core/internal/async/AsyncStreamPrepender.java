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

package software.amazon.awssdk.core.internal.async;

import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public class AsyncStreamPrepender<T> implements Publisher<T> {
    private final Publisher<T> delegate;
    private final T firstItem;
    private Subscriber<? super T> subscriber;
    private volatile boolean complete = false;
    private volatile boolean firstRequest = true;

    public AsyncStreamPrepender(Publisher<T> delegate, T firstItem) {
        this.delegate = delegate;
        this.firstItem = firstItem;
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
        subscriber = s;
        delegate.subscribe(new DelegateSubscriber());
    }

    private class DelegateSubscriber implements Subscriber<T> {
        @Override
        public void onSubscribe(Subscription subscription) {
            subscriber.onSubscribe(new Subscription() {
                private final AtomicLong requests = new AtomicLong(0L);
                private volatile boolean cancelled = false;
                private volatile boolean isOutermostCall = true;

                @Override
                public void request(long n) {
                    if (cancelled) {
                        return;
                    }
                    if (n <= 0) {
                        subscription.cancel();
                        subscriber.onError(new IllegalArgumentException("Requested " + n + " items"));
                    }

                    if (firstRequest) {
                        firstRequest = false;
                        if (n - 1 > 0) {
                            requests.addAndGet(n - 1);
                        }
                        isOutermostCall = false;
                        subscriber.onNext(firstItem);
                        isOutermostCall = true;
                        if (complete) {
                            subscriber.onComplete();
                            return;
                        }
                    } else {
                        requests.addAndGet(n);
                    }
                    if (isOutermostCall) {
                        try {
                            isOutermostCall = false;
                            long l;
                            while ((l = requests.getAndSet(0L)) > 0) {
                                subscription.request(l);
                            }
                        } finally {
                            isOutermostCall = true;
                        }
                    }
                }

                @Override
                public void cancel() {
                    cancelled = true;
                    subscription.cancel();
                    subscriber = null;
                }
            });
        }

        @Override
        public void onNext(T item) {
            subscriber.onNext(item);
        }

        @Override
        public void onError(Throwable t) {
            subscriber.onError(t);
        }

        @Override
        public void onComplete() {
            complete = true;
            if (!firstRequest) {
                subscriber.onComplete();
            }
        }
    }
}
