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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.Abortable;
import software.amazon.awssdk.utils.Validate;

/**
 * Decodes an aws-chunked encoded response body, stripping chunk framing and extracting trailers.
 *
 * <p>Returns clean data bytes to the caller. After EOF, trailer key-value pairs are available
 * via {@link #trailers()}.
 *
 * <p>Handles incremental byte-at-a-time decoding.
 */
@SdkInternalApi
public final class AwsChunkedResponseDecoder extends InputStream implements Abortable {

    private final InputStream in;
    private final long expectedDecodedLength;

    private long chunkRemaining = -1;
    private long totalDecoded = 0;
    private boolean done = false;
    private Map<String, String> trailerMap;

    public AwsChunkedResponseDecoder(InputStream in, long expectedDecodedLength) {
        Validate.paramNotNull(in, "in");
        this.in = in;
        this.expectedDecodedLength = expectedDecodedLength;
    }

    /**
     * Returns trailer key-value pairs. Only available after EOF (read() returned -1).
     */
    public Map<String, String> trailers() {
        return trailerMap != null ? Collections.unmodifiableMap(trailerMap) : Collections.emptyMap();
    }

    @Override
    public int read() throws IOException {
        if (done) {
            return -1;
        }

        if (chunkRemaining <= 0) {
            if (!advanceToNextChunk()) {
                return -1;
            }
        }

        int b = readByte("chunk data");
        chunkRemaining--;
        totalDecoded++;
        return b;
    }

    @Override
    public int read(byte[] buf, int off, int len) throws IOException {
        if (done) {
            return -1;
        }
        if (buf == null) {
            throw new NullPointerException("Buffer must not be null");
        }
        if (off < 0 || len < 0 || len > buf.length - off) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return 0;
        }

        if (chunkRemaining <= 0) {
            if (!advanceToNextChunk()) {
                return -1;
            }
        }

        int toRead = (int) Math.min(len, chunkRemaining);
        int bytesRead = in.read(buf, off, toRead);
        if (bytesRead == -1) {
            throw sdkException("Unexpected end of stream while reading chunk data");
        }
        chunkRemaining -= bytesRead;
        totalDecoded += bytesRead;
        return bytesRead;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    @Override
    public void abort() {
        if (in instanceof Abortable) {
            ((Abortable) in).abort();
        }
    }

    /**
     * Advances past the current chunk boundary to the next chunk.
     * Returns true if there's data to read, false if we hit the terminal chunk (EOF).
     */
    private boolean advanceToNextChunk() throws IOException {
        if (chunkRemaining == 0) {
            consumeCrlf("after chunk data");
        }

        String sizeLine = readLine();
        // Strip chunk extensions (everything after ';')
        int semiIndex = sizeLine.indexOf(';');
        String hexSize = semiIndex >= 0 ? sizeLine.substring(0, semiIndex) : sizeLine;
        hexSize = hexSize.trim();

        if (hexSize.isEmpty()) {
            throw sdkException("Empty chunk size");
        }

        // RFC 9112 — chunk size is 1*HEXDIG
        for (int i = 0; i < hexSize.length(); i++) {
            char c = hexSize.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) {
                throw sdkException("Invalid character in chunk size: " + hexSize);
            }
        }

        long size;
        try {
            size = Long.parseLong(hexSize, 16);
        } catch (NumberFormatException e) {
            throw sdkException("Invalid hex chunk size: " + hexSize);
        }

        if (size == 0) {
            readTrailers();
            verifyNoTrailingData();
            validateLength();
            done = true;
            return false;
        }

        chunkRemaining = size;
        return true;
    }

    /**
     * Reads trailer lines until an empty line (CRLF).
     */
    private void readTrailers() throws IOException {
        trailerMap = new LinkedHashMap<>();
        while (true) {
            String line = readLine();
            if (line.isEmpty()) {
                return;
            }
            int colonIndex = line.indexOf(':');
            if (colonIndex < 0) {
                throw sdkException("Malformed trailer (no colon): " + line);
            }
            String name = line.substring(0, colonIndex).trim().toLowerCase(Locale.ROOT);
            String value = line.substring(colonIndex + 1).trim();
            trailerMap.put(name, value);
        }
    }

    /**
     * Reads a CRLF-terminated line from the stream. Returns the line content without CRLF.
     */
    private String readLine() throws IOException {
        StringBuilder sb = new StringBuilder();
        boolean seenCr = false;
        while (true) {
            int b = readByte("line");
            if (b == '\r') {
                seenCr = true;
            } else if (b == '\n') {
                if (!seenCr) {
                    throw sdkException("LF without preceding CR");
                }
                return sb.toString();
            } else {
                if (seenCr) {
                    throw sdkException("CR not followed by LF");
                }
                sb.append((char) b);
            }
        }
    }

    private void consumeCrlf(String context) throws IOException {
        int cr = readByte(context);
        if (cr != '\r') {
            throw sdkException("Expected CRLF " + context + ", got: " + cr);
        }
        int lf = readByte(context);
        if (lf != '\n') {
            throw sdkException("Expected CRLF " + context + ", got: CR+" + lf);
        }
    }

    private int readByte(String context) throws IOException {
        int b = in.read();
        if (b == -1) {
            throw sdkException("Unexpected end of stream while reading " + context);
        }
        return b;
    }

    private void verifyNoTrailingData() throws IOException {
        int b = in.read();
        if (b != -1) {
            throw sdkException("Data found after terminal chunk and trailers");
        }
    }

    private void validateLength() {
        if (expectedDecodedLength >= 0 && totalDecoded != expectedDecodedLength) {
            throw sdkException(String.format(
                "Decoded content length mismatch: expected %d bytes but decoded %d bytes",
                expectedDecodedLength, totalDecoded));
        }
    }

    private static SdkClientException sdkException(String message) {
        return SdkClientException.builder()
            .message("Malformed aws-chunked response: " + message)
            .build();
    }
}
