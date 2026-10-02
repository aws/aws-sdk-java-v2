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

@SdkInternalApi
final class S3CrtBorrowedBufferInputStream extends InputStream implements Abortable {

    private static final Logger log = Logger.loggerFor(S3CrtBorrowedBufferInputStream.class);

    private enum Signal {
        COMPLETE,
        CLOSED
    }

    private static final class Failure {
        private final Throwable cause;

        private Failure(Throwable cause) {
            this.cause = cause;
        }
    }

    private static final class CloseReason {
        private final Throwable cause;

        private CloseReason(Throwable cause) {
            this.cause = cause;
        }
    }

    private final BlockingQueue<Object> events = new LinkedBlockingQueue<>();
    // Reads nest readLock -> terminalLock -> stateLock; close wakes blocked readers before taking readLock.
    private final Object stateLock = new Object();
    private final Object readLock = new Object();
    private final Object terminalLock = new Object();
    private final byte[] singleByte = new byte[1];
    private final Runnable cancellationAction;
    private final AtomicReference<CloseReason> closeReason = new AtomicReference<>();
    private final AtomicBoolean cancellationStarted = new AtomicBoolean();

    private volatile boolean accepting = true;
    private volatile boolean upstreamTerminal;
    private volatile boolean complete;
    private volatile Throwable readFailure;
    private S3CrtBorrowedBuffer current;

    S3CrtBorrowedBufferInputStream(Runnable cancellationAction) {
        this.cancellationAction = Validate.paramNotNull(cancellationAction, "cancellationAction");
    }

    boolean onBuffer(S3CrtBorrowedBuffer buffer) {
        Objects.requireNonNull(buffer, "buffer");
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
        Objects.requireNonNull(error, "error");
        synchronized (stateLock) {
            if (accepting) {
                accepting = false;
                upstreamTerminal = true;
                enqueue(new Failure(error));
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
                    if (event instanceof S3CrtBorrowedBuffer) {
                        current = (S3CrtBorrowedBuffer) event;
                    } else if (event == Signal.COMPLETE) {
                        complete = true;
                        return copied == 0 ? -1 : copied;
                    } else if (event == Signal.CLOSED) {
                        return copied == 0 ? throwClosed(closeReason.get()) : copied;
                    } else {
                        readFailure = ((Failure) event).cause;
                        return copied == 0 ? throwFailure(readFailure) : copied;
                    }
                }

                synchronized (terminalLock) {
                    CloseReason terminalReason = closeReason.get();
                    if (terminalReason != null) {
                        return copied == 0 ? throwClosed(terminalReason) : copied;
                    }
                    ByteBufferReader reader = new ByteBufferReader(current);
                    copied += reader.read(destination, offset + copied, length - copied);
                    if (reader.exhausted()) {
                        S3CrtBorrowedBuffer consumed = current;
                        current = null;
                        try {
                            consumed.consumed();
                        } catch (Throwable t) {
                            failLocally(t);
                            return copied == 0 ? throwFailure(t) : copied;
                        }
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
        synchronized (terminalLock) {
            synchronized (stateLock) {
                accepting = false;
                enqueue(Signal.CLOSED);
            }
        }

        Throwable failure = cancelUpstream(null);
        synchronized (readLock) {
            failure = discard(current, failure);
            current = null;
            failure = discardQueued(failure);
        }
        if (failure != null) {
            if (error != null) {
                addFailure(error, failure);
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
            Thread.currentThread().interrupt();
            InterruptedIOException failure = new InterruptedIOException("Interrupted while waiting for response data");
            failure.initCause(e);
            failLocally(failure);
            throw failure;
        }
    }

    private void failLocally(Throwable failure) {
        synchronized (stateLock) {
            accepting = false;
            if (readFailure == null) {
                readFailure = failure;
            }
            enqueue(new Failure(readFailure));
        }

        Throwable cleanupFailure = cancelUpstream(null);
        cleanupFailure = discard(current, cleanupFailure);
        current = null;
        cleanupFailure = discardQueued(cleanupFailure);
        if (cleanupFailure != null && cleanupFailure != failure) {
            failure.addSuppressed(cleanupFailure);
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
            if (event instanceof S3CrtBorrowedBuffer) {
                failure = discard((S3CrtBorrowedBuffer) event, failure);
            }
        }
        return failure;
    }

    private static Throwable discard(S3CrtBorrowedBuffer buffer, Throwable failure) {
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

    private static final class ByteBufferReader {
        private final S3CrtBorrowedBuffer source;

        private ByteBufferReader(S3CrtBorrowedBuffer source) {
            this.source = source;
        }

        private int read(byte[] destination, int offset, int length) {
            int count = Math.min(source.buffer().remaining(), length);
            source.buffer().get(destination, offset, count);
            return count;
        }

        private boolean exhausted() {
            return !source.buffer().hasRemaining();
        }
    }
}
