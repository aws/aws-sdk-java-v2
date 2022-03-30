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
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * An opaque token that holds the state and can be used to resume a
 * paused download operation.
 *
 * TODO: 1. should we just store GetObjectResponse?
 *   2. consider providing a way to serialize and deserialize the token
 *   3. Do we need to store file checksum?
 *
 * @see S3TransferManager#downloadFile(DownloadFileRequest)
 */
@SdkPublicApi
public final class ResumableFileDownload implements ResumableTransfer,
                                                    ToCopyableBuilder<ResumableFileDownload.Builder, ResumableFileDownload> {
    private final DownloadFileRequest downloadFileRequest;
    private final long bytesTransferred;
    private final Instant lastModified;
    private final Long transferSizeInBytes;

    private ResumableFileDownload(DefaultBuilder builder) {
        this.downloadFileRequest = Validate.paramNotNull(builder.downloadFileRequest, "downloadFileRequest");
        this.bytesTransferred = builder.bytesTransferred == null ? 0 : builder.bytesTransferred;
        this.lastModified = builder.lastModified;
        this.transferSizeInBytes = builder.transferSizeInBytes;
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
        if (!Objects.equals(lastModified, that.lastModified)) {
            return false;
        }
        return Objects.equals(transferSizeInBytes, that.transferSizeInBytes);
    }

    @Override
    public int hashCode() {
        int result = downloadFileRequest.hashCode();
        result = 31 * result + (int) (bytesTransferred ^ (bytesTransferred >>> 32));
        result = 31 * result + (lastModified != null ? lastModified.hashCode() : 0);
        result = 31 * result + (transferSizeInBytes != null ? transferSizeInBytes.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("ResumableFileDownload")
                       .add("downloadFileRequest", downloadFileRequest)
                       .add("bytesTransferred", bytesTransferred)
                       .add("lastModified", lastModified)
                       .add("transferSizeInBytes", transferSizeInBytes)
                       .build();
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
     * Last modified time on Amazon S3 for this object.
     */
    public Instant lastModified() {
        return lastModified;
    }

    /**
     * The total size of the transfer in bytes, or {@link Optional#empty()} if unknown
     *
     * @return the optional total size of the transfer.
     */
    public Optional<Long> transferSizeInBytes() {
        return Optional.ofNullable(transferSizeInBytes);
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
         * @param transferSizeInBytes the transfer size in bytes
         * @return a reference to this object so that method calls can be chained together.
         */
        Builder transferSizeInBytes(Long transferSizeInBytes);

        /**
         * Sets the last modified time of the object
         *
         * @param lastModified the last modified time of the object
         * @return a reference to this object so that method calls can be chained together.
         */
        Builder lastModified(Instant lastModified);
    }

    private static final class DefaultBuilder implements Builder {
        private DownloadFileRequest downloadFileRequest;
        private Long bytesTransferred;
        private Instant lastModified;
        private Long transferSizeInBytes;

        private DefaultBuilder() {

        }

        private DefaultBuilder(ResumableFileDownload persistableFileDownload) {
            this.downloadFileRequest = persistableFileDownload.downloadFileRequest;
            this.bytesTransferred = persistableFileDownload.bytesTransferred;
            this.lastModified = persistableFileDownload.lastModified;
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
        public Builder transferSizeInBytes(Long transferSizeInBytes) {
            this.transferSizeInBytes = transferSizeInBytes;
            return this;
        }

        @Override
        public Builder lastModified(Instant lastModified) {
            this.lastModified = lastModified;
            return this;
        }

        @Override
        public ResumableFileDownload build() {
            return new ResumableFileDownload(this);
        }
    }
}
