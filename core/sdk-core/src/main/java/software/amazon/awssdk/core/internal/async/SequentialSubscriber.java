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

package software.amazon.awssdk.core.internal.async;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * A simple implementation of {@link Subscriber} that requests data one at a time.
 *
 * @param <T> Type of data requested
 */
@SdkInternalApi
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
        // TODO Update spotbugs version when new version is released and remove this filter the spotbugs-suppressions.xml file
        // SpotBugs incorrectly reports NP_NONNULL_PARAM_VIOLATION when passing null. The fix is not released yet
        // https://github.com/spotbugs/spotbugs/issues/484
        future.complete(null);
    }
}
