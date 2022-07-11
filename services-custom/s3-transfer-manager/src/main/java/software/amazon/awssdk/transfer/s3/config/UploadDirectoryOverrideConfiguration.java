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

package software.amazon.awssdk.transfer.s3.config;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPreviewApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.UploadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Configuration options for {@link S3TransferManager#uploadDirectory}. All values are optional, and not specifying them will
 * use the SDK default values.
 *
 * <p>Use {@link #builder()} to create a set of options.
 * @see S3TransferManager#uploadDirectory(UploadDirectoryRequest)
 */
@SdkPublicApi
@SdkPreviewApi
public final class UploadDirectoryOverrideConfiguration implements ToCopyableBuilder<UploadDirectoryOverrideConfiguration.Builder,
    UploadDirectoryOverrideConfiguration> {

    private final Boolean followSymbolicLinks;
    private final Integer maxDepth;
    private final Boolean recursive;
    private final Consumer<UploadFileRequest.Builder> uploadFileRequestTransformer;

    public UploadDirectoryOverrideConfiguration(DefaultBuilder builder) {
        this.followSymbolicLinks = builder.followSymbolicLinks;
        this.maxDepth = Validate.isPositiveOrNull(builder.maxDepth, "maxDepth");
        this.recursive = builder.recursive;
        this.uploadFileRequestTransformer = builder.uploadFileRequestTransformer;
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
    public Optional<Integer> maxDepth() {
        return Optional.ofNullable(maxDepth);
    }

    /**
     * @return whether to recursively upload all files under the specified directory
     * @see Builder#recursive(Boolean)
     */
    public Optional<Boolean> recursive() {
        return Optional.ofNullable(recursive);
    }

    /**
     * @return the upload request transformer if not null, otherwise no-op
     * @see UploadDirectoryOverrideConfiguration.Builder#uploadFileRequestTransformer(Consumer)
     */
    public Consumer<UploadFileRequest.Builder> uploadFileRequestTransformer() {
        return uploadFileRequestTransformer == null ? ignore -> { } : uploadFileRequestTransformer;
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

        UploadDirectoryOverrideConfiguration that = (UploadDirectoryOverrideConfiguration) o;

        if (!Objects.equals(followSymbolicLinks, that.followSymbolicLinks)) {
            return false;
        }
        if (!Objects.equals(maxDepth, that.maxDepth)) {
            return false;
        }
        if (!Objects.equals(uploadFileRequestTransformer, that.uploadFileRequestTransformer)) {
            return false;
        }
        return Objects.equals(recursive, that.recursive);
    }

    @Override
    public int hashCode() {
        int result = followSymbolicLinks != null ? followSymbolicLinks.hashCode() : 0;
        result = 31 * result + (maxDepth != null ? maxDepth.hashCode() : 0);
        result = 31 * result + (recursive != null ? recursive.hashCode() : 0);
        result = 31 * result + (uploadFileRequestTransformer != null ? uploadFileRequestTransformer.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("UploadDirectoryConfiguration")
                       .add("followSymbolicLinks", followSymbolicLinks)
                       .add("maxDepth", maxDepth)
                       .add("recursive", recursive)
                       .add("uploadFileRequestTransformer", uploadFileRequestTransformer)
                       .build();
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }

    public static Class<? extends Builder> serializableBuilderClass() {
        return DefaultBuilder.class;
    }

    // TODO: consider consolidating maxDepth and recursive
    public interface Builder extends CopyableBuilder<Builder, UploadDirectoryOverrideConfiguration> {

        /**
         * Specify whether to recursively upload all files under the specified directory
         *
         * <p>
         * Default to true
         *
         * @param recursive whether enable recursive upload
         * @return This builder for method chaining.
         */
        Builder recursive(Boolean recursive);

        /**
         * Specify whether to follow symbolic links when traversing the file tree.
         * <p>
         * Default to false
         *
         * @param followSymbolicLinks whether to follow symbolic links
         * @return This builder for method chaining.
         */
        Builder followSymbolicLinks(Boolean followSymbolicLinks);

        /**
         * Specify the maximum number of levels of directories to visit. Must be positive.
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
         * Specify a function used to transform the {@link UploadFileRequest}s generated by this {@link UploadDirectoryRequest}.
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
         * TransferRequestOverrideConfiguration fileUploadConfiguration =
         *     TransferRequestOverrideConfiguration.builder()
         *         .addListener(LoggingTransferListener.create())
         *         .build();
         *
         * UploadDirectoryOverrideConfiguration directoryUploadConfiguration =
         *     UploadDirectoryOverrideConfiguration.builder()
         *         .uploadFileRequestTransformer(request -> request.overrideConfiguration(fileUploadConfiguration))
         *         .build();
         *
         * UploadDirectoryRequest request =
         *     UploadDirectoryRequest.builder()
         *         .sourceDirectory(Paths.get("."))
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
        UploadDirectoryOverrideConfiguration build();
    }

    private static final class DefaultBuilder implements Builder {
        private Boolean followSymbolicLinks;
        private Integer maxDepth;
        private Boolean recursive;
        private Consumer<UploadFileRequest.Builder> uploadFileRequestTransformer;

        private DefaultBuilder(UploadDirectoryOverrideConfiguration configuration) {
            this.followSymbolicLinks = configuration.followSymbolicLinks;
            this.maxDepth = configuration.maxDepth;
            this.recursive = configuration.recursive;
            this.uploadFileRequestTransformer = configuration.uploadFileRequestTransformer;
        }

        private DefaultBuilder() {
        }

        @Override
        public Builder recursive(Boolean recursive) {
            this.recursive = recursive;
            return this;
        }

        public Boolean getRecursive() {
            return recursive;
        }

        public void setRecursive(Boolean recursive) {
            recursive(recursive);
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
        public UploadDirectoryOverrideConfiguration build() {
            return new UploadDirectoryOverrideConfiguration(this);
        }

    }
}
