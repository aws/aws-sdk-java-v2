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
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Configuration options for {@link S3TransferManager#uploadDirectory}. All values are optional, and not specifying them will
 * use the SDK default values.
 *
 * <p>Use {@link #builder()} to create a set of options.</p>
 */
@SdkPublicApi
@SdkPreviewApi
public final class UploadDirectoryConfiguration implements ToCopyableBuilder<UploadDirectoryConfiguration.Builder,
    UploadDirectoryConfiguration> {

    private final Boolean followSymbolicLinks;
    private final Integer maxDepth;
    private final Boolean recursive;

    public UploadDirectoryConfiguration(DefaultBuilder builder) {
        this.followSymbolicLinks = builder.followSymbolicLinks;
        this.maxDepth = builder.maxDepth;
        this.recursive = builder.recursive;
    }

    /**
     * @return whether to follow symbolic links
     */
    public Optional<Boolean> followSymbolicLinks() {
        return Optional.ofNullable(followSymbolicLinks);
    }

    /**
     * @return the maximum number of directory levels to traverse
     */
    public Optional<Integer> maxDepth() {
        return Optional.ofNullable(maxDepth);
    }

    /**
     * @return whether to recursively upload all files under the specified directory
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

        UploadDirectoryConfiguration that = (UploadDirectoryConfiguration) o;

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

    public interface Builder extends CopyableBuilder<Builder, UploadDirectoryConfiguration> {

        /**
         * Specify whether to recursively upload all files under the specified directory
         *
         * @param recursive whether enable recursive upload
         * @return This builder for method chaining.
         */
        Builder recursive(Boolean recursive);

        /**
         * Specify whether to follow Follow symbolic links when traverse the directory tree.
         * <p>
         * Default to false
         *
         * @param followSymbolicLinks whether to follow symbolic links
         * @return This builder for method chaining.
         */
        Builder followSymbolicLinks(Boolean followSymbolicLinks);

        /**
         * Specify the maximum number of directory levels to traverse
         * @param maxDepth the maximum number of directory levels
         * @return This builder for method chaining.
         */
        Builder maxDepth(Integer maxDepth);

        @Override
        UploadDirectoryConfiguration build();
    }

    private static final class DefaultBuilder implements Builder {
        private Boolean followSymbolicLinks;
        private Integer maxDepth;
        private Boolean recursive;

        private DefaultBuilder(UploadDirectoryConfiguration configuration) {
            this.followSymbolicLinks = configuration.followSymbolicLinks;
            this.maxDepth = configuration.maxDepth;
            this.recursive = configuration.recursive;
        }

        private DefaultBuilder() {
        }

        @Override
        public DefaultBuilder recursive(Boolean recursive) {
            this.recursive = recursive;
            return this;
        }

        @Override
        public DefaultBuilder followSymbolicLinks(Boolean followSymbolicLinks) {
            this.followSymbolicLinks = followSymbolicLinks;
            return this;
        }

        @Override
        public DefaultBuilder maxDepth(Integer maxDepth) {
            this.maxDepth = maxDepth;
            return this;
        }

        @Override
        public UploadDirectoryConfiguration build() {
            return new UploadDirectoryConfiguration(this);
        }

    }
}
