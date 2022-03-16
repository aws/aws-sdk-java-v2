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

package software.amazon.awssdk.transfer.s3.internal;

import static software.amazon.awssdk.transfer.s3.exception.TransferPauseException.ErrorCode.ALREADY_FINISHED;
import static software.amazon.awssdk.transfer.s3.exception.TransferPauseException.ErrorCode.NOT_STARTED;
import static software.amazon.awssdk.transfer.s3.exception.TransferPauseException.ErrorCode.PAUSE_IN_PROGRESS;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.transfer.s3.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.FileDownload;
import software.amazon.awssdk.transfer.s3.ResumableFileDownload;
import software.amazon.awssdk.transfer.s3.exception.TransferPauseException;
import software.amazon.awssdk.transfer.s3.internal.progress.DownloadFileMonitor;
import software.amazon.awssdk.transfer.s3.progress.TransferProgress;
import software.amazon.awssdk.utils.ToString;

@SdkInternalApi
public final class DefaultFileDownload implements FileDownload {

    private final CompletableFuture<CompletedFileDownload> completionFuture;
    private final TransferProgress progress;
    private final DownloadFileMonitor monitor;
    private volatile boolean paused;

    DefaultFileDownload(CompletableFuture<CompletedFileDownload> completionFuture,
                        TransferProgress progress,
                        DownloadFileMonitor monitor) {
        this.completionFuture = completionFuture;
        this.progress = progress;
        this.monitor = monitor;
    }

    @Override
    public TransferProgress progress() {
        return progress;
    }

    @Override
    public ResumableFileDownload pause() {
        validatePauseStatus();
        paused = true;
        completionFuture.cancel(false);
        GetObjectResponse getObjectResponse = monitor.initialResponse().get();
        long bytesTransferred = progress.snapshot().bytesTransferred();
        return ResumableFileDownload.builder()
                                    .downloadFileRequest(monitor.downloadFileRequest())
                                    .lastModified(getObjectResponse.lastModified())
                                    .bytesTransferred(bytesTransferred)
                                    .build();
    }

    private void validatePauseStatus() {
        if (paused) {
            throw TransferPauseException.create(PAUSE_IN_PROGRESS,
                                                "Pause failed because a previous pause was requested");
        }

        if (monitor.isFinished()) {
            throw TransferPauseException.create(ALREADY_FINISHED, "Pause failed because the transfer has already finished");
        }

        if (!monitor.initialResponse().isPresent()) {
            throw TransferPauseException.create(NOT_STARTED, "Pause failed because the transfer has not been initiated yet. "
                                                             + "Please try later.");
        }
    }

    @Override
    public CompletableFuture<CompletedFileDownload> completionFuture() {
        return completionFuture;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultFileDownload that = (DefaultFileDownload) o;

        if (!Objects.equals(completionFuture, that.completionFuture)) {
            return false;
        }

        if (!Objects.equals(monitor, that.monitor)) {
            return false;
        }

        return Objects.equals(progress, that.progress);
    }

    @Override
    public int hashCode() {
        int result = completionFuture != null ? completionFuture.hashCode() : 0;
        result = 31 * result + (monitor != null ? monitor.hashCode() : 0);
        result = 31 * result + (progress != null ? progress.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("DefaultFileDownload")
                       .add("completionFuture", completionFuture)
                       .add("progress", progress)
                       .add("monitor", monitor)
                       .build();
    }
}
