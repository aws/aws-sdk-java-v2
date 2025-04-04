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

import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.s3.internal.crt.S3MetaRequestPauseObservable;
import software.amazon.awssdk.services.s3.internal.multipart.MultipartDownloadUtils;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.transfer.s3.model.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.FileDownload;
import software.amazon.awssdk.transfer.s3.model.ResumableFileDownload;
import software.amazon.awssdk.transfer.s3.progress.TransferProgress;
import software.amazon.awssdk.transfer.s3.progress.TransferProgressSnapshot;
import software.amazon.awssdk.utils.Lazy;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class CrtFileDownload implements FileDownload {
    private final CompletableFuture<CompletedFileDownload> completionFuture;
    private final Lazy<ResumableFileDownload> resumableFileDownload;
    private final TransferProgress progress;
    private final Supplier<DownloadFileRequest> requestSupplier;
    private final ResumableFileDownload resumedDownload;
    private final S3MetaRequestPauseObservable observable;

    public CrtFileDownload(CompletableFuture<CompletedFileDownload> completedFileDownloadFuture,
                           TransferProgress progress,
                           Supplier<DownloadFileRequest> requestSupplier,
                           ResumableFileDownload resumedDownload, S3MetaRequestPauseObservable observable) {
        this.completionFuture = Validate.paramNotNull(completedFileDownloadFuture, "completedFileDownloadFuture");
        this.progress = Validate.paramNotNull(progress, "progress");
        this.requestSupplier = Validate.paramNotNull(requestSupplier, "requestSupplier");
        this.observable = observable;
        this.resumableFileDownload = new Lazy<>(this::doPause);
        this.resumedDownload = resumedDownload;
    }

    @Override
    public TransferProgress progress() {
        return progress;
    }

    @Override
    public ResumableFileDownload pause() {
        return resumableFileDownload.getValue();
    }

    private ResumableFileDownload doPause() {
        observable.cancel();

        try {
            completionFuture.join();
        } catch (CompletionException e) {
            // TODO: Ensure that this was actually a "Request successfully cancelled"
        }

        Instant s3objectLastModified = null;
        Long totalSizeInBytes = null;
        TransferProgressSnapshot snapshot = progress.snapshot();

        if (snapshot.sdkResponse().isPresent() && snapshot.sdkResponse().get() instanceof GetObjectResponse) {
            GetObjectResponse getObjectResponse = (GetObjectResponse) snapshot.sdkResponse().get();
            s3objectLastModified = getObjectResponse.lastModified();
            totalSizeInBytes = getObjectResponse.contentLength();
        } else if (resumedDownload != null) {
            s3objectLastModified = resumedDownload.s3ObjectLastModified().orElse(null);
            totalSizeInBytes = resumedDownload.totalSizeInBytes().isPresent() ? resumedDownload.totalSizeInBytes().getAsLong()
                                                                              : null;
        }

        DownloadFileRequest request = requestSupplier.get();
        File destination = request.destination().toFile();
        long length = destination.length();
        Instant fileLastModified = Instant.ofEpochMilli(destination.lastModified());
        List<Integer> completedParts = MultipartDownloadUtils.completedParts(request.getObjectRequest());
        return ResumableFileDownload.builder()
                                    .downloadFileRequest(request)
                                    .s3ObjectLastModified(s3objectLastModified)
                                    .fileLastModified(fileLastModified)
                                    .bytesTransferred(length)
                                    .totalSizeInBytes(totalSizeInBytes)
                                    .completedParts(completedParts)
                                    .build();
    }

    @Override
    public CompletableFuture<CompletedFileDownload> completionFuture() {
        return completionFuture;
    }

    @Override
    public String toString() {
        return ToString.builder("CrtFileDownload")
                       .add("completionFuture", completionFuture)
                       .add("progress", progress)
                       .add("request", requestSupplier.get())
                       .build();
    }

}
