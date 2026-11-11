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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

/**
 * aws-chunked decoding vectors shared by the sync and async decoder tests. Each runs whole, 1-byte and 2-byte split.
 */
public final class AwsChunkedTestVectors {

    /**
     * Input split sizes: whole input, 1 byte, 2 bytes.
     */
    public static final List<Integer> SPLIT_SIZES = Collections.unmodifiableList(Arrays.asList(Integer.MAX_VALUE, 1, 2));

    private static final String CRC32_TRAILER = "x-amz-response-checksum-crc32";

    private AwsChunkedTestVectors() {
    }

    public static final class Vector {
        public final String name;
        public final byte[] encoded;
        public final long expectedDecodedLength;
        public final Set<String> declaredTrailers;
        public final byte[] expectedBody;
        public final Map<String, String> expectedTrailers;
        public final String expectedError;

        private Vector(String name, String encoded, long expectedDecodedLength, Set<String> declaredTrailers,
                       String expectedBody, Map<String, String> expectedTrailers, String expectedError) {
            this.name = name;
            this.encoded = encoded.getBytes(StandardCharsets.ISO_8859_1);
            this.expectedDecodedLength = expectedDecodedLength;
            this.declaredTrailers = declaredTrailers;
            this.expectedBody = expectedBody == null ? null : expectedBody.getBytes(StandardCharsets.ISO_8859_1);
            this.expectedTrailers = expectedTrailers;
            this.expectedError = expectedError;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static List<Vector> successVectors() {
        List<Vector> v = new ArrayList<>();
        // Standard success cases; decoded length equals body length
        v.add(success("single chunk with trailer",
                      "5\r\nhello\r\n0\r\ntrailer-key:abc123==\r\n\r\n", "hello", trailers("trailer-key", "abc123==")));
        v.add(success("multiple chunks with trailer",
                      "5\r\nhello\r\n6\r\n world\r\n0\r\ntrailer-key:AAAA==\r\n\r\n", "hello world",
                      trailers("trailer-key", "AAAA==")));
        v.add(success("chunk data containing CRLF",
                      "5\r\na\r\nbc\r\n0\r\n\r\n", "a\r\nbc", trailers()));
        v.add(success("trailer value surrounded by whitespace",
                      "3\r\nfoo\r\n0\r\ntrailer-key:  abc123==  \r\n\r\n", "foo", trailers("trailer-key", "abc123==")));
        v.add(success("chunk extensions",
                      "5;chunk-signature=abc;date=20260101\r\nhello\r\n0;chunk-signature=def\r\ntrailer-key:xyz==\r\n\r\n",
                      "hello", trailers("trailer-key", "xyz==")));
        v.add(success("multiple trailers",
                      "5\r\nhello\r\n0\r\nfirst-trailer:value1\r\nsecond-trailer:value2\r\n\r\n", "hello",
                      trailers("first-trailer", "value1", "second-trailer", "value2")));
        v.add(success("empty data",
                      "0\r\ntrailer-key:abc==\r\n\r\n", "", trailers("trailer-key", "abc==")));
        v.add(success("no trailer",
                      "5\r\nhello\r\n0\r\n\r\n", "hello", trailers()));

        // Additional cases
        v.add(success("trailer name is lower-cased",
                      "3\r\nfoo\r\n0\r\nX-Amz-Response-Checksum-CRC32:abc==\r\n\r\n", "foo",
                      trailers(CRC32_TRAILER, "abc==")));
        v.add(success("upper-case hex chunk size",
                      "A\r\n1234567890\r\n0\r\n\r\n", "1234567890", trailers()));
        v.add(new Vector("declared trailer is accepted",
                         "5\r\nhello\r\n0\r\nx-amz-response-checksum-crc32:abc==\r\n\r\n", 5, declared(CRC32_TRAILER),
                         "hello", trailers(CRC32_TRAILER, "abc=="), null));
        v.add(new Vector("unknown decoded length skips length check",
                         "5\r\nhello\r\n0\r\n\r\n", -1, null, "hello", trailers(), null));
        v.add(success("chunk size line at max length is accepted",
                      "5;" + repeat('a', AwsChunkedResponseParser.MAX_LINE_LENGTH - 2) + "\r\nhello\r\n0\r\n\r\n",
                      "hello", trailers()));
        return v;
    }

    public static List<Vector> errorVectors() {
        List<Vector> v = new ArrayList<>();
        // Standard error cases
        v.add(error("invalid hex in chunk size", "ZZ\r\n", "Invalid character in chunk size"));
        v.add(error("missing CRLF after chunk data", "3\r\nfooX", "Expected CRLF after chunk data"));
        v.add(error("malformed trailer (no colon)", "0\r\nnot-a-valid-trailer\r\n", "Malformed trailer (no colon)"));
        v.add(error("data after terminal chunk and trailer",
                    "3\r\nfoo\r\n0\r\ntrailer-key:abc==\r\n\r\nextra garbage", "Data found after terminal chunk"));
        v.add(new Vector("decoded content length mismatch",
                         "5\r\nhello\r\n0\r\ntrailer-key:abc==\r\n\r\n", 100, null, null, null,
                         "Decoded content length mismatch: expected 100 bytes but decoded 5 bytes"));
        v.add(error("multiple zero-size chunks", "0\r\n0\r\n\r\n", "Malformed trailer (no colon)"));
        v.add(new Vector("data exceeds decoded content length",
                         "5\r\nhello\r\n0\r\n\r\n", 3, null, null, null,
                         "Chunk of 5 bytes exceeds decoded content length: expected 3 bytes, already decoded 0 bytes"));

        // Chunk size line
        v.add(error("leading plus in chunk size", "+5\r\nhello\r\n0\r\n\r\n", "Invalid character in chunk size"));
        v.add(error("empty chunk size", "\r\nhello\r\n0\r\n\r\n", "Empty chunk size"));
        v.add(error("chunk size overflows long", "ffffffffffffffffff\r\n", "Invalid hex chunk size"));
        v.add(error("CR CR LF", "5\r\r\nhello\r\n0\r\n\r\n", "CR not followed by LF"));
        v.add(error("LF without CR", "5\nhello\r\n0\r\n\r\n", "LF without preceding CR"));
        v.add(error("chunk size line one byte over max length",
                    "5;" + repeat('a', AwsChunkedResponseParser.MAX_LINE_LENGTH - 1), "exceeds maximum length"));

        // Trailers
        v.add(error("empty trailer name", "0\r\n:abc==\r\n\r\n", "Malformed trailer (empty name)"));
        v.add(error("duplicate trailer",
                    "0\r\nx-amz-response-checksum-crc32:abc==\r\nX-Amz-Response-Checksum-CRC32:def==\r\n\r\n",
                    "Duplicate trailer: x-amz-response-checksum-crc32"));
        v.add(new Vector("undeclared trailer",
                         "5\r\nhello\r\n0\r\nother-trailer:abc==\r\n\r\n", 5, declared(CRC32_TRAILER), null, null,
                         "Trailer not declared in x-amz-trailer: other-trailer"));
        v.add(error("trailer section over max length", manyTrailers(), "Trailer section exceeds maximum length"));

        // Truncation
        v.add(error("empty input", "", "Unexpected end of stream while in state CHUNK_SIZE"));
        v.add(error("truncated mid chunk data", "5\r\nhel", "Unexpected end of stream while in state CHUNK_DATA"));
        return v;
    }

    /**
     * Each success vector combined with each split size.
     */
    public static Stream<Arguments> successCases() {
        return successVectors().stream().flatMap(v -> SPLIT_SIZES.stream().map(s -> Arguments.of(v, s)));
    }

    /**
     * Each error vector combined with each split size.
     */
    public static Stream<Arguments> errorCases() {
        return errorVectors().stream().flatMap(v -> SPLIT_SIZES.stream().map(s -> Arguments.of(v, s)));
    }

    /**
     * Splits {@code bytes} into pieces of at most {@code size} bytes.
     */
    public static List<byte[]> split(byte[] bytes, int size) {
        List<byte[]> pieces = new ArrayList<>();
        for (int i = 0; i < bytes.length; i += size) {
            pieces.add(Arrays.copyOfRange(bytes, i, (int) Math.min((long) i + size, bytes.length)));
        }
        return pieces;
    }

    /**
     * Encodes {@code data} with random 1-16 KiB chunks, followed by the trailer line.
     */
    public static byte[] encodeAwsChunked(byte[] data, Random random, String trailerLine) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int pos = 0;
        while (pos < data.length) {
            int len = Math.min(1 + random.nextInt(16 * 1024), data.length - pos);
            writeAscii(out, Integer.toHexString(len) + "\r\n");
            out.write(data, pos, len);
            writeAscii(out, "\r\n");
            pos += len;
        }
        writeAscii(out, "0\r\n" + trailerLine + "\r\n\r\n");
        return out.toByteArray();
    }

    private static Vector success(String name, String encoded, String expectedBody, Map<String, String> expectedTrailers) {
        return new Vector(name, encoded, expectedBody.length(), null, expectedBody, expectedTrailers, null);
    }

    private static Vector error(String name, String encoded, String expectedError) {
        return new Vector(name, encoded, -1, null, null, null, expectedError);
    }

    private static Map<String, String> trailers(String... keyValues) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    private static Set<String> declared(String... names) {
        return new HashSet<>(Arrays.asList(names));
    }

    /**
     * Terminal chunk followed by many short, distinct trailers whose total exceeds the trailer section limit.
     */
    private static String manyTrailers() {
        StringBuilder sb = new StringBuilder("0\r\n");
        for (int i = 0; sb.length() <= AwsChunkedResponseParser.MAX_LINE_LENGTH + 3; i++) {
            sb.append('t').append(i).append(":v\r\n");
        }
        return sb.append("\r\n").toString();
    }

    private static String repeat(char c, int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, c);
        return new String(chars);
    }

    private static void writeAscii(ByteArrayOutputStream out, String s) {
        byte[] bytes = s.getBytes(StandardCharsets.US_ASCII);
        out.write(bytes, 0, bytes.length);
    }
}
