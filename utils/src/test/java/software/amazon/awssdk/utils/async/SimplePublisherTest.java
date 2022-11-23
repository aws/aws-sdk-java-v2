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
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.async.StoringSubscriber.Event;
import software.amazon.awssdk.utils.async.StoringSubscriber.EventType;

public class SimplePublisherTest {
    /**
     * This class has tests that try to break things for a fixed period of time, and then make sure nothing broke.
     * This flag controls how long those tests run. Longer values provider a better guarantee of catching an issue, but
     * increase the build time. 5 seconds seems okay for now, but if a flaky test is found try increasing the duration to make
     * it reproduce more reliably.
     */
    private static final Duration STOCHASTIC_TEST_DURATION = Duration.ofSeconds(5);

    @Test
    public void immediateSuccessWorks() {
        SimplePublisher<Integer> publisher = new SimplePublisher<>();
        StoringSubscriber<Integer> subscriber = new StoringSubscriber<>(1);
        publisher.subscribe(subscriber);
        publisher.complete();

        assertThat(subscriber.poll().get().type()).isEqualTo(EventType.ON_COMPLETE);
        assertThat(subscriber.poll()).isNotPresent();
    }

    @Test
    public void immediateFailureWorks() {
        RuntimeException error = new RuntimeException();

        SimplePublisher<Integer> publisher = new SimplePublisher<>();
        StoringSubscriber<Integer> subscriber = new StoringSubscriber<>(1);
        publisher.subscribe(subscriber);
        publisher.error(error);

        assertThat(subscriber.peek().get().type()).isEqualTo(EventType.ON_ERROR);
        assertThat(subscriber.peek().get().runtimeError()).isEqualTo(error);

        subscriber.poll();

        assertThat(subscriber.poll()).isNotPresent();
    }

    @Test
    public void writeAfterCompleteFails() {
        SimplePublisher<Integer> publisher = new SimplePublisher<>();
        publisher.subscribe(new StoringSubscriber<>(1));
        publisher.complete();
        assertThat(publisher.send(5)).isCompletedExceptionally();
    }

    @Test
    public void writeAfterErrorFails() {
        SimplePublisher<Integer> publisher = new SimplePublisher<>();
        publisher.subscribe(new StoringSubscriber<>(1));
        publisher.error(new Throwable());
        assertThat(publisher.send(5)).isCompletedExceptionally();
    }

    @Test
    public void completeAfterCompleteFails() {
        SimplePublisher<Integer> publisher = new SimplePublisher<>();
        publisher.subscribe(new StoringSubscriber<>(1));
        publisher.complete();
        assertThat(publisher.complete()).isCompletedExceptionally();
    }

    @Test
    public void completeAfterErrorFails() {
        SimplePublisher<Integer> publisher = new SimplePublisher<>();
        publisher.subscribe(new StoringSubscriber<>(1));
        publisher.error(new Throwable());
        assertThat(publisher.complete()).isCompletedExceptionally();
    }

    @Test
    public void errorAfterCompleteFails() {
        SimplePublisher<Integer> publisher = new SimplePublisher<>();
        publisher.subscribe(new StoringSubscriber<>(1));
        publisher.complete();
        assertThat(publisher.error(new Throwable())).isCompletedExceptionally();
    }

    @Test
    public void errorAfterErrorFails() {
        SimplePublisher<Integer> publisher = new SimplePublisher<>();
        publisher.subscribe(new StoringSubscriber<>(1));
        publisher.error(new Throwable());
        assertThat(publisher.error(new Throwable())).isCompletedExceptionally();
    }

    @Test
    public void oneDemandWorks() {
        SimplePublisher<Integer> publisher = new SimplePublisher<>();
        StoringSubscriber<Integer> subscriber = new StoringSubscriber<>(1);
        publisher.subscribe(subscriber);

        publisher.send(1);
        publisher.send(2);
        publisher.complete();

        assertThat(subscriber.peek().get().type()).isEqualTo(EventType.ON_NEXT);
        assertThat(subscriber.peek().get().value()).isEqualTo(1);

        subscriber.poll();

        assertThat(subscriber.peek().get().type()).isEqualTo(EventType.ON_NEXT);
        assertThat(subscriber.peek().get().value()).isEqualTo(2);

        subscriber.poll();

        assertThat(subscriber.poll().get().type()).isEqualTo(EventType.ON_COMPLETE);
        assertThat(subscriber.poll()).isNotPresent();
    }

    @Test
    public void highDemandWorks() {
        SimplePublisher<Integer> publisher = new SimplePublisher<>();
        ControllableSubscriber<Integer> subscriber = new ControllableSubscriber<>();
        publisher.subscribe(subscriber);
        subscriber.subscription.request(Long.MAX_VALUE);

        publisher.send(1);
        subscriber.subscription.request(Long.MAX_VALUE);
        publisher.send(2);
        subscriber.subscription.request(Long.MAX_VALUE);
        publisher.complete();
        subscriber.subscription.request(Long.MAX_VALUE);

        assertThat(subscriber.eventQueue.peek().get().type()).isEqualTo(EventType.ON_NEXT);
        assertThat(subscriber.eventQueue.peek().get().value()).isEqualTo(1);

        subscriber.eventQueue.poll();

        assertThat(subscriber.eventQueue.peek().get().type()).isEqualTo(EventType.ON_NEXT);
        assertThat(subscriber.eventQueue.peek().get().value()).isEqualTo(2);

        subscriber.eventQueue.poll();

        assertThat(subscriber.eventQueue.peek().get().type()).isEqualTo(EventType.ON_COMPLETE);

        subscriber.eventQueue.poll();

        assertThat(subscriber.eventQueue.poll()).isNotPresent();
    }

    @Test
    public void writeFuturesDoNotCompleteUntilAfterOnNext() {
        SimplePublisher<Integer> publisher = new SimplePublisher<>();
        ControllableSubscriber<Integer> subscriber = new ControllableSubscriber<>();
        publisher.subscribe(subscriber);

        CompletableFuture<Void> writeFuture = publisher.send(5);

        assertThat(subscriber.eventQueue.peek()).isNotPresent();
        assertThat(writeFuture).isNotCompleted();

        subscriber.subscription.request(1);

        assertThat(subscriber.eventQueue.peek().get().type()).isEqualTo(EventType.ON_NEXT);
        assertThat(subscriber.eventQueue.peek().get().value()).isEqualTo(5);
        assertThat(writeFuture).isCompletedWithValue(null);
    }

    @Test
    public void completeFuturesDoNotCompleteUntilAfterOnComplete() {
        SimplePublisher<Integer> publisher = new SimplePublisher<>();
        ControllableSubscriber<Integer> subscriber = new ControllableSubscriber<>();

        publisher.subscribe(subscriber);
        publisher.send(5);
        CompletableFuture<Void> completeFuture = publisher.complete();

        assertThat(subscriber.eventQueue.peek()).isNotPresent();
        assertThat(completeFuture).isNotCompleted();

        subscriber.subscription.request(1);
        subscriber.eventQueue.poll(); // Drop the 5 value

        assertThat(subscriber.eventQueue.peek().get().type()).isEqualTo(EventType.ON_COMPLETE);
        assertThat(completeFuture).isCompletedWithValue(null);
    }

    @Test
    public void errorFuturesDoNotCompleteUntilAfterOnError() {
        RuntimeException error = new RuntimeException();

        SimplePublisher<Integer> publisher = new SimplePublisher<>();
        ControllableSubscriber<Integer> subscriber = new ControllableSubscriber<>();

        publisher.subscribe(subscriber);
        publisher.send(5);
        CompletableFuture<Void> errorFuture = publisher.error(error);

        assertThat(subscriber.eventQueue.peek()).isNotPresent();
        assertThat(errorFuture).isNotCompleted();

        subscriber.subscription.request(1);
        subscriber.eventQueue.poll(); // Drop the 5 value

        assertThat(subscriber.eventQueue.peek().get().type()).isEqualTo(EventType.ON_ERROR);
        assertThat(subscriber.eventQueue.peek().get().runtimeError()).isEqualTo(error);
        assertThat(errorFuture).isCompletedWithValue(null);
    }

    @Test
    public void completeBeforeSubscribeIsDeliveredOnSubscribe() {
        SimplePublisher<Integer> publisher = new SimplePublisher<>();
        StoringSubscriber<Integer> subscriber = new StoringSubscriber<>(Integer.MAX_VALUE);

        publisher.complete();
        publisher.subscribe(subscriber);
        assertThat(subscriber.peek().get().type()).isEqualTo(EventType.ON_COMPLETE);
    }

    @Test
    public void errorBeforeSubscribeIsDeliveredOnSubscribe() {
        SimplePublisher<Integer> publisher = new SimplePublisher<>();
        StoringSubscriber<Integer> subscriber = new StoringSubscriber<>(Integer.MAX_VALUE);

        RuntimeException error = new RuntimeException();
        publisher.error(error);
        publisher.subscribe(subscriber);
        assertThat(subscriber.peek().get().type()).isEqualTo(EventType.ON_ERROR);
        assertThat(subscriber.peek().get().runtimeError()).isEqualTo(error);
    }

    @Test
    public void writeBeforeSubscribeIsDeliveredOnSubscribe() {
        SimplePublisher<Integer> publisher = new SimplePublisher<>();
        StoringSubscriber<Integer> subscriber = new StoringSubscriber<>(Integer.MAX_VALUE);

        publisher.send(5);
        publisher.subscribe(subscriber);
        assertThat(subscriber.peek().get().type()).isEqualTo(EventType.ON_NEXT);
        assertThat(subscriber.peek().get().value()).isEqualTo(5);
    }

    @Test
    public void cancelFailsAnyInFlightFutures() {
        SimplePublisher<Integer> publisher = new SimplePublisher<>();
        ControllableSubscriber<Integer> subscriber = new ControllableSubscriber<>();

        publisher.subscribe(subscriber);
        CompletableFuture<Void> writeFuture = publisher.send(5);
        CompletableFuture<Void> completeFuture = publisher.complete();

        subscriber.subscription.cancel();

        assertThat(writeFuture).isCompletedExceptionally();
        assertThat(completeFuture).isCompletedExceptionally();
    }

    @Test
    public void newCallsAfterCancelFail() {
        SimplePublisher<Integer> publisher = new SimplePublisher<>();
        ControllableSubscriber<Integer> subscriber = new ControllableSubscriber<>();

        publisher.subscribe(subscriber);
        subscriber.subscription.cancel();

        assertThat(publisher.send(5)).isCompletedExceptionally();
        assertThat(publisher.complete()).isCompletedExceptionally();
        assertThat(publisher.error(new Throwable())).isCompletedExceptionally();
    }

    @Test
    public void negativeDemandSkipsOutstandingMessages() {
        SimplePublisher<Integer> publisher = new SimplePublisher<>();
        ControllableSubscriber<Integer> subscriber = new ControllableSubscriber<>();

        publisher.subscribe(subscriber);
        CompletableFuture<Void> sendFuture = publisher.send(0);
        CompletableFuture<Void> completeFuture = publisher.complete();
        subscriber.subscription.request(-1);

        assertThat(sendFuture).isCompletedExceptionally();
        assertThat(completeFuture).isCompletedExceptionally();
        assertThat(subscriber.eventQueue.poll().get().type()).isEqualTo(EventType.ON_ERROR);
    }

    @Test
    public void evilDownstreamPublisherThrowingInOnNextStillCancelsInFlightFutures() {
        SimplePublisher<Integer> publisher = new SimplePublisher<>();
        ControllableSubscriber<Integer> subscriber = new ControllableSubscriber<>();
        subscriber.failureInOnNext = new RuntimeException();

        CompletableFuture<Void> writeFuture = publisher.send(5);
        CompletableFuture<Void> completeFuture = publisher.complete();

        publisher.subscribe(subscriber);
        subscriber.subscription.request(1);

        assertThat(writeFuture).isCompletedExceptionally();
        assertThat(completeFuture).isCompletedExceptionally();
    }

    @Test
    public void stochastic_onNext_singleProducerSeemsThreadSafe() throws Exception {
        // Single-producer is interesting because we can validate the ordering of messages, unlike with multi-producer.
        seemsThreadSafeWithProducerCount(1);
    }

    @Test
    public void stochastic_onNext_multiProducerSeemsThreadSafe() throws Exception {
        seemsThreadSafeWithProducerCount(3);
    }

    @Test
    public void stochastic_completeAndError_seemThreadSafe() throws Exception {
        assertTimeoutPreemptively(STOCHASTIC_TEST_DURATION.plusSeconds(5), () -> {
            Instant start = Instant.now();
            Instant end = start.plus(STOCHASTIC_TEST_DURATION);

            ExecutorService executor = Executors.newCachedThreadPool();

            while (end.isAfter(Instant.now())) {
                SimplePublisher<Integer> publisher = new SimplePublisher<>();
                ControllableSubscriber<Integer> subscriber = new ControllableSubscriber<>();
                publisher.subscribe(subscriber);
                subscriber.subscription.request(1);

                AtomicBoolean scenarioStart = new AtomicBoolean(false);
                CountDownLatch allAreWaiting = new CountDownLatch(3);

                Runnable waitForStart = () -> {
                    allAreWaiting.countDown();
                    while (!scenarioStart.get()) {
                        Thread.yield();
                    }
                };

                Future<?> writeCall = executor.submit(() -> {
                    waitForStart.run();
                    publisher.send(0).join();
                });

                Future<?> completeCall = executor.submit(() -> {
                    waitForStart.run();
                    publisher.complete().join();
                });

                Future<?> errorCall = executor.submit(() -> {
                    Throwable t = new Throwable();
                    waitForStart.run();
                    publisher.error(t).join();
                });

                allAreWaiting.await();
                scenarioStart.set(true);

                List<Pair<String, Throwable>> failures = new ArrayList<>();
                addIfFailed(failures, "write", writeCall);
                boolean writeSucceeded = failures.isEmpty();

                addIfFailed(failures, "complete", completeCall);
                addIfFailed(failures, "error", errorCall);

                int expectedFailures = writeSucceeded ? 1 : 2;
                assertThat(failures).hasSize(expectedFailures);
            }
        });
    }

    private void addIfFailed(List<Pair<String, Throwable>> failures, String callName, Future<?> call) {
        try {
            call.get();
        } catch (Throwable t) {
            failures.add(Pair.of(callName, t));
        }
    }

    private void seemsThreadSafeWithProducerCount(int producerCount) {
        assertTimeoutPreemptively(STOCHASTIC_TEST_DURATION.plusSeconds(5), () -> {
            AtomicBoolean runProducers = new AtomicBoolean(true);
            AtomicBoolean runConsumers = new AtomicBoolean(true);
            AtomicInteger completesReceived = new AtomicInteger(0);

            AtomicLong messageSendCount = new AtomicLong(0);
            AtomicLong messageReceiveCount = new AtomicLong(0);

            Semaphore productionLimiter = new Semaphore(101);
            Semaphore requestLimiter = new Semaphore(57);
            ExecutorService executor = Executors.newFixedThreadPool(2 + producerCount);

            SimplePublisher<Long> publisher = new SimplePublisher<>();
            ControllableSubscriber<Long> subscriber = new ControllableSubscriber<>();
            publisher.subscribe(subscriber);

            // Producer tasks
            CompletableFuture<?> completed = new CompletableFuture<>();
            List<Future<?>> producers = new ArrayList<>();
            for (int i = 0; i < producerCount; i++) {
                producers.add(executor.submit(() -> {
                    while (runProducers.get()) {
                        productionLimiter.acquire();
                        publisher.send(messageSendCount.getAndIncrement());
                    }
                    publisher.complete().thenRun(() -> completed.complete(null)); // All but one producer sending this will fail.
                    return null;
                }));
            }

            // Requester Task
            Future<?> requester = executor.submit(() -> {
                while (runConsumers.get()) {
                    requestLimiter.acquire();
                    subscriber.subscription.request(1);
                }
                return null;
            });

            // Consumer Task
            Future<?> consumer = executor.submit(() -> {
                int expectedEvent = 0;
                while (runConsumers.get() || subscriber.eventQueue.peek().isPresent()) {
                    Optional<Event<Long>> event = subscriber.eventQueue.poll();

                    if (!event.isPresent()) {
                        continue;
                    }

                    // When we only have 1 producer, we can verify the messages are in order.
                    if (producerCount == 1 && event.get().type() == EventType.ON_NEXT) {
                        assertThat(event.get().value()).isEqualTo(expectedEvent);
                        expectedEvent++;
                    }

                    if (event.get().type() == EventType.ON_NEXT) {
                        messageReceiveCount.incrementAndGet();
                        productionLimiter.release();
                        requestLimiter.release();
                    }

                    if (event.get().type() == EventType.ON_COMPLETE) {
                        completesReceived.incrementAndGet();
                    }
                }
            });

            Thread.sleep(STOCHASTIC_TEST_DURATION.toMillis());

            // Shut down producers
            runProducers.set(false);
            productionLimiter.release(producerCount);
            for (Future<?> producer : producers) {
                producer.get();
            }

            // Make sure to flush out everything left in the queue.
            completed.get();
            subscriber.subscription.request(Long.MAX_VALUE);

            // Shut down consumers
            runConsumers.set(false);
            requestLimiter.release();
            requester.get();
            consumer.get();

            assertThat(messageReceiveCount.get()).isEqualTo(messageSendCount.get());
            assertThat(completesReceived.get()).isEqualTo(1);

            // Make sure we actually tested something
            assertThat(messageSendCount.get()).isGreaterThan(10);
        });
    }

    private class ControllableSubscriber<T> implements Subscriber<T> {
        private final StoringSubscriber<T> eventQueue = new StoringSubscriber<>(Integer.MAX_VALUE);
        private Subscription subscription;
        private RuntimeException failureInOnNext;

        @Override
        public void onSubscribe(Subscription s) {
            this.subscription = new ControllableSubscription(s);

            // Give the event queue a subscription we just ignore. We are the captain of the subscription!
            eventQueue.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                }

                @Override
                public void cancel() {
                }
            });
        }

        @Override
        public void onNext(T o) {
            if (failureInOnNext != null) {
                throw failureInOnNext;
            }
            eventQueue.onNext(o);
        }

        @Override
        public void onError(Throwable t) {
            eventQueue.onError(t);
        }

        @Override
        public void onComplete() {
            eventQueue.onComplete();
        }

        private class ControllableSubscription implements Subscription {
            private final Subscription delegate;

            private ControllableSubscription(Subscription s) {
                delegate = s;
            }

            @Override
            public void request(long n) {
                delegate.request(n);
            }

            @Override
            public void cancel() {
                delegate.cancel();
            }
        }
    }

}