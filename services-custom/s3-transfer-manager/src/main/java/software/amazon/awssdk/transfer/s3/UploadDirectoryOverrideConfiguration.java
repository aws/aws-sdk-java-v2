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

import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPreviewApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
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

    public UploadDirectoryOverrideConfiguration(DefaultBuilder builder) {
        this.followSymbolicLinks = builder.followSymbolicLinks;
        this.maxDepth = Validate.isPositiveOrNull(builder.maxDepth, "maxDepth");
        this.recursive = builder.recursive;
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
        return Objects.equals(recursive, that.recursive);
    }

    @Override
    public int hashCode() {
        int result = followSymbolicLinks != null ? followSymbolicLinks.hashCode() : 0;
        result = 31 * result + (maxDepth != null ? maxDepth.hashCode() : 0);
        result = 31 * result + (recursive != null ? recursive.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("UploadDirectoryConfiguration")
                       .add("followSymbolicLinks", followSymbolicLinks)
                       .add("maxDepth", maxDepth)
                       .add("recursive", recursive)
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

        @Override
        UploadDirectoryOverrideConfiguration build();
    }

    private static final class DefaultBuilder implements Builder {
        private Boolean followSymbolicLinks;
        private Integer maxDepth;
        private Boolean recursive;

        private DefaultBuilder(UploadDirectoryOverrideConfiguration configuration) {
            this.followSymbolicLinks = configuration.followSymbolicLinks;
            this.maxDepth = configuration.maxDepth;
            this.recursive = configuration.recursive;
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
        public UploadDirectoryOverrideConfiguration build() {
            return new UploadDirectoryOverrideConfiguration(this);
        }

    }
}
