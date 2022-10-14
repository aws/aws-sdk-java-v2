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

package software.amazon.awssdk.services.s3;

import java.net.URI;
import java.nio.file.Path;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * Builder API to build instance of Common Run Time based S3AsyncClient.
 */
@SdkPublicApi
public interface S3CrtAsyncClientBuilder extends SdkBuilder<S3CrtAsyncClientBuilder, S3AsyncClient> {


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
    S3CrtAsyncClientBuilder credentialsProvider(AwsCredentialsProvider credentialsProvider);

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
    S3CrtAsyncClientBuilder region(Region region);

    /**
     * Sets the minimum part size for transfer parts. Decreasing the minimum part size causes
     * multipart transfer to be split into a larger number of smaller parts. Setting this value too low
     * has a negative effect on transfer speeds, causing extra latency and network communication for each part.
     *
     * <p>
     * By default, it is 8MB
     *
     * @param uploadPartSize The minimum part size for transfer parts.
     * @return this builder for method chaining.
     */
    S3CrtAsyncClientBuilder minimumPartSizeInBytes(Long uploadPartSize);

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
    S3CrtAsyncClientBuilder targetThroughputInGbps(Double targetThroughputInGbps);

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
    S3CrtAsyncClientBuilder maxConcurrency(Integer maxConcurrency);

    /**
     * Configure the endpoint override with which the SDK should communicate.
     *
     * @param endpointOverride the endpoint override to be used
     * @return this builder for method chaining.
     */
    S3CrtAsyncClientBuilder endpointOverride(URI endpointOverride);

    /**
     * Option to disable checksum validation for streaming operations such as
     * {@link S3AsyncClient#getObject(GetObjectRequest, Path)}
     * and {@link S3AsyncClient#putObject(PutObjectRequest, Path)}
     *
     * <p>
     * Checksum validation using CRC32 is enabled by default.
     *
     */
    S3CrtAsyncClientBuilder checksumValidationEnabled(Boolean checksumValidationEnabled);

    S3CrtAsyncClientBuilder readBufferSizeInBytes(Long readBufferSizeInBytes);

    @Override
    S3AsyncClient build();
}