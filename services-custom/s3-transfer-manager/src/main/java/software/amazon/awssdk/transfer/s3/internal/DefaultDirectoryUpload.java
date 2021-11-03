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

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkPreviewApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.transfer.s3.CompletedDirectoryUpload;
import software.amazon.awssdk.transfer.s3.DirectoryUpload;
import software.amazon.awssdk.utils.ToString;

@SdkPublicApi
@SdkPreviewApi
public final class DefaultDirectoryUpload implements DirectoryUpload {
    
    private final CompletableFuture<CompletedDirectoryUpload> completionFuture;

    DefaultDirectoryUpload(CompletableFuture<CompletedDirectoryUpload> completionFuture) {
        this.completionFuture = completionFuture;
    }

    @Override
    public CompletableFuture<CompletedDirectoryUpload> completionFuture() {
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

        DefaultDirectoryUpload that = (DefaultDirectoryUpload) o;

        return Objects.equals(completionFuture, that.completionFuture);
    }

    @Override
    public int hashCode() {
        return completionFuture != null ? completionFuture.hashCode() : 0;
    }

    @Override
    public String toString() {
        return ToString.builder("DefaultDirectoryUpload")
                       .add("completionFuture", completionFuture)
                       .build();
    }
}
