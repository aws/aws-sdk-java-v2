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

import static software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME;
import static software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR;
import static software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute.SDK_HTTP_EXECUTION_ATTRIBUTES;
import static software.amazon.awssdk.services.s3.internal.crt.S3InternalSdkHttpExecutionAttribute.HTTP_CHECKSUM;
import static software.amazon.awssdk.services.s3.internal.crt.S3InternalSdkHttpExecutionAttribute.OPERATION_NAME;
import static software.amazon.awssdk.services.s3.internal.crt.S3InternalSdkHttpExecutionAttribute.REQUEST_CHECKSUM_CALCULATION;
import static software.amazon.awssdk.services.s3.internal.crt.S3InternalSdkHttpExecutionAttribute.RESPONSE_CHECKSUM_VALIDATION;
import static software.amazon.awssdk.services.s3.internal.crt.S3InternalSdkHttpExecutionAttribute.SIGNING_NAME;
import static software.amazon.awssdk.services.s3.internal.crt.S3InternalSdkHttpExecutionAttribute.SIGNING_REGION;
import static software.amazon.awssdk.services.s3.internal.crt.S3InternalSdkHttpExecutionAttribute.USE_S3_EXPRESS_AUTH;
import static software.amazon.awssdk.services.s3.internal.crt.S3NativeClientConfiguration.DEFAULT_PART_SIZE_IN_BYTES;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.checksums.ChecksumValidation;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.util.ClassLoaderHelper;
import software.amazon.awssdk.core.signer.NoOpSigner;
import software.amazon.awssdk.crt.io.ExponentialBackoffRetryOptions;
import software.amazon.awssdk.crt.io.StandardRetryOptions;
import software.amazon.awssdk.crt.s3.S3MetaRequestOptions;
import software.amazon.awssdk.http.SdkHttpExecutionAttributes;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.DelegatingS3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;
import software.amazon.awssdk.services.s3.crt.S3CrtHttpConfiguration;
import software.amazon.awssdk.services.s3.crt.S3CrtRetryConfiguration;
import software.amazon.awssdk.services.s3.internal.checksums.ChecksumsEnabledValidator;
import software.amazon.awssdk.services.s3.internal.multipart.CopyObjectHelper;
import software.amazon.awssdk.services.s3.internal.s3express.S3ExpressUtils;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class DefaultS3CrtAsyncClient extends DelegatingS3AsyncClient implements S3CrtAsyncClient {
    public static final ExecutionAttribute<Path> OBJECT_FILE_PATH = new ExecutionAttribute<>("objectFilePath");
    public static final ExecutionAttribute<Path> RESPONSE_FILE_PATH = new ExecutionAttribute<>("responseFilePath");
    public static final ExecutionAttribute<S3MetaRequestOptions.ResponseFileOption> RESPONSE_FILE_OPTION =
        new ExecutionAttribute<>("responseFileOption");
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
    public CompletableFuture<GetObjectResponse> getObject(GetObjectRequest getObjectRequest, Path destinationPath) {
        AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> responseTransformer =
            new CrtResponseFileResponseTransformer<>();

        AwsRequestOverrideConfiguration overrideConfig =
            getObjectRequest.overrideConfiguration()
                            .map(config -> config.toBuilder().putExecutionAttribute(RESPONSE_FILE_PATH, destinationPath))
                            .orElseGet(() -> AwsRequestOverrideConfiguration.builder()
                                                                            .putExecutionAttribute(RESPONSE_FILE_PATH,
                                                                                                   destinationPath))
                            .build();

        return getObject(getObjectRequest.toBuilder().overrideConfiguration(overrideConfig).build(), responseTransformer);
    }

    @Override
    public CompletableFuture<CopyObjectResponse> copyObject(CopyObjectRequest copyObjectRequest) {
        return copyObjectHelper.copyObject(copyObjectRequest);
    }

    private static S3AsyncClient initializeS3AsyncClient(DefaultS3CrtClientBuilder builder) {
        ClientOverrideConfiguration.Builder overrideConfigurationBuilder =
            ClientOverrideConfiguration.builder()
                                       // Disable checksum for streaming operations, retry policy and signer because they are
                                       // handled in crt
                                       .putAdvancedOption(SdkAdvancedClientOption.SIGNER, new NoOpSigner())
                                       .putExecutionAttribute(SdkExecutionAttribute.HTTP_RESPONSE_CHECKSUM_VALIDATION,
                                                              ChecksumValidation.FORCE_SKIP)
                                       .putExecutionAttribute(ChecksumsEnabledValidator.SKIP_MD5_TRAILING_CHECKSUM, true)
                                       .retryStrategy(AwsRetryStrategy.doNotRetry())
                                       .addExecutionInterceptor(new ValidateRequestInterceptor())
                                       .addExecutionInterceptor(new AttachHttpAttributesExecutionInterceptor());

        if (builder.executionInterceptors != null) {
            builder.executionInterceptors.forEach(overrideConfigurationBuilder::addExecutionInterceptor);
        }

        DefaultS3CrtClientBuilder finalBuilder = resolveChecksumConfiguration(builder);

        S3AsyncClientBuilder s3AsyncClientBuilder =
            S3AsyncClient.builder()
                         .requestChecksumCalculation(finalBuilder.requestChecksumCalculation)
                         .responseChecksumValidation(finalBuilder.responseChecksumValidation)
                         .region(finalBuilder.region)
                         .endpointOverride(finalBuilder.endpointOverride)
                         .credentialsProvider(finalBuilder.credentialsProvider)
                         .overrideConfiguration(overrideConfigurationBuilder.build())
                         .accelerate(finalBuilder.accelerate)
                         .forcePathStyle(finalBuilder.forcePathStyle)
                         .crossRegionAccessEnabled(finalBuilder.crossRegionAccessEnabled)
                         .putAuthScheme(new CrtS3ExpressNoOpAuthScheme())
                         .httpClientBuilder(initializeS3CrtAsyncHttpClient(finalBuilder))
                         .disableS3ExpressSessionAuth(finalBuilder.disableS3ExpressSessionAuth);


        if (finalBuilder.futureCompletionExecutor != null) {
            s3AsyncClientBuilder.asyncConfiguration(b -> b.advancedOption(FUTURE_COMPLETION_EXECUTOR,
                                                                          finalBuilder.futureCompletionExecutor));
        }
        return s3AsyncClientBuilder.build();
    }

    private static DefaultS3CrtClientBuilder resolveChecksumConfiguration(DefaultS3CrtClientBuilder builder) {
        Boolean checksumEnabledValidation = builder.checksumValidationEnabled;
        RequestChecksumCalculation requestChecksumCalculation = builder.requestChecksumCalculation;
        ResponseChecksumValidation responseChecksumValidation = builder.responseChecksumValidation;

        if (checksumEnabledValidation != null) {
            Validate.validState(requestChecksumCalculation == null && responseChecksumValidation == null,
                                "Checksum behavior has been configured on the S3CrtAsyncClientBuilder using the deprecated "
                                + "checksumEnabledValidation() AND one or both of requestChecksumCalculation() and "
                                + "responseChecksumValidation()");
            if (checksumEnabledValidation) {
                requestChecksumCalculation = RequestChecksumCalculation.WHEN_SUPPORTED;
                responseChecksumValidation = ResponseChecksumValidation.WHEN_SUPPORTED;
            } else {
                requestChecksumCalculation = RequestChecksumCalculation.WHEN_REQUIRED;
                responseChecksumValidation = ResponseChecksumValidation.WHEN_REQUIRED;
            }
        }

        return builder.requestChecksumCalculation(requestChecksumCalculation)
                      .responseChecksumValidation(responseChecksumValidation);
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
                                       .targetThroughputInGbps(builder.targetThroughputInGbps)
                                       .partSizeInBytes(builder.minimalPartSizeInBytes)
                                       .maxConcurrency(builder.maxConcurrency)
                                       .signingRegion(builder.region == null ? null : builder.region.id())
                                       .endpointOverride(builder.endpointOverride)
                                       .credentialsProvider(builder.credentialsProvider)
                                       .readBufferSizeInBytes(builder.readBufferSizeInBytes)
                                       .httpConfiguration(builder.httpConfiguration)
                                       .thresholdInBytes(builder.thresholdInBytes)
                                       .maxNativeMemoryLimitInBytes(builder.maxNativeMemoryLimitInBytes);

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
        private IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider;
        private Region region;
        private Long minimalPartSizeInBytes;
        private Double targetThroughputInGbps;
        private Long maxNativeMemoryLimitInBytes;
        private Integer maxConcurrency;
        private URI endpointOverride;
        private Boolean checksumValidationEnabled;
        private RequestChecksumCalculation requestChecksumCalculation;
        private ResponseChecksumValidation responseChecksumValidation;
        private S3CrtHttpConfiguration httpConfiguration;
        private Boolean accelerate;
        private Boolean forcePathStyle;

        private List<ExecutionInterceptor> executionInterceptors;
        private S3CrtRetryConfiguration retryConfiguration;
        private boolean crossRegionAccessEnabled;
        private Long thresholdInBytes;
        private Executor futureCompletionExecutor;
        private Boolean disableS3ExpressSessionAuth;

        @Override
        public DefaultS3CrtClientBuilder credentialsProvider(
            IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider) {
            this.credentialsProvider = credentialsProvider;
            return this;
        }

        @Override
        public DefaultS3CrtClientBuilder region(Region region) {
            this.region = region;
            return this;
        }

        @Override
        public DefaultS3CrtClientBuilder minimumPartSizeInBytes(Long partSizeBytes) {
            this.minimalPartSizeInBytes = partSizeBytes;
            return this;
        }

        @Override
        public DefaultS3CrtClientBuilder targetThroughputInGbps(Double targetThroughputInGbps) {
            this.targetThroughputInGbps = targetThroughputInGbps;
            return this;
        }

        @Override
        public DefaultS3CrtClientBuilder maxNativeMemoryLimitInBytes(Long maxNativeMemoryLimitInBytes) {
            this.maxNativeMemoryLimitInBytes = maxNativeMemoryLimitInBytes;
            return this;
        }

        @Override
        public DefaultS3CrtClientBuilder maxConcurrency(Integer maxConcurrency) {
            this.maxConcurrency = maxConcurrency;
            return this;
        }

        @Override
        public DefaultS3CrtClientBuilder endpointOverride(URI endpointOverride) {
            this.endpointOverride = endpointOverride;
            return this;
        }

        @Override
        public DefaultS3CrtClientBuilder checksumValidationEnabled(Boolean checksumValidationEnabled) {
            this.checksumValidationEnabled = checksumValidationEnabled;
            return this;
        }

        @Override
        public DefaultS3CrtClientBuilder requestChecksumCalculation(RequestChecksumCalculation requestChecksumCalculation) {
            this.requestChecksumCalculation = requestChecksumCalculation;
            return this;
        }

        @Override
        public DefaultS3CrtClientBuilder responseChecksumValidation(ResponseChecksumValidation responseChecksumValidation) {
            this.responseChecksumValidation = responseChecksumValidation;
            return this;
        }

        @Override
        public DefaultS3CrtClientBuilder initialReadBufferSizeInBytes(Long readBufferSizeInBytes) {
            this.readBufferSizeInBytes = readBufferSizeInBytes;
            return this;
        }

        @Override
        public DefaultS3CrtClientBuilder httpConfiguration(S3CrtHttpConfiguration configuration) {
            this.httpConfiguration = configuration;
            return this;
        }

        @Override
        public DefaultS3CrtClientBuilder accelerate(Boolean accelerate) {
            this.accelerate = accelerate;
            return this;
        }

        @Override
        public DefaultS3CrtClientBuilder forcePathStyle(Boolean forcePathStyle) {
            this.forcePathStyle = forcePathStyle;
            return this;
        }

        @SdkTestInternalApi
        DefaultS3CrtClientBuilder addExecutionInterceptor(ExecutionInterceptor executionInterceptor) {
            if (executionInterceptors == null) {
                this.executionInterceptors = new ArrayList<>();
            }
            executionInterceptors.add(executionInterceptor);
            return this;
        }

        @Override
        public DefaultS3CrtClientBuilder retryConfiguration(S3CrtRetryConfiguration retryConfiguration) {
            this.retryConfiguration = retryConfiguration;
            return this;
        }

        @Override
        public DefaultS3CrtClientBuilder crossRegionAccessEnabled(Boolean crossRegionAccessEnabled) {
            this.crossRegionAccessEnabled = crossRegionAccessEnabled;
            return this;
        }

        @Override
        public DefaultS3CrtClientBuilder thresholdInBytes(Long thresholdInBytes) {
            this.thresholdInBytes = thresholdInBytes;
            return this;
        }

        @Override
        public DefaultS3CrtClientBuilder futureCompletionExecutor(Executor futureCompletionExecutor) {
            this.futureCompletionExecutor = futureCompletionExecutor;
            return this;
        }

        @Override
        public DefaultS3CrtClientBuilder disableS3ExpressSessionAuth(Boolean disableS3ExpressSessionAuth) {
            this.disableS3ExpressSessionAuth = disableS3ExpressSessionAuth;
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
                       .put(USE_S3_EXPRESS_AUTH, S3ExpressUtils.useS3ExpressAuthScheme(executionAttributes))
                       .put(SIGNING_NAME, executionAttributes.getAttribute(SERVICE_SIGNING_NAME))
                       .put(REQUEST_CHECKSUM_CALCULATION,
                            executionAttributes.getAttribute(SdkInternalExecutionAttribute.REQUEST_CHECKSUM_CALCULATION))
                       .put(RESPONSE_CHECKSUM_VALIDATION,
                            executionAttributes.getAttribute(SdkInternalExecutionAttribute.RESPONSE_CHECKSUM_VALIDATION))
                       .put(S3InternalSdkHttpExecutionAttribute.RESPONSE_FILE_PATH,
                            executionAttributes.getAttribute(RESPONSE_FILE_PATH))
                       .put(S3InternalSdkHttpExecutionAttribute.RESPONSE_FILE_OPTION,
                            executionAttributes.getAttribute(RESPONSE_FILE_OPTION))
                       .build();

            // We rely on CRT to perform checksum validation, disable SDK flexible checksum implementation
            executionAttributes.putAttribute(SdkInternalExecutionAttribute.HTTP_CHECKSUM, null);
            executionAttributes.putAttribute(SdkExecutionAttribute.RESOLVED_CHECKSUM_SPECS, null);

            executionAttributes.putAttribute(SDK_HTTP_EXECUTION_ATTRIBUTES,
                                             attributes);
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
                if (overrideConfiguration.credentialsIdentityProvider().isPresent()) {
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
