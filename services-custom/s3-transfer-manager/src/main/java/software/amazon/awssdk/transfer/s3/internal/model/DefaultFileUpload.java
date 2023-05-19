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
import software.amazon.awssdk.transfer.s3.model.CompletedFileUpload;
import software.amazon.awssdk.transfer.s3.model.FileUpload;
import software.amazon.awssdk.transfer.s3.model.ResumableFileUpload;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;
import software.amazon.awssdk.transfer.s3.progress.TransferProgress;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class DefaultFileUpload implements FileUpload {
    private final CompletableFuture<CompletedFileUpload> completionFuture;
    private final TransferProgress progress;
    private final UploadFileRequest request;

    public DefaultFileUpload(CompletableFuture<CompletedFileUpload> completionFuture,
                             TransferProgress progress,
                             UploadFileRequest request) {
        this.completionFuture = Validate.paramNotNull(completionFuture, "completionFuture");
        this.progress = Validate.paramNotNull(progress, "progress");
        this.request = Validate.paramNotNull(request, "request");
    }

    @Override
    public ResumableFileUpload pause() {
        throw new UnsupportedOperationException("Pausing an upload is not supported in a non CRT-based S3 Client. For "
                                                + "upload pause support, pass an AWS CRT-based S3 client to S3TransferManager"
                                                + "instead: S3AsyncClient.crtBuilder().build();");
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

        if (!completionFuture.equals(that.completionFuture)) {
            return false;
        }
        if (!progress.equals(that.progress)) {
            return false;
        }
        return request.equals(that.request);
    }

    @Override
    public int hashCode() {
        int result = completionFuture.hashCode();
        result = 31 * result + progress.hashCode();
        result = 31 * result + request.hashCode();
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
