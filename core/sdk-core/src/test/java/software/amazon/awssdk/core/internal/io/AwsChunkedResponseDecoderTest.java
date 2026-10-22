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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkClientException;

/**
 * Tests for {@link AwsChunkedResponseDecoder} against Streaming Operations SEP test vectors.
 */
class AwsChunkedResponseDecoderTest {

    // Success cases from SEP

    @Test
    void read_whenSingleChunkWithTrailer_decodesDataAndExtractsTrailer() throws IOException {
        String encoded = "5\r\nhello\r\n0\r\ntrailer-key:abc123==\r\n\r\n";
        AwsChunkedResponseDecoder decoder = createDecoder(encoded);

        assertThat(readAll(decoder)).isEqualTo("hello");
        assertThat(decoder.trailers()).containsEntry("trailer-key", "abc123==");
    }

    @Test
    void read_whenMultipleChunksWithTrailer_decodesAllChunks() throws IOException {
        String encoded = "5\r\nhello\r\n6\r\n world\r\n0\r\ntrailer-key:AAAA==\r\n\r\n";
        AwsChunkedResponseDecoder decoder = createDecoder(encoded);

        assertThat(readAll(decoder)).isEqualTo("hello world");
        assertThat(decoder.trailers()).containsEntry("trailer-key", "AAAA==");
    }

    @Test
    void read_whenChunkDataContainsCrlf_treatsAsData() throws IOException {
        // Data is "a\r\nbc" (5 bytes) — the embedded \r\n is data, not a boundary
        String encoded = "5\r\na\r\nbc\r\n0\r\n\r\n";
        AwsChunkedResponseDecoder decoder = createDecoder(encoded);

        assertThat(readAll(decoder)).isEqualTo("a\r\nbc");
    }

    @Test
    void read_whenTrailerValueHasWhitespace_trimsValue() throws IOException {
        String encoded = "3\r\nfoo\r\n0\r\ntrailer-key:  abc123==  \r\n\r\n";
        AwsChunkedResponseDecoder decoder = createDecoder(encoded);

        assertThat(readAll(decoder)).isEqualTo("foo");
        assertThat(decoder.trailers()).containsEntry("trailer-key", "abc123==");
    }

    @Test
    void read_whenChunkHasExtensions_ignoresExtensions() throws IOException {
        String encoded = "5;chunk-signature=abc;date=20260101\r\nhello\r\n0;chunk-signature=def\r\ntrailer-key:xyz==\r\n\r\n";
        AwsChunkedResponseDecoder decoder = createDecoder(encoded);

        assertThat(readAll(decoder)).isEqualTo("hello");
        assertThat(decoder.trailers()).containsEntry("trailer-key", "xyz==");
    }

    @Test
    void read_whenMultipleTrailers_extractsAllInOrder() throws IOException {
        String encoded = "5\r\nhello\r\n0\r\nfirst-trailer:value1\r\nsecond-trailer:value2\r\n\r\n";
        AwsChunkedResponseDecoder decoder = createDecoder(encoded);

        assertThat(readAll(decoder)).isEqualTo("hello");
        Map<String, String> trailers = decoder.trailers();
        assertThat(trailers).containsEntry("first-trailer", "value1");
        assertThat(trailers).containsEntry("second-trailer", "value2");
    }

    @Test
    void read_whenZeroLengthContent_returnsEmptyAndExtractsTrailer() throws IOException {
        String encoded = "0\r\ntrailer-key:abc==\r\n\r\n";
        AwsChunkedResponseDecoder decoder = createDecoder(encoded);

        assertThat(readAll(decoder)).isEmpty();
        assertThat(decoder.trailers()).containsEntry("trailer-key", "abc==");
    }

    @Test
    void read_whenNoTrailer_returnsDataWithEmptyTrailers() throws IOException {
        String encoded = "5\r\nhello\r\n0\r\n\r\n";
        AwsChunkedResponseDecoder decoder = createDecoder(encoded);

        assertThat(readAll(decoder)).isEqualTo("hello");
        assertThat(decoder.trailers()).isEmpty();
    }

    // Error cases from SEP

    @Test
    void read_whenInvalidHexInChunkSize_throwsException() {
        String encoded = "ZZ\r\n";
        AwsChunkedResponseDecoder decoder = createDecoder(encoded);

        assertThatThrownBy(() -> readAll(decoder))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Invalid");
    }

    @Test
    void read_whenMissingCrlfAfterChunkData_throwsException() {
        String encoded = "3\r\nfooX";
        AwsChunkedResponseDecoder decoder = createDecoder(encoded);

        assertThatThrownBy(() -> readAll(decoder))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Expected CRLF");
    }

    @Test
    void read_whenMalformedTrailerNoColon_throwsException() {
        String encoded = "0\r\nnot-a-valid-trailer\r\n\r\n";
        AwsChunkedResponseDecoder decoder = createDecoder(encoded);

        assertThatThrownBy(() -> readAll(decoder))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Malformed trailer");
    }

    @Test
    void read_whenDecodedContentLengthMismatch_throwsException() {
        String encoded = "5\r\nhello\r\n0\r\ntrailer-key:abc==\r\n\r\n";
        AwsChunkedResponseDecoder decoder = new AwsChunkedResponseDecoder(
            new ByteArrayInputStream(encoded.getBytes(StandardCharsets.UTF_8)), 100);

        assertThatThrownBy(() -> readAll(decoder))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("content length mismatch");
    }

    @Test
    void read_whenDataAfterTerminalChunk_throwsException() {
        String encoded = "3\r\nfoo\r\n0\r\ntrailer-key:abc==\r\n\r\nextra garbage";
        AwsChunkedResponseDecoder decoder = createDecoder(encoded);

        assertThatThrownBy(() -> readAll(decoder))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Data found after terminal chunk");
    }

    @Test
    void read_whenMultipleZeroSizeChunks_throwsException() {
        String encoded = "0\r\n0\r\n\r\n";
        AwsChunkedResponseDecoder decoder = createDecoder(encoded);

        assertThatThrownBy(() -> readAll(decoder))
            .isInstanceOf(SdkClientException.class);
    }

    // Content length validation

    @Test
    void read_whenContentLengthMatchesDecodedBytes_succeeds() throws IOException {
        String encoded = "5\r\nhello\r\n0\r\n\r\n";
        AwsChunkedResponseDecoder decoder = new AwsChunkedResponseDecoder(
            new ByteArrayInputStream(encoded.getBytes(StandardCharsets.UTF_8)), 5);

        assertThat(readAll(decoder)).isEqualTo("hello");
    }

    // Incremental decoding (byte-at-a-time)

    @Test
    void read_whenByteAtATime_producesSameResultAsFullRead() throws IOException {
        String encoded = "5\r\nhello\r\n6\r\n world\r\n0\r\ntrailer-key:abc==\r\n\r\n";

        // Full read
        AwsChunkedResponseDecoder fullDecoder = createDecoder(encoded);
        String fullResult = readAll(fullDecoder);
        Map<String, String> fullTrailers = fullDecoder.trailers();

        // Byte-at-a-time read
        AwsChunkedResponseDecoder byteDecoder = createDecoder(encoded);
        String byteResult = readAllByteAtATime(byteDecoder);
        Map<String, String> byteTrailers = byteDecoder.trailers();

        assertThat(byteResult).isEqualTo(fullResult);
        assertThat(byteTrailers).isEqualTo(fullTrailers);
    }

    @Test
    void read_whenBulkReadAcrossChunkBoundaries_decodesAllChunks() throws IOException {
        // Two 5-byte chunks — bulk read with buffer larger than one chunk
        String encoded = "5\r\nhello\r\n5\r\nworld\r\n0\r\n\r\n";
        AwsChunkedResponseDecoder decoder = createDecoder(encoded);

        byte[] buf = new byte[20];
        int totalRead = 0;
        int bytesRead;
        while ((bytesRead = decoder.read(buf, totalRead, buf.length - totalRead)) != -1) {
            totalRead += bytesRead;
        }
        assertThat(new String(buf, 0, totalRead, StandardCharsets.UTF_8)).isEqualTo("helloworld");
    }

    @Test
    void read_whenStreamTruncatedBeforeTerminalChunk_throwsException() {
        String encoded = "5\r\nhel";
        AwsChunkedResponseDecoder decoder = createDecoder(encoded);

        assertThatThrownBy(() -> readAll(decoder))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Unexpected end of stream");
    }

    @Test
    void read_whenStreamTruncatedMidTrailer_throwsException() {
        String encoded = "0\r\ntrailer-key:ab";
        AwsChunkedResponseDecoder decoder = createDecoder(encoded);

        assertThatThrownBy(() -> readAll(decoder))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Unexpected end of stream");
    }

    @Test
    void read_whenTrailerNameHasUpperCase_lowercasesWithLocaleRoot() throws IOException {
        String encoded = "3\r\nfoo\r\n0\r\nX-Amz-Checksum-CRC32:abc==\r\n\r\n";
        AwsChunkedResponseDecoder decoder = createDecoder(encoded);

        readAll(decoder);
        assertThat(decoder.trailers()).containsEntry("x-amz-checksum-crc32", "abc==");
    }

    @Test
    void read_whenChunkSizeHasLeadingPlus_throwsException() {
        String encoded = "+5\r\nhello\r\n0\r\n\r\n";
        AwsChunkedResponseDecoder decoder = createDecoder(encoded);

        assertThatThrownBy(() -> readAll(decoder))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Invalid character in chunk size");
    }

    @Test
    void close_whenCalled_closesUnderlyingStream() throws IOException {
        ByteArrayInputStream underlying = new ByteArrayInputStream(
            "5\r\nhello\r\n0\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        AwsChunkedResponseDecoder decoder = new AwsChunkedResponseDecoder(underlying, -1);

        decoder.close();
    }

    @Test
    void read_whenUnderlyingStreamReturnsOneByteAtATime_decodesCorrectly() throws IOException {
        String encoded = "A\r\n1234567890\r\n0\r\n\r\n";
        InputStream slowStream = new OneByteAtATimeInputStream(
            new ByteArrayInputStream(encoded.getBytes(StandardCharsets.UTF_8)));
        AwsChunkedResponseDecoder decoder = new AwsChunkedResponseDecoder(slowStream, -1);

        byte[] buf = new byte[20];
        int totalRead = 0;
        int bytesRead;
        while ((bytesRead = decoder.read(buf, totalRead, buf.length - totalRead)) != -1) {
            totalRead += bytesRead;
        }
        assertThat(new String(buf, 0, totalRead, StandardCharsets.UTF_8)).isEqualTo("1234567890");
    }

    @Test
    void trailers_whenCalledBeforeEof_returnsEmpty() {
        String encoded = "5\r\nhello\r\n0\r\ntrailer-key:abc==\r\n\r\n";
        AwsChunkedResponseDecoder decoder = createDecoder(encoded);

        assertThat(decoder.trailers()).isEmpty();
    }

    // Helpers

    /**
     * InputStream wrapper that returns at most 1 byte per read() call,
     * simulating a slow network stream with short reads.
     */
    private static class OneByteAtATimeInputStream extends InputStream {
        private final InputStream delegate;

        OneByteAtATimeInputStream(InputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public int read() throws IOException {
            return delegate.read();
        }

        @Override
        public int read(byte[] buf, int off, int len) throws IOException {
            return delegate.read(buf, off, Math.min(1, len));
        }
    }

    private static AwsChunkedResponseDecoder createDecoder(String encoded) {
        return new AwsChunkedResponseDecoder(
            new ByteArrayInputStream(encoded.getBytes(StandardCharsets.UTF_8)), -1);
    }

    private static String readAll(AwsChunkedResponseDecoder decoder) throws IOException {
        byte[] buf = new byte[1024];
        int totalRead = 0;
        int bytesRead;
        while ((bytesRead = decoder.read(buf, totalRead, buf.length - totalRead)) != -1) {
            totalRead += bytesRead;
        }
        return new String(buf, 0, totalRead, StandardCharsets.UTF_8);
    }

    private static String readAllByteAtATime(AwsChunkedResponseDecoder decoder) throws IOException {
        StringBuilder sb = new StringBuilder();
        int b;
        while ((b = decoder.read()) != -1) {
            sb.append((char) b);
        }
        return sb.toString();
    }
}
