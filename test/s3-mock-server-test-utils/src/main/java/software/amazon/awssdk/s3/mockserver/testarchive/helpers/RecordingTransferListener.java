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

package software.amazon.awssdk.s3.mockserver.testarchive.helpers;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import software.amazon.awssdk.s3.mockserver.helpers.Immutable;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;
import software.amazon.awssdk.transfer.s3.progress.TransferProgressSnapshot;

public final class RecordingTransferListener implements TransferListener {

    private static final BooleanSupplier NO_MARKER = () -> false;

    public static final class Sample {
        private final long transferredBytes;
        private final Long totalBytes;
        private final boolean marker;
        private final String thread;

        public Sample(long transferredBytes, Long totalBytes, boolean marker, String thread) {
            this.transferredBytes = transferredBytes;
            this.totalBytes = totalBytes;
            this.marker = marker;
            this.thread = thread;
        }

        public long transferredBytes() {
            return transferredBytes;
        }

        public Long totalBytes() {
            return totalBytes;
        }

        public boolean marker() {
            return marker;
        }

        public String thread() {
            return thread;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Sample that = (Sample) o;
            return transferredBytes == that.transferredBytes
                    && Objects.equals(totalBytes, that.totalBytes)
                    && marker == that.marker
                    && Objects.equals(thread, that.thread);
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(transferredBytes);
            result = 31 * result + Objects.hashCode(totalBytes);
            result = 31 * result + Objects.hashCode(marker);
            result = 31 * result + Objects.hashCode(thread);
            return result;
        }

        public boolean full() {
            return totalBytes != null && transferredBytes == totalBytes;
        }

        @Override
        public String toString() {
            return transferredBytes + "/" + totalBytes + (marker ? " [marker]" : "") + " on " + thread;
        }
    }

    private final BooleanSupplier marker;

    private final AtomicInteger initiatedCount = new AtomicInteger();
    private final AtomicInteger completedCount = new AtomicInteger();
    private final AtomicInteger failedCount = new AtomicInteger();

    private final List<Sample> samples = new CopyOnWriteArrayList<>();
    private final AtomicReference<Sample> atInitiation = new AtomicReference<>();
    private final AtomicReference<Sample> atSettlement = new AtomicReference<>();
    private final AtomicReference<Throwable> failure = new AtomicReference<>();
    private final CountDownLatch settled = new CountDownLatch(1);

    private RecordingTransferListener(BooleanSupplier marker) {
        this.marker = marker;
    }

    public static RecordingTransferListener create() {
        return new RecordingTransferListener(NO_MARKER);
    }

    public static RecordingTransferListener sampling(BooleanSupplier marker) {
        return new RecordingTransferListener(marker);
    }

    @Override
    public void transferInitiated(Context.TransferInitiated context) {
        initiatedCount.incrementAndGet();
        atInitiation.set(sample(context.progressSnapshot()));
    }

    @Override
    public void bytesTransferred(Context.BytesTransferred context) {
        samples.add(sample(context.progressSnapshot()));
    }

    @Override
    public void transferComplete(Context.TransferComplete context) {
        completedCount.incrementAndGet();
        atSettlement.set(sample(context.progressSnapshot()));
        settled.countDown();
    }

    @Override
    public void transferFailed(Context.TransferFailed context) {
        failedCount.incrementAndGet();
        failure.set(context.exception());
        atSettlement.set(sample(context.progressSnapshot()));
        settled.countDown();
    }

    public boolean awaitSettlement(Duration timeout) {
        try {
            return settled.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public int initiatedCount() {
        return initiatedCount.get();
    }

    public int completedCount() {
        return completedCount.get();
    }

    public int failedCount() {
        return failedCount.get();
    }

    public List<Sample> samples() {
        return Immutable.copyOfList(samples);
    }

    public Sample atInitiation() {
        return atInitiation.get();
    }

    public Sample atSettlement() {
        return atSettlement.get();
    }

    public Throwable failure() {
        return failure.get();
    }

    public long lastReportedBytes() {
        List<Sample> seen = samples();
        return seen.isEmpty() ? -1 : seen.get(seen.size() - 1).transferredBytes();
    }

    public long peakReportedBytes() {
        return samples().stream().mapToLong(Sample::transferredBytes).max().orElse(-1);
    }

    public String render() {
        StringBuilder sb = new StringBuilder();
        sb.append("initiated=").append(initiatedCount()).append(" complete=").append(completedCount())
                .append(" failed=").append(failedCount()).append('\n');
        sb.append("  at initiation: ").append(atInitiation()).append('\n');
        for (Sample sample : samples()) {
            sb.append("  bytesTransferred: ").append(sample).append('\n');
        }
        sb.append("  at settlement: ").append(atSettlement()).append('\n');
        if (failure() != null) {
            sb.append("  failure: ").append(failure()).append('\n');
        }
        return sb.toString();
    }

    private Sample sample(TransferProgressSnapshot snapshot) {
        return new Sample(
                snapshot.transferredBytes(),
                snapshot.totalBytes().isPresent() ? snapshot.totalBytes().getAsLong() : null,
                marker.getAsBoolean(),
                Thread.currentThread().getName());
    }
}
