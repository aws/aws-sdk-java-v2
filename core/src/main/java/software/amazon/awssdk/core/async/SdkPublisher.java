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
import software.amazon.awssdk.core.async.internal.BufferingSubscriber;
import software.amazon.awssdk.core.async.internal.DelegatingSubscriber;
import software.amazon.awssdk.core.async.internal.FilteringSubscriber;
import software.amazon.awssdk.core.async.internal.FlatteningSubscriber;
import software.amazon.awssdk.core.async.internal.LimitingSubscriber;
import software.amazon.awssdk.core.async.internal.SequentialSubscriber;

/**
 * Interface that is implemented by the Async auto-paginated responses.
 */
public interface SdkPublisher<T> extends Publisher<T> {

    // TODO remove
    default CompletableFuture<Void> forEach(Consumer<T> consumer) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        subscribe(new SequentialSubscriber<>(consumer, future));
        return future;
    }

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

    default <U extends T> SdkPublisher<U> filter(Class<U> clzz) {
        return filter(clzz::isInstance).map(clzz::cast);
    }

    default SdkPublisher<T> filter(Predicate<T> predicate) {
        return subscriber -> subscribe(new FilteringSubscriber<>(subscriber, predicate));
    }

    default <U> SdkPublisher<U> map(Function<T, U> mapper) {
        return subscriber -> subscribe(new DelegatingSubscriber<T, U>(subscriber) {
            @Override
            public void onNext(T t) {
                subscriber.onNext(mapper.apply(t));
            }
        });
    }

    default <U> SdkPublisher<U> flatMap(Function<T, List<U>> mapper) {
        return subscriber -> map(mapper).subscribe(new FlatteningSubscriber<>(subscriber));
    }

    default SdkPublisher<List<T>> buffer(int bufferSize) {
        return subscriber -> subscribe(new BufferingSubscriber<>(subscriber, bufferSize));
    }

    default SdkPublisher<T> limit(int limit) {
        return subscriber -> subscribe(new LimitingSubscriber<>(subscriber, limit));
    }

    default CompletableFuture<Void> subscribe(Consumer<T> consumer) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        subscribe(new SequentialSubscriber<>(consumer, future));
        return future;
    }

}
