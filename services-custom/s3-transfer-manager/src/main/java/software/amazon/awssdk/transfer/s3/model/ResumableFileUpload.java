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

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.config.TransferRequestOverrideConfiguration;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * POJO class that holds the state and can be used to resume a paused upload file operation.
 * <p>
 * <b>Serialization: </b>When serializing this token, the following structures will not be preserved/persisted:
 * <ul>
 *     <li>{@link TransferRequestOverrideConfiguration}</li>
 *     <li>{@link AwsRequestOverrideConfiguration} (from {@link PutObjectRequest})</li>
 * </ul>
 *
 * @see S3TransferManager#uploadFile(UploadFileRequest)
 */
@SdkPublicApi
public final class ResumableFileUpload implements ResumableTransfer,
                                                  ToCopyableBuilder<ResumableFileUpload.Builder, ResumableFileUpload> {

    private final UploadFileRequest uploadFileRequest;
    private final Long partSizeInBytes;
    private final String multipartUploadId;
    private final long totalNumOfParts;
    private final Instant fileLastModified;

    private ResumableFileUpload(DefaultBuilder builder) {
        this.uploadFileRequest = Validate.paramNotNull(builder.uploadFileRequest, "uploadFileRequest");
        this.partSizeInBytes = builder.partSizeInBytes == null ? 0 : Validate.isNotNegative(builder.partSizeInBytes,
                                                                                            "partSizeInBytes");
        this.multipartUploadId = builder.multipartUploadId;
        this.totalNumOfParts = builder.totalNumOfParts == null ? 0 :
                               Validate.isPositiveOrNull(builder.totalNumOfParts, "totalNumOfParts");
        this.fileLastModified = Validate.paramNotNull(builder.fileLastModified, "fileLastModified");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ResumableFileUpload that = (ResumableFileUpload) o;

        if (totalNumOfParts != that.totalNumOfParts) {
            return false;
        }
        if (!uploadFileRequest.equals(that.uploadFileRequest)) {
            return false;
        }
        if (!Objects.equals(partSizeInBytes, that.partSizeInBytes)) {
            return false;
        }
        if (!Objects.equals(multipartUploadId, that.multipartUploadId)) {
            return false;
        }
        return fileLastModified.equals(that.fileLastModified);
    }

    @Override
    public int hashCode() {
        int result = uploadFileRequest.hashCode();
        result = 31 * result + (partSizeInBytes != null ? partSizeInBytes.hashCode() : 0);
        result = 31 * result + (multipartUploadId != null ? multipartUploadId.hashCode() : 0);
        result = 31 * result + (int) (totalNumOfParts ^ (totalNumOfParts >>> 32));
        result = 31 * result + fileLastModified.hashCode();
        return result;
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }

    /**
     * @return the {@link UploadFileRequest} to resume
     */
    public UploadFileRequest uploadFileRequest() {
        return uploadFileRequest;
    }

    /**
     * Retrieve the part size in bytes
     * @return the part size
     */
    public Long partSizeInBytes() {
        return partSizeInBytes;
    }

    /**
     * Last modified time of the file since last pause
     */
    public Instant fileLastModified() {
        return fileLastModified;
    }

    public long totalNumOfParts() {
        return totalNumOfParts;
    }

    /**
     * The total size of the transfer in bytes, or {@link Optional#empty()} if unknown
     *
     * @return the optional total size of the transfer.
     */
    public Optional<String> multipartUploadId() {
        return Optional.ofNullable(multipartUploadId);
    }

    @Override
    public String toString() {
        return ToString.builder("ResumableFileUpload")
                       .add("partSizeInBytes", partSizeInBytes)
                       .add("fileLastModified", fileLastModified)
                       .add("multipartUploadId", multipartUploadId)
                       .add("uploadFileRequest", uploadFileRequest)
                       .build();
    }

    @Override
    public Builder toBuilder() {
        return new DefaultBuilder(this);
    }

    public interface Builder extends CopyableBuilder<Builder, ResumableFileUpload> {

        /**
         * Sets the upload file request
         *
         * @param uploadFileRequest the upload file request
         * @return a reference to this object so that method calls can be chained together.
         */
        Builder uploadFileRequest(UploadFileRequest uploadFileRequest);

        /**
         * The {@link UploadFileRequest} request
         *
         * <p>
         * This is a convenience method that creates an instance of the {@link UploadFileRequest} builder avoiding the
         * need to create one manually via {@link UploadFileRequest#builder()}.
         *
         * @param uploadFileRequestBuilder the upload file request builder
         * @return a reference to this object so that method calls can be chained together.
         * @see #uploadFileRequest(UploadFileRequest)
         */
        default ResumableFileUpload.Builder uploadFileRequest(Consumer<UploadFileRequest.Builder>
                                                                      uploadFileRequestBuilder) {
            UploadFileRequest request = UploadFileRequest.builder()
                                                             .applyMutation(uploadFileRequestBuilder)
                                                             .build();
            uploadFileRequest(request);
            return this;
        }

        /**
         * Sets multipart upload ID
         * @param multipartUploadId multipart upload ID
         * @return a reference to this object so that method calls can be chained together.
         */
        Builder multipartUploadId(String multipartUploadId);

        /**
         * Sets the last modified time of the object
         *
         * @param lastModified the last modified time of the object
         * @return a reference to this object so that method calls can be chained together.
         */
        Builder fileLastModified(Instant lastModified);

        /**
         * Sets the number of parts
         *
         * @param numOfParts the number of parts
         * @return a reference to this object so that method calls can be chained together.
         */
        Builder numOfParts(Long numOfParts);
    }

    private static final class DefaultBuilder implements Builder {

        private UploadFileRequest uploadFileRequest;
        private Long partSizeInBytes;
        private String multipartUploadId;
        private Instant fileLastModified;
        private Long totalNumOfParts;

        private DefaultBuilder() {
        }

        private DefaultBuilder(ResumableFileUpload persistableFileUpload) {
            this.uploadFileRequest = persistableFileUpload.uploadFileRequest;
            this.partSizeInBytes = persistableFileUpload.partSizeInBytes;
            this.multipartUploadId = persistableFileUpload.multipartUploadId;
            this.fileLastModified = persistableFileUpload.fileLastModified;
            this.totalNumOfParts = persistableFileUpload.totalNumOfParts;
        }

        @Override
        public Builder uploadFileRequest(UploadFileRequest uploadFileRequest) {
            this.uploadFileRequest = uploadFileRequest;
            return this;
        }

        @Override
        public Builder multipartUploadId(String multipartUploadId) {
            this.multipartUploadId = multipartUploadId;
            return this;
        }

        @Override
        public Builder fileLastModified(Instant fileLastModified) {
            this.fileLastModified = fileLastModified;
            return this;
        }

        @Override
        public Builder numOfParts(Long numOfParts) {
            this.totalNumOfParts = numOfParts;
            return this;
        }

        @Override
        public ResumableFileUpload build() {
            return new ResumableFileUpload(this);
        }
    }
}
