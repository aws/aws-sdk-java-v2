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

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.custom.s3.transfer.util.SizeConstant;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * The configuration object for multipart downloads in {@link S3TransferManager}.
 */
@SdkPublicApi
public final class MultipartDownloadConfiguration implements
        ToCopyableBuilder<MultipartDownloadConfiguration.Builder, MultipartDownloadConfiguration> {
    /**
     * The default value for whether multipart downloads are enabled.
     */
    public static final boolean DEFAULT_ENABLE_MULTIPART_DOWNLOADS = true;

    /**
     * The default value for the multipart download threshold.
     */
    public static final long DEFAULT_MULTIPART_DOWNLOAD_THRESHOLD = 16 * SizeConstant.MiB;

    /**
     * The default value for the maximum download part count.
     */
    public static final int DEFAULT_MAX_DOWNLOAD_PART_COUNT = 10_000;

    /**
     * The default for value for minimum download part size.
     */
    public static final long DEFAULT_MIN_DOWNLOAD_PART_SIZE = 5 * SizeConstant.MiB;

    private static final MultipartDownloadConfiguration DEFAULT = MultipartDownloadConfiguration.builder()
            .enableMultipartDownloads(DEFAULT_ENABLE_MULTIPART_DOWNLOADS)
            .multipartDownloadThreshold(DEFAULT_MULTIPART_DOWNLOAD_THRESHOLD)
            .maxDownloadPartCount(DEFAULT_MAX_DOWNLOAD_PART_COUNT)
            .minDownloadPartSize(DEFAULT_MIN_DOWNLOAD_PART_SIZE)
            .build();

    private final Boolean enableMultipartDownloads;
    private final Long multipartDownloadThreshold;
    private final Integer maxDownloadPartCount;
    private final Long minDownloadPartSize;

    private MultipartDownloadConfiguration(BuilderImpl builder) {
        this.enableMultipartDownloads = resolveEnableMultipartDownloads(builder.enableMultipartDownloads);
        this.multipartDownloadThreshold = resolveMultipartDownloadThreshold(builder.multipartDownloadThreshold);
        this.maxDownloadPartCount = resolveMaxDownloadPartCount(builder.maxDownloadPartCount);
        this.minDownloadPartSize = resolveMinDownloadPartSize(builder.minDownloadPartSize);
        validateConfig();
    }

    /**
     * @return Whether multipart downloads are enabled.
     */
    public Boolean enableMultipartDownloads() {
        return enableMultipartDownloads;
    }

    /**
     * @return The minimum size for an object for it to be downloaded in
     * multiple parts.
     */
    public Long multipartDownloadThreshold() {
        return multipartDownloadThreshold;
    }

    /**
     * @return The maximum number of parts to download a single object.
     */
    public Integer maxDownloadPartCount() {
        return maxDownloadPartCount;
    }

    /**
     * @return The minimum size for a part of an object to download.
     */
    public Long minDownloadPartSize() {
        return minDownloadPartSize;
    }

    /**
     * @return A new {@link Builder} with the current values of this
     * configuration.
     */
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    /**
     * @return An instance of this class using the default values.
     */
    public static MultipartDownloadConfiguration defaultConfig() {
        return DEFAULT;
    }

    private Boolean resolveEnableMultipartDownloads(Boolean configured) {
        return Optional.ofNullable(configured).orElse(DEFAULT_ENABLE_MULTIPART_DOWNLOADS);
    }

    private Long resolveMultipartDownloadThreshold(Long configured) {
        return Optional.ofNullable(configured).orElse(DEFAULT_MULTIPART_DOWNLOAD_THRESHOLD);
    }

    private Integer resolveMaxDownloadPartCount(Integer configured) {
        return Optional.ofNullable(configured).orElse(DEFAULT_MAX_DOWNLOAD_PART_COUNT);
    }

    private Long resolveMinDownloadPartSize(Long configured) {
        return Optional.ofNullable(configured).orElse(DEFAULT_MIN_DOWNLOAD_PART_SIZE);
    }

    private void validateConfig() {
        Validate.isPositive(multipartDownloadThreshold, "multipartDownloadThreshold");
        Validate.isPositive(maxDownloadPartCount, "maxDownloadPartCount");
        Validate.isTrue(maxDownloadPartCount <= DEFAULT_MAX_DOWNLOAD_PART_COUNT,
                "maxDownloadPartCount must be at most %d", DEFAULT_MAX_DOWNLOAD_PART_COUNT);
        Validate.isPositive(minDownloadPartSize, "minDownloadPartSize");
        Validate.isTrue(minDownloadPartSize <= multipartDownloadThreshold,
                "minDownloadPartSize must not be greater than multipartDownloadThreshold");
    }

    public interface Builder extends CopyableBuilder<Builder, MultipartDownloadConfiguration> {
        /**
         * Set whether multipart downloads are enabled.
         *
         * @param enableMultipartDownloads Whether multipart downloads are
         * enabled.
         * @return This object for method chaining.
         */
        Builder enableMultipartDownloads(Boolean enableMultipartDownloads);

        /**
         * Set the minimum size for an object for it to be downloaded in
         * multiple parts.
         *
         * @param multipartDownloadThreshold The threshold.
         * @return This object for method chaining.
         */
        Builder multipartDownloadThreshold(Long multipartDownloadThreshold);

        /**
         * Set the maximum number of parts to download a single object.
         *
         * @param maxDownloadPartCount The maximum part count.
         * @return This object for method chaining.
         */
        Builder maxDownloadPartCount(Integer maxDownloadPartCount);

        /**
         * Set the minimum size for a part of an object to download.
         *
         * @param minDownloadPartSize The minimum part size.
         * @return This object for method chaining.
         */
        Builder minDownloadPartSize(Long minDownloadPartSize);
    }

    private static final class BuilderImpl implements Builder {
        private Boolean enableMultipartDownloads;
        private Long multipartDownloadThreshold;
        private Integer maxDownloadPartCount;
        private Long minDownloadPartSize;

        private BuilderImpl() {
        }

        private BuilderImpl(MultipartDownloadConfiguration original) {
            enableMultipartDownloads = original.enableMultipartDownloads;
            multipartDownloadThreshold = original.multipartDownloadThreshold;
            maxDownloadPartCount = original.maxDownloadPartCount;
            minDownloadPartSize = original.minDownloadPartSize;
        }

        @Override
        public Builder enableMultipartDownloads(Boolean enableMultipartDownloads) {
            this.enableMultipartDownloads = enableMultipartDownloads;
            return this;
        }

        @Override
        public Builder multipartDownloadThreshold(Long multipartDownloadThreshold) {
            this.multipartDownloadThreshold = multipartDownloadThreshold;
            return this;
        }

        @Override
        public Builder maxDownloadPartCount(Integer maxDownloadPartCount) {
            this.maxDownloadPartCount = maxDownloadPartCount;
            return this;
        }

        @Override
        public Builder minDownloadPartSize(Long minDownloadPartSize) {
            this.minDownloadPartSize = minDownloadPartSize;
            return this;
        }

        @Override
        public MultipartDownloadConfiguration build() {
            return new MultipartDownloadConfiguration(this);
        }
    }
}
