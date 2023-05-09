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

import java.util.concurrent.CompletableFuture;
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
    private final CompletableFuture<?> returnFuture;
    private final Function<T, CompletableFuture<?>> consumer;
    private final int maxConcurrentExecutions;
    private final AtomicInteger numRequestsInFlight;
    private volatile boolean upstreamDone;
    private Subscription subscription;

    public AsyncBufferingSubscriber(Function<T, CompletableFuture<?>> consumer,
                                    CompletableFuture<Void> returnFuture,
                                    int maxConcurrentExecutions) {
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
        numRequestsInFlight.incrementAndGet();
        consumer.apply(item).whenComplete((r, t) -> {
            checkForCompletion(numRequestsInFlight.decrementAndGet());
            synchronized (this) {
                subscription.request(1);
            }
        });
    }

    @Override
    public void onError(Throwable t) {
        // Need to complete future exceptionally first to prevent
        // accidental successful completion by a concurrent checkForCompletion.
        returnFuture.completeExceptionally(t);
        upstreamDone = true;
    }

    @Override
    public void onComplete() {
        upstreamDone = true;
        checkForCompletion(numRequestsInFlight.get());
    }

    private void checkForCompletion(int requestsInFlight) {
        if (upstreamDone && requestsInFlight == 0) {
            // This could get invoked multiple times, but it doesn't matter
            // because future.complete is idempotent.
            returnFuture.complete(null);
        }
    }

    /**
     * @return the number of requests that are currently in flight
     */
    public int numRequestsInFlight() {
        return numRequestsInFlight.get();
    }
}
