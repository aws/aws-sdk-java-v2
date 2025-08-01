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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.exception.NonRetryableException;
import software.amazon.awssdk.core.internal.util.NoopSubscription;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.Validate;

/**
 * An implementation of {@link AsyncRequestBody} that buffers the entire content and supports multiple concurrent subscribers.
 * 
 * <p>This class allows data to be sent incrementally via the {@link #send(ByteBuffer)} method, buffered internally,
 * and then replayed to multiple subscribers independently. Each subscriber receives a complete copy of all buffered data
 * when they subscribe and request it.
 * 
 * <p>Usage Pattern:
 * {@snippet :
 * BufferingAsyncRequestBody body = new BufferingAsyncRequestBody(contentLength);
 * 
 * // Send data incrementally
 * body.send(ByteBuffer.wrap("Hello ".getBytes()));
 * body.send(ByteBuffer.wrap("World".getBytes()));
 * 
 * // Mark data as complete and ready for subscription
 * body.complete();
 * 
 * // Multiple subscribers can now consume the buffered data
 * body.subscribe(subscriber1);
 * body.subscribe(subscriber2);
 * }
 * 
 * <h3>Thread Safety:</h3>
 * This class is thread-safe and supports concurrent operations:
 * <ul>
 *   <li>Multiple threads can call {@link #send(ByteBuffer)} concurrently</li>
 *   <li>Multiple subscribers can be added concurrently</li>
 *   <li>Each subscriber operates independently with its own state</li>
 * </ul>
 * 
 * <h3>Subscription Behavior:</h3>
 * <ul>
 *   <li>Subscribers can only subscribe after {@link #complete()} has been called</li>
 *   <li>Each subscriber receives a read-only view of the buffered data</li>
 *   <li>Subscribers receive data independently based on their own demand signaling</li>
 *   <li>If the body is closed, new subscribers will receive an error immediately</li>
 * </ul>
 * 
 * <h3>Resource Management:</h3>
 * The body should be closed when no longer needed to free buffered data and notify active subscribers.
 * Closing the body will:
 * <ul>
 *   <li>Clear all buffered data</li>
 *   <li>Send error notifications to all active subscribers</li>
 *   <li>Prevent new subscriptions</li>
 * </ul>
 *
 */
@ThreadSafe
@SdkInternalApi
public final class BufferingAsyncRequestBody implements AsyncRequestBody, SdkAutoCloseable {
    private static final Logger log = Logger.loggerFor(BufferingAsyncRequestBody.class);

    private final Long length;
    private final List<ByteBuffer> bufferedData = new ArrayList<>();
    private boolean dataReady;
    private boolean closed;
    private final Set<ReplayableByteBufferSubscription> subscriptions;
    private final Object lock = new Object();

    /**
     * Creates a new BufferingAsyncRequestBody with the specified content length.
     * 
     * @param length the total content length in bytes, or null if unknown
     */
    BufferingAsyncRequestBody(Long length) {
        this.length = length;
        this.subscriptions = ConcurrentHashMap.newKeySet();
    }

    /**
     * Sends a chunk of data to be buffered for later consumption by subscribers.
     * 
     * @param data the data to buffer, must not be null
     * @throws NullPointerException if data is null
     */
    public void send(ByteBuffer data) {
        Validate.paramNotNull(data, "data");
        synchronized (lock) {
            if (closed) {
                throw new IllegalStateException("Cannot send data to closed body");
            }
            if (dataReady) {
                throw new IllegalStateException("Request body has already been completed");
            }
            bufferedData.add(data);
        }
    }

    /**
     * Marks the request body as complete and ready for subscription.
     * 
     * <p>This method must be called before any subscribers can successfully subscribe
     * to this request body. After calling this method, no more data should be sent
     * via {@link #send(ByteBuffer)}.
     * 
     * <p>Once complete, multiple subscribers can subscribe and will each receive
     * the complete buffered content independently.
     */
    public void complete() {
        synchronized (lock) {
            if (dataReady) {
                return;
            }
            if (closed) {
                throw new IllegalStateException("The AsyncRequestBody has been closed");
            }
            dataReady = true;
        }
    }

    @Override
    public Optional<Long> contentLength() {
        return Optional.ofNullable(length);
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
        Validate.paramNotNull(subscriber, "subscriber");

        synchronized (lock) {
            if (!dataReady) {
                subscriber.onSubscribe(new NoopSubscription(subscriber));
                subscriber.onError(NonRetryableException.create(
                    "Unexpected error occurred. Data is not ready to be subscribed"));
                return;
            }

            if (closed) {
                subscriber.onSubscribe(new NoopSubscription(subscriber));
                subscriber.onError(NonRetryableException.create(
                    "AsyncRequestBody has been closed"));
                return;
            }
        }

        ReplayableByteBufferSubscription replayableByteBufferSubscription =
            new ReplayableByteBufferSubscription(subscriber, bufferedData);
        subscriber.onSubscribe(replayableByteBufferSubscription);
        subscriptions.add(replayableByteBufferSubscription);
    }

    @Override
    public String body() {
        return BodyType.BYTES.getName();
    }

    /**
     * <p>Closes this request body and releases all resources. This will:
     * <ul>
     *   <li>Clear all buffered data to free memory</li>
     *   <li>Notify all active subscribers with an error</li>
     *   <li>Prevent new subscriptions from succeeding</li>
     * </ul>
     * 
     * <p>This method is idempotent - calling it multiple times has no additional effect.
     * It is safe to call this method concurrently from multiple threads.
     */
    @Override
    public void close() {
        synchronized (lock) {
            if (closed) {
                return;
            }

            closed = true;
            bufferedData.clear();
            subscriptions.forEach(s -> s.notifyError(new IllegalStateException("The publisher has been closed")));
            subscriptions.clear();
        }

    }

    @SdkTestInternalApi
    List<ByteBuffer> bufferedData() {
        return Collections.unmodifiableList(bufferedData);
    }

    private class ReplayableByteBufferSubscription implements Subscription {
        private final AtomicInteger index = new AtomicInteger(0);
        private final AtomicBoolean done = new AtomicBoolean(false);
        private final List<ByteBuffer> buffers;
        private final AtomicBoolean processingRequest = new AtomicBoolean(false);
        private Subscriber<? super ByteBuffer> currentSubscriber;
        private final AtomicLong outstandingDemand = new AtomicLong();

        private ReplayableByteBufferSubscription(Subscriber<? super ByteBuffer> subscriber, List<ByteBuffer> buffers) {
            this.buffers = buffers;
            this.currentSubscriber = subscriber;
        }

        @Override
        public void request(long n) {
            if (n <= 0) {
                currentSubscriber.onError(new IllegalArgumentException("§3.9: non-positive requests are not allowed!"));
                currentSubscriber = null;
                return;
            }

            if (done.get()) {
                return;
            }

            outstandingDemand.updateAndGet(current -> {
                if (Long.MAX_VALUE - current < n) {
                    return Long.MAX_VALUE;
                }

                return current + n;
            });
            processRequest();
        }

        private void processRequest() {
            do {
                if (!processingRequest.compareAndSet(false, true)) {
                    // Some other thread is processing the queue, so we don't need to.
                    return;
                }

                try {
                    doProcessRequest();
                } catch (Throwable e) {
                    notifyError(new IllegalStateException("Encountered fatal error in publisher", e));
                    subscriptions.remove(this);
                    break;
                } finally {
                    processingRequest.set(false);
                }

            } while (shouldProcessRequest());
        }

        private boolean shouldProcessRequest() {
            return !done.get() && outstandingDemand.get() > 0 && index.get() < buffers.size();
        }

        private void doProcessRequest() {
            while (true) {
                if (!shouldProcessRequest()) {
                    return;
                }

                int currentIndex = this.index.getAndIncrement();

                if (currentIndex >= buffers.size()) {
                    // This should never happen because shouldProcessRequest() ensures that index.get() < buffers.size()
                    // before incrementing. If this condition is true, it likely indicates a concurrency bug or that buffers
                    // was modified unexpectedly. This defensive check is here to catch such rare, unexpected situations.
                    notifyError(new IllegalStateException("Index out of bounds"));
                    subscriptions.remove(this);
                    return;
                }

                ByteBuffer buffer = buffers.get(currentIndex);
                currentSubscriber.onNext(buffer.asReadOnlyBuffer());
                outstandingDemand.decrementAndGet();

                if (currentIndex == buffers.size() - 1) {
                    done.set(true);
                    currentSubscriber.onComplete();
                    subscriptions.remove(this);
                    break;
                }
            }
        }

        @Override
        public void cancel() {
            done.set(true);
            subscriptions.remove(this);
        }

        public void notifyError(Exception exception) {
            if (currentSubscriber != null) {
                done.set(true);
                currentSubscriber.onError(exception);
                currentSubscriber = null;
            }
        }
    }
}
