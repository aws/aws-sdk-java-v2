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
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.s3.multipart.PauseObservable;
import software.amazon.awssdk.services.s3.multipart.S3ResumeToken;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedFileUpload;
import software.amazon.awssdk.transfer.s3.model.FileUpload;
import software.amazon.awssdk.transfer.s3.model.ResumableFileUpload;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;
import software.amazon.awssdk.transfer.s3.progress.TransferProgress;
import software.amazon.awssdk.utils.Lazy;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class DefaultFileUpload implements FileUpload {
    private static final Logger log = Logger.loggerFor(S3TransferManager.class);

    private final Lazy<ResumableFileUpload> resumableFileUpload;
    private final CompletableFuture<CompletedFileUpload> completionFuture;
    private final TransferProgress progress;
    private final UploadFileRequest request;
    private final PauseObservable pauseObservable;

    public DefaultFileUpload(CompletableFuture<CompletedFileUpload> completionFuture,
                             TransferProgress progress,
                             PauseObservable pauseObservable,
                             UploadFileRequest request) {
        this.completionFuture = Validate.paramNotNull(completionFuture, "completionFuture");
        this.progress = Validate.paramNotNull(progress, "progress");
        this.request = Validate.paramNotNull(request, "request");
        this.pauseObservable = pauseObservable;
        this.resumableFileUpload = new Lazy<>(this::doPause);
    }

    @Override
    public ResumableFileUpload pause() {
        if (pauseObservable == null) {
            throw new UnsupportedOperationException("Pausing an upload is not supported in a non CRT-based S3Client that does "
                                                    + "not have multipart configuration enabled. For upload pause support, pass "
                                                    + "a CRT-based S3Client or an S3Client with multipart enabled to "
                                                    + "S3TransferManager.");
        }

        return resumableFileUpload.getValue();
    }

    private ResumableFileUpload doPause() {
        File sourceFile = request.source().toFile();
        Instant fileLastModified = Instant.ofEpochMilli(sourceFile.lastModified());

        ResumableFileUpload.Builder resumableFileBuilder = ResumableFileUpload.builder()
                                                                              .fileLastModified(fileLastModified)
                                                                              .fileLength(sourceFile.length())
                                                                              .uploadFileRequest(request);

        boolean futureCompletedExceptionally = completionFuture.isCompletedExceptionally();
        if (completionFuture.isDone() && !futureCompletedExceptionally) {
            log.debug(() -> "The upload future was finished and was not completed exceptionally. There will be no S3ResumeToken "
                            + "returned.");

            return resumableFileBuilder.build();
        }

        S3ResumeToken token = pauseObservable.pause();

        if (token == null) {
            log.debug(() -> "The upload hasn't started yet, or it's a single object upload. There will be no S3ResumeToken "
                            + "returned.");
            return resumableFileBuilder.build();
        }

        if (futureCompletedExceptionally) {
            log.debug(() -> "The upload future was completed exceptionally but has been successfully paused and a S3ResumeToken "
                            + "was returned.");
        } else {
            log.debug(() -> "The upload was successfully paused and a S3ResumeToken was returned.");
        }

        return resumableFileBuilder.multipartUploadId(token.uploadId())
                                   .totalParts(token.totalNumParts())
                                   .transferredParts(token.numPartsCompleted())
                                   .partSizeInBytes(token.partSize())
                                   .build();
    }

    @Override
    public CompletableFuture<CompletedFileUpload> completionFuture() {
        return completionFuture;
    }

    @Override
    public TransferProgress progress() {
        return progress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultFileUpload that = (DefaultFileUpload) o;

        if (!resumableFileUpload.equals(that.resumableFileUpload)) {
            return false;
        }
        if (!completionFuture.equals(that.completionFuture)) {
            return false;
        }
        if (!progress.equals(that.progress)) {
            return false;
        }
        if (!request.equals(that.request)) {
            return false;
        }
        return pauseObservable == that.pauseObservable;
    }

    @Override
    public int hashCode() {
        int result = resumableFileUpload.hashCode();
        result = 31 * result + completionFuture.hashCode();
        result = 31 * result + progress.hashCode();
        result = 31 * result + request.hashCode();
        result = 31 * result + pauseObservable.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("DefaultFileUpload")
                       .add("completionFuture", completionFuture)
                       .add("progress", progress)
                       .add("request", request)
                       .build();
    }
}
