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
import static software.amazon.awssdk.services.s3.internal.crt.S3InternalSdkHttpExecutionAttribute.OPERATION_NAME;
import static software.amazon.awssdk.services.s3.internal.crt.S3InternalSdkHttpExecutionAttribute.CHECKSUM_SPECS;

import java.net.URI;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.signer.NoOpSigner;
import software.amazon.awssdk.http.SdkHttpExecutionAttributes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.internal.DelegatingS3AsyncClient;

@SdkInternalApi
public final class DefaultS3CrtAsyncClient extends DelegatingS3AsyncClient implements S3CrtAsyncClient {

    private DefaultS3CrtAsyncClient(DefaultS3CrtClientBuilder builder) {
        super(initializeS3AsyncClient(builder));
    }

    private static S3AsyncClient initializeS3AsyncClient(DefaultS3CrtClientBuilder builder) {
        return S3AsyncClient.builder()
                            // Disable checksum, retry policy and signer because they are handled in crt
                            .serviceConfiguration(S3Configuration.builder()
                                                                 .checksumValidationEnabled(false)
                                                                 .build())
                            .region(builder.region)
                            .endpointOverride(builder.endpointOverride)
                            .credentialsProvider(builder.credentialsProvider)
                            .overrideConfiguration(o -> o.putAdvancedOption(SdkAdvancedClientOption.SIGNER,
                                                                            new NoOpSigner())
                                                         .retryPolicy(RetryPolicy.none())
                                                         .addExecutionInterceptor(new ValidateRequestInterceptor())
                                                         .addExecutionInterceptor(new AttachHttpAttributesExecutionInterceptor()))
                            .httpClientBuilder(initializeS3CrtAsyncHttpClient(builder))
                            .build();
    }

    private static S3CrtAsyncHttpClient.Builder initializeS3CrtAsyncHttpClient(DefaultS3CrtClientBuilder builder) {
        return S3CrtAsyncHttpClient.builder()
                                   .targetThroughputInGbps(builder.targetThroughputInGbps())
                                   .minimumPartSizeInBytes(builder.minimumPartSizeInBytes())
                                   .maxConcurrency(builder.maxConcurrency)
                                   .region(builder.region)
                                   .endpointOverride(builder.endpointOverride)
                                   .credentialsProvider(builder.credentialsProvider);
    }

    @Override
    public String serviceName() {
        return SERVICE_NAME;
    }

    public static final class DefaultS3CrtClientBuilder implements S3CrtAsyncClientBuilder {
        private AwsCredentialsProvider credentialsProvider;
        private Region region;
        private Long minimalPartSizeInBytes;
        private Double targetThroughputInGbps;
        private Integer maxConcurrency;
        private URI endpointOverride;

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
        public S3CrtAsyncClient build() {
            return new DefaultS3CrtAsyncClient(this);
        }
    }

    private static final class AttachHttpAttributesExecutionInterceptor implements ExecutionInterceptor {
        @Override
        public void afterMarshalling(Context.AfterMarshalling context,
                                     ExecutionAttributes executionAttributes) {
            SdkHttpExecutionAttributes attributes =
                SdkHttpExecutionAttributes.builder()
                                          .put(OPERATION_NAME,
                                               executionAttributes.getAttribute(SdkExecutionAttribute.OPERATION_NAME))
                                          .put(CHECKSUM_SPECS,
                                               executionAttributes.getAttribute(SdkExecutionAttribute.RESOLVED_CHECKSUM_SPECS))
                                          .build();

            executionAttributes.putAttribute(SDK_HTTP_EXECUTION_ATTRIBUTES, attributes);
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
            }
        }
    }
}
