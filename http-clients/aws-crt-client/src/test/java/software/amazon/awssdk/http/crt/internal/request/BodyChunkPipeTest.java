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

package software.amazon.awssdk.http.crt.internal.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class BodyChunkPipeTest {

    @Test
    void pollDrain_emptyOpenPipe_returnsZero() {
        BodyChunkPipe pipe = new BodyChunkPipe(2, 8);
        ByteBuffer dst = ByteBuffer.allocate(8);

        int n = pipe.pollDrain(dst);

        assertThat(n).isZero();
        assertThat(dst.position()).isZero();
    }

    @Test
    void pollDrain_afterEofWithEmptyQueue_returnsMinusOne() {
        BodyChunkPipe pipe = new BodyChunkPipe(2, 8);
        pipe.signalEof();

        int n = pipe.pollDrain(ByteBuffer.allocate(8));

        assertThat(n).isEqualTo(-1);
    }

    @Test
    void publish_thenDrain_consumerSeesProducerBytes() throws Exception {
        BodyChunkPipe pipe = new BodyChunkPipe(2, 8);
        Chunk c = pipe.acquireForFill();
        byte[] payload = {1, 2, 3, 4, 5};
        System.arraycopy(payload, 0, c.data(), 0, payload.length);
        c.pos(0);
        c.len(payload.length);
        pipe.publish(c);
        pipe.signalEof();
        ByteBuffer dst = ByteBuffer.allocate(16);

        int first = pipe.pollDrain(dst);
        int second = pipe.pollDrain(dst);

        assertThat(first).isEqualTo(payload.length);
        assertThat(second).isEqualTo(-1);
        dst.flip();
        byte[] out = new byte[dst.remaining()];
        dst.get(out);
        assertThat(out).containsExactly(payload);
    }

    @Test
    void signalError_pollDrainThrows() {
        BodyChunkPipe pipe = new BodyChunkPipe(2, 8);
        pipe.signalError(new RuntimeException("boom"));

        assertThatThrownBy(() -> pipe.pollDrain(ByteBuffer.allocate(8)))
            .hasMessageContaining("Producer failed")
            .hasRootCauseMessage("boom");
    }

    @Test
    void abort_emptiesReadyAndChangesState() throws Exception {
        BodyChunkPipe pipe = new BodyChunkPipe(2, 8);
        Chunk c = pipe.acquireForFill();
        c.len(4);
        pipe.publish(c);

        pipe.abort();

        assertThatThrownBy(() -> pipe.pollDrain(ByteBuffer.allocate(8)))
            .hasMessageContaining("aborted");
    }

    @Test
    void pollDrain_signalErrorWithQueuedChunks_drainsThenThrows() throws Exception {
        BodyChunkPipe pipe = new BodyChunkPipe(2, 8);
        Chunk c = pipe.acquireForFill();
        byte[] payload = {7, 8, 9};
        System.arraycopy(payload, 0, c.data(), 0, payload.length);
        c.len(payload.length);
        pipe.publish(c);
        pipe.signalError(new RuntimeException("boom"));

        ByteBuffer dst = ByteBuffer.allocate(payload.length);
        int drained = pipe.pollDrain(dst);

        assertThat(drained).isEqualTo(payload.length);
        dst.flip();
        byte[] out = new byte[dst.remaining()];
        dst.get(out);
        assertThat(out).containsExactly(payload);
        assertThatThrownBy(() -> pipe.pollDrain(ByteBuffer.allocate(8)))
            .hasMessageContaining("Producer failed")
            .hasRootCauseMessage("boom");
    }

    @Test
    void pollDrain_signalEofWithQueuedChunks_drainsThenReturnsMinusOne() throws Exception {
        BodyChunkPipe pipe = new BodyChunkPipe(2, 8);
        Chunk c = pipe.acquireForFill();
        byte[] payload = {10, 20, 30};
        System.arraycopy(payload, 0, c.data(), 0, payload.length);
        c.len(payload.length);
        pipe.publish(c);
        pipe.signalEof();

        ByteBuffer dst = ByteBuffer.allocate(payload.length);
        int drained = pipe.pollDrain(dst);
        int afterDrain = pipe.pollDrain(ByteBuffer.allocate(8));

        assertThat(drained).isEqualTo(payload.length);
        dst.flip();
        byte[] out = new byte[dst.remaining()];
        dst.get(out);
        assertThat(out).containsExactly(payload);
        assertThat(afterDrain).isEqualTo(-1);
    }

    @Test
    void abort_afterSignalEof_leavesStateAsEof() {
        BodyChunkPipe pipe = new BodyChunkPipe(2, 8);
        pipe.signalEof();

        pipe.abort();

        assertThat(pipe.state()).isEqualTo(BodyChunkPipe.State.EOF);
        assertThat(pipe.pollDrain(ByteBuffer.allocate(8))).isEqualTo(-1);
    }

    @Test
    void abort_afterSignalEofWithQueuedChunks_doesNotClearReady() throws Exception {
        BodyChunkPipe pipe = new BodyChunkPipe(2, 8);
        Chunk c = pipe.acquireForFill();
        byte[] payload = {1, 2, 3};
        System.arraycopy(payload, 0, c.data(), 0, payload.length);
        c.len(payload.length);
        pipe.publish(c);
        pipe.signalEof();

        pipe.abort();

        assertThat(pipe.state()).isEqualTo(BodyChunkPipe.State.EOF);
        ByteBuffer dst = ByteBuffer.allocate(payload.length);
        int drained = pipe.pollDrain(dst);
        assertThat(drained).isEqualTo(payload.length);
        assertThat(pipe.pollDrain(ByteBuffer.allocate(8))).isEqualTo(-1);
    }

    @Test
    void recycle_intoEofPipe_doesNotThrowAndDoesNotCorruptPool() throws Exception {
        BodyChunkPipe pipe = new BodyChunkPipe(2, 8);
        Chunk c = pipe.acquireForFill();
        c.len(4);
        pipe.publish(c);
        pipe.signalEof();

        ByteBuffer dst = ByteBuffer.allocate(8);
        int drained = pipe.pollDrain(dst);
        int afterDrain = pipe.pollDrain(ByteBuffer.allocate(8));

        assertThat(drained).isEqualTo(4);
        assertThat(afterDrain).isEqualTo(-1);
        assertThat(pipe.allocatedForTest()).isEqualTo(1);
    }

    @Test
    void recycle_intoAbortedPipe_doesNotThrow() throws Exception {
        BodyChunkPipe pipe = new BodyChunkPipe(2, 8);
        Chunk c = pipe.acquireForFill();
        pipe.abort();

        c.len(0);
        pipe.publish(c);

        assertThat(pipe.state()).isEqualTo(BodyChunkPipe.State.ABORTED);
    }

    @Test
    void recycle_intoErrorPipe_doesNotThrow() throws Exception {
        BodyChunkPipe pipe = new BodyChunkPipe(2, 8);
        Chunk c = pipe.acquireForFill();
        pipe.signalError(new RuntimeException("boom"));

        c.len(0);
        pipe.publish(c);

        assertThat(pipe.state()).isEqualTo(BodyChunkPipe.State.ERROR);
    }

    @Test
    void constructor_doesNotAllocateChunks() {
        BodyChunkPipe pipe = new BodyChunkPipe(4, 16);

        assertThat(pipe.allocatedForTest()).isZero();
    }

    @Test
    void acquireForFill_firstCall_allocatesOneChunk() throws Exception {
        BodyChunkPipe pipe = new BodyChunkPipe(4, 16);

        Chunk c = pipe.acquireForFill();

        assertThat(c).isNotNull();
        assertThat(c.data()).hasSize(16);
        assertThat(pipe.allocatedForTest()).isEqualTo(1);
    }

    @Test
    void acquireForFill_uniqueChunksUpToDepth_thenStopsAllocating() throws Exception {
        BodyChunkPipe pipe = new BodyChunkPipe(3, 8);
        Chunk c1 = pipe.acquireForFill();
        Chunk c2 = pipe.acquireForFill();
        Chunk c3 = pipe.acquireForFill();

        c1.len(1);
        pipe.publish(c1);
        pipe.pollDrain(ByteBuffer.allocate(8));
        Chunk reused = pipe.acquireForFill();

        assertThat(c1).isNotSameAs(c2).isNotSameAs(c3);
        assertThat(c2).isNotSameAs(c3);
        assertThat(pipe.allocatedForTest()).isEqualTo(3);
        assertThat(reused).isSameAs(c1);
    }

    @Test
    void acquireForFill_recycledChunkReused_noNewAllocation() throws Exception {
        BodyChunkPipe pipe = new BodyChunkPipe(2, 8);
        Chunk c = pipe.acquireForFill();
        c.len(3);
        pipe.publish(c);
        pipe.pollDrain(ByteBuffer.allocate(8));

        Chunk reused = pipe.acquireForFill();

        assertThat(reused).isSameAs(c);
        assertThat(pipe.allocatedForTest()).isEqualTo(1);
    }

    @Test
    void acquireForFill_afterAbort_returnsNull() throws Exception {
        BodyChunkPipe pipe = new BodyChunkPipe(2, 8);
        pipe.abort();

        Chunk c = pipe.acquireForFill();

        assertThat(c).isNull();
    }

    @Test
    void acquireForFill_afterSignalError_returnsNull() throws Exception {
        BodyChunkPipe pipe = new BodyChunkPipe(2, 8);
        pipe.signalError(new RuntimeException("boom"));

        Chunk c = pipe.acquireForFill();

        assertThat(c).isNull();
    }

    @Test
    void constructor_invalidDepth_throws() {
        assertThatThrownBy(() -> new BodyChunkPipe(0, 8))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("depth");
    }

    @Test
    void constructor_invalidChunkSize_throws() {
        assertThatThrownBy(() -> new BodyChunkPipe(2, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("chunkSize");
    }

    /**
     * Multi-threaded ordering test: producer races to call {@link BodyChunkPipe#signalError(Throwable)}
     * while a consumer is concurrently spinning on {@link BodyChunkPipe#pollDrain(java.nio.ByteBuffer)}.
     * The contract is that whenever the consumer observes the ERROR state, the cause must already
     * be visible (no {@code RuntimeException("Producer failed", null)}). RepeatedTest amplifies the
     * race window. With the cause published before the CAS, this should pass on every iteration.
     */
    @RepeatedTest(50)
    void signalError_concurrentPollDrain_consumerNeverSeesNullCause() throws Exception {
        BodyChunkPipe pipe = new BodyChunkPipe(2, 16);
        IllegalStateException expected = new IllegalStateException("boom");
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> consumerError = new AtomicReference<>();
        AtomicReference<Throwable> nullCauseSighting = new AtomicReference<>();

        Thread consumer = new Thread(() -> {
            try {
                start.await();
                ByteBuffer dst = ByteBuffer.allocate(16);
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                while (System.nanoTime() < deadline) {
                    try {
                        int n = pipe.pollDrain(dst);
                        if (n < 0) {
                            return;
                        }
                        dst.clear();
                    } catch (RuntimeException re) {
                        if (re.getCause() == null) {
                            nullCauseSighting.set(re);
                        }
                        return;
                    }
                }
            } catch (Throwable t) {
                consumerError.set(t);
            }
        }, "pipe-consumer");

        Thread producer = new Thread(() -> {
            try {
                start.await();
                pipe.signalError(expected);
            } catch (Throwable t) {
                consumerError.set(t);
            }
        }, "pipe-producer");

        consumer.start();
        producer.start();
        start.countDown();
        producer.join(5_000);
        consumer.join(5_000);

        assertThat(consumer.isAlive()).isFalse();
        assertThat(producer.isAlive()).isFalse();
        assertThat(consumerError.get()).isNull();
        assertThat(nullCauseSighting.get()).isNull();
    }

    /**
     * Multi-threaded test for the recycle/notify path: producer is forced to block on
     * {@link BodyChunkPipe#acquireForFill()} because all chunks are in flight, then the consumer
     * drains a chunk which {@code recycle()}s and notifies the producer to wake. This exercises the
     * full {@code freeLock.notifyAll()} hand-off rather than relying on the defensive 50ms wakeup.
     */
    @Test
    void acquireForFill_blocksUntilConsumerRecycles_thenWakesAndCompletes() throws Exception {
        BodyChunkPipe pipe = new BodyChunkPipe(1, 8);
        Chunk first = pipe.acquireForFill();
        first.len(4);
        pipe.publish(first);

        CountDownLatch producerEntered = new CountDownLatch(1);
        AtomicReference<Chunk> reused = new AtomicReference<>();
        AtomicReference<Throwable> producerError = new AtomicReference<>();
        Thread producer = new Thread(() -> {
            try {
                producerEntered.countDown();
                Chunk c = pipe.acquireForFill();
                reused.set(c);
            } catch (Throwable t) {
                producerError.set(t);
            }
        }, "pipe-producer");

        producer.start();
        producerEntered.await();
        // Drain so the chunk is recycled and the producer is woken via notifyAll.
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (reused.get() == null && System.nanoTime() < deadline) {
            pipe.pollDrain(ByteBuffer.allocate(8));
            producer.join(50);
        }

        assertThat(producerError.get()).isNull();
        assertThat(reused.get()).isSameAs(first);
        assertThat(pipe.allocatedForTest()).isEqualTo(1);
    }
}
