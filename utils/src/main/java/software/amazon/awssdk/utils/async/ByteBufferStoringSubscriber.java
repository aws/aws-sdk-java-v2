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

import static software.amazon.awssdk.utils.async.StoringSubscriber.EventType.ON_NEXT;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.async.StoringSubscriber.Event;

/**
 * An implementation of {@link Subscriber} that stores {@link ByteBuffer} events it receives for retrieval.
 *
 * <p>Stored bytes can be read via {@link #transferTo(ByteBuffer)}.
 */
@SdkProtectedApi
public class ByteBufferStoringSubscriber implements Subscriber<ByteBuffer> {
    /**
     * The minimum amount of data (in bytes) that should be buffered in memory at a time. The subscriber will request new byte
     * buffers from upstream until the bytes received equals or exceeds this value.
     */
    private final long minimumBytesBuffered;

    /**
     * The amount of data (in bytes) currently stored in this subscriber. The subscriber will request more data when this value
     * is below the {@link #minimumBytesBuffered}.
     */
    private final AtomicLong bytesBuffered = new AtomicLong(0L);

    /**
     * A delegate subscriber that we use to store the buffered bytes in the order they are received.
     */
    private final StoringSubscriber<ByteBuffer> storingSubscriber;

    /**
     * The active subscription. Set when {@link #onSubscribe(Subscription)} is invoked.
     */
    private Subscription subscription;

    /**
     * Create a subscriber that stores at least {@code minimumBytesBuffered} in memory for retrieval.
     */
    public ByteBufferStoringSubscriber(long minimumBytesBuffered) {
        this.minimumBytesBuffered = Validate.isPositive(minimumBytesBuffered, "Data buffer minimum must be positive");
        this.storingSubscriber = new StoringSubscriber<>(Integer.MAX_VALUE);
    }

    /**
     * Transfer the data stored by this subscriber into the provided byte buffer.
     *
     * <p>If the data stored by this subscriber exceeds {@code out}'s {@code limit}, then {@code out} will be filled. If the data
     * stored by this subscriber is less than {@code out}'s {@code limit}, then all stored data will be written to {@code out}.
     *
     * <p>If {@link #onError(Throwable)} was called on this subscriber, as much data as is available will be transferred into
     * {@code out} before the provided exception is thrown (as a {@link RuntimeException}).
     *
     * <p>If {@link #onComplete()} was called on this subscriber, as much data as is available will be transferred into
     * {@code out}, and this will return {@link TransferResult#END_OF_STREAM}.
     *
     * <p>Note: This method MUST NOT be called concurrently. Other methods on this class may be called concurrently with this
     * one.
     */
    public TransferResult transferTo(ByteBuffer out) {
        int transferred = 0;

        Optional<Event<ByteBuffer>> next = storingSubscriber.peek();

        while (out.hasRemaining()) {
            if (!next.isPresent() || next.get().type() != ON_NEXT) {
                break;
            }

            transferred += transfer(next.get().value(), out);
            next = storingSubscriber.peek();
        }

        addBufferedDataAmount(-transferred);

        if (!next.isPresent()) {
            return TransferResult.SUCCESS;
        }

        switch (next.get().type()) {
            case ON_COMPLETE:
                return TransferResult.END_OF_STREAM;
            case ON_ERROR:
                throw next.get().runtimeError();
            case ON_NEXT:
                return TransferResult.SUCCESS;
            default:
                throw new IllegalStateException("Unknown stored type: " + next.get().type());
        }
    }

    private int transfer(ByteBuffer in, ByteBuffer out) {
        int amountToTransfer = Math.min(in.remaining(), out.remaining());

        ByteBuffer truncatedIn = in.duplicate();
        truncatedIn.limit(truncatedIn.position() + amountToTransfer);

        out.put(truncatedIn);
        in.position(truncatedIn.position());

        if (!in.hasRemaining()) {
            storingSubscriber.poll();
        }

        return amountToTransfer;
    }

    @Override
    public void onSubscribe(Subscription s) {
        storingSubscriber.onSubscribe(new DemandIgnoringSubscription(s));
        subscription = s;
        subscription.request(1);
    }

    @Override
    public void onNext(ByteBuffer byteBuffer) {
        storingSubscriber.onNext(byteBuffer.duplicate());
        addBufferedDataAmount(byteBuffer.remaining());
    }

    @Override
    public void onError(Throwable t) {
        storingSubscriber.onError(t);
    }

    @Override
    public void onComplete() {
        storingSubscriber.onComplete();
    }

    private void addBufferedDataAmount(long amountToAdd) {
        long currentDataBuffered = bytesBuffered.addAndGet(amountToAdd);
        maybeRequestMore(currentDataBuffered);
    }

    private void maybeRequestMore(long currentDataBuffered) {
        if (currentDataBuffered < minimumBytesBuffered) {
            subscription.request(1);
        }
    }

    /**
     * The result of {@link #transferTo(ByteBuffer)}.
     */
    public enum TransferResult {
        /**
         * Data was successfully transferred to {@code out}, and the end of stream has been reached. No future calls to
         * {@link #transferTo(ByteBuffer)} will yield additional data.
         */
        END_OF_STREAM,

        /**
         * Data was successfully transferred to {@code out}, but the end of stream has not been reached. Future calls to
         * {@link #transferTo(ByteBuffer)} may yield additional data.
         */
        SUCCESS
    }

    private static final class DemandIgnoringSubscription implements Subscription {
        private final Subscription delegate;

        private DemandIgnoringSubscription(Subscription delegate) {
            this.delegate = delegate;
        }

        @Override
        public void request(long n) {
            // Ignore demand requests from downstream, they want too much.
            // We feed them the amount that we want.
        }

        @Override
        public void cancel() {
            delegate.cancel();
        }
    }
}
