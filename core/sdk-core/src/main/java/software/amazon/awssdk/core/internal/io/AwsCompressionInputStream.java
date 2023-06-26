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

package software.amazon.awssdk.core.internal.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.compression.Compressor;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.io.SdkInputStream;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * A wrapper class of InputStream that implements compression in chunks.
 */
@SdkInternalApi
public class AwsCompressionInputStream extends SdkInputStream {

    public static final int COMPRESSION_CHUNK_SIZE = 128 * 1024;
    public static final int COMPRESSION_BUFFER_SIZE = 256 * 1024;
    private static final String CRLF = "\r\n";
    private static final Logger log = Logger.loggerFor(AwsCompressionInputStream.class);
    private Compressor compressor;
    private InputStream is;
    private ChunkContentIterator currentChunkIterator;
    private UnderlyingStreamBuffer uncompressedStreamBuffer;
    private boolean isAtStart = true;
    private boolean isTerminating = false;

    private AwsCompressionInputStream(InputStream in, Compressor compressor) {
        this.compressor = compressor;
        if (in instanceof AwsCompressionInputStream) {
            // This could happen when the request is retried.
            AwsCompressionInputStream originalCompressionStream = (AwsCompressionInputStream) in;
            is = originalCompressionStream.is;
            uncompressedStreamBuffer = originalCompressionStream.uncompressedStreamBuffer;
        } else {
            this.is = in;
            uncompressedStreamBuffer = null;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public int read() throws IOException {
        byte[] tmp = new byte[1];
        int count = read(tmp, 0, 1);
        if (count > 0) {
            log.debug(() -> "One byte read from the stream.");
            int unsignedByte = (int) tmp[0] & 0xFF;
            return unsignedByte;
        } else {
            return count;
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        abortIfNeeded();
        Validate.notNull(b, "buff");
        if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        if (currentChunkIterator == null || !currentChunkIterator.hasNext()) {
            if (isTerminating) {
                return -1;
            }
            isTerminating = setUpNextChunk();
        }

        int count = currentChunkIterator.read(b, off, len);
        if (count > 0) {
            isAtStart = false;
            log.trace(() -> count + " byte read from the stream.");
        }
        return count;
    }

    private boolean setUpNextChunk() throws IOException {
        byte[] chunkData = new byte[COMPRESSION_CHUNK_SIZE];
        int chunkSizeInBytes = 0;
        while (chunkSizeInBytes < COMPRESSION_CHUNK_SIZE) {
            /** Read from the buffer of the uncompressed stream */
            if (uncompressedStreamBuffer != null && uncompressedStreamBuffer.hasNext()) {
                chunkData[chunkSizeInBytes++] = uncompressedStreamBuffer.next();
            } else { /** Read from the wrapped stream */
                int bytesToRead = COMPRESSION_CHUNK_SIZE - chunkSizeInBytes;
                int count = is.read(chunkData, chunkSizeInBytes, bytesToRead);
                if (count != -1) {
                    if (uncompressedStreamBuffer != null) {
                        uncompressedStreamBuffer.buffer(chunkData, chunkSizeInBytes, count);
                    }
                    chunkSizeInBytes += count;
                } else {
                    break;
                }
            }
        }
        if (chunkSizeInBytes == 0) {
            byte[] finalChunk = createFinalChunk();
            currentChunkIterator = new ChunkContentIterator(finalChunk);
            return true;
        } else {
            if (chunkSizeInBytes < chunkData.length) {
                chunkData = Arrays.copyOf(chunkData, chunkSizeInBytes);
            }
            // Compress the chunk
            byte[] compressedChunkData = compressor.compress(chunkData);
            byte[] chunkContent = createChunk(compressedChunkData);
            currentChunkIterator = new ChunkContentIterator(chunkContent);
            return false;
        }
    }

    private byte[] createChunk(byte[] compressedChunkData) {
        StringBuilder chunkHeader = new StringBuilder();
        chunkHeader.append(Integer.toHexString(compressedChunkData.length));
        chunkHeader.append(CRLF);
        try {
            byte[] header = chunkHeader.toString().getBytes(StandardCharsets.UTF_8);
            byte[] trailer = CRLF.getBytes(StandardCharsets.UTF_8);
            byte[] chunk = new byte[header.length + compressedChunkData.length + trailer.length];
            System.arraycopy(header, 0, chunk, 0, header.length);
            System.arraycopy(compressedChunkData, 0, chunk, header.length, compressedChunkData.length);
            System.arraycopy(trailer, 0,
                             chunk, header.length + compressedChunkData.length,
                             trailer.length);
            return chunk;
        } catch (Exception e) {
            throw SdkClientException.builder()
                                    .message("Unable to create chunked data. " + e.getMessage())
                                    .cause(e)
                                    .build();
        }
    }

    private byte[] createFinalChunk() {
        byte[] finalChunk = new byte[0];
        StringBuilder chunkHeader = new StringBuilder();
        // chunk-size
        chunkHeader.append(Integer.toHexString(finalChunk.length));
        chunkHeader.append(CRLF);
        return chunkHeader.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected InputStream getWrappedInputStream() {
        return is;
    }

    @Override
    public long skip(long n) throws IOException {
        if (n <= 0) {
            return 0;
        }
        long remaining = n;
        int toskip = (int) Math.min(COMPRESSION_BUFFER_SIZE, n);
        byte[] temp = new byte[toskip];
        while (remaining > 0) {
            int count = read(temp, 0, toskip);
            if (count < 0) {
                break;
            }
            remaining -= count;
        }
        return n - remaining;
    }

    /**
     * @see java.io.InputStream#markSupported()
     */
    @Override
    public boolean markSupported() {
        return true;
    }

    /**
     * The readlimit parameter is ignored.
     */
    @Override
    public void mark(int readlimit) {
        abortIfNeeded();
        if (!isAtStart) {
            throw new UnsupportedOperationException("Compression stream only supports mark() at the start of the stream.");
        }
        if (is.markSupported()) {
            log.debug(() -> "AwsCompressionInputStream marked at the start of the stream "
                            + "(will directly mark the wrapped stream since it's mark-supported).");
            is.mark(readlimit);
        } else {
            log.debug(() -> "AwsCompressionInputStream marked at the start of the stream "
                            + "(initializing the buffer since the wrapped stream is not mark-supported).");
            uncompressedStreamBuffer = new UnderlyingStreamBuffer(COMPRESSION_BUFFER_SIZE);
        }
    }

    /**
     * Reset the stream, either by resetting the wrapped stream or using the
     * buffer created by this class.
     */
    @Override
    public void reset() throws IOException {
        abortIfNeeded();
        // Clear up any encoded data
        currentChunkIterator = null;
        // Reset the wrapped stream if it is mark-supported,
        // otherwise use our buffered data.
        if (is.markSupported()) {
            log.debug(() -> "AwsCompressionInputStream reset "
                            + "(will reset the wrapped stream because it is mark-supported).");
            is.reset();
        } else {
            log.debug(() -> "AwsCompressionInputStream reset (will use the buffer of the decoded stream).");
            Validate.notNull(uncompressedStreamBuffer, "Cannot reset the stream because the mark is not set.");
            uncompressedStreamBuffer.startReadBuffer();
        }
        isAtStart = true;
        isTerminating = false;
    }

    @Override
    public void close() throws IOException {
        is.close();
    }

    public static final class Builder {
        InputStream inputStream;
        Compressor compressor;

        public AwsCompressionInputStream build() {
            return new AwsCompressionInputStream(
                this.inputStream, this.compressor);
        }

        public Builder inputStream(InputStream inputStream) {
            this.inputStream = inputStream;
            return this;
        }

        public Builder compressor(Compressor compressor) {
            this.compressor = compressor;
            return this;
        }
    }
}
