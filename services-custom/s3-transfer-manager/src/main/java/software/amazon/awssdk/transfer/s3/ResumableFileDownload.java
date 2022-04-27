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

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * An opaque token that holds the state and can be used to resume a
 * paused download operation.
 *
 * @see S3TransferManager#downloadFile(DownloadFileRequest)
 */
@SdkPublicApi
public final class ResumableFileDownload implements ResumableTransfer,
                                                    ToCopyableBuilder<ResumableFileDownload.Builder, ResumableFileDownload> {
    private final DownloadFileRequest downloadFileRequest;
    private final long bytesTransferred;
    private final Instant s3ObjectLastModified;
    private final Long totalSizeInBytes;
    private final Instant fileLastModified;

    private ResumableFileDownload(DefaultBuilder builder) {
        this.downloadFileRequest = Validate.paramNotNull(builder.downloadFileRequest, "downloadFileRequest");
        Validate.isPositiveOrNull(builder.bytesTransferred, "bytesTransferred");
        this.bytesTransferred = builder.bytesTransferred == null ? 0 : builder.bytesTransferred;
        this.s3ObjectLastModified = builder.s3ObjectLastModified;
        this.totalSizeInBytes = Validate.isPositiveOrNull(builder.totalSizeInBytes, "totalSizeInBytes");
        this.fileLastModified = builder.fileLastModified;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ResumableFileDownload that = (ResumableFileDownload) o;

        if (bytesTransferred != that.bytesTransferred) {
            return false;
        }
        if (!downloadFileRequest.equals(that.downloadFileRequest)) {
            return false;
        }
        if (!Objects.equals(s3ObjectLastModified, that.s3ObjectLastModified)) {
            return false;
        }
        if (!Objects.equals(fileLastModified, that.fileLastModified)) {
            return false;
        }
        return Objects.equals(totalSizeInBytes, that.totalSizeInBytes);
    }

    @Override
    public int hashCode() {
        int result = downloadFileRequest.hashCode();
        result = 31 * result + (int) (bytesTransferred ^ (bytesTransferred >>> 32));
        result = 31 * result + (s3ObjectLastModified != null ? s3ObjectLastModified.hashCode() : 0);
        result = 31 * result + (fileLastModified != null ? fileLastModified.hashCode() : 0);
        result = 31 * result + (totalSizeInBytes != null ? totalSizeInBytes.hashCode() : 0);
        return result;
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }

    /**
     * @return the {@link DownloadFileRequest} to resume
     */
    public DownloadFileRequest downloadFileRequest() {
        return downloadFileRequest;
    }

    /**
     * Retrieve the number of bytes that have been transferred.
     * @return the number of bytes
     */
    public long bytesTransferred() {
        return bytesTransferred;
    }

    /**
     * Last modified time of the S3 object since last pause, or {@link Optional#empty()} if unknown
     */
    public Optional<Instant> s3ObjectLastModified() {
        return Optional.ofNullable(s3ObjectLastModified);
    }

    /**
     * Last modified time of the file since last pause
     */
    public Instant fileLastModified() {
        return fileLastModified;
    }

    /**
     * The total size of the transfer in bytes, or {@link Optional#empty()} if unknown
     *
     * @return the optional total size of the transfer.
     */
    public Optional<Long> totalSizeInBytes() {
        return Optional.ofNullable(totalSizeInBytes);
    }

    @Override
    public Builder toBuilder() {
        return new DefaultBuilder(this);
    }

    public interface Builder extends CopyableBuilder<Builder, ResumableFileDownload> {

        /**
         * Sets the download file request
         *
         * @param downloadFileRequest the download file request
         * @return a reference to this object so that method calls can be chained together.
         */
        Builder downloadFileRequest(DownloadFileRequest downloadFileRequest);

        /**
         * Sets the number of bytes transferred
         *
         * @param bytesTransferred the number of bytes transferred
         * @return a reference to this object so that method calls can be chained together.
         */
        Builder bytesTransferred(Long bytesTransferred);

        /**
         * Sets the total transfer size in bytes
         * @param totalSizeInBytes the transfer size in bytes
         * @return a reference to this object so that method calls can be chained together.
         */
        Builder totalSizeInBytes(Long totalSizeInBytes);

        /**
         * Sets the last modified time of the object
         *
         * @param s3ObjectLastModified the last modified time of the object
         * @return a reference to this object so that method calls can be chained together.
         */
        Builder s3ObjectLastModified(Instant s3ObjectLastModified);

        /**
         * Sets the last modified time of the object
         *
         * @param lastModified the last modified time of the object
         * @return a reference to this object so that method calls can be chained together.
         */
        Builder fileLastModified(Instant lastModified);
    }

    private static final class DefaultBuilder implements Builder {

        private DownloadFileRequest downloadFileRequest;
        private Long bytesTransferred;
        private Instant s3ObjectLastModified;
        private Long totalSizeInBytes;
        private Instant fileLastModified;

        private DefaultBuilder() {
        }

        private DefaultBuilder(ResumableFileDownload persistableFileDownload) {
            this.downloadFileRequest = persistableFileDownload.downloadFileRequest;
            this.bytesTransferred = persistableFileDownload.bytesTransferred;
            this.s3ObjectLastModified = persistableFileDownload.s3ObjectLastModified;
        }

        @Override
        public Builder downloadFileRequest(DownloadFileRequest downloadFileRequest) {
            this.downloadFileRequest = downloadFileRequest;
            return this;
        }

        @Override
        public Builder bytesTransferred(Long bytesTransferred) {
            this.bytesTransferred = bytesTransferred;
            return this;
        }

        @Override
        public Builder totalSizeInBytes(Long totalSizeInBytes) {
            this.totalSizeInBytes = totalSizeInBytes;
            return this;
        }

        @Override
        public Builder s3ObjectLastModified(Instant s3ObjectLastModified) {
            this.s3ObjectLastModified = s3ObjectLastModified;
            return this;
        }

        @Override
        public Builder fileLastModified(Instant fileLastModified) {
            this.fileLastModified = fileLastModified;
            return this;
        }

        @Override
        public ResumableFileDownload build() {
            return new ResumableFileDownload(this);
        }
    }
}
