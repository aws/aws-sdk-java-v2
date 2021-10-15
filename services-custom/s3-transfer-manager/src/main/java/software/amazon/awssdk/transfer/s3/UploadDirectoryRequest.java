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
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Request object to upload a local directory to S3 using the Transfer Manager.
 *
 * @see S3TransferManager#uploadDirectory(UploadDirectoryRequest)
 */
@SdkPublicApi
@SdkPreviewApi
public final class UploadDirectoryRequest implements TransferRequest, ToCopyableBuilder<UploadDirectoryRequest.Builder,
    UploadDirectoryRequest> {

    private final Path sourceDirectory;
    private final String bucket;
    private final String prefix;
    private final UploadDirectoryOverrideConfiguration overrideConfiguration;
    private final String delimiter;

    public UploadDirectoryRequest(DefaultBuilder builder) {
        this.sourceDirectory = Validate.paramNotNull(builder.sourceDirectory, "sourceDirectory");
        this.bucket = Validate.paramNotNull(builder.bucket, "bucket");
        this.prefix = builder.prefix;
        this.overrideConfiguration = builder.configuration;
        this.delimiter = builder.delimiter;
    }

    /**
     * The source directory to upload
     *
     * @return the source directory
     * @see Builder#sourceDirectory(Path)
     */
    public Path sourceDirectory() {
        return sourceDirectory;
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
     * @see Builder#overrideConfiguration(UploadDirectoryOverrideConfiguration)
     */
    public Optional<UploadDirectoryOverrideConfiguration> overrideConfiguration() {
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

        UploadDirectoryRequest that = (UploadDirectoryRequest) o;

        if (!sourceDirectory.equals(that.sourceDirectory)) {
            return false;
        }
        if (!bucket.equals(that.bucket)) {
            return false;
        }
        if (!Objects.equals(prefix, that.prefix)) {
            return false;
        }
        if (!Objects.equals(delimiter, that.delimiter)) {
            return false;
        }
        return Objects.equals(overrideConfiguration, that.overrideConfiguration);
    }

    @Override
    public int hashCode() {
        int result = sourceDirectory.hashCode();
        result = 31 * result + bucket.hashCode();
        result = 31 * result + (prefix != null ? prefix.hashCode() : 0);
        result = 31 * result + (delimiter != null ? delimiter.hashCode() : 0);
        result = 31 * result + (overrideConfiguration != null ? overrideConfiguration.hashCode() : 0);
        return result;
    }

    public interface Builder extends CopyableBuilder<Builder, UploadDirectoryRequest> {

        /**
         * Specify the source directory to upload. The source directory must exist.
         * Fle wildcards are not supported and treated literally. Hidden files/directories are visited.
         *
         * <p>
         * Note that the current user must have read access to all directories and files,
         * otherwise {@link SecurityException} will be thrown.
         *
         * @param sourceDirectory the source directory
         * @return This builder for method chaining.
         * @see UploadDirectoryOverrideConfiguration
         */
        Builder sourceDirectory(Path sourceDirectory);

        /**
         * The name of the bucket to upload objects to.
         *
         * @param bucket the bucket name
         * @return This builder for method chaining.
         */
        Builder bucket(String bucket);

        /**
         * Specify the key prefix to use for the objects. If not provided, files will be uploaded to the root of the bucket
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
         * @param prefix the key prefix
         * @return This builder for method chaining.
         * @see #delimiter(String)
         */
        Builder prefix(String prefix);

        /**
         * Specify the delimiter. A delimiter causes a list operation to roll up all the keys that share a common prefix into a
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
        Builder overrideConfiguration(UploadDirectoryOverrideConfiguration configuration);

        /**
         * Similar to {@link #overrideConfiguration(UploadDirectoryOverrideConfiguration)}, but takes a lambda to configure a new
         * {@link UploadDirectoryOverrideConfiguration.Builder}. This removes the need to call
         * {@link UploadDirectoryOverrideConfiguration#builder()} and
         * {@link UploadDirectoryOverrideConfiguration.Builder#build()}.
         *
         * @param uploadConfigurationBuilder the upload configuration
         * @return this builder for method chaining.
         * @see #overrideConfiguration(UploadDirectoryOverrideConfiguration)
         */
        default Builder overrideConfiguration(Consumer<UploadDirectoryOverrideConfiguration.Builder> uploadConfigurationBuilder) {
            Validate.paramNotNull(uploadConfigurationBuilder, "uploadConfigurationBuilder");
            return overrideConfiguration(UploadDirectoryOverrideConfiguration.builder()
                                                                             .applyMutation(uploadConfigurationBuilder)
                                                                             .build());
        }

        @Override
        UploadDirectoryRequest build();
    }

    private static final class DefaultBuilder implements Builder {

        private Path sourceDirectory;
        private String bucket;
        private String prefix;
        private UploadDirectoryOverrideConfiguration configuration;
        private String delimiter;

        private DefaultBuilder() {
        }

        private DefaultBuilder(UploadDirectoryRequest request) {
            this.sourceDirectory = request.sourceDirectory;
            this.bucket = request.bucket;
            this.prefix = request.prefix;
            this.configuration = request.overrideConfiguration;
        }

        @Override
        public Builder sourceDirectory(Path sourceDirectory) {
            this.sourceDirectory = sourceDirectory;
            return this;
        }

        public void setSourceDirectory(Path sourceDirectory) {
            sourceDirectory(sourceDirectory);
        }

        public Path getSourceDirectory() {
            return sourceDirectory;
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
        public Builder overrideConfiguration(UploadDirectoryOverrideConfiguration configuration) {
            this.configuration = configuration;
            return this;
        }

        public void setOverrideConfiguration(UploadDirectoryOverrideConfiguration configuration) {
            overrideConfiguration(configuration);
        }

        public UploadDirectoryOverrideConfiguration getOverrideConfiguration() {
            return configuration;
        }

        @Override
        public UploadDirectoryRequest build() {
            return new UploadDirectoryRequest(this);
        }
    }
}
