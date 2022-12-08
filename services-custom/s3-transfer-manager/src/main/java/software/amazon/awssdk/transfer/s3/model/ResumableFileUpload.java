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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.config.TransferRequestOverrideConfiguration;
import software.amazon.awssdk.transfer.s3.internal.serialization.ResumableFileUploadSerializer;
import software.amazon.awssdk.utils.IoUtils;
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
 * @see S3TransferManager#resumeUploadFile(ResumableFileUpload)
 */
@SdkPublicApi
public final class ResumableFileUpload implements ResumableTransfer,
                                                  ToCopyableBuilder<ResumableFileUpload.Builder, ResumableFileUpload> {

    private final UploadFileRequest uploadFileRequest;
    private final Instant fileLastModified;
    private final String multipartUploadId;
    private final Long partSizeInBytes;
    private final Long totalParts;
    private final long fileLength;
    private final Long transferredParts;

    private ResumableFileUpload(DefaultBuilder builder) {
        this.uploadFileRequest = Validate.paramNotNull(builder.uploadFileRequest, "uploadFileRequest");
        this.fileLastModified = Validate.paramNotNull(builder.fileLastModified, "fileLastModified");
        this.fileLength = Validate.paramNotNull(builder.fileLength, "fileLength");
        this.multipartUploadId = builder.multipartUploadId;
        this.totalParts = builder.totalParts;
        this.partSizeInBytes = builder.partSizeInBytes;
        this.transferredParts = builder.transferredParts;
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

        if (fileLength != that.fileLength) {
            return false;
        }
        if (!uploadFileRequest.equals(that.uploadFileRequest)) {
            return false;
        }
        if (!fileLastModified.equals(that.fileLastModified)) {
            return false;
        }
        if (!Objects.equals(multipartUploadId, that.multipartUploadId)) {
            return false;
        }
        if (!Objects.equals(partSizeInBytes, that.partSizeInBytes)) {
            return false;
        }

        if (!Objects.equals(transferredParts, that.transferredParts)) {
            return false;
        }

        return Objects.equals(totalParts, that.totalParts);
    }

    @Override
    public int hashCode() {
        int result = uploadFileRequest.hashCode();
        result = 31 * result + fileLastModified.hashCode();
        result = 31 * result + (multipartUploadId != null ? multipartUploadId.hashCode() : 0);
        result = 31 * result + (partSizeInBytes != null ? partSizeInBytes.hashCode() : 0);
        result = 31 * result + (totalParts != null ? totalParts.hashCode() : 0);
        result = 31 * result + (transferredParts != null ? transferredParts.hashCode() : 0);
        result = 31 * result + (int) (fileLength ^ (fileLength >>> 32));
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
     * Last modified time of the file since last pause
     */
    public Instant fileLastModified() {
        return fileLastModified;
    }

    /**
     * File length since last pause
     */
    public long fileLength() {
        return fileLength;
    }

    /**
     * Return the part size in bytes or {@link OptionalLong#empty()} if unknown
     */
    public OptionalLong partSizeInBytes() {
        return partSizeInBytes == null ? OptionalLong.empty() : OptionalLong.of(partSizeInBytes);
    }

    /**
     * Return the total number of parts associated with this transfer or {@link OptionalLong#empty()} if unknown
     */
    public OptionalLong totalParts() {
        return totalParts == null ? OptionalLong.empty() : OptionalLong.of(totalParts);
    }

    /**
     * The multipart upload ID, or {@link Optional#empty()} if unknown
     *
     * @return the optional total size of the transfer.
     */
    public Optional<String> multipartUploadId() {
        return Optional.ofNullable(multipartUploadId);
    }

    /**
     * Return the total number of parts completed with this transfer or {@link OptionalLong#empty()} if unknown
     */
    public OptionalLong transferredParts() {
        return transferredParts == null ? OptionalLong.empty() : OptionalLong.of(transferredParts);
    }

    @Override
    public void serializeToFile(Path path) {
        try {
            Files.write(path, ResumableFileUploadSerializer.toJson(this));
        } catch (IOException e) {
            throw SdkClientException.create("Failed to write to " + path, e);
        }
    }

    @Override
    public void serializeToOutputStream(OutputStream outputStream) {
        byte[] bytes = ResumableFileUploadSerializer.toJson(this);
        try {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
            IoUtils.copy(byteArrayInputStream, outputStream);
        } catch (IOException e) {
            throw SdkClientException.create("Failed to write this download object to the given OutputStream", e);
        }
    }

    @Override
    public String serializeToString() {
        return new String(ResumableFileUploadSerializer.toJson(this), StandardCharsets.UTF_8);
    }

    @Override
    public SdkBytes serializeToBytes() {
        return SdkBytes.fromByteArrayUnsafe(ResumableFileUploadSerializer.toJson(this));
    }

    @Override
    public InputStream serializeToInputStream() {
        return new ByteArrayInputStream(ResumableFileUploadSerializer.toJson(this));
    }

    /**
     * Deserializes data at the given path into a {@link ResumableFileUpload}.
     *
     * @param path The {@link Path} to the file with serialized data
     * @return the deserialized {@link ResumableFileUpload}
     */
    public static ResumableFileUpload fromFile(Path path) {
        try (InputStream stream = Files.newInputStream(path)) {
            return ResumableFileUploadSerializer.fromJson(stream);
        } catch (IOException e) {
            throw SdkClientException.create("Failed to create a ResumableFileUpload from " + path, e);
        }
    }

    /**
     * Deserializes bytes with JSON data into a {@link ResumableFileUpload}.
     *
     * @param bytes the serialized data
     * @return the deserialized {@link ResumableFileUpload}
     */
    public static ResumableFileUpload fromBytes(SdkBytes bytes) {
        return ResumableFileUploadSerializer.fromJson(bytes.asByteArrayUnsafe());
    }

    /**
     * Deserializes a string with JSON data into a {@link ResumableFileUpload}.
     *
     * @param contents the serialized data
     * @return the deserialized {@link ResumableFileUpload}
     */
    public static ResumableFileUpload fromString(String contents) {
        return ResumableFileUploadSerializer.fromJson(contents);
    }
    
    
    @Override
    public String toString() {
        return ToString.builder("ResumableFileUpload")
                       .add("fileLastModified", fileLastModified)
                       .add("multipartUploadId", multipartUploadId)
                       .add("uploadFileRequest", uploadFileRequest)
                       .add("fileLength", fileLength)
                       .add("totalParts", totalParts)
                       .add("partSizeInBytes", partSizeInBytes)
                       .add("transferredParts", transferredParts)
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
         * Sets multipart ID associated with this transfer
         *
         * @param multipartUploadId the multipart ID
         * @return a reference to this object so that method calls can be chained together.
         */
        Builder multipartUploadId(String multipartUploadId);

        /**
         * Sets the last modified time of the object
         *
         * @param fileLastModified the last modified time of the file
         * @return a reference to this object so that method calls can be chained together.
         */
        Builder fileLastModified(Instant fileLastModified);

        /**
         * Sets the file length
         *
         * @param fileLength the last modified time of the object
         * @return a reference to this object so that method calls can be chained together.
         */
        Builder fileLength(Long fileLength);

        /**
         * Sets the total number of parts
         *
         * @param totalParts the total number of parts
         * @return a reference to this object so that method calls can be chained together.
         */
        Builder totalParts(Long totalParts);

        /**
         * Set the total number of parts transferred
         *
         * @param transferredParts the number of parts completed
         * @return a reference to this object so that method calls can be chained together.
         */
        Builder transferredParts(Long transferredParts);

        /**
         * The part size associated with this transfer
         * @param partSizeInBytes the part size in bytes
         * @return a reference to this object so that method calls can be chained together.
         */
        Builder partSizeInBytes(Long partSizeInBytes);
    }

    private static final class DefaultBuilder implements Builder {

        private String multipartUploadId;
        private UploadFileRequest uploadFileRequest;
        private Long partSizeInBytes;
        private Long totalParts;
        private Instant fileLastModified;
        private Long fileLength;

        private Long transferredParts;

        private DefaultBuilder() {
        }

        private DefaultBuilder(ResumableFileUpload persistableFileUpload) {
            this.multipartUploadId = persistableFileUpload.multipartUploadId;
            this.uploadFileRequest = persistableFileUpload.uploadFileRequest;
            this.partSizeInBytes = persistableFileUpload.partSizeInBytes;
            this.fileLastModified = persistableFileUpload.fileLastModified;
            this.totalParts = persistableFileUpload.totalParts;
            this.fileLength = persistableFileUpload.fileLength;
            this.transferredParts = persistableFileUpload.transferredParts;
        }

        @Override
        public Builder uploadFileRequest(UploadFileRequest uploadFileRequest) {
            this.uploadFileRequest = uploadFileRequest;
            return this;
        }

        @Override
        public Builder multipartUploadId(String mutipartUploadId) {
            this.multipartUploadId = mutipartUploadId;
            return this;
        }


        @Override
        public Builder fileLastModified(Instant fileLastModified) {
            this.fileLastModified = fileLastModified;
            return this;
        }

        @Override
        public Builder fileLength(Long fileLength) {
            this.fileLength = fileLength;
            return this;
        }

        @Override
        public Builder totalParts(Long totalParts) {
            this.totalParts = totalParts;
            return this;
        }

        @Override
        public Builder transferredParts(Long transferredParts) {
            this.transferredParts = transferredParts;
            return this;
        }

        @Override
        public Builder partSizeInBytes(Long partSizeInBytes) {
            this.partSizeInBytes = partSizeInBytes;
            return this;
        }

        @Override
        public ResumableFileUpload build() {
            return new ResumableFileUpload(this);
        }
    }
}
