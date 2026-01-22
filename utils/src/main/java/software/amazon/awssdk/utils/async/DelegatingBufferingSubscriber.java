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
import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

@SdkProtectedApi
public class DelegatingBufferingSubscriber extends BaseSubscriberAdapter<ByteBuffer, ByteBuffer> {
    private static final Logger log = Logger.loggerFor(DelegatingBufferingSubscriber.class);

    /**
     * The maximum amount of bytes allowed to be stored in the StoringSubscriber
     */
    private final long maximumBufferInBytes;

    /**
     * Current amount of bytes buffered in the StoringSubscriber
     */
    private final AtomicLong currentlyBuffered = new AtomicLong(0);

    /**
     * Stores the bytes received from the upstream publisher, awaiting sending them to the delegate once the buffer size is
     * reached.
     */
    private final StoringSubscriber<ByteBuffer> storage = new StoringSubscriber<>(Integer.MAX_VALUE);

    protected DelegatingBufferingSubscriber(Long maximumBufferInBytes, Subscriber<? super ByteBuffer> delegate) {
        super(Validate.notNull(delegate, "delegate must not be null"));
        this.maximumBufferInBytes = Validate.notNull(maximumBufferInBytes, "maximumBufferInBytes must not be null");
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        storage.onSubscribe(new DemandIgnoringSubscription(subscription));
        super.onSubscribe(subscription);
    }

    @Override
    void doWithItem(ByteBuffer buffer) {
        storage.onNext(buffer.duplicate());
        currentlyBuffered.addAndGet(buffer.remaining());
    }

    @Override
    protected void fulfillDownstreamDemand() {
        storage.poll()
               .filter(event -> event.type() == ON_NEXT)
               .ifPresent(byteBufferEvent -> {
                   currentlyBuffered.addAndGet(-byteBufferEvent.value().remaining());
                   downstreamDemand.decrementAndGet();
                   log.trace(() -> "demand: " + downstreamDemand.get());
                   subscriber.onNext(byteBufferEvent.value());
               });
    }

    /**
     * Returns true if we need to call onNext downstream.
     */
    @Override
    boolean additionalOnNextNeededCheck() {
        return storage.peek().map(event -> event.type() == ON_NEXT).orElse(false);
    }

    /**
     * Returns true if we need to call onComplete downstream.
     */
    @Override
    boolean additionalOnCompleteNeededCheck() {
        return storage.peek().map(event -> event.type() == ON_COMPLETE).orElse(true);
    }

    /**
     * Returns true if we need to increase our upstream demand.
     */
    @Override
    boolean additionalUpstreamDemandNeededCheck() {
        return currentlyBuffered.get() < maximumBufferInBytes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long maximumBufferInBytes;
        private Subscriber<? super ByteBuffer> delegate;

        public Builder maximumBufferInBytes(Long maximumBufferInBytes) {
            this.maximumBufferInBytes = maximumBufferInBytes;
            return this;
        }

        public Builder delegate(Subscriber<? super ByteBuffer> delegate) {
            this.delegate = delegate;
            return this;
        }

        public DelegatingBufferingSubscriber build() {
            return new DelegatingBufferingSubscriber(maximumBufferInBytes, delegate);
        }
    }
}
