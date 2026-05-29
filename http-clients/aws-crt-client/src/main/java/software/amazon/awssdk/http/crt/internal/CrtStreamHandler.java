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

package software.amazon.awssdk.http.crt.internal;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.http.HttpStreamBase;
import software.amazon.awssdk.utils.CompletableFutureUtils;

/**
 * Manages the lifecycle of a CRT HTTP stream, providing thread-safe access to stream operations.
 * Shared between the request executor (for writing body data) and the response handler (for
 * incrementing the window and releasing/closing the connection).
 *
 * <p>The handler is constructed with a {@link CompletableFuture} representing stream acquisition.
 * The caller (request executor) completes that future once the underlying CRT stream manager has
 * either acquired the stream or failed. All operations on this handler chain off that future, so
 * writes issued before acquisition completes are queued.
 */
@SdkInternalApi
public final class CrtStreamHandler {

    private final Object streamLock = new Object();
    private final CompletableFuture<HttpStreamBase> streamFuture;
    private boolean streamClosed;

    public CrtStreamHandler(CompletableFuture<HttpStreamBase> streamFuture) {
        this.streamFuture = streamFuture;
    }

    /**
     * Blocks until the stream has been acquired or acquisition has failed. Returns the acquired
     * stream on success. If acquisition failed, the failure cause is rethrown wrapped in a
     * {@link CompletionException} so callers can use the same handling as for response futures.
     */
    public HttpStreamBase waitForStream() {
        return CompletableFutureUtils.joinInterruptibly(streamFuture);
    }

    /**
     * Write data to the stream. The returned future chains on stream acquisition: if the stream
     * is not yet ready, the write is queued until the {@code streamFuture} passed to the
     * constructor completes. Failures from either stream acquisition or the underlying CRT write
     * are propagated as the original cause (not wrapped in {@link CompletionException}) so callers
     * see the same exception type whether the failure happens before or after {@code thenCompose}-
     * style chaining.
     */
    public CompletableFuture<Void> writeData(byte[] data, boolean endStream) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        streamFuture.handle((s, t) -> {
            if (t != null) {
                result.completeExceptionally(unwrap(t));
                return null;
            }
            doWrite(s, data, endStream, result);
            return null;
        }).exceptionally(t -> {
            result.completeExceptionally(t);
            closeConnection();
            return null;
        });
        return result;
    }

    private void doWrite(HttpStreamBase s, byte[] data, boolean endStream, CompletableFuture<Void> result) {
        try {
            CompletableFuture<Void> writeFuture;
            synchronized (streamLock) {
                if (streamClosed) {
                    result.completeExceptionally(new IOException("Stream is already closed, cannot write data."));
                    return;
                }
                writeFuture = s.writeData(data, endStream);
            }
            writeFuture.whenComplete((v, err) -> {
                if (err != null) {
                    result.completeExceptionally(unwrap(err));
                } else {
                    result.complete(null);
                }
            });
        } catch (Throwable th) {
            result.completeExceptionally(th);
            closeConnection();
        }
    }

    private static Throwable unwrap(Throwable t) {
        return t instanceof CompletionException && t.getCause() != null ? t.getCause() : t;
    }

    public void incrementWindow(int windowSize) {
        synchronized (streamLock) {
            HttpStreamBase s = streamIfAvailable();
            if (!streamClosed && s != null) {
                s.incrementWindow(windowSize);
            }
        }
    }

    /**
     * Release the connection back to the pool so that it may be reused. This should be called when the request
     * completes successfully and the response has been fully consumed.
     */
    public void releaseConnection() {
        synchronized (streamLock) {
            HttpStreamBase s = streamIfAvailable();
            if (!streamClosed && s != null) {
                streamClosed = true;
                s.close();
            }
        }
    }

    /**
     * Cancel and close the stream, forcing the underlying connection to shut down rather than be returned to the
     * connection pool. This should be called on error paths or when the stream is aborted before the response is
     * fully consumed. {@code cancel()} must be invoked before {@code close()} per the CRT contract.
     */
    public void closeConnection() {
        synchronized (streamLock) {
            HttpStreamBase s = streamIfAvailable();
            if (!streamClosed && s != null) {
                streamClosed = true;
                s.cancel();
                s.close();
            }
        }
    }

    /**
     * Returns the acquired stream if {@link #streamFuture} completed normally, otherwise {@code null}.
     * Tolerates exceptional or pending completion (in contrast to {@link CompletableFuture#getNow}, which
     * throws {@link CompletionException} when the future is exceptional).
     */
    private HttpStreamBase streamIfAvailable() {
        if (!streamFuture.isDone() || streamFuture.isCompletedExceptionally()) {
            return null;
        }
        return streamFuture.getNow(null);
    }
}
