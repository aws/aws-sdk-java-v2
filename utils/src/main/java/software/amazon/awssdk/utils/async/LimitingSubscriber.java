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

import java.util.concurrent.atomic.AtomicInteger;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.internal.async.EmptySubscription;

@SdkProtectedApi
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
        this.subscription = subscription;
        if (limit == 0) {
            subscription.cancel();
            super.onSubscribe(new EmptySubscription(super.subscriber));
        } else {
            super.onSubscribe(subscription);
        }
    }

    @Override
    public void onNext(T t) {
        int deliveredItems = delivered.incrementAndGet();
        // We may get more events even after cancelling so we ignore them.
        if (deliveredItems <= limit) {
            subscriber.onNext(t);
            if (deliveredItems == limit) {
                subscription.cancel();
                subscriber.onComplete();
            }
        }
    }
}
