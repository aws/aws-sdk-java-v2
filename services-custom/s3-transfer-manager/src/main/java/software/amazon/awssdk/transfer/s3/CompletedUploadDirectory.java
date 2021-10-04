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

package software.amazon.awssdk.transfer.s3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.SdkPreviewApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * Represents a completed upload directory transfer to Amazon S3. It can be used to track
 * failed single file uploads.
 *
 * @see S3TransferManager#uploadDirectory(UploadDirectoryRequest)
 */
@SdkPublicApi
@SdkPreviewApi
public final class CompletedUploadDirectory implements CompletedTransfer {
    private final List<FailedSingleFileUpload> failedUploads;

    private CompletedUploadDirectory(DefaultBuilder builder) {
        this.failedUploads = Collections.unmodifiableList(new ArrayList<>(Validate.paramNotNull(builder.failedUploads,
                                                                                                "failedUploads")));
    }

    /**
     * An immutable collection of failed uploads with error details, request metadata about each file that is failed to
     * upload.
     *
     * @return a list of failed uploads
     */
    public Collection<FailedSingleFileUpload> failedUploads() {
        return failedUploads;
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

        CompletedUploadDirectory that = (CompletedUploadDirectory) o;

        return failedUploads.equals(that.failedUploads);
    }

    @Override
    public int hashCode() {
        return failedUploads.hashCode();
    }

    @Override
    public String toString() {
        return ToString.builder("CompletedUploadDirectory")
                       .add("failedUploads", failedUploads)
                       .build();
    }

    public interface Builder {

        /**
         * Sets a collection of {@link FailedSingleFileUpload}s
         *
         * @param failedUploads failed uploads
         * @return This builder for method chaining.
         */
        Builder failedUploads(Collection<FailedSingleFileUpload> failedUploads);

        /**
         * Builds a {@link CompletedUploadDirectory} based on the properties supplied to this builder
         * @return An initialized {@link CompletedUploadDirectory}
         */
        CompletedUploadDirectory build();
    }

    private static final class DefaultBuilder implements Builder {
        private Collection<FailedSingleFileUpload> failedUploads = Collections.emptyList();

        private DefaultBuilder() {
        }

        @Override
        public Builder failedUploads(Collection<FailedSingleFileUpload> failedUploads) {
            this.failedUploads = failedUploads;
            return this;
        }

        public Collection<FailedSingleFileUpload> getFailedUploads() {
            return failedUploads;
        }

        public void setFailedUploads(Collection<FailedSingleFileUpload> failedUploads) {
            failedUploads(failedUploads);
        }

        @Override
        public CompletedUploadDirectory build() {
            return new CompletedUploadDirectory(this);
        }
    }
}
