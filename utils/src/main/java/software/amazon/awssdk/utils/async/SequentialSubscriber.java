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

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * A simple implementation of {@link Subscriber} that requests data one at a time.
 *
 * @param <T> Type of data requested
 */
@SdkProtectedApi
public class SequentialSubscriber<T> implements Subscriber<T> {

    private final Consumer<T> consumer;
    private final CompletableFuture<?> future;
    private Subscription subscription;

    public SequentialSubscriber(Consumer<T> consumer,
                                CompletableFuture<Void> future) {
        this.consumer = consumer;
        this.future = future;
    }

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
    public void onError(Throwable t) {
        future.completeExceptionally(t);
    }

    @Override
    public void onComplete() {
        future.complete(null);
    }
}
