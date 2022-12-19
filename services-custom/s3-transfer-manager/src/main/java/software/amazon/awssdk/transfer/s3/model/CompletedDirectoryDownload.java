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
 * Represents a completed download directory transfer to Amazon S3. It can be used to track
 * failed single file downloads.
 *
 * @see S3TransferManager#downloadDirectory(DownloadDirectoryRequest)
 */
@SdkPublicApi
public final class CompletedDirectoryDownload implements CompletedDirectoryTransfer,
                                                         ToCopyableBuilder<CompletedDirectoryDownload.Builder,
                                                             CompletedDirectoryDownload> {

    private final List<FailedFileDownload> failedTransfers;

    private CompletedDirectoryDownload(DefaultBuilder builder) {
        this.failedTransfers = Collections.unmodifiableList(
            new ArrayList<>(Validate.paramNotNull(builder.failedTransfers, "failedTransfers")));
    }

    @Override
    public List<FailedFileDownload> failedTransfers() {
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

    @Override
    public Builder toBuilder() {
        return new DefaultBuilder(this);
    }

    public interface Builder extends CopyableBuilder<CompletedDirectoryDownload.Builder,
        CompletedDirectoryDownload>  {

        /**
         * Sets a collection of {@link FailedFileDownload}s
         *
         * @param failedTransfers failed download
         * @return This builder for method chaining.
         */
        Builder failedTransfers(Collection<FailedFileDownload> failedTransfers);

        /**
         * Adds a {@link FailedFileDownload}
         *
         * @param failedTransfer failed download
         * @return This builder for method chaining.
         */
        Builder addFailedTransfer(FailedFileDownload failedTransfer);

        /**
         * Builds a {@link CompletedDirectoryDownload} based on the properties supplied to this builder
         * @return An initialized {@link CompletedDirectoryDownload}
         */
        CompletedDirectoryDownload build();
    }

    private static final class DefaultBuilder implements Builder {
        private Collection<FailedFileDownload> failedTransfers = new ArrayList<>();

        private DefaultBuilder() {
        }

        private DefaultBuilder(CompletedDirectoryDownload completedDirectoryDownload) {
            this.failedTransfers = new ArrayList<>(completedDirectoryDownload.failedTransfers);
        }

        @Override
        public Builder failedTransfers(Collection<FailedFileDownload> failedTransfers) {
            this.failedTransfers = new ArrayList<>(failedTransfers);
            return this;
        }

        @Override
        public Builder addFailedTransfer(FailedFileDownload failedTransfer) {
            failedTransfers.add(failedTransfer);
            return this;
        }

        public Collection<FailedFileDownload> getFailedTransfers() {
            return Collections.unmodifiableCollection(failedTransfers);
        }

        public void setFailedTransfers(Collection<FailedFileDownload> failedTransfers) {
            failedTransfers(failedTransfers);
        }

        @Override
        public CompletedDirectoryDownload build() {
            return new CompletedDirectoryDownload(this);
        }
    }
}
