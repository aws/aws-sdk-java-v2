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

package software.amazon.awssdk.auth.signer.internal.chunkedencoding;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.io.SdkInputStream;
import software.amazon.awssdk.utils.Logger;

/**
 * A wrapper of InputStream that implements chunked encoding.
 * <p/>
 * Each chunk will be buffered for the calculation of the chunk signature
 * which is added at the head of each chunk. The request signature and the chunk signatures will
 * be assumed to be hex-encoded strings.
 * <p/>
 * This class will use the mark() & reset() of the wrapped InputStream if they
 * are supported, otherwise it will create a buffer for bytes read from
 * the wrapped stream.
 */
@SdkInternalApi
public final class AwsChunkedEncodingInputStream extends SdkInputStream {

    private static final int SKIP_BUFFER_SIZE = 256 * 1024;
    private static final String CRLF = "\r\n";
    private static final String CHUNK_SIGNATURE_HEADER = ";chunk-signature=";
    private static final byte[] FINAL_CHUNK = new byte[0];
    private static final Logger log = Logger.loggerFor(AwsChunkedEncodingInputStream.class);

    private InputStream is = null;
    private final int chunkSize;
    private final int maxBufferSize;
    private final String headerSignature;
    private String previousChunkSignature;
    private final AwsChunkSigner chunkSigner;

    /**
     * Iterator on the current chunk that has been signed
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
     * Creates a chunked encoding input stream initialized with the originating stream, an http request seed signature
     * and a signer that can sign a chunk of bytes according to a chosen algorithm. The configuration allows
     * specification of the size of each chunk, as well as the buffer size. Use the same values as when
     * calculating total length of the stream {@link #calculateStreamContentLength(long, int, AwsChunkedEncodingConfig)}.
     *
     * @param in              The original InputStream.
     * @param headerSignature The signature of the signed headers of the request. This will be used for
     *                        calculating the signature of the first chunk. Observe that the format of
     *                        this parameter should be a hex-encoded string.
     * @param chunkSigner     The signer for each chunk of data, implementing the {@link AwsChunkSigner} interface.
     * @param config          The configuration allows the user to customize chunk size and buffer size.
     *                        See {@link AwsChunkedEncodingConfig} for default values.
     */
    public AwsChunkedEncodingInputStream(InputStream in,
                                         String headerSignature,
                                         AwsChunkSigner chunkSigner,
                                         AwsChunkedEncodingConfig config) {
        int providedMaxBufferSize = config.bufferSize();
        if (in instanceof AwsChunkedEncodingInputStream) {
            // This could happen when the request is retried, and we need to re-calculate the signatures.
            AwsChunkedEncodingInputStream originalChunkedStream = (AwsChunkedEncodingInputStream) in;
            providedMaxBufferSize = Math.max(originalChunkedStream.maxBufferSize, providedMaxBufferSize);
            is = originalChunkedStream.is;
            decodedStreamBuffer = originalChunkedStream.decodedStreamBuffer;
        } else {
            is = in;
            decodedStreamBuffer = null;
        }

        this.chunkSize = config.chunkSize();
        this.maxBufferSize = providedMaxBufferSize;

        this.headerSignature = headerSignature;
        this.previousChunkSignature = headerSignature;
        this.chunkSigner = chunkSigner;

        if (maxBufferSize < chunkSize) {
            throw new IllegalArgumentException("Max buffer size should not be less than chunk size");
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
        previousChunkSignature = headerSignature;
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

        currentChunkIterator = null;
        isAtStart = true;
        isTerminating = false;
    }

    /**
     * Calculates the expected total length of signed payload chunked stream.
     *
     * @param originalLength The length of the data
     * @param signatureLength The length of a calculated signature, dependent on which {@link AwsChunkSigner} is used
     * @param config The chunked encoding config determines the size of the chunks. Use the same values as when
     *               initializing the stream
     *               {@link #AwsChunkedEncodingInputStream(InputStream, String, AwsChunkSigner, AwsChunkedEncodingConfig)}.
     */
    public static long calculateStreamContentLength(long originalLength,
                                                    int signatureLength,
                                                    AwsChunkedEncodingConfig config) {
        if (originalLength < 0) {
            throw new IllegalArgumentException("Nonnegative content length expected.");
        }
        int chunkSize = config.chunkSize();
        long maxSizeChunks = originalLength / chunkSize;
        long remainingBytes = originalLength % chunkSize;
        return maxSizeChunks * calculateSignedChunkLength(chunkSize, signatureLength)
                + (remainingBytes > 0 ? calculateSignedChunkLength(remainingBytes, signatureLength) : 0)
                + calculateSignedChunkLength(0, signatureLength);
    }

    private static long calculateSignedChunkLength(long chunkDataSize, int signatureLength) {
        return Long.toHexString(chunkDataSize).length()
                + CHUNK_SIGNATURE_HEADER.length()
                + signatureLength
                + CRLF.length()
                + chunkDataSize
                + CRLF.length();
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
            byte[] signedFinalChunk = createSignedChunk(FINAL_CHUNK);
            currentChunkIterator = new ChunkContentIterator(signedFinalChunk);
            return true;
        } else {
            if (chunkSizeInBytes < chunkData.length) {
                chunkData = Arrays.copyOf(chunkData, chunkSizeInBytes);
            }
            byte[] signedChunkContent = createSignedChunk(chunkData);
            currentChunkIterator = new ChunkContentIterator(signedChunkContent);
            return false;
        }
    }

    private byte[] createSignedChunk(byte[] chunkData) {
        try {
            byte[] header = createSignedChunkHeader(chunkData);
            byte[] trailer = CRLF.getBytes(StandardCharsets.UTF_8);
            byte[] signedChunk = new byte[header.length + chunkData.length + trailer.length];
            System.arraycopy(header, 0, signedChunk, 0, header.length);
            System.arraycopy(chunkData, 0, signedChunk, header.length, chunkData.length);
            System.arraycopy(trailer, 0,
                    signedChunk, header.length + chunkData.length,
                    trailer.length);
            return signedChunk;
        } catch (Exception e) {
            throw SdkClientException.builder()
                                    .message("Unable to sign the chunked data. " + e.getMessage())
                                    .cause(e)
                                    .build();
        }
    }

    private byte[] createSignedChunkHeader(byte[] chunkData) {
        String chunkSignature = chunkSigner.signChunk(chunkData, previousChunkSignature);
        previousChunkSignature = chunkSignature;

        StringBuilder chunkHeader = new StringBuilder();
        chunkHeader.append(Integer.toHexString(chunkData.length));
        chunkHeader.append(CHUNK_SIGNATURE_HEADER)
                   .append(chunkSignature)
                   .append(CRLF);
        return chunkHeader.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected InputStream getWrappedInputStream() {
        return is;
    }
}
