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
import software.amazon.awssdk.transfer.s3.model.CompletedDirectoryDownload;
import software.amazon.awssdk.transfer.s3.model.DirectoryDownload;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class DefaultDirectoryDownload implements DirectoryDownload {

    private final CompletableFuture<CompletedDirectoryDownload> completionFuture;

    public DefaultDirectoryDownload(CompletableFuture<CompletedDirectoryDownload> completionFuture) {
        this.completionFuture = Validate.paramNotNull(completionFuture, "completionFuture");
    }

    @Override
    public CompletableFuture<CompletedDirectoryDownload> completionFuture() {
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

        DefaultDirectoryDownload that = (DefaultDirectoryDownload) o;

        return Objects.equals(completionFuture, that.completionFuture);
    }

    @Override
    public int hashCode() {
        return completionFuture != null ? completionFuture.hashCode() : 0;
    }

    @Override
    public String toString() {
        return ToString.builder("DefaultDirectoryDownload")
                       .add("completionFuture", completionFuture)
                       .build();
    }
}
