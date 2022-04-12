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

import java.io.File;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.transfer.s3.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.FileDownload;
import software.amazon.awssdk.transfer.s3.ResumableFileDownload;
import software.amazon.awssdk.transfer.s3.progress.TransferProgress;
import software.amazon.awssdk.transfer.s3.progress.TransferProgressSnapshot;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class DefaultFileDownload implements FileDownload {
    private static final Logger log = Logger.loggerFor(FileDownload.class);
    private final CompletableFuture<CompletedFileDownload> completionFuture;
    private final CompletableFuture<TransferProgress> progressFuture;
    private final CompletableFuture<DownloadFileRequest> requestFuture;
    private volatile ResumableFileDownload resumableFileDownload;
    private final Object lock = new Object();

    DefaultFileDownload(CompletableFuture<CompletedFileDownload> completedFileDownloadFuture,
                        CompletableFuture<TransferProgress> progressFuture,
                        CompletableFuture<DownloadFileRequest> requestFuture) {
        this.completionFuture = Validate.paramNotNull(completedFileDownloadFuture, "completedFileDownloadFuture");
        this.progressFuture = Validate.paramNotNull(progressFuture, "progressFuture");
        this.requestFuture = Validate.paramNotNull(requestFuture, "requestFuture");
    }

    @Override
    public CompletableFuture<TransferProgress> progress() {
        return progressFuture;
    }

    @Override
    public ResumableFileDownload pause() {
        log.debug(() -> "Start to pause ");
        if (resumableFileDownload == null) {
            synchronized (lock) {
                if (resumableFileDownload == null) {
                    completionFuture.cancel(true);

                    if (!requestFuture.isDone() || !progressFuture.isDone()) {
                        throw SdkClientException.create("DownloadFileRequest is unknown, not able to pause. This is likely "
                                                        + "because you are trying to pause a resumed download request that "
                                                        + "hasn't started yet. Please try later");
                    }
                    DownloadFileRequest request = requestFuture.join();
                    TransferProgress progress = progressFuture.join();

                    Instant s3objectLastModified = null;
                    Long totalBytesTransferred = null;
                    TransferProgressSnapshot snapshot = progress.snapshot();
                    if (snapshot.sdkResponse().isPresent() && snapshot.sdkResponse().get() instanceof GetObjectResponse) {
                        GetObjectResponse getObjectResponse = (GetObjectResponse) snapshot.sdkResponse().get();
                        s3objectLastModified = getObjectResponse.lastModified();
                        totalBytesTransferred = getObjectResponse.contentLength();
                    }
                    File destination = request.destination().toFile();
                    long length = destination.length();
                    Instant fileLastModified = Instant.ofEpochMilli(destination.lastModified());
                    resumableFileDownload = ResumableFileDownload.builder()
                                                                 .downloadFileRequest(request)
                                                                 .s3ObjectLastModified(s3objectLastModified)
                                                                 .fileLastModified(fileLastModified)
                                                                 .bytesTransferred(length)
                                                                 .totalSizeInBytes(totalBytesTransferred)
                                                                 .build();
                }

            }
        }
        return resumableFileDownload;
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

        if (!Objects.equals(requestFuture, that.requestFuture)) {
            return false;
        }

        return Objects.equals(progressFuture, that.progressFuture);
    }

    @Override
    public int hashCode() {
        int result = completionFuture != null ? completionFuture.hashCode() : 0;
        result = 31 * result + (requestFuture != null ? requestFuture.hashCode() : 0);
        result = 31 * result + (progressFuture != null ? progressFuture.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("DefaultFileDownload")
                       .add("completionFuture", completionFuture)
                       .add("progress", progressFuture)
                       .add("request", requestFuture)
                       .build();
    }
}
