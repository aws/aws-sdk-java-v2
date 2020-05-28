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

package utils;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class FakePublisher<T> implements Publisher<T> {
    private Subscriber<? super T> delegateSubscriber;

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        this.delegateSubscriber = subscriber;
        this.delegateSubscriber.onSubscribe(new FakeSubscription());
    }

    public void publish(T str) {
        this.delegateSubscriber.onNext(str);
    }

    public void complete() {
        this.delegateSubscriber.onComplete();
    }

    public void doThrow(Throwable t) {
        this.delegateSubscriber.onError(t);
    }

    private static final class FakeSubscription implements Subscription {
        @Override
        public void request(long n) {

        }

        @Override
        public void cancel() {

        }
    }
}
