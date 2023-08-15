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
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Class that hold configuration properties related to multipart operation for a {@link S3AsyncClient}. Passing this class to the
 * {@link S3AsyncClientBuilder#multipartConfiguration(MultipartConfiguration)} will enable automatic conversion of
 * {@link S3AsyncClient#putObject(Consumer, AsyncRequestBody)}, {@link S3AsyncClient#copyObject(CopyObjectRequest)} to their
 * respective multipart operation.
 * <p>
 * <em>Note</em>: The multipart operation for {@link S3AsyncClient#getObject(GetObjectRequest, AsyncResponseTransformer)} is
 * temporarily disabled and will result in throwing a {@link UnsupportedOperationException} if called when configured for
 * multipart operation.
 */
@SdkPublicApi
public final class MultipartConfiguration implements ToCopyableBuilder<MultipartConfiguration.Builder, MultipartConfiguration> {

    private final Long thresholdInBytes;
    private final Long minimumPartSizeInBytes;
    private final Long apiCallBufferSizeInBytes;

    private MultipartConfiguration(DefaultMultipartConfigBuilder builder) {
        this.thresholdInBytes = builder.thresholdInBytes;
        this.minimumPartSizeInBytes = builder.minimumPartSizeInBytes;
        this.apiCallBufferSizeInBytes = builder.apiCallBufferSizeInBytes;
    }

    public static Builder builder() {
        return new DefaultMultipartConfigBuilder();
    }

    @Override
    public Builder toBuilder() {
        return builder()
            .apiCallBufferSizeInBytes(apiCallBufferSizeInBytes)
            .minimumPartSizeInBytes(minimumPartSizeInBytes)
            .thresholdInBytes(thresholdInBytes);
    }

    /**
     * Indicates the value of the configured threshold, in bytes. Any request whose size is less than the configured value will
     * not use multipart operation
     * @return the value of the configured threshold.
     */
    public Long thresholdInBytes() {
        return this.thresholdInBytes;
    }

    /**
     * Indicated the size, in bytes, of each individual part of the part requests. The actual part size used might be bigger to
     * conforms to the maximum number of parts allowed per multipart requests.
     * @return the value of the configured part size.
     */
    public Long minimumPartSizeInBytes() {
        return this.minimumPartSizeInBytes;
    }

    /**
     * The maximum memory, in bytes, that the SDK will use to buffer requests content into memory.
     * @return the value of the configured maximum memory usage.
     */
    public Long apiCallBufferSizeInBytes() {
        return this.apiCallBufferSizeInBytes;
    }

    /**
     * Builder for a {@link MultipartConfiguration}.
     */
    public interface Builder extends CopyableBuilder<Builder, MultipartConfiguration> {

        /**
         * Configure the size threshold, in bytes, for when to use multipart upload. Uploads/copies over this size will
         * automatically use a multipart upload strategy, while uploads/copies smaller than this threshold will use a single
         * connection to upload/copy the whole object.
         *
         * <p>
         * Multipart uploads are easier to recover from and also potentially faster than single part uploads, especially when the
         * upload parts can be uploaded in parallel. Because there are additional network API calls, small objects are still
         * recommended to use a single connection for the upload. See
         * <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/mpuoverview.html">Uploading and copying objects using
         * multipart upload</a>.
         *
         * <p>
         * By default, it is the same as {@link #minimumPartSizeInBytes(Long)}.
         *
         * @param thresholdInBytes the value of the threshold to set.
         * @return an instance of this builder.
         */
        Builder thresholdInBytes(Long thresholdInBytes);

        /**
         * Indicates the value of the configured threshold.
         * @return the value of the threshold.
         */
        Long thresholdInBytes();

        /**
         * Configures the part size, in bytes, to be used in each individual part requests.
         * Only used for putObject and copyObject operations.
         * <p>
         * When uploading large payload, the size of the payload of each individual part requests might actually be
         * bigger than
         * the configured value since there is a limit to the maximum number of parts possible per multipart request. If the
         * configured part size would lead to a number of parts higher than the maximum allowed, a larger part size will be
         * calculated instead to allow fewer part to be uploaded, to avoid the limit imposed on the maximum number of parts.
         * <p>
         * In the case where the {@code minimumPartSizeInBytes} is set to a value higher than the {@code thresholdInBytes}, when
         * the client receive a request with a size smaller than a single part multipart operation will <em>NOT</em> be performed
         * even if the size of the request is larger than the threshold.
         * <p>
         * Default value: 8 Mib
         *
         * @param minimumPartSizeInBytes the value of the part size to set
         * @return an instance of this builder.
         */
        Builder minimumPartSizeInBytes(Long minimumPartSizeInBytes);

        /**
         * Indicated the value of the part configured size.
         * @return the value of the part size
         */
        Long minimumPartSizeInBytes();

        /**
         * Configures the maximum amount of memory, in bytes, the SDK will use to buffer content of requests in memory.
         * Increasing this value may lead to better performance at the cost of using more memory.
         * <p>
         * Default value: If not specified, the SDK will use the equivalent of four parts worth of memory, so 32 Mib by default.
         *
         * @param apiCallBufferSizeInBytes the value of the maximum memory usage.
         * @return an instance of this builder.
         */
        Builder apiCallBufferSizeInBytes(Long apiCallBufferSizeInBytes);

        /**
         * Indicates the value of the maximum memory usage that the SDK will use.
         * @return the value of the maximum memory usage.
         */
        Long apiCallBufferSizeInBytes();
    }

    private static class DefaultMultipartConfigBuilder implements Builder {
        private Long thresholdInBytes;
        private Long minimumPartSizeInBytes;
        private Long apiCallBufferSizeInBytes;

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
        public Builder apiCallBufferSizeInBytes(Long maximumMemoryUsageInBytes) {
            this.apiCallBufferSizeInBytes = maximumMemoryUsageInBytes;
            return this;
        }

        @Override
        public Long apiCallBufferSizeInBytes() {
            return apiCallBufferSizeInBytes;
        }

        @Override
        public MultipartConfiguration build() {
            return new MultipartConfiguration(this);
        }
    }
}
