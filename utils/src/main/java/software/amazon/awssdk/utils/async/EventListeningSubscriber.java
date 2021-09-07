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

import java.util.function.Consumer;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.Logger;

/**
 * A {@link Subscriber} that can invoke callbacks during various parts of the subscriber and subscription lifecycle.
 */
@SdkProtectedApi
public final class EventListeningSubscriber<T> extends DelegatingSubscriber<T, T> {
    private static final Logger log = Logger.loggerFor(EventListeningSubscriber.class);

    private final Runnable afterCompleteListener;
    private final Consumer<Throwable> afterErrorListener;
    private final Runnable afterCancelListener;

    public EventListeningSubscriber(Subscriber<T> subscriber,
                                    Runnable afterCompleteListener,
                                    Consumer<Throwable> afterErrorListener,
                                    Runnable afterCancelListener) {
        super(subscriber);
        this.afterCompleteListener = afterCompleteListener;
        this.afterErrorListener = afterErrorListener;
        this.afterCancelListener = afterCancelListener;
    }

    @Override
    public void onNext(T t) {
        super.subscriber.onNext(t);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        super.onSubscribe(new CancelListeningSubscriber(subscription));
    }

    @Override
    public void onError(Throwable throwable) {
        super.onError(throwable);
        if (afterErrorListener != null) {
            callListener(() -> afterErrorListener.accept(throwable),
                         "Post-onError callback failed. This exception will be dropped.");
        }
    }

    @Override
    public void onComplete() {
        super.onComplete();
        callListener(afterCompleteListener, "Post-onComplete callback failed. This exception will be dropped.");
    }

    private class CancelListeningSubscriber extends DelegatingSubscription {
        protected CancelListeningSubscriber(Subscription s) {
            super(s);
        }

        @Override 
        public void cancel() {
            super.cancel();
            callListener(afterCancelListener, "Post-cancel callback failed. This exception will be dropped.");
        }
    }

    private void callListener(Runnable listener, String listenerFailureMessage) {
        if (listener != null) {
            try {
                listener.run();
            } catch (RuntimeException e) {
                log.error(() -> listenerFailureMessage, e);
            }
        }
    }
}
