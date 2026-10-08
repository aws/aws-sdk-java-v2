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

package software.amazon.awssdk.services.s3.internal.multipart.utils;

import java.util.ArrayDeque;
import java.util.Deque;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.CloseableAsyncRequestBody;

/**
 * A Subscription that mimics {@link software.amazon.awssdk.utils.async.SimplePublisher}: queued signals
 * are delivered synchronously on the thread that calls request(), onNext delivery is gated on demand,
 * and onComplete is delivered once the queue drains, without needing demand.
 */
public final class ControlledSubscription implements Subscription {
    private final Subscriber<? super CloseableAsyncRequestBody> subscriber;
    private final Deque<CloseableAsyncRequestBody> queuedBodies = new ArrayDeque<>();
    private long demand;
    private boolean streamComplete;
    private boolean onCompleteDelivered;
    private boolean draining;

    public ControlledSubscription(Subscriber<? super CloseableAsyncRequestBody> subscriber) {
        this.subscriber = subscriber;
    }

    @Override
    public void request(long n) {
        demand += n;
        drain();
    }

    @Override
    public void cancel() {
    }

    public void enqueueBodyAndDeliver(CloseableAsyncRequestBody body) {
        queuedBodies.add(body);
        drain();
    }

    public void enqueueBodyQuietly(CloseableAsyncRequestBody body) {
        queuedBodies.add(body);
    }

    public void enqueueStreamCompleteQuietly() {
        streamComplete = true;
    }

    private void drain() {
        if (draining) {
            return; // mirrors SimplePublisher's processingQueue flag: no re-entrant delivery
        }
        draining = true;
        try {
            while (demand > 0 && !queuedBodies.isEmpty()) {
                demand--;
                subscriber.onNext(queuedBodies.poll());
            }
            if (streamComplete && queuedBodies.isEmpty() && !onCompleteDelivered) {
                onCompleteDelivered = true;
                subscriber.onComplete();
            }
        } finally {
            draining = false;
        }
    }
}
