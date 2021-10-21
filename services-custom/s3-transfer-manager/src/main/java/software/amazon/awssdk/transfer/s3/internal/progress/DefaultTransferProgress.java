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

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Mutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.transfer.s3.internal.progress.DefaultTransferProgressSnapshot.Builder;
import software.amazon.awssdk.transfer.s3.progress.TransferProgress;
import software.amazon.awssdk.transfer.s3.progress.TransferProgressSnapshot;
import software.amazon.awssdk.utils.ToString;

/**
 * An SDK-internal implementation of {@link TransferProgress}. This implementation acts as a thin wrapper around {@link
 * AtomicReference}, where calls to get the latest {@link #snapshot()} simply return the latest reference, while {@link
 * TransferProgressUpdater} is responsible for continuously updating the latest reference.
 *
 * @see TransferProgress
 */
@Mutable
@ThreadSafe
@SdkInternalApi
public final class DefaultTransferProgress implements TransferProgress {

    private final AtomicReference<TransferProgressSnapshot> snapshot;

    public DefaultTransferProgress(TransferProgressSnapshot snapshot) {
        this.snapshot = new AtomicReference<>(snapshot);
    }

    /**
     * Atomically convert the current snapshot reference to its {@link Builder}, perform updates using the provided {@link
     * Consumer}, and save the result as the latest snapshot.
     */
    public TransferProgressSnapshot updateAndGet(Consumer<DefaultTransferProgressSnapshot.Builder> updater) {
        return this.snapshot.updateAndGet(s -> ((DefaultTransferProgressSnapshot) s).copy(updater));
    }

    @Override
    public TransferProgressSnapshot snapshot() {
        return snapshot.get();
    }

    @Override
    public String toString() {
        return ToString.builder("TransferProgress")
                       .add("snapshot", snapshot)
                       .build();
    }
}
