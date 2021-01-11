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
 *
 * Original source licensed under the Apache License 2.0 by playframework.
 */

package software.amazon.awssdk.http.nio.netty.internal.nrs.util;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * This class contains source imported from https://github.com/playframework/netty-reactive-streams,
 * licensed under the Apache License 2.0, available at the time of the fork (1/31/2020) here:
 * https://github.com/playframework/netty-reactive-streams/blob/master/LICENSE.txt
 *
 * All original source licensed under the Apache License 2.0 by playframework. All modifications are
 * licensed under the Apache License 2.0 by Amazon Web Services.
 */
public class SubscriberProbe<T> extends Probe implements Subscriber<T> {

    private final Subscriber<T> subscriber;

    public SubscriberProbe(Subscriber<T> subscriber, String name) {
        super(name);
        this.subscriber = subscriber;
    }

    SubscriberProbe(Subscriber<T> subscriber, String name, long start) {
        super(name, start);
        this.subscriber = subscriber;
    }

    @Override
    public void onSubscribe(final Subscription s) {
        String sName = s == null ? "null" : s.getClass().getName();
        log("invoke onSubscribe with subscription " + sName);
        subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {
                log("invoke request " + n);
                s.request(n);
                log("finish request");
            }

            @Override
            public void cancel() {
                log("invoke cancel");
                s.cancel();
                log("finish cancel");
            }
        });
        log("finish onSubscribe");
    }

    @Override
    public void onNext(T t) {
        log("invoke onNext with message " + t);
        subscriber.onNext(t);
        log("finish onNext");
    }

    @Override
    public void onError(Throwable t) {
        String tName = t == null ? "null" : t.getClass().getName();
        log("invoke onError with " + tName);
        subscriber.onError(t);
        log("finish onError");
    }

    @Override
    public void onComplete() {
        log("invoke onComplete");
        subscriber.onComplete();
        log("finish onComplete");
    }
}
