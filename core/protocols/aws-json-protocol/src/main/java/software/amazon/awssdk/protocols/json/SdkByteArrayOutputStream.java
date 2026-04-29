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
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.ContentStreamProvider;

/**
 * A {@link ByteArrayOutputStream} subclass that behaves identically to the JDK implementation for
 * small payloads, but caps internal buffer growth to avoid G1GC humongous object allocations for
 * large payloads.
 *
 * <p><b>How it works:</b> Writes flow into the inherited {@code ByteArrayOutputStream} buffer
 * normally. When a write would cause the buffer to grow beyond {@link #MAX_BUFFER_SIZE}, the
 * current buffer contents are frozen into the first "chunk" and subsequent writes go into
 * fixed-size overflow chunks ({@link #CHUNK_SIZE} bytes each). No single allocation ever exceeds
 * {@code MAX_BUFFER_SIZE}.
 *
 * <p><b>Performance characteristics:</b>
 * <ul>
 *   <li>Payloads &le; {@code MAX_BUFFER_SIZE}: Identical to {@code ByteArrayOutputStream} — the
 *       JIT can inline and optimize the write path exactly as it does for the stock class. Zero
 *       overhead.</li>
 *   <li>Payloads &gt; {@code MAX_BUFFER_SIZE}: Overflow writes go through a simple chunked path.
 *       This is slightly slower per-byte than {@code ByteArrayOutputStream}'s doubling strategy,
 *       but avoids allocations that exceed half the G1 region size.</li>
 * </ul>
 *
 * <p>This class is not thread-safe.
 */
@SdkInternalApi
final class SdkByteArrayOutputStream extends ByteArrayOutputStream {

    /**
     * Maximum size of the primary ByteArrayOutputStream buffer before overflow kicks in.
     * Chosen to be well below the G1 humongous threshold (region_size / 2). With a 4 GB heap
     * the default region size is 2 MB, so humongous threshold is 1 MB. 128 KB is safely below
     * that for any reasonable heap size (humongous threshold is 512 KB for a 256 MB heap).
     */
    static final int MAX_BUFFER_SIZE = 128 * 1024;

    /**
     * Size of each overflow chunk. 64 KB is well below any G1 humongous threshold.
     */
    static final int CHUNK_SIZE = 64 * 1024;

    private List<byte[]> overflowChunks;
    private int overflowChunkOffset;
    private int overflowTotalBytes;
    private boolean overflowing;

    SdkByteArrayOutputStream(int initialCapacity) {
        super(initialCapacity);
    }

    @Override
    public void write(int b) {
        if (overflowing) {
            ensureOverflowCapacity(1);
            currentOverflowChunk()[overflowChunkOffset++] = (byte) b;
            overflowTotalBytes++;
        } else if (count + 1 > MAX_BUFFER_SIZE) {
            startOverflow();
            write(b);
        } else {
            super.write(b);
        }
    }

    @Override
    public void write(byte[] b, int off, int len) {
        if (overflowing) {
            writeToOverflow(b, off, len);
        } else if (count + len > MAX_BUFFER_SIZE) {
            // Write what fits into the base buffer, then overflow the rest
            int fits = MAX_BUFFER_SIZE - count;
            if (fits > 0) {
                super.write(b, off, fits);
            }
            startOverflow();
            writeToOverflow(b, off + fits, len - fits);
        } else {
            super.write(b, off, len);
        }
    }

    /**
     * Returns the total number of bytes written (base buffer + overflow).
     */
    @Override
    public int size() {
        return count + overflowTotalBytes;
    }

    /**
     * Returns all written data as a single contiguous byte array. Exists for backward
     * compatibility via {@link #toByteArray()} but should not be used on the hot path.
     */
    @Override
    public byte[] toByteArray() {
        if (!overflowing) {
            return super.toByteArray();
        }
        int total = size();
        byte[] result = new byte[total];
        // Copy base buffer
        System.arraycopy(buf, 0, result, 0, count);
        // Copy overflow chunks
        int destOff = count;
        for (int i = 0; i < overflowChunks.size(); i++) {
            int len = (i < overflowChunks.size() - 1) ? overflowChunks.get(i).length : overflowChunkOffset;
            System.arraycopy(overflowChunks.get(i), 0, result, destOff, len);
            destOff += len;
        }
        return result;
    }

    /**
     * Resets this stream so that all currently accumulated output is discarded, including any
     * overflow chunks. After calling this method, the stream can be reused as if freshly constructed.
     */
    @Override
    public void reset() {
        super.reset();
        overflowing = false;
        overflowChunks = null;
        overflowChunkOffset = 0;
        overflowTotalBytes = 0;
    }

    /**
     * Writes the complete contents of this stream to the specified output stream, including
     * any overflow chunks.
     */
    @Override
    public void writeTo(OutputStream out) throws IOException {
        if (!overflowing) {
            super.writeTo(out);
            return;
        }
        // Write base buffer
        out.write(buf, 0, count);
        // Write overflow chunks
        for (int i = 0; i < overflowChunks.size(); i++) {
            int len = (i < overflowChunks.size() - 1) ? overflowChunks.get(i).length : overflowChunkOffset;
            out.write(overflowChunks.get(i), 0, len);
        }
    }

    /**
     * Returns a {@link ContentStreamProvider} that streams directly from the internal buffers
     * without creating a contiguous copy. For small payloads this wraps the single base buffer;
     * for large payloads it chains the base buffer and overflow chunks via
     * {@link SequenceInputStream}.
     */
    ContentStreamProvider contentStreamProvider() {
        if (!overflowing) {
            // Small payload: single buffer, wrap directly
            byte[] b = buf;
            int c = count;
            return () -> new ByteArrayInputStream(b, 0, c);
        }

        // Large payload: chain base buffer + overflow chunks
        byte[] baseBuf = buf;
        int baseCount = count;
        List<byte[]> chunks = overflowChunks;
        int lastChunkLen = overflowChunkOffset;

        return () -> {
            List<InputStream> streams = new ArrayList<>(1 + chunks.size());
            streams.add(new ByteArrayInputStream(baseBuf, 0, baseCount));
            for (int i = 0; i < chunks.size(); i++) {
                int len = (i < chunks.size() - 1) ? chunks.get(i).length : lastChunkLen;
                streams.add(new ByteArrayInputStream(chunks.get(i), 0, len));
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

    private void startOverflow() {
        overflowing = true;
        overflowChunks = new ArrayList<>();
        overflowChunks.add(new byte[CHUNK_SIZE]);
        overflowChunkOffset = 0;
        overflowTotalBytes = 0;
    }

    private void writeToOverflow(byte[] b, int off, int len) {
        int remaining = len;
        int srcOff = off;
        while (remaining > 0) {
            ensureOverflowCapacity(1);
            int space = currentOverflowChunk().length - overflowChunkOffset;
            int toCopy = Math.min(remaining, space);
            System.arraycopy(b, srcOff, currentOverflowChunk(), overflowChunkOffset, toCopy);
            overflowChunkOffset += toCopy;
            overflowTotalBytes += toCopy;
            srcOff += toCopy;
            remaining -= toCopy;
        }
    }

    private byte[] currentOverflowChunk() {
        return overflowChunks.get(overflowChunks.size() - 1);
    }

    private void ensureOverflowCapacity(int needed) {
        if (overflowChunkOffset + needed > currentOverflowChunk().length) {
            overflowChunks.add(new byte[CHUNK_SIZE]);
            overflowChunkOffset = 0;
        }
    }
}
