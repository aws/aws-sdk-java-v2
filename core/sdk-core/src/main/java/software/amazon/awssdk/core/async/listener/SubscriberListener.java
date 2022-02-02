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

package software.amazon.awssdk.core.async.listener;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * Listener interface that invokes callbacks associated with a {@link Subscriber}.
 *
 * @see AsyncResponseTransformerListener
 * @see PublisherListener
 */
@SdkProtectedApi
public interface SubscriberListener<T> {
    /**
     * Invoked before {@link Subscriber#onNext(Object)}
     */
    default void subscriberOnNext(T t) {
    }

    /**
     * Invoked before {@link Subscriber#onComplete()}
     */
    default void subscriberOnComplete() {
    }

    /**
     * Invoked before {@link Subscriber#onError(Throwable)}
     */
    default void subscriberOnError(Throwable t) {
    }

    /**
     * Invoked before {@link Subscription#cancel()}
     */
    default void subscriptionCancel() {
    }

    /**
     * Wrap a {@link Subscriber} with a new one that will notify a {@link SubscriberListener} of important events occurring.
     */
    static <T> Subscriber<T> wrap(Subscriber<? super T> delegate, SubscriberListener<? super T> listener) {
        return new NotifyingSubscriber<>(delegate, listener);
    }

    @SdkInternalApi
    final class NotifyingSubscriber<T> implements Subscriber<T> {
        private static final Logger log = Logger.loggerFor(NotifyingSubscriber.class);

        private final Subscriber<? super T> delegate;
        private final SubscriberListener<? super T> listener;

        NotifyingSubscriber(Subscriber<? super T> delegate,
                            SubscriberListener<? super T> listener) {
            this.delegate = Validate.notNull(delegate, "delegate");
            this.listener = Validate.notNull(listener, "listener");
        }

        @Override
        public void onSubscribe(Subscription s) {
            delegate.onSubscribe(new NotifyingSubscription(s));
        }

        @Override
        public void onNext(T t) {
            invoke(() -> listener.subscriberOnNext(t), "subscriberOnNext");
            delegate.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            invoke(() -> listener.subscriberOnError(t), "subscriberOnError");
            delegate.onError(t);
        }

        @Override
        public void onComplete() {
            invoke(listener::subscriberOnComplete, "subscriberOnComplete");
            delegate.onComplete();
        }

        static void invoke(Runnable runnable, String callbackName) {
            try {
                runnable.run();
            } catch (Exception e) {
                log.error(() -> callbackName + " callback failed. This exception will be dropped.", e);
            }
        }

        @SdkInternalApi
        final class NotifyingSubscription implements Subscription {
            private final Subscription delegateSubscription;

            NotifyingSubscription(Subscription delegateSubscription) {
                this.delegateSubscription = Validate.notNull(delegateSubscription, "delegateSubscription");
            }

            @Override
            public void request(long n) {
                delegateSubscription.request(n);
            }

            @Override
            public void cancel() {
                invoke(listener::subscriptionCancel, "subscriptionCancel");
                delegateSubscription.cancel();
            }
        }
    }
}
