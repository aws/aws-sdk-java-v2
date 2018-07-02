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

package software.amazon.awssdk.core.async.internal;

import java.util.concurrent.atomic.AtomicInteger;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class LimitingSubscriber<T> extends DelegatingSubscriber<T, T> {

    private final int limit;
    private final AtomicInteger delivered = new AtomicInteger(0);

    private Subscription subscription;

    public LimitingSubscriber(Subscriber<? super T> subscriber, int limit) {
        super(subscriber);
        this.limit = limit;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        super.onSubscribe(subscription);
        this.subscription = subscription;
    }

    @Override
    public void onNext(T t) {
        // We may get more events even after cancelling so we ignore them.
        if (delivered.get() < limit) {
            subscriber.onNext(t);
        }
        // If we've met the limit then we can cancel the subscription
        if (delivered.incrementAndGet() >= limit) {
            subscription.cancel();
        }
    }
}
