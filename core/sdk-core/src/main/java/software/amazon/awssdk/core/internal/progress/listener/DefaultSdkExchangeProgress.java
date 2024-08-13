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

package software.amazon.awssdk.core.internal.progress.listener;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Mutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.internal.progress.snapshot.DefaultProgressSnapshot;
import software.amazon.awssdk.core.progress.listener.SdkExchangeProgress;
import software.amazon.awssdk.core.progress.snapshot.ProgressSnapshot;

/**
 * An SDK-internal implementation of {@link SdkExchangeProgress}. This implementation acts as a thin wrapper around {@link
 * AtomicReference}, where calls to get the latest {@link #progressSnapshot()} simply return the latest reference, while {@link
 * DefaultProgressUpdater} is responsible for continuously updating the latest reference.
 *
 * @see SdkExchangeProgress
 */
@Mutable
@ThreadSafe
@SdkInternalApi
public class DefaultSdkExchangeProgress implements SdkExchangeProgress {

    private final AtomicReference<ProgressSnapshot> snapshot;

    public DefaultSdkExchangeProgress(ProgressSnapshot snapshot) {
        this.snapshot = new AtomicReference<>(snapshot);
    }

    /**
     * Atomically convert the current snapshot reference to its {@link Builder}, perform updates using the provided {@link
     * Consumer}, and save the result as the latest snapshot.
     */
    public ProgressSnapshot updateAndGet(Consumer<DefaultProgressSnapshot.Builder> updater) {
        return this.snapshot.updateAndGet(s -> ((DefaultProgressSnapshot) s).copy(updater));
    }

    @Override
    public ProgressSnapshot progressSnapshot() {
        return this.snapshot.get();
    }
}
