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

package software.amazon.awssdk.services.s3.internal.crt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.core.exception.SdkClientException;

@Timeout(10)
class S3CrtBorrowedBufferInputStreamTest {
    @Test
    void emptyCompletion_returnsRepeatedEof() throws Exception {
        S3CrtBorrowedBufferInputStream stream = new S3CrtBorrowedBufferInputStream(() -> { });
        stream.onComplete();

        assertThat(stream.read()).isEqualTo(-1);
        assertThat(stream.read(new byte[1])).isEqualTo(-1);
    }

    @Test
    void interruptedBlockedReader_restoresInterruptAndCancelsUpstream() throws Exception {
        AtomicInteger cancellations = new AtomicInteger();
        AtomicBoolean interrupted = new AtomicBoolean();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        S3CrtBorrowedBufferInputStream stream = new S3CrtBorrowedBufferInputStream(cancellations::incrementAndGet);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);
        Thread reader = new Thread(() -> {
            started.countDown();
            try {
                stream.read();
            } catch (Throwable t) {
                failure.set(t);
                interrupted.set(Thread.currentThread().isInterrupted());
            } finally {
                finished.countDown();
            }
        });
        reader.start();
        assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();

        reader.interrupt();

        assertThat(finished.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(failure.get()).isInstanceOf(InterruptedIOException.class)
                                 .hasMessage("Interrupted while waiting for response data");
        assertThat(interrupted).isTrue();
        assertThat(cancellations).hasValue(1);
    }

    @Test
    void preInterruptedReader_consumesQueuedDataWithoutCancellation() throws Exception {
        AtomicInteger cancellations = new AtomicInteger();
        AtomicInteger releases = new AtomicInteger();
        AtomicLong credited = new AtomicLong();
        S3CrtBorrowedBufferInputStream stream = new S3CrtBorrowedBufferInputStream(cancellations::incrementAndGet);
        stream.onBuffer(buffer("a", releases::incrementAndGet, credited));
        stream.onComplete();

        Thread.currentThread().interrupt();
        try {
            assertThat(stream.read()).isEqualTo('a');
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
            assertThat(releases).hasValue(1);
            assertThat(credited).hasValue(1);
                assertThat(cancellations).hasValue(0);
        } finally {
            Thread.interrupted();
            stream.close();
        }
    }

    @Test
    void read_drainsAcrossBuffersAndCreditsOnlyAfterFullConsumption() throws Exception {
        AtomicInteger cancellations = new AtomicInteger();
        AtomicInteger releases = new AtomicInteger();
        AtomicLong credited = new AtomicLong();
        S3CrtBorrowedBufferInputStream stream = new S3CrtBorrowedBufferInputStream(cancellations::incrementAndGet);
        stream.onBuffer(buffer("abc", releases::incrementAndGet, credited));
        stream.onBuffer(buffer("def", releases::incrementAndGet, credited));
        stream.onComplete();

        assertThat(stream.read(new byte[0])).isZero();
        assertThat(stream.read()).isEqualTo('a');
        assertThat(releases).hasValue(0);
        assertThat(credited).hasValue(0);

        byte[] destination = new byte[7];
        assertThat(stream.read(destination, 1, 5)).isEqualTo(5);
        assertThat(new String(destination, 1, 5, StandardCharsets.UTF_8)).isEqualTo("bcdef");
        assertThat(releases).hasValue(2);
        assertThat(credited).hasValue(6);
        assertThat(stream.read()).isEqualTo(-1);
        assertThat(stream.read(destination)).isEqualTo(-1);
        assertThat(cancellations).hasValue(0);
    }

    @Test
    void remoteFailure_isReportedAfterAcceptedBytes() throws Exception {
        RuntimeException failure = new RuntimeException("request failed");
        AtomicInteger releases = new AtomicInteger();
        AtomicLong credited = new AtomicLong();
        S3CrtBorrowedBufferInputStream stream = new S3CrtBorrowedBufferInputStream(() -> { });
        stream.onBuffer(buffer("abc", releases::incrementAndGet, credited));
        stream.onError(failure);

        byte[] destination = new byte[8];
        assertThat(stream.read(destination)).isEqualTo(3);
        assertThat(new String(destination, 0, 3, StandardCharsets.UTF_8)).isEqualTo("abc");
        assertThat(releases).hasValue(1);
        assertThat(credited).hasValue(3);
        assertThatThrownBy(stream::read)
            .isInstanceOf(IOException.class)
            .hasCause(failure);
    }

    @Test
    void sdkClientFailure_isReportedWithoutWrapping() {
        SdkClientException failure = SdkClientException.create("request failed");
        S3CrtBorrowedBufferInputStream stream = new S3CrtBorrowedBufferInputStream(() -> { });
        stream.onError(failure);

        assertThatThrownBy(stream::read).isSameAs(failure);
    }

    @Test
    void close_discardsCurrentAndQueuedBuffersWithoutCredit() throws Exception {
        AtomicInteger cancellations = new AtomicInteger();
        AtomicInteger releases = new AtomicInteger();
        AtomicLong credited = new AtomicLong();
        S3CrtBorrowedBufferInputStream stream = new S3CrtBorrowedBufferInputStream(cancellations::incrementAndGet);
        stream.onBuffer(buffer("abc", releases::incrementAndGet, credited));
        stream.onBuffer(buffer("def", releases::incrementAndGet, credited));

        assertThat(stream.read()).isEqualTo('a');
        stream.close();
        stream.close();
        stream.abort();

        assertThat(releases).hasValue(2);
        assertThat(credited).hasValue(0);
        assertThat(cancellations).hasValue(1);
        assertThatThrownBy(stream::read)
            .isInstanceOf(IOException.class)
            .hasMessage("Stream is closed");
    }

    @Test
    void close_racingActiveReadDiscardsNextBufferWithoutCredit() throws Exception {
        AtomicInteger cancellations = new AtomicInteger();
        AtomicInteger releases = new AtomicInteger();
        AtomicLong credited = new AtomicLong();
        CountDownLatch firstReleaseStarted = new CountDownLatch(1);
        CountDownLatch continueFirstRelease = new CountDownLatch(1);
        AtomicReference<Thread> closeThread = new AtomicReference<>();
        S3CrtBorrowedBufferInputStream stream = new S3CrtBorrowedBufferInputStream(cancellations::incrementAndGet);
        stream.onBuffer(buffer("a", () -> {
            releases.incrementAndGet();
            firstReleaseStarted.countDown();
            await(continueFirstRelease);
        }, credited));
        stream.onBuffer(buffer("b", releases::incrementAndGet, credited));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> readResult = executor.submit(() -> stream.read(new byte[2]));
            assertThat(firstReleaseStarted.await(5, TimeUnit.SECONDS)).isTrue();
            Future<?> closeResult = executor.submit(() -> {
                closeThread.set(Thread.currentThread());
                stream.close();
                return null;
            });
            awaitBlocked(closeThread);

            continueFirstRelease.countDown();

            assertThat(readResult.get(5, TimeUnit.SECONDS)).isEqualTo(1);
            closeResult.get(5, TimeUnit.SECONDS);
            assertThat(releases).hasValue(2);
            assertThat(credited).hasValue(1);
            assertThat(cancellations).hasValue(1);
        } finally {
            continueFirstRelease.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void close_wakesBlockedReader() throws Exception {
        AtomicInteger cancellations = new AtomicInteger();
        S3CrtBorrowedBufferInputStream stream = new S3CrtBorrowedBufferInputStream(cancellations::incrementAndGet);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch started = new CountDownLatch(1);
        try {
            Future<Integer> result = executor.submit(() -> {
                started.countDown();
                return stream.read();
            });
            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();

            stream.close();

            assertThatThrownBy(() -> result.get(5, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(IOException.class);
            assertThat(cancellations).hasValue(1);
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void releaseFailure_failsStreamCancelsUpstreamAndDiscardsQueuedBuffer() throws Exception {
        RuntimeException failure = new RuntimeException("release failed");
        AtomicInteger cancellations = new AtomicInteger();
        AtomicInteger firstReleases = new AtomicInteger();
        AtomicInteger secondReleases = new AtomicInteger();
        AtomicLong credited = new AtomicLong();
        S3CrtBorrowedBufferInputStream stream = new S3CrtBorrowedBufferInputStream(cancellations::incrementAndGet);
        stream.onBuffer(buffer("a", () -> {
            firstReleases.incrementAndGet();
            throw failure;
        }, credited));
        stream.onBuffer(buffer("b", secondReleases::incrementAndGet, credited));

        assertThat(stream.read()).isEqualTo('a');
        assertThatThrownBy(stream::read)
            .isInstanceOf(IOException.class)
            .hasCause(failure);
        assertThat(firstReleases).hasValue(1);
        assertThat(secondReleases).hasValue(1);
        assertThat(credited).hasValue(0);
        assertThat(cancellations).hasValue(1);
    }

    @Test
    void close_attemptsAllCleanupActionsWithoutThrowing() throws Exception {
        AtomicInteger cancellations = new AtomicInteger();
        AtomicInteger releases = new AtomicInteger();
        AtomicLong credited = new AtomicLong();
        S3CrtBorrowedBufferInputStream stream = new S3CrtBorrowedBufferInputStream(() -> {
            cancellations.incrementAndGet();
            throw new RuntimeException("request cancel failed");
        });
        stream.onBuffer(buffer("abc", () -> {
            releases.incrementAndGet();
            throw new RuntimeException("first release failed");
        }, credited));
        stream.onBuffer(buffer("def", () -> {
            releases.incrementAndGet();
            throw new RuntimeException("second release failed");
        }, credited));
        assertThat(stream.read()).isEqualTo('a');

        assertThatCode(stream::close).doesNotThrowAnyException();
        assertThat(cancellations).hasValue(1);
        assertThat(releases).hasValue(2);
        assertThat(credited).hasValue(0);
        stream.close();
    }

    @Test
    void abortAsFirstTerminalOperation_cancelsAndDiscards() {
        AtomicInteger cancellations = new AtomicInteger();
        AtomicInteger releases = new AtomicInteger();
        AtomicLong credited = new AtomicLong();
        S3CrtBorrowedBufferInputStream stream = new S3CrtBorrowedBufferInputStream(cancellations::incrementAndGet);
        stream.onBuffer(buffer("abc", releases::incrementAndGet, credited));

        stream.abort();
        stream.abort();

        assertThat(releases).hasValue(1);
        assertThat(credited).hasValue(0);
        assertThat(cancellations).hasValue(1);
    }

    @Test
    void abort_cleanupFailuresDoNotEscape() {
        AtomicInteger cancellations = new AtomicInteger();
        AtomicInteger releases = new AtomicInteger();
        AtomicLong credited = new AtomicLong();
        S3CrtBorrowedBufferInputStream stream = new S3CrtBorrowedBufferInputStream(() -> {
            cancellations.incrementAndGet();
            throw new RuntimeException("request cancel failed");
        });
        stream.onBuffer(buffer("abc", () -> {
            releases.incrementAndGet();
            throw new RuntimeException("release failed");
        }, credited));

        assertThatCode(stream::abort).doesNotThrowAnyException();

        assertThat(cancellations).hasValue(1);
        assertThat(releases).hasValue(1);
        assertThat(credited).hasValue(0);
    }

    @Test
    void closeBeforeLateBuffer_discardsWithoutCredit() throws Exception {
        AtomicInteger cancellations = new AtomicInteger();
        AtomicInteger releases = new AtomicInteger();
        AtomicLong credited = new AtomicLong();
        S3CrtBorrowedBufferInputStream stream = new S3CrtBorrowedBufferInputStream(cancellations::incrementAndGet);

        stream.close();
        assertThat(stream.onBuffer(buffer("abc", releases::incrementAndGet, credited))).isFalse();

        assertThat(cancellations).hasValue(1);
        assertThat(releases).hasValue(1);
        assertThat(credited).hasValue(0);
    }

    @Test
    void bufferAfterTerminalSignal_isDiscarded() {
        AtomicInteger releases = new AtomicInteger();
        AtomicLong credited = new AtomicLong();
        S3CrtBorrowedBufferInputStream completed = new S3CrtBorrowedBufferInputStream(() -> { });
        S3CrtBorrowedBufferInputStream failed = new S3CrtBorrowedBufferInputStream(() -> { });
        completed.onComplete();
        failed.onError(new RuntimeException("failed"));

        assertThat(completed.onBuffer(buffer("a", releases::incrementAndGet, credited))).isFalse();
        assertThat(failed.onBuffer(buffer("b", releases::incrementAndGet, credited))).isFalse();

        assertThat(releases).hasValue(2);
        assertThat(credited).hasValue(0);
    }

    @Test
    void zeroLengthBuffer_isConsumedBeforeEof() throws Exception {
        AtomicInteger releases = new AtomicInteger();
        AtomicInteger creditCalls = new AtomicInteger();
        S3CrtBorrowedBufferInputStream stream = new S3CrtBorrowedBufferInputStream(() -> { });
        stream.onBuffer(new S3CrtBorrowedBuffer(ByteBuffer.allocateDirect(0), 0,
                                              releases::incrementAndGet, bytes -> creditCalls.incrementAndGet()));
        stream.onComplete();

        assertThat(stream.read()).isEqualTo(-1);
        assertThat(releases).hasValue(1);
        assertThat(creditCalls).hasValue(1);
    }

    @Test
    void completionWithUnreadData_discardsWithoutCancellingCompletedUpstream() throws Exception {
        AtomicInteger cancellations = new AtomicInteger();
        AtomicInteger releases = new AtomicInteger();
        AtomicLong credited = new AtomicLong();
        S3CrtBorrowedBufferInputStream stream = new S3CrtBorrowedBufferInputStream(cancellations::incrementAndGet);
        stream.onBuffer(buffer("abc", releases::incrementAndGet, credited));
        stream.onComplete();

        stream.close();

        assertThat(releases).hasValue(1);
        assertThat(credited).hasValue(0);
        assertThat(cancellations).hasValue(0);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Timed out waiting for test latch");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private static void awaitBlocked(AtomicReference<Thread> threadReference) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            Thread thread = threadReference.get();
            if (thread != null && thread.getState() == Thread.State.BLOCKED) {
                return;
            }
            TimeUnit.MILLISECONDS.sleep(10);
        }
        throw new AssertionError("Close thread did not block on the active read");
    }

    private static S3CrtBorrowedBuffer buffer(String value, Runnable release, AtomicLong credited) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        ByteBuffer view = ByteBuffer.allocateDirect(bytes.length);
        view.put(bytes).flip();
        return new S3CrtBorrowedBuffer(view, bytes.length, release, credited::addAndGet);
    }

}
