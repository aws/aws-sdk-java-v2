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

package software.amazon.awssdk.utils.async;

import java.util.function.Predicate;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public class FilteringSubscriber<T> extends DelegatingSubscriber<T, T> {

    private final Predicate<T> predicate;

    private Subscription subscription;

    public FilteringSubscriber(Subscriber<? super T> sourceSubscriber, Predicate<T> predicate) {
        super(sourceSubscriber);
        this.predicate = predicate;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        super.onSubscribe(subscription);
        this.subscription = subscription;
    }

    @Override
    public void onNext(T t) {
        if (predicate.test(t)) {
            subscriber.onNext(t);
        } else {
            // Consumed a demand but didn't deliver. Request other to make up for it
            subscription.request(1);
        }
    }
}
