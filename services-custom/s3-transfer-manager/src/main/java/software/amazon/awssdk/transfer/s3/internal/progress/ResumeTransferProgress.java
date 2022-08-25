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

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.transfer.s3.progress.TransferProgress;
import software.amazon.awssdk.transfer.s3.progress.TransferProgressSnapshot;
import software.amazon.awssdk.utils.Validate;

/**
 * An implementation of {@link TransferProgress} used when resuming a transfer. This uses a bytes-transferred of 0 until the real
 * progress is available (when the transfer starts).
 */
@SdkInternalApi
public class ResumeTransferProgress implements TransferProgress {
    private CompletableFuture<TransferProgress> progressFuture;

    public ResumeTransferProgress(CompletableFuture<TransferProgress> progressFuture) {
        this.progressFuture = Validate.paramNotNull(progressFuture, "progressFuture");
    }

    @Override
    public TransferProgressSnapshot snapshot() {
        if (progressFuture.isDone() && !progressFuture.isCompletedExceptionally()) {
            return progressFuture.join().snapshot();
        }
        return DefaultTransferProgressSnapshot.builder().transferredBytes(0L).build();
    }
}
