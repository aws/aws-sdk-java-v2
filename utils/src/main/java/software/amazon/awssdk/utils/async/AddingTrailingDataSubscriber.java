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

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * Allows to send trailing data before invoking onComplete on the downstream subscriber.
 * trailingDataIterable will be created when the upstream subscriber has called onComplete.
 */
@SdkProtectedApi
public class AddingTrailingDataSubscriber<T> extends DelegatingSubscriber<T, T> {
    private static final Logger log = Logger.loggerFor(AddingTrailingDataSubscriber.class);

    /**
     * The subscription to the upstream subscriber.
     */
    private Subscription upstreamSubscription;

    /**
     * The amount of unfulfilled demand the downstream subscriber has opened against us.
     */
    private final AtomicLong downstreamDemand = new AtomicLong(0);

    /**
     * Whether the upstream subscriber has called onComplete on us.
     */
    private volatile boolean onCompleteCalledByUpstream = false;

    /**
     * Whether the upstream subscriber has called onError on us.
     */
    private volatile boolean onErrorCalledByUpstream = false;

    /**
     * Whether we have called onComplete on the downstream subscriber.
     */
    private volatile boolean onCompleteCalledOnDownstream = false;

    private final Supplier<Iterable<T>> trailingDataIterableSupplier;
    private Iterator<T> trailingDataIterator;

    public AddingTrailingDataSubscriber(Subscriber<? super T> subscriber,
                                        Supplier<Iterable<T>> trailingDataIterableSupplier) {
        super(Validate.paramNotNull(subscriber, "subscriber"));
        this.trailingDataIterableSupplier = Validate.paramNotNull(trailingDataIterableSupplier, "trailingDataIterableSupplier");
    }

    @Override
    public void onSubscribe(Subscription subscription) {

        if (upstreamSubscription != null) {
            log.warn(() -> "Received duplicate subscription, cancelling the duplicate.", new IllegalStateException());
            subscription.cancel();
            return;
        }

        upstreamSubscription = subscription;

        subscriber.onSubscribe(new Subscription() {

            @Override
            public void request(long l) {
                if (onErrorCalledByUpstream || onCompleteCalledOnDownstream) {
                    return;
                }

                addDownstreamDemand(l);

                if (onCompleteCalledByUpstream) {
                    sendTrailingDataAndCompleteIfNeeded();
                    return;
                }
                upstreamSubscription.request(l);
            }

            @Override
            public void cancel() {
                upstreamSubscription.cancel();
            }
        });
    }

    @Override
    public void onError(Throwable throwable) {
        onErrorCalledByUpstream = true;
        subscriber.onError(throwable);
    }

    @Override
    public void onNext(T t) {
        Validate.paramNotNull(t, "item");
        downstreamDemand.decrementAndGet();
        subscriber.onNext(t);
    }

    @Override
    public void onComplete() {
        onCompleteCalledByUpstream = true;
        sendTrailingDataAndCompleteIfNeeded();
    }

    private void addDownstreamDemand(long l) {

        if (l > 0) {
            downstreamDemand.getAndUpdate(current -> {
                long newValue = current + l;
                return newValue >= 0 ? newValue : Long.MAX_VALUE;
            });
        } else {
            upstreamSubscription.cancel();
            onError(new IllegalArgumentException("Demand must not be negative"));
        }
    }

    private synchronized void sendTrailingDataAndCompleteIfNeeded() {
        if (onCompleteCalledOnDownstream) {
            return;
        }

        if (trailingDataIterator == null) {
            Iterable<T> supplier = trailingDataIterableSupplier.get();
            if (supplier == null) {
                completeDownstreamSubscriber();
                return;
            }

            trailingDataIterator = supplier.iterator();
        }

        sendTrailingDataIfNeeded();

        if (!trailingDataIterator.hasNext()) {
            completeDownstreamSubscriber();
        }
    }

    private void sendTrailingDataIfNeeded() {
        long demand = downstreamDemand.get();

        while (trailingDataIterator.hasNext() && demand > 0) {
            subscriber.onNext(trailingDataIterator.next());
            demand = downstreamDemand.decrementAndGet();
        }
    }

    private void completeDownstreamSubscriber() {
        subscriber.onComplete();
        onCompleteCalledOnDownstream = true;
    }
}
