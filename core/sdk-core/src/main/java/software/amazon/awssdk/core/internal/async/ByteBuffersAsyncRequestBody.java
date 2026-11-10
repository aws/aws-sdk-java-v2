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
import java.util.Arrays;
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
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncRequestBodySplitConfiguration;
import software.amazon.awssdk.core.async.CloseableAsyncRequestBody;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.exception.NonRetryableException;
import software.amazon.awssdk.core.internal.util.Mimetype;
import software.amazon.awssdk.core.internal.util.NoopSubscription;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.async.SimplePublisher;

/**
 * An implementation of {@link AsyncRequestBody} for providing data from the supplied {@link ByteBuffer} array. This is created
 * using static methods on {@link AsyncRequestBody}
 *
 * <h3>Subscription Behavior:</h3>
 * <ul>
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
 * <h3>Splitting:</h3>
 * Because all data is already in memory, this implementation splits into independent
 * {@code ByteBuffersAsyncRequestBody} parts backed by read-only slices of the same data rather than using
 * {@link SplittingPublisher} making each part replayable.
 *
 * @see AsyncRequestBody#fromBytes(byte[])
 * @see AsyncRequestBody#fromBytesUnsafe(byte[])
 * @see AsyncRequestBody#fromByteBuffer(ByteBuffer)
 * @see AsyncRequestBody#fromByteBufferUnsafe(ByteBuffer)
 * @see AsyncRequestBody#fromByteBuffers(ByteBuffer...)
 * @see AsyncRequestBody#fromByteBuffersUnsafe(ByteBuffer...)
 * @see AsyncRequestBody#fromString(String)
 */
@SdkInternalApi
public final class ByteBuffersAsyncRequestBody implements CloseableAsyncRequestBody {
    private static final Logger log = Logger.loggerFor(ByteBuffersAsyncRequestBody.class);

    private final String mimetype;
    private final Long length;
    private List<ByteBuffer> buffers;
    private final Set<ReplayableByteBufferSubscription> subscriptions;
    private final Object lock = new Object();
    private boolean closed;

    private ByteBuffersAsyncRequestBody(String mimetype,
                                        Long length,
                                        List<ByteBuffer> buffers) {
        this.mimetype = mimetype;
        this.buffers = buffers;
        this.length = length;
        this.subscriptions = ConcurrentHashMap.newKeySet();
    }

    @Override
    public Optional<Long> contentLength() {
        return Optional.ofNullable(length);
    }

    @Override
    public String contentType() {
        return mimetype;
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
        Validate.paramNotNull(subscriber, "subscriber");
        synchronized (lock) {
            if (closed) {
                subscriber.onSubscribe(new NoopSubscription(subscriber));
                subscriber.onError(NonRetryableException.create(
                    "AsyncRequestBody has been closed"));
                return;
            }
        }

        try {
            ReplayableByteBufferSubscription replayableByteBufferSubscription =
                new ReplayableByteBufferSubscription(subscriber);
            subscriber.onSubscribe(replayableByteBufferSubscription);
            subscriptions.add(replayableByteBufferSubscription);
        } catch (Throwable ex) {
            log.error(() -> subscriber + " violated the Reactive Streams rule 2.13 by throwing an exception from onSubscribe.",
                      ex);
        }
    }

    @Override
    public String body() {
        return BodyType.BYTES.getName();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Behaves the same as {@link #splitCloseable(AsyncRequestBodySplitConfiguration)}; the emitted parts are
     * closeable, even though they are exposed here as plain {@link AsyncRequestBody}s.
     */
    @Override
    public SdkPublisher<AsyncRequestBody> split(AsyncRequestBodySplitConfiguration splitConfiguration) {
        // The identity mapping only widens the element type. SdkPublisher is invariant, so the publisher returned by
        // splitCloseable cannot be returned directly.
        return splitCloseable(splitConfiguration).map(body -> body);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Each part is an independent, replayable {@code ByteBuffersAsyncRequestBody} backed by read-only slices of this
     * body's data, so no data is copied and each part may be subscribed to more than once (i.e. it supports retries).
     * Closing a part releases only that part's slices; it does not affect this body or any sibling part.
     *
     * <p>{@link AsyncRequestBodySplitConfiguration#bufferSizeInBytes()} is not applicable here and is ignored, since all
     * of the data is already buffered in memory.
     */
    @Override
    public SdkPublisher<CloseableAsyncRequestBody> splitCloseable(AsyncRequestBodySplitConfiguration splitConfiguration) {
        Validate.notNull(splitConfiguration, "splitConfiguration");
        long chunkSizeInBytes = splitConfiguration.chunkSizeInBytes() == null
                                ? AsyncRequestBodySplitConfiguration.defaultConfiguration().chunkSizeInBytes()
                                : splitConfiguration.chunkSizeInBytes();

        SimplePublisher<CloseableAsyncRequestBody> publisher = new SimplePublisher<>();

        // Reading closed and snapshotting buffers must be atomic, otherwise a concurrent close() could
        // split an already-emptied buffer list. Nothing is published while the lock is held.
        List<ByteBuffer> dataToSplit = null;
        boolean alreadyClosed;
        synchronized (lock) {
            alreadyClosed = closed;
            if (!alreadyClosed) {
                dataToSplit = new ArrayList<>(buffers);
            }
        }

        if (alreadyClosed) {
            publisher.error(NonRetryableException.create("AsyncRequestBody has been closed"));
            return SdkPublisher.adapt(publisher);
        }

        try {
            sendParts(publisher, dataToSplit, chunkSizeInBytes);
            publisher.complete();
        } catch (Throwable t) {
            log.debug(() -> "Failed to split the in-memory request body", t);
            publisher.error(t);
        }
        return SdkPublisher.adapt(publisher);
    }

    /**
     * Slices {@code dataToSplit} at {@code chunkSizeInBytes} boundaries and sends one part per chunk. The slices are
     * read-only views over the existing data, so this does not copy the payload. A single empty part is sent if there is
     * no data, mirroring the behavior of {@link SplittingPublisher} for an empty body.
     */
    private void sendParts(SimplePublisher<CloseableAsyncRequestBody> publisher,
                           List<ByteBuffer> dataToSplit,
                           long chunkSizeInBytes) {
        List<ByteBuffer> currentPart = new ArrayList<>();
        long currentPartLength = 0;
        boolean anyPartSent = false;

        for (ByteBuffer data : dataToSplit) {
            // A read-only view (no underlying copy)
            ByteBuffer remainingData = data.asReadOnlyBuffer();

            while (remainingData.hasRemaining()) {
                int lengthToRead = Math.toIntExact(Math.min(chunkSizeInBytes - currentPartLength,
                                                            remainingData.remaining()));
                // No underlying copy, just a view on top of the already read-only buffer.
                ByteBuffer slice = remainingData.duplicate();
                slice.limit(slice.position() + lengthToRead);
                remainingData.position(remainingData.position() + lengthToRead);

                currentPart.add(slice);
                currentPartLength += lengthToRead;

                if (currentPartLength == chunkSizeInBytes) {
                    publisher.send(new ByteBuffersAsyncRequestBody(mimetype, currentPartLength, currentPart));
                    anyPartSent = true;
                    currentPart = new ArrayList<>();
                    currentPartLength = 0;
                }
            }
        }

        if (currentPartLength > 0 || !anyPartSent) {
            publisher.send(new ByteBuffersAsyncRequestBody(mimetype, currentPartLength, currentPart));
        }
    }

    public static ByteBuffersAsyncRequestBody of(List<ByteBuffer> buffers, long length) {
        return new ByteBuffersAsyncRequestBody(Mimetype.MIMETYPE_OCTET_STREAM, length, buffers);
    }

    public static ByteBuffersAsyncRequestBody of(List<ByteBuffer> buffers) {
        long length = buffers.stream()
                            .mapToLong(ByteBuffer::remaining)
                            .sum();
        return new ByteBuffersAsyncRequestBody(Mimetype.MIMETYPE_OCTET_STREAM, length, buffers);
    }

    public static ByteBuffersAsyncRequestBody of(ByteBuffer... buffers) {
        List<ByteBuffer> byteBuffers = Arrays.asList(buffers);
        long length = byteBuffers.stream()
                             .mapToLong(ByteBuffer::remaining)
                             .sum();
        return of(byteBuffers, length);
    }

    public static ByteBuffersAsyncRequestBody of(Long length, ByteBuffer... buffers) {
        return new ByteBuffersAsyncRequestBody(Mimetype.MIMETYPE_OCTET_STREAM, length, Arrays.asList(buffers));
    }

    public static ByteBuffersAsyncRequestBody of(String mimetype, ByteBuffer... buffers) {
        long length = Arrays.stream(buffers)
                            .mapToLong(ByteBuffer::remaining)
                            .sum();
        return new ByteBuffersAsyncRequestBody(mimetype, length, Arrays.asList(buffers));
    }

    public static ByteBuffersAsyncRequestBody of(String mimetype, Long length, ByteBuffer... buffers) {
        return new ByteBuffersAsyncRequestBody(mimetype, length, Arrays.asList(buffers));
    }

    public static ByteBuffersAsyncRequestBody from(byte[] bytes) {
        return new ByteBuffersAsyncRequestBody(Mimetype.MIMETYPE_OCTET_STREAM, (long) bytes.length,
                                               Collections.singletonList(ByteBuffer.wrap(bytes)));
    }

    public static ByteBuffersAsyncRequestBody from(String mimetype, byte[] bytes) {
        return new ByteBuffersAsyncRequestBody(mimetype, (long) bytes.length,
                                               Collections.singletonList(ByteBuffer.wrap(bytes)));
    }

    @Override
    public void close() {
        synchronized (lock) {
            if (closed) {
                return;
            }

            closed = true;
            buffers = new ArrayList<>();
            subscriptions.forEach(s -> s.notifyError(new IllegalStateException("The publisher has been closed")));
            subscriptions.clear();
        }
    }

    @SdkTestInternalApi
    public List<ByteBuffer> bufferedData() {
        return buffers;
    }

    private class ReplayableByteBufferSubscription implements Subscription {
        private final AtomicInteger index = new AtomicInteger(0);
        private volatile boolean done;
        private final AtomicBoolean processingRequest = new AtomicBoolean(false);
        private Subscriber<? super ByteBuffer> currentSubscriber;
        private final AtomicLong outstandingDemand = new AtomicLong();

        private ReplayableByteBufferSubscription(Subscriber<? super ByteBuffer> subscriber) {
            this.currentSubscriber = subscriber;
        }

        @Override
        public void request(long n) {
            if (n <= 0) {
                currentSubscriber.onError(new IllegalArgumentException("§3.9: non-positive requests are not allowed!"));
                currentSubscriber = null;
                return;
            }

            if (done) {
                return;
            }

            if (buffers.size() == 0) {
                currentSubscriber.onComplete();
                done = true;
                subscriptions.remove(this);
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
            return !done && outstandingDemand.get() > 0 && index.get() < buffers.size();
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
                    done = true;
                    currentSubscriber.onComplete();
                    subscriptions.remove(this);
                    break;
                }
            }
        }

        @Override
        public void cancel() {
            done = true;
            subscriptions.remove(this);
        }

        public void notifyError(Exception exception) {
            if (currentSubscriber != null) {
                done = true;
                currentSubscriber.onError(exception);
                currentSubscriber = null;
            }
        }
    }
}
