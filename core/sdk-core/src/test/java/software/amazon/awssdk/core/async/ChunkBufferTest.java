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

package software.amazon.awssdk.core.async;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.core.internal.async.ChunkBuffer;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.StringUtils;

class ChunkBufferTest {

    @ParameterizedTest
    @ValueSource(ints = {1, 6, 10, 23, 25})
    void numberOfChunk_Not_MultipleOfTotalBytes_KnownLength(int totalBytes) {
        int bufferSize = 5;

        String inputString = RandomStringUtils.randomAscii(totalBytes);
        ChunkBuffer chunkBuffer = ChunkBuffer.builder()
                                             .bufferSize(bufferSize)
                                             .totalBytes(inputString.getBytes(StandardCharsets.UTF_8).length)
                                             .build();
        Iterable<ByteBuffer> byteBuffers =
            chunkBuffer.split(ByteBuffer.wrap(inputString.getBytes(StandardCharsets.UTF_8)));

        AtomicInteger index = new AtomicInteger(0);
        int count = (int) Math.ceil(totalBytes / (double) bufferSize);
        int remainder = totalBytes % bufferSize;

        byteBuffers.forEach(r -> {
            int i = index.get();

            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(inputString.getBytes(StandardCharsets.UTF_8))) {
                byte[] expected;
                if (i == count - 1 && remainder != 0) {
                    expected = new byte[remainder];
                } else {
                    expected = new byte[bufferSize];
                }
                inputStream.skip(i * bufferSize);
                inputStream.read(expected);
                byte[] actualBytes = BinaryUtils.copyBytesFrom(r);
                assertThat(actualBytes).isEqualTo(expected);
                index.incrementAndGet();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 6, 10, 23, 25})
    void numberOfChunk_Not_MultipleOfTotalBytes_UnknownLength(int totalBytes) {
        int bufferSize = 5;

        String inputString = RandomStringUtils.randomAscii(totalBytes);
        ChunkBuffer chunkBuffer = ChunkBuffer.builder()
                                             .bufferSize(bufferSize)
                                             .build();
        Iterable<ByteBuffer> byteBuffers =
            chunkBuffer.split(ByteBuffer.wrap(inputString.getBytes(StandardCharsets.UTF_8)));

        AtomicInteger index = new AtomicInteger(0);
        int count = (int) Math.ceil(totalBytes / (double) bufferSize);
        int remainder = totalBytes % bufferSize;

        byteBuffers.forEach(r -> {
            int i = index.get();

            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(inputString.getBytes(StandardCharsets.UTF_8))) {
                byte[] expected;
                if (i == count - 1 && remainder != 0) {
                    expected = new byte[remainder];
                } else {
                    expected = new byte[bufferSize];
                }
                inputStream.skip(i * bufferSize);
                inputStream.read(expected);
                byte[] actualBytes = BinaryUtils.copyBytesFrom(r);
                assertThat(actualBytes).isEqualTo(expected);
                index.incrementAndGet();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void zeroTotalBytesAsInput_returnsZeroByte_KnownLength() {
        byte[] zeroByte = new byte[0];
        ChunkBuffer chunkBuffer = ChunkBuffer.builder()
                                             .bufferSize(5)
                                             .totalBytes(zeroByte.length)
                                             .build();
        Iterable<ByteBuffer> byteBuffers =
            chunkBuffer.split(ByteBuffer.wrap(zeroByte));

        AtomicInteger iteratedCounts = new AtomicInteger();
        byteBuffers.forEach(r -> {
            iteratedCounts.getAndIncrement();
        });
        assertThat(iteratedCounts.get()).isEqualTo(1);
    }

    @Test
    void zeroTotalBytesAsInput_returnsZeroByte_UnknownLength() {
        byte[] zeroByte = new byte[0];
        ChunkBuffer chunkBuffer = ChunkBuffer.builder()
                                             .bufferSize(5)
                                             .build();
        Iterable<ByteBuffer> byteBuffers =
            chunkBuffer.split(ByteBuffer.wrap(zeroByte));

        AtomicInteger iteratedCounts = new AtomicInteger();
        byteBuffers.forEach(r -> {
            iteratedCounts.getAndIncrement();
        });
        assertThat(iteratedCounts.get()).isEqualTo(1);
    }

    @Test
    void emptyAllocatedBytes_returnSameNumberOfEmptyBytes_knownLength() {
        int totalBytes = 17;
        int bufferSize = 5;
        ByteBuffer wrap = ByteBuffer.allocate(totalBytes);
        ChunkBuffer chunkBuffer = ChunkBuffer.builder()
                                             .bufferSize(bufferSize)
                                             .totalBytes(wrap.remaining())
                                             .build();
        Iterable<ByteBuffer> byteBuffers =
            chunkBuffer.split(wrap);

        AtomicInteger iteratedCounts = new AtomicInteger();
        byteBuffers.forEach(r -> {
            iteratedCounts.getAndIncrement();
            if (iteratedCounts.get() * bufferSize < totalBytes) {
                // array of empty bytes
                assertThat(BinaryUtils.copyBytesFrom(r)).isEqualTo(ByteBuffer.allocate(bufferSize).array());
            } else {
                assertThat(BinaryUtils.copyBytesFrom(r)).isEqualTo(ByteBuffer.allocate(totalBytes % bufferSize).array());
            }
        });
        assertThat(iteratedCounts.get()).isEqualTo(4);
    }

    @Test
    void emptyAllocatedBytes_returnSameNumberOfEmptyBytes_unknownLength() {
        int totalBytes = 17;
        int bufferSize = 5;
        ByteBuffer wrap = ByteBuffer.allocate(totalBytes);
        ChunkBuffer chunkBuffer = ChunkBuffer.builder()
                                             .bufferSize(bufferSize)
                                             .build();
        Iterable<ByteBuffer> byteBuffers =
            chunkBuffer.split(wrap);

        AtomicInteger iteratedCounts = new AtomicInteger();
        byteBuffers.forEach(r -> {
            iteratedCounts.getAndIncrement();
            if (iteratedCounts.get() * bufferSize < totalBytes) {
                // array of empty bytes
                assertThat(BinaryUtils.copyBytesFrom(r)).isEqualTo(ByteBuffer.allocate(bufferSize).array());
            } else {
                assertThat(BinaryUtils.copyBytesFrom(r)).isEqualTo(ByteBuffer.allocate(totalBytes % bufferSize).array());
            }
        });
        assertThat(iteratedCounts.get()).isEqualTo(3);

        Optional<ByteBuffer> lastBuffer = chunkBuffer.getBufferedData();
        assertThat(lastBuffer).isPresent();
        assertThat(lastBuffer.get().remaining()).isEqualTo(2);
    }


    /**
     * * Total bytes 11(ChunkSize) 3 (threads)
     * * Buffering Size of 5
     * threadOne 22222222222
     * threadTwo 33333333333
     * threadThree 11111111111

     *
     * * Streaming sequence as below
     * *
     * start 22222222222
     * 22222
     * 22222
     * end 22222222222
     * *
     * start streaming 33333333333
     * 2 is from previous sequence which is buffered
     * 23333
     * 33333
     * end 33333333333
     * *
     * start 11111111111
     * 33 is from previous sequence which is buffered *
     * 33111
     * 11111
     * 111
     * end 11111111111
     * 111 is given as output since we consumed all the total bytes*
     */
    @Test
    void concurrentTreads_calling_bufferAndCreateChunks_knownLength() throws ExecutionException, InterruptedException {
        int totalBytes = 17;
        int bufferSize = 5;
        int threads = 8;

        ByteBuffer wrap = ByteBuffer.allocate(totalBytes);
        ChunkBuffer chunkBuffer = ChunkBuffer.builder()
                                             .bufferSize(bufferSize)
                                             .totalBytes(wrap.remaining() * threads)
                                             .build();

        ExecutorService service = Executors.newFixedThreadPool(threads);

        Collection<Future<Iterable>> futures;

        AtomicInteger counter = new AtomicInteger(0);

        futures = IntStream.range(0, threads).<Future<Iterable>>mapToObj(t -> service.submit(() -> {
            String inputString = StringUtils.repeat(Integer.toString(counter.incrementAndGet()), totalBytes);
            return chunkBuffer.split(ByteBuffer.wrap(inputString.getBytes(StandardCharsets.UTF_8)));
        })).collect(Collectors.toCollection(() -> new ArrayList<>(threads)));

        AtomicInteger filledBuffers = new AtomicInteger(0);
        AtomicInteger remainderBytesBuffers = new AtomicInteger(0);
        AtomicInteger otherSizeBuffers = new AtomicInteger(0);
        AtomicInteger remainderBytes = new AtomicInteger(0);

        for (Future<Iterable> bufferedFuture : futures) {
            Iterable<ByteBuffer> buffers = bufferedFuture.get();
            buffers.forEach(b -> {
                if (b.remaining() == bufferSize) {
                    filledBuffers.incrementAndGet();
                } else if (b.remaining() == ((totalBytes * threads) % bufferSize)) {
                    remainderBytesBuffers.incrementAndGet();
                    remainderBytes.set(b.remaining());

                } else {
                    otherSizeBuffers.incrementAndGet();
                }

            });
        }

        assertThat(filledBuffers.get()).isEqualTo((totalBytes * threads) / bufferSize);
        assertThat(remainderBytes.get()).isEqualTo((totalBytes * threads) % bufferSize);
        assertThat(remainderBytesBuffers.get()).isOne();
        assertThat(otherSizeBuffers.get()).isZero();
    }
}
