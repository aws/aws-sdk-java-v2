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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullResponse;

class SyncRequestBodyPumpTest {

    @Test
    void pump_happyPath_consumerSeesAllProducerBytes() throws Exception {
        byte[] payload = new byte[200];
        for (int i = 0; i < payload.length; i++) {
            payload[i] = (byte) (i & 0xFF);
        }
        BodyChunkPipe pipe = new BodyChunkPipe(2, 32);
        SyncRequestBodyPump pump = new SyncRequestBodyPump(ContentStreamProvider.fromByteArray(payload), pipe);
        AtomicReference<Throwable> producerError = new AtomicReference<>();
        Thread producer = new Thread(() -> {
            try {
                pump.pump();
            } catch (Throwable t) {
                producerError.set(t);
            }
        }, "pump-producer");

        producer.start();
        byte[] consumed = drainAll(pipe, payload.length);
        producer.join(5_000);

        assertThat(producerError.get()).isNull();
        assertThat(producer.isAlive()).isFalse();
        assertThat(consumed).containsExactly(payload);
        assertThat(pipe.state()).isEqualTo(BodyChunkPipe.State.EOF);
    }

    @Test
    void pump_emptyStream_signalsEofWithoutPublish() throws Exception {
        BodyChunkPipe pipe = new BodyChunkPipe(2, 16);
        SyncRequestBodyPump pump = new SyncRequestBodyPump(ContentStreamProvider.fromByteArray(new byte[0]), pipe);

        pump.pump();

        assertThat(pipe.state()).isEqualTo(BodyChunkPipe.State.EOF);
        assertThat(pipe.pollDrain(ByteBuffer.allocate(8))).isEqualTo(-1);
    }

    @Test
    void pump_inputStreamThrowsIoException_pumpSignalsErrorAndRethrows() {
        IOException ioe = new IOException("disk gone");
        BodyChunkPipe pipe = new BodyChunkPipe(2, 16);
        ContentStreamProvider provider = () -> new InputStream() {
            @Override
            public int read() {
                throw new UnsupportedOperationException();
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                throw ioe;
            }
        };
        SyncRequestBodyPump pump = new SyncRequestBodyPump(provider, pipe);

        assertThatThrownBy(pump::pump).isSameAs(ioe);
        assertThat(pipe.state()).isEqualTo(BodyChunkPipe.State.ERROR);
        assertThatThrownBy(() -> pipe.pollDrain(ByteBuffer.allocate(8)))
            .hasMessageContaining("Producer failed")
            .hasRootCauseMessage("disk gone");
    }

    @Test
    void pump_abortedWhilePumping_returnsWithoutSignalingEof() throws Exception {
        // pipe depth 1 + payload larger than chunk forces producer to block on second acquireForFill,
        // giving the test thread a deterministic point to call abort().
        BodyChunkPipe pipe = new BodyChunkPipe(1, 8);
        byte[] payload = new byte[64];
        SyncRequestBodyPump pump = new SyncRequestBodyPump(ContentStreamProvider.fromByteArray(payload), pipe);
        AtomicReference<Throwable> producerError = new AtomicReference<>();
        Thread producer = new Thread(() -> {
            try {
                pump.pump();
            } catch (Throwable t) {
                producerError.set(t);
            }
        }, "pump-producer");

        producer.start();
        waitUntilStateIsOpenWithChunkInFlight(pipe);
        pump.abort();
        producer.join(5_000);

        assertThat(producer.isAlive()).isFalse();
        assertThat(producerError.get()).isNull();
        assertThat(pipe.state()).isEqualTo(BodyChunkPipe.State.ABORTED);
    }

    /**
     * Regression test for the producer-livelock-on-CRT-failure path.
     *
     * <p>If CRT signals request failure (network error, idle/health timeout, etc.) while the
     * producer is parked in {@link BodyChunkPipe#acquireForFill()}, nothing in the pipe's normal
     * contract wakes it without a recycle/abort. The fix in {@code AwsCrtHttpClient.CrtHttpRequest.call()}
     * registers a {@code responseFuture.whenComplete(...)} hook that calls {@code pump.abort()}
     * when the response future completes <em>exceptionally</em>. This test reproduces that wiring
     * at the unit level: a pump runs against a pipe with no consumer, the producer parks once the
     * pipe is full, and we then complete a separate response future exceptionally with the same
     * hook to verify the producer unblocks and {@code pump()} returns.
     *
     * <p>Without the hook (or equivalent abort path), {@code producer.join(5_000)} would time out
     * and the test would fail.
     */
    @Test
    void pump_responseFutureFailsExceptionally_whileProducerParked_unblocksProducerViaAbortHook() throws Exception {
        BodyChunkPipe pipe = new BodyChunkPipe(2, 8);
        // Payload larger than depth*chunkSize forces the producer to park on acquireForFill once
        // both chunks are sitting in the ready queue with no consumer draining.
        byte[] payload = new byte[128];
        SyncRequestBodyPump pump = new SyncRequestBodyPump(ContentStreamProvider.fromByteArray(payload), pipe);
        CompletableFuture<SdkHttpFullResponse> responseFuture = new CompletableFuture<>();
        responseFuture.whenComplete((r, t) -> {
            if (t != null) {
                pump.abort();
            }
        });

        AtomicReference<Throwable> producerError = new AtomicReference<>();
        Thread producer = new Thread(() -> {
            try {
                pump.pump();
            } catch (Throwable t) {
                producerError.set(t);
            }
        }, "pump-producer");

        producer.start();
        waitUntilProducerIsParked(pipe);
        responseFuture.completeExceptionally(new IOException("simulated CRT failure"));
        producer.join(5_000);

        assertThat(producer.isAlive()).isFalse();
        assertThat(producerError.get()).isNull();
        assertThat(pipe.state()).isEqualTo(BodyChunkPipe.State.ABORTED);
    }

    @Test
    void abort_propagatesToPipe() {
        BodyChunkPipe pipe = new BodyChunkPipe(2, 8);
        SyncRequestBodyPump pump = new SyncRequestBodyPump(
            ContentStreamProvider.fromByteArray(new byte[0]), pipe);

        pump.abort();

        assertThat(pipe.state()).isEqualTo(BodyChunkPipe.State.ABORTED);
    }

    private static byte[] drainAll(BodyChunkPipe pipe, int expected) {
        byte[] out = new byte[expected];
        int written = 0;
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (written < expected && System.nanoTime() < deadline) {
            ByteBuffer scratch = ByteBuffer.allocate(Math.min(64, expected - written));
            int n = pipe.pollDrain(scratch);
            if (n < 0) {
                break;
            }
            if (n == 0) {
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
                continue;
            }
            scratch.flip();
            scratch.get(out, written, n);
            written += n;
        }
        if (written < expected) {
            throw new AssertionError("Drained only " + written + " of " + expected + " bytes");
        }
        return out;
    }

    private static void waitUntilStateIsOpenWithChunkInFlight(BodyChunkPipe pipe) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (pipe.allocatedForTest() >= 1) {
                return;
            }
            Thread.sleep(1);
        }
        throw new AssertionError("Producer did not allocate a chunk within timeout");
    }

    /**
     * Wait for the producer to park on {@code acquireForFill}. Detected by the pipe reaching its
     * configured depth in allocations and then staying there for a couple of consecutive observations
     * (the producer can't make further progress without a recycle).
     */
    private static void waitUntilProducerIsParked(BodyChunkPipe pipe) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        int stableObservations = 0;
        int lastAllocated = -1;
        while (System.nanoTime() < deadline) {
            int allocated = pipe.allocatedForTest();
            if (allocated == lastAllocated && allocated > 0) {
                if (++stableObservations >= 3) {
                    return;
                }
            } else {
                stableObservations = 0;
                lastAllocated = allocated;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("Producer did not park within timeout");
    }
}
