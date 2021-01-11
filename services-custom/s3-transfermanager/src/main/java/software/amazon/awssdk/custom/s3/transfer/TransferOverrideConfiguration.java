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

package software.amazon.awssdk.custom.s3.transfer;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Override configuration for a single transfer. Override configurations take
 * precedence those set on {@link S3TransferManager}.
 */
@SdkPublicApi
public final class TransferOverrideConfiguration implements
        ToCopyableBuilder<TransferOverrideConfiguration.Builder, TransferOverrideConfiguration> {
    private final Long maxTransferBytesPerSecond;
    private final MultipartDownloadConfiguration multipartDownloadConfiguration;
    private final MultipartUploadConfiguration multipartUploadConfiguration;

    private TransferOverrideConfiguration(BuilderImpl builder) {
        this.maxTransferBytesPerSecond = builder.maxTransferBytesPerSecond;
        this.multipartDownloadConfiguration = builder.multipartDownloadConfiguration;
        this.multipartUploadConfiguration = builder.multipartUploadConfiguration;
    }

    /**
     * @return The maximum rate for this transfer in bytes per second.
     */
    public Long maxTransferBytesPerSecond() {
        return maxTransferBytesPerSecond;
    }

    /**
     * @return The multipart download configuration to use for this transfer.
     */
    public MultipartDownloadConfiguration multipartDownloadConfiguration() {
        return multipartDownloadConfiguration;
    }

    /**
     * @return The multipart upload configuration to use for this transfer.
     */
    public MultipartUploadConfiguration multipartUploadConfiguration() {
        return multipartUploadConfiguration;
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder extends CopyableBuilder<Builder, TransferOverrideConfiguration> {
        /**
         * Set the maximum rate for this transfer in bytes per second.
         *
         * @param maxTransferBytesPerSecond The maximum rate.
         * @return This object for method chaining.
         */
        Builder maxTransferBytesPerSecond(Long maxTransferBytesPerSecond);

        /**
         * Set the multipart download configuration to use for this transfer.
         *
         * @param multipartDownloadConfiguration The multipart download
         * configuration.
         * @return This object for method chaining.
         */
        Builder multipartDownloadConfiguration(MultipartDownloadConfiguration multipartDownloadConfiguration);

        /**
         * Set the multipart upload configuration to use for this transfer.
         *
         * @param multipartUploadConfiguration The multipart upload
         * configuration.
         * @return This object for method chaining.
         */
        Builder multipartUploadConfiguration(MultipartUploadConfiguration multipartUploadConfiguration);

        /**
         * @return The build override configuration.
         */
        TransferOverrideConfiguration build();
    }

    private static final class BuilderImpl implements Builder {
        private Long maxTransferBytesPerSecond;
        private MultipartDownloadConfiguration multipartDownloadConfiguration;
        private MultipartUploadConfiguration multipartUploadConfiguration;

        private BuilderImpl(TransferOverrideConfiguration other) {
            this.maxTransferBytesPerSecond = other.maxTransferBytesPerSecond;
            this.multipartDownloadConfiguration = other.multipartDownloadConfiguration;
            this.multipartUploadConfiguration = other.multipartUploadConfiguration;
        }

        private BuilderImpl() {
        }

        @Override
        public Builder maxTransferBytesPerSecond(Long maxTransferBytesPerSecond) {
            this.maxTransferBytesPerSecond = maxTransferBytesPerSecond;
            return this;
        }

        @Override
        public Builder multipartDownloadConfiguration(MultipartDownloadConfiguration multipartDownloadConfiguration) {
            this.multipartDownloadConfiguration = multipartDownloadConfiguration;
            return this;
        }

        @Override
        public Builder multipartUploadConfiguration(MultipartUploadConfiguration multipartUploadConfiguration) {
            this.multipartUploadConfiguration = multipartUploadConfiguration;
            return this;
        }

        @Override
        public TransferOverrideConfiguration build() {
            return new TransferOverrideConfiguration(this);
        }
    }
}
