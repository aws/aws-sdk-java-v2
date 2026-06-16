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

package software.amazon.awssdk.utils.async;

import static software.amazon.awssdk.utils.CompletableFutureUtils.joinInterruptibly;
import static software.amazon.awssdk.utils.CompletableFutureUtils.joinInterruptiblyIgnoringFailures;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * A publisher to which an {@link InputStream} can be written.
 * <p>
 * See {@link #doBlockingWrite(InputStream)}.
 */
@SdkProtectedApi
public class InputStreamConsumingPublisher implements Publisher<ByteBuffer> {
    private static final int BUFFER_SIZE = 16 * 1024; // 16 KB

    /**
     * Minimum allocation size when sizing buffers based on {@link InputStream#available()}. Avoids excessive
     * per-read overhead from very small allocations.
     */
    private static final int MIN_BUFFER_SIZE = 1024;

    /**
     * If a {@link InputStream#read(byte[])} call returns fewer than this many bytes, the data is copied into a
     * right-sized array before being passed downstream. This is a fallback for cases where {@link InputStream#available()}
     * overestimated or was 0, ensuring oversized backing arrays don't pin memory as they flow through buffered pipelines
     * (e.g., retry buffers, Netty write queue).
     */
    private static final int TRIM_THRESHOLD = BUFFER_SIZE / 2;

    private final SimplePublisher<ByteBuffer> delegate = new SimplePublisher<>();

    /**
     * Write the provided input stream to the stream subscribed to this publisher.
     * <p>
     * This method will block the calling thread to write until: (1) the provided input stream is fully consumed,
     * (2) the subscription is cancelled, (3) reading from the input stream fails, or (4) {@link #cancel()} is called.
     *
     * @return The amount of data written to the downstream subscriber.
     */
    public long doBlockingWrite(InputStream inputStream) {
        try {
            long dataWritten = 0;
            while (true) {
                byte[] data = new byte[allocSize(inputStream)];
                int dataLength = inputStream.read(data);
                if (dataLength > 0) {
                    dataWritten += dataLength;
                    joinInterruptibly(delegate.send(toByteBuffer(data, dataLength)));
                } else if (dataLength < 0) {
                    // We ignore cancel failure on completion, because as long as our onNext calls have succeeded, the
                    // subscriber got everything we wanted to send.
                    joinInterruptiblyIgnoringCancellation(delegate.complete());
                    break;
                }
            }
            return dataWritten;
        } catch (IOException e) {
            joinInterruptiblyIgnoringFailures(delegate.error(e));
            throw new UncheckedIOException(e);
        } catch (RuntimeException | Error e) {
            joinInterruptiblyIgnoringFailures(delegate.error(e));
            throw e;
        }
    }

    /**
     * Choose an allocation size for the next read based on {@link InputStream#available()}. Streams that report
     * useful availability (e.g., {@link java.io.PipedInputStream} with a small pipe buffer, sockets) get
     * right-sized allocations, eliminating the need to trim oversized arrays after the read. Streams that
     * always report 0 (or that throw) fall back to {@link #BUFFER_SIZE}.
     */
    private static int allocSize(InputStream inputStream) {
        try {
            int avail = inputStream.available();
            if (avail <= 0) {
                return BUFFER_SIZE;
            }
            return Math.min(BUFFER_SIZE, Math.max(MIN_BUFFER_SIZE, avail));
        } catch (IOException ignored) {
            return BUFFER_SIZE;
        }
    }

    /**
     * Wrap the given byte array into a {@link ByteBuffer}, trimming to the actual data length if the read was
     * significantly underfilled. This is a fallback for cases where the upfront allocation in {@link #allocSize}
     * overestimated. Avoids holding onto oversized backing arrays in downstream pipelines that may retain
     * references for extended periods (retry buffers, async write queues, etc.).
     */
    private static ByteBuffer toByteBuffer(byte[] data, int dataLength) {
        if (dataLength < data.length && dataLength < TRIM_THRESHOLD) {
            byte[] trimmed = new byte[dataLength];
            System.arraycopy(data, 0, trimmed, 0, dataLength);
            return ByteBuffer.wrap(trimmed);
        }
        return ByteBuffer.wrap(data, 0, dataLength);
    }

    /**
     * Cancel an ongoing {@link #doBlockingWrite(InputStream)} call.
     */
    public void cancel() {
        delegate.error(new CancellationException("Input stream has been cancelled."));
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> s) {
        delegate.subscribe(s);
    }

    private void joinInterruptiblyIgnoringCancellation(CompletableFuture<Void> complete) {
        try {
            joinInterruptibly(complete);
        } catch (CancellationException e) {
            // Ignore
        }
    }
}
