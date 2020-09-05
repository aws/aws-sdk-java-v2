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

package software.amazon.awssdk.http.crt.internal;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongUnaryOperator;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.http.HttpClientConnection;
import software.amazon.awssdk.crt.http.HttpStream;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * Adapts an AWS Common Runtime Response Body stream from CrtHttpStreamHandler to a Publisher<ByteBuffer>
 */
@SdkInternalApi
public final class AwsCrtResponseBodyPublisher implements Publisher<ByteBuffer> {
    private static final Logger log = Logger.loggerFor(AwsCrtResponseBodyPublisher.class);
    private static final LongUnaryOperator DECREMENT_IF_GREATER_THAN_ZERO = x -> ((x > 0) ? (x - 1) : (x));

    private final HttpClientConnection connection;
    private final HttpStream stream;
    private final CompletableFuture<Void> responseComplete;
    private final AtomicLong outstandingRequests = new AtomicLong(0);
    private final int windowSize;
    private final AtomicBoolean isCancelled = new AtomicBoolean(false);
    private final AtomicBoolean areNativeResourcesReleased = new AtomicBoolean(false);
    private final AtomicBoolean isSubscriptionComplete = new AtomicBoolean(false);
    private final AtomicBoolean queueComplete = new AtomicBoolean(false);
    private final AtomicInteger mutualRecursionDepth = new AtomicInteger(0);
    private final AtomicInteger queuedBytes = new AtomicInteger(0);
    private final AtomicReference<Subscriber<? super ByteBuffer>> subscriberRef = new AtomicReference<>(null);
    private final Queue<byte[]> queuedBuffers = new ConcurrentLinkedQueue<>();
    private final AtomicReference<Throwable> error = new AtomicReference<>(null);

    /**
     * Adapts a streaming AWS CRT Http Response Body to a Publisher<ByteBuffer>
     * @param stream The AWS CRT Http Stream for this Response
     * @param windowSize The max allowed bytes to be queued. The sum of the sizes of all queued ByteBuffers should
     *                   never exceed this value.
     */
    public AwsCrtResponseBodyPublisher(HttpClientConnection connection, HttpStream stream,
                                       CompletableFuture<Void> responseComplete, int windowSize) {
        this.connection = Validate.notNull(connection, "HttpConnection must not be null");
        this.stream = Validate.notNull(stream, "Stream must not be null");
        this.responseComplete = Validate.notNull(responseComplete, "ResponseComplete future must not be null");
        this.windowSize = Validate.isPositive(windowSize, "windowSize must be > 0");
    }

    /**
     * Method for the users consuming the Http Response Body to register a subscriber.
     * @param subscriber The Subscriber to register.
     */
    @Override
    public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
        Validate.notNull(subscriber, "Subscriber must not be null");

        boolean wasFirstSubscriber = subscriberRef.compareAndSet(null, subscriber);

        if (!wasFirstSubscriber) {
            log.error(() -> "Only one subscriber allowed");

            // onSubscribe must be called first before onError gets called, so give it a do-nothing Subscription
            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    // This is a dummy implementation to allow the onError call
                }

                @Override
                public void cancel() {
                    // This is a dummy implementation to allow the onError call
                }
            });
            subscriber.onError(new IllegalStateException("Only one subscriber allowed"));
        } else {
            subscriber.onSubscribe(new AwsCrtResponseBodySubscription(this));
        }
    }

    /**
     * Adds a Buffer to the Queue to be published to any Subscribers
     * @param buffer The Buffer to be queued.
     */
    public void queueBuffer(byte[] buffer) {
        Validate.notNull(buffer, "ByteBuffer must not be null");

        if (isCancelled.get()) {
            // Immediately open HttpStream's IO window so it doesn't see any IO Back-pressure.
            // AFAIK there's no way to abort an in-progress HttpStream, only free it's memory by calling close()
            stream.incrementWindow(buffer.length);
            return;
        }

        queuedBuffers.add(buffer);
        int totalBytesQueued = queuedBytes.addAndGet(buffer.length);

        if (totalBytesQueued > windowSize) {
            throw new IllegalStateException("Queued more than Window Size: queued=" + totalBytesQueued
                                            + ", window=" + windowSize);
        }
    }

    /**
     * Function called by Response Body Subscribers to request more Response Body buffers.
     * @param n The number of buffers requested.
     */
    protected void request(long n) {
        Validate.inclusiveBetween(1, Long.MAX_VALUE, n, "request");

        // Check for overflow of outstanding Requests, and clamp to LONG_MAX.
        long outstandingReqs;
        if (n > (Long.MAX_VALUE - outstandingRequests.get())) {
            outstandingRequests.set(Long.MAX_VALUE);
            outstandingReqs = Long.MAX_VALUE;
        } else {
            outstandingReqs = outstandingRequests.addAndGet(n);
        }

        /*
         * Since we buffer, in the case where the subscriber came in after the publication has already begun,
         * go ahead and flush what we have.
         */
        publishToSubscribers();

        log.trace(() -> "Subscriber Requested more Buffers. Outstanding Requests: " + outstandingReqs);
    }

    public void setError(Throwable t) {
        log.error(() -> "Error processing Response Body", t);
        error.compareAndSet(null, t);
    }

    protected void setCancelled() {
        isCancelled.set(true);
        /**
         * subscriberRef must set to null due to ReactiveStream Spec stating references to Subscribers must be deleted
         * when onCancel() is called.
         */
        subscriberRef.set(null);
    }

    private synchronized void releaseNativeResources() {
        boolean alreadyReleased = areNativeResourcesReleased.getAndSet(true);

        if (!alreadyReleased) {
            stream.close();
            connection.close();
        }
    }

    /**
     * Called when the final Buffer has been queued and no more data is expected.
     */
    public void setQueueComplete() {
        log.trace(() -> "Response Body Publisher queue marked as completed.");
        queueComplete.set(true);
        // We're done with the Native Resources, release them so they can be used by another request.
        releaseNativeResources();
    }

    /**
     * Completes the Subscription by calling either the .onError() or .onComplete() callbacks exactly once.
     */
    protected void completeSubscriptionExactlyOnce() {
        boolean alreadyComplete = isSubscriptionComplete.getAndSet(true);

        if (alreadyComplete) {
            return;
        }

        // Subscriber may have cancelled their subscription, in which case this may be null.
        Optional<Subscriber<? super ByteBuffer>> subscriber = Optional.ofNullable(subscriberRef.getAndSet(null));

        Throwable throwable = error.get();

        // We're done with the Native Resources, release them so they can be used by another request.
        releaseNativeResources();

        // Complete the Futures
        if (throwable != null) {
            log.error(() -> "Error before ResponseBodyPublisher could complete: " + throwable.getMessage());
            try {
                subscriber.ifPresent(s -> s.onError(throwable));
            } catch (Exception e) {
                log.warn(() -> "Failed to exceptionally complete subscriber future with: " + throwable.getMessage());
            }
            responseComplete.completeExceptionally(throwable);
        } else {
            log.debug(() -> "ResponseBodyPublisher Completed Successfully");
            try {
                subscriber.ifPresent(Subscriber::onComplete);
            } catch (Exception e) {
                log.warn(() -> "Failed to successfully complete subscriber future");
            }
            responseComplete.complete(null);
        }
    }

    /**
     * Publishes any queued data to any Subscribers if there is data queued and there is an outstanding Subscriber
     * request for more data. Will also call onError() or onComplete() callbacks if needed.
     *
     * This method MUST be synchronized since it can be called simultaneously from both the Native EventLoop Thread and
     * the User Thread. If this method wasn't synchronized, it'd be possible for each thread to dequeue a buffer by
     * calling queuedBuffers.poll(), but then have the 2nd thread call subscriber.onNext(buffer) first, resulting in the
     * subscriber seeing out-of-order data. To avoid this race condition, this method must be synchronized.
     */
    protected void publishToSubscribers() {
        boolean shouldComplete = true;
        synchronized (this) {
            if (error.get() == null) {
                if (isSubscriptionComplete.get() || isCancelled.get()) {
                    log.debug(() -> "Subscription already completed or cancelled, can't publish updates to Subscribers.");
                    return;
                }

                if (mutualRecursionDepth.get() > 0) {
                    /**
                     * If our depth is > 0, then we already made a call to publishToSubscribers() further up the stack that
                     * will continue publishing to subscribers, and this call should return without completing work to avoid
                     * infinite recursive loop between: "subscription.request() -> subscriber.onNext() -> subscription.request()"
                     */
                    return;
                }

                int totalAmountTransferred = 0;

                while (outstandingRequests.get() > 0 && !queuedBuffers.isEmpty()) {
                    byte[] buffer = queuedBuffers.poll();
                    outstandingRequests.getAndUpdate(DECREMENT_IF_GREATER_THAN_ZERO);
                    int amount = buffer.length;
                    publishWithoutMutualRecursion(subscriberRef.get(), ByteBuffer.wrap(buffer));
                    totalAmountTransferred += amount;
                }

                if (totalAmountTransferred > 0) {
                    queuedBytes.addAndGet(-totalAmountTransferred);

                    // We may have released the Native HttpConnection and HttpStream if they completed before the Subscriber
                    // has finished reading the data.
                    if (!areNativeResourcesReleased.get()) {
                        // Open HttpStream's IO window so HttpStream can keep track of IO back-pressure
                        // This is why it is correct to return 0 from AwsCrtAsyncHttpStreamAdapter::onResponseBody
                        stream.incrementWindow(totalAmountTransferred);
                    }
                }

                shouldComplete = queueComplete.get() && queuedBuffers.isEmpty();
            } else {
                shouldComplete = true;
            }
        }

        // Check if Complete, consider no subscriber as a completion.
        if (shouldComplete) {
            completeSubscriptionExactlyOnce();
        }
    }

    /**
     * This method is used to avoid a StackOverflow due to the potential infinite loop between
     * "subscription.request() -> subscriber.onNext() -> subscription.request()" calls. We only call subscriber.onNext()
     * if the recursion depth is zero, otherwise we return up to the stack frame with depth zero and continue publishing
     * from there.
     * @param subscriber The Subscriber to publish to.
     * @param buffer The buffer to publish to the subscriber.
     */
    private synchronized void publishWithoutMutualRecursion(Subscriber<? super ByteBuffer> subscriber, ByteBuffer buffer) {
        try {
            /**
             * Need to keep track of recursion depth between .onNext() -> .request() calls
             */
            int depth = mutualRecursionDepth.getAndIncrement();
            if (depth == 0) {
                subscriber.onNext(buffer);
            }
        } finally {
            mutualRecursionDepth.decrementAndGet();
        }
    }

    static class AwsCrtResponseBodySubscription implements Subscription {
        private final AwsCrtResponseBodyPublisher publisher;

        AwsCrtResponseBodySubscription(AwsCrtResponseBodyPublisher publisher) {
            this.publisher = publisher;
        }

        @Override
        public void request(long n) {
            if (n <= 0) {
                // Reactive Stream Spec requires us to call onError() callback instead of throwing Exception here.
                publisher.setError(new IllegalArgumentException("Request is for <= 0 elements: " + n));
                publisher.publishToSubscribers();
                return;
            }

            publisher.request(n);
            publisher.publishToSubscribers();
        }

        @Override
        public void cancel() {
            publisher.setCancelled();
        }
    }

}
