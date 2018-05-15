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

package software.amazon.awssdk.core.flow;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;

/**
 * Extension of {@link Publisher} with some convenience methods added.
 *
 * @param <T> Type of object being published.
 */
@ReviewBeforeRelease("Similiar to SdkPublisher. Consider consolidating.")
public interface FlowPublisher<T> extends Publisher<T> {

    /**
     * Requests items from the publisher serially and delivers them to the provided {@link Consumer}. Consumer
     * is called on the async client thread so it's <b>STRONGLY</b> recommended to not make blocking calls.
     *
     * @param consumer Consumer that will process the items being published.
     * @return CompletableFuture that will be completed successful when all items have been consumed or completed
     * exceptionally if an error occurs.
     */
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

    /**
     * Transforms the {@link Publisher} into a blocking {@link Iterator}.
     *
     * @return Blocking {@link Iterator} that can be used to iterate items in a traditionally manner.
     */
    @ReviewBeforeRelease("If we are building sync client on top of async these may no longer be necessary.")
    default Iterator<T> toBlocking() {
        // TODO review implementation choice and batch size
        SubscriberIterator<T> subscriberIterator = new SubscriberIterator<>(new LinkedList<>(), 128);
        subscribe(subscriberIterator);
        return subscriberIterator;
    }

    /**
     * Transforms the {@link Publisher} into a blocking {@link Stream} that can be used to map/filter/collect
     * the items.
     *
     * @return Stream of items.
     */
    default Stream<T> toStream() {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(toBlocking(), Spliterator.ORDERED),
                                    false);
    }

}
