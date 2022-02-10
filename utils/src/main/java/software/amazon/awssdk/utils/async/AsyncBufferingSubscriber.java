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

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * An implementation of {@link Subscriber} that delivers data to async consumer and limits the number of concurrent requests by
 * only requesting data if the number of inflight request is less than the configured maxConcurrentRequests.
 *
 * @param <T> Type of data requested
 */
@SdkProtectedApi
public class AsyncBufferingSubscriber<T> implements Subscriber<T> {
    private static final Logger log = Logger.loggerFor(AsyncBufferingSubscriber.class);
    private final Event<T> completeEvent = new CompleteEvent<>();
    private final Queue<Event<T>> buffer;
    private final CompletableFuture<?> future;
    private final Function<T, CompletableFuture<?>> consumer;
    private final int maxConcurrentRequests;
    private final AtomicInteger numRequestsInFlight;
    private final AtomicBoolean isDelivering = new AtomicBoolean(false);
    private volatile boolean isStreamingDone;
    private Subscription subscription;
    private final List<CompletableFuture<?>> futures;

    public AsyncBufferingSubscriber(Function<T, CompletableFuture<?>> consumer,
                                    CompletableFuture<Void> future,
                                    int maxConcurrentRequests) {
        this.buffer = new ConcurrentLinkedQueue<>();
        this.future = future;
        this.consumer = consumer;
        this.maxConcurrentRequests = maxConcurrentRequests;
        this.numRequestsInFlight = new AtomicInteger(0);
        this.futures = new ArrayList<>();
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        Validate.paramNotNull(subscription, "subscription");
        if (this.subscription != null) {
            subscription.cancel();
            return;
        }
        this.subscription = subscription;
        subscription.request(1);
    }

    @Override
    public void onNext(T item) {
        if (item == null) {
            subscription.cancel();
            throw new NullPointerException("Item must not be null");
        }

        try {
            buffer.add(new DataEvent<>(item));
            flushBuffer();
        } catch (Exception e) {
            isStreamingDone = true;
            subscription.cancel();
            future.completeExceptionally(e);
        }
    }

    private void deliverItem(T item) {
        log.debug(() -> "Delivering next item, numRequestInFlight=" + numRequestsInFlight);
        numRequestsInFlight.incrementAndGet();
        consumer.apply(item).whenComplete((r, t) -> {
            numRequestsInFlight.decrementAndGet();
            if (!isStreamingDone) {
                subscription.request(1);
            } else {
                flushBuffer();
            }
        });
    }

    private void flushBuffer() {
        if (buffer.isEmpty()) {
            if (isStreamingDone && numRequestsInFlight.get() == 0) {
                future.complete(null);
            } else {
                subscription.request(1);
            }
            return;
        }

        if (isDelivering.compareAndSet(false, true)) {
            Event<T> firstEvent = buffer.peek();
            if (firstEvent != null && isCompleteEvent(firstEvent)) {
                Event<T> event = buffer.poll();
                handleCompleteEvent(event);
                isDelivering.set(false);
                return;
            }

            while (!buffer.isEmpty() && numRequestsInFlight.get() < maxConcurrentRequests) {
                Event<T> event = buffer.poll();
                if (event == null) {
                    break;
                }

                if (isCompleteEvent(event)) {
                    handleCompleteEvent(event);
                    isDelivering.set(false);
                    return;
                }

                DataEvent<T> dataEvent = (DataEvent<T>) event;
                deliverItem(dataEvent.data);
            }
            isDelivering.set(false);
        }
    }

    private void handleCompleteEvent(Event<T> event) {
        isStreamingDone = true;
        if (numRequestsInFlight.get() == 0) {
            future.complete(null);
        }
    }

    @Override
    public void onError(Throwable t) {
        handleError(t);
    }

    private void handleError(Throwable t) {
        future.completeExceptionally(t);
        buffer.clear();
    }

    @Override
    public void onComplete() {
        buffer.add(completeEvent);
        flushBuffer();
    }

    private boolean isCompleteEvent(Event<T> event) {
        return event.type() == EventType.COMPLETE;
    }

    private enum EventType {
        DATA,
        COMPLETE
    }
    
    private interface Event<T> {
        EventType type();
    }

    private static final class DataEvent<T> implements Event<T> {
        private final T data;

        DataEvent(T data) {
            this.data = data;
        }

        @Override
        public EventType type() {
            return EventType.DATA;
        }

        public T data() {
            return data;
        }
    }

    private static final class CompleteEvent<T> implements Event<T> {

        @Override
        public EventType type() {
            return EventType.COMPLETE;
        }
    }
}
