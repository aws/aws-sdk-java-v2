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

package software.amazon.awssdk.services.s3.internal.crt;

import static software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute.SDK_HTTP_EXECUTION_ATTRIBUTES;
import static software.amazon.awssdk.services.s3.internal.crt.S3InternalSdkHttpExecutionAttribute.HTTP_CHECKSUM;
import static software.amazon.awssdk.services.s3.internal.crt.S3InternalSdkHttpExecutionAttribute.OPERATION_NAME;
import static software.amazon.awssdk.services.s3.internal.crt.S3InternalSdkHttpExecutionAttribute.SIGNING_REGION;
import static software.amazon.awssdk.services.s3.internal.crt.S3NativeClientConfiguration.DEFAULT_PART_SIZE_IN_BYTES;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.checksums.ChecksumValidation;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.util.ClassLoaderHelper;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.signer.NoOpSigner;
import software.amazon.awssdk.crt.io.ExponentialBackoffRetryOptions;
import software.amazon.awssdk.crt.io.StandardRetryOptions;
import software.amazon.awssdk.http.SdkHttpExecutionAttributes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.DelegatingS3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;
import software.amazon.awssdk.services.s3.crt.S3CrtHttpConfiguration;
import software.amazon.awssdk.services.s3.crt.S3CrtRetryConfiguration;
import software.amazon.awssdk.services.s3.internal.multipart.CopyObjectHelper;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class DefaultS3CrtAsyncClient extends DelegatingS3AsyncClient implements S3CrtAsyncClient {
    public static final ExecutionAttribute<Path> OBJECT_FILE_PATH = new ExecutionAttribute<>("objectFilePath");
    private static final String CRT_CLIENT_CLASSPATH = "software.amazon.awssdk.crt.s3.S3Client";
    private final CopyObjectHelper copyObjectHelper;

    private DefaultS3CrtAsyncClient(DefaultS3CrtClientBuilder builder) {
        super(initializeS3AsyncClient(builder));
        long partSizeInBytes = builder.minimalPartSizeInBytes == null ? DEFAULT_PART_SIZE_IN_BYTES :
                               builder.minimalPartSizeInBytes;
        long thresholdInBytes = builder.thresholdInBytes == null ? partSizeInBytes : builder.thresholdInBytes;
        this.copyObjectHelper = new CopyObjectHelper((S3AsyncClient) delegate(),
                                                     partSizeInBytes,
                                                     thresholdInBytes);
    }

    @Override
    public CompletableFuture<PutObjectResponse> putObject(PutObjectRequest putObjectRequest, Path sourcePath) {
        AwsRequestOverrideConfiguration overrideConfig =
            putObjectRequest.overrideConfiguration()
                            .map(config -> config.toBuilder().putExecutionAttribute(OBJECT_FILE_PATH, sourcePath))
                            .orElseGet(() -> AwsRequestOverrideConfiguration.builder()
                                                                            .putExecutionAttribute(OBJECT_FILE_PATH, sourcePath))
                            .build();

        return putObject(putObjectRequest.toBuilder().overrideConfiguration(overrideConfig).build(),
                         new CrtContentLengthOnlyAsyncFileRequestBody(sourcePath));
    }

    @Override
    public CompletableFuture<CopyObjectResponse> copyObject(CopyObjectRequest copyObjectRequest) {
        return copyObjectHelper.copyObject(copyObjectRequest);
    }

    private static S3AsyncClient initializeS3AsyncClient(DefaultS3CrtClientBuilder builder) {
        ClientOverrideConfiguration.Builder overrideConfigurationBuilder =
            ClientOverrideConfiguration.builder()
                                       // Disable checksum, retry policy and signer because they are handled in crt
                                       .putAdvancedOption(SdkAdvancedClientOption.SIGNER, new NoOpSigner())
                                       .putExecutionAttribute(SdkExecutionAttribute.HTTP_RESPONSE_CHECKSUM_VALIDATION,
                                                              ChecksumValidation.FORCE_SKIP)
                                       .retryPolicy(RetryPolicy.none())
                                       .addExecutionInterceptor(new ValidateRequestInterceptor())
                                       .addExecutionInterceptor(new AttachHttpAttributesExecutionInterceptor());

        if (builder.executionInterceptors != null) {
            builder.executionInterceptors.forEach(overrideConfigurationBuilder::addExecutionInterceptor);
        }

        return S3AsyncClient.builder()
                            // Disable checksum, it is handled in CRT
                            .serviceConfiguration(S3Configuration.builder()
                                                                 .checksumValidationEnabled(false)
                                                                 .build())
                            .region(builder.region)
                            .endpointOverride(builder.endpointOverride)
                            .credentialsProvider(builder.credentialsProvider)
                            .overrideConfiguration(overrideConfigurationBuilder.build())
                            .accelerate(builder.accelerate)
                            .forcePathStyle(builder.forcePathStyle)
                            .crossRegionAccessEnabled(builder.crossRegionAccessEnabled)
                            .httpClientBuilder(initializeS3CrtAsyncHttpClient(builder))
                            .build();
    }

    private static S3CrtAsyncHttpClient.Builder initializeS3CrtAsyncHttpClient(DefaultS3CrtClientBuilder builder) {
        validateCrtInClassPath();
        Validate.isPositiveOrNull(builder.readBufferSizeInBytes, "initialReadBufferSizeInBytes");
        Validate.isPositiveOrNull(builder.maxConcurrency, "maxConcurrency");
        Validate.isPositiveOrNull(builder.targetThroughputInGbps, "targetThroughputInGbps");
        Validate.isPositiveOrNull(builder.minimalPartSizeInBytes, "minimalPartSizeInBytes");
        Validate.isPositiveOrNull(builder.thresholdInBytes, "thresholdInBytes");

        S3NativeClientConfiguration.Builder nativeClientBuilder =
            S3NativeClientConfiguration.builder()
                                       .checksumValidationEnabled(builder.checksumValidationEnabled)
                                       .targetThroughputInGbps(builder.targetThroughputInGbps)
                                       .partSizeInBytes(builder.minimalPartSizeInBytes)
                                       .maxConcurrency(builder.maxConcurrency)
                                       .signingRegion(builder.region == null ? null : builder.region.id())
                                       .endpointOverride(builder.endpointOverride)
                                       .credentialsProvider(builder.credentialsProvider)
                                       .readBufferSizeInBytes(builder.readBufferSizeInBytes)
                                       .httpConfiguration(builder.httpConfiguration)
                                       .thresholdInBytes(builder.thresholdInBytes);

        if (builder.retryConfiguration != null) {
            nativeClientBuilder.standardRetryOptions(
                new StandardRetryOptions()
                    .withBackoffRetryOptions(new ExponentialBackoffRetryOptions()
                                                 .withMaxRetries(builder.retryConfiguration.numRetries())));
        }
        return S3CrtAsyncHttpClient.builder()
                                   .s3ClientConfiguration(nativeClientBuilder.build());
    }

    public static final class DefaultS3CrtClientBuilder implements S3CrtAsyncClientBuilder {
        private Long readBufferSizeInBytes;
        private AwsCredentialsProvider credentialsProvider;
        private Region region;
        private Long minimalPartSizeInBytes;
        private Double targetThroughputInGbps;
        private Integer maxConcurrency;
        private URI endpointOverride;
        private Boolean checksumValidationEnabled;
        private S3CrtHttpConfiguration httpConfiguration;
        private Boolean accelerate;
        private Boolean forcePathStyle;

        private List<ExecutionInterceptor> executionInterceptors;
        private S3CrtRetryConfiguration retryConfiguration;
        private boolean crossRegionAccessEnabled;
        private Long thresholdInBytes;

        public AwsCredentialsProvider credentialsProvider() {
            return credentialsProvider;
        }

        public Region region() {
            return region;
        }

        public Long minimumPartSizeInBytes() {
            return minimalPartSizeInBytes;
        }

        public Double targetThroughputInGbps() {
            return targetThroughputInGbps;
        }

        public Integer maxConcurrency() {
            return maxConcurrency;
        }

        public URI endpointOverride() {
            return endpointOverride;
        }

        public Long readBufferSizeInBytes() {
            return readBufferSizeInBytes;
        }

        public boolean crossRegionAccessEnabled() {
            return crossRegionAccessEnabled;
        }

        @Override
        public S3CrtAsyncClientBuilder credentialsProvider(AwsCredentialsProvider credentialsProvider) {
            this.credentialsProvider = credentialsProvider;
            return this;
        }

        @Override
        public S3CrtAsyncClientBuilder region(Region region) {
            this.region = region;
            return this;
        }

        @Override
        public S3CrtAsyncClientBuilder minimumPartSizeInBytes(Long partSizeBytes) {
            this.minimalPartSizeInBytes = partSizeBytes;
            return this;
        }

        @Override
        public S3CrtAsyncClientBuilder targetThroughputInGbps(Double targetThroughputInGbps) {
            this.targetThroughputInGbps = targetThroughputInGbps;
            return this;
        }

        @Override
        public S3CrtAsyncClientBuilder maxConcurrency(Integer maxConcurrency) {
            this.maxConcurrency = maxConcurrency;
            return this;
        }

        @Override
        public S3CrtAsyncClientBuilder endpointOverride(URI endpointOverride) {
            this.endpointOverride = endpointOverride;
            return this;
        }

        @Override
        public S3CrtAsyncClientBuilder checksumValidationEnabled(Boolean checksumValidationEnabled) {
            this.checksumValidationEnabled = checksumValidationEnabled;
            return this;
        }

        @Override
        public S3CrtAsyncClientBuilder initialReadBufferSizeInBytes(Long readBufferSizeInBytes) {
            this.readBufferSizeInBytes = readBufferSizeInBytes;
            return this;
        }

        @Override
        public S3CrtAsyncClientBuilder httpConfiguration(S3CrtHttpConfiguration configuration) {
            this.httpConfiguration = configuration;
            return this;
        }

        @Override
        public S3CrtAsyncClientBuilder accelerate(Boolean accelerate) {
            this.accelerate = accelerate;
            return this;
        }

        @Override
        public S3CrtAsyncClientBuilder forcePathStyle(Boolean forcePathStyle) {
            this.forcePathStyle = forcePathStyle;
            return this;
        }

        @SdkTestInternalApi
        S3CrtAsyncClientBuilder addExecutionInterceptor(ExecutionInterceptor executionInterceptor) {
            if (executionInterceptors == null) {
                this.executionInterceptors = new ArrayList<>();
            }
            executionInterceptors.add(executionInterceptor);
            return this;
        }

        @Override
        public S3CrtAsyncClientBuilder retryConfiguration(S3CrtRetryConfiguration retryConfiguration) {
            this.retryConfiguration = retryConfiguration;
            return this;
        }

        @Override
        public S3CrtAsyncClientBuilder crossRegionAccessEnabled(Boolean crossRegionAccessEnabled) {
            this.crossRegionAccessEnabled = crossRegionAccessEnabled;
            return this;
        }

        @Override
        public S3CrtAsyncClientBuilder thresholdInBytes(Long thresholdInBytes) {
            this.thresholdInBytes = thresholdInBytes;
            return this;
        }

        @Override
        public S3CrtAsyncClient build() {
            return new DefaultS3CrtAsyncClient(this);
        }
    }

    private static final class AttachHttpAttributesExecutionInterceptor implements ExecutionInterceptor {
        @Override
        public void afterMarshalling(Context.AfterMarshalling context,
                                     ExecutionAttributes executionAttributes) {

            SdkHttpExecutionAttributes existingHttpAttributes = executionAttributes.getAttribute(SDK_HTTP_EXECUTION_ATTRIBUTES);

            SdkHttpExecutionAttributes.Builder builder = existingHttpAttributes != null ?
                                                         existingHttpAttributes.toBuilder() :
                                                         SdkHttpExecutionAttributes.builder();

            SdkHttpExecutionAttributes attributes =
                builder.put(OPERATION_NAME,
                            executionAttributes.getAttribute(SdkExecutionAttribute.OPERATION_NAME))
                       .put(HTTP_CHECKSUM, executionAttributes.getAttribute(SdkInternalExecutionAttribute.HTTP_CHECKSUM))
                       .put(SIGNING_REGION, executionAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNING_REGION))
                       .put(S3InternalSdkHttpExecutionAttribute.OBJECT_FILE_PATH,
                            executionAttributes.getAttribute(OBJECT_FILE_PATH))
                       .build();

            // For putObject and getObject, we rely on CRT to perform checksum validation
            disableChecksumForPutAndGet(context, executionAttributes);

            executionAttributes.putAttribute(SDK_HTTP_EXECUTION_ATTRIBUTES,
                                             attributes);
        }

        private static void disableChecksumForPutAndGet(Context.AfterMarshalling context,
                                                        ExecutionAttributes executionAttributes) {
            if (context.request() instanceof PutObjectRequest || context.request() instanceof GetObjectRequest) {
                // TODO: is there a better way to disable SDK flexible checksum implementation
                // Clear HTTP_CHECKSUM and RESOLVED_CHECKSUM_SPECS to disable SDK flexible checksum implementation.
                executionAttributes.putAttribute(SdkInternalExecutionAttribute.HTTP_CHECKSUM, null);
                executionAttributes.putAttribute(SdkInternalExecutionAttribute.RESOLVED_CHECKSUM_SPECS, null);
            }
        }
    }

    private static final class ValidateRequestInterceptor implements ExecutionInterceptor {
        @Override
        public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
            validateOverrideConfiguration(context.request());
        }

        private static void validateOverrideConfiguration(SdkRequest request) {
            if (!(request instanceof AwsRequest)) {
                return;
            }
            if (request.overrideConfiguration().isPresent()) {
                AwsRequestOverrideConfiguration overrideConfiguration =
                    (AwsRequestOverrideConfiguration) request.overrideConfiguration().get();
                if (overrideConfiguration.signer().isPresent()) {
                    throw new UnsupportedOperationException("Request-level signer override is not supported");
                }

                // TODO: support request-level credential override
                if (overrideConfiguration.credentialsProvider().isPresent()) {
                    throw new UnsupportedOperationException("Request-level credentials override is not supported");
                }

                if (!CollectionUtils.isNullOrEmpty(overrideConfiguration.metricPublishers())) {
                    throw new UnsupportedOperationException("Request-level Metric Publishers override is not supported");
                }

                if (overrideConfiguration.apiCallAttemptTimeout().isPresent()) {
                    throw new UnsupportedOperationException("Request-level apiCallAttemptTimeout override is not supported");
                }
            }
        }
    }

    private static void validateCrtInClassPath() {
        try {
            ClassLoaderHelper.loadClass(CRT_CLIENT_CLASSPATH, false);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Could not load classes from AWS Common Runtime (CRT) library."
                                            + "software.amazon.awssdk.crt:crt is a required dependency; make sure you have it "
                                            + "on the classpath.", e);
        }
    }
}
