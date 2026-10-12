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

package software.amazon.awssdk.utils.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.utils.async.ByteBufferStoringSubscriber.TransferResult.END_OF_STREAM;
import static software.amazon.awssdk.utils.async.ByteBufferStoringSubscriber.TransferResult.SUCCESS;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;

public class InputStreamConsumingPublisherTest {
    private static final ExecutorService EXECUTOR =
        Executors.newCachedThreadPool(new ThreadFactoryBuilder().daemonThreads(true).build());
    private ByteBufferStoringSubscriber subscriber;
    private InputStreamConsumingPublisher publisher;

    @BeforeEach
    public void setup() {
        this.subscriber = new ByteBufferStoringSubscriber(Long.MAX_VALUE);
        this.publisher = new InputStreamConsumingPublisher();
    }

    @Test
    public void subscribeAfterWrite_completes() throws InterruptedException {
        EXECUTOR.submit(() -> publisher.doBlockingWrite(streamOfLength(0)));
        Thread.sleep(200);
        publisher.subscribe(subscriber);

        assertThat(subscriber.transferTo(ByteBuffer.allocate(0))).isEqualTo(END_OF_STREAM);
    }

    @Test
    public void zeroKb_completes() {
        publisher.subscribe(subscriber);

        assertThat(publisher.doBlockingWrite(streamOfLength(0))).isEqualTo(0);
        assertThat(subscriber.transferTo(ByteBuffer.allocate(0))).isEqualTo(END_OF_STREAM);
    }

    @Test
    public void oneKb_writesAndCompletes() {
        publisher.subscribe(subscriber);

        assertThat(publisher.doBlockingWrite(streamOfLength(1024))).isEqualTo(1024);
        assertThat(subscriber.transferTo(ByteBuffer.allocate(1023))).isEqualTo(SUCCESS);
        assertThat(subscriber.transferTo(ByteBuffer.allocate(1))).isEqualTo(END_OF_STREAM);
    }

    @Test
    public void bytesAreDeliveredInOrder() {
        publisher.subscribe(subscriber);

        assertThat(publisher.doBlockingWrite(streamWithAllBytesInOrder())).isEqualTo(256);

        ByteBuffer output = ByteBuffer.allocate(256);
        assertThat(subscriber.transferTo(output)).isEqualTo(END_OF_STREAM);
        output.flip();

        for (int i = 0; i < 256; i++) {
            assertThat(output.get()).isEqualTo((byte) i);
        }
    }

    @Test
    public void failedRead_signalsOnError() {
        publisher.subscribe(subscriber);

        assertThatThrownBy(() -> publisher.doBlockingWrite(streamWithFailedReadAfterLength(1024)))
            .isInstanceOf(UncheckedIOException.class);
    }

    @Test
    public void cancel_signalsOnError() {
        publisher.subscribe(subscriber);
        publisher.cancel();

        assertThatThrownBy(() -> subscriber.transferTo(ByteBuffer.allocate(0))).isInstanceOf(CancellationException.class);
    }

    @Test
    public void cancel_stopsRunningWrites() {
        publisher.subscribe(subscriber);
        Future<?> write = EXECUTOR.submit(() -> publisher.doBlockingWrite(streamOfLength(Integer.MAX_VALUE)));
        publisher.cancel();

        assertThatThrownBy(write::get).hasRootCauseInstanceOf(CancellationException.class);
    }

    @Test
    public void cancel_beforeWrite_stopsWrite() {
        publisher.subscribe(subscriber);
        publisher.cancel();
        assertThatThrownBy(() -> publisher.doBlockingWrite(streamOfLength(Integer.MAX_VALUE)))
            .hasRootCauseInstanceOf(CancellationException.class);
    }

    @Test
    public void cancel_beforeSubscribe_stopsWrite() {
        publisher.cancel();
        publisher.subscribe(subscriber);
        assertThatThrownBy(() -> publisher.doBlockingWrite(streamOfLength(Integer.MAX_VALUE)))
            .hasRootCauseInstanceOf(CancellationException.class);
    }

    /**
     * Verifies the publisher delivers the same total bytes regardless of how many bytes per call the underlying
     * {@link InputStream#read(byte[])} returns. This protects the data-correctness contract across the changes
     * that adjust per-read allocation sizing based on {@link InputStream#available()}.
     */
    @ParameterizedTest
    @ValueSource(ints = {1, 100, 1024, 4096, 8192, 16384, 32768})
    public void doBlockingWrite_whenStreamReturnsVariousChunkSizes_deliversAllBytesInOrder(int chunkSize) {
        int totalBytes = 64 * 1024; // 64 KB total
        publisher.subscribe(subscriber);

        long written = publisher.doBlockingWrite(fixedChunkSizeStream(totalBytes, chunkSize));

        assertThat(written).isEqualTo(totalBytes);
        ByteBuffer output = ByteBuffer.allocate(totalBytes);
        assertThat(subscriber.transferTo(output)).isEqualTo(END_OF_STREAM);
        output.flip();
        for (int i = 0; i < totalBytes; i++) {
            assertThat(output.get()).as("byte at index %d", i).isEqualTo((byte) (i & 0xFF));
        }
    }

    /**
     * Verifies the publisher delivers correct data and tolerates a stream whose {@link InputStream#available()}
     * implementation throws {@link IOException}. The publisher must fall back to a default allocation size and
     * continue delivering data.
     */
    @Test
    public void doBlockingWrite_whenAvailableThrows_deliversAllBytes() {
        int totalBytes = 4 * 1024;
        publisher.subscribe(subscriber);

        long written = publisher.doBlockingWrite(streamWithThrowingAvailable(totalBytes));

        assertThat(written).isEqualTo(totalBytes);
        assertThat(subscriber.transferTo(ByteBuffer.allocate(totalBytes - 1))).isEqualTo(SUCCESS);
        assertThat(subscriber.transferTo(ByteBuffer.allocate(1))).isEqualTo(END_OF_STREAM);
    }

    /**
     * Verifies the publisher delivers correct data and tolerates a stream whose {@link InputStream#available()}
     * implementation reports a negative value (a misbehaving but theoretically possible implementation).
     */
    @Test
    public void doBlockingWrite_whenAvailableReturnsNegative_deliversAllBytes() {
        int totalBytes = 4 * 1024;
        publisher.subscribe(subscriber);

        long written = publisher.doBlockingWrite(streamWithFixedAvailable(totalBytes, -1));

        assertThat(written).isEqualTo(totalBytes);
        assertThat(subscriber.transferTo(ByteBuffer.allocate(totalBytes - 1))).isEqualTo(SUCCESS);
        assertThat(subscriber.transferTo(ByteBuffer.allocate(1))).isEqualTo(END_OF_STREAM);
    }

    /**
     * Verifies that when a stream returns small chunks (mimicking a {@link java.io.PipedInputStream} with a small
     * pipe buffer), the {@link ByteBuffer}s emitted downstream do not retain oversized backing arrays. Without the
     * trim/right-size logic, every emitted buffer would have the publisher's full 16 KB internal buffer size as
     * its backing array length even though the actual data is much smaller. This pinned memory in downstream
     * pipelines (retry buffers, async write queues) and caused unbounded heap growth for streams that report
     * small {@code read()} return values.
     */
    @Test
    public void doBlockingWrite_whenStreamReturnsSmallChunks_emittedBuffersAreRightSized() throws Exception {
        int chunkSize = 1024; // mimics PipedInputStream default 1KB buffer
        int totalBytes = 32 * 1024; // 32 KB → many small chunks

        CapturingSubscriber capturing = new CapturingSubscriber();
        publisher.subscribe(capturing);
        publisher.doBlockingWrite(fixedChunkSizeStream(totalBytes, chunkSize));
        capturing.awaitComplete(5, TimeUnit.SECONDS);

        assertThat(capturing.totalBytes()).isEqualTo(totalBytes);
        // Every emitted buffer's backing array should be sized close to the actual data length, not the
        // publisher's internal 16KB allocation. We allow a small slack to account for the upfront sizing
        // hint and the trim threshold.
        int internalBufferSize = 16 * 1024;
        for (ByteBuffer buffer : capturing.received()) {
            assertThat(buffer.array().length)
                .as("ByteBuffer backing array should not retain the publisher's full 16KB allocation when "
                    + "underlying reads return %dB. Got backing array of size %d for a buffer with %d bytes "
                    + "of data.", chunkSize, buffer.array().length, buffer.remaining())
                .isLessThan(internalBufferSize);
        }
    }

    /**
     * Verifies that when the underlying stream returns large chunks (close to or equal to the publisher's
     * internal buffer size), the publisher does not pay an unnecessary copy cost — the emitted {@link ByteBuffer}
     * wraps the originally allocated array directly.
     */
    @Test
    public void doBlockingWrite_whenStreamReturnsLargeChunks_doesNotTrimBackingArray() throws Exception {
        int chunkSize = 16 * 1024; // matches publisher internal BUFFER_SIZE
        int totalBytes = 64 * 1024; // 4 chunks

        CapturingSubscriber capturing = new CapturingSubscriber();
        publisher.subscribe(capturing);
        publisher.doBlockingWrite(new ByteArrayInputStream(sequentialBytes(totalBytes)));
        capturing.awaitComplete(5, TimeUnit.SECONDS);

        assertThat(capturing.totalBytes()).isEqualTo(totalBytes);
        // For full reads, the publisher should not pay the trim cost — backing arrays should match the
        // emitted data length.
        for (ByteBuffer buffer : capturing.received()) {
            assertThat(buffer.array().length).isEqualTo(buffer.remaining());
        }
    }

    private static byte[] sequentialBytes(int length) {
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) (i & 0xFF);
        }
        return bytes;
    }

    /**
     * Returns an InputStream that produces {@code totalBytes} of sequential data, but only returns at most
     * {@code chunkSize} bytes per {@link InputStream#read(byte[])} call. {@link InputStream#available()} returns
     * the number of bytes available up to {@code chunkSize}, mimicking streams like
     * {@link java.io.PipedInputStream} with a small internal buffer.
     */
    private InputStream fixedChunkSizeStream(int totalBytes, int chunkSize) {
        return new InputStream() {
            int position = 0;

            @Override
            public int read() {
                if (position >= totalBytes) {
                    return -1;
                }
                return (position++) & 0xFF;
            }

            @Override
            public int read(byte[] b, int off, int len) {
                if (position >= totalBytes) {
                    return -1;
                }
                int toRead = Math.min(Math.min(len, chunkSize), totalBytes - position);
                for (int i = 0; i < toRead; i++) {
                    b[off + i] = (byte) ((position + i) & 0xFF);
                }
                position += toRead;
                return toRead;
            }

            @Override
            public int available() {
                return Math.min(chunkSize, totalBytes - position);
            }
        };
    }

    private InputStream streamWithThrowingAvailable(int totalBytes) {
        return new InputStream() {
            int position = 0;

            @Override
            public int read() {
                return position >= totalBytes ? -1 : (position++) & 0xFF;
            }

            @Override
            public int available() throws IOException {
                throw new IOException("available() not supported");
            }
        };
    }

    private InputStream streamWithFixedAvailable(int totalBytes, int availableValue) {
        return new InputStream() {
            int position = 0;

            @Override
            public int read() {
                return position >= totalBytes ? -1 : (position++) & 0xFF;
            }

            @Override
            public int available() {
                return availableValue;
            }
        };
    }

    /**
     * Test {@link Subscriber} that captures every emitted {@link ByteBuffer} so that tests can inspect the
     * backing array length, not just the visible data.
     */
    private static final class CapturingSubscriber implements Subscriber<ByteBuffer> {
        private final List<ByteBuffer> received = new ArrayList<>();
        private final CountDownLatch done = new CountDownLatch(1);

        @Override
        public void onSubscribe(Subscription s) {
            s.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            received.add(byteBuffer);
        }

        @Override
        public void onError(Throwable t) {
            done.countDown();
        }

        @Override
        public void onComplete() {
            done.countDown();
        }

        void awaitComplete(long timeout, TimeUnit unit) throws InterruptedException {
            assertThat(done.await(timeout, unit)).as("subscriber did not complete in time").isTrue();
        }

        List<ByteBuffer> received() {
            return received;
        }

        long totalBytes() {
            return received.stream().mapToLong(ByteBuffer::remaining).sum();
        }
    }

    public InputStream streamOfLength(int length) {
        return new InputStream() {
            int i = 0;
            @Override
            public int read() throws IOException {
                if (i >= length) {
                    return -1;
                }
                ++i;
                return 1;
            }
        };
    }

    public InputStream streamWithAllBytesInOrder() {
        return new InputStream() {
            int i = 0;
            @Override
            public int read() throws IOException {
                if (i > 255) {
                    return -1;
                }
                return i++;
            }
        };
    }

    public InputStream streamWithFailedReadAfterLength(int length) {
        return new InputStream() {
            int i = 0;
            @Override
            public int read() throws IOException {
                if (i > length) {
                    throw new IOException("Failed to read!");
                }
                ++i;
                return 1;
            }
        };
    }
}