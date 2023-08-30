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

package software.amazon.awssdk.core;

import java.nio.file.Path;
import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Configuration options for {@link AsyncRequestBody#fromFile(FileRequestBodyConfiguration)} to configure how the SDK
 * should read the file.
 *
 * @see #builder()
 */
@SdkPublicApi
public final class FileRequestBodyConfiguration implements ToCopyableBuilder<FileRequestBodyConfiguration.Builder,
    FileRequestBodyConfiguration> {
    private final Integer chunkSizeInBytes;
    private final Long position;
    private final Long numBytesToRead;
    private final Path path;

    private FileRequestBodyConfiguration(DefaultBuilder builder) {
        this.path = Validate.notNull(builder.path, "path");
        this.chunkSizeInBytes = Validate.isPositiveOrNull(builder.chunkSizeInBytes, "chunkSizeInBytes");
        this.position = Validate.isNotNegativeOrNull(builder.position, "position");
        this.numBytesToRead = Validate.isNotNegativeOrNull(builder.numBytesToRead, "numBytesToRead");
    }

    /**
     * Create a {@link Builder}, used to create a {@link FileRequestBodyConfiguration}.
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }

    /**
     * @return the size of each chunk to read from the file
     */
    public Integer chunkSizeInBytes() {
        return chunkSizeInBytes;
    }

    /**
     * @return the file position at which the request body begins.
     */
    public Long position() {
        return position;
    }

    /**
     * @return the number of bytes to read from this file.
     */
    public Long numBytesToRead() {
        return numBytesToRead;
    }

    /**
     * @return the file path
     */
    public Path path() {
        return path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FileRequestBodyConfiguration that = (FileRequestBodyConfiguration) o;

        if (!Objects.equals(chunkSizeInBytes, that.chunkSizeInBytes)) {
            return false;
        }
        if (!Objects.equals(position, that.position)) {
            return false;
        }
        if (!Objects.equals(numBytesToRead, that.numBytesToRead)) {
            return false;
        }
        return Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        int result = chunkSizeInBytes != null ? chunkSizeInBytes.hashCode() : 0;
        result = 31 * result + (position != null ? position.hashCode() : 0);
        result = 31 * result + (numBytesToRead != null ? numBytesToRead.hashCode() : 0);
        result = 31 * result + (path != null ? path.hashCode() : 0);
        return result;
    }

    @Override
    public Builder toBuilder() {
        return new DefaultBuilder(this);
    }

    public interface Builder extends CopyableBuilder<Builder, FileRequestBodyConfiguration> {

        /**
         * Sets the {@link Path} to the file containing data to send to the service
         *
         * @param path Path to file to read.
         * @return This builder for method chaining.
         */
        Builder path(Path path);

        /**
         * Sets the size of chunks read from the file. Increasing this will cause more data to be buffered into memory but
         * may yield better latencies. Decreasing this will reduce memory usage but may cause reduced latency. Setting this value
         * is very dependent on upload speed and requires some performance testing to tune.
         *
         * <p>The default chunk size is 16 KiB</p>
         *
         * @param chunkSize New chunk size in bytes.
         * @return This builder for method chaining.
         */
        Builder chunkSizeInBytes(Integer chunkSize);

        /**
         * Sets the file position at which the request body begins.
         *
         * <p>By default, it's 0, i.e., reading from the beginning.
         *
         * @param position the position of the file
         * @return The builder for method chaining.
         */
        Builder position(Long position);

        /**
         * Sets the number of bytes to read from this file.
         *
         * <p>By default, it's same as the file length.
         *
         * @param numBytesToRead number of bytes to read
         * @return The builder for method chaining.
         */
        Builder numBytesToRead(Long numBytesToRead);
    }

    private static final class DefaultBuilder implements Builder {
        private Long position;
        private Path path;
        private Integer chunkSizeInBytes;
        private Long numBytesToRead;

        private DefaultBuilder(FileRequestBodyConfiguration configuration) {
            this.position = configuration.position;
            this.path = configuration.path;
            this.chunkSizeInBytes = configuration.chunkSizeInBytes;
            this.numBytesToRead = configuration.numBytesToRead;
        }

        private DefaultBuilder() {

        }

        @Override
        public Builder path(Path path) {
            this.path = path;
            return this;
        }

        @Override
        public Builder chunkSizeInBytes(Integer chunkSizeInBytes) {
            this.chunkSizeInBytes = chunkSizeInBytes;
            return this;
        }

        @Override
        public Builder position(Long position) {
            this.position = position;
            return this;
        }

        @Override
        public Builder numBytesToRead(Long numBytesToRead) {
            this.numBytesToRead = numBytesToRead;
            return this;
        }

        @Override
        public FileRequestBodyConfiguration build() {
            return new FileRequestBodyConfiguration(this);
        }
    }

}