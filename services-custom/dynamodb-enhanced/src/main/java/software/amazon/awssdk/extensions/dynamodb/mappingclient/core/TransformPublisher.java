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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.core;

import java.util.function.Function;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.SdkPublisher;

// TODO: Consider moving to SDK core
@SdkInternalApi
public class TransformPublisher<T, R> implements SdkPublisher<R> {
    private final SdkPublisher<T> wrappedPublisher;
    private final Function<T, R> transformFunction;

    private TransformPublisher(SdkPublisher<T> wrappedPublisher, Function<T, R> transformFunction) {
        this.wrappedPublisher = wrappedPublisher;
        this.transformFunction = transformFunction;
    }

    public static <T, R> TransformPublisher<T, R> of(SdkPublisher<T> publisher, Function<T, R> transformFunction) {
        return new TransformPublisher<>(publisher, transformFunction);
    }

    @Override
    public void subscribe(Subscriber<? super R> subscriber) {
        wrappedPublisher.subscribe(new TransformSubscriber(subscriber));
    }

    private final class TransformSubscriber implements Subscriber<T> {
        private final Subscriber<? super R> delegateSubscriber;

        private TransformSubscriber(Subscriber<? super R> delegateSubscriber) {
            this.delegateSubscriber = delegateSubscriber;
        }


        @Override
        public void onSubscribe(Subscription subscription) {
            delegateSubscriber.onSubscribe(subscription);
        }

        @Override
        public void onNext(T t) {
            delegateSubscriber.onNext(transformFunction.apply(t));
        }

        @Override
        public void onError(Throwable throwable) {
            delegateSubscriber.onError(throwable);
        }

        @Override
        public void onComplete() {
            delegateSubscriber.onComplete();
        }
    }
}
