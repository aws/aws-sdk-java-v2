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

package software.amazon.awssdk.http.auth.aws.internal.signer.io;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Validate;

/**
 * Temporarily used for buffering all data into memory. TODO(sra-identity-auth): Remove this by supporting chunked encoding. We
 * should not buffer everything into memory.
 */
@SdkInternalApi
public class InMemoryPublisher implements Publisher<ByteBuffer> {
    private final AtomicBoolean subscribed = new AtomicBoolean(false);
    private final List<ByteBuffer> data;

    public InMemoryPublisher(List<ByteBuffer> data) {
        this.data = new ArrayList<>(Validate.noNullElements(data, "Data must not contain null elements."));
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> s) {
        if (!subscribed.compareAndSet(false, true)) {
            s.onSubscribe(new NoOpSubscription());
            s.onError(new IllegalStateException("InMemoryPublisher cannot be subscribed to twice."));
            return;
        }

        s.onSubscribe(new Subscription() {
            private final AtomicBoolean sending = new AtomicBoolean(false);

            private final Object doneLock = new Object();
            private final AtomicBoolean done = new AtomicBoolean(false);
            private final AtomicLong demand = new AtomicLong(0);
            private int position = 0;

            @Override
            public void request(long n) {
                if (done.get()) {
                    return;
                }

                try {
                    demand.addAndGet(n);
                    fulfillDemand();
                } catch (Throwable t) {
                    finish(() -> s.onError(t));
                }
            }

            private void fulfillDemand() {
                do {
                    if (sending.compareAndSet(false, true)) {
                        try {
                            send();
                        } finally {
                            sending.set(false);
                        }
                    }
                } while (!done.get() && demand.get() > 0);
            }

            private void send() {
                while (true) {
                    assert position >= 0;
                    assert position <= data.size();

                    if (done.get()) {
                        break;
                    }

                    if (position == data.size()) {
                        finish(s::onComplete);
                        break;
                    }

                    if (demand.get() == 0) {
                        break;
                    }

                    demand.decrementAndGet();
                    int dataIndex = position;
                    s.onNext(data.get(dataIndex));
                    data.set(dataIndex, null); // We're done with this data here, so allow it to be garbage collected
                    position++;
                }
            }

            @Override
            public void cancel() {
                finish(() -> {
                });
            }

            private void finish(Runnable thingToDo) {
                synchronized (doneLock) {
                    if (done.compareAndSet(false, true)) {
                        thingToDo.run();
                    }
                }
            }
        });
    }

    private static class NoOpSubscription implements Subscription {
        @Override
        public void request(long n) {
        }

        @Override
        public void cancel() {
        }
    }
}
