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
import software.amazon.awssdk.crt.CrtRuntimeException;
import software.amazon.awssdk.crt.s3.ResumeToken;
import software.amazon.awssdk.services.s3.internal.crt.S3MetaRequestPauseObservable;
import software.amazon.awssdk.transfer.s3.internal.S3ClientType;
import software.amazon.awssdk.transfer.s3.model.CompletedFileUpload;
import software.amazon.awssdk.transfer.s3.model.FileUpload;
import software.amazon.awssdk.transfer.s3.model.ResumableFileUpload;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;
import software.amazon.awssdk.transfer.s3.progress.TransferProgress;
import software.amazon.awssdk.utils.Lazy;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class DefaultFileUpload implements FileUpload {
    private final Lazy<ResumableFileUpload> resumableFileUpload;
    private final CompletableFuture<CompletedFileUpload> completionFuture;
    private final TransferProgress progress;
    private final UploadFileRequest request;
    private final S3MetaRequestPauseObservable observable;
    private final S3ClientType clientType;

    public DefaultFileUpload(CompletableFuture<CompletedFileUpload> completionFuture,
                             TransferProgress progress,
                             S3MetaRequestPauseObservable observable,
                             UploadFileRequest request,
                             S3ClientType clientType) {
        this.completionFuture = Validate.paramNotNull(completionFuture, "completionFuture");
        this.progress = Validate.paramNotNull(progress, "progress");
        this.observable = Validate.paramNotNull(observable, "observable");
        this.request = Validate.paramNotNull(request, "request");
        this.clientType = Validate.paramNotNull(clientType, "clientType");
        this.resumableFileUpload = new Lazy<>(this::doPause);
    }

    @Override
    public ResumableFileUpload pause() {
        return resumableFileUpload.getValue();
    }

    private ResumableFileUpload doPause() {
        if (clientType != S3ClientType.CRT_BASED) {
            throw new UnsupportedOperationException("Pausing an upload is not supported in a non CRT-based S3 Client. For "
                                                    + "upload pause support, pass a CRT-based S3 client to S3TransferManager "
                                                    + "instead: S3AsyncClient.crtBuilder().build();");
        }

        File sourceFile = request.source().toFile();
        if (completionFuture.isDone()) {
            Instant fileLastModified = Instant.ofEpochMilli(sourceFile.lastModified());
            return ResumableFileUpload.builder()
                                      .fileLastModified(fileLastModified)
                                      .fileLength(sourceFile.length())
                                      .uploadFileRequest(request)
                                      .build();
        }


        Instant fileLastModified = Instant.ofEpochMilli(sourceFile
                                                            .lastModified());
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
        // Upload hasn't started yet, or it's a single object upload
        if (token == null) {
            return ResumableFileUpload.builder()
                                      .fileLastModified(fileLastModified)
                                      .fileLength(sourceFile.length())
                                      .uploadFileRequest(request)
                                      .build();
        }

        return ResumableFileUpload.builder()
                                  .multipartUploadId(token.getUploadId())
                                  .totalParts(token.getTotalNumParts())
                                  .transferredParts(token.getNumPartsCompleted())
                                  .partSizeInBytes(token.getPartSize())
                                  .fileLastModified(fileLastModified)
                                  .fileLength(sourceFile.length())
                                  .uploadFileRequest(request)
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
        if (clientType != that.clientType) {
            return false;
        }
        return observable == that.observable;
    }

    @Override
    public int hashCode() {
        int result = resumableFileUpload.hashCode();
        result = 31 * result + completionFuture.hashCode();
        result = 31 * result + progress.hashCode();
        result = 31 * result + request.hashCode();
        result = 31 * result + observable.hashCode();
        result = 31 * result + clientType.hashCode();
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
