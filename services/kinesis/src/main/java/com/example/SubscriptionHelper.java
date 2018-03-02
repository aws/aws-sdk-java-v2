/*
 * Copyright 2012-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package com.example;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import org.reactivestreams.Subscription;


/**
 * Utility methods to help working with Subscriptions and their methods.
 */
public class SubscriptionHelper {

    /**
     * A singleton Subscription that represents a cancelled subscription instance and should not be leaked to clients as it
     * represents a terminal state. <br> If algorithms need to hand out a subscription, replace this with {@code
     * EmptySubscription#INSTANCE} because there is no standard way to tell if a Subscription is cancelled or not
     * otherwise.
     *
     * @return a singleton noop {@link Subscription}
     */
    public static Subscription cancelled() {
        return CancelledSubscription.INSTANCE;
    }

    /**
     * Atomically swaps in the single CancelledSubscription instance and returns true
     * if this was the first of such operation on the target field.
     *
     * @param <F> the field type
     * @param field the field accessor
     * @param instance the parent instance of the field
     * @return true if the call triggered the cancellation of the underlying Subscription instance
     */
    public static <F> boolean terminate(AtomicReferenceFieldUpdater<F, Subscription> field, F instance) {
        Subscription a = field.get(instance);
        if (a != SubscriptionHelper.cancelled()) {
            a = field.getAndSet(instance, SubscriptionHelper.cancelled());
            if (a != null && a != SubscriptionHelper.cancelled()) {
                a.cancel();
                return true;
            }
        }
        return false;
    }

    public static <F> boolean set(AtomicReferenceFieldUpdater<F, Subscription> field, F instance, Subscription s) {
        for (; ; ) {
            Subscription a = field.get(instance);
            if (a == SubscriptionHelper.cancelled()) {
                s.cancel();
                return false;
            }
            if (field.compareAndSet(instance, a, s)) {
                if (a != null) {
                    a.cancel();
                }
                return true;
            }
        }
    }

    /**
     * Sets the given subscription once and returns true if successful, false
     * if the field has a subscription already or has been cancelled.
     *
     * @param <F> the instance type containing the field
     * @param field the field accessor
     * @param instance the parent instance
     * @param s the subscription to set once
     * @return true if successful, false if the target was not empty or has been cancelled
     */
    public static <F> boolean setOnce(AtomicReferenceFieldUpdater<F, Subscription> field, F instance, Subscription s) {
        Subscription a = field.get(instance);
        if (a == SubscriptionHelper.cancelled()) {
            s.cancel();
            return false;
        }
        if (a != null) {
            return false;
        }

        if (field.compareAndSet(instance, null, s)) {
            return true;
        }

        a = field.get(instance);

        if (a == SubscriptionHelper.cancelled()) {
            s.cancel();
            return false;
        }

        s.cancel();
        return false;
    }

    enum CancelledSubscription implements Subscription {
        INSTANCE;

        @Override
        public void request(long n) {
            // deliberately no op
        }

        @Override
        public void cancel() {
            // deliberately no op
        }


    }

}
