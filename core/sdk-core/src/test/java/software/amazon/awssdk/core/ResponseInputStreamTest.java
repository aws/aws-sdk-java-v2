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

package software.amazon.awssdk.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.http.Abortable;
import software.amazon.awssdk.http.AbortableInputStream;

class ResponseInputStreamTest {

    InputStream stream;
    Abortable abortable;
    AbortableInputStream abortableInputStream;

    @BeforeEach
    public void setUp() throws Exception {
        stream = Mockito.mock(InputStream.class);
        abortable = Mockito.mock(Abortable.class);
        abortableInputStream = AbortableInputStream.create(stream, abortable);
    }

    @Test
    void abort_withAbortable_closesUnderlyingStream() throws IOException {
        ResponseInputStream<Object> responseInputStream = new ResponseInputStream<>(new Object(), abortableInputStream);

        responseInputStream.abort();

        verify(abortable).abort();
        verify(stream).close();
        assertThat(responseInputStream.hasTimeoutTask()).isTrue();
        assertThat(responseInputStream.timeoutTaskDoneOrCancelled()).isTrue();
    }

    @Test
    void failedClose_withinAbort_isIgnored() throws IOException {
        ResponseInputStream<Object> responseInputStream = new ResponseInputStream<>(new Object(), abortableInputStream);

        Mockito.doThrow(new IOException()).when(stream).close();
        assertThatCode(responseInputStream::abort).doesNotThrowAnyException();

        verify(abortable).abort();
        verify(stream).close();
        assertThat(responseInputStream.hasTimeoutTask()).isTrue();
        assertThat(responseInputStream.timeoutTaskDoneOrCancelled()).isTrue();
    }

    @Test
    void abort_withoutAbortable_closesUnderlyingStream() throws IOException {
        ResponseInputStream<Object> responseInputStream = new ResponseInputStream<>(new Object(), stream);

        responseInputStream.abort();

        verify(stream).close();
        assertThat(responseInputStream.hasTimeoutTask()).isTrue();
        assertThat(responseInputStream.timeoutTaskDoneOrCancelled()).isTrue();
    }

    @Test
    void close_withAbortable_closesUnderlyingStream() throws IOException {
        ResponseInputStream<Object> responseInputStream = new ResponseInputStream<>(new Object(), abortableInputStream);

        responseInputStream.close();

        verify(abortable, never()).abort();
        verify(stream).close();
        assertThat(responseInputStream.hasTimeoutTask()).isTrue();
        assertThat(responseInputStream.timeoutTaskDoneOrCancelled()).isFalse();
    }

    @Test
    void customTimeout_noRead_abortsAfterTimeout() throws Exception {
        ResponseInputStream<Object> responseInputStream = responseInputStream(Duration.ofSeconds(1));
        Thread.sleep(2000);

        verify(abortable).abort();
        verify(stream).close();
        assertThat(responseInputStream.hasTimeoutTask()).isTrue();
        assertThat(responseInputStream.timeoutTaskDoneOrCancelled()).isTrue();
    }

    @Test
    void customTimouet_readBeforeTimeout_cancelsTimeout() throws Exception {
        ResponseInputStream<Object> responseInputStream = responseInputStream(Duration.ofSeconds(1));
        responseInputStream.read();
        Thread.sleep(2000);

        verify(abortable, never()).abort();
        verify(stream).read();
        assertThat(responseInputStream.hasTimeoutTask()).isTrue();
        assertThat(responseInputStream.timeoutTaskDoneOrCancelled()).isTrue();
    }

    @Test
    void zeroTimeout_disablesTimeout() throws Exception {
        ResponseInputStream<Object> responseInputStream = responseInputStream(Duration.ZERO);
        Thread.sleep(2000);

        verify(abortable, never()).abort();
        verify(stream, never()).close();
        assertThat(responseInputStream.hasTimeoutTask()).isFalse();
    }

    @Test
    void negativeTimeout_disablesTimeout() throws Exception {
        ResponseInputStream<Object> responseInputStream = responseInputStream(Duration.ofSeconds(-1));
        Thread.sleep(2000);

        verify(abortable, never()).abort();
        verify(stream, never()).close();
        assertThat(responseInputStream.hasTimeoutTask()).isFalse();
    }

    @Test
    void gzipConcatenatedMembers_whenAvailableTransientlyZero_decodesAllMembers() throws IOException {
        InputStream underlying = new TrickleStream(concatenatedGzip("PART_ONE;", "PART_TWO;"), false);
        ResponseInputStream<Object> ris = new ResponseInputStream<>(new Object(), underlying, Duration.ZERO);

        assertThat(readAllGzip(ris)).isEqualTo("PART_ONE;PART_TWO;");
    }

    @Test
    void gzipManyMembers_whenAvailableTransientlyZero_decodesAllMembers() throws IOException {
        InputStream underlying = new TrickleStream(concatenatedGzip("A;", "B;", "C;", "D;", "E;"), false);
        ResponseInputStream<Object> ris = new ResponseInputStream<>(new Object(), underlying, Duration.ZERO);

        assertThat(readAllGzip(ris)).isEqualTo("A;B;C;D;E;");
    }

    @Test
    void gzipSingleMember_whenAvailableZero_decodesWithoutHanging() {
        // Preemptive timeout guards against the final-boundary probe hanging.
        String decoded = assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            InputStream underlying = new TrickleStream(concatenatedGzip("ONLY_ONE_MEMBER;"), false);
            ResponseInputStream<Object> ris = new ResponseInputStream<>(new Object(), underlying, Duration.ZERO);
            return readAllGzip(ris);
        });

        assertThat(decoded).isEqualTo("ONLY_ONE_MEMBER;");
    }

    @Test
    void gzipConcatenatedMembers_whenNeverZeroAvailable_decodesAllMembers() throws IOException {
        InputStream underlying = new TrickleStream(concatenatedGzip("PART_ONE;", "PART_TWO;"), true);
        ResponseInputStream<Object> ris = new ResponseInputStream<>(new Object(), underlying, Duration.ZERO);

        assertThat(readAllGzip(ris)).isEqualTo("PART_ONE;PART_TWO;");
    }

    @Test
    void bufferedReader_whenNonGzip_deliversLineWithoutBlocking() {
        ControllableStream underlying = new ControllableStream();
        underlying.feed("event: E1\n");
        ResponseInputStream<Object> ris = new ResponseInputStream<>(new Object(), underlying, Duration.ZERO);
        BufferedReader reader = new BufferedReader(new InputStreamReader(ris, StandardCharsets.UTF_8));

        assertTimeoutPreemptively(Duration.ofSeconds(2), () ->
            assertThat(reader.readLine()).isEqualTo("event: E1"));
    }

    @Test
    void abort_whenGzipWrapperInserted_propagatesToOriginalAbortable() throws IOException {
        AtomicBoolean aborted = new AtomicBoolean(false);
        InputStream body = new TrickleStream(concatenatedGzip("HELLO"), false);
        AbortableInputStream abortableBody = AbortableInputStream.create(body, () -> aborted.set(true));
        ResponseInputStream<Object> ris = new ResponseInputStream<>(new Object(), abortableBody, Duration.ZERO);

        ris.abort();

        assertThat(aborted).isTrue();
    }

    @Test
    void gzip_whenSourceBlocksThenSignalsEof_decodesWithoutHanging() {
        // A genuinely blocking source: after the coerced available()==1 triggers a final-boundary probe, the read must
        // still terminate once the source signals EOF rather than hang.
        String decoded = assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            InputStream underlying = new BlockingEofStream(concatenatedGzip("ONLY_ONE;"));
            ResponseInputStream<Object> ris = new ResponseInputStream<>(new Object(), underlying, Duration.ZERO);
            return readAllGzip(ris);
        });

        assertThat(decoded).isEqualTo("ONLY_ONE;");
    }

    @Test
    void markReset_throughWrapper_reReadsSameBytes() throws IOException {
        InputStream body = new ByteArrayInputStream("hello-world".getBytes(StandardCharsets.UTF_8));
        ResponseInputStream<Object> ris = new ResponseInputStream<>(new Object(), body, Duration.ZERO);

        assertThat(ris.markSupported()).isTrue();
        ris.mark(16);
        int first = ris.read();
        int second = ris.read();
        ris.reset();

        assertThat(ris.read()).isEqualTo(first);
        assertThat(ris.read()).isEqualTo(second);
    }

    private ResponseInputStream<Object> responseInputStream(Duration timeout) {
        return new ResponseInputStream<>(new Object(), abortableInputStream, timeout);
    }

    private static String readAllGzip(InputStream in) throws IOException {
        try (GZIPInputStream gz = new GZIPInputStream(in)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[64];
            int n;
            while ((n = gz.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static byte[] concatenatedGzip(String... members) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (String member : members) {
            ByteArrayOutputStream one = new ByteArrayOutputStream();
            try (GZIPOutputStream gz = new GZIPOutputStream(one)) {
                gz.write(member.getBytes(StandardCharsets.UTF_8));
            }
            out.write(one.toByteArray());
        }
        return out.toByteArray();
    }

    /** Serves bytes one at a time; reports available()==0 unless {@code neverZero} */
    private static final class TrickleStream extends InputStream {
        private final byte[] data;
        private final boolean neverZero;
        private int pos = 0;

        TrickleStream(byte[] data, boolean neverZero) {
            this.data = data;
            this.neverZero = neverZero;
        }

        @Override
        public int read() {
            return pos < data.length ? (data[pos++] & 0xff) : -1;
        }

        @Override
        public int read(byte[] b, int off, int len) {
            if (len == 0) {
                return 0;
            }
            if (pos >= data.length) {
                return -1;
            }
            b[off] = (byte) (data[pos++] & 0xff);
            return 1;
        }

        @Override
        public int available() {
            return neverZero ? 1 : 0;
        }
    }

    /** A blocking "live feed": read() waits for fed data or finish(); read(byte[]) returns only buffered bytes. */
    private static final class ControllableStream extends InputStream {
        private final LinkedBlockingQueue<Integer> queue = new LinkedBlockingQueue<>();
        private volatile boolean finished = false;

        void feed(String s) {
            for (byte b : s.getBytes(StandardCharsets.UTF_8)) {
                queue.add(b & 0xff);
            }
        }

        void finish() {
            finished = true;
        }

        @Override
        public int read() throws IOException {
            try {
                Integer b;
                while ((b = queue.poll(50, TimeUnit.MILLISECONDS)) == null) {
                    if (finished) {
                        return -1;
                    }
                }
                return b;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException(e);
            }
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (len == 0) {
                return 0;
            }
            int first = read();
            if (first < 0) {
                return -1;
            }
            b[off] = (byte) first;
            int n = 1;
            while (n < len) {
                Integer next = queue.poll();
                if (next == null) {
                    break;
                }
                b[off + n] = (byte) (int) next;
                n++;
            }
            return n;
        }

        @Override
        public int available() {
            return queue.size();
        }
    }

    /** Serves bytes one at a time (available()==0), then blocks briefly once before signalling EOF. */
    private static final class BlockingEofStream extends InputStream {
        private final byte[] data;
        private int pos = 0;
        private boolean blocked = false;

        BlockingEofStream(byte[] data) {
            this.data = data;
        }

        @Override
        public int read() throws IOException {
            if (pos < data.length) {
                return data[pos++] & 0xff;
            }
            if (!blocked) {
                blocked = true;
                try {
                    Thread.sleep(150);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException(e);
                }
            }
            return -1;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (len == 0) {
                return 0;
            }
            int first = read();
            if (first < 0) {
                return -1;
            }
            b[off] = (byte) first;
            return 1;
        }

        @Override
        public int available() {
            return 0;
        }
    }
}
