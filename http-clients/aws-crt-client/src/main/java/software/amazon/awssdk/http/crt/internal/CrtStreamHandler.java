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
import java.util.concurrent.CountDownLatch;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.http.HttpStreamBase;

/**
 * Manages the lifecycle of a CRT HTTP stream, providing thread-safe access to stream operations.
 * Shared between the request executor (for writing body data) and the response handler (for
 * incrementing the window and releasing/closing the connection).
 */
@SdkInternalApi
public final class CrtStreamHandler {

    private final Object streamLock = new Object();
    private final CountDownLatch streamLatch = new CountDownLatch(1);
    private HttpStreamBase stream;
    private boolean streamClosed;

    /**
     * Sets the stream. Called once when the stream is acquired from the connection pool.
     */
    public void setStream(HttpStreamBase stream) {
        this.stream = stream;
        streamLatch.countDown();
    }

    /**
     * Blocks until the stream has been acquired.
     */
    public void waitForStream() {
        try {
            streamLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for stream", e);
        }
    }

    /**
     * Write data to the stream. The caller must ensure the stream is ready (via {@link #waitForStream()})
     * before calling this method.
     */
    public CompletableFuture<Void> writeData(byte[] data, boolean endStream) {
        if (streamLatch.getCount() != 0) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(
                new IllegalStateException("writeData called before stream is ready. Call waitForStream() first."));
            return future;
        }
        synchronized (streamLock) {
            if (streamClosed) {
                CompletableFuture<Void> future = new CompletableFuture<>();
                future.completeExceptionally(
                    new IOException("Stream is already closed, cannot write data."));
                return future;
            }
            return stream.writeData(data, endStream);
        }
    }

    public void incrementWindow(int windowSize) {
        if (streamLatch.getCount() != 0) {
            throw new IllegalStateException("incrementWindow called before stream is ready.");
        }
        synchronized (streamLock) {
            if (!streamClosed) {
                stream.incrementWindow(windowSize);
            }
        }
    }

    /**
     * Release the connection back to the pool so that it may be reused. This should be called when the request
     * completes successfully and the response has been fully consumed.
     */
    public void releaseConnection() {
        synchronized (streamLock) {
            if (!streamClosed && stream != null) {
                streamClosed = true;
                stream.close();
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
            if (!streamClosed && stream != null) {
                streamClosed = true;
                stream.cancel();
                stream.close();
            }
        }
    }
}
