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

package software.amazon.awssdk.services.s3.multipart;

import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Class that hold configuration properties related to multipart operation for a {@link S3AsyncClient}. Passing this class to the
 * {@link S3AsyncClientBuilder#multipartConfiguration(MultipartConfiguration)} will enable automatic conversion of
 * {@link S3AsyncClient#putObject(Consumer, AsyncRequestBody)}, {@link S3AsyncClient#copyObject(CopyObjectRequest)} to their
 * respective multipart operation.
 * <p></p>
 * note: The multipart operation for {@link S3AsyncClient#getObject(GetObjectRequest, AsyncResponseTransformer)} is temporarily
 * disabled and will result in throwing a {@link UnsupportedOperationException} if called when configured for multipart operation.
 */
@SdkPublicApi
public final class MultipartConfiguration implements ToCopyableBuilder<MultipartConfiguration.Builder, MultipartConfiguration> {
    public static final AttributeMap.Key<MultipartConfiguration> MULTIPART_CONFIGURATION_KEY =
        new AttributeMap.Key<MultipartConfiguration>(MultipartConfiguration.class){};

    private final Boolean multipartEnabled;
    private final Long thresholdInBytes;
    private final Long minimumPartSizeInBytes;
    private final Long maximumMemoryUsageInBytes;
    private final MultipartDownloadType multipartDownloadType;

    private MultipartConfiguration(DefaultMultipartConfigBuilder builder) {
        this.multipartEnabled = Validate.getOrDefault(builder.multipartEnabled, () -> Boolean.TRUE);
        this.thresholdInBytes = builder.thresholdInBytes;
        this.minimumPartSizeInBytes = builder.minimumPartSizeInBytes;
        this.maximumMemoryUsageInBytes = builder.maximumMemoryUsageInBytes;
        this.multipartDownloadType = builder.multipartDownloadType;
    }

    public static Builder builder() {
        return new DefaultMultipartConfigBuilder();
    }

    public static MultipartConfiguration create() {
        return builder().build();
    }

    @Override
    public Builder toBuilder() {
        return builder()
            .minimumPartSizeInBytes(minimumPartSizeInBytes)
            .multipartDownloadType(multipartDownloadType)
            .multipartEnabled(multipartEnabled)
            .thresholdInBytes(thresholdInBytes);
    }

    /**
     * Indicated if the {@link S3AsyncClient} should use multipart operations of not.
     * @return if the {@link S3AsyncClient} should use multipart operations of not.
     */
    public Boolean multipartEnabled() {
        return this.multipartEnabled;
    }

    /**
     * Indicates the value of the configured threshold. Any request whose body size is less than the configured value will not
     * use multipart operation
     * @return the value of the configured threshold.
     */
    public Long thresholdInBytes() {
        return this.thresholdInBytes;
    }

    public Long minimumPartSizeInBytes() {
        return this.minimumPartSizeInBytes;
    }

    public Long maximumMemoryUsageInBytes() {
        return this.maximumMemoryUsageInBytes;
    }

    public MultipartDownloadType multipartDownloadType() {
        return this.multipartDownloadType;
    }


    /**
     * Builder for a {@link MultipartConfiguration}.
     */
    public interface Builder extends CopyableBuilder<Builder, MultipartConfiguration> {

        /**
         * Defines if the client should use multipart operation or not. <p></p>
         * Default value: True.
         * @param multipartEnabled the value of the boolean to set. Set this to false to disable multipart operation completely.
         * @return an instance of this builder.
         */
        Builder multipartEnabled(Boolean multipartEnabled);

        /**
         * Indicated if the {@link S3AsyncClient} should use multipart operations of not.
         * @return if the {@link S3AsyncClient} should use multipart operations of not.
         */
        Boolean multipartEnabled();

        /**
         * Defines the minimum number of bytes of the body of the request required for requests to be converted to their multipart
         * equivalent. Any request whose body size is less than the configured value will not use multipart operation,
         * regardless of the value passed to {@link Builder#multipartEnabled(Boolean)}
         * @param thresholdInBytes the value of the threshold to set.
         * @return an instance of this builder.
         */
        Builder thresholdInBytes(Long thresholdInBytes);

        /**
         * Indicates the value of the configured threshold.
         * @return the value of the configured threshold.
         */
        Long thresholdInBytes();

        /**
         *
         * @param minimumPartSizeInBytes
         * @return an instance of this builder.
         */
        Builder minimumPartSizeInBytes(Long minimumPartSizeInBytes);

        /**
         *
         * @return
         */
        Long minimumPartSizeInBytes();

        Builder maximumMemoryUsageInBytes(Long maximumMemoryUsageInBytes);

        /**
         *
         * @return
         */
        Long maximumMemoryUsageInBytes();

        /**
         *
         * @param multipartDownloadType
         * @return an instance of this builder.
         */
        Builder multipartDownloadType(MultipartDownloadType multipartDownloadType);

        /**
         *
         * @return
         */
        MultipartDownloadType multipartDownloadType();
    }

    private static class DefaultMultipartConfigBuilder implements Builder {
        private Boolean multipartEnabled;
        private Long thresholdInBytes;
        private Long minimumPartSizeInBytes;
        private Long maximumMemoryUsageInBytes;
        private MultipartDownloadType multipartDownloadType;

        public Builder multipartEnabled(Boolean multipartEnabled) {
            this.multipartEnabled = multipartEnabled;
            return this;
        }

        public Boolean multipartEnabled() {
            return this.multipartEnabled;
        }

        public Builder thresholdInBytes(Long thresholdInBytes) {
            this.thresholdInBytes = thresholdInBytes;
            return this;
        }

        public Long thresholdInBytes() {
            return this.thresholdInBytes;
        }

        public Builder minimumPartSizeInBytes(Long minimumPartSizeInBytes) {
            this.minimumPartSizeInBytes = minimumPartSizeInBytes;
            return this;
        }

        public Long minimumPartSizeInBytes() {
            return this.minimumPartSizeInBytes;
        }

        @Override
        public Builder maximumMemoryUsageInBytes(Long maximumMemoryUsageInBytes) {
            this.maximumMemoryUsageInBytes = maximumMemoryUsageInBytes;
            return this;
        }

        @Override
        public Long maximumMemoryUsageInBytes() {
            return maximumMemoryUsageInBytes;
        }

        public Builder multipartDownloadType(MultipartDownloadType multipartDownloadType) {
            this.multipartDownloadType = multipartDownloadType;
            return this;
        }

        public MultipartDownloadType multipartDownloadType() {
            return this.multipartDownloadType;
        }

        @Override
        public MultipartConfiguration build() {
            return new MultipartConfiguration(this);
        }
    }
}
