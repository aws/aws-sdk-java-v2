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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * Allows to send trailing data before invoking onComplete on the downstream subscriber.
 * If the trailingDataSupplier returns null, this class will invoke onComplete directly
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
    private AtomicBoolean onCompleteCalledOnDownstream = new AtomicBoolean(false);

    private final Supplier<T> trailingDataSupplier;
    private volatile T trailingData;

    public AddingTrailingDataSubscriber(Subscriber<? super T> subscriber,
                                        Supplier<T> trailingDataSupplier) {
        super(Validate.paramNotNull(subscriber, "subscriber"));
        this.trailingDataSupplier = Validate.paramNotNull(trailingDataSupplier, "trailingDataSupplier");
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
                if (onErrorCalledByUpstream) {
                    return;
                }

                if (onCompleteCalledByUpstream) {
                    sendTrailingDataIfNeededAndComplete();
                    return;
                }

                addDownstreamDemand(l);
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

        trailingData = trailingDataSupplier.get();
        if (trailingData == null || downstreamDemand.get() > 0) {
            sendTrailingDataIfNeededAndComplete();
        }
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

    private void sendTrailingDataIfNeededAndComplete() {
        if (onCompleteCalledOnDownstream.compareAndSet(false, true)) {
            if (trailingData != null) {
                subscriber.onNext(trailingData);
            }
            subscriber.onComplete();
        }
    }
}
