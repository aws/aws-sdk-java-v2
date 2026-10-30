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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongConsumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.crt.s3.S3BorrowedBuffer;
import software.amazon.awssdk.http.Abortable;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * An {@link InputStream} that reads CRT borrowed buffers.
 *
 * <p>Fully reading a buffer releases it and credits the CRT read window by its byte count.
 *
 * <p>A read waits only when nothing is queued. It returns the queued bytes, which may be fewer than requested. If the end or
 * a failure arrives partway through a read, that read returns the bytes it copied and the next read reports it. Reads are
 * serialized.
 *
 * <p>{@link #close()} and {@link #abort()} may be called from any thread, including during a blocked read. They release unread
 * buffers and cancel the request if it is still running.
 */
@SdkInternalApi
public final class S3CrtBorrowedBufferInputStream extends InputStream implements Abortable {

    private static final Logger log = Logger.loggerFor(S3CrtBorrowedBufferInputStream.class);

    private enum Signal {
        /**
         * Enqueued by close() only to wake a reader blocked in take(). The close state is in closeReason.
         */
        CLOSED,
        COMPLETE
    }

    private static final class CloseReason {
        private final Throwable cause;

        private CloseReason(Throwable cause) {
            this.cause = cause;
        }
    }

    /**
     * Accepted buffers and terminal signals in delivery order.
     */
    private final BlockingQueue<Object> events = new LinkedBlockingQueue<>();

    /**
     * Serializes reads and guards {@link #endOfStream}, {@link #current} and {@link #currentByteCount}.
     */
    private final Object readLock = new Object();

    /**
     * Guards {@link #deliveryEnded}, so no buffer is queued after completion, failure or close.
     */
    private final Object stateLock = new Object();
    private final Runnable cancellationAction;
    private final LongConsumer readWindowAction;
    private final AtomicReference<CloseReason> closeReason = new AtomicReference<>();

    /**
     * Set by completion, failure or close. Buffers that arrive after that are released.
     */
    private boolean deliveryEnded;

    private boolean endOfStream;
    private S3BorrowedBuffer current;
    private long currentByteCount;

    S3CrtBorrowedBufferInputStream(Runnable cancellationAction, LongConsumer readWindowAction) {
        this.cancellationAction = cancellationAction;
        this.readWindowAction = readWindowAction;
    }

    /**
     * Queues a buffer, or releases it when upstream delivery has ended or the stream is closed.
     */
    boolean onNextBuffer(S3BorrowedBuffer buffer) {
        boolean accepted;
        synchronized (stateLock) {
            accepted = !deliveryEnded;
            if (accepted) {
                events.add(buffer);
            }
        }
        if (!accepted) {
            buffer.close();
        }
        return accepted;
    }

    void onError(Throwable error) {
        synchronized (stateLock) {
            if (!deliveryEnded) {
                deliveryEnded = true;
                events.add(error);
            }
        }
    }

    void onComplete() {
        synchronized (stateLock) {
            if (!deliveryEnded) {
                deliveryEnded = true;
                events.add(Signal.COMPLETE);
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
        Validate.paramNotNull(destination, "destination");
        if (offset < 0 || length < 0 || length > destination.length - offset) {
            throw new IndexOutOfBoundsException();
        }
        if (length == 0) {
            return 0;
        }

        synchronized (readLock) {
            int copied = 0;
            // Wait for the first byte only. Once some bytes are copied, return them instead of waiting for more.
            while (copied < length && loadNextBuffer(copied == 0)) {
                copied += readCurrent(destination, offset + copied, length - copied);
            }
            if (copied > 0) {
                return copied;
            }
            ensureReadable();
            return -1;
        }
    }

    // Waits for the next event only if waitIfEmpty is true. Returns whether current can be read.
    private boolean loadNextBuffer(boolean waitIfEmpty) throws IOException {
        if (current == null && !isTerminal()) {
            Object event = events.poll();
            if (event == null && waitIfEmpty) {
                event = takeEvent();
            }
            if (event instanceof S3BorrowedBuffer) {
                current = (S3BorrowedBuffer) event;
                currentByteCount = current.asByteBuffer().remaining();
            } else if (event == Signal.COMPLETE) {
                endOfStream = true;
            } else if (event instanceof Throwable) {
                close((Throwable) event);
            } else if (event != null && event != Signal.CLOSED) {
                IllegalStateException failure = new IllegalStateException(
                    "Unexpected borrowed response stream event type: " + event.getClass().getName());
                close(failure);
            }
        }
        return current != null && !isTerminal();
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

    private boolean isTerminal() {
        return endOfStream || closeReason.get() != null;
    }

    private int readCurrent(byte[] destination, int offset, int length) {
        ByteBuffer view = current.asByteBuffer();
        int count = Math.min(view.remaining(), length);
        view.get(destination, offset, count);
        if (!view.hasRemaining()) {
            S3BorrowedBuffer consumed = current;
            current = null;
            try {
                consumed.close();
                readWindowAction.accept(currentByteCount);
            } catch (Throwable t) {
                // Fail on the next read, so the bytes already copied are still returned.
                close(t);
            }
        }
        return count;
    }

    private void ensureReadable() throws IOException {
        CloseReason terminalReason = closeReason.get();
        if (terminalReason != null) {
            throwClosed(terminalReason);
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

    private void close(Throwable closeCause) {
        if (!closeReason.compareAndSet(null, new CloseReason(closeCause))) {
            return;
        }
        boolean upstreamRunning;
        synchronized (stateLock) {
            // close runs once, so deliveryEnded is still false here only if upstream hasn't completed or failed.
            upstreamRunning = !deliveryEnded;
            deliveryEnded = true;
            // Wake the reader blocked in take(). Only the readLock holder can block there, and other readers see
            // closeReason first.
            events.add(Signal.CLOSED);
        }

        if (upstreamRunning) {
            cancelUpstream();
        }
        synchronized (readLock) {
            release(current);
            current = null;
            releaseQueued();
        }
    }

    private void cancelUpstream() {
        try {
            cancellationAction.run();
        } catch (Throwable t) {
            log.error(() -> "Failed to cancel borrowed response stream", t);
        }
    }

    private void releaseQueued() {
        Object event;
        while ((event = events.poll()) != null) {
            if (event instanceof S3BorrowedBuffer) {
                release((S3BorrowedBuffer) event);
            }
        }
    }

    private static void release(S3BorrowedBuffer buffer) {
        if (buffer != null) {
            try {
                buffer.close();
            } catch (Throwable t) {
                log.error(() -> "Failed to release borrowed response buffer", t);
            }
        }
    }

    private void throwClosed(CloseReason reason) throws IOException {
        if (reason != null && reason.cause != null) {
            throwFailure(reason.cause);
        }
        throw new IOException("Stream is closed");
    }

    private void throwFailure(Throwable failure) throws IOException {
        if (failure instanceof IOException) {
            throw (IOException) failure;
        }
        if (failure instanceof SdkClientException) {
            throw (SdkClientException) failure;
        }
        throw new IOException("Failed to read borrowed response data", failure);
    }
}
