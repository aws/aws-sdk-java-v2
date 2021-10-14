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
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPreviewApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Configuration options for how {@link S3TransferManager} processes requests.
 * <p>
 * All values are optional, and not specifying them will use default values provided bt the SDK.
 *
 * <p>Use {@link #builder()} to create a set of options.
 * @see S3TransferManager.Builder#transferConfiguration(S3TransferManagerOverrideConfiguration)
 */
@SdkPublicApi
@SdkPreviewApi
public final class S3TransferManagerOverrideConfiguration implements
                                                          ToCopyableBuilder<S3TransferManagerOverrideConfiguration.Builder,
                                                              S3TransferManagerOverrideConfiguration> {
    private final Executor executor;
    private final UploadDirectoryOverrideConfiguration uploadDirectoryConfiguration;

    private S3TransferManagerOverrideConfiguration(DefaultBuilder builder) {
        this.executor = builder.executor;
        this.uploadDirectoryConfiguration = builder.uploadDirectoryConfiguration;
    }

    /**
     * @return the optional SDK executor specified
     */
    public Optional<Executor> executor() {
        return Optional.ofNullable(executor);
    }

    /**
     * @return the optional upload directory configuration specified
     */
    public Optional<UploadDirectoryOverrideConfiguration> uploadDirectoryConfiguration() {
        return Optional.ofNullable(uploadDirectoryConfiguration);
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

        S3TransferManagerOverrideConfiguration that = (S3TransferManagerOverrideConfiguration) o;

        if (!Objects.equals(executor, that.executor)) {
            return false;
        }
        return Objects.equals(uploadDirectoryConfiguration, that.uploadDirectoryConfiguration);
    }

    @Override
    public int hashCode() {
        int result = executor != null ? executor.hashCode() : 0;
        result = 31 * result + (uploadDirectoryConfiguration != null ? uploadDirectoryConfiguration.hashCode() : 0);
        return result;
    }

    /**
     * Creates a default builder for {@link S3TransferManagerOverrideConfiguration}.
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }

    public static Class<? extends Builder> serializableBuilderClass() {
        return DefaultBuilder.class;
    }

    /**
     * The builder definition for a {@link S3TransferManagerOverrideConfiguration}.
     */
    public interface Builder extends CopyableBuilder<Builder, S3TransferManagerOverrideConfiguration> {

        /**
         * Specify the executor that {@link S3TransferManager} will use to execute background tasks before handing them off to
         * the underlying S3 async client, such as visiting file tree in a
         * {@link S3TransferManager#uploadDirectory(UploadDirectoryRequest)} operation
         *
         * <p>
         * The SDK will create an executor if not provided
         *
         * <p>
         * <b>This executor must be shut down by the user when it is ready to be disposed. The SDK will not close the executor
         * when the s3 transfer manager is closed.</b>
         *
         * @param executor the executor to use
         * @return this builder for method chaining.
         */
        Builder executor(Executor executor);

        /**
         * Specify the configuration options for upload directory operation
         *
         * @param uploadDirectoryConfiguration the configuration for upload directory
         * @return this builder for method chaining.
         * @see S3TransferManager#uploadDirectory(UploadDirectoryRequest)
         */
        Builder uploadDirectoryConfiguration(UploadDirectoryOverrideConfiguration uploadDirectoryConfiguration);

        /**
         * Similar to {@link #uploadDirectoryConfiguration}, but takes a lambda to configure a new
         * {@link UploadDirectoryOverrideConfiguration.Builder}. This removes the need to call
         * {@link UploadDirectoryOverrideConfiguration#builder()} and
         * {@link UploadDirectoryOverrideConfiguration.Builder#build()}.
         *
         * @param uploadConfigurationBuilder the configuration for upload directory
         * @return this builder for method chaining.
         * @see #uploadDirectoryConfiguration(UploadDirectoryOverrideConfiguration)
         */
        default Builder uploadDirectoryConfiguration(Consumer<UploadDirectoryOverrideConfiguration.Builder>
                                                         uploadConfigurationBuilder) {
            Validate.paramNotNull(uploadConfigurationBuilder, "uploadConfigurationBuilder");
            return uploadDirectoryConfiguration(UploadDirectoryOverrideConfiguration.builder()
                                                                                    .applyMutation(uploadConfigurationBuilder)
                                                                                    .build());
        }
    }

    private static final class DefaultBuilder implements Builder {
        private Executor executor;
        private UploadDirectoryOverrideConfiguration uploadDirectoryConfiguration;

        private DefaultBuilder() {
        }

        private DefaultBuilder(S3TransferManagerOverrideConfiguration configuration) {
            this.executor = configuration.executor;
            this.uploadDirectoryConfiguration = configuration.uploadDirectoryConfiguration;
        }

        @Override
        public Builder executor(Executor executor) {
            this.executor = executor;
            return this;
        }

        public void setExecutor(Executor executor) {
            executor(executor);
        }

        public Executor getExecutor() {
            return executor;
        }

        @Override
        public Builder uploadDirectoryConfiguration(UploadDirectoryOverrideConfiguration uploadDirectoryConfiguration) {
            this.uploadDirectoryConfiguration = uploadDirectoryConfiguration;
            return this;
        }

        public void setUploadDirectoryConfiguration(UploadDirectoryOverrideConfiguration uploadDirectoryConfiguration) {
            uploadDirectoryConfiguration(uploadDirectoryConfiguration);
        }

        public UploadDirectoryOverrideConfiguration getUploadDirectoryConfiguration() {
            return uploadDirectoryConfiguration;
        }

        @Override
        public S3TransferManagerOverrideConfiguration build() {
            return new S3TransferManagerOverrideConfiguration(this);
        }
    }
}
