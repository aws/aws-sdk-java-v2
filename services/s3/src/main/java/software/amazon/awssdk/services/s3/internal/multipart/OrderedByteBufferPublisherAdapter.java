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

package software.amazon.awssdk.services.s3.internal.multipart;

import java.nio.ByteBuffer;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.utils.Logger;

/**
 * Converts a {@code Publisher<OrderedByteBuffer>} to a {@code Publisher<ByteBuffer>} keeping in a buffer of the specified
 * buffer size.
 */
public class OrderedByteBufferPublisherAdapter implements SdkPublisher<ByteBuffer> {
    private static final Logger log = Logger.loggerFor(OrderedByteBufferPublisherAdapter.class);

    /**
     * The buffered parts to keep in memory
     */
    private final PriorityQueue<OrderedByteBuffer> bufferedParts = new PriorityQueue<>();
    private final Publisher<OrderedByteBuffer> publisher;
    private final AtomicLong totalMemBufferedInBytes = new AtomicLong(0);
    private final long bufferSize;

    private final Object flushLock = new Object();

    private final AsyncResponseTransformer<?, ?> asyncResponseTransformer;

    private final AtomicBoolean onStreamCalled = new AtomicBoolean(false);

    public OrderedByteBufferPublisherAdapter(AsyncResponseTransformer<?, ?> asyncResponseTransformer,
                                             Publisher<OrderedByteBuffer> publisher,
                                             long bufferSize) {
        this.bufferSize = bufferSize;
        this.publisher = publisher;
        this.asyncResponseTransformer = asyncResponseTransformer;
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> s) {
        publisher.subscribe(new BufferedSubscriber(s));
    }

    private boolean bufferIsFull() {
        return totalMemBufferedInBytes.get() >= this.bufferSize;
    }

    private class BufferedSubscriber implements Subscriber<OrderedByteBuffer> {
        private Subscriber<? super ByteBuffer> subscriberToSend;
        private final AtomicBoolean isComplete = new AtomicBoolean(false);
        private Subscription subscription;

        public BufferedSubscriber(Subscriber<? super ByteBuffer> subscriberToSend) {
            this.subscriberToSend = subscriberToSend;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (this.subscription != null) {
                s.cancel();
                return;
            }
            this.subscription = s;
            s.request(bufferSize);
        }

        @Override
        public void onNext(OrderedByteBuffer orderedByteBuffer) {
            synchronized (flushLock) {
                bufferedParts.offer(orderedByteBuffer);
            }
            totalMemBufferedInBytes.getAndAdd(orderedByteBuffer.buffer().capacity());
            if (isComplete.get() || bufferIsFull()) {
                flushBuffer();
            }
            if (!isComplete.get()) {
                // request more
                subscription.request(Math.max(0, bufferSize - totalMemBufferedInBytes.get()));
            }
            log.info(() -> "OrderedByteBufferPublisher: Completed onNext, position:" + orderedByteBuffer.position());
        }

        @Override
        public void onError(Throwable t) {
            subscriberToSend.onError(t);
        }

        @Override
        public void onComplete() {
            if (!bufferedParts.isEmpty()) {
                flushBuffer();
            }
            isComplete.set(true);
            subscriberToSend.onComplete();
        }

        private void flushBuffer() {
            log.info(() -> String.format(
                "OrderedByteBufferPublisher: sending full buffer to downstream publisher"));
            synchronized (flushLock) {
                while (!bufferedParts.isEmpty()) {
                    OrderedByteBuffer buffer = bufferedParts.poll();
                    buffer.buffer().position(0);
                    subscriberToSend.onNext(buffer.buffer());
                }
                totalMemBufferedInBytes.set(0);
            }
        }
    }

}
