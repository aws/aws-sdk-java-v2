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
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Unit tests for {@link GzipAvailabilityInputStream}. */
class GzipAvailabilityInputStreamTest {

    @ParameterizedTest
    @MethodSource
    void available_afterHeaderRead_returnsExpected(byte[] payload, int expected) throws IOException {
        GzipAvailabilityInputStream stream = new GzipAvailabilityInputStream(new ZeroAvailableStream(payload));

        stream.read();
        stream.read();
        stream.read();

        assertThat(stream.available()).isEqualTo(expected);
    }

    static Stream<Arguments> available_afterHeaderRead_returnsExpected() throws IOException {
        return Stream.of(
            arguments(gzip("HELLO"), 1),
            arguments("event: E1\n".getBytes(StandardCharsets.UTF_8), 0),
            arguments(new byte[] {(byte) 0x1f, (byte) 0x8b, 0x09, 0, 0}, 0)); // wrong method (09), not gzip
    }

    @Test
    void available_whenGzipButDelegateNonZero_returnsDelegateValue() throws IOException {
        GzipAvailabilityInputStream stream =
            new GzipAvailabilityInputStream(new FixedAvailableStream(gzip("HELLO"), 5));

        stream.read();
        stream.read();
        stream.read();

        assertThat(stream.available()).isEqualTo(5);
    }

    @Test
    void available_whenGzipAtEof_returnsZero() throws IOException {
        GzipAvailabilityInputStream stream = new GzipAvailabilityInputStream(new ZeroAvailableStream(gzip("HI")));

        while (stream.read() != -1) {
        }

        assertThat(stream.available()).isEqualTo(0);
    }

    @Test
    void available_whenClosed_returnsZero() throws IOException {
        GzipAvailabilityInputStream stream =
            new GzipAvailabilityInputStream(new ZeroAvailableStream(gzip("HELLO")));

        stream.read();
        stream.read();
        stream.read();
        assertThat(stream.available()).isEqualTo(1);

        stream.close();

        assertThat(stream.available()).isEqualTo(0);
    }

    @Test
    void available_whenGzipDetectedViaBulkRead_returnsOne() throws IOException {
        GzipAvailabilityInputStream stream =
            new GzipAvailabilityInputStream(new BulkZeroStream(gzip("HELLO")));

        stream.read(new byte[8], 0, 8);

        assertThat(stream.available()).isEqualTo(1);
    }

    @Test
    void available_whenPartialHeaderThenEof_returnsZero() throws IOException {
        byte[] partial = {(byte) 0x1f, (byte) 0x8b};
        GzipAvailabilityInputStream stream =
            new GzipAvailabilityInputStream(new ZeroAvailableStream(partial));

        stream.read();
        stream.read();
        assertThat(stream.read()).isEqualTo(-1);
        assertThat(stream.available()).isEqualTo(0);
    }

    @Test
    void read_whenZeroLengthAfterEof_keepsAvailableZero() throws IOException {
        // A zero-length read after EOF returns 0 without moving the stream and must not clear EOF.
        GzipAvailabilityInputStream stream =
            new GzipAvailabilityInputStream(new ZeroAvailableStream(gzip("HELLO")));

        while (stream.read() != -1) {
        }
        assertThat(stream.available()).isEqualTo(0);

        int n = stream.read(new byte[4], 0, 0);

        assertThat(n).isEqualTo(0);
        assertThat(stream.available()).isEqualTo(0);
    }

    @Test
    void skip_whenBeforeClassification_abandonsGzipDetection() throws IOException {
        // Junk prefix then a real gzip header: without abandoning detection, skipping the 2 junk bytes would
        // expose 1f 8b 08 and be misdetected as gzip.
        byte[] gz = gzip("HELLO");
        byte[] data = new byte[gz.length + 2];
        System.arraycopy(gz, 0, data, 2, gz.length);
        GzipAvailabilityInputStream stream = new GzipAvailabilityInputStream(new ZeroAvailableStream(data));

        stream.skip(2);
        stream.read();
        stream.read();
        stream.read();

        assertThat(stream.available()).isEqualTo(0);
    }

    @Test
    void reset_whenMarkedMidHeader_restoresClassification() throws IOException {
        GzipAvailabilityInputStream stream =
            new GzipAvailabilityInputStream(new MarkableZeroStream(gzip("HELLO")));

        stream.mark(100);
        stream.read();
        stream.read();
        stream.reset();

        stream.read();
        stream.read();
        stream.read();

        assertThat(stream.available()).isEqualTo(1);
    }

    @Test
    void reset_whenAfterEofWithoutMark_reDetectsGzip() throws IOException {
        // reset() without a prior mark rewinds to position 0 (like ByteArrayInputStream, whose mark defaults to 0);
        // the wrapper must clear its stale EOF/classification so gzip is re-detected on re-read.
        GzipAvailabilityInputStream stream =
            new GzipAvailabilityInputStream(new MarkableZeroStream(gzip("HELLO")));

        while (stream.read() != -1) {
        }
        stream.reset();

        stream.read();
        stream.read();
        stream.read();

        assertThat(stream.available()).isEqualTo(1);
    }

    @Test
    void release_whenDelegateReleasable_propagates() {
        ReleasableZeroStream delegate = new ReleasableZeroStream();
        GzipAvailabilityInputStream stream = new GzipAvailabilityInputStream(delegate);

        stream.release();

        assertThat(delegate.released).isTrue();
    }

    private static byte[] gzip(String s) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream g = new GZIPOutputStream(bos)) {
            g.write(s.getBytes(StandardCharsets.UTF_8));
        }
        return bos.toByteArray();
    }

    /** Serves bytes but always reports available()==0. */
    private static final class ZeroAvailableStream extends InputStream {
        private final byte[] data;
        private int pos;

        ZeroAvailableStream(byte[] data) {
            this.data = data;
        }

        @Override
        public int read() {
            return pos < data.length ? (data[pos++] & 0xff) : -1;
        }

        @Override
        public int available() {
            return 0;
        }
    }

    /** {@link ZeroAvailableStream} that also supports mark/reset (mark defaults to 0). */
    private static final class MarkableZeroStream extends InputStream {
        private final byte[] data;
        private int pos;
        private int markPos;

        MarkableZeroStream(byte[] data) {
            this.data = data;
        }

        @Override
        public int read() {
            return pos < data.length ? (data[pos++] & 0xff) : -1;
        }

        @Override
        public int available() {
            return 0;
        }

        @Override
        public boolean markSupported() {
            return true;
        }

        @Override
        public synchronized void mark(int readlimit) {
            markPos = pos;
        }

        @Override
        public synchronized void reset() {
            pos = markPos;
        }
    }

    /** Returns bytes in bulk (up to len per read) but reports available()==0. */
    private static final class BulkZeroStream extends InputStream {
        private final byte[] data;
        private int pos;

        BulkZeroStream(byte[] data) {
            this.data = data;
        }

        @Override
        public int read() {
            return pos < data.length ? (data[pos++] & 0xff) : -1;
        }

        @Override
        public int read(byte[] b, int off, int len) {
            if (pos >= data.length) {
                return -1;
            }
            int n = Math.min(len, data.length - pos);
            System.arraycopy(data, pos, b, off, n);
            pos += n;
            return n;
        }

        @Override
        public int available() {
            return 0;
        }
    }

    /** Records whether release() was called; its close() is a no-op. */
    private static final class ReleasableZeroStream extends InputStream implements Releasable {
        private boolean released;

        @Override
        public int read() {
            return -1;
        }

        @Override
        public int available() {
            return 0;
        }

        @Override
        public void release() {
            released = true;
        }
    }

    /** Serves bytes but reports a fixed available() value. */
    private static final class FixedAvailableStream extends InputStream {
        private final byte[] data;
        private final int avail;
        private int pos;

        FixedAvailableStream(byte[] data, int avail) {
            this.data = data;
            this.avail = avail;
        }

        @Override
        public int read() {
            return pos < data.length ? (data[pos++] & 0xff) : -1;
        }

        @Override
        public int available() {
            return avail;
        }
    }
}
