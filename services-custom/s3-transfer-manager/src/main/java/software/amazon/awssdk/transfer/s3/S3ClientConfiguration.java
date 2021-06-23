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
import software.amazon.awssdk.annotations.SdkPreviewApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Optional Configurations for the underlying S3 client for which the TransferManager already provides
 * sensible defaults.
 *
 * <p>Use {@link #builder()} to create a set of options.</p>
 */
@SdkPublicApi
@SdkPreviewApi
public final class S3ClientConfiguration implements ToCopyableBuilder<S3ClientConfiguration.Builder, S3ClientConfiguration> {
    private final AwsCredentialsProvider credentialsProvider;
    private final Region region;
    private final Long minimumPartSizeInBytes;
    private final Double targetThroughputInGbps;
    private final Integer maxConcurrency;

    private S3ClientConfiguration(DefaultBuilder builder) {
        this.credentialsProvider = builder.credentialsProvider;
        this.region = builder.region;
        this.minimumPartSizeInBytes = Validate.isPositiveOrNull(builder.minimumPartSizeInBytes, "minimumPartSizeInBytes");
        this.targetThroughputInGbps = Validate.isPositiveOrNull(builder.targetThroughputInGbps, "targetThroughputInGbps");
        this.maxConcurrency = Validate.isPositiveOrNull(builder.maxConcurrency,
                                                        "maxConcurrency");
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
        return Optional.ofNullable(minimumPartSizeInBytes);
    }

    /**
     * @return the optional target throughput
     */
    public Optional<Double> targetThroughputInGbps() {
        return Optional.ofNullable(targetThroughputInGbps);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        S3ClientConfiguration that = (S3ClientConfiguration) o;

        if (!Objects.equals(credentialsProvider, that.credentialsProvider)) {
            return false;
        }
        if (!Objects.equals(region, that.region)) {
            return false;
        }
        if (!Objects.equals(minimumPartSizeInBytes, that.minimumPartSizeInBytes)) {
            return false;
        }
        if (!Objects.equals(targetThroughputInGbps, that.targetThroughputInGbps)) {
            return false;
        }
        return Objects.equals(maxConcurrency, that.maxConcurrency);
    }

    @Override
    public int hashCode() {
        int result = credentialsProvider != null ? credentialsProvider.hashCode() : 0;
        result = 31 * result + (region != null ? region.hashCode() : 0);
        result = 31 * result + (minimumPartSizeInBytes != null ? minimumPartSizeInBytes.hashCode() : 0);
        result = 31 * result + (targetThroughputInGbps != null ? targetThroughputInGbps.hashCode() : 0);
        result = 31 * result + (maxConcurrency != null ? maxConcurrency.hashCode() : 0);
        return result;
    }

    /**
     * Creates a default builder for {@link S3ClientConfiguration}.
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }

    /**
     * The builder definition for a {@link S3ClientConfiguration}.
     */
    public interface Builder extends CopyableBuilder<Builder, S3ClientConfiguration>  {

        /**
         * Configure the credentials that should be used to authenticate with S3.
         *
         * <p>The default provider will attempt to identify the credentials automatically using the following checks:
         * <ol>
         *   <li>Java System Properties - <code>aws.accessKeyId</code> and <code>aws.secretKey</code></li>
         *   <li>Environment Variables - <code>AWS_ACCESS_KEY_ID</code> and <code>AWS_SECRET_ACCESS_KEY</code></li>
         *   <li>Credential profiles file at the default location (~/.aws/credentials) shared by all AWS SDKs and the AWS CLI</li>
         *   <li>Credentials delivered through the Amazon EC2 container service if AWS_CONTAINER_CREDENTIALS_RELATIVE_URI
         *   environment variable is set and security manager has permission to access the variable.</li>
         *   <li>Instance profile credentials delivered through the Amazon EC2 metadata service</li>
         * </ol>
         *
         * <p>If the credentials are not found in any of the locations above, an exception will be thrown at {@link #build()}
         * time.
         * </p>
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
         * will be opened. Whether the transfer manager can achieve the configured target throughput depends
         * on various factors such as the network bandwidth of the environment and the configured {@link #maxConcurrency}.
         *
         * <p>
         * By default, it is 5Gbps
         *
         * @param targetThroughputInGbps the target throughput in Gbps
         * @return this builder for method chaining.
         * @see #maxConcurrency(Integer)
         */
        Builder targetThroughputInGbps(Double targetThroughputInGbps);

        /**
         * Specifies the maximum number of S3 connections that should be established during
         * a transfer.
         *
         * <p>
         * If not provided, the TransferManager will calculate the optional number of connections
         * based on {@link #targetThroughputInGbps}. If the value is too low, the S3TransferManager
         * might not achieve the specified target throughput.
         *
         * @param maxConcurrency the max number of concurrent requests
         * @return this builder for method chaining.
         * @see #targetThroughputInGbps(Double)
         */
        Builder maxConcurrency(Integer maxConcurrency);
    }

    private static final class DefaultBuilder implements Builder {
        private AwsCredentialsProvider credentialsProvider;
        private Region region;
        private Long minimumPartSizeInBytes;
        private Double targetThroughputInGbps;
        private Integer maxConcurrency;

        private DefaultBuilder() {
        }

        private DefaultBuilder(S3ClientConfiguration configuration) {
            this.credentialsProvider = configuration.credentialsProvider;
            this.region = configuration.region;
            this.minimumPartSizeInBytes = configuration.minimumPartSizeInBytes;
            this.targetThroughputInGbps = configuration.targetThroughputInGbps;
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
            this.minimumPartSizeInBytes = partSizeBytes;
            return this;
        }

        @Override
        public Builder targetThroughputInGbps(Double targetThroughputInGbps) {
            this.targetThroughputInGbps = targetThroughputInGbps;
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
