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

package software.amazon.awssdk.transfer.s3.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Represents a completed upload directory transfer to Amazon S3. It can be used to track
 * failed single file uploads.
 *
 * @see S3TransferManager#uploadDirectory(UploadDirectoryRequest)
 */
@SdkPublicApi
public final class CompletedDirectoryUpload implements CompletedDirectoryTransfer,
                                                       ToCopyableBuilder<CompletedDirectoryUpload.Builder,
                                                           CompletedDirectoryUpload> {
    
    private final List<FailedFileUpload> failedTransfers;

    private CompletedDirectoryUpload(DefaultBuilder builder) {
        this.failedTransfers = Collections.unmodifiableList(
            new ArrayList<>(Validate.paramNotNull(builder.failedTransfers, "failedTransfers")));
    }
    
    @Override
    public List<FailedFileUpload> failedTransfers() {
        return failedTransfers;
    }

    /**
     * Creates a default builder for {@link CompletedDirectoryUpload}.
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

        CompletedDirectoryUpload that = (CompletedDirectoryUpload) o;

        return Objects.equals(failedTransfers, that.failedTransfers);
    }

    @Override
    public int hashCode() {
        return failedTransfers != null ? failedTransfers.hashCode() : 0;
    }

    @Override
    public String toString() {
        return ToString.builder("CompletedDirectoryUpload")
                       .add("failedTransfers", failedTransfers)
                       .build();
    }

    public static Class<? extends Builder> serializableBuilderClass() {
        return DefaultBuilder.class;
    }

    @Override
    public Builder toBuilder() {
        return new DefaultBuilder(this);
    }

    public interface Builder extends CopyableBuilder<CompletedDirectoryUpload.Builder,
        CompletedDirectoryUpload> {

        /**
         * Sets a collection of {@link FailedFileUpload}s
         *
         * @param failedTransfers failed uploads
         * @return This builder for method chaining.
         */
        Builder failedTransfers(Collection<FailedFileUpload> failedTransfers);

        /**
         * Adds a {@link FailedFileUpload}
         *
         * @param failedTransfer failed upload
         * @return This builder for method chaining.
         */
        Builder addFailedTransfer(FailedFileUpload failedTransfer);

        /**
         * Builds a {@link CompletedDirectoryUpload} based on the properties supplied to this builder
         * @return An initialized {@link CompletedDirectoryUpload}
         */
        CompletedDirectoryUpload build();
    }

    private static final class DefaultBuilder implements Builder {
        private Collection<FailedFileUpload> failedTransfers = new ArrayList<>();

        private DefaultBuilder() {
        }

        private DefaultBuilder(CompletedDirectoryUpload completedDirectoryUpload) {
            this.failedTransfers = new ArrayList<>(completedDirectoryUpload.failedTransfers);
        }

        @Override
        public Builder failedTransfers(Collection<FailedFileUpload> failedTransfers) {
            this.failedTransfers = new ArrayList<>(failedTransfers);
            return this;
        }

        @Override
        public Builder addFailedTransfer(FailedFileUpload failedTransfer) {
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
        public CompletedDirectoryUpload build() {
            return new CompletedDirectoryUpload(this);
        }
    }
}
