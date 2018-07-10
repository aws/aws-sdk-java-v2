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

package software.amazon.awssdk.core.pagination.async;

import java.util.concurrent.atomic.AtomicBoolean;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * A NoOp implementation of {@link Subscription} interface.
 *
 * This subscription calls {@link Subscriber#onComplete()} on first request for data and then terminates the subscription.
 */
@SdkProtectedApi
public final class EmptySubscription implements Subscription {

    private final AtomicBoolean isTerminated = new AtomicBoolean(false);
    private final Subscriber subscriber;

    public EmptySubscription(Subscriber subscriber) {
        this.subscriber = subscriber;
    }

    @Override
    public void request(long n) {
        if (isTerminated()) {
            return;
        }

        if (n <= 0) {
            throw new IllegalArgumentException("Non-positive request signals are illegal");
        }

        if (terminate()) {
            subscriber.onComplete();
        }
    }

    @Override
    public void cancel() {
        terminate();
    }

    private boolean terminate() {
        return isTerminated.compareAndSet(false, true);
    }

    private boolean isTerminated() {
        return isTerminated.get();
    }
}
