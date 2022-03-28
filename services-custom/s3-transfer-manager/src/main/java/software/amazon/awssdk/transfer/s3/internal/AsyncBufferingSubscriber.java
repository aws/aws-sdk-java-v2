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

package software.amazon.awssdk.transfer.s3.internal;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * An implementation of {@link Subscriber} that execute the provided function for every event and limits the number of concurrent
 * function execution to the given {@code maxConcurrentRequests}
 *
 * @param <T> Type of data requested
 */
@SdkInternalApi
public class AsyncBufferingSubscriber<T> implements Subscriber<T> {
    private static final Logger log = Logger.loggerFor(AsyncBufferingSubscriber.class);
    private static final Object COMPLETE_EVENT = new Object();
    private final Queue<Object> buffer;
    private final CompletableFuture<?> returnFuture;
    private final Function<T, CompletableFuture<?>> consumer;
    private final int maxConcurrentExecutions;
    private final AtomicInteger numRequestsInFlight;
    private final AtomicBoolean isDelivering = new AtomicBoolean(false);
    private volatile boolean isStreamingDone;
    private volatile Subscription subscription;

    public AsyncBufferingSubscriber(Function<T, CompletableFuture<?>> consumer,
                                    CompletableFuture<Void> returnFuture,
                                    int maxConcurrentExecutions) {
        this.buffer = new ConcurrentLinkedQueue<>();
        this.returnFuture = returnFuture;
        this.consumer = consumer;
        this.maxConcurrentExecutions = maxConcurrentExecutions;
        this.numRequestsInFlight = new AtomicInteger(0);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        Validate.paramNotNull(subscription, "subscription");
        if (this.subscription != null) {
            log.warn(() -> "The subscriber has already been subscribed. Cancelling the incoming subscription");
            subscription.cancel();
            return;
        }
        this.subscription = subscription;
        subscription.request(maxConcurrentExecutions);
    }

    @Override
    public void onNext(T item) {
        if (item == null) {
            subscription.cancel();
            NullPointerException exception = new NullPointerException("Item must not be null");
            returnFuture.completeExceptionally(exception);
            throw exception;
        }

        try {
            buffer.add(item);
            flushBufferIfNeeded();
        } catch (Exception e) {
            isStreamingDone = true;
            subscription.cancel();
            returnFuture.completeExceptionally(e);
        }
    }

    private void flushBufferIfNeeded() {
        if (buffer.isEmpty()) {
            if (isStreamingDone && numRequestsInFlight.get() == 0) {
                returnFuture.complete(null);
            } else {
                subscription.request(1);
            }
            return;
        }

        if (isDelivering.compareAndSet(false, true)) {
            try {
                Object firstEvent = buffer.peek();
                if (isCompleteEvent(firstEvent)) {
                    Object event = buffer.poll();
                    handleCompleteEvent(event);
                    return;
                }

                while (!buffer.isEmpty() && numRequestsInFlight.get() < maxConcurrentExecutions) {
                    Object item = buffer.poll();
                    if (item == null) {
                        break;
                    }

                    if (isCompleteEvent(item)) {
                        handleCompleteEvent(item);
                        return;
                    }

                    deliverItem((T) item);
                }
            } finally {
                isDelivering.set(false);
            }
        }
    }

    private void deliverItem(T item) {
        int numberOfRequestInFlight = numRequestsInFlight.incrementAndGet();
        log.debug(() -> "Delivering next item, numRequestInFlight=" + numberOfRequestInFlight);

        consumer.apply(item).whenComplete((r, t) -> {
            numRequestsInFlight.decrementAndGet();
            if (!isStreamingDone) {
                subscription.request(1);
            } else {
                flushBufferIfNeeded();
            }
        });
    }

    private void handleCompleteEvent(Object event) {
        isStreamingDone = true;
        if (numRequestsInFlight.get() == 0) {
            returnFuture.complete(null);
        }
    }

    @Override
    public void onError(Throwable t) {
        handleError(t);
    }

    private void handleError(Throwable t) {
        returnFuture.completeExceptionally(t);
        buffer.clear();
    }

    @Override
    public void onComplete() {
        buffer.add(COMPLETE_EVENT);
        flushBufferIfNeeded();
    }

    /**
     * @return the number of requests that are currently in flight
     */
    public int numRequestsInFlight() {
        return numRequestsInFlight.get();
    }

    private static boolean isCompleteEvent(Object event) {
        return COMPLETE_EVENT.equals(event);
    }
}
