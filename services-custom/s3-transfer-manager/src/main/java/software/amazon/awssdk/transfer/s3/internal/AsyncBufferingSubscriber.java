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

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.async.DemandIgnoringSubscription;
import software.amazon.awssdk.utils.async.StoringSubscriber;

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
    private final AtomicBoolean isDelivering = new AtomicBoolean(false);
    private volatile boolean isStreamingDone;
    private Subscription subscription;

    private final StoringSubscriber<T> storingSubscriber;

    public AsyncBufferingSubscriber(Function<T, CompletableFuture<?>> consumer,
                                    CompletableFuture<Void> returnFuture,
                                    int maxConcurrentExecutions) {
        this.returnFuture = returnFuture;
        this.consumer = consumer;
        this.maxConcurrentExecutions = maxConcurrentExecutions;
        this.numRequestsInFlight = new AtomicInteger(0);
        this.storingSubscriber = new StoringSubscriber<>(Integer.MAX_VALUE);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        Validate.paramNotNull(subscription, "subscription");
        if (this.subscription != null) {
            log.warn(() -> "The subscriber has already been subscribed. Cancelling the incoming subscription");
            subscription.cancel();
            return;
        }
        storingSubscriber.onSubscribe(new DemandIgnoringSubscription(subscription));
        this.subscription = subscription;
        subscription.request(maxConcurrentExecutions);
    }

    @Override
    public void onNext(T item) {
        storingSubscriber.onNext(item);
        flushBufferIfNeeded();
    }

    private void flushBufferIfNeeded() {
        if (isDelivering.compareAndSet(false, true)) {
            try {
                Optional<StoringSubscriber.Event<T>> next = storingSubscriber.peek();
                while (numRequestsInFlight.get() < maxConcurrentExecutions) {
                    if (!next.isPresent()) {
                        subscription.request(1);
                        break;
                    }

                    switch (next.get().type()) {
                        case ON_COMPLETE:
                            handleCompleteEvent();
                            break;
                        case ON_ERROR:
                            handleError(next.get().runtimeError());
                            break;
                        case ON_NEXT:
                            handleOnNext(next.get().value());
                            break;
                        default:
                            handleError(new IllegalStateException("Unknown stored type: " + next.get().type()));
                            break;
                    }

                    next = storingSubscriber.peek();
                }
            } finally {
                isDelivering.set(false);
            }
        }
    }

    private void handleOnNext(T item) {
        storingSubscriber.poll();

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

    private void handleCompleteEvent() {
        if (numRequestsInFlight.get() == 0) {
            returnFuture.complete(null);
            storingSubscriber.poll();
        }
    }

    @Override
    public void onError(Throwable t) {
        handleError(t);
        storingSubscriber.onError(t);
    }

    private void handleError(Throwable t) {
        returnFuture.completeExceptionally(t);
        storingSubscriber.poll();
    }

    @Override
    public void onComplete() {
        isStreamingDone = true;
        storingSubscriber.onComplete();
        flushBufferIfNeeded();
    }

    /**
     * @return the number of requests that are currently in flight
     */
    public int numRequestsInFlight() {
        return numRequestsInFlight.get();
    }
}
