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

package software.amazon.awssdk.services.s3.internal.crt;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.ApiCallAttemptTimeoutException;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.Abortable;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * An {@link InputStream} that reads CRT pooled buffers, delivered as {@link S3CrtBorrowedBufferLease}s.
 *
 * <p>Fully reading a lease returns its memory to the pool and credits the CRT read window by its byte count.
 *
 * <p>A read waits only when nothing is queued. It returns the queued bytes, which may be fewer than requested. If the end or
 * a failure arrives partway through a read, that read returns the bytes it copied and the next read reports it. Reads are
 * serialized.
 *
 * <p>{@link #close()} and {@link #abort()} may be called from any thread, including during a blocked read. They release unread
 * leases and cancel the request if it is still running.
 */
@SdkInternalApi
public final class S3CrtBorrowedBufferInputStream extends InputStream implements Abortable {

    private static final Logger log = Logger.loggerFor(S3CrtBorrowedBufferInputStream.class);

    private enum Signal {
        COMPLETE,
        CLOSED
    }

    private static final class CloseReason {
        private final Throwable cause;

        private CloseReason(Throwable cause) {
            this.cause = cause;
        }
    }

    private final BlockingQueue<Object> events = new LinkedBlockingQueue<>();
    // Lock order: readLock, stateLock
    private final Object readLock = new Object();
    private final Object stateLock = new Object();
    private final Runnable cancellationAction;
    private final AtomicReference<CloseReason> closeReason = new AtomicReference<>();
    private final AtomicBoolean cancellationStarted = new AtomicBoolean();

    private volatile boolean accepting = true;
    private volatile boolean upstreamTerminal;
    private volatile boolean complete;
    private volatile Throwable readFailure;
    private S3CrtBorrowedBufferLease current;

    S3CrtBorrowedBufferInputStream(Runnable cancellationAction) {
        this.cancellationAction = cancellationAction;
    }

    boolean onBuffer(S3CrtBorrowedBufferLease buffer) {
        boolean accepted;
        synchronized (stateLock) {
            accepted = accepting;
            if (accepted) {
                enqueue(buffer);
            }
        }
        if (!accepted) {
            buffer.discard();
        }
        return accepted;
    }

    void onError(Throwable error) {
        synchronized (stateLock) {
            if (accepting) {
                accepting = false;
                upstreamTerminal = true;
                enqueue(error);
            }
        }
    }

    void onComplete() {
        synchronized (stateLock) {
            if (accepting) {
                accepting = false;
                upstreamTerminal = true;
                enqueue(Signal.COMPLETE);
            }
        }
    }

    @Override
    public int read() throws IOException {
        byte[] singleByte = new byte[1];
        int result = read(singleByte, 0, 1);
        return result < 0 ? result : singleByte[0] & 0xff;
    }

    @Override
    public int read(byte[] destination, int offset, int length) throws IOException {
        Objects.requireNonNull(destination, "destination");
        if (offset < 0 || length < 0 || length > destination.length - offset) {
            throw new IndexOutOfBoundsException();
        }
        if (length == 0) {
            return 0;
        }

        synchronized (readLock) {
            ensureReadable();
            if (complete) {
                return -1;
            }
            int copied = 0;
            while (copied < length) {
                if (current == null) {
                    Object event = events.poll();
                    if (event == null && copied == 0) {
                        event = takeEvent();
                    }
                    if (event == null) {
                        return copied;
                    }
                    if (event instanceof S3CrtBorrowedBufferLease) {
                        current = (S3CrtBorrowedBufferLease) event;
                    } else if (event == Signal.COMPLETE) {
                        complete = true;
                        return copied == 0 ? -1 : copied;
                    } else if (event == Signal.CLOSED) {
                        return copied == 0 ? throwClosed(closeReason.get()) : copied;
                    } else {
                        readFailure = (Throwable) event;
                        return copied == 0 ? throwFailure(readFailure) : copied;
                    }
                }

                CloseReason terminalReason = closeReason.get();
                if (terminalReason != null) {
                    return copied == 0 ? throwClosed(terminalReason) : copied;
                }
                ByteBuffer buffer = current.buffer();
                int count = Math.min(buffer.remaining(), length - copied);
                buffer.get(destination, offset + copied, count);
                copied += count;
                if (!buffer.hasRemaining()) {
                    S3CrtBorrowedBufferLease consumed = current;
                    current = null;
                    try {
                        consumed.consumed();
                    } catch (Throwable t) {
                        close(t);
                        return copied == 0 ? throwFailure(t) : copied;
                    }
                }
            }
            return copied;
        }
    }

    @Override
    public void close() {
        close(null);
    }

    @Override
    public void abort() {
        close();
    }

    void abort(Throwable error) {
        close(Validate.paramNotNull(error, "error"));
    }

    private void close(Throwable error) {
        if (!closeReason.compareAndSet(null, new CloseReason(error))) {
            return;
        }
        synchronized (stateLock) {
            accepting = false;
            // Wake a reader blocked in take() while holding readLock, so we can take readLock below.
            enqueue(Signal.CLOSED);
        }

        Throwable failure = cancelUpstream(null);
        synchronized (readLock) {
            failure = discard(current, failure);
            current = null;
            failure = discardQueued(failure);
        }
        if (failure != null) {
            if (error != null) {
                if (error != failure) {
                    error.addSuppressed(failure);
                }
            } else {
                Throwable cleanupFailure = failure;
                log.warn(() -> "Failed to clean up borrowed response stream", cleanupFailure);
            }
        }
    }

    private void ensureReadable() throws IOException {
        CloseReason terminalReason = closeReason.get();
        if (terminalReason != null) {
            throwClosed(terminalReason);
        }
        if (readFailure != null) {
            throwFailure(readFailure);
        }
    }

    private void enqueue(Object event) {
        if (!events.offer(event)) {
            throw new IllegalStateException("Failed to enqueue borrowed response stream event");
        }
    }

    private Object takeEvent() throws IOException {
        try {
            return events.take();
        } catch (InterruptedException e) {
            InterruptedIOException failure = new InterruptedIOException("Interrupted while waiting for response data");
            failure.initCause(e);
            close(failure);
            Thread.currentThread().interrupt();
            throw failure;
        }
    }

    private Throwable cancelUpstream(Throwable failure) {
        if (upstreamTerminal || !cancellationStarted.compareAndSet(false, true)) {
            return failure;
        }

        try {
            cancellationAction.run();
        } catch (Throwable t) {
            failure = addFailure(failure, t);
        }
        return failure;
    }

    private Throwable discardQueued(Throwable failure) {
        Object event;
        while ((event = events.poll()) != null) {
            if (event instanceof S3CrtBorrowedBufferLease) {
                failure = discard((S3CrtBorrowedBufferLease) event, failure);
            }
        }
        return failure;
    }

    private static Throwable discard(S3CrtBorrowedBufferLease buffer, Throwable failure) {
        if (buffer != null) {
            try {
                buffer.discard();
            } catch (Throwable t) {
                failure = addFailure(failure, t);
            }
        }
        return failure;
    }

    private static Throwable addFailure(Throwable existing, Throwable additional) {
        if (existing == null) {
            return additional;
        }
        if (existing != additional) {
            existing.addSuppressed(additional);
        }
        return existing;
    }

    private int throwClosed(CloseReason reason) throws IOException {
        if (reason != null && reason.cause != null) {
            throwFailure(reason.cause);
        }
        throw new IOException("Stream is closed");
    }

    private int throwFailure(Throwable failure) throws IOException {
        if (failure instanceof IOException) {
            throw (IOException) failure;
        }
        if (failure instanceof SdkClientException
            && !(failure instanceof ApiCallTimeoutException)
            && !(failure instanceof ApiCallAttemptTimeoutException)) {
            throw (SdkClientException) failure;
        }
        throw new IOException("Failed to read borrowed response data", failure);
    }
}
