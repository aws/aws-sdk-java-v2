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
import software.amazon.awssdk.utils.Logger;

public class ThreadSafeEmittingSubscription<T> implements Subscription {

    private final AtomicBoolean emitting = new AtomicBoolean(false);
    private final Subscriber<T> downstreamSubscriber;
    private final AtomicLong outstandingDemand;
    private final Runnable onCancel;
    private final AtomicBoolean isCancelled;
    private final Supplier<T> supplier;
    private final Logger log;

    public ThreadSafeEmittingSubscription(Subscriber<T> downstreamSubscriber, AtomicLong outstandingDemand,
                                          Runnable onCancel, AtomicBoolean isCancelled, Logger log, Supplier<T> supplier) {
        this.downstreamSubscriber = downstreamSubscriber;
        this.outstandingDemand = outstandingDemand;
        this.onCancel = onCancel;
        this.isCancelled = isCancelled;
        this.log = log;
        this.supplier = supplier;
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
                downstreamSubscriber.onNext(supplier.get());
            }
        }
        return false;
    }

}
