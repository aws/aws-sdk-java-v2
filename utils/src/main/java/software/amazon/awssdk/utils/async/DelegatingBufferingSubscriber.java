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

import static software.amazon.awssdk.utils.async.StoringSubscriber.EventType.ON_COMPLETE;
import static software.amazon.awssdk.utils.async.StoringSubscriber.EventType.ON_NEXT;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkProtectedApi;

@SdkProtectedApi
public class DelegatingBufferingSubscriber extends AbstractFlatteningSubscriber<ByteBuffer, ByteBuffer> {
    /**
     * The maximum amount of bytes allowed to be stored in the StoringSubscriber
     */
    private final long maximumBuffer;

    /**
     * Current amount of bytes buffered in the StoringSubscriber
     */
    private final AtomicLong currentlyBuffered = new AtomicLong(0);

    /**
     * Stores the bytes received from the upstream publisher, awaiting sending them to the delegate once the buffer size is
     * reached.
     */
    private final StoringSubscriber<ByteBuffer> storage = new StoringSubscriber<>(Integer.MAX_VALUE);

    public DelegatingBufferingSubscriber(long maximumBuffer, Subscriber<? super ByteBuffer> subscriber) {
        super(subscriber);
        this.maximumBuffer = maximumBuffer;
    }

    @Override
    public void onNext(ByteBuffer item) {
        if (currentlyBuffered.get() + item.remaining() > maximumBuffer) {
            flushStorageToDelegate();
        }
        // calling super.onNext will eventually fulfill any remaining demand
        super.onNext(item);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        storage.onSubscribe(new DemandIgnoringSubscription(subscription));
        super.onSubscribe(subscription);
    }

    @Override
    protected void doWithItem(ByteBuffer buffer) {
        if (currentlyBuffered.get() + buffer.remaining() > maximumBuffer) {
            flushStorageToDelegate();
        }
        storage.onNext(buffer.duplicate());
        currentlyBuffered.addAndGet(buffer.remaining());
    }

    @Override
    protected void fulfillDownstreamDemand() {
        flushStorageToDelegate();
    }

    /**
     * Returns true if we need to call onNext downstream.
     */
    @Override
    protected boolean onNextNeeded() {
        return super.onNextNeeded() && storage.peek().isPresent() && storage.peek().get().type() == ON_NEXT;
    }

    /**
     * Returns true if we need to call onComplete downstream.
     */
    @Override
    protected boolean onCompleteNeeded() {
        boolean emptyStorage = !storage.peek().isPresent();
        boolean storageReceivedOnComplete = storage.peek().isPresent() && storage.peek().get().type() == ON_COMPLETE;
        return super.onCompleteNeeded() && (emptyStorage || storageReceivedOnComplete);
    }

    /**
     * Returns true if we need to increase our upstream demand.
     */
    @Override
    protected boolean upstreamDemandNeeded() {
        return super.upstreamDemandNeeded() && currentlyBuffered.get() < maximumBuffer;
    }

    private void flushStorageToDelegate() {
        long totalBufferRemaining = currentlyBuffered.get();
        Optional<StoringSubscriber.Event<ByteBuffer>> next = storage.poll();
        while (totalBufferRemaining > 0) {
            if (!next.isPresent() || next.get().type() != ON_NEXT) {
                break;
            }
            StoringSubscriber.Event<ByteBuffer> byteBufferEvent = next.get();
            totalBufferRemaining -= byteBufferEvent.value().remaining();
            currentlyBuffered.addAndGet(-byteBufferEvent.value().remaining());
            subscriber.onNext(byteBufferEvent.value());
            next = storage.poll();
        }
    }

}
