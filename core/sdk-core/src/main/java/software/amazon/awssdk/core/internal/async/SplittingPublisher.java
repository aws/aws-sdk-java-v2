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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.utils.async.SimplePublisher;

public class SplittingPublisher implements SdkPublisher<AsyncRequestBody> {
    private final AsyncRequestBody upstreamPublisher;
    private final SplittingSubscriber splittingSubscriber = new SplittingSubscriber();
    private final SimplePublisher<AsyncRequestBody> downstreamPublisher = new SimplePublisher<>();
    private final long partSizeInBytes;
    private final long maxMemoryUsageInBytes;

    public SplittingPublisher(AsyncRequestBody asyncRequestBody,
                              long partSizeInBytes,
                              long maxMemoryUsageInBytes) {
        this.upstreamPublisher = asyncRequestBody;
        this.partSizeInBytes = partSizeInBytes;
        this.maxMemoryUsageInBytes = maxMemoryUsageInBytes;
    }

    @Override
    public void subscribe(Subscriber<? super AsyncRequestBody> downstreamSubscriber) {
        downstreamPublisher.subscribe(downstreamSubscriber);
        upstreamPublisher.subscribe(splittingSubscriber);
    }

    private class SplittingSubscriber implements Subscriber<ByteBuffer> {
        private Subscription upstreamSubscription;
        private final Long upstreamSize = upstreamPublisher.contentLength().orElse(null);

        private int partNumber = 0;
        private DownstreamBody currentBody;

        private final AtomicBoolean hasOpenUpstreamDemand = new AtomicBoolean(false);
        private final AtomicLong dataBuffered = new AtomicLong(0);

        @Override
        public void onSubscribe(Subscription s) {
            this.upstreamSubscription = s;
            this.currentBody = new DownstreamBody(calculatePartSize());
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            hasOpenUpstreamDemand.set(false);

            while (true) {
                int amountRemainingInPart = Math.toIntExact(partSizeInBytes - currentBody.partLength);

                if (amountRemainingInPart < byteBuffer.remaining()) {
                    // TODO: should we avoid sending empty byte buffers, which can happen here?
                    currentBody.send(byteBuffer);
                    break;
                }

                ByteBuffer firstHalf = byteBuffer.duplicate();
                int newLimit = firstHalf.position() + amountRemainingInPart;
                firstHalf.limit(newLimit); // TODO: Am I off by one here?
                currentBody.send(firstHalf);
                currentBody.complete();

                ++partNumber;
                currentBody = new DownstreamBody(calculatePartSize());
                downstreamPublisher.send(currentBody);
                byteBuffer.position(newLimit); // TODO: Am I off by one here?
            }

            maybeRequestMoreUpstreamData();
        }

        @Override
        public void onComplete() {
            currentBody.complete();
        }

        @Override
        public void onError(Throwable t) {
            currentBody.error(t);
        }

        private Long calculatePartSize() {
            Long dataRemaining = dataRemaining();
            if (dataRemaining == null) {
                return null;
            }

            return Math.min(partSizeInBytes, dataRemaining);
        }

        private void maybeRequestMoreUpstreamData() {
            if (dataBuffered.get() < maxMemoryUsageInBytes && hasOpenUpstreamDemand.compareAndSet(false, true)) {
                // TODO: max memory usage might not be the best name, since we can technically go a little above
                // this limit when we add on a new byte buffer. But we don't know what the size of a buffer we request
                // will be, so I don't think we can have a truly accurate max. Maybe we call it minimum buffer size instead?
                upstreamSubscription.request(1);
            }
        }

        private Long dataRemaining() {
            if (upstreamSize == null) {
                return null;
            }
            return upstreamSize - (partNumber * partSizeInBytes);
        }

        private class DownstreamBody implements AsyncRequestBody {
            private final Long size;
            private final SimplePublisher<ByteBuffer> delegate = new SimplePublisher<>();
            private long partLength = 0;

            private DownstreamBody(Long size) {
                this.size = size;
            }

            @Override
            public Optional<Long> contentLength() {
                return Optional.ofNullable(size);
            }

            public void send(ByteBuffer data) {
                int length = data.remaining();
                partLength += length;
                addDataBuffered(length);
                delegate.send(data).thenRun(() -> addDataBuffered(-length));
            }

            public void complete() {
                delegate.complete();
            }

            public void error(Throwable error) {
                delegate.error(error);
            }

            @Override
            public void subscribe(Subscriber<? super ByteBuffer> s) {
                delegate.subscribe(s);
            }

            private void addDataBuffered(int length) {
                dataBuffered.addAndGet(length);
                if (length < 0) {
                    maybeRequestMoreUpstreamData();
                }
            }
        }
    }
}
