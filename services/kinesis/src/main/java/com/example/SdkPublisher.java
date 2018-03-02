/*
 * Copyright 2012-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package com.example;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.util.Throwables;

public interface SdkPublisher<T> extends Publisher<T> {

    default CompletableFuture<Void> forEach(Consumer<T> consumer) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        this.subscribe(new Subscriber<T>() {
            private Subscription subscription;

            @Override
            public void onSubscribe(Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(T t) {
                consumer.accept(t);
                subscription.request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                future.completeExceptionally(throwable);
            }

            @Override
            public void onComplete() {
                future.complete(null);
            }
        });
        return future;
    }

    default Iterator<T> toBlocking() {
        SubscriberIterator<T> subscriberIterator = new SubscriberIterator<>(new LinkedList<>(), 128);
        subscribe(subscriberIterator);
        return subscriberIterator;
    }

    final class SubscriberIterator<T> implements Subscriber<T>, Iterator<T> {

        final Queue<T> queue;

        final long batchSize;

        final long limit;

        final Lock lock;

        final Condition condition;

        long produced;

        volatile Subscription s;
        @SuppressWarnings("rawtypes")
        static final AtomicReferenceFieldUpdater<SubscriberIterator, Subscription> S =
            AtomicReferenceFieldUpdater.newUpdater(SubscriberIterator.class, Subscription.class, "s");

        volatile boolean done;
        Throwable error;

        volatile boolean cancelled;

        public SubscriberIterator(Queue<T> queue, long batchSize) {
            this.queue = queue;
            this.batchSize = batchSize;
            this.limit = batchSize - (batchSize >> 2);
            this.lock = new ReentrantLock();
            this.condition = lock.newCondition();
        }

        @Override
        public boolean hasNext() {
            for (; ; ) {
                if (cancelled) {
                    return false;
                }
                boolean d = done;
                boolean empty = queue.isEmpty();
                if (d) {
                    Throwable e = error;
                    if (e != null) {
                        throw Throwables.failure(e);
                    } else if (empty) {
                        return false;
                    }
                }
                if (empty) {
                    lock.lock();
                    try {
                        while (!cancelled && !done && queue.isEmpty()) {
                            condition.await();
                        }
                    } catch (InterruptedException ex) {
                        run();
                        throw Throwables.failure(ex);
                    } finally {
                        lock.unlock();
                    }
                } else {
                    return true;
                }
            }
        }

        @Override
        public T next() {
            if (hasNext()) {
                T v = queue.poll();

                if (v == null) {
                    run();

                    throw new IllegalStateException("Queue empty?!");
                }

                long p = produced + 1;
                if (p == limit) {
                    produced = 0;
                    s.request(p);
                } else {
                    produced = p;
                }

                return v;
            }
            throw new NoSuchElementException();
        }

        public void run() {
            SubscriptionHelper.terminate(S, this);
            signalConsumer();
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.setOnce(S, this, s)) {
                s.request(batchSize);
            }
        }

        @Override
        public void onNext(T t) {
            if (!queue.offer(t)) {
                SubscriptionHelper.terminate(S, this);

                onError(new IllegalStateException("Queue full?!"));
            } else {
                signalConsumer();
            }
        }

        @Override
        public void onError(Throwable t) {
            error = t;
            done = true;
            signalConsumer();
        }

        @Override
        public void onComplete() {
            done = true;
            signalConsumer();
        }

        void signalConsumer() {
            lock.lock();
            try {
                condition.signalAll();
            } finally {
                lock.unlock();
            }
        }

    }


}
