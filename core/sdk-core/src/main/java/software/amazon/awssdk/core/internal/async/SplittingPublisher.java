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
import software.amazon.awssdk.core.async.ClosableAsyncRequestBody;
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
 */
@SdkInternalApi
public class SplittingPublisher implements SdkPublisher<ClosableAsyncRequestBody> {
    private static final Logger log = Logger.loggerFor(SplittingPublisher.class);
    private final AsyncRequestBody upstreamPublisher;
    private final SplittingSubscriber splittingSubscriber;
    private final SimplePublisher<ClosableAsyncRequestBody> downstreamPublisher = new SimplePublisher<>();
    private final long chunkSizeInBytes;
    private final long bufferSizeInBytes;
    private final boolean enableRetryableSubAsyncRequestBody;
    private final AtomicBoolean currentBodySent = new AtomicBoolean(false);
    private final String sourceBodyName;

    public SplittingPublisher(AsyncRequestBody asyncRequestBody,
                              AsyncRequestBodySplitConfiguration splitConfiguration,
                              boolean enableRetryableSubAsyncRequestBody) {
        this.upstreamPublisher = Validate.paramNotNull(asyncRequestBody, "asyncRequestBody");
        Validate.notNull(splitConfiguration, "splitConfiguration");
        this.chunkSizeInBytes = splitConfiguration.chunkSizeInBytes() == null ?
                                AsyncRequestBodySplitConfiguration.defaultConfiguration().chunkSizeInBytes() :
                                splitConfiguration.chunkSizeInBytes();

        this.bufferSizeInBytes = splitConfiguration.bufferSizeInBytes() == null ?
                                 AsyncRequestBodySplitConfiguration.defaultConfiguration().bufferSizeInBytes() :
                                 splitConfiguration.bufferSizeInBytes();

        this.splittingSubscriber = new SplittingSubscriber(upstreamPublisher.contentLength().orElse(null));

        this.enableRetryableSubAsyncRequestBody = enableRetryableSubAsyncRequestBody;
        this.sourceBodyName = asyncRequestBody.body();
        if (!upstreamPublisher.contentLength().isPresent()) {
            Validate.isTrue(bufferSizeInBytes >= chunkSizeInBytes,
                            "bufferSizeInBytes must be larger than or equal to " +
                            "chunkSizeInBytes if the content length is unknown");
        }
    }

    @Override
    public void subscribe(Subscriber<? super ClosableAsyncRequestBody> downstreamSubscriber) {
        downstreamPublisher.subscribe(downstreamSubscriber);
        upstreamPublisher.subscribe(splittingSubscriber);
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
                                             partNumber.get());
            // We need to request subscription *after* we set currentBody because onNext could be invoked right away.
            upstreamSubscription.request(1);
        }

        private SubAsyncRequestBody initializeNextDownstreamBody(boolean contentLengthKnown, long chunkSize, int chunkNumber) {
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
            
            if (enableRetryableSubAsyncRequestBody) {
                body = new RetryableSubAsyncRequestBody(config);
            } else {
                body = new NonRetryableSubAsyncRequestBody(config);
            }

            currentBodySent.set(false);
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
                currentBody = initializeNextDownstreamBody(upstreamSize != null, chunkSize, nextChunk);
            }
        }

        private int amountRemainingInChunk() {
            return Math.toIntExact(currentBody.maxLength() - currentBody.receivedBytesLength());
        }


        private void completeCurrentBody() {
            log.debug(() -> "completeCurrentBody for part " + currentBody.partNumber());
            // For unknown content length, we always create a new DownstreamBody because we don't know if there is data
            // left or not, so we need to only send the body if there is actually data
            long bufferedLength = currentBody.receivedBytesLength();
            if (bufferedLength == 0) {
                return;
            }

            Long totalLength = currentBody.maxLength();
            if (currentBody.contentLengthKnown() && totalLength != bufferedLength) {
                upstreamSubscription.cancel();
                downstreamPublisher.error(new IllegalStateException(
                    String.format("Content length of buffered data mismatches "
                                  + "with the expected content length, buffered data content length: %d, "
                                  + "expected length: %d", totalLength,
                                  bufferedLength)));
                return;
            }
            currentBody.complete();

            // Current body could be completed in either onNext or onComplete, so we need to guard against sending the last body
            // twice.
            if (upstreamSize == null && currentBodySent.compareAndSet(false, true)) {
                sendCurrentBody(currentBody);
            }
        }

        @Override
        public void onComplete() {
            upstreamComplete = true;
            log.debug(() -> "Received onComplete()");
            completeCurrentBody();
            downstreamPublisher.complete();
        }

        @Override
        public void onError(Throwable t) {
            log.debug(() -> "Received onError()", t);
            downstreamPublisher.error(t);
        }

        private void sendCurrentBody(SubAsyncRequestBody body) {
            log.debug(() -> "sendCurrentBody for part " + body.partNumber());
            downstreamPublisher.send(body).exceptionally(t -> {
                downstreamPublisher.error(t);
                upstreamSubscription.cancel();
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
            } else {
                log.trace(() -> "Should not request more data, current data buffered: " + buffered);
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
}