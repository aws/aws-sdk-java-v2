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
import software.amazon.awssdk.core.internal.chunked.AwsChunkedEncodingConfig;
import software.amazon.awssdk.core.io.SdkInputStream;
import software.amazon.awssdk.utils.Logger;

/**
 * A wrapper of InputStream that implements chunked encoding.
 * <p/>
 * Each chunk will be buffered.
 * <p/>
 * This class will use the mark() & reset() of the wrapped InputStream if they
 * are supported, otherwise it will create a buffer for bytes read from
 * the wrapped stream.
 */
@SdkInternalApi
public abstract class AwsChunkedEncodingInputStream extends SdkInputStream {

    private static final int SKIP_BUFFER_SIZE = 256 * 1024;
    private static final byte[] FINAL_CHUNK = new byte[0];
    private static final Logger log = Logger.loggerFor(AwsChunkedEncodingInputStream.class);

    private InputStream is = null;
    private final int chunkSize;
    private final int maxBufferSize;

    /**
     * Iterator on the current chunk.
     */
    private ChunkContentIterator currentChunkIterator;

    /**
     * Iterator on the buffer of the decoded stream,
     * Null if the wrapped stream is marksupported,
     * otherwise it will be initialized when this wrapper is marked.
     */
    private DecodedStreamBuffer decodedStreamBuffer;

    private boolean isAtStart = true;
    private boolean isTerminating = false;

    /**
     * Creates a chunked encoding input stream initialized with the originating stream. The configuration allows
     * specification of the size of each chunk, as well as the buffer size. Use the same values as when
     * calculating total length of the stream.
     *
     * @param in              The original InputStream.
     * @param config          The configuration allows the user to customize chunk size and buffer size.
     *                        See {@link AwsChunkedEncodingConfig} for default values.
     */
    protected AwsChunkedEncodingInputStream(InputStream in,
                                         AwsChunkedEncodingConfig config) {

        AwsChunkedEncodingConfig awsChunkedEncodingConfig = config == null ? AwsChunkedEncodingConfig.create() : config;

        int providedMaxBufferSize = awsChunkedEncodingConfig.bufferSize();
        if (in instanceof AwsChunkedEncodingInputStream) {
            // This could happen when the request is retried.
            AwsChunkedEncodingInputStream originalChunkedStream = (AwsChunkedEncodingInputStream) in;
            providedMaxBufferSize = Math.max(originalChunkedStream.maxBufferSize, providedMaxBufferSize);
            is = originalChunkedStream.is;
            decodedStreamBuffer = originalChunkedStream.decodedStreamBuffer;
        } else {
            is = in;
            decodedStreamBuffer = null;
        }
        this.chunkSize = awsChunkedEncodingConfig.chunkSize();
        this.maxBufferSize = providedMaxBufferSize;
        if (maxBufferSize < chunkSize) {
            throw new IllegalArgumentException("Max buffer size should not be less than chunk size");
        }
    }

    protected abstract static class Builder<T extends Builder> {


        private InputStream inputStream;
        private AwsChunkedEncodingConfig awsChunkedEncodingConfig;

        protected Builder() {
        }

        protected InputStream inputStream() {
            return inputStream;
        }

        protected AwsChunkedEncodingConfig chunkedEncodingConfig() {
            return awsChunkedEncodingConfig;
        }

        /**
         * @param inputStream The original InputStream.
         * @return
         */
        public T inputStream(InputStream inputStream) {
            this.inputStream = inputStream;
            return (T) this;
        }

        /**
         * @param awsChunkedEncodingConfig Maximum number of bytes buffered by this class.
         * @return
         */
        public T awsChunkedEncodingConfig(AwsChunkedEncodingConfig awsChunkedEncodingConfig) {
            this.awsChunkedEncodingConfig = awsChunkedEncodingConfig;
            return (T) this;
        }

    }

    @Override
    public int read() throws IOException {
        byte[] tmp = new byte[1];
        int count = read(tmp, 0, 1);
        if (count != -1) {
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
        if (b == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        if (null == currentChunkIterator || !currentChunkIterator.hasNext()) {
            if (isTerminating) {
                return -1;
            } else {
                isTerminating = setUpNextChunk();
            }
        }

        int count = currentChunkIterator.read(b, off, len);
        if (count > 0) {
            isAtStart = false;
            log.trace(() -> count + " byte read from the stream.");
        }
        return count;
    }

    @Override
    public long skip(long n) throws IOException {
        if (n <= 0) {
            return 0;
        }
        long remaining = n;
        int toskip = (int) Math.min(SKIP_BUFFER_SIZE, n);
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
            throw new UnsupportedOperationException("Chunk-encoded stream only supports mark() at the start of the stream.");
        }
        if (is.markSupported()) {
            log.debug(() -> "AwsChunkedEncodingInputStream marked at the start of the stream "
                            + "(will directly mark the wrapped stream since it's mark-supported).");
            is.mark(readlimit);
        } else {
            log.debug(() -> "AwsChunkedEncodingInputStream marked at the start of the stream "
                            + "(initializing the buffer since the wrapped stream is not mark-supported).");
            decodedStreamBuffer = new DecodedStreamBuffer(maxBufferSize);
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
            log.debug(() -> "AwsChunkedEncodingInputStream reset "
                            + "(will reset the wrapped stream because it is mark-supported).");
            is.reset();
        } else {
            log.debug(() -> "AwsChunkedEncodingInputStream reset (will use the buffer of the decoded stream).");
            if (null == decodedStreamBuffer) {
                throw new IOException("Cannot reset the stream because the mark is not set.");
            }
            decodedStreamBuffer.startReadBuffer();
        }

        isAtStart = true;
        isTerminating = false;
    }


    /**
     * Read in the next chunk of data, and create the necessary chunk extensions.
     *
     * @return Returns true if next chunk is the last empty chunk.
     */
    private boolean setUpNextChunk() throws IOException {
        byte[] chunkData = new byte[chunkSize];
        int chunkSizeInBytes = 0;
        while (chunkSizeInBytes < chunkSize) {
            /** Read from the buffer of the decoded stream */
            if (null != decodedStreamBuffer && decodedStreamBuffer.hasNext()) {
                chunkData[chunkSizeInBytes++] = decodedStreamBuffer.next();
            } else { /** Read from the wrapped stream */
                int bytesToRead = chunkSize - chunkSizeInBytes;
                int count = is.read(chunkData, chunkSizeInBytes, bytesToRead);
                if (count != -1) {
                    if (null != decodedStreamBuffer) {
                        decodedStreamBuffer.buffer(chunkData, chunkSizeInBytes, count);
                    }
                    chunkSizeInBytes += count;
                } else {
                    break;
                }
            }
        }
        if (chunkSizeInBytes == 0) {
            byte[] signedFinalChunk = createFinalChunk(FINAL_CHUNK);
            currentChunkIterator = new ChunkContentIterator(signedFinalChunk);
            return true;
        } else {
            if (chunkSizeInBytes < chunkData.length) {
                chunkData = Arrays.copyOf(chunkData, chunkSizeInBytes);
            }
            byte[] signedChunkContent = createChunk(chunkData);
            currentChunkIterator = new ChunkContentIterator(signedChunkContent);
            return false;
        }
    }


    @Override
    protected InputStream getWrappedInputStream() {
        return is;
    }


    /**
     * The final chunk.
     *
     * @param finalChunk The last byte which will be often 0 byte.
     * @return Final chunk that will be appended with CRLF or any required signatures.
     */
    protected abstract byte[] createFinalChunk(byte[] finalChunk);

    /**
     * Creates chunk for the given buffer.
     * The chucks could be appended with Signatures or any additional bytes by Concrete classes.
     *
     * @param chunkData The chunk of original data.
     * @return Chunked data which will have signature if signed or just data if unsigned.
     */
    protected abstract byte[] createChunk(byte[] chunkData);

}
