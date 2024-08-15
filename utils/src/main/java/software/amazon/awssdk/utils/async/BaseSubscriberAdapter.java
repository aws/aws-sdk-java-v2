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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.Logger;

/**
 * Base of subscribers that can adapt one type to another. This subscriber will receive onNext signal with the {@code U} type,
 * but will need to {@link BaseSubscriberAdapter#fulfillDownstreamDemand() fulfill the downstream demand} of the delegate
 * subscriber with instance of the {@code T} type.
 *
 * @param <T> the type that the delegate subscriber demands.
 * @param <U> the type sent by the publisher this subscriber is subscribed to.
 */
@SdkProtectedApi
public abstract class BaseSubscriberAdapter<T, U> extends DelegatingSubscriber<T, U> {
    private static final Logger log = Logger.loggerFor(BaseSubscriberAdapter.class);

    /**
     * The amount of unfulfilled demand open against the upstream subscriber.
     */
    protected final AtomicLong upstreamDemand = new AtomicLong(0);

    /**
     * The amount of unfulfilled demand the downstream subscriber has opened against us.
     */
    protected final AtomicLong downstreamDemand = new AtomicLong(0);

    /**
     * A flag that is used to ensure that only one thread is handling updates to the state of this subscriber at a time. This
     * allows us to ensure that the downstream onNext, onComplete and onError are only ever invoked serially.
     */
    protected final AtomicBoolean handlingStateUpdate = new AtomicBoolean(false);

    /**
     * Whether the upstream subscriber has called onError on us. If this is null, we haven't gotten an onError. If it's non-null
     * this will be the exception that the upstream passed to our onError. After we get an onError, we'll call onError on the
     * downstream subscriber as soon as possible.
     */
    protected final AtomicReference<Throwable> onErrorFromUpstream = new AtomicReference<>(null);

    /**
     * Whether we have called onComplete or onNext on the downstream subscriber.
     */
    protected volatile boolean terminalCallMadeDownstream = false;

    /**
     * Whether the upstream subscriber has called onComplete on us. After this happens, we'll drain any outstanding items in the
     * allItems queue and then call onComplete on the downstream subscriber.
     */
    protected volatile boolean onCompleteCalledByUpstream = false;

    /**
     * The subscription to the upstream subscriber.
     */
    protected Subscription upstreamSubscription;

    protected BaseSubscriberAdapter(Subscriber<? super U> subscriber) {
        super(subscriber);
    }

    /**
     * This method is called inside the onNext signal. Implementation should do what is required to store the data
     * before fulfilling the demand from the downstream subscriber.
     *
     * @param item the value with which onNext was called.
     */
    abstract void doWithItem(T item);

    /**
     * This method is called when demand from the downstream subscriber needs to be fulfilled. Called in a loop
     * until {@code downstreamDemand} is no longer needed. Implementations are responsible for decrementing the {@code
     * downstreamDemand} accordingly as demand gets fulfilled.
     */
    protected abstract void fulfillDownstreamDemand();

    @Override
    public void onSubscribe(Subscription subscription) {
        if (upstreamSubscription != null) {
            log.warn(() -> "Received duplicate subscription, cancelling the duplicate.", new IllegalStateException());
            subscription.cancel();
            return;
        }

        upstreamSubscription = subscription;
        subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long l) {
                addDownstreamDemand(l);
                handleStateUpdate();
            }

            @Override
            public void cancel() {
                subscription.cancel();
            }
        });
    }

    @Override
    public void onNext(T item) {
        try {
            doWithItem(item);
        } catch (RuntimeException e) {
            upstreamSubscription.cancel();
            onError(e);
            throw e;
        }

        upstreamDemand.decrementAndGet();
        handleStateUpdate();
    }

    @Override
    public void onError(Throwable throwable) {
        onErrorFromUpstream.compareAndSet(null, throwable);
        handleStateUpdate();
    }

    @Override
    public void onComplete() {
        onCompleteCalledByUpstream = true;
        handleStateUpdate();
    }

    /**
     * Increment the downstream demand by the provided value, accounting for overflow.
     */
    private void addDownstreamDemand(long l) {
        if (l > 0) {
            downstreamDemand.getAndUpdate(current -> {
                long newValue = current + l;
                return newValue >= 0 ? newValue : Long.MAX_VALUE;
            });
        } else {
            log.error(() -> "Demand " + l + " must not be negative.");
            upstreamSubscription.cancel();
            onError(new IllegalArgumentException("Demand must not be negative"));
        }
    }

    /**
     * This is invoked after each downstream request or upstream onNext, onError or onComplete.
     */
    protected void handleStateUpdate() {
        do {
            // Anything that happens after this if statement and before we set handlingStateUpdate to false is guaranteed to only
            // happen on one thread. For that reason, we should only invoke onNext, onComplete or onError within that block.
            if (!handlingStateUpdate.compareAndSet(false, true)) {
                return;
            }

            try {
                // If we've already called onComplete or onError, don't do anything.
                if (terminalCallMadeDownstream) {
                    return;
                }

                // Call onNext, onComplete and onError as needed based on the current subscriber state.
                handleOnNextState();
                handleUpstreamDemandState();
                handleOnCompleteState();
                handleOnErrorState();
            } catch (Error e) {
                throw e;
            } catch (Throwable e) {
                log.error(() -> "Unexpected exception encountered that violates the reactive streams specification. Attempting "
                                + "to terminate gracefully.", e);
                upstreamSubscription.cancel();
                onError(e);
            } finally {
                handlingStateUpdate.set(false);
            }

            // It's possible we had an important state change between when we decided to release the state update flag, and we
            // actually released it. If that seems to have happened, try to handle that state change on this thread, because
            // another thread is not guaranteed to come around and do so.
        } while (onNextNeeded() || upstreamDemandNeeded() || onCompleteNeeded() || onErrorNeeded());
    }

    /**
     * Fulfill downstream demand by flushing
     */
    private void handleOnNextState() {
        while (onNextNeeded() && !onErrorNeeded()) {
            fulfillDownstreamDemand();
        }
    }

    /**
     * Returns true if we need to call onNext downstream. If this is executed outside the handling-state-update condition, the
     * result is subject to change.
     */
    private boolean onNextNeeded() {
        return downstreamDemand.get() > 0 && additionalOnNextNeededCheck();
    }


    /**
     * Can be overridden by subclasses to provide additional checks before calling onNext on downstream.
     */
    boolean additionalOnNextNeededCheck() {
        return true;
    }

    /**
     * Request more upstream demand if it's needed.
     */
    private void handleUpstreamDemandState() {
        if (upstreamDemandNeeded()) {
            ensureUpstreamDemandExists();
        }
    }

    /**
     * Returns true if we need to increase our upstream demand.
     */
    private boolean upstreamDemandNeeded() {
        return upstreamDemand.get() <= 0 && downstreamDemand.get() > 0 && additionalUpstreamDemandNeededCheck();
    }

    /**
     * Can be overridden by subclasses to provide additional checks to see if we need to increase our upstream demand.
     */
    boolean additionalUpstreamDemandNeededCheck() {
        return true;
    }

    /**
     * If there are zero pending items in the queue and the upstream has called onComplete, then tell the downstream we're done.
     */
    private void handleOnCompleteState() {
        if (onCompleteNeeded()) {
            terminalCallMadeDownstream = true;
            subscriber.onComplete();
        }
    }

    /**
     * Returns true if we need to call onComplete downstream. If this is executed outside the handling-state-update condition, the
     * result is subject to change.
     */
    private boolean onCompleteNeeded() {
        return onCompleteCalledByUpstream && !terminalCallMadeDownstream && additionalOnCompleteNeededCheck();
    }

    /**
     * Can be overridden by subclasses to provide additional checks before calling onComplete on downstream
     */
    boolean additionalOnCompleteNeededCheck() {
        return true;
    }

    /**
     * If the upstream has called onError, then tell the downstream we're done, no matter what state the queue is in.
     */
    private void handleOnErrorState() {
        if (onErrorNeeded()) {
            terminalCallMadeDownstream = true;
            subscriber.onError(onErrorFromUpstream.get());
        }
    }

    /**
     * Returns true if we need to call onError downstream. If this is executed outside the handling-state-update condition, the
     * result is subject to change.
     */
    private boolean onErrorNeeded() {
        return onErrorFromUpstream.get() != null && !terminalCallMadeDownstream;
    }

    /**
     * Ensure that we have at least 1 demand upstream, so that we can get more items.
     */
    private void ensureUpstreamDemandExists() {
        if (this.upstreamDemand.get() < 0) {
            log.error(() -> "Upstream delivered more data than requested. Resetting state to prevent a frozen stream.",
                      new IllegalStateException());
            upstreamDemand.set(1);
            upstreamSubscription.request(1);
        } else if (this.upstreamDemand.compareAndSet(0, 1)) {
            upstreamSubscription.request(1);
        }
    }
}
