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

package software.amazon.awssdk.transfer.s3.internal.model;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.transfer.s3.model.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.model.PresignedFileDownload;
import software.amazon.awssdk.transfer.s3.progress.TransferProgress;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class DefaultPresignedFileDownload implements PresignedFileDownload {
    private final CompletableFuture<CompletedFileDownload> completionFuture;
    private final TransferProgress progress;

    public DefaultPresignedFileDownload(CompletableFuture<CompletedFileDownload> completionFuture,
                                        TransferProgress progress) {
        this.completionFuture = Validate.paramNotNull(completionFuture, "completionFuture");
        this.progress = Validate.paramNotNull(progress, "progress");
    }

    @Override
    public CompletableFuture<CompletedFileDownload> completionFuture() {
        return completionFuture;
    }

    @Override
    public TransferProgress progress() {
        return progress;
    }

    @Override
    public String toString() {
        return ToString.builder("DefaultPresignedFileDownload")
                       .add("completionFuture", completionFuture)
                       .add("progress", progress)
                       .build();
    }
}
