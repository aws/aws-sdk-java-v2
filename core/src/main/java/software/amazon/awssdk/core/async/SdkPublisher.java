/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.async;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.async.internal.BufferingSubscriber;
import software.amazon.awssdk.core.async.internal.DelegatingSubscriber;
import software.amazon.awssdk.core.async.internal.FilteringSubscriber;
import software.amazon.awssdk.core.async.internal.FlatteningSubscriber;
import software.amazon.awssdk.core.async.internal.LimitingSubscriber;
import software.amazon.awssdk.core.async.internal.SequentialSubscriber;

/**
 * Interface that is implemented by the Async auto-paginated responses.
 */
@SdkPublicApi
public interface SdkPublisher<T> extends Publisher<T> {

    /**
     * Adapts a {@link Publisher} to {@link SdkPublisher}.
     *
     * @param toAdapt {@link Publisher} to adapt.
     * @param <T> Type of object being published.
     * @return SdkPublisher
     */
    static <T> SdkPublisher<T> adapt(Publisher<T> toAdapt) {
        return toAdapt::subscribe;
    }

    /**
     * Filters published events to just those that are instances of the given class. This changes the type of
     * publisher to the type specified in the {@link Class}.
     *
     * @param clzz Class to filter to. Includes subtypes of the class.
     * @param <U> Type of class to filter to.
     * @return New publisher, filtered to the given class.
     */
    default <U extends T> SdkPublisher<U> filter(Class<U> clzz) {
        return filter(clzz::isInstance).map(clzz::cast);
    }

    /**
     * Filters published events to just those that match the given predicate. Unlike {@link #filter(Class)}, this method
     * does not change the type of the {@link Publisher}.
     *
     * @param predicate Predicate to match events.
     * @return New publisher, filtered to just the events that match the predicate.
     */
    default SdkPublisher<T> filter(Predicate<T> predicate) {
        return subscriber -> subscribe(new FilteringSubscriber<>(subscriber, predicate));
    }

    /**
     * Perform a mapping on the published events. Returns a new publisher of the mapped events. Typically this method will
     * change the type of the publisher.
     *
     * @param mapper Mapping function to apply.
     * @param <U> Type being mapped to.
     * @return New publisher with events mapped according to the given function.
     */
    default <U> SdkPublisher<U> map(Function<T, U> mapper) {
        return subscriber -> subscribe(new DelegatingSubscriber<T, U>(subscriber) {
            @Override
            public void onNext(T t) {
                subscriber.onNext(mapper.apply(t));
            }
        });
    }

    /**
     * Performs a mapping on the published events and creates a new publisher that emits the mapped events one by one.
     *
     * @param mapper Mapping function that produces an {@link Iterable} of new events to be flattened.
     * @param <U> Type of flattened event being mapped to.
     * @return New publisher of flattened events.
     */
    default <U> SdkPublisher<U> flatMapIterable(Function<T, Iterable<U>> mapper) {
        return subscriber -> map(mapper).subscribe(new FlatteningSubscriber<>(subscriber));
    }

    /**
     * Buffers the events into lists of the given buffer size. Note that the last batch of events may be less than
     * the buffer size.
     *
     * @param bufferSize Number of events to buffer before delivering downstream.
     * @return New publisher of buffered events.
     */
    default SdkPublisher<List<T>> buffer(int bufferSize) {
        return subscriber -> subscribe(new BufferingSubscriber<>(subscriber, bufferSize));
    }

    /**
     * Limit the number of published events and cancel the subscription after that limit has been reached. The limit
     * may never be reached if the downstream publisher doesn't have many events to publish.
     *
     * @param limit Number of events to publish.
     * @return New publisher that will only publish up to the specified number of events.
     */
    default SdkPublisher<T> limit(int limit) {
        return subscriber -> subscribe(new LimitingSubscriber<>(subscriber, limit));
    }

    /**
     * Subscribes to the publisher with the given {@link Consumer}. This consumer will be called for each event
     * published. There is no backpressure using this method if the Consumer dispatches processing asynchronously. If more
     * control over backpressure is required, consider using {@link #subscribe(Subscriber)}.
     *
     * @param consumer Consumer to process event.
     * @return CompletableFuture that will be notified when all events have been consumed or if an error occurs.
     */
    default CompletableFuture<Void> subscribe(Consumer<T> consumer) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        subscribe(new SequentialSubscriber<>(consumer, future));
        return future;
    }

}
