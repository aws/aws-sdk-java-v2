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
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.config.TransferRequestOverrideConfiguration;
import software.amazon.awssdk.transfer.s3.internal.serialization.ResumableFileDownloadSerializer;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * An opaque token that holds the state and can be used to resume a paused download operation.
 * <p>
 * <b>Serialization: </b>When serializing this token, the following structures will not be preserved/persisted:
 * <ul>
 *     <li>{@link TransferRequestOverrideConfiguration}</li>
 *     <li>{@link AwsRequestOverrideConfiguration} (from {@link GetObjectRequest})</li>
 * </ul>
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
        this.bytesTransferred = builder.bytesTransferred == null ? 0 : Validate.isNotNegative(builder.bytesTransferred,
                                                                                              "bytesTransferred");
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
     * The total size of the transfer in bytes or {@link OptionalLong#empty()} if unknown
     *
     * @return the optional total size of the transfer.
     */
    public OptionalLong totalSizeInBytes() {
        return totalSizeInBytes == null ? OptionalLong.empty() : OptionalLong.of(totalSizeInBytes);
    }

    @Override
    public String toString() {
        return ToString.builder("ResumableFileDownload")
                       .add("bytesTransferred", bytesTransferred)
                       .add("fileLastModified", fileLastModified)
                       .add("s3ObjectLastModified", s3ObjectLastModified)
                       .add("totalSizeInBytes", totalSizeInBytes)
                       .add("downloadFileRequest", downloadFileRequest)
                       .build();
    }

    /**
     * Persists this download object to a file in Base64-encoded JSON format.
     *
     * @param path The path to the file to which you want to write the serialized download object.
     */
    @Override
    public void serializeToFile(Path path) {
        try {
            Files.write(path, ResumableFileDownloadSerializer.toJson(this));
        } catch (IOException e) {
            throw SdkClientException.create("Failed to write to " + path, e);
        }
    }

    /**
     * Writes the serialized JSON data representing this object to an output stream.
     * Note that the {@link OutputStream} is not closed or flushed after writing.
     *
     * @param outputStream The output stream to write the serialized object to.
     */
    @Override
    public void serializeToOutputStream(OutputStream outputStream) {
        byte[] bytes = ResumableFileDownloadSerializer.toJson(this);
        try {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
            IoUtils.copy(byteArrayInputStream, outputStream);
        } catch (IOException e) {
            throw SdkClientException.create("Failed to write this download object to the given OutputStream", e);
        }
    }

    /**
     * Returns the serialized JSON data representing this object as a string.
     */
    @Override
    public String serializeToString() {
        return new String(ResumableFileDownloadSerializer.toJson(this), StandardCharsets.UTF_8);
    }

    /**
     * Returns the serialized JSON data representing this object as an {@link SdkBytes} object.
     *
     * @return the serialized JSON as {@link SdkBytes}
     */
    @Override
    public SdkBytes serializeToBytes() {
        return SdkBytes.fromByteArrayUnsafe(ResumableFileDownloadSerializer.toJson(this));
    }

    /**
     * Returns the serialized JSON data representing this object as an {@link InputStream}.
     *
     * @return the serialized JSON input stream
     */
    @Override
    public InputStream serializeToInputStream() {
        return new ByteArrayInputStream(ResumableFileDownloadSerializer.toJson(this));
    }

    /**
     * Deserialize data at the given path into a {@link ResumableFileDownload}.
     *
     * @param path The {@link Path} to the file with serialized data
     * @return the deserialized {@link ResumableFileDownload}
     */
    public static ResumableFileDownload fromFile(Path path) {
        try (InputStream stream = Files.newInputStream(path)) {
            return ResumableFileDownloadSerializer.fromJson(stream);
        } catch (IOException e) {
            throw SdkClientException.create("Failed to create a ResumableFileDownload from " + path, e);
        }
    }

    /**
     * Deserialize bytes with JSON data into a {@link ResumableFileDownload}.
     *
     * @param bytes the serialized data
     * @return the deserialized {@link ResumableFileDownload}
     */
    public static ResumableFileDownload fromBytes(SdkBytes bytes) {
        return ResumableFileDownloadSerializer.fromJson(bytes.asByteArrayUnsafe());
    }

    /**
     * Deserialize a string with JSON data into a {@link ResumableFileDownload}.
     *
     * @param contents the serialized data
     * @return the deserialized {@link ResumableFileDownload}
     */
    public static ResumableFileDownload fromString(String contents) {
        return ResumableFileDownloadSerializer.fromJson(contents);
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
         * The {@link DownloadFileRequest} request
         *
         * <p>
         * This is a convenience method that creates an instance of the {@link DownloadFileRequest} builder avoiding the
         * need to create one manually via {@link DownloadFileRequest#builder()}.
         *
         * @param downloadFileRequestBuilder the download file request builder
         * @return a reference to this object so that method calls can be chained together.
         * @see #downloadFileRequest(DownloadFileRequest)
         */
        default ResumableFileDownload.Builder downloadFileRequest(Consumer<DownloadFileRequest.Builder>
                                                                      downloadFileRequestBuilder) {
            DownloadFileRequest request = DownloadFileRequest.builder()
                                                             .applyMutation(downloadFileRequestBuilder)
                                                             .build();
            downloadFileRequest(request);
            return this;
        }

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
            this.totalSizeInBytes = persistableFileDownload.totalSizeInBytes;
            this.fileLastModified = persistableFileDownload.fileLastModified;
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
