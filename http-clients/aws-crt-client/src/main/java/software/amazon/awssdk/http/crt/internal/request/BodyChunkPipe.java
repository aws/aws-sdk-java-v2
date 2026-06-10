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

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.utils.Logger;

/**
 * Bounded producer/consumer hand-off between the caller thread (producer) and the CRT event-loop thread (consumer).
 *
 * <p>The producer reads from the customer's {@code InputStream} and {@link #publish(Chunk) publishes} chunks
 * into a bounded {@link ArrayBlockingQueue}. The consumer drains those chunks via {@link #pollDrain(ByteBuffer)},
 * which is non-blocking: if no data is ready the consumer returns 0 bytes and CRT reschedules itself via
 * {@code aws_channel_schedule_task_now}.
 *
 * <p>Drained chunks are returned to a free {@link ArrayDeque} (LIFO for cache hotness) guarded by this
 * monitor. The producer parks on this monitor when the free deque is empty, providing back-pressure.
 *
 * <p>Chunk byte[] buffers are allocated lazily on the producer's first {@link #acquireForFill()}, not in
 * the constructor. This keeps per-request heap minimal while a request is queued on the CRT connection
 * pool waiting for a stream: the pipe object exists but its backing buffers do not.
 *
 * <p>State machine: {@code OPEN -> {EOF | ERROR | ABORTED}}. Transitions are one-way.
 */
@SdkInternalApi
final class BodyChunkPipe {
    private static final Logger LOG = Logger.loggerFor(BodyChunkPipe.class);

    enum State {
        OPEN,
        EOF,
        ERROR,
        ABORTED
    }

    /**
     * Defense-in-depth wait timeout for {@link #acquireForFill()}. Even if a code path forgets
     * to call {@link #abort()}, a parked producer wakes every {@value} ms to re-check state.
     * Spurious wakeups are harmless.
     */
    private static final long ACQUIRE_WAIT_TIMEOUT_MS = 50L;

    private final int depth;
    private final int chunkSize;
    private final ArrayBlockingQueue<Chunk> ready;
    private final Deque<Chunk> free;
    private final AtomicReference<State> state = new AtomicReference<>(State.OPEN);
    /**
     * Guards the free deque, allocated counter, and producer wait/notify protocol. Kept private
     * so external code cannot synchronize on the pipe instance and stall the producer.
     */
    private final Object freeLock = new Object();

    private int allocated;
    private volatile Throwable error;
    private Chunk pendingDrain;

    BodyChunkPipe(int depth, int chunkSize) {
        if (depth < 1) {
            throw new IllegalArgumentException("depth must be >= 1");
        }
        if (chunkSize < 1) {
            throw new IllegalArgumentException("chunkSize must be >= 1");
        }
        this.depth = depth;
        this.chunkSize = chunkSize;
        this.ready = new ArrayBlockingQueue<>(depth);
        this.free = new ArrayDeque<>(depth);
    }

    /**
     * Producer side: acquire a chunk to fill. Blocks if all chunks are currently in flight.
     * Returns {@code null} only if the pipe was aborted while the producer was waiting.
     *
     * <p>Allocates the chunk's backing byte[] on first use up to the configured depth. This keeps the
     * per-request footprint minimal until the producer actually starts pumping (i.e., until after the
     * CRT stream has been acquired).
     */
    Chunk acquireForFill() throws InterruptedException {
        synchronized (freeLock) {
            while (true) {
                State s = state.get();
                if (s == State.ABORTED || s == State.ERROR) {
                    LOG.debug(() -> "acquireForFill returning null, state=" + s);
                    return null;
                }
                Chunk c = free.pollFirst();
                if (c != null) {
                    return c;
                }
                if (allocated < depth) {
                    allocated++;
                    return new Chunk(chunkSize);
                }
                freeLock.wait(ACQUIRE_WAIT_TIMEOUT_MS);
            }
        }
    }

    /**
     * Producer side: publish a filled chunk to the consumer. Caller must have set
     * {@link Chunk#len(int)} before calling.
     *
     * <p>If the chunk is empty (zero-length read), it is recycled back to the free deque rather than
     * pushed to the ready queue: an empty chunk would otherwise be leaked from the bounded pool, and
     * the consumer would interpret it as a no-op anyway.
     */
    void publish(Chunk chunk) throws InterruptedException {
        if (chunk.len() == 0) {
            recycle(chunk);
            return;
        }
        // ready.put() blocks if the queue is full, but the queue capacity == pool size,
        // so this can only block briefly while the consumer drains.
        ready.put(chunk);
    }

    /**
     * Producer side: signal end-of-stream. Idempotent.
     */
    void signalEof() {
        if (state.compareAndSet(State.OPEN, State.EOF)) {
            LOG.debug(() -> "state OPEN -> EOF");
        }
    }

    /**
     * Producer side: signal a fatal producer-side error. Idempotent.
     */
    void signalError(Throwable t) {
        synchronized (freeLock) {
            // Publish the cause BEFORE flipping state so a consumer's lock-free read in pollDrain
            // never observes state==ERROR with error==null. The volatile write to `error` is
            // harmless if the CAS later loses (idempotent signal).
            error = t;
            if (state.compareAndSet(State.OPEN, State.ERROR)) {
                LOG.debug(() -> "state OPEN -> ERROR (" + t.getClass().getSimpleName() + ")");
            }
            freeLock.notifyAll();
        }
    }

    /**
     * External-cancel: clear ready queue, flip state, wake producer.
     */
    void abort() {
        synchronized (freeLock) {
            if (state.compareAndSet(State.OPEN, State.ABORTED)) {
                LOG.debug(() -> "state OPEN -> ABORTED");
                ready.clear();
            }
            freeLock.notifyAll();
        }
    }

    /**
     * Consumer side: drain bytes into {@code dst}. NEVER blocks.
     *
     * <p>Single-consumer: CRT invokes this only on the request's outgoing-stream task, which is
     * scheduled serially on one event-loop thread per stream. {@code pendingDrain} is therefore
     * not volatile - it is written and read by that single consumer thread.
     *
     * @return number of bytes drained, or {@code -1} on EOF with no remaining data.
     * @throws RuntimeException if the pipe is in ERROR or ABORTED state with no remaining data.
     */
    int pollDrain(ByteBuffer dst) {
        int totalBytesConsumed = 0;
        while (dst.hasRemaining()) {
            if (pendingDrain == null) {
                pendingDrain = ready.poll();
            }
            if (pendingDrain == null) {
                switch (state.get()) {
                    case ERROR:
                        throw new RuntimeException("Producer failed", error);
                    case ABORTED:
                        throw new RuntimeException("Request body stream was aborted");
                    case EOF:
                        return totalBytesConsumed > 0 ? totalBytesConsumed : -1;
                    case OPEN:
                    default:
                        // OPEN with empty queue: return what we have (possibly 0); CRT will retry.
                        return totalBytesConsumed;
                }
            }
            int n = Math.min(dst.remaining(), pendingDrain.len() - pendingDrain.pos());
            dst.put(pendingDrain.data(), pendingDrain.pos(), n);
            pendingDrain.pos(pendingDrain.pos() + n);
            totalBytesConsumed += n;
            if (pendingDrain.pos() >= pendingDrain.len()) {
                // The chunk has been fully copied into dst, so we return it to the free deque
                // (and notify the producer in case it was waiting). This is what bounds the pool:
                // chunks only re-enter the producer pool after the consumer has drained them.
                Chunk drained = pendingDrain;
                pendingDrain = null;
                recycle(drained);
            }
        }
        return totalBytesConsumed;
    }

    /**
     * Visible-for-test / test-only helper: current pipe state.
     */
    @SdkTestInternalApi
    State state() {
        return state.get();
    }

    /**
     * Visible-for-test / test-only helper: number of {@link Chunk} buffers minted so far. The pipe
     * lazily allocates chunks on the producer's first {@link #acquireForFill()}, so this is 0 until
     * the producer starts pumping and grows up to {@code depth}.
     */
    @SdkTestInternalApi
    int allocatedForTest() {
        synchronized (freeLock) {
            return allocated;
        }
    }

    private void recycle(Chunk c) {
        c.pos(0);
        c.len(0);
        synchronized (freeLock) {
            free.push(c);
            freeLock.notifyAll();
        }
    }
}
