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
import java.util.function.Supplier;
import software.amazon.awssdk.crt.CrtRuntimeException;
import software.amazon.awssdk.crt.s3.ResumeToken;
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
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

public class CrtFileDownload implements FileDownload {
    private static final Logger log = Logger.loggerFor(CrtFileDownload.class);

    private final CompletableFuture<CompletedFileDownload> completionFuture;
    private final Lazy<ResumableFileDownload> resumableFileDownload;
    private final TransferProgress progress;
    private final Supplier<DownloadFileRequest> requestSupplier;
    private final ResumableFileDownload resumedDownload;
    private final S3MetaRequestPauseObservable observable;

    public CrtFileDownload(CompletableFuture<CompletedFileDownload> completedFileDownloadFuture,
                           TransferProgress progress,
                           Supplier<DownloadFileRequest> requestSupplier,
                           S3MetaRequestPauseObservable observable,
                           ResumableFileDownload resumedDownload) {
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
        DownloadFileRequest request = requestSupplier.get();

        File destinationFile = request.destination().toFile();

        Instant s3objectLastModified = null;
        Long totalSizeInBytes = null;
        TransferProgressSnapshot snapshot = progress.snapshot();

        // TODO: Is this an issue when the file has not been created yet?
        long length = destinationFile.length();
        Instant fileLastModified = Instant.ofEpochMilli(destinationFile.lastModified());

        // TODO: I think the SDK response will NEVER be set unless we have actually completed
        // that might be okay for our usage?
        if (snapshot.sdkResponse().isPresent() && snapshot.sdkResponse().get() instanceof GetObjectResponse) {
            GetObjectResponse getObjectResponse = (GetObjectResponse) snapshot.sdkResponse().get();
            s3objectLastModified = getObjectResponse.lastModified();
            totalSizeInBytes = getObjectResponse.contentLength();
        } else if (resumedDownload != null) {
            s3objectLastModified = resumedDownload.s3ObjectLastModified().orElse(null);
            totalSizeInBytes = resumedDownload.totalSizeInBytes().isPresent() ? resumedDownload.totalSizeInBytes().getAsLong() : null;
        }

        boolean futureCompletedExceptionally = completionFuture.isCompletedExceptionally();

        if (completionFuture.isDone() && !futureCompletedExceptionally) {
            log.debug(() -> "The download future was completed. There will be no ResumeToken returned.");

            return ResumableFileDownload.builder()
                                        .downloadFileRequest(request)
                                        .s3ObjectLastModified(s3objectLastModified)
                                        .fileLastModified(fileLastModified)
                                        .totalSizeInBytes(totalSizeInBytes)
                                        .build();
        }

        ResumeToken token = null;
        try {
            token = observable.pause();
        } catch (CrtRuntimeException exception) {
            // CRT throws exception if it is a single part
            if (!exception.errorName.equals("AWS_ERROR_UNSUPPORTED_OPERATION")) {
                throw exception;
            }
        }

        completionFuture.cancel(true);

        if (token == null) {
            if (futureCompletedExceptionally) {
                log.debug(() -> "The download future was completed exceptionally and the ResumeToken returned by the "
                                + "S3 MetaRequest was null.");
            } else {
                log.debug(() -> "The download hasn't started yet or it's a single object upload. There will be no ResumeToken "
                                + "returned");
            }

            return ResumableFileDownload.builder()
                                        .downloadFileRequest(request)
                                        .s3ObjectLastModified(s3objectLastModified)
                                        .fileLastModified(fileLastModified)
                                        .totalSizeInBytes(totalSizeInBytes)
                                        .build();
        }

        // TODO: We only get the NUMBER of parts downloaded, not the actual numbers
        System.out.println("Resume Token: " +  token);

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
        return ToString.builder("DefaultFileDownload")
                       .add("completionFuture", completionFuture)
                       .add("progress", progress)
                       .add("request", requestSupplier.get())
                       .build();
    }
}
