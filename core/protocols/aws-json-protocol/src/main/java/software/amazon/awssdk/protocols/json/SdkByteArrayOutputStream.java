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

package software.amazon.awssdk.protocols.json;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.ContentStreamProvider;

/**
 * A {@link ByteArrayOutputStream} subclass that behaves identically to the JDK implementation for
 * small payloads, but caps internal buffer growth to avoid large object allocations for
 * large payloads.
 *
 * <p>
 * Writes flow into the inherited {@code ByteArrayOutputStream} buffer
 * normally. When a write would cause the buffer to grow beyond {@link #MAX_BUFFER_SIZE}, the
 * current buffer contents are frozen and subsequent writes go into fixed-size chunks
 * ({@link #CHUNK_SIZE} bytes each). No single allocation ever exceeds {@code MAX_BUFFER_SIZE}.
 *
 * <p>
 * The stream is in "chunked mode" when {@code chunks != null}. This is derived from state
 * rather than tracked by a separate boolean, eliminating a class of state-sync bugs.
 *
 */
@NotThreadSafe
@SdkInternalApi
final class SdkByteArrayOutputStream extends ByteArrayOutputStream {
    // 128 KB, chosen to be well below 1 MB "humongous threshold" for most heap sizes
    static final int MAX_BUFFER_SIZE = 128 * 1024;
    static final int CHUNK_SIZE = 64 * 1024;

    private List<byte[]> chunks;
    private int chunkOffset;
    private int chunkedBytes;

    SdkByteArrayOutputStream(int initialCapacity) {
        super(initialCapacity);
    }

    @Override
    public void write(int b) {
        if (chunks != null) {
            ensureChunkCapacity(1);
            currentChunk()[chunkOffset++] = (byte) b;
            chunkedBytes++;
        } else if (count + 1 > MAX_BUFFER_SIZE) {
            startChunking();
            write(b);
        } else {
            super.write(b);
        }
    }

    @Override
    public void write(byte[] b, int off, int len) {
        if (chunks != null) {
            writeToChunks(b, off, len);
        } else if (count + len > MAX_BUFFER_SIZE) {
            // Write what fits into the base buffer, then chunk the rest
            int fits = MAX_BUFFER_SIZE - count;
            if (fits > 0) {
                super.write(b, off, fits);
            }
            startChunking();
            writeToChunks(b, off + fits, len - fits);
        } else {
            super.write(b, off, len);
        }
    }

    /**
     * Returns the total number of bytes written (base buffer + chunks).
     */
    @Override
    public int size() {
        return count + chunkedBytes;
    }

    /**
     * Returns all written data as a single contiguous byte array. Exists for backward
     * compatibility via {@link #toByteArray()} but should not be used on the hot path.
     */
    @Override
    public byte[] toByteArray() {
        if (chunks == null) {
            return super.toByteArray();
        }
        int total = size();
        byte[] result = new byte[total];
        // Copy base buffer
        System.arraycopy(buf, 0, result, 0, count);
        // Copy chunks
        int destOff = count;
        for (int i = 0; i < chunks.size(); i++) {
            int len = (i < chunks.size() - 1) ? chunks.get(i).length : chunkOffset;
            System.arraycopy(chunks.get(i), 0, result, destOff, len);
            destOff += len;
        }
        return result;
    }

    /**
     * Resets this stream so that all currently accumulated output is discarded, including any
     * chunks. After calling this method, the stream can be reused as if freshly constructed.
     */
    @Override
    public void reset() {
        super.reset();
        chunks = null;
        chunkOffset = 0;
        chunkedBytes = 0;
    }

    /**
     * Writes the complete contents of this stream to the specified output stream, including
     * any chunks.
     */
    @Override
    public void writeTo(OutputStream out) throws IOException {
        if (chunks == null) {
            super.writeTo(out);
            return;
        }
        // Write base buffer
        out.write(buf, 0, count);
        // Write chunks
        for (int i = 0; i < chunks.size(); i++) {
            int len = (i < chunks.size() - 1) ? chunks.get(i).length : chunkOffset;
            out.write(chunks.get(i), 0, len);
        }
    }

    /**
     * Returns a {@link ContentStreamProvider} that streams directly from the internal buffers
     * without creating a contiguous copy. For small payloads this wraps the single base buffer;
     * for large payloads it chains the base buffer and chunks via {@link SequenceInputStream}.
     */
    ContentStreamProvider contentStreamProvider() {
        if (chunks == null) {
            // Small payload: single buffer, wrap directly.
            // Safe to capture buf because callers (SdkJsonGenerator.contentStreamProvider()) close the
            // generator before calling this, guaranteeing no further writes will mutate buf.
            byte[] b = buf;
            int c = count;
            return () -> new ByteArrayInputStream(b, 0, c);
        }

        // Large payload: chain base buffer + chunks
        byte[] baseBuf = buf;
        int baseCount = count;
        List<byte[]> capturedChunks = chunks;
        int lastChunkLen = chunkOffset;

        return () -> {
            List<InputStream> streams = new ArrayList<>(1 + capturedChunks.size());
            streams.add(new ByteArrayInputStream(baseBuf, 0, baseCount));
            for (int i = 0; i < capturedChunks.size(); i++) {
                int len = (i < capturedChunks.size() - 1) ? capturedChunks.get(i).length : lastChunkLen;
                streams.add(new ByteArrayInputStream(capturedChunks.get(i), 0, len));
            }
            return new SequenceInputStream(Collections.enumeration(streams));
        };
    }

    /**
     * Returns the content size without copying.
     */
    int contentSize() {
        return size();
    }

    private void startChunking() {
        chunks = new ArrayList<>();
        chunks.add(new byte[CHUNK_SIZE]);
        chunkOffset = 0;
        chunkedBytes = 0;
    }

    private void writeToChunks(byte[] b, int off, int len) {
        int remaining = len;
        int srcOff = off;
        while (remaining > 0) {
            ensureChunkCapacity(1);
            int space = currentChunk().length - chunkOffset;
            int toCopy = Math.min(remaining, space);
            System.arraycopy(b, srcOff, currentChunk(), chunkOffset, toCopy);
            chunkOffset += toCopy;
            chunkedBytes += toCopy;
            srcOff += toCopy;
            remaining -= toCopy;
        }
    }

    private byte[] currentChunk() {
        return chunks.get(chunks.size() - 1);
    }

    private void ensureChunkCapacity(int needed) {
        if (chunkOffset + needed > currentChunk().length) {
            chunks.add(new byte[CHUNK_SIZE]);
            chunkOffset = 0;
        }
    }
}
