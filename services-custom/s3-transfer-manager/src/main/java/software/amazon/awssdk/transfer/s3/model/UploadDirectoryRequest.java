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


import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.config.TransferRequestOverrideConfiguration;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Request object to upload a local directory to S3 using the Transfer Manager.
 *
 * @see S3TransferManager#uploadDirectory(UploadDirectoryRequest)
 */
@SdkPublicApi
public final class UploadDirectoryRequest
    implements TransferDirectoryRequest, ToCopyableBuilder<UploadDirectoryRequest.Builder, UploadDirectoryRequest> {

    private final Path source;
    private final String bucket;
    private final String s3Prefix;
    private final String s3Delimiter;
    private final Boolean followSymbolicLinks;
    private final Integer maxDepth;
    private final Consumer<UploadFileRequest.Builder> uploadFileRequestTransformer;


    public UploadDirectoryRequest(DefaultBuilder builder) {
        this.source = Validate.paramNotNull(builder.source, "source");
        this.bucket = Validate.paramNotNull(builder.bucket, "bucket");
        this.s3Prefix = builder.s3Prefix;
        this.s3Delimiter = builder.s3Delimiter;
        this.followSymbolicLinks = builder.followSymbolicLinks;
        this.maxDepth = builder.maxDepth;
        this.uploadFileRequestTransformer = builder.uploadFileRequestTransformer;
    }

    /**
     * The source directory to upload
     *
     * @return the source directory
     * @see Builder#source(Path)
     */
    public Path source() {
        return source;
    }

    /**
     * The name of the bucket to upload objects to.
     *
     * @return bucket name
     * @see Builder#bucket(String)
     */
    public String bucket() {
        return bucket;
    }

    /**
     * @return the optional key prefix
     * @see Builder#s3Prefix(String)
     */
    public Optional<String> s3Prefix() {
        return Optional.ofNullable(s3Prefix);
    }

    /**
     * @return the optional delimiter
     * @see Builder#s3Delimiter(String)
     */
    public Optional<String> s3Delimiter() {
        return Optional.ofNullable(s3Delimiter);
    }


    /**
     * @return whether to follow symbolic links
     * @see Builder#followSymbolicLinks(Boolean)
     */
    public Optional<Boolean> followSymbolicLinks() {
        return Optional.ofNullable(followSymbolicLinks);
    }

    /**
     * @return the maximum number of directory levels to traverse
     * @see Builder#maxDepth(Integer)
     */
    public OptionalInt maxDepth() {
        return maxDepth == null ? OptionalInt.empty() : OptionalInt.of(maxDepth);
    }

    /**
     * @return the upload request transformer if not null, otherwise no-op
     * @see Builder#uploadFileRequestTransformer(Consumer)
     */
    public Consumer<UploadFileRequest.Builder> uploadFileRequestTransformer() {
        return uploadFileRequestTransformer == null ? ignore -> { } : uploadFileRequestTransformer;
    }


    public static Builder builder() {
        return new DefaultBuilder();
    }

    public static Class<? extends Builder> serializableBuilderClass() {
        return DefaultBuilder.class;
    }

    @Override
    public Builder toBuilder() {
        return new DefaultBuilder(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UploadDirectoryRequest that = (UploadDirectoryRequest) o;

        if (!Objects.equals(source, that.source)) {
            return false;
        }
        if (!Objects.equals(bucket, that.bucket)) {
            return false;
        }
        if (!Objects.equals(s3Prefix, that.s3Prefix)) {
            return false;
        }
        if (!Objects.equals(followSymbolicLinks, that.followSymbolicLinks)) {
            return false;
        }
        if (!Objects.equals(maxDepth, that.maxDepth)) {
            return false;
        }
        if (!Objects.equals(uploadFileRequestTransformer, that.uploadFileRequestTransformer)) {
            return false;
        }
        return Objects.equals(s3Delimiter, that.s3Delimiter);
    }

    @Override
    public int hashCode() {
        int result = source != null ? source.hashCode() : 0;
        result = 31 * result + (bucket != null ? bucket.hashCode() : 0);
        result = 31 * result + (s3Prefix != null ? s3Prefix.hashCode() : 0);
        result = 31 * result + (s3Delimiter != null ? s3Delimiter.hashCode() : 0);
        result = 31 * result + (followSymbolicLinks != null ? followSymbolicLinks.hashCode() : 0);
        result = 31 * result + (maxDepth != null ? maxDepth.hashCode() : 0);
        result = 31 * result + (uploadFileRequestTransformer != null ? uploadFileRequestTransformer.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("UploadDirectoryRequest")
                       .add("source", source)
                       .add("bucket", bucket)
                       .add("s3Prefix", s3Prefix)
                       .add("s3Delimiter", s3Delimiter)
                       .add("followSymbolicLinks", followSymbolicLinks)
                       .add("maxDepth", maxDepth)
                       .add("uploadFileRequestTransformer", uploadFileRequestTransformer)
                       .build();
    }

    public interface Builder extends CopyableBuilder<Builder, UploadDirectoryRequest> {
        /**
         * Specifies the source directory to upload. The source directory must exist.
         * Fle wildcards are not supported and treated literally. Hidden files/directories are visited.
         *
         * <p>
         * Note that the current user must have read access to all directories and files,
         * otherwise {@link SecurityException} will be thrown.
         *
         * @param source the source directory
         * @return This builder for method chaining.
         */
        Builder source(Path source);

        /**
         * The name of the bucket to upload objects to.
         *
         * @param bucket the bucket name
         * @return This builder for method chaining.
         */
        Builder bucket(String bucket);

        /**
         * Specifies the key prefix to use for the objects. If not provided, files will be uploaded to the root of the bucket
         * <p>
         * See <a
         * href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-prefixes.html">Organizing objects using
         * prefixes</a>
         *
         * <p>
         * Note: if the provided prefix ends with the same string as delimiter, it will get "normalized" when generating the key
         * name. For example, assuming the prefix provided is "foo/" and the delimiter is "/" and the source directory has the
         * following structure:
         *
         * <pre>
         * |- test
         *     |- obj1.txt
         *     |- obj2.txt
         * </pre>
         *
         * the object keys will be "foo/obj1.txt" and "foo/obj2.txt" as apposed to "foo//obj1.txt" and "foo//obj2.txt"
         *
         * @param s3Prefix the key prefix
         * @return This builder for method chaining.
         * @see #s3Delimiter(String)
         */
        Builder s3Prefix(String s3Prefix);

        /**
         * Specifies the delimiter. A delimiter causes a list operation to roll up all the keys that share a common prefix into a
         * single summary list result. If not provided, {@code "/"} will be used.
         *
         * See <a
         * href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-prefixes.html">Organizing objects using
         * prefixes</a>
         *
         * <p>
         * Note: if the provided prefix ends with the same string as delimiter, it will get "normalized" when generating the key
         * name. For example, assuming the prefix provided is "foo/" and the delimiter is "/" and the source directory has the
         * following structure:
         *
         * <pre>
         * |- test
         *     |- obj1.txt
         *     |- obj2.txt
         * </pre>
         *
         * the object keys will be "foo/obj1.txt" and "foo/obj2.txt" as apposed to "foo//obj1.txt" and "foo//obj2.txt"
         *
         * @param s3Delimiter the delimiter
         * @return This builder for method chaining.
         * @see #s3Prefix(String)
         */
        Builder s3Delimiter(String s3Delimiter);

        /**
         * Specifies whether to follow symbolic links when traversing the file tree in
         * {@link S3TransferManager#downloadDirectory} operation
         * <p>
         * Default to false
         *
         * @param followSymbolicLinks whether to follow symbolic links
         * @return This builder for method chaining.
         */
        Builder followSymbolicLinks(Boolean followSymbolicLinks);

        /**
         * Specifies the maximum number of levels of directories to visit. Must be positive.
         * 1 means only the files directly within the provided source directory are visited.
         *
         * <p>
         * Default to {@code Integer.MAX_VALUE}
         *
         * @param maxDepth the maximum number of directory levels to visit
         * @return This builder for method chaining.
         */
        Builder maxDepth(Integer maxDepth);

        /**
         * Specifies a function used to transform the {@link UploadFileRequest}s generated by this {@link UploadDirectoryRequest}.
         * The provided function is called once for each file that is uploaded, allowing you to modify the paths resolved by
         * TransferManager on a per-file basis, modify the created {@link PutObjectRequest} before it is passed to S3, or
         * configure a {@link TransferRequestOverrideConfiguration}.
         *
         * <p>The factory receives the {@link UploadFileRequest}s created by Transfer Manager for each file in the directory
         * being uploaded, and returns a (potentially modified) {@code UploadFileRequest}.
         *
         * <p>
         * <b>Usage Example:</b>
         * <pre>
         * {@code
         * // Add a LoggingTransferListener to every transfer within the upload directory request
         *
         * UploadDirectoryOverrideConfiguration directoryUploadConfiguration =
         *     UploadDirectoryOverrideConfiguration.builder()
         *         .uploadFileRequestTransformer(request -> request.addTransferListenerf(LoggingTransferListener.create())
         *         .build();
         *
         * UploadDirectoryRequest request =
         *     UploadDirectoryRequest.builder()
         *         .source(Paths.get("."))
         *         .bucket("bucket")
         *         .prefix("prefix")
         *         .overrideConfiguration(directoryUploadConfiguration)
         *         .build()
         *
         * UploadDirectoryTransfer uploadDirectory = transferManager.uploadDirectory(request);
         *
         * // Wait for the transfer to complete
         * CompletedUploadDirectory completedUploadDirectory = uploadDirectory.completionFuture().join();
         *
         * // Print out the failed uploads
         * completedUploadDirectory.failedUploads().forEach(System.out::println);
         * }
         * </pre>
         *
         * @param uploadFileRequestTransformer A transformer to use for modifying the file-level upload requests before execution
         * @return This builder for method chaining
         */
        Builder uploadFileRequestTransformer(Consumer<UploadFileRequest.Builder> uploadFileRequestTransformer);



        @Override
        UploadDirectoryRequest build();
    }


    private static final class DefaultBuilder implements Builder {

        private Path source;
        private String bucket;
        private String s3Prefix;
        private String s3Delimiter;
        private Boolean followSymbolicLinks;
        private Integer maxDepth;
        private Consumer<UploadFileRequest.Builder> uploadFileRequestTransformer;

        private DefaultBuilder() {
        }

        private DefaultBuilder(UploadDirectoryRequest request) {
            this.source = request.source;
            this.bucket = request.bucket;
            this.s3Prefix = request.s3Prefix;
            this.s3Delimiter = request.s3Delimiter;
            this.followSymbolicLinks = request.followSymbolicLinks;
            this.maxDepth = request.maxDepth;
            this.uploadFileRequestTransformer = request.uploadFileRequestTransformer;
        }

        @Override
        public Builder source(Path source) {
            this.source = source;
            return this;
        }

        public void setSource(Path source) {
            source(source);
        }

        public Path getSource() {
            return source;
        }

        @Override
        public Builder bucket(String bucket) {
            this.bucket = bucket;
            return this;
        }

        public void setBucket(String bucket) {
            bucket(bucket);
        }

        public String getBucket() {
            return bucket;
        }

        @Override
        public Builder s3Prefix(String s3Prefix) {
            this.s3Prefix = s3Prefix;
            return this;
        }

        public void setS3Prefix(String s3Prefix) {
            s3Prefix(s3Prefix);
        }

        public String getS3Prefix() {
            return s3Prefix;
        }

        @Override
        public Builder s3Delimiter(String s3Delimiter) {
            this.s3Delimiter = s3Delimiter;
            return this;
        }

        public void setS3Delimiter(String s3Delimiter) {
            s3Delimiter(s3Delimiter);
        }

        public String getS3Delimiter() {
            return s3Delimiter;
        }

        @Override
        public Builder followSymbolicLinks(Boolean followSymbolicLinks) {
            this.followSymbolicLinks = followSymbolicLinks;
            return this;
        }

        public void setFollowSymbolicLinks(Boolean followSymbolicLinks) {
            followSymbolicLinks(followSymbolicLinks);
        }

        public Boolean getFollowSymbolicLinks() {
            return followSymbolicLinks;
        }

        @Override
        public Builder maxDepth(Integer maxDepth) {
            this.maxDepth = maxDepth;
            return this;
        }

        public void setMaxDepth(Integer maxDepth) {
            maxDepth(maxDepth);
        }

        public Integer getMaxDepth() {
            return maxDepth;
        }

        @Override
        public Builder uploadFileRequestTransformer(Consumer<UploadFileRequest.Builder> uploadFileRequestTransformer) {
            this.uploadFileRequestTransformer = uploadFileRequestTransformer;
            return this;
        }

        public Consumer<UploadFileRequest.Builder> getUploadFileRequestTransformer() {
            return uploadFileRequestTransformer;
        }

        public void setUploadFileRequestTransformer(Consumer<UploadFileRequest.Builder> uploadFileRequestTransformer) {
            this.uploadFileRequestTransformer = uploadFileRequestTransformer;
        }

        @Override
        public UploadDirectoryRequest build() {
            return new UploadDirectoryRequest(this);
        }
    }
}
