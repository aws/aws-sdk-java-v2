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

package software.amazon.awssdk.http.auth.aws.chunkedencoding;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.http.auth.aws.internal.io.SdkInputStream;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.Validate;


/**
 * An implementation of chunked transfer encoding, which wraps an @{link InputStream}.
 * This implementation supports chunk-headers, chunk-extensions, and trailers.
 */
@SdkProtectedApi
public final class ChunkedEncodedInputStream extends SdkInputStream {
    private static final Logger LOG = Logger.loggerFor(ChunkedEncodedInputStream.class);
    private static final byte[] CRLF = {'\r', '\n'};
    private static final byte[] END = {};

    private final InputStream inputStream;
    private final int chunkSize;

    private final ChunkHeader header;
    private final List<ChunkExtension> extensions = new ArrayList<>();
    private final List<Trailer> trailers = new ArrayList<>();

    private Chunk currentChunk;
    private boolean isFinished = false;

    private ChunkedEncodedInputStream(BuilderImpl builder) {
        this.inputStream = Validate.notNull(builder.inputStream, "Input-Stream cannot be null!");
        this.chunkSize = Validate.isPositive(builder.chunkSize, "Chunk-size must be greater than 0!");
        this.header = Validate.notNull(builder.header, "Header cannot be null!");
        this.extensions.addAll(Validate.notNull(builder.extensions, "Extensions cannot be null!"));
        this.trailers.addAll(Validate.notNull(builder.trailers, "Trailers cannot be null!"));
    }

    @Override
    protected InputStream getWrappedInputStream() {
        return inputStream;
    }

    @Override
    public int read() throws IOException {
        if (currentChunk == null || currentChunk.hasEnded() && !isFinished) {
            currentChunk = getChunk(inputStream);
        }

        return currentChunk.stream().read();
    }

    /**
     * Get an encoded chunk from the input-stream, or the final chunk if we've reached the end of the input-stream.
     */
    private Chunk getChunk(InputStream stream) throws IOException {
        LOG.debug(() -> "Reading next chunk.");
        if (currentChunk != null) {
            currentChunk.close();
        }
        // we *have* to read from the backing stream in order to figure out if it's the end or not
        byte[] chunkData = new byte[chunkSize];
        int read = read(stream, chunkData, chunkSize);

        if (read > 0) {
            // set the current chunk to the newly written chunk
            return getNextChunk(Arrays.copyOf(chunkData, read));
        }

        LOG.debug(() -> "End of backing stream reached. Reading final chunk.");
        isFinished = true;
        // set the current chunk to the written final chunk
        return getFinalChunk();
    }

    /**
     * Read from an input-stream, up to a max number of bytes, storing them in a byte-array.
     * The actual number of bytes can be less than the max in the event that we reach the end of the stream.
     * <p>
     * This method is necessary because we cannot assume the backing stream uses the default implementation of
     * {@code read(byte b[], int off, int len)}
     */
    private int read(InputStream inputStream, byte[] buf, int maxBytesToRead) throws IOException {
        int read = 0;
        int offset = 0;
        while (read >= 0 && offset < maxBytesToRead) {
            read = inputStream.read();
            if (read >= 0) {
                buf[offset] = (byte) read;
                offset += 1;
            }
        }
        return offset;
    }

    /**
     * Create a chunk from a byte-array, which includes the header, the extensions, and the chunk data.
     * The input array should be correctly sized, i.e. the number of bytes should equal its length.
     */
    private Chunk getNextChunk(byte[] data) throws IOException {
        ByteArrayOutputStream chunkStream = new ByteArrayOutputStream();
        writeChunk(data, chunkStream);
        chunkStream.write(CRLF);
        byte[] newChunkData = chunkStream.toByteArray();

        return Chunk.create(new ByteArrayInputStream(newChunkData), newChunkData.length);
    }

    /**
     * Create the final chunk, which includes the header, the extensions, the chunk (if applicable), and the trailer
     */
    private Chunk getFinalChunk() throws IOException {
        ByteArrayOutputStream chunkStream = new ByteArrayOutputStream();
        writeChunk(END, chunkStream);
        writeTrailers(END, chunkStream);
        chunkStream.write(CRLF);
        byte[] newChunkData = chunkStream.toByteArray();

        return Chunk.create(new ByteArrayInputStream(newChunkData), newChunkData.length);
    }

    private void writeChunk(byte[] chunk, ByteArrayOutputStream outputStream) throws IOException {
        writeHeader(chunk, outputStream);
        writeExtensions(chunk, outputStream);
        outputStream.write(CRLF);
        outputStream.write(chunk);
    }

    private void writeHeader(byte[] chunk, ByteArrayOutputStream outputStream) throws IOException {
        byte[] hdr = header.get(chunk);
        outputStream.write(hdr);
    }

    private void writeExtensions(byte[] chunk, ByteArrayOutputStream outputStream) throws IOException {
        for (ChunkExtension chunkExtension : extensions) {
            Pair<byte[], byte[]> ext = chunkExtension.get(chunk);
            outputStream.write((byte) ';');
            outputStream.write(ext.left());
            outputStream.write((byte) '=');
            outputStream.write(ext.right());
        }
    }

    private void writeTrailers(byte[] chunk, ByteArrayOutputStream outputStream) throws IOException {
        for (Trailer trailer : trailers) {
            Pair<byte[], byte[]> tlr = trailer.get(chunk);
            outputStream.write(tlr.left());
            outputStream.write((byte) ':');
            outputStream.write(tlr.right());
            outputStream.write(CRLF);
        }
    }

    @Override
    public synchronized void mark(int readlimit) {
        // TODO: Implement this
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized void reset() {
        // TODO: Implement this
        throw new UnsupportedOperationException();
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder {

        /**
         * Set the backing input stream.
         */
        Builder inputStream(InputStream inputStream);

        /**
         * Set the size of chunks from input stream.
         * The actual size (in bytes) of an encoded chunk depends on the configuration.
         */
        Builder chunkSize(int chunkSize);

        /**
         * Set the header to be used when creating an encoded chunk.
         * This header will be the first part of an encoded chunk.
         */
        Builder header(ChunkHeader header);

        /**
         * Set the chunk-extensions to be used when creating an encoded chunk.
         * These extensions will immediately follow the header.
         */
        Builder extensions(List<ChunkExtension> extensions);

        /**
         * Add a chunk-extension.
         */
        Builder addExtension(ChunkExtension extension);

        /**
         * Set the trailers to be used when creating the final chunk.
         * These trailers will immediately follow the final encoded chunk.
         */
        Builder trailers(List<Trailer> trailers);

        /**
         * Add a trailer.
         */
        Builder addTrailer(Trailer trailer);

        ChunkedEncodedInputStream build();
    }

    private static class BuilderImpl implements Builder {
        private InputStream inputStream;
        private int chunkSize;
        private ChunkHeader header = chunk -> Integer.toHexString(chunk.length).getBytes(StandardCharsets.UTF_8);
        private final List<ChunkExtension> extensions = new ArrayList<>();
        private final List<Trailer> trailers = new ArrayList<>();

        @Override
        public Builder inputStream(InputStream inputStream) {
            this.inputStream = inputStream;
            return this;
        }

        @Override
        public Builder chunkSize(int chunkSize) {
            this.chunkSize = chunkSize;
            return this;
        }

        @Override
        public Builder header(ChunkHeader header) {
            this.header = header;
            return this;
        }

        @Override
        public Builder extensions(List<ChunkExtension> extensions) {
            this.extensions.clear();
            extensions.forEach(this::addExtension);
            return this;
        }

        @Override
        public Builder addExtension(ChunkExtension extension) {
            this.extensions.add(Validate.notNull(extension, "Extension cannot be null!"));
            return this;
        }

        @Override
        public Builder trailers(List<Trailer> trailers) {
            this.trailers.clear();
            trailers.forEach(this::addTrailer);
            return this;
        }

        @Override
        public Builder addTrailer(Trailer trailer) {
            this.trailers.add(Validate.notNull(trailer, "Trailer cannot be null!"));
            return this;
        }

        @Override
        public ChunkedEncodedInputStream build() {
            return new ChunkedEncodedInputStream(this);
        }
    }
}

