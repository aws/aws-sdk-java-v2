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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.transfer.s3.CompletedUpload;
import software.amazon.awssdk.transfer.s3.CompletedUploadDirectory;
import software.amazon.awssdk.transfer.s3.FailedUpload;
import software.amazon.awssdk.utils.ToString;

@SdkInternalApi
public final class DefaultCompletedUploadDirectory implements CompletedUploadDirectory {
    private final List<CompletedUpload> completedUploads;
    private final List<FailedUpload> failedUploads;

    private DefaultCompletedUploadDirectory(DefaultBuilder builder) {
        this.completedUploads = Collections.unmodifiableList(new ArrayList<>(builder.completedUploads));
        this.failedUploads = Collections.unmodifiableList(builder.failedObjects);
    }

    @Override
    public List<FailedUpload> failedUploads() {
        return failedUploads;
    }

    @Override
    public List<CompletedUpload> successfulObjects() {
        return completedUploads;
    }

    /**
     * Creates a default builder for {@link CompletedUpload}.
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultCompletedUploadDirectory that = (DefaultCompletedUploadDirectory) o;

        if (!completedUploads.equals(that.completedUploads)) {
            return false;
        }
        return failedUploads.equals(that.failedUploads);
    }

    @Override
    public int hashCode() {
        int result = completedUploads.hashCode();
        result = 31 * result + failedUploads.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("CompletedUploadDirectory")
                       .add("failedUploads", failedUploads)
                       .add("successfulUploads", completedUploads)
                       .build();
    }

    interface Builder {

        Builder successfulUploads(List<CompletedUpload> successfulUploads);

        Builder failedUploads(List<FailedUpload> failedUploads);

        CompletedUploadDirectory build();
    }

    private static class DefaultBuilder implements Builder {
        private List<CompletedUpload> completedUploads = Collections.emptyList();
        private List<FailedUpload> failedObjects = Collections.emptyList();

        @Override
        public Builder successfulUploads(List<CompletedUpload> completedUploads) {
            this.completedUploads = completedUploads;
            return this;
        }

        @Override
        public Builder failedUploads(List<FailedUpload> failedUploads) {
            this.failedObjects = failedUploads;
            return this;
        }

        @Override
        public CompletedUploadDirectory build() {
            return new DefaultCompletedUploadDirectory(this);
        }
    }
}
