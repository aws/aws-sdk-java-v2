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

package software.amazon.awssdk.http.auth.aws.internal.signer.chunkedencoding;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.Validate;


/**
 * An implementation of chunk-transfer encoding, but by wrapping an {@link InputStream}. This implementation supports
 * chunk-headers, chunk-extensions, and trailers.
 * <p>
 * Per <a href="https://datatracker.ietf.org/doc/html/rfc7230#section-4.1">RFC-7230</a>, a chunk-transfer encoded message is
 * defined as:
 * <pre>
 *     chunked-body   = *chunk
 *                      last-chunk
 *                      trailer-part
 *                      CRLF
 *     chunk          = chunk-size [ chunk-ext ] CRLF
 *                      chunk-data CRLF
 *     chunk-size     = 1*HEXDIG
 *     last-chunk     = 1*("0") [ chunk-ext ] CRLF
 *     chunk-data     = 1*OCTET ; a sequence of chunk-size octets
 * </pre>
 */
@SdkInternalApi
public final class ChunkedEncodedInputStream extends InputStream {
    private static final Logger LOG = Logger.loggerFor(ChunkedEncodedInputStream.class);
    private static final byte[] CRLF = {'\r', '\n'};
    private static final byte[] END = {};
    private static final byte[] SEMICOLON = {';'};
    private static final byte[] EQUALS = {'='};
    private static final byte[] COLON = {':'};
    private static final byte[] COMMA = {','};

    private final InputStream inputStream;
    private final int chunkSize;

    private final ChunkHeaderProvider header;
    private final List<ChunkExtensionProvider> extensions = new ArrayList<>();
    private final List<TrailerProvider> trailers = new ArrayList<>();

    private Chunk currentChunk;
    private boolean isFinished = false;

    private ChunkedEncodedInputStream(Builder builder) {
        this.inputStream = Validate.notNull(builder.inputStream, "Input-Stream cannot be null!");
        this.chunkSize = Validate.isPositive(builder.chunkSize, "Chunk-size must be greater than 0!");
        this.header = Validate.notNull(builder.header, "Header cannot be null!");
        this.extensions.addAll(Validate.notNull(builder.extensions, "Extensions cannot be null!"));
        this.trailers.addAll(Validate.notNull(builder.trailers, "Trailers cannot be null!"));
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public int read() throws IOException {
        return currentChunk().stream().read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return currentChunk().stream().read(b, off, len);
    }

    private Chunk currentChunk() throws IOException {
        if (currentChunk == null || !currentChunk.hasRemaining() && !isFinished) {
            currentChunk = getChunk(inputStream);
        }

        return currentChunk;
    }

    /**
     * Get an encoded chunk from the input-stream, or the final chunk if we've reached the end of the input-stream.
     */
    private Chunk getChunk(InputStream stream) throws IOException {
        LOG.debug(() -> "Reading next chunk.");
        if (currentChunk != null) {
            currentChunk.close();
        }

        // We have to read from the input stream into a format that can be used for signing and headers.
        byte[] chunkData = new byte[chunkSize];
        int read = read(stream, chunkData, chunkSize);

        if (read > 0) {
            // set the current chunk to the newly written chunk
            return getNextChunk(ByteBuffer.wrap(chunkData, 0, read));
        }

        LOG.debug(() -> "End of backing stream reached. Reading final chunk.");
        isFinished = true;
        // set the current chunk to the written final chunk
        return getFinalChunk();
    }

    /**
     * Read from an input-stream, up to a max number of bytes, storing them in a byte-array. The actual number of bytes can be
     * less than the max in the event that we reach the end of the stream.
     * <p>
     * This method is necessary because we cannot assume the backing stream uses the default implementation of
     * {@code read(byte b[], int off, int len)}
     */
    private int read(InputStream inputStream, byte[] buf, int maxBytesToRead) throws IOException {
        int read;
        int offset = 0;
        do {
            read = inputStream.read(buf, offset, maxBytesToRead - offset);
            assert read != 0;
            if (read > 0) {
                offset += read;
            }
        } while (read > 0 && offset < maxBytesToRead);

        return offset;
    }

    /**
     * Create a chunk from a byte-array, which includes the header, the extensions, and the chunk data. The input array should be
     * correctly sized, i.e. the number of bytes should equal its length.
     */
    private Chunk getNextChunk(ByteBuffer data) {
        LengthAwareSequenceInputStream newChunkData =
            LengthAwareSequenceInputStream.builder()
                                          .add(createChunkStream(data))
                                          .add(CRLF)
                                          .build();
        return Chunk.create(newChunkData, newChunkData.size);
    }

    /**
     * Create the final chunk, which includes the header, the extensions, the chunk (if applicable), and the trailer
     */
    private Chunk getFinalChunk() throws IOException {
        LengthAwareSequenceInputStream chunkData =
            LengthAwareSequenceInputStream.builder()
                                          .add(createChunkStream(ByteBuffer.wrap(END)))
                                          .add(createTrailerStream())
                                          .add(CRLF)
                                          .build();

        return Chunk.create(chunkData, chunkData.size);
    }

    private LengthAwareSequenceInputStream createChunkStream(ByteBuffer chunkData) {
        return LengthAwareSequenceInputStream.builder()
                                             .add(createHeaderStream(chunkData.asReadOnlyBuffer()))
                                             .add(createExtensionsStream(chunkData.asReadOnlyBuffer()))
                                             .add(CRLF)
                                             .add(new ByteArrayInputStream(chunkData.array(),
                                                                           chunkData.arrayOffset(),
                                                                           chunkData.remaining()))
                                             .build();
    }

    private ByteArrayInputStream createHeaderStream(ByteBuffer chunkData) {
        return new ByteArrayInputStream(header.get(chunkData));
    }

    private LengthAwareSequenceInputStream createExtensionsStream(ByteBuffer chunkData) {
        LengthAwareSequenceInputStream.Builder result = LengthAwareSequenceInputStream.builder();
        for (ChunkExtensionProvider chunkExtensionProvider : extensions) {
            Pair<byte[], byte[]> ext = chunkExtensionProvider.get(chunkData);
            result.add(SEMICOLON);
            result.add(ext.left());
            result.add(EQUALS);
            result.add(ext.right());
        }
        return result.build();
    }

    private LengthAwareSequenceInputStream createTrailerStream() throws IOException {
        LengthAwareSequenceInputStream.Builder result = LengthAwareSequenceInputStream.builder();
        for (TrailerProvider trailer : trailers) {
            Pair<String, List<String>> tlr = trailer.get();
            result.add(tlr.left().getBytes(StandardCharsets.UTF_8));
            result.add(COLON);
            for (String trailerValue : tlr.right()) {
                result.add(trailerValue.getBytes(StandardCharsets.UTF_8));
                result.add(COMMA);
            }

            // Replace trailing comma with clrf
            result.replaceLast(new ByteArrayInputStream(CRLF), COMMA.length);
        }
        return result.build();
    }

    @Override
    public synchronized void reset() throws IOException {
        trailers.forEach(TrailerProvider::reset);
        extensions.forEach(ChunkExtensionProvider::reset);
        header.reset();
        inputStream.reset();
        isFinished = false;
        currentChunk = null;
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }

    public static class Builder {
        private final List<ChunkExtensionProvider> extensions = new ArrayList<>();
        private final List<TrailerProvider> trailers = new ArrayList<>();
        private InputStream inputStream;
        private int chunkSize;
        private ChunkHeaderProvider header =
            chunk -> Integer.toHexString(chunk.remaining()).getBytes(StandardCharsets.UTF_8);

        public InputStream inputStream() {
            return this.inputStream;
        }

        public Builder inputStream(InputStream inputStream) {
            this.inputStream = inputStream;
            return this;
        }

        public Builder chunkSize(int chunkSize) {
            this.chunkSize = chunkSize;
            return this;
        }

        public Builder header(ChunkHeaderProvider header) {
            this.header = header;
            return this;
        }

        public Builder extensions(List<ChunkExtensionProvider> extensions) {
            this.extensions.clear();
            extensions.forEach(this::addExtension);
            return this;
        }

        public Builder addExtension(ChunkExtensionProvider extension) {
            this.extensions.add(Validate.notNull(extension, "ExtensionProvider cannot be null!"));
            return this;
        }

        public List<TrailerProvider> trailers() {
            return new ArrayList<>(trailers);
        }

        public Builder trailers(List<TrailerProvider> trailers) {
            this.trailers.clear();
            trailers.forEach(this::addTrailer);
            return this;
        }

        public Builder addTrailer(TrailerProvider trailer) {
            this.trailers.add(Validate.notNull(trailer, "TrailerProvider cannot be null!"));
            return this;
        }

        public ChunkedEncodedInputStream build() {
            return new ChunkedEncodedInputStream(this);
        }
    }


    private static class LengthAwareSequenceInputStream extends SequenceInputStream {
        private final int size;

        private LengthAwareSequenceInputStream(Builder builder) {
            super(Collections.enumeration(builder.streams));
            this.size = builder.size;
        }

        private static Builder builder() {
            return new Builder();
        }

        private static class Builder {
            private final List<InputStream> streams = new ArrayList<>();
            private int size = 0;

            public Builder add(ByteArrayInputStream stream) {
                streams.add(stream);
                size += stream.available();
                return this;
            }

            public Builder add(byte[] stream) {
                return add(new ByteArrayInputStream(stream));
            }

            public Builder add(LengthAwareSequenceInputStream stream) {
                streams.add(stream);
                size += stream.size;
                return this;
            }

            public Builder replaceLast(ByteArrayInputStream stream, int lastLength) {
                streams.set(streams.size() - 1, stream);
                size -= lastLength;
                size += stream.available();
                return this;
            }

            public LengthAwareSequenceInputStream build() {
                return new LengthAwareSequenceInputStream(this);
            }
        }
    }
}

