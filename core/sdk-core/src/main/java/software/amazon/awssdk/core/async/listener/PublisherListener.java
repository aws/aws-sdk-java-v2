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


import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * Listener interface that invokes callbacks associated with a {@link Publisher} and any resulting {@link Subscriber}.
 *
 * @see AsyncResponseTransformerListener
 * @see SubscriberListener
 */
@SdkProtectedApi
public interface PublisherListener<T> extends SubscriberListener<T> {
    /**
     * Invoked before {@link Publisher#subscribe(Subscriber)}
     */
    default void publisherSubscribe(Subscriber<? super T> subscriber) {
    }

    /**
     * Wrap a {@link SdkPublisher} with a new one that will notify a {@link PublisherListener} of important events occurring.
     */
    static <T> SdkPublisher<T> wrap(SdkPublisher<T> delegate, PublisherListener<T> listener) {
        return new NotifyingPublisher<>(delegate, listener);
    }

    @SdkInternalApi
    final class NotifyingPublisher<T> implements SdkPublisher<T> {
        private static final Logger log = Logger.loggerFor(NotifyingPublisher.class);

        private final SdkPublisher<T> delegate;
        private final PublisherListener<T> listener;

        NotifyingPublisher(SdkPublisher<T> delegate,
                           PublisherListener<T> listener) {
            this.delegate = Validate.notNull(delegate, "delegate");
            this.listener = Validate.notNull(listener, "listener");
        }

        @Override
        public void subscribe(Subscriber<? super T> s) {
            invoke(() -> listener.publisherSubscribe(s), "publisherSubscribe");
            delegate.subscribe(SubscriberListener.wrap(s, listener));
        }

        static void invoke(Runnable runnable, String callbackName) {
            try {
                runnable.run();
            } catch (Exception e) {
                log.error(() -> callbackName + " callback failed. This exception will be dropped.", e);
            }
        }
    }
}
