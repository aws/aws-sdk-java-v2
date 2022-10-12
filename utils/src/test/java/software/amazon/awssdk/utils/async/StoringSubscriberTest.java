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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.utils.async.StoringSubscriber.Event;
import software.amazon.awssdk.utils.async.StoringSubscriber.EventType;

public class StoringSubscriberTest {
    @Test
    public void constructorCalled_withNonPositiveSize_throwsException() {
        assertThatCode(() -> new StoringSubscriber<>(1)).doesNotThrowAnyException();
        assertThatCode(() -> new StoringSubscriber<>(Integer.MAX_VALUE)).doesNotThrowAnyException();

        assertThatThrownBy(() -> new StoringSubscriber<>(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new StoringSubscriber<>(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new StoringSubscriber<>(Integer.MIN_VALUE)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void doesNotStoreMoreThanMaxElements() {
        StoringSubscriber<Integer> subscriber = new StoringSubscriber<>(2);
        Subscription subscription = mock(Subscription.class);

        subscriber.onSubscribe(subscription);
        verify(subscription).request(2);

        subscriber.onNext(0);
        subscriber.onNext(0);
        subscriber.peek();
        verifyNoMoreInteractions(subscription);

        subscriber.poll();
        subscriber.poll();
        verify(subscription, times(2)).request(1);

        assertThat(subscriber.peek()).isNotPresent();
        verifyNoMoreInteractions(subscription);
    }

    @Test
    public void returnsEmptyEventWithOutstandingDemand() {
        StoringSubscriber<Integer> subscriber = new StoringSubscriber<>(2);
        subscriber.onSubscribe(mock(Subscription.class));
        assertThat(subscriber.peek()).isNotPresent();
    }

    @Test
    public void returnsCompleteOnComplete() {
        StoringSubscriber<Integer> subscriber = new StoringSubscriber<>(2);
        subscriber.onSubscribe(mock(Subscription.class));
        subscriber.onComplete();
        assertThat(subscriber.peek().get().type()).isEqualTo(EventType.ON_COMPLETE);
    }

    @Test
    public void returnsErrorOnError() {
        RuntimeException error = new RuntimeException();
        StoringSubscriber<Integer> subscriber = new StoringSubscriber<>(2);
        subscriber.onSubscribe(mock(Subscription.class));
        subscriber.onError(error);
        assertThat(subscriber.peek().get().type()).isEqualTo(EventType.ON_ERROR);
        assertThat(subscriber.peek().get().runtimeError()).isEqualTo(error);
    }

    @Test
    public void errorWrapsCheckedExceptions() {
        Exception error = new Exception();
        StoringSubscriber<Integer> subscriber = new StoringSubscriber<>(2);
        subscriber.onSubscribe(mock(Subscription.class));
        subscriber.onError(error);
        assertThat(subscriber.peek().get().type()).isEqualTo(EventType.ON_ERROR);
        assertThat(subscriber.peek().get().runtimeError()).hasCause(error);
    }

    @Test
    public void deliversMessagesInTheCorrectOrder() {
        StoringSubscriber<Integer> subscriber = new StoringSubscriber<>(2);
        Subscription subscription = mock(Subscription.class);

        subscriber.onSubscribe(subscription);
        subscriber.onNext(1);
        subscriber.onNext(2);
        subscriber.onComplete();

        assertThat(subscriber.peek().get().type()).isEqualTo(EventType.ON_NEXT);
        assertThat(subscriber.peek().get().value()).isEqualTo(1);
        subscriber.poll();

        assertThat(subscriber.peek().get().type()).isEqualTo(EventType.ON_NEXT);
        assertThat(subscriber.peek().get().value()).isEqualTo(2);
        subscriber.poll();

        assertThat(subscriber.peek().get().type()).isEqualTo(EventType.ON_COMPLETE);
        subscriber.poll();

        assertThat(subscriber.peek()).isNotPresent();
    }

    @Test
    @Timeout(30)
    public void stochastic_subscriberSeemsThreadSafe() throws Throwable {
        ExecutorService producer = Executors.newFixedThreadPool(1);
        ExecutorService consumer = Executors.newFixedThreadPool(1);
        try {
            StoringSubscriber<Integer> subscriber = new StoringSubscriber<>(10);

            AtomicBoolean testRunning = new AtomicBoolean(true);
            AtomicInteger messageNumber = new AtomicInteger(0);

            AtomicReference<Throwable> producerFailure = new AtomicReference<>();
            Subscription subscription = new Subscription() {
                @Override
                public void request(long n) {
                    producer.submit(() -> {
                        try {
                            for (int i = 0; i < n; i++) {
                                subscriber.onNext(messageNumber.getAndIncrement());
                            }
                        } catch (Throwable t) {
                            producerFailure.set(t);
                        }
                    });
                }

                @Override
                public void cancel() {
                    producerFailure.set(new AssertionError("Cancel not expected."));
                }
            };

            subscriber.onSubscribe(subscription);

            Future<Object> consumerFuture = consumer.submit(() -> {
                int expectedMessageNumber = 0;
                while (testRunning.get()) {
                    Thread.sleep(1);

                    Optional<Event<Integer>> current = subscriber.peek();
                    Optional<Event<Integer>> current2 = subscriber.peek();

                    if (current.isPresent()) {
                        assertThat(current.get()).isSameAs(current2.get());
                        Event<Integer> event = current.get();

                        assertThat(event.type()).isEqualTo(EventType.ON_NEXT);
                        assertThat(event.value()).isEqualTo(expectedMessageNumber);
                        expectedMessageNumber++;
                    }

                    subscriber.poll();
                }
                return null;
            });

            Thread.sleep(5_000);
            testRunning.set(false);
            consumerFuture.get();
            if (producerFailure.get() != null) {
                throw producerFailure.get();
            }
            assertThat(messageNumber.get()).isGreaterThan(10); // ensure we actually tested something
        } finally {
            producer.shutdownNow();
            consumer.shutdownNow();
        }
    }
}