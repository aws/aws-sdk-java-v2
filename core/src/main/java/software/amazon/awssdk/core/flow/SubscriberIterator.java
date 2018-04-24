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
package software.amazon.awssdk.core.flow;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.core.util.Throwables;

/**
 * Adapter being a {@link Subscriber} and a traditional blocking {@link Iterator}.
 *
 * @param <T> Type being published/iterated.
 */
@ReviewBeforeRelease("This was copied from RxJava. Take another pass")
public final class SubscriberIterator<T> implements Subscriber<T>, Iterator<T> {

    /**
     * Queue to buffer items and block in the iterator until the next item is available.
     */
    private final Queue<T> queue;

    /**
     * Number of items to request at the start and to keep buffered in the queue.
     */
    private final long batchSize;

    /**
     * Number of items that can be delivered before requesting more.
     */
    private final long limit;

    /**
     * Number of items that have currently been delivered in this batch. Reset to 0 when
     * new items are requested.
     */
    private long produced;

    /**
     * Lock to signal changes in demand.
     */
    private final Lock lock;

    /**
     * Condition to signal changes in demand. Notified when new items arrive, the subscription is completed,
     * or an error occurs.
     */
    private final Condition condition;

    private volatile Subscription s;

    @SuppressWarnings("rawtypes")
    static final AtomicReferenceFieldUpdater<SubscriberIterator, Subscription> S =
        AtomicReferenceFieldUpdater.newUpdater(SubscriberIterator.class, Subscription.class, "s");

    /**
     * Set to true when all items have been published or an error occurs.
     */
    private volatile boolean done;

    /**
     * Set if an error occurs while publishing items.
     */
    private Throwable error;

    SubscriberIterator(Queue<T> queue, long batchSize) {
        this.queue = queue;
        this.batchSize = batchSize;
        this.limit = batchSize - (batchSize >> 2);
        this.lock = new ReentrantLock();
        this.condition = lock.newCondition();
    }

    @Override
    public boolean hasNext() {
        for (; ; ) {
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
                waitForNewItemOrCompletion();
            } else {
                return true;
            }
        }
    }

    @Override
    public T next() {
        if (hasNext()) {
            T nextItem = queue.poll();

            if (nextItem == null) {
                terminate();
                throw new IllegalStateException("Queue empty?!");
            }

            long p = produced + 1;
            if (p == limit) {
                produced = 0;
                s.request(p);
            } else {
                produced = p;
            }

            return nextItem;
        }
        throw new NoSuchElementException();
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

    /**
     * Keep waiting on the condition until a new item is delivered to the queue or
     * the publisher completes (successfully or unsuccessfully).
     */
    private void waitForNewItemOrCompletion() {
        lock.lock();
        try {
            while (!done && queue.isEmpty()) {
                condition.await();
            }
        } catch (InterruptedException ex) {
            terminate();
            // Preserve the interrupt status
            Thread.currentThread().interrupt();
            throw Throwables.failure(ex);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Terminates the {@link Subscription} if an unrecoverable exception occurs in the iterator.
     */
    private void terminate() {
        SubscriptionHelper.terminate(S, this);
        signalConsumer();
    }

    /**
     * Notify the condition that an event has occurred.
     */
    private void signalConsumer() {
        lock.lock();
        try {
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

}
