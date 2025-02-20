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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.crt.S3CrtHttpConfiguration;
import software.amazon.awssdk.services.s3.crt.S3CrtRetryConfiguration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.utils.Validate;
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
    default S3CrtAsyncClientBuilder credentialsProvider(AwsCredentialsProvider credentialsProvider) {
        return credentialsProvider((IdentityProvider<? extends AwsCredentialsIdentity>) credentialsProvider);
    }

    /**
     * Configure the credentials that should be used to authenticate with S3.
     *
     * <p>The default provider will attempt to identify the credentials automatically using the following checks:
     * <ol>
     *   <li>Java System Properties - {@code aws.accessKeyId} and {@code aws.secretKey}</li>
     *   <li>Environment Variables - {@code AWS_ACCESS_KEY_ID} and {@code AWS_SECRET_ACCESS_KEY}</li>
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
    default S3CrtAsyncClientBuilder credentialsProvider(IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider) {
        throw new UnsupportedOperationException();
    }

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
     * Sets the minimum part size for transfer parts. Decreasing the minimum part size causes multipart transfer to be split into
     * a larger number of smaller parts. Setting this value too low has a negative effect on transfer speeds, causing extra
     * latency and network communication for each part.
     *
     * <p>
     * By default, it is 8MB. See <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/qfacts.html">Amazon S3 multipart
     * upload limits</a> for guidance.
     *
     * @param uploadPartSize The minimum part size for transfer parts.
     * @return this builder for method chaining.
     */
    S3CrtAsyncClientBuilder minimumPartSizeInBytes(Long uploadPartSize);

    /**
     * The amount of native memory that CRT is allowed to use when making requests to S3.
     * <p>
     * If not provided, the CRT attempts to limit native memory usage in an optimal way, based on a number of parameters
     * such as target throughput. Therefore, only configure the memory limit explicitly when needed.
     * <p>
     * Supported range:
     * <ul>
     *     <li><b>Min: </b>1 GB</li>
     *     <li><b>Max: </b>The lowest value of the supplied value and the SIZE_MAX of the system</li>
     * </ul>
     *
     * @param maxNativeMemoryLimitInBytes
 the native memory limit in bytes
     * @return this builder for method chaining.
     * @see #targetThroughputInGbps(Double)
     */
    S3CrtAsyncClientBuilder maxNativeMemoryLimitInBytes(Long maxNativeMemoryLimitInBytes
);

    /**
     * The target throughput for transfer requests. Higher value means more connections will be established with S3.
     *
     * <p>
     * Whether the transfer manager can achieve the configured target throughput depends on various factors such as the network
     * bandwidth of the environment, whether {@link #maxConcurrency} is configured and amount of available memory.
     *
     * <p>
     * By default, it is 10 gigabits per second. If users want to transfer as fast as possible, it's recommended to set it to the
     * maximum network bandwidth on the host that the application is running on. For EC2 instances, you can find network
     * bandwidth for a specific
     * instance type in <a href="https://aws.amazon.com/ec2/instance-types/">Amazon EC2 instance type page</a>.
     * If you are running into out of file descriptors error, consider using {@link #maxConcurrency(Integer)} to limit the
     * number of connections.
     * <p>
     * <b>Note: </b> This setting affects the native memory usage used by CRT; a higher throughput value will result in a larger
     * memory usage. Typically, a range of throughput values maps to a discrete memory limit value in CRT, with a maximum upper
     * limit.
     *
     * @param targetThroughputInGbps the target throughput in Gbps
     * @return this builder for method chaining.
     * @see #maxConcurrency(Integer)
     * @see #maxNativeMemoryLimitInBytes(Long)
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
     * Option to disable checksum validation for {@link S3AsyncClient#getObject(GetObjectRequest, Path)} and
     * {@link S3AsyncClient#putObject(PutObjectRequest, Path)}.
     *
     * <p>
     * Checksum validation using CRC32 is enabled by default.
     *
     * @deprecated This option has been replaced with
     * {@link S3CrtAsyncClientBuilder#requestChecksumCalculation(RequestChecksumCalculation)} and
     * {@link S3CrtAsyncClientBuilder#responseChecksumValidation(ResponseChecksumValidation)}. If both this and one of those
     * options are set, an exception will be thrown.
     */
    @Deprecated
    S3CrtAsyncClientBuilder checksumValidationEnabled(Boolean checksumValidationEnabled);

    /**
     * Configures the client behavior for request checksum calculation.
     */
    S3CrtAsyncClientBuilder requestChecksumCalculation(RequestChecksumCalculation requestChecksumCalculation);

    /**
     * Configures the client behavior for response checksum validation.
     */
    S3CrtAsyncClientBuilder responseChecksumValidation(ResponseChecksumValidation responseChecksumValidation);

    /**
     * Configure the starting buffer size the client will use to buffer the parts downloaded from S3. Maintain a larger window to
     * keep up a high download throughput; parts cannot download in parallel unless the window is large enough to hold multiple
     * parts. Maintain a smaller window to limit the amount of data buffered in memory.
     *
     * <p>
     * By default, it is equal to the resolved part size * 10
     *
     * @param initialReadBufferSizeInBytes the initial read buffer size
     * @return this builder for method chaining.
     */
    S3CrtAsyncClientBuilder initialReadBufferSizeInBytes(Long initialReadBufferSizeInBytes);


    /**
     * Sets the HTTP configuration to use for this client.
     *
     * @param configuration The http proxy configuration to use
     * @return The builder of the method chaining.
     */
    S3CrtAsyncClientBuilder httpConfiguration(S3CrtHttpConfiguration configuration);

    /**
     * Sets the Retry configuration to use for this client.
     *
     * @param retryConfiguration The retry configurations to be used.
     * @return The builder of the method chaining.
     */
    S3CrtAsyncClientBuilder retryConfiguration(S3CrtRetryConfiguration retryConfiguration);

    /**
     * A convenience method that creates an instance of the {@link S3CrtHttpConfiguration} builder, avoiding the
     * need to create one manually via {@link S3CrtHttpConfiguration#builder()}.
     *
     * @param configurationBuilder The health checks config builder to use
     * @return The builder of the method chaining.
     * @see #httpConfiguration(S3CrtHttpConfiguration)
     */
    default S3CrtAsyncClientBuilder httpConfiguration(Consumer<S3CrtHttpConfiguration.Builder> configurationBuilder) {
        Validate.paramNotNull(configurationBuilder, "configurationBuilder");
        return httpConfiguration(S3CrtHttpConfiguration.builder()
                                                       .applyMutation(configurationBuilder)
                                                       .build());
    }

    // S3 client context params, copied from S3BaseClientBuilder. Note we only have accelerate and path style because they're
    // the only ones we can support in the CRT client (does not affect signing).
    /**
     * Enables this client to use S3 Transfer Acceleration endpoints.
     */
    S3CrtAsyncClientBuilder accelerate(Boolean accelerate);

    /**
     * Forces this client to use path-style addressing for buckets.
     */
    S3CrtAsyncClientBuilder forcePathStyle(Boolean forcePathStyle);

    /**
     * A convenience method that creates an instance of the {@link S3CrtRetryConfiguration} builder, avoiding the
     * need to create one manually via {@link S3CrtRetryConfiguration#builder()}.
     *
     * @param retryConfigurationBuilder The retry config builder to use
     * @return The builder of the method chaining.
     * @see #retryConfiguration(S3CrtRetryConfiguration)
     */
    default S3CrtAsyncClientBuilder retryConfiguration(Consumer<S3CrtRetryConfiguration.Builder> retryConfigurationBuilder) {
        Validate.paramNotNull(retryConfigurationBuilder, "retryConfigurationBuilder");
        return retryConfiguration(S3CrtRetryConfiguration.builder()
                                                         .applyMutation(retryConfigurationBuilder)
                                                         .build());
    }

    /**
     * <p> Configures whether cross-region bucket access is enabled for clients using the configuration.
     * <p>The following behavior is used when this mode is enabled:
     * <ol>
     *     <li>This method allows enabling or disabling cross-region bucket access for clients. When cross-region bucket
     *     access is enabled, requests that do not act on an existing bucket (e.g., createBucket API) will be routed to the
     *     region configured on the client</li>
     *     <li>The first time a request is made that references an existing bucket (e.g., putObject API), a request will be
     *     made to the client-configured region. If the bucket does not exist in this region, the service will include the
     *     actual region in the error responses. Subsequently, the API will be called using the correct region obtained
     *     from the error response. </li>
     *     <li>This location may be cached in the client for subsequent requests to the same bucket.</li>
     * </ol>
     * <p>Enabling this mode has several drawbacks, as it can increase latency if the bucket's location is physically far
     * from the location of the request.Therefore, it is strongly advised, whenever possible, to know the location of your
     * buckets and create a region-specific client to access them
     *
     * @param crossRegionAccessEnabled Whether cross region bucket access should be enabled.
     * @return The builder object for method chaining.
     */
    S3CrtAsyncClientBuilder crossRegionAccessEnabled(Boolean crossRegionAccessEnabled);

    /**
     * Configure the size threshold, in bytes, for when to use multipart upload. Uploads/copies over this size will automatically
     * use a multipart upload strategy, while uploads/copies smaller than this threshold will use a single connection to
     * upload/copy the whole object.
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
    S3CrtAsyncClientBuilder thresholdInBytes(Long thresholdInBytes);

    /**
     * Configure the {@link Executor} that should be used to complete the {@link CompletableFuture} that is returned by the async
     * service client. By default, this is a dedicated, per-client {@link ThreadPoolExecutor} that is managed by the SDK.
     * <p>
     * The configured {@link Executor} will be invoked by the async HTTP client's I/O threads (e.g., EventLoops), which must be
     * reserved for non-blocking behavior. Blocking an I/O thread can cause severe performance degradation, including across
     * multiple clients, as clients are configured, by default, to share a single I/O thread pool (e.g., EventLoopGroup).
     * <p>
     * You should typically only want to customize the future-completion {@link Executor} for a few possible reasons:
     * <ol>
     *     <li>You want more fine-grained control over the {@link ThreadPoolExecutor} used, such as configuring the pool size
     *     or sharing a single pool between multiple clients.
     *     <li>You want to add instrumentation (i.e., metrics) around how the {@link Executor} is used.
     * </ol>
     * <p>
     * <b>WARNING</b>
     * We strongly <strong>discourage</strong> using {@code Runnable::run}, which executes the future-completion directly from
     * within the I/O thread because it may block the I/O thread and cause deadlock, especially if you are sending
     * another SDK request in the {@link CompletableFuture} chain since the SDK may perform blocking calls in some cases.
     *
     * @param futureCompletionExecutor the executor
     * @return an instance of this builder.
     */
    S3CrtAsyncClientBuilder futureCompletionExecutor(Executor futureCompletionExecutor);

    /**
     * Configure whether to disable this client's usage of Session Auth for S3Express buckets and reverts to using conventional
     * SigV4 for those.
     *
     * @param disableS3ExpressSessionAuth whether Session Auth for S3Express should be disabled
     * @return an instance of this builder
     */
    S3CrtAsyncClientBuilder disableS3ExpressSessionAuth(Boolean disableS3ExpressSessionAuth);


    @Override
    S3AsyncClient build();
}