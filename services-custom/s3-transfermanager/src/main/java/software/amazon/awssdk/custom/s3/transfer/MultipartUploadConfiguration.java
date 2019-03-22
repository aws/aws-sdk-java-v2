/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
 * The configuration object for multipart uplaods in {@link S3TransferManager}.
 */
@SdkPublicApi
public final class MultipartUploadConfiguration implements
        ToCopyableBuilder<MultipartUploadConfiguration.Builder, MultipartUploadConfiguration> {
    /**
     * The default value for whether multipart uploads are enabled.
     */
    public static final boolean DEFAULT_ENABLE_MULTIPART_UPLOAD = true;

    /**
     * The default value for the multipart upload threshold.
     */
    public static final long DEFAULT_MULTIPART_UPLOAD_THRESHOLD = 16 * SizeConstant.MiB;

    /**
     * The default value for maximum upload part count.
     */
    public static final int DEFAULT_MAX_UPLOAD_PART_COUNT = 10_000;

    /**
     * The default for value for minimum upload part size.
     */
    public static final long DEFAULT_MIN_UPLOAD_PART_SIZE = 5 * SizeConstant.MiB;

    public static final long MULTIPART_MIN_PART_SIZE = 5 * SizeConstant.MiB;


    private static final MultipartUploadConfiguration DEFAULT = MultipartUploadConfiguration.builder()
            .enableMultipartUploads(DEFAULT_ENABLE_MULTIPART_UPLOAD)
            .multipartUploadThreshold(DEFAULT_MULTIPART_UPLOAD_THRESHOLD)
            .maxUploadPartCount(DEFAULT_MAX_UPLOAD_PART_COUNT)
            .minUploadPartSize(DEFAULT_MIN_UPLOAD_PART_SIZE)
            .build();

    private final Boolean enableMultipartUploads;
    private final Long multipartUploadThreshold;
    private final Integer maxUploadPartCount;
    private final Long minUploadPartSize;

    private MultipartUploadConfiguration(BuilderImpl builder) {
        this.enableMultipartUploads = resolveEnableMultipartUploads(builder.enableMultipartUploads);
        this.multipartUploadThreshold = resolveMultipartUploadThreshold(builder.multipartUploadThreshold);
        this.maxUploadPartCount = resolveMaxUploadPartCount(builder.maxUploadPartCount);
        this.minUploadPartSize = resolveMinUploadPartSize(builder.minUploadPartSize);
        validateConfig();
    }

    /**
     * @return Whether multipart uploads are enabled.
     */
    public Boolean enableMultipartUploads() {
        return enableMultipartUploads;
    }

    /**
     * @return The minimum size for an object for it to be uploaded in multiple
     * parts.
     */
    public Long multipartUploadThreshold() {
        return multipartUploadThreshold;
    }

    /**
     * @return The maximum number of parts to upload an object in.
     */
    public Integer maxUploadPartCount() {
        return maxUploadPartCount;
    }

    /**
     * @return The minimum size for a part of an object to upload.
     */
    public Long minimumUploadPartSize() {
        return minUploadPartSize;
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    /**
     * @return An instance of this class using the default values.
     */
    public static MultipartUploadConfiguration defaultConfig() {
        return DEFAULT;
    }

    private static Boolean resolveEnableMultipartUploads(Boolean configured) {
        return Optional.ofNullable(configured).orElse(DEFAULT_ENABLE_MULTIPART_UPLOAD);
    }

    private static Long resolveMultipartUploadThreshold(Long configured) {
        return Optional.ofNullable(configured).orElse(DEFAULT_MULTIPART_UPLOAD_THRESHOLD);
    }

    private static Integer resolveMaxUploadPartCount(Integer configured) {
        return Optional.ofNullable(configured).orElse(DEFAULT_MAX_UPLOAD_PART_COUNT);
    }

    private static Long resolveMinUploadPartSize(Long configured) {
        return Optional.ofNullable(configured).orElse(DEFAULT_MIN_UPLOAD_PART_SIZE);
    }

    private void validateConfig() {
        Validate.isPositive(multipartUploadThreshold, "multipartUploadThreshold");
        Validate.isPositive(minUploadPartSize, "minUploadPartSize");
        Validate.isTrue(minUploadPartSize >= MULTIPART_MIN_PART_SIZE,
                        "minUploadPartSize must be at least %d", MULTIPART_MIN_PART_SIZE);
        Validate.isPositive(maxUploadPartCount, "maxUploadPartCount");
        Validate.isTrue(maxUploadPartCount <= DEFAULT_MAX_UPLOAD_PART_COUNT,
                        "maxUploadPartCount must be at most %d", DEFAULT_MAX_UPLOAD_PART_COUNT);

    }

    public interface Builder extends CopyableBuilder<Builder, MultipartUploadConfiguration> {
        /**
         * Set whether multipart uploads are enabled.
         *
         * @param enableMultipartUploads Whether multipart uploads are enabled.
         * @return This object for method chaining.
         */
        Builder enableMultipartUploads(Boolean enableMultipartUploads);

        /**
         * Set the minimum size for an object for it to be uploaded in
         * multiple parts.
         *
         * @param multipartUploadThreshold The threshold.
         * @return This object for method chaining.
         */
        Builder multipartUploadThreshold(Long multipartUploadThreshold);

        /**
         * Se the maximum number of parts to upload an object in. This must be
         * {@code <=} 10,000, the maximum parts allowed by S3 for a multipart
         * object.
         *
         * @param maxUploadPartCount The max part count.
         * @return This object for method chaining.
         */
        Builder maxUploadPartCount(Integer maxUploadPartCount);

        /**
         * Set the minimum size for a part of an object to upload.
         *
         * @param minimumUploadPartSize The minimum part size.
         * @return This object for method chaining.
         */
        Builder minUploadPartSize(Long minimumUploadPartSize);
    }

    private static final class BuilderImpl implements Builder {
        private Boolean enableMultipartUploads;
        private Long multipartUploadThreshold;
        private Integer maxUploadPartCount;
        private Long minUploadPartSize;

        private BuilderImpl(MultipartUploadConfiguration other) {
            this.enableMultipartUploads = other.enableMultipartUploads;
            this.multipartUploadThreshold = other.multipartUploadThreshold;
            this.maxUploadPartCount = other.maxUploadPartCount;
            this.minUploadPartSize = other.minUploadPartSize;
        }

        private BuilderImpl() {
        }

        @Override
        public Builder enableMultipartUploads(Boolean enableMultipartUploads) {
            this.enableMultipartUploads = enableMultipartUploads;
            return this;
        }

        @Override
        public Builder multipartUploadThreshold(Long multipartUploadThreshold) {
            this.multipartUploadThreshold = multipartUploadThreshold;
            return this;
        }

        @Override
        public Builder maxUploadPartCount(Integer maxUploadPartCount) {
            this.maxUploadPartCount = maxUploadPartCount;
            return this;
        }

        @Override
        public Builder minUploadPartSize(Long minUploadPartSize) {
            this.minUploadPartSize = minUploadPartSize;
            return this;
        }

        @Override
        public MultipartUploadConfiguration build() {
            return new MultipartUploadConfiguration(this);
        }
    }
}
