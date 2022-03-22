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

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.transfer.s3.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.FileDownload;
import software.amazon.awssdk.transfer.s3.ResumableFileDownload;
import software.amazon.awssdk.transfer.s3.progress.TransferProgress;
import software.amazon.awssdk.transfer.s3.progress.TransferProgressSnapshot;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.ToString;

@SdkInternalApi
public final class DefaultFileDownload implements FileDownload {
    private static final Logger log = Logger.loggerFor(FileDownload.class);
    private final CompletableFuture<CompletedFileDownload> completionFuture;
    private final TransferProgress progress;
    private final DownloadFileRequest request;
    private final AtomicReference<ResumableFileDownload> resumableFileDownload;

    DefaultFileDownload(CompletableFuture<CompletedFileDownload> completionFuture,
                        TransferProgress progress,
                        DownloadFileRequest request) {
        this.completionFuture = completionFuture;
        this.progress = progress;
        this.resumableFileDownload = new AtomicReference<>();
        this.request = request;
    }

    @Override
    public TransferProgress progress() {
        return progress;
    }

    @Override
    public ResumableFileDownload pause() {
        log.trace(() -> "Start to pause " + request);
        if (resumableFileDownload.get() == null) {
            completionFuture.cancel(false);

            Instant lastModified = null;
            Long totalBytesTransferred = null;
            TransferProgressSnapshot snapshot = progress.snapshot();
            if (snapshot.sdkResponse().isPresent() && snapshot.sdkResponse().get() instanceof GetObjectResponse) {
                GetObjectResponse getObjectResponse = (GetObjectResponse) snapshot.sdkResponse().get();
                lastModified = getObjectResponse.lastModified();
                totalBytesTransferred = getObjectResponse.contentLength();
            }

            long bytesTransferred = snapshot.bytesTransferred();
            ResumableFileDownload fileDownload = ResumableFileDownload.builder()
                                                                      .downloadFileRequest(request)
                                                                      .lastModified(lastModified)
                                                                      .bytesTransferred(bytesTransferred)
                                                                      .transferSizeInBytes(totalBytesTransferred)
                                                                      .build();
            resumableFileDownload.set(fileDownload);
        }
        return resumableFileDownload.get();
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

        if (!Objects.equals(request, that.request)) {
            return false;
        }

        return Objects.equals(progress, that.progress);
    }

    @Override
    public int hashCode() {
        int result = completionFuture != null ? completionFuture.hashCode() : 0;
        result = 31 * result + (request != null ? request.hashCode() : 0);
        result = 31 * result + (progress != null ? progress.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("DefaultFileDownload")
                       .add("completionFuture", completionFuture)
                       .add("progress", progress)
                       .add("request", request)
                       .build();
    }
}
