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

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPreviewApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Configuration values for which the TransferManager already provides sensible defaults. All values are optional
 *
 * <p>Use {@link #builder()} to create a set of options.</p>
 */
@SdkPublicApi
@SdkPreviewApi
public final class S3ClientConfiguration implements ToCopyableBuilder<S3ClientConfiguration.Builder, S3ClientConfiguration> {
    private final AwsCredentialsProvider credentialsProvider;
    private final Region region;
    private final Long partSizeBytes;
    private final Double targetThroughputGbps;
    private final Integer maxConcurrency;

    private S3ClientConfiguration(DefaultBuilder builder) {
        this.credentialsProvider = builder.credentialsProvider;
        this.region = builder.region;
        this.partSizeBytes = builder.partSizeBytes;
        this.targetThroughputGbps = builder.targetThroughputGbps;
        this.maxConcurrency = builder.maxConcurrency;
    }

    /**
     * @return the optional credentials that should be used to authenticate with S3.
     */
    public Optional<AwsCredentialsProvider> credentialsProvider() {
        return Optional.ofNullable(credentialsProvider);
    }

    /**
     * @return the optional region with which the SDK should communicate.
     */
    public Optional<Region> region() {
        return Optional.ofNullable(region);
    }

    /**
     * @return the optional minimum part size for transfer parts.
     */
    public Optional<Long> minimumPartSizeInBytes() {
        return Optional.ofNullable(partSizeBytes);
    }

    /**
     * @return the optional target throughput
     */
    public Optional<Double> targetThroughputGbps() {
        return Optional.ofNullable(targetThroughputGbps);
    }

    /**
     * @return the optional maximum number of concurrent Amazon S3 transfer requests that can run at the same time.
     */
    public Optional<Integer> maxConcurrency() {
        return Optional.ofNullable(maxConcurrency);
    }

    @Override
    public Builder toBuilder() {
        return new DefaultBuilder(this);
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }

    public interface Builder extends CopyableBuilder<Builder, S3ClientConfiguration>  {

        /**
         * Configure the credentials that should be used to authenticate with S3.
         *
         * @param credentialsProvider the credentials to use
         * @return This builder for method chaining.
         */
        Builder credentialsProvider(AwsCredentialsProvider credentialsProvider);

        /**
         * Configure the region with which the SDK should communicate.
         *
         * <p>If this is not specified, the SDK will attempt to identify the endpoint automatically using the following logic:
         * <ol>
         *     <li>Check the 'aws.region' system property for the region.</li>
         *     <li>Check the 'AWS_REGION' environment variable for the region.</li>
         *     <li>Check the {user.home}/.aws/credentials and {user.home}/.aws/config files for the region.</li>
         *     <li>If running in EC2, check the EC2 metadata service for the region.</li>
         * </ol>
         *
         * @param region the region to be used
         * @return this builder for method chaining.
         */
        Builder region(Region region);

        /**
         * Sets the minimum part size for transfer parts. Decreasing the minimum part size causes
         * multipart transfer to be split into a larger number of smaller parts. Setting this value too low
         * has a negative effect on transfer speeds, causing extra latency and network communication for each part.
         *
         * <p>
         * By default, it is 8MB
         *
         * @param partSizeBytes The minimum part size for transfer parts.
         * @return this builder for method chaining.
         */
        Builder minimumPartSizeInBytes(Long partSizeBytes);

        /**
         * The target throughput for transfer requests. Higher value means more S3 connections
         * will be opened.
         *
         * <p>
         * By default, it is 5Gbps
         *
         * @param targetThroughputGbps the target throughput Gbps
         * @return this builder for method chaining.
         */
        Builder targetThroughputGbps(Double targetThroughputGbps);

        /**
         * Specifies the maximum number of concurrent Amazon S3 transfer requests that can run at the same time.
         *
         * @param maxConcurrency the max number of concurrent requests
         * @return this builder for method chaining.
         */
        Builder maxConcurrency(Integer maxConcurrency);
    }

    private static final class DefaultBuilder implements Builder {
        private AwsCredentialsProvider credentialsProvider;
        private Region region;
        private Long partSizeBytes;
        private Double targetThroughputGbps;
        private Integer maxConcurrency;

        private DefaultBuilder() {
        }

        private DefaultBuilder(S3ClientConfiguration configuration) {
            this.credentialsProvider = configuration.credentialsProvider;
            this.region = configuration.region;
            this.partSizeBytes = configuration.partSizeBytes;
            this.targetThroughputGbps = configuration.targetThroughputGbps;
            this.maxConcurrency = configuration.maxConcurrency;
        }

        @Override
        public Builder credentialsProvider(AwsCredentialsProvider credentialsProvider) {
            this.credentialsProvider = credentialsProvider;
            return this;
        }

        @Override
        public Builder region(Region region) {
            this.region = region;
            return this;
        }

        @Override
        public Builder minimumPartSizeInBytes(Long partSizeBytes) {
            this.partSizeBytes = partSizeBytes;
            return this;
        }

        @Override
        public Builder targetThroughputGbps(Double targetThroughputGbps) {
            this.targetThroughputGbps = targetThroughputGbps;
            return this;
        }

        @Override
        public Builder maxConcurrency(Integer maxConcurrency) {
            this.maxConcurrency = maxConcurrency;
            return this;
        }

        @Override
        public S3ClientConfiguration build() {
            return new S3ClientConfiguration(this);
        }
    }
}
