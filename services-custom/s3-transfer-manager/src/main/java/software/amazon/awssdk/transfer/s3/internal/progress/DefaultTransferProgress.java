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

import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Mutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.transfer.s3.internal.progress.DefaultTransferProgressSnapshot.Builder;
import software.amazon.awssdk.transfer.s3.progress.TransferProgress;
import software.amazon.awssdk.transfer.s3.progress.TransferProgressSnapshot;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * An SDK-internal implementation of {@link TransferProgress}. The transferred byte count is kept in an {@link AtomicLong} and the
 * rest of the snapshot in an {@link AtomicReference}. {@link #snapshot()} combines the two, while {@link TransferProgressUpdater}
 * is responsible for updating them.
 *
 * @see TransferProgress
 */
@Mutable
@ThreadSafe
@SdkInternalApi
public final class DefaultTransferProgress implements TransferProgress {

    /**
     * The transferred byte count. This is the only source of truth for transferred bytes, and may be outside
     * {@code [0, totalBytes]}; snapshots cap it to that range.
     */
    private final AtomicLong transferredBytes;

    /**
     * The total bytes and response. Its transferred byte count is replaced by {@link #transferredBytes} on every read.
     */
    private final AtomicReference<TransferProgressSnapshot> snapshot;

    public DefaultTransferProgress(TransferProgressSnapshot snapshot) {
        this.transferredBytes = new AtomicLong(snapshot.transferredBytes());
        this.snapshot = new AtomicReference<>(snapshot);
    }

    /**
     * Atomically updates the total bytes or response of the latest snapshot and returns the result. The updater sees the current
     * transferred byte count but must not change it.
     *
     * @throws IllegalArgumentException if the updater changes the transferred byte count
     */
    public TransferProgressSnapshot updateAndGet(Consumer<Builder> updater) {
        TransferProgressSnapshot updated =
            snapshot.updateAndGet(s -> ((DefaultTransferProgressSnapshot) s).copy(b -> applyUpdate(b, updater)));
        return withTransferredBytes(updated, transferredBytes.get());
    }

    /**
     * Adds {@code numBytes}, which may be negative, to the transferred byte count and returns the new count.
     */
    public long addTransferredBytes(long numBytes) {
        return transferredBytes.addAndGet(numBytes);
    }

    /**
     * Sets the transferred byte count to zero.
     */
    public void resetTransferredBytes() {
        transferredBytes.set(0L);
    }

    /**
     * Returns whether the total size of the transfer is known.
     */
    public boolean hasTotalBytes() {
        return snapshot.get().totalBytes().isPresent();
    }

    /**
     * Returns whether {@code numBytes} reaches the total size of the transfer, or {@code false} if the total is unknown.
     */
    public boolean reachesTotalBytes(long numBytes) {
        OptionalLong totalBytes = snapshot.get().totalBytes();
        return totalBytes.isPresent() && numBytes >= totalBytes.getAsLong();
    }

    /**
     * Returns the latest snapshot with its transferred byte count set to {@code numBytes}, capped to {@code [0, totalBytes]}.
     */
    public TransferProgressSnapshot snapshot(long numBytes) {
        return withTransferredBytes(snapshot.get(), numBytes);
    }

    @Override
    public TransferProgressSnapshot snapshot() {
        return snapshot(transferredBytes.get());
    }

    private void applyUpdate(Builder builder, Consumer<Builder> updater) {
        long currentBytes = transferredBytes.get();
        builder.transferredBytes(currentBytes);
        updater.accept(builder);
        Validate.isTrue(builder.getTransferredBytes() == currentBytes,
                        "updateAndGet must not change transferredBytes. Use addTransferredBytes or resetTransferredBytes.");
        Long totalBytes = builder.getTotalBytes();
        builder.transferredBytes(capBytes(currentBytes, totalBytes == null ? Long.MAX_VALUE : totalBytes));
    }

    private static TransferProgressSnapshot withTransferredBytes(TransferProgressSnapshot snapshot, long numBytes) {
        OptionalLong totalBytes = snapshot.totalBytes();
        long cappedBytes = capBytes(numBytes, totalBytes.isPresent() ? totalBytes.getAsLong() : Long.MAX_VALUE);
        if (snapshot.transferredBytes() == cappedBytes) {
            return snapshot;
        }
        return ((DefaultTransferProgressSnapshot) snapshot).copy(b -> b.transferredBytes(cappedBytes));
    }

    private static long capBytes(long numBytes, long maxBytes) {
        return Math.min(Math.max(0L, numBytes), maxBytes);
    }

    @Override
    public String toString() {
        return ToString.builder("TransferProgress")
                       .add("snapshot", snapshot())
                       .build();
    }
}
