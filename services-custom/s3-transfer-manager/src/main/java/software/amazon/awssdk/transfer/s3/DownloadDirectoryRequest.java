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


import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPreviewApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Request object to download the objects in the provided S3 bucket to a local directory using the Transfer Manager.
 *
 * @see S3TransferManager#downloadDirectory(DownloadDirectoryRequest)
 */
@SdkPublicApi
@SdkPreviewApi
public final class DownloadDirectoryRequest
    implements TransferDirectoryRequest, ToCopyableBuilder<DownloadDirectoryRequest.Builder, DownloadDirectoryRequest> {

    private final Path destinationDirectory;
    private final String bucket;
    private final String prefix;
    private final String delimiter;
    private final DownloadDirectoryOverrideConfiguration overrideConfiguration;

    public DownloadDirectoryRequest(DefaultBuilder builder) {
        this.destinationDirectory = Validate.paramNotNull(builder.destinationDirectory, "destinationDirectory");
        this.bucket = Validate.paramNotNull(builder.bucket, "bucket");
        this.prefix = builder.prefix;
        this.delimiter = builder.delimiter;
        this.overrideConfiguration = builder.configuration;
    }

    /**
     * The destination directory to download
     *
     * @return the destination directory
     * @see Builder#destinationDirectory(Path)
     */
    public Path destinationDirectory() {
        return destinationDirectory;
    }

    /**
     * The name of the bucket
     *
     * @return bucket name
     * @see Builder#bucket(String)
     */
    public String bucket() {
        return bucket;
    }

    /**
     * @return the optional key prefix
     * @see Builder#prefix(String)
     */
    public Optional<String> prefix() {
        return Optional.ofNullable(prefix);
    }

    /**
     * @return the optional delimiter
     * @see Builder#delimiter(String)
     */
    public Optional<String> delimiter() {
        return Optional.ofNullable(delimiter);
    }

    /**
     * @return the optional override configuration
     * @see Builder#overrideConfiguration(DownloadDirectoryOverrideConfiguration)
     */
    public Optional<DownloadDirectoryOverrideConfiguration> overrideConfiguration() {
        return Optional.ofNullable(overrideConfiguration);
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

        DownloadDirectoryRequest that = (DownloadDirectoryRequest) o;

        if (!Objects.equals(destinationDirectory, that.destinationDirectory)) {
            return false;
        }
        if (!Objects.equals(bucket, that.bucket)) {
            return false;
        }
        if (!Objects.equals(prefix, that.prefix)) {
            return false;
        }
        if (!Objects.equals(overrideConfiguration, that.overrideConfiguration)) {
            return false;
        }
        return Objects.equals(delimiter, that.delimiter);
    }

    @Override
    public int hashCode() {
        int result = destinationDirectory != null ? destinationDirectory.hashCode() : 0;
        result = 31 * result + (bucket != null ? bucket.hashCode() : 0);
        result = 31 * result + (prefix != null ? prefix.hashCode() : 0);
        result = 31 * result + (overrideConfiguration != null ? overrideConfiguration.hashCode() : 0);
        result = 31 * result + (delimiter != null ? delimiter.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("DownloadDirectoryRequest")
                       .add("destinationDirectory", destinationDirectory)
                       .add("bucket", bucket)
                       .add("prefix", prefix)
                       .add("delimiter", delimiter)
                       .add("overrideConfiguration", overrideConfiguration)
                       .build();
    }

    public interface Builder extends CopyableBuilder<Builder, DownloadDirectoryRequest> {
        /**
         * Specify the destination directory to download. The destination directory must exist.
         *
         * @param destinationDirectory the destination directory
         * @return This builder for method chaining.
         * @see DownloadDirectoryOverrideConfiguration
         */
        Builder destinationDirectory(Path destinationDirectory);

        /**
         * The name of the bucket to download objects to.
         *
         * @param bucket the bucket name
         * @return This builder for method chaining.
         */
        Builder bucket(String bucket);

        /**
         * Specify the key prefix for the virtual directory. If not provided, all subdirectories will be downloaded recursively
         * <p>
         * See <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-prefixes.html">Organizing objects using
         * prefixes</a>
         *
         * @param prefix the key prefix
         * @return This builder for method chaining.
         */
        Builder prefix(String prefix);

        /**
         * Specify the delimiter that will be used to retrieve the objects within the provided bucket. A delimiter causes a list
         * operation to roll up all the keys that share a common prefix into a single summary list result. If not provided, {@code
         * "/"} will be used.
         * <p>
         * See <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-prefixes.html">Organizing objects using
         * prefixes</a>
         *
         * @param delimiter the delimiter
         * @return This builder for method chaining.
         * @see #prefix(String)
         */
        Builder delimiter(String delimiter);

        /**
         * Add an optional request override configuration.
         *
         * @param configuration The override configuration.
         * @return This builder for method chaining.
         */
        Builder overrideConfiguration(DownloadDirectoryOverrideConfiguration configuration);

        /**
         * Similar to {@link #overrideConfiguration(DownloadDirectoryOverrideConfiguration)}, but takes a lambda to configure a
         * new {@link DownloadDirectoryOverrideConfiguration.Builder}. This removes the need to call {@link
         * DownloadDirectoryOverrideConfiguration#builder()} and {@link DownloadDirectoryOverrideConfiguration.Builder#build()}.
         *
         * @param downloadConfigurationBuilder the download configuration
         * @return this builder for method chaining.
         * @see #overrideConfiguration(DownloadDirectoryOverrideConfiguration)
         */
        default Builder overrideConfiguration(
            Consumer<DownloadDirectoryOverrideConfiguration.Builder> downloadConfigurationBuilder) {
            Validate.paramNotNull(downloadConfigurationBuilder, "downloadConfigurationBuilder");
            return overrideConfiguration(DownloadDirectoryOverrideConfiguration.builder()
                                                                               .applyMutation(downloadConfigurationBuilder)
                                                                               .build());
        }

        @Override
        DownloadDirectoryRequest build();
    }

    private static final class DefaultBuilder implements Builder {

        private Path destinationDirectory;
        private String bucket;
        private String prefix;
        private String delimiter;
        private DownloadDirectoryOverrideConfiguration configuration;

        private DefaultBuilder() {
        }

        private DefaultBuilder(DownloadDirectoryRequest request) {
            this.destinationDirectory = request.destinationDirectory;
            this.bucket = request.bucket;
            this.prefix = request.prefix;
            this.configuration = request.overrideConfiguration;
        }

        @Override
        public Builder destinationDirectory(Path destinationDirectory) {
            this.destinationDirectory = destinationDirectory;
            return this;
        }

        public void setDestinationDirectory(Path destinationDirectory) {
            destinationDirectory(destinationDirectory);
        }

        public Path getDestinationDirectory() {
            return destinationDirectory;
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
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public void setPrefix(String prefix) {
            prefix(prefix);
        }

        public String getPrefix() {
            return prefix;
        }

        @Override
        public Builder delimiter(String delimiter) {
            this.delimiter = delimiter;
            return this;
        }

        public void setDelimiter(String delimiter) {
            delimiter(delimiter);
        }

        public String getDelimiter() {
            return delimiter;
        }

        @Override
        public Builder overrideConfiguration(DownloadDirectoryOverrideConfiguration configuration) {
            this.configuration = configuration;
            return this;
        }

        public void setOverrideConfiguration(DownloadDirectoryOverrideConfiguration configuration) {
            overrideConfiguration(configuration);
        }

        public DownloadDirectoryOverrideConfiguration getOverrideConfiguration() {
            return configuration;
        }

        @Override
        public DownloadDirectoryRequest build() {
            return new DownloadDirectoryRequest(this);
        }
    }
}
