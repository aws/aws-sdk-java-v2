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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncRequestBodySplitConfiguration;
import software.amazon.awssdk.core.async.CloseableAsyncRequestBody;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.async.SimplePublisher;

/**
 * Splits an {@link AsyncRequestBody} to multiple smaller {@link AsyncRequestBody}s, each of which publishes a specific portion of
 * the original data.
 *
 * <p>If content length is known, each {@link AsyncRequestBody} is sent to the subscriber right after it's initialized.
 * Otherwise, it is sent after the entire content for that chunk is buffered. This is required to get content length.
 * When {@code bufferBeforeSend} is set to {@code true}, the known-content-length path also defers sending until the
 * part is fully buffered, guaranteeing that the retry buffer is populated before the downstream subscriber receives it.
 */
@SdkInternalApi
public class SplittingPublisher implements SdkPublisher<CloseableAsyncRequestBody> {
    private static final Logger log = Logger.loggerFor(SplittingPublisher.class);
    private final AsyncRequestBody upstreamPublisher;
    private final SplittingSubscriber splittingSubscriber;
    private final SimplePublisher<CloseableAsyncRequestBody> downstreamPublisher = new SimplePublisher<>();
    private final long chunkSizeInBytes;
    private final long bufferSizeInBytes;
    private final boolean retryableSubAsyncRequestBodyEnabled;
    private final boolean bufferBeforeSend;
    private final AtomicBoolean currentBodySent = new AtomicBoolean(false);
    private final String sourceBodyName;

    private SplittingPublisher(Builder builder) {
        this.upstreamPublisher = Validate.paramNotNull(builder.asyncRequestBody, "asyncRequestBody");
        Validate.notNull(builder.splitConfiguration, "splitConfiguration");
        this.chunkSizeInBytes = builder.splitConfiguration.chunkSizeInBytes() == null ?
                                AsyncRequestBodySplitConfiguration.defaultConfiguration().chunkSizeInBytes() :
                                builder.splitConfiguration.chunkSizeInBytes();

        this.bufferSizeInBytes = builder.splitConfiguration.bufferSizeInBytes() == null ?
                                 AsyncRequestBodySplitConfiguration.defaultConfiguration().bufferSizeInBytes() :
                                 builder.splitConfiguration.bufferSizeInBytes();

        this.splittingSubscriber = new SplittingSubscriber(upstreamPublisher.contentLength().orElse(null));

        this.retryableSubAsyncRequestBodyEnabled = Validate.paramNotNull(builder.retryableSubAsyncRequestBodyEnabled,
                                                                         "retryableSubAsyncRequestBodyEnabled");
        this.bufferBeforeSend = builder.bufferBeforeSend;
        this.sourceBodyName = builder.asyncRequestBody.body();
        if (!upstreamPublisher.contentLength().isPresent() || bufferBeforeSend) {
            Validate.isTrue(bufferSizeInBytes >= chunkSizeInBytes,
                            "bufferSizeInBytes must be larger than or equal to " +
                            "chunkSizeInBytes when the content length is unknown or bufferBeforeSend is enabled");
        }
    }

    /**
     * Returns a newly initialized builder object for a {@link SplittingPublisher}
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void subscribe(Subscriber<? super CloseableAsyncRequestBody> downstreamSubscriber) {
        downstreamPublisher.subscribe(downstreamSubscriber);
        try {
            upstreamPublisher.subscribe(splittingSubscriber);
        } catch (Throwable t) {
            splittingSubscriber.failStream(t);
        }
    }

    private class SplittingSubscriber implements Subscriber<ByteBuffer> {
        private Subscription upstreamSubscription;
        private final Long upstreamSize;
        /**
         * 1 based index number for each part/chunk
         */
        private final AtomicInteger partNumber = new AtomicInteger(1);
        private volatile SubAsyncRequestBody currentBody;
        private final AtomicBoolean hasOpenUpstreamDemand = new AtomicBoolean(false);
        private final AtomicLong dataBuffered = new AtomicLong(0);
        private final AtomicBoolean streamTerminated = new AtomicBoolean(false);

        /**
         * A hint to determine whether we will exceed maxMemoryUsage by the next OnNext call.
         */
        private int byteBufferSizeHint;
        private volatile boolean upstreamComplete;
        private volatile boolean upstreamTerminated;

        SplittingSubscriber(Long upstreamSize) {
            this.upstreamSize = upstreamSize;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (s == null) {
                throw new NullPointerException("s must not be null");
            }
            this.upstreamSubscription = s;
            try {
                initializeNextDownstreamBody(upstreamSize != null, calculateChunkSize(upstreamSize),
                                             partNumber.get());
                // We need to request subscription *after* we set currentBody because onNext could be invoked right away.
                requestUpstream();
            } catch (Throwable t) {
                failStream(t);
            }
        }

        private void initializeNextDownstreamBody(boolean contentLengthKnown, long chunkSize, int chunkNumber) {
            SubAsyncRequestBody body;
            log.debug(() -> "initializing next downstream body " + partNumber);

            SubAsyncRequestBodyConfiguration config = SubAsyncRequestBodyConfiguration.builder()
                    .contentLengthKnown(contentLengthKnown)
                    .maxLength(chunkSize)
                    .partNumber(chunkNumber)
                    .onNumBytesReceived(data -> addDataBuffered(data))
                    .onNumBytesConsumed(data -> addDataBuffered(-data))
                    .sourceBodyName(sourceBodyName)
                    .build();
            
            if (retryableSubAsyncRequestBodyEnabled) {
                body = new RetryableSubAsyncRequestBody(config);
            } else {
                body = new NonRetryableSubAsyncRequestBody(config);
            }

            currentBodySent.set(false);
            currentBody = body;
            if (contentLengthKnown && !bufferBeforeSend) {
                sendCurrentBody(body);
            }
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            if (byteBuffer == null) {
                throw new NullPointerException("byteBuffer must not be null");
            }
            if (streamTerminated.get()) {
                return;
            }

            try {
                processOnNext(byteBuffer);
            } catch (Throwable t) {
                failStream(t);
            }
        }

        private void processOnNext(ByteBuffer byteBuffer) {
            hasOpenUpstreamDemand.set(false);
            byteBufferSizeHint = byteBuffer.remaining();

            while (true) {

                if (!byteBuffer.hasRemaining() || streamTerminated.get()) {
                    break;
                }

                long amountRemainingInChunk = amountRemainingInChunk();

                // If we have fulfilled this chunk,
                // complete the current body
                if (amountRemainingInChunk == 0) {
                    completeCurrentBodyAndCreateNewIfNeeded(byteBuffer);
                    if (streamTerminated.get()) {
                        break;
                    }
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
                int newLimit = firstHalf.position() + Math.toIntExact(amountRemainingInChunk);
                firstHalf.limit(newLimit);
                byteBuffer.position(newLimit);
                currentBody.send(firstHalf);
            }

            maybeRequestMoreUpstreamData();
        }

        private void completeCurrentBodyAndCreateNewIfNeeded(ByteBuffer byteBuffer) {
            completeCurrentBody();
            if (streamTerminated.get()) {
                return;
            }
            int nextChunk = partNumber.incrementAndGet();
            boolean shouldCreateNewDownstreamRequestBody;
            Long dataRemaining = totalDataRemaining();

            if (upstreamSize == null) {
                shouldCreateNewDownstreamRequestBody = !upstreamComplete || byteBuffer.hasRemaining();
            } else {
                shouldCreateNewDownstreamRequestBody = dataRemaining != null && dataRemaining > 0;
            }

            if (shouldCreateNewDownstreamRequestBody) {
                long chunkSize = calculateChunkSize(dataRemaining);
                initializeNextDownstreamBody(upstreamSize != null, chunkSize, nextChunk);
            }
        }

        private long amountRemainingInChunk() {
            return currentBody.maxLength() - currentBody.receivedBytesLength();
        }


        private void completeCurrentBody() {
            log.debug(() -> "completeCurrentBody for part " + currentBody.partNumber());
            long bufferedLength = currentBody.receivedBytesLength();
            // For unknown content length, we always create a new DownstreamBody once the current one is sent
            // because we don't know if there is data
            // left or not, so we need to check the length and only send the body if there is actually data
            if (bufferedLength == 0) {
                return;
            }

            Long totalLength = currentBody.maxLength();
            if (upstreamSize != null && totalLength != bufferedLength) {
                failStream(new IllegalStateException(
                    String.format("Content length of buffered data mismatches "
                                  + "with the expected content length, buffered data content length: %d, "
                                  + "expected length: %d", bufferedLength,
                                  totalLength)));
                return;
            }
            currentBody.complete();

            // Current body could be completed in either onNext or onComplete, so we need to guard against sending the last body
            // twice.
            if ((upstreamSize == null || bufferBeforeSend) && currentBodySent.compareAndSet(false, true)) {
                sendCurrentBody(currentBody);
            }
        }

        @Override
        public void onComplete() {
            if (streamTerminated.get()) {
                return;
            }
            upstreamTerminated = true;
            upstreamComplete = true;
            log.debug(() -> "Received onComplete()");
            try {
                completeCurrentBody();
                if (streamTerminated.compareAndSet(false, true)) {
                    downstreamPublisher.complete();
                }
            } catch (Throwable t) {
                failStream(t);
            }
        }

        @Override
        public void onError(Throwable t) {
            if (t == null) {
                throw new NullPointerException("t must not be null");
            }
            upstreamTerminated = true;
            log.debug(() -> "Received onError()", t);
            failStream(t);
        }

        private void sendCurrentBody(SubAsyncRequestBody body) {
            log.debug(() -> "sendCurrentBody for part " + body.partNumber());
            downstreamPublisher.send(body).exceptionally(t -> {
                failStream(t);
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
            if (streamTerminated.get() || upstreamTerminated) {
                return;
            }
            long buffered = dataBuffered.get();
            if (shouldRequestMoreData(buffered) &&
                hasOpenUpstreamDemand.compareAndSet(false, true)) {
                log.trace(() -> "Requesting more data, current data buffered: " + buffered);
                requestUpstream();
            } else {
                log.trace(() -> "Should not request more data, current data buffered: " + buffered);
            }
        }

        private void requestUpstream() {
            if (streamTerminated.get() || upstreamTerminated) {
                return;
            }
            try {
                upstreamSubscription.request(1);
            } catch (Throwable t) {
                failStream(t);
            }
        }

        private void failStream(Throwable t) {
            if (!streamTerminated.compareAndSet(false, true)) {
                return;
            }

            // Reactive Streams rule 2.3 prohibits calling Subscription methods from onError or onComplete.
            if (!upstreamTerminated) {
                cancelUpstream();
            }
            SubAsyncRequestBody body = currentBody;
            if (body != null) {
                try {
                    body.error(t);
                } catch (Throwable bodyError) {
                    log.debug(() -> "Failed to signal an error to the active split request body", bodyError);
                }
            }
            downstreamPublisher.error(t);
        }

        private void cancelUpstream() {
            Subscription subscription = upstreamSubscription;
            if (subscription == null) {
                return;
            }

            try {
                subscription.cancel();
            } catch (Throwable t) {
                log.debug(() -> "Upstream Subscription.cancel() threw an exception", t);
            }
        }

        private boolean shouldRequestMoreData(long buffered) {
            return buffered <= 0 || buffered + byteBufferSizeHint <= bufferSizeInBytes;
        }

        private Long totalDataRemaining() {
            if (upstreamSize == null) {
                return null;
            }
            return upstreamSize - ((partNumber.get() - 1) * chunkSizeInBytes);
        }

        private void addDataBuffered(long length) {
            log.trace(() -> "Adding data buffered " + length);
            dataBuffered.addAndGet(length);
            if (length < 0) {
                maybeRequestMoreUpstreamData();
            }
        }
    }

    public static final class Builder {
        private AsyncRequestBody asyncRequestBody;
        private AsyncRequestBodySplitConfiguration splitConfiguration;
        private Boolean retryableSubAsyncRequestBodyEnabled;
        private boolean bufferBeforeSend = false;

        private Builder() {
        }

        /**
         * Sets the AsyncRequestBody to be split.
         */
        public Builder asyncRequestBody(AsyncRequestBody asyncRequestBody) {
            this.asyncRequestBody = asyncRequestBody;
            return this;
        }

        /**
         * Sets the split configuration.
         */
        public Builder splitConfiguration(AsyncRequestBodySplitConfiguration splitConfiguration) {
            this.splitConfiguration = splitConfiguration;
            return this;
        }

        /**
         * Sets whether to enable retryable sub async request bodies.
         */
        public Builder retryableSubAsyncRequestBodyEnabled(Boolean retryableSubAsyncRequestBodyEnabled) {
            this.retryableSubAsyncRequestBodyEnabled = retryableSubAsyncRequestBodyEnabled;
            return this;
        }

        /**
         * Sets whether to enable full buffering before sending parts downstream.
         * When enabled, parts are only sent to the downstream subscriber after
         * all data for that part has been received and complete() has been called.
         *
         * <p>This does not increase the maximum memory footprint. Buffered data remains
         * bounded by {@code bufferSizeInBytes} in the split configuration.
         */
        public Builder bufferBeforeSend(boolean bufferBeforeSend) {
            this.bufferBeforeSend = bufferBeforeSend;
            return this;
        }

        /**
         * Builds a {@link SplittingPublisher} object based on the values held by this builder.
         */
        public SplittingPublisher build() {
            return new SplittingPublisher(this);
        }
    }
}
