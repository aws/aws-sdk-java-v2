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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.http.auth.aws.internal.io.SdkInputStream;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Pair;


/**
 * An implementation of chunked transfer encoding, which wrap an @{link InputStream}.
 * This implementation supports chunk-headers, chunk-extensions, and trailers.
 */
@SdkProtectedApi
public final class ChunkEncodedInputStream extends SdkInputStream {
    private static final Logger LOG = Logger.loggerFor(ChunkEncodedInputStream.class);
    private static final byte[] CRLF = {'\r', '\n'};
    private static final byte[] END = {};

    private final InputStream inputStream;
    private final int chunkSize;

    private final ChunkHeader header;
    private final List<ChunkExtension> extensions = new ArrayList<>();
    private final List<Trailer> trailers = new ArrayList<>();

    private Chunk currentChunk;
    private boolean isFinished = false;

    public ChunkEncodedInputStream(
        InputStream inputStream,
        int chunkSize,
        ChunkHeader header,
        List<ChunkExtension> extensions,
        List<Trailer> trailers
    ) {
        this.inputStream = inputStream;
        this.chunkSize = chunkSize;
        this.header = header;
        this.extensions.addAll(extensions);
        this.trailers.addAll(trailers);
    }

    @Override
    protected InputStream getWrappedInputStream() {
        return inputStream;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    @Override
    public int read() throws IOException {
        if (currentChunk == null || currentChunk.ended() && !isFinished) {
            readChunk(inputStream);
        }

        return currentChunk.stream().read();
    }

    private void readChunk(InputStream stream) throws IOException {
        LOG.debug(() -> "Reading next chunk.");
        if (currentChunk != null) {
            currentChunk.close();
        }
        // we *have* to read from the backing stream in order to figure out if it's the end or not
        byte[] chunkData = new byte[chunkSize];
        int read = stream.read(chunkData, 0, chunkSize);

        if (read > 0) {
            // form the new chunk, which includes the header, the extensions, and the chunk data
            byte[] chunk = Arrays.copyOf(chunkData, read);
            ByteArrayOutputStream chunkStream = new ByteArrayOutputStream();
            writeChunk(chunk, chunkStream);
            chunkStream.write(CRLF);
            chunk = chunkStream.toByteArray();
            // set the current chunk to the newly written chunk
            currentChunk = Chunk.create(new ByteArrayInputStream(chunk), chunk.length);
        } else {
            LOG.debug(() -> "End of backing stream reached. Reading final chunk.");
            isFinished = true;

            // form the final chunk, which includes the header, the extensions, the chunk (if applicable), and the trailer
            byte[] chunk = END;
            ByteArrayOutputStream chunkStream = new ByteArrayOutputStream();
            writeChunk(chunk, chunkStream);
            writeTrailers(chunk, chunkStream);
            chunkStream.write(CRLF);
            chunk = chunkStream.toByteArray();
            // set the current chunk to the newly written chunk
            currentChunk = Chunk.create(new ByteArrayInputStream(chunk), chunk.length);
        }
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
        List<Pair<byte[], byte[]>> exts = extensions.stream().map(ext -> ext.get(chunk)).collect(Collectors.toList());
        for (Pair<byte[], byte[]> ext : exts) {
            outputStream.write((byte) ';');
            outputStream.write(ext.left());
            outputStream.write((byte) '=');
            outputStream.write(ext.right());
        }
    }

    private void writeTrailers(byte[] chunk, ByteArrayOutputStream outputStream) throws IOException {
        List<Pair<byte[], byte[]>> tlrs = trailers.stream().map(tlr -> tlr.get(chunk)).collect(Collectors.toList());
        for (Pair<byte[], byte[]> tlr : tlrs) {
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
}

