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
import java.util.Arrays;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.internal.compression.Compressor;
import software.amazon.awssdk.utils.Validate;

/**
 * A wrapper class of InputStream that implements compression in chunks.
 */
@SdkInternalApi
public final class AwsCompressionInputStream extends AwsChunkedInputStream {
    private final Compressor compressor;

    private AwsCompressionInputStream(InputStream in, Compressor compressor) {
        this.compressor = compressor;
        if (in instanceof AwsCompressionInputStream) {
            // This could happen when the request is retried.
            AwsCompressionInputStream originalCompressionStream = (AwsCompressionInputStream) in;
            this.is = originalCompressionStream.is;
            this.underlyingStreamBuffer = originalCompressionStream.underlyingStreamBuffer;
        } else {
            this.is = in;
            this.underlyingStreamBuffer = null;
        }
    }

    public static Builder builder() {
        return new Builder();
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
        byte[] chunkData = new byte[DEFAULT_CHUNK_SIZE];
        int chunkSizeInBytes = 0;
        while (chunkSizeInBytes < DEFAULT_CHUNK_SIZE) {
            /** Read from the buffer of the uncompressed stream */
            if (underlyingStreamBuffer != null && underlyingStreamBuffer.hasNext()) {
                chunkData[chunkSizeInBytes++] = underlyingStreamBuffer.next();
            } else { /** Read from the wrapped stream */
                int bytesToRead = DEFAULT_CHUNK_SIZE - chunkSizeInBytes;
                int count = is.read(chunkData, chunkSizeInBytes, bytesToRead);
                if (count != -1) {
                    if (underlyingStreamBuffer != null) {
                        underlyingStreamBuffer.buffer(chunkData, chunkSizeInBytes, count);
                    }
                    chunkSizeInBytes += count;
                } else {
                    break;
                }
            }
        }
        if (chunkSizeInBytes == 0) {
            return true;
        }

        if (chunkSizeInBytes < chunkData.length) {
            chunkData = Arrays.copyOf(chunkData, chunkSizeInBytes);
        }
        // Compress the chunk
        byte[] compressedChunkData = compressor.compress(chunkData);
        currentChunkIterator = new ChunkContentIterator(compressedChunkData);
        return false;
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
            underlyingStreamBuffer = new UnderlyingStreamBuffer(SKIP_BUFFER_SIZE);
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
            Validate.notNull(underlyingStreamBuffer, "Cannot reset the stream because the mark is not set.");
            underlyingStreamBuffer.startReadBuffer();
        }
        isAtStart = true;
        isTerminating = false;
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
