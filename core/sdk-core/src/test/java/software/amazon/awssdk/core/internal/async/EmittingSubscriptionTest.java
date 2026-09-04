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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

class EmittingSubscriptionTest {

    @Test
    void request_cancelledWhileEmitting_doesNotThrow() throws Exception {
        CountDownLatch supplierEntered = new CountDownLatch(1);
        CountDownLatch cancelCompleted = new CountDownLatch(1);
        AtomicBoolean firstSupplierCall = new AtomicBoolean(true);
        AtomicInteger onNextCount = new AtomicInteger();

        EmittingSubscription<Object> subscription =
            EmittingSubscription.builder()
                                .downstreamSubscriber(subscriber(onNextCount, new AtomicReference<>()))
                                .onCancel(() -> {
                                })
                                .supplier(() -> {
                                    // Hold the emitting thread inside the supplier so cancel() lands between the
                                    // isCancelled check and the downstream signal.
                                    if (firstSupplierCall.compareAndSet(true, false)) {
                                        supplierEntered.countDown();
                                        await(cancelCompleted);
                                    }
                                    return new Object();
                                })
                                .build();

        Thread canceller = new Thread(() -> {
            await(supplierEntered);
            subscription.cancel();
            cancelCompleted.countDown();
        });
        canceller.start();

        assertThatCode(() -> subscription.request(2)).doesNotThrowAnyException();

        canceller.join(TimeUnit.SECONDS.toMillis(10));
        // Signalling a subscriber that cancelled mid-emit is allowed (spec rule 2.8), but the loop must stop after it.
        assertThat(onNextCount.get()).isLessThanOrEqualTo(1);
    }

    @Test
    void request_negativeDemandAfterCancel_doesNotThrow() {
        AtomicReference<Throwable> onErrorValue = new AtomicReference<>();

        EmittingSubscription<Object> subscription =
            EmittingSubscription.builder()
                                .downstreamSubscriber(subscriber(new AtomicInteger(), onErrorValue))
                                .onCancel(() -> {
                                })
                                .supplier(Object::new)
                                .build();

        subscription.cancel();

        assertThatCode(() -> subscription.request(0)).doesNotThrowAnyException();
        assertThat(onErrorValue.get()).isNull();
    }

    private static Subscriber<Object> subscriber(AtomicInteger onNextCount, AtomicReference<Throwable> onErrorValue) {
        return new Subscriber<Object>() {
            @Override
            public void onSubscribe(Subscription s) {
            }

            @Override
            public void onNext(Object o) {
                onNextCount.incrementAndGet();
            }

            @Override
            public void onError(Throwable t) {
                onErrorValue.set(t);
            }

            @Override
            public void onComplete() {
            }
        };
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for latch");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
