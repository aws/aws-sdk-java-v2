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
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Input stream implementation which allows an asynchronous publisher to push data to the stream
 * while the synchronous stream consumer will block on read() until data is available and receive data as it's available.
 *
 * Additionally a publisher can subscribe to read events so that it may implement any required backpressure mechanisms.
 *
 * It's implemented with a queue of byte[] with a lock and condition variable.
 */
public class CRTAsyncInputStreamAdapter extends InputStream {

    private final Queue<byte[]> dataQueue = new LinkedList<>();
    private final Lock lock = new ReentrantLock(true);  // Fair locking
    private final Condition newDataCondition = lock.newCondition();

    private boolean closed = false;
    private byte[] currentBuffer;
    private int currentBufferIndex = 0;

    private final ReadObserver observer;

    public CRTAsyncInputStreamAdapter(ReadObserver observer) {
        this.observer = observer;
    }

    /**
     * Reads a byte from the stream if one is available, otherwise it blocks until data becomes available
     * from the asynchronous source.
     * @return The byte that was read or 0 for EOS
     * @throws IOException Throws if the reading thread is interrupted by the system (presumably at shutdown)
     */
    @Override
    public int read() throws IOException {
        lock.lock();
        try {
            ensureBufferNotEmpty();

            if (closed && (currentBuffer == null || currentBufferIndex >= currentBuffer.length)) {
                return -1;
            }

            byte result = currentBuffer[currentBufferIndex];
            currentBufferIndex++;

            if (observer != null) {
                observer.bytesRead(1);
            }

            if (currentBufferIndex >= currentBuffer.length) {
                currentBuffer = null;
            }

            return result & 0xFF;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Reads a byte range from the stream if data is available, otherwise it blocks until data becomes available
     * from the asynchronous source.
     * @return The amount of bytes read or -1 for EOS
     * @throws IOException Throws if the reading thread is interrupted by the system (presumably at shutdown)
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        }

        lock.lock();
        try {
            int bytesRead = 0;
            while (bytesRead < len) {
                ensureBufferNotEmpty();

                if (closed && (currentBuffer == null || currentBufferIndex >= currentBuffer.length)) {
                    return bytesRead == 0 ? -1 : bytesRead;
                }

                int available = currentBuffer.length - currentBufferIndex;
                int toRead = Math.min(len - bytesRead, available);

                System.arraycopy(currentBuffer, currentBufferIndex, b, off + bytesRead, toRead);
                bytesRead += toRead;
                currentBufferIndex += toRead;
                if (currentBufferIndex >= currentBuffer.length) {
                    currentBuffer = null;
                }
            }

            if (observer != null) {
                observer.bytesRead(bytesRead);
            }

            return bytesRead;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the number of bytes that can be read from the stream without blocking.
     *
     * @return the number of bytes that can be read from the stream without blocking.
     * @throws IOException Does not throw
     */
    @Override
    public int available() throws IOException {
        int availableCount = 0;

        lock.lock();
        try {
            if (!closed) {
                for (byte[] buffer : dataQueue) {
                    if (buffer == currentBuffer) {
                        availableCount += currentBuffer.length - currentBufferIndex;
                    } else {
                        availableCount += buffer.length;
                    }
                }
            }
        } finally {
            lock.unlock();
        }

        return availableCount;
    }

    /**
     * Makes sure the current buffer is set to the top of the data queue and waits until
     * data is published if no buffer is available. The lock must be held before calling this function
     * and unlocked after calling this function.
     *
     * Throws an exception when the program is interrupted (presumably for program termination)
     */
    private void ensureBufferNotEmpty() throws IOException {
        while (currentBuffer == null || currentBufferIndex >= currentBuffer.length) {
            if (!dataQueue.isEmpty()) {
                currentBuffer = dataQueue.poll();
                currentBufferIndex = 0;
            } else if (closed) {
                break;
            } else {
                try {
                    newDataCondition.await();
                } catch (InterruptedException e) {
                    throw new IOException("Interrupted while waiting for data", e);
                }
            }
        }
    }

    /**
     * Invoked by the async publisher when it has data to queue into the stream.
     * Upon calling this any waiting consumer calling read() will wake-up and
     * will try to read from the queue again, likely returning data to the consumer.
     * @param data to publish to the stream.
     */
    public void onDataReceived(byte[] data) {
        lock.lock();
        try {
            // stream is closed don't queue data.
            if (!closed) {
                dataQueue.offer(data);
                newDataCondition.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() throws IOException {
        lock.lock();
        try {
            closed = true;
            newDataCondition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    // Inner class
    public interface ReadObserver {
        /**
         * Invoked when bytes are read by the consumer.
         * This function is intended for backpressure implementors.
         * @param count amount of bytes read.
         */
        void bytesRead(int count);
    }
}
