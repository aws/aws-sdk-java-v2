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

package software.amazon.awssdk.core.internal.chunked;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;

/**
 * Push-based aws-chunked response parser shared by the sync and async decoders.
 *
 * <p>Chunk data is never copied. While {@link #dataRemaining()} is zero, feed bytes to {@link #consumeFramingByte(int)};
 * otherwise pass data through and report it via {@link #consumeData(long)}. Call {@link #endOfInput()} when input ends.
 *
 * <p>Chunk extensions, including {@code chunk-signature}, are ignored; signatures are not verified.
 *
 * <p>After any error, every call rethrows it. Not thread safe.
 */
@SdkInternalApi
public final class AwsChunkedResponseParser {

    /**
     * Max length of a chunk-size or trailer line, and of the whole trailer section. Generous to leave room for unknown
     * chunk extensions.
     */
    static final int MAX_LINE_LENGTH = 64 * 1024;

    // Max server content echoed into an error message
    private static final int MAX_ECHOED_LENGTH = 64;

    private enum State {
        CHUNK_SIZE,       // reading hex size line
        CHUNK_DATA,       // caller is consuming chunk data
        CHUNK_DATA_CRLF,  // expecting CRLF after chunk data
        TRAILER_LINES,    // reading trailer lines after terminal chunk
        COMPLETE          // terminal chunk and trailers fully parsed
    }

    private final long expectedDecodedLength;
    private final Set<String> declaredTrailers;
    private final StringBuilder lineBuffer = new StringBuilder();
    private final Map<String, String> trailers = new LinkedHashMap<>();

    private State state = State.CHUNK_SIZE;
    private long chunkRemaining;
    private long totalDecoded;
    private boolean seenCr;
    private boolean inputEnded;
    private int trailerSectionLength;
    private SdkClientException failure;

    /**
     * @param expectedDecodedLength {@code x-amz-decoded-content-length}, or negative to skip the length check
     * @param declaredTrailers      names from {@code x-amz-trailer}, compared case-insensitively. Each must arrive, and no
     *                              others are accepted. {@code null} skips both checks.
     */
    public AwsChunkedResponseParser(long expectedDecodedLength, Set<String> declaredTrailers) {
        this.expectedDecodedLength = expectedDecodedLength;
        this.declaredTrailers = declaredTrailers == null ? null : lowerCase(declaredTrailers);
    }

    private static Set<String> lowerCase(Set<String> names) {
        Set<String> result = new HashSet<>();
        for (String name : names) {
            result.add(name.toLowerCase(Locale.ROOT));
        }
        return result;
    }

    /**
     * Data bytes left in the current chunk; zero when the next byte is framing.
     */
    public long dataRemaining() {
        throwIfFailed();
        return state == State.CHUNK_DATA ? chunkRemaining : 0;
    }

    /**
     * Records {@code count} data bytes consumed; must not exceed {@link #dataRemaining()}.
     */
    public void consumeData(long count) {
        throwIfFailed();
        if (state != State.CHUNK_DATA || count < 0 || count > chunkRemaining) {
            throw new IllegalStateException("Cannot consume " + count + " data bytes in state " + state
                                            + " with " + chunkRemaining + " bytes remaining in chunk");
        }
        chunkRemaining -= count;
        totalDecoded += count;
        if (chunkRemaining == 0) {
            state = State.CHUNK_DATA_CRLF;
            seenCr = false;
        }
    }

    /**
     * Feeds one framing byte (0-255). Only valid while {@link #dataRemaining()} is zero.
     */
    public void consumeFramingByte(int b) {
        throwIfFailed();
        switch (state) {
            case CHUNK_SIZE:
                String sizeLine = appendLineByte(b);
                if (sizeLine != null) {
                    handleChunkSizeLine(sizeLine);
                }
                break;
            case CHUNK_DATA_CRLF:
                handleChunkDataCrlfByte(b);
                break;
            case TRAILER_LINES:
                if (++trailerSectionLength > MAX_LINE_LENGTH) {
                    throw fail("Trailer section exceeds maximum length of " + MAX_LINE_LENGTH + " bytes");
                }
                String trailerLine = appendLineByte(b);
                if (trailerLine != null) {
                    handleTrailerLine(trailerLine);
                }
                break;
            case COMPLETE:
                throw fail("Data found after terminal chunk and trailers");
            case CHUNK_DATA:
                throw new IllegalStateException("Framing byte fed while " + chunkRemaining + " data bytes remain");
            default:
                throw new IllegalStateException("Unknown state: " + state);
        }
    }

    /**
     * Signals end of input; fails if the message is incomplete.
     */
    public void endOfInput() {
        throwIfFailed();
        if (state != State.COMPLETE) {
            throw fail("Unexpected end of stream while in state " + state);
        }
        inputEnded = true;
    }

    /**
     * Whether the terminal chunk and trailers have been parsed.
     */
    public boolean isComplete() {
        throwIfFailed();
        return state == State.COMPLETE;
    }

    /**
     * Trailers with lower-cased names, in order received.
     *
     * @throws IllegalStateException if {@link #endOfInput()} has not succeeded
     */
    public Map<String, String> trailers() {
        if (failure != null || !inputEnded) {
            throw new IllegalStateException("Trailers are only available after the stream has been fully decoded");
        }
        return Collections.unmodifiableMap(trailers);
    }

    /**
     * @return the completed line without CRLF, or {@code null} if incomplete
     */
    private String appendLineByte(int b) {
        if (b == '\r') {
            if (seenCr) {
                throw fail("CR not followed by LF");
            }
            seenCr = true;
            return null;
        }
        if (b == '\n') {
            if (!seenCr) {
                throw fail("LF without preceding CR");
            }
            seenCr = false;
            String line = lineBuffer.toString();
            lineBuffer.setLength(0);
            return line;
        }
        if (seenCr) {
            throw fail("CR not followed by LF");
        }
        if (lineBuffer.length() >= MAX_LINE_LENGTH) {
            throw fail("Line exceeds maximum length of " + MAX_LINE_LENGTH + " bytes");
        }
        lineBuffer.append((char) (b & 0xFF));
        return null;
    }

    private void handleChunkSizeLine(String line) {
        // Chunk extensions are ignored
        int semiIndex = line.indexOf(';');
        String hexSize = (semiIndex >= 0 ? line.substring(0, semiIndex) : line).trim();

        if (hexSize.isEmpty()) {
            throw fail("Empty chunk size");
        }

        // Chunk size is 1*HEXDIG (RFC 9112)
        for (int i = 0; i < hexSize.length(); i++) {
            char c = hexSize.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) {
                throw fail("Invalid character in chunk size: " + truncate(hexSize));
            }
        }

        long size;
        try {
            size = Long.parseLong(hexSize, 16);
        } catch (NumberFormatException e) {
            throw fail("Invalid hex chunk size: " + truncate(hexSize));
        }

        if (size == 0) {
            if (expectedDecodedLength >= 0 && totalDecoded != expectedDecodedLength) {
                throw fail(String.format("Decoded content length mismatch: expected %d bytes but decoded %d bytes",
                                         expectedDecodedLength, totalDecoded));
            }
            state = State.TRAILER_LINES;
        } else {
            // Fail before handing out any data beyond the declared length
            if (expectedDecodedLength >= 0 && size > expectedDecodedLength - totalDecoded) {
                throw fail(String.format("Chunk of %d bytes exceeds decoded content length: expected %d bytes, "
                                         + "already decoded %d bytes", size, expectedDecodedLength, totalDecoded));
            }
            chunkRemaining = size;
            state = State.CHUNK_DATA;
        }
    }

    private void handleChunkDataCrlfByte(int b) {
        if (!seenCr) {
            if (b != '\r') {
                throw fail("Expected CRLF after chunk data, got: " + b);
            }
            seenCr = true;
            return;
        }
        if (b != '\n') {
            throw fail("Expected CRLF after chunk data, got: CR+" + b);
        }
        seenCr = false;
        state = State.CHUNK_SIZE;
    }

    private void handleTrailerLine(String line) {
        if (line.isEmpty()) {
            // End of trailers: every declared trailer must have arrived
            if (declaredTrailers != null) {
                Set<String> missing = new TreeSet<>(declaredTrailers);
                missing.removeAll(trailers.keySet());
                if (!missing.isEmpty()) {
                    throw fail("Declared trailer not received: " + truncate(String.join(", ", missing)));
                }
            }
            state = State.COMPLETE;
            return;
        }

        int colonIndex = line.indexOf(':');
        if (colonIndex < 0) {
            throw fail("Malformed trailer (no colon): " + truncate(line));
        }
        String name = line.substring(0, colonIndex).trim().toLowerCase(Locale.ROOT);
        if (name.isEmpty()) {
            throw fail("Malformed trailer (empty name): " + truncate(line));
        }
        if (declaredTrailers != null && !declaredTrailers.contains(name)) {
            throw fail("Trailer not declared in x-amz-trailer: " + truncate(name));
        }
        String value = line.substring(colonIndex + 1).trim();
        if (trailers.put(name, value) != null) {
            throw fail("Duplicate trailer: " + truncate(name));
        }
    }

    private static String truncate(String s) {
        return s.length() <= MAX_ECHOED_LENGTH ? s : s.substring(0, MAX_ECHOED_LENGTH) + "...";
    }

    private void throwIfFailed() {
        if (failure != null) {
            throw failure;
        }
    }

    private SdkClientException fail(String message) {
        failure = SdkClientException.builder()
                                    .message("Malformed aws-chunked response: " + message)
                                    .build();
        return failure;
    }
}
