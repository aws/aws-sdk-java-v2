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
import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPreviewApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * Represents a completed download directory transfer to Amazon S3. It can be used to track
 * failed single file downloads.
 *
 * @see S3TransferManager#uploadDirectory(UploadDirectoryRequest)
 */
@SdkPublicApi
@SdkPreviewApi
public final class CompletedDirectoryDownload implements CompletedDirectoryTransfer {

    private final Collection<FailedFileUpload> failedTransfers;

    private CompletedDirectoryDownload(DefaultBuilder builder) {
        this.failedTransfers = Collections.unmodifiableCollection(
            Validate.paramNotNull(builder.failedTransfers, "failedTransfers"));
    }

    @Override
    public Collection<FailedFileUpload> failedTransfers() {
        return failedTransfers;
    }

    /**
     * Creates a default builder for {@link CompletedDirectoryDownload}.
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

        CompletedDirectoryDownload that = (CompletedDirectoryDownload) o;

        return Objects.equals(failedTransfers, that.failedTransfers);
    }

    @Override
    public int hashCode() {
        return failedTransfers != null ? failedTransfers.hashCode() : 0;
    }

    @Override
    public String toString() {
        return ToString.builder("CompletedDirectoryDownload")
                       .add("failedTransfers", failedTransfers)
                       .build();
    }

    public static Class<? extends Builder> serializableBuilderClass() {
        return DefaultBuilder.class;
    }

    public interface Builder {

        /**
         * Sets a collection of {@link FailedFileUpload}s
         *
         * @param failedTransfers failed uploads
         * @return This builder for method chaining.
         */
        Builder failedTransfers(Collection<FailedFileUpload> failedTransfers);

        /**
         * Add a {@link FailedFileUpload}
         *
         * @param failedTransfer failed upload
         * @return This builder for method chaining.
         */
        Builder addFailedTransfer(FailedFileUpload failedTransfer);

        /**
         * Builds a {@link CompletedDirectoryDownload} based on the properties supplied to this builder
         * @return An initialized {@link CompletedDirectoryDownload}
         */
        CompletedDirectoryDownload build();
    }

    private static final class DefaultBuilder implements Builder {
        private Collection<FailedFileUpload> failedTransfers;

        @Override
        public Builder failedTransfers(Collection<FailedFileUpload> failedTransfers) {
            this.failedTransfers = new ArrayList<>(failedTransfers);
            return this;
        }

        @Override
        public Builder addFailedTransfer(FailedFileUpload failedTransfer) {
            if (failedTransfers == null) {
                failedTransfers = new ArrayList<>();
            }
            failedTransfers.add(failedTransfer);
            return this;
        }

        public Collection<FailedFileUpload> getFailedTransfers() {
            return Collections.unmodifiableCollection(failedTransfers);
        }

        public void setFailedTransfers(Collection<FailedFileUpload> failedTransfers) {
            failedTransfers(failedTransfers);
        }

        @Override
        public CompletedDirectoryDownload build() {
            return new CompletedDirectoryDownload(this);
        }
    }
}
