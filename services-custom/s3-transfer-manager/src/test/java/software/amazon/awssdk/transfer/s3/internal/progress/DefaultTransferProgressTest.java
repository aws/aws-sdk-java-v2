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

package software.amazon.awssdk.transfer.s3.internal.progress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.transfer.s3.progress.TransferProgressSnapshot;

class DefaultTransferProgressTest {

    @Test
    void snapshot_whenBytesAdded_includesAddedBytes() {
        DefaultTransferProgress progress = progressWithTotal(100L);

        progress.addTransferredBytes(30);
        progress.addTransferredBytes(40);

        assertThat(progress.snapshot().transferredBytes()).isEqualTo(70L);
        assertThat(progress.snapshot().ratioTransferred()).hasValue(0.7);
    }

    @Test
    void addTransferredBytes_whenBytesAdded_returnsNewCount() {
        DefaultTransferProgress progress = progressWithTotal(100L);

        assertThat(progress.addTransferredBytes(30)).isEqualTo(30L);
        assertThat(progress.addTransferredBytes(40)).isEqualTo(70L);
    }

    @Test
    void addTransferredBytes_whenNegative_subtractsBytes() {
        DefaultTransferProgress progress = progressWithTotal(100L);
        progress.addTransferredBytes(60);

        assertThat(progress.addTransferredBytes(-60)).isZero();
        assertThat(progress.snapshot().transferredBytes()).isZero();
    }

    @Test
    void resetTransferredBytes_whenBytesTransferred_setsCountToZero() {
        DefaultTransferProgress progress = progressWithTotal(100L);
        progress.addTransferredBytes(60);

        progress.resetTransferredBytes();

        assertThat(progress.snapshot().transferredBytes()).isZero();
        assertThat(progress.addTransferredBytes(5)).isEqualTo(5L);
    }

    @Test
    void addTransferredBytes_whenCalledConcurrently_snapshotHasExactSumAndNeverGoesBackward() throws Exception {
        int writers = 8;
        int readers = 2;
        int addsPerWriter = 10_000;
        long bytesPerAdd = 3;
        DefaultTransferProgress progress = progressWithTotal(null);

        ExecutorService executor = Executors.newFixedThreadPool(writers + readers);
        try {
            CountDownLatch start = new CountDownLatch(1);
            AtomicBoolean writersDone = new AtomicBoolean();
            AtomicBoolean wentBackward = new AtomicBoolean();

            List<Future<?>> writerFutures = new ArrayList<>();
            for (int i = 0; i < writers; i++) {
                writerFutures.add(executor.submit(() -> {
                    awaitQuietly(start);
                    for (int j = 0; j < addsPerWriter; j++) {
                        progress.addTransferredBytes(bytesPerAdd);
                    }
                }));
            }

            List<Future<?>> readerFutures = new ArrayList<>();
            for (int i = 0; i < readers; i++) {
                readerFutures.add(executor.submit(() -> {
                    awaitQuietly(start);
                    long previous = 0;
                    while (!writersDone.get()) {
                        long current = progress.snapshot().transferredBytes();
                        if (current < previous) {
                            wentBackward.set(true);
                        }
                        previous = current;
                    }
                }));
            }

            start.countDown();
            for (Future<?> f : writerFutures) {
                f.get(30, TimeUnit.SECONDS);
            }
            writersDone.set(true);
            for (Future<?> f : readerFutures) {
                f.get(30, TimeUnit.SECONDS);
            }

            assertThat(wentBackward).isFalse();
            assertThat(progress.snapshot().transferredBytes()).isEqualTo(writers * addsPerWriter * bytesPerAdd);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void addTransferredBytes_whenSubtractRacesConcurrentUpdate_doesNotThrowOrLoseBytes() throws Exception {
        DefaultTransferProgress progress = progressWithTotal(2000L);
        progress.addTransferredBytes(1000);

        runWhileUpdateInFlight(progress, () -> {
            progress.addTransferredBytes(-1000);
            assertThat(progress.snapshot().transferredBytes()).isZero();
        });

        assertThat(progress.snapshot().transferredBytes()).isZero();
        progress.addTransferredBytes(1000);
        assertThat(progress.snapshot().transferredBytes()).isEqualTo(1000L);
    }

    @Test
    void resetTransferredBytes_whenRacesConcurrentUpdate_doesNotRestoreBytes() throws Exception {
        DefaultTransferProgress progress = progressWithTotal(2000L);
        progress.addTransferredBytes(1000);

        runWhileUpdateInFlight(progress, () -> {
            progress.resetTransferredBytes();
            assertThat(progress.snapshot().transferredBytes()).isZero();
        });

        assertThat(progress.snapshot().transferredBytes()).isZero();
        progress.addTransferredBytes(500);
        assertThat(progress.snapshot().transferredBytes()).isEqualTo(500L);
    }

    @Test
    void updateAndGet_whenBytesTransferred_updaterSeesTransferredBytes() {
        DefaultTransferProgress progress = progressWithTotal(100L);
        progress.addTransferredBytes(25);
        AtomicLong seenByUpdater = new AtomicLong(-1);

        TransferProgressSnapshot snapshot = progress.updateAndGet(b -> seenByUpdater.set(b.getTransferredBytes()));

        assertThat(seenByUpdater).hasValue(25L);
        assertThat(snapshot.transferredBytes()).isEqualTo(25L);
    }

    @Test
    void updateAndGet_whenUpdaterChangesTransferredBytes_throwsAndKeepsSnapshot() {
        DefaultTransferProgress progress = progressWithTotal(100L);
        progress.addTransferredBytes(25);

        assertThatThrownBy(() -> progress.updateAndGet(b -> b.transferredBytes(0L).totalBytes(200L)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must not change transferredBytes");

        TransferProgressSnapshot snapshot = progress.snapshot();
        assertThat(snapshot.transferredBytes()).isEqualTo(25L);
        assertThat(snapshot.totalBytes()).hasValue(100L);
    }

    @Test
    void snapshot_whenBytesExceedTotal_capsAtTotal() {
        DefaultTransferProgress progress = progressWithTotal(100L);
        progress.addTransferredBytes(150);

        TransferProgressSnapshot snapshot = progress.snapshot();

        assertThat(snapshot.transferredBytes()).isEqualTo(100L);
        assertThat(snapshot.totalBytes()).hasValue(100L);
    }

    @Test
    void snapshot_whenBytesNegative_capsAtZero() {
        DefaultTransferProgress progress = progressWithTotal(100L);
        progress.addTransferredBytes(-10);

        assertThat(progress.snapshot().transferredBytes()).isZero();
    }

    @Test
    void snapshot_withByteCount_usesGivenCountCappedAtTotal() {
        DefaultTransferProgress progress = progressWithTotal(100L);
        progress.addTransferredBytes(10);

        assertThat(progress.snapshot(40L).transferredBytes()).isEqualTo(40L);
        assertThat(progress.snapshot(150L).transferredBytes()).isEqualTo(100L);
    }

    @Test
    void updateAndGet_whenUpdaterRaisesTotalAboveTransferredBytes_keepsAllBytes() {
        DefaultTransferProgress progress = progressWithTotal(100L);
        progress.addTransferredBytes(150);

        TransferProgressSnapshot snapshot = progress.updateAndGet(b -> b.totalBytes(200L));

        assertThat(snapshot.transferredBytes()).isEqualTo(150L);
        assertThat(snapshot.totalBytes()).hasValue(200L);
    }

    @Test
    void updateAndGet_whenUpdaterLowersTotalBelowTransferredBytes_capsWithoutLosingBytes() {
        DefaultTransferProgress progress = progressWithTotal(200L);
        progress.addTransferredBytes(150);

        TransferProgressSnapshot lowered = progress.updateAndGet(b -> b.totalBytes(100L));
        TransferProgressSnapshot raised = progress.updateAndGet(b -> b.totalBytes(200L));

        assertThat(lowered.transferredBytes()).isEqualTo(100L);
        assertThat(raised.transferredBytes()).isEqualTo(150L);
    }

    @Test
    void reachesTotalBytes_whenTotalUnknown_returnsFalse() {
        DefaultTransferProgress progress = progressWithTotal(null);

        assertThat(progress.hasTotalBytes()).isFalse();
        assertThat(progress.reachesTotalBytes(Long.MAX_VALUE)).isFalse();
    }

    @Test
    void reachesTotalBytes_whenTotalKnown_returnsTrueAtOrAboveTotal() {
        DefaultTransferProgress progress = progressWithTotal(100L);

        assertThat(progress.hasTotalBytes()).isTrue();
        assertThat(progress.reachesTotalBytes(99L)).isFalse();
        assertThat(progress.reachesTotalBytes(100L)).isTrue();
        assertThat(progress.reachesTotalBytes(101L)).isTrue();
    }

    @Test
    void toString_whenBytesTransferred_includesTransferredBytes() {
        DefaultTransferProgress progress = progressWithTotal(100L);
        progress.addTransferredBytes(5);

        assertThat(progress.toString()).contains("transferredBytes=5");
    }

    /**
     * Runs {@code action} while another thread is blocked inside {@link DefaultTransferProgress#updateAndGet}, then lets that
     * update finish and asserts it did not fail.
     */
    private static void runWhileUpdateInFlight(DefaultTransferProgress progress, Runnable action) throws Exception {
        CountDownLatch updaterEntered = new CountDownLatch(1);
        CountDownLatch releaseUpdater = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> update = executor.submit(() -> progress.updateAndGet(b -> {
                updaterEntered.countDown();
                awaitQuietly(releaseUpdater);
            }));
            assertThat(updaterEntered.await(5, TimeUnit.SECONDS)).isTrue();
            try {
                action.run();
            } finally {
                releaseUpdater.countDown();
            }
            update.get(5, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }

    private static DefaultTransferProgress progressWithTotal(Long totalBytes) {
        return new DefaultTransferProgress(DefaultTransferProgressSnapshot.builder()
                                                                          .transferredBytes(0L)
                                                                          .totalBytes(totalBytes)
                                                                          .build());
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
