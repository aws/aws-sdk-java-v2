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

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
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
    private volatile boolean onErrorInvoked;
    private volatile Subscription subscription;

    private final Set<CompletableFuture<?>> requestsInFlight;

    public AsyncBufferingSubscriber(Function<T, CompletableFuture<?>> consumer,
                                    CompletableFuture<Void> returnFuture,
                                    int maxConcurrentExecutions) {
        this.returnFuture = returnFuture;
        this.consumer = consumer;
        this.maxConcurrentExecutions = maxConcurrentExecutions;
        this.numRequestsInFlight = new AtomicInteger(0);
        this.requestsInFlight = ConcurrentHashMap.newKeySet();

        returnFuture.whenComplete((r, t) -> {
            if (t != null) {
                requestsInFlight.forEach(f -> f.cancel(true));
                // Skip cancelling when the failure came from onError: upstream has already terminated, and cancelling here
                // would call Subscription::cancel from within onError (Reactive Streams rule 2.3). Still cancel on an
                // external abort.
                if (!onErrorInvoked) {
                    synchronized (this) {
                        if (subscription != null) {
                            subscription.cancel();
                        }
                    }
                }
            }
        });
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
        // Reactive Streams rule 2.13: onNext must throw NullPointerException on a null element.
        Validate.paramNotNull(item, "item");
        numRequestsInFlight.incrementAndGet();
        CompletableFuture<?> currentRequest;

        try {
            currentRequest = consumer.apply(item);
        } catch (Throwable t) {
            synchronized (this) {
                subscription.cancel();
            }
            onError(t);
            return;
        }

        requestsInFlight.add(currentRequest);

        // When returnFuture completes exceptionally, the cancel handler iterates requestsInFlight and cancels every future in
        // it. That iteration only happens once. If it already ran before we added currentRequest to the set, currentRequest
        // was missed. This check ensures we cancel it ourselves in that case.
        if (returnFuture.isCompletedExceptionally()) {
            currentRequest.cancel(true);
        }
        currentRequest.whenComplete((r, t) -> {
            checkForCompletion(numRequestsInFlight.decrementAndGet());
            requestsInFlight.remove(currentRequest);
            synchronized (this) {
                subscription.request(1);
            }
        });
    }

    @Override
    public void onError(Throwable t) {
        // Set before completing the future: completeExceptionally may run the whenComplete handler synchronously, and it
        // must see this flag to avoid cancelling the subscription from within onError (see constructor).
        onErrorInvoked = true;
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
