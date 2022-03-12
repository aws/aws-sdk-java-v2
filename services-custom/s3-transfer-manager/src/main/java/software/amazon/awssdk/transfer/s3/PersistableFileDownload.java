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
public final class PersistableFileDownload implements PersistableTransfer,
                                                      ToCopyableBuilder<PersistableFileDownload.Builder, PersistableFileDownload> {
    private final DownloadFileRequest downloadFileRequest;
    private final long bytesTransferred;
    private final Instant lastModified;

    private PersistableFileDownload(DefaultBuilder builder) {
        this.downloadFileRequest = builder.downloadFileRequest;
        this.bytesTransferred = Validate.paramNotNull(builder.bytesTransferred, "bytesTransferred");
        this.lastModified = Validate.paramNotNull(builder.lastModified, "lastModified");
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

    @Override
    public Builder toBuilder() {
        return new DefaultBuilder(this);
    }

    public interface Builder extends CopyableBuilder<Builder, PersistableFileDownload> {

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

        private DefaultBuilder() {

        }

        private DefaultBuilder(PersistableFileDownload persistableFileDownload) {
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
        public Builder lastModified(Instant lastModified) {
            this.lastModified = lastModified;
            return this;
        }

        @Override
        public PersistableFileDownload build() {
            return new PersistableFileDownload(this);
        }
    }
}
