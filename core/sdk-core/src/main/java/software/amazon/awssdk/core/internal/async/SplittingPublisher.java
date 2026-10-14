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
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncRequestBodySplitConfiguration;
import software.amazon.awssdk.core.async.CloseableAsyncRequestBody;
import software.amazon.awssdk.core.async.SdkPublisher;
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
 * When {@code bufferBeforeSend} is set to {@code true}, the known-content-length path also defers sending until the
 * part is fully buffered, guaranteeing that the retry buffer is populated before the downstream subscriber receives it.
 *
 * <p>Cancellation: when the downstream subscriber cancels its subscription, the upstream subscription is cancelled right
 * away (or never made, if the downstream cancelled from within {@code onSubscribe}), and the part that is still being
 * filled, if any, is failed: its subscriber receives the data already buffered for that part and then {@code onError}
 * with a {@link CancellationException}. A part subscriber with no outstanding demand gets the {@code onError} once it
 * requests the buffered data, and a part that has not been subscribed to delivers both to its first subscriber. Parts
 * that were already complete are not affected.
 *
 * <p>If the upstream {@link Subscription} throws from {@code request()} (Reactive Streams rule 3.16), the upstream is
 * cancelled and the downstream and the in-progress part fail with an {@link IllegalStateException}.
 *
 * <p>Only one subscriber is supported. Every call on the upstream {@link Subscription} is serialized (Reactive Streams
 * rule 2.7), regardless of whether it originates from the upstream thread, a part consumer, or the downstream canceller.
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
    private final AtomicBoolean subscribed = new AtomicBoolean(false);
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

    private static CancellationException downstreamCancelledException() {
        return new CancellationException("The downstream subscription of the split AsyncRequestBody was cancelled");
    }

    @Override
    public void subscribe(Subscriber<? super CloseableAsyncRequestBody> downstreamSubscriber) {
        Validate.paramNotNull(downstreamSubscriber, "downstreamSubscriber");
        if (!subscribed.compareAndSet(false, true)) {
            // Reject without touching the upstream: re-subscribing it would start a second read into the same splitter.
            downstreamSubscriber.onSubscribe(new NoopSubscription(downstreamSubscriber));
            downstreamSubscriber.onError(new IllegalStateException("Only one subscription may be active at a time."));
            return;
        }
        downstreamPublisher.subscribe(new CancelObservingSubscriber<>(downstreamSubscriber,
                                                                      splittingSubscriber::markDownstreamCancelled,
                                                                      splittingSubscriber::propagateDownstreamCancel));
        if (splittingSubscriber.isStopped()) {
            log.debug(() -> "Downstream cancelled during onSubscribe, not subscribing to the upstream");
            return;
        }
        upstreamPublisher.subscribe(splittingSubscriber);
    }

    private class SplittingSubscriber implements Subscriber<ByteBuffer> {
        private final UpstreamSignals upstream = new UpstreamSignals(this::onUpstreamRequestFailure);
        private final Long upstreamSize;
        /**
         * 1 based index number for each part/chunk
         */
        private final AtomicInteger partNumber = new AtomicInteger(1);
        private volatile SubAsyncRequestBody currentBody;
        private final AtomicBoolean hasOpenUpstreamDemand = new AtomicBoolean(false);
        private final AtomicLong dataBuffered = new AtomicLong(0);
        /**
         * Why the splitter stopped: the downstream cancelled, or the upstream subscription failed. {@code null} while
         * running. The first cause wins. Once set, the splitter creates no more parts and drops late upstream signals.
         */
        private final AtomicReference<Throwable> stopCause = new AtomicReference<>();
        /**
         * The exception a downstream cancel offers as {@link #stopCause}. Set at most once, before that offer, so that a
         * thread that reads it from {@code stopCause} can tell that the downstream cancelled and must not be signalled.
         */
        private final AtomicReference<CancellationException> downstreamCancelCause = new AtomicReference<>();
        private final AtomicBoolean cancelPropagated = new AtomicBoolean(false);

        /**
         * A hint to determine whether we will exceed maxMemoryUsage by the next OnNext call.
         */
        private volatile int byteBufferSizeHint;
        private volatile boolean upstreamComplete;

        SplittingSubscriber(Long upstreamSize) {
            this.upstreamSize = upstreamSize;
        }

        boolean isStopped() {
            return stopCause.get() != null;
        }

        /**
         * First half of a downstream cancel, invoked synchronously from the downstream subscriber's
         * {@code Subscription.cancel()} on the canceller's thread, before the cancel reaches {@link SimplePublisher}.
         * Stops the splitter so that later upstream signals are dropped.
         *
         * <p>Writes the stop state that {@link #propagateDownstreamCancel()} later pairs with a read of {@code currentBody};
         * see there.
         */
        void markDownstreamCancelled() {
            CancellationException cause = downstreamCancelledException();
            if (downstreamCancelCause.compareAndSet(null, cause)) {
                stopCause.compareAndSet(null, cause);
            }
        }

        /**
         * Records {@code cause} as the stop cause unless the splitter already stopped, and returns the cause that won.
         */
        private Throwable stop(Throwable cause) {
            return stopCause.compareAndSet(null, cause) ? cause : stopCause.get();
        }

        /**
         * Fails the in-progress part and the downstream with {@code cause}. If {@code cause} is the downstream's own cancel,
         * the downstream is not signalled: it is cancelling, and may not have reached {@link SimplePublisher} yet.
         */
        private void signalFailure(Throwable cause) {
            SubAsyncRequestBody body = currentBody;
            if (body != null) {
                body.error(cause);
            }
            if (cause != downstreamCancelCause.get()) {
                downstreamPublisher.error(cause);
            }
        }

        /**
         * Second half of a downstream cancel, invoked after {@link SimplePublisher} has recorded the cancel: cancels the
         * upstream and fails the in-progress part. Runs at most once.
         *
         * <p>Pairs with {@link #failCurrentBodyIfStopped()}: the canceller writes {@code stopCause} (in
         * {@link #markDownstreamCancelled()}) and then reads {@code currentBody} here; part creation writes
         * {@code currentBody} then reads {@code stopCause}. Both are volatile, so at least one side sees the other and the
         * in-progress part is always failed.
         */
        void propagateDownstreamCancel() {
            if (!cancelPropagated.compareAndSet(false, true)) {
                return;
            }
            log.debug(() -> "Downstream cancelled, cancelling the upstream subscription");
            upstream.cancel();
            SubAsyncRequestBody body = currentBody;
            if (body != null) {
                body.error(stopCause.get());
            }
        }

        /**
         * Invoked by {@link UpstreamSignals} when the upstream {@code Subscription.request()} threw, right before it
         * cancels the upstream. Fail the stream so that it does not wait forever for data that was never requested.
         * Recording the cause before the cancel keeps it from being replaced by an {@code onError} that the upstream
         * signals from its own {@code cancel()} (rule 1.8).
         */
        private void onUpstreamRequestFailure(Throwable t) {
            IllegalStateException failure = new IllegalStateException(
                "The upstream Subscription.request() threw an exception, which violates Reactive Streams rule 3.16", t);
            log.debug(() -> "Upstream Subscription.request() threw, failing the split stream", t);
            failStream(failure);
        }

        /**
         * Stops the splitter, cancels the upstream, and fails the in-progress part and the downstream. If the splitter had
         * already stopped (for example a downstream cancel whose pending sends now fail), the earlier cause is used, and a
         * downstream that cancelled is not signalled (see {@link #signalFailure}). Every step is idempotent, so this is safe
         * to call more than once. Returns the cause that was used.
         *
         * <p>Like {@link #propagateDownstreamCancel()}, writes {@code stopCause} before reading {@code currentBody}, which
         * pairs with {@link #failCurrentBodyIfStopped()} on the part-creation side.
         */
        private Throwable failStream(Throwable cause) {
            Throwable winner = stop(cause);
            upstream.cancel();
            signalFailure(winner);
            return winner;
        }

        private boolean failCurrentBodyIfStopped() {
            Throwable cause = stopCause.get();
            if (cause == null) {
                return false;
            }
            SubAsyncRequestBody body = currentBody;
            if (body != null) {
                body.error(cause);
            }
            return true;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (!upstream.setUpstream(s)) {
                log.warn(() -> "Received a second upstream subscription, cancelling it");
                s.cancel();
                return;
            }
            if (isStopped()) {
                upstream.cancel();
                return;
            }
            initializeNextDownstreamBody(upstreamSize != null, calculateChunkSize(upstreamSize), partNumber.get());
            if (failCurrentBodyIfStopped()) {
                return;
            }
            // We need to request subscription *after* we set currentBody because onNext could be invoked right away.
            upstream.request(1);
        }

        /**
         * Creates the next part, assigns it to {@link #currentBody} and, in the known-length / no-buffer-before-send mode,
         * sends it downstream. {@code currentBody} is assigned before the send so a cancel from within the downstream
         * {@code onNext} can see (and fail) it.
         */
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
            hasOpenUpstreamDemand.set(false);
            if (isStopped()) {
                // The splitter has stopped (downstream cancelled or upstream failed); drop late signals (rule 2.8).
                return;
            }
            byteBufferSizeHint = byteBuffer.remaining();

            while (true) {

                if (!byteBuffer.hasRemaining() || isStopped()) {
                    break;
                }

                int amountRemainingInChunk = amountRemainingInChunk();

                // If we have fulfilled this chunk,
                // complete the current body
                if (amountRemainingInChunk == 0) {
                    completeCurrentBodyAndCreateNewIfNeeded(byteBuffer);
                    if (isStopped()) {
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

            if (shouldCreateNewDownstreamRequestBody && !isStopped()) {
                long chunkSize = calculateChunkSize(dataRemaining);
                initializeNextDownstreamBody(upstreamSize != null, chunkSize, nextChunk);
                failCurrentBodyIfStopped();
            }
        }

        private int amountRemainingInChunk() {
            return Math.toIntExact(currentBody.maxLength() - currentBody.receivedBytesLength());
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
                                  + "expected length: %d", totalLength,
                                  bufferedLength)));
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
            upstreamComplete = true;
            log.debug(() -> "Received onComplete()");
            if (currentBody != null && !isStopped()) {
                completeCurrentBody();
            }
            downstreamPublisher.complete();
        }

        @Override
        public void onError(Throwable t) {
            log.debug(() -> "Received onError()", t);
            // If the splitter already stopped, keep the original cause: the upstream may be erroring because the splitter
            // cancelled it (rule 1.8), possibly from inside that cancel() call.
            signalFailure(stop(t));
        }

        private void sendCurrentBody(SubAsyncRequestBody body) {
            log.debug(() -> "sendCurrentBody for part " + body.partNumber());
            downstreamPublisher.send(body).exceptionally(t -> {
                Throwable cause = failStream(t);
                // The part may already be in the downstream's hands (e.g. its onNext threw) and may no longer be
                // currentBody; make sure it terminates.
                body.error(cause);
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
            if (isStopped()) {
                return;
            }
            long buffered = dataBuffered.get();
            if (shouldRequestMoreData(buffered) &&
                hasOpenUpstreamDemand.compareAndSet(false, true)) {
                log.trace(() -> "Requesting more data, current data buffered: " + buffered);
                upstream.request(1);
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

    /**
     * Serializes every call on the upstream {@link Subscription} (Reactive Streams rule 2.7). {@code request(1)} can come from
     * the upstream thread ({@code onNext}) or from part consumers (bytes released), and {@code cancel()} from the downstream
     * canceller or a failed send. Callers record their intent and whichever thread wins {@code wip} issues the calls, so
     * a synchronous upstream's {@code request -> onNext -> request} recursion also becomes iteration. Once cancelled, later
     * requests are dropped.
     *
     * <p>A cancel issued while another thread is inside {@code request(n)} is performed by that thread once
     * {@code request(n)} returns, because rule 2.7 forbids calling {@code cancel()} concurrently. An upstream whose
     * {@code request()} blocks (a rule 3.4 violation) therefore delays the cancel until it returns.
     *
     * <p>Fails closed: if {@code request(n)} throws (a rule 3.16 violation), later calls are dropped, {@code onRequestFailure}
     * is invoked with the exception so the caller can fail the stream instead of waiting for data that was never requested,
     * and then the upstream is cancelled. A throwing {@code cancel()} (a rule 3.15 violation) is logged and
     * swallowed: the stream is already being stopped and the callers deliver their own terminal signals.
     */
    private static final class UpstreamSignals {
        private final AtomicInteger wip = new AtomicInteger();
        private final AtomicLong pendingRequests = new AtomicLong();
        private final AtomicBoolean upstreamSet = new AtomicBoolean();
        private final Consumer<Throwable> onRequestFailure;
        private volatile Subscription subscription;
        private volatile boolean cancelRequested;
        /** Only read and written by the thread that holds {@code wip}. */
        private boolean cancelled;

        UpstreamSignals(Consumer<Throwable> onRequestFailure) {
            this.onRequestFailure = onRequestFailure;
        }

        boolean setUpstream(Subscription s) {
            if (!upstreamSet.compareAndSet(false, true)) {
                return false;
            }
            subscription = s;
            drain();
            return true;
        }

        void request(long n) {
            pendingRequests.getAndUpdate(current -> Long.MAX_VALUE - current < n ? Long.MAX_VALUE : current + n);
            drain();
        }

        void cancel() {
            cancelRequested = true;
            drain();
        }

        private void drain() {
            if (wip.getAndIncrement() != 0) {
                return;
            }
            int missed = 1;
            do {
                Subscription s = subscription;
                if (s != null && !cancelled) {
                    if (cancelRequested) {
                        cancelled = true;
                        pendingRequests.set(0);
                        cancelQuietly(s);
                    } else {
                        long n = pendingRequests.getAndSet(0);
                        if (n > 0) {
                            try {
                                s.request(n);
                            } catch (Throwable t) {
                                // Still holding wip: re-entrant request()/cancel() calls from the callback are no-ops.
                                cancelled = true;
                                pendingRequests.set(0);
                                // Report before cancelling, so the failure is recorded before the upstream can signal
                                // onError from its own cancel() (rule 1.8).
                                try {
                                    onRequestFailure.accept(t);
                                } finally {
                                    cancelQuietly(s);
                                }
                            }
                        }
                    }
                }
                missed = wip.addAndGet(-missed);
            } while (missed != 0);
        }

        private static void cancelQuietly(Subscription s) {
            try {
                s.cancel();
            } catch (Throwable t) {
                log.warn(() -> "Upstream Subscription.cancel() threw (rule 3.15 violation)", t);
            }
        }
    }

    /**
     * Wraps the downstream subscriber so that {@code Subscription.cancel()} is observed synchronously. {@link SimplePublisher}
     * defers a cancel issued inside {@code onSubscribe} and has no cancel callback, so the splitter could otherwise only learn
     * about a cancel on its next {@code send()}. Holds no reference back to {@link SplittingPublisher} state other than the
     * callbacks; {@link SimplePublisher} drops this wrapper (and so the downstream subscriber) on cancel (rule 3.13).
     *
     * <p>{@code cancel()} runs in three steps. {@code markCancelled} first stops the splitter, so it skips further work.
     * The cancel is then forwarded to {@link SimplePublisher}, so it records the cancel before anything else happens.
     * {@code propagateCancel} then cancels the upstream and fails the in-progress part. The upstream may signal
     * {@code onComplete}/{@code onError} synchronously from its own {@code cancel()} (rule 1.8); because
     * {@link SimplePublisher} already holds the cancel, that terminal signal is dropped instead of reaching the downstream
     * re-entrantly inside its {@code cancel()} call. When the cancel comes from inside the downstream {@code onNext} (the
     * queue-processing thread), the cancel entry sits in {@link SimplePublisher}'s high-priority queue and is processed
     * before any later terminal entry.
     */
    private static final class CancelObservingSubscriber<T> implements Subscriber<T> {
        private final Subscriber<? super T> delegate;
        private final Runnable markCancelled;
        private final Runnable propagateCancel;

        private CancelObservingSubscriber(Subscriber<? super T> delegate, Runnable markCancelled, Runnable propagateCancel) {
            this.delegate = delegate;
            this.markCancelled = markCancelled;
            this.propagateCancel = propagateCancel;
        }

        @Override
        public void onSubscribe(Subscription s) {
            delegate.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    s.request(n);
                }

                @Override
                public void cancel() {
                    markCancelled.run();
                    try {
                        s.cancel();
                    } finally {
                        propagateCancel.run();
                    }
                }
            });
        }

        @Override
        public void onNext(T t) {
            delegate.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            delegate.onError(t);
        }

        @Override
        public void onComplete() {
            delegate.onComplete();
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
