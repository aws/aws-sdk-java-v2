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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.async.SimplePublisher;

/**
 * Splits an {@link SdkPublisher} to multiple smaller {@link AsyncRequestBody}s, each of which publishes a specific portion of the
 * original data.
 * // TODO: create a default method in AsyncRequestBody for this
 * // TODO: fix the case where content length is null
 */
@SdkInternalApi
public class SplittingPublisher implements SdkPublisher<AsyncRequestBody> {
    private static final Logger log = Logger.loggerFor(SplittingPublisher.class);
    private final AsyncRequestBody upstreamPublisher;
    private final SplittingSubscriber splittingSubscriber;
    private final SimplePublisher<AsyncRequestBody> downstreamPublisher = new SimplePublisher<>();
    private final long chunkSizeInBytes;
    private final long maxMemoryUsageInBytes;
    private final CompletableFuture<Void> future;

    private SplittingPublisher(Builder builder) {
        this.upstreamPublisher =  Validate.paramNotNull(builder.asyncRequestBody, "asyncRequestBody");
        this.chunkSizeInBytes = Validate.paramNotNull(builder.chunkSizeInBytes, "chunkSizeInBytes");
        this.splittingSubscriber = new SplittingSubscriber(upstreamPublisher.contentLength().orElse(null));
        this.maxMemoryUsageInBytes = builder.maxMemoryUsageInBytes == null ? Long.MAX_VALUE : builder.maxMemoryUsageInBytes;
        this.future = builder.future;

        // We need to cancel upstream subscription if the future gets cancelled.
        future.whenComplete((r, t) -> {
            if (t != null) {
                if (splittingSubscriber.upstreamSubscription != null) {
                    log.trace(() -> "Cancelling subscription because return future completed exceptionally ", t);
                    splittingSubscriber.upstreamSubscription.cancel();
                }
            }
        });
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void subscribe(Subscriber<? super AsyncRequestBody> downstreamSubscriber) {
        downstreamPublisher.subscribe(downstreamSubscriber);
        upstreamPublisher.subscribe(splittingSubscriber);
    }

    private class SplittingSubscriber implements Subscriber<ByteBuffer> {
        private Subscription upstreamSubscription;
        private final Long upstreamSize;
        private final AtomicInteger chunkNumber = new AtomicInteger(0);
        private volatile DownstreamBody currentBody;
        private final AtomicBoolean hasOpenUpstreamDemand = new AtomicBoolean(false);
        private final AtomicLong dataBuffered = new AtomicLong(0);

        /**
         * A hint to determine whether we will exceed maxMemoryUsage by the next OnNext call.
         */
        private int byteBufferSizeHint;

        SplittingSubscriber(Long upstreamSize) {
            this.upstreamSize = upstreamSize;
        }

        @Override
        public void onSubscribe(Subscription s) {
            this.upstreamSubscription = s;
            this.currentBody = new DownstreamBody(calculateChunkSize(), chunkNumber.get());
            sendCurrentBody();
            // We need to request subscription *after* we set currentBody because onNext could be invoked right away.
            upstreamSubscription.request(1);
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            hasOpenUpstreamDemand.set(false);
            byteBufferSizeHint = byteBuffer.remaining();

            while (true) {
                int amountRemainingInPart = amountRemainingInPart();
                int finalAmountRemainingInPart = amountRemainingInPart;
                if (amountRemainingInPart == 0) {
                    currentBody.complete();
                    int currentChunk = chunkNumber.incrementAndGet();
                    Long partSize = calculateChunkSize();
                    currentBody = new DownstreamBody(partSize, currentChunk);
                    sendCurrentBody();
                }

                amountRemainingInPart = amountRemainingInPart();
                if (amountRemainingInPart >= byteBuffer.remaining()) {
                    currentBody.send(byteBuffer.duplicate());
                    break;
                }

                ByteBuffer firstHalf = byteBuffer.duplicate();
                int newLimit = firstHalf.position() + amountRemainingInPart;
                firstHalf.limit(newLimit);
                byteBuffer.position(newLimit);
                currentBody.send(firstHalf);
            }

            maybeRequestMoreUpstreamData();
        }

        private int amountRemainingInPart() {
            return Math.toIntExact(currentBody.totalLength - currentBody.transferredLength);
        }

        @Override
        public void onComplete() {
            log.trace(() -> "Received onComplete()");
            downstreamPublisher.complete().thenRun(() -> future.complete(null));
            currentBody.complete();
        }

        @Override
        public void onError(Throwable t) {
            currentBody.error(t);
        }

        private void sendCurrentBody() {
            downstreamPublisher.send(currentBody).exceptionally(t -> {
                downstreamPublisher.error(t);
                return null;
            });
        }

        private Long calculateChunkSize() {
            Long dataRemaining = dataRemaining();
            if (dataRemaining == null) {
                return null;
            }

            return Math.min(chunkSizeInBytes, dataRemaining);
        }

        private void maybeRequestMoreUpstreamData() {
            long buffered = dataBuffered.get();
            if (shouldRequestMoreData(buffered) &&
                hasOpenUpstreamDemand.compareAndSet(false, true)) {
                log.trace(() -> "Requesting more data, current data buffered: " + buffered);
                upstreamSubscription.request(1);
            }
        }

        private boolean shouldRequestMoreData(long buffered) {
            return buffered == 0 || buffered + byteBufferSizeHint < maxMemoryUsageInBytes;
        }

        private Long dataRemaining() {
            if (upstreamSize == null) {
                return null;
            }
            return upstreamSize - (chunkNumber.get() * chunkSizeInBytes);
        }

        private class DownstreamBody implements AsyncRequestBody {
            private final Long totalLength;
            private final SimplePublisher<ByteBuffer> delegate = new SimplePublisher<>();
            private final int chunkNumber;
            private volatile long transferredLength = 0;

            private DownstreamBody(Long totalLength, int chunkNumber) {
                this.totalLength = totalLength;
                this.chunkNumber = chunkNumber;
            }

            @Override
            public Optional<Long> contentLength() {
                return Optional.ofNullable(totalLength);
            }

            public void send(ByteBuffer data) {
                log.trace(() -> "Sending bytebuffer " + data);
                int length = data.remaining();
                transferredLength += length;
                addDataBuffered(length);
                delegate.send(data).whenComplete((r, t) -> {
                    addDataBuffered(-length);
                    if (t != null) {
                        error(t);
                    }
                });
            }

            public void complete() {
                log.debug(() -> "Received complete() for chunk number: " + chunkNumber);
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
    
    public static final class Builder {
        private AsyncRequestBody asyncRequestBody;
        private Long chunkSizeInBytes;
        private Long maxMemoryUsageInBytes;
        private CompletableFuture<Void> future;

        /**
         * Configures the asyncRequestBody to split
         *
         * @param asyncRequestBody The new asyncRequestBody value.
         * @return This object for method chaining.
         */
        public Builder asyncRequestBody(AsyncRequestBody asyncRequestBody) {
            this.asyncRequestBody = asyncRequestBody;
            return this;
        }

        /**
         * Configures the size of the chunk for each {@link AsyncRequestBody} to publish
         *
         * @param chunkSizeInBytes The new chunkSizeInBytes value.
         * @return This object for method chaining.
         */
        public Builder chunkSizeInBytes(Long chunkSizeInBytes) {
            this.chunkSizeInBytes = chunkSizeInBytes;
            return this;
        }

        /**
         * Sets the maximum memory usage in bytes. By default, it uses unlimited memory.
         *
         * @param maxMemoryUsageInBytes The new maxMemoryUsageInBytes value.
         * @return This object for method chaining.
         */
        // TODO: max memory usage might not be the best name, since we may technically go a little above this limit when we add
        //  on a new byte buffer. But we don't know for sure what the size of a buffer we request will be (we do use the size
        //  for the last byte buffer as a hint), so I don't think we can have a truly accurate max. Maybe we call it minimum
        //  buffer size instead?
        public Builder maxMemoryUsageInBytes(Long maxMemoryUsageInBytes) {
            this.maxMemoryUsageInBytes = maxMemoryUsageInBytes;
            return this;
        }

        /**
         * Sets the result future. The future will be completed when all request bodies
         * have been sent.
         *
         * @param future The new future value.
         * @return This object for method chaining.
         */
        public Builder resultFuture(CompletableFuture<Void> future) {
            this.future = future;
            return this;
        }

        public SplittingPublisher build() {
            return new SplittingPublisher(this);
        }
    }
}
