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

package software.amazon.awssdk.core.internal.async;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.utils.Logger;

/**
 * Subscription which can emit {@link Subscriber#onNext(T)} signals to a subscriber, based on the demand received with the
 * {@link Subscription#request(long)}. It tracks the outstandingDemand that has not yet been fulfilled and used a Supplier
 * passed to it to create the object it needs to emit.
 * @param <T> the type of object to emit to the subscriber.
 */
@SdkInternalApi
@ThreadSafe
public final class EmittingSubscription<T> implements Subscription {
    private static final Logger log = Logger.loggerFor(EmittingSubscription.class);

    private Subscriber<? super T> downstreamSubscriber;
    private final AtomicBoolean emitting;
    private final AtomicLong outstandingDemand;
    private final Runnable onCancel;
    private final AtomicBoolean isCancelled;
    private final Supplier<T> supplier;

    private EmittingSubscription(Builder<T> builder) {
        this.downstreamSubscriber = builder.downstreamSubscriber;
        this.onCancel = builder.onCancel;
        this.supplier = builder.supplier;
        this.isCancelled = new AtomicBoolean();
        this.outstandingDemand = new AtomicLong(0);
        this.emitting = new AtomicBoolean();
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    @Override
    public void request(long n) {
        if (n <= 0) {
            downstreamSubscriber.onError(new IllegalArgumentException("Amount requested must be positive"));
            return;
        }
        long newDemand = outstandingDemand.updateAndGet(current -> {
            if (Long.MAX_VALUE - current < n) {
                return Long.MAX_VALUE;
            }
            return current + n;
        });
        log.trace(() -> String.format("new outstanding demand: %s", newDemand));
        emit();
    }

    @Override
    public void cancel() {
        isCancelled.set(true);
        downstreamSubscriber = null;
        onCancel.run();
    }

    private void emit() {
        do {
            if (!emitting.compareAndSet(false, true)) {
                return;
            }
            try {
                if (doEmit()) {
                    return;
                }
            } finally {
                emitting.compareAndSet(true, false);
            }
        } while (outstandingDemand.get() > 0);
    }

    private boolean doEmit() {
        long demand = outstandingDemand.get();

        while (demand > 0) {
            if (isCancelled.get()) {
                return true;
            }
            if (outstandingDemand.get() > 0) {
                demand = outstandingDemand.decrementAndGet();
                T value;
                try {
                    value = supplier.get();
                } catch (Exception e) {
                    downstreamSubscriber.onError(e);
                    return true;
                }
                downstreamSubscriber.onNext(value);
            }
        }
        return false;
    }

    public static class Builder<T> {
        private Subscriber<? super T> downstreamSubscriber;
        private Runnable onCancel;
        private Supplier<T> supplier;

        public Builder<T> downstreamSubscriber(Subscriber<? super T> subscriber) {
            this.downstreamSubscriber = subscriber;
            return this;
        }

        public Builder<T> onCancel(Runnable onCancel) {
            this.onCancel = onCancel;
            return this;
        }

        public Builder<T> supplier(Supplier<T> supplier) {
            this.supplier = supplier;
            return this;
        }

        public EmittingSubscription<T> build() {
            return new EmittingSubscription<>(this);
        }
    }


}
