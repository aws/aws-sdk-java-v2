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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncRequestBodySplitConfiguration;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.exception.NonRetryableException;
import software.amazon.awssdk.core.internal.util.NoopSubscription;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.async.SimplePublisher;

/**
 * Splits an {@link AsyncRequestBody} to multiple smaller {@link AsyncRequestBody}s, each of which publishes a specific portion of
 * the original data.
 *
 * <p>If content length is known, each {@link AsyncRequestBody} is sent to the subscriber right after it's initialized.
 * Otherwise, it is sent after the entire content for that chunk is buffered. This is required to get content length.
 */
@SdkInternalApi
public class SplittingPublisher implements SdkPublisher<AsyncRequestBody> {
    private static final Logger log = Logger.loggerFor(SplittingPublisher.class);
    private final AsyncRequestBody upstreamPublisher;
    private final SplittingSubscriber splittingSubscriber;
    private final SimplePublisher<AsyncRequestBody> downstreamPublisher = new SimplePublisher<>();
    private final long chunkSizeInBytes;
    private final long bufferSizeInBytes;

    public SplittingPublisher(AsyncRequestBody asyncRequestBody,
                              AsyncRequestBodySplitConfiguration splitConfiguration) {
        this.upstreamPublisher = Validate.paramNotNull(asyncRequestBody, "asyncRequestBody");
        Validate.notNull(splitConfiguration, "splitConfiguration");
        this.chunkSizeInBytes = splitConfiguration.chunkSizeInBytes() == null ?
                                AsyncRequestBodySplitConfiguration.defaultConfiguration().chunkSizeInBytes() :
                                splitConfiguration.chunkSizeInBytes();

        this.bufferSizeInBytes = splitConfiguration.bufferSizeInBytes() == null ?
                                 AsyncRequestBodySplitConfiguration.defaultConfiguration().bufferSizeInBytes() :
                                 splitConfiguration.bufferSizeInBytes();

        this.splittingSubscriber = new SplittingSubscriber(upstreamPublisher.contentLength().orElse(null));

        if (!upstreamPublisher.contentLength().isPresent()) {
            Validate.isTrue(bufferSizeInBytes >= chunkSizeInBytes,
                            "bufferSizeInBytes must be larger than or equal to " +
                            "chunkSizeInBytes if the content length is unknown");
        }
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
        private volatile boolean upstreamComplete;

        SplittingSubscriber(Long upstreamSize) {
            this.upstreamSize = upstreamSize;
        }

        @Override
        public void onSubscribe(Subscription s) {
            this.upstreamSubscription = s;
            this.currentBody =
                initializeNextDownstreamBody(upstreamSize != null, calculateChunkSize(upstreamSize),
                                             chunkNumber.get());
            // We need to request subscription *after* we set currentBody because onNext could be invoked right away.
            upstreamSubscription.request(1);
        }

        private DownstreamBody initializeNextDownstreamBody(boolean contentLengthKnown, long chunkSize, int chunkNumber) {
            DownstreamBody body = new DownstreamBody(contentLengthKnown, chunkSize, chunkNumber);
            if (contentLengthKnown) {
                sendCurrentBody(body);
            }
            return body;
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            hasOpenUpstreamDemand.set(false);
            byteBufferSizeHint = byteBuffer.remaining();

            while (true) {

                if (!byteBuffer.hasRemaining()) {
                    break;
                }

                int amountRemainingInChunk = amountRemainingInChunk();

                // If we have fulfilled this chunk,
                // complete the current body
                if (amountRemainingInChunk == 0) {
                    completeCurrentBodyAndCreateNewIfNeeded(byteBuffer);
                    amountRemainingInChunk = amountRemainingInChunk();
                }

                // If the current ByteBuffer < this chunk, send it as-is
                if (amountRemainingInChunk > byteBuffer.remaining()) {
                    currentBody.send(byteBuffer.duplicate());
                    break;
                }

                // If the current ByteBuffer == this chunk, send it as-is and
                // complete the current body
                if (amountRemainingInChunk == byteBuffer.remaining()) {
                    currentBody.send(byteBuffer.duplicate());
                    completeCurrentBodyAndCreateNewIfNeeded(byteBuffer);
                    break;
                }

                // If the current ByteBuffer > this chunk, split this ByteBuffer
                ByteBuffer firstHalf = byteBuffer.duplicate();
                int newLimit = firstHalf.position() + amountRemainingInChunk;
                firstHalf.limit(newLimit);
                byteBuffer.position(newLimit);
                currentBody.send(firstHalf);
            }

            maybeRequestMoreUpstreamData();
        }

        private void completeCurrentBodyAndCreateNewIfNeeded(ByteBuffer byteBuffer) {
            completeCurrentBody();
            int currentChunk = chunkNumber.incrementAndGet();
            boolean shouldCreateNewDownstreamRequestBody;
            Long dataRemaining = totalDataRemaining();

            if (upstreamSize == null) {
                shouldCreateNewDownstreamRequestBody = !upstreamComplete || byteBuffer.hasRemaining();
            } else {
                shouldCreateNewDownstreamRequestBody = dataRemaining != null && dataRemaining > 0;
            }

            if (shouldCreateNewDownstreamRequestBody) {
                long chunkSize = calculateChunkSize(dataRemaining);
                currentBody = initializeNextDownstreamBody(upstreamSize != null, chunkSize, currentChunk);
            }
        }

        private int amountRemainingInChunk() {
            return Math.toIntExact(currentBody.maxLength - currentBody.transferredLength);
        }

        private void completeCurrentBody() {
            log.debug(() -> "completeCurrentBody for chunk " + chunkNumber.get());
            currentBody.complete();
            if (upstreamSize == null) {
                sendCurrentBody(currentBody);
            }
        }

        @Override
        public void onComplete() {
            upstreamComplete = true;
            log.trace(() -> "Received onComplete()");
            completeCurrentBody();
            downstreamPublisher.complete();
        }

        @Override
        public void onError(Throwable t) {
            log.trace(() -> "Received onError()", t);
            downstreamPublisher.error(t);
        }

        private void sendCurrentBody(AsyncRequestBody body) {
            downstreamPublisher.send(body).exceptionally(t -> {
                downstreamPublisher.error(t);
                return null;
            });
        }

        private long calculateChunkSize(Long dataRemaining) {
            // Use default chunk size if the content length is unknown
            if (dataRemaining == null) {
                return chunkSizeInBytes;
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
            return buffered == 0 || buffered + byteBufferSizeHint <= bufferSizeInBytes;
        }

        private Long totalDataRemaining() {
            if (upstreamSize == null) {
                return null;
            }
            return upstreamSize - (chunkNumber.get() * chunkSizeInBytes);
        }

        private final class DownstreamBody implements AsyncRequestBody {

            /**
             * The maximum length of the content this AsyncRequestBody can hold. If the upstream content length is known, this is
             * the same as totalLength
             */
            private final long maxLength;
            private final Long totalLength;
            private final SimplePublisher<ByteBuffer> delegate = new SimplePublisher<>();
            private final int chunkNumber;
            private final AtomicBoolean subscribeCalled = new AtomicBoolean(false);
            private volatile long transferredLength = 0;

            private DownstreamBody(boolean contentLengthKnown, long maxLength, int chunkNumber) {
                this.totalLength = contentLengthKnown ? maxLength : null;
                this.maxLength = maxLength;
                this.chunkNumber = chunkNumber;
            }

            @Override
            public Optional<Long> contentLength() {
                return totalLength != null ? Optional.of(totalLength) : Optional.of(transferredLength);
            }

            public void send(ByteBuffer data) {
                log.trace(() -> String.format("Sending bytebuffer %s to chunk %d", data, chunkNumber));
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
                log.debug(() -> "Received complete() for chunk number: " + chunkNumber + " length " + transferredLength);
                delegate.complete().whenComplete((r, t) -> {
                    if (t != null) {
                        error(t);
                    }
                });
            }

            public void error(Throwable error) {
                delegate.error(error);
            }

            @Override
            public void subscribe(Subscriber<? super ByteBuffer> s) {
                if (subscribeCalled.compareAndSet(false, true)) {
                    delegate.subscribe(s);
                } else {
                    s.onSubscribe(new NoopSubscription(s));
                    s.onError(NonRetryableException.create(
                        "A retry was attempted, but AsyncRequestBody.split does not "
                        + "support retries."));
                }
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
