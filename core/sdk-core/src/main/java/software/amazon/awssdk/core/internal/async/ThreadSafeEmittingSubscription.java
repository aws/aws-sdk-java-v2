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
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.utils.Logger;

@SdkInternalApi
public class ThreadSafeEmittingSubscription<T> implements Subscription {

    private Subscriber<? super T> downstreamSubscriber;
    private final AtomicBoolean emitting = new AtomicBoolean(false);
    private final AtomicLong outstandingDemand;
    private final Runnable onCancel;
    private final AtomicBoolean isCancelled;
    private final Supplier<T> supplier;
    private final Logger log;


    private ThreadSafeEmittingSubscription(Builder<T> builder) {
        this.downstreamSubscriber = builder.downstreamSubscriber;
        this.outstandingDemand = builder.outstandingDemand;
        this.onCancel = builder.onCancel;
        this.isCancelled = builder.isCancelled;
        this.log = builder.log;
        this.supplier = builder.supplier;
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
        private AtomicLong outstandingDemand = new AtomicLong(0);
        private AtomicBoolean isCancelled = new AtomicBoolean(false);
        private Logger log = Logger.loggerFor(ThreadSafeEmittingSubscription.class);
        private Runnable onCancel;
        private Supplier<T> supplier;

        public Builder<T> downstreamSubscriber(Subscriber<? super T> subscriber) {
            this.downstreamSubscriber = subscriber;
            return this;
        }

        public Builder<T> outstandingDemand(AtomicLong outstandingDemand) {
            this.outstandingDemand = outstandingDemand;
            return this;
        }

        public Builder<T> onCancel(Runnable onCancel) {
            this.onCancel = onCancel;
            return this;
        }

        public Builder<T> isCancelled(AtomicBoolean isCancelled) {
            this.isCancelled = isCancelled;
            return this;
        }

        public Builder<T> log(Logger log) {
            this.log = log;
            return this;
        }

        public Builder<T> supplier(Supplier<T> supplier) {
            this.supplier = supplier;
            return this;
        }

        public ThreadSafeEmittingSubscription<T> build() {
            return new ThreadSafeEmittingSubscription<>(this);
        }
    }


}
