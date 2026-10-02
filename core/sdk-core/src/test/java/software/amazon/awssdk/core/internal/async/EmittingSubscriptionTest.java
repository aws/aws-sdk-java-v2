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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

class EmittingSubscriptionTest {

    @Test
    void request_withPositiveDemand_emitsOneItemPerRequested() {
        RecordingSubscriber subscriber = new RecordingSubscriber();
        EmittingSubscription<Object> subscription = subscriptionFor(subscriber);

        subscription.request(3);

        assertThat(subscriber.onNextCount.get()).isEqualTo(3);
        assertThat(subscriber.onError.get()).isNull();
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, Long.MIN_VALUE})
    void request_withNonPositiveDemand_signalsIllegalArgumentException(long demand) {
        RecordingSubscriber subscriber = new RecordingSubscriber();
        EmittingSubscription<Object> subscription = subscriptionFor(subscriber);

        subscription.request(demand);

        assertThat(subscriber.onError.get()).isInstanceOf(IllegalArgumentException.class);
        assertThat(subscriber.onNextCount.get()).isZero();
    }

    @Test
    void cancel_thenRequest_emitsNothing() {
        RecordingSubscriber subscriber = new RecordingSubscriber();
        EmittingSubscription<Object> subscription = subscriptionFor(subscriber);

        subscription.cancel();
        subscription.request(5);

        assertThat(subscriber.onNextCount.get()).isZero();
        assertThat(subscriber.onError.get()).isNull();
    }

    /**
     * A {@code cancel()} concurrent with an in-flight emit loop must not throw.
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void cancel_concurrentWithEmit_neverThrowsNullPointerException() throws InterruptedException {
        int rounds = 1_000;
        // A large demand keeps the emit loop running long enough to overlap the cancel.
        long demandPerRound = 10_000;

        for (int round = 0; round < rounds; round++) {
            RecordingSubscriber subscriber = new RecordingSubscriber();
            EmittingSubscription<Object> subscription = subscriptionFor(subscriber);

            AtomicReference<Throwable> emitFailure = new AtomicReference<>();
            CountDownLatch startSignal = new CountDownLatch(1);

            Thread emitter = new Thread(() -> {
                awaitQuietly(startSignal);
                try {
                    subscription.request(demandPerRound);
                } catch (Throwable t) {
                    emitFailure.set(t);
                }
            });
            Thread canceller = new Thread(() -> {
                awaitQuietly(startSignal);
                subscription.cancel();
            });

            emitter.start();
            canceller.start();
            startSignal.countDown();
            emitter.join();
            canceller.join();

            assertThat(emitFailure.get())
                .withFailMessage("cancel() racing the emit loop threw: %s", emitFailure.get())
                .isNull();
        }
    }

    private static EmittingSubscription<Object> subscriptionFor(Subscriber<Object> subscriber) {
        return EmittingSubscription.<Object>builder()
                                   .downstreamSubscriber(subscriber)
                                   .onCancel(() -> { })
                                   .supplier(Object::new)
                                   .build();
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static final class RecordingSubscriber implements Subscriber<Object> {
        private final AtomicInteger onNextCount = new AtomicInteger();
        private final AtomicReference<Throwable> onError = new AtomicReference<>();

        @Override
        public void onSubscribe(Subscription s) {
        }

        @Override
        public void onNext(Object item) {
            onNextCount.incrementAndGet();
        }

        @Override
        public void onError(Throwable t) {
            onError.compareAndSet(null, t);
        }

        @Override
        public void onComplete() {
        }
    }
}
