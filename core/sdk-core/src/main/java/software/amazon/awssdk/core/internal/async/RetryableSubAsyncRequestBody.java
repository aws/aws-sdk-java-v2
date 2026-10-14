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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.NonRetryableException;
import software.amazon.awssdk.core.internal.util.NoopSubscription;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.async.SimplePublisher;

/**
 * A {@link SubAsyncRequestBody} implementation that supports resubscribe/retry once the part has completed, i.e. all of its
 * data was received through {@link #send} and {@link #complete()} was called before {@link #error}.
 *
 * <p>A part is terminated exactly once: by {@link #complete()} or by {@link #error}, whichever comes first. A part failed
 * through {@link #error} before it completed cannot be retried: a resubscribe fails with {@link NonRetryableException}. A
 * later {@link #error} on a completed part is ignored, and the part keeps its retry buffer.
 */
@SdkInternalApi
public final class RetryableSubAsyncRequestBody implements SubAsyncRequestBody {
    private static final Logger log = Logger.loggerFor(RetryableSubAsyncRequestBody.class);
    /**
     * Sentinel for {@link #unreleasedBytes} once {@link #close()} has run.
     */
    private static final long CLOSED = -1;
    /**
     * The maximum length of the content this AsyncRequestBody can hold. If the upstream content length is known, this is
     * the same as totalLength
     */
    private final SubAsyncRequestBodyConfiguration configuration;
    private final int partNumber;
    private final boolean contentLengthKnown;
    private final String sourceBodyName;

    private volatile long bufferedLength = 0;
    /**
     * Bytes counted through {@code onNumBytesReceived} and not yet given back through {@code onNumBytesConsumed}, or
     * {@link #CLOSED}. Each byte is released exactly once: by {@link #close()}, or right away by {@link #send} after close.
     */
    private final AtomicLong unreleasedBytes = new AtomicLong();
    /**
     * The first of {@link #complete()} and {@link #error} wins and decides whether the part can be retried.
     */
    private final AtomicReference<Termination> termination = new AtomicReference<>(Termination.NONE);
    /**
     * The retry body. Only set once the part completed (and was not closed or errored first); {@code null} means a
     * resubscribe fails with {@link NonRetryableException}. It shares {@link #buffers}, whose bytes {@link #close()}
     * releases.
     */
    private volatile ByteBuffersAsyncRequestBody bufferedAsyncRequestBody;
    private List<ByteBuffer> buffers = new ArrayList<>();
    private final AtomicBoolean subscribeCalled = new AtomicBoolean(false);
    private final SimplePublisher<ByteBuffer> delegate = new SimplePublisher<>();
    private final Consumer<Long> onNumBytesReceived;
    private final Consumer<Long> onNumBytesConsumed;
    private final Object buffersLock = new Object();

    /**
     * Creates a new RetryableSubAsyncRequestBody with the given configuration.
     */
    public RetryableSubAsyncRequestBody(SubAsyncRequestBodyConfiguration configuration) {
        this.configuration = Validate.paramNotNull(configuration, "configuration");
        this.partNumber = configuration.partNumber();
        this.contentLengthKnown = configuration.contentLengthKnown();
        this.sourceBodyName = configuration.sourceBodyName();
        this.onNumBytesReceived = configuration.onNumBytesReceived();
        this.onNumBytesConsumed = configuration.onNumBytesConsumed();
    }

    @Override
    public Optional<Long> contentLength() {
        return contentLengthKnown ? Optional.of(configuration.maxLength()) : Optional.of(bufferedLength);
    }

    @Override
    public void send(ByteBuffer data) {
        log.trace(() -> String.format("Sending bytebuffer %s to part number %d", data, partNumber));
        long length = data.remaining();
        bufferedLength += length;

        onNumBytesReceived.accept(length);
        delegate.send(data.asReadOnlyBuffer()).whenComplete((r, t) -> {
            if (t != null) {
                delegate.error(t);
            }
        });
        synchronized (buffersLock) {
            // After close() there is no retry buffer to fill.
            if (buffers != null) {
                buffers.add(data.asReadOnlyBuffer());
            }
        }
        if (unreleasedBytes.getAndUpdate(v -> v == CLOSED ? CLOSED : v + length) == CLOSED) {
            // Already closed: nobody will release these bytes later, so give them back now.
            onNumBytesConsumed.accept(length);
        }
    }

    @Override
    public void complete() {
        log.debug(() -> "Received complete() for part number: " + partNumber);
        // ByteBuffersAsyncRequestBody MUST be created before we complete the current
        // request because retry may happen right after
        synchronized (buffersLock) {
            // Closed or errored before complete: do not build a retry body.
            if (buffers != null && termination.get() == Termination.NONE) {
                bufferedAsyncRequestBody = ByteBuffersAsyncRequestBody.of(buffers, bufferedLength);
            }
        }
        if (!termination.compareAndSet(Termination.NONE, Termination.COMPLETED)) {
            if (termination.get() == Termination.ERRORED) {
                // error() won the race. Drop the retry body; it shares buffers, which close() still clears and releases.
                synchronized (buffersLock) {
                    bufferedAsyncRequestBody = null;
                }
                log.debug(() -> "Ignoring complete() for part number " + partNumber + ", it already failed");
            } else {
                // SplittingPublisher can complete the last part twice (onNext and onComplete); keep the retry body.
                log.debug(() -> "Ignoring repeated complete() for part number " + partNumber);
            }
            return;
        }
        delegate.complete().exceptionally(e -> {
            delegate.error(e);
            return null;
        });
    }

    @Override
    public long maxLength() {
        return configuration.maxLength();
    }

    @Override
    public void error(Throwable error) {
        // Set ERRORED before delegate.error() delivers onError, so a retry triggered by that error never sees a retry body.
        if (termination.compareAndSet(Termination.NONE, Termination.ERRORED)) {
            delegate.error(error);
        } else {
            log.debug(() -> "Ignoring error() for part number " + partNumber + ", it already terminated", error);
        }
    }

    @Override
    public long receivedBytesLength() {
        return bufferedLength;
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> s) {
        log.debug(() -> "Subscribe for part number: " + partNumber);
        if (subscribeCalled.compareAndSet(false, true)) {
            delegate.subscribe(s);
        } else {
            log.debug(() -> "Resubscribe for part number " + partNumber);
            ByteBuffersAsyncRequestBody retryBody = bufferedAsyncRequestBody;
            if (termination.get() == Termination.ERRORED || retryBody == null) {
                s.onSubscribe(new NoopSubscription(s));
                s.onError(NonRetryableException.create(
                    "A retry was attempted, but data is not buffered successfully for retry for partNumber: " + partNumber));
                return;
            }
            retryBody.subscribe(s);
        }
    }

    @Override
    public void close() {
        long unreleased = unreleasedBytes.getAndSet(CLOSED);
        if (unreleased == CLOSED) {
            log.debug(() -> "Body " + partNumber + " already closed");
            return;
        }
        try {
            log.debug(() -> "Closing current body " + partNumber);
            onNumBytesConsumed.accept(unreleased);
            synchronized (buffersLock) {
                if (buffers != null) {
                    buffers.clear();
                    buffers = null;
                }
            }
            ByteBuffersAsyncRequestBody retryBody = bufferedAsyncRequestBody;
            if (retryBody != null) {
                retryBody.close();
                bufferedAsyncRequestBody = null;
            }
        } catch (Throwable e) {
            log.warn(() -> String.format("Unexpected error thrown from cleaning up AsyncRequestBody for part number %d, "
                                         + "resource may be leaked", partNumber));
        }
    }

    @Override
    public int partNumber() {
        return partNumber;
    }

    @Override
    public String body() {
        return sourceBodyName;
    }

    private enum Termination {
        NONE,
        COMPLETED,
        ERRORED
    }
}
