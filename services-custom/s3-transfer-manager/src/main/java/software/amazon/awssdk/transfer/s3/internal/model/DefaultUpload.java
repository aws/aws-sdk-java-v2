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

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.transfer.s3.model.CompletedUpload;
import software.amazon.awssdk.transfer.s3.model.Upload;
import software.amazon.awssdk.transfer.s3.progress.TransferProgress;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class DefaultUpload implements Upload {
    
    private final CompletableFuture<CompletedUpload> completionFuture;
    private final TransferProgress progress;

    public DefaultUpload(CompletableFuture<CompletedUpload> completionFuture, TransferProgress progress) {
        this.completionFuture = Validate.paramNotNull(completionFuture, "completionFuture");
        this.progress = Validate.paramNotNull(progress, "progress");
    }

    @Override
    public CompletableFuture<CompletedUpload> completionFuture() {
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

        DefaultUpload that = (DefaultUpload) o;

        if (!Objects.equals(completionFuture, that.completionFuture)) {
            return false;
        }
        return Objects.equals(progress, that.progress);
    }

    @Override
    public int hashCode() {
        int result = completionFuture != null ? completionFuture.hashCode() : 0;
        result = 31 * result + (progress != null ? progress.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("DefaultUpload")
                       .add("completionFuture", completionFuture)
                       .add("progress", progress)
                       .build();
    }
}
