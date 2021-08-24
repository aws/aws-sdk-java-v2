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

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.Logger;

@SdkProtectedApi
public class FlatteningSubscriber<U> extends DelegatingSubscriber<Iterable<U>, U> {
    private static final Logger log = Logger.loggerFor(FlatteningSubscriber.class);

    private final AtomicLong demand = new AtomicLong(0);
    private final Object lock = new Object();

    private boolean requestedNextBatch = false;
    private boolean onCompleteCalledByUpstream = false;
    private boolean terminalCallMadeDownstream = false;

    private Subscription sourceSubscription;
    private Queue<U> currentBatch;

    public FlatteningSubscriber(Subscriber<? super U> subscriber) {
        super(subscriber);
        currentBatch = new LinkedList<>();
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        if (sourceSubscription != null) {
            Exception failure = new IllegalStateException("Duplicate subscription.");
            log.warn(() -> "Received duplicate subscription, cancelling the duplicate.", failure);
            subscription.cancel();
        }

        sourceSubscription = subscription;
        subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long l) {
                synchronized (lock) {
                    demand.addAndGet(l);
                    // Execution goes into `if` block only once for the initial request
                    // After that requestedNextBatch is always true and more requests are made in fulfillDemand()
                    if (!requestedNextBatch) {
                        requestedNextBatch = true;
                        sourceSubscription.request(1);
                    } else {
                        fulfillDemand();
                    }
                }
            }

            @Override
            public void cancel() {
                subscription.cancel();
            }
        });
    }

    @Override
    public void onNext(Iterable<U> nextItems) {
        synchronized (lock) {
            currentBatch = StreamSupport.stream(nextItems.spliterator(), false)
                                        .collect(Collectors.toCollection(LinkedList::new));
            fulfillDemand();
        }
    }

    private void fulfillDemand() {
        if (terminalCallMadeDownstream) {
            return;
        }

        while (demand.get() > 0 && !currentBatch.isEmpty()) {
            demand.decrementAndGet();
            subscriber.onNext(currentBatch.poll());
        }

        if (onCompleteCalledByUpstream && currentBatch.isEmpty()) {
            subscriber.onComplete();
        } else if (currentBatch.isEmpty() && demand.get() > 0) {
            requestedNextBatch = true;
            sourceSubscription.request(1);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        synchronized (lock) {
            subscriber.onError(throwable);
            terminalCallMadeDownstream = true;
        }
    }

    @Override
    public void onComplete() {
        synchronized (lock) {
            onCompleteCalledByUpstream = true;
            if (currentBatch.isEmpty()) {
                subscriber.onComplete();
                terminalCallMadeDownstream = true;
            }
        }
    }
}
