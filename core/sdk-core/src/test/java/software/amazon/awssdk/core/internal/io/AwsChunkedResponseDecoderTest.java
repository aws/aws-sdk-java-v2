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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.internal.io.AwsChunkedTestVectors.Vector;
import software.amazon.awssdk.http.Abortable;
import software.amazon.awssdk.http.AbortableInputStream;

/**
 * Tests for {@link AwsChunkedResponseDecoder}: shared vectors plus InputStream-specific behavior.
 */
class AwsChunkedResponseDecoderTest {

    // Shared vectors

    @ParameterizedTest(name = "{0}, split={1}")
    @MethodSource("software.amazon.awssdk.core.internal.io.AwsChunkedTestVectors#successCases")
    void read_successVector_bulkRead_decodesBodyAndTrailers(Vector vector, int splitSize) throws IOException {
        AwsChunkedResponseDecoder decoder = decoder(vector, splitSize);

        assertThat(readAllBulk(decoder)).isEqualTo(vector.expectedBody);
        assertThat(decoder.trailers()).containsExactlyEntriesOf(vector.expectedTrailers);
    }

    @ParameterizedTest(name = "{0}, split={1}")
    @MethodSource("software.amazon.awssdk.core.internal.io.AwsChunkedTestVectors#successCases")
    void read_successVector_singleByteRead_decodesBodyAndTrailers(Vector vector, int splitSize) throws IOException {
        AwsChunkedResponseDecoder decoder = decoder(vector, splitSize);

        assertThat(readAllSingleByte(decoder)).isEqualTo(vector.expectedBody);
        assertThat(decoder.trailers()).containsExactlyEntriesOf(vector.expectedTrailers);
    }

    @ParameterizedTest(name = "{0}, split={1}")
    @MethodSource("software.amazon.awssdk.core.internal.io.AwsChunkedTestVectors#errorCases")
    void read_errorVector_bulkRead_throws(Vector vector, int splitSize) {
        AwsChunkedResponseDecoder decoder = decoder(vector, splitSize);

        assertThatThrownBy(() -> readAllBulk(decoder))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining(vector.expectedError);
    }

    @ParameterizedTest(name = "{0}, split={1}")
    @MethodSource("software.amazon.awssdk.core.internal.io.AwsChunkedTestVectors#errorCases")
    void read_errorVector_singleByteRead_throws(Vector vector, int splitSize) {
        AwsChunkedResponseDecoder decoder = decoder(vector, splitSize);

        assertThatThrownBy(() -> readAllSingleByte(decoder))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining(vector.expectedError);
    }

    // InputStream-specific behavior

    @Test
    void read_whenLargePayloadWithRandomShortReads_decodesExactBytes() throws IOException {
        Random random = new Random(42);
        byte[] data = new byte[256 * 1024];
        random.nextBytes(data);
        byte[] encoded = AwsChunkedTestVectors.encodeAwsChunked(data, random, "x-amz-response-checksum-crc32:abc==");

        InputStream underlying = new RandomShortReadInputStream(new ByteArrayInputStream(encoded), random);
        AwsChunkedResponseDecoder decoder = new AwsChunkedResponseDecoder(underlying, data.length);

        assertThat(readAllBulk(decoder)).isEqualTo(data);
        assertThat(decoder.trailers()).containsEntry("x-amz-response-checksum-crc32", "abc==");
    }

    @Test
    void read_whenCalledAgainAfterError_rethrowsSameError() {
        AwsChunkedResponseDecoder decoder = decoder("5\r\r\nhello\r\n0\r\n\r\n");

        SdkClientException first = (SdkClientException) catchThrowable(() -> decoder.read());
        assertThat(first).hasMessageContaining("CR not followed by LF");

        assertThatThrownBy(decoder::read).isSameAs(first);
        assertThatThrownBy(() -> decoder.read(new byte[10], 0, 10)).isSameAs(first);
    }

    @Test
    void read_whenCalledAfterEof_returnsMinusOne() throws IOException {
        AwsChunkedResponseDecoder decoder = decoder("5\r\nhello\r\n0\r\n\r\n");
        readAllBulk(decoder);

        assertThat(decoder.read()).isEqualTo(-1);
        assertThat(decoder.read(new byte[10], 0, 10)).isEqualTo(-1);
    }

    @Test
    void read_whenLengthIsZero_returnsZeroWithoutReading() throws IOException {
        AwsChunkedResponseDecoder decoder = decoder("5\r\nhello\r\n0\r\n\r\n");

        assertThat(decoder.read(new byte[10], 0, 0)).isZero();
        assertThat(readAllBulk(decoder)).isEqualTo("hello".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void read_whenSingleByteAndBulkReadsMixed_decodesCorrectly() throws IOException {
        AwsChunkedResponseDecoder decoder = decoder("5\r\nhello\r\n6\r\n world\r\n0\r\n\r\n");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[3];

        out.write(decoder.read());
        int n;
        while ((n = decoder.read(buf, 0, buf.length)) != -1) {
            out.write(buf, 0, n);
            int b = decoder.read();
            if (b == -1) {
                break;
            }
            out.write(b);
        }

        assertThat(new String(out.toByteArray(), StandardCharsets.UTF_8)).isEqualTo("hello world");
    }

    @Test
    void read_whenBufferIsNull_throwsNullPointerException() {
        AwsChunkedResponseDecoder decoder = decoder("0\r\n\r\n");

        assertThatThrownBy(() -> decoder.read(null, 0, 1)).isInstanceOf(NullPointerException.class)
                                                          .hasMessageContaining("buf");
    }

    @ParameterizedTest(name = "buffer={0}, off={1}, len={2}")
    @CsvSource({"4, 2, 3", "4, -1, 1", "4, 0, -1", "4, 5, 0"})
    void read_whenOffsetOrLengthOutOfBounds_throwsIndexOutOfBounds(int bufferLength, int off, int len) {
        AwsChunkedResponseDecoder decoder = decoder("0\r\n\r\n");

        assertThatThrownBy(() -> decoder.read(new byte[bufferLength], off, len))
            .isInstanceOf(IndexOutOfBoundsException.class)
            .hasMessage("off=" + off + ", len=" + len + ", buffer length=" + bufferLength);
    }

    @Test
    void read_whenMalformedLineIsLong_truncatesItInErrorMessage() {
        StringBuilder longSize = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longSize.append('Z');
        }
        AwsChunkedResponseDecoder decoder = decoder(longSize + "\r\n");

        assertThatThrownBy(() -> decoder.read())
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Invalid character in chunk size")
            .hasMessageEndingWith("...")
            .satisfies(e -> assertThat(e.getMessage()).hasSizeLessThan(200));
    }

    @Test
    void trailers_whenCalledBeforeEof_throwsIllegalState() throws IOException {
        AwsChunkedResponseDecoder decoder = decoder("5\r\nhello\r\n0\r\ntrailer-key:abc==\r\n\r\n");

        assertThatThrownBy(decoder::trailers).isInstanceOf(IllegalStateException.class)
                                              .hasMessageContaining("only available after");

        // Data fully read but EOF not yet observed
        decoder.read(new byte[5], 0, 5);
        assertThatThrownBy(decoder::trailers).isInstanceOf(IllegalStateException.class)
                                              .hasMessageContaining("only available after");
    }

    @Test
    void trailers_whenDecodingFailed_throwsIllegalState() {
        AwsChunkedResponseDecoder decoder = decoder("3\r\nfoo\r\n0\r\ntrailer-key:abc==\r\n\r\nextra");

        assertThatThrownBy(() -> readAllBulk(decoder)).isInstanceOf(SdkClientException.class);
        assertThatThrownBy(decoder::trailers).isInstanceOf(IllegalStateException.class)
                                              .hasMessageContaining("only available after");
    }

    @Test
    void close_whenCalled_closesUnderlyingStream() throws IOException {
        InputStream underlying = spy(new ByteArrayInputStream(
            "5\r\nhello\r\n0\r\n\r\n".getBytes(StandardCharsets.UTF_8)));
        AwsChunkedResponseDecoder decoder = new AwsChunkedResponseDecoder(underlying, -1);

        decoder.close();

        verify(underlying).close();
    }

    @Test
    void abort_whenUnderlyingStreamIsAbortable_delegatesAbort() {
        Abortable abortable = mock(Abortable.class);
        InputStream underlying = AbortableInputStream.create(
            new ByteArrayInputStream("5\r\nhello\r\n0\r\n\r\n".getBytes(StandardCharsets.UTF_8)), abortable);
        AwsChunkedResponseDecoder decoder = new AwsChunkedResponseDecoder(underlying, -1);

        decoder.abort();

        verify(abortable).abort();
    }

    // Helpers

    private static Throwable catchThrowable(ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (Throwable t) {
            return t;
        }
        throw new AssertionError("Expected an exception");
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static AwsChunkedResponseDecoder decoder(String encoded) {
        return new AwsChunkedResponseDecoder(new ByteArrayInputStream(encoded.getBytes(StandardCharsets.UTF_8)), -1);
    }

    private static AwsChunkedResponseDecoder decoder(Vector vector, int splitSize) {
        InputStream underlying = new MaxReadSizeInputStream(new ByteArrayInputStream(vector.encoded), splitSize);
        return new AwsChunkedResponseDecoder(underlying, vector.expectedDecodedLength, vector.declaredTrailers);
    }

    private static byte[] readAllBulk(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int n;
        while ((n = in.read(buf, 0, buf.length)) != -1) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    private static byte[] readAllSingleByte(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            out.write(b);
        }
        return out.toByteArray();
    }

    /**
     * Returns at most {@code maxReadSize} bytes per read, simulating short network reads.
     */
    private static final class MaxReadSizeInputStream extends InputStream {
        private final InputStream delegate;
        private final int maxReadSize;

        private MaxReadSizeInputStream(InputStream delegate, int maxReadSize) {
            this.delegate = delegate;
            this.maxReadSize = maxReadSize;
        }

        @Override
        public int read() throws IOException {
            return delegate.read();
        }

        @Override
        public int read(byte[] buf, int off, int len) throws IOException {
            return delegate.read(buf, off, Math.min(maxReadSize, len));
        }
    }

    /**
     * Returns 1 to 8 KiB per read.
     */
    private static final class RandomShortReadInputStream extends InputStream {
        private final InputStream delegate;
        private final Random random;

        private RandomShortReadInputStream(InputStream delegate, Random random) {
            this.delegate = delegate;
            this.random = random;
        }

        @Override
        public int read() throws IOException {
            return delegate.read();
        }

        @Override
        public int read(byte[] buf, int off, int len) throws IOException {
            return delegate.read(buf, off, Math.min(len, 1 + random.nextInt(8192)));
        }
    }
}
