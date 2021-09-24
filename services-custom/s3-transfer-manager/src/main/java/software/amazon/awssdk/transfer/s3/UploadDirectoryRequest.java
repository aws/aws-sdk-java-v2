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
 */
@SdkPublicApi
@SdkPreviewApi
public final class UploadDirectoryRequest implements TransferRequest, ToCopyableBuilder<UploadDirectoryRequest.Builder,
    UploadDirectoryRequest> {

    private final Path sourceDirectory;
    private final String bucket;
    private final String prefix;
    private final UploadDirectoryConfiguration overrideConfiguration;

    public UploadDirectoryRequest(DefaultBuilder builder) {
        this.sourceDirectory = Validate.paramNotNull(builder.sourceDirectory, "sourceDirectory");
        this.bucket = Validate.paramNotNull(builder.bucket, "bucketName");
        this.prefix = builder.prefix;
        this.overrideConfiguration = builder.configuration;
    }

    /**
     * The source directory to upload
     *
     * @return the source directory
     */
    public Path sourceDirectory() {
        return sourceDirectory;
    }

    /**
     * The name of the bucket to upload objects to.
     *
     * @return bucket name
     */
    public String bucket() {
        return bucket;
    }

    /**
     * @return the key prefix of the virtual directory to upload to
     */
    public String prefix() {
        return prefix;
    }

    /**
     * @return the optional override configuration
     */
    public Optional<UploadDirectoryConfiguration> overrideConfiguration() {
        return Optional.ofNullable(overrideConfiguration);
    }

    public static Builder builder() {
        return new DefaultBuilder();
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
        return Objects.equals(overrideConfiguration, that.overrideConfiguration);
    }

    @Override
    public int hashCode() {
        int result = sourceDirectory.hashCode();
        result = 31 * result + bucket.hashCode();
        result = 31 * result + (prefix != null ? prefix.hashCode() : 0);
        result = 31 * result + (overrideConfiguration != null ? overrideConfiguration.hashCode() : 0);
        return result;
    }

    public interface Builder extends CopyableBuilder<Builder, UploadDirectoryRequest> {

        /**
         * Specify the source directory to upload
         *
         * @param sourceDirectory the source directory
         * @return This builder for method chaining.
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
         * Specify the key prefix of the virtual directory to upload to.
         * If not provided, files will be uploaded to the root of the bucket
         *
         * @param prefix the key prefix
         * @return This builder for method chaining.
         */
        Builder prefix(String prefix);

        /**
         * Add an optional request override configuration.
         *
         * @param configuration The override configuration.
         * @return This builder for method chaining.
         */
        Builder overrideConfiguration(UploadDirectoryConfiguration configuration);

        /**
         * Similar to {@link #overrideConfiguration(UploadDirectoryConfiguration)}, but takes a lambda to configure a new
         * {@link UploadDirectoryConfiguration.Builder}. This removes the need to call
         * {@link UploadDirectoryConfiguration#builder()} and {@link UploadDirectoryConfiguration.Builder#build()}.
         *
         * @param uploadConfigurationBuilder the upload configuration
         * @return this builder for method chaining.
         * @see #overrideConfiguration(UploadDirectoryConfiguration)
         */
        default Builder overrideConfiguration(Consumer<UploadDirectoryConfiguration.Builder> uploadConfigurationBuilder) {
            Validate.paramNotNull(uploadConfigurationBuilder, "uploadConfigurationBuilder");
            return overrideConfiguration(UploadDirectoryConfiguration.builder()
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
        private UploadDirectoryConfiguration configuration;

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

        @Override
        public Builder bucket(String bucket) {
            this.bucket = bucket;
            return this;
        }

        @Override
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        @Override
        public Builder overrideConfiguration(UploadDirectoryConfiguration configuration) {
            this.configuration = configuration;
            return this;
        }

        @Override
        public UploadDirectoryRequest build() {
            return new UploadDirectoryRequest(this);
        }
    }
}
