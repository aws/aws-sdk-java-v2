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

package software.amazon.awssdk.core.async;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkProtectedApi;

@SdkProtectedApi
public class EmptyPublisher<T> implements Publisher<T> {
    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        subscriber.onSubscribe(new EmptySubscription<>(subscriber));
    }

    private static class EmptySubscription<T> implements Subscription {
        private final Subscriber<T> subscriber;
        private volatile boolean done = false;

        EmptySubscription(Subscriber<T> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void request(long l) {
            if (!done) {
                done = true;
                if (l <= 0) {
                    this.subscriber.onError(new IllegalArgumentException("Demand must be positive"));
                } else {
                    this.subscriber.onComplete();
                }
            }
        }

        @Override
        public void cancel() {
            done = true;
        }
    }
}
