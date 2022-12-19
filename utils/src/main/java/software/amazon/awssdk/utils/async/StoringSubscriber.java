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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.Validate;

/**
 * An implementation of {@link Subscriber} that stores the events it receives for retrieval.
 *
 * <p>Events can be observed via {@link #peek()} and {@link #poll()}. The number of events stored is limited by the
 * {@code maxElements} configured at construction.
 */
@SdkProtectedApi
public class StoringSubscriber<T> implements Subscriber<T> {
    /**
     * The maximum number of events that can be stored in this subscriber. The number of events in {@link #events} may be
     * slightly higher once {@link #onComplete()} and {@link #onError(Throwable)} events are added.
     */
    private final int maxEvents;

    /**
     * The events stored in this subscriber. The maximum size of this queue is approximately {@link #maxEvents}.
     */
    private final Queue<Event<T>> events;

    /**
     * The active subscription. Set when {@link #onSubscribe(Subscription)} is invoked.
     */
    private Subscription subscription;

    /**
     * Create a subscriber that stores up to {@code maxElements} events for retrieval.
     */
    public StoringSubscriber(int maxEvents) {
        Validate.isPositive(maxEvents, "Max elements must be positive.");
        this.maxEvents = maxEvents;
        this.events = new ConcurrentLinkedQueue<>();
    }

    /**
     * Check the first event stored in this subscriber.
     *
     * <p>This will return empty if no events are currently available (outstanding demand has not yet
     * been filled).
     */
    public Optional<Event<T>> peek() {
        return Optional.ofNullable(events.peek());
    }

    /**
     * Remove and return the first event stored in this subscriber.
     *
     * <p>This will return empty if no events are currently available (outstanding demand has not yet
     * been filled).
     */
    public Optional<Event<T>> poll() {
        Event<T> result = events.poll();
        if (result != null) {
            subscription.request(1);
            return Optional.of(result);
        }
        return Optional.empty();
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        if (this.subscription != null) {
            subscription.cancel();
        }

        this.subscription = subscription;
        subscription.request(maxEvents);
    }

    @Override
    public void onNext(T t) {
        Validate.notNull(t, "onNext(null) is not allowed.");

        try {
            events.add(Event.value(t));
        } catch (RuntimeException e) {
            subscription.cancel();
            onError(new IllegalStateException("Failed to store element.", e));
        }
    }

    @Override
    public void onComplete() {
        events.add(Event.complete());
    }

    @Override
    public void onError(Throwable throwable) {
        events.add(Event.error(throwable));
    }

    /**
     * An event stored for later retrieval by this subscriber.
     *
     * <p>Stored events are one of the follow {@link #type()}s:
     * <ul>
     *     <li>{@code VALUE} - A value received by {@link #onNext(Object)}, available via {@link #value()}.</li>
     *     <li>{@code COMPLETE} - Indicating {@link #onComplete()} was called.</li>
     *     <li>{@code ERROR} - Indicating {@link #onError(Throwable)} was called. The exception is available via
     *     {@link #runtimeError()}</li>
     *     <li>{@code EMPTY} - Indicating that no events remain in the queue (but more from upstream may be given later).</li>
     * </ul>
     */
    public static final class Event<T> {
        private final EventType type;
        private final T value;
        private final Throwable error;

        private Event(EventType type, T value, Throwable error) {
            this.type = type;
            this.value = value;
            this.error = error;
        }

        private static <T> Event<T> complete() {
            return new Event<>(EventType.ON_COMPLETE, null, null);
        }

        private static <T> Event<T> error(Throwable error) {
            return new Event<>(EventType.ON_ERROR, null, error);
        }

        private static <T> Event<T> value(T value) {
            return new Event<>(EventType.ON_NEXT, value, null);
        }

        /**
         * Retrieve the {@link EventType} of this event.
         */
        public EventType type() {
            return type;
        }

        /**
         * The value stored in this {@code VALUE} type. Null for all other event types.
         */
        public T value() {
            return value;
        }

        /**
         * The error stored in this {@code ERROR} type. Null for all other event types. If a checked exception was received via
         * {@link #onError(Throwable)}, this will return a {@code RuntimeException} with the checked exception as its cause.
         */
        public RuntimeException runtimeError() {
            if (type != EventType.ON_ERROR) {
                return null;
            }

            if (error instanceof RuntimeException) {
                return (RuntimeException) error;
            }

            if (error instanceof IOException) {
                return new UncheckedIOException((IOException) error);
            }

            return new RuntimeException(error);
        }
    }

    public enum EventType {
        ON_NEXT,
        ON_COMPLETE,
        ON_ERROR
    }
}
